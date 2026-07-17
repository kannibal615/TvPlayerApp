# Catalogue, Playlist et Lecture

Derniere mise a jour: 2026-07-17.

Les colonnes centrales Live TV, Movies et Series utilisent une recherche compacte: un bouton carre ouvre le champ et le clavier TV uniquement apres validation. Leur filtre partage le visuel `FilterList` de la colonne Categories Live et le titre indique le nombre d'elements du dossier actif. Les lignes Movies affichent note, genre, annee et duree; leur matching TMDB retire d'abord les prefixes fournisseur courts (`EX -`, `VOD -`, etc.) et l'annee entre parentheses, puis isole chaque echec d'enrichissement afin que les lignes suivantes continuent. Les lignes Series enrichissent progressivement backdrop paysage, saisons et episodes sans attendre un clic. Si Room ne contient pas encore les episodes d'une serie, leur comptage Xtream est charge en arriere-plan avec concurrence bornee. Le poster final du mini-player Series n'utilise plus le cover portrait comme fallback paysage.

La fiche Movie formate la duree en `xh xxm`, masque le badge d'extension conteneur et agrandit le casting informatif non focusable avec portraits `2:3` et noms, sans ajouter de scroll global.

Depuis le 2026-07-17, l'EPG XMLTV est cache par URL afin d'isoler les profils. Le parseur respecte l'offset XMLTV, rejette les programmes termines ou sans horaire exploitable et conserve au plus le programme courant puis les programmes futurs. La lecture refiltre avec l'heure courante: la preview Live TV ne montre donc plus d'emission terminee. Toute synchronisation catalogue reussie declenche au meme endroit un rafraichissement EPG stale-aware non bloquant; le Worker reseau horaire et le refresh au clic chaine restent actifs, sans double telechargement M3U.

`SyncStatus.SyncSectionProgress` publie maintenant le message d'etape, le total connu et la phase `FILTERING` pour les profils Kids. Les cartes Home affichent profil Kids, duree ecoulee, compteurs et etape reelle; un appel reseau au total inconnu conserve son libelle au lieu de simuler une progression lineaire.

## Catalogue Kids

- `KidsContentFilter` separe les regles categorie, chaine Live et contenu VOD. Il centralise normalisation Unicode, dictionnaires multilingues, alias complets de chaines, score explicable et exclusions adultes prioritaires; `RuleVersion = 2` invalide les anciennes decisions.
- `KidsCatalogFilterEngine` evalue chaque categorie une fois. Une categorie `SAFE_KIDS_CATEGORY` transmet son statut aux items sans appeler le score individuel; seule une garde adulte locale et certaine reste appliquee. Les categories ambigues/non Kids utilisent le score local de l'item sans TMDB ni requete reseau.
- Room v16 ajoute `kids_category_decisions` et `kids_item_decisions`, indexes par source/type/categorie ou decision. La source est empreintee sans credentials bruts et la cle n'inclut pas `profileId`: deux profils Kids utilisant la meme source partagent donc les classifications.
- Les empreintes categorie/metadonnees et la version des regles rendent le traitement incremental. Les ecritures contenus/categories et decisions sont groupees par lots de `256` dans la transaction de section.
- Pour un profil `KIDS`, les listes Xtream ou M3U sont filtrees pendant l'import Room. Seuls les items autorises et leurs categories non vides sont persistes sous le `profileId` Kids; le chemin des profils standards reste inchange.
- Le log sanitise `SVKidsFilter` publie categories traitees/evaluees/cachees, categories Kids, items herites/analyses/cachees/ambigus/rejetes, duree et `networkRequests=0`.
- Recherches, favoris, historiques, reprises, tendances et Home reutilisent donc les repositories existants avec des donnees deja filtrees.
- Les routes detail film/serie verifient que l'ID existe dans le catalogue Room du profil Kids avant de monter l'ecran; les lecteurs exigent aussi une entite locale via `buildPlaybackRequest`.
- Une suppression de profil efface categories, medias, episodes, favoris, progression, tendances, caches, mappings TMDB, synchro et historique YouTube du `profileId`.

## 1. Objectif

Documenter le chargement Xtream ou M3U, le cache Room, les ecrans Live TV / Films / Series, l'EPG XMLTV, les favoris, l'historique, la progression et la lecture ExoPlayer.

## 2. Fonctionnement actuel

SmartVision charge les contenus depuis la source active choisie par l'utilisateur: Xtream ou M3U, jamais les deux simultanement. Les donnees sont synchronisees vers Room et affichees dans les ecrans Compose actifs sous `ui/*`. La lecture plein ecran passe par Media3 ExoPlayer.

Les ecrans actifs sont routes par `ui/navigation/AppNavigation.kt`. Ne pas modifier une ancienne architecture sans verifier qu'elle est importee par la navigation active.

Depuis le 2026-06-30, la navigation Home / Live TV / Movies / Series ne doit plus declencher de telechargement Xtream global ni charger les categories depuis le reseau. Ces ecrans lisent le catalogue local Room via `CatalogRepository`. Les appels reseau Xtream sont limites aux controles rapides de disponibilite et aux synchronisations catalogue autorisees.

Depuis le 2026-07-01, les ecrans Live TV / Movies / Series ne doivent plus ouvrir un snapshot complet du catalogue. Le startup Compose de `MainActivity` prechauffe uniquement les categories/counts, puis les ViewModels catalogue chargent les contenus par pages depuis Room avec `LIMIT/OFFSET`. La RAM ne doit pas contenir tout le catalogue pour ouvrir un ecran.

Home charge ses donnees legeres hors splash: slides, historique recent limite a `10` et tendances films/series `10 + 10` depuis Room. Les snapshots memoire sont valides par `profileId`; au changement de profil, l'ancien contenu et le player preview sont arretes avant la nouvelle emission Room.

Depuis le 2026-07-03, le startup Compose de `MainActivity` ne lance aucune synchronisation et ne charge plus le catalogue pendant le splash. Il lit seulement l'activation locale, efface toute demande startup residuelle et rend `AppNavigation` / Home immediatement apres le splash. La decision de synchronisation automatique est prise par Home apres son premier rendu: seule une vraie `Synchronize` peut bloquer la telecommande.

Depuis le 2026-07-12, `trending_media` est recalcule uniquement a la fin d'une synchronisation reussie. La selection `10` films + `10` series est persistante par `profileId`, deterministe (note, date d'ajout, annee, id), completee par les categories nouveautes normalisees ou la date d'ajout, et filtree contre les marqueurs adultes avant sauvegarde et lecture.

Les cards Tendances HOME prechargent de facon bornee les metadonnees des items visibles/proches, independamment du focus. Apres `550 ms` de focus stable, le player peut utiliser le lien film Xtream ou le premier episode Xtream disponible. Le focus ne declenche plus de requete et ne remplace jamais `item.imageUrl` par le backdrop de preview. Le cache Room `home_trending_preview_cache` stocke seulement type/id/extension/position; l'URL credentialisee est reconstruite en memoire.

Clarification stockage/performance:
- Room est le stockage local persistant de l'application sur l'appareil; les catalogues synchronises restent disponibles apres fermeture ou redemarrage de l'app tant que les donnees de l'application ne sont pas effacees.
- Le cache memoire applicatif est uniquement en RAM; il peut garder de petites pages deja ouvertes, mais ne doit pas garder tout le catalogue pour l'ouverture d'ecran.
- Le chargement local post-startup ne doit pas bloquer Home ni la telecommande. Les ecrans Live TV / Movies / Series lisent d'abord `20` categories Room maximum, puis completent discretement la liste complete de categories dans leur ViewModel.
- Apres une vraie reouverture ou un process tue par Android, l'UI doit relire Room, mais uniquement par petits jeux de donnees pagines.
- La synchronisation reseau complete est separee du chargement local et depend de `SyncFrequencyPolicy`: `A chaque demarrage` est consomme une seule fois par instance d'application, pas a chaque retour Home; `24h`/`48h` utilisent le dernier `sync_state` reussi et `Manuelle`/`Jamais` evitent la synchro automatique sauf catalogue absent.
- Recommandation d'optimisation: ne remettre aucun prechauffage Room dans le splash; preferer une frequence `24h` ou `48h` pour eviter les telechargements reseau inutiles tout en gardant les catalogues frais.
- Les index Room sur `categoryId`, tri par numero/titre/nom et `episodes.seriesId` supportent le chargement pagine local.

## Controle parental local

- `ParentalKeywordPolicy` est la regle canonique de normalisation: espaces nettoyes, ordre preserve, doublons insensibles a la casse refuses. `PlayerSettings.parentalKeywordValues` lit la nouvelle liste JSON DataStore; l'ancienne chaine separee reste disponible et est migree automatiquement pour compatibilite.
- Live TV, Films, Series et episodes utilisent les champs disponibles categorie, titre/nom, description et genre. Films ne filtre plus sur la note. Quand le controle est desactive, aucun resultat potentiel n'est expose.
- `RoomParentalCatalogRepository` materialise le resultat dans `parental_filter_snapshots` et `parental_hidden_items` (Room v17). Il groupe les dossiers par section/categorie, deduplique via `EXISTS`, utilise les cles stables `type:id` et pagine les elements par lots de `40` sans charger le catalogue complet en RAM.
- Le snapshot reste reutilise apres fermeture/reouverture de l'ecran et apres redemarrage de l'application. Il est regenere uniquement si le profil, l'empreinte des mots-cles ou le `sync_state.lastSync` du catalogue change; l'ouverture du panneau ne relance donc pas le filtrage.
- Le clic/OK sur un dossier de la colonne gauche charge uniquement ses elements masques dans la colonne droite. Le filtrage parental s'applique aussi aux dossiers `Historique` Live/Films/Series et a Home > `Continue watching`, en resolvant les champs locaux reels titre, resume, genre et categorie avant affichage.

Depuis le 2026-07-10, la synchronisation manuelle depuis Info compte publie une progression par section Live TV / Films / Series dans `SyncStatus`: phase, pourcentage et nombre d'elements importes. Les appels reseau Xtream restent hors transaction Room; seules les remplacements locaux par section sont transactionnels, avec progression emise apres chaque lot importe. Cette separation evite de verrouiller les lectures locales pendant les reponses lentes du fournisseur.

Depuis le 2026-07-01, la synchronisation Xtream emet aussi des logs memoire `SVSyncMemory` pour diagnostiquer les OOM Firestick. Les jalons couvrent le debut de synchro, les appels compte/categories/Live/Films/Series, les ecritures Room par section, l'invalidation du cache local et les erreurs. Ces logs ne changent pas le comportement utilisateur et ne doivent pas exposer les identifiants Xtream.

Depuis le 2026-07-10, le telechargement global Xtream `get_series` est borne par un timeout dur. Si ce endpoint global reste bloque alors que des categories Series existent, la synchronisation bascule vers `get_series&category_id=...`, complete les `category_id` manquants, deduplique par `series_id`, puis importe Room. Si le fallback par categorie bloque aussi, la synchro echoue proprement.

Depuis le 2026-07-01, un lien M3U peut devenir la source active du catalogue Live TV. Les entrees M3U sont parsees depuis `#EXTINF`, groupees par `group-title`, stockees dans `live_streams` avec `source = m3u` et `directStreamUrl`, puis lues sans `XtreamUrlFactory`. Quand M3U est actif, Movies et Series sont vides pour respecter la regle une seule source active.

Depuis le 2026-07-01, l'URL EPG XMLTV est telechargee dans un cache local borne lors d'une synchronisation manuelle ou catalogue, mais pas pendant le splash. Depuis le 2026-07-05, `EpgRepository.synchronizeIfStale(url, minAgeMs)` conserve `lastSuccessAt` et `urlHash`, un `EpgSyncWorker` horaire rafraichit l'EPG avec contrainte reseau sans synchronisation catalogue complete, et le clic chaine tente un refresh stale-aware avant de rafraichir uniquement la chaine selectionnee dans l'UI.

Depuis le 2026-07-03, le splash ne verifie plus le lien M3U, ne synchronise plus et ne precharge plus Home/Live TV. Les ecrans Movies et Series affichent toujours un etat vide explicite quand M3U est actif, au lieu d'une erreur Xtream. Live TV reconnait un lien M3U comme source jouable meme sans compte Xtream local.

Depuis le 2026-07-09, `XtreamAccountManager` porte des profils playlist locaux et `PlaylistProfile` reste la source de verite des identifiants. Le pipeline d'activation partage invalide seulement les caches memoire et publie `catalogRevision`; Room reste isole par `profileId`. Un profil deja synchronise ne relance le reseau que lorsque 24h/48h est reellement echu.

Depuis le 2026-07-10, les caches de snapshot catalogue et les caches Xtream repository sont explicitement scopes par profil actif. Une synchronisation capture `profileId`, credentials et host au demarrage de la sync, ecrit Room pour ce profil uniquement, puis ne marque le profil synchronise que si le profil actif n'a pas change entre-temps. Les appels Xtream API peuvent recevoir un host override interne afin qu'une sync deja lancee ne bascule pas silencieusement vers le nouveau profil. Les URLs preview/player Movies/Series sont reconstruites depuis le `PlaybackRequest` issu du catalogue Room actif; ne pas reutiliser une URL deja construite par un autre profil.

Depuis le 2026-07-12, `CatalogRepository.synchronize(profileId)` accepte un profil cible explicite. Info compte peut synchroniser un profil secondaire sans l'activer; credentials resolus, catalogue, filtre Kids, tendances et `sync_state` utilisent tous le profil cible. Historique et favoris ne sont pas modifies.

Depuis le 2026-07-10, les playlists envoyees depuis le site web alimentent le profil `PlaylistWeb`. Si ce profil existe encore sous ce nom, il est mis a jour; s'il a ete renomme, un nouveau profil `PlaylistWeb` est cree. Quand les credentials Xtream ou le lien M3U changent pour le meme profil, la navigation considere l'ancien catalogue local comme stale et relance la synchronisation; un simple changement vers un autre profil deja synchronise reste cache-first.

Depuis le 2026-07-10, la creation d'un nouveau `PlaylistWeb` ne remplace plus le profil actif existant. Dans Who's watching, OK active seulement le profil et ouvre Home sans attendre le reseau. Si le profil choisi n'a aucun catalogue local valide, Home lance apres son premier rendu la synchronisation classique visible sur les cards Live TV / Films / Series. Un profil possedant deja un `sync_state` reussi et du contenu Room ne resynchronise pas lors de sa selection.

Depuis le 2026-07-05, Live TV conserve le layout 3 zones `Categories / Chaines / Apercu`, mais les panneaux sont plus compacts, le header categories n'a plus de bouton `EPG`, le header chaines garde seulement le titre et la recherche, et les lignes affichent une numerotation relative au dossier visible (`Tous`, `Favoris`, `Historique` ont chacune leur numerotation). Le badge EPG texte est remplace par l'image `res/drawable-nodpi/ic_epg_badge.png`, sans deformation. Les logos chaines reels n'ont plus de fond/cadre; seul le fallback texte garde un fond discret.

Depuis le 2026-07-12, le header Categories Live TV expose des filtres rapides generes uniquement depuis les prefixes de dossiers reels. `CategoryCodeParser` accepte un code de deux lettres explicitement delimite au debut, `CategoryFilterResolver` applique les priorites SmartVision (`AR` = Monde arabe, regions `EU/AS/AF`, alias `UK -> GB`) avant la resolution ISO, et `FlagResolver` reste entierement local avec fallback texte. `LiveTvUiState.categories` demeure la liste source intacte; `visibleCategories` est une projection filtree. Un changement de profil/catalogue reconstruit les filtres et revient a Tous si le filtre actif disparait.

Depuis le 2026-07-05, l'apercu Live TV met `Regarder`, `Favori` et, uniquement dans `Historique`, `Supprimer` dans le header de la zone Apercu en boutons carres icon-only. Les lignes Historique n'ont plus de bouton supprimer inline et gardent le format des autres chaines. La confirmation existante reste utilisee; apres suppression, la selection cible la chaine suivante visible, sinon la precedente, sinon vide l'apercu.

Depuis le 2026-07-05, le mini-player Live TV de l'apercu est plus compact: radius reduit, plus de badge `LIVE` ni logo haut droit, overlay bas fixe transparent avec logo, separateur, nom chaine, EPG courant ou categorie et horaires. L'ancien bloc `En cours` sous le mini-player est supprime. Les programmes EPG sont des lignes focusables avec titre gras a gauche, horaires a droite, separateurs, et OK ouvre/ferme le detail en rideau seulement si une description existe.

Depuis le 2026-07-06, l'ouverture Live TV garde le layout reel en skeleton shimmer pendant au moins 1 seconde et jusqu'a readiness categories, sans afficher les vraies donnees avant la fin. La selection initiale ne cible plus `Historique`: elle ouvre le premier dossier situe apres `Historique`, sinon la premiere categorie disponible. L'icone EPG des lignes utilise `ic_epg_premium.png`, genere localement en PNG transparent avec halo neon, et s'affiche plus petite pour ne pas impacter la hauteur des lignes. Quand une chaine selectionnee n'a aucun programme EPG local, la zone sous mini-player affiche un panneau informatif non focusable `A propos de la chaine` avec chaine, numero visible du dossier courant, categorie, source, EPG indisponible et pays detecte simplement; si l'EPG devient disponible, la liste EPG classique remplace ce panneau.

Depuis le 2026-07-06, Live TV corrige la selection automatique initiale quand un snapshot partiel precede la liste complete: tant que l'utilisateur n'a pas choisi manuellement de dossier, le ViewModel recalcule le premier dossier reel apres `Historique` dans l'ordre visuel final incluant `sortedByHistorySignals(...)`, puis annule/recharge le job chaines si la cible change. Les annulations normales de `channelsJob` ne sont plus converties en erreur visible `standaloneCoroutine was cancelled`. L'icone EPG utilise maintenant l'image fournie `epg a mettre.png` copiee telle quelle dans `ic_epg_premium.png`; les lignes sans EPG sous le mini-player perdent le titre/cadre/fond externe et s'integrent au style des lignes chaines.

Depuis le 2026-07-06, la recherche Live TV est pilotee par `LiveTvViewModel` et interroge Room par pages au lieu de filtrer uniquement les chaines deja chargees en memoire. `ALL` recherche dans tout `live_streams`, les categories normales recherchent dans leur `categoryId`, et `Favoris` / `Historique` gardent un filtre local sur leurs listes deja materialisees. La pagination reste a `96` elements et les annulations normales de recherche ne doivent pas devenir des erreurs visibles.

Depuis le 2026-07-07, Live TV garde le layout 3 colonnes mais stabilise la restauration de la categorie initiale cote UI: l'item selectionne est scrolle jusqu'a etre visible avant `requestFocus()`, pour eviter un decalage entre dossier selectionne, dossier focusse et dossier visible. Le loading interne de la colonne Chaines utilise un skeleton de lignes numerotees/logo/texte, distinct du skeleton global. Les logos chaines reels restent dans un conteneur fixe avec clipping et leger zoom `Fit` pour mieux exploiter l'espace sans deformation ni changement de hauteur de ligne. Sous le mini-player, la section avec EPG affiche le titre `Programme de la chaine`, l'icone EPG non focusable a droite, un separateur dedie et des lignes EPG plus compactes; la section sans EPG affiche `Info chaine`, le logo a droite du titre, des lignes informatives sans icones et sans ligne `EPG indisponible`. Quand aucune chaine n'est selectionnee en mode free ads, le bloc Premium est uniquement cosmetique: style dark navy/or, couronne dessinee, QR et code TV dynamiques conserves, sans bouton, prix ni CTA.

Depuis le 2026-07-17, `LiveTvViewModel` separe le job categories du job de page chaines. Une reapplication des categories ne peut plus annuler silencieusement le chargement initial `ALL`; une vraie annulation remet atomiquement les trois loaders a `false`, invalide la categorie chargee et relance depuis l'offset `0`.

Depuis le 2026-07-07, les lots TMDB 1 a 8 ajoutent une couche de metadonnees enrichies non bloquante pour Films et Series. Les associations Xtream -> TMDB sont stockees en Room dans `tmdb_content_mapping`, les details film dans `tmdb_movie_metadata` et les details serie dans `tmdb_series_metadata`. Les fiches films et series affichent les champs TMDB disponibles en priorite (poster, backdrop, synopsis, genres, duree, note et casting), tout en gardant Xtream comme fallback et source de lecture principale. Depuis le 2026-07-14, la fiche film s'arrete au casting photo et la fiche serie a la liste interne de quatre episodes visibles; les deux fiches tiennent sans scroll global. Depuis le 2026-07-17, les listes Movies/Series enrichissent tous les items de chaque page Room chargee, par petits lots avec concurrence bornee a `2`, et reutilisent le cache TMDB Room. Les lignes Films utilisent le backdrop paysage TMDB en priorite et leur preview reutilise les memes metadonnees. Le nombre de saisons TMDB (`number_of_seasons`) est conserve dans `tmdb_series_metadata` (Room v19) et affiche a droite du titre des lignes Series. TMDB est optionnel: sans token local, l'application garde le comportement Xtream.

Depuis le 2026-07-09, Movies et Series utilisent un layout catalogue 3 colonnes aligne sur Live TV: categories a gauche sans recherche de dossier, liste paginee au centre avec recherche Room locale, et panneau `Preview` a droite. Le premier OK selectionne le contenu et lance le mini-preview. Un deuxieme OK ouvre le player fullscreen pour un film, mais ouvre la fiche detail pour une serie depuis le 2026-07-14. Les boutons `Play` et `Details` du panneau preview restent accessibles au D-pad. La recherche centrale interroge Room par pages pour `ALL` et categories normales; `Favoris` et `Historique` restent filtres sur leurs listes utilisateur bornees.

Le mini-preview Movies/Series reconstruit l'URL de lecture en memoire avec l'id Xtream et l'extension stockee localement, utilise Media3 ExoPlayer avec fade-in audio local, tente une sequence 10% / 30% / 50% / 80% de la duree avec 15 secondes par segment, puis stoppe l'animation visuelle sur un backdrop TMDB si disponible, sinon poster recadre en 16:9. Pour les series, l'episode repris depuis l'historique est prioritaire; sinon le premier episode disponible sert au preview, au bouton `Play` et au deuxieme OK.

Depuis le 2026-07-09, Movies et Series reutilisent aussi le comportement Live TV pendant les chargements: skeleton 3 colonnes au premier chargement et skeleton de lignes dans la liste centrale pendant les rechargements. Quand aucun film/serie n'est selectionne en mode `FREE_WITH_ADS`, le panneau Preview affiche un mini-frame pub idle VAST puis une section Premium avec QR/code TV; hors free ads, il affiche un prompt SmartVision non focusable. Les lignes Films/Series privilegient le backdrop TMDB/Xtream comme image paysage 16:9 et retombent sur le poster seulement si aucun paysage n'est disponible. Le mini-preview VOD demarre par un court warmup au debut du flux avant de tenter les segments 10/30/50/80, et retombe sur le backdrop/poster sans message d'erreur quand un fournisseur refuse le seek preview. La section detail sous le mini-player est focusable et scrollable au D-pad. L'overlay fullscreen VOD garde progressbar et controles -15s/play/+15s, expose precedent/suivant quand un item adjacent existe, et le bouton Favori reflete/toggle l'etat reel.

Depuis le 2026-07-13, Live TV, Movies et Series utilisent les memes paddings, hauteurs de header/panneau, gaps et proportions `0.24 / 0.42 / 0.34`. Les titres centraux VOD sont fixes: `Movies` et `Series`, sans categorie ni compteur. Le composant de ligne VOD partage mesure `56dp`, affiche une miniature 16:9 croppee et laisse tout l'espace restant au titre/metadonnees avec seulement `10dp` a droite. Les backdrops restent prioritaires, avec poster/fallback stable.

Depuis le 2026-07-10, les items Movies/Series lus depuis Room reconstruisent les URLs preview/player avec `containerExtension` au lieu de retomber sur `mp4` par defaut. Cela protege notamment le dossier `ALL`, qui passe par les pages Room globales, pour les fournisseurs qui utilisent `mkv`, `avi` ou une autre extension.

Depuis le 2026-07-11, l'overlay fullscreen Films/Series conserve son design proportionnel 1680x945 mais corrige son contrat TV: DPAD Bas masque immediatement l'overlay sans interrompre Media3, Haut cible une progressbar focusable et seekable avec le halo bleu partage, et le slider luminosite remplace l'icone sur la meme ligne. Precedent/Suivant ne dependent plus du cache memoire Xtream: les films adjacents sont resolus dans l'ordre Room de la categorie courante et les episodes dans l'ordre saison/episode Room, y compris les passages inter-saisons. Les controles sans cible adjacente sont desactives.

Depuis le 2026-07-14, Films/Series affichent avant lecture un dialogue bloquant `Reprendre` / `Recommencer` des qu'une progression exploitable existe. Ce choix est resolu avant publicite, preparation Media3 et carte episode suivant; une reprise proche de la fin ne declenche pas automatiquement l'episode suivant. L'overlay VOD ajoute un bouton `Recommencer depuis le debut` dans la meme chaine D-pad que les autres controles.

Depuis le 2026-07-11, `LiveFullscreenControlsOverlay` remplace le bandeau glass fullscreen Live par la meme geometrie, le meme gradient, la meme progressbar, le meme halo, le meme slider et les memes icones que Films/Series. Il affiche logo/nom, EPG courant/suivant et progression temporelle EPG. La presence de -10/+10 et le focus de la barre dependent des capacites Media3 reelles reevaluees sur timeline/media/commandes: `isCurrentMediaItemSeekable`, `COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM`, fenetre live/dynamique et duree valide. Un flux non seekable garde seulement luminosite, chaine precedente, play/pause, chaine suivante et sortie fullscreen; sa barre EPG reste informative.

Depuis le 2026-07-07, `Media` ajoute une categorie `Media prives` separee des fichiers locaux. Android consomme uniquement `PrivateMediaRepository` vers les endpoints SmartVision `api/media/private/*`; aucun endpoint Eporner n'est appele depuis l'APK. Les sous-dossiers prives sont des categories/recherches admin (`Nouveautes`, `Populaires`, `Top semaine`, etc.) et la query/theme de chaque sous-dossier alimente `/items`. En mode prive, l'UI suit le fonctionnement YouTube: liste compacte et section principale dediee au player/miniature, premier OK lance le player inline, second OK ouvre le plein ecran. Depuis le 2026-07-08, les embeds prives utilisent `PrivateMediaTvWebView`: OK/Enter est d'abord laisse aux controles provider du WebView, avec fallback JS seulement si le WebView ne traite pas la touche. Le backend normalise les reponses provider en DTO internes et renvoie `HLS`/`MP4` uniquement pour des URLs directes compatibles; sinon `EMBED`/`PAGE_ONLY`/`UNAVAILABLE`. Le mode admin `Forcer lecture native HLS/MP4` est un mode de test base sur un vrai flux direct configure; il ne transforme pas un embed en flux. La fiche detail privee affiche les metadonnees et garde `Lecture indisponible` si le backend ne fournit pas un stream natif compatible.

## 3. Workflow utilisateur

- Live TV: categories a gauche, chaines, apercu/mini player; OK lance l'apercu, OK sur la meme chaine ouvre le plein ecran.
- Films: categories conservees, liste centrale paginee, preview a droite, puis lecteur fullscreen au bouton `Play` ou deuxieme OK sur la meme ligne. Le premier OK garde le focus sur la ligne selectionnee. La fiche detail film s'ouvre uniquement via le bouton `Details` du panneau preview. Apres changement de profil ou premier chargement sans historique, l'ecran ouvre `ALL` ou une categorie avec contenu plutot que `Historique` vide.
- Series: categories conservees, liste centrale paginee et preview a droite sur l'episode repris ou le premier episode. Le bouton `Play` lance l'episode; un deuxieme OK sur la meme ligne ouvre la fiche detail. Les lignes n'affichent plus le dossier categorie, placent la note en premier et limitent le genre a une seule valeur.
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
- La synchronisation Series tente d'abord `get_series` global avec timeout dur; en cas de blocage, elle recharge les series par categorie, assigne le dossier aux reponses sans `category_id`, deduplique par `series_id`, puis importe Room.
- Depuis le 2026-07-03, les URL d'images catalogue sont normalisees a l'entree et a la sortie Room: `stream_icon`, `cover`, posters et backdrops Xtream acceptent URL absolue, URL `//`, chemin absolu `/...`, chemin relatif, espaces et slashes echappes. Les valeurs vides comme `null`, `n/a`, `none` ou `0` sont ignorees. Cette logique restaure les logos Live TV et posters Films/Series quand un fournisseur ne renvoie pas directement une URL HTTP complete.
- `SynchronizeCatalogUseCase` declenche la synchro.
- `XtreamUrlFactory.kt` construit les URL de lecture.
- `data/xtream/XtreamConnectionManager.kt` centralise l'etat connecte / erreur reseau / identifiants invalides / reponse invalide.

M3U / EPG:
- `data/playlist/M3uPlaylistClient.kt` telecharge et parse les playlists M3U.
- Les logos `tvg-logo` M3U relatifs sont resolus contre l'URL du fichier M3U avant stockage Room.
- `data/playlist/EpgRepository.kt` telecharge, parse et cache les programmes XMLTV.
- `data/playlist/EpgSyncWorker.kt` planifie un refresh EPG horaire stale-aware avec contrainte reseau, separe de la synchronisation catalogue.
- `DefaultCatalogRepository.kt` choisit la branche de synchronisation selon `PlaylistSource`.

Room:
- `SVDatabase.kt` version 16.
- Entites catalogue: profiles, categories, live streams, movies, series, episodes, favorites, playback progress, sync state, historique YouTube et caches de decisions Kids categorie/item.
- Les tables IPTV multi-profils utilisent `profileId` dans leurs cles primaires pour permettre deux profils avec les memes ids Xtream/M3U sans collision. Les historiques/reprises/favoris IPTV suivent donc le profil actif. YouTube et Media local restent globaux.
- `home_trending_preview_cache` stocke par profil poster, backdrop, duree et sample Xtream film/episode sans URL brute; la lecture Home reconstruit l'URL en memoire.
- Tables TMDB separees du catalogue Xtream: `tmdb_movie_metadata` et `tmdb_series_metadata` restent globales par `tmdbId/language`; `tmdb_content_mapping` est profile par `profileId`. Ne pas y stocker d'URL de lecture ni de secrets.
- Tables Media Center separees du catalogue: `media_folders`, `media_files`, `recording_jobs` via `MediaCenterDao.kt` et `MediaCenterEntities.kt`. Ne pas les melanger avec `MediaDao.kt`.

Playback:
- `FullScreenPlayerScreen.kt` gere Live, Movie, Episode et la lecture locale video/audio via `UserContentType.LocalMedia`.
- Live peut utiliser `.ts` et fallback `.m3u8`.
- Films/series sauvegardent la progression.
- Depuis le 2026-07-03, la sauvegarde d'un episode conserve les metadonnees d'historique existantes et enrichit depuis Room quand possible: titre de serie, label saison/episode, poster de serie et `parentContentId`. Le lecteur utilise aussi le cache de details Xtream deja charge pour eviter un titre generique `Series` quand l'episode vient d'un detail serie. Si une ancienne ligne historique possede deja `parentContentId`, `UserContentRepository.enrichProgress()` peut reparer titre/image depuis la serie Room meme si l'entite episode locale n'existe pas encore. La categorie speciale Series > Historique ne doit plus afficher des episodes sous forme `Episode <id>` ou `Series` sans image quand les metadonnees serie sont disponibles.
- Depuis le 2026-07-03, Live TV > Historique rehydrate ses lignes depuis Room via les ids regardes quand possible. Les lignes historiques recuperent ainsi `epgChannelId`, programmes EPG, badge `E` et details sous mini-player comme les dossiers Live TV normaux.
- Overlay plein ecran Live TV: la video reste plein ecran; numero de chaine reel en haut droite seulement quand il est connu, bandeau bas full-width sombre/transparent coherent avec les overlays Films/Series, logo chaine sans cadre, separateur, nom chaine blanc lisible, ligne EPG locale si disponible et actions icon-only. Les commandes -10/+10 restent disponibles quand la fenetre Live Media3 est seekable. Le bouton EPG n'est plus rendu dans le player fullscreen Live, sans supprimer la logique EPG ailleurs. `Record` et `Info` ne sont pas rendus dans ce lot UI; la fonctionnalite Recorder reste en attente hors overlay cible. Le logo SmartVision n'est pas affiche dans cet overlay. Depuis le 2026-07-07, la luminosite Live remplace temporairement les icones par le meme slider que Films/Series et applique seulement un voile video. Depuis le 2026-07-08, les composants visuels Live de cet overlay sont separes dans `LivePlayerOverlay.kt`; `FullScreenPlayerScreen.kt` garde le cycle de vie Media3, les routes player et les etats.
- Les lecteurs plein ecran appliquent le format video global `Fit`/`Fill`/`Zoom`; le panneau Live peut le remplacer pour la session courante. Le `DefaultLoadControl` utilise les profils Low latency (5-20 s), Standard (15-50 s) ou Stable (30-90 s). La reconnexion autorise au maximum deux relances du meme flux quand elle est active; le fallback Live TS vers HLS reste independant.
- Recorder Live MVP: `RecordingService` en foreground service `dataSync` telecharge le flux Live vers `SmartVisionMedia/Recordings` avec fichier temporaire `.part`, finalise le fichier puis l'indexe dans Media Center. Un seul enregistrement actif est autorise. Le Recorder doit continuer apres sortie de la chaine/player; l'arret normal passe par la notification Stop, la duree choisie ou une erreur. Les flux progressifs reconnectent si le serveur ferme la socket avant la duree demandee, avec grace de recuperation apres premiere donnees recues; HLS simple non chiffre est supporte; les HLS chiffres echouent proprement.
- Overlay plein ecran Films/Series: depuis le 2026-07-07, le player VOD/episode utilise un overlay fullscreen bas distinct du Live TV. Films: titre/categorie, favoris, luminosite, parametres, plein ecran, controles -15s/play/+15s. Series: titre serie + episode, bouton `Autres episodes` en premiere icone utilitaire, favoris, luminosite, parametres, plein ecran, controles episode precedent/-15s/play/+15s/episode suivant. Le panneau `Autres episodes` est cache-only depuis les episodes Xtream deja charges, s'ouvre a droite, masque l'overlay principal et rend le focus au bouton d'ouverture a la fermeture. La card `Episode suivant` apparait uniquement pour les series quand il reste 10 secondes ou moins et peut etre annulee pour la lecture courante. Le slider luminosite remplace temporairement les icones utilitaires et applique seulement un voile noir sur la video, sans modifier la luminosite systeme.
- Lecture locale Media: `media_player/{mediaFileId}` charge `MediaCenterPlayback` depuis `MediaRepository`; video/audio reutilisent le player fullscreen sans preroll pub ni verification Xtream, photo ouvre un viewer plein ecran local. Les actions Media Center `Renommer`, `Deplacer` et `Supprimer` ciblent le fichier par id explicite; `Supprimer` est accessible dans la premiere rangee d'actions avec `Lire` et `Renommer`, puis confirme via un dialog focusable.
- Transfert telephone/TV Media MVP: `MediaTransferServer` demarre un serveur HTTP local temporaire depuis `MediaScreen`. Import: QR `/u/{token}`, upload mobile par `PUT`, ecriture dans `SmartVisionMedia/Transfers` puis indexation. Export: QR `/d/{token}`, download du seul fichier selectionne via `/download/{token}`. La feature est gatee par `media_phone_transfer`; l'import est expose dans la bibliotheque par un bloc `Telephone -> TV` / `Hub transfert` qui explique soit `Recevoir fichier`, soit le verrou Premium/essai. Depuis le Lot 16, l'ecran Media affiche un agencement premium avec hero studio, stats bibliotheque, lignes enrichies et preview hero sans changer les endpoints ni la logique stockage.
- Media prives: `private_media_detail/{id}` ouvre une fiche detail alimentee par le proxy SmartVision. Les feature flags `private_media`, `private_media_eporner` et `private_media_native_playback` controlent respectivement l'acces, le provider et la lecture native future.

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
- `data/playlist/EpgSyncWorker.kt`
- `data/tmdb/TmdbApiService.kt`
- `data/tmdb/TmdbRepository.kt`
- `data/tmdb/TmdbMatcher.kt`
- `data/tmdb/TmdbImageResolver.kt`
- `data/repository/UserContentRepository.kt`
- `data/local/SVDatabase.kt`
- `data/local/dao/*`
- `data/local/entity/*`
- `data/local/dao/MediaCenterDao.kt`
- `data/local/entity/MediaCenterEntities.kt`
- `media/MediaRepository.kt`
- `media/MediaStorageManager.kt`
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
- Ne pas afficher, logguer ou versionner les secrets TMDB; le token reste dans `local.properties`.
- Ne pas bloquer la lecture, Home, Movies, Series ou les fiches detail si TMDB echoue ou n'est pas configure.
- Ne pas remplacer les ids/URLs Xtream par TMDB pour la lecture; TMDB enrichit seulement les metadonnees.
- La lecture doit rester native Media3 ExoPlayer.
- Pour Live TV, ne pas sauvegarder la progression comme VOD.
- Garder les categories speciales Favoris/Historiques coherentes.
- Ne pas bloquer l'affichage si du contenu partiel est deja disponible.
- Ne pas publier un catalogue partiel apres echec de synchronisation. La synchro Xtream actuelle ecrit Live, Movies, Series et `SyncState` dans une transaction Room unique; les sections sont traitees en serie et en batchs pour reduire les listes simultanees en RAM.
- Une seule source catalogue peut etre active: Xtream ou M3U. Activer une source desactive l'autre cote preference locale.
- En multi-profils local, une seule source/profil est active a la fois. Les catalogues Room sont conserves par `profileId`; ne pas revenir a un nettoyage global au changement de profil.
- M3U alimente Live TV uniquement; ne pas fabriquer des films/series sans structure fiable.
- Quand M3U est actif, ne pas afficher de messages d'erreur Xtream sur Movies/Series; afficher un etat vide source-aware.
- Le badge EPG des lignes Live TV doit se baser sur les programmes locaux disponibles, pas seulement sur la presence d'une URL EPG.
- Ne pas remettre de filtre admin/API pour masquer les chaines par nom `###`: cette demande est explicitement exclue de la mission Live TV du 2026-07-05.
- Ne pas lancer de synchronisation globale pendant la navigation Home / Live TV / Movies / Series / categories / listes.
- Ne pas charger un snapshot complet Live TV / Movies / Series pour ouvrir un ecran catalogue; utiliser categories/counts puis pages Room locales.
- Le premier chargement local ne doit plus etre dans `MainActivity`: les categories initiales sont limitees a `20` par section dans les ViewModels Live TV / Movies / Series, puis le reste charge discretement apres premier affichage. Ne pas remettre les details tendances Xtream, EPG reseau ni snapshots complets Movies/Series dans `MainActivity` pour les tres gros catalogues.
- Ne pas confondre prechargement local et synchro reseau: reconstruire le cache memoire depuis Room au demarrage est normal; retelecharger le catalogue complet ne doit arriver que si la politique de frequence le demande ou si l'utilisateur lance Synchroniser.
- Ne pas bloquer la telecommande pendant un chargement local Room; le blocage Home est reserve aux synchronisations reseau catalogue.
- Ne pas stocker d'URL de lecture brute en base pour les previews HOME; garder uniquement les ids/extensions et reconstruire via `XtreamUrlFactory`.
- Ne pas stocker d'URL de lecture brute en base pour les previews Movies/Series; les URLs du mini-preview et du fullscreen doivent etre reconstruites en memoire depuis les ids/extensions Xtream.
- Pour les pages Room Movies/Series, ne pas reconstruire les URLs avec l'id seul si `containerExtension` est disponible: utiliser l'extension locale pour eviter les previews `ALL` indisponibles chez les fournisseurs non-`mp4`.
- Ne pas promettre zero chargement local apres fermeture complete: si le process Android a ete tue, le cache RAM n'existe plus et doit etre reconstruit depuis Room.
- AutoSync et sync manuelle doivent verifier Xtream avant de synchroniser; seules les erreurs reseau confirmees sont retentees automatiquement.
- La verification de connexion Xtream est obligatoire au premier affichage actif, mais elle reste non bloquante pendant ses essais silencieux et ne doit pas forcer une resynchronisation globale si la politique de frequence ne la demande pas.
- Les routes player/detail doivent aussi respecter le blocage Xtream; ne pas compter uniquement sur Home/Header pour bloquer l'acces.
- Les diagnostics de synchro doivent rester non intrusifs: pas de credentials dans `logcat`, seulement les compteurs catalogue et les valeurs memoire Runtime.
- Media prives: la lecture native n'est autorisee que si le backend SmartVision renvoie explicitement un flux direct HLS/MP4. Si le provider officiel expose seulement un embed/page, Android utilise `private_media_player/{id}` avec WebView embed; aucun scraping/extraction HTML ni URL provider construite dans l'APK.

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
- 2026-07-07: ajout lots TMDB 1 a 6: plan A-Z, tables Room `tmdb_*`, API Retrofit TMDB optionnelle, matching automatique films/series, cache local, enrichissement visuel fiches film/serie, Home preview enrichie et grilles catalogue cache-only.
- 2026-07-07: Media prives ajoute preview video et fullscreen: ExoPlayer pour HLS/MP4 SmartVision, WebView embed officiel sinon, avec premier OK mini-player et second OK fullscreen.
- 2026-07-07: Media prives ajoute sous-dossiers admin gerables et detection backend de streams directs HLS/MP4 sans scraping.
- 2026-07-07: Media prives ajoute le mode admin de test `Forcer lecture native HLS/MP4`, corrige le WebView prive Android TV et compacte le mini-player preview.
- 2026-07-07: Media prives rapproche son layout de YouTube: section principale player plus large, controls Media3 visibles/focusables en natif, WebView embed focusable et overlay plein ecran bas.
- 2026-07-09: Movies et Series passent au layout 3 colonnes type Live TV, recherche centrale Room paginee, premier OK preview + focus Play, deuxieme OK fullscreen, et fiches detail accessibles uniquement via le bouton `Details`.
- 2026-07-10: Movies/Series reconstruisent les URLs preview/player depuis Room avec l'extension locale, corrigent le cas `ALL` non-`mp4`, et le mini-player Preview devient une cible D-pad entre header et details.
- 2026-07-10: apres OK sur une categorie Live/Movies/Series, la premiere ligne recoit le focus sans selection ni preview; le retour fullscreen Film attend la recomposition de la ligne source au lieu d'abandonner vers Categories.
- 2026-07-10: l'apercu Series remplace les infos generiques par un navigateur saisons/episodes D-pad, choisit l'episode historique sinon S01E01, affiche la progression et conserve l'episode manuel pour Play et le deuxieme OK.
- 2026-07-10: les overlays Live/Film/Serie partagent `PremiumPlayerOverlayFrame`, la surface glass et le contexte contenu, sans changement Media3, publicite ou ordre Back.
- 2026-07-10: Films et Series utilisent maintenant `VodFullscreenControlsOverlay`, calibre par proportions 1680x945: gradient noir sans panneau, titre et saison/episode, progression reelle, exactement sept commandes, sauts bornes de 10 secondes et sortie fullscreen fonctionnelle. Live TV et Media local conservent leurs overlays dedies/existants.
- 2026-07-11: stabilisation fullscreen Films/Series (DPAD Bas, progressbar focusable/seekable, luminosite inline, voisins Room reels) puis remplacement de l'overlay Live par la meme reference visuelle, adaptee aux capacites seekables Media3 et a la progression EPG non seekable.
- 2026-07-12: Live TV lie le catalogue charge a son identifiant de categorie; un filtre vide l'ancien resultat avant la premiere categorie valide. Les invocations concurrentes de synchronisation sont dedupliquees. Le filtre Kids central exclut aussi crime, horreur, adulte/sexuel, pornographie, guerre, violence et gore. Les images hero valides sont conservees 24 h dans un cache disque atomique avec repli local.
- 2026-07-13: le filtrage Kids parcourt Live TV, Films et Series par lots de 128 elements, publie les compteurs reels traites/conserves/exclus dans le `SyncStatus` partage et cede regulierement le thread. Home et Info compte affichent le meme avancement. Les listes centrales proposent aussi un tri local stable applique apres recherche/categorie, sans modifier Room.
- 2026-07-17: Movies enrichit toutes les lignes de chaque page/dossier avec les metadonnees TMDB, aligne l'ordre des details de ligne sur Series, privilegie le backdrop paysage et ignore les annulations normales de chargement au lieu d'afficher une erreur fugace. Series conserve le nombre de saisons TMDB en Room v19 et l'affiche a droite du titre.
