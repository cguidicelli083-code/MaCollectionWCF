@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.nawash.macollectionwcf.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.nawash.macollectionwcf.data.CollectionItem
import com.nawash.macollectionwcf.ui.theme.CardGradient
import com.nawash.macollectionwcf.ui.theme.NeonBorder
import com.nawash.macollectionwcf.ui.theme.NeonCyan
import com.nawash.macollectionwcf.ui.theme.NeonPink
import com.nawash.macollectionwcf.ui.theme.NeonPurple

@Composable
fun ItemDetailScreen(
    vm: AppViewModel,
    item: CollectionItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val galleryPhotos by vm.photosFor(item.id).collectAsState(initial = emptyList())
    var fullscreenUri by remember { mutableStateOf<String?>(null) }
    var editingPrice by remember { mutableStateOf(false) }
    val context = LocalContext.current

    GamerScreenBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(item.character.ifBlank { "Figurine sans nom" }, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
                )
            }
        ) { p ->
            Column(
                Modifier.padding(p).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedButton(
                    onClick = {
                        val query = listOfNotNull(
                            item.character.ifBlank { null },
                            item.series?.ifBlank { null }
                        ).joinToString(" ")
                        val url = "https://www.ebay.fr/sch/i.html?_nkw=" + Uri.encode(query) + "&LH_Sold=1&LH_Complete=1"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        // L'app ne peut pas lire le navigateur externe : on relance en parallèle le
                        // même calcul eBay+IA utilisé partout ailleurs, la fiche se met à jour seule.
                        vm.recheckPrice(item)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🔍 Rechercher sur eBay (ventes réussies)") }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(240.dp).clip(RoundedCornerShape(48.dp))
                            .background(Brush.radialGradient(listOf(NeonPurple.copy(alpha = 0.55f), Color.Transparent)))
                    )
                    if (item.imageUri != null) {
                        AsyncImage(
                            model = item.imageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(220.dp).clickable { fullscreenUri = item.imageUri }
                        )
                    } else {
                        Text(licenceEmoji(item.licence), fontSize = 96.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))

                Text(
                    listOfNotNull(item.licence.label, item.series?.ifBlank { null }, item.manufacturer.ifBlank { null }, item.releaseYear?.toString())
                        .joinToString(" • "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ConditionBadge(item.condition)
                    Spacer(Modifier.width(6.dp)); TinyTag("Boîte", filled = item.hasBox)
                    Spacer(Modifier.width(6.dp)); TinyTag("Accessoires", filled = item.hasAccessories)
                }
                item.heightCm?.let {
                    Spacer(Modifier.height(6.dp))
                    Text("Taille : ${it} cm", color = Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    formatPrice(item.priceCents) + if (item.priceIsAiEstimate) " (IA)" else "",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonCyan
                )
                item.info?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { editingPrice = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Modifier le prix")
                }
                if (item.isWishlist) {
                    Button(
                        onClick = { vm.saveCollectionItem(item.copy(isWishlist = false)); onBack() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Passer en collection") }
                }

                item.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(CardGradient).border(1.dp, NeonBorder, RoundedCornerShape(16.dp)).padding(14.dp)
                    ) {
                        Text(it, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (galleryPhotos.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Text("Galerie (${galleryPhotos.size})", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(galleryPhotos, key = { it.id }) { photo ->
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, NeonBorder, RoundedCornerShape(14.dp))
                                    .clickable { fullscreenUri = photo.uri }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(6.dp)); Text("Modifier")
                    }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Delete, null, tint = NeonPink); Spacer(Modifier.width(6.dp)); Text("Supprimer")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

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

    if (editingPrice) {
        var priceText by remember { mutableStateOf(item.priceCents?.let { centsToText(it) } ?: "") }
        AlertDialog(
            onDismissRequest = { editingPrice = false },
            title = { Text("Modifier le prix") },
            text = {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Prix (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val cents = parsePriceToCents(priceText)
                    vm.saveCollectionItem(item.copy(priceCents = cents, priceIsManual = cents != null, priceIsAiEstimate = false))
                    editingPrice = false
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { editingPrice = false }) { Text("Annuler") } }
        )
    }
}
