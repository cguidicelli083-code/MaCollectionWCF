package com.nawash.macollectionwcf.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.nawash.macollectionwcf.data.CollectionItem
import com.nawash.macollectionwcf.data.Condition
import com.nawash.macollectionwcf.data.Licence
import com.nawash.macollectionwcf.data.MediaUtils
import com.nawash.macollectionwcf.data.selectableLicences
import com.nawash.macollectionwcf.data.toFigurePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddCollectionForm(
    vm: AppViewModel,
    onSave: (CollectionItem, List<String>) -> Unit,
    onCancel: () -> Unit,
    existing: CollectionItem? = null,
    initialCharacter: String = "",
    initialBarcode: String? = null,
    initialCoverUri: String? = null,
    initialLicence: Licence? = null,
    initialSeries: String? = null,
    initialYear: Int? = null,
    initialNumero: String? = null,
    initialHeightCm: Double? = null,
    isWishlist: Boolean = false,
    fromEncyclo: Boolean = false
) {
    val isEdit = existing != null
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val catalogEntries by vm.catalogEntries.collectAsState()

    var licence by remember { mutableStateOf(existing?.licence ?: initialLicence ?: Licence.ONE_PIECE) }
    var character by remember { mutableStateOf(existing?.character ?: initialCharacter) }
    var name by remember { mutableStateOf(existing?.name ?: initialCharacter) }
    var series by remember { mutableStateOf(existing?.series ?: initialSeries ?: "") }
    var numero by remember { mutableStateOf(existing?.numero ?: initialNumero ?: "") }
    var manufacturer by remember { mutableStateOf(existing?.manufacturer ?: "Banpresto") }
    var year by remember { mutableStateOf(existing?.releaseYear?.toString() ?: initialYear?.toString() ?: "") }
    var heightCm by remember { mutableStateOf(existing?.heightCm?.toString() ?: initialHeightCm?.toString() ?: "") }
    var condition by remember { mutableStateOf(existing?.condition ?: Condition.BON) }
    var hasBox by remember { mutableStateOf(existing?.hasBox ?: true) }
    var hasAccessories by remember { mutableStateOf(existing?.hasAccessories ?: true) }
    var price by remember { mutableStateOf(existing?.priceCents?.let { centsToText(it) } ?: "") }
    var barcode by remember { mutableStateOf(existing?.barcode ?: initialBarcode ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var coverUrl by remember { mutableStateOf(existing?.imageUri ?: initialCoverUri) }
    var fullscreenCoverUri by remember { mutableStateOf<String?>(null) }

    val savedGalleryPhotos by vm.photosFor(existing?.id ?: -1L).collectAsState(initial = emptyList())
    var pendingGalleryPhotos by remember { mutableStateOf<List<String>>(emptyList()) }
    var cropForGallery by remember { mutableStateOf(false) }

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { cropped ->
                scope.launch {
                    val saved = withContext(Dispatchers.IO) { MediaUtils.copyToInternal(context, cropped) }
                    if (saved != null) {
                        if (cropForGallery) {
                            if (existing != null) vm.addPhotos(existing.id, listOf(saved))
                            else pendingGalleryPhotos = pendingGalleryPhotos + saved
                            if (coverUrl.isNullOrBlank()) coverUrl = saved
                        } else {
                            coverUrl = saved
                        }
                    }
                }
            }
        }
    }
    fun launchCrop(uri: Uri, forGallery: Boolean) {
        cropForGallery = forGallery
        cropLauncher.launch(
            CropImageContractOptions(uri, CropImageOptions(activityMenuIconColor = android.graphics.Color.WHITE, cropMenuCropButtonTitle = "OK", initialCropWindowPaddingRatio = 0.2f))
        )
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) launchCrop(uri, forGallery = coverUrl != null)
    }
    var pendingCameraFile by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var cameraForGallery by remember { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val pending = pendingCameraFile
        if (success && pending != null) launchCrop(pending.first, cameraForGallery)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val pair = MediaUtils.newCameraFile(context)
            pendingCameraFile = pair
            cameraLauncher.launch(pair.first)
        }
    }
    fun launchCamera(forGallery: Boolean) {
        cameraForGallery = forGallery
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val pair = MediaUtils.newCameraFile(context)
            pendingCameraFile = pair
            cameraLauncher.launch(pair.first)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    FormScaffold(title = if (isEdit) "Modifier la figurine" else if (isWishlist) "Nouveau souhait" else "Nouvelle figurine", onCancel = onCancel) {
        if (!isEdit && !fromEncyclo) {
            val customPresets by vm.customPresets.collectAsState()
            var searchQuery by remember { mutableStateOf("") }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Rechercher dans le catalogue (facultatif)") },
                modifier = Modifier.fillMaxWidth()
            )
            val q = searchQuery.trim().lowercase()
            if (q.length >= 2) {
                val matches = (catalogEntries.map { it.toFigurePreset() } + customPresets.map { it.toFigurePreset() })
                    .filter { it.character.lowercase().contains(q) || it.name.lowercase().contains(q) }
                    .take(8)
                if (matches.isEmpty()) {
                    Text("Aucun résultat dans le catalogue local.", style = MaterialTheme.typography.bodySmall)
                } else {
                    matches.forEach { match ->
                        OutlinedButton(
                            onClick = {
                                licence = match.licence
                                character = match.character
                                name = match.name
                                match.series?.let { series = it }
                                match.year?.let { year = it.toString() }
                                match.numero?.let { numero = it }
                                match.heightCm?.let { heightCm = it.toString() }
                                if (coverUrl.isNullOrBlank()) match.imagePath?.let { coverUrl = it }
                                if (match.catalogId != null && !match.photoChecked) {
                                    val catalogId = match.catalogId
                                    val matchLicence = match.licence
                                    val matchCharacter = match.character
                                    val matchSeries = match.series
                                    scope.launch {
                                        val fetched = vm.enrichCatalogEntryNow(catalogId, matchLicence, matchCharacter, matchSeries)
                                        if (coverUrl.isNullOrBlank() && fetched != null) coverUrl = fetched
                                    }
                                }
                                searchQuery = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${match.character} — ${match.series ?: match.licence.label}")
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Choisir une photo") }
        OutlinedButton(onClick = { launchCamera(forGallery = coverUrl != null) }, modifier = Modifier.fillMaxWidth()) {
            Text("Prendre une photo")
        }
        coverUrl?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp))
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { coverUrl = null }) { Text("Retirer la photo") }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Galerie de photos", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(savedGalleryPhotos, key = { "saved_${it.id}" }) { photo ->
                GalleryThumb(uri = photo.uri, onRemove = { vm.deletePhoto(photo) })
            }
            items(pendingGalleryPhotos, key = { "pending_$it" }) { uri ->
                GalleryThumb(uri = uri, onRemove = { pendingGalleryPhotos = pendingGalleryPhotos - uri; MediaUtils.deleteFile(uri) })
            }
        }
        Spacer(Modifier.height(8.dp))

        ThemedChoiceDropdown(
            leading = "Licence",
            selectedLabel = "${licenceEmoji(licence)} ${licence.label}",
            options = selectableLicences,
            optionLabel = { "${licenceEmoji(it)} ${it.label}" },
            onSelect = { licence = it }
        )
        Field(character, "Personnage") { character = it; if (name.isBlank() || name == character) name = it }

        var seriesExpanded by remember { mutableStateOf(false) }
        val seriesOptions = remember(catalogEntries, licence) {
            catalogEntries.filter { it.licence == licence }.map { it.series }.distinct().sorted()
        }
        val seriesSuggestions = remember(seriesOptions, series) {
            if (series.isBlank()) seriesOptions.take(10) else seriesOptions.filter { it.contains(series, ignoreCase = true) }.take(10)
        }
        Box {
            Field(series, "Gamme / vague WCF") { series = it; seriesExpanded = true }
            DropdownMenu(expanded = seriesExpanded && seriesSuggestions.isNotEmpty(), onDismissRequest = { seriesExpanded = false }) {
                seriesSuggestions.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = { series = opt; seriesExpanded = false })
                }
            }
        }
        Field(numero, "Code (ex. OP-427, laisser vide si inconnu)") { numero = it }
        Field(year, "Année de sortie", KeyboardType.Number) { year = it.filter { c -> c.isDigit() }.take(4) }
        Field(heightCm, "Taille (cm)", KeyboardType.Decimal) { heightCm = it }
        LabeledDropdown("État", Condition.entries.toList(), condition, { it.label }) { condition = it }
        SwitchRow("Avec boîte", hasBox) { hasBox = it }
        SwitchRow("Avec accessoires (armes, pièces...)", hasAccessories) { hasAccessories = it }
        Field(price, "Prix (€, laisser vide si inconnu)", KeyboardType.Decimal) { price = it }

        Spacer(Modifier.height(6.dp))
        var isSaving by remember { mutableStateOf(false) }
        Button(
            onClick = {
                isSaving = true
                val typedPriceCents = parsePriceToCents(price)
                onSave(
                    CollectionItem(
                        id = existing?.id ?: 0,
                        licence = licence,
                        series = series.trim().ifBlank { null },
                        character = character.trim(),
                        name = name.trim().ifBlank { character.trim() },
                        numero = numero.trim().ifBlank { null },
                        manufacturer = manufacturer.trim().ifBlank { "Banpresto" },
                        condition = condition,
                        hasBox = hasBox,
                        hasAccessories = hasAccessories,
                        heightCm = heightCm.toDoubleOrNull(),
                        releaseYear = year.toIntOrNull(),
                        priceCents = typedPriceCents,
                        priceIsManual = typedPriceCents != null,
                        barcode = barcode.trim().ifBlank { null },
                        description = description.trim().ifBlank { null },
                        imageUri = coverUrl,
                        isWishlist = existing?.isWishlist ?: isWishlist,
                        createdAt = existing?.createdAt ?: 0
                    ),
                    pendingGalleryPhotos
                )
                isSaving = false
            },
            enabled = character.isNotBlank() && !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = androidx.compose.ui.graphics.Color.White)
            } else {
                Text(if (isEdit) "Mettre à jour" else if (isWishlist) "Ajouter aux souhaits" else "Enregistrer")
            }
        }
    }

    fullscreenCoverUri?.let { uri ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { fullscreenCoverUri = null }) {
            ZoomableImage(uri = uri, modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
    }
}
