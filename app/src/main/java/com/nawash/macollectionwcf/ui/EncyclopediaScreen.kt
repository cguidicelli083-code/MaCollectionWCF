package com.nawash.macollectionwcf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.nawash.macollectionwcf.data.CustomFigurePreset
import com.nawash.macollectionwcf.data.FigurePreset
import com.nawash.macollectionwcf.data.Licence
import com.nawash.macollectionwcf.data.MediaUtils
import com.nawash.macollectionwcf.data.upcomingReleaseLabel
import com.nawash.macollectionwcf.ui.theme.NeonCyan
import com.nawash.macollectionwcf.ui.theme.NeonPurple
import com.nawash.macollectionwcf.data.toFigurePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Fiche d'Encyclopédie fusionnée : catalogue intégré (Phase 5) ou fiche perso (voir [custom]). */
private data class EncycloEntry(val preset: FigurePreset, val custom: CustomFigurePreset?)

@Composable
fun FigureEncyclopediaScreen(
    vm: AppViewModel,
    onAddToCollection: (FigurePreset) -> Unit,
    onAddToWishlist: (FigurePreset) -> Unit,
    onEditPreset: (CustomFigurePreset) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val catalogEntries by vm.catalogEntries.collectAsState()
    val customPresets by vm.customPresets.collectAsState()
    val photoOverrides by vm.photoOverrides.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var licenceFilter by remember { mutableStateOf<Licence?>(null) }
    var seriesFilter by remember { mutableStateOf<String?>(null) }
    var opened by remember { mutableStateOf<EncycloEntry?>(null) }

    // Le choix de vague/volume dépend de la licence sélectionnée : on l'invalide si elle change.
    LaunchedEffect(licenceFilter) { seriesFilter = null }

    val seriesOptions = remember(catalogEntries, customPresets, licenceFilter) {
        (catalogEntries.map { it.toFigurePreset() } + customPresets.map { it.toFigurePreset() })
            .filter { licenceFilter == null || it.licence == licenceFilter }
            .mapNotNull { it.series?.ifBlank { null } }
            .distinct()
            .sorted()
    }

    val entries = remember(catalogEntries, customPresets, searchQuery, licenceFilter, seriesFilter) {
        val all = catalogEntries.map { EncycloEntry(it.toFigurePreset(), null) } +
            customPresets.map { EncycloEntry(it.toFigurePreset(), it) }
        val q = searchQuery.trim().lowercase()
        all.filter { e ->
            (licenceFilter == null || e.preset.licence == licenceFilter) &&
                (seriesFilter == null || e.preset.series == seriesFilter) &&
                (q.isEmpty() || e.preset.character.lowercase().contains(q) || e.preset.name.lowercase().contains(q))
        }.sortedWith(compareBy({ it.preset.licence.label }, { it.preset.series ?: "" }, { it.preset.numero ?: "" }, { it.preset.name.lowercase() }))
    }

    Column(modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(8.dp))
        ThemedSearchField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = "Rechercher un personnage")
        Spacer(Modifier.height(8.dp))
        LicenceFilterDropdown(licenceFilter) { licenceFilter = it }
        if (seriesOptions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            ThemedChoiceDropdown(
                leading = "Vague/Volume",
                selectedLabel = seriesFilter ?: "Toutes les vagues",
                options = listOf(null) + seriesOptions,
                optionLabel = { it ?: "Toutes les vagues" },
                onSelect = { seriesFilter = it }
            )
        }
        Spacer(Modifier.height(10.dp))

        if (entries.isEmpty()) {
            EmptyState(
                if (customPresets.isEmpty())
                    "Le catalogue intégré arrivera dans une prochaine mise à jour. En attendant, ajoute tes propres fiches avec le bouton +."
                else "Aucun résultat pour cette recherche."
            )
        } else {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(entries, key = { index, entry -> entry.custom?.let { "custom_${it.id}" } ?: "preset_${index}_${entry.preset.name}" }) { index, entry ->
                        val overrideUri = photoOverrides[entry.preset.name]
                        val showHeader = entry.custom == null &&
                            (index == 0 || entries[index - 1].preset.series != entry.preset.series || entries[index - 1].custom != null)
                        Column {
                            if (showHeader && !entry.preset.series.isNullOrBlank()) {
                                Text(
                                    entry.preset.series!!,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.padding(top = 4.dp, bottom = if (upcomingReleaseLabel(entry.preset.series) != null) 0.dp else 4.dp)
                                )
                                upcomingReleaseLabel(entry.preset.series)?.let { label ->
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                            FigureRow(
                                entry.preset,
                                overrideUri,
                                onEnrich = { p -> vm.enrichCatalogEntry(p.catalogId!!, p.licence, p.character, p.series) }
                            ) { opened = entry }
                        }
                    }
                    item { Spacer(Modifier.height(90.dp)) }
                }
                ScrollProgressBar(
                    listState,
                    Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 8.dp, horizontal = 2.dp)
                )
            }
        }
    }

    opened?.let { entry ->
        val overrideUri = photoOverrides[entry.preset.name] ?: entry.preset.imagePath
        val presetWithPhoto = entry.preset.copy(imagePath = overrideUri)
        FigureDetailDialog(
            vm = vm,
            preset = entry.preset,
            photoUri = overrideUri,
            isCustom = entry.custom != null,
            onAddToCollection = { onAddToCollection(presetWithPhoto); opened = null },
            onAddToWishlist = { onAddToWishlist(presetWithPhoto); opened = null },
            onEdit = entry.custom?.let { { onEditPreset(it); opened = null } },
            onDelete = entry.custom?.let { { vm.deleteCustomPreset(it); opened = null } },
            onDismiss = { opened = null }
        )
    }
}

@Composable
private fun FigureRow(preset: FigurePreset, overrideUri: String?, onEnrich: (FigurePreset) -> Unit, onClick: () -> Unit) {
    LaunchedEffect(preset.catalogId) {
        if (preset.catalogId != null && !preset.photoChecked) onEnrich(preset)
    }
    GamerCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val photo = overrideUri ?: preset.imagePath
            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(NeonPurple, NeonCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(licenceEmoji(preset.licence), style = MaterialTheme.typography.headlineSmall)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(preset.character.ifBlank { preset.name }, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                Text(
                    listOfNotNull(preset.numero, preset.licence.label, preset.series?.ifBlank { null }).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FigureDetailDialog(
    vm: AppViewModel,
    preset: FigurePreset,
    photoUri: String?,
    isCustom: Boolean,
    onAddToCollection: () -> Unit,
    onAddToWishlist: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var fullscreenUri by remember { mutableStateOf<String?>(null) }
    // Mise à jour optimiste : `photoUri` vient d'un instantané figé au moment de l'ouverture
    // (voir `opened` dans FigureEncyclopediaScreen), donc il ne se rafraîchirait pas tout seul
    // après l'enregistrement en base — on l'affiche immédiatement ici en attendant.
    var localPhotoOverride by remember(preset.catalogId) { mutableStateOf<String?>(null) }
    val displayedPhoto = localPhotoOverride ?: photoUri
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { cropped ->
                scope.launch {
                    val saved = withContext(Dispatchers.IO) { MediaUtils.copyToInternal(context, cropped) }
                    if (saved != null) {
                        localPhotoOverride = saved
                        preset.catalogId?.let { vm.setCatalogPhoto(it, saved) }
                    }
                }
            }
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            cropLauncher.launch(CropImageContractOptions(uri, CropImageOptions(activityMenuIconColor = android.graphics.Color.WHITE, cropMenuCropButtonTitle = "OK", initialCropWindowPaddingRatio = 0.2f)))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(preset.character.ifBlank { preset.name }) },
        text = {
            Column {
                if (displayedPhoto != null) {
                    AsyncImage(
                        model = displayedPhoto,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp))
                            .clickable { fullscreenUri = displayedPhoto }
                    )
                } else {
                    Box(
                        Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(NeonPurple, NeonCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(licenceEmoji(preset.licence), style = MaterialTheme.typography.displayMedium)
                            Text(
                                "Photo officielle à venir",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (!isCustom && preset.catalogId != null) {
                    OutlinedButton(
                        onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (displayedPhoto != null) "Changer la photo" else "Ajouter une photo") }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    listOfNotNull(
                        preset.numero,
                        preset.licence.label,
                        preset.series?.ifBlank { null },
                        preset.year?.toString(),
                        preset.heightCm?.let { "${if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString()} cm" }
                    ).joinToString(" • ")
                )
                upcomingReleaseLabel(preset.series)?.let { label ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$label — coffret pas encore commercialisé, visuel officiel à venir.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
                preset.priceCents?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Côte indicative : ${formatPrice(it)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
                if (preset.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(preset.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Column {
                Button(onClick = onAddToCollection, modifier = Modifier.fillMaxWidth()) { Text("Ajouter à ma collection") }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onAddToWishlist, modifier = Modifier.fillMaxWidth()) { Text("Ajouter aux souhaits") }
                if (onEdit != null || onDelete != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth()) {
                        onEdit?.let { TextButton(onClick = it, modifier = Modifier.weight(1f)) { Text("Modifier") } }
                        onDelete?.let { TextButton(onClick = it, modifier = Modifier.weight(1f)) { Text("Supprimer") } }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Fermer") }
                }
            }
        }
    )

    fullscreenUri?.let { uri ->
        Dialog(onDismissRequest = { fullscreenUri = null }) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                ZoomableImage(uri = uri, modifier = Modifier.fillMaxWidth().padding(16.dp))
                IconButton(onClick = { fullscreenUri = null }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun AddCustomFigurePresetForm(
    vm: AppViewModel,
    existing: CustomFigurePreset? = null,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var licence by remember { mutableStateOf(existing?.licence ?: Licence.ONE_PIECE) }
    var character by remember { mutableStateOf(existing?.character ?: "") }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var series by remember { mutableStateOf(existing?.series ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var photoUri by remember { mutableStateOf(existing?.let { vm.photoOverrides.value[it.name] ?: it.photoUri }) }

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { cropped ->
                scope.launch {
                    val saved = withContext(Dispatchers.IO) { MediaUtils.copyToInternal(context, cropped) }
                    if (saved != null) photoUri = saved
                }
            }
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            cropLauncher.launch(CropImageContractOptions(uri, CropImageOptions(activityMenuIconColor = android.graphics.Color.WHITE, cropMenuCropButtonTitle = "OK", initialCropWindowPaddingRatio = 0.2f)))
        }
    }

    FormScaffold(title = if (existing != null) "Modifier la fiche" else "Nouvelle fiche Encyclo", onCancel = onCancel) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Licence.entries.forEach { l ->
                NeonChip("${licenceEmoji(l)} ${l.label}", licence == l) { licence = l }
            }
        }
        Field(character, "Personnage") { character = it; if (name.isBlank()) name = it }
        Field(name, "Nom commercial") { name = it }
        Field(series, "Gamme / vague WCF") { series = it }
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )
        OutlinedButton(
            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Choisir une photo") }
        photoUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp))
            )
        }
        Button(
            onClick = {
                val preset = CustomFigurePreset(
                    licence = licence,
                    character = character.trim(),
                    name = name.trim().ifBlank { character.trim() },
                    series = series.trim().ifBlank { null },
                    description = description.trim(),
                    photoUri = photoUri
                )
                if (existing != null) vm.updateCustomPreset(existing, preset) else vm.addCustomPreset(preset)
                onSaved()
            },
            enabled = character.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Enregistrer") }
    }
}
