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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nawash.macollectionwcf.data.WcfNewsEntry
import com.nawash.macollectionwcf.ui.theme.NeonCyan
import com.nawash.macollectionwcf.ui.theme.NeonPurple

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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(news, key = { it.id }) { entry -> NewsCard(entry) }
    }
}

@Composable
private fun NewsCard(entry: WcfNewsEntry) {
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
                Text(entry.series, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                if (entry.releaseDateRaw.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(entry.releaseDateRaw, fontSize = 12.sp, color = NeonCyan)
                }
                if (entry.priceRaw.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(entry.priceRaw, fontSize = 12.sp, color = NeonPurple)
                }
                val characters = entry.characters.split("|").filter { it.isNotBlank() }
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
