package com.nawash.macollectionwcf.data

/**
 * Catalogue vérifié des figurines WCF HUNTER×HUNTER (Banpresto). Ajouté après la mise en place
 * de l'onglet Actu (qui a révélé cette licence absente de l'Encyclo). Chaque roster confirmé par
 * au moins 2 sources indépendantes avant d'être codé en dur (jamais fabriqué) — voir sources
 * ci-dessous. Recherche menée le 2026-07-21.
 *
 * - Hunter Exam Vol.1 (全5種, ~7cm) : roster confirmé via cloud-catcher.jp, chatchak.ocnk.net et
 *   la fiche boîte toywiz.com (box de 12 = 2 exemplaires de chaque des 6... non, 5 types).
 * - Phantom Troupe (幻影旅団, 全5種, ~9cm) : seulement 5 des 13 membres du manga sont dans CE
 *   coffret (confirmé via ShandoraShop + prize-house.com + chatchak.ocnk.net, 2 sources
 *   indépendantes s'accordent sur Chrollo/Uvogin/Nobunaga/Pakunoda/Hisoka). Hisoka figure dans ce
 *   coffret bien qu'il ne soit pas un membre officiel de la troupe dans le manga — confirmé
 *   intentionnel côté produit Bandai, pas une erreur de tri.
 *
 * Prix : aucune côte individuelle trouvée nulle part (produits de machines à griffes japonaises,
 * jamais vendus à l'unité sur les sites vérifiés) — volontairement laissés vides plutôt que
 * d'utiliser un prix de coffret complet comme prix par personnage.
 */
private data class HhWcfBox(val series: String, val year: Int?, val heightCm: Double?, val characters: List<String>)

private val hunterHunterSeries = listOf(
    HhWcfBox("Hunter Exam Vol.1", null, 7.0, listOf("Gon", "Killua", "Hisoka", "Satotz", "Tonpa")),
    HhWcfBox("Phantom Troupe", null, 9.0, listOf("Chrollo", "Uvogin", "Nobunaga", "Pakunoda", "Hisoka"))
)

val figurePresetsHunterHunter: List<FigurePreset> = hunterHunterSeries.flatMap { box ->
    box.characters.map { char ->
        FigurePreset(licence = Licence.HUNTER_X_HUNTER, character = char, name = char, series = box.series, year = box.year, heightCm = box.heightCm)
    }
}
