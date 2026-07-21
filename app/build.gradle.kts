import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Lit les clés API depuis local.properties (non versionné).
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val ebayClientId: String = localProps.getProperty("ebayClientId") ?: ""
val ebayClientSecret: String = localProps.getProperty("ebayClientSecret") ?: ""
val geminiApiKey: String = localProps.getProperty("geminiApiKey") ?: ""
val groqApiKey: String = localProps.getProperty("groqApiKey") ?: ""
val tavilyApiKey: String = localProps.getProperty("tavilyApiKey") ?: ""

android {
    namespace = "com.nawash.macollectionwcf"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nawash.macollectionwcf"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "EBAY_CLIENT_ID", "\"$ebayClientId\"")
        buildConfigField("String", "EBAY_CLIENT_SECRET", "\"$ebayClientSecret\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")
        buildConfigField("String", "TAVILY_API_KEY", "\"$tavilyApiKey\"")

        // N'embarque que l'architecture ARM 64 (tous les téléphones modernes) -> APK plus léger.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        // Signature de production (Play Store) : clé dans un keystore hors du dépôt, chemin et
        // mots de passe lus depuis local.properties (jamais versionné). Absent tant que ces
        // 3 propriétés ne sont pas renseignées : les builds debug restent possibles sans keystore.
        val releaseStorePath = localProps.getProperty("releaseKeystorePath")
        if (!releaseStorePath.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseStorePath)
                storePassword = localProps.getProperty("releaseKeystorePassword")
                keyAlias = localProps.getProperty("releaseKeyAlias")
                keyPassword = localProps.getProperty("releaseKeystorePassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        // Pas de raison d'obfusquer le debug ici : contrairement à MaCollection, cette app n'a
        // ni gamification/boutique/billing à protéger d'un testeur externe.
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Scan par photo : code-barres (caméra + image) et OCR (lecture du titre sur la boîte).
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // Modèle dédié pour le japonais (kanji/hiragana/katakana) : les boîtes de figurines WCF
    // portent souvent du texte japonais que le modèle latin par défaut ne lit pas.
    implementation("com.google.mlkit:text-recognition-japanese:16.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Argus (eBay) + estimation IA de secours (Gemini/Groq/Tavily).
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Recadrage de photo après sélection/prise (figurine, boîte...).
    implementation("com.github.CanHub:Android-Image-Cropper:4.5.0")

    // Publicité (bandeaux + interstitiel Total) + consentement RGPD (obligatoire en UE).
    implementation("com.google.android.gms:play-services-ads:23.3.0")
    implementation("com.google.android.ump:user-messaging-platform:2.2.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
