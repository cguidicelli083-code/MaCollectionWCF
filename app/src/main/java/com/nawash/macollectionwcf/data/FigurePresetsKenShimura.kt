package com.nawash.macollectionwcf.data

/**
 * Catalogue vérifié des figurines WCF Ken Shimura (Banpresto — gamme hommage à l'humoriste
 * japonais Ken Shimura, décédé en 2020 ; les "personnages" sont ses personas comiques récurrents
 * réels/documentés, pas des personnages de fiction inventés). Ajouté après la mise en place de
 * l'onglet Actu. Recherche menée le 2026-07-21.
 *
 * - Vol.1 (全4種, ~7cm) : roster confirmé via le compte X officiel Bandai Spirits (@BANPRE_PZ) —
 *   バカ殿様/Baka Tono-sama, 変なおじさん/Henna Ojisan, ひとみばあさん/Hitomi Baa-san,
 *   いいよなおじさん/Iiyona Ojisan.
 * - Vol.2 (全4種, ~6-8cm) : sortie prévue le 2026-07-28 (postérieure à cette recherche) — roster
 *   extrait du texte descriptif officiel de la fiche bsp-prize.jp (item 2840282), qui nomme
 *   explicitement les 4 personas de ce coffret. Aucune fiche revendeur/liste A-B-C-D tierce
 *   n'existe encore pour croiser la répartition exacte (produit pas encore commercialisé au
 *   moment de la recherche) — a re-vérifier une fois le coffret disponible.
 *
 * Prix : aucune côte trouvée (produits de machines à griffes japonaises, jamais vendus à l'unité
 * sur les sites vérifiés ; Vol.2 pas encore commercialisé) — volontairement laissés vides.
 */
private data class KsWcfBox(val series: String, val year: Int?, val characters: List<String>)

private val kenShimuraSeries = listOf(
    KsWcfBox("Ken Shimura Vol.1", null, listOf("Baka Tono-sama", "Henna Ojisan", "Hitomi Baa-san", "Iiyona Ojisan")),
    KsWcfBox("Ken Shimura Vol.2", 2026, listOf("Shimura Encho", "Hakuchou (Swan)", "Baka Tono-sama (childhood ver.)", "Henna Ojisan"))
)

val figurePresetsKenShimura: List<FigurePreset> = kenShimuraSeries.flatMap { box ->
    box.characters.map { char ->
        FigurePreset(licence = Licence.KEN_SHIMURA, character = char, name = char, series = box.series, year = box.year, heightCm = 7.0)
    }
}
