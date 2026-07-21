package com.nawash.macollectionwcf.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Bandeau publicitaire léger (format standard 320x50, le plus petit format banner Google —
 * volontairement pas le format adaptatif plus grand) affiché en bas de chaque écran principal
 * (voir `bottomBar` dans `MainActivity`, au-dessus de la barre de navigation). C'est le seul
 * format pub "passif" de l'app — l'interstitiel de l'onglet Total est le seul qui s'ouvre seul.
 */
@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdIds.BANNER
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
