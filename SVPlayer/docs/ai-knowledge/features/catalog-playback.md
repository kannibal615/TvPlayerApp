# Catalogue, Playlist et Lecture

Derniere mise a jour: 2026-07-03.

## 1. Objectif

Documenter le chargement Xtream ou M3U, le cache Room, les ecrans Live TV / Films / Series, l'EPG XMLTV, les favoris, l'historique, la progression et la lecture ExoPlayer.

## 2. Fonctionnement actuel

SmartVision charge les contenus depuis la source active choisie par l'utilisateur: Xtream ou M3U, jamais les deux simultanement. Les donnees sont synchronisees vers Room et affichees dans les ecrans Compose actifs sous `ui/*`. La lecture plein ecran passe par Media3 ExoPlayer.

Les ecrans actifs sont routes par `ui/navigation/AppNavigation.kt`. Ne pas modifier une ancienne architecture sans verifier qu'elle est importee par la navigation active.

Depuis le 2026-06-30, la navigation Home / Live TV / Movies / Series ne doit plus declencher de telechargement Xtream global ni charger les categories depuis le reseau. Ces ecrans lisent le catalogue local Room via `CatalogRepository`. Les appels reseau Xtream sont limites aux controles rapides de disponibilite et aux synchronisations catalogue autorisees.

Depuis le 2026-07-01, les ecrans Live TV / Movies / Series ne doivent plus ouvrir un snapshot complet du catalogue. Le startup Compose de `MainActivity` prechauffe uniquement les categories/counts, puis les ViewModels catalogue chargent les contenus par pages depuis Room avec `LIMIT/OFFSET`. La RAM ne doit pas contenir tout le catalogue pour ouvrir un ecran.

Home charge ses donnees legeres hors splash: slides, historique recent limite a `10` et tendances films/series `10 + 10` depuis Room. `HomeViewModel` initialise `HomeUiState` avec les caches disponibles puis rafraichit les donnees locales apres le premier rendu si necessaire. Ne pas reutiliser les snapshots complets Movies / Series pour initialiser Home.

Depuis le 2026-07-03, le startup Compose de `MainActivity` ne lance aucune synchronisation et ne charge plus le catalogue pendant le splash. Il lit seulement l'activation locale, efface toute demande startup residuelle et rend `AppNavigation` / Home immediatement apres le splash. La decision de synchronisation automatique est prise par Home apres son premier rendu: seule une vraie `Synchronize` peut bloquer la telecommande.

Depuis le 2026-07-03, les tendances Home ne recalculent plus `trending_media`, ne valident plus les URL de lecture et ne demandent plus les details Xtream. Home affiche `10` films et `10` series aleatoires depuis Room, hors marqueurs adultes, avec uniquement les metadonnees locales disponibles.

Depuis le 2026-07-03, les cards Tendances HOME peuvent preparer des metadonnees premium apres affichage: `get_vod_info` pour les films, `get_series_info` pour les series, premier episode disponible pour le sample preview. Cette preparation est bornee aux items visibles/proches ou focussees et utilise le cache Room `home_trending_preview_cache`. Ce cache ne stocke pas les URLs de lecture brutes contenant les credentials; il stocke seulement type/id/extension/position de depart, puis l'URL est reconstruite en memoire.

Clarification stockage/performance:
- Room est le stockage local persistant de l'application sur l'appareil; les catalogues synchronises restent disponibles apres fermeture ou redemarrage de l'app tant que les donnees de l'application ne sont pas effacees.
- Le cache memoire applicatif est uniquement en RAM; il peut garder de petites pages deja ouvertes, mais ne doit pas garder tout le catalogue pour l'ouverture d'ecran.
- Le chargement local post-startup ne doit pas bloquer Home ni la telecommande. Les ecrans Live TV / Movies / Series lisent d'abord `20` categories Room maximum, puis completent discretement la liste complete de categories dans leur ViewModel.
- Apres une vraie reouverture ou un process tue par Android, l'UI doit relire Room, mais uniquement par petits jeux de donnees pagines.
- La synchronisation reseau complete est separee du chargement local et depend de `SyncFrequencyPolicy`: `A chaque demarrage` force une synchro a chaque ouverture, `24h`/`48h` ne resynchronisent que si la derniere synchro est obsolete, `Manuelle`/`Jamais` evitent la synchro automatique.
- Recommandation d'optimisation: ne remettre aucun prechauffage Room dans le splash; preferer une frequence `24h` ou `48h` pour eviter les telechargements reseau inutiles tout en gardant les catalogues frais.
- Les index Room sur `categoryId`, tri par numero/titre/nom et `episodes.seriesId` supportent le chargement pagine local.

Depuis le 2026-06-30, la synchronisation manuelle depuis Info compte publie aussi une progression par section Live TV / Films / Series dans `SyncStatus`. Les compteurs de l'ancienne synchro servent d'estimation de progression; chaque section passe a 100% quand son endpoint principal est termine.

Depuis le 2026-07-01, la synchronisation Xtream emet aussi des logs memoire `SVSyncMemory` pour diagnostiquer les OOM Firestick. Les jalons couvrent le debut de synchro, les appels compte/categories/Live/Films/Series, les ecritures Room par section, l'invalidation du cache local et les erreurs. Ces logs ne changent pas le comportement utilisateur et ne doivent pas exposer les identifiants Xtream.

Depuis le 2026-07-01, un lien M3U peut devenir la source active du catalogue Live TV. Les entrees M3U sont parsees depuis `#EXTINF`, groupees par `group-title`, stockees dans `live_streams` avec `source = m3u` et `directStreamUrl`, puis lues sans `XtreamUrlFactory`. Quand M3U est actif, Movies et Series sont vides pour respecter la regle une seule source active.

Depuis le 2026-07-01, l'URL EPG XMLTV est telechargee dans un cache local borne lors d'une synchronisation manuelle ou catalogue, mais pas pendant le splash. Les programmes enrichissent l'apercu Live TV via `tvg-id`/nom de chaine; la zone EPG de l'apercu est scrollable si la liste est longue.

Depuis le 2026-07-03, le splash ne verifie plus le lien M3U, ne synchronise plus et ne precharge plus Home/Live TV. Les ecrans Movies et Series affichent toujours un etat vide explicite quand M3U est actif, au lieu d'une erreur Xtream. Live TV reconnait un lien M3U comme source jouable meme sans compte Xtream local.

Depuis le 2026-07-01, les lignes Live TV affichent un petit badge bleu `E` a droite quand des programmes EPG locaux sont disponibles pour la chaine. Depuis le 2026-07-03, les dossiers Live TV avec EPG n'ajoutent plus de badge separe: leur compteur de chaines est affiche dans un cadre bleu. Le header des categories Live TV remplace la recherche dossier par un bouton `EPG`; active, il n'affiche que les dossiers contenant au moins une chaine avec EPG local disponible.

## 3. Workflow utilisateur

- Live TV: categories a gauche, chaines, apercu/mini player; OK lance l'apercu, OK sur la meme chaine ouvre le plein ecran.
- Films: categories conservees, grille de cards, detail film, puis lecteur.
- Series: categories conservees, detail serie avec saisons/episodes, puis lecteur episode.
- Favoris et Historiques sont des categories speciales.
- Si aucun compte Xtream n'est configure, l'utilisateur voit un QR de configuration.
- Si la verification Xtream de demarrage echoue apres 3 essais confirmes, Home reste accessible mais Live TV / Movies / Series et les reprises de lecture sont bloques avec popup et overlay.
- Si une route lecteur est atteinte malgre un ancien cache local, un buffering persistant doit afficher une erreur locale puis lancer une confirmation Xtream; il ne doit pas marquer toute la connexion KO avant les 3 essais.

## 4. Workflow technique

Xtream:
- `XtreamApiService.kt` appelle `player_api.php`.
- `XtreamRepository.kt` gere les appels remote.
- `DefaultCatalogRepository.kt` consolide remote/cache.
- La synchronisation Films tente d'abord `get_vod_streams` global. Si le fournisseur renvoie `0` film alors que des categories VOD existent, elle recharge les films par `category_id`, assigne le dossier aux reponses sans `category_id`, deduplique par `stream_id`, puis importe Room. Ne pas remplacer ce fallback sans verifier les providers Xtream qui ne servent pas l'endpoint VOD global.
- Depuis le 2026-07-03, les URL d'images catalogue sont normalisees a l'entree et a la sortie Room: `stream_icon`, `cover`, posters et backdrops Xtream acceptent URL absolue, URL `//`, chemin absolu `/...`, chemin relatif, espaces et slashes echappes. Les valeurs vides comme `null`, `n/a`, `none` ou `0` sont ignorees. Cette logique restaure les logos Live TV et posters Films/Series quand un fournisseur ne renvoie pas directement une URL HTTP complete.
- `SynchronizeCatalogUseCase` declenche la synchro.
- `XtreamUrlFactory.kt` construit les URL de lecture.
- `data/xtream/XtreamConnectionManager.kt` centralise l'etat connecte / erreur reseau / identifiants invalides / reponse invalide.

M3U / EPG:
- `data/playlist/M3uPlaylistClient.kt` telecharge et parse les playlists M3U.
- Les logos `tvg-logo` M3U relatifs sont resolus contre l'URL du fichier M3U avant stockage Room.
- `data/playlist/EpgRepository.kt` telecharge, parse et cache les programmes XMLTV.
- `DefaultCatalogRepository.kt` choisit la branche de synchronisation selon `PlaylistSource`.

Room:
- `SVDatabase.kt` version 8.
- Entites: profiles, categories, live streams, movies, series, episodes, favorites, playback progress, sync state, historique YouTube.
- `home_trending_preview_cache` stocke les metadonnees premium des cards Tendances HOME: poster, backdrop, duree, sample preview, extension, position 15%/fallback, etats backdrop/preview, `lastSync`.

Playback:
- `FullScreenPlayerScreen.kt` gere Live, Movie et Episode.
- Live peut utiliser `.ts` et fallback `.m3u8`.
- Films/series sauvegardent la progression.
- Depuis le 2026-07-03, la sauvegarde d'un episode conserve les metadonnees d'historique existantes et enrichit depuis Room quand possible: titre de serie, label saison/episode, poster de serie et `parentContentId`. Le lecteur utilise aussi le cache de details Xtream deja charge pour eviter un titre generique `Series` quand l'episode vient d'un detail serie. Si une ancienne ligne historique possede deja `parentContentId`, `UserContentRepository.enrichProgress()` peut reparer titre/image depuis la serie Room meme si l'entite episode locale n'existe pas encore. La categorie speciale Series > Historique ne doit plus afficher des episodes sous forme `Episode <id>` ou `Series` sans image quand les metadonnees serie sont disponibles.
- Depuis le 2026-07-03, Live TV > Historique rehydrate ses lignes depuis Room via les ids regardes quand possible. Les lignes historiques recuperent ainsi `epgChannelId`, programmes EPG, badge `E` et details sous mini-player comme les dossiers Live TV normaux.
- Overlay plein ecran Live TV: la video reste plein ecran; numero de chaine reel en haut droite seulement quand il est connu, bandeau bas rectangulaire full-width plus bas/compact avec effet verre, logo chaine sans cadre, nom legerement reduit, ligne EPG locale si disponible et boutons `EPG`, `Settings`, `Record`, `Back to List` serres a droite. Le logo SmartVision n'est pas affiche dans cet overlay. Live TV n'affiche plus pause, -10s, +10s, fullscreen ni barre de progression. `Record` est un placeholder avec TODO DVR.
- Overlay plein ecran Films/Series: conserve le modele glassmorphism avec barre de progression et controles de lecture VOD/episodes.

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
- Le premier chargement local ne doit plus etre dans `MainActivity`: les categories initiales sont limitees a `20` par section dans les ViewModels Live TV / Movies / Series, puis le reste charge discretement apres premier affichage. Ne pas remettre les details tendances Xtream, EPG reseau ni snapshots complets Movies/Series dans `MainActivity` pour les tres gros catalogues.
- Ne pas confondre prechargement local et synchro reseau: reconstruire le cache memoire depuis Room au demarrage est normal; retelecharger le catalogue complet ne doit arriver que si la politique de frequence le demande ou si l'utilisateur lance Synchroniser.
- Ne pas bloquer la telecommande pendant un chargement local Room; le blocage Home est reserve aux synchronisations reseau catalogue.
- Ne pas stocker d'URL de lecture brute en base pour les previews HOME; garder uniquement les ids/extensions et reconstruire via `XtreamUrlFactory`.
- Ne pas promettre zero chargement local apres fermeture complete: si le process Android a ete tue, le cache RAM n'existe plus et doit etre reconstruit depuis Room.
- AutoSync et sync manuelle doivent verifier Xtream avant de synchroniser; seules les erreurs reseau confirmees sont retentees automatiquement.
- La verification de connexion Xtream est obligatoire au premier affichage actif, mais elle reste non bloquante pendant ses essais silencieux et ne doit pas forcer une resynchronisation globale si la politique de frequence ne la demande pas.
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
- 2026-07-02: tendances Home prechargees en cache final pendant le startup via `HomeContentRepository`; `HomeViewModel` consomme ce cache pour eviter une deuxieme passe details/backdrop a l'ouverture Home. La cle de cache tient compte source/compte/derniere synchro et les details sont charges avec concurrence bornee.
- 2026-07-03: correction performance Splash/Home: les details tendances Xtream ne bloquent plus le splash; Home demarre depuis les caches disponibles et rafraichit les tendances apres le rendu initial.
- 2026-07-03: suppression de toute synchro et de tout chargement catalogue dans le splash; Home s'affiche immediatement, decide la synchro automatique apres premier rendu, bloque la telecommande uniquement pendant `Synchronize`, les categories initiales sont limitees a `20` par section et les tendances deviennent `10` films + `10` series aleatoires depuis Room hors adulte.
- 2026-07-03: ajout du cache `home_trending_preview_cache` pour enrichir progressivement les cards Tendances HOME avec backdrop, duree et preview 15%, sans prechargement splash ni URL brute stockee.
- 2026-07-03: correction historique Series: les progres episodes preservent/enrichissent titre de serie, poster, label episode et parent serie avant upsert Room; le lecteur relit aussi le cache details Xtream et les anciennes lignes avec `parentContentId` peuvent etre reparees depuis la serie Room.
- 2026-07-03: correction synchro Films Xtream avec fallback `get_vod_streams` par categorie quand l'endpoint global renvoie 0, et correction Live TV EPG pour historique, compteurs dossiers et details scrollables.
- 2026-07-03: normalisation des URL d'images catalogue Xtream/M3U pour restaurer logos chaines, posters Films/Series et backdrops details/Home quand les providers renvoient des chemins relatifs ou echappes.
- 2026-07-04: verification Xtream durcie contre les faux positifs: 3 essais silencieux, test principal sur `user_info`, pas de blocage/popup avant echec confirme, et buffering lecteur separe d'une panne globale.
- 2026-07-05: overlay plein ecran Live TV remplace puis affine en modele dedie direct: bandeau bas rectangulaire compact, logo chaine sans cadre, numero reel, EPG local courant, panneau Settings aspect ratio enrichi, Record placeholder et suppression des controles VOD.
