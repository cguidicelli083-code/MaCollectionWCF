package com.nawash.macollectionwcf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nawash.macollectionwcf.data.Licence
import com.nawash.macollectionwcf.ui.theme.CardGradient
import com.nawash.macollectionwcf.ui.theme.NeonBorder
import com.nawash.macollectionwcf.ui.theme.NeonCyan
import com.nawash.macollectionwcf.ui.theme.NeonPurple

/** Critères de tri proposés pour la complétion par gamme de l'onglet Total. */
private enum class CompletionSort(val label: String) {
    PERCENT_DESC("% (élevé → bas)"),
    PERCENT_ASC("% (bas → élevé)")
}

@Composable
fun TotalScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val total by vm.totalCents.collectAsState()
    val items by vm.allOwnedItems.collectAsState()
    val completion by vm.seriesCompletion.collectAsState()
    var completionLicenceFilter by remember { mutableStateOf<Licence?>(null) }
    var completionSort by remember { mutableStateOf(CompletionSort.PERCENT_DESC) }
    val filteredCompletion = remember(completion, completionLicenceFilter, completionSort) {
        completion
            .filter { completionLicenceFilter == null || it.licence == completionLicenceFilter }
            .let { list ->
                when (completionSort) {
                    CompletionSort.PERCENT_DESC -> list.sortedByDescending { it.percent }
                    CompletionSort.PERCENT_ASC -> list.sortedBy { it.percent }
                }
            }
    }

    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CardGradient)
                .border(1.5.dp, NeonBorder, RoundedCornerShape(22.dp))
                .padding(vertical = 16.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Valeur de la collection",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(formatPrice(total), fontSize = 30.sp, fontWeight = FontWeight.Black, color = NeonCyan)
                Spacer(Modifier.height(4.dp))
                Text("${items.size} figurine(s)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
        }
        if (completion.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Complétion par gamme",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            LicenceFilterDropdown(completionLicenceFilter) { completionLicenceFilter = it }
            Spacer(Modifier.height(8.dp))
            ThemedChoiceDropdown(
                leading = "Trier",
                selectedLabel = completionSort.label,
                options = CompletionSort.entries.toList(),
                optionLabel = { it.label },
                onSelect = { completionSort = it }
            )
            Spacer(Modifier.height(10.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredCompletion, key = { it.licence.name + it.series }) { SeriesCompletionRow(it) }
                }
            }
        } else {
            Spacer(Modifier.height(20.dp))
            Text(
                "La complétion par gamme s'affichera dès que le catalogue Encyclo aura des vagues WCF référencées.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SeriesCompletionRow(completion: SeriesCompletion) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, NeonPurple.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 14.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                "${licenceEmoji(completion.licence)} ${completion.series}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${completion.owned}/${completion.total} — ${completion.percent}%",
                style = MaterialTheme.typography.bodySmall,
                color = NeonCyan
            )
        }
    }
}
