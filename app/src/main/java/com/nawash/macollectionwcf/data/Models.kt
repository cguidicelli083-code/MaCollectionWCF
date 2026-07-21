package com.nawash.macollectionwcf.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/** Licence de la figurine (dimension de filtre/compteur, joue le rôle de l'ancien ItemType). */
enum class Licence(val label: String) {
    ONE_PIECE("One Piece"),
    BLEACH("Bleach"),
    DRAGON_BALL("Dragon Ball Z/Super"),
    NARUTO("Naruto"),
    DEMON_SLAYER("Demon Slayer"),
    JUJUTSU_KAISEN("Jujutsu Kaisen"),
    MY_HERO_ACADEMIA("My Hero Academia"),
    DISNEY("Disney"),
    MARVEL("Marvel"),
    AUTRE("Autre")
}

/**
 * Licences proposées dans les menus déroulants (filtre, formulaire d'ajout) : celles qui ont un
 * vrai catalogue Encyclo peuplé. `BLEACH`/`JUJUTSU_KAISEN`/`DISNEY`/`AUTRE` restent des valeurs
 * d'enum valides (utilisées en interne par la reconnaissance IA/l'import de tableur comme repli
 * générique, ou pour d'anciennes fiches déjà enregistrées) mais ne sont plus proposées au choix
 * tant qu'aucun catalogue n'existe pour elles — à retirer de cette liste dès qu'une licence est
 * peuplée (voir `data/FigurePresets<Licence>.kt`).
 */
val selectableLicences: List<Licence> = Licence.entries.filterNot {
    it == Licence.BLEACH || it == Licence.JUJUTSU_KAISEN || it == Licence.DISNEY || it == Licence.AUTRE
}

/** État physique de la figurine (influence la cote). */
enum class Condition(val label: String) {
    HS("HS"),
    MAUVAIS("Mauvais"),
    BON("Bon"),
    TRES_BON("Très bon"),
    MINT("Mint"),
    NEUF("Neuf")
}

/** Une figurine possédée dans la collection (ou un souhait, voir [isWishlist]). */
@Entity(tableName = "collection_items")
data class CollectionItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val licence: Licence,
    /** Gamme/vague WCF officielle (ex. "WCF History of Rufy vol.6"), si connue. */
    val series: String? = null,
    val character: String,
    /** Nom commercial complet (variante comprise). */
    val name: String,
    /** Référence interne du catalogue (ex. "OP-427"), si connue — voir [FigureCatalogEntry.numero]. */
    val numero: String? = null,
    val manufacturer: String = "Banpresto",
    val condition: Condition,
    val hasBox: Boolean,
    /** Pièces interchangeables/armes/effets fournis présents (facteur de cote réel sur les WCF). */
    val hasAccessories: Boolean,
    val heightCm: Double? = null,
    val releaseYear: Int? = null,
    val priceCents: Int? = null,
    /**
     * true si [priceCents] a été saisi à la main par l'utilisateur : dans ce cas,
     * l'actualisation automatique des cotes (eBay) ne doit JAMAIS l'écraser.
     */
    val priceIsManual: Boolean = false,
    val barcode: String? = null,
    val description: String? = null,
    val imageUri: String? = null,
    val sourceUrl: String? = null,
    /** Message du dernier essai de cote en ligne (ex. "Aucune annonce trouvée"), si pas de prix. */
    val info: String? = null,
    /** true si [priceCents] vient d'une estimation par IA (moins fiable qu'une vraie annonce). */
    val priceIsAiEstimate: Boolean = false,
    /**
     * true si cette figurine est un souhait (onglet « Souhaits ») plutôt qu'une figurine
     * réellement possédée : même fiche/formulaire que la collection, mais exclu de la valeur
     * totale et de la liste de l'onglet Collection.
     */
    val isWishlist: Boolean = false,
    val createdAt: Long = 0
)

/** Un point d'historique de prix (cote relevée à une date). Scaffoldé dès la Phase 1, utilisé à partir de la Phase 4. */
@Entity(tableName = "price_history")
data class PriceHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val priceCents: Int,
    val timestamp: Long
)

/** Une photo supplémentaire associée à une figurine de la collection (galerie). */
@Entity(tableName = "item_photos")
data class ItemPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val uri: String,
    val position: Int = 0,
    val createdAt: Long = 0
)

/**
 * Une fiche de catalogue ajoutée manuellement par l'utilisateur (figurine non couverte par le
 * catalogue intégré) : apparaît dans l'Encyclopédie au même titre que les fiches intégrées
 * (voir Phase 5), et peut servir de base de pré-remplissage comme elles.
 */
@Entity(tableName = "custom_figure_presets")
data class CustomFigurePreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val licence: Licence,
    val character: String,
    val name: String,
    val series: String? = null,
    val description: String = "",
    val photoUri: String? = null,
    val createdAt: Long = 0
)

/** Vue « fiche de catalogue » d'une fiche perso (même forme que les fiches du catalogue intégré). */
fun CustomFigurePreset.toFigurePreset(): FigurePreset = FigurePreset(
    licence = licence,
    character = character,
    name = name,
    series = series,
    year = null,
    description = description
)

/**
 * Une fiche du catalogue WCF intégré (Encyclo), stockée en base SQLite (Room) — structurée
 * comme MyFigureCollection (licence / volume-vague / numéro / date de sortie), à la demande de
 * l'utilisateur, mais peuplée hors-ligne (voir [FigureCatalogSeeder]) plutôt que scrapée en
 * direct sur MFC : pas d'appel réseau vers MFC (accès API incertain, jamais vérifié par
 * l'utilisateur lui-même), les données restent celles déjà vérifiées manuellement dans
 * `FigurePresetsOnePiece.kt`/`...Extra.kt`. [numero] est une référence interne séquentielle
 * générée par l'app (ex. "OP-001"), PAS un numéro officiel Banpresto — ceux-ci n'existent pas
 * de façon uniforme sur les boîtes WCF. [imagePath]/[priceCents] restent `null` jusqu'au premier
 * passage de [photoChecked] (enrichissement paresseux via eBay, voir `AppViewModel.enrichCatalogEntry`).
 */
@Entity(tableName = "figure_catalog")
data class FigureCatalogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val licence: Licence,
    /** Volume/vague officielle (ex. "One Piece Vol.5", "Wano Country Vol.3"). */
    val series: String,
    /** Référence interne séquentielle (ex. "OP-001"), sert de repère de tri/regroupement. */
    val numero: String,
    val character: String,
    /** Granularité actuelle : année seule (voir les fichiers `FigurePresetsOnePiece*.kt`). */
    val releaseYear: Int? = null,
    /** Taille standard des figurines WCF (~7cm), sauf gamme "Mega" (plus grande, non standardisée). */
    val heightCm: Double? = null,
    val imagePath: String? = null,
    val priceCents: Int? = null,
    /** true dès qu'un essai d'enrichissement (photo/côte via eBay) a eu lieu, même sans résultat. */
    val photoChecked: Boolean = false,
    /**
     * true si [imagePath] vient d'une photo de référence fournie manuellement (voir
     * `CatalogReferencePhotos`, dossier `photos/` associé par code) — dans ce cas, ne JAMAIS
     * l'écraser par l'enrichissement automatique eBay/Tavily, ni par une purge globale.
     */
    val photoLocked: Boolean = false
)

/** Vue « fiche de catalogue » d'une entrée du catalogue intégré. */
fun FigureCatalogEntry.toFigurePreset(): FigurePreset = FigurePreset(
    licence = licence,
    character = character,
    name = character,
    series = series,
    year = releaseYear,
    heightCm = heightCm,
    numero = numero,
    imagePath = imagePath,
    catalogId = id,
    photoChecked = photoChecked,
    priceCents = priceCents
)

/**
 * Photo personnalisée choisie par l'utilisateur pour une fiche du catalogue intégré, qui prime
 * sur l'image par défaut quand elle existe. Clé = nom exact du preset.
 */
@Entity(tableName = "preset_photo_overrides")
data class PresetPhotoOverride(
    @PrimaryKey val presetName: String,
    val photoUri: String
)

/**
 * Annonce de nouveauté WCF (onglet Actu), alimentée par le flux JSON publié par le scraper
 * (`scripts/scrape_wcf_news.py` + GitHub Actions, voir `NewsRepository`). `id` reprend
 * l'identifiant produit bsp-prize.jp — sert de clé pour l'upsert et évite les doublons.
 */
@Entity(tableName = "wcf_news")
data class WcfNewsEntry(
    @PrimaryKey val id: String,
    val series: String,
    /** Personnages/variantes listés par le site officiel, séparés par "|" (vide si non détaillé). */
    val characters: String,
    val releaseDateRaw: String,
    val priceRaw: String,
    val imageUrl: String,
    val itemUrl: String,
    val scrapedAt: String
)

/** Convertit les énumérations pour le stockage en base. */
class Converters {
    @TypeConverter fun licenceToString(v: Licence): String = v.name
    @TypeConverter fun stringToLicence(v: String): Licence = Licence.valueOf(v)
    @TypeConverter fun conditionToString(v: Condition): String = v.name
    @TypeConverter fun stringToCondition(v: String): Condition = Condition.valueOf(v)
}
