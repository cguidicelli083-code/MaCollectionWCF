package com.nawash.macollectionwcf.data

import android.util.Base64
import com.nawash.macollectionwcf.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import kotlin.math.roundToInt

// --- DTO eBay ---
private data class EbayToken(val access_token: String?, val expires_in: Long?)
private data class EbaySearch(val total: Int?, val itemSummaries: List<EbayItem>?)
private data class EbayItem(val title: String?, val price: EbayPrice?, val condition: String?, val image: EbayImage?)
private data class EbayPrice(val value: String?, val currency: String?)
private data class EbayImage(val imageUrl: String?)

private interface EbayApi {
    @FormUrlEncoded
    @POST("identity/v1/oauth2/token")
    suspend fun token(
        @Header("Authorization") basic: String,
        @Field("grant_type") grantType: String,
        @Field("scope") scope: String
    ): EbayToken

    @GET("buy/browse/v1/item_summary/search")
    suspend fun search(
        @Header("Authorization") bearer: String,
        @Header("X-EBAY-C-MARKETPLACE-ID") market: String,
        @Query("q") q: String? = null,
        @Query("gtin") gtin: String? = null,
        @Query("category_ids") categoryIds: String? = null,
        @Query("filter") filter: String? = null,
        @Query("limit") limit: Int = 50
    ): EbaySearch
}

/**
 * Cote « best-effort » d'une figurine, basée sur les annonces eBay (marché FR, EUR).
 *
 * ⚠️ LIMITE IMPORTANTE (technique, pas un choix d'implémentation) : eBay expose deux APIs
 * différentes — Browse API (`item_summary/search`, annonces ACTIVES, seule accessible avec des
 * clés "grand public") et Marketplace Insights API (`item_sales/search`, ventes RÉUSSIES, réservée
 * aux comptes partenaires approuvés). Tant que Marketplace Insights n'est pas accordé, la cote
 * ci-dessous est calculée sur les annonces actives comparables — la meilleure estimation possible
 * avec les clés actuelles.
 *
 * `CAT_FIGURES` a été vérifié en direct via l'API eBay Taxonomy (`get_category_suggestions`,
 * marketplace EBAY_FR, categoryTreeId 71) plutôt que deviné : la requête "figurine anime"/
 * "one piece figurine" remonte systématiquement `Jouets et jeux(220) > Figurines, statues(246) >
 * Anime, manga(158666)`, la catégorie la plus précise pour des figurines WCF.
 */
object EbayPrices {

    data class PriceResult(val priceCents: Int?, val count: Int, val info: String?, val imageUrl: String? = null)

    private const val MARKET = "EBAY_FR"
    private const val SCOPE = "https://api.ebay.com/oauth/api_scope"
    private const val FIXED_PRICE = "buyingOptions:{FIXED_PRICE}"
    private const val CAT_FIGURES = "158666" // Jouets et jeux > Figurines, statues > Anime, manga

    private val clientId: String get() = BuildConfig.EBAY_CLIENT_ID
    private val clientSecret: String get() = BuildConfig.EBAY_CLIENT_SECRET

    private val api: EbayApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.ebay.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EbayApi::class.java)
    }

    private val mutex = Mutex()
    private var cachedToken: String? = null
    private var tokenExpiry: Long = 0

    fun isConfigured(): Boolean = clientId.isNotBlank() && clientSecret.isNotBlank()

    private suspend fun token(): String? = mutex.withLock {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiry) return@withLock cachedToken
        val basic = "Basic " + Base64.encodeToString(
            "$clientId:$clientSecret".toByteArray(), Base64.NO_WRAP
        )
        val t = try {
            api.token(basic, "client_credentials", SCOPE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        cachedToken = t?.access_token
        tokenExpiry = System.currentTimeMillis() + ((t?.expires_in ?: 0L) * 1000) - 60_000
        cachedToken
    }

    /**
     * Cote estimée d'une figurine : percentile bas (35e) des annonces eBay FR comparables, après
     * élimination des valeurs aberrantes (IQR). Le profil de comparaison tient compte de
     * [condition] (Neuf, Mint, Très bon, Bon, Mauvais, HS) et de [hasBox]/[hasAccessories]
     * (boîte et pièces/armes fournies présentes ou non) : on ne compare jamais une figurine loose
     * à une figurine complète en boîte, ni un exemplaire abîmé à un exemplaire neuf.
     */
    suspend fun lookup(
        barcode: String?,
        licence: Licence,
        character: String,
        name: String,
        condition: Condition = Condition.BON,
        hasBox: Boolean = true,
        hasAccessories: Boolean = true
    ): PriceResult {
        if (!isConfigured()) return PriceResult(null, 0, null)
        val tok = token() ?: return PriceResult(null, 0, "Connexion eBay impossible")
        val bearer = "Bearer $tok"

        val filter = "$FIXED_PRICE,conditionIds:{${conditionIdsFor(condition)}}"

        var items = emptyList<EbayItem>()
        if (!barcode.isNullOrBlank()) {
            items = try {
                api.search(bearer, MARKET, gtin = barcode, categoryIds = CAT_FIGURES, filter = filter).itemSummaries.orEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        }

        if (items.isEmpty()) {
            // Repli progressif : requête qualifiée (licence + personnage + nom + état) d'abord,
            // puis sans état, puis nom seul en dernier recours si eBay ne trouve rien du tout.
            val attempts = listOf(
                buildQuery(licence, character, name, condition) to filter,
                buildQuery(licence, character, name, condition = null) to FIXED_PRICE,
                buildQuery(Licence.AUTRE, character, name, condition = null) to FIXED_PRICE
            )
            for ((q, f) in attempts) {
                if (q.isBlank()) continue
                items = try {
                    api.search(bearer, MARKET, q = q, categoryIds = CAT_FIGURES, filter = f).itemSummaries.orEmpty()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyList()
                }
                if (items.isNotEmpty()) break
            }
        }

        // Certains vendeurs classent des goodies hors-sujet (posters, porte-clés, artbooks...)
        // dans la même catégorie malgré tout — le filtre de catégorie seul ne suffit pas.
        items = items.filterNot { isLikelyUnrelatedItem(it.title.orEmpty()) }

        val matching = items.filter { matchesCompleteness(it.title.orEmpty(), hasBox, hasAccessories) }
        val approximate = matching.isEmpty() && items.isNotEmpty()
        val kept0 = matching.ifEmpty { items }

        // Photo de la meilleure annonce correspondante — utile pour illustrer une figurine ajoutée
        // sans photo perso (scan par lot notamment), voir AppViewModel.resolvePriceAndFinish.
        val imageUrl = kept0.firstOrNull { !it.image?.imageUrl.isNullOrBlank() }?.image?.imageUrl

        val prices = kept0
            .mapNotNull { it.price }
            .filter { it.currency == "EUR" }
            .mapNotNull { it.value?.toDoubleOrNull() }
            .filter { it > 0 }
            .sorted()

        val profile = profileLabel(condition, hasBox, hasAccessories)
        if (prices.isEmpty()) {
            return PriceResult(null, 0, "Aucune annonce eBay trouvée pour ce profil ($profile)", imageUrl)
        }

        // eBay ne donne que des annonces EN COURS (prix demandés) : on élimine d'abord les valeurs
        // aberrantes (méthode IQR), puis on vise le 35e centile plutôt que la médiane pour corriger
        // le biais bien connu des prix demandés au-dessus du marché réel.
        val filtered = if (prices.size >= 4) {
            val q1 = percentile(prices, 0.25)
            val q3 = percentile(prices, 0.75)
            val iqr = q3 - q1
            prices.filter { it in (q1 - 1.5 * iqr)..(q3 + 1.0 * iqr) }.ifEmpty { prices }
        } else prices
        val estimate = percentile(filtered, 0.35)
        val approxNote = if (approximate) " (profil boîte/accessoires approximatif : aucune annonce ne le confirmait explicitement)" else ""
        val rangeNote = if (filtered.size > 1) " (de ${formatEuro(filtered.first())} à ${formatEuro(filtered.last())})" else ""

        return PriceResult(
            priceCents = (estimate * 100).roundToInt(),
            count = prices.size,
            info = "Estimation sur ${prices.size} annonce(s) eBay FR en cours$rangeNote — $profile$approxNote " +
                "(ventes réussies indisponibles : accès Marketplace Insights non accordé par eBay)",
            imageUrl = imageUrl
        )
    }

    private fun formatEuro(value: Double): String = "${value.roundToInt()} €"

    private fun percentile(sorted: List<Double>, p: Double): Double {
        if (sorted.size == 1) return sorted[0]
        val idx = p * (sorted.size - 1)
        val lower = idx.toInt()
        val upper = minOf(lower + 1, sorted.size - 1)
        val frac = idx - lower
        return sorted[lower] + (sorted[upper] - sorted[lower]) * frac
    }

    /** Titre de la première annonce eBay correspondant à ce code-barres (EAN/JAN), ou null. */
    suspend fun titleForBarcode(barcode: String): String? {
        if (!isConfigured() || barcode.isBlank()) return null
        val tok = token() ?: return null
        return try {
            api.search(bearer = "Bearer $tok", market = MARKET, gtin = barcode).itemSummaries
                ?.firstOrNull()?.title
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** Vrai si le titre ressemble à un goodie hors-sujet (poster, porte-clés...) plutôt qu'à une figurine. */
    private fun isLikelyUnrelatedItem(title: String): Boolean {
        val t = title.lowercase()
        return listOf(
            "poster", "affiche", "porte-cle", "porte-clé", "keychain", "sticker", "autocollant",
            "artbook", "art book", "carte postale", "postcard", "badge", "pin's", "pins",
            "tapis de souris", "mousepad", "coussin", "peluche", "plush"
        ).any { t.contains(it) }
    }

    /** Vrai si le titre confirme (vocabulaire figurine) le même profil boîte/accessoires déclaré. */
    private fun matchesCompleteness(title: String, hasBox: Boolean, hasAccessories: Boolean): Boolean {
        val t = title.lowercase()
        val looseHit = listOf("loose", "sans boite", "sans boîte", "out of box", "no box", "figurine seule").any { t.contains(it) }
        val completeHit = listOf("misb", "mint in box", "in box", "boxed", "boite", "boîte", "neuf", "neuve").any { t.contains(it) }
        return when {
            hasBox && hasAccessories -> completeHit && !looseHit
            hasBox || hasAccessories -> !looseHit
            else -> looseHit || !completeHit
        }
    }

    /** Identifiant(s) d'état eBay (champ structuré conditionIds), identiques quelle que soit la catégorie. */
    private fun conditionIdsFor(condition: Condition): String = when (condition) {
        Condition.NEUF -> "1000"   // New
        Condition.HS -> "7000"     // For parts or not working
        else -> "3000"             // Used (Mint/Très bon/Bon/Mauvais : eBay n'a pas plus fin)
    }

    private fun profileLabel(condition: Condition, hasBox: Boolean, hasAccessories: Boolean): String {
        val completeness = when {
            hasBox && hasAccessories -> "complet (boîte + accessoires)"
            hasBox -> "boîte sans accessoires"
            else -> "loose, sans boîte"
        }
        return "état ${condition.label}, $completeness"
    }

    /**
     * Construit la requête mots-clés. [condition] optionnel : l'omettre (null) élargit la
     * recherche quand la version stricte ne trouve rien (voir [lookup]).
     */
    private fun buildQuery(licence: Licence, character: String, name: String, condition: Condition?): String {
        val base = if (character.isBlank() || name.lowercase().contains(character.lowercase())) {
            name
        } else {
            "$character $name"
        }
        val licenceSuffix = if (licence != Licence.AUTRE && !base.lowercase().contains(licence.label.lowercase())) " ${licence.label}" else ""
        val hsSuffix = if (condition == Condition.HS) " HS pour pieces" else ""
        return "$base$licenceSuffix figurine$hsSuffix".trim()
    }
}
