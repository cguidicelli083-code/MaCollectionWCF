package com.nawash.macollectionwcf.data

import kotlinx.coroutines.CancellationException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- DTO ShandoraShop (Shopify, API publique de recherche prédictive) ---
private data class ShSuggestResponse(val resources: ShResources?)
private data class ShResources(val results: ShResults?)
private data class ShResults(val products: List<ShProduct>?)
private data class ShProduct(val image: String?, val featured_image: ShImage?, val variants: List<ShVariant>?)
private data class ShVariant(val title: String?, val image: String?, val featured_image: ShImage?)
private data class ShImage(val url: String?)

private interface ShandoraApi {
    @GET("search/suggest.json")
    suspend fun suggest(
        @Query("q") q: String,
        @Query("resources[type]") type: String = "product",
        @Query("resources[limit]") limit: Int = 3
    ): ShSuggestResponse
}

/**
 * Photos officielles WCF via ShandoraShop (shandorashop.com, revendeur espagnol spécialisé en
 * import Japon — One Piece/Dragon Ball/Naruto/MHA/Demon Slayer/Jujutsu Kaisen), sur suggestion de
 * l'utilisateur le 2026-07-19. Boutique Shopify standard : `robots.txt` autorise explicitement
 * l'exploration (`Allow: /`, contrairement à MyFigureCollection.net qui bloque nommément les
 * crawlers IA) et expose une API JSON publique de recherche prédictive
 * (`/search/suggest.json?q=...`), vérifiée en direct — retourne une vraie photo produit par
 * coffret WCF (parfois par personnage via les variantes), avec des noms de coffrets qui
 * correspondent souvent exactement aux séries déjà cataloguées (ex. "One Piece Wcf Onigashima
 * Vol.5 Banpresto", "One Piece Wcf Dressrosa vol.2 Banpresto"). Pas de couverture garantie pour
 * les très vieux coffrets (série numérotée principale 2010-2012) qui ne sont plus en stock.
 */
object ShandoraShop {

    private val api: ShandoraApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://shandorashop.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ShandoraApi::class.java)
    }

    /**
     * Cherche une photo officielle pour un coffret/personnage WCF. [series] (vague/volume, ex.
     * "One Piece Onigashima Vol.5") est prioritaire dans la requête car les noms de produits de la
     * boutique sont organisés par coffret, pas par personnage seul — le personnage seul renverrait
     * souvent le mauvais coffret. Retourne l'image de la variante correspondant au personnage si
     * plusieurs variantes existent (certains coffrets ont une variante Shopify par figurine), sinon
     * l'image principale du produit.
     */
    suspend fun findImage(character: String, series: String?): String? {
        val query = if (!series.isNullOrBlank()) series else character
        val product = try {
            api.suggest(query).resources?.results?.products?.firstOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } ?: return null

        val matchingVariant = product.variants?.firstOrNull { nameMatches(it.title, character) }
        return matchingVariant?.image
            ?: matchingVariant?.featured_image?.url
            ?: product.image
            ?: product.featured_image?.url
    }

    /**
     * Comparaison souple nom de variante Shopify / personnage catalogue : une correspondance
     * stricte (`contains`) échoue souvent car les conventions de nom diffèrent d'une source à
     * l'autre (ex. catalogue "Monkey D. Luffy (Halloween)" vs variante boutique "Luffy D. Monkey" —
     * même personnage, ordre des mots différent, suffixe absent). On retire le suffixe entre
     * parenthèses, on découpe en mots, et on considère qu'il y a correspondance si au moins un mot
     * significatif (plus de 2 lettres, hors particules "d"/"de"/"no") est commun aux deux.
     */
    private fun nameMatches(variantTitle: String?, character: String): Boolean {
        if (character.isBlank() || variantTitle.isNullOrBlank()) return false
        fun tokens(s: String) = s.substringBefore("(")
            .split(Regex("[^\\p{L}]+"))
            .map { it.lowercase() }
            .filter { it.length > 2 && it !in setOf("de", "no", "the", "van", "der") }
            .toSet()
        return tokens(variantTitle).intersect(tokens(character)).isNotEmpty()
    }
}
