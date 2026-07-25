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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nawash.macollectionwcf.R
import com.nawash.macollectionwcf.ui.theme.NeonCyan
import kotlinx.coroutines.launch

private data class OnboardingPage(val titleRes: Int, val textRes: Int)

private val onboardingPages = listOf(
    OnboardingPage(R.string.onboarding_page1_title, R.string.onboarding_page1_text),
    OnboardingPage(R.string.onboarding_page2_title, R.string.onboarding_page2_text),
    OnboardingPage(R.string.onboarding_page3_title, R.string.onboarding_page3_text),
    OnboardingPage(R.string.onboarding_page4_title, R.string.onboarding_page4_text),
    OnboardingPage(R.string.onboarding_page5_title, R.string.onboarding_page5_text),
    OnboardingPage(R.string.onboarding_page6_title, R.string.onboarding_page6_text)
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
                        stringResource(onboardingPages[page].titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(onboardingPages[page].textRes),
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
                TextButton(onClick = onFinish) { Text(stringResource(R.string.onboarding_skip)) }
                Button(onClick = {
                    if (pagerState.currentPage < onboardingPages.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                }) {
                    Text(stringResource(if (pagerState.currentPage < onboardingPages.lastIndex) R.string.onboarding_next else R.string.onboarding_start))
                }
            }
        }
    }
}
