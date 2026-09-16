# Modèle de confidentialité : L'enfant do

## 1. Engagement fondamental et vérifiable

> « L'application n'établit aucune connexion Internet et ne programme aucune sauvegarde sur un service en ligne. »

- **Aucune permission réseau dans l'APK final :** Les permissions `android.permission.INTERNET` et `android.permission.ACCESS_NETWORK_STATE` sont explicitement exclues par le manifeste via `tools:node="remove"`.
- **Désactivation totale de la sauvegarde système :** `android:allowBackup="false"` combiné à `backup_rules.xml` et `data_extraction_rules.xml` interdisant tout envoi sur Google Drive ou service tiers lors d'un backup OS.
- **Aucune délégation distante :** Retrait complet de Google Play Services Auth, Google Drive API, Health Connect et Calendar Provider.
- **Pas de télémétrie ni de SDK analytique :** Aucun traceur, aucun rapport de crash distant, aucun ping de vérification de version.

---

## 2. Stockage et cycle de vie des données

- **Stockage sur l'appareil :** Toutes les sessions, préférences et annotations sont enregistrées exclusivement dans la base de données Room privée de l'application (`/data/data/fr.lenfantdo/databases/`).
- **Snapshots locaux de précaution :** Créés dans `noBackupFilesDir` avant un import ou une opération critique. Ils sont effacés lors de la désinstallation de l'application.
- **Sauvegardes exportables :**
  - Fichiers au format JSON versionné, chiffrés par défaut avec **AES-256-GCM** et dérivation de clé **PBKDF2** à partir d'un mot de passe utilisateur.
  - Export CSV lisible disponible sur demande explicite avec avertissement sur l'absence de protection cryptographique.
  - Sauvegarde locale contrôlée (dossier Téléchargements via MediaStore ou sélecteur SAF restreint aux fournisseurs locaux).

---

## 3. Limites de la garantie

- L'application ne peut pas protéger ses données locales si l'appareil hôte est compromis (accès root malveillant, logiciel espion système).
- Une fois qu'un fichier est exporté par l'utilisateur dans le stockage partagé de son téléphone, ce fichier peut être synchronisé ou lu par d'autres applications autorisées par l'utilisateur sur son appareil.
