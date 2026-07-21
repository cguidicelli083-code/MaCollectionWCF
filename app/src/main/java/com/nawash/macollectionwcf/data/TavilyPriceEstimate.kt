package com.nawash.macollectionwcf.data

import com.nawash.macollectionwcf.BuildConfig
import kotlinx.coroutines.CancellationException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import kotlin.math.roundToInt

// --- DTO Tavily (recherche web) ---
private data class TavilySearchRequest(
    val api_key: String,
    val query: String,
    val search_depth: String = "basic",
    val max_results: Int = 5,
    val include_images: Boolean = false
)
private data class TavilyResult(val title: String?, val content: String?)
private data class TavilySearchResponse(val results: List<TavilyResult>?, val images: List<String>? = null)

private interface TavilyApi {
    @POST("search")
    suspend fun search(@Body body: TavilySearchRequest): TavilySearchResponse
}

/**
 * 3e recours pour l'estimation de cote d'une figurine, après eBay (annonces réelles) puis Gemini
 * (IA + recherche intégrée) — nécessaire car le quota gratuit Gemini est très bas (~20 req/jour,
 * vérifié sur le projet MaCollection original). Tavily (recherche web, quota séparé et plus
 * généreux) trouve des annonces/prix, puis Groq (modèle texte) en extrait un prix — même format
 * JSON que [GeminiVision.estimatePrice], donc réutilise son parsing.
 */
object TavilyPriceEstimate {

    private val api: TavilyApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.tavily.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TavilyApi::class.java)
    }

    private val key: String get() = BuildConfig.TAVILY_API_KEY

    fun isConfigured(): Boolean = key.isNotBlank() && GroqVision.isConfigured()

    suspend fun estimatePrice(
        licence: Licence, character: String, name: String,
        condition: Condition, hasBox: Boolean, hasAccessories: Boolean
    ): Int? {
        if (!isConfigured()) return null
        val completeness = when {
            hasBox && hasAccessories -> "complète en boîte avec ses accessoires"
            hasBox -> "en boîte sans ses accessoires"
            else -> "loose (sans boîte)"
        }
        val itemName = if (character.isBlank() || name.lowercase().contains(character.lowercase())) name else "$character $name"
        val licenceNote = if (licence != Licence.AUTRE && !itemName.lowercase().contains(licence.label.lowercase())) " ${licence.label}" else ""

        val results = try {
            api.search(TavilySearchRequest(key, "prix occasion figurine WCF $itemName$licenceNote ${condition.label} France vente")).results.orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
        if (results.isEmpty()) return null

        val snippets = results.take(5).joinToString("\n\n") { r ->
            "${r.title.orEmpty()} : ${r.content.orEmpty().take(400)}"
        }
        val prompt = "Voici des résultats de recherche web pour cette figurine WCF d'occasion " +
            "\"$itemName\"$licenceNote, état ${condition.label}, $completeness :\n\n$snippets\n\nÀ partir " +
            "de CES résultats UNIQUEMENT (n'invente rien d'autre), donne une estimation de prix en " +
            "euros. Réponds UNIQUEMENT en JSON strict, sans texte ni balise autour : {\"prix_eur\": " +
            "nombre en euros, ou null si ces résultats ne permettent pas d'estimer un prix fiable}."

        val text = runCatching { GroqVision.completeText(prompt) }.getOrNull() ?: return null
        val price = GeminiVision.extractJsonPrice(text) ?: GeminiVision.extractPriceFromFreeText(text) ?: return null
        return if (price <= 0) null else (price * 100).roundToInt()
    }

    /**
     * Recours photo quand eBay n'a aucune annonce (donc aucune image) pour une fiche du
     * catalogue Encyclo : recherche web Tavily avec images incluses (`include_images`, vérifié
     * en direct — renvoie de vraies URLs d'images des pages trouvées, pas de placeholder), et
     * renvoie la première image trouvée. Quota Tavily séparé et plus généreux que Gemini.
     * [series] (vague/volume WCF) est indispensable à la précision : un même personnage ressort
     * souvent dans plusieurs coffrets différents, le rechercher seul renvoie une version au hasard.
     *
     * Les figurines WCF étant japonaises, on ajoute des noms de revendeurs japonais/spécialisés
     * connus (Amiami, Suruga-ya, Mandarake, PlazaJapan, Nin-Nin Game, NamekToys — ces 3 derniers
     * ajoutés le 2026-07-19 sur suggestion de l'utilisateur) comme MOTS-CLÉS de la requête (PAS en
     * filtre `include_domains` — testé en direct : le filtre de domaine renvoie surtout des
     * logos/icônes de site plutôt que de vraies photos produit, alors que les citer en mots-clés
     * fait remonter naturellement ces boutiques d'import japonaises parmi des résultats globaux
     * pertinents).
     */
    suspend fun findImage(licence: Licence, character: String, series: String? = null): String? {
        if (key.isBlank()) return null
        val licenceNote = if (licence != Licence.AUTRE && series.isNullOrBlank()) " ${licence.label}" else ""
        val seriesNote = if (!series.isNullOrBlank()) " $series" else ""
        val query = "World Collectable Figure Banpresto $character$seriesNote$licenceNote amiami suruga-ya mandarake plazajapan nin-nin-game namektoys"
        val images = try {
            api.search(TavilySearchRequest(key, query, max_results = 3, include_images = true)).images.orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        return images.firstOrNull()
    }
}
