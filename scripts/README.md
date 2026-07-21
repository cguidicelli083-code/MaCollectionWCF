# Scraper de nouveautes WCF

`scrape_wcf_news.py` recupere le planning de sortie des figurines World Collectable Figure
(Banpresto/Bandai Spirits) depuis le site officiel **bsp-prize.jp** et produit :

- `docs/wcf_news.json` — flux JSON consomme par l'app Android (onglet **Actu**) via
  `NewsRepository.kt`, servi statiquement par **GitHub Pages** (dossier `/docs` sur `main`).
- `docs/images/wcf/<id>.jpg` — images telechargees localement.
- `scripts/wcf_news.sqlite3` — base de dedup (committee dans le depot pour persister entre
  deux executions du workflow GitHub Actions).

## Execution manuelle

```bash
pip install -r scripts/requirements.txt
python scripts/scrape_wcf_news.py \
    --db scripts/wcf_news.sqlite3 \
    --out docs/wcf_news.json \
    --images-dir docs/images/wcf \
    --max-pages 10 \
    --delay 2
```

`--delay` (secondes entre deux requetes) et `--max-pages` (nombre de pages de pagination du
planning a parcourir) sont ajustables. Le script est idempotent : relance-le autant de fois que
tu veux, seuls les nouveaux produits WCF sont retraites/re-televerses (dedup sur l'identifiant
produit bsp-prize.jp).

## Automatisation (deja en place)

Le workflow `.github/workflows/scrape.yml` execute ce script chaque nuit a 03h00 UTC (et sur
declenchement manuel via l'onglet Actions de GitHub), puis committe automatiquement
`docs/wcf_news.json`, `docs/images/wcf/` et `scripts/wcf_news.sqlite3` s'il y a du nouveau.

## Mise en place initiale (a faire une seule fois, cote GitHub)

1. Cree un depot GitHub (public, necessaire pour que GitHub Pages soit gratuit) et pousse ce
   projet dessus :
   ```bash
   git remote add origin https://github.com/<ton-user>/<ton-repo>.git
   git branch -M main
   git push -u origin main
   ```
2. Dans les parametres du depot GitHub : **Settings > Pages > Source** = "Deploy from a
   branch", branche `main`, dossier `/docs`. GitHub te donnera l'URL publique, du type
   `https://<ton-user>.github.io/<ton-repo>/`.
3. Ouvre `app/src/main/java/com/nawash/macollectionwcf/data/NewsRepository.kt` et remplace la
   constante `FEED_URL` par `https://<ton-user>.github.io/<ton-repo>/wcf_news.json`, puis
   recompile/reinstalle l'app.
4. Verifie que le workflow a les droits d'ecriture : **Settings > Actions > General >
   Workflow permissions** = "Read and write permissions" (necessaire pour qu'il puisse
   committer `docs/wcf_news.json` automatiquement chaque nuit).

Tant que l'etape 3 n'est pas faite, l'app utilise un `docs/wcf_news.json` vide (`[]`) commite
par defaut et l'onglet Actu affiche simplement "Aucune actu pour le moment" — aucun crash.
