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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nawash.macollectionwcf.R
import com.nawash.macollectionwcf.data.AppPrefs
import com.nawash.macollectionwcf.data.CollectionItem
import com.nawash.macollectionwcf.data.SpreadsheetImport
import com.nawash.macollectionwcf.ui.theme.NeonCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupScreen(vm: AppViewModel, modifier: Modifier = Modifier, onOpenPremium: () -> Unit = {}) {
    val isPremium by vm.isPremium.collectAsState()
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
                resultMessage = context.getString(if (ok) R.string.backup_export_success else R.string.backup_export_failure)
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
                resultMessage = context.getString(if (ok) R.string.excel_export_success else R.string.excel_export_failure)
            }
        }
    }
    val importSheetLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            working = true
            scope.launch {
                val sheet = withContext(Dispatchers.IO) { SpreadsheetImport.parseFile(context, uri) }
                working = false
                resultMessage = if (sheet != null) null else context.getString(R.string.import_read_failure)
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
            stringResource(R.string.backup_intro),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { exportLauncher.launch("macollectionwcf-${System.currentTimeMillis()}.zip") },
            enabled = !working,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.backup_save_button)) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/zip")) },
            enabled = !working,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.backup_restore_button)) }

        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.excel_export_intro),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                if (isPremium) excelExportLauncher.launch("macollectionwcf-${System.currentTimeMillis()}.xls")
                else onOpenPremium()
            },
            enabled = !working,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(if (isPremium) R.string.excel_export_button else R.string.excel_export_locked)) }

        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.sheet_import_intro),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { importSheetLauncher.launch(arrayOf("*/*")) },
            enabled = !working,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.sheet_import_button)) }

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
            title = { Text(stringResource(R.string.backup_restore_button)) },
            text = { Text(stringResource(R.string.backup_restore_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestoreUri = null
                    working = true
                    scope.launch {
                        val ok = vm.importBackup(uri)
                        working = false
                        resultMessage = context.getString(if (ok) R.string.backup_restore_success else R.string.backup_restore_failure)
                    }
                }) { Text(stringResource(R.string.restore)) }
            },
            dismissButton = { TextButton(onClick = { confirmRestoreUri = null }) { Text(stringResource(R.string.cancel)) } }
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
                resultMessage = context.getString(R.string.import_success_count, selected.size)
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
        SpreadsheetImport.ImportField.CHARACTER to stringResource(R.string.field_character),
        SpreadsheetImport.ImportField.NAME to stringResource(R.string.field_commercial_name),
        SpreadsheetImport.ImportField.LICENCE to stringResource(R.string.licence_label),
        SpreadsheetImport.ImportField.SERIES to stringResource(R.string.field_series),
        SpreadsheetImport.ImportField.MANUFACTURER to stringResource(R.string.field_manufacturer),
        SpreadsheetImport.ImportField.CONDITION to stringResource(R.string.field_condition),
        SpreadsheetImport.ImportField.HAS_BOX to stringResource(R.string.field_has_box),
        SpreadsheetImport.ImportField.HAS_ACCESSORIES to stringResource(R.string.field_has_accessories),
        SpreadsheetImport.ImportField.YEAR to stringResource(R.string.field_year),
        SpreadsheetImport.ImportField.HEIGHT to stringResource(R.string.field_height),
        SpreadsheetImport.ImportField.PRICE to stringResource(R.string.field_price),
        SpreadsheetImport.ImportField.BARCODE to stringResource(R.string.field_barcode),
        SpreadsheetImport.ImportField.DESCRIPTION to stringResource(R.string.field_description)
    )
    val noColumnLabel = stringResource(R.string.no_column)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.match_columns_title)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.match_columns_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                SpreadsheetImport.ImportField.values().forEach { field ->
                    LabeledDropdown(
                        label = fieldLabels[field].orEmpty(),
                        options = columnOptions,
                        selected = mapping[field],
                        optionLabel = { idx -> if (idx == null) noColumnLabel else headers.getOrElse(idx) { "?" } },
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
            ) { Text(stringResource(R.string.next)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
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
        title = { Text(stringResource(R.string.batch_scan_title, items.size)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.batch_scan_selected_count, selectedCount, items.size),
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
            ) { Text(stringResource(R.string.import_action)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
