@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.nawash.macollectionwcf

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.nawash.macollectionwcf.ads.AdsManager
import com.nawash.macollectionwcf.ads.BannerAdView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.nawash.macollectionwcf.data.AppPrefs
import com.nawash.macollectionwcf.data.CollectionItem
import com.nawash.macollectionwcf.data.CurrencyOptions
import com.nawash.macollectionwcf.data.CustomFigurePreset
import com.nawash.macollectionwcf.data.FigurePreset
import com.nawash.macollectionwcf.data.GeminiVision
import com.nawash.macollectionwcf.data.GroqVision
import com.nawash.macollectionwcf.data.Licence
import com.nawash.macollectionwcf.data.MediaUtils
import com.nawash.macollectionwcf.data.ScanTools
import com.nawash.macollectionwcf.ui.ActuScreen
import com.nawash.macollectionwcf.ui.AddCollectionForm
import com.nawash.macollectionwcf.ui.AddCustomFigurePresetForm
import com.nawash.macollectionwcf.ui.AppViewModel
import com.nawash.macollectionwcf.ui.BackupScreen
import com.nawash.macollectionwcf.ui.BatchScanDialog
import com.nawash.macollectionwcf.ui.CollectionScreen
import com.nawash.macollectionwcf.ui.FigureEncyclopediaScreen
import com.nawash.macollectionwcf.ui.GamerScreenBackground
import com.nawash.macollectionwcf.ui.ItemDetailScreen
import com.nawash.macollectionwcf.ui.OnboardingScreen
import com.nawash.macollectionwcf.ui.TotalScreen
import com.nawash.macollectionwcf.ui.theme.AppTheme
import com.nawash.macollectionwcf.ui.theme.MaCollectionWcfTheme
import com.nawash.macollectionwcf.ui.theme.NeonPurple
import com.nawash.macollectionwcf.ui.theme.SurfaceBg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPrefs.load(this)
        AdsManager.init(this)
        setContent {
            MaCollectionWcfTheme {
                AppRoot()
            }
        }
    }
}

/** BACKUP n'a pas d'icône dans la barre de navigation : accessible via le menu Réglages. */
enum class Tab { COLLECTION, WISHLIST, ENCYCLO, TOTAL, ACTU, BACKUP }

/** État du formulaire Collection : nouvelle figurine (scan ou manuel) OU modification. */
private data class CollectionEditor(
    val existing: CollectionItem? = null,
    val character: String = "",
    val barcode: String? = null,
    val coverUri: String? = null,
    val licence: Licence? = null,
    val series: String? = null,
    val year: Int? = null,
    val numero: String? = null,
    val heightCm: Double? = null,
    val isWishlist: Boolean = false,
    /** true si l'ajout part de l'Encyclo : après enregistrement, on ouvre directement la fiche
     * dans Collection/Souhaits au lieu de revenir juste à la liste. */
    val fromEncyclo: Boolean = false
)

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.refreshAllPrices()
        vm.refreshCurrencyRates()
    }

    var showOnboarding by remember { mutableStateOf(!AppPrefs.onboardingSeen) }
    var tab by remember { mutableStateOf(Tab.COLLECTION) }
    var encycloSelectionMode by remember { mutableStateOf(false) }
    var showChooser by remember { mutableStateOf(false) }
    var chooserForWishlist by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<CollectionEditor?>(null) }
    var viewing by remember { mutableStateOf<CollectionItem?>(null) }
    var addingCustomPreset by remember { mutableStateOf(false) }
    var editingCustomPreset by remember { mutableStateOf<CustomFigurePreset?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var deepScanning by remember { mutableStateOf(false) }
    var batchScanning by remember { mutableStateOf(false) }
    var batchResults by remember { mutableStateOf<List<GeminiVision.BatchItem>?>(null) }
    var batchError by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showTipsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    // Position de défilement de chaque liste, hissée ici pour survivre à un aller-retour vers
    // la fiche de détail/modification ou à un changement d'onglet.
    val collectionListState = rememberLazyListState()
    val wishlistListState = rememberLazyListState()
    val encycloListState = rememberLazyListState()

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val croppedUri = result.uriContent
            if (croppedUri != null) {
                scope.launch {
                    scanning = true
                    val r = runCatching { ScanTools.scanImage(context, croppedUri) { deepScanning = true } }.getOrNull()
                    val saved = withContext(Dispatchers.IO) { MediaUtils.copyToInternal(context, croppedUri) }
                    scanning = false
                    deepScanning = false
                    editor = CollectionEditor(
                        character = r?.suggestedName ?: "",
                        barcode = r?.barcode,
                        coverUri = saved,
                        licence = r?.licence,
                        series = r?.series,
                        isWishlist = chooserForWishlist
                    )
                }
            }
        }
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            cropLauncher.launch(CropImageContractOptions(uri, CropImageOptions(activityMenuIconColor = android.graphics.Color.WHITE, cropMenuCropButtonTitle = "OK")))
        }
    }
    // Scan multiple : photo de lot envoyée telle quelle (pas de recadrage restrictif). Copiée en
    // stockage interne pour persister : faute de pouvoir découper une photo par figurine, elle sera
    // attachée telle quelle comme photo principale de chaque figurine ajoutée (voir vm.saveBatch).
    var batchCoverUri by remember { mutableStateOf<String?>(null) }
    val batchPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            scanning = true; batchScanning = true; batchError = false
            batchCoverUri = withContext(Dispatchers.IO) { MediaUtils.copyToInternal(context, uri) }
            val res = runCatching { GeminiVision.identifyBatch(context, uri) }.getOrNull()
                ?: runCatching { GroqVision.identifyBatch(context, uri) }.getOrNull()
            scanning = false; batchScanning = false
            if (res == null) batchError = true else batchResults = res
        }
    }
    var pendingCameraFile by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val pending = pendingCameraFile
        if (success && pending != null) {
            cropLauncher.launch(CropImageContractOptions(pending.first, CropImageOptions(activityMenuIconColor = android.graphics.Color.WHITE, cropMenuCropButtonTitle = "OK")))
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val pair = MediaUtils.newCameraFile(context)
            pendingCameraFile = pair
            cameraLauncher.launch(pair.first)
        }
    }
    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val pair = MediaUtils.newCameraFile(context)
            pendingCameraFile = pair
            cameraLauncher.launch(pair.first)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Choix d'une image de fond personnalisée (Réglages) : copiée en stockage interne pour
    // persister, puis enregistrée comme préférence (état Compose -> mise à jour immédiate).
    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) { MediaUtils.copyToInternal(context, uri) }
                if (saved != null) AppPrefs.setBackgroundImageUri(context, saved)
            }
        }
    }

    if (showOnboarding) {
        OnboardingScreen(onFinish = { AppPrefs.setOnboardingSeen(context); showOnboarding = false })
        return
    }

    if (scanning) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(
                    when {
                        batchScanning -> "Analyse du lot en cours…"
                        deepScanning -> "Reconnaissance du personnage (IA)…"
                        else -> "Analyse de la photo…"
                    },
                    color = Color.White
                )
            }
        }
        return
    }

    if (addingCustomPreset) {
        BackHandler { addingCustomPreset = false }
        AddCustomFigurePresetForm(vm = vm, onSaved = { addingCustomPreset = false }, onCancel = { addingCustomPreset = false })
        return
    }
    editingCustomPreset?.let { edited ->
        BackHandler { editingCustomPreset = null }
        AddCustomFigurePresetForm(vm = vm, existing = edited, onSaved = { editingCustomPreset = null }, onCancel = { editingCustomPreset = null })
        return
    }
    editor?.let { ed ->
        BackHandler { editor = null }
        AddCollectionForm(
            vm = vm,
            existing = ed.existing,
            initialCharacter = ed.character,
            initialBarcode = ed.barcode,
            initialCoverUri = ed.coverUri,
            initialLicence = ed.licence,
            initialSeries = ed.series,
            initialYear = ed.year,
            initialNumero = ed.numero,
            initialHeightCm = ed.heightCm,
            isWishlist = ed.isWishlist,
            fromEncyclo = ed.fromEncyclo,
            onSave = { item, photos ->
                vm.saveCollectionItem(item, photos) { id ->
                    if (ed.fromEncyclo) viewing = item.copy(id = id)
                }
                editor = null
            },
            onCancel = { editor = null }
        )
        return
    }
    viewing?.let { snapshot ->
        val liveItems by vm.items.collectAsState()
        val liveWishlist by vm.wishlist.collectAsState()
        val item = (liveItems + liveWishlist).firstOrNull { it.id == snapshot.id } ?: snapshot
        BackHandler { viewing = null }
        ItemDetailScreen(
            vm = vm,
            item = item,
            onEdit = { editor = CollectionEditor(existing = item); viewing = null },
            onDelete = { vm.deleteCollectionItem(item); viewing = null },
            onBack = { viewing = null }
        )
        return
    }

    if (showChooser) {
        AlertDialog(
            onDismissRequest = { showChooser = false },
            title = { Text(if (chooserForWishlist) "Nouveau souhait" else "Ajouter une figurine") },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showChooser = false }) { Text("Annuler") } },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showChooser = false
                            scope.launch {
                                val code = runCatching { ScanTools.scanCamera(context) }.getOrNull()
                                if (code != null) {
                                    editor = CollectionEditor(barcode = code, isWishlist = chooserForWishlist)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Scanner un code-barres") }
                    TextButton(
                        onClick = { showChooser = false; photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Choisir une photo") }
                    TextButton(
                        onClick = { showChooser = false; launchCamera() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Prendre une photo") }
                    TextButton(
                        onClick = {
                            showChooser = false
                            batchPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Scanner un lot (plusieurs figurines)") }
                    TextButton(
                        onClick = { showChooser = false; editor = CollectionEditor(isWishlist = chooserForWishlist) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Saisie manuelle") }
                }
            }
        )
    }

    if (batchError) {
        AlertDialog(
            onDismissRequest = { batchError = false },
            title = { Text("Analyse indisponible") },
            text = { Text("Analyse IA momentanément indisponible (limite quotidienne atteinte ou connexion). Réessaie plus tard.") },
            confirmButton = { TextButton(onClick = { batchError = false }) { Text("Fermer") } }
        )
    }

    batchResults?.let { res ->
        if (res.isEmpty()) {
            AlertDialog(
                onDismissRequest = { batchResults = null },
                title = { Text("Aucune figurine détectée") },
                text = { Text("Aucune figurine détectée sur cette photo.") },
                confirmButton = { TextButton(onClick = { batchResults = null }) { Text("Fermer") } }
            )
        } else {
            BatchScanDialog(
                items = res,
                onConfirm = { selected -> vm.saveBatch(selected, chooserForWishlist, batchCoverUri); batchResults = null },
                onDismiss = { batchResults = null }
            )
        }
    }

    val batchSavingCount by vm.batchSaving.collectAsState()
    batchSavingCount?.let { count ->
        AlertDialog(
            onDismissRequest = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Ajout de $count figurine(s) en cours…")
                }
            },
            confirmButton = {}
        )
    }

    if (showOptionsMenu) {
        AlertDialog(
            onDismissRequest = { showOptionsMenu = false },
            title = { Text("Réglages") },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showOptionsMenu = false }) { Text("Fermer") } },
            text = {
                Column {
                    TextButton(
                        onClick = { showOptionsMenu = false; showCurrencyDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("💱 Devise") }
                    TextButton(
                        onClick = { showOptionsMenu = false; tab = Tab.BACKUP },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("💾 Sauvegarde") }
                    TextButton(
                        onClick = { showOptionsMenu = false; showOnboarding = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("❓ Revoir le tutoriel") }
                    TextButton(
                        onClick = { showOptionsMenu = false; showTipsDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("💡 Astuces") }
                    TextButton(
                        onClick = {
                            showOptionsMenu = false
                            backgroundPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🖼️ Image de fond") }
                    if (AppPrefs.backgroundImageUri.value != null) {
                        TextButton(
                            onClick = { showOptionsMenu = false; AppPrefs.setBackgroundImageUri(context, null) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("↩️ Fond par défaut") }
                    }
                    TextButton(
                        onClick = { showOptionsMenu = false; showThemeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🎨 Thème") }
                }
            }
        )
    }

    if (showThemeDialog) {
        val currentThemeId = AppPrefs.selectedTheme.value
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Thème de l'application") },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Fermer") } },
            text = {
                Column {
                    Text(
                        "Choisis un thème par licence. Tous sont gratuits et applicables immédiatement.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    AppTheme.entries.forEach { theme ->
                        val selected = currentThemeId == theme.id
                        TextButton(
                            onClick = { AppPrefs.setSelectedTheme(context, theme.id); showThemeDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(theme.emoji, fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(if (selected) "${theme.label}  ✓" else theme.label, modifier = Modifier.weight(1f))
                                Box(Modifier.size(16.dp).background(theme.accent, androidx.compose.foundation.shape.CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Box(Modifier.size(16.dp).background(theme.accentAlt, androidx.compose.foundation.shape.CircleShape))
                            }
                        }
                    }
                }
            }
        )
    }

    if (showCurrencyDialog) {
        val currentCurrency = AppPrefs.currency.value
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("💱 Devise") },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text("Fermer") } },
            text = {
                Column {
                    Text(
                        "Devise d'affichage et de saisie des cotes (toujours convertie depuis l'euro, au taux le plus récent récupéré au lancement).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                        CurrencyOptions.list.forEach { option ->
                            val name = "${option.symbol}  ${option.label} (${option.code})"
                            TextButton(
                                onClick = { AppPrefs.setCurrency(context, option.code); showCurrencyDialog = false },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (currentCurrency == option.code) "$name  ✓" else name) }
                        }
                    }
                }
            }
        )
    }

    if (showTipsDialog) {
        AlertDialog(
            onDismissRequest = { showTipsDialog = false },
            title = { Text("💡 Astuces") },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showTipsDialog = false }) { Text("Fermer") } },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text("📸 Bien cadrer la photo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Cadre la photo bien serrée sur la figurine ou sa boîte, à plat et de face, en pleine lumière sans reflet ni flou : plus le texte de la boîte est net et lisible, plus la suggestion de nom automatique sera fiable.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("🏷️ Bien renseigner une figurine", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Indique le personnage, la licence et si possible la gamme/vague WCF exacte (visible sur la boîte) : ça aidera à retrouver la bonne fiche dans l'Encyclo et, plus tard, à obtenir une cote plus précise.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("💰 Prix saisi à la main", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tant que l'estimation automatique n'est pas disponible, saisis toi-même le prix sur chaque fiche (bouton « Modifier le prix ») pour que la valeur totale de ta collection reste à jour.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        )
    }

    GamerScreenBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(titleFor(tab), fontWeight = FontWeight.Bold) },
                    actions = {
                        if (tab == Tab.ENCYCLO) {
                            IconButton(onClick = { encycloSelectionMode = !encycloSelectionMode }) {
                                Icon(
                                    Icons.Filled.ChecklistRtl,
                                    contentDescription = "Sélection multiple",
                                    tint = if (encycloSelectionMode) NeonPurple else Color.White
                                )
                            }
                        }
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Réglages", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
                )
            },
            bottomBar = {
                val navTheme = AppTheme.byId(AppPrefs.selectedTheme.value)
                val context = LocalContext.current
                Column {
                    // Bandeau pub léger : affiché en bas de CHAQUE écran (partagé par tous les
                    // onglets via le bottomBar du Scaffold), au-dessus de la barre de navigation.
                    BannerAdView()
                    NavigationBar(containerColor = SurfaceBg) {
                        NavigationBarItem(
                            selected = tab == Tab.COLLECTION,
                            onClick = { tab = Tab.COLLECTION },
                            icon = { ThemedNavIcon(navTheme, 0, Icons.Filled.Star) },
                            label = { NavLabel("Collection") },
                            colors = navColors()
                        )
                        NavigationBarItem(
                            selected = tab == Tab.WISHLIST,
                            onClick = { tab = Tab.WISHLIST },
                            icon = { ThemedNavIcon(navTheme, 1, Icons.Filled.Favorite) },
                            label = { NavLabel("Souhaits") },
                            colors = navColors()
                        )
                        NavigationBarItem(
                            selected = tab == Tab.ENCYCLO,
                            onClick = { tab = Tab.ENCYCLO },
                            icon = { ThemedNavIcon(navTheme, 2, Icons.Filled.MenuBook) },
                            label = { NavLabel("Encyclo") },
                            colors = navColors()
                        )
                        NavigationBarItem(
                            selected = tab == Tab.TOTAL,
                            onClick = {
                                tab = Tab.TOTAL
                                // Seul endroit de l'app où une pub s'ouvre d'elle-même (voir AdsManager).
                                (context as? android.app.Activity)?.let { AdsManager.showInterstitialOnTotal(it) }
                            },
                            icon = { ThemedNavIcon(navTheme, 3, Icons.Filled.Euro) },
                            label = { NavLabel("Total") },
                            colors = navColors()
                        )
                        NavigationBarItem(
                            selected = tab == Tab.ACTU,
                            onClick = { tab = Tab.ACTU },
                            icon = { ThemedNavIcon(navTheme, 4, Icons.Filled.Campaign) },
                            label = { NavLabel("Actu") },
                            colors = navColors()
                        )
                    }
                }
            },
            floatingActionButton = {
                if (tab == Tab.COLLECTION || tab == Tab.WISHLIST || tab == Tab.ENCYCLO) {
                    FloatingActionButton(
                        onClick = {
                            when (tab) {
                                Tab.WISHLIST -> { chooserForWishlist = true; showChooser = true }
                                Tab.ENCYCLO -> addingCustomPreset = true
                                else -> { chooserForWishlist = false; showChooser = true }
                            }
                        },
                        containerColor = NeonPurple,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter")
                    }
                }
            }
        ) { padding ->
            val swipeOrder = listOf(Tab.COLLECTION, Tab.WISHLIST, Tab.ENCYCLO, Tab.TOTAL, Tab.ACTU)
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(tab) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = {
                                val threshold = 72.dp.toPx()
                                val idx = swipeOrder.indexOf(tab)
                                if (idx >= 0) {
                                    if (totalDrag <= -threshold && idx < swipeOrder.lastIndex) tab = swipeOrder[idx + 1]
                                    else if (totalDrag >= threshold && idx > 0) tab = swipeOrder[idx - 1]
                                }
                            }
                        ) { change, dragAmount -> totalDrag += dragAmount; change.consume() }
                    }
            ) {
                when (tab) {
                    Tab.COLLECTION -> CollectionScreen(
                        vm = vm,
                        onOpen = { viewing = it },
                        onEdit = { editor = CollectionEditor(existing = it) },
                        modifier = Modifier.padding(padding),
                        listState = collectionListState
                    )
                    Tab.WISHLIST -> CollectionScreen(
                        vm = vm,
                        onOpen = { viewing = it },
                        onEdit = { editor = CollectionEditor(existing = it) },
                        modifier = Modifier.padding(padding),
                        wishlist = true,
                        listState = wishlistListState
                    )
                    Tab.ENCYCLO -> FigureEncyclopediaScreen(
                        vm = vm,
                        selectionMode = encycloSelectionMode,
                        onAddToCollection = { preset -> editor = presetToEditor(preset, isWishlist = false).copy(fromEncyclo = true) },
                        onAddToWishlist = { preset -> editor = presetToEditor(preset, isWishlist = true).copy(fromEncyclo = true) },
                        onEditPreset = { editingCustomPreset = it },
                        modifier = Modifier.padding(padding),
                        listState = encycloListState
                    )
                    Tab.TOTAL -> TotalScreen(vm, Modifier.padding(padding))
                    Tab.ACTU -> ActuScreen(vm, Modifier.padding(padding))
                    Tab.BACKUP -> BackupScreen(vm, Modifier.padding(padding))
                }
            }
        }
    }
}

private fun presetToEditor(preset: FigurePreset, isWishlist: Boolean) = CollectionEditor(
    character = preset.character,
    coverUri = preset.imagePath,
    licence = preset.licence,
    series = preset.series,
    year = preset.year,
    numero = preset.numero,
    heightCm = preset.heightCm,
    isWishlist = isWishlist
)

@Composable
private fun NavLabel(text: String) {
    Text(text, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
}

/**
 * Icône d'un onglet : l'emoji du thème actif si défini (thèmes par licence), sinon l'icône
 * Material d'origine (thème par défaut). Emojis standards uniquement — aucune image de licence.
 */
@Composable
private fun ThemedNavIcon(theme: AppTheme, index: Int, fallback: androidx.compose.ui.graphics.vector.ImageVector) {
    val emoji = theme.navIcons.getOrNull(index)
    if (emoji != null) Text(emoji, fontSize = 20.sp) else Icon(fallback, contentDescription = null)
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.secondary,
    selectedTextColor = MaterialTheme.colorScheme.secondary,
    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
    unselectedIconColor = Color(0xFF9A9AB5),
    unselectedTextColor = Color(0xFF9A9AB5)
)

@Composable
private fun titleFor(tab: Tab): String = when (tab) {
    Tab.COLLECTION -> "Ma collection"
    Tab.WISHLIST -> "Souhaits"
    Tab.ENCYCLO -> "Encyclo"
    Tab.TOTAL -> "Total"
    Tab.ACTU -> "Actu"
    Tab.BACKUP -> "Sauvegarde"
}
