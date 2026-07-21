package com.nawash.macollectionwcf.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nawash.macollectionwcf.data.CollectionItem
import com.nawash.macollectionwcf.data.Licence
import com.nawash.macollectionwcf.data.selectableLicences
import com.nawash.macollectionwcf.ui.theme.NeonCyan
import com.nawash.macollectionwcf.ui.theme.NeonPink
import com.nawash.macollectionwcf.ui.theme.NeonPurple

@Composable
fun CollectionScreen(
    vm: AppViewModel,
    onOpen: (CollectionItem) -> Unit,
    onEdit: (CollectionItem) -> Unit,
    modifier: Modifier = Modifier,
    wishlist: Boolean = false,
    listState: LazyListState = rememberLazyListState()
) {
    val allItems by (if (wishlist) vm.wishlist else vm.items).collectAsState()
    val sort by vm.sortOption.collectAsState()
    val filter by vm.licenceFilter.collectAsState()
    val seriesFilter by vm.seriesFilter.collectAsState()
    val seriesOptions by vm.availableSeries.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val items = remember(allItems, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) allItems
        else allItems.filter {
            it.character.lowercase().contains(q) || it.name.lowercase().contains(q) || (it.series?.lowercase()?.contains(q) == true)
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(8.dp))
        ThemedSearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = if (wishlist) "Rechercher dans les souhaits" else "Rechercher dans la collection"
        )
        Spacer(Modifier.height(8.dp))
        LicenceFilterDropdown(filter) { vm.setLicenceFilter(it) }
        if (seriesOptions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            ThemedChoiceDropdown(
                leading = "Vague/Volume",
                selectedLabel = seriesFilter ?: "Toutes les vagues",
                options = listOf(null) + seriesOptions,
                optionLabel = { it ?: "Toutes les vagues" },
                onSelect = { vm.setSeriesFilter(it) }
            )
        }
        Spacer(Modifier.height(8.dp))
        SortDropdown(sort) { vm.setSort(it) }
        Spacer(Modifier.height(10.dp))

        if (items.isEmpty()) {
            val message = when {
                searchQuery.isNotBlank() -> "Aucun résultat pour cette recherche."
                wishlist -> "Aucun souhait pour l'instant. Ajoute une figurine que tu vises avec le bouton +."
                else -> "Ta collection est vide. Ajoute ta première figurine avec le bouton +."
            }
            EmptyState(message)
        } else {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items, key = { it.id }) { item ->
                        CollectionCard(
                            item = item,
                            onOpen = { onOpen(item) },
                            onEdit = { onEdit(item) },
                            onDelete = { vm.deleteCollectionItem(item) }
                        )
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
}

@Composable
private fun CollectionCard(
    item: CollectionItem,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GamerCard(onClick = onOpen) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.imageUri != null) {
                    AsyncImage(
                        model = item.imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(NeonPurple, NeonCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(licenceEmoji(item.licence), fontSize = 24.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.character.ifBlank { "Figurine sans nom" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        listOfNotNull(item.licence.label, item.series?.ifBlank { null }, item.releaseYear?.toString())
                            .joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ConditionBadge(item.condition)
                        Spacer(Modifier.width(6.dp)); TinyTag("Boîte", filled = item.hasBox)
                        Spacer(Modifier.width(6.dp)); TinyTag("Accessoires", filled = item.hasAccessories)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        formatPrice(item.priceCents) + if (item.priceIsAiEstimate) " (IA)" else "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    item.description?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = NeonPurple) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = NeonPink) }
            }
        }
    }
}

@Composable
private fun sortOptionLabel(option: SortOption): String = option.label

@Composable
fun SortDropdown(current: SortOption, onSelect: (SortOption) -> Unit) {
    ThemedChoiceDropdown(
        leading = "Trier",
        selectedLabel = sortOptionLabel(current),
        options = SortOption.values().toList(),
        optionLabel = { sortOptionLabel(it) },
        onSelect = onSelect
    )
}

@Composable
private fun licenceFilterLabel(l: Licence?): String =
    if (l == null) "Toutes les licences" else "${licenceEmoji(l)} ${l.label}"

@Composable
fun LicenceFilterDropdown(current: Licence?, onSelect: (Licence?) -> Unit) {
    ThemedChoiceDropdown(
        leading = "Licence",
        selectedLabel = licenceFilterLabel(current),
        options = listOf(null) + selectableLicences,
        optionLabel = { licenceFilterLabel(it) },
        onSelect = onSelect
    )
}
