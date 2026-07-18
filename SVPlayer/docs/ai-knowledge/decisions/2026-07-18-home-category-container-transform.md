# Decision - Container transform des categories Home

Date: 2026-07-18

## Contexte

Les cards Home Live TV, Movies et Series doivent se transformer visuellement en leur ecran reel sans deplacer le header, casser le D-pad ou remesurer les catalogues complexes a chaque frame.

## Decision

- `AppNavigation` enveloppe le `NavHost` Navigation 2 dans `SharedTransitionLayout`.
- Seul un clic direct sur une card arme la transition, avec les cles `home-category:live_tv`, `home-category:movies` et `home-category:series`.
- Une surface visuelle legere de la card partage ses bounds avec une cible transparente couvrant la zone catalogue sous le header; le contenu reel apparait separement.
- Le header utilise une cle partagee distincte pour rester a la meme position.
- L'ouverture dure `320 ms`, le repli `260 ms`; D-pad, OK et Back sont consommes pendant le mouvement, puis le focus source est restaure apres Back.

## Consequences

- Les ViewModels, repositories, donnees, routes et comportements catalogue restent inchanges.
- Les navigations du header et les cards bloquees ne declenchent pas le container transform.
- L'API Compose etant experimentale dans la version actuelle, l'opt-in reste limite au fichier de navigation.
