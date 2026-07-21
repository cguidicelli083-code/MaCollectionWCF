package com.nawash.macollectionwcf.data

/**
 * Coffrets WCF officiellement annoncés mais pas encore commercialisés au moment de la mise à jour
 * du catalogue. Sert à afficher un badge « À paraître » sur les fiches Encyclo (voir
 * [com.nawash.macollectionwcf.ui.FigureEncyclopediaScreen]) : pour ces coffrets, l'absence de
 * photo individuelle est normale (les figurines n'existent pas encore physiquement, seuls des
 * visuels promo/préviews officiels circulent).
 *
 * Dérivé à l'affichage à partir du nom exact de la série (aucune migration Room nécessaire) —
 * clé = [FigureCatalogEntry.series]/[FigurePreset.series] à l'identique, valeur = mois de sortie
 * prévu. À MAINTENIR À LA MAIN : retirer une entrée dès que le coffret sort, en ajouter une quand
 * un nouveau coffret futur est intégré au catalogue.
 *
 * État arrêté au 2026-07-21 (dates de sortie officielles, One Piece Wiki / bsp-prize.jp).
 */
val upcomingSeries: Map<String, String> = mapOf(
    "One Piece Red Hair Pirates VS Bartolomeo" to "juillet 2026",
    "One Piece Post-Egghead Banquet Vol.1" to "août 2026",
    "One Piece Post-Egghead Banquet Vol.2" to "septembre 2026",
    "One Piece Post-Egghead Banquet Vol.3" to "octobre 2026",
    "One Piece Elbaph Arc Vol.1" to "novembre 2026",
    "One Piece Elbaph Arc Vol.2" to "décembre 2026"
)

/** Libellé « À paraître · <mois> » à afficher pour un coffret non encore sorti, sinon `null`. */
fun upcomingReleaseLabel(series: String?): String? {
    val month = series?.let { upcomingSeries[it] } ?: return null
    return "À paraître · $month"
}
