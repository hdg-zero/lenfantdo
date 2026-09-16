# Assets Google Play Store — L'enfant do 🌙

Ce dossier regroupe l'ensemble des éléments graphiques requis pour la publication de l'application **L'enfant do** sur le Google Play Console.

---

## 📁 Contenu du dossier

| Fichier | Format | Dimensions | Spécification Play Store | Description |
| :--- | :---: | :---: | :---: | :--- |
| [`icon.svg`](icon.svg) | SVG | 512 × 512 | Vectoriel source | Icône officielle avec masque squircle doux |
| [`icon-512.png`](icon-512.png) | PNG (32-bit RGBA) | 512 × 512 | Requis (Max 1024 Ko) | Icône Play Store prête à l'upload |
| [`icon-square.svg`](icon-square.svg) | SVG | 512 × 512 | Vectoriel plein cadre | Variante sans coins arrondis (pleine trame) |
| [`icon-square-512.png`](icon-square-512.png) | PNG (24-bit RGB) | 512 × 512 | Alternative Play Console | Variante pleine trame (coins gérés par Google) |
| [`banner.svg`](banner.svg) | SVG | 1 024 × 500 | Vectoriel source | Bannière / Graphique des fonctionnalités |
| [`banner-1024x500.png`](banner-1024x500.png) | PNG (24-bit RGB) | 1 024 × 500 | Requis (Max 15 Mo) | Bannière Play Store prête à l'upload (195 Ko) |

---

## 🎨 Détails Graphiques & Conformité

### 1. Icône de l'application (512 × 512 px)
- **Palette :** Dégradé nocturne (`#0B132B` ➔ `#1C2541` ➔ `#1E1B4B`), croissant de lune doré éclatant (`#FEF08A` ➔ `#F59E0B`), étoiles étincelantes et onde de berceau.
- **Poids :** ~70 Ko (très inférieur à la limite de 1 024 Ko).
- **Emplacement console :** *Fiche de l'application > Éléments graphiques > Icône de l'application*.

### 2. Graphique des fonctionnalités / Bannière (1 024 × 500 px)
- **Composition :**
  - **Zone de sécurité (Safe Zone) :** Respect des marges de 15% pour éviter toute coupure lors du redimensionnement dynamique sur mobile, tablette ou web.
  - **Gauche :** Titre officiel, signature *"Journal de sommeil privé, assisté et sans réseau"*, et badges glassmorphism (*100% Hors Ligne*, *AES-256 local*, *Zéro WakeLock*).
  - **Droite :** Croissant de lune céleste emblématique avec halo starlight et ondulation de berceau.
- **Poids :** 195 Ko (très inférieur à la limite de 15 Mo).
- **Emplacement console :** *Fiche de l'application > Éléments graphiques > Graphique des fonctionnalités*.
