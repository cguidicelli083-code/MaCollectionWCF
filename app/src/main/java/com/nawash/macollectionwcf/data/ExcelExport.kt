package com.nawash.macollectionwcf.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.Locale

/**
 * Export "Excel" du registre complet (collection + souhaits) sous forme de tableau avec une
 * photo par ligne. Pas de vraie librairie .xlsx (trop lourde sur Android) : on génère un document
 * **MHTML** (page web "fichier unique", multipart/related) — Excel l'ouvre nativement (avec un
 * avertissement de compatibilité de format, sans conséquence).
 *
 * Une image en data-URI directement dans `<img src="data:...">` ne s'affiche PAS de façon fiable
 * dans le moteur de rendu d'Excel. Le format MHTML référence chaque image via `cid:` et la
 * transporte comme une pièce jointe MIME séparée (`Content-ID`) — méthode native et fiable.
 */
object ExcelExport {

    private const val THUMBNAIL_MAX_DIMENSION = 200
    private const val THUMBNAIL_JPEG_QUALITY = 70
    private const val BOUNDARY = "----MACOLLECTIONWCF_EXPORT_BOUNDARY"

    private fun escapeHtml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun toThumbnailJpeg(bytes: ByteArray): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= THUMBNAIL_MAX_DIMENSION &&
            bounds.outHeight / (sampleSize * 2) >= THUMBNAIL_MAX_DIMENSION
        ) {
            sampleSize *= 2
        }
        val decoded = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: return null

        val scale = THUMBNAIL_MAX_DIMENSION.toFloat() / maxOf(decoded.width, decoded.height)
        val thumbnail = if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true)
        } else decoded

        val out = ByteArrayOutputStream()
        thumbnail.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_JPEG_QUALITY, out)
        return out.toByteArray()
    }

    private fun readPhotoBytes(uri: String): ByteArray? = try {
        if (uri.startsWith("file://")) {
            val path = Uri.parse(uri).path ?: return null
            val file = File(path)
            if (!file.exists()) null else file.readBytes()
        } else {
            URL(uri).openConnection().apply {
                connectTimeout = 5000
                readTimeout = 5000
            }.getInputStream().use { it.readBytes() }
        }
    } catch (e: Exception) {
        null
    }

    private fun priceCell(cents: Int?): String {
        if (cents == null || cents == 0) return ""
        val rate = AppPrefs.currencyRates.value[AppPrefs.currency.value] ?: 1.0
        val symbol = CurrencyOptions.symbolFor(AppPrefs.currency.value)
        return String.format(Locale.FRANCE, "%.2f %s", (cents / 100.0) * rate, symbol)
    }

    /** Encode en base64 avec retour à la ligne tous les 76 caractères, comme l'exige le MIME. */
    private fun mimeBase64(bytes: ByteArray): String {
        val raw = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return raw.chunked(76).joinToString("\r\n")
    }

    suspend fun export(context: Context, items: List<CollectionItem>, destUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val htmlBody = StringBuilder()
                val imageParts = StringBuilder()
                var photoIndex = 0

                htmlBody.append("<html><head><meta charset=\"UTF-8\"></head><body>")
                htmlBody.append("<table border=\"1\">")
                htmlBody.append(
                    "<tr>" +
                        "<th>Photo</th><th>Statut</th><th>Licence</th><th>Personnage</th><th>Nom commercial</th>" +
                        "<th>Gamme WCF</th><th>Éditeur</th><th>État</th><th>Boîte</th><th>Accessoires</th>" +
                        "<th>Année</th><th>Taille (cm)</th><th>Code-barres</th><th>Prix</th><th>Description</th>" +
                        "</tr>"
                )
                for (item in items) {
                    var photoCell = ""
                    val rawUri = item.imageUri
                    if (rawUri != null) {
                        val bytes = readPhotoBytes(rawUri)
                        val thumb = bytes?.let { toThumbnailJpeg(it) }
                        if (thumb != null) {
                            val cid = "photo$photoIndex"
                            photoIndex++
                            photoCell = "<img src=\"cid:$cid\" width=\"$THUMBNAIL_MAX_DIMENSION\" />"
                            imageParts.append("--$BOUNDARY\r\n")
                            imageParts.append("Content-Type: image/jpeg\r\n")
                            imageParts.append("Content-Transfer-Encoding: base64\r\n")
                            imageParts.append("Content-ID: <$cid>\r\n")
                            imageParts.append("Content-Location: $cid.jpg\r\n\r\n")
                            imageParts.append(mimeBase64(thumb))
                            imageParts.append("\r\n\r\n")
                        }
                    }
                    htmlBody.append("<tr>")
                    htmlBody.append("<td>$photoCell</td>")
                    htmlBody.append("<td>${if (item.isWishlist) "Souhait" else "Collection"}</td>")
                    htmlBody.append("<td>${escapeHtml(item.licence.label)}</td>")
                    htmlBody.append("<td>${escapeHtml(item.character)}</td>")
                    htmlBody.append("<td>${escapeHtml(item.name)}</td>")
                    htmlBody.append("<td>${escapeHtml(item.series.orEmpty())}</td>")
                    htmlBody.append("<td>${escapeHtml(item.manufacturer)}</td>")
                    htmlBody.append("<td>${escapeHtml(item.condition.label)}</td>")
                    htmlBody.append("<td>${if (item.hasBox) "Oui" else "Non"}</td>")
                    htmlBody.append("<td>${if (item.hasAccessories) "Oui" else "Non"}</td>")
                    htmlBody.append("<td>${item.releaseYear ?: ""}</td>")
                    htmlBody.append("<td>${item.heightCm ?: ""}</td>")
                    htmlBody.append("<td>${escapeHtml(item.barcode.orEmpty())}</td>")
                    htmlBody.append("<td>${priceCell(item.priceCents)}</td>")
                    htmlBody.append("<td>${escapeHtml(item.description.orEmpty())}</td>")
                    htmlBody.append("</tr>")
                }
                htmlBody.append("</table></body></html>")

                val mhtml = StringBuilder()
                mhtml.append("MIME-Version: 1.0\r\n")
                mhtml.append("Content-Type: multipart/related; boundary=\"$BOUNDARY\"; type=\"text/html\"\r\n\r\n")
                mhtml.append("--$BOUNDARY\r\n")
                mhtml.append("Content-Type: text/html; charset=\"utf-8\"\r\n")
                mhtml.append("Content-Transfer-Encoding: 8bit\r\n")
                mhtml.append("Content-Location: macollectionwcf.html\r\n\r\n")
                mhtml.append(htmlBody)
                mhtml.append("\r\n\r\n")
                mhtml.append(imageParts)
                mhtml.append("--$BOUNDARY--\r\n")

                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    out.write(mhtml.toString().toByteArray(Charsets.UTF_8))
                } ?: return@withContext false
                true
            } catch (e: Exception) {
                false
            }
        }
}
