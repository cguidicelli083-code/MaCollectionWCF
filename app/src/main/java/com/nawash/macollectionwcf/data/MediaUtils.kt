package com.nawash.macollectionwcf.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/** Outils de gestion des images locales. */
object MediaUtils {

    /** Nom de fichier affiché d'un URI de contenu (ex. "collection.xlsx"), ou null si indisponible. */
    fun displayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * Télécharge une photo distante (ex. photo d'annonce eBay associée à une figurine identifiée
     * par IA sans photo perso) et la copie dans le stockage interne, comme une photo importée à la
     * main ([copyToInternal]). Best-effort : renvoie null en cas d'échec réseau.
     */
    suspend fun downloadToInternal(context: Context, url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MaCollectionWcfApp/1.0 (Android app personnelle de collection)")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body ?: return@withContext null
                val file = File(context.filesDir, "item_${System.currentTimeMillis()}.jpg")
                body.byteStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                Uri.fromFile(file).toString()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Copie l'image choisie (galerie) dans le stockage interne de l'app
     * pour qu'elle persiste, et renvoie son URI ("file://...") ou null en cas d'échec.
     */
    fun copyToInternal(context: Context, uri: Uri): String? = try {
        val file = File(context.filesDir, "item_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        null
    }

    /**
     * Prépare un fichier vide dans le stockage interne pour une photo prise à la caméra.
     * Renvoie l'URI "content://" (à donner à la caméra pour qu'elle y écrive) et l'URI
     * "file://" correspondante (à utiliser ensuite dans l'app, comme [copyToInternal]).
     */
    fun newCameraFile(context: Context): Pair<Uri, String> {
        val file = File(context.filesDir, "item_${System.currentTimeMillis()}.jpg")
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return contentUri to Uri.fromFile(file).toString()
    }

    /** Supprime le fichier local d'une photo (galerie perso) lorsqu'elle est retirée. */
    fun deleteFile(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                uri.path?.let { File(it).delete() }
            }
        } catch (e: Exception) {
            // Pas grave si le fichier n'existe plus / n'est pas local.
        }
    }
}
