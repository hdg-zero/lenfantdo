# Checklist de livraison (Release) : L'enfant do

Avant toute publication d'une release ou release candidate de l'application :

## 1. Intégrité hors ligne & Sécurité
- [ ] L'APK release final est inspecté avec `apkanalyzer` :
  - Absence stricte de `android.permission.INTERNET`
  - Absence stricte de `android.permission.ACCESS_NETWORK_STATE`
  - Présence de `allowBackup="false"`
- [ ] Aucun composant Google Play Services, Google Drive, Health Connect ou Calendar présent dans l'APK.
- [ ] Aucun crash reporter ou SDK tiers distant intégré.

## 2. Robustesse fonctionnelle
- [ ] Suite de tests unitaires JVM au vert : `./gradlew test`
- [ ] Tests d'étanchéité des migrations Room v5 -> v6 validés.
- [ ] Tests d'importation CSV originaux (legacy Plees Tracker) validés.
- [ ] Comportement en mode Avion vérifié : suivi, modification, export local, restauration.
- [ ] Arrêt brutal du processus pendant un suivi actif : reprise immédiate de la session active sans altération d'état ni doublon.

## 3. Accessibilité & UX
- [ ] Cibles tactiles d'au moins 48 dp.
- [ ] Contraste conforme WCAG AA en mode sombre et en mode clair.
- [ ] Parcours complets testés et navigables avec TalkBack actif.
- [ ] Support du redimensionnement de police à 200 % sans troncature ni superposition.

## 4. Licence & Mentions légales
- [ ] Licence MIT d'origine conservée et documentée.
- [ ] Mentions des licences des bibliothèques open-source embarquées dans l'écran de paramètres.
