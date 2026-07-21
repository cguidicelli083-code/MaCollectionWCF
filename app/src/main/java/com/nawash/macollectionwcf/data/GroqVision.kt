package com.nawash.macollectionwcf.data

import android.content.Context
import android.net.Uri
import com.nawash.macollectionwcf.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// --- DTO Groq (API compatible OpenAI /chat/completions) ---
private data class GqImageUrl(val url: String)
private data class GqContent(val type: String, val text: String? = null, val image_url: GqImageUrl? = null)
private data class GqMessage(val role: String, val content: List<GqContent>)
private data class GqRequest(
    val model: String,
    val messages: List<GqMessage>,
    val temperature: Double = 0.2,
    val reasoning_effort: String? = null
)
private data class GqRespMsg(val content: String?)
private data class GqChoice(val message: GqRespMsg?)
private data class GqResponse(val choices: List<GqChoice>?)

private interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun chat(@Header("Authorization") auth: String, @Body body: GqRequest): GqResponse
}

/**
 * Reconnaissance visuelle de secours (Groq, modèle multimodal) + modèle texte, utilisés quand
 * Gemini échoue — typiquement quota journalier gratuit atteint. Réutilise le prétraitement
 * d'image et le parsing JSON de [GeminiVision] : même prompt, même type de résultat
 * (VisualResult), donc interchangeable avec Gemini sans rien changer en aval. Ne lève jamais :
 * renvoie null en cas d'échec.
 */
object GroqVision {

    // Modèle multimodal disponible en free tier. Llama 4 Scout a été décommissionné par Groq le
    // 17/07/2026 ; remplacé par Qwen3.6 27B, le seul autre modèle vision proposé par Groq à cette
    // date. Modèle « preview » chez Groq (pas de SLA production).
    private const val VISION_MODEL = "qwen/qwen3.6-27b"
    // Modèle texte rapide (utilisé par TavilyPriceEstimate), quota séparé de la vision.
    private const val TEXT_MODEL = "llama-3.1-8b-instant"

    private val api: GroqApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApi::class.java)
    }

    private val key: String get() = BuildConfig.GROQ_API_KEY

    fun isConfigured(): Boolean = key.isNotBlank()

    /** Équivalent Groq de [GeminiVision.identify], utilisé quand Gemini échoue/quota épuisé. */
    suspend fun identify(context: Context, uri: Uri): GeminiVision.VisualResult? =
        request(context, uri, GeminiVision.PROMPT)?.let { GeminiVision.parseProduct(it) }

    /** Équivalent Groq de [GeminiVision.identifyBatch] (scan multiple), utilisé en repli. */
    suspend fun identifyBatch(context: Context, uri: Uri): List<GeminiVision.BatchItem>? =
        request(context, uri, GeminiVision.BATCH_PROMPT)?.let { GeminiVision.parseBatch(it) }

    /** Envoie un prompt texte brut à Groq et renvoie sa réponse telle quelle, ou null en cas d'échec. */
    suspend fun completeText(prompt: String): String? {
        if (!isConfigured()) return null
        val body = GqRequest(TEXT_MODEL, listOf(GqMessage("user", listOf(GqContent("text", text = prompt)))))
        return try {
            api.chat("Bearer $key", body).choices?.firstOrNull()?.message?.content
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** Vrai si l'erreur HTTP correspond à la limite de DÉBIT Groq (tokens/minute), pas à un quota
     *  journalier : vérifié en conditions réelles, le modèle vision gratuit (8000 TPM) est presque
     *  entièrement consommé par une seule photo un peu lourde + la réponse verbeuse du modèle
     *  "thinking" — un simple 429 "rate_limit_exceeded", pas un blocage pour le reste de la journée. */
    private fun isRateLimitError(e: retrofit2.HttpException): Boolean {
        if (e.code() != 429) return false
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        return body.contains("rate_limit_exceeded")
    }

    // Envoie l'image (prétraitée par ImagePreprocess via GeminiVision) + le prompt, renvoie le
    // texte brut de la réponse (JSON éventuellement entre balises, nettoyé par le parseur partagé).
    // Une seule nouvelle tentative après une pause si la limite de débit (tokens/minute) est
    // atteinte : la fenêtre se libère au bout de quelques secondes (l'API l'indique elle-même,
    // "Please try again in ~10s"), pas la peine d'abandonner tout de suite comme un vrai échec.
    private suspend fun request(context: Context, uri: Uri, prompt: String): String? {
        if (!isConfigured()) return null
        val base64 = withContext(Dispatchers.IO) { GeminiVision.encodeImage(context, uri) } ?: return null
        val body = GqRequest(
            model = VISION_MODEL,
            messages = listOf(GqMessage("user", listOf(
                GqContent("text", text = prompt),
                GqContent("image_url", image_url = GqImageUrl("data:image/jpeg;base64,$base64"))
            ))),
            // Qwen3.6 est un modèle "thinking" : sans ce paramètre, il génère plusieurs MILLIERS de
            // tokens de raisonnement interne avant sa vraie réponse (1784 tokens observés en test
            // réel pour une simple description !), engloutissant à lui seul l'essentiel du budget
            // gratuit Groq (8000 tokens/minute). "none" supprime ce raisonnement (vérifié en direct :
            // fait chuter la consommation de ~3600 à ~2000 tokens sur le même test) — laisse bien
            // plus de marge pour enchaîner des scans sans retomber dans la limite de débit.
            reasoning_effort = "none"
        )
        repeat(2) { attempt ->
            try {
                return api.chat("Bearer $key", body).choices?.firstOrNull()?.message?.content
            } catch (e: CancellationException) {
                throw e
            } catch (e: retrofit2.HttpException) {
                if (!(isRateLimitError(e) && attempt == 0)) return null
                delay(11_000L)
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }
}
