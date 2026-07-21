package com.nawash.macollectionwcf.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/** Préférences simples de l'application, persistées dans les SharedPreferences. */
object AppPrefs {
    private const val PREFS = "macollectionwcf_prefs"
    private const val KEY_LANG = "lang"
    private const val KEY_ONBOARDING_SEEN = "onboardingSeen"
    private const val KEY_BACKGROUND_URI = "backgroundUri"
    private const val KEY_CURRENCY = "currency"
    private const val KEY_CURRENCY_MANUAL = "currencyManual"
    private const val KEY_RATES = "currencyRates"
    private const val KEY_THEME = "selectedTheme"
    private const val KEY_LAST_BACKUP_URI = "lastBackupUri"
    private const val KEY_CATALOG_PHOTO_REFINEMENT_V1 = "catalogPhotoRefinementV1"

    /**
     * Image de fond personnalisée (URI "file://..."), ou null pour le dégradé par défaut.
     * État Compose pour que l'arrière-plan se mette à jour immédiatement partout dans l'app.
     */
    val backgroundImageUri = mutableStateOf<String?>(null)

    /** Devise affichée/éditée (la cote reste stockée en euros : conversion à l'affichage uniquement). */
    val currency = mutableStateOf("EUR")

    /** Derniers taux de change connus (base EUR), mis à jour au lancement si le réseau répond. */
    val currencyRates = mutableStateOf<Map<String, Double>>(emptyMap())

    /** true dès que l'utilisateur a choisi sa devise lui-même : un changement de langue ne doit
     * alors plus jamais l'écraser silencieusement. */
    @Volatile
    private var currencyManuallySet: Boolean = false

    /** Code langue courant ("fr", "en"...). Par défaut français. */
    @Volatile
    var language: String = "fr"
        private set

    /** true une fois que le tutoriel de premier lancement a été vu (ou passé). */
    @Volatile
    var onboardingSeen: Boolean = false
        private set

    /**
     * Thème visuel appliqué (id : "default" ou une licence à partir de la Phase 3). État Compose
     * pour que tout le thème de l'app change immédiatement quand on le sélectionne.
     */
    val selectedTheme = mutableStateOf("default")

    /** URI de la dernière sauvegarde .zip exportée, pour rouvrir l'import dans le même dossier. */
    val lastBackupUri = mutableStateOf<String?>(null)

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        language = p.getString(KEY_LANG, "fr") ?: "fr"
        onboardingSeen = p.getBoolean(KEY_ONBOARDING_SEEN, false)
        backgroundImageUri.value = p.getString(KEY_BACKGROUND_URI, null)
        currencyManuallySet = p.getBoolean(KEY_CURRENCY_MANUAL, false)
        currency.value = p.getString(KEY_CURRENCY, null) ?: CurrencyOptions.defaultForLanguage(language)
        currencyRates.value = p.getString(KEY_RATES, null)?.let { raw ->
            raw.split(",").mapNotNull { entry ->
                val parts = entry.split(":")
                val rate = parts.getOrNull(1)?.toDoubleOrNull()
                if (parts.size == 2 && rate != null) parts[0] to rate else null
            }.toMap()
        } ?: emptyMap()
        selectedTheme.value = p.getString(KEY_THEME, "default") ?: "default"
        lastBackupUri.value = p.getString(KEY_LAST_BACKUP_URI, null)
    }

    /** Mémorise le fichier de la dernière sauvegarde exportée (dossier initial de l'import). */
    fun setLastBackupUri(context: Context, uri: String) {
        lastBackupUri.value = uri
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LAST_BACKUP_URI, uri).apply()
    }

    /** Applique et persiste le thème choisi (paramètres → Thème, à partir de la Phase 3). */
    fun setSelectedTheme(context: Context, id: String) {
        selectedTheme.value = id
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_THEME, id).apply()
    }

    /** Choix explicite de l'utilisateur (réglages) : prime désormais sur la langue à tout jamais. */
    fun setCurrency(context: Context, code: String) {
        currency.value = code
        currencyManuallySet = true
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_CURRENCY, code).putBoolean(KEY_CURRENCY_MANUAL, true).apply()
    }

    /** Devise par défaut associée à [tag], sauf si l'utilisateur en a déjà choisi une lui-même. */
    fun applyLanguageCurrencyDefault(context: Context, tag: String) {
        if (currencyManuallySet) return
        val code = CurrencyOptions.defaultForLanguage(tag)
        currency.value = code
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_CURRENCY, code).apply()
    }

    /** Best-effort, appelé au lancement : garde les anciens taux si la requête réseau échoue. */
    fun setCurrencyRates(context: Context, rates: Map<String, Double>) {
        if (rates.isEmpty()) return
        currencyRates.value = rates
        val serialized = rates.entries.joinToString(",") { "${it.key}:${it.value}" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_RATES, serialized).apply()
    }

    /** uri = null pour revenir au fond par défaut. */
    fun setBackgroundImageUri(context: Context, uri: String?) {
        backgroundImageUri.value = uri
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_BACKGROUND_URI, uri).apply()
    }

    fun setOnboardingSeen(context: Context) {
        onboardingSeen = true
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply()
    }

    fun setLanguage(context: Context, lang: String) {
        language = lang
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LANG, lang).apply()
    }

    /**
     * true tant que le catalogue Encyclo n'a pas encore été purgé une fois de ses photos mises en
     * cache par l'ancienne requête (personnage seul, sans vague/volume) — voir `AppViewModel.init`.
     */
    fun catalogPhotoRefinementV1Done(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_CATALOG_PHOTO_REFINEMENT_V1, false)

    fun markCatalogPhotoRefinementV1Done(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_CATALOG_PHOTO_REFINEMENT_V1, true).apply()
    }
}
