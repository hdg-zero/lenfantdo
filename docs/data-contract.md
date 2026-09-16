# Contrat de données et persistance : L'enfant do

## 1. Modèle de données (Room v6)

### Table `active_tracking`
Garantit au plus une session active à tout instant :
- `slot`: INTEGER PRIMARY KEY (toujours égal à `1`)
- `session_id`: TEXT NOT NULL (UUID v4)
- `started_at_epoch_ms`: INTEGER NOT NULL (horodatage UTC en millisecondes)
- `started_timezone`: TEXT NOT NULL (identifiant de fuseau horaire IANA)
- `request_id`: TEXT NOT NULL (UUID d'idempotence de la commande `Start`)

### Table `sleep_session`
Représente une session terminée (brouillon ou confirmée) :
- `id`: TEXT PRIMARY KEY (UUID v4)
- `started_at_epoch_ms`: INTEGER NOT NULL (UTC)
- `ended_at_epoch_ms`: INTEGER NOT NULL (UTC, invariant `ended_at > started_at`)
- `status`: TEXT NOT NULL (`DRAFT` ou `CONFIRMED`)
- `session_type`: TEXT NOT NULL (`MAIN`, `NAP`, `OTHER`)
- `started_timezone`: TEXT NOT NULL
- `ended_timezone`: TEXT NOT NULL
- `sleep_date_override`: TEXT NULL (date civile `YYYY-MM-DD` si surcharge explicite)
- `rating`: INTEGER NOT NULL (0 si non noté, 1 à 5)
- `comment`: TEXT NOT NULL (texte libre)
- `wakes`: INTEGER NOT NULL (nombre de réveils déclarés)
- `revision`: INTEGER NOT NULL DEFAULT 1
- `created_at_epoch_ms`: INTEGER NOT NULL
- `updated_at_epoch_ms`: INTEGER NOT NULL
- `legacy_sid`: INTEGER NULL (conservation de l'ancien identifiant Plees Tracker pour traçabilité)

### Table `day_annotation`
Annotations explicites par journée civile :
- `date`: TEXT PRIMARY KEY (`YYYY-MM-DD`)
- `is_completed`: INTEGER NOT NULL (1 si déclarée complètement renseignée)
- `zero_sleep_confirmed`: INTEGER NOT NULL (1 si l'utilisateur a explicitement confirmé 0h de sommeil)
- `notes`: TEXT NOT NULL

---

## 2. Invariants et intégrité

1. **Horodatages absolus :** Tous les calculs d'intervalles utilisent l'horodatage UTC absolu (`Instant`).
2. **Pas de 0 fictif :** Une information inconnue reste `null` ou non renseignée ; elle n'est jamais remplacée silencieusement par 0.
3. **Idempotence des commandes :** Un double clic sur « Démarrer » retourne la session active en cours sans créer de doublon. Un « Arrêter » obsolète est rejeté sans altérer une nouvelle session active.
4. **Pas d'auto-destruction :** `fallbackToDestructiveMigration()` est strictement proscrit.
