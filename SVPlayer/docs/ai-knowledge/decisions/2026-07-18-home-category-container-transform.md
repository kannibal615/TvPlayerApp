# Decision - Container transform des categories Home

Date: 2026-07-18

## Contexte

Les cards Home Live TV, Movies et Series doivent se transformer visuellement en leur ecran reel sans deplacer le header, casser le D-pad ou remesurer les catalogues complexes a chaque frame.

## Decision

- `AppNavigation` enveloppe le `NavHost` Navigation 2 dans `SharedTransitionLayout`.
- Seul un clic direct sur une card arme la transition, avec les cles `home-category:live_tv`, `home-category:movies` et `home-category:series`.
- La card complete partage ses bounds avec le vrai conteneur catalogue sous le header. Une cible vide/transparente est interdite: elle anime les coordonnees sans produire le morphing visuel attendu.
- Le `BoundsTransform` impose une keyframe centree a `1,12x`: ouverture `560 ms` (`220 ms` jusqu'au centre, puis expansion/reveal), retour `480 ms` (`300 ms` jusqu'au centre, puis retour a la card).
- Le header utilise une cle partagee distincte pour rester a la meme position.
- D-pad, OK et Back sont consommes pendant le mouvement, puis le focus source est restaure apres Back.

## Consequences

- Les ViewModels, repositories, donnees, routes et comportements catalogue restent inchanges.
- Les navigations du header et les cards bloquees ne declenchent pas le container transform.
- L'API Compose etant experimentale dans la version actuelle, l'opt-in reste limite au fichier de navigation.
- Le meme langage de mouvement est applique au mini-player des trois catalogues vers le lecteur plein ecran: bounds reels, passage centre, expansion/repli, sans logique ViewModel.
