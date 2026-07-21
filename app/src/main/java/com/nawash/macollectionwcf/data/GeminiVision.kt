package com.nawash.macollectionwcf.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import com.nawash.macollectionwcf.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

// --- DTO Gemini (generateContent) ---
private data class GemPart(val text: String? = null, val inline_data: GemInlineData? = null)
private data class GemInlineData(val mime_type: String, val data: String)
private data class GemContent(val parts: List<GemPart>)
private class GemGoogleSearch
private data class GemTool(val google_search: GemGoogleSearch = GemGoogleSearch())
private data class GemRequest(val contents: List<GemContent>, val tools: List<GemTool>? = null)
private data class GemCandidate(val content: GemContent?)
private data class GemResponse(val candidates: List<GemCandidate>?)
// JSON structuré renvoyé par le prompt d'estimation de cote (voir [GeminiVision.estimatePrice]).
private data class GemPriceEstimate(val prix_eur: Double?)
// JSON structuré renvoyé par le prompt d'identification visuelle (voir [GeminiVision.identify]).
private data class GemFigureResult(val licence: String?, val personnage: String?, val serie: String?)
// Une entrée du tableau JSON renvoyé par le scan multiple (lot/étagère).
private data class GemBatchEntry(val licence: String?, val personnage: String?, val serie: String?)

private interface GeminiApi {
    // flash-lite : quota gratuit journalier bien plus élevé que gemini-2.5-flash (vérifié sur
    // le projet MaCollection original : 20 req/jour sur ce dernier, contre un bucket séparé et
    // plus généreux pour flash-lite).
    @POST("v1beta/models/gemini-2.5-flash-lite:generateContent")
    suspend fun generate(
        @Query("key") key: String,
        @Body body: GemRequest
    ): GemResponse
}

/**
 * Reconnaissance visuelle par IA (identification du personnage) + estimation de cote, toutes
 * deux via Gemini. Ne lève jamais : renvoie null en cas d'échec pour laisser Groq/Tavily prendre
 * le relais.
 */
object GeminiVision {

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }

    private val key: String get() = BuildConfig.GEMINI_API_KEY

    fun isConfigured(): Boolean = key.isNotBlank()

    // Mémorise jusqu'à quand (minuit) le quota quotidien gratuit est épuisé (vérifié en conditions
    // réelles sur le projet original : 20 requêtes/JOUR sur ce modèle). Une fois un 429 "PerDay" vu,
    // on saute directement au repli Groq/Tavily pour le reste de la session.
    @Volatile
    private var quotaExhaustedUntil: Long = 0L

    private fun isQuotaExhausted(): Boolean = System.currentTimeMillis() < quotaExhaustedUntil

    private fun markQuotaExhausted() {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        quotaExhaustedUntil = cal.timeInMillis
    }

    private fun isDailyQuotaError(e: retrofit2.HttpException): Boolean {
        if (e.code() != 429) return false
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        return body.contains("PerDay")
    }

    /** Personnage identifié visuellement par l'IA (licence + nom traduit/romanisé + gamme si visible). */
    data class VisualResult(val licence: Licence, val character: String, val series: String?)

    // Beaucoup de boîtes WCF ne portent QUE du texte japonais (nom du personnage en katakana/kanji,
    // gamme en japonais) : un simple OCR ne suffit pas à en tirer un nom exploitable pour une
    // recherche de cote. On demande donc à l'IA de RECONNAÎTRE le personnage visuellement (logos,
    // apparence, artwork de la boîte) et de traduire tout texte japonais nécessaire vers son nom
    // internationalement connu (romanisation standard), plutôt que de renvoyer le texte brut lu.
    internal const val PROMPT =
        "Tu es un expert des figurines Banpresto World Collectable Figure (WCF), issues de licences " +
        "d'anime/manga japonaises. Analyse cette photo (figurine ou boîte, éventuellement couverte de " +
        "texte japonais) et identifie précisément le personnage représenté, en t'aidant de son " +
        "apparence, des artworks et logos visibles, et en traduisant tout texte japonais nécessaire " +
        "(katakana/kanji) vers le nom du personnage internationalement connu (romanisation standard, " +
        "ex. \"モンキー・D・ルフィ\" -> \"Monkey D. Luffy\"). Réponds UNIQUEMENT en JSON strict, sans " +
        "texte ni balise autour : {\"licence\":\"one_piece|bleach|dragon_ball|naruto|autre\"," +
        "\"personnage\":\"nom du personnage\",\"serie\":\"gamme/vague WCF visible sur la boîte, ou null\"}. " +
        "Si tu ne reconnais rien de fiable : {\"licence\":null,\"personnage\":null,\"serie\":null}."

    /**
     * Identification visuelle structurée d'une figurine, ou null (image illisible, IA non
     * configurée/quota épuisé, rien de reconnu ou échec réseau).
     */
    suspend fun identify(context: Context, uri: Uri): VisualResult? {
        if (!isConfigured() || isQuotaExhausted()) return null
        val base64 = withContext(Dispatchers.IO) { encodeImage(context, uri) } ?: return null
        val request = GemRequest(
            listOf(GemContent(listOf(
                GemPart(text = PROMPT),
                GemPart(inline_data = GemInlineData("image/jpeg", base64))
            )))
        )
        return try {
            val text = api.generate(key, request)
                .candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.text != null }?.text
                ?: return null
            parseProduct(text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            if (isDailyQuotaError(e)) markQuotaExhausted()
            null
        } catch (e: Exception) {
            null
        }
    }

    /** Une figurine détectée dans un lot par le scan multiple. */
    data class BatchItem(val licence: Licence, val character: String, val series: String?)

    internal const val BATCH_PROMPT =
        "Tu es un expert des figurines Banpresto World Collectable Figure (WCF), issues de licences " +
        "d'anime/manga japonaises. Cette photo montre PLUSIEURS figurines (une étagère, un lot…). " +
        "Isole visuellement chaque figurine distincte et identifie précisément le personnage de " +
        "chacune, en traduisant tout texte japonais nécessaire (katakana/kanji) vers son nom " +
        "internationalement connu (romanisation standard). Réponds UNIQUEMENT par un tableau JSON " +
        "strict, sans texte ni balise autour : [{\"licence\":\"one_piece|bleach|dragon_ball|naruto|" +
        "autre\",\"personnage\":\"nom du personnage\",\"serie\":\"gamme/vague WCF visible, ou null\"}]. " +
        "Une entrée par figurine réellement visible et identifiable, MÊME si plusieurs exemplaires " +
        "identiques du même personnage sont présents (ex. 2 Luffy côte à côte = 2 entrées distinctes, " +
        "pas une seule) : ne fusionne jamais deux personnages identiques en une seule entrée. Ne " +
        "devine pas un personnage que tu ne reconnais pas vraiment. Si rien de fiable : []."

    /**
     * Scan multiple : identifie toutes les figurines présentes sur une photo de lot. L'image
     * traitée (contraste rehaussé, sans recadrage) est envoyée à l'IA. Null si non configuré/quota
     * épuisé/échec ; liste (éventuellement vide) des figurines détectées sinon.
     */
    suspend fun identifyBatch(context: Context, uri: Uri): List<BatchItem>? {
        if (!isConfigured() || isQuotaExhausted()) return null
        val base64 = withContext(Dispatchers.IO) { encodeImage(context, uri) } ?: return null
        val request = GemRequest(
            listOf(GemContent(listOf(
                GemPart(text = BATCH_PROMPT),
                GemPart(inline_data = GemInlineData("image/jpeg", base64))
            )))
        )
        return try {
            val text = api.generate(key, request)
                .candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.text != null }?.text
                ?: return null
            parseBatch(text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            if (isDailyQuotaError(e)) markQuotaExhausted()
            null
        } catch (e: Exception) {
            null
        }
    }

    internal fun parseBatch(rawText: String): List<BatchItem>? {
        val arr = runCatching { Gson().fromJson(stripFences(rawText), Array<GemBatchEntry>::class.java) }.getOrNull()
            ?: return null
        return arr.mapNotNull { e ->
            e.personnage?.trim()?.takeIf { it.isNotBlank() && it.length in 2..80 }?.let { character ->
                BatchItem(matchLicence(e.licence), character, e.serie?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", true) })
            }
        }
    }

    /**
     * Estimation de cote pour une figurine WCF. Renvoie le prix en centimes d'euro, ou null
     * (IA non configurée/quota épuisé, rien trouvé, échec réseau).
     */
    suspend fun estimatePrice(
        licence: Licence, character: String, name: String,
        condition: Condition, hasBox: Boolean, hasAccessories: Boolean
    ): Int? {
        if (!isConfigured() || isQuotaExhausted()) return null
        val completeness = when {
            hasBox && hasAccessories -> "complète en boîte avec ses accessoires (armes, pièces interchangeables...)"
            hasBox -> "en boîte sans ses accessoires"
            else -> "loose (sans boîte)"
        }
        val itemName = if (character.isBlank() || name.lowercase().contains(character.lowercase())) name else "$character $name"
        val licenceNote = if (licence != Licence.AUTRE && !itemName.lowercase().contains(licence.label.lowercase())) " (licence ${licence.label})" else ""
        val prompt = "Cherche sur internet des annonces ou ventes récentes en France pour cette figurine " +
            "World Collectable Figure (WCF, Banpresto) d'occasion : \"$itemName\"$licenceNote, état " +
            "${condition.label}, $completeness. Regarde en particulier sur Vinted, LeBoncoin et eBay. " +
            "Réponds UNIQUEMENT en JSON strict, sans texte ni balise autour : {\"prix_eur\": nombre en " +
            "euros, ou null si tu ne trouves vraiment rien de fiable}."
        val request = GemRequest(
            contents = listOf(GemContent(listOf(GemPart(text = prompt)))),
            tools = listOf(GemTool())
        )
        repeat(3) { attempt ->
            try {
                val parts = api.generate(key, request)
                    .candidates?.firstOrNull()?.content?.parts?.mapNotNull { it.text } ?: return null
                if (parts.isEmpty()) return null
                val fullText = parts.joinToString("\n")
                val price = extractJsonPrice(fullText) ?: extractPriceFromFreeText(fullText) ?: return null
                return if (price <= 0) null else (price * 100).roundToInt()
            } catch (e: CancellationException) {
                throw e
            } catch (e: retrofit2.HttpException) {
                if (isDailyQuotaError(e)) {
                    markQuotaExhausted()
                    return null
                }
                if (attempt == 2) return null
                delay(700L)
            } catch (e: Exception) {
                if (attempt == 2) return null
                delay(700L)
            }
        }
        return null
    }

    /**
     * Cherche le JSON `{"prix_eur": ...}` demandé n'importe où dans un texte. Internal (pas
     * private) : réutilisé par [TavilyPriceEstimate], 3e recours de prix.
     */
    internal fun extractJsonPrice(text: String): Double? {
        val match = Regex("\\{[^{}]*\"prix_eur\"[^{}]*}").find(text) ?: return null
        return runCatching { Gson().fromJson(match.value, GemPriceEstimate::class.java) }.getOrNull()?.prix_eur
    }

    /** Dernier recours si le JSON demandé est introuvable ([extractJsonPrice]). */
    internal fun extractPriceFromFreeText(text: String): Double? {
        val after = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:€|euros?)", RegexOption.IGNORE_CASE).find(text)
        if (after != null) return after.groupValues[1].replace(',', '.').toDoubleOrNull()
        val before = Regex("€\\s*(\\d+(?:[.,]\\d+)?)", RegexOption.IGNORE_CASE).find(text) ?: return null
        return before.groupValues[1].replace(',', '.').toDoubleOrNull()
    }

    // Parsing partagé (réutilisé par [GroqVision]) : nettoie les balises ```json et un éventuel
    // bloc <think>...</think> (certains modèles "thinking" enveloppent leur réponse dedans avant
    // le JSON demandé — vérifié en conditions réelles sur le projet original avec Qwen3.6/Groq).
    internal fun stripFences(text: String): String =
        text.trim()
            .replace(Regex("(?s)<think>.*?</think>"), "")
            .trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

    internal fun parseProduct(rawText: String): VisualResult? {
        val p = runCatching { Gson().fromJson(stripFences(rawText), GemFigureResult::class.java) }.getOrNull() ?: return null
        val character = p.personnage?.trim().orEmpty()
        if (character.isBlank() || character.length > 80) return null
        return VisualResult(matchLicence(p.licence), character, p.serie?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", true) })
    }

    private fun matchLicence(raw: String?): Licence {
        val n = raw?.trim()?.lowercase().orEmpty()
        return when {
            n.contains("one") && n.contains("piece") -> Licence.ONE_PIECE
            n.contains("bleach") -> Licence.BLEACH
            n.contains("dragon") && n.contains("ball") -> Licence.DRAGON_BALL
            n.contains("naruto") -> Licence.NARUTO
            else -> Licence.AUTRE
        }
    }

    // Prétraite l'image (sous-échantillonnage + contraste, voir [ImagePreprocess]) puis la
    // ré-encode en JPEG base64 : requête plus légère et texte/logos plus lisibles pour l'IA.
    internal fun encodeImage(context: Context, uri: Uri): String? = try {
        ImagePreprocess.enhance(context, uri)?.let { bmp ->
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            bmp.recycle()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }
    } catch (e: Exception) {
        null
    }
}
