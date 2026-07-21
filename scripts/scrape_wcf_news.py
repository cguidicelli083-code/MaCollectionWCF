#!/usr/bin/env python3
"""
Scraper de nouveautes WCF (World Collectable Figure, Banpresto/Bandai Spirits)
depuis le site officiel bsp-prize.jp (planning de sorties, page /schedule/).

Usage:
    python scrape_wcf_news.py [--db wcf_news.sqlite3] [--out wcf_news.json]
                               [--images-dir images/wcf] [--max-pages 6]
                               [--delay 1.5]

Le script :
  1. Parcourt les pages du planning bsp-prize.jp/schedule/ (pagination mensuelle).
  2. Ne garde que les produits dont le nom contient "ワールドコレクタブルフィギュア"
     (World Collectable Figure) -- le site couvre TOUTES les gammes Banpresto/prize,
     pas seulement WCF.
  3. Visite la fiche produit de chaque nouveaute WCF pour recuperer le detail
     (liste des personnages/variantes, prix, images additionnelles).
  4. Insere en base SQLite locale (dedup sur l'identifiant produit, extrait de
     l'URL /item/<id>/) pour ne jamais retelecharger/re-traiter un produit deja vu.
  5. Telecharge l'image principale dans images/wcf/<id>.jpg (une seule fois).
  6. Exporte l'integralite de la table dans un JSON structure (wcf_news.json),
     utilise ensuite par l'app Android (onglet "Actu") via GitHub Pages.

Aucune donnee n'est inventee : tout ce qui est exporte provient directement du HTML
du site officiel. Si un champ est absent/ambigu sur la fiche produit, il est laisse
vide plutot que devine.
"""
from __future__ import annotations

import argparse
import json
import re
import sqlite3
import sys
import time
import unicodedata
from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urljoin

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

# Memes 11 langues que MaCollection (retrogaming) : (cle JSON/app, code deep-translator).
# "ja" = original, jamais traduit (recopie telle quelle). "zh" utilise le code deep-translator
# "zh-CN" mais est expose sous la cle "zh" (correspond a Locale.getDefault().language sur Android).
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


def list_schedule_items(session: requests.Session, max_pages: int, delay: float):
    """Parcourt le planning (pagination mensuelle via ?p=N) et yield (item_id, name, url, thumb_url)."""
    seen_ids = set()
    for page in range(1, max_pages + 1):
        url = SCHEDULE_URL if page == 1 else f"{SCHEDULE_URL}?page={page}"
        print(f"[schedule] page {page}: {url}")
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
            # plus rien de nouveau (pagination bouclee ou epuisee)
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
    # Migration douce pour une base existante creee avant l'ajout des traductions multilingues
    # (colonnes series_fr/characters_fr_json/... d'une version anterieure : plus utilisees,
    # laissees telles quelles si presentes, remplacees par translations_json).
    existing_cols = {row[1] for row in conn.execute("PRAGMA table_info(wcf_news)")}
    if "translations_json" not in existing_cols:
        conn.execute("ALTER TABLE wcf_news ADD COLUMN translations_json TEXT")
    conn.commit()
    return conn


def item_exists(conn: sqlite3.Connection, item_id: str) -> bool:
    cur = conn.execute("SELECT 1 FROM wcf_news WHERE item_id = ?", (item_id,))
    return cur.fetchone() is not None


def insert_item(conn: sqlite3.Connection, item: NewsItem) -> None:
    # INSERT OR IGNORE : ne jamais ecraser/dupliquer un item deja present (dedup).
    conn.execute(
        """
        INSERT OR IGNORE INTO wcf_news
            (item_id, series, characters_json, release_date_raw, price_raw,
             translations_json, image_url, item_url, local_image_path, scraped_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            item.item_id,
            item.series,
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


def export_json(conn: sqlite3.Connection, out_path: Path) -> int:
    cur = conn.execute(
        """
        SELECT item_id, series, characters_json, release_date_raw, price_raw,
               translations_json, image_url, item_url, local_image_path, scraped_at
        FROM wcf_news
        ORDER BY item_id DESC
        """
    )
    rows = cur.fetchall()
    data = [
        {
            "id": r[0],
            "series": r[1],
            "characters": json.loads(r[2]),
            "releaseDateRaw": r[3],
            "priceRaw": r[4],
            "translations": json.loads(r[5]) if r[5] else {},
            "imageUrl": r[6],
            "itemUrl": r[7],
            "localImagePath": r[8],
            "scrapedAt": r[9],
        }
        for r in rows
    ]
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    return len(data)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--db", default="wcf_news.sqlite3", type=Path)
    parser.add_argument("--out", default="wcf_news.json", type=Path)
    parser.add_argument("--images-dir", default="images/wcf", type=Path)
    parser.add_argument("--max-pages", default=6, type=int, help="nb de pages du planning a parcourir")
    parser.add_argument("--delay", default=1.5, type=float, help="delai (s) entre deux requetes")
    args = parser.parse_args()

    session = requests.Session()
    conn = init_db(args.db)

    new_count = 0
    skipped_count = 0

    for item_id, name, item_url, thumb in list_schedule_items(session, args.max_pages, args.delay):
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

    total = export_json(conn, args.out)
    conn.close()

    print(f"\nTermine : {new_count} nouveaute(s) ajoutee(s), {skipped_count} deja connue(s) ignoree(s).")
    print(f"Export JSON : {args.out} ({total} entrees au total)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
