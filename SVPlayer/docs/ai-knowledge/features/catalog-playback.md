# Catalogue, Playlist et Lecture

Derniere mise a jour: 2026-06-30.

## 1. Objectif

Documenter le chargement Xtream, le cache Room, les ecrans Live TV / Films / Series, les favoris, l'historique, la progression et la lecture ExoPlayer.

## 2. Fonctionnement actuel

SmartVision charge les contenus depuis le compte Xtream de l'utilisateur. Les donnees sont synchronisees vers Room et affichees dans les ecrans Compose actifs sous `ui/*`. La lecture plein ecran passe par Media3 ExoPlayer.

Les ecrans actifs sont routes par `ui/navigation/AppNavigation.kt`. Ne pas modifier une ancienne architecture sans verifier qu'elle est importee par la navigation active.

## 3. Workflow utilisateur

- Live TV: categories a gauche, chaines, apercu/mini player; OK lance l'apercu, OK sur la meme chaine ouvre le plein ecran.
- Films: categories conservees, grille de cards, detail film, puis lecteur.
- Series: categories conservees, detail serie avec saisons/episodes, puis lecteur episode.
- Favoris et Historiques sont des categories speciales.
- Si aucun compte Xtream n'est configure, l'utilisateur voit un QR de configuration.

## 4. Workflow technique

Xtream:
- `XtreamApiService.kt` appelle `player_api.php`.
- `XtreamRepository.kt` gere les appels remote.
- `DefaultCatalogRepository.kt` consolide remote/cache.
- `SynchronizeCatalogUseCase` declenche la synchro.
- `XtreamUrlFactory.kt` construit les URL de lecture.

Room:
- `SVDatabase.kt` version 4.
- Entites: profiles, categories, live streams, movies, series, episodes, favorites, playback progress, sync state, historique YouTube.

Playback:
- `FullScreenPlayerScreen.kt` gere Live, Movie et Episode.
- Live peut utiliser `.ts` et fallback `.m3u8`.
- Films/series sauvegardent la progression.
- Overlay plein ecran Live/Films/Series: bandeau haut glassmorphism avec logo, badge type contenu, titre et meta droite; le nom dossier/categorie n'est pas affiche dans le bandeau haut. Bandeau bas glassmorphism bleu tres transparent avec boutons circulaires distingues, bordure/glow neon et barre de progression uniquement pour Films/Series.

## 5. Ecrans concernes

- `ui/live/LiveTvScreen.kt`
- `ui/movies/MoviesScreen.kt`
- `ui/series/SeriesScreen.kt`
- `ui/detail/MovieDetailScreen.kt`
- `ui/detail/SeriesDetailScreen.kt`
- `ui/player/FullScreenPlayerScreen.kt`
- `ui/home/ContinueWatchingRow.kt`
- `ui/home/HomeCollectionsScreen.kt`

## 6. Fichiers de code concernes

- `data/remote/XtreamApiService.kt`
- `data/remote/XtreamApiClient.kt`
- `data/remote/XtreamUrlFactory.kt`
- `data/repository/XtreamRepository.kt`
- `data/repository/DefaultCatalogRepository.kt`
- `data/repository/UserContentRepository.kt`
- `data/local/SVDatabase.kt`
- `data/local/dao/*`
- `data/local/entity/*`
- `domain/usecase/UseCases.kt`
- `domain/model/CategoryHistoryPolicy.kt`
- `sync/SyncWorker.kt`
- `startup/PlaylistSyncWorker.kt`

## 7. Donnees / API / Backend / Admin

Endpoints Xtream utilises:
- `get_live_categories`
- `get_live_streams`
- `get_vod_categories`
- `get_vod_streams`
- `get_vod_info`
- `get_series_categories`
- `get_series`
- `get_series_info`

URL de lecture:
- Live: `/live/{username}/{password}/{stream_id}.ts`
- Live fallback: `/live/{username}/{password}/{stream_id}.m3u8`
- Film: `/movie/{username}/{password}/{stream_id}.{extension}`
- Episode: `/series/{username}/{password}/{episode_id}.{extension}`

## 8. Dependances

- Activation/Xtream pour credentials.
- UI TV/focus pour navigation catalogue.
- Monetisation/tracking pour preroll et evenements lecteur.
- Backend/admin pour config features et pubs.

## 9. Regles a ne pas casser

- L'application ne doit pas fournir de contenus par defaut comme service final.
- Ne pas afficher ou logguer les identifiants Xtream.
- La lecture doit rester native Media3 ExoPlayer.
- Pour Live TV, ne pas sauvegarder la progression comme VOD.
- Garder les categories speciales Favoris/Historiques coherentes.
- Ne pas bloquer l'affichage si du contenu partiel est deja disponible.

## 10. Problemes connus

- Les loaders doivent tenir compte de `visibleItems` pour eviter un blocage malgre le contenu partiel.
- Les changements de focus pendant fermeture lecteur peuvent provoquer des erreurs Compose si un FocusRequester survit a son composable.
- Les crashes playback doivent etre diagnostiques depuis fatal stack ou anomalies avant correction UI.

## 11. Quand lire ce fichier ?

Lire ce fichier si la demande concerne:
- playlist Xtream;
- synchro;
- Live TV;
- films;
- series;
- lecteur plein ecran;
- mini lecteur;
- ExoPlayer;
- progression;
- favoris;
- historique.

Ne pas lire ce fichier si la demande concerne uniquement:
- paiement web;
- admin sans effet catalogue;
- documentation pure du workflow agent.

## 12. Historique court

- 2026-06-29: migration vers documentation specialisee.
- 2026-06-29: documentation des routes `player`, `movie_player`, `episode_player`, `series_detail`.
- 2026-06-30: refonte overlay player Live/Films/Series vers le modele glassmorphism TV premium, sans modifier le rendu video Media3.
- 2026-06-30: ajustement overlay player: categorie retiree du bandeau haut, bandeaux plus compacts, bleu plus transparent, bordure/glow neon plus visible.
