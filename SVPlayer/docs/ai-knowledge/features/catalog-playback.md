# Catalogue, Playlist et Lecture

Derniere mise a jour: 2026-07-02.

## 1. Objectif

Documenter le chargement Xtream ou M3U, le cache Room, les ecrans Live TV / Films / Series, l'EPG XMLTV, les favoris, l'historique, la progression et la lecture ExoPlayer.

## 2. Fonctionnement actuel

SmartVision charge les contenus depuis la source active choisie par l'utilisateur: Xtream ou M3U, jamais les deux simultanement. Les donnees sont synchronisees vers Room et affichees dans les ecrans Compose actifs sous `ui/*`. La lecture plein ecran passe par Media3 ExoPlayer.

Les ecrans actifs sont routes par `ui/navigation/AppNavigation.kt`. Ne pas modifier une ancienne architecture sans verifier qu'elle est importee par la navigation active.

Depuis le 2026-06-30, la navigation Home / Live TV / Movies / Series ne doit plus declencher de telechargement Xtream global ni charger les categories depuis le reseau. Ces ecrans lisent le catalogue local Room via `CatalogRepository`. Les appels reseau Xtream sont limites aux controles rapides de disponibilite et aux synchronisations catalogue autorisees.

Depuis le 2026-07-01, les ecrans Live TV / Movies / Series ne doivent plus ouvrir un snapshot complet du catalogue. Le startup Compose de `MainActivity` prechauffe uniquement les categories/counts, puis les ViewModels catalogue chargent les contenus par pages depuis Room avec `LIMIT/OFFSET`. La RAM ne doit pas contenir tout le catalogue pour ouvrir un ecran.

Home charge ses donnees legeres en petits jeux bornes: le splash precharge uniquement l'historique recent `10`, les tendances films `10` et les tendances series `10`, puis `HomeViewModel` relit les memes limites depuis Room. Ne pas reutiliser les snapshots complets Movies / Series pour initialiser Home.

Depuis le 2026-07-02, apres une synchro splash ou un demarrage sans synchro, le splash prechauffe aussi les categories Live/Films/Series et la premiere page locale bornee des catalogues (`96` Live, `72` Films, `72` Series). `DefaultCatalogRepository` garde ces categories et premieres pages en cache memoire jusqu'a invalidation; les ViewModels catalogue les utilisent pour eviter un loader initial inutile, puis continuent la pagination Room normale.

Depuis le 2026-07-02, les tendances Home sont separees en films et series. La synchro Xtream alimente `trending_media` avec maximum `50` films et `50` series, sans filtre note par defaut, en excluant les marqueurs adultes et en validant rapidement une URL de lecture avant insertion. Pour les series, la validation passe par un episode obtenu via `get_series_info`. Le Home filtre ensuite les sections sur les medias qui exposent un poster paysage `backdropUrl`, sauf configuration distante contraire via `app_config.php`.

Clarification stockage/performance:
- Room est le stockage local persistant de l'application sur l'appareil; les catalogues synchronises restent disponibles apres fermeture ou redemarrage de l'app tant que les donnees de l'application ne sont pas effacees.
- Le cache memoire applicatif est uniquement en RAM; il peut garder de petites pages deja ouvertes, mais ne doit pas garder tout le catalogue pour l'ouverture d'ecran.
- Le chargement local au startup relit Room pour les categories/counts et, depuis le 2026-07-02, les petits jeux Home bornes `10/10/10`; il ne doit pas etre confondu avec une synchronisation reseau.
- Apres une vraie reouverture ou un process tue par Android, l'UI doit relire Room, mais uniquement par petits jeux de donnees pagines.
- La synchronisation reseau complete est separee du chargement local et depend de `SyncFrequencyPolicy`: `A chaque demarrage` force une synchro a chaque ouverture, `24h`/`48h` ne resynchronisent que si la derniere synchro est obsolete, `Manuelle`/`Jamais` evitent la synchro automatique.
- Recommandation d'optimisation: garder le prechauffage Room au splash et preferer une frequence `24h` ou `48h` pour eviter les telechargements reseau inutiles tout en gardant les catalogues frais.
- Les index Room sur `categoryId`, tri par numero/titre/nom et `episodes.seriesId` supportent le chargement pagine local.

Depuis le 2026-06-30, la synchronisation manuelle depuis Info compte publie aussi une progression par section Live TV / Films / Series dans `SyncStatus`. Les compteurs de l'ancienne synchro servent d'estimation de progression; chaque section passe a 100% quand son endpoint principal est termine.

Depuis le 2026-07-01, la synchronisation Xtream emet aussi des logs memoire `SVSyncMemory` pour diagnostiquer les OOM Firestick. Les jalons couvrent le debut de synchro, les appels compte/categories/Live/Films/Series, les ecritures Room par section, l'invalidation du cache local et les erreurs. Ces logs ne changent pas le comportement utilisateur et ne doivent pas exposer les identifiants Xtream.

Depuis le 2026-07-01, un lien M3U peut devenir la source active du catalogue Live TV. Les entrees M3U sont parsees depuis `#EXTINF`, groupees par `group-title`, stockees dans `live_streams` avec `source = m3u` et `directStreamUrl`, puis lues sans `XtreamUrlFactory`. Quand M3U est actif, Movies et Series sont vides pour respecter la regle une seule source active.

Depuis le 2026-07-01, l'URL EPG XMLTV est telechargee dans un cache local borne lors d'une synchronisation manuelle ou catalogue, mais pas pendant le splash. Les programmes enrichissent l'apercu Live TV via `tvg-id`/nom de chaine; la zone EPG de l'apercu est scrollable si la liste est longue.

Depuis le 2026-07-01, le splash tient compte de la source active: en M3U, il verifie le lien, synchronise et precharge Home + Live TV uniquement, sans afficher de faux statuts de chargement Films/Series. Les ecrans Movies et Series affichent un etat vide explicite quand M3U est actif, au lieu d'une erreur Xtream. Live TV reconnait un lien M3U comme source jouable meme sans compte Xtream local.

Depuis le 2026-07-01, les lignes Live TV affichent un petit badge bleu `E` a droite quand des programmes EPG locaux sont disponibles pour la chaine. Le header des categories Live TV remplace la recherche dossier par un bouton `EPG`; active, il n'affiche que les dossiers contenant au moins une chaine avec EPG local disponible.

## 3. Workflow utilisateur

- Live TV: categories a gauche, chaines, apercu/mini player; OK lance l'apercu, OK sur la meme chaine ouvre le plein ecran.
- Films: categories conservees, grille de cards, detail film, puis lecteur.
- Series: categories conservees, detail serie avec saisons/episodes, puis lecteur episode.
- Favoris et Historiques sont des categories speciales.
- Si aucun compte Xtream n'est configure, l'utilisateur voit un QR de configuration.
- Si la verification Xtream de demarrage echoue, Home reste accessible mais Live TV / Movies / Series et les reprises de lecture sont bloques avec popup et overlay.
- Si une route lecteur est atteinte malgre un ancien cache local, un buffering persistant doit etre converti en etat Xtream indisponible, message utilisateur et anomalie; le spinner ne doit pas tourner indefiniment.

## 4. Workflow technique

Xtream:
- `XtreamApiService.kt` appelle `player_api.php`.
- `XtreamRepository.kt` gere les appels remote.
- `DefaultCatalogRepository.kt` consolide remote/cache.
- `SynchronizeCatalogUseCase` declenche la synchro.
- `XtreamUrlFactory.kt` construit les URL de lecture.
- `data/xtream/XtreamConnectionManager.kt` centralise l'etat connecte / erreur reseau / identifiants invalides / reponse invalide.

M3U / EPG:
- `data/playlist/M3uPlaylistClient.kt` telecharge et parse les playlists M3U.
- `data/playlist/EpgRepository.kt` telecharge, parse et cache les programmes XMLTV.
- `DefaultCatalogRepository.kt` choisit la branche de synchronisation selon `PlaylistSource`.

Room:
- `SVDatabase.kt` version 8.
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
- `data/playlist/M3uPlaylistClient.kt`
- `data/playlist/EpgRepository.kt`
- `data/repository/UserContentRepository.kt`
- `data/local/SVDatabase.kt`
- `data/local/dao/*`
- `data/local/entity/*`
- `domain/usecase/UseCases.kt`
- `domain/model/CategoryHistoryPolicy.kt`
- `sync/SyncWorker.kt`
- `startup/PlaylistSyncWorker.kt`
- `data/xtream/XtreamConnectionManager.kt`
- `data/xtream/XtreamConnectionNotifier.kt`

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
- M3U Live: URL directe issue de la ligne suivant `#EXTINF`, stockee dans `live_streams.directStreamUrl`.
- EPG XMLTV: rapprochement par `tvg-id`/nom de chaine et affichage dans l'apercu Live TV.

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
- Ne pas publier un catalogue partiel apres echec de synchronisation. La synchro Xtream actuelle ecrit Live, Movies, Series et `SyncState` dans une transaction Room unique; les sections sont traitees en serie et en batchs pour reduire les listes simultanees en RAM.
- Une seule source catalogue peut etre active: Xtream ou M3U. Activer une source desactive l'autre cote preference locale.
- M3U alimente Live TV uniquement; ne pas fabriquer des films/series sans structure fiable.
- Quand M3U est actif, ne pas afficher de messages d'erreur Xtream sur Movies/Series; afficher un etat vide source-aware.
- Le badge EPG des lignes Live TV doit se baser sur les programmes locaux disponibles, pas seulement sur la presence d'une URL EPG.
- Ne pas lancer de synchronisation globale pendant la navigation Home / Live TV / Movies / Series / categories / listes.
- Ne pas charger un snapshot complet Live TV / Movies / Series pour ouvrir un ecran catalogue; utiliser categories/counts puis pages Room locales.
- Le premier chargement local au startup doit rester leger: categories/counts, premieres pages locales bornees `96/72/72` et Home borne `10 historiques / 10 tendances films / 10 tendances series`. Ne pas remettre EPG reseau ni snapshots complets Movies/Series dans `MainActivity` pour les tres gros catalogues.
- Ne pas confondre prechargement local et synchro reseau: reconstruire le cache memoire depuis Room au demarrage est normal; retelecharger le catalogue complet ne doit arriver que si la politique de frequence le demande ou si l'utilisateur lance Synchroniser.
- Ne pas promettre zero chargement local apres fermeture complete: si le process Android a ete tue, le cache RAM n'existe plus et doit etre reconstruit depuis Room.
- AutoSync et sync manuelle doivent verifier Xtream avant de synchroniser; seules les erreurs reseau sont retentees automatiquement.
- La verification de connexion Xtream est obligatoire au premier affichage actif, mais ne doit pas forcer une resynchronisation globale si la politique de frequence ne la demande pas.
- Les routes player/detail doivent aussi respecter le blocage Xtream; ne pas compter uniquement sur Home/Header pour bloquer l'acces.
- Les diagnostics de synchro doivent rester non intrusifs: pas de credentials dans `logcat`, seulement les compteurs catalogue et les valeurs memoire Runtime.

## 10. Problemes connus

- Les loaders doivent tenir compte de `visibleItems` pour eviter un blocage malgre le contenu partiel.
- Les changements de focus pendant fermeture lecteur peuvent provoquer des erreurs Compose si un FocusRequester survit a son composable.
- Les crashes playback doivent etre diagnostiques depuis fatal stack ou anomalies avant correction UI.
- Capture Firestick 2026-07-01 avant optimisation: une synchro Xtream tres volumineuse pouvait atteindre environ 118 Mo utilises sur 128 Mo max avant l'ecriture Room, puis provoquer ANR/OOM.
- Capture Firestick 2026-07-01 apres optimisation candidate: deux synchronisations du catalogue `21769 Live / 104005 Movies / 24325 Series` ont reussi sans OOM ni ANR; le pic `SVSyncMemory` observe est descendu a environ 59 Mo pendant `after_get_movies`.
- Les ecrans catalogue utilisent maintenant une pagination Room locale; si une lenteur persiste, mesurer le temps des requetes page par page et l'effet des filtres recherche/parental.

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
- 2026-06-30: ajout verification Xtream rapide au demarrage, gating Home/Header, anomalies/notification locale, navigation catalogue locale-only et synchro staging avant remplacement local.
- 2026-06-30: correction cache ancien + serveur Xtream KO: controle backend puis Xtream au demarrage, routes player/detail bloquees, buffering persistant transforme en anomalie `XTREAM_FAILED`.
- 2026-06-30: ajout d'un cache memoire de snapshots catalogue locaux pour accelerer les retours Live TV / Movies / Series sans resynchronisation reseau.
- 2026-06-30: ajout progression detaillee Live TV / Films / Series dans `SyncStatus` pour le popup manuel de synchronisation Xtream du profil.
- 2026-07-01: deplacement du prechargement Home / Live TV / Movies / Series dans `SplashActivity`, apres la verification de fraicheur de synchronisation.
- 2026-07-01: premiere etape EPG: stockage/affichage de l'URL EPG dans Info compte et transfert web vers TV, sans encore traiter les donnees XMLTV.
- 2026-07-01: ajout de la source active Xtream/M3U exclusive, parsing M3U vers Live TV, URL directe en Room et cache EPG XMLTV affiche dans l'apercu Live TV.
- 2026-07-01: correction M3U source-aware: splash Home/Live uniquement, Movies/Series en etat vide explicite, popup sync M3U avec lien M3U, et badge `E` sur les chaines avec EPG.
- 2026-07-01: clarification optimisation demarrage: Room persiste sur l'appareil, le cache memoire se reconstruit au splash, et la synchro reseau doit dependre de la frequence configuree.
- 2026-07-01: ajout instrumentation `SVSyncMemory` et script ADB Firestick pour mesurer les pics memoire pendant la synchronisation Xtream.
- 2026-07-01: optimisation candidate Firestick: synchro Xtream par sections/batchs dans une transaction Room, counts/categories via SQL, splash limite aux categories sans Home/EPG/snapshots complets, et bouton EPG dans les categories Live TV.
- 2026-07-01: Live TV / Movies / Series passent en chargement pagine Room; Home ne lit plus les snapshots complets Movies/Series pour ses tendances.
- 2026-07-02: prechauffage splash des categories et premieres pages locales bornees, avec cache memoire reutilise par Live TV / Movies / Series pour reduire le loader initial apres synchro.
- 2026-07-02: tendances Home sans filtre note par defaut; filtre adulte conserve, validation lecture conservee, filtre poster paysage applique au chargement Home et parametres exposes par `api/app_config.php`.
- 2026-07-02: prechauffage startup deplace dans `MainActivity`; `SplashActivity` et l'ecran handoff Compose ne sont plus utilises.
