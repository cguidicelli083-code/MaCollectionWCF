package com.nawash.macollectionwcf.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Outils de reconnaissance par photo (ML Kit + IA visuelle). La plupart des boîtes WCF ne
 * portent QUE du texte japonais (nom du personnage en katakana/kanji, gamme en japonais) :
 * un simple OCR ne suffit pas à en tirer un nom exploitable pour une recherche de cote. On lit
 * donc le code-barres par OCR/ML Kit (rapide, hors-ligne), puis on demande à une IA visuelle
 * (Gemini, repli Groq) d'identifier et traduire le personnage — bien plus fiable qu'un OCR brut
 * pour ce cas précis, voir [GeminiVision.identify].
 */
object ScanTools {

    data class ScanResult(
        val barcode: String?,
        val suggestedName: String?,
        val licence: Licence? = null,
        val series: String? = null
    )

    /** Scanner caméra : renvoie le code-barres lu, ou null si annulé / introuvable. */
    suspend fun scanCamera(context: Context): String? = try {
        GmsBarcodeScanning.getClient(context).startScan().await().rawValue
    } catch (e: Exception) {
        null
    }

    /**
     * Analyse une photo : code-barres (ML Kit) + identification du personnage. L'IA visuelle
     * (Gemini, repli Groq) est essayée en priorité — elle reconnaît le personnage à partir de son
     * apparence et traduit le texte japonais nécessaire, plutôt que de le lire tel quel. Si l'IA
     * échoue (non configurée, quota épuisé, réseau), on retombe sur la plus grosse ligne de texte
     * lue par OCR (latin + japonais) comme simple suggestion de nom. Ne lève jamais d'exception.
     */
    suspend fun scanImage(context: Context, uri: Uri, onDeepScan: (() -> Unit)? = null): ScanResult {
        val image = try {
            ImagePreprocess.enhance(context, uri)?.let { InputImage.fromBitmap(it, 0) }
                ?: InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            return ScanResult(null, null)
        }

        val barcodeScanner = BarcodeScanning.getClient()
        val barcode = try {
            barcodeScanner.process(image).await().firstOrNull()?.rawValue
        } catch (e: Exception) {
            null
        } finally {
            barcodeScanner.close()
        }

        onDeepScan?.invoke()
        val visual = runCatching { GeminiVision.identify(context, uri) }.getOrNull()
            ?: runCatching { GroqVision.identify(context, uri) }.getOrNull()
        if (visual != null) {
            return ScanResult(barcode, visual.character, visual.licence, visual.series)
        }

        val ocrGuess = runCatching { ocrBiggestLine(image) }.getOrNull()
        return ScanResult(barcode, ocrGuess)
    }

    /** Repli hors-ligne : plus grosse ligne de texte lue (OCR latin + japonais), sans traduction ni identification. */
    private suspend fun ocrBiggestLine(image: InputImage): String? {
        val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        try {
            val textLatin = latinRecognizer.process(image).await()
            val textJapanese: Text? = runCatching { japaneseRecognizer.process(image).await() }.getOrNull()

            fun cleanCandidate(raw: String) = raw.trim().replace(Regex("\\s+"), " ")
            data class TextCandidate(val text: String, val height: Int)

            val lineCandidates = textLatin.textBlocks.flatMap { block -> block.lines.map { TextCandidate(it.text, it.boundingBox?.height() ?: 0) } } +
                (textJapanese?.textBlocks?.flatMap { block -> block.lines.map { TextCandidate(it.text, it.boundingBox?.height() ?: 0) } } ?: emptyList())

            return lineCandidates
                .map { TextCandidate(cleanCandidate(it.text), it.height) }
                .filter { c ->
                    c.text.length in 3..50 &&
                        c.text.count { it.isDigit() }.toDouble() / c.text.length < 0.3
                }
                .distinctBy { it.text }
                .sortedByDescending { it.height }
                .map { it.text }
                .firstOrNull()
        } finally {
            latinRecognizer.close()
            japaneseRecognizer.close()
        }
    }
}
