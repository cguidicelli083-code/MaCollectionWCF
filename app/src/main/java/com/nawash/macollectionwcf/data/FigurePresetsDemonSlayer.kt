package com.nawash.macollectionwcf.data

/**
 * Catalogue vérifié des figurines WCF Demon Slayer (Kimetsu no Yaiba, Banpresto).
 *
 * "Demon Slayer Vol.1" (5 figurines) : fiche produit en.namektoys.com (revendeur spécialisé) +
 * confirmé par une annonce eBay intitulée explicitement "Complete 5 Figure Set" (Tanjiro Kamado,
 * Sakonji Urokodaki, Sabito, Makomo, Hand Demon — personnages de l'arc "Sélection finale").
 * **Ce coffret compte réellement 5 figurines, pas 6** : vérifié le 2026-07-19 après un retour
 * utilisateur signalant un manque apparent — ce n'était pas une fiche incomplète, ce line-up
 * existe officiellement à 5. La taille standard des coffrets WCF n'est PAS toujours 6 (voir aussi
 * One Piece à 8, Dragon Ball à 8, et de nombreux coffrets One Piece/Dragon Ball plus récents à
 * 5 ou 7 dans les fichiers dédiés) — chaque effectif est celui confirmé par la fiche produit
 * réelle, jamais une supposition d'un total "standard".
 *
 * "Demon Slayer Special Vol.1" (6 figurines, 2026-07-19) : coffret DISTINCT du précédent (nom
 * commercial complet "You're in the Presence of Oyakata-Sama Vol.1 Special", sorti juin 2021,
 * confirmé via shumistore.com + bigbadtoystore.com) — Tanjiro Kamado, Nezuko Kamado, Inosuke
 * Hashibira, Zenitsu Agatsuma, Shinjuro Rengoku, Muzan Kibutsuji, agenouillés devant Oyakata-sama.
 */
private data class DsWcfBox(val series: String, val year: Int?, val characters: List<String>, val heightCm: Double? = 7.0)

private val demonSlayerSeries = listOf(
    DsWcfBox("Demon Slayer Vol.1", null, listOf("Tanjiro Kamado", "Sakonji Urokodaki", "Sabito", "Makomo", "Hand Demon")),
    DsWcfBox("Demon Slayer Special Vol.1", 2021, listOf("Tanjiro Kamado", "Nezuko Kamado", "Inosuke Hashibira", "Zenitsu Agatsuma", "Shinjuro Rengoku", "Muzan Kibutsuji")),
    // 2026-07-20 — Vol.10 à 13 + Inosuke Hashibira Collection ajoutés sur demande de l'utilisateur.
    // Rosters vérifiés officiellement via bsp-prize.jp (Bandai Spirits) : dates/effectifs/tailles
    // confirmés fiche par fiche. Prix/photos individuels partiellement récupérés (toywiz.com,
    // en.namektoys.com — revendeurs spécialisés vendant certaines figurines à l'unité) : quand
    // aucune fiche produit individuelle fiable n'a été trouvée pour un personnage précis, la photo
    // et le prix restent `null` plutôt que d'être devinés (l'enrichissement auto eBay/ShandoraShop
    // prendra le relais plus tard si une annonce existe).
    DsWcfBox("Demon Slayer Vol.10", 2023, listOf("Tanjiro Kamado", "Nezuko Kamado", "Tengen Uzui", "Daki", "Gyutaro")),
    DsWcfBox("Demon Slayer Vol.11", 2023, listOf("Mitsuri Kanroji", "Kotetsu", "Yoriichi Type Zero", "Tanjiro Kamado", "Hotaru Haganezuka")),
    DsWcfBox("Demon Slayer Vol.12", 2023, listOf("Muichiro Tokito", "Gyokko", "Genya Shinazugawa", "Nezuko Kamado", "Mitsuri Kanroji")),
    DsWcfBox("Demon Slayer Vol.13", 2023, listOf("Tanjiro Kamado", "Hantengu", "Gyokko (Perfect Beautiful Form)", "Muichiro Tokito", "Gyutaro")),
    // Coffret d'un seul personnage sous 5 poses différentes (pas 5 personnages distincts) —
    // confirmé bsp-prize.jp ("色んな表情、ポーズが楽しめる") + désignations lettrées A-E vues chez
    // des revendeurs (baitme.com "Special - D Inosuke Hashibira").
    DsWcfBox("Demon Slayer Inosuke Hashibira Collection", 2023, listOf(
        "Inosuke Hashibira (Ver. A)", "Inosuke Hashibira (Ver. B)", "Inosuke Hashibira (Ver. C)",
        "Inosuke Hashibira (Ver. D)", "Inosuke Hashibira (Ver. E)"
    ))
)

val figurePresetsDemonSlayer: List<FigurePreset> = demonSlayerSeries.flatMap { box ->
    box.characters.map { char ->
        FigurePreset(licence = Licence.DEMON_SLAYER, character = char, name = char, series = box.series, year = box.year, heightCm = box.heightCm)
    }
}
