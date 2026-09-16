# Architecture logicielle : L'enfant do

## 1. Principes directeurs

- **Hors ligne strict :** Aucune permission réseau (`INTERNET`), aucun client HTTP, aucun SDK analytique ou de synchronisation cloud.
- **Séparation claire des responsabilités :** Trois modules de production (`:domain`, `:data`, `:app`) et des modules de support (`:testing`, `:baselineprofile`).
- **Fiabilité et intégrité du suivi :** Persistance immédiate et atomique des transitions d'état dans Room. Aucun Foreground Service permanent n'est nécessaire pour maintenir un compteur de temps en mémoire la nuit.
- **Interface déclarative moderne :** Jetpack Compose et Material 3 sous architecture unidirectionnelle (UDF / MVI-MVVM) avec `StateFlow`.

---

## 2. Découpage modulaire

```
:domain (Kotlin pur - 0 dépendance Android)
  ├── model/            # Modèles métier immuables (SleepSession, ActiveTracking, etc.)
  ├── repository/       # Contrats d'accès aux données (interfaces SleepRepository, SettingsRepository)
  ├── usecase/          # Cas d'usage métier (StartTrackingUseCase, StopTrackingUseCase, etc.)
  ├── analytics/        # Algorithmes purs (moyenne circulaire, variance de Welford, ratios de couverture)
  └── assistance/       # Règles déterministes d'aide et de détection d'anomalies

:data (Persistance & Transferts Android locaux)
  ├── database/         # Room Database v6, DAO, entités et migrations testées
  ├── preferences/      # DataStore typé pour les réglages utilisateur
  ├── repository/       # Implémentations concrètes des contrats de repository du domaine
  └── transfer/         # Import/Export CSV legacy, sauvegardes chiffrées AES-GCM, snapshots locaux

:app (Android Application & Compose UI)
  ├── ui/designsystem/  # Tokens Material 3 (thème nuit apaisant, typographie, espacements, composants)
  ├── feature/home/     # Écran principal, gros bouton de suivi, synthèse de nuit
  ├── feature/review/   # Validation rapide du réveil et bilan matinal facultatif
  ├── feature/journal/  # Historique chronologique, édition, pagination locale
  ├── feature/insights/ # Tendances, graphiques Vico/Canvas accessibles
  ├── feature/settings/ # Paramètres locaux, gestion des sauvegardes et confidentialité
  └── platform/         # Notifications Android, tuile Quick Settings, widget d'accueil
```

---

## 3. Flux de données unidirectionnel (UDF)

1. L'utilisateur déclenche une intention via un composable (ex: clic sur « Commencer le suivi » ou « Arrêter »).
2. Le `ViewModel` traduit l'intention en commande de cas d'usage (`StartTrackingUseCase(requestId, startedAt)`).
3. Le cas d'usage délègue au `SleepRepository`.
4. La couche `:data` exécute une transaction Room atomique et idempotente (`slot = 1` pour `ActiveTracking`, transition vers `SleepSession(DRAFT)` à l'arrêt).
5. Room émet un nouveau `Flow` d'état observé par le `ViewModel` via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ...)`.
6. L'UI Compose se recompose automatiquement à partir du `UiState` immuable.
