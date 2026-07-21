package com.nawash.macollectionwcf.data

/**
 * Catalogue vérifié des figurines WCF Naruto (Banpresto), gamme "NarutoP99" (les 100 personnages
 * les plus populaires élus par les fans, illustrations originales de Kishimoto).
 *
 * 2026-07-19 — Vol.1 à 5 complétés en coffrets COMPLETS (5/5 figurines chacun), sur demande de
 * l'utilisateur qui a fourni les fiches shandorashop.com de chaque volume. Ces fiches (API JSON
 * Shopify) donnent hauteur/date/nombre de types mais PAS les noms des personnages ; retrouvés en
 * croisant l'ID produit Bandai visible dans le nom du fichier photo (ex. "2690112_2a.jpg") avec
 * sa fiche officielle bsp-prize.jp/item/<id>/ correspondante (IDs séquentiels 2690110 à 2690113
 * pour Vol.1-4, 2694859 pour Vol.5) :
 * - Vol.1 (déc. 2023, ~6cm) : Naruto Uzumaki, Shikamaru Nara, Gaara, Jiraiya, Hashirama Senju.
 * - Vol.2 (déc. 2023, ~7cm) : Minato Namikaze, Kakashi Hatake, Sakumo Hatake, Obito Uchiha,
 *   Madara Uchiha.
 * - Vol.3 (janv. 2024, ~7cm) : Itachi Uchiha, Shisui Uchiha, Neji Hyuga, Hinata Hyuga,
 *   Minato Namikaze (Gold ver.).
 * - Vol.4 (févr. 2024, ~7cm) : Sakura Haruno, Rock Lee, Might Guy, Sasori, Itachi Uchiha
 *   (Gold ver.) — confirme et complète l'entrée déjà connue via toywiz.com.
 * - Vol.5 (mars 2024, ~6cm) : Sasuke Uchiha, Sakura Haruno (Gold ver.), Tobirama Senju, Deidara,
 *   Renard à neuf queues (Kurama) — confirme et complète l'entrée déjà connue via toywiz.com.
 * Léger écart de date entre bsp-prize (sortie Japon officielle) et ShandoraShop (import/mise en
 * ligne boutique) sur certains volumes — seule l'ANNÉE est retenue ici (granularité du champ
 * `year`), donc sans impact : les deux sources s'accordent sur 2023 (Vol.1-2) et 2024 (Vol.3-5).
 *
 * Rappel scope : deux figurines solo "NARUTOP99" (Naruto Uzumaki, Kakashi Hatake, ~13cm,
 * shandorashop.com) signalées par l'utilisateur le même jour ne sont PAS incluses ici — gamme
 * Bandai différente (pas "World Collectible Figure" sur leur fiche produit, échelle différente),
 * hors périmètre WCF de cette app.
 */
private data class NaWcfEntry(val series: String, val year: Int?, val character: String, val heightCm: Double? = 7.0)

private val narutoEntries = listOf(
    NaWcfEntry("Naruto NarutoP99 Vol.1", 2023, "Naruto Uzumaki", heightCm = 6.0),
    NaWcfEntry("Naruto NarutoP99 Vol.1", 2023, "Shikamaru Nara", heightCm = 6.0),
    NaWcfEntry("Naruto NarutoP99 Vol.1", 2023, "Gaara", heightCm = 6.0),
    NaWcfEntry("Naruto NarutoP99 Vol.1", 2023, "Jiraiya", heightCm = 6.0),
    NaWcfEntry("Naruto NarutoP99 Vol.1", 2023, "Hashirama Senju", heightCm = 6.0),
    NaWcfEntry("Naruto NarutoP99 Vol.2", 2023, "Minato Namikaze"),
    NaWcfEntry("Naruto NarutoP99 Vol.2", 2023, "Kakashi Hatake"),
    NaWcfEntry("Naruto NarutoP99 Vol.2", 2023, "Sakumo Hatake"),
    NaWcfEntry("Naruto NarutoP99 Vol.2", 2023, "Obito Uchiha"),
    NaWcfEntry("Naruto NarutoP99 Vol.2", 2023, "Madara Uchiha"),
    NaWcfEntry("Naruto NarutoP99 Vol.3", 2024, "Itachi Uchiha"),
    NaWcfEntry("Naruto NarutoP99 Vol.3", 2024, "Shisui Uchiha"),
    NaWcfEntry("Naruto NarutoP99 Vol.3", 2024, "Neji Hyuga"),
    NaWcfEntry("Naruto NarutoP99 Vol.3", 2024, "Hinata Hyuga"),
    NaWcfEntry("Naruto NarutoP99 Vol.3", 2024, "Minato Namikaze (Gold ver.)"),
    NaWcfEntry("Naruto NarutoP99 Vol.4", 2024, "Sakura Haruno"),
    NaWcfEntry("Naruto NarutoP99 Vol.4", 2024, "Rock Lee"),
    NaWcfEntry("Naruto NarutoP99 Vol.4", 2024, "Might Guy"),
    NaWcfEntry("Naruto NarutoP99 Vol.4", 2024, "Sasori"),
    NaWcfEntry("Naruto NarutoP99 Vol.4", 2024, "Itachi Uchiha (Gold ver.)"),
    NaWcfEntry("Naruto NarutoP99 Vol.5", 2024, "Sasuke Uchiha", heightCm = 6.0),
    NaWcfEntry("Naruto NarutoP99 Vol.5", 2024, "Sakura Haruno (Gold ver.)", heightCm = 6.0),
    NaWcfEntry("Naruto NarutoP99 Vol.5", 2024, "Tobirama Senju", heightCm = 6.0),
    NaWcfEntry("Naruto NarutoP99 Vol.5", 2024, "Deidara", heightCm = 6.0),
    NaWcfEntry("Naruto NarutoP99 Vol.5", 2024, "Kurama (Renard à neuf queues)", heightCm = 6.0)
)

val figurePresetsNaruto: List<FigurePreset> = narutoEntries.map { entry ->
    FigurePreset(licence = Licence.NARUTO, character = entry.character, name = entry.character, series = entry.series, year = entry.year, heightCm = entry.heightCm)
}
