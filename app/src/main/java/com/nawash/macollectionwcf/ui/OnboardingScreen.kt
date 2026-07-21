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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nawash.macollectionwcf.ui.theme.NeonCyan
import kotlinx.coroutines.launch

private data class OnboardingPage(val title: String, val text: String)

private val onboardingPages = listOf(
    OnboardingPage(
        "Bienvenue dans Ma Collection WCF 🎎",
        "Cette appli t'aide à cataloguer ta collection de figurines World Collectable Figure (One Piece, Bleach, Dragon Ball, Naruto...), avec estimation des cotes."
    ),
    OnboardingPage(
        "📷 Ajouter une figurine",
        "Appuie sur le bouton + pour ajouter une figurine : scanne un code-barres, prends une photo, choisis-en une dans ta galerie, ou saisis tout à la main."
    ),
    OnboardingPage(
        "📚 Encyclo",
        "Consulte le catalogue des figurines et ajoute directement une fiche à ta collection ou à tes souhaits. Le catalogue intégré s'enrichit progressivement ; en attendant, ajoute tes propres fiches (avec photo) via le bouton +."
    ),
    OnboardingPage(
        "❤️ Souhaits & 💎 Total",
        "L'onglet Souhaits liste tes futures acquisitions. L'onglet Total affiche la valeur estimée de toute ta collection."
    ),
    OnboardingPage(
        "💾 Sauvegarde",
        "Sauvegarde toute ta collection (données et photos) dans un fichier que tu choisis où enregistrer, et restaure-la à tout moment — utile en cas de réinstallation ou de changement de téléphone. Dans les réglages (engrenage), tu peux aussi changer l'image de fond de toute l'application."
    ),
    OnboardingPage(
        "💡 Astuces",
        "Dans les réglages (engrenage), un onglet « Astuces » résume les conseils pour bien cadrer tes photos et bien renseigner tes figurines en saisie manuelle."
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()

    GamerScreenBackground {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        onboardingPages[page].title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        onboardingPages[page].text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                onboardingPages.indices.forEach { i ->
                    Box(
                        Modifier
                            .size(if (i == pagerState.currentPage) 10.dp else 7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (i == pagerState.currentPage) NeonCyan else Color.White.copy(alpha = 0.25f))
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onFinish) { Text("Passer") }
                Button(onClick = {
                    if (pagerState.currentPage < onboardingPages.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                }) {
                    Text(if (pagerState.currentPage < onboardingPages.lastIndex) "Suivant" else "Commencer")
                }
            }
        }
    }
}
