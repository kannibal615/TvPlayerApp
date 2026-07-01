# Decision: Xtream startup gating and local-only catalog navigation

Date: 2026-06-30.

## Contexte

La demande impose une verification rapide Xtream au demarrage, un blocage propre des contenus si le serveur utilisateur est indisponible, et l'interdiction de relancer une synchronisation globale pendant la navigation Home / Live TV / Movies / Series.

## Decision

Centraliser l'etat Xtream dans `data/xtream/XtreamConnectionManager.kt` avec les statuts:
- `CONNECTED`;
- `NETWORK_ERROR`;
- `INVALID_CREDENTIALS`;
- `INVALID_RESPONSE`;
- `UNKNOWN_ERROR`;
- `NOT_CONFIGURED`.

Les ecrans Home / Live TV / Movies / Series lisent le catalogue local Room via `CatalogRepository`. Les appels reseau Xtream sont reserves:
- au test rapide de disponibilite;
- a la synchronisation complete au demarrage si obsolescence;
- a AutoSync Android;
- a la synchronisation manuelle depuis les parametres/profil.

Depuis le 2026-07-01, la premiere lecture locale lourde doit etre deplacee dans `SplashActivity`: le splash prechauffe les caches memoire Home / Live TV / Movies / Series apres verification Xtream et apres synchronisation si elle est due. Les ecrans Compose utilisent ensuite ces caches et ne doivent plus presenter un loader plein ecran quand ces donnees sont deja disponibles.

## Consequences

- Si Xtream echoue, Home reste accessible mais Live TV / Movies / Series, Header et reprises de lecture affichent l'alerte au lieu de naviguer.
- Les erreurs `NETWORK_ERROR` peuvent etre retentees en arriere-plan.
- Les erreurs d'identifiants ou de reponse invalide ne sont pas retentees automatiquement; l'utilisateur doit modifier les identifiants.
- La synchronisation complete recupere toutes les reponses principales avant de remplacer les tables locales, pour eviter un catalogue partiel.
- Le splash natif devient le seul ecran avec progress bar de demarrage; `ActivationScreen` ne doit plus reintroduire un loader visuel secondaire.
- Home seed son etat depuis les caches memoire de slides, progression recente enrichie et snapshots Movies / Series.

## Fichiers principaux

- `app/src/main/java/com/smartvision/svplayer/data/xtream/XtreamConnectionManager.kt`
- `app/src/main/java/com/smartvision/svplayer/data/xtream/XtreamConnectionNotifier.kt`
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/home/HomeSlidesRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/UserContentRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `server/public_html/admin/index.php`
