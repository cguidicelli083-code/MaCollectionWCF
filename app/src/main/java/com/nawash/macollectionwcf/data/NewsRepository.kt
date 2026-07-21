package com.nawash.macollectionwcf.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

/** Reflet JSON exact d'une entrée de `wcf_news.json` (voir `scripts/scrape_wcf_news.py`). */
private data class WcfNewsDto(
    val id: String,
    val series: String,
    val characters: List<String>?,
    val releaseDateRaw: String?,
    val priceRaw: String?,
    val imageUrl: String?,
    val itemUrl: String,
    val scrapedAt: String
)

private interface NewsApi {
    @GET
    suspend fun fetchNews(@Url url: String): List<WcfNewsDto>
}

/**
 * Récupère les nouveautés WCF publiées par le scraper (`scripts/scrape_wcf_news.py`, exécuté
 * chaque nuit par le workflow GitHub Actions `.github/workflows/scrape.yml`) sous forme de JSON
 * statique hébergé sur GitHub Pages. Best-effort : en cas d'échec réseau, on garde les dernières
 * actus connues en base plutôt que de vider l'onglet ou planter.
 *
 * [FEED_URL] pointe vers GitHub Pages (dépôt `cguidicelli083-code/MaCollectionWCF`, dossier
 * `/docs` sur `main`), alimenté chaque nuit par `.github/workflows/scrape.yml`.
 */
object NewsRepository {
    private const val FEED_URL = "https://cguidicelli083-code.github.io/MaCollectionWCF/wcf_news.json"

    private val api: NewsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://example.invalid/") // non utilisée, l'URL complète est passée à @Url
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApi::class.java)
    }

    suspend fun fetchLatest(): List<WcfNewsEntry>? = try {
        api.fetchNews(FEED_URL).map {
            WcfNewsEntry(
                id = it.id,
                series = it.series,
                characters = (it.characters ?: emptyList()).joinToString("|"),
                releaseDateRaw = it.releaseDateRaw ?: "",
                priceRaw = it.priceRaw ?: "",
                imageUrl = it.imageUrl ?: "",
                itemUrl = it.itemUrl,
                scrapedAt = it.scrapedAt
            )
        }
    } catch (e: Exception) {
        null
    }
}
