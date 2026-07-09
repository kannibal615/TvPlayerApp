# Decision: isolation Room multi-profils playlist

Date: 2026-07-09

## Decision

Les catalogues IPTV locaux sont isoles par `profileId` en Room. Les tables concernees sont `categories`, `live_streams`, `movies`, `series`, `episodes`, `sync_state`, `favorites`, `playback_progress`, `trending_media`, `home_trending_preview_cache` et `tmdb_content_mapping`.

## Raison

Les fournisseurs Xtream/M3U peuvent reutiliser les memes ids de contenus entre deux profils. Sans `profileId` dans les cles primaires, un nouveau profil peut ecraser ou lire les donnees du profil principal, notamment catalogue, favoris et historique.

## Regles

- Le profil actif reste la source unique affichee dans Live TV, Films et Series.
- Changer de profil invalide les caches memoire et publie `catalogRevision`, mais ne vide plus Room.
- Si le profil actif possede deja un catalogue local synchronise, il ne faut pas relancer une synchronisation reseau au simple changement de profil.
- Les historiques, reprises et favoris IPTV suivent le profil actif.
- YouTube et Media local restent globaux.
- Les metadonnees TMDB par `tmdbId/language` restent globales; seul le mapping Xtream -> TMDB est profile.
