package com.nawash.macollectionwcf.data

/**
 * Catalogue vérifié des figurines WCF Marvel (Banpresto — gamme japonaise, existence confirmée
 * via plusieurs annonces/articles réels avant d'écrire quoi que ce soit en dur, notamment
 * marveltoynews.com/banpresto-iron-man-world-collectible-figures-photos-preview, consulté le
 * 2026-07-19). Première passe (Iron Man Vol.1 et Vol.2) — jamais de fiche fabriquée. D'autres
 * lignes Marvel WCF existent (ex. Spider-Man) mais n'ont pas pu être vérifiées fiche par fiche
 * dans cette passe, donc pas encore codées.
 *
 * 2026-07-19 — dates de sortie confirmées via figuya.com (fiches produit individuelles Vol.1/
 * Vol.2, "Release date: Mid December 2014" sur les deux) — les deux volumes datent donc de 2014.
 */
private data class MvWcfBox(val series: String, val year: Int?, val characters: List<String>, val heightCm: Double? = 7.0)

private val marvelSeries = listOf(
    MvWcfBox("Marvel Iron Man Vol.1", 2014, listOf("Iron Man Mark I", "Iron Man Mark II", "Iron Man Mark III", "Red Snapper Iron Man", "Starboost Iron Man", "Iron Man Mark XLII")),
    MvWcfBox("Marvel Iron Man Vol.2", 2014, listOf("Iron Monger", "Iron Patriot", "War Machine", "Heartbreaker Iron Man", "Iron Man Mark IV", "Iron Man Mark VI"))
)

val figurePresetsMarvel: List<FigurePreset> = marvelSeries.flatMap { box ->
    box.characters.map { char ->
        FigurePreset(licence = Licence.MARVEL, character = char, name = char, series = box.series, year = box.year, heightCm = box.heightCm)
    }
}
