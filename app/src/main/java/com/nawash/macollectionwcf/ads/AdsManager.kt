package com.nawash.macollectionwcf.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * ⚠️ IDs de TEST Google officiels — jamais de vraies impressions facturées avec ceux-ci.
 * À remplacer par les vrais ID d'unité publicitaire du compte AdMob de l'utilisateur avant toute
 * mise en ligne réelle (créer les unités "Bannière" et "Interstitiel" dans la console AdMob).
 */
object AdIds {
    const val BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
}

/**
 * Initialisation AdMob + recueil du consentement RGPD (obligatoire pour tout utilisateur en UE,
 * voir la UMP SDK de Google) — sans ce recueil, afficher des pubs personnalisées à un utilisateur
 * européen serait non conforme. Tant que le consentement n'est pas obtenu (ou refusé), les pubs
 * demandées restent non-personnalisées par défaut (comportement standard du SDK Google).
 *
 * Interstitiel : chargé à l'avance et gardé en mémoire, affiché UNIQUEMENT au clic sur l'onglet
 * Total (voir `MainActivity`) — c'est le seul endroit de l'app où une pub s'ouvre d'elle-même,
 * jamais ailleurs. Rechargé automatiquement après chaque affichage (ou échec) pour être prêt la
 * fois suivante.
 */
object AdsManager {
    private const val TAG = "AdsManager"
    private var initialized = false
    private var interstitialAd: InterstitialAd? = null

    fun init(activity: Activity) {
        val consentInfo: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) Log.w(TAG, "Formulaire de consentement : ${formError.message}")
                    startMobileAdsIfNeeded(activity)
                }
            },
            { requestError -> Log.w(TAG, "Consentement RGPD : ${requestError.message}") }
        )
        // Si le consentement n'est pas requis (ex. hors UE) ou déjà connu, initialiser quand même.
        if (consentInfo.canRequestAds()) startMobileAdsIfNeeded(activity)
    }

    private fun startMobileAdsIfNeeded(activity: Activity) {
        if (initialized) return
        initialized = true
        MobileAds.initialize(activity) {
            loadInterstitial(activity)
        }
    }

    private fun loadInterstitial(activity: Activity) {
        InterstitialAd.load(
            activity,
            AdIds.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    /** Affiche l'interstitiel s'il est prêt (silencieux sinon — ne bloque jamais la navigation). */
    fun showInterstitialOnTotal(activity: Activity) {
        val ad = interstitialAd
        if (ad == null) {
            loadInterstitial(activity)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial(activity)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadInterstitial(activity)
            }
        }
        ad.show(activity)
    }
}
