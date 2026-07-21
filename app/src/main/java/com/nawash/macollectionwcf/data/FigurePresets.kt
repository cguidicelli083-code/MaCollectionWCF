package com.nawash.macollectionwcf.data

/** Fiche de catalogue « Encyclo » (intégrée ou perso, voir [CustomFigurePreset.toFigurePreset]). */
data class FigurePreset(
    val licence: Licence,
    val character: String,
    val name: String,
    val series: String? = null,
    val year: Int? = null,
    /** Taille standard des figurines WCF (~7cm), sauf gamme "Mega" (plus grande) — voir doc par licence. */
    val heightCm: Double? = null,
    val description: String = "",
    /** Référence interne séquentielle (ex. "OP-001"), non nul seulement pour le catalogue intégré. */
    val numero: String? = null,
    /** Photo mise en cache localement après enrichissement (voir [FigureCatalogEntry]), si trouvée. */
    val imagePath: String? = null,
    /** Id de la ligne `figure_catalog`, non nul seulement pour le catalogue intégré (sert à déclencher l'enrichissement). */
    val catalogId: Long? = null,
    /** Côte indicative (eBay ou prix de référence revendeur), si connue — voir [FigureCatalogEntry.priceCents]. */
    val priceCents: Int? = null,
    /** true si l'enrichissement photo/côte a déjà été tenté (toujours vrai pour une fiche perso, qui n'en a pas besoin). */
    val photoChecked: Boolean = true
)

/**
 * Catalogue intégré des figurines WCF officielles, par licence (Phase 5, chantier de données
 * mené en plusieurs passes). Chaque fiche est vérifiée avant d'être codée en dur (jamais
 * fabriquée/devinée) — voir les fichiers `FigurePresets<Licence>.kt` pour les sources utilisées.
 * Peuplé licence par licence ; les licences pas encore traitées restent absentes (l'Encyclopédie
 * reste pleinement utilisable via les fiches perso [CustomFigurePreset] en attendant).
 */
val figurePresets: List<FigurePreset> = figurePresetsOnePiece + figurePresetsOnePieceExtra + figurePresetsDragonBall +
    figurePresetsDemonSlayer + figurePresetsMyHeroAcademia + figurePresetsMarvel + figurePresetsNaruto +
    figurePresetsHunterHunter + figurePresetsChainsawMan + figurePresetsKenShimura
