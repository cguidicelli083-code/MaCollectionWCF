package com.nawash.macollectionwcf.data

/**
 * Catalogue vérifié des figurines WCF Chainsaw Man (Banpresto, gamme du film "Chainsaw Man The
 * Movie: Reze Arc"). Ajouté après la mise en place de l'onglet Actu. Rosters confirmés par
 * plusieurs sources indépendantes avant d'être codés en dur — voir sources ci-dessous. Recherche
 * menée le 2026-07-21.
 *
 * - Reze Arc Vol.1 (全5種, ~7cm) : roster confirmé via ShandoraShop + le compte X officiel du
 *   magasin GiGO Akiba (@GiGO_Akiba_5) + figisland.net/prize-house.com.
 * - Reze Arc Vol.2 (全5種, ~7cm) : roster confirmé via un article dédié JP (Boom App Games) +
 *   une fiche revendeur (kaitori-world.jp, nom seul vu en aperçu de recherche, page non
 *   récupérée — robots.txt bloquant). Confiance légèrement moindre que Vol.1 mais deux sources
 *   indépendantes s'accordent.
 *
 * Prix : aucune côte individuelle trouvée (produits de machines à griffes japonaises, jamais
 * vendus à l'unité sur les sites vérifiés) — volontairement laissés vides.
 */
private data class CmWcfBox(val series: String, val year: Int?, val characters: List<String>)

private val chainsawManSeries = listOf(
    CmWcfBox("Chainsaw Man Reze Arc Vol.1", null, listOf("Denji", "Reze", "Makima", "Aki Hayakawa", "Angel Devil")),
    CmWcfBox("Chainsaw Man Reze Arc Vol.2", null, listOf("Chainsaw Man", "Bomb", "Beam", "Violence Fiend", "Kobeni"))
)

val figurePresetsChainsawMan: List<FigurePreset> = chainsawManSeries.flatMap { box ->
    box.characters.map { char ->
        FigurePreset(licence = Licence.CHAINSAW_MAN, character = char, name = char, series = box.series, year = box.year, heightCm = 7.0)
    }
}
