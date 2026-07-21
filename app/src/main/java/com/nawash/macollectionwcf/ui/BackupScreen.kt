package com.nawash.macollectionwcf.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nawash.macollectionwcf.data.AppPrefs
import com.nawash.macollectionwcf.data.CollectionItem
import com.nawash.macollectionwcf.data.SpreadsheetImport
import com.nawash.macollectionwcf.ui.theme.NeonCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var working by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var confirmRestoreUri by remember { mutableStateOf<Uri?>(null) }

    var pendingSheet by remember { mutableStateOf<SpreadsheetImport.ParsedSheet?>(null) }
    var pendingPreviewItems by remember { mutableStateOf<List<CollectionItem>?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            working = true
            scope.launch {
                val ok = vm.exportBackup(uri)
                working = false
                if (ok) AppPrefs.setLastBackupUri(context, uri.toString())
                resultMessage = if (ok) "Sauvegarde enregistrée avec succès." else "Échec de la sauvegarde."
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmRestoreUri = uri
    }
    val excelExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri ->
        if (uri != null) {
            working = true
            scope.launch {
                val ok = vm.exportExcel(uri)
                working = false
                resultMessage = if (ok) "Tableau Excel exporté avec succès." else "Échec de l'export du tableau Excel."
            }
        }
    }
    val importSheetLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            working = true
            scope.launch {
                val sheet = withContext(Dispatchers.IO) { SpreadsheetImport.parseFile(context, uri) }
                working = false
                resultMessage = if (sheet != null) null else "Impossible de lire ce fichier. Formats acceptés : .xlsx ou .csv."
                pendingSheet = sheet
            }
        }
    }

    Column(
        modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Sauvegarde toute ta collection (données et photos) dans un fichier que tu choisis (téléchargements, Drive...). Ce fichier reste disponible même si tu désinstalles l'application.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { exportLauncher.launch("macollectionwcf-${System.currentTimeMillis()}.zip") },
            enabled = !working,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sauvegarder la collection") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/zip")) },
            enabled = !working,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Restaurer une sauvegarde") }

        Spacer(Modifier.height(20.dp))
        Text(
            "Exporte toute ta collection (et tes souhaits) sous forme de tableau avec une photo par ligne — lisible dans Excel ou tout tableur (Excel affichera un avertissement de format à l'ouverture, sans conséquence : clique sur « Oui »).",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { excelExportLauncher.launch("macollectionwcf-${System.currentTimeMillis()}.xls") },
            enabled = !working,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Exporter en tableau Excel") }

        Spacer(Modifier.height(20.dp))
        Text(
            "Importer une collection depuis un fichier Excel (.xlsx) ou CSV — même si ses colonnes ne correspondent pas au format de l'appli, tu pourras les faire correspondre à l'étape suivante.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { importSheetLauncher.launch(arrayOf("*/*")) },
            enabled = !working,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Importer un fichier Excel/CSV") }

        if (working) {
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator()
        }
        resultMessage?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = NeonCyan, textAlign = TextAlign.Center)
        }
    }

    confirmRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestoreUri = null },
            title = { Text("Restaurer une sauvegarde") },
            text = { Text("Cette action remplace entièrement ta collection actuelle par le contenu de la sauvegarde. Continuer ?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestoreUri = null
                    working = true
                    scope.launch {
                        val ok = vm.importBackup(uri)
                        working = false
                        resultMessage = if (ok) "Collection restaurée avec succès." else "Échec de la restauration : fichier invalide ou endommagé."
                    }
                }) { Text("Restaurer") }
            },
            dismissButton = { TextButton(onClick = { confirmRestoreUri = null }) { Text("Annuler") } }
        )
    }

    pendingSheet?.let { sheet ->
        ImportMappingDialog(
            headers = sheet.headers,
            initialMapping = remember(sheet) { SpreadsheetImport.guessMapping(sheet.headers) },
            onConfirm = { mapping ->
                val rate = AppPrefs.currencyRates.value[AppPrefs.currency.value] ?: 1.0
                pendingPreviewItems = sheet.rows.mapNotNull {
                    SpreadsheetImport.buildItem(it, mapping, isWishlist = false, currencyRate = rate)
                }
                pendingSheet = null
            },
            onDismiss = { pendingSheet = null }
        )
    }

    pendingPreviewItems?.let { items ->
        ImportPreviewDialog(
            items = items,
            onConfirm = { selected ->
                vm.importSpreadsheet(selected)
                resultMessage = "${selected.size} figurine(s) importée(s) avec succès."
                pendingPreviewItems = null
            },
            onDismiss = { pendingPreviewItems = null }
        )
    }
}

@Composable
private fun ImportMappingDialog(
    headers: List<String>,
    initialMapping: Map<SpreadsheetImport.ImportField, Int?>,
    onConfirm: (Map<SpreadsheetImport.ImportField, Int?>) -> Unit,
    onDismiss: () -> Unit
) {
    val mapping = remember { mutableStateMapOf<SpreadsheetImport.ImportField, Int?>().apply { putAll(initialMapping) } }
    val columnOptions = remember(headers) { listOf<Int?>(null) + headers.indices.toList() }
    val fieldLabels = mapOf(
        SpreadsheetImport.ImportField.CHARACTER to "Personnage",
        SpreadsheetImport.ImportField.NAME to "Nom commercial",
        SpreadsheetImport.ImportField.LICENCE to "Licence",
        SpreadsheetImport.ImportField.SERIES to "Gamme / vague WCF",
        SpreadsheetImport.ImportField.MANUFACTURER to "Éditeur",
        SpreadsheetImport.ImportField.CONDITION to "État",
        SpreadsheetImport.ImportField.HAS_BOX to "Avec boîte",
        SpreadsheetImport.ImportField.HAS_ACCESSORIES to "Avec accessoires",
        SpreadsheetImport.ImportField.YEAR to "Année de sortie",
        SpreadsheetImport.ImportField.HEIGHT to "Taille (cm)",
        SpreadsheetImport.ImportField.PRICE to "Prix",
        SpreadsheetImport.ImportField.BARCODE to "Code-barres",
        SpreadsheetImport.ImportField.DESCRIPTION to "Description"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Faire correspondre les colonnes") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "Indique à quelle colonne de ton fichier correspond chaque champ. Seul le Personnage est obligatoire ; les autres champs non trouvés resteront vides.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                SpreadsheetImport.ImportField.values().forEach { field ->
                    LabeledDropdown(
                        label = fieldLabels[field].orEmpty(),
                        options = columnOptions,
                        selected = mapping[field],
                        optionLabel = { idx -> if (idx == null) "Aucune colonne" else headers.getOrElse(idx) { "?" } },
                        onSelect = { mapping[field] = it }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(mapping.toMap()) },
                enabled = mapping[SpreadsheetImport.ImportField.CHARACTER] != null
            ) { Text("Suivant") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun ImportPreviewDialog(
    items: List<CollectionItem>,
    onConfirm: (List<CollectionItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val checked = remember(items) { mutableStateListOf<Boolean>().apply { repeat(items.size) { add(true) } } }
    val selectedCount = checked.count { it }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${items.size} figurine(s) détectée(s)") },
        text = {
            Column {
                Text(
                    "$selectedCount / ${items.size} sélectionnée(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    itemsIndexed(items) { i, item ->
                        Row(
                            Modifier.fillMaxWidth().clickable { checked[i] = !checked[i] }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked[i], onCheckedChange = { checked[i] = it })
                            Column(Modifier.weight(1f)) {
                                Text(item.character, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(item.licence.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(items.filterIndexed { i, _ -> checked[i] }) },
                enabled = checked.any { it }
            ) { Text("Importer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
