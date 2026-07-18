# Decision: integration TMDB locale et non bloquante

Date: 2026-07-07.

## Contexte

SmartVision doit integrer TMDB pour enrichir les fiches films/series, avec une licence personnelle pendant le developpement et une licence a revoir avant commercialisation.

## Decision

- Le cache principal TMDB est local Room, pas MySQL/backend pour le MVP.
- Les secrets TMDB restent dans `local.properties` et sont exposes au runtime via `BuildConfig.TMDB_READ_ACCESS_TOKEN`.
- TMDB est optionnel: absence de token ou erreur reseau = fallback Xtream sans blocage UI.
- Le matching films/series est automatique, sans validation admin.
- Les providers JustWatch sont conserves comme metadonnees d'information.
- L'attribution TMDB/JustWatch est exposee dans un nouveau menu Settings.
- Les donnees adultes suivent le statut parental: `include_adult=false` quand le controle parental est actif.
- L'utilisateur peut desactiver TMDB depuis Settings. OFF masque le cache local et interdit le reseau sans supprimer les donnees; ON restaure immediatement la lecture du cache.

## Consequences

- Les tables Room `tmdb_content_mapping`, `tmdb_movie_metadata` et `tmdb_series_metadata` restent separees du catalogue Xtream.
- Les ids TMDB ne remplacent jamais les ids Xtream pour la lecture.
- Les prochains lots peuvent enrichir Series/Home/catalogues depuis les memes tables sans reintroduire de chargement massif au demarrage.
- Avant commercialisation, revoir licence TMDB, attribution officielle et politique cache/providers.
- Le cache Home enrichi doit etre bypassé pendant OFF pour ne laisser aucune image ou duree TMDB visible; il reste intact pour une reactivation ulterieure et aucune migration Room n'est necessaire.
