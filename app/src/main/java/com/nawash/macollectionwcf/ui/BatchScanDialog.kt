package com.nawash.macollectionwcf.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nawash.macollectionwcf.data.GeminiVision

/** Aperçu (cases à cocher) des figurines détectées sur une photo de lot, avant ajout en masse. */
@Composable
fun BatchScanDialog(
    items: List<GeminiVision.BatchItem>,
    onConfirm: (List<GeminiVision.BatchItem>) -> Unit,
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
                                Text(
                                    listOfNotNull(item.licence.label, item.series?.ifBlank { null }).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
            ) { Text("Valider l'ajout au lot") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
