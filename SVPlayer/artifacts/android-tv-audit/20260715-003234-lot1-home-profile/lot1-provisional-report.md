# Audit Android TV — Lot 1 provisoire

- Date : 2026-07-15 (Europe/Paris)
- Appareil : Amazon Fire TV `192.168.1.33:5555`
- Application : `com.smartvision.svplayer` 0.1.120 (168)
- Périmètre : navigation globale, Home, accès à Info profil
- État final : Home affiché, focus restauré sur l’avatar du profil
- Modifications applicatives : aucune

## Analyse préalable du code

Les routes et composants ont été lus avant toute navigation :

- `ui/navigation/AppNavigation.kt` : routes Home/Profile et retour Profile vers Home avec cible de focus Profile.
- `ui/home/TvHeader.kt` : ordre et cibles du header.
- `ui/home/HomeScreen.kt` : Hero non focusable, catégories, Continue Watching, tendances films et séries.
- `ui/profile/ProfileScreen.kt` : menu latéral, cible initiale Info compte, accès au contenu et retour au header.

Le composant historique `SideNavBar.kt` n’est pas le menu actif de ce parcours ; le « menu latéral » couvert est la colonne de sections de Profile.

## Couverture

| Zone / parcours | Résultat |
|---|---|
| Focus initial Home | OK — carte Live TV |
| Header Home → Live TV → Movies → Series → Media → YouTube → licence → notifications → avatar | OK — focus visible, aucun élément ouvert sauf l’avatar identifié |
| Home : DPAD_LEFT/RIGHT sur les catégories | OK — limites et déplacement Live/Movies/Series cohérents |
| Home : DPAD_UP/DOWN entre header, catégories et sections | OK — enchaînement déterministe |
| Hero | Couvert visuellement ; non focusable conformément au code |
| Continue Watching | OK — deux cartes parcourues, retour gauche et transitions verticales vérifiés |
| Tendances films | OK — deux cartes parcourues |
| Tendances séries | OK — deux cartes parcourues |
| Posters, textes et données au focus | Aucun changement anormal confirmé après stabilisation ; seules l’emphase et la bordure de focus changent |
| Info profil | OK — ouverture depuis l’avatar ; aucun profil ouvert, modifié ou sélectionné |
| Menu latéral Profile | Tous les items atteints au focus, sans validation : Info compte, Contrôle parental, Synchronisation, Historique, Aide, Paramètres |
| Header ↔ Info compte | OK — UP vers Home, DOWN vers Info compte |
| Info compte ↔ contenu | Défaut confirmé sur le retour LEFT, détaillé ci-dessous |
| Back Profile → Home | OK — retour Home et restauration du focus sur l’avatar, vérifiés deux fois |
| Lecteur, PIN, synchronisation, paramètres, édition/suppression | Non atteints conformément aux interdictions |

## Anomalies confirmées

### LOT1-FOCUS-001 — LEFT depuis la carte du profil revient sur Synchronisation

- Catégorie : focus / navigation TV
- Gravité : moyenne
- Reproductibilité : 3/3
- Résultat actuel : depuis `Info compte`, RIGHT place le focus sur la carte du profil actif ; LEFT revient sur `Synchronisation`.
- Résultat attendu : LEFT doit restaurer `Info compte`, c’est-à-dire l’élément ayant servi d’entrée dans le panneau.
- Impact : rupture de symétrie du parcours et risque d’ouvrir involontairement la synchronisation lors de l’action suivante.
- Cause probable dans le code : la ligne de profil gère explicitement RIGHT vers son toggle, mais ne définit pas de cible LEFT vers `xtreamSectionFocusRequester`; Compose choisit alors géométriquement l’item Synchronisation.
- Correction recommandée : passer le requester du menu Info compte aux lignes de profil et définir une cible LEFT explicite, tout en conservant le comportement RIGHT existant.

Étapes exactes :

1. Depuis Home, placer le focus sur l’avatar du header.
2. Presser OK pour ouvrir Profile / Info compte.
3. Presser RIGHT : la carte du profil actif reçoit le focus.
4. Presser LEFT.
5. Observer le focus sur Synchronisation au lieu d’Info compte.

Preuves :

- Avant LEFT : `checkpoints/38-profile-info-right-content/screenshot.png`
- Reproduction 1 : `checkpoints/39-profile-content-left-info/screenshot.png`
- Reproduction 2 : `checkpoints/43-repro2-profile-card-left-sync/screenshot.png`
- Reproduction 3 : `checkpoints/47-repro3-profile-card-left-sync/screenshot.png`

### LOT1-PERF-001 — piste d’investigation performance (mesure Lot 1 non isolée)

- Catégorie : performance
- Classement à l’issue du Lot 1 : piste d’investigation, pas anomalie confirmée
- Gravité : non attribuée dans ce lot
- Reproductibilité : continue pendant le parcours
- Logcat : 203 GC concurrents entre 01:05:24 et 01:13:17, soit environ un toutes les 2,3 secondes. Chaque cycle libère généralement 4 à 6 Mo et environ 150 000 à 208 000 objets ; le temps total d’un cycle est typiquement 226 à 267 ms, avec des pauses courtes inférieures à 0,6 ms.
- Gfxinfo cumulatif : 291 210 → 367 687 frames ; les 76 477 frames ajoutées au compteur sont toutes classées janky. Le p50 passe de 28 à 29 ms, le p90 de 81 à 85 ms et le p99 de 97 à 105 ms.
- Impact : pression CPU/mémoire et risque de saccades sur Firestick, même si aucun blocage franc n’a été observé pendant ce lot.
- Limite : `gfxinfo` n’a pas été réinitialisé et couvre aussi l’historique antérieur du processus ; le pourcentage absolu ne doit pas être considéré comme une mesure isolée du seul parcours.
- Correction recommandée : lors d’une phase autorisée, isoler un parcours court avec Perfetto/Compose tracing et identifier la source des allocations périodiques avant toute optimisation.
- Statut ultérieur : cette piste est réévaluée séparément dans le rapport du Lot 1B, sur trois compteurs réinitialisés.

Preuves :

- Début : `checkpoints/00-home-initial/gfxinfo-framestats.txt`
- Fin et logcat : `checkpoints/48-profile-sync-back-home-avatar-final/gfxinfo-framestats.txt`, `checkpoints/48-profile-sync-back-home-avatar-final/logcat.txt`

## UI / UX

- Bordure jaune de focus nette et cohérente sur header, cartes Home et menu Profile.
- Alignements, espacements et dimensions cohérents à 1920 × 1080.
- Les titres longs des contenus utilisent une troncature attendue ; aucun chevauchement d’éléments interactifs confirmé.
- Aucun crash, ANR, `FATAL EXCEPTION`, erreur FocusRequester ou OutOfMemoryError trouvé dans les logs collectés.

## Captures principales

- Home initial : `checkpoints/00-home-initial/screenshot.png`
- Continue Watching : `checkpoints/11-continue-second-left-first/screenshot.png`
- Tendances films : `checkpoints/13b-trending-movies-second-stable-no-input/screenshot.png`
- Tendances séries : `checkpoints/15c-trending-series-second-stable-retry-no-input/screenshot.png`
- Avatar du header : `checkpoints/27c-header-profile-avatar-stable-retry-no-input/screenshot.png`
- Info profil : `checkpoints/28-profile-avatar-ok-open-info/screenshot.png`
- Menu Profile / Paramètres au focus : `checkpoints/35b-profile-settings-focus-stable-no-input/screenshot.png`
- Retour Home / avatar restauré : `checkpoints/36-profile-settings-back-home-avatar/screenshot.png`

Chaque checkpoint capturé contient `screenshot.png`, `window.txt`, `activity.txt`, `logcat.txt`, `gfxinfo-framestats.txt` et `metadata.json`. Les checkpoints purement directionnels 16, 17 et 30 ont volontairement omis le screenshot ; leurs états avant/après sont couverts par les checkpoints adjacents.

## Limites de couverture

1. `uiautomator` n’a pas été relancé : l’erreur `could not get idle state` est traitée comme limitation Fire OS.
2. `screencap` Fire OS omet parfois temporairement certaines couches Compose juste après un déplacement. Les preuves importantes ont été recapturées sans nouvelle touche ; ce phénomène n’est pas classé comme défaut applicatif.
3. Aucun OK n’a été envoyé sur Contrôle parental, Synchronisation, Historique, Aide, Paramètres, contenu Home ou profil individuel.
4. Aucun lecteur, PIN, changement de profil, synchronisation, réglage, installation, désinstallation ou réinitialisation n’a été déclenché.
5. La performance nécessite une trace temporelle isolée pour attribuer précisément les allocations et le jank.

## Conclusion provisoire

Le lot 1 est fonctionnel dans son ensemble. Le défaut de retour LEFT dans Info compte est confirmé 3/3. Le signal de pression d’allocation et de jank mérite une investigation dédiée, sans qu’une correction puisse être proposée avec certitude à partir du seul compteur cumulatif. Aucune correction n’a été appliquée.
