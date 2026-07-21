# Ecrans Home, Catalogues, Profile, Settings et YouTube

Derniere mise a jour: 2026-07-21.

## Correctifs audit ecrans - 2026-07-21

- YouTube conserve la liste, le scroll et la video source apres fermeture du player, sans recharger automatiquement Tendances.
- Le detail Series memorise l'identifiant de l'episode lance et restaure ce focus exact apres Back.
- Gerer les profils dissocie les actions d'ajout de la derniere carte profil selectionnee; aucune action obsolete n'est rendue ou joignable depuis ces cartes.
- Les libelles visibles speciaux Catalogue, Preview, Film et usage de licence passent tous par la langue active. Les cartes de choix de fond Settings sont elargies par une grille a deux colonnes.

## Cartes Home et preview partagee - 2026-07-17

- Un chargement a froid de Continue/Tendances conserve son skeleton au moins `650 ms`; un snapshot memoire s'affiche directement.
- Continue repare les anciens titres generiques `Serie/Series <id>` depuis l'episode, `parentContentId` puis l'identifiant extrait. Films, directs et series portent un badge noir `FILM`/`LIVE`/`SERIE`; `LIVE` ajoute un point rouge a droite du libelle.
- Les noms des chaines Continue Watching restent strictement sur une ligne. La taille passe de `12 sp` a `10 sp` uniquement si le premier rendu deborde, puis conserve une ellipsis de securite.
- La ligne basse Continue est remontee a `8 dp`: Film affiche barre puis temps restant, Serie affiche `Sx Ex`, barre puis temps restant, Live n'affiche ni barre ni compteur. Les libelles restants sont localises en anglais/francais.
- Tendances garde le titre sur une ligne (`15 sp`, repli `12 sp`, ellipsis), affiche la note numerique avec une etoile jaune separee et place duree Film ou `xS xxE` dans un badge noir en haut a gauche. Une duree nulle/invalide reste masquee.
- Les gradients statiques inferieurs de Continue et Tendances terminent a `0.96` d'opacite; l'overlay allege pendant la video reste inchange.
- Quand une preview Continue/Tendances affiche sa premiere image video, un aplat noir couvre le poster derriere la surface Media3. Le poster reste conserve jusqu'a cette premiere image et le cycle de vie du mini-player ne change pas.
- Les trois cards categories ne rendent plus les badges `LIVE`/`VOD`/`SERIES`. Leur titre reste a `42 sp`; le compteur Room Live/Films/Series est place juste au-dessus a `27 sp` (environ 65 % du titre), tandis que l'action `Watch now`/`Explore` reste seule sur la ligne basse. Pendant la synchronisation, l'arc cyan du loader occupe `46,8 %` du perimetre de la card. Les valeurs de taille et position sont centralisees dans `HomeCategoryCardLayout`.
- Les cards categories font de nouveau `170 dp` de haut. Pendant une synchronisation/erreur, leur typographie et leur espacement passent uniquement en variante compacte afin que le detail reste visible sous la barre sans changer la rangee de trois cards.
- Un clic sur une card Live TV/Movies/Series utilise de nouveau la navigation NavHost standard, sans deplacement, zoom, container transform ni verrouillage temporaire des touches. Le header et le focus suivent le comportement catalogue normal.
- Le poster reste visible jusqu'a la premiere image decodee. La carte passe alors a `1.2x` et ne garde qu'un titre sur une ligne et un gradient bas court.
- Le controleur conserve un seul player partage. Sa vue Android utilise `surface_view` pour eviter le chemin `TextureView` acquire-fence/pertes d'images; verifier le clipping et l'ordre des overlays sur chaque cible.

## Profils ADMIN, NORMAL et KIDS

- `PlaylistProfile` porte `ProfileType`, `CredentialsMode` et `isLocked`; l'ID historique est conserve lors de la migration du premier profil vers `ADMIN`.
- `ProfilePickerScreen` utilise une `LazyRow`: profils configures reels ordonnes ADMIN puis date de creation, suivis des actions i18n enfant et profil normal. Les cartes sont cleees par `profile.id` et leur seul libelle est `PlaylistProfile.name`; aucun nom `Admin` ou `PlaylistWeb` n'est fabrique par l'UI.
- Au demarrage, `AppNavigation` et le picker sont precomposes sous le splash. Le picker signale sa disponibilite uniquement apres un layout de taille non nulle; ses cartes restent a l'etat initial masque puis declenchent leur apparition courte et echelonnee au handoff du splash. Le splash conserve sa duree et sa progression locale: l'optimisation supprime l'ecran vide apres le splash sans accelerer artificiellement celui-ci.
- Toutes les cards du picker, profils et actions d'ajout, utilisent le meme gradient bleu opaque `#123764 -> #071A33`. Leur hauteur commune est calculee depuis l'avatar et le plus haut libelle reellement mesure; les noms courts sont ancres en bas de leur slot afin de garder `14 dp` visibles au-dessus et sous le contenu sans reserve fixe de `54 dp`.
- Si l'administrateur est verrouille, les deux cartes d'ajout utilisent le PIN parental avant d'ouvrir le formulaire. Les profils verrouilles utilisent le meme dialogue.
- Le formulaire partage les identifiants ADMIN par defaut et conserve la validation existante. Il est commun au picker et a Info compte, adapte son titre/type, place nom et avatars sur la meme rangee, garde un footer fixe et ouvre Xtream/M3U dans un accordeon exclusif ferme par defaut.
- Le filtre catalogue du formulaire affiche uniquement les codes (`AE`, `FR`, `X`), regroupe les codes actifs en debut de rangee et accepte l'ajout manuel d'un code de 1-3 lettres. `[X]` et `[x]` sont detectes comme le meme code `X`.
- Le header affiche l'avatar actif. En mode Kids, il garde uniquement Home, Live TV, Movies, Series, YouTube et l'avatar a droite.
- Les photos de profil utilisent des emplacements carres arrondis avec un rayon proportionnel de `16 %`, pilotes uniquement par `avatarId` et `ProfileType`. Le masque partage s'applique au picker, a sa transition vers Home, au header, aux panneaux profil/parental et aux presets; les PNG utilisent `Crop` pour remplir le cadre sans deformation. ADMIN conserve `admin` par defaut mais peut choisir tous les avatars `classic_*`; KIDS choisit un avatar `kid_*` aleatoire a la creation; NORMAL utilise le set `classic_*`. Les anciens IDs couleur restent compatibles via un mapping vers les nouveaux PNG, sans dependance au nom affiche.
- Le Home Kids conserve hero, cards et carrousels; seuls les assets `kids_home_hero`, `kids_live_tv_bg`, `kids_movies_bg`, `kids_series_bg` et les donnees filtrees changent.

## 1. Objectif

Cartographier les principaux ecrans utilisateur et les chemins de code pour orienter rapidement les interventions UI.

## 2. Fonctionnement actuel

Les ecrans actifs sont routes depuis `ui/navigation/AppNavigation.kt`. Le header principal expose Home, Live TV, Movies, Series, Media et YouTube en onglets centraux icon-first. Ils utilisent six PNG generes, transparents et monochromes blancs de `34 dp`. Chaque onglet utilise le focus TV partage; la route active garde en plus un etat visuel `active` persistant et distinct du focus. Les actions globales restent dans le bloc droite, puis la date/heure non focusable sans separateur vertical dedie. L'horloge et ses secondes sont configurables globalement dans Personnalisation et reagissent immediatement. Le bouton licence/Premium n'est plus rendu dans le header depuis le 2026-07-20; le dialogue Premium reste appele par les features verrouillees. Les ecrans de contenu affichent un QR Xtream si aucun compte n'est configure. Si un compte Xtream existe mais que la verification rapide echoue, Home reste accessible et les entrees Live TV / Movies / Series sont bloquees par popup et overlay. Pendant une verification Xtream en cours, la navigation catalogue peut rester temporairement protegee si aucun etat connecte valide n'est deja connu, mais Home et le header ne doivent afficher les warnings/overlays "Connexion indisponible" qu'apres un echec Xtream confirme.

Tous les ecrans principaux reutilisent le meme `ui/home/TvHeader.kt` et le meme shell exterieur: `25 dp` de marge horizontale et `14 dp` de marge verticale. La hauteur mesuree du header est `52 dp` pour aligner les glyphes agrandis et leur label sans recouvrement; le degagement Home sous header reste `8 dp`. Home garde son hero dans le flux sous ce shell et remet son scroll vertical a zero lors d'un retour cible vers le header.
Un spacer structurel non scrollable de `14 dp`, identique au padding haut du shell, separe le header du viewport Home afin d'equilibrer visuellement ses espaces haut/bas. Le `verticalScroll` utilise une politique `BringIntoView` neutre: les transitions D-pad verticales restent l'unique proprietaire du `ScrollState`, tandis que les `LazyRow` internes recuperent la politique Compose standard pour leur revelation horizontale. Le clipping tous axes explicite est retire afin de laisser les halos depasser lateralement; le conteneur scrollable conserve son clipping directionnel vertical.

## 3. Workflow utilisateur

- Home: hero, cartes Live/Movies/Series, continue watching si disponible, `Trending movies`, `Trending series`, notifications/profil/settings.
- Quand l'utilisateur est deja sur Home, le bouton HOME du header ne doit pas relancer la navigation vers la meme route: il rafraichit localement slides/tendances, remet les listes horizontales a l'index `0`, remonte l'ecran et redonne le focus aux categories.
- Home affiche un overlay "Connexion indisponible" sur les cartes et reprises de lecture quand Xtream est indisponible avec echec confirme, pas pendant le simple etat `checking`.
- Home ne doit pas afficher cet overlay pendant un simple refresh rapide si la derniere verification Xtream est encore `CONNECTED`.
- Home affiche slides caches, historique recent limite a `10` et tendances persistantes `10 + 10` du profil actif. Historique, tendances et compteurs sont scopes par `profileId + catalogRevision`; un changement vide immediatement les anciennes lignes/previews, annule les calculs precedents et refuse toute reponse tardive avant de relire Room.
- Depuis le 2026-07-03, Home decide la synchronisation automatique apres son premier rendu. `Synchronize` lance une synchro catalogue complete sur Home; `LoadLocal` n'est plus utilise pour bloquer ou charger l'interface depuis le splash. Pendant une vraie synchro, Home intercepte Back/D-pad/OK, empeche la navigation/clics et affiche un overlay sombre progressif sur les cards Live TV / Films / Series.
- Les overlays Home agissent comme une progression inversee pendant `Synchronize`: la card commence sombre, puis la partie gauche redevient normale selon le pourcentage de section. Les cards terminees perdent leur overlay; les cards en attente restent sombres. Les phases visibles viennent de `SyncStatus.SyncSectionPhase`: `WAITING`, `RUNNING`, `IMPORTING`, `COMPLETED`, `ERROR`; `LOADING_TRENDS` n'est plus declenche par la synchro allegee.
- `HomeViewModel` observe atomiquement `activeProfileId + catalogRevision` et `SyncStatus`. Le token vu a l'enregistrement est capture avant de lancer le collector, afin qu'une bascule precoce ne soit jamais perdue. Apres synchronisation, compteurs et tendances sont relus avant de publier Home prete; une erreur de synchronisation est un etat terminal et ne laisse jamais les skeletons actifs. Films reste lu depuis Room; Series revalide la selection Room bornee avec notes normalisees sur `5`, minimum `3/5`, recent puis meilleur fallback.
- Sur Home, Continue watching et les deux lignes Tendances utilisent directement des cards paysage 16:9 de taille stable. Le focus agrandit legerement le conteneur sans changer son ratio. Apres `550 ms`, un controleur Media3 partage et muet lance uniquement l'URL Xtream; il libere le player a la perte de focus, sortie ecran, changement de profil ou arriere-plan.
- Les cards categories, Continue Watching et Tendances partagent le halo bleu des cards Who's Watching. Le viewport Home ne clippe plus les depassements horizontaux des cards categories de bord; les `LazyRow` Historique/Tendances gardent `20 dp` de padding horizontal interne pour conserver leurs halos et cinq cards paysage completes.
- Sur Home, Continue watching utilise aussi le mini-player Media3 au focus: le mini-player est cree seulement apres un focus stable court, le poster/thumbnail reste visible jusqu'a `onRenderedFirstFrame`, puis Films et Episodes lisent depuis la position de reprise; la boucle reste de 20 secondes avec overlay i18n `Resume playback` / `Reprendre la lecture`. Le son suit le meme fade-in local: 1 seconde muet, puis montee de 1 seconde vers le volume player complet. Pour les apercus LiveImmediate, l'effet audio ne doit pas etre annule juste apres `play()`. Les URLs preview qui echouent avec un conteneur Media3 non supporte sont ignorees en poster-only pour le reste de la session.
- Une reprise episode affiche `SERIE` en haut et `Sx Ex` en bas. A l'ouverture d'un profil, Continue Watching reste strictement Room-only et best effort: aucune recuperation `get_series_info` ne peut retenir Who's Watching; les metadonnees distantes manquantes sont resolues plus tard par les parcours detail/lecture.
- La progression d'une lecture plein ecran reste rattachee au profil qui a ouvert le player, y compris si sa sauvegarde asynchrone se termine apres l'activation d'un autre profil.
- Sur Home, les tendances Films/Series conservent strictement `item.imageUrl` pendant le focus et la preparation preview. Les cles Lazy utilisent `item.id`; les backdrops caches sont hydrates avant emission et les cinq premieres Series sont preparees avec concurrence `2` avant retrait du skeleton. Le prechargement suivant ne remplace jamais l'image de la card uniquement parce que le focus entre dans la section.
- Une distance unique de `12 dp` separe le bas des cards categories du titre Continue Watching, le bas des cards Continue Watching du titre Trending Movies et le bas des cards Trending Movies du titre Trending Series. `HomeContentRowHeight` correspond maintenant exactement a `24 dp` de titre + `6 dp` de gap + `94,5 dp` de card, sans reserve vide basse.
- Sur Home, `Continue watching`, `Trending movies` et `Trending series` affichent des skeleton cards non focusables pendant les chargements initiaux de leurs donnees. Les cibles D-pad haut/bas continuent de viser uniquement les vraies lignes avec contenu pour eviter les focus morts.
- Sur Home, les lignes Continue watching / Trending movies / Trending series ont un padding interne horizontal pour que la premiere card, sa bordure focus et le mini-player ne soient pas tronques a gauche. Les slots `LazyRow` gardent une largeur stable quand les previews sont activees; le focus ne doit plus animer la largeur d'item lazy. Le scroll automatique horizontal est limite aux cas ou la card focussee n'est pas deja entierement visible.
- Les `LazyListState` des lignes Home ne sont pas sauvegardes/restaures entre jeux de donnees; ils sont recrees par signature de liste et remis a l'index `0`, afin que les tendances ne s'ouvrent pas sur un ancien item comme le 14e.
- Le D-pad bas/haut entre Continue watching et Tendances conserve le scroll horizontal de chaque ligne et cible la card visible dont le centre horizontal est le plus proche de la card source. Le retour force a l'index `0` reste reserve a l'entree depuis les categories et au rafraichissement explicite Home. Le parent vertical ignore la relocation automatique post-focus; les lignes gardent une hauteur fixe egale a leur contenu visible.
- Les sections Continue watching / Trending movies / Trending series ne sont rendues que si elles contiennent des items; les cibles D-pad haut/bas tiennent compte de ces lignes absentes pour eviter un focus mort.
- Les boutons `View all` et les routes secondaires `continue_watching` / `trending` ont ete supprimes; Home reste un apercu limite.
- Live TV: categories, chaines, apercu puis plein ecran. Le mini-player d'apercu Live TV demarre muet, attend 1 seconde apres lancement, puis augmente progressivement le volume player a `1f` sur 1 seconde; le fallback `.m3u8`, le buffering et les erreurs restent inchanges.
- Live TV ouvre directement son lecteur plein ecran, sans expansion/repli depuis le mini-player. Movies et Series conservent la transition fondee sur les bounds reels de leur mini-player, avec verrouillage temporaire des touches pendant leur morphing.
- Movies: trois colonnes Categories / `Movies` / Preview, lignes partagees compactes 16:9, detail et lecture conserves.
- Series: trois colonnes Categories / `Series` / Preview, memes lignes partagees 16:9; saisons, episodes, detail et lecture restent inchanges.
- Live TV / Movies / Series affichent les categories puis chargent les contenus par pages Room locales pendant le scroll, sans reconstruire toute la playlist en RAM.
- Live TV / Movies / Series chargent d'abord `20` categories Room maximum, puis completent discretement la liste complete dans leur ViewModel. Aucune categorie ni premiere page catalogue n'est prechauffee dans le splash.
- Media affiche uniquement le Media Center local: fichiers, dossiers, enregistrements, imports et transferts telephone/TV. Depuis le 2026-07-20, il ne contient plus de source privee, route privee, player prive, flag prive ou entree admin associee.
- Les logos Live TV et images Films/Series affiches dans les listes, details, Home et historiques viennent des champs normalises en repository. Ne pas corriger les images directement dans chaque composable sauf besoin UI specifique; les ecrans doivent consommer les URL catalogue deja nettoyees.
- La route compatible `profile` rend `Info profil`; `profile/manage` rend `Gerer les profils`. Ces deux routes restent reservees a ADMIN. Le menu commun suit l'ordre Info profil, Gerer les profils, Controle parental, Synchronisation, Aide; Historique et le raccourci Parametres ont ete retires de ce menu sans supprimer Settings du header global.
- `Info profil` ne contient aucun CRUD ni identifiant: profil actif, compteurs Live/Films/Series/elements masques, derniere synchronisation et action de synchronisation. `Changer de profil` n'active rien localement: l'action revient d'abord sur Home, attend que cette route soit composee et l'application au premier plan, puis ouvre l'unique `ProfilePickerScreen` global. L'activation ne commence qu'apres OK sur une carte profil.
- Le menu Profile met a jour sa destination locale avant les callbacks de route. Ainsi, `Info profil` reste actionnable depuis Synchronisation sur la route `profile`; `profile/manage` demeure la seule destination utilisant une route NavHost distincte.
- `Gerer les profils` reutilise `PlaylistProfileEditorDialog` pour creer NORMAL/KIDS et modifier. La `LazyRow` utilise les IDs stables, puis les PNG `avatar_action_add_kids` et `avatar_action_add_profile`; Droite depuis le menu cible le premier profil ou Add kids si la liste est vide. Le detail compact place avatar et nom sur une ligne puis les metadonnees dessous sans augmenter le panneau.
- `Synchronisation` reste l'unique ecran de configuration du demarrage, de la synchro en arriere-plan et de la frequence. Le panneau parental garde le header et le menu gauche de Profile: `Change PIN` est dans le header droit, Activation occupe toute la largeur avec toggle global et cards avatar/nom/toggle pour tous les profils, puis viennent Mots-cles et Resultats.
- Les playlists envoyees depuis `/playlist/` arrivent dans un profil dedie `PlaylistWeb`. Si ce profil garde son nom, les envois suivants mettent a jour ses champs; si l'utilisateur le renomme, le prochain envoi web cree un nouveau `PlaylistWeb`.
- Apres le splash et avant Home, `ProfilePickerScreen` affiche tous les profils configures dans une `LazyRow` sans limite artificielle et couvre le premier rendu avant toute frame Home visible. Si Home ou le `NavHost` est encore en stabilisation, le picker reste visible au-dessus du masque opaque; ce masque ne doit jamais apparaitre seul. `Info profil`, le dialogue de sortie et l'action profil Kids reutilisent ce meme Who's Watching global, precompose au-dessus de Home. Pendant son ouverture, Home reste composee uniquement comme support cache, non interactif, sans focus, preview, son ni synchronisation post-rendu. OK active ensuite le profil et invalide les caches Home/Xtream. Le reveal final attend le profil/revision cible, la synchronisation eventuellement due, les compteurs et les tendances; Continue Watching peut terminer ensuite sans bloquer. Un `HomeViewModel` unique, possede et collecte par la navigation, fournit la disponibilite. Home remplace tout `HomeUiState.profileId` obsolete par un etat cible vide/loading avant rendu. La synchronisation ne se lance que si le catalogue manque ou si 24h/48h est echu; `A chaque demarrage` n'est jamais rejoue par un retour Home.
- Le total `Elements masques` additionne les exclusions Kids de la derniere synchronisation reussie et le snapshot parental courant uniquement lorsqu'il est actif pour le profil. Room v18 ajoute trois compteurs `sync_state.kidsExcluded*` avec migration additive 17 -> 18; une erreur conserve les valeurs de la derniere reussite.
- Quand M3U est actif, Movies et Series affichent un etat vide source-aware au lieu d'une erreur Xtream.
- Live TV affiche un badge bleu `E` a droite des lignes de chaines qui ont des programmes EPG locaux, y compris dans le dossier Historique quand la chaine existe encore en Room. Les dossiers Live TV avec EPG affichent leur compteur dans un cadre bleu au lieu d'ajouter un badge EPG separe. Le panneau details EPG sous le mini-player est focusable et scrollable au D-pad.
- Settings commence par `Licence SmartVision`, puis Preferences generales, attribution TMDB, personnalisation, mises a jour et donnees locales. `Network Activity`, `Controle parental` et `Synchronisation` ne sont plus visibles. Le menu gauche fait `230 dp`, ses boutons `44 dp`; Preferences et Personnalisation utilisent deux cartes par ligne, padding compact `12 x 10 dp` et boutons d'options `36 dp`.
- Preferences applique le format video plein ecran (`Fit`, `Fill`, `Zoom`), les animations de focus reduites, trois profils de buffer Media3 et la reconnexion automatique. Personnalisation conserve les roles de focus et ajoute l'affichage de l'horloge et des secondes.
- Le titre principal et son icone restent places au-dessus du contenu dans toutes les destinations Profile et Settings. Les colonnes droites n'ont plus de cadre global: seuls les cadres des sous-sections restent visibles. `TvSectionCard` peut omettre entierement son en-tete; Manage Profiles l'utilise pour ses deux zones et Licence SmartVision conserve uniquement le titre principal Settings, sans shell imbrique de `LicensePanel`.
- Le clic `Controle parental` dans le shell profil ouvre immediatement `NumericPinDialog`: verification du PIN existant ou creation avec confirmation s'il n'est pas configure. Toutes les saisies PIN utilisent ce composant.
- Le panneau parental actif est pilote par `ParentalControlViewModel`. L'activation globale et une liste DataStore des profils explicitement desactives produisent l'etat parental effectif du profil actif; Admin est ciblable, les installations existantes et nouveaux profils sont ON par defaut, OFF global conserve la selection et le reset efface aussi les exclusions. L'apercu de resultats reste disponible a l'Admin meme si son propre toggle est OFF. Les mots-cles sont ordonnes et normalises; les resultats restent un snapshot Room v17 persistant avec pagination par lots de `40`.
- Home > `Continue watching` et les dossiers `Historique` Live/Films/Series reappliquent le filtre parental avec les metadonnees catalogue locales avant de rendre une card.
- Les fiches detail Film et Serie n'ont plus de scroll global. Film s'arrete aux photos du casting, sans trailers/teasers ni titre sur le poster droit. Serie s'arrete apres une liste interne scrollable de quatre episodes visibles, sans section Rating ni titre sur le poster droit.
- Dans Movies, la colonne centrale publie chaque enrichissement TMDB des qu'il est resolu. Avant la reponse TMDB, chaque ligne affiche immediatement les metadonnees Xtream disponibles ou le dossier courant; elle ne reste plus sur `Informations en cours...`. La fiche Film reserve l'espace sous les portraits pour afficher les noms du casting.
- Depuis le 2026-07-10, Back depuis Settings retourne sur Home avec le focus sur l'icone Settings du header (et non l'onglet Home). Profile cible l'icone Profile et Notifications cible l'icone Notifications. Le scroll Home revient a zero pour afficher le hero complet sous le header.
- Le hero Home ne contient plus de bouton et n'entre jamais dans le parcours D-pad. Son index est reinitialise au changement de profil/du mode Kids; les images/route peuvent venir du backend, mais le titre et le sous-titre utilisent toujours les `SmartVisionStrings` localises afin de ne pas melanger francais et anglais.
- Settings > Attribution TMDB affiche le statut configure/non configure du token TMDB sans exposer sa valeur, l'attribution TMDB, la mention JustWatch providers et la note licence developpement personnel.
- YouTube: recherche/suggestions/player, favoris, parametres YouTube et queue autoplay, soumis a feature lock.
- Notifications reprend le layout TV a deux colonnes: menu gauche `250 dp` (Mise a jour, Playlist, Information importante, Historique) et liste flexible a droite. `Toutes` a ete retire; l'ouverture cible Mise a jour. Les badges rouges comptent uniquement les non-lues; une notification rejoint l'historique apres ouverture et y reste jusqu'a purge admin ou action utilisateur `Clear All`.
- L'ecran Notifications garde le refresh automatique a l'ouverture, mais n'affiche plus de bouton Refresh manuel. Hors Historique, un bouton `Tout marquer vu` apparait seulement si au moins deux notifications sont visibles et demande confirmation. Dans Historique, `Clear All` apparait seulement si la liste contient des notifications et demande confirmation avant purge locale de l'historique de l'appareil.
- Les mises a jour rouvrent le dialogue canonique `AppUpdateViewModel.openFromNotification()`. Les playlists et messages admin ouvrent un dialogue detaille; les anciennes lignes sans payload utilisent un dialogue generique.

## 4. Workflow technique

Navigation:
- `AppNavigation.kt` cree les repositories/viewmodels communs.
- `headerTabs()` definit les onglets.
- `navigateSingleTop()` evite les doublons.
- `RemoteSettingsNavigation` relaie les touches globales Settings/Menu recues par `MainActivity.dispatchKeyEvent()` vers la route `settings` avec `navigateSingleTop()`, sans modifier le routage D-pad, Back ou media des ecrans.
- Les routes player sont separees par type de contenu.

Etat:
- `ActivationViewModel` pour acces;
- `ActivationViewModel.localStateReady` empeche le rendu temporaire de l'ecran activation avant la lecture du cache local;
- `AppConfigViewModel` pour feature flags et consent;
- `AppUpdateViewModel` pour update;
- `SettingsRepository` pour preferences.
- `ParentalControlViewModel` combine settings, scope parental global, liste de profils, profil actif et `catalogRevision`; `RoomParentalCatalogRepository` reutilise le snapshot persistant valide ou le regenere sans charger le catalogue complet en memoire.
- `HomeViewModel` seme son etat initial uniquement depuis les caches correspondant au profil actif. Une bascule produit une seule revision catalogue, annule puis detache les anciens jobs, vide tendances/reprises et relit compteurs/tendances avec le `profileId` capture plutot que le profil actif mutable. Chaque cycle possede une generation et chaque requete un ID monotone: une tentative annulee, remplacee ou en erreur ne peut ni publier `0/0/0` ni marquer la revision chargee. Les compteurs, leur flag de chargement et la revision chargee vivent dans le meme `HomeLoadGate` et sont publies atomiquement.
- L'ouverture du picker et la selection sont deux evenements distincts. `openProfilePickerFromHome()` ne recoit aucun `profileId`, ne cree aucun `requestId` et ne touche ni au profil actif ni au catalogue. La requete `requestId + profileId` nait uniquement de `ProfilePickerScreen.onSelectProfile`, apres composition de Home au premier plan.
- Who's Watching identifie chaque clic par `requestId + profileId`. La transition conserve ses animations existantes mais ne se termine que si cette requete est encore courante, le profil actif correspond, l'activation/synchronisation est terminee et un jeton Home `(profileId, catalogRevision)` correspond encore a la revision autoritative du repository apres relecture differee d'une frame. Les compteurs Room du profil cible sont verifies avant et apres l'activation: un cache local vide force une synchronisation meme avec une empreinte courante, puis reste en erreur/retry si la source ne fournit toujours aucun contenu. La navigation horizontale du picker boucle explicitement du premier profil vers `Add Profile` et de `Add Profile` vers le premier profil apres composition lazy de la cible. Pour la fluidite D-pad, le focus simple ne scale plus les cards et ne met plus a jour les bounds de toutes les cards.
- `HomeViewModel` expose aussi les etats `continueWatchingLoading` et `trendingLoading` pour afficher les skeleton rows sans rendre de cibles focusables vides.
- `HomeViewModel` prepare les previews premium de tendances avec une concurrence bornee a `2`, deduplique les jobs par item et met a jour progressivement les listes sans bloquer HOME.
- `HomeHeroBanner` route les CTA applicatifs via la navigation active et n'ouvre le QR offre que pour les liens externes/non-routes.
- Diagnostic temporaire `PERF_DIAG`: en build `releaseDiagnostic`, Home ecrit des evenements locaux sur l'etat visible, les caches, les transitions focus/scroll `LazyRow` et les callbacks mini-player Media3. Ces logs servent uniquement au diagnostic performance Splash/Home et doivent etre retires apres exploitation.

## 5. Ecrans concernes

- `HomeScreen.kt`
- `HomeCollectionsScreen.kt`
- `ContinueWatchingRow.kt`
- `TrendingContentRow.kt`
- `LiveTvScreen.kt`
- `MoviesScreen.kt`
- `SeriesScreen.kt`
- `MovieDetailScreen.kt`
- `SeriesDetailScreen.kt`
- `ProfileScreen.kt`
- `SettingsScreen.kt`
- `YoutubeScreen.kt`
- `NotificationsScreen.kt`

## 6. Fichiers de code concernes

- `ui/navigation/AppNavigation.kt`
- `ui/home/*`
- `data/xtream/XtreamConnectionManager.kt`
- `ui/live/*`
- `ui/movies/*`
- `ui/series/*`
- `ui/detail/*`
- `ui/profile/ProfileScreen.kt`
- `ui/profile/ParentalControlPanel.kt`
- `ui/profile/ParentalControlViewModel.kt`
- `ui/profile/ProfilePickerScreen.kt`
- `ui/settings/SettingsScreen.kt`
- `ui/settings/ParentalContentFilter.kt`
- `data/tmdb/*`
- `ui/youtube/*`
- `ui/notifications/*`
- `ui/update/*`
- `ui/i18n/SmartVisionStrings.kt`

## 7. Donnees / API / Backend / Admin

Sources:
- Room pour catalogues, favoris, progression.
- DataStore pour settings et activation locale.
- `api/home_slides.php` pour hero slides.
- `api/notifications.php` pour notifications.
- `api/app_config.php` pour feature flags/consentement.
- `api/app_update.php` pour update in-app.

## 8. Dependances

- UI TV/focus.
- Activation/licence pour gating.
- Catalogue/playback pour contenus.
- Monetisation/consentement pour locks et popups.
- Backend/admin pour slides, notifications, config et update.

## 9. Regles a ne pas casser

- Les sections contenu doivent garder le fallback QR Xtream si aucun compte.
- Si Xtream est indisponible, ne pas ouvrir Live TV / Movies / Series depuis Home ou Header; afficher le popup "Connexion Xtream indisponible".
- Si Xtream est indisponible, ou en verification sans connexion valide connue, ne pas ouvrir non plus les routes profondes `player/*`, `movie_player/*`, `movie_detail/*`, `episode_player/*` et `series_detail/*`.
- Le header doit rester coherent entre ecrans.
- YouTube lock doit respecter `app_config`.
- Settings langue visible limitee a English et Francais, English par defaut.
- Toute nouvelle ligne ou libelle visible dans l'app TV doit passer par `SmartVisionStrings.kt` avec valeur anglaise par defaut et traduction francaise.
- Les demandes sont souvent formulees en francais, mais la copie officielle de l'application reste l'anglais.
- Ne pas reintroduire des langues non demandees sans consigne.
- Toute attribution TMDB/JustWatch visible doit rester dans Settings et ne doit pas exposer le token.
- Garder les actions TV focusables.
- YouTube: ne pas recharger toute la liste de suggestions a chaque video; consommer la queue courante, enlever la video lancee et recharger en arriere-plan seulement les elements manquants.

## 10. Problemes connus

- Certaines traductions visibles restent a externaliser selon `TRANSLATION_PROGRESS.md`.
- Les releases anciennes dans les trackers ne sont pas la source actuelle.
- Les notifications update doivent ouvrir le popup update, pas seulement Home.
- Les notifications playlist et information importante sont cliquables et affichent leur dialogue detaille; les secrets playlist ne sont fournis qu'apres validation du token de la TV ciblee.
- Le popup update ne doit pas s'ouvrir automatiquement apres un check silencieux; ouverture uniquement depuis notification update ou bouton Settings > Updates > Check for update.
- Info compte doit garder les cartes source Xtream/M3U/EPG compactes pour tenir sans scroll sur TV; les bascules source sont ON vert / OFF rouge et un seul ON est autorise.
- Les actions Modifier par QR / Modifier / Supprimer de la carte Xtream doivent rester en boutons icones focusables pour limiter l'encombrement.
- Les ecrans catalogue ne doivent pas ouvrir un snapshot complet Live TV / Movies / Series; garder la pagination Room locale pour proteger les gros catalogues Firestick.

## 11. Quand lire ce fichier ?

Lire ce fichier si la demande concerne:
- Home;
- ecran catalogue;
- profil;
- settings;
- YouTube;
- notifications;
- traduction UI;
- update popup;
- header ou onglets.

Ne pas lire ce fichier si la demande concerne uniquement:
- schema SQL backend sans ecran;
- build Gradle pur;
- player plein ecran tres technique, lire plutot catalogue/playback.

## 12. Historique court

- 2026-07-20: header principal ajuste avec six PNG custom regeneres et detoures en alpha reel: suppression du rail sous les icones, retour au focus standard `tvFocusTarget`, icones `44 dp` hors focus puis `32 dp` au focus/selection pour afficher le label. La hauteur reste `44 dp`.
- 2026-07-21: les six onglets principaux passent aux vecteurs `ic_header_*` et a un rendu focus dedie: distinction monochrome/original, YouTube rouge sans tint, scale conteneur `1.15`, halo partage glissant, barre bleue et label uniquement au focus. Notifications, avatar, Parametres, logo, horloge, ordre et routes ne changent pas.
- 2026-07-18: Who's Watching active seulement le profil et termine sa transition; aucun statut, loader ou erreur catalogue n'est affiche sur une card profil et aucune synchronisation reseau ne part derriere le picker.
- 2026-07-20: Who's Watching masque Home au demarrage tant qu'aucun profil n'est selectionne, affiche le picker sans dependance foreground, et allege le D-pad gauche/droite en retirant le scale de focus simple et les mesures de bounds hors selection.
- 2026-07-18: pendant la synchronisation Home, seule la card de la phase Live TV, Films ou Series affiche une couronne cyan 4 dp et une barre basse avec phase, compteurs et pourcentage. Son badge Watch now/Explore est masque sans changer les dimensions ni le focus; les actions catalogue restent bloquees jusqu'a la fin globale.
- 2026-07-18: Attribution TMDB ajoute un toggle focusable ON/OFF, ON par defaut, avec libelles anglais et francais.
- 2026-06-29: migration vers documentation specialisee.
- 2026-06-29: ajout du domaine YouTube et notifications dans le routage UI.
- 2026-06-30: ajout des routes `continue_watching` et `trending`.
- 2026-06-30: clarification politique langue English par defaut / Francais secondaire et popup update non automatique.
- 2026-06-30: YouTube enrichi avec favoris visibles dans le header, bouton parametres persistant, nettoyage historiques/favoris et suggestions consommees dynamiquement.
- 2026-06-30: Home/Header ajoutent l'etat bloque Xtream avec overlay et popup d'alerte.
- 2026-06-30: extension du blocage Xtream aux routes detail/player et a l'etat verification en cours.
- 2026-06-30: correction du flicker Home: `CONNECTED + checking` ne bloque plus les cartes; seuls les etats inconnus/en erreur pendant verification restent bloquants.
- 2026-06-30: Profil > Identifiants Xtream affiche la derniere synchronisation sous le bouton et lance la synchro via un popup dedie avec compteurs, progress bars et routage focus vers Appareil et catalogue.
- 2026-07-01: Home reste limite a des petits jeux locaux; les ecrans Live TV / Movies / Series chargent les contenus par pagination Room locale.
- 2026-07-01: `SplashActivity` affiche un fond video Compose Media3 muet et boucle, avec le logo, la progress bar et les statuts startup au-dessus.
- 2026-07-01: suppression du flash transitoire "Activer SmartVision" apres splash pour les appareils deja actifs.
- 2026-07-01: Profil client renomme Info compte; la section Xtream adopte une mise en page cartes/actions et ajoute l'affichage/edition de l'URL EPG.
- 2026-07-01: Info compte reutilise le header principal, reduit le menu gauche, agrandit/compacte le panneau droit, retire les cadres d'icones dans la section Xtream/EPG, et corrige le flash temporaire du bouton YouTube verrouille pendant le chargement de la config.
- 2026-07-01: Info compte compacte la section source, ajoute le lien M3U et les bascules exclusives Xtream/M3U avec etat ON/OFF visuel.
- 2026-07-01: Info compte compact: icone utilisateur bleue, badge usage deplace vers Licence, expiration Xtream dans l'en-tete Info compte, boutons Xtream en icones et identifiants sur une seule ligne.
- 2026-07-01: badge `E` sur les lignes Live TV avec EPG et etats Movies/Series explicites quand M3U est actif.
- 2026-07-02: Continue watching ajoute le mini-player au focus et les ecrans catalogue reutilisent le prechauffage splash categories/premieres pages pour reduire le loading d'ouverture.
- 2026-07-02: correction Home: padding de bord sur les carrousels, reset horizontal vers le premier item avant focus vertical, et logs diagnostics `SVHomeFocus`.
- 2026-07-02: mini-player Home poster-first: filtre tendances sur `backdropUrl`, poster paysage pendant preparation video, demarrage apres 4 secondes de focus et fondu plus lent.
- 2026-07-02: mini-player Home ajuste: Continue watching passe par fond noir sans poster, Trends limite la lecture a 40 secondes puis revient au poster paysage, le D-pad gauche est bloque sur la premiere card des carrousels et le scroll horizontal automatique par focus est retire pour reduire la vibration.
- 2026-07-02: stabilisation Home verticale: suppression du `bringIntoView()` repete sur Continue/Trending, scroll vertical unique par touche avec annulation du job precedent, focus demande apres scroll et hauteur de ligne fixe.
- 2026-07-02: Home consomme un cache final tendances prepare au startup, Continue watching rend le `PlayerView` visible et lance `play()` immediatement, et les carrousels ancrent l'item focus horizontal en premiere position visible sauf en fin de liste.
- 2026-07-02: Home corrige l'ordre initial des tendances en supprimant la restauration d'un ancien scroll horizontal, cache les sections vides, route HOME header vers un refresh local sans ecran gris et localise les libelles/fallbacks Home/Hero.
- 2026-07-03: optimisation Splash/Home locale: le splash ne bloque plus sur les details tendances Xtream, `HomeUiState` demarre depuis les caches disponibles, les carrousels gardent des slots fixes et les mini-players attendent un focus stable avec poster conserve jusqu'a la premiere frame.
- 2026-07-03: splash allege sans synchro ni chargement catalogue; Home s'affiche immediatement, decide la synchro apres premier rendu, bloque la telecommande uniquement pendant `Synchronize`, charge les categories initiales par lots de `20` dans les ecrans catalogue et affiche les tendances `10 + 10` aleatoires depuis Room hors adulte.
- 2026-07-03: Home Tendances premium ajoute `TrendingContentRow`, focus stable `1,3s`, transformation 16:9, backdrop/fallback, mini-preview muet a 15% et cache Room `home_trending_preview_cache` sans URL brute stockee.
- 2026-07-03: Live TV corrige l'affichage EPG: historique rehydrate depuis Room, badges `E` conserves sur lignes de chaines, compteur dossier bleu quand EPG disponible et details EPG scrollables au D-pad.
- 2026-07-03: les ecrans catalogue consomment des logos/posters/backdrops normalises depuis les repositories pour supporter les URL Xtream/M3U absolues, relatives ou echappees.
- 2026-07-05: Settings ajoute le menu `Network Activity` / `Activite reseau`, base sur `NetworkActivityTracker`, et Home corrige la duree de vie du fade-in audio des mini-players Continue watching LiveImmediate et Tendances.
- 2026-07-05: Settings > Activite reseau rend le panneau droit focusable et scrollable au D-pad pour parcourir toute la liste active/recente.
- 2026-07-05: le header principal affiche date/heure sur deux lignes a droite, avec logo/onglets/boutons legerement compactes pour conserver l'espace sans changer les actions.
- 2026-07-07: Settings ajoute `Attribution TMDB`; Movie detail et Series detail affichent les metadonnees TMDB en priorite quand elles sont cachees/disponibles. Home Tendances enrichit les previews via TMDB pendant la preparation visible/proche, et les grilles Movies/Series reutilisent le cache TMDB local sans recherche massive.
- 2026-07-07: Details films/series deviennent scrollables avec sections TMDB riches: mini-player trailers/teasers YouTube, casting/realisateur/createurs avec photos, note TMDB + note utilisateur locale et recommandations. Home Tendances ne joue plus les URLs Xtream d'origine en preview et affiche des skeleton cards pendant les chargements Continue watching / tendances.
- 2026-07-10: Settings reutilise le header principal et focalise le premier item du menu a l'ouverture. Back depuis Settings ou Info compte retourne explicitement sur Home avec focus header Home. Info compte supprime le menu gauche `Appareil et catalogue`, garde son contenu en bas de `Info compte` comme section focusable non cliquable, et les changements de profil synchronisent le catalogue du profil actif sans faux popup Xtream. Who's watching remplace la barre lineaire par le cercle de chargement bleu.
- 2026-07-10: Info compte > Appareil et catalogue supprime les lignes techniques identifiant appareil/version/portail, ajoute le profil proprietaire du catalogue et integre la barre/message de synchronisation dans la carte pour rester visible.
- 2026-07-10: Info compte > Appareil et catalogue recoit le focus apres fermeture du popup sync, affiche le statut dans son header, regroupe Code TV/profil catalogue, retire Derniere sync et reutilise les visuels HOME dans les trois cartes.
- 2026-07-10: HOME garde les phases `SyncStatus.CatalogProgress` par carte et expose aussi le succes pendant 2,5 secondes avant retour a l'etat neutre.
- 2026-07-10: Notifications reutilise le header principal, montre non-lu/date/priorite, fournit retry/etat vide et force chaque refresh GET sans cache avant mise a jour du badge.
- 2026-07-13: Parametres utilise une `LazyColumn` auto-scrollable et son premier item est relie au header; Info compte expose une pastille parentale reactive. Who's Watching renforce le focus par scale/z-index. Le filtre AR utilise `ic_filter_ar.png` et le libelle `Arabic` / `Arabe`.
- 2026-07-14: Notifications adopte les categories typees et le cycle non lu -> historique -> purge. Synchronisation est ajoute entre Controle parental et Historique dans Profile tout en restant provisoirement dans Settings; l'ancien Controle parental est retire de Settings.
- 2026-07-14: suppression du doublon Synchronisation dans Settings, alignement de Settings sur le ratio `250 dp / 16 dp` de Profile/Notifications et refonte de la colonne droite Profile > Synchronisation selon les cartes Options generales, Frequence et Resume.
- 2026-07-15: separation de `Info profil` et `Gerer les profils`, ajout de `profile/manage`, statistiques cachees exactes persistantes Room v18 et restauration du menu Profile complet.
- 2026-07-17: Who's Watching anime le profil choisi vers le centre avec zoom, disparition des autres cartes et loader suivant le perimetre carre arrondi de l'avatar jusqu'au chargement complet. La sortie revele Home puis deplace l'avatar vers les coordonnees mesurees du bouton profil du header; cet avatar remplit desormais le bouton sans fond colore intermediaire.
- 2026-07-18: le formulaire commun de creation/modification ADMIN, NORMAL et KIDS propose une multiselection D-pad des pays/langues. Aucun choix signifie catalogue complet; les choix sont stockes dans le JSON profil existant, sans migration Room. Un profil actif est resynchronise apres modification, un profil inactif est invalide jusqu'a sa prochaine ouverture.
- 2026-07-18: Who's Watching affiche sous la carte choisie la phase reelle, une progression determinee uniquement si des totaux existent, les compteurs Live/Movies/Series charges/total et le temps ecoule. Une erreur conserve le picker avec Retry/Back. La transition Home exige profil actif, empreinte catalogue courante, Home pret et application au premier plan; un retour apres bouton Home ne saute plus silencieusement vers Kids.
- 2026-07-18: `Info profil > Changer de profil` supprime son selecteur local et redirige vers le Who's Watching global seulement apres composition de Home au premier plan. L'ouverture ne cree aucune activation ni synchronisation; le choix explicite d'une carte reste l'unique declencheur.
- 2026-07-10: Personnalisation est regroupee en style de focus puis roles Selected/Active/Parent, avec menu iconé existant et apercu direct des quatre etats.
