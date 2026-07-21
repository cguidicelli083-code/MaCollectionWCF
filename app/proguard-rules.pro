# Règles ProGuard/R8 du projet.
#
# Certaines classes sont sérialisées/désérialisées par Gson (sauvegarde .zip, réponses eBay/
# Gemini/Groq/Tavily) : Gson les construit par réflexion (sans passer par leur constructeur
# Kotlin), donc R8 ne voit jamais de site de construction "réel" pour elles et peut, avec les
# optimisations agressives (proguard-android-optimize.txt), purement et simplement SUPPRIMER
# leurs champs (propagation de valeurs) malgré une règle -keepclassmembers — piège déjà rencontré
# sur MaCollection (a cassé la restauration de sauvegarde). Il faut donc un -keep complet (pas
# seulement -keepclassmembers) sur ces classes précises, à ajouter au fur et à mesure qu'elles
# sont créées (entités Room dès la phase 1, DTO réseau dès la phase 4).

-keepattributes Signature
-keepattributes *Annotation*

# Entités Room sérialisées par Gson dans la sauvegarde .zip (BackupManager).
-keep class com.nawash.macollectionwcf.data.CollectionItem { *; }
-keep class com.nawash.macollectionwcf.data.PriceHistory { *; }
-keep class com.nawash.macollectionwcf.data.ItemPhoto { *; }
-keep class com.nawash.macollectionwcf.data.CustomFigurePreset { *; }
-keep class com.nawash.macollectionwcf.data.PresetPhotoOverride { *; }
-keep class com.nawash.macollectionwcf.data.BackupPayload { *; }

# Taux de change (CurrencyRates.kt).
-keep class com.nawash.macollectionwcf.data.RatesResponse { *; }

# eBay (EbayPrices.kt).
-keep class com.nawash.macollectionwcf.data.EbayToken { *; }
-keep class com.nawash.macollectionwcf.data.EbaySearch { *; }
-keep class com.nawash.macollectionwcf.data.EbayItem { *; }
-keep class com.nawash.macollectionwcf.data.EbayPrice { *; }
-keep class com.nawash.macollectionwcf.data.EbayImage { *; }

# Gemini (identification visuelle + estimation de cote par IA, GeminiVision.kt).
-keep class com.nawash.macollectionwcf.data.GemPart { *; }
-keep class com.nawash.macollectionwcf.data.GemInlineData { *; }
-keep class com.nawash.macollectionwcf.data.GemContent { *; }
-keep class com.nawash.macollectionwcf.data.GemRequest { *; }
-keep class com.nawash.macollectionwcf.data.GemCandidate { *; }
-keep class com.nawash.macollectionwcf.data.GemResponse { *; }
-keep class com.nawash.macollectionwcf.data.GemPriceEstimate { *; }
-keep class com.nawash.macollectionwcf.data.GemFigureResult { *; }
-keep class com.nawash.macollectionwcf.data.GemBatchEntry { *; }

# Groq (identification visuelle de secours + extraction de prix, GroqVision.kt).
-keep class com.nawash.macollectionwcf.data.GqImageUrl { *; }
-keep class com.nawash.macollectionwcf.data.GqContent { *; }
-keep class com.nawash.macollectionwcf.data.GqMessage { *; }
-keep class com.nawash.macollectionwcf.data.GqRequest { *; }
-keep class com.nawash.macollectionwcf.data.GqRespMsg { *; }
-keep class com.nawash.macollectionwcf.data.GqChoice { *; }
-keep class com.nawash.macollectionwcf.data.GqResponse { *; }

# Tavily (recherche web de secours, TavilyPriceEstimate.kt).
-keep class com.nawash.macollectionwcf.data.TavilySearchRequest { *; }
-keep class com.nawash.macollectionwcf.data.TavilyResult { *; }
-keep class com.nawash.macollectionwcf.data.TavilySearchResponse { *; }

# Les enums (Licence, Condition) sont aussi lus par Gson via leur nom (CollectionItem en
# contient) : protégés par précaution.
-keepclassmembers enum com.nawash.macollectionwcf.data.** { *; }
