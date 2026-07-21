package com.nawash.macollectionwcf.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nawash.macollectionwcf.data.Licence
import com.nawash.macollectionwcf.data.WcfNewsEntry
import com.nawash.macollectionwcf.ui.theme.NeonCyan
import com.nawash.macollectionwcf.ui.theme.NeonPurple
import java.util.Calendar
import java.util.Locale

/** Traduction d'un item pour une langue (voir `translate_item()` dans `scripts/scrape_wcf_news.py`). */
private data class NewsTranslation(
    val series: String?,
    val characters: List<String>?,
    val releaseDate: String?,
    val price: String?
)

private val newsGson = Gson()
private val translationsMapType = object : TypeToken<Map<String, NewsTranslation>>() {}.type

/** Mêmes 11 langues que MaCollection (retrogaming) et que le scraper WCF. */
private val SUPPORTED_LANGS = setOf("fr", "en", "es", "it", "de", "pt", "ru", "el", "tr", "zh", "ja")

/** Année*12 + mois du moment présent (repère pour ne garder que les sorties à venir). */
private fun currentYearMonth(): Int {
    val c = Calendar.getInstance()
    return c.get(Calendar.YEAR) * 12 + (c.get(Calendar.MONTH) + 1)
}

private val jpYearMonth = Regex("""(\d{4})\s*年\s*(\d{1,2})\s*月""")

/**
 * Extrait année*12+mois d'une date de sortie brute japonaise (ex. "2026年11月発売" ->
 * 2026*12+11), ou `null` si non reconnue. Sert à masquer les annonces déjà passées : une
 * « actu » doit rester une sortie à venir (voir [ActuScreen]).
 */
private fun releaseYearMonth(raw: String): Int? {
    val m = jpYearMonth.find(raw) ?: return null
    val year = m.groupValues[1].toIntOrNull() ?: return null
    val month = m.groupValues[2].toIntOrNull() ?: return null
    if (month !in 1..12) return null
    return year * 12 + month
}

/**
 * Choisit la traduction correspondant à la langue de l'appareil (repli sur "en" puis sur le
 * japonais original si la langue de l'appareil n'est pas couverte ou si le JSON est absent).
 */
private fun WcfNewsEntry.localized(): NewsTranslation {
    val fallback = NewsTranslation(series, characters.split("|").filter { it.isNotBlank() }, releaseDateRaw, priceRaw)
    if (translationsJson.isBlank()) return fallback
    val translations = try {
        newsGson.fromJson<Map<String, NewsTranslation>>(translationsJson, translationsMapType) ?: emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }
    val deviceLang = Locale.getDefault().language.lowercase().takeIf { it in SUPPORTED_LANGS } ?: "en"
    return translations[deviceLang] ?: translations["en"] ?: translations["ja"] ?: fallback
}

/** Bascule d'affichage de l'onglet Actu (voir [ActuScreen]). */
private enum class ActuTimeFilter { UPCOMING, PAST }

/**
 * Onglet Actu : nouveautés WCF annoncées sur bsp-prize.jp, récupérées par le scraper
 * (`scripts/scrape_wcf_news.py`, exécuté chaque nuit par GitHub Actions) — voir [NewsRepository][
 * com.nawash.macollectionwcf.data.NewsRepository]. Lecture seule, aucun lien avec la collection.
 *
 * Le cache Room accumule les items au fil des synchronisations, y compris d'anciennes sorties
 * déjà passées : plutôt que de les cacher purement et simplement, une bascule « À venir » /
 * « Anciennes » les sépare clairement (les dates non reconnues sont classées en « À venir » par
 * prudence, pour ne jamais masquer une vraie annonce faute de format de date compris).
 */
@Composable
fun ActuScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val allNews by vm.wcfNews.collectAsState()

    if (allNews.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Aucune actu pour le moment", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Les nouveautés WCF apparaîtront ici après la prochaine synchronisation.",
                    fontSize = 13.sp,
                    color = Color(0xFFB5B5CC)
                )
            }
        }
        return
    }

    val nowYm = remember { currentYearMonth() }
    var timeFilter by remember { mutableStateOf(ActuTimeFilter.UPCOMING) }
    val news = remember(allNews, nowYm, timeFilter) {
        allNews.filter { entry ->
            val ym = releaseYearMonth(entry.releaseDateRaw)
            when (timeFilter) {
                ActuTimeFilter.UPCOMING -> ym == null || ym >= nowYm
                ActuTimeFilter.PAST -> ym != null && ym < nowYm
            }
        }
    }

    var licenceFilter by remember { mutableStateOf<Licence?>(null) }
    var opened by remember { mutableStateOf<WcfNewsEntry?>(null) }
    val availableLicences = remember(news) {
        news.map { it.licence }.distinct().sortedBy { it.label }
    }
    val filtered = remember(news, licenceFilter) {
        if (licenceFilter == null) news else news.filter { it.licence == licenceFilter }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeonChip("À venir", timeFilter == ActuTimeFilter.UPCOMING) { timeFilter = ActuTimeFilter.UPCOMING }
            NeonChip("Anciennes actus", timeFilter == ActuTimeFilter.PAST) { timeFilter = ActuTimeFilter.PAST }
        }
        ThemedChoiceDropdown(
            leading = "Licence",
            selectedLabel = licenceFilter?.let { "${licenceEmoji(it)} ${it.label}" } ?: "Toutes les licences",
            options = listOf(null) + availableLicences,
            optionLabel = { it?.let { l -> "${licenceEmoji(l)} ${l.label}" } ?: "Toutes les licences" },
            onSelect = { licenceFilter = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (timeFilter == ActuTimeFilter.UPCOMING) "Aucune actu à venir pour le moment."
                    else "Aucune ancienne actu pour cette licence.",
                    fontSize = 13.sp,
                    color = Color(0xFFB5B5CC),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { entry -> NewsCard(entry) { opened = entry } }
            }
        }
    }

    opened?.let { entry -> NewsDetailDialog(entry) { opened = null } }
}

@Composable
private fun NewsCard(entry: WcfNewsEntry, onClick: () -> Unit) {
    val loc = entry.localized()
    GamerCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            if (entry.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = entry.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E1E2C))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.fillMaxWidth()) {
                Text(loc.series ?: entry.series, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Text(entry.series, fontSize = 11.sp, color = Color(0xFF7A7A96))
                val releaseDate = loc.releaseDate ?: entry.releaseDateRaw
                if (releaseDate.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(releaseDate, fontSize = 12.sp, color = NeonCyan)
                }
                val price = loc.price ?: entry.priceRaw
                if (price.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(price, fontSize = 12.sp, color = NeonPurple)
                }
                val characters = loc.characters ?: entry.characters.split("|").filter { it.isNotBlank() }
                if (characters.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        characters.joinToString("・"),
                        fontSize = 12.sp,
                        color = Color(0xFFB5B5CC),
                        maxLines = 3
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text("Appuie pour lire / agrandir la photo", fontSize = 11.sp, color = NeonCyan)
            }
        }
    }
}

/**
 * Fiche complète d'une actu : grande photo (tap = plein écran zoomable), infos traduites et lien
 * vers la fiche officielle bsp-prize.jp (ouvre le navigateur).
 */
@Composable
private fun NewsDetailDialog(entry: WcfNewsEntry, onDismiss: () -> Unit) {
    val loc = entry.localized()
    val context = LocalContext.current
    var fullscreen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(loc.series ?: entry.series, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (entry.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = entry.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E1E2C))
                            .clickable { fullscreen = true }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Appuie sur la photo pour l’agrandir", fontSize = 11.sp, color = Color(0xFF7A7A96))
                    Spacer(Modifier.height(8.dp))
                }
                Text(entry.series, fontSize = 11.sp, color = Color(0xFF7A7A96))
                val releaseDate = loc.releaseDate ?: entry.releaseDateRaw
                if (releaseDate.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(releaseDate, fontSize = 13.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
                val price = loc.price ?: entry.priceRaw
                if (price.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(price, fontSize = 13.sp, color = NeonPurple)
                }
                val characters = loc.characters ?: entry.characters.split("|").filter { it.isNotBlank() }
                if (characters.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Figurines", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(characters.joinToString("・"), fontSize = 13.sp, color = Color(0xFFB5B5CC))
                }
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth()) {
                if (entry.itemUrl.isNotBlank()) {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.itemUrl)))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Voir la fiche officielle") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Fermer") }
                }
            }
        }
    )

    if (fullscreen && entry.imageUrl.isNotBlank()) {
        Dialog(onDismissRequest = { fullscreen = false }) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                ZoomableImage(uri = entry.imageUrl, modifier = Modifier.fillMaxWidth().padding(16.dp))
                IconButton(onClick = { fullscreen = false }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Color.White)
                }
            }
        }
    }
}
