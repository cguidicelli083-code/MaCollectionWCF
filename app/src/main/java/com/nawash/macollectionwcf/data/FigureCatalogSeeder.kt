package com.nawash.macollectionwcf.data

/**
 * Peuple la table Room `figure_catalog` à partir des listes Kotlin vérifiées `figurePresets`
 * (voir `FigurePresetsOnePiece.kt`/`...Extra.kt`) au premier lancement suivant leur ajout.
 * Le contrôle se fait licence par licence (pas juste "table vide ?") pour pouvoir peupler
 * Bleach/Dragon Ball/Naruto plus tard sans dupliquer ce qui est déjà en base.
 */
object FigureCatalogSeeder {

    suspend fun seedIfNeeded(dao: FigureCatalogDao) {
        val alreadySeeded = dao.distinctLicences().toSet()
        val toSeed = figurePresets.filter { it.licence !in alreadySeeded }
        if (toSeed.isEmpty()) return
        val entries = toSeed.groupBy { it.licence }.flatMap { (licence, list) ->
            list.mapIndexed { index, preset ->
                FigureCatalogEntry(
                    licence = licence,
                    series = preset.series?.ifBlank { null } ?: "Autre",
                    numero = "%s-%03d".format(licencePrefix(licence), index + 1),
                    character = preset.character,
                    releaseYear = preset.year,
                    heightCm = preset.heightCm
                )
            }
        }
        dao.insertAll(entries)
    }

    /**
     * Rattrape [FigureCatalogEntry.releaseYear]/[heightCm] sur des fiches déjà en base pour une
     * licence déjà peuplée (le seeding normal ne s'exécute qu'une fois par licence — sans ce
     * passage, mettre à jour `FigurePresetsXxx.kt` avec une date/taille nouvellement trouvée ne se
     * répercuterait jamais sur les lignes déjà insérées). Ne fait qu'AJOUTER de l'info (ne
     * remplace jamais une valeur déjà connue par du vide), et ne touche à rien d'autre (photo,
     * côte, verrouillage restent intacts). Correspondance par (série, personnage) — suffisant tant
     * qu'un même personnage n'apparaît qu'une fois par coffret dans les fichiers de données.
     */
    suspend fun backfillMissingData(dao: FigureCatalogDao) {
        for ((licence, presets) in figurePresets.groupBy { it.licence }) {
            val existing = dao.byLicence(licence).groupBy { (it.series to it.character) }
            for (preset in presets) {
                val row = existing[(preset.series?.ifBlank { null } ?: "Autre") to preset.character]?.firstOrNull() ?: continue
                val newYear = row.releaseYear ?: preset.year
                val newHeight = row.heightCm ?: preset.heightCm
                if (newYear != row.releaseYear || newHeight != row.heightCm) {
                    dao.backfill(row.id, newYear, newHeight)
                }
            }
        }
    }

    /**
     * Corrige la numérotation `numero` (ex. "MH-001"..."MH-006" pour un Vol.1 de 6 figurines) sur
     * des fiches déjà en base, pour toute licence déjà peuplée. Nécessaire car [seedIfNeeded] ne
     * s'exécute qu'une seule fois par licence : si `FigurePresetsXxx.kt` est corrigé après coup
     * (ex. 2 personnages oubliés dans un coffret, décalant tout le reste), les figurines
     * manquantes doivent être insérées et la référence `numero` de TOUTES les fiches de la licence
     * réalignée sur l'ordre du code. Correspondance par (série, personnage) : les fiches déjà
     * enrichies (photo/côte/verrouillage) ne perdent jamais ces données, seul `numero` change et
     * les nouvelles entrées manquantes sont insérées.
     */
    suspend fun resyncNumbering(dao: FigureCatalogDao) {
        for ((licence, presets) in figurePresets.groupBy { it.licence }) {
            val existing = dao.byLicence(licence)
            val existingByKey = existing.associateBy { it.series to it.character }
            val presetKeys = presets.map { (it.series?.ifBlank { null } ?: "Autre") to it.character }.toSet()

            val obsolete = existing.filter { (it.series to it.character) !in presetKeys }
            if (obsolete.isNotEmpty()) dao.deleteAll(obsolete)

            // Purge des doublons en surnombre : si un même (série, personnage) apparaît plusieurs
            // fois en base mais moins souvent dans le code (ex. un des deux "Monkey D. Luffy"
            // identiques d'un coffret a été renommé en "Monkey D. Luffy (Tuxedo)" après découverte
            // que c'étaient deux figurines distinctes — voir Treasure Rally III Roger, 2026-07-19),
            // on ne garde que le nombre de lignes encore nécessaire, en gardant en priorité celles
            // qui ont déjà une photo/côte plutôt que des doublons vides.
            val neededCountByKey = presets.groupingBy { (it.series?.ifBlank { null } ?: "Autre") to it.character }.eachCount()
            val remainingByKey = (existing - obsolete.toSet()).groupBy { it.series to it.character }
            val surplus = mutableListOf<FigureCatalogEntry>()
            for ((key, rows) in remainingByKey) {
                val needed = neededCountByKey[key] ?: 0
                if (rows.size > needed) {
                    val sorted = rows.sortedByDescending { (if (it.imagePath != null) 1 else 0) + (if (it.priceCents != null) 1 else 0) }
                    surplus += sorted.drop(needed)
                }
            }
            if (surplus.isNotEmpty()) dao.deleteAll(surplus)

            val toInsert = mutableListOf<FigureCatalogEntry>()
            presets.forEachIndexed { index, preset ->
                val numero = "%s-%03d".format(licencePrefix(licence), index + 1)
                val key = (preset.series?.ifBlank { null } ?: "Autre") to preset.character
                val row = existingByKey[key]
                if (row == null) {
                    toInsert += FigureCatalogEntry(
                        licence = licence,
                        series = key.first,
                        numero = numero,
                        character = preset.character,
                        releaseYear = preset.year,
                        heightCm = preset.heightCm
                    )
                } else if (row.numero != numero) {
                    dao.updateNumero(row.id, numero)
                }
            }
            if (toInsert.isNotEmpty()) dao.insertAll(toInsert)
        }
    }

    private fun licencePrefix(licence: Licence): String = when (licence) {
        Licence.ONE_PIECE -> "OP"
        Licence.BLEACH -> "BL"
        Licence.DRAGON_BALL -> "DB"
        Licence.NARUTO -> "NA"
        Licence.DEMON_SLAYER -> "DS"
        Licence.JUJUTSU_KAISEN -> "JJ"
        Licence.MY_HERO_ACADEMIA -> "MH"
        Licence.DISNEY -> "DI"
        Licence.MARVEL -> "MV"
        Licence.AUTRE -> "AU"
    }
}
