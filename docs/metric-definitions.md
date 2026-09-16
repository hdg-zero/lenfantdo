# Définition des métriques et algorithmes : L'enfant do

## 1. Distinction fondamentale : Durée suivie vs Sommeil estimé

- **Durée suivie (Tracked Duration) :**
  $$\Delta t = t_{fin} - t_{debut}$$
  Intervalle brut entre l'heure déclarée de coucher et de réveil. Métrique de base comparable avec l'historique Plees Tracker original.

- **Temps au lit déclaré (Time in Bed) :**
  Disponible uniquement si l'utilisateur valide que les bornes correspondent à l'entrée et la sortie effectives du lit.

- **Sommeil estimé (Estimated Sleep) :**
  Calculé uniquement si la latence d'endormissement ($L$), les éveils nocturnes en durée ($E_{nocturne}$) et le temps d'éveil final ($E_{final}$) sont tous renseignés :
  $$T_{estime} = T_{lit} - L - E_{nocturne} - E_{final}$$
  Si l'une des composantes est inconnue, cette métrique n'est **pas affichée** (pas d'extrapolation arbitraire).

---

## 2. Contrat temporel civil

- **Date de rattachement :** Par défaut, une session nocturne est rattachée à sa **date locale de fin** (`LocalDate` au réveil).
- **Gestion des jours civils :** Utilisation systématique de `java.time.LocalDate` et `ZoneId`. Interdiction de diviser les millisecondes par $86\,400\,000$ pour compter les jours, garantissant la justesse lors des passages heure d'été / heure d'hiver.
- **Données manquantes et couverture :** Une journée sans enregistrement signifie une **absence de données**, pas 0h de sommeil. Le taux de couverture ($\text{jours renseignés} / \text{jours totaux}$) est toujours affiché aux côtés des médianes ou moyennes.

---

## 3. Algorithmes statistiques

- **Moyenne circulaire des horaires :**
  Pour éviter l'aberration arithmétique où la moyenne de 23h50 ($1430$ min) et 00h10 ($10$ min) donnerait 12h00 ($720$ min), les horaires sont convertis en angles sur le cercle trigonométrique de 24h ($1440$ min) :
  $$\theta_i = \frac{m_i \times 2\pi}{1440}$$
  $$\bar{S} = \frac{1}{N}\sum \sin(\theta_i), \quad \bar{C} = \frac{1}{N}\sum \cos(\theta_i)$$
  $$R = \sqrt{\bar{S}^2 + \bar{C}^2}$$
  L'angle moyen $\bar{\theta} = \text{atan2}(\bar{S}, \bar{C})$ donne la minute moyenne, avec $R$ comme indicateur de concentration (si $R < 10^{-6}$, la moyenne circulaire est déclarée indéfinie).

- **Variance et écart-type (Algorithme de Welford) :**
  Calcul incrémental en précision `Double` pour éviter l'instabilité numérique de la formule naïve $E[X^2] - E[X]^2$.
