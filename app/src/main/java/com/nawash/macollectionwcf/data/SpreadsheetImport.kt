package com.nawash.macollectionwcf.data

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.text.Normalizer
import java.util.zip.ZipFile
import kotlin.math.roundToInt

/**
 * Import d'un fichier tableur externe (.xlsx ou .csv) vers des [CollectionItem], même si ses
 * colonnes ne correspondent pas au format d'export de l'appli — l'utilisateur fait correspondre
 * lui-même chaque colonne à un champ via [ImportField] (voir l'écran de correspondance).
 *
 * Pas de vraie librairie .xlsx (trop lourde sur Android, voir la même remarque dans
 * [ExcelExport]) : un fichier .xlsx est un simple ZIP contenant des XML, on lit directement
 * `xl/sharedStrings.xml` + la première feuille avec les classes standard du JDK/Android
 * (`java.util.zip`, `XmlPullParser`) sans dépendance supplémentaire.
 */
object SpreadsheetImport {

    data class ParsedSheet(val headers: List<String>, val rows: List<List<String>>)

    enum class ImportField {
        CHARACTER, NAME, LICENCE, SERIES, MANUFACTURER, CONDITION, HAS_BOX, HAS_ACCESSORIES,
        YEAR, HEIGHT, PRICE, BARCODE, DESCRIPTION
    }

    /** Lit le fichier désigné par [uri] ; renvoie null si le format n'est pas reconnu/lisible. */
    fun parseFile(context: Context, uri: Uri): ParsedSheet? {
        val displayName = MediaUtils.displayName(context, uri).orEmpty().lowercase()
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        return when {
            displayName.endsWith(".csv") -> parseCsv(bytes)
            displayName.endsWith(".xlsx") -> parseXlsx(context, uri)
            bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() -> parseXlsx(context, uri)
            else -> parseCsv(bytes)
        }
    }

    // -----------------------------------------------------------------------
    // CSV
    // -----------------------------------------------------------------------

    private fun parseCsv(bytes: ByteArray): ParsedSheet? {
        val text = bytes.toString(Charsets.UTF_8).removePrefix("﻿")
        val lines = text.split("\r\n", "\n", "\r").filter { it.isNotBlank() }
        if (lines.isEmpty()) return null
        val delimiter = if (lines.first().count { it == ';' } > lines.first().count { it == ',' }) ';' else ','
        val parsed = lines.map { splitCsvLine(it, delimiter) }
        val width = parsed.first().size
        return ParsedSheet(parsed.first(), parsed.drop(1).map { row -> row + List((width - row.size).coerceAtLeast(0)) { "" } })
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { current.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                c == delimiter && !inQuotes -> { cells.add(current.toString()); current.clear() }
                else -> current.append(c)
            }
            i++
        }
        cells.add(current.toString())
        return cells.map { it.trim() }
    }

    // -----------------------------------------------------------------------
    // XLSX (zip + xml minimal)
    // -----------------------------------------------------------------------

    private fun parseXlsx(context: Context, uri: Uri): ParsedSheet? {
        val temp = File.createTempFile("import", ".xlsx", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { input.copyTo(it) }
            } ?: return null

            ZipFile(temp).use { zip ->
                val sharedStrings = zip.getEntry("xl/sharedStrings.xml")?.let { entry ->
                    zip.getInputStream(entry).use { parseSharedStrings(it) }
                } ?: emptyList()

                val sheetEntry = zip.entries().asSequence()
                    .filter { it.name.matches(Regex("xl/worksheets/sheet\\d+\\.xml")) }
                    .minByOrNull { it.name.substringAfter("sheet").substringBefore(".xml").toIntOrNull() ?: Int.MAX_VALUE }
                    ?: return null

                val rows = zip.getInputStream(sheetEntry).use { parseWorksheet(it, sharedStrings) }
                if (rows.isEmpty()) return null
                val width = rows.maxOf { it.size }
                val normalized = rows.map { row -> row + List((width - row.size).coerceAtLeast(0)) { "" } }
                return ParsedSheet(normalized.first(), normalized.drop(1))
            }
        } catch (e: Exception) {
            return null
        } finally {
            temp.delete()
        }
    }

    private fun parseSharedStrings(input: java.io.InputStream): List<String> {
        val result = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")
        var current: StringBuilder? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> if (parser.name == "si") current = StringBuilder()
                XmlPullParser.TEXT -> current?.append(parser.text)
                XmlPullParser.END_TAG -> if (parser.name == "si") {
                    result.add(current?.toString().orEmpty())
                    current = null
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun columnIndex(cellRef: String): Int {
        val letters = cellRef.takeWhile { it.isLetter() }
        return letters.fold(0) { acc, c -> acc * 26 + (c.uppercaseChar() - 'A' + 1) } - 1
    }

    private fun parseWorksheet(input: java.io.InputStream, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        var currentType: String? = null
        var currentCol = -1
        var currentValue: StringBuilder? = null

        val parser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> currentRow = mutableListOf()
                    "c" -> {
                        currentType = parser.getAttributeValue(null, "t")
                        currentCol = parser.getAttributeValue(null, "r")?.let { columnIndex(it) } ?: (currentRow.size)
                        while (currentRow.size < currentCol) currentRow.add("")
                    }
                    "v" -> currentValue = StringBuilder()
                    "t" -> if (currentType == "inlineStr") currentValue = StringBuilder()
                }
                XmlPullParser.TEXT -> {
                    if (currentValue != null) currentValue!!.append(parser.text)
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "c" -> {
                        val raw = currentValue?.toString().orEmpty()
                        val resolved = if (currentType == "s") raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: "" else raw
                        while (currentRow.size <= currentCol) currentRow.add("")
                        if (currentCol >= 0) currentRow[currentCol] = resolved
                        currentValue = null
                    }
                    "row" -> rows.add(currentRow)
                }
            }
            event = parser.next()
        }
        return rows
    }

    // -----------------------------------------------------------------------
    // Correspondance automatique des colonnes (devinette best-effort)
    // -----------------------------------------------------------------------

    private val fieldKeywords: Map<ImportField, List<String>> = mapOf(
        ImportField.CHARACTER to listOf("personnage", "character", "nom du personnage"),
        ImportField.NAME to listOf("nom commercial", "nom", "titre", "name", "title", "designation", "libelle"),
        ImportField.LICENCE to listOf("licence", "license", "serie tv", "anime"),
        ImportField.SERIES to listOf("gamme", "vague", "serie", "series", "wave"),
        ImportField.MANUFACTURER to listOf("editeur", "manufacturer", "marque", "brand", "fabricant"),
        ImportField.CONDITION to listOf("etat", "condition", "state"),
        ImportField.HAS_BOX to listOf("boite", "box", "boxed"),
        ImportField.HAS_ACCESSORIES to listOf("accessoire", "accessories"),
        ImportField.YEAR to listOf("annee", "year", "sortie"),
        ImportField.HEIGHT to listOf("taille", "hauteur", "height", "size"),
        ImportField.PRICE to listOf("prix", "cote", "valeur", "price", "value"),
        ImportField.BARCODE to listOf("codebarre", "codebarres", "barcode", "ean", "gtin", "upc"),
        ImportField.DESCRIPTION to listOf("description", "desc", "resume", "synopsis")
    )

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")

    /** Devine la colonne de chaque champ à partir des en-têtes ; null = pas de correspondance trouvée. */
    fun guessMapping(headers: List<String>): Map<ImportField, Int?> {
        val normalizedHeaders = headers.map { normalize(it) }
        val used = mutableSetOf<Int>()
        val result = mutableMapOf<ImportField, Int?>()
        for (field in ImportField.values()) {
            val keywords = fieldKeywords[field].orEmpty()
            val match = normalizedHeaders.indices.firstOrNull { idx ->
                idx !in used && keywords.any { normalizedHeaders[idx].contains(it) }
            }
            result[field] = match
            if (match != null) used.add(match)
        }
        return result
    }

    // -----------------------------------------------------------------------
    // Ligne -> CollectionItem (tolérant : valeurs libres, casse, accents...)
    // -----------------------------------------------------------------------

    private fun cell(row: List<String>, mapping: Map<ImportField, Int?>, field: ImportField): String? =
        mapping[field]?.let { row.getOrNull(it) }?.trim()?.takeIf { it.isNotEmpty() }

    private fun matchEnum(text: String?, options: List<Pair<String, String>>): String? {
        if (text.isNullOrBlank()) return null
        val n = normalize(text)
        return options.firstOrNull { normalize(it.second) == n || normalize(it.second).contains(n) }?.first
    }

    private fun parseBool(text: String?, default: Boolean): Boolean {
        if (text.isNullOrBlank()) return default
        val n = normalize(text)
        return when {
            listOf("oui", "yes", "true", "1", "x", "vrai").any { n == it } -> true
            listOf("non", "no", "false", "0", "faux").any { n == it } -> false
            else -> default
        }
    }

    private fun parsePriceCents(text: String?, currencyRate: Double): Int? {
        if (text.isNullOrBlank()) return null
        val cleaned = text.replace(Regex("[^0-9,.\\-]"), "").replace(",", ".")
        val value = cleaned.toDoubleOrNull() ?: return null
        if (currencyRate <= 0.0) return null
        return ((value / currencyRate) * 100).roundToInt()
    }

    /** Construit un [CollectionItem] depuis une ligne ; null si le personnage (champ obligatoire) manque. */
    fun buildItem(
        row: List<String>,
        mapping: Map<ImportField, Int?>,
        isWishlist: Boolean,
        currencyRate: Double
    ): CollectionItem? {
        val character = cell(row, mapping, ImportField.CHARACTER) ?: return null

        val licenceMatch = matchEnum(cell(row, mapping, ImportField.LICENCE), Licence.entries.map { it.name to it.label })
        val licence = licenceMatch?.let { Licence.valueOf(it) } ?: Licence.AUTRE

        val conditionMatch = matchEnum(cell(row, mapping, ImportField.CONDITION), Condition.entries.map { it.name to it.label })
        val condition = conditionMatch?.let { Condition.valueOf(it) } ?: Condition.BON

        val priceCents = parsePriceCents(cell(row, mapping, ImportField.PRICE), currencyRate)

        return CollectionItem(
            licence = licence,
            series = cell(row, mapping, ImportField.SERIES),
            character = character,
            name = cell(row, mapping, ImportField.NAME) ?: character,
            manufacturer = cell(row, mapping, ImportField.MANUFACTURER) ?: "Banpresto",
            condition = condition,
            hasBox = parseBool(cell(row, mapping, ImportField.HAS_BOX), default = true),
            hasAccessories = parseBool(cell(row, mapping, ImportField.HAS_ACCESSORIES), default = true),
            heightCm = cell(row, mapping, ImportField.HEIGHT)?.replace(",", ".")?.toDoubleOrNull(),
            releaseYear = cell(row, mapping, ImportField.YEAR)?.toIntOrNull(),
            priceCents = priceCents,
            priceIsManual = priceCents != null,
            barcode = cell(row, mapping, ImportField.BARCODE),
            description = cell(row, mapping, ImportField.DESCRIPTION),
            isWishlist = isWishlist
        )
    }
}
