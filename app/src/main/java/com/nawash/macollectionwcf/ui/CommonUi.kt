@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.nawash.macollectionwcf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nawash.macollectionwcf.data.Condition
import com.nawash.macollectionwcf.data.AppPrefs
import com.nawash.macollectionwcf.data.Licence
import com.nawash.macollectionwcf.ui.theme.CardGradient
import com.nawash.macollectionwcf.ui.theme.NeonBorder
import com.nawash.macollectionwcf.ui.theme.NeonCyan
import com.nawash.macollectionwcf.ui.theme.NeonPurple
import com.nawash.macollectionwcf.ui.theme.themedGradient
import java.util.Locale
import kotlin.math.roundToInt

/** Fond plein écran de l'app : image perso choisie par l'utilisateur, sinon dégradé du thème actif. */
@Composable
fun GamerScreenBackground(content: @Composable () -> Unit) {
    val customBg by AppPrefs.backgroundImageUri
    Box(Modifier.fillMaxSize()) {
        if (customBg != null) {
            coil.compose.AsyncImage(
                model = customBg,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
        } else {
            Box(Modifier.fillMaxSize().background(themedGradient()))
        }
        content()
    }
}

@Composable
fun GamerCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    var m = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(CardGradient)
        .border(1.dp, NeonBorder, shape)
    if (onClick != null) m = m.clickable { onClick() }
    Box(m.padding(14.dp)) { content() }
}

@Composable
fun FormScaffold(
    title: String,
    onCancel: () -> Unit,
    content: @Composable () -> Unit
) {
    GamerScreenBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { p ->
            Column(
                Modifier
                    .padding(p)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                content()
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun Field(
    value: String,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = checked, onCheckedChange = onChange)
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White)
    }
}

@Composable
fun NeonChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NeonPurple,
            selectedLabelColor = Color.White
        )
    )
}

fun conditionColor(c: Condition): Color = when (c) {
    Condition.HS -> Color(0xFFFF5252)
    Condition.MAUVAIS -> Color(0xFFFF8A4C)
    Condition.BON -> Color(0xFFFFD54F)
    Condition.TRES_BON -> Color(0xFF9CCC65)
    Condition.MINT -> Color(0xFF4DD0E1)
    Condition.NEUF -> Color(0xFF69F0AE)
}

@Composable
fun ConditionBadge(condition: Condition) {
    val color = conditionColor(condition)
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(condition.label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TinyTag(label: String, filled: Boolean = true) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (filled) NeonCyan.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f))
            .let { if (filled) it.border(1.dp, NeonCyan.copy(alpha = 0.5f), shape) else it }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            color = if (filled) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = if (filled) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Barre latérale fine indiquant la position de défilement dans une LazyColumn (proportion de la
 * liste déjà parcourue) — utile sur les longues listes (Collection, Souhaits, Encyclo). Invisible
 * si tout tient déjà à l'écran (rien à faire défiler).
 */
@Composable
fun ScrollProgressBar(listState: androidx.compose.foundation.lazy.LazyListState, modifier: Modifier = Modifier) {
    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleCount = layoutInfo.visibleItemsInfo.size
    if (totalItems == 0 || visibleCount >= totalItems) return

    val maxFirstVisible = (totalItems - visibleCount).coerceAtLeast(1)
    val progress = (listState.firstVisibleItemIndex.toFloat() / maxFirstVisible).coerceIn(0f, 1f)
    val thumbFraction = (visibleCount.toFloat() / totalItems).coerceIn(0.08f, 1f)

    androidx.compose.foundation.layout.BoxWithConstraints(modifier.width(4.dp)) {
        val thumbHeight = maxHeight * thumbFraction
        val offsetY = (maxHeight - thumbHeight) * progress
        Box(
            Modifier.fillMaxSize().clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            Modifier.width(4.dp).height(thumbHeight)
                .offset(y = offsetY)
                .clip(RoundedCornerShape(2.dp))
                .background(NeonCyan.copy(alpha = 0.85f))
        )
    }
}

/** Liste de choix « gamer » : encadré arrondi au dégradé de carte + contour néon. */
@Composable
fun <T> ThemedChoiceDropdown(
    leading: String,
    selectedLabel: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        Row(
            Modifier
                .menuAnchor()
                .fillMaxWidth()
                .clip(shape)
                .background(CardGradient)
                .border(1.5.dp, NeonBorder, shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading.isNotBlank()) {
                Text("$leading : ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                selectedLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            ExposedDropdownMenuDefaults.TrailingIcon(expanded)
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(optionLabel(opt)) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}

@Composable
fun ThemedSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardGradient)
            .border(1.5.dp, NeonBorder, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun <T> LabeledDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(optionLabel(o)) }, onClick = { onSelect(o); expanded = false })
            }
        }
    }
}

@Composable
fun GalleryThumb(uri: String, onRemove: () -> Unit) {
    Box(Modifier.size(84.dp)) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(84.dp).clip(RoundedCornerShape(14.dp))
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(22.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(50))
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

/** Photo plein écran avec zoom (pincement) et déplacement une fois zoomée. */
@Composable
fun ZoomableImage(uri: String, modifier: Modifier = Modifier) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 6f)
        scale = newScale
        offset = if (newScale <= 1f) Offset.Zero else offset + panChange
    }
    AsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
            .transformable(state)
    )
}

fun licenceEmoji(l: Licence): String = when (l) {
    Licence.ONE_PIECE -> "🏴‍☠️"
    Licence.BLEACH -> "⚔️"
    Licence.DRAGON_BALL -> "🐉"
    Licence.NARUTO -> "🍥"
    Licence.DEMON_SLAYER -> "🔥"
    Licence.JUJUTSU_KAISEN -> "👹"
    Licence.MY_HERO_ACADEMIA -> "💥"
    Licence.DISNEY -> "🏰"
    Licence.MARVEL -> "🦸"
    Licence.AUTRE -> "🎎"
}

// --- Prix : stockage toujours en euros. La conversion multi-devises arrive en Phase 2. ---

fun formatPrice(cents: Int?): String =
    if (cents == null || cents == 0) "—"
    else String.format(Locale.FRANCE, "%.2f €", cents / 100.0)

fun centsToText(cents: Int): String = String.format(Locale.FRANCE, "%.2f", cents / 100.0)

fun parsePriceToCents(text: String): Int? {
    val cleaned = text.replace(",", ".").trim()
    if (cleaned.isEmpty()) return null
    val value = cleaned.toDoubleOrNull() ?: return null
    return (value * 100).roundToInt()
}
