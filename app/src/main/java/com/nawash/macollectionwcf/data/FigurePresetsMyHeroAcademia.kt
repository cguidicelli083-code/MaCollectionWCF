package com.nawash.macollectionwcf.data

/**
 * Catalogue vérifié des figurines WCF My Hero Academia (Banpresto). Vol.1 : jeu complet de
 * 6 figures confirmé via deux sources indépendantes (toysack.toys "Vol. 1 Set of 6" — liste
 * exacte "Midoriya, Iida, All Might, Minoru, Ochaco & Tsuyu" ; bigbadtoystore.com "Set of 6
 * Figures" — même liste des 6 personnages). Correction du 2026-07-19 : les 2 personnages
 * manquants (Tsuyu Asui, Minoru Mineta) avaient été omis par erreur, ce qui décalait la
 * numérotation officielle 01-06/07-12. Vol.2 : fiche bigbadtoystore.com "My Hero Academia
 * World Collectable Figure Vol.2 Set of 6 Figures" (liste "Box Contents" propre à cette page,
 * cohérente — 6 personnages distincts sans doublon — contrairement à des blurbs génériques
 * repérés ailleurs lors de recherches sur d'autres licences et écartés pour cette raison).
 * Jamais de fiche fabriquée.
 *
 * 2026-07-19 — dates de sortie confirmées : Vol.1 via figuya.com ("Release date: Mid October
 * 2019") et toysack.toys (année 2019) ; Vol.2 via hlj.com/Hobby Link Japan
 * ("Release Date: 2019/09/03", date précise).
 */
private data class MhWcfBox(val series: String, val year: Int?, val characters: List<String>, val heightCm: Double? = 7.0)

private val myHeroAcademiaSeries = listOf(
    MhWcfBox("My Hero Academia Vol.1", 2019, listOf("Izuku Midoriya", "All Might", "Ochaco Uraraka", "Tenya Iida", "Tsuyu Asui", "Minoru Mineta")),
    MhWcfBox("My Hero Academia Vol.2", 2019, listOf("Shoto Todoroki", "Momo Yaoyorozu", "Shota Aizawa", "Fumikage Tokoyami", "Yuga Aoyama", "Denki Kaminari"))
)

val figurePresetsMyHeroAcademia: List<FigurePreset> = myHeroAcademiaSeries.flatMap { box ->
    box.characters.map { char ->
        FigurePreset(licence = Licence.MY_HERO_ACADEMIA, character = char, name = char, series = box.series, year = box.year, heightCm = box.heightCm)
    }
}
