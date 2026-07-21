package com.nawash.macollectionwcf.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Applique les photos de référence fournies manuellement dans `app/src/main/assets/catalog_photos/`
 * aux fiches du catalogue Encyclo correspondantes, identifiées par leur code (ex. "OP-427" dans le
 * nom de fichier). Ces photos sont VERROUILLÉES ([FigureCatalogEntry.photoLocked]) : elles ne sont
 * plus jamais écrasées par l'enrichissement automatique eBay/Tavily, ni par une purge globale.
 *
 * Workflow attendu : déposer les photos dans `E:\MaCollectionWCF\photos\` (nommées avec le code du
 * personnage visé, ex. "OP-427 - Mansherry.png"), puis les copier dans
 * `app/src/main/assets/catalog_photos/` avant un build (un asset Android ne peut pas pointer vers
 * un dossier hors du module `app`).
 *
 * Sécurité : si deux fichiers différents revendiquent le MÊME code, aucun n'est appliqué (conflit —
 * impossible de deviner lequel est le bon sans se tromper) ; le code appelant journalise
 * [ApplyResult.conflicts] pour signaler l'anomalie plutôt que de choisir au hasard.
 */
object CatalogReferencePhotos {
    private const val ASSET_DIR = "catalog_photos"
    private val CODE_REGEX = Regex("\\b(OP|BL|DB|NA)[\\s-]*([0-9]{2,4})\\b", RegexOption.IGNORE_CASE)

    /**
     * Convention réservée pour un rapprochement par (série, personnage) plutôt que par code interne
     * — utilisée quand une photo précise est trouvée pour un personnage nommément identifié (ex.
     * via ShandoraShop) mais dont le code "OP-xxx" auto-généré n'est pas connu à l'avance. Le nom
     * de fichier encode les deux textes normalisés séparés par un double underscore ; comparé au
     * même format normalisé côté base pour retrouver la bonne fiche sans ambiguïté.
     */
    private val NAME_MATCH_REGEX = Regex("^wcfmatch__(.+?)__(.+?)\\.[a-zA-Z0-9]+$", RegexOption.IGNORE_CASE)
    private fun slug(s: String) = s.lowercase().replace(Regex("[^a-z0-9]+"), "")

    data class ApplyResult(val applied: Int, val conflicts: List<String>, val unmatched: List<String>)

    suspend fun applyAll(context: Context, dao: FigureCatalogDao): ApplyResult {
        val files = try {
            context.assets.list(ASSET_DIR)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val byNumero = mutableMapOf<String, MutableList<String>>()
        val nameMatchFiles = mutableListOf<String>()
        val unmatched = mutableListOf<String>()
        for (file in files) {
            val nameMatch = NAME_MATCH_REGEX.find(file)
            if (nameMatch != null) {
                nameMatchFiles += file
                continue
            }
            val match = CODE_REGEX.find(file)
            if (match == null) {
                unmatched += file
                continue
            }
            val numero = "%s-%03d".format(match.groupValues[1].uppercase(), match.groupValues[2].toInt())
            byNumero.getOrPut(numero) { mutableListOf() }.add(file)
        }
        val conflicts = mutableListOf<String>()
        var applied = 0
        for ((numero, matchingFiles) in byNumero) {
            if (matchingFiles.size > 1) {
                conflicts += "$numero : ${matchingFiles.joinToString(", ")}"
                continue
            }
            val file = matchingFiles.first()
            val entry = dao.byNumero(numero) ?: continue
            val copied = copyAssetToInternal(context, file)
            if (copied != null) {
                dao.setLockedPhoto(entry.id, copied)
                applied++
            }
        }
        if (nameMatchFiles.isNotEmpty()) {
            val allEntries = dao.observeAll().first()
            for (file in nameMatchFiles) {
                val (seriesSlug, characterSlug) = NAME_MATCH_REGEX.find(file)!!.destructured
                val entry = allEntries.firstOrNull { slug(it.series) == seriesSlug && slug(it.character) == characterSlug }
                if (entry == null) {
                    unmatched += file
                    continue
                }
                val copied = copyAssetToInternal(context, file)
                if (copied != null) {
                    dao.setLockedPhoto(entry.id, copied)
                    applied++
                }
            }
        }
        return ApplyResult(applied, conflicts, unmatched)
    }

    private fun copyAssetToInternal(context: Context, assetName: String): String? = try {
        val file = File(context.filesDir, "catalog_ref_${assetName.hashCode()}.jpg")
        context.assets.open("$ASSET_DIR/$assetName").use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        null
    }
}
