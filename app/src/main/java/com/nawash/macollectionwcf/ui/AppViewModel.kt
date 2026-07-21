package com.nawash.macollectionwcf.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nawash.macollectionwcf.data.AppDatabase
import com.nawash.macollectionwcf.data.AppPrefs
import com.nawash.macollectionwcf.data.BackupManager
import com.nawash.macollectionwcf.data.CatalogReferencePhotos
import com.nawash.macollectionwcf.data.CatalogReferencePrices
import com.nawash.macollectionwcf.data.CollectionItem
import com.nawash.macollectionwcf.data.Condition
import com.nawash.macollectionwcf.data.CurrencyRates
import com.nawash.macollectionwcf.data.CustomFigurePreset
import com.nawash.macollectionwcf.data.EbayPrices
import com.nawash.macollectionwcf.data.FigureCatalogEntry
import com.nawash.macollectionwcf.data.FigureCatalogSeeder
import com.nawash.macollectionwcf.data.GeminiVision
import com.nawash.macollectionwcf.data.ItemPhoto
import com.nawash.macollectionwcf.data.Licence
import com.nawash.macollectionwcf.data.MediaUtils
import com.nawash.macollectionwcf.data.NewsRepository
import com.nawash.macollectionwcf.data.WcfNewsEntry
import com.nawash.macollectionwcf.data.PresetPhotoOverride
import com.nawash.macollectionwcf.data.PriceHistory
import com.nawash.macollectionwcf.data.ShandoraShop
import com.nawash.macollectionwcf.data.TavilyPriceEstimate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Critères de tri proposés dans les onglets Collection/Souhaits. */
enum class SortOption(val label: String) {
    NAME("Nom"),
    PRICE_DESC("Prix (élevé → bas)"),
    PRICE_ASC("Prix (bas → élevé)"),
    RELEASE("Date de sortie (ancien → récent)"),
    RELEASE_DESC("Date de sortie (récent → ancien)")
}

/** Taux de complétion d'un coffret/vague WCF (ex. "MHA Vol.1 : 4/6 - 66%") — voir [AppViewModel.seriesCompletion]. */
data class SeriesCompletion(val licence: Licence, val series: String, val owned: Int, val total: Int) {
    val percent: Int get() = if (total == 0) 0 else (owned * 100 / total)
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val collectionDao = db.collectionDao()
    private val historyDao = db.priceHistoryDao()
    private val photoDao = db.itemPhotoDao()
    private val customPresetDao = db.customFigurePresetDao()
    private val photoOverrideDao = db.presetPhotoOverrideDao()
    private val catalogDao = db.figureCatalogDao()
    private val newsDao = db.wcfNewsDao()

    init {
        viewModelScope.launch {
            // Best-effort, silencieux : sans réseau ou si le flux n'est pas encore configuré
            // (voir NewsRepository.FEED_URL), on garde simplement les dernières actus en base.
            NewsRepository.fetchLatest()?.let { newsDao.upsertAll(it) }
        }
        viewModelScope.launch {
            FigureCatalogSeeder.seedIfNeeded(catalogDao)
            // Rattrape les dates/tailles nouvellement trouvées sur des licences déjà peuplées
            // (le seeding ci-dessus ne s'exécute qu'une fois par licence) — réappliqué à chaque
            // lancement, sans risque : n'ajoute jamais que de l'info manquante, jamais d'écrasement.
            FigureCatalogSeeder.backfillMissingData(catalogDao)
            // Réaligne la numérotation (numero) et complète les figurines manquantes d'un coffret
            // déjà peuplé (ex. My Hero Academia Vol.1 : 2 personnages oubliés décalaient la
            // numérotation du Vol.2) — réappliqué à chaque lancement, sans risque pour les photos/
            // côtes déjà enregistrées (correspondance par série+personnage, jamais d'écrasement).
            FigureCatalogSeeder.resyncNumbering(catalogDao)
            // Purge ponctuelle : les photos mises en cache par l'ancienne requête (personnage
            // seul, sans vague/volume WCF) pouvaient tomber sur la mauvaise version d'un même
            // personnage (ex. "Issho" ressort dans plusieurs coffrets) — on les force à repasser
            // par le nouvel enrichissement (personnage + série), une seule fois.
            if (!AppPrefs.catalogPhotoRefinementV1Done(app)) {
                catalogDao.entriesWithCachedPhoto().forEach { it.imagePath?.let(MediaUtils::deleteFile) }
                catalogDao.resetAllEnrichment()
                AppPrefs.markCatalogPhotoRefinementV1Done(app)
            }
            // Réappliqué à chaque lancement (pas gardé par un flag "déjà fait") : si de nouvelles
            // photos de référence sont ajoutées à `assets/catalog_photos/` lors d'un futur build,
            // elles sont prises en compte sans avoir besoin d'un nouveau bump de version de base.
            val result = CatalogReferencePhotos.applyAll(app, catalogDao)
            if (result.conflicts.isNotEmpty()) Log.w("CatalogReferencePhotos", "Conflits (code utilisé par plusieurs fichiers) : ${result.conflicts}")
            Log.i("CatalogReferencePhotos", "Appliquées: ${result.applied}, non reconnues: ${result.unmatched}")
            // Prix de référence ShandoraShop (voir CatalogReferencePrices) : n'écrase jamais une côte déjà connue.
            val pricesApplied = CatalogReferencePrices.applyAll(app, catalogDao)
            Log.i("CatalogReferencePrices", "Prix appliqués: $pricesApplied")
        }
    }

    /** Catalogue intégré (Phase 5), stocké en base — voir [FigureCatalogEntry]. */
    val catalogEntries: StateFlow<List<FigureCatalogEntry>> =
        catalogDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Nouveautés WCF (onglet Actu) — voir [NewsRepository]. */
    val wcfNews: StateFlow<List<WcfNewsEntry>> =
        newsDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Récupère paresseusement une photo (+ une côte indicative) pour une fiche du catalogue
     * intégré qui n'en a pas encore, via eBay (même mécanisme déjà accepté pour la photo auto du
     * scan par lot, voir [resolvePriceAndFinish]) — mise en cache définitivement en local
     * ([FigureCatalogDao.markEnriched] pose aussi `photoChecked = true`, même sans résultat, pour
     * ne jamais re-solliciter eBay inutilement pour cette fiche). [series] (vague/volume WCF,
     * ex. "One Piece Dressrosa Vol.3") est indispensable à la précision : un même personnage
     * ressort souvent dans plusieurs coffrets différents, le personnage seul ne suffit pas à
     * retrouver la BONNE version.
     */
    fun enrichCatalogEntry(id: Long, licence: Licence, character: String, series: String?) = viewModelScope.launch {
        fetchAndCacheCatalogPhoto(id, licence, character, series)
    }

    /**
     * Impose manuellement la photo d'une fiche du catalogue intégré (bouton "Changer la photo"
     * de l'Encyclo) — pour corriger une fiche sans photo (source vide) ou avec une mauvaise
     * photo (mauvaise version d'un personnage récurrent). Verrouillée comme les photos de
     * référence (`CatalogReferencePhotos`) : plus jamais écrasée par l'enrichissement auto.
     */
    fun setCatalogPhoto(id: Long, uri: String) = viewModelScope.launch {
        catalogDao.setLockedPhoto(id, uri)
    }

    /**
     * Même enrichissement que [enrichCatalogEntry], mais `suspend` : utilisé par le formulaire
     * d'ajout (recherche dans le catalogue) pour appliquer la photo tout de suite au formulaire
     * en cours si elle vient d'être trouvée, pas seulement la mettre en cache pour la prochaine fois.
     */
    suspend fun enrichCatalogEntryNow(id: Long, licence: Licence, character: String, series: String?): String? =
        fetchAndCacheCatalogPhoto(id, licence, character, series)

    /**
     * Photo (+ côte si eBay) d'une fiche catalogue : eBay d'abord (annonce réelle, avec prix),
     * puis ShandoraShop (revendeur WCF spécialisé, photos officielles produit — voir
     * [ShandoraShop.findImage], ajouté le 2026-07-19 sur suggestion de l'utilisateur), puis
     * recherche web Tavily en dernier recours (voir [TavilyPriceEstimate.findImage]) — pour ne pas
     * laisser une fiche sans photo juste parce qu'il n'y a aucune annonce en cours pour un
     * personnage rare. La requête qualifie toujours personnage + vague/volume ([series]), jamais
     * le personnage seul, pour ne pas récupérer la photo d'une version différente du même
     * personnage (ex. "Issho" ressort dans plusieurs coffrets WCF ; sans la vague, on ne peut pas
     * distinguer laquelle).
     */
    private suspend fun fetchAndCacheCatalogPhoto(id: Long, licence: Licence, character: String, series: String?): String? {
        val queryName = if (series.isNullOrBlank()) character else "$character $series"
        val ebayResult = if (EbayPrices.isConfigured()) {
            runCatching { EbayPrices.lookup(null, licence, character, queryName, Condition.BON, hasBox = true, hasAccessories = true) }.getOrNull()
        } else null
        val remoteUrl = ebayResult?.imageUrl
            ?: runCatching { ShandoraShop.findImage(character, series) }.getOrNull()
            ?: runCatching { TavilyPriceEstimate.findImage(licence, character, series) }.getOrNull()
        val imagePath = remoteUrl?.let { MediaUtils.downloadToInternal(getApplication(), it) }
        catalogDao.markEnriched(id, imagePath, ebayResult?.priceCents)
        return imagePath
    }

    /** Fiches de catalogue ajoutées manuellement par l'utilisateur (figurines hors catalogue intégré). */
    val customPresets: StateFlow<List<CustomFigurePreset>> =
        customPresetDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Photos personnalisées (nom du preset -> URI locale), qui priment sur l'image par défaut. */
    val photoOverrides: StateFlow<Map<String, String>> =
        photoOverrideDao.observeAll()
            .map { list -> list.associate { it.presetName to it.photoUri } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setPresetPhoto(presetName: String, photoUri: String) = viewModelScope.launch {
        photoOverrideDao.upsert(PresetPhotoOverride(presetName, photoUri))
    }

    fun resetPresetPhoto(presetName: String) = viewModelScope.launch {
        photoOverrideDao.delete(presetName)
    }

    fun addCustomPreset(preset: CustomFigurePreset) = viewModelScope.launch {
        customPresetDao.insert(preset.copy(createdAt = System.currentTimeMillis()))
        preset.photoUri?.let { photoOverrideDao.upsert(PresetPhotoOverride(preset.name, it)) }
    }

    /**
     * Modifie une fiche perso existante. Le rendu passe par le nom (clé des [PresetPhotoOverride]),
     * donc en cas de renommage on déplace l'override photo de l'ancien nom vers le nouveau.
     */
    fun updateCustomPreset(old: CustomFigurePreset, updated: CustomFigurePreset) = viewModelScope.launch {
        customPresetDao.update(updated.copy(id = old.id, createdAt = old.createdAt))
        val renamed = !old.name.equals(updated.name, ignoreCase = true)
        val currentPhoto = photoOverrideDao.observeAll().first().firstOrNull { it.presetName == old.name }?.photoUri
        if (renamed) photoOverrideDao.delete(old.name)
        val newPhoto = updated.photoUri ?: currentPhoto
        if (newPhoto != null) photoOverrideDao.upsert(PresetPhotoOverride(updated.name, newPhoto))
        else if (!renamed) photoOverrideDao.delete(updated.name)
        if (currentPhoto != null && currentPhoto != newPhoto) MediaUtils.deleteFile(currentPhoto)
    }

    fun deleteCustomPreset(preset: CustomFigurePreset) = viewModelScope.launch {
        customPresetDao.delete(preset)
        photoOverrideDao.delete(preset.name)
        preset.photoUri?.let { MediaUtils.deleteFile(it) }
    }

    val sortOption = MutableStateFlow(SortOption.NAME)
    val licenceFilter = MutableStateFlow<Licence?>(null)
    /** Vague/volume WCF sélectionnée (dépend de la licence choisie, remise à zéro si elle change). */
    val seriesFilter = MutableStateFlow<String?>(null)

    /** Collection (figurines réellement possédées), filtrée et triée selon les choix de l'utilisateur. */
    val items: StateFlow<List<CollectionItem>> =
        combine(collectionDao.observeAll(), sortOption, licenceFilter, seriesFilter) { list, sort, filter, series ->
            list.filter { !it.isWishlist && (filter == null || it.licence == filter) && (series == null || it.series == series) }
                .sortedWith(sorter(sort))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Souhaits (même fiche que la collection, juste exclue du total). */
    val wishlist: StateFlow<List<CollectionItem>> =
        combine(collectionDao.observeAll(), sortOption, licenceFilter, seriesFilter) { list, sort, filter, series ->
            list.filter { it.isWishlist && (filter == null || it.licence == filter) && (series == null || it.series == series) }
                .sortedWith(sorter(sort))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Vagues/volumes disponibles pour le menu déroulant (Collection ET Souhaits confondus, filtré
     * par licence mais PAS par [seriesFilter] lui-même — sinon la liste des choix se réduirait à
     * un seul élément dès qu'on en sélectionne un).
     */
    val availableSeries: StateFlow<List<String>> =
        combine(collectionDao.observeAll(), licenceFilter) { list, filter ->
            list.filter { filter == null || it.licence == filter }
                .mapNotNull { it.series?.ifBlank { null } }
                .distinct()
                .sorted()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Collection complète (hors souhaits), JAMAIS filtrée par licence : sert aux décomptes de
     * l'onglet Total, qui ne doivent pas dépendre du filtre affiché sur l'onglet Collection.
     */
    val allOwnedItems: StateFlow<List<CollectionItem>> =
        collectionDao.observeAll()
            .map { list -> list.filter { !it.isWishlist } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Valeur totale de la collection (somme des cotes connues), hors souhaits. */
    val totalCents: StateFlow<Int> =
        collectionDao.observeAll()
            .map { list -> list.filter { !it.isWishlist }.sumOf { it.priceCents ?: 0 } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Nombre total de figurines réellement possédées (hors souhaits) — voir onglet Total/Stats. */
    val totalOwnedCount: StateFlow<Int> =
        allOwnedItems.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Taux de complétion d'un coffret/vague WCF du catalogue intégré (ex. "MHA Vol.1 : 4/6 - 66%"),
     * calculé en comparant la collection possédée au nombre de fiches connues dans `figure_catalog`
     * pour la même (licence, série). Le rapprochement se fait sur le libellé exact de [CollectionItem.series]
     * (déjà rempli avec le libellé du catalogue quand la figurine vient d'une recherche/de l'Encyclo —
     * voir `AddCollectionForm`) ; une figurine ajoutée à la main avec un libellé de série différent ou
     * absent n'est simplement pas comptée dans une complétion (pas de faux 100%).
     */
    val seriesCompletion: StateFlow<List<SeriesCompletion>> =
        combine(allOwnedItems, catalogEntries) { owned, catalog ->
            catalog.groupBy { it.licence to it.series }
                .map { (key, entries) ->
                    val (licence, series) = key
                    val ownedCount = owned.count { it.licence == licence && it.series == series }
                    SeriesCompletion(licence, series, ownedCount.coerceAtMost(entries.size), entries.size)
                }
                .sortedWith(compareBy({ it.licence.label }, { it.series }))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun sorter(sort: SortOption): Comparator<CollectionItem> = when (sort) {
        SortOption.NAME -> compareBy { it.name.lowercase() }
        SortOption.PRICE_DESC -> compareByDescending { it.priceCents ?: -1 }
        SortOption.PRICE_ASC -> compareBy { it.priceCents ?: Int.MAX_VALUE }
        SortOption.RELEASE -> compareBy { it.releaseYear ?: Int.MAX_VALUE }
        SortOption.RELEASE_DESC -> compareByDescending { it.releaseYear ?: Int.MIN_VALUE }
    }

    fun setSort(s: SortOption) { sortOption.value = s }
    fun setLicenceFilter(l: Licence?) { licenceFilter.value = l; seriesFilter.value = null }
    fun setSeriesFilter(s: String?) { seriesFilter.value = s }

    /**
     * Crée la figurine si nouvelle (id == 0), sinon met à jour l'existante. Si l'utilisateur a
     * saisi un prix à la main (`priceIsManual`), il est conservé tel quel et ne déclenche AUCUNE
     * recherche en ligne. Sinon, une cote est recherchée sur eBay (+ repli IA), voir [resolvePrice].
     */
    fun saveCollectionItem(item: CollectionItem, newPhotoUris: List<String> = emptyList(), onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        val (id, stored) = insertOrUpdateBareItem(item)
        onSaved(id)
        resolvePriceAndFinish(id, stored, newPhotoUris)
    }

    /**
     * Non nul pendant [saveBatch] (peut prendre un moment pour un gros lot) : nombre de figurines
     * en cours d'ajout, affiché côté UI dans un indicateur de chargement. Null = inactif.
     */
    val batchSaving = MutableStateFlow<Int?>(null)

    /**
     * Ajoute les figurines cochées d'un lot détecté par le scan multiple
     * ([GeminiVision.identifyBatch]). [coverUri] est la photo du lot elle-même (pas recadrée par
     * figurine) : faute de mieux, elle est attachée telle quelle comme photo principale de
     * chaque figurine ajoutée, à remplacer/recadrer ensuite au cas par cas via Modifier.
     */
    fun saveBatch(items: List<GeminiVision.BatchItem>, isWishlist: Boolean, coverUri: String? = null) = viewModelScope.launch {
        batchSaving.value = items.size
        try {
            for (batchItem in items) {
                val item = CollectionItem(
                    licence = batchItem.licence,
                    series = batchItem.series,
                    character = batchItem.character,
                    name = batchItem.character,
                    condition = Condition.BON,
                    hasBox = true,
                    hasAccessories = true,
                    imageUri = coverUri,
                    isWishlist = isWishlist
                )
                val (id, stored) = insertOrUpdateBareItem(item)
                resolvePriceAndFinish(id, stored, fetchNetPhoto = true)
            }
        } finally {
            batchSaving.value = null
        }
    }

    /** Insère (ou met à jour) la figurine SANS résoudre son prix — juste la ligne en base. */
    private suspend fun insertOrUpdateBareItem(item: CollectionItem): Pair<Long, CollectionItem> {
        val id: Long
        val stored: CollectionItem
        if (item.id == 0L) {
            stored = item.copy(createdAt = System.currentTimeMillis())
            id = collectionDao.insert(stored)
        } else {
            stored = item
            collectionDao.update(stored)
            id = stored.id
        }
        return id to stored
    }

    /**
     * Résout le prix (eBay/IA) d'une figurine déjà insérée/mise à jour et attache ses éventuelles
     * photos. [fetchNetPhoto] (scan par lot uniquement, voir [saveBatch]) : si eBay renvoie la
     * photo d'une annonce correspondante, elle remplace la photo actuelle (le cas échéant la photo
     * du lot partagée entre plusieurs figurines) — jamais activé pour un ajout normal, où la photo
     * est toujours celle prise/choisie par l'utilisateur, jamais remplacée automatiquement.
     */
    private suspend fun resolvePriceAndFinish(
        id: Long, stored: CollectionItem, newPhotoUris: List<String> = emptyList(), fetchNetPhoto: Boolean = false
    ) {
        var finalImageUri = stored.imageUri
        if (stored.priceIsManual && stored.priceCents != null) {
            recordPrice(id, stored.priceCents)
        } else {
            val resolved = resolvePrice(stored.barcode, stored.licence, stored.character, stored.name, stored.condition, stored.hasBox, stored.hasAccessories)
            // Si ni eBay ni l'IA ne renvoient rien cette fois (ex. réseau indisponible pendant une
            // simple modif de photo), on garde la précédente cote connue plutôt que de l'effacer.
            val keptOldPrice = resolved.priceCents == null && stored.priceCents != null
            val finalPriceCents = resolved.priceCents ?: stored.priceCents
            val finalIsAiEstimate = if (keptOldPrice) stored.priceIsAiEstimate else resolved.isAiEstimate
            val finalInfo = if (keptOldPrice) stored.info else resolved.info
            if (fetchNetPhoto && resolved.imageUrl != null) {
                MediaUtils.downloadToInternal(getApplication(), resolved.imageUrl)?.let { finalImageUri = it }
            }
            collectionDao.update(stored.copy(
                id = id, priceCents = finalPriceCents, priceIsManual = false,
                priceIsAiEstimate = finalIsAiEstimate, info = finalInfo, imageUri = finalImageUri
            ))
            if (finalPriceCents != null && !keptOldPrice) {
                recordPrice(id, finalPriceCents)
            }
        }
        if (newPhotoUris.isNotEmpty()) attachPhotos(id, newPhotoUris)
    }

    /**
     * Revérifie la cote d'UNE figurine à la demande (bouton "Rechercher sur eBay" de la fiche
     * détail) : relance eBay+IA en tâche de fond, la fiche affichée se met à jour automatiquement
     * dès que le résultat arrive (elle est branchée en direct sur `collectionDao.observeAll()`,
     * voir `MainActivity.AppRoot`). Ne touche jamais une cote saisie à la main.
     */
    fun recheckPrice(item: CollectionItem) = viewModelScope.launch {
        if (item.priceIsManual) return@launch
        val resolved = resolvePrice(item.barcode, item.licence, item.character, item.name, item.condition, item.hasBox, item.hasAccessories)
        collectionDao.update(item.copy(
            priceCents = resolved.priceCents, priceIsManual = false,
            priceIsAiEstimate = resolved.isAiEstimate, info = resolved.info
        ))
        if (resolved.priceCents != null) recordPrice(item.id, resolved.priceCents)
    }

    /**
     * Appelé à chaque ouverture : réactualise les cotes de toute la collection via eBay/IA. Ne
     * touche JAMAIS une figurine dont la cote a été saisie à la main (`priceIsManual`).
     */
    fun refreshAllPrices() = viewModelScope.launch {
        if (!EbayPrices.isConfigured()) return@launch
        val list = collectionDao.observeAll().first()
        for (it in list) {
            if (it.priceIsManual) continue
            val resolved = resolvePrice(it.barcode, it.licence, it.character, it.name, it.condition, it.hasBox, it.hasAccessories)
            collectionDao.update(it.copy(
                priceCents = resolved.priceCents, priceIsManual = false,
                priceIsAiEstimate = resolved.isAiEstimate, info = resolved.info
            ))
            if (resolved.priceCents != null) recordPrice(it.id, resolved.priceCents)
        }
    }

    /** Résultat de [resolvePrice] : prix final (centimes), origine, message affiché sous la cote, photo eBay éventuelle. */
    private data class PriceResolution(val priceCents: Int?, val isAiEstimate: Boolean, val info: String?, val imageUrl: String? = null)

    /**
     * Estimation IA, avec repli automatique : Gemini (recherche intégrée) en premier, puis
     * Tavily+Groq si Gemini échoue (quota quotidien gratuit très bas).
     */
    private suspend fun aiEstimatePrice(
        licence: Licence, character: String, name: String,
        condition: Condition, hasBox: Boolean, hasAccessories: Boolean
    ): Pair<Int, Boolean>? {
        runCatching { GeminiVision.estimatePrice(licence, character, name, condition, hasBox, hasAccessories) }
            .getOrNull()?.let { return it to false }
        runCatching { TavilyPriceEstimate.estimatePrice(licence, character, name, condition, hasBox, hasAccessories) }
            .getOrNull()?.let { return it to true }
        return null
    }

    /**
     * Calcule la cote d'une figurine en combinant eBay (source principale) et l'IA :
     *  - eBay ne trouve rien → l'IA prend le relais seule, marquée priceIsAiEstimate (« (IA) »).
     *  - eBay trouve seulement 1 ou 2 annonces → on demande aussi l'IA et on fait une moyenne
     *    pondérée par le nombre d'annonces eBay, plutôt que de se fier à une poignée d'annonces
     *    isolées qui peuvent être mal catégorisées ou hors marché.
     *  - eBay trouve 3 annonces ou plus → assez fiable, on ne sollicite pas l'IA.
     */
    private suspend fun resolvePrice(
        barcode: String?, licence: Licence, character: String, name: String,
        condition: Condition, hasBox: Boolean, hasAccessories: Boolean
    ): PriceResolution {
        val r = EbayPrices.lookup(barcode, licence, character, name, condition, hasBox, hasAccessories)
        // Repli photo : si eBay n'a aucune annonce (donc aucune image), recherche web Tavily
        // avant d'abandonner — évite une figurine sans photo juste parce qu'aucune annonce n'est
        // en cours pour un personnage rare (voir AppViewModel.fetchAndCacheCatalogPhoto, même logique).
        val imageUrl = r.imageUrl ?: runCatching { TavilyPriceEstimate.findImage(licence, character) }.getOrNull()
        if (r.priceCents == null) {
            val ai = aiEstimatePrice(licence, character, name, condition, hasBox, hasAccessories)
            return if (ai != null) {
                val (priceCents, viaTavily) = ai
                val label = if (viaTavily) {
                    "Estimation par IA (recherche web) — aucune annonce eBay comparable trouvée, quota Gemini indisponible"
                } else {
                    "Estimation par IA (recherche en ligne) — aucune annonce eBay comparable trouvée"
                }
                PriceResolution(priceCents, true, label, imageUrl)
            } else {
                PriceResolution(null, false, r.info, imageUrl)
            }
        }
        if (r.count < 3) {
            val ai = aiEstimatePrice(licence, character, name, condition, hasBox, hasAccessories)
            if (ai != null) {
                val (priceCents, _) = ai
                val blended = ((r.priceCents.toLong() * r.count + priceCents) / (r.count + 1)).toInt()
                return PriceResolution(blended, false, "${r.info} — recoupé avec une estimation IA (peu d'annonces eBay disponibles)", imageUrl)
            }
        }
        return PriceResolution(r.priceCents, false, r.info, imageUrl)
    }

    /** Ajoute un point d'historique si le prix a changé depuis le dernier relevé. */
    private suspend fun recordPrice(itemId: Long, priceCents: Int) {
        val last = historyDao.latest(itemId)
        if (last == null || last.priceCents != priceCents) {
            historyDao.insert(PriceHistory(itemId = itemId, priceCents = priceCents, timestamp = System.currentTimeMillis()))
        }
    }

    fun deleteCollectionItem(item: CollectionItem) = viewModelScope.launch {
        photoDao.observeForItem(item.id).first().forEach { MediaUtils.deleteFile(it.uri) }
        photoDao.deleteForItem(item.id)
        collectionDao.delete(item)
    }

    /** Photos de galerie d'une figurine (en plus de sa photo principale), dans l'ordre d'ajout. */
    fun photosFor(itemId: Long): Flow<List<ItemPhoto>> = photoDao.observeForItem(itemId)

    fun addPhotos(itemId: Long, uris: List<String>) = viewModelScope.launch { attachPhotos(itemId, uris) }

    fun deletePhoto(photo: ItemPhoto) = viewModelScope.launch {
        photoDao.delete(photo)
        MediaUtils.deleteFile(photo.uri)
    }

    private suspend fun attachPhotos(itemId: Long, uris: List<String>) {
        val startPosition = photoDao.observeForItem(itemId).first().size
        uris.forEachIndexed { index, uri ->
            photoDao.insert(ItemPhoto(itemId = itemId, uri = uri, position = startPosition + index, createdAt = System.currentTimeMillis()))
        }
    }

    /** Historique de prix d'une figurine (du plus ancien au plus récent). */
    suspend fun priceHistory(itemId: Long): List<PriceHistory> = historyDao.forItem(itemId)

    /** Best-effort, appelé au lancement : laisse les anciens taux en cache si le réseau échoue. */
    fun refreshCurrencyRates() = viewModelScope.launch {
        CurrencyRates.fetchLatest()?.let { AppPrefs.setCurrencyRates(getApplication(), it) }
    }

    /** Sauvegarde toute la collection (données + photos) dans le fichier .zip choisi par l'utilisateur. */
    suspend fun exportBackup(destUri: Uri): Boolean = BackupManager.export(getApplication(), db, destUri)

    /** Restaure une sauvegarde .zip : remplace entièrement le contenu actuel. */
    suspend fun importBackup(srcUri: Uri): Boolean = BackupManager.import(getApplication(), db, srcUri)

    /** Export "Excel" (tableau HTML avec photos) de toute la collection + des souhaits. */
    suspend fun exportExcel(destUri: Uri): Boolean =
        com.nawash.macollectionwcf.data.ExcelExport.export(getApplication(), collectionDao.observeAll().first(), destUri)

    /**
     * Insère en lot des figurines déjà entièrement construites (import d'un fichier Excel/CSV
     * externe, voir [com.nawash.macollectionwcf.data.SpreadsheetImport]) : résout aussi leur prix
     * (eBay/IA) pour celles sans prix déjà saisi dans le fichier, comme [saveCollectionItem].
     */
    fun importSpreadsheet(items: List<CollectionItem>) = viewModelScope.launch {
        for (item in items) {
            val (id, stored) = insertOrUpdateBareItem(item.copy(id = 0))
            resolvePriceAndFinish(id, stored)
        }
    }
}
