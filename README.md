# L'enfant do 🌙

**Journal de sommeil privé, assisté et 100% hors ligne.**  
*Fork moderne et résilient de Plees Tracker.*

[![Build & Test](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Offline](https://img.shields.io/badge/network-0%20permissions-blue.svg)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 💡 Philosophie et Principes directeurs

1. **Vie privée absolue et étanchéité hors ligne :**
   - Aucune permission réseau (`android.permission.INTERNET` et `ACCESS_NETWORK_STATE` sont formellement révoquées et absentes de l'APK).
   - Aucune télémétrie, aucun tracker, aucun SDK publicitaire.
   - Sauvegarde cloud automatique Android désactivée (`allowBackup="false"` et règles strictes de non-extraction).
   - Vos données restent exclusivement sur votre appareil.

2. **Sobriété système :**
   - **Zéro service permanent la nuit :** Le suivi ne maintient pas de Foreground Service énergivore pendant votre sommeil.
   - Les horaires de coucher et de réveil sont persistés de façon transactionnelle et atomique dans Room (`slot = 1`).
   - Une notification locale standard avec chronomètre système (`setUsesChronometer(true)`) garantit un réveil précis sans charge CPU ni drainage de batterie.

3. **Statistiques temporelles civiles rigoureuses :**
   - **Moyenne circulaire des horaires :** Les heures de coucher et de réveil sont projetées sur le cercle unité trigonométrique (\(24\text{h} = 360^\circ\)), éliminant définitivement le « piège de minuit » où une moyenne arithmétique naïve entre 23h50 et 00h10 donnerait midi (12h00).
   - **Algorithme de Welford :** Calcul numérique stable de la durée moyenne et de l'écart-type en simple passe.
   - **Gestion explicite des absences :** Les jours sans relevé ne sont jamais assimilés à 0h de sommeil (ce qui fausserait les moyennes), mais comptabilisés séparément sous forme de taux de couverture.

4. **Souveraineté des données & Sauvegardes locales :**
   - **Import / Export CSV :** Rétrocompatibilité totale avec les sauvegardes de Plees Tracker original.
   - **Sauvegarde chiffrée de bout en bout (.enc) :** Chiffrement symétrique fort AES-256-GCM avec dérivation de clé PBKDF2 (65 536 itérations, sel aléatoire 16 octets et vecteur d'initialisation 12 octets unique).
   - **Instantanés de sécurité :** Création automatique d'un snapshot de précaution dans `noBackupFilesDir` avant toute opération de restauration.

5. **Interface moderne et accessible :**
   - Conçue avec **Jetpack Compose** et **Material 3**.
   - Thème nuit doux et apaisant préservant la vue dans l'obscurité.
   - Ergonomie tactile renforcée (cibles tactiles $\ge 48\text{dp}$).
   - Accessibilité **TalkBack** complète, avec description sémantique des cartes et table textuelle alternative accompagnant les graphiques.

---

## 🏗️ Architecture Multi-Modules

```
:app                       Interface Jetpack Compose, Material 3, Navigation & ViewModels
  ├── ui/theme/            Tokens de design M3, typographie et palette nuit apaisante
  ├── ui/component/        Bouton de suivi, carte de nuit, sélecteur de période, métriques
  ├── ui/navigation/       NavHost et destinations typées (Home, Journal, Insights, Detail, Settings)
  └── feature/             Écrans Accueil, Bilan de réveil, Journal, Tendances, Détail, Paramètres, Confidentialité

:domain                    Module Kotlin pur (zéro dépendance Android)
  ├── model/               SleepSession, ActiveTracking, DayAnnotation
  ├── repository/          Contrats d'accès aux données (SleepRepository, SettingsRepository)
  ├── usecase/             StartTrackingUseCase, StopTrackingUseCase, DeleteSleepSessionUseCase...
  ├── analytics/           CircularTimeStatistics, WelfordVariance, CoverageCalculator, SleepDateResolver
  └── assistance/          AssistanceEngine (règles déterministes d'anomalies de durée)

:data                      Couche de persistance et de transfert locale (Android Library)
  ├── local/               Room v7 AppDatabase, DAOs, migrations préservées v1->v7
  ├── repository/          SleepRepositoryImpl (Room transactions), SettingsRepositoryImpl (DataStore)
  ├── export/              LegacyCsvParser (tolérant aux BOM et virgules), SnapshotManager (SHA-256)
  └── transfer/            EncryptedBackupManager (AES-256-GCM + PBKDF2)
```

---

## 🛠️ Compilation et Tests

### Prérequis
- JDK 21
- Android SDK (API 26+)

### Exécuter la suite de tests unitaires
```bash
./gradlew testDebugUnitTest
```

### Vérifier la conformité des en-têtes de licence
```bash
./tools/license-check.sh
```

### Compiler l'APK et auditer les permissions
```bash
./gradlew assembleRelease
./tools/audit-release-apk.sh app/build/outputs/apk/release/app-release.apk
```

---

## 📜 Licences et Attribution

Ce logiciel est distribué sous licence **MIT**.
- Copyright (c) 2020-2023 Miklos Vajna et les contributeurs de Plees Tracker.
- Copyright (c) 2026 Les contributeurs de L'enfant do.

Consultez le fichier [LICENSE](LICENSE) pour les termes complets.