package com.nawash.macollectionwcf.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nawash.macollectionwcf.data.Licence
import com.nawash.macollectionwcf.data.WcfNewsEntry
import com.nawash.macollectionwcf.ui.theme.NeonCyan
import com.nawash.macollectionwcf.ui.theme.NeonPurple
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

/**
 * Onglet Actu : nouveautés WCF annoncées sur bsp-prize.jp, récupérées par le scraper
 * (`scripts/scrape_wcf_news.py`, exécuté chaque nuit par GitHub Actions) — voir [NewsRepository][
 * com.nawash.macollectionwcf.data.NewsRepository]. Lecture seule, aucun lien avec la collection.
 */
@Composable
fun ActuScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val news by vm.wcfNews.collectAsState()

    if (news.isEmpty()) {
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

    var licenceFilter by remember { mutableStateOf<Licence?>(null) }
    val availableLicences = remember(news) {
        news.map { it.licence }.distinct().sortedBy { it.label }
    }
    val filtered = remember(news, licenceFilter) {
        if (licenceFilter == null) news else news.filter { it.licence == licenceFilter }
    }

    Column(modifier.fillMaxSize()) {
        ThemedChoiceDropdown(
            leading = "Licence",
            selectedLabel = licenceFilter?.let { "${licenceEmoji(it)} ${it.label}" } ?: "Toutes les licences",
            options = listOf(null) + availableLicences,
            optionLabel = { it?.let { l -> "${licenceEmoji(l)} ${l.label}" } ?: "Toutes les licences" },
            onSelect = { licenceFilter = it },
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        )
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Aucune actu pour cette licence pour le moment.",
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
                items(filtered, key = { it.id }) { entry -> NewsCard(entry) }
            }
        }
    }
}

@Composable
private fun NewsCard(entry: WcfNewsEntry) {
    val loc = entry.localized()
    GamerCard {
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
            }
        }
    }
}
