# Checklist mise en ligne Play Store — Ma Collection WCF

## ✅ Fait automatiquement (2026-07-25)
- [x] Traduction complète de l'app en 11 langues (fr, en, es, it, de, pt, ru, el, tr, ja, zh) — sélecteur de langue dans Réglages.
- [x] Tutoriel de premier lancement traduit dans les 11 langues.
- [x] Keystore de release généré : `C:\Users\Nawash\AndroidKeystores\macollectionwcf-release.jks` (valide jusqu'en 2053).
- [x] Signature configurée dans `local.properties` (gitignore, jamais commit).
- [x] Build de release testé : `app/build/outputs/bundle/release/app-release.aab` généré et signé avec succès.
- [x] R8/ProGuard actif sur le build release (déjà configuré avant cette session).
- [x] Politique de confidentialité rédigée (fr + en) : `docs/privacy-policy.html`.
- [x] Icône Play Store 512×512 générée : `docs/store/icon-512.png`.
- [x] Bannière de présentation 1024×500 générée : `docs/store/feature-graphic-1024x500.png`.
- [x] Fiche Store (titre, descriptions courte/longue fr+en) rédigée : `docs/store/fiche-play-store.md`.
- [x] Premium (achat unique, argent réel) implémenté : Google Play Billing, retire les pubs + débloque l'export Excel. Traduit dans les 11 langues.

## ⚠️ À FAIRE PAR TOI avant de publier (nécessite ton compte / des captures d'écran)

0. **Play Console — produit in-app Premium** (bloquant pour que le bouton "Passer Premium" fonctionne) :
   - Play Console → ton app → Monétiser → Produits → Produits gérés → Créer un produit.
   - ID produit (à taper EXACTEMENT, sensible à la casse) : `premium_all_access_iap`
   - Titre/description libres (ex. "Accès Premium — sans pub + export Excel"), fixe un prix.
   - Active le produit. Tant qu'il n'est pas créé et actif, le bouton "Passer Premium" affiche "Bientôt disponible" dans l'app (comportement normal et voulu, pas un bug).

1. **AdMob — IDs réels** (bloquant, actuellement en IDs de TEST Google) :
   - Crée un compte AdMob si pas déjà fait (https://admob.google.com).
   - Crée l'app dans AdMob, puis 2 unités publicitaires : une "Bannière" et une "Interstitiel".
   - Remplace dans `app/src/main/AndroidManifest.xml` (ligne ~34, `APPLICATION_ID`) et dans
     `app/src/main/java/.../ads/AdsManager.kt` (`AdIds.BANNER` / `AdIds.INTERSTITIAL`) par tes vrais IDs.
   - Sans ça, l'app tournera avec des pubs de test (aucun revenu, et Google peut refuser la publication si détecté).

2. **Compte développeur Google Play** (25 $ one-shot si pas déjà fait) : https://play.google.com/console/signup

3. **Captures d'écran** (2 minimum, jusqu'à 8 recommandé, format téléphone) : Collection, Encyclo, fiche détail d'une figurine, sélecteur de langue, Réglages... Dis-le-moi si tu veux qu'on les prenne ensemble.

4. **Activer GitHub Pages** pour héberger la politique de confidentialité :
   `github.com/cguidicelli083-code/MaCollectionWCF → Settings → Pages → Source: branch "main", dossier "/docs"`.
   L'URL sera alors `https://cguidicelli083-code.github.io/MaCollectionWCF/privacy-policy.html`.

5. **Dans Play Console** (nouvelle app) :
   - Nom, description courte/longue, icône, bannière → copier depuis `docs/store/fiche-play-store.md` et `docs/store/*.png`.
   - Coller l'URL de la politique de confidentialité (étape 4).
   - Questionnaire de classification du contenu (aucun contenu sensible dans l'app).
   - Section "Sécurité des données" (Data safety) : déclarer les données collectées — voir `docs/privacy-policy.html` pour la liste exacte (photos envoyées à Gemini/Groq pour reconnaissance, requêtes de recherche envoyées à eBay/Tavily, IDs publicitaires via AdMob). Cocher aussi "Contient des achats intégrés" (voir section dédiée dans `fiche-play-store.md`).
   - Uploader `app/build/outputs/bundle/release/app-release.aab` dans une release (commencer par un test interne/fermé est recommandé avant production).

6. **Après publication** : pense à sauvegarder le keystore (`macollectionwcf-release.jks`) et son mot de passe (dans `local.properties`) ailleurs qu'sur ce PC (gestionnaire de mots de passe, cloud chiffré...) — leur perte rendrait impossible toute future mise à jour de l'app.
