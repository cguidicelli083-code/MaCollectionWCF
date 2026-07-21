#!/usr/bin/env python3
"""
Scraper de nouveautes WCF (World Collectable Figure, Banpresto/Bandai Spirits)
depuis le site officiel bsp-prize.jp.

Usage:
    python scrape_wcf_news.py [--db wcf_news.sqlite3] [--out wcf_news.json]
                               [--images-dir images/wcf] [--max-pages 15]
                               [--delay 1.5]

Le script combine deux sources bsp-prize.jp (memes structure/markup HTML) :
  1. /schedule/ : planning de sortie a court terme (utile pour la date precise).
  2. /search/?kw=<mot-cle WCF> : recherche plein texte sur TOUT le catalogue
     (~400 produits, toutes licences confondues -- One Piece, Dragon Ball,
     HUNTER×HUNTER, Chainsaw Man, etc. -- bien plus large que le seul planning
     a court terme, qui ne remonte que 1-2 boites a la fois).
Les deux sources sont fusionnees (dedup sur l'identifiant produit dans l'URL
/item/<id>/) avant traitement, pour ne jamais visiter deux fois la meme fiche.

Pour chaque produit WCF trouve (nom contenant "ワールドコレクタブルフィギュア" --
le site couvre TOUTES les gammes Banpresto/prize, pas seulement WCF) :
  1. Visite la fiche produit pour recuperer le detail (personnages/variantes,
     prix, date de sortie precise, images additionnelles).
  2. Insere en base SQLite locale (dedup sur l'identifiant produit) pour ne
     jamais retraiter un produit deja vu lors d'une execution precedente.
  3. Telecharge l'image principale dans images/wcf/<id>.jpg (une seule fois).
  4. Traduit dans les 11 langues de l'app (voir translate_item()).
Exporte l'integralite de la table dans un JSON structure (wcf_news.json),
utilise ensuite par l'app Android (onglet "Actu") via GitHub Pages.

Aucune donnee n'est inventee : tout ce qui est exporte provient directement du HTML
du site officiel. Si un champ est absent/ambigu sur la fiche produit, il est laisse
vide plutot que devine.
"""
from __future__ import annotations

import argparse
import itertools
import json
import re
import sqlite3
import sys
import time
import unicodedata
from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import quote, urljoin

import requests
from bs4 import BeautifulSoup

try:
    from deep_translator import GoogleTranslator
except ImportError:
    GoogleTranslator = None  # traduction desactivee si le paquet n'est pas installe

if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

BASE_URL = "https://bsp-prize.jp"
SCHEDULE_URL = f"{BASE_URL}/schedule/"
WCF_KEYWORD = "ワールドコレクタブルフィギュア"
# Recherche plein texte sur tout le catalogue (~400 produits, toutes licences confondues) --
# bien plus large que /schedule/ qui ne montre que les 1-2 boites en cours de precommande.
SEARCH_URL = f"{BASE_URL}/search/"

# Memes 11 langues que MaCollection (retrogaming) : (cle JSON/app, code deep-translator).
# "ja" = original, jamais traduit (recopie telle quelle). "zh" utilise le code deep-translator
# "zh-CN" mais est expose sous la cle "zh" (correspond a Locale.getDefault().language sur Android).
# Classification en licence (memes valeurs que l'enum Kotlin `Licence` de l'app) a partir de
# mots-cles japonais/anglais reperes dans le nom de serie original -- une simple categorisation
# d'affichage (filtre dans l'onglet Actu), pas une donnee inventee. "AUTRE" si aucun mot-cle ne
# correspond (ex. HUNTER×HUNTER, Chainsaw Man, Ken Shimura, collaborations...).
LICENCE_KEYWORDS = [
    ("ONE_PIECE", ["ワンピース", "ONE PIECE"]),
    ("BLEACH", ["ブリーチ", "BLEACH"]),
    ("DRAGON_BALL", ["ドラゴンボール", "DRAGON BALL"]),
    ("NARUTO", ["ナルト", "NARUTO", "BORUTO"]),
    ("DEMON_SLAYER", ["鬼滅の刃", "キメツ", "DEMON SLAYER"]),
    ("JUJUTSU_KAISEN", ["呪術廻戦", "JUJUTSU KAISEN"]),
    ("MY_HERO_ACADEMIA", ["ヒーローアカデミア", "ヒロアカ", "MY HERO ACADEMIA"]),
    ("DISNEY", ["ディズニー", "DISNEY"]),
    ("MARVEL", ["マーベル", "MARVEL"]),
]


def classify_licence(series: str) -> str:
    upper = series.upper()
    for licence, keywords in LICENCE_KEYWORDS:
        if any(kw.upper() in upper for kw in keywords):
            return licence
    return "AUTRE"


TARGET_LANGS = [
    ("fr", "fr"), ("en", "en"), ("es", "es"), ("it", "it"), ("de", "de"),
    ("pt", "pt"), ("ru", "ru"), ("el", "el"), ("tr", "tr"), ("zh", "zh-CN"),
]


def translate_item(series: str, characters: list, release_date_raw: str, price_raw: str) -> dict:
    """Traduit un item dans les 11 langues de l'app, best-effort (jamais bloquant : une langue
    en echec retombe sur le texte japonais original plutot que de faire planter le scraper).
    Un seul appel reseau par langue (traduction en lot de tous les champs de l'item), plutot
    qu'un appel par champ, pour limiter le nombre de requetes vers le service de traduction.
    Retourne {"ja": {...}, "fr": {...}, "en": {...}, ...}."""
    texts = [series, release_date_raw, price_raw] + characters
    original = {"series": series, "releaseDate": release_date_raw, "price": price_raw, "characters": characters}
    result = {"ja": original}
    if GoogleTranslator is None:
        for json_key, _ in TARGET_LANGS:
            result[json_key] = original
        return result

    non_empty_idx = [i for i, t in enumerate(texts) if t]
    non_empty_texts = [texts[i] for i in non_empty_idx]

    for json_key, dt_code in TARGET_LANGS:
        try:
            translated = (
                GoogleTranslator(source="ja", target=dt_code).translate_batch(non_empty_texts)
                if non_empty_texts else []
            )
            full = list(texts)
            for idx, val in zip(non_empty_idx, translated):
                full[idx] = val or texts[idx]
            result[json_key] = {
                "series": full[0], "releaseDate": full[1], "price": full[2], "characters": full[3:],
            }
        except Exception as exc:
            print(f"  [erreur traduction {json_key}] {series[:40]}... : {exc}", file=sys.stderr)
            result[json_key] = original
        time.sleep(0.5)
    return result

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    ),
    "Accept-Language": "ja,en-US;q=0.8,en;q=0.6",
}


@dataclass
class NewsItem:
    item_id: str
    series: str
    licence: str  # voir classify_licence() -- meme valeurs que l'enum Kotlin `Licence`
    characters: list  # liste de noms de personnages/variantes (peut etre vide), japonais
    release_date_raw: str
    price_raw: str
    translations: dict  # {"ja": {...}, "fr": {...}, "en": {...}, ...} -- voir translate_item()
    image_url: str
    item_url: str
    local_image_path: str
    scraped_at: str


def fetch(session: requests.Session, url: str, delay: float) -> BeautifulSoup | None:
    """GET avec rate limiting et gestion simple des erreurs/403."""
    try:
        resp = session.get(url, headers=HEADERS, timeout=20)
    except requests.RequestException as exc:
        print(f"  [erreur reseau] {url} : {exc}", file=sys.stderr)
        return None
    finally:
        time.sleep(delay)

    if resp.status_code == 403:
        print(f"  [403 bloque] {url} -- reduire la frequence ou changer d'IP/UA", file=sys.stderr)
        return None
    if resp.status_code >= 400:
        print(f"  [HTTP {resp.status_code}] {url}", file=sys.stderr)
        return None

    resp.encoding = resp.apparent_encoding or "utf-8"
    return BeautifulSoup(resp.text, "html.parser")


def extract_item_id(href: str) -> str | None:
    m = re.search(r"/item/(\d+)/?", href)
    return m.group(1) if m else None


def clean_text(s: str) -> str:
    s = unicodedata.normalize("NFKC", s)
    return re.sub(r"\s+", " ", s).strip()


def list_products(session: requests.Session, label: str, page_url_fn, max_pages: int, delay: float, seen_ids: set):
    """Parcourt une liste paginee bsp-prize.jp (planning OU resultats de recherche, meme markup
    HTML) et yield (item_id, name, url, thumb_url) pour chaque produit WCF non deja vu dans
    `seen_ids` (partage entre les differentes sources pour eviter les doublons). `page_url_fn(page)`
    construit l'URL de la page N (1-indexe)."""
    for page in range(1, max_pages + 1):
        url = page_url_fn(page)
        print(f"[{label}] page {page}: {url}")
        soup = fetch(session, url, delay)
        if soup is None:
            break

        items = soup.select("div.products_item")
        if not items:
            print("  (aucun item trouve, fin de pagination)")
            break

        new_on_page = 0
        for it in items:
            a = it.select_one("a[href*='/item/']")
            if not a:
                continue
            item_id = extract_item_id(a.get("href", ""))
            if not item_id or item_id in seen_ids:
                continue
            name_tag = it.select_one("p.products_name")
            name = clean_text(name_tag.get_text()) if name_tag else ""
            if WCF_KEYWORD not in name:
                continue
            img_tag = it.select_one("figure.products_img img")
            thumb = urljoin(BASE_URL, img_tag.get("src")) if img_tag else ""
            item_url = urljoin(BASE_URL, a.get("href"))
            seen_ids.add(item_id)
            new_on_page += 1
            yield item_id, name, item_url, thumb

        if new_on_page == 0 and page > 1:
            # plus rien de nouveau sur cette source (pagination bouclee ou epuisee)
            break


def scrape_item_detail(session: requests.Session, item_url: str, delay: float) -> dict:
    """Recupere prix, liste de personnages/variantes et image haute def depuis la fiche produit."""
    soup = fetch(session, item_url, delay)
    result = {"characters": [], "price_raw": "", "release_date_raw": "", "hi_res_image": ""}
    if soup is None:
        return result

    date_tag = soup.select_one("p.releaseDate")
    if date_tag:
        result["release_date_raw"] = clean_text(date_tag.get_text())

    spec = soup.select_one("dl.productDetail_spec dd")
    if spec:
        raw_html = spec.decode_contents()
        lines = [clean_text(l) for l in re.split(r"<br\s*/?>", raw_html)]
        lines = [BeautifulSoup(l, "html.parser").get_text() for l in lines]
        lines = [l.strip("・ ").strip() for l in lines if l.strip()]

        for line in lines:
            if re.search(r"価格|円|税込", line):
                result["price_raw"] = line
                continue
            if re.match(r"^・?全\d+種", line):
                continue  # "全9種" = juste un compte, pas un nom de personnage
            if re.match(r"^約?\d", line) or re.match(r"^[0-9約~〜～\s]+cm", line, re.IGNORECASE):
                continue  # ligne "約5cm~約7cm" seule = plage de taille generale, pas un perso
            if re.search(r"\d", line) and ("cm" in line.lower() or "約" in line):
                # ligne du type "超サイヤ人4 孫悟空(ミニ)・・・約5cm" -> on ne garde que le nom
                name_part = re.split(r"[・.]{2,}", line)[0].strip()
                if name_part and not re.match(r"^約?\d", name_part):
                    result["characters"].append(name_part)

    view = soup.select_one("div.productDetail_views a[href]")
    if view:
        result["hi_res_image"] = urljoin(BASE_URL, view.get("href"))

    return result


def download_image(session: requests.Session, urls, dest: Path, delay: float) -> bool:
    """Essaie chaque URL de `urls` dans l'ordre (ex: haute def puis miniature de repli)."""
    if dest.exists():
        return True
    for url in urls:
        if not url:
            continue
        try:
            resp = session.get(url, headers={**HEADERS, "Referer": BASE_URL + "/"}, timeout=30)
            resp.raise_for_status()
        except requests.RequestException as exc:
            print(f"  [erreur image] {url} : {exc}", file=sys.stderr)
            time.sleep(delay)
            continue
        time.sleep(delay)
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(resp.content)
        return True
    return False


def init_db(db_path: Path) -> sqlite3.Connection:
    conn = sqlite3.connect(db_path)
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS wcf_news (
            item_id TEXT PRIMARY KEY,
            series TEXT NOT NULL,
            licence TEXT,
            characters_json TEXT NOT NULL,
            release_date_raw TEXT,
            price_raw TEXT,
            translations_json TEXT,
            image_url TEXT,
            item_url TEXT NOT NULL,
            local_image_path TEXT,
            scraped_at TEXT NOT NULL
        )
        """
    )
    # Migration douce pour une base existante creee avant l'ajout des traductions multilingues/de
    # la licence (colonnes series_fr/characters_fr_json/... d'une version anterieure : plus
    # utilisees, laissees telles quelles si presentes, remplacees par translations_json/licence).
    existing_cols = {row[1] for row in conn.execute("PRAGMA table_info(wcf_news)")}
    if "translations_json" not in existing_cols:
        conn.execute("ALTER TABLE wcf_news ADD COLUMN translations_json TEXT")
    if "licence" not in existing_cols:
        conn.execute("ALTER TABLE wcf_news ADD COLUMN licence TEXT")
    conn.commit()
    return conn


def item_exists(conn: sqlite3.Connection, item_id: str) -> bool:
    cur = conn.execute("SELECT 1 FROM wcf_news WHERE item_id = ?", (item_id,))
    return cur.fetchone() is not None


def backfill_licence(conn: sqlite3.Connection) -> int:
    """Classe en licence les lignes deja en base d'avant l'ajout de ce champ (aucun appel
    reseau, juste une re-lecture du nom de serie deja stocke)."""
    rows = conn.execute("SELECT item_id, series FROM wcf_news WHERE licence IS NULL").fetchall()
    for item_id, series in rows:
        conn.execute("UPDATE wcf_news SET licence = ? WHERE item_id = ?", (classify_licence(series), item_id))
    conn.commit()
    return len(rows)


def insert_item(conn: sqlite3.Connection, item: NewsItem) -> None:
    # INSERT OR IGNORE : ne jamais ecraser/dupliquer un item deja present (dedup).
    conn.execute(
        """
        INSERT OR IGNORE INTO wcf_news
            (item_id, series, licence, characters_json, release_date_raw, price_raw,
             translations_json, image_url, item_url, local_image_path, scraped_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            item.item_id,
            item.series,
            item.licence,
            json.dumps(item.characters, ensure_ascii=False),
            item.release_date_raw,
            item.price_raw,
            json.dumps(item.translations, ensure_ascii=False),
            item.image_url,
            item.item_url,
            item.local_image_path,
            item.scraped_at,
        ),
    )
    conn.commit()


_JP_YEAR_MONTH = re.compile(r"(\d{4})\s*年\s*(\d{1,2})\s*月")


def _release_year_month(raw: str):
    """Annee*12+mois d'une date de sortie brute japonaise (ex. "2026年11月発売"), ou None."""
    m = _JP_YEAR_MONTH.search(raw or "")
    if not m:
        return None
    year, month = int(m.group(1)), int(m.group(2))
    if not 1 <= month <= 12:
        return None
    return year * 12 + month


def export_json(conn: sqlite3.Connection, out_path: Path) -> int:
    cur = conn.execute(
        """
        SELECT item_id, series, licence, characters_json, release_date_raw, price_raw,
               translations_json, image_url, item_url, local_image_path, scraped_at
        FROM wcf_news
        ORDER BY item_id DESC
        """
    )
    rows = cur.fetchall()
    # Une « actu » doit rester une sortie a venir : on ne publie que le mois en cours et au-dela
    # (la base garde tout pour le dedup ; les dates non reconnues sont conservees par prudence).
    now = datetime.now()
    current_ym = now.year * 12 + now.month
    data = [
        {
            "id": r[0],
            "series": r[1],
            "licence": r[2] or "AUTRE",
            "characters": json.loads(r[3]),
            "releaseDateRaw": r[4],
            "priceRaw": r[5],
            "translations": json.loads(r[6]) if r[6] else {},
            "imageUrl": r[7],
            "itemUrl": r[8],
            "localImagePath": r[9],
            "scrapedAt": r[10],
        }
        for r in rows
        if (_release_year_month(r[4]) is None) or (_release_year_month(r[4]) >= current_ym)
    ]
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    return len(data)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--db", default="wcf_news.sqlite3", type=Path)
    parser.add_argument("--out", default="wcf_news.json", type=Path)
    parser.add_argument("--images-dir", default="images/wcf", type=Path)
    parser.add_argument("--max-pages", default=15, type=int, help="nb de pages max par source a parcourir")
    parser.add_argument("--delay", default=1.5, type=float, help="delai (s) entre deux requetes")
    args = parser.parse_args()

    session = requests.Session()
    conn = init_db(args.db)

    new_count = 0
    skipped_count = 0

    seen_ids: set = set()
    schedule_items = list_products(
        session, "schedule", lambda p: SCHEDULE_URL if p == 1 else f"{SCHEDULE_URL}?page={p}",
        args.max_pages, args.delay, seen_ids,
    )
    search_kw = quote(WCF_KEYWORD)
    search_items = list_products(
        session, "search",
        lambda p: f"{SEARCH_URL}?kw={search_kw}" if p == 1 else f"{SEARCH_URL}?kw={search_kw}&page={p}",
        args.max_pages, args.delay, seen_ids,
    )

    for item_id, name, item_url, thumb in itertools.chain(schedule_items, search_items):
        if item_exists(conn, item_id):
            skipped_count += 1
            continue

        print(f"[nouveau] {item_id} : {name}")
        detail = scrape_item_detail(session, item_url, args.delay)

        image_url = detail["hi_res_image"] or thumb
        local_path = args.images_dir / f"{item_id}.jpg"
        downloaded = download_image(session, [detail["hi_res_image"], thumb], local_path, args.delay)

        translations = translate_item(
            name, detail["characters"], detail["release_date_raw"], detail["price_raw"]
        )

        item = NewsItem(
            item_id=item_id,
            series=name,
            licence=classify_licence(name),
            characters=detail["characters"],
            release_date_raw=detail["release_date_raw"],
            price_raw=detail["price_raw"],
            translations=translations,
            image_url=image_url,
            item_url=item_url,
            local_image_path=str(local_path) if downloaded else "",
            scraped_at=datetime.now(timezone.utc).isoformat(),
        )
        insert_item(conn, item)
        new_count += 1

    backfilled = backfill_licence(conn)
    total = export_json(conn, args.out)
    conn.close()

    print(f"\nTermine : {new_count} nouveaute(s) ajoutee(s), {skipped_count} deja connue(s) ignoree(s).")
    if backfilled:
        print(f"Licence retro-appliquee sur {backfilled} ancienne(s) entree(s).")
    print(f"Export JSON : {args.out} ({total} entrees au total)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
