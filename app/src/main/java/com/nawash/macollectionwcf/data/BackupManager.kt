package com.nawash.macollectionwcf.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Tous les champs liste sont déclarés nullables : Gson construit cet objet par réflexion sans
 * jamais appeler le constructeur Kotlin, donc un champ absent d'une ANCIENNE sauvegarde (créée
 * avant son ajout) resterait à `null` malgré un type Kotlin non-nullable — neutralisé via
 * `?: emptyList()` à l'usage (piège documenté sur MaCollection : a cassé la restauration une fois).
 */
private data class BackupPayload(
    val version: Int = 1,
    val collectionItems: List<CollectionItem>?,
    val priceHistory: List<PriceHistory>?,
    val itemPhotos: List<ItemPhoto>?,
    val customPresets: List<CustomFigurePreset>?,
    val photoOverrides: List<PresetPhotoOverride>? = null
)

/**
 * Sauvegarde/restauration complète de la collection (données + photos personnelles) dans un
 * unique fichier .zip choisi par l'utilisateur (Téléchargements, Drive, clé USB...). Ce fichier
 * vit hors du stockage interne de l'app, donc survit à une désinstallation.
 */
object BackupManager {

    private val gson = Gson()

    suspend fun export(context: Context, db: AppDatabase, destUri: Uri): Boolean = withContext(Dispatchers.IO) { try {
        val collectionItems = db.collectionDao().observeAll().first()
        val itemPhotos = db.itemPhotoDao().getAll()
        val customPresets = db.customFigurePresetDao().observeAll().first()
        val priceHistory = db.priceHistoryDao().getAll()
        val photoOverrides = db.presetPhotoOverrideDao().observeAll().first()

        val localFiles = (collectionItems.mapNotNull { it.imageUri } +
            itemPhotos.map { it.uri } +
            customPresets.mapNotNull { it.photoUri } +
            photoOverrides.map { it.photoUri })
            .filter { it.startsWith("file://") }
            .distinct()

        val payload = BackupPayload(
            collectionItems = collectionItems,
            priceHistory = priceHistory,
            itemPhotos = itemPhotos,
            customPresets = customPresets,
            photoOverrides = photoOverrides
        )

        context.contentResolver.openOutputStream(destUri)?.use { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(gson.toJson(payload).toByteArray())
                zip.closeEntry()

                for (uriString in localFiles) {
                    val path = Uri.parse(uriString).path ?: continue
                    val file = File(path)
                    if (!file.exists()) continue
                    zip.putNextEntry(ZipEntry("photos/${file.name}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: return@withContext false
        true
    } catch (e: Exception) {
        false
    } }

    /** Remplace entièrement le contenu actuel par celui de la sauvegarde. */
    suspend fun import(context: Context, db: AppDatabase, srcUri: Uri): Boolean = withContext(Dispatchers.IO) { try {
        var payload: BackupPayload? = null

        context.contentResolver.openInputStream(srcUri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "data.json" -> {
                            val json = zip.readBytes().toString(Charsets.UTF_8)
                            payload = gson.fromJson(json, BackupPayload::class.java)
                        }
                        entry.name.startsWith("photos/") -> {
                            val fileName = entry.name.removePrefix("photos/")
                            val outFile = File(context.filesDir, fileName)
                            outFile.outputStream().use { out -> zip.copyTo(out) }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: return@withContext false

        val data = payload ?: return@withContext false

        db.clearAllTables()
        db.collectionDao().insertAll(data.collectionItems ?: emptyList())
        db.priceHistoryDao().insertAll(data.priceHistory ?: emptyList())
        db.itemPhotoDao().insertAll(data.itemPhotos ?: emptyList())
        db.customFigurePresetDao().insertAll(data.customPresets ?: emptyList())
        db.presetPhotoOverrideDao().insertAll(data.photoOverrides ?: emptyList())
        true
    } catch (e: Exception) {
        false
    } }
}
