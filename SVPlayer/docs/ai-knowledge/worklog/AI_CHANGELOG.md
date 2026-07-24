# AI Changelog

- 2026-07-24: correction du triptyque Admin/PlaylistWeb/background: `Reset Xtream` efface aussi l inventaire v2 et force le statut manquant; les profils configures jamais synchronises sont synchronises automatiquement par `profileId`, meme inactifs; les chemins relatifs des fonds importes par l admin sont convertis en URL SmartVision absolue avant chargement Coil et la config est relue au retour au premier plan.

- 2026-07-24: refonte ciblee de `Parametres > Licence SmartVision` d apres le modele TV: statut/expiration en en-tete, carte Premium compacte sans scroll, QR/code TV a droite et liste des capacites Premium reelles (sans pubs, Recorder, Media Center, transfert telephone-TV, multi-profils, controle parental). `Activer` devient l unique bouton du panneau et ouvre le dialogue Premium existant; `Actualiser` est retire. Copies EN/FR centralisees dans `SmartVisionStrings`.

- 2026-07-24: les livraisons web demandant `PlaylistWeb` reutilisent desormais le profil reserve existant au lieu de creer `PlaylistWeb (2)`. Who's Watching met en file une synchronisation Home quand le profil valide est vide ou obsolete, meme s'il etait deja actif, et les compteurs Live/Films/Series sont relus apres chaque section terminee. Le fond global retire totalement son voile tout en gardant `ContentScale.Crop`. Le dialogue Premium centre icone/texte dans le bouton bleu Activer, ajoute une icone Fermer et route Bas vers Fermer lorsque l'action d'activation est desactivee.

- 2026-07-24: inventaire appareil v2 authentifie et chiffre pour tous les profils locaux, dont Kids; statut Xtream admin recalcule, onglet Xtream admin en session `no-store`, suppression transactionnelle des appareils et upload URL/fichier pour la personnalisation. PlaylistWeb ajoute EPG Xtream auto/editable, onglets mobiles sur une ligne et succes modal accessible. Hero respecte les textes admin vides et le voile du fond global passe a 32 %. `SmartVisionQrDialog` adopte la carte Activation/Xtream `680 x 410 dp` tout en conservant l ecran applicatif courant derriere un scrim a 72 %; logo/formulaire/actions sont a gauche, QR/code TV a droite et le bouton X est supprime.

- 2026-07-24: fusion de l activation initiale et de la fin d essai dans une seule surface `680 x 410 dp` inspiree du dialogue Xtream, avec fond splash, QR Premium `174 dp`, code TV persistant et parcours D-pad action verte -> licence -> activation. `ActivationOfferMode` empeche un statut expire de reproposer un second essai; les copies et erreurs sont centralisees EN/FR. `:app:assembleRelease` reussit; `testReleaseUnitTest` reste bloque par le `FakeSettingsRepository` parental preexistant qui ne suit plus cinq methodes de `SettingsRepository`.

- 2026-07-23: suppression du flash activation avant le dialogue Xtream. `ActivationViewModel` expose une resolution d acces initiale explicite; `AppNavigation` conserve le startup jusqu au verdict distant, puis affiche directement Activation, Home ou l unique `XtreamQrSetupPanel`. L ancien `XtreamSetupDialog` dormant est supprime.

- 2026-07-23: les QR de configuration Xtream du dialogue et du Profil utilisent maintenant le code TV persistant et l URL simple `/playlist/?device={publicDeviceCode}`. La page Playlist pre-remplit et valide ce code; le client Android courant ne cree plus de session Xtream temporaire, tandis que l ancien endpoint PHP reste compatible avec les APK existantes.

- 2026-07-22: `XtreamQrSetupPanel` adopte le standard visuel valide `Generated image 2.png`: fond officiel du splash, carte compacte centree, formulaire a gauche, QR et code TV a droite, focus D-pad bleu/blanc et copies EN/FR centralisees. `:app:compileReleaseKotlin` reussi, sans test Fire Stick.

- 2026-07-22: le controleur mini-player Home detache explicitement l ancienne `PlayerView` avant d attacher la surface active au player partage; la liberation d une ancienne card ne peut plus conserver ou retirer la mauvaise surface.

- 2026-07-22: Home vide l etat Hero puis force `api/home_slides.php` a l entree. `Admin > Personnalisation` regroupe les images Hero multiples et le fond global expose par `api/app_config.php > appearance`; le choix local de fond est retire des Parametres TV.

- 2026-07-22: Licence SmartVision retire `Saisir une licence`, enrichit statut/expiration/appareil/publicites et affiche le QR Premium/code TV directement. Ajout de `ui-ux/dialog-inventory.md` pour les dialogues actifs, dormants et leur future standardisation. Validation locale: `:app:compileReleaseKotlin` et syntaxes PHP reussis, sans test appareil/ADB.

- 2026-07-22: correction de l'ecart de hauteur des mini-players Movies/Series: le lecteur actif, l'etat vide et le skeleton utilisent maintenant le ratio de preview Live TV `1.88` au lieu de `16/9`.

- 2026-07-22: le bouton Filtre des Categories Live bascule maintenant la barre des le premier OK. Le guide de zapping conserve son etat entre les changements rapides de chaine, relance son delai sans flash et adopte un rendu plus petit a gauche: logo et nom uniquement, sans numeros ni cadres globaux/logo.

- 2026-07-22: le fullscreen Live reserve Haut/Bas au zapping, laisse l'ouverture des Parametres a Menu et affiche pendant 3 secondes un guide transparent de cinq chaines Room centre sur la lecture courante. Les filtres pays Live sont maintenant replies par defaut, sans popup, avec un parcours D-pad explicite entre header, bouton Filtre, drapeaux et categories. Aucune migration Room ni modification Movies/Series.

- 2026-07-22: Who's Watching reutilise maintenant le fond cinema du Splash, conserve le focus courant sur D-pad Haut et affiche uniquement Quitter/Annuler sur Back. Les cards gagnent `6 dp` de padding vertical en haut et en bas. Le spinner Splash devient un `AnimatedVectorDrawable` autonome dans une vue Android, entierement decoratif et sans dependance a la progression ou aux recompositions de statut.

- 2026-07-22: le bloc Premium/QR du panneau Preview Movies/Series appelle maintenant le meme composable que Live TV, avec les textes VOD; rendu, dimensions et focus ne peuvent plus diverger. Navigation et gating Premium inchanges.

- 2026-07-22: transition Splash -> Who's Watching ciblee: le cercle devient un spinner cyan indetermine, dessine statiquement puis tourne sur sa propre couche graphique en boucle lineaire `800 ms`, independante des changements de statut. Le splash attend une vraie card precomposee suivie d'une frame, puis revele toutes les cards ensemble sur `140 ms`. La card selectionnee conserve le loader cyan autour de l'avatar pendant au moins `3 s` apres centrage et passe sa bordure en or pour separer les deux signaux visuels. Aucun flux d'activation, de Home, de synchronisation ou de navigation D-pad n'est modifie.
- 2026-07-21: correction des 12 anomalies de l'audit TV: dossiers fournisseur preserves apres normalisation, erreur VOD bornee et relancable, charge D-pad Live reduite, BackHandler rendu unique, restauration exacte Episode/YouTube, retour Haut Live, actions profil d'ajout neutralisees, libelles EN/FR completes, route active distincte du focus, grille Settings non tronquee et protection R8 Android 9 contre le chemin `ClassValue`. Validation appareil volontairement omise faute de Fire Stick; compilation et publication release documentees dans ce meme lot.
- 2026-07-21: lot de corrections TV ciblees: header sur `TvFocusStyle` avec route active persistante, focus visuel non cliquable des mini-players et progression VOD fullscreen renforcee, arret de la preview Live lors des changements de selection, dossiers Favoris/Historique masques a vide, contenu `ALL` groupe dans l'ordre des dossiers, plateformes alignees a gauche, fiche Serie reequilibree, Who's Watching precompose avec animation au handoff et loaders affines. Validation locale: `:app:compileReleaseKotlin` reussi; assemblage Release execute dans le meme lot.
- 2026-07-21: correction groupee UI/performance: sous-dossiers plateformes alignes a gauche, HOME header ramene a la racine avec cache chaud stale-while-revalidate, fiche Serie avec poster TMDB de saison/fallback et cinq episodes visibles, overlay Live agrandi avec luminosite exclusive, et splash unique cinema 1920 x 1080 avec logo officiel et loader circulaire reel. Aucune migration Room, SplashActivity ni modification backend.
- 2026-07-21: correction premium du header apres validation Fire Stick: six PNG generes blancs `34 dp`, alignement dans un header `52 dp`, focus local blanc discret, suppression du halo bleu mobile et du trait bleu. Les logos Netflix/Prime/Apple/Disney sont remplaces par des PNG generes centres. `AppNavigation`/Who's Watching est precompose sous le splash et sa premiere frame termine la progression locale a quatre etapes. Les filtres pays/drapeaux sont retires de Movies/Series avec leurs requetes Room multi-categories; Live TV et les groupes de marques restent inchanges.
- 2026-07-21: Movies et Series reutilisent les filtres pays/drapeaux de Live TV jusque dans les requetes Room multi-categories de `ALL`, la recherche et la pagination. Les dossiers NETFLIX/PRIME/APPLE/DISNEY sont regroupes en accordeon exclusif avec parents logo fictif, sans compteur ni mutation du catalogue; OK replie/deplie et Droite entre dans le premier sous-dossier. Validation locale: 158 tests release sans echec et `:app:compileReleaseKotlin` reussi, sans APK ni deploiement.
- 2026-07-21: refonte ciblee des six onglets du header avec `VectorDrawable` 24 x 24, modele `HeaderIconStyle` monochrome/original, YouTube rouge non teinte, scale `1.15` par `graphicsLayer`, halo radial partage anime entre les bounds, barre bleue et label uniquement au focus. Les routes, l'ordre, le header `44 dp`, le D-pad et le bloc Notifications/Profil/Parametres restent inchanges. Validation locale: `:app:compileReleaseKotlin` reussi.
- 2026-07-21: header principal unifie sur les fiches Detail Films/Series via `TvHeader`, avec onglet actif rendu par l'etat `selected` du focus global. Detail Serie remplace Reprendre/Favoris par les boutons saisons, deplace les episodes en bas a gauche et la description saison a droite sans poster. La preview Series lance le player fullscreen au OK/clic sur une ligne episode, et le skeleton Preview Films/Series borne ses placeholders pour eviter la deformation en bas a droite.
- 2026-07-21: regeneration des assets systeme Android uniquement: icones launcher carre/ronde/adaptative et bannieres TV `tv_banner`, avec logo SmartVision recadre dans une safe zone pour eviter le rognage par les masques Android. Les logos internes de l'application restent inchanges.
- 2026-07-20: correction du flash puis ecran vide apres Splash: `canDisplayGlobalProfilePicker` garde Who's Watching visible pendant `openProfilePickerAfterHome`/staging Home, tandis que la selection reste bloquee jusqu'a Home foreground. Le Splash n'affiche plus une barre vide et ne revele pas le loading si les checks sont deja termines au moment du handoff.
- 2026-07-20: nettoyage Splash/Header/Media/Notifications: `MainActivity` retire le Crossfade splash->app et utilise `shouldRevealStartupLoading`; `AppNavigation` rend l'app sur fond opaque pour eviter la frame noire avant Who's Watching. Le header ne rend plus le bouton Premium ni le separateur vertical avant l'horloge, tout en conservant le dialogue Premium pour les features verrouillees. Media prive est retire des routes Android, repositories, ecran Media, flags, endpoints PHP, admin et deploy. Notifications remplace Refresh par `Tout marquer vu` et `Clear All` conditionnels avec confirmations, et `api/notifications.php` ajoute `clear_history`.
- 2026-07-20: refonte du header principal selon le choix `M04/H01`: les onglets centraux deviennent icon-first avec icones Compose Canvas glass bleues, rail lumineux et libelle visible seulement au focus ou sur la route active. Un separateur est ajoute apres YouTube avant le bloc droite, qui conserve ses boutons cle premium, notifications, avatar, parametres et horloge. La hauteur mesuree du header reste `44 dp`; le degagement Home sous header est reduit a `8 dp`.
- 2026-07-20: correction supplementaire Who's Watching apres retour terrain: Home est maintenant couverte par un masque opaque tant qu'un picker profil est requis et qu'aucun profil n'est selectionne, ce qui evite tout flash Home apres le splash. Le picker s'affiche sans attendre l'etat foreground, tandis que la selection reste protegee par Home foreground. Le focus gauche/droite retire le scale anime simple, reduit l'ombre et ne mesure les bounds que pour la card selectionnee afin de fluidifier la telecommande.
- 2026-07-19: Who's Watching s'affiche des le premier rendu de demarrage sans frame Home visible; Home reste composee mais cachee/non interactive pendant le picker. La navigation D-pad boucle maintenant `Add Profile` -> premier profil et premier profil -> `Add Profile` via scroll lazy protege, avec animations de focus profil raccourcies. Validation locale: tests release et compilation release lances dans ce lot.
- 2026-07-19: annulation ciblee des animations demandees: les cards Home Live/Films/Series reviennent a la navigation NavHost standard sans container transform ni verrouillage de touches, et le mini-player Live ouvre directement le plein ecran sans expansion/repli. La hauteur `170 dp`, la variante sync anti-clipping, Who's Watching et les transitions mini-player Films/Series restent inchanges. Validation release locale executee, sans APK ni deploiement.
- 2026-07-18: correction du rendu des transitions signalee sur TV: Home partage maintenant la card categorie complete avec le vrai conteneur catalogue, ajoute une etape centree a `1,12x` avant expansion/repli et garde le header stable avec verrouillage des touches/restauration du focus. Les mini-players Live, Films et Series mesurent leurs bounds et utilisent le meme mouvement centre puis plein ecran, reversible. Les cards Home reviennent a `170 dp` avec variante sync compacte anti-clipping. Validation locale: `:app:compileReleaseKotlin` reussi apres correction d'une expression keyframe; aucun APK ni deploiement.
- 2026-07-18: les cards categories Home passent a `190 dp` pour afficher le detail sous leur progress bar; Who's Watching mesure le plus haut libelle, uniformise profils/actions d'ajout avec paddings haut/bas `14 dp` et gradient bleu opaque. Les clics directs Live/Movies/Series utilisent un container transform Compose reversible sous le header, bloquent les entrees pendant l'animation et restaurent la card source au Back. Validation locale: `:app:compileReleaseKotlin` reussi, sans APK ni deploiement.
- 2026-07-18: tous les emplacements d'avatars profil adoptent un masque carre arrondi proportionnel a 16 % au lieu du cercle: Who's Watching, transition vers Home, header, gestion/edition et controle parental reutilisent `ProfileAvatarShape`; les images remplissent le cadre en `Crop` et le loader suit le meme perimetre sans changer les tailles ni le focus.
- 2026-07-18: le segment cyan du loader des cards categories Home passe de 36 % a 46,8 % du perimetre, soit une nouvelle augmentation de 30 %, sans modifier son epaisseur, sa couleur ni sa vitesse.
- 2026-07-18: le segment cyan du loader de synchronisation autour des cards categories Home passe de 24 % a 36 % du perimetre, soit une longueur augmentee de 50 %, sans changer son epaisseur ni sa vitesse.
- 2026-07-18: Home limite les films Tendances aux durees strictement superieures a 80 minutes avec backfill classe et preparation bornee, scope les mini-players par rangee, deplace toute synchronisation Who's Watching vers Home avec progression sequentielle sur les cards Live/Films/Series, et ajoute le toggle TMDB ON/OFF persiste qui masque le cache sans le supprimer.
- 2026-07-18: l'editeur de profil accepte le prefixe special `[X]`/`[x]`, compacte les boutons de filtre au code reel sans icone ni libelle duplique, place les filtres actifs en tete, route D-pad Nom -> filtres -> saisie manuelle -> source et permet l'ajout manuel localise d'un code de 1-3 lettres.
- 2026-07-18: bloc profils/catalogue/notifications/Live TV: titres Film Continue sur une ligne `15 -> 12 -> 10sp`, recherche Films/Series avec espaces, compteur Live Room categorie+recherche, filtres de prefixes par profil avec empreinte de fraicheur Kids/Admin, notification `New playlist added` transactionnelle et observable, puis session ExoPlayer Live partagee avec expansion/repli. Le chargement profil est durci: Home critique Room-only, Continue Watching hors verrou, aucune recuperation episode reseau, une seule revision par bascule, lectures scopees au profil capture et composition Home garantie depuis Profile/Manage. La course finale `Success`/revision et le faux succes `finally` des compteurs sont supprimes par observation combinee profil+revision, generations/IDs de requete, publication atomique du `HomeLoadGate` et jeton ready `(profil, revision)` revalide contre le repository; un catalogue local vide force desormais une synchronisation du profil cible avant toute entree Home.
- 2026-07-17: correction structurelle du changement de profil audite sur TCL: Info profil reutilise maintenant la requete animee `requestId + profileId` de Who's Watching, et Home masque tout etat d'un ancien profil avant la relecture Room. Les compteurs des cards categories passent au-dessus du titre a `27 sp`; les noms Live de Continue restent sur une ligne avec repli `12 -> 10 sp`, le badge `LIVE` gagne un point rouge, et les tendances Films/Series deviennent uniques par poster normalise avec remplacement par le candidat suivant. Tests Release et build Release locaux executes, sans deploiement production.
- 2026-07-17: stabilisation changement de profil et lecteur series: requetes picker `requestId + profileId`, Home scopee par profil/revision avec annulation des reponses tardives et refresh terminal apres sync, progression rattachee au profil qui a ouvert le player, fallback `get_series_info` persiste en Room, Continue Watching avec badge `SERIE` et mini-player reparable, vrais voisins/titres episode, bouton `Detail serie`, et Back Live en un clic hors panneau secondaire. Validation locale: `:app:testReleaseUnitTest` et `:app:assembleRelease` reussis; tests Fire Stick a reprendre, aucun deploiement production.
- 2026-07-17: Who's Watching remplace l'attente fixe par une transition premium pilotee par l'etat reel du Home: card selectionnee centree et agrandie, autres cartes effacees, loader circulaire au rayon de l'avatar, Home precharge derriere l'overlay puis avatar deplace vers le bouton mesure du header. L'avatar du header est maintenant bord a bord sans fond de couleur. Validation: `assembleDebug`, deux `assembleRelease`, installation signee `adb install -r` et parcours Walid -> Home sur Fire Stick AFTSSS reussis sans crash/ANR.
- 2026-07-17: correction de compilation dans `ContentProgressCard`: suppression de l'usage direct de `Modifier.weight` pour la barre de progression du bloc Continue Watching, afin d'eviter l'acces Compose internal pendant le build release.
- 2026-07-17: Home place un fond noir derriere les mini-players actifs Continue/Tendances sans modifier leur cycle de vie; les cards Live/Films/Series retirent leurs badges de type, agrandissent les titres de 50 %, remontent leur contenu et affichent les compteurs Room face aux actions, avec constantes de layout centralisees.
- 2026-07-17: correction ciblee Catalogue/Home sans refonte: `DPAD_DOWN` sort des recherches non vides vers la liste composee; Continue repare les titres Series generiques, ajoute badges Film/Live et compteurs alignes; Tendances normalise les notes, exclut les Series sous `3/5`, masque `0min`, stabilise les cinq premiers backdrops et nettoie les codes `(BE)/(JP)`; Haut/Bas entre carrousels conserve la colonne visuelle la plus proche au lieu de forcer l'index `0`. Aucun schema, endpoint, test, build ou deploiement.
- 2026-07-17: Home affiche des skeletons a froid, nettoie les titres Continue/Tendances, utilise les backdrops TMDB 16:9 et les metadonnees Film/Serie, ouvre les fiches depuis Tendances et remplace le `TextureView` du mini-player partage par un `SurfaceView`; les cartes grandissent de 20% apres la premiere image et reduisent alors leur overlay au titre.
- 2026-07-17: Continue Watching distingue Live/Film/Serie sans badge de type ni texte secondaire, conserve une barre de progression basse pour VOD et ajoute le badge `Sxx Exx` des episodes.
- 2026-07-17: le Back du lecteur Live masque d'abord l'overlay puis quitte au second appui, et l'index de focus fantome entre luminosite et chaine precedente est supprime.
- 2026-07-17: casting Film focusable et agrandi, lignes Films/Series simplifiees avec annee finale, fallback d'image Series, titres plus grands et recherche TMDB basee sur le meilleur score global avec revalidation des mappings faibles.
- 2026-07-17: correction definitive du matching TMDB des lignes Films: suppression prioritaire des prefixes fournisseur et annees entre parentheses, fallback d'annee extrait du titre au lieu du timestamp Xtream, et isolation des erreurs par item comme sur Series.
- 2026-07-17: suppression complete de Network Activity et de son instrumentation; Preferences/Personnalisation passent en grille compacte et appliquent format video, animations reduites, profils buffer, reconnexion et horloge configurable.
- 2026-07-17: correction de la course categories/pages Live TV qui laissait `ALL` en skeleton; les jobs categories et chaines sont separes et toute annulation remet les loaders a zero avant relance.
- 2026-07-17: DPAD Haut/Bas zappe maintenant la chaine Live avec l'overlay principal visible ou masque, sans masquer l'overlay avec Bas; les panneaux secondaires conservent leur navigation.
- 2026-07-17: les lignes Films utilisent un fallback de metadonnees immediat au lieu de `Informations en cours...`; les portraits de casting sont compactes pour rendre les noms visibles sous les photos.

- 2026-07-17: uniformisation des recherches compactes, filtres et compteurs des colonnes centrales Live TV/Movies/Series; les lignes Movies affichent note/genre/annee/duree et Series charge progressivement backdrops paysage et compteurs saisons/episodes sans clic.
- 2026-07-17: fiche Movie en duree `xh xxm`, sans badge conteneur, avec casting informatif agrandi en portraits `2:3` nommes; le poster final de preview Series reste paysage.
- 2026-07-17: navigation circulaire bidirectionnelle du header, des trois categories Home, de Continue Watching et des rangees Tendances.
- 2026-07-17: Home neutralise la relocation verticale Compose post-focus tout en conservant la revelation horizontale native des `LazyRow`; le hero reste complet sous le header apres cinq secondes et pendant les transitions D-pad.
- 2026-07-17: suppression du clipping tous axes du viewport Home pour rendre les halos Live TV/Series entiers, localisation du texte hero via `SmartVisionStrings`, et rythme vertical unique de `12 dp` entre categories/Continue/Tendances.
- 2026-07-17: Home aligne le clearance bas du header sur son padding haut a `14 dp`, pour un espace vertical symetrique avant le hero sans modifier la hauteur des controles.
- 2026-07-17: Movies actualise maintenant les metadonnees externes de la colonne centrale ligne par ligne; une requete lente ne bloque plus tout le paquet et les valeurs Xtream non fiables restent masquees pendant l'enrichissement.
- 2026-07-17: suppression des cadres globaux des colonnes de contenu Profile et Settings; les titres restent externes et seuls les cadres propres aux sous-sections sont conserves, sans changement de focus ni de comportement.

## 2026-07-17 - EPG courant/futur et Playlist multi-profils

- Scope les caches EPG par URL, respecte les offsets XMLTV, retire les emissions terminees et centralise le refresh EPG stale-aware apres toutes les synchronisations catalogue.
- Ajoute le registre serveur non sensible des profils, le ciblage Admin/Normal multi-selection, l'import idempotent par `config_id` et la conversion autonome des profils partageant les identifiants Admin.
- Recompose `/playlist/` avec validation du code au sixieme caractere, cartes de profils, creation combinee, champs Xtream sur une ligne et responsive mobile; Kids reste refuse cote serveur et TV.
- Ajoute les endpoints, tables SQL et uploads de deploiement associes; publie la release production `0.1.120 (177)` avec migration SQL, manifeste, endpoint de mise a jour et APK verifies publiquement.

## 2026-07-17 - Titres externes Profile et Settings

- Rend l'en-tete de `TvSectionCard` optionnel sans modifier le style des sections qui le conservent.
- Place les titres principaux des panneaux Profile et Settings au-dessus de leur cadre de contenu et retire les en-tetes internes de Manage Profiles.
- Supprime les titres et cadres imbriques de Licence SmartVision dans Settings tout en preservant le rendu autonome de `LicensePanel` dans ses autres usages.
- Stabilise le scroll Home apres le focus initial ou le retour depuis son bouton de header et conserve l'espace sous le header comme padding non scrollable du viewport, afin que `bringIntoView` ne puisse plus masquer le hero.
- Applique aux cards categories, Continue Watching et Tendances le halo bleu `25 dp` partage avec les cartes de selection de profil.

## 2026-07-16 - Header principal partage et navigation Profile

- Centralise le shell du header principal a `34 dp / 18 dp` avec un espace contenu de `14 dp`, puis l'applique a Home, Live TV, Films, Series, Media, YouTube, Notifications, Profile et Settings sans dupliquer `TvHeader`.
- Maintient le hero Home sous le header dans la zone scrollable bornee et conserve le reset du scroll lors des retours vers le header.
- Corrige Synchronisation -> Info profil en mettant a jour la destination Compose locale avant le callback de navigation, sans modifier le PIN ni le routage D-pad.

## 2026-07-16 - Uniformisation visuelle Profile et Settings

- Extrait le style des sections Synchronisation dans une primitive Compose commune: icone `23 dp`, titre `Label`, padding `16 x 14 dp`, bordure et coins `8 dp`.
- Aligne Info profil, Gestion des profils et Controle parental, puis decoupe chaque destination Settings en groupes logiques encadres sans changer l'ordre ni les controles.
- Conserve les proportions `250 dp / 16 dp`, la navigation D-pad, les toggles, dialogues et zones scrollables existantes.

## 2026-07-16 - Profils TV, focus parental et isolation Home

- Aligne les panneaux droits Info/Gestion sur Synchronisation, retire Historique/Parametres du menu Profile, reutilise les avatars d'ajout du picker et compacte le detail profil.
- Cable le D-pad menu -> Gestion/Activation et rend les toggles parentaux global/par profil focusables et actionnables au OK/Enter.
- Unifie l'activation de profil, isole Continue watching/tendances/previews par `profileId`, ajoute le fallback hero et evite le spinner Hidden items sur chaque reouverture.
- Consomme `A chaque demarrage` une seule fois par process; les changements de profil utilisent catalogue absent ou echeance 24h/48h sans resync a chaque retour Home.

## 2026-07-15 - Separation Info profil et Gerer les profils

- Conserve `profile` pour la consultation/activation/synchronisation et ajoute `profile/manage` pour creation, modification, verrouillage et suppression, avec shell ADMIN partage et menu TV complet.
- Reutilise `XtreamAccountManager`, `PlaylistProfileEditorDialog`, le PIN, `CatalogRepository` et les flows legacy; focus, selection detail et profil actif restent independants.
- Ajoute Room 17 -> 18 pour persister les exclusions Kids Live/Films/Series de la derniere synchronisation reussie et les combiner sans doublon avec le snapshot parental actif.
- Mutualise la decision de synchronisation necessaire entre Home et Info profil via `SyncFrequencyPolicy`.
- Validation: `testReleaseUnitTest` 102/102, `compileReleaseKotlin`, `assembleRelease`, `git diff --check` et installation signee `adb install -r` reussis. Fire TV: Room 17 -> 18, Info profil, compteurs, D-pad vers Synchroniser et route Gerer les profils verifies sans crash/ANR; CRUD persistant non execute pour ne pas toucher au PIN/profils reels.

## 2026-07-15 - Controle parental par profil et nettoyage des ecrans TV

- Ajoute un scope parental DataStore: activation globale plus profils explicitement desactives; l'etat consomme par Home/catalogues/details devient effectif pour le profil actif, avec compatibilite ON par defaut pour les profils existants et futurs.
- Recompose Activation en pleine largeur avec cards avatar/nom/toggle, deplace `Change PIN` dans le header et cable le D-pad entre global, profils et menu.
- Retire `Toutes` de Notifications, corrige le retour vers l'icone header et empeche l'etat vide de recevoir le focus.
- Le retrait temporaire Historique/Aide/Parametres a ete remplace par le shell Profile complet lors de la separation Info/Gestion.
- Validation: 22 tests unitaires release parentaux/notifications et compilation Kotlin release reussis.

## 2026-07-14 - Controle parental persistant, catalogues VOD et reprise player

- Ajoute le snapshot Room v17 des resultats parentaux, invalide uniquement apres changement de profil/mots-cles ou synchronisation; le clic dossier pilote maintenant les elements masques de droite.
- Applique le filtrage parental aux historiques Live/Films/Series et a Home > Continue watching avec les metadonnees catalogue locales.
- Enrichit de facon bornee les premieres lignes Films/Series via le cache/integration TMDB existants; adapte la preview Film et le second OK Serie.
- Simplifie les fiches Film/Serie pour tenir sans scroll global: casting photo final pour Film, quatre episodes visibles et scroll interne pour Serie.
- Ajoute le dialogue prioritaire Reprendre/Recommencer et le bouton de redemarrage dans l'overlay VOD.
- Validation: compilation Kotlin release, tests unitaires release cibles, assemblage release local et campagne AVD Android TV (Room 16->17, D-pad, panneaux parentaux, fiches, prompt/overlay) reussis; le flux episode fournisseur de test etait indisponible.

## 2026-07-14 - Refonte Synchronisation Profil et proportions Settings

- Retire le menu/panneau Synchronisation legacy de Settings et son callback de navigation devenu inutile; les reglages restent exclusivement dans Profile.
- Aligne Settings sur Profile/Notifications avec menu gauche `250 dp`, espacement `16 dp` et panneau droit flexible.
- Recompose uniquement la colonne droite Profile > Synchronisation en cartes `Options generales`, `Frequence` et `Resume de synchronisation`, avec toggles compacts, grille cinq frequences et valeurs du profil actif.
- Route explicitement le D-pad entre menu, toggles et choix de frequence; le serveur M3U est reduit a son host afin de ne pas exposer l'URL complete.
- Validation: `:app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain` reussi en 3 min 37 s apres retrait d'un import Compose interne inutile.

## 2026-07-14 - Refonte Notifications, Synchronisation Profil et nettoyage Profile

- Ajoute le contrat type `app_update` / `playlist_added` / `important_info`, les payloads playlist chiffres, l'historique par `seen_at` et la purge durable `purged_at` avec action admin `PURGER` auditee.
- Recompose Notifications en deux colonnes TV avec filtres, badges non lus, dialogues typés, marquage individuel et restauration du focus apres consultation.
- Retire l'ancien Controle parental de Settings et partage les reglages Synchronisation entre Settings et Profile, ou l'entree est placee entre Controle parental et Historique.
- Supprime neuf composables Profile sans reference et extrait `ProfileViewModel`, ses etats et son assemblage UI dans `ProfileViewModel.kt`.
- Validation intermediaire: PHP lint OK; `:app:compileReleaseKotlin` OK apres extraction. Tests Notifications et installation debug documentes a la fin de l'intervention.

## 2026-07-14 - Actions mots-cles Controle parental

- Corrige l'interception de OK par la carte parente qui bloquait Add, Edit et Delete.
- Rend l'ajout/modification/suppression immediatement visibles dans l'etat UI; Edit conserve son dialogue pre-rempli et Delete utilise `TvConfirmationDialog` avant mutation.
- Compacte la grille pour trois cards par ligne des `420 dp`, quatre au-dessus de `720 dp`, avec replis etroits a deux/une colonne.
- Validation: 4 tests `ParentalControlViewModelTest` reussis, puis `:app:compileReleaseKotlin` reussi en 1 min 4 s.

## 2026-07-14 - Correctifs visuels et interactions Controle parental

- Aligne le titre, les titres de sections, icones et toggle sur les dimensions d'Info compte; retire la hauteur fixe des cartes Activation/PIN et separe le titre Mots-cles des controles.
- Rend le premier OK/clic effectif pour Activation, Change PIN et ouverture du clavier, puis ajoute les clics explicites Edit/Supprimer.
- Renforce le scroll de la zone droite et la visibilite basse de Resultats, sans modifier header ni menu gauche.
- Validation: `:app:compileReleaseKotlin` termine avec code de sortie `0`; aucune verification visuelle TV/AVD executee.

## 2026-07-14 - Refonte Controle parental et resultats Room

- Remplace uniquement le panneau droit de Profile par les cartes Activation, Code PIN, Mots-cles et Resultats, avec grille adaptative quatre/deux/une colonnes et scroll local.
- Ajoute `ParentalControlViewModel`, la navigation TV a deux niveaux, la restauration de focus apres suppression, la saisie clavier seulement apres OK et le changement PIN via `NumericPinDialog`.
- Persiste la liste ordonnee en JSON DataStore avec migration de l'ancienne chaine, normalisation et compatibilite du champ historique.
- Ajoute des requetes Room sans migration de schema pour compteurs exacts, dossiers groupes et elements Live/Films/Series/episodes caches pagines par `40`; OFF retourne immediatement zero.
- Centralise le filtrage categorie/titre/description/genre et retire la note du filtre Films.
- Validation: 11 tests unitaires release cibles reussis; compilation release executee apres mise a jour finale.

## 2026-07-14 - Popups compacts de gestion des profils

- Recompose le formulaire partage picker/Info compte en popup TV responsive avec nom et avatars alignes, zone centrale scrollable et footer fixe.
- Masque Xtream/M3U par defaut dans un accordeon exclusif; conserve les valeurs, validations, partage ADMIN et sauvegarde existants.
- Route D-pad Bas vers `Enregistrer` depuis les derniers controles visibles et rend la rangee d'avatars horizontalement navigable.
- Autorise ADMIN a conserver son avatar dedie ou a selectionner tous les avatars CLASSIC.
- Validation: `:app:compileReleaseKotlin` reussi; aucune validation visuelle TV/AVD executee.

## 2026-07-14 - Nouveaux avatars de profils

- Remplace les avatars generiques par 17 PNG circulaires transparents: 1 ADMIN, 8 KIDS et 8 CLASSIC non humains.
- Centralise le rendu `painterResource` par `avatarId` / `ProfileType` dans le picker et les selecteurs existants, avec fallback par type et alias pour les anciens IDs.
- Fixe `avatar_admin` pour ADMIN, choisit aleatoirement un avatar Kids lors d'une creation sans avatar et attribue un avatar Classic aux profils NORMAL.
- Aligne les cartes d'action du picker sur les cartes profil: meme zone avatar, meme composant de libelle et meme position verticale.
- Ajoute deux PNG dedies aux actions `Add kids profile` et `Add profile`, generes depuis les references fournies puis normalises en transparence 512x512.

## 2026-07-13 - Demarrage unifie et picker de profils dynamique

- Unifie la preview Android et Compose autour du meme visuel, avec etats `LogoOnly`, `Loading` retarde et fondu croise vers l'application.
- Retire pourcentage et statuts codes en dur; progression et textes EN/FR correspondent aux etapes locales terminees.
- Remplace le picker fixe par une `LazyRow` a cles stables: noms exclusivement issus de `PlaylistProfile.name`, actions localisees, focus actif/ADMIN/premier et crayon accessible au D-pad Bas/Haut.
- Compacte et centre le groupe de cartes, uniformise le diametre des avatars et place les crayons sous les cartes, hors du cadre.
- Deplace toute synchronisation hors du picker; la selection ne fait que l'activation, la purge des caches memoire et la validation visuelle avant Home.
- Validation: `:app:compileReleaseKotlin`, 6 tests release cibles et `:app:assembleRelease` reussis. APK installe sur Firestick AFTSSS; rendu, centrage et navigation carte/crayon verifies en `1920x1080`.

## 2026-07-13 - Filtre Kids v2, catalogues harmonises et focus TV

- Ajoute le moteur Kids category-first multilingue, l'heritage sans score item, les exclusions adultes, les raisons/scores et le cache partage incremental Room v16.
- Instrumente les metriques `SVKidsFilter` et traite/import les decisions par lots sans requete TMDB/reseau par item.
- Aligne Live/Movies/Series sur les memes panneaux et proportions; titres centraux fixes, lignes VOD `56dp`, miniatures 16:9 et texte en largeur restante.
- Stabilise les images Tendances sur `item.imageUrl`, deplace la preparation hors focus et centralise les espacements Home a `10dp`.
- Fiabilise les transitions D-pad trois colonnes et Media avec attente de composition Lazy via `awaitItemVisible()`.
- Validation: 15 tests release cibles puis 61 tests release complets reussis. `:app:assembleRelease` reussi en 7 min 55 s; APK local `0.1.120 (151)`, `47354422` octets, SHA256 `abe50e9b9fee3bffda1a1b0f963ffd858f8840c4da58554336d10b99c515df50`. APK exact installe et lance sans crash sur AVD TV 720p; aucun deploiement production demande.

## 2026-07-12 - Home paysage, tendances par profil et filtres Live

- Remplace les cards portrait/expansion par des cards paysage fixes `220,4 x 124 dp` (au moins cinq visibles en 1280 px), avec scale conteneur et preview Xtream muet partage apres 550 ms.
- Supprime YouTube des previews Home, les boutons View all, les routes `continue_watching` / `trending` et `HomeCollectionsScreen`.
- Persiste les tendances uniquement pendant la synchronisation, par profil, avec tri deterministe note/nouveaute et exclusion adulte; Room passe en v15.
- Corrige le cache Continue Watching par profil, le retour Home avec pile nettoyee et la synchronisation Admin d'un profil cible sans activation.
- Ouvre Live/Movies/Series sur All, conserve All dans un filtre Live, deplace le filtre actif en deuxieme position, supprime la fleche droite et applique au premier clic.
- Restaure la demande PIN/creation PIN des l'ouverture de Controle parental dans Info compte.
- Release `0.1.118` / `148`: `:app:assembleRelease` reussi en 13 min 14 s; APK `47263353` octets, SHA256 `a4abb71da9eafaaf1990bdc77f6117a93d1d5c59aab4640a773623ddf67a9bea`.
- Production verifiee: manifeste et `api/app_update.php` en `0.1.118 (148)`, APK `smartvision-tv-v148-a4abb71d.apk`. Le test post-upload `create_playlist_setup_session.php` a retourne HTTP 500 apres publication et reste a diagnostiquer.

## 2026-07-12 - Menus, PIN, diagnostic Kids et filtres Live

- Deplace le panneau Licence en premiere position de Settings et le controle parental dans Info compte.
- Centralise les saisies PIN dans un pave numerique TV sans clavier Android.
- Ajoute la phase reelle `FILTERING`, profil Kids, compteurs et duree aux diagnostics de synchronisation Home.
- Fixe les fleches de filtres Live hors de la LazyRow et resynchronise scroll, filtre, categorie et chaines.

## 2026-07-12 - Header catalogue en profil Kids

- Movies, Series et YouTube transmettent maintenant le profil actif au header catalogue partage.
- En mode Kids, Licence/Premium, Notifications et Parametres y sont masques comme sur Home et Live TV; avatar et date/heure restent visibles.

## 2026-07-12 - Live TV, cache hero et identite Kids

- Corrige la divergence categorie/chaines Live TV, compacte les filtres et rend la fleche droite reellement scrollante.
- Deduplique les synchronisations concurrentes et ajoute le cache hero disque quotidien valide.
- Ajoute badge/fond/date Kids, renforce le filtre central et ajoute le changement de profil au dialogue de sortie.

## 2026-07-12 - Profils ADMIN/NORMAL/KIDS

- Ajout migration ADMIN unique, credentials partages/custom chiffres, PIN parental hache, verrouillage profils et permissions routes/header.
- Picker TV adapte avec cartes Kids/Normal, badge/cadenas, PIN, Crossfade, formulaire scroll/IME et validation Xtream.
- Filtre Kids multilingue central applique avant Room aux synchros Xtream/M3U; routes details et suppression profilee renforcees.
- Room 13->14 profile l'historique/recommandations YouTube; reglages de lecture scopes par profil.
- Ajout de quatre visuels Kids originaux embarques et de tests unitaires migration/permissions/filtrage.

## 2026-07-12 - Filtres rapides des categories Live TV

Type:
- Android TV / Live TV
- Catalogue Xtream et M3U
- UI/UX D-pad

Resume:
- Detection stricte des prefixes de dossiers, priorites SmartVision et ISO generique avec fallback texte local.
- Barre dynamique Tous/regions/pays/codes inconnus, panneau complet partage, compteurs, semantique et distinction focus/selection.
- Projection filtree sans modifier les categories fournisseur; recalcul automatique au changement de catalogue/profil.

Validation:
- `:app:compileReleaseKotlin` : succes.
- `:app:testReleaseUnitTest --tests com.smartvision.svplayer.ui.live.CategoryFilterResolverTest --tests com.smartvision.svplayer.data.repository.LocalCatalogSnapshotCacheTest` : succes.
- Validation visuelle et D-pad sur appareil TV reel non executee dans cette intervention.

## 2026-07-11 - Stabilisation fullscreen VOD et overlay Live unifie

Type:
- Android TV / Fire TV
- UI/UX D-pad
- Media3 ExoPlayer

Resume:
- Films/Series: DPAD Bas masque l'overlay, progressbar focusable avec seek borne et halo partage, luminosite inline, voisins Films/Episodes resolus depuis Room et boutons de limites desactives.
- Live TV: nouvel overlay conforme a la reference Films/Series, logo/nom/EPG courant-suivant/progression EPG, commandes non seekables sans -10/+10 et adaptation dynamique aux capacites Media3 reelles.
- Les changements de film, episode ou chaine reutilisent le player route existant et protegent un appui OK contre les doubles navigations.

Validation:
- `:app:compileReleaseKotlin` : succes.
- `:app:assembleRelease` : succes (`0.1.116`, versionCode `140`).
- APK final installe sur AVD `SmartVision_TV_720p_Light` : succes.
- Film et Serie reels: DPAD Bas, progression, luminosite inline, Film suivant et Episode suivant verifies; S01E01 vers S01E02 confirme.
- Live non seekable reel BFM Business: EPG courant/suivant, progression EPG, absence -10/+10 et DPAD Bas verifies.
- Aucun flux Live seekable reel disponible n'a permis une validation runtime de la variante DVR; la logique est compilee et bornee par la timeline Media3.

## 2026-07-10 - Overlay fullscreen Films/Series pixel-perfect

Type:
- Android TV
- UI/UX D-pad
- player Media3

Resume:
- Films et Series utilisent un composable partage `VodFullscreenControlsOverlay`, calibre par proportions sur la reference 1680x945.
- Le panneau glass, badges, favoris, episodes, settings et labels sont retires de cet overlay; il reste le gradient, titre/saison-episode, progression et exactement sept commandes.
- Les sauts passent a 10 secondes, la sortie fullscreen devient fonctionnelle et le routage explicite Gauche/Droite/OK conserve un halo bleu unique sur l'index actif.
- Live TV et Media local conservent leur implementation actuelle.

Validation:
- `:app:compileReleaseKotlin` : succes.
- `:app:assembleRelease` : succes (`0.1.116`, versionCode `139`).
- APK final installe sur AVD `SmartVision_TV_720p_Light` : succes.
- captures Film et Serie 1280x720 + override viewport 1920x1080 : effectuees.
- lecture/pause, +10 s (`32:41 -> 32:51`), navigation horizontale, halo mobile, luminosite et sortie fullscreen : verifies sur AVD.
- comparaison cote a cote et superposition 50 % avec la reference : effectuees.

Fichiers MD mis a jour:
- `features/catalog-playback.md`
- `ui-ux/tv-navigation-focus.md`


## 2026-07-10 - Catalogues, Series, overlays, profil, notifications, EPG et focus global

Type:
- Android TV
- UI/UX D-pad
- backend/API
- bugfix

Resume:
- Les categories Live/Movies/Series focalisent la premiere ligne sans selection ni preview; le retour Film attend la ligne source.
- Series ajoute saisons, episodes, progression et reprise dans Preview; le deuxieme OK joue l'episode courant en fullscreen.
- Les overlays fullscreen partagent une surface premium sans changer Media3, publicite ou Back.
- Info compte/Home alignent visuels et progression catalogue; Notifications reutilise le header principal et force un refresh sans cache.
- PlaylistWeb distingue les champs fournis et permet de supprimer l'EPG; le theme separe Focused/Selected/Active/Parent.

Validation:
- `:app:compileReleaseKotlin` : succes.
- `php -l server/public_html/api/save_playlist_config.php` : succes.
- `php -l server/public_html/playlist/index.php` : succes.

Fichiers MD mis a jour:
- `features/catalog-playback.md`
- `ui-ux/tv-navigation-focus.md`
- `ui-ux/screens-home-profile-settings.md`
- `technical/backend-admin-api-deploy.md`
- decision `2026-07-10-focus-state-roles.md`


## 2026-07-10 - Correction livraison PlaylistWeb depuis le site

Type:
- backend/API
- activation
- bugfix

Resume:
- Diagnostic production sur `RCRPX8`: `device_status.php` indiquait `playlist_configured=true`, mais sans `device_token` valide il ne livrait pas `playlist_config`; `notifications.php` ne remontait aucune notification visible.
- `create_playlist_setup_session.php` rattache maintenant le `device_token` courant a la session playlist deja `validated`.
- Apres un envoi playlist accepte via `/playlist/` ou `save_playlist_config.php`, toutes les sessions pending du device sont validees afin que le token local conserve par l'app puisse recuperer la playlist.

Validation:
- `php -l server/public_html/api/helpers.php` : succes.
- `php -l server/public_html/api/create_playlist_setup_session.php` : succes.
- `php -l server/public_html/api/save_playlist_config.php` : succes.
- `php -l server/public_html/playlist/index.php` : succes.
- `php -l server/public_html/api/device_status.php` : succes.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `server/public_html/api/helpers.php`
- `server/public_html/api/create_playlist_setup_session.php`

## 2026-07-10 - Nettoyage Appareil et catalogue Info compte

Type:
- Android TV
- UI TV
- profil

Resume:
- La section integree `Appareil et catalogue` n'affiche plus les lignes `Identifiant appareil`, `Version` et `Portail`.
- Ajout de la ligne `Profil catalogue` pour montrer le profil actif auquel appartiennent les compteurs catalogue.
- La barre et le message de synchronisation sont rendus dans cette section, afin de rester visibles pendant une synchronisation.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --console=plain` : succes.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`

## 2026-07-10 - Timeout et fallback Series pour synchro Xtream bloquee

Type:
- Android TV
- bugfix
- catalogue
- diagnostic Fire Stick

Resume:
- Diagnostic Fire Stick: Network Activity affichait `Catalog synchronization` et `Catalog Series` en `Running` depuis plus de 63 minutes, bloque sur `Telechargement des series...` avant `after_get_series`.
- Ajout d'un timeout dur sur le `get_series` global Xtream et d'un fallback par categorie `get_series&category_id=...` avec deduplication par `series_id`.
- En cas de timeout aussi sur une categorie, la synchro echoue proprement et les activites Network Activity passent en erreur au lieu de rester indefiniment actives.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --console=plain` : succes.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`

## 2026-07-10 - Profil PlaylistWeb pour envois depuis le site

Type:
- Android TV
- profils playlist
- backend/API
- catalogue

Resume:
- Les `playlist_config` recus depuis `device_status.php` ne creent plus un compte legacy `activation_portal`; ils alimentent un profil dedie `PlaylistWeb`.
- `PlaylistWeb` est mis a jour par nom si l'utilisateur ne l'a pas renomme; s'il a ete renomme, le prochain push web cree un nouveau profil `PlaylistWeb`.
- Le payload playlist chiffre stocke la derniere source envoyee (`xtream` ou `m3u`) pour choisir la source active du profil web.
- Les notifications `Configuration playlist recue` declenchent un `checkStatus()` cote TV, ce qui importe la playlist quand l'app est deja ouverte.
- Apres un envoi playlist valide, `/playlist/` et `save_playlist_config.php` valident la derniere session pending de l'appareil pour permettre au token de polling courant de recevoir la nouvelle playlist sans revenir a la restauration libre des anciennes configs.
- Si les credentials du meme profil changent, AppNavigation force une nouvelle synchro catalogue; les changements de profil vers un catalogue deja local restent cache-first.

Validation:
- `php -l server/public_html/api/helpers.php` : succes.
- `php -l server/public_html/playlist/index.php` : succes.
- `php -l server/public_html/api/save_playlist_config.php` : succes.
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --console=plain` : succes.
- `.\gradlew.bat :app:assembleRelease --no-daemon --console=plain` : succes.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-10 - Sync profils, URLs lecture et restauration playlist

Type:
- Android TV
- profils playlist
- catalogue
- backend/API
- securite

Resume:
- La synchronisation catalogue capture le profil actif, ses credentials et son host Xtream au lancement, puis ecrit Room/cache uniquement pour ce profil.
- Les caches catalogue et Xtream repository sont profiles par `profileId`; un changement de profil invalide les caches sans reutiliser l'ancien catalogue.
- Les URLs preview/player Movies/Series sont reconstruites depuis le catalogue Room du profil actif, avec extension locale, pour eviter de lire avec l'ancien profil.
- `device_status.php` ne renvoie plus `playlist_config` avec un `device_token` de session `pending`; il faut un appareil actif et une session `validated`.
- La suppression d'un profil playlist appelle `api/clear_playlist_config.php`, qui efface la config serveur seulement si elle correspond au profil supprime, afin d'eviter la recreation du profil par defaut au redemarrage.
- `ActivationRepository.registerDevice()` conserve un token local deja present au lieu de l'ecraser par le nouveau token pending du register.

Validation:
- `php -l server/public_html/api/helpers.php` : succes.
- `php -l server/public_html/api/device_status.php` : succes.
- `php -l server/public_html/api/clear_playlist_config.php` : succes.
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --console=plain` : succes.
- `.\gradlew.bat :app:assembleRelease --no-daemon --console=plain` : succes.
- Parse PowerShell `scripts/deploy_activation_phase1.ps1` : succes.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-10 - Settings header, focus profil et preview VOD ALL

Type:
- Android TV
- focus/navigation
- profils playlist
- UI catalogue

Resume:
- Settings reutilise le header principal comme Info compte, donne le focus initial au premier item du menu et Back depuis Settings/Info compte retourne sur Home avec focus sur l'onglet Home du header.
- Info compte supprime le menu gauche `Appareil et catalogue`; son contenu reste en bas de `Info compte` comme section focusable non cliquable pour permettre le scroll.
- Les changements de profil depuis Info compte et Who's watching invalident les caches et synchronisent le catalogue du profil actif sans declencher le faux popup Xtream du quick-check.
- Who's watching remplace la barre lineaire par le cercle de chargement bleu.
- Movies/Series reconstruisent les URLs preview/player depuis Room avec l'extension locale, corrigeant les previews `ALL` chez les fournisseurs non-`mp4`; le mini-player Preview devient focusable entre header et details.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin` : succes.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-10 - Who's watching, multi-profils premium et correctifs catalogues

Type:
- Android TV
- profils playlist
- admin/backend
- UI catalogue

Resume:
- `ProfilePickerScreen` supprime `Manage profiles`; `Add Profile` ouvre le popup profil existant, le stylo du nom ouvre le meme formulaire pre-rempli, et OK sur un profil affiche une progress bar jusqu'a l'ouverture Home.
- Les profils stockent maintenant un `avatar_id` avec 10 avatars standards; le popup creation/modification permet de choisir la photo de profil.
- Info compte > Profils masque le detail par defaut et l'affiche/masque au clic sur une ligne; l'avatar du header ouvre un popup de selection puis focus `Enregistrer`, et le nom se modifie via le stylo.
- Le contenu `Appareil et catalogue` est affiche en bas de `Info compte`; le menu gauche `Appareil et catalogue` reste conserve en attente de validation avant suppression. Le menu `Mode d'utilisation` est supprime.
- Ajout du feature flag admin/API/Android `multi_profile`, Premium + essai 7 jours autorises, Free Ads verrouille avec couronne sur l'ajout.
- Movies/Series evitent le flash d'erreur transitoire pendant les chargements locaux, compactent les lignes VOD a `64dp`, augmentent l'espace texte, et Series affiche `Saisons / episodes` sous le mini-player.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin` : succes.
- `.\scripts\guard_release_version.ps1` : succes apres bump `versionCode 132`.
- `.\gradlew.bat :app:assembleRelease --no-daemon --max-workers=1 --console=plain` : succes.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata` : succes.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall` : succes.
- Production verifiee: manifeste `smartvision-tv-v132-04eaf015.apk`, `api/app_update.php` en `0.1.116` / `132`, APK stable/versionne HTTP 200, SHA256 `04eaf0154a106c91f1d403325480217cdb95eece95bfe94d3accc2f71d3ebe5c`, taille `41116724`, `multi_profile` Premium=true Trial=true FreeAds=false.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-09 - Movies/Series skeleton, free ads preview et focus detail

Type:
- Android TV
- UI catalogue
- monetisation
- focus

Resume:
- Movies et Series affichent maintenant un skeleton 3 colonnes au chargement global et un skeleton de lignes pendant les rechargements de liste.
- Quand aucun contenu n'est selectionne en mode `FREE_WITH_ADS`, le panneau Preview affiche le mini-player pub idle VAST puis une carte Premium avec QR/code TV, comme Live TV.
- Les lignes Films/Series utilisent le backdrop paysage en priorite, suppriment le padding/cadre interne de miniature, reduisent la hauteur a `68dp`, et ajoutent genre/note etoilee/metadonnees/duree.
- Le premier OK sur une ligne lance le preview en gardant le focus sur la ligne; le deuxieme OK sur la meme ligne ouvre le fullscreen.
- Le mini-preview VOD demarre par un warmup avant les segments et retombe sur le backdrop/poster sans afficher `Preview indisponible` quand le fournisseur refuse le seek.
- La section detail sous le mini-player est focusable et scrollable au D-pad avec resume, duree, annee, note, genre, credits et cast quand disponibles.
- Les fiches detail Films/Series scrollent en haut puis focalisent `Regarder`/`Reprendre` sans `bringIntoView` initial, ce qui evite l'ouverture decalee sur les episodes.
- L'overlay fullscreen Films/Series reorganise titre/progressbar/controles/actions, rend la progressbar focusable visuellement, active le toggle Favori reel et affiche precedent/suivant quand un item adjacent existe.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin` : succes.
- `.\scripts\guard_release_version.ps1` : succes apres bump `versionCode 131`.
- `.\gradlew.bat :app:assembleRelease --no-daemon --max-workers=1 --console=plain` : succes.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata` : succes.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall` : succes.
- Production verifiee: manifeste `smartvision-tv-v131-b854eab7.apk`, `api/app_update.php` en `0.1.116` / `131`, APK stable/versionne HTTP 200, SHA256 `b854eab7100b4179f4f08ddb9b1511c99e65d9a065cf27e85d53c439fc299103`, taille `41083958`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-09 - Correctifs focus picker et profils non destructifs

Type:
- Android TV
- profils playlist
- navigation
- documentation

Resume:
- Les toggles ON/OFF des lignes profils sont joignables au D-pad: droite depuis la ligne vers le toggle, gauche depuis le toggle vers la ligne.
- Les anciens flows Xtream/M3U ne recopient plus `_accounts` dans les profils existants; ils servent uniquement a creer le premier profil si aucun profil n'existe.
- `ProfilePickerScreen` active le profil localement et quitte l'ecran sans attendre la synchro reseau; `Add Profile` et `Manage profiles` ouvrent Info compte apres montage du `NavHost`, ce qui evite le crash.
- Le focus initial du picker cible le profil actif.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK.

## 2026-07-09 - Refonte Movies/Series en layout 3 colonnes

Type:
- fonctionnalite
- UI TV
- documentation

Resume:
- Movies et Series adoptent un layout 3 colonnes aligne sur Live TV: categories sans recherche a gauche, liste centrale paginee, panneau Preview a droite.
- La recherche centrale Films/Series interroge Room par pages pour `ALL` et categories normales; Favoris/Historique restent filtres sur leurs listes utilisateur bornees.
- Premier OK sur une ligne lance le mini-preview VOD et donne le focus au bouton `Play`; deuxieme OK sur la meme ligne ouvre le fullscreen.
- Le mini-preview joue les segments 10% / 30% / 50% / 80% avec fade-in audio, puis affiche backdrop TMDB ou poster 16:9.
- Les fiches detail existantes restent accessibles uniquement via `Details`; la suppression Historique passe par le header Preview.

Verification:
- `.\gradlew.bat :app:compileReleaseKotlin` : succes.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/catalog/VodThreePaneComponents.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/domain/repository/CatalogRepository.kt`

## 2026-07-09 - Details profil sur cartes source existantes

Type:
- Android TV
- UI profil
- documentation

Resume:
- Info compte > Profils n'affiche plus la fiche texte ajoutee pour le detail profil.
- OK sur une ligne profil remplit le panneau de details deja existant avec les cartes `Compte SmartVision`, `Lien M3U`, `URL EPG` et `Synchroniser/Supprimer`.
- Les editions M3U/EPG et la selection de source dans ce panneau modifient le profil selectionne; le toggle ON/OFF de la ligne reste l'action d'activation du profil.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK.

## 2026-07-09 - Script release prod complet

Type:
- tooling
- release
- documentation

Resume:
- Ajout de `scripts/release_prod.ps1` pour lancer en une commande le process prod: garde-fou version, build `:app:assembleRelease`, garde-fou metadata, deploy production et verification publique.
- Le wrapper incremente automatiquement `versionCode` avant le build et conserve `deploy_activation_phase1.ps1 -SkipInstall -SkipTests` par defaut, avec options `-RunSqlInstall`, `-RunDeployTests`, `-SkipAdb` et `-SkipPublicVerification`.
- Le suivi visuel PowerShell affiche les etapes, pourcentages, duree ecoulee, headers et resultats OK/echec.
- La verification publique controle `downloads/smartvision-tv.version.json`, `api/app_update.php`, l'APK versionne telecharge/hash SHA256 et l'acces a l'APK stable.

Validation:
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\release_prod.ps1 -SkipPublicVerification`: non lance pour eviter un build/deploiement prod non demande.

## 2026-07-08 - Overlay Live compact et OK Media prives

Type:
- Android TV
- UI player
- documentation

Resume:
- Overlay Live plein ecran aligne sur la specification cible: actions `Favori`, `Luminosite`, `Parametres`, `Exit fullscreen`, sans `Record` ni `Info` dans ce lot.
- Les composants visuels Live sont sortis dans `LivePlayerOverlay.kt`; `FullScreenPlayerScreen.kt` conserve le cycle de vie Media3, les etats et la navigation player.
- Les icones du bandeau Live utilisent les variantes outline spec, avec hit area compacte et espacement reduit.
- Media prives laisse OK/Enter au WebView/provider avant fallback JS, afin que les controles natifs du fournisseur puissent reagir a la telecommande.
- Les embeds prives utilisent `FOCUS_AFTER_DESCENDANTS` et l'overlay fullscreen Compose ne vole plus le focus initial du WebView.

Validation:
- En cours: build release puis installation/test Firestick.

## 2026-07-07 - Media prives player style YouTube

Type:
- Android TV
- documentation

Resume:
- Media prives passe en layout proche YouTube: categories a gauche, liste compacte au centre, grande section principale a droite pour miniature/player.
- Le premier OK sur une video privee lance le player inline; le second OK ouvre le fullscreen existant.
- Le mini-player embed devient focusable/clicable au D-pad pour atteindre Play dans le provider; le fullscreen prive ajoute controls Media3 natifs visibles et overlay bas retour/play/pause/-15s/+15s.
- Le champ recherche Media garde un mode edition explicite pour ouvrir le clavier TV via OK/clic.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK.
- `.\scripts\guard_release_version.ps1`: OK apres bump `0.1.116` / `120`.
- `.\gradlew.bat :app:assembleRelease --no-daemon --max-workers=1 --console=plain`: OK.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK.
- Production verifiee: manifeste `smartvision-tv-v120-e13560d3.apk`, `api/app_update.php` en `0.1.116` / `120`, APK stable/versionne HTTP 200 taille `40969256`.

## 2026-07-07 - Media prives WebView TV et mode natif force test

Type:
- Android TV
- backend PHP
- admin
- documentation

Resume:
- Media prive durcit le fallback WebView mini/fullscreen pour Android TV: cookies tiers, DOM storage, user-agent navigateur, viewport, hardware layer et autoplay sans geste utilisateur.
- Le mini-player prive est reduit en 16:9 compact; le titre video sous preview passe en police plus petite.
- Le champ recherche Media demande explicitement le focus et le clavier logiciel sur OK/clic; les focus des lignes Media utilisent `LocalTvFocusStyle`.
- Admin > Bibliotheque privee ajoute `Forcer lecture native HLS/MP4` et `Flux HLS/MP4 de test`; le backend priorise ce vrai flux direct en mode test, sinon renvoie `UNAVAILABLE` au lieu d'inventer une conversion d'embed.

Validation:
- `php -l server/public_html/api/media/private/private_media_service.php`: OK.
- `php -l server/public_html/admin/index.php`: OK.
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK.
- AVD `SmartVision_TV_720p_Light` demarre en `emulator-5554`; APK modifie non installe car `assembleRelease` a depasse 20 minutes et n'a pas regenere d'artefact.

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/ui/media/MediaScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/media/PrivateMediaDetailScreen.kt`
- `server/public_html/api/media/private/private_media_service.php`
- `server/public_html/admin/index.php`

## 2026-07-07 - Sous-dossiers prives admin et boutons QR Media local

Type:
- Android TV
- backend PHP
- admin
- documentation

Resume:
- `Media local` contient maintenant les actions `Importer tel.` et `Exporter tel.` sous forme de lignes compactes avec icone QR, au meme niveau visuel que les autres sous-elements.
- `Media prives` conserve ses sous-dossiers compactes; les premiers themes par defaut sont crees cote backend et l'ancien dossier unique est migre automatiquement.
- Admin > `Bibliotheque privee` gere les sous-dossiers TV avec nom, recherche/theme, ordre, activation et suppression explicite, tout en conservant les IDs existants.
- Playback prive detecte et renvoie des streams directs HLS/MP4 quand ils existent dans la reponse backend/provider; sinon l'app reste sur embed/page sans scraping HTML.

Validation:
- `php -l server/public_html/api/media/private/private_media_service.php`: OK.
- `php -l server/public_html/admin/index.php`: OK.
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK.
- Release `0.1.114` / `118`: `.\scripts\guard_release_version.ps1`, `.\gradlew.bat :app:assembleRelease --no-daemon --max-workers=1 --console=plain`, puis `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK.
- Production verifiee: manifeste `smartvision-tv-v118-34935253.apk`, `api/app_update.php` en `0.1.114` / `118`, categories privees HTTP 200, `items.php` HTTP 200 avec items, `playback.php` HTTP 200 en fallback `EMBED`, POST admin `private_media_sync_removed` HTTP 200 sans HTTP 500.

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/ui/media/MediaScreen.kt`
- `server/public_html/api/media/private/private_media_service.php`
- `server/public_html/admin/index.php`

## 2026-07-07 - Media prives recherche, playback TV et sync removed

Type:
- android
- ui-tv
- player
- backend
- admin
- documentation

Resume:
- `Media prives` devient expandable avec sous-categories backend visibles sous le parent.
- La colonne liste remplace refresh par un champ de recherche focusable TV; la recherche locale filtre les fichiers et la recherche privee interroge SmartVision avec `query`.
- Premier OK sur une video privee charge le mini-player; second OK ouvre `private_media_player/{id}`.
- Playback prive: ExoPlayer pour HLS/MP4 fournis par SmartVision, WebView embed officiel sinon; pas de scraping ni d'URL provider dans l'APK.
- `Synchroniser removed` admin est borne par lot, transactionnel et fail-safe pour eviter les HTTP 500.
- Version release publiee en production: `0.1.113` / `versionCode 117`.

Validation:
- `php -l server/public_html/api/media/private/private_media_service.php`: OK.
- `php -l server/public_html/api/media/private/items.php`: OK.
- `php -l server/public_html/admin/index.php`: OK.
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK.
- `.\scripts\guard_release_version.ps1`: OK.
- `.\gradlew.bat :app:assembleRelease --no-daemon --max-workers=1 --console=plain`: OK en 18m35s.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.113 (117)`.
- `apksigner verify --verbose`: OK v1/v2, 1 signer.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK.
- Production verifiee: manifeste et `api/app_update.php` annoncent `0.1.113` / `117`, APK `smartvision-tv-v117-0ebeb50b.apk`, SHA256 `0ebeb50b812c7ca5298cfa2268302f35ad032dc1d0fed73d967407d51b0f8538`, taille `40952871`.
- Production verifiee: `private_media_sync_removed` admin retourne HTTP 200; `items.php?query=test` retourne des items avec categorie admin reelle; `playback.php` retourne `EMBED` + `embedUrl` pour l'item teste.

## 2026-07-07 - Media local expandable et bibliotheque privee proxy

Type:
- android
- ui-tv
- backend
- admin
- provider-proxy
- documentation

Resume:
- `Media` garde le layout Live TV 3 colonnes, mais la colonne gauche devient hierarchique: `Media local` expandable avec `All files`, `Recordings`, `Imports`, `Transfers`, plus `Media prives`.
- Ajout d'une couche Android `PrivateMediaRepository` qui appelle uniquement les endpoints SmartVision `api/media/private/*`.
- Ajout d'une fiche `private_media_detail/{id}` avec metadonnees provider normalisees et etat `Lecture indisponible` si aucun flux natif HLS/MP4 n'est fourni.
- Ajout du proxy PHP `api/media/private/*` avec search/id/removed officiels Eporner, cache provider, removed ids, health, rate limit simple et DTO internes.
- Ajout du menu admin `Bibliotheque privee`, des flags `private_media`, `private_media_eporner`, `private_media_native_playback`, et des uploads deploy associes.
- Android ne connait aucun endpoint Eporner et ne fait ni scraping ni extraction HTML.

Validation:
- `php -l` sur les nouveaux endpoints private media, `admin/index.php` et `api/app_config.php`: OK.
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK.
- `.\scripts\guard_release_version.ps1`: OK apres increment `0.1.112` / `versionCode 116`.
- `.\gradlew.bat :app:assembleRelease --no-daemon --max-workers=1 --console=plain`: OK en 14m10s.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.112 (116)`.
- `apksigner verify --verbose`: OK v1/v2, 1 signer.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK apres correction de creation des dossiers `api/media/private/*`.
- Production verifiee: manifeste et `api/app_update.php` annoncent `0.1.112` / `116`, APK `smartvision-tv-v116-23bd43e4.apk`, SHA256 `23bd43e4d227c029be794a5d128fa88ed3d443aa5b769f937cd9ae22c75f3cdb`, taille `40936499`.
- Production verifiee: `api/app_config.php` expose `private_media`, `private_media_eporner`, `private_media_native_playback`; `api/media/private/libraries`, `categories`, `items`, `providers/health` retournent 200 avec provider desactive par defaut.

## 2026-07-07 - Overlays fullscreen Live/Films/Series et focus details

Type:
- android
- ui-tv
- player
- focus
- documentation

Resume:
- Les overlays fullscreen Live/Film/Serie sont harmonises sur le meme style visuel: fond sombre/gradient, icones outline sans labels, focus discret bleu/blanc et luminosite integree.
- Le player plein ecran Films/Series remplace l'ancien bandeau VOD par un overlay fullscreen bas: titres a gauche, utilitaires icon-only a droite, progression lisible et controles centraux.
- Les films masquent toutes les actions propres aux series; les episodes affichent `Autres episodes`, precedent/suivant episode, panneau lateral saisons/episodes et card auto `Episode suivant`.
- Le player fullscreen Live remplace `EPG / Settings / Record / Back to List` par `Favori / Record / Luminosite / Settings / Exit fullscreen`, sans supprimer la logique EPG hors bouton overlay.
- La card `Episode suivant` se declenche a 10 secondes ou moins de la fin, avec action `Voir episode` et annulation par lecture courante.
- Le mode luminosite remplace temporairement les icones utilitaires et applique un voile noir sur la video uniquement.
- Les fiches detail Films/Series ajoutent un bring-into-view sur les actions, saisons, episodes et etoiles de note pour garder le focus visible apres scroll.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK.

## 2026-07-07 - Release TMDB 0.1.110 lots 7 et 8 details riches

Type:
- android
- tmdb
- room
- ui-tv
- home
- release
- deploy
- firestick
- documentation

Resume:
- Version Gradle incrementee en `0.1.110` / `versionCode 114`.
- Room passe au schema `12` pour stocker cast/realisateur/createurs, videos et recommandations TMDB, avec TTL cache 7 jours, fallback stale et nettoyage 90 jours.
- Les ecrans details films et series deviennent scrollables et ajoutent les sections TMDB riches: trailers/teasers YouTube en mini-player, casting/realisateur/createurs avec photos, note TMDB + note utilisateur locale et recommandations.
- Home Tendances utilise les posters/backdrops TMDB et le trailer YouTube TMDB comme preview quand disponible; les anciens caches preview Xtream `movie`/`episode` sont neutralises et ignores.
- Home affiche des skeleton cards non focusables pour Continue watching et les tendances pendant les chargements initiaux.
- Release publiee en production avec manifeste `smartvision-tv-v114-f84c329b.apk`.
- Documentation TMDB, catalogue, Home/UI, release et roadmap tendances mise a jour.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --console=plain`: OK en 6m40s, puis OK apres correction cache/migration en 2m13s.
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.110 (114)` > prod `0.1.109 (113)`.
- `.\gradlew.bat :app:assembleRelease --no-daemon --console=plain`: OK en 17m01s.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.110 (114)`.
- `apksigner verify --verbose`: OK v1/v2, 1 signer.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK, tests publics/site/admin du script OK.
- Production verifiee: manifeste, `api/app_update.php`, APK stable et APK versionne annoncent `0.1.110` / `114`, taille `40854578`, SHA256 `f84c329bf7fbb5c11215713261a562827b1560bb4b076eb85a6d65b36ef7fdee`.
- Firestick `192.168.1.33:5555`: installation OK, `versionCode=114`, `versionName=0.1.110`, cold start OK, MainActivity/Home rendu, aucun crash/ANR/Room error dans les logs filtres.
- Capture Home: `build/verification/svplayer-home-v114.png`. Navigation Movies capturee; ouverture fiche detail via ADB non confirmee pendant cette verification manuelle.

## 2026-07-07 - Release TMDB 0.1.109 en production

Type:
- android
- tmdb
- release
- deploy
- firestick
- documentation

Resume:
- Version Gradle incrementee en `0.1.109` / `versionCode 113` apres publication intermediaire `0.1.108` sans jeton TMDB configure.
- Le jeton TMDB est maintenant lu depuis `local.properties` local non versionne; `local.properties.example` reste un placeholder vide.
- Release TMDB lots 1 a 6 publiee en production avec manifeste `smartvision-tv-v113-6b467713.apk`.
- Firestick `192.168.1.33:5555` installee/lancee en `0.1.109`; Home rendu, Settings TMDB affiche `Active`, et Network Activity montre un appel TMDB HTTP 200.

Validation:
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.109 (113)` > prod `0.1.108 (112)`.
- `.\gradlew.bat :app:assembleRelease --no-daemon --console=plain`: OK en 13m32s.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.109 (113)`.
- `apksigner verify --verbose`: OK v1/v2, 1 signer.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK, tests publics/site/admin du script OK.
- Production verifiee: manifeste, `api/app_update.php`, APK versionne et APK stable hashent `6b467713c176c876eee2517b36a9592238d54d3c72e19bb57cd9572e879cbab3`, taille `40821819`, `app_config.php`, `ads-config` et VAST en 200.
- Firestick: installation OK, `versionCode=113`, cold start OK, MainActivity focus, aucun crash/ANR/Room error dans les logs filtres.

## 2026-07-07 - TMDB lots 5 et 6 series, Home et catalogues cache-only

Type:
- android
- tmdb
- ui-tv
- home
- documentation

Resume:
- `SeriesDetailScreen` affiche maintenant les metadonnees TMDB disponibles en priorite: titre, poster, backdrop, synopsis, genres, date, note, duree episode, casting, createurs, certification et providers.
- Les saisons, episodes, ids et URLs de lecture Series restent Xtream.
- `HomeContentRepository.prepareTrendingPreview()` enrichit les cards Tendances avec TMDB pendant la preparation preview deja bornee aux items visibles/proches, sans appel TMDB au splash.
- `MoviesViewModel` et `SeriesViewModel` reutilisent uniquement le cache TMDB Room local sur les premiers items charges; aucun matching/reseau massif n'est lance depuis les grilles.
- Documentation TMDB, catalogue, UI et changelog mis a jour.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --console=plain`: OK.

## 2026-07-07 - TMDB lots 1 a 4 cache Room, matching et fiche film

Type:
- android
- tmdb
- room
- ui-tv
- documentation

Resume:
- Ajout du plan A-Z `docs/ai-knowledge/features/tmdb-integration-plan.md` et d'une decision TMDB local Room/non bloquant.
- Passage Room en version 11 avec tables `tmdb_content_mapping`, `tmdb_movie_metadata`, `tmdb_series_metadata` et schema exporte `11.json`.
- Ajout du client Retrofit TMDB optionnel, du matching automatique films/series, du cache local et du type Network Activity `Tmdb`.
- `MovieDetailScreen` enrichit les champs visuels film depuis TMDB quand disponibles, avec fallback Xtream et boutons lecture/favori inchanges.
- `SeriesDetailScreen` lance l'association/cache TMDB sans refonte visuelle serie.
- Settings ajoute le menu `Attribution TMDB` avec statut token non sensible, attribution TMDB, providers JustWatch et note licence.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --console=plain`: OK. Avertissements existants sur icones Compose depreciees et `Notification.Builder`, non lies a TMDB.

## 2026-07-07 - Live TV focus, EPG/Info et Premium minimal luxury

Type:
- android
- live-tv
- ui-tv
- focus
- release
- deploy
- documentation

Resume:
- La restauration de focus Categories attend maintenant que la categorie selectionnee soit visible avant `requestFocus()`, afin de synchroniser selection, focus et scroll a l'ouverture Live TV.
- Le loader interne de la colonne Chaines est remplace par un skeleton de lignes numerotees/logo/texte, sans changer le skeleton global Live TV.
- Les logos chaines reels restent dans un conteneur fixe avec clipping et leger zoom `Fit`, sans deformation ni changement de hauteur de ligne.
- L'icone EPG quitte le header Apercu et passe a droite du titre `Programme de la chaine`; D-pad haut depuis la premiere ligne EPG cible `Regarder` dans le header Apercu.
- La section `Info chaine` reprend la structure titre/separateur/lignes de l'EPG, sans icones de lignes, sans logo dans la ligne Channel et sans ligne `EPG indisponible`.
- Le bloc Premium sous mini-player adopte un style minimal luxury dark navy/or avec couronne dessinee, QR et code TV dynamiques conserves, sans bouton, prix ni CTA.
- Version finale publiee en `0.1.105` / `versionCode 109`.

Validation:
- `.\gradlew.bat :app:assembleRelease --no-daemon --console=plain`: OK avant bump UI, OK en `0.1.104 (108)`, puis OK final en `0.1.105 (109)`.
- `.\scripts\guard_release_version.ps1`: OK avant build final, local `0.1.105 (109)` > prod `0.1.104 (108)`, warning metadata ancien attendu.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK apres build final, metadata `0.1.105 (109)`.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK, tests publics/site/admin du script OK.
- Production verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.105` / `109`; APK versionne `smartvision-tv-v109-d480fc66.apk` et APK stable repondent en `200`, taille `40756264`, SHA256 `d480fc66030d83a22236ef0b4dcb4c0a304ebbb861ba34e78808c0f492044d9a`.
- `git diff --check`: OK, uniquement avertissements CRLF Windows.
- `adb` absent du PATH pendant le guard: version installee TV non verifiee.

## 2026-07-06 - Raccourcis telecommande Settings/Menu globaux

Type:
- android
- ui-tv
- remote-control
- settings
- release-local
- documentation

Resume:
- `MainActivity.dispatchKeyEvent()` intercepte uniquement `KEYCODE_SETTINGS`, `KEYCODE_MENU` et `KEYCODE_MEDIA_TOP_MENU`.
- `ACTION_DOWN` est consomme sans action; `ACTION_UP` emet une demande d'ouverture Settings.
- `AppNavigation` collecte cette demande via `RemoteSettingsNavigation` et ouvre `settings` avec `navigateSingleTop()` pour eviter l'empilement.
- Les touches D-pad, OK, Back, Play/Pause/Rewind/FastForward et toutes les autres touches restent transmises a `super.dispatchKeyEvent(event)`.
- Version locale preparee en `0.1.103` / `versionCode 107` pour build release local sans mise en prod.

Validation:
- `git diff --check`: OK, uniquement avertissements CRLF Windows.
- `.\scripts\guard_release_version.ps1`: OK avant build, local `0.1.103 (107)` > prod `0.1.102 (106)`, warning metadata ancien attendu.
- `.\gradlew.bat :app:assembleRelease`: OK.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK apres build, metadata `0.1.103 (107)`, prod toujours `0.1.102 (106)`.
- APK local: `app/build/outputs/apk/release/app-release.apk`, taille `40759014`, SHA256 `B05B9994123874475B9EA0F25D52878425907FE388664E9E5C754567983F20E8`.
- Aucun deploiement prod effectue.

## 2026-07-06 - Recherche Live TV Room et focus liste vide

Type:
- android
- live-tv
- room
- focus
- documentation

Resume:
- La recherche des chaines Live TV est deplacee dans `LiveTvViewModel` et interroge Room par pages pour `ALL` et les categories normales.
- Le filtre local de `LiveTvScreen` sur les seules chaines deja chargees est supprime; la pagination des resultats de recherche reste active.
- `Favoris` et `Historique` conservent un filtre local sur leurs listes deja chargees.
- D-pad droite depuis les categories cible le champ recherche quand la liste chaines est vide.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin`: OK.

## 2026-07-06 - Live TV focus deterministe et categorie initiale corrigee

Type:
- android
- live-tv
- ui-tv
- focus
- epg
- release
- deploy
- documentation

Resume:
- Correction de la selection automatique Live TV pour recalculer le premier dossier reel apres `Historique` dans l'ordre visuel final, meme apres snapshot partiel et tri `sortedByHistorySignals(...)`.
- Les annulations normales de chargement chaines ne sont plus transformees en erreur visible `standaloneCoroutine was cancelled`.
- Les transitions D-pad critiques restaurent le focus apres `scrollToItem(...)` et verification `visibleItemsInfo`.
- Le champ recherche devient l'etape intermediaire entre liste chaines et header.
- L'icone EPG utilise l'image fournie `epg a mettre.png` telle quelle, sans cadre/fond, dans les lignes et le header Apercu.
- Les tailles texte chaines/sous-titres/titres EPG sont reduites selon la demande; le panneau sans EPG est integre au style des lignes chaines.

Validation:
- `git diff --check`: OK, uniquement avertissements CRLF Windows.
- `.\scripts\guard_release_version.ps1`: OK avant build, local `0.1.102 (106)` > prod `0.1.101 (105)`, warning metadata ancien attendu.
- `.\gradlew.bat :app:assembleRelease`: OK.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK avant deploy, metadata `0.1.102 (106)`.
- `.\scripts\deploy_activation_phase1.ps1`: OK.
- Production verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.102` / `106`; APK versionne `smartvision-tv-v106-73da2e18.apk` et APK stable repondent en range `206`, taille `40759019`, SHA256 `73da2e187ea9d5043599608c8268a17d3c0b1af711bfb79cb43474d3bb1edd12`.
- `adb` absent du PATH pendant le guard: version installee TV non verifiee.

## 2026-07-06 - Live TV premium focus header et panneau sans EPG

Type:
- android
- live-tv
- ui-tv
- focus
- epg
- release
- deploy
- documentation

Resume:
- Live TV garde un skeleton 3 panneaux au moins 1 seconde avant d'afficher les donnees reelles.
- La selection initiale ouvre le premier dossier apres `Historique`, avec fallback premiere categorie.
- Le header principal accepte un focus requester d'onglet courant et une cible D-pad bas vers le contenu; Live TV route header/contenu via `Live TV` et la categorie selectionnee.
- Les headers Live TV recoivent un padding gauche coherent, les categories sont plus compactes, les noms chaines sont agrandis et les titres EPG sous mini-player sont plus lisibles.
- L'icone EPG est regeneree en PNG transparent avec halo neon et affichee plus petite dans les lignes; une icone EPG informative non focusable apparait dans le header Apercu quand la chaine a de l'EPG.
- Le mini-player Apercu renforce son overlay glassmorphism et conserve le fallback categorie quand aucun programme courant n'existe.
- Sans EPG, la zone sous mini-player affiche `A propos de la chaine` avec chaine, numero visible, categorie, source, EPG et pays detecte simplement.

Validation:
- `git diff --check`: OK, uniquement avertissements CRLF Windows.
- `.\scripts\guard_release_version.ps1`: OK avant build, local `0.1.101 (105)` > prod `0.1.100 (104)`, warning metadata ancien attendu.
- `.\gradlew.bat :app:assembleRelease`: OK.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK avant deploy, metadata `0.1.101 (105)`.
- `.\scripts\deploy_activation_phase1.ps1`: OK.
- Production verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.101` / `105`; APK versionne `smartvision-tv-v105-34681073.apk` et APK stable repondent en range `206`, taille `40399158`, SHA256 `34681073bcb47ad115d1f61afebf2af678ff359ed2d3bcf5fe7b52645496db7b`.
- Le guard `-RequireBuildMetadata` relance apres deploy echoue logiquement car la prod est maintenant egale au local `105`; c'est attendu pour une verification post-publication.
- `adb` absent du PATH pendant le guard: version installee TV non verifiee.

## 2026-07-05 - Release prod 0.1.100 (104) Live TV UI focus EPG

Type:
- android
- live-tv
- ui-tv
- epg
- release
- deploy
- documentation

Resume:
- Live TV conserve le layout 3 zones `Categories / Chaines / Apercu`, avec panneaux et lignes plus compacts.
- Ajout des libelles Live TV EN/FR et d'un skeleton shimmer 3 panneaux au chargement.
- Suppression du bouton `EPG` du header categories et du compteur/icone filtre du header chaines; recherche alignee avec le titre.
- Numerotation relative au dossier visible, ancrage focus chaines autour de la 3e ligne et retour D-pad gauche vers le dossier selectionne.
- Logos chaines sans fond/cadre quand un logo existe, badge EPG remplace par `ic_epg_badge.png`.
- Actions `Regarder`, `Favori` et `Supprimer` de l'Historique deplacees dans le header Apercu; suppression inline retiree.
- Mini-player Apercu compact avec overlay bas et lignes EPG focusables ouvrables en rideau.
- EPG stale-aware via `EpgRepository.synchronizeIfStale`, Worker horaire reseau et refresh leger au clic chaine.
- Aucun filtre admin/API `###` ni bloc `live_tv` ajoute.

Validation:
- `.\scripts\guard_release_version.ps1`: OK apres bump, local `0.1.100 (104)` > prod precedente `0.1.99 (103)`.
- Premier `.\gradlew.bat :app:assembleRelease`: echec Kotlin sur import `key` retire; correction appliquee.
- Deuxieme `.\gradlew.bat :app:assembleRelease`: OK en 10m50.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK avant deploy, metadata `0.1.100 (104)`.
- `.\scripts\deploy_activation_phase1.ps1`: OK.
- Production verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.100` / `104`; APK versionne `smartvision-tv-v104-b7a7822d.apk` et APK stable repondent en range `206`, taille `40351582`, SHA256 `b7a7822d4fab7dfa29e0b22468383cb23723b8c28be617be83cb932ad886f5df`.
- `adb` absent du PATH pendant le guard: version installee TV non verifiee.

## 2026-07-05 - Media compact, mini-player local et anti-flash couronne

Type:
- android
- media-center
- ui-tv
- navigation
- documentation

Resume:
- Media aligne ses proportions sur Live TV (`0.24 / 0.42 / 0.34`).
- Suppression du hero `Premium media studio`, du sous-titre `Library snapshot` et des cards stats colorees du panneau central.
- Le hub `Telephone -> TV` devient une tuile focusable unique visible, sans bouton QR cache hors ecran.
- Le panneau gauche expose aussi `TV -> Phone` avec le meme QR de transfert que l'export du fichier selectionne.
- Le panneau apercu lance un mini-player local ExoPlayer quand le fichier selectionne est une video, avec cadre noir et remplissage type mini-player Live TV.
- Les details fichier sont simplifies: taille, date de mise a jour et type uniquement.
- `AppNavigation` applique le meme comportement que YouTube pendant le chargement app-config: pas de couronne Media provisoire avant la config reelle.

Validation:
- `.\gradlew.bat --no-daemon :app:compileReleaseKotlin`: OK.
- `.\gradlew.bat --no-daemon :app:assembleRelease`: OK.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, local/built `0.1.99` / `versionCode 103`, remote precedent `0.1.98` / `102`.
- `apksigner verify --verbose --print-certs`: OK, v1=true, v2=true, certificat `CN=SmartVision, OU=Android TV, O=SmartVision, C=FR`.
- Firestick `192.168.1.33:5555`: APK release installe, route Media lancee, pas de crash fatal, mini-player video rendu dans `diagnostics/media-screen-firestick/sv_media_miniplayer_live_style.png`.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK.
- Verification publique OK: manifeste `smartvision-tv.version.json`, `api/app_update.php`, APK versionne `smartvision-tv-v103-b1f73b9d.apk` et APK stable en `200`, SHA256 `b1f73b9df54227edc898c640f84aef49abbcbbab66c607341a32feb3767ee4a6`, taille `40314076`.
## 2026-07-05 - Release prod 0.1.96 (100) refonte premium Media

Type:
- android
- media-center
- ui-tv
- release
- deploy
- documentation

Resume:
- Incrementation Android en `0.1.96` / `versionCode 100`.
- Refonte premium de l'ecran Media: hero `Studio media premium`, compteurs de zones, carte stockage, hub transfert telephone, stats bibliotheque, lignes fichiers enrichies et preview hero.
- Conservation des comportements existants: lecture locale, renommer, deplacer, supprimer, import/export telephone, focus DPAD et gates Premium/admin.
- Aucun changement volontaire des parametres admin.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK avant release.
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.96 (100)` > prod `0.1.95 (99)`.
- `.\gradlew.bat --% :app:assembleRelease --no-daemon --max-workers=1 --console=plain`: OK en 12m54.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.96 (100)`.
- `apksigner verify --verbose --print-certs`: OK, v1=true, v2=true, certificat `CN=SmartVision, OU=Android TV, O=SmartVision, C=FR`.
- APK local `app/build/outputs/apk/release/app-release.apk`: taille `40330472`, SHA256 `bd7812fbabd30519b6810c9b80364097a582b0a43818edf9c29574a4b38a5c8f`.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK.
- Production verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.96` / `100`; APK versionne `smartvision-tv-v100-bd7812fb.apk` et APK stable ont taille `40330472` et SHA256 `bd7812fbabd30519b6810c9b80364097a582b0a43818edf9c29574a4b38a5c8f`.
- Parametres admin: non modifies volontairement pendant cette intervention.

## 2026-07-05 - UX import telephone Media

Type:
- android
- media-center
- ui-tv
- documentation

Resume:
- Remplacement du bouton lateral `Importer tel.` par un panneau `Telephone -> TV` dans Media Center.
- Le panneau explique le fonctionnement QR/Wi-Fi quand l'acces est autorise.
- En cas de verrou `media_phone_transfer`, le bouton reste comprehensible (`Debloquer`) et affiche la raison Premium/essai au lieu d'apparaitre comme un bouton gris muet.
- Ajout des libelles EN/FR correspondants.

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK.

## 2026-07-05 - Release prod 0.1.95 (99) accessibilite actions Media

Type:
- android
- release
- deploy
- media-center
- ui-tv
- documentation

Resume:
- Incrementation Android en `0.1.95` / `versionCode 99`.
- Media Center: `Supprimer` est remonte dans la premiere rangee d'actions avec `Lire` et `Renommer`.
- Les dialogs Renommer/Deplacer/Supprimer ciblent le fichier par id explicite, au lieu de dependre de la selection courante.
- Le dialog de suppression prend le focus TV sur la confirmation.
- Correction post-deploy de `app_feature_access`: `recorder`, `media_center`, `media_file_management` et `media_phone_transfer` restent `premium=true`, `trial=true`, `free_ads=false`.

Validation:
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.95 (99)` > prod `0.1.94 (98)`.
- `.\gradlew.bat --% :app:assembleRelease --no-daemon --max-workers=1 --console=plain`: OK en 12m42.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.95 (99)`.
- `apksigner verify --verbose --print-certs`: OK, v1=true, v2=true, certificat `CN=SmartVision, OU=Android TV, O=SmartVision, C=FR`.
- Production verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.95` / `99`; APK versionne `smartvision-tv-v99-a8450f83.apk` et APK stable ont taille `40314082` et SHA256 `a8450f83e6e06c8b4f912094acb2a8a63a7020405a994244c336bec8d4f18536`.
- `api/app_config.php`: flags Recorder/Media/gestion fichiers/transfert `premium=true`, `trial=true`, `free_ads=false`.

## 2026-07-05 - Release prod 0.1.94 (98) stabilisation Recorder

Type:
- android
- release
- deploy
- recorder
- documentation

Resume:
- Incrementation Android en `0.1.94` / `versionCode 98`.
- Build release signe incluant la stabilisation Recorder: quitter la chaine/player ne doit pas stopper l'enregistrement, reconnexion progressive prolongee, activite `Recorder` dans Network Activity sans URL Xtream.
- Deploiement prod via `deploy_activation_phase1.ps1 -SkipInstall`; premier passage interrompu par reset cPanel sur upload image, deuxieme passage OK.
- Correction post-deploy de `app_feature_access`: `recorder`, `media_center`, `media_file_management` et `media_phone_transfer` restent `premium=true`, `trial=true`, `free_ads=false`.

Validation:
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.94 (98)` > prod `0.1.93 (97)`.
- `.\gradlew.bat --% :app:assembleRelease --no-daemon --max-workers=1 --console=plain`: OK en 12m20.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.94 (98)`.
- `apksigner verify --verbose --print-certs`: OK, v1=true, v2=true, certificat `CN=SmartVision, OU=Android TV, O=SmartVision, C=FR`.
- Production verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.94` / `98`; APK versionne `smartvision-tv-v98-f861e960.apk` et APK stable ont taille `40314086` et SHA256 `f861e960078050444cf323de1719b0ce1594c92e3f9101b391e6de2d2568b936`.
- `api/app_config.php`: flags Recorder/Media/gestion fichiers/transfert `premium=true`, `trial=true`, `free_ads=false`.
- `api/app/ads-config`: OK, provider `HILLTOPADS_VAST`; `api/app/ads-vast.php`: HTTP 200.

## 2026-07-05 - Lot 14 stabilisation Recorder + Media

Type:
- android
- recorder
- media-center
- ui-tv
- documentation

Resume:
- Correction Recorder: quitter la chaine/player ne doit plus arreter l'enregistrement Live; le service utilise `START_REDELIVER_INTENT`.
- `RecordingEngine` reconnecte les flux progressifs si la socket se ferme avant la duree demandee, sauf Stop explicite, echecs initiaux sans donnees ou grace de reconnexion expiree.
- Ajout d'une activite `Recorder` dans Settings > Network Activity: titre sanitise, section Live TV, progression, octets, debit et statut final sans URL Xtream.
- Media Center: correction focus DPAD droite liste -> apercu vers le premier bouton actif, message visible pendant preparation import/export telephone, fermeture de session QR apres upload reussi.
- Ajout i18n EN/FR pour l'etat transfert et les libelles de type/source Media.

Validation:
- `.\gradlew.bat --% :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK en 3m15 apres ajout Network Activity Recorder, warnings deprecation Android/Room kapt uniquement.

## 2026-07-05 - Release prod 0.1.93 (97) pour Lots 12 et 13 Media Transfer

Type:
- android
- release
- deploy
- media-center
- documentation

Resume:
- Incrementation Android en `0.1.93` / `versionCode 97`.
- Build release signe incluant les lots 12 et 13: import telephone vers TV et export TV vers telephone par QR local.
- Deploiement prod via `deploy_activation_phase1.ps1 -SkipInstall`.
- Correction de la configuration prod `app_feature_access`: `recorder`, `media_center`, `media_file_management` et `media_phone_transfer` restent Premium/Trial et sont bloques en Free Ads.

Validation:
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.93 (97)` > prod `0.1.92 (96)`.
- `.\gradlew.bat --% :app:assembleRelease --no-daemon --max-workers=1 --console=plain`: OK en 14m15.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.93 (97)`.
- `apksigner verify --verbose --print-certs`: OK, v1=true, v2=true, certificat `CN=SmartVision, OU=Android TV, O=SmartVision, C=FR`.
- Production verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.93` / `97`; APK versionne `smartvision-tv-v97-68adc147.apk` et APK stable ont taille `40314069` et SHA256 `68adc147ebd988359f7402e6b77dc9f4fa7917c92d4f897955ee858bf21c3a9b`.
- `api/app_config.php`: flags Recorder/Media/gestion fichiers/transfert `premium=true`, `trial=true`, `free_ads=false`.

## 2026-07-05 - Lots 12 et 13 transfert telephone/TV Media Center

Type:
- android
- media-center
- ui-tv
- documentation

Resume:
- Ajout de `MediaTransferServer`, serveur HTTP local temporaire pour sessions QR Media.
- Import telephone vers TV: QR upload, page mobile, envoi `PUT`, ecriture streaming dans `SmartVisionMedia/Transfers` avec fichier `.part`, puis indexation Media.
- Export TV vers telephone: QR download du fichier Media selectionne, sans exposer de listing global ni chemin arbitraire.
- Branchement des boutons `Importer tel.` et `Exporter tel.` dans `MediaScreen`, controles par `PremiumFeature.MEDIA_PHONE_TRANSFER`.
- Ajout des strings EN/FR et documentation Recorder/Media, catalog/playback, focus/navigation.

Validation:
- `.\gradlew.bat --% :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK en 2m31 apres correction suspend upload/export, warning Room kapt uniquement.

## 2026-07-05 - Lots 9, 10 et 11 Recorder Live MVP reel

Type:
- android
- recorder
- media-center
- documentation

Resume:
- Ajout du package `recorder`: `RecorderController`, `RecordingService`, `RecordingEngine`, `RecordingRepository` et modeles Recorder.
- Ajout du foreground service `dataSync` avec notification Recorder et action `Stop`.
- Persistance des jobs dans `recording_jobs` avec statuts `queued`, `running`, `completed`, `failed`, `cancelled`.
- Branchement du bouton `Record` Live: durees 30/60/120 min et option EPG jusqu'a fin programme demarrent un vrai enregistrement.
- Ecriture dans `SmartVisionMedia/Recordings` via fichier temporaire `.part`, finalisation atomique puis indexation Media Center.
- Support MVP des flux progressifs et HLS simple non chiffre; echec propre pour HLS chiffre/non supporte.

Validation:
- `.\gradlew.bat --% :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK en 5m26, warnings deprecation uniquement.

## 2026-07-05 - Release prod 0.1.92 (96) pour Lots 7 et 8 Recorder + Media

Type:
- android
- release
- deploy
- documentation

Resume:
- Incrementation Android en `0.1.92` / `versionCode 96` pour depasser la prod deja en `0.1.91 (95)`.
- Build release signe de l'etat courant incluant les lots 7 et 8 Recorder + Media et les retouches header/focus/Media demandees.
- Deploiement prod via `deploy_activation_phase1.ps1 -SkipInstall` avec validations publiques et admin OK.

Validation:
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.92 (96)` > prod `0.1.91 (95)`.
- `.\gradlew.bat --% :app:assembleRelease --no-daemon --max-workers=1 --console=plain`: OK en 17m32.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.92 (96)`.
- `apksigner verify --verbose --print-certs`: OK, v1=true, v2=true, certificat `CN=SmartVision, OU=Android TV, O=SmartVision, C=FR`.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK, tests site/activation/admin OK.
- Production verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.92` / `96`; APK versionne `smartvision-tv-v96-c91c7b49.apk` et APK stable `smartvision-tv.apk?v=96` repondent en HTTP 200 avec taille `40264839` et SHA256 `c91c7b49f87e9a6f41d83c40ed58c3c9ca94e1bdb0bb974c4600875df77655e8`.

## 2026-07-05 - Lots 7 et 8 Media lecture locale + Recorder UI Live

Type:
- android
- ui-tv
- media-center
- documentation

Resume:
- Ajout de la route `media_player/{mediaFileId}` et du bouton `Lire` actif pour les fichiers Media video/audio/photo.
- Reutilisation du player fullscreen pour video/audio locaux via `UserContentType.LocalMedia`, sans preroll pub ni verification Xtream.
- Ajout d'un viewer photo local plein ecran.
- Branchement du bouton `Record` Live au gate `PremiumFeature.RECORDER` avec lock/couronne et popup MVP EPG/duree sans lancement de service DVR reel.
- Amelioration du focus et du design Media: DPAD droite liste -> actions apercu, dossiers focusables, panneau apercu compact, boutons actions en grille.
- Compactage des headers Home/Catalogue/Details, libelle YouTube reduit a `YT`, et couronne locked positionnee au-dessus du label.

Validation:
- `.\gradlew.bat :app:compileDebugKotlin`: OK en 2m10 apres correction d'un import `padding` manquant.

Fichiers principaux:
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/media/MediaScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/TvHeader.kt`
- `app/src/main/java/com/smartvision/svplayer/media/MediaRepository.kt`
- `docs/RECORDER_MEDIA_PLAN.md`

## 2026-07-05 - Lots 5 et 6 Media Center stockage local + gestion fichiers

Type:
- android
- ui-tv
- room
- documentation
- release

Resume:
- Ajout du schema Room Media Center en version 10: `media_folders`, `media_files`, `recording_jobs`.
- Ajout du stockage app-specific `SmartVisionMedia` avec dossiers `Recordings`, `Imports`, `Transfers`.
- Ajout de `MediaRepository` / `MediaStorageManager` et raccord `AppContainer.mediaRepository`.
- Remplacement du MVP visuel par un ecran Media connecte aux vrais fichiers/dossiers locaux.
- Actions branchees: Actualiser, Renommer, Deplacer, Supprimer.
- Version Android preparee pour release: `0.1.91` / `versionCode 95`.

Fichiers code/version:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/SVDatabase.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaCenterDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/entity/MediaCenterEntities.kt`
- `app/src/main/java/com/smartvision/svplayer/media/MediaCenterModels.kt`
- `app/src/main/java/com/smartvision/svplayer/media/MediaRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/media/MediaStorageManager.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/media/MediaScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/media/MediaViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`

Fichiers MD mis a jour:
- `docs/RECORDER_MEDIA_PLAN.md`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Validation:
- `git diff --check`: OK, avertissements CRLF uniquement.
- `php -l server/public_html/admin/index.php`: OK.
- `php -l server/public_html/api/app_config.php`: OK.
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.91 (95)` > prod `0.1.90 (94)`.
- `.\gradlew.bat assembleRelease`: OK en 12m29, APK `app/build/outputs/apk/release/app-release.apk`.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.91 (95)`.
- `apksigner verify --verbose --print-certs`: OK, v1=true, v2=true, certificat `CN=SmartVision, OU=Android TV, O=SmartVision, C=FR`.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK, tests site/activation/admin OK.
- Production verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.91` / `95`; APK versionne `smartvision-tv-v95-f66182d2.apk` et APK stable `smartvision-tv.apk` repondent avec taille `40232068` et SHA256 `f66182d26909fac5a5011357992fdb9963878aa6ec3fcd354d0866180469a6da`.

## 2026-07-05 - Lots 3 et 4 Media Center route/header + MVP visuel

Type:
- android
- ui-tv
- admin
- documentation

Resume:
- Ajout de la route `media` et de l'onglet `Media` dans les headers.
- Branchement du menu Media sur `PremiumFeatureGate` et le flag admin/API `media_center`.
- Affichage verrouille avec couronne et popup Premium pour les statuts sans acces.
- Alignement du header Detail avec les badges locked/warning deja presents sur Home/Catalogue.
- Ajout d'un ecran `MediaScreen` MVP visuel avec bibliotheque, zone vide, apercu et actions fichiers desactivees.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/detail/DetailCommon.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/media/MediaScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`
- `app/src/main/java/com/smartvision/svplayer/data/appconfig/AppConfigRepository.kt`
- `server/public_html/admin/index.php`
- `server/public_html/api/app_config.php`

Fichiers MD mis a jour:
- `docs/RECORDER_MEDIA_PLAN.md`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Validation:
- `git diff --check`: OK, avertissements CRLF uniquement.
- `php -l server/public_html/admin/index.php`: OK.
- `php -l server/public_html/api/app_config.php`: OK.
- `.\gradlew.bat :app:compileDebugKotlin`: OK en 1m49, avertissements deprecation preexistants.

## 2026-07-05 - Lot 2 PremiumFeatureGate Recorder + Media Center

Type:
- android
- backend
- admin
- documentation

Resume:
- Ajout d'un gate central `PremiumFeatureGate` pour les futures surfaces Recorder et Media Center.
- Ajout des defaults Android/admin/API pour `recorder`, `media_center`, `media_file_management`, `media_phone_transfer`.
- Ajout des strings i18n necessaires aux prochains branchements UI Media/Recorder.
- Documentation du perimetre Lot 2 et de la decision structurante.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/domain/access/PremiumFeatureGate.kt`
- `app/src/main/java/com/smartvision/svplayer/data/appconfig/AppConfigRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`
- `server/public_html/admin/index.php`
- `server/public_html/api/app_config.php`

Fichiers MD mis a jour:
- `docs/RECORDER_MEDIA_PLAN.md`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/decisions/2026-07-05-premium-feature-gate-recorder-media.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Validation:
- `git diff --check`: OK, avertissements CRLF uniquement.
- `php -l server/public_html/admin/index.php`: OK.
- `php -l server/public_html/api/app_config.php`: OK.
- `.\gradlew.bat :app:compileDebugKotlin`: OK, avertissements deprecation preexistants.

## 2026-07-05 - Lot 1 Recorder + Media Center

Type:
- documentation
- analyse technique
- roadmap

Resume:
- Analyse de la demande Recorder + Media Center avant code.
- Creation du document de suivi `docs/RECORDER_MEDIA_PLAN.md`.
- Indexation du domaine Recorder/Media Center dans le routeur `docs/ai-knowledge/ROOT.md`.
- Aucun changement Android/PHP/Gradle.

Fichiers MD mis a jour:
- `docs/RECORDER_MEDIA_PLAN.md`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code:
- aucun

## 2026-07-05 - Retouches overlay Live TV et release 0.1.90

Type:
- android
- ui-tv
- playback
- release
- documentation

Resume:
- Haut/Bas ne zappe plus quand un panneau Live EPG/Settings est ouvert; le panneau garde la priorite de navigation.
- Le numero haut droite n'affiche plus l'id technique temporaire avant enrichissement Room, ce qui supprime le flash a 6 chiffres.
- Le logo SmartVision haut gauche est retire de l'overlay Live.
- Le titre chaine est legerement reduit, les boutons sont plus rapproches/serres a droite, plus opaques et avec un focus moins epais.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/build.gradle.kts`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Validation:
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.90 (94)` > prod `0.1.89 (93)`.
- `.\gradlew.bat assembleRelease`: OK en environ 15m25.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.90 (94)`.
- `git diff --check`: OK, avertissements CRLF uniquement.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK.
- Production verifiee: `app_update.php` sert `latest_version_code=94`, manifeste public `0.1.90`, APK versionne `smartvision-tv-v94-f4ad6681.apk` et APK stable `smartvision-tv.apk` repondent `200`, taille `40133769`, SHA256 `f4ad66813f308ec109186881b731c82939aec17eec45154da56e27b57c2aa674`.
- `apksigner verify --verbose --print-certs`: OK, signatures v1/v2 valides, certificat `CN=SmartVision`; avertissements META-INF non bloquants.

## 2026-07-05 - Affinage overlay Live TV et release 0.1.89

Type:
- android
- ui-tv
- playback
- release
- documentation

Resume:
- Le bandeau Live plein ecran devient rectangulaire, plus compact et plus sobre visuellement.
- Le logo de chaine est affiche sans cadre et les infos chaine sont decalees vers la gauche.
- Le numero affiche en haut a droite reprend le numero reel/pad de la liste Live.
- L'EPG sous le nom de chaine utilise le programme local courant quand il existe.
- Haut/Bas zappe en priorite sur Live TV, meme si un panneau EPG/Settings est ouvert.
- Le panneau Settings Live recupere le focus, retire la ligne vitesse indisponible et expose plus de choix d'aspect ratio.
- A la sortie du fullscreen Live, la liste restaure le focus sur la derniere chaine ouverte, y compris apres zapping.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/domain/repository/CatalogRepository.kt`
- `app/build.gradle.kts`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Validation:
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.89 (93)` > prod `0.1.88 (92)`.
- `.\gradlew.bat assembleRelease`: premier essai KO sur `firstLiveActionFocusRequester` manquant, corrige; second essai OK en 9m.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.89 (93)`.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK.
- `apksigner verify --verbose --print-certs`: OK, signatures v1/v2 valides, certificat `CN=SmartVision`.
- Production verifiee: `api/app_update.php` sert `latest_version_code=93`, APK versionne `smartvision-tv-v93-36ec0343.apk` et APK stable `smartvision-tv.apk` repondent `200`, taille `40133779`, SHA256 `36ec03430e065b5234252ee2142cf7b057e8429a62894fa15d90e3e737a03bcb`.

## 2026-07-05 - Header date et heure

Type:
- android
- ui-tv
- documentation

Resume:
- Le header principal affiche maintenant l'heure `HH:mm:ss` et la date `dd/MM/yyyy` sur deux lignes tout a droite.
- Le logo, les espacements d'onglets et les boutons icones du header sont legerement compactes pour conserver le rendu sur une seule ligne.
- Le bloc date/heure reste non focusable et ne change pas le routage D-pad ni les callbacks de navigation existants.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/ui/home/TvHeader.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin`: OK en 23s apres ajustement final.

## 2026-07-05 - Overlay player Live TV et release 0.1.88

Type:
- android
- ui-tv
- playback
- release
- documentation

Resume:
- Le player plein ecran Live TV utilise maintenant un overlay dedie au direct: logo SmartVision en haut gauche, numero de chaine en haut droite et bandeau bas full-width colle au bas de l'ecran.
- Le bandeau Live affiche logo chaine, nom, ligne EPG locale si disponible, puis les boutons compacts `EPG`, `Settings`, `Record`, `Back to List`.
- Les controles VOD ont ete retires du Live TV plein ecran: plus de pause, -10s, +10s, fullscreen ni barre de progression.
- Le panneau EPG lateral affiche les programmes locaux avec horaire, titre, description et mise en evidence du programme courant.
- Le panneau Settings Live expose le mode d'affichage `Fit`, `Fill`, `Zoom`, `16:9`, `Auto`; la vitesse est affichee comme indisponible pour Live TV.
- `Record` reste un placeholder avec TODO DVR et message court, sans developper l'enregistrement.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/build.gradle.kts`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Validation:
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.88 (92)` > prod `0.1.87 (91)`.
- `.\gradlew.bat assembleRelease`: OK en 10m20s.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.88 (92)`.
- `apksigner verify --verbose --print-certs`: OK, signatures v1/v2 valides, certificat `CN=SmartVision`.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK.
- Production verifiee: `api/app_update.php` sert `latest_version_code=92`, APK versionne `smartvision-tv-v92-4709cf61.apk` et APK stable `smartvision-tv.apk` repondent `200`, taille `40133757`, SHA256 `4709cf614b68ae0c489efb7323e9aca9a9f096a7a2979bfc2007b0e28c2e95c7`.

## 2026-07-05 - Scroll Activite reseau et timeout notifications

### Changements
- Settings > Activite reseau: le panneau droit devient focusable et scrollable au D-pad pour parcourir toute la liste active/recente.
- `api/notifications.php`: la jointure utilisateur n'est plus executee pour les notifications `all` / `devices`; elle reste reservee aux candidates `target_scope = users`, ce qui reduit les risques de `SocketTimeoutException`.
- Version Android preparee pour release: `0.1.87` / `versionCode 91`.

### Validation
- `php -l server/public_html/api/notifications.php`: OK.
- `./gradlew :app:compileReleaseKotlin`: OK apres correction de l'import `focusable`.

## 2026-07-05 - Activite reseau Settings et correction audio Home

Type:
- android
- ui-tv
- playback
- observability
- release
- documentation

Resume:
- Home garde maintenant le fade-in audio des mini-players jusqu'a disparition/changement du composable pour `Continue watching` LiveImmediate et Tendances; le volume n'est plus remis a `0f` juste apres la premiere frame.
- Ajout de `NetworkActivityTracker`, centralise dans `AppContainer`, avec travaux actifs/recents, progression, taille, debit, duree, source/section et erreurs.
- Instrumentation OkHttp SmartVision/Xtream via titres sanitises host/chemin uniquement; les query params, tokens, identifiants Xtream, mots de passe et URLs de lecture ne sont pas affiches.
- Settings ajoute le menu `Network Activity` / `Activite reseau`, compact et extensible, pour visualiser catalogue Live/Films/Series, M3U, EPG, Home slides/tendances, verification Xtream, update APK et requetes HTTP.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/data/network/NetworkActivityTracker.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/ContentProgressCard.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/TrendingContentRow.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat :app:compileReleaseKotlin` OK.
- Version bump release `0.1.86` / `versionCode 90` car la prod servait deja `0.1.85 (89)`.
- `.\scripts\guard_release_version.ps1 -SkipAdb` OK: local `90` > prod `89`.
- `.\gradlew.bat --% :app:assembleRelease --no-daemon --console=plain -Dkotlin.compiler.execution.strategy=in-process` OK.
- `.\scripts\guard_release_version.ps1 -SkipAdb -RequireBuildMetadata` OK: metadata `0.1.86 (90)`.
- `apksigner verify --verbose --print-certs` OK: signatures v1/v2 valides, certificat `CN=SmartVision`.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall` OK.
- Production verifiee: `smartvision-tv.version.json`, `api/app_update.php`, APK versionne `smartvision-tv-v90-80b95e80.apk` et APK stable `smartvision-tv.apk` servent SHA256 `80b95e80e07d5f4786e1fb5247f60d014a66324399db4ed4c9ad10c8a51e5df0`.

## 2026-07-05 - Mini-players audio Home/Live et release 0.1.85 non publiee

Type:
- android
- ui-tv
- playback
- release-blocked
- documentation

Resume:
- Home `Continue watching` active maintenant le son du mini-player apres 1 seconde de lecture, avec montee progressive du volume player de `0f` a `1f` sur 1 seconde.
- Home `Trending movies` / `Trending series` alignent la transition visuelle de focus sur `Continue watching`: scale focus `1.0f`, animation largeur via `SmartVisionDimensions.FocusAnimationMillis`, stabilisation/transformation a `1_000 ms`.
- Les mini-players tendances et l'apercu Live TV appliquent le meme fade-in audio local, annule au changement d'URL/focus, a l'erreur ou a la disparition du composable.
- `versionCode` a ete incremente de `88` a `89` pour la prochaine release, avec `versionName` conserve a `0.1.85`.
- Release non publiee: `assembleRelease` compile le code mais echoue a `:app:packageRelease` car `local.properties` est absent et la signature release n'a pas `RELEASE_STORE_FILE`. Le vrai `local.properties`, la keystore et les secrets de signature/deploiement doivent etre repris sur l'autre PC.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/ui/home/ContentProgressCard.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/TrendingContentRow.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`
- `app/build.gradle.kts`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`
- `job_progress.md`

Verification:
- `.\gradlew.bat compileDebugKotlin` OK avec `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot` et `ANDROID_HOME=C:\Users\bbess\AppData\Local\Android\Sdk`.
- `guard_release_version.ps1 -SkipAdb` OK avant build: local `0.1.85 (89)`, prod `0.1.85 (88)`.
- `assembleRelease` lance et compile, puis echoue au packaging: `SigningConfig "release" is missing required property "storeFile"`.
- Aucune mise en prod effectuee depuis ce PC.

## 2026-07-04 - Verification Xtream confirmee avant popup

Type:
- android
- xtream
- playback
- documentation

Resume:
- `XtreamConnectionManager.verifyQuick()` confirme maintenant les echecs Xtream sur 3 essais silencieux avant notification, popup ou blocage catalogue.
- La verification principale s'appuie sur `player_api.php` sans `action` et `user_info.status == active`; les categories ne sont plus utilisees pour decider si les identifiants sont valides.
- Un buffering lecteur signale d'abord un incident de flux puis lance une confirmation Xtream, sans transformer immediatement un flux bloque en panne globale.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/data/xtream/XtreamConnectionManager.kt`
- `app/src/main/java/com/smartvision/svplayer/data/remote/XtreamApiClient.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat :app:compileDebugKotlin --no-daemon --max-workers=1 --console=plain` OK.
- `.\gradlew.bat :app:testDebugUnitTest --tests "com.smartvision.svplayer.data.xtream.*" --no-daemon --max-workers=1 --console=plain` OK.
- `assembleDebug` lance puis stoppe a la demande utilisateur avant completion.

## 2026-07-03 - Robustesse historique Series

Type:
- android
- catalogue
- playback
- documentation

Resume:
- Le lecteur episode utilise les details Xtream deja charges pour sauvegarder l'historique avec le vrai titre de serie et le poster, meme si la liste serie Xtream en memoire ne contient pas la serie.
- L'enrichissement historique repare aussi les anciennes lignes episode qui ont `parentContentId` en retrouvant titre/image depuis la serie Room, meme sans entite episode locale.
- Les titres generiques `Series` / `Serie` sont traites comme des fallbacks episode.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/data/repository/XtreamRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/UserContentRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain` OK.

## 2026-07-03 - Normalisation images catalogue et release prod 0.1.85

Type:
- android
- catalogue
- ui-tv
- release-prod
- documentation

Resume:
- Ajout d'une normalisation commune des URL d'images catalogue pour Xtream: URL absolues, `//`, chemins `/...`, chemins relatifs, espaces et slashes echappes.
- Application a l'ingestion Room et a la lecture Room pour Live TV, Movies, Series, historiques et tendances Home.
- Application aux details Xtream utilises par les backdrops/posters Home premium.
- Les logos M3U `tvg-logo` relatifs sont resolus contre l'URL de la playlist.
- Bump release `0.1.85` / `versionCode 88`.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/data/repository/ImageUrlNormalizer.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/Mappers.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/XtreamRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/remote/XtreamUrlFactory.kt`
- `app/src/main/java/com/smartvision/svplayer/data/playlist/M3uPlaylistClient.kt`
- `app/build.gradle.kts`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain` OK.
- `.\scripts\guard_release_version.ps1` OK avant build: local `0.1.85` / `88`, prod `0.1.84` / `87`.
- `.\gradlew.bat assembleRelease --no-daemon --max-workers=1 --console=plain` OK en 13m53s.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata` OK apres build: metadata `0.1.85` / `88`.
- `.\scripts\deploy_activation_phase1.ps1` OK, tests publics et admin OK.
- Prod verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.85` / `88`, APK `smartvision-tv-v88-57ca0938.apk`, SHA256 `57ca0938ddb1b75cf22ff4afddc4c894e059cf5d9e14bb0cf88bc7a9511aa2cb`.
- Hash de `https://smartvisions.net/downloads/smartvision-tv.apk` verifie identique au build local.

## 2026-07-03 - Synchro Films Info compte et corrections EPG Live TV

Type:
- android
- catalogue
- ui-tv
- focus
- documentation

Resume:
- La synchro catalogue Films garde l'appel global `get_vod_streams`, puis bascule sur un fallback par `category_id` si le provider renvoie `0` film alors que des categories VOD existent.
- Les films recuperes par categorie sont rattaches a leur dossier quand la reponse ne porte pas `category_id`, puis dedupliques par `stream_id`.
- Live TV > Historique rehydrate les lignes depuis Room via les ids regardes, ce qui restaure `epgChannelId`, programmes EPG, badge `E` et details EPG sous mini-player.
- Les dossiers Live TV avec EPG affichent leur compteur dans un cadre bleu au lieu d'un badge `E` supplementaire.
- Le panneau details EPG sous le mini-player est focusable: D-pad haut/bas scrolle les programmes, puis D-pad bas va vers `Regarder` quand le bas est atteint.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain` OK.

## 2026-07-03 - Correction historique Series et release prod 0.1.84

Type:
- android
- catalogue
- playback
- release-prod
- documentation

Resume:
- Correction de la sauvegarde `playback_progress` pour les episodes Series.
- Les sauvegardes player conservent les metadonnees d'historique existantes au lieu de les remplacer par des champs vides.
- Les entrees `episode` sont enrichies depuis Room quand possible avec titre de serie, label saison/episode, poster de serie et `parentContentId`.
- Les titres generiques `Episode <id>` / `Series` / `Serie` et les sous-titres generiques sont remplaces par les metadonnees serie/episode disponibles.
- Publication prod `0.1.84` / `versionCode 87`.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/data/repository/UserContentRepository.kt`
- `app/build.gradle.kts`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain` OK.
- `.\scripts\guard_release_version.ps1` OK avant build: local `0.1.84` / `87`, prod `0.1.83` / `86`.
- `.\gradlew.bat assembleRelease --no-daemon --max-workers=1 --console=plain` OK en 12m13s.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata` OK apres build.
- `.\scripts\deploy_activation_phase1.ps1` OK, tests publics et admin OK.
- Prod verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.84` / `87`, APK `smartvision-tv-v87-60b47236.apk`, SHA256 `60b472366e98c48e33b7c23f0ae495bdd9e7fc07a672aa18c5bffa631d76a109`.
- Hash de `https://smartvisions.net/downloads/smartvision-tv.apk` verifie identique au build local.

## 2026-07-03 - HOME Tendances premium

Type:
- android
- ui-tv
- catalogue
- documentation

Resume:
- `Trending movies` et `Trending series` utilisent maintenant `TrendingContentRow`, separe de `ContinueWatchingRow`.
- Les cards Tendances restent portrait au focus court, attendent `1,3s`, s'ancrent a gauche, passent en 16:9, affichent backdrop/fallback poster floute, puis lancent un mini-preview Media3 muet.
- Les previews demarrent a 15% de la duree connue avec tentative fallback 30% si la premiere frame n'arrive pas.
- Les details premium sont prepares seulement pour les items visibles/proches ou focussees, avec concurrence bornee a `2`.
- Ajout du cache Room `home_trending_preview_cache` metadata-only; aucune URL de lecture brute n'est stockee en base.
- Ajout de la feuille de route `ui-ux/home-trending-premium-roadmap.md` et d'une decision cache metadata-only.

Fichiers code/schema:
- `app/src/main/java/com/smartvision/svplayer/ui/home/TrendingContentRow.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/data/home/HomeContentRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/SVDatabase.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/entity/MediaEntities.kt`
- `app/schemas/com.smartvision.svplayer.data.local.SVDatabase/9.json`

Verification:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain` OK.
- `.\gradlew.bat assembleRelease --no-daemon --max-workers=1 --console=plain` OK.
- `adb -s 192.168.1.33:5555 install -r app\build\outputs\apk\release\app-release.apk` OK.
- TV verifiee: `versionName=0.1.83`, `versionCode=86`.

## 2026-07-03 - Correction warnings Home transitoires et retour tendances

Type:
- android
- ui-tv
- catalogue

Resume:
- `AppNavigation` separe le blocage navigation Xtream du blocage visuel Home/Header.
- Les warnings jaunes et overlays "Connexion indisponible" ne sont plus affiches pendant le simple etat `checking`; ils restent visibles apres echec Xtream confirme.
- `HomeScreen` relance les tendances en cache-first apres la premiere frame Home, sans repasser par le splash ni forcer de synchronisation catalogue.
- `HomeCollectionsScreen` relance aussi ce chargement cache-first sur la route `Trending`.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeCollectionsScreen.kt`

Verification:
- `.\gradlew.bat :app:assembleRelease --no-daemon --max-workers=1 --console=plain` OK.
- `adb -s 192.168.1.33:5555 install -r app\build\outputs\apk\release\app-release.apk` OK.
- Firestick `0.1.83` / `versionCode 86`: captures sous `diagnostics/home-badges-trending-fix-20260703-055415`, Home sans badge jaune transitoire et lignes `Trending movies` / `Trending series` visibles apres D-pad bas.

## 2026-07-03 - Suppression du tampon bleu fonce entre splash et Home

Type:
- android
- startup
- ui-tv

Resume:
- `MainActivity` conserve le snapshot d'activation locale lu pendant le splash dans `AppContainer`.
- `ActivationViewModel` demarre avec ce snapshot quand il est disponible, avec `localStateReady=true` des sa creation.
- `AppNavigation` ne passe plus par l'etat tampon `localReady=false` sur le chemin Home deja active, ce qui supprime l'ecran bleu fonce intermediaire apres le splash.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/activation/ActivationViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`

Verification:
- `.\gradlew.bat :app:assembleRelease --no-daemon --max-workers=1 --console=plain` OK.
- `adb -s 192.168.1.33:5555 install -r app\build\outputs\apk\release\app-release.apk` OK.
- Logcat Firestick: apres `startup complete: rendering AppNavigation`, premier etat observe `state localReady=true checking=false activated=true playableSource=true route=home`.

## 2026-07-03 - Splash sans synchro, Home immediat et release prod 0.1.83

Type:
- android
- performance
- ui-tv
- catalogue
- release-prod

Resume:
- `MainActivity` ne lance plus aucune synchro, aucun `LoadLocal`, aucun prechargement Home et aucun chargement catalogue pendant le splash.
- Le splash garde seulement les controles legers indispensables, lit l'activation locale, efface les demandes startup residuelles et rend directement `AppNavigation` / Home.
- Home decide la synchronisation automatique apres premier rendu et bloque la telecommande uniquement pendant une vraie `Synchronize`.
- Live TV / Movies / Series chargent d'abord `20` categories Room maximum, puis completent discretement la liste complete dans chaque ViewModel.
- Les tendances Home utilisent `10` films + `10` series aleatoires depuis Room, hors adulte, sans recalcul `trending_media`, sans validation URL et sans appels details Xtream.
- Version release incrementee a `0.1.83` / `versionCode 86`.

Fichiers code/version:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/data/home/HomeContentRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/CategoryDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/domain/repository/CatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeCollectionsScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesViewModel.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -ExecutionPolicy Bypass -File .\scripts\guard_release_version.ps1` OK.
- `.\gradlew.bat :app:assembleRelease --console=plain --no-daemon '-Dkotlin.compiler.execution.strategy=in-process' '-Dorg.gradle.workers.max=1'` OK en 13m30.
- `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -ExecutionPolicy Bypass -File .\scripts\guard_release_version.ps1 -RequireBuildMetadata` OK.
- `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -ExecutionPolicy Bypass -File .\scripts\deploy_activation_phase1.ps1 -SkipInstall` OK.
- Prod verifiee: `downloads/smartvision-tv.version.json`, `api/app_update.php`, APK versionne `smartvision-tv-v86-9fcd8f69.apk` et APK stable cache-buste publient `0.1.83` / `versionCode 86`.
- SHA256 local/versionne/stable: `9fcd8f69555e3c7e99b495d8c136134f6d3814ef758afc21e17d97dd26568751`.

## 2026-07-03 - Deplacement synchro/catalogue Splash vers Home et release locale 0.1.82

Type:
- android
- ui-tv
- catalogue
- release

Resume:
- `MainActivity` ne lance plus la synchronisation catalogue complete et ne charge plus categories/pages catalogue pendant le splash.
- Ajout de `StartupCatalogWorkRequest` dans `AppContainer` pour transmettre a Home `Synchronize`, `LoadLocal` ou aucun travail.
- Home orchestre les traitements section par section avec overlays sombres progressifs sur Live TV / Films / Series, blocage telecommande pendant traitement et deblocage en succes ou erreur.
- `SyncStatus` expose maintenant les phases `WAITING`, `RUNNING`, `IMPORTING`, `LOADING_TRENDS`, `COMPLETED`, `ERROR` et un pourcentage par section.
- Les tendances sauvegardees sont chargees explicitement par section; le recalcul `trending_media` reste limite a la synchronisation catalogue.
- Frequence de synchronisation par defaut ramenee a `24h`.
- Release locale `0.1.82` / `versionCode 85` construite et installee sur Firestick `192.168.1.33:5555`.

Fichiers code/scripts:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/startup/StartupCatalogWork.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/home/HomeContentRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeCategoryCard.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`
- `TROUBLESHOOTING.md`

Verification:
- `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -ExecutionPolicy Bypass -File .\scripts\guard_release_version.ps1` OK.
- `.\gradlew.bat :app:assembleRelease --console=plain --no-daemon '-Dkotlin.compiler.execution.strategy=in-process' '-Dorg.gradle.workers.max=1'` OK.
- `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -ExecutionPolicy Bypass -File .\scripts\guard_release_version.ps1 -RequireBuildMetadata` OK.
- `adb -s 192.168.1.33:5555 install -r app\build\outputs\apk\release\app-release.apk` OK.
- `adb -s 192.168.1.33:5555 shell monkey -p com.smartvision.svplayer 1` OK.

## 2026-07-03 - Corrections performance Splash/Home diagnostic

Type:
- android
- performance
- ui-tv
- diagnostics

Resume:
- Le splash ne bloque plus sur `HomeContentRepository.preloadTrending()` ni sur les details Xtream/backdrops des tendances; Home rafraichit les tendances apres le premier rendu si le cache est absent.
- `HomeViewModel` initialise `HomeUiState` avec les caches disponibles pour eviter un premier rendu Home vide quand l'historique/slides/tendances sont deja en memoire.
- Le mini-player Home attend un focus stable court avant de creer ExoPlayer, garde le poster/thumbnail jusqu'a `onRenderedFirstFrame` et ignore en poster-only les URLs Media3 non supportees pendant la session.
- Les carrousels Home gardent des slots `LazyRow` stables quand les previews sont activees et limitent le scroll automatique aux cards non entierement visibles.
- `PerformanceDiagnosticRecorder` ecrit les fichiers via un writer arriere-plan pour moins perturber les mesures de jank.
- Le splash systeme a un logo plus petit/remonte, une progress bar recentree sous le logo et un status simplifie `pourcentage + statut`.
- Build local `:app:assembleReleaseDiagnostic` OK et APK `0.1.81-diag` installe sur la Firestick `192.168.1.33:5555`.

Fichiers code/scripts:
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/data/diagnostics/PerformanceDiagnosticRecorder.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/ContinueWatchingRow.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/ContentProgressCard.kt`
- `app/src/main/res/drawable/splash_background.xml`

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat :app:assembleReleaseDiagnostic --no-daemon --max-workers=1 --console=plain` OK.
- `adb -s 192.168.1.33:5555 install -r app\build\outputs\apk\releaseDiagnostic\app-releaseDiagnostic.apk` OK.

## 2026-07-03 - Diagnostic performance local Splash/Home Firestick

Type:
- android
- diagnostics
- perf
- adb

Resume:
- Ajout d'un build local `releaseDiagnostic` (`0.1.81-diag`, meme `applicationId`) qui active uniquement `BuildConfig.PERF_DIAGNOSTICS_ENABLED` et `profileable` shell.
- Ajout de `PerformanceDiagnosticRecorder`, balise `PERF_DIAG`, qui ecrit CSV/JSONL locaux sous `/sdcard/Android/data/com.smartvision.svplayer/files/diagnostics/splash-home-*`.
- Instrumentation diagnostic non fonctionnelle sur `MainActivity`, `HomeViewModel`, `HomeContentRepository`, `HomeScreen`, `ContinueWatchingRow` et `ContentProgressCard`: statuts splash, donnees chargees, cache Home, focus/scroll LazyRow, mini-player Media3 et premiere frame.
- Ajout du script `scripts/capture_firestick_splash_home_perf.ps1` pour installer l'APK diagnostic, capturer logcat/gfxinfo/meminfo/screenshots, pull les fichiers app, generer `perf-diagnostics.xlsx` et creer un ZIP.
- Capture Firestick produite sous `diagnostics/firestick-splash-home-perf-20260703-001037.zip`; Perfetto indisponible sur cette Firestick via CLI ancienne, erreurs conservees dans les logs bruts.

Fichiers code/scripts:
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/smartvision/svplayer/data/diagnostics/PerformanceDiagnosticRecorder.kt`
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/*`
- `app/src/main/java/com/smartvision/svplayer/data/home/HomeContentRepository.kt`
- `scripts/capture_firestick_splash_home_perf.ps1`

Fichiers MD mis a jour:
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`
- `TROUBLESHOOTING.md`

## 2026-07-02 - Splash hybride et diagnostic startup

Type:
- android
- startup
- ui-tv
- release-prod

Resume:
- `Theme.SVPlayer.Splash` affiche le fond + logo reduit dans la preview systeme pendant tout le startup.
- `MainActivity` ne redessine plus fond/logo en Compose; il affiche uniquement progress bar, statut et diagnostics au-dessus de la preview systeme.
- Le fond de fenetre splash est remplace par un fond opaque neutre juste avant `AppNavigation`, pour eviter que fond/logo splash restent visibles derriere Home.
- Les diagnostics startup affichent pourcentage, etape courante/total, elements traites/restants, temps ecoule, ETA estimee et details Live/Films/Series pendant une synchro catalogue.
- Version locale incrementee a `0.1.81` / `versionCode 84` car `0.1.80` / `83` est deja publie.
- `AppNavigation` attend `ActivationViewModel.localStateReady` avant de rendre `ActivationScreen`, pour eviter le flash activation apres splash sur appareil deja actif.
- Les tendances Home sont preparees pendant le startup dans `HomeContentRepository`: config tendances, details `backdropUrl`, mapping final `ContinueItem`, puis cache consomme par `HomeViewModel`.
- Le mini-player Continue watching rend le `PlayerView` visible et lance `play()` immediatement au focus, sans attendre la duree ou la premiere frame derriere un alpha `0`.
- Les carrousels Continue watching / Trending ancrent horizontalement la card focussee via `LazyListState.animateScrollToItem`, avec borne de fin de liste.
- Les lignes tendances Home ne restaurent plus un ancien scroll sauvegarde: chaque jeu de donnees cree un `LazyListState` neuf et se remet a l'index `0`, ce qui evite une ouverture directement sur un item au milieu de liste.
- Le clic sur HOME dans le header quand l'utilisateur est deja sur Home fait un refresh local Home et remet les lignes a gauche au lieu de renaviguer vers la meme route.
- Les sections Home vides ne sont plus affichees et les routes D-pad ignorent les lignes absentes.
- Release prod publiee en `0.1.81` / `versionCode 84` avec APK `smartvision-tv-v84-1577e450.apk`, SHA256 `1577e4508feb3ae94d5ba672f67ff851d0a1779b6b94a34dd257044b4a65afb0`; `downloads/smartvision-tv.version.json`, `api/app_update.php`, APK versionne et APK stable verifies.

Fichiers code/version:
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/data/home/HomeContentRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeCategoryCard.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeHeroBanner.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeCollectionsScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/ContinueWatchingRow.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/ContentProgressCard.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`
- `app/src/main/res/drawable/splash_background.xml`

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat --% :app:compileReleaseKotlin --no-daemon --console=plain -Dkotlin.compiler.execution.strategy=in-process` OK.
- `.\scripts\guard_release_version.ps1` OK avant build.
- `.\gradlew.bat --% :app:assembleRelease --no-daemon --console=plain -Dkotlin.compiler.execution.strategy=in-process` OK en 8m27.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata` OK apres build.
- `apksigner verify --verbose --print-certs app-release.apk` OK.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall` OK; verification publique du manifeste, `app_update.php`, telechargement APK versionne/stable et hash OK.

## 2026-07-02 - Stabilisation verticale Home

Type:
- android
- ui-tv
- release
- deploy

Resume:
- Home ne fait plus de `bringIntoView()` repete apres `requestFocus()` pour les transitions Continue watching / Trending.
- Le routage Up/Down annule le job precedent, remet la ligne cible au premier item, lance un seul scroll vertical `ScrollState.animateScrollTo()`, puis demande le focus apres la fin du scroll.
- Les lignes Continue watching / Trending gardent une hauteur fixe pour isoler l'animation de largeur de la card focussee des calculs de scroll vertical.
- Les logs `SVHomeFocus` tracent `scroll start`, `scroll end` et `focus requested`.
- Release prod publiee en `0.1.78` / `versionCode 81` avec APK `smartvision-tv-v81-bcf5a8d7.apk`, hash SHA256 `bcf5a8d7e1201e28fdd78f44cfe26c349f61f1896aa3e92d52b8731a56cedd5c`.

Fichiers code/version:
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/ContinueWatchingRow.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/theme/Dimensions.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-02 - Splash systeme MainActivity sans handoff

Type:
- android
- startup
- release-prep

Resume:
- `MainActivity` devient l'unique activite launcher TV/Android avec `Theme.SVPlayer.Splash` pour afficher immediatement le fond splash systeme.
- La logique startup/preload anciennement portee par `SplashActivity.runStartupChecks()` est deplacee dans un etat Compose de `MainActivity`.
- `SplashActivity` et `StartupHandoffScreen` sont supprimes; apres le statut `Demarrage en cours...`, `MainActivity` rend directement `AppNavigation`.
- `MainActivity` ne pose plus `window.setBackgroundDrawableResource(R.drawable.splash_background)` afin d'eviter le retour du fond splash derriere Home.
- Version locale incrementee a `0.1.78` / `versionCode 81` car `0.1.77` / `80` etait deja publie.

Fichiers code/version:
- `app/build.gradle.kts`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-02 - Mini-player Home et config tendances admin

Type:
- android
- backend
- admin
- release-prep

Resume:
- Continue watching n'affiche plus de poster dans le mini-player: le focus lance la video sur fond noir avec fondu au demarrage.
- Tendances films/series affichent uniquement le poster paysage pendant 4 secondes, puis fondent vers la video; la lecture mini-player est bornee a 40 secondes avant retour au poster.
- Navigation Home: `LEFT` est bloque sur la premiere card des carrousels et le scroll horizontal automatique a chaque focus est retire pour reduire la latence/vibration.
- Selection tendances: filtre note retire par defaut, filtre adulte conserve, filtre poster paysage conserve au chargement Home.
- `Admin > Fonctionnalites` ajoute les parametres Tendances Home, exposes par `api/app_config.php` et consommes par Android.
- Handoff splash affiche aussi logo, barre pleine et statut `Demarrage en cours...` pour eviter le fond seul avant Home.
- Release prod publiee en `0.1.77` / `versionCode 80` avec APK `smartvision-tv-v80-8c4d880b.apk`, hash SHA256 `8c4d880b1b686aee1c101b668f0ff018181ee2c2fd1600a8ee14c42cc44bd1f0`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-02 - Mini-player Home poster paysage

Type:
- android
- ui-tv
- release-prep

Resume:
- Home filtre les tendances films/series au chargement de section pour ne garder que les medias avec une image paysage `backdropUrl` issue des details Xtream.
- Les cards Home portent maintenant une image `previewImageUrl` dediee au mini-player, distincte du poster portrait.
- Le mini-player garde le poster paysage affiche pendant la preparation Media3, attend 4 secondes de focus pour films/series, puis affiche la video en crossfade plus lent afin d'eviter l'impression de coupure/ecran noir.
- Le routage focus Home demande le focus avant le recentrage vertical pour reduire le decalage entre mouvement d'ecran et focus visible.
- Release prod publiee en `0.1.76` / `versionCode 79` avec APK `smartvision-tv-v79-54bfa614.apk`, hash SHA256 `54bfa6145236657f92be05e012fa76ca4416ccddb34b764a8ff71188418e937d`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-02 - Handoff splash et focus Home durable

Type:
- android
- ui-tv
- release-prep

Resume:
- `MainActivity` applique le fond `splash_background` avant `setContent` et `AppNavigation` affiche `smartvision_splash_bg` pendant la rehydratation locale pour eviter l'ecran noir entre splash et Home.
- Home renforce le routage D-pad vertical: chaque descente vers une ligne cible remet la `LazyRow` a l'index `0`, attend les frames Compose, scrolle la ligne visible, puis demande le focus sur le premier item.
- Les carrousels Continue watching / Trending movies / Trending series recoivent un padding horizontal interne pour que la premiere card, le focus et le mini-player ne soient pas tronques.
- Ajout de logs diagnostics `SVStartup` et `SVHomeFocus` pour verifier le handoff splash et les problemes de focus sur Firestick.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-02 - Splash immediat, focus Home et preview Continue watching

Type:
- android
- ui-tv
- catalogue
- release-prep

Resume:
- `SplashActivity` attend la premiere frame Compose avant les checks startup et `SVPlayerApplication` differe l'initialisation diagnostic hors thread UI pour afficher logo/progress bar sans retard perceptible.
- Le splash prechauffe apres synchro ou sans synchro les categories et premieres pages locales bornees Live/Films/Series, reutilisees par les ViewModels catalogue pour limiter les loaders d'ouverture.
- Home corrige le D-pad bas vers Continue watching / Trending movies / Trending series avec bring-into-view avant `requestFocus`.
- Continue watching utilise le mini-player Media3 muet au focus: Live immediat, Films/Episodes depuis la position de reprise, boucle de 20 secondes et overlay i18n `Resume playback` / `Reprendre la lecture`.
- Release prod publiee en `0.1.74` / `versionCode 77` avec APK `smartvision-tv-v77-055a7642.apk`, hash SHA256 `055a764235f6036795617e82fdd47ae71a68c3c448cc861beb4a44a5033205ab`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-02 - Splash image et tendances Home separees

Type:
- android
- ui-tv
- catalogue
- release-prep

Resume:
- Retour du splash sur le fond image `smartvision_splash_bg`, sans video Compose et sans second splash applicatif.
- Home separe les tendances en `Trending movies` et `Trending series`, limite l'historique a `10` items et precharge au splash uniquement les jeux Home bornes `10/10/10`.
- Ajout de `trending_media` en Room pour stocker maximum `50` films et `50` series valides pendant la synchro Xtream, avec selection note `10/10` puis `9/10`, exclusion adulte et controle court d'URL de lecture.
- Les cards tendances passent en 16:9 au focus, gardent le poster immediat puis lancent un mini-player Media3 muet par segments avec fondu.
- D-pad bas sur Home cible le premier item de la ligne suivante: categories -> premiere ligne disponible, Continue watching -> Trending movies, Trending movies -> Trending series.
- Release prod publiee en `0.1.73` / `versionCode 76` avec APK `smartvision-tv-v76-6a70e99f.apk`, hash SHA256 `6a70e99fda1ee40aaf14d45791cd8fde93ef9b8c6833e121e99e1c2a4df686e9`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-01 - Pagination Room des catalogues

Type:
- android
- performance
- documentation

Resume:
- Abandon de l'idee de mini catalogue backend pour cette optimisation: Room reste le stockage persistant complet sur l'appareil.
- Ajout de requetes catalogue paginees locales `LIMIT/OFFSET` et de methodes `CatalogRepository` pour Live TV, Movies et Series.
- Live TV / Movies / Series ne chargent plus les snapshots complets pour ouvrir les ecrans; ils affichent les categories puis chargent les contenus au scroll.
- Home ne seme plus ses tendances depuis les snapshots complets Movies/Series; il reste sur des requetes limitees et l'historique recent.
- Ajout d'une decision durable: `docs/ai-knowledge/decisions/2026-07-01-room-paged-catalog-ui.md`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/decisions/2026-07-01-room-paged-catalog-ui.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/domain/repository/CatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesViewModel.kt`

## 2026-07-01 - Splash leger et filtre EPG Live TV

Type:
- android
- ui
- performance
- documentation

Resume:
- `SplashActivity` ne precharge plus Home ni EPG; le demarrage lit seulement les categories/counts apres les verifications et l'eventuelle synchro catalogue.
- Le splash garde la video, mais ajoute `@drawable/splash_background` en preview systeme et un poster `smartvision_splash_bg` jusqu'a la premiere frame Media3 pour reduire l'ecran noir avant la video.
- Live TV remplace la recherche du header Categories par un bouton `EPG`; actif, il filtre les dossiers pour n'afficher que ceux qui ont au moins une chaine avec EPG local disponible.
- Le cache EPG passe par un fichier local borne et un parsing streaming, et la synchro EPG manuelle ne rend plus la synchro catalogue rouge si elle echoue seule.
- Les demandes de focus du profil/popup sont differees et protegees; le test Firestick 10 minutes a revele des crashes `FocusRequester is not initialized` sur DPAD droite, corriges par suppression des liens `focusProperties` vers items lazy et filet de securite dans `MainActivity`.
- Release prod publiee en `0.1.71` / `versionCode 74` avec APK `smartvision-tv-v74-d76a5da8.apk`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/data/playlist/EpgRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/catalog/MediaCatalogComponents.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `app/src/main/res/values/styles.xml`

## 2026-07-01 - Diagnostic memoire synchro Xtream Firestick

Type:
- android
- tooling
- documentation

Resume:
- Ajout de logs `SVSyncMemory` dans la synchronisation Xtream pour mesurer les pics memoire autour des appels reseau, de l'ecriture Room et de l'invalidation du cache local.
- Ajout du script `scripts/capture_firestick_xtream_sync.ps1` pour capturer `logcat` et `dumpsys meminfo` sur la Firestick `192.168.1.33:5555`.
- Documentation du chemin ADB Windows de reference: `C:\Users\ONEDEV\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Premiere capture Firestick: un tres gros catalogue a atteint `usedMb=118` juste avant l'ecriture Room puis a fini en ANR; au redemarrage, la reconstruction locale a lance deux OOM de 62 Mo avant qu'une synchro plus reduite reussisse.
- Optimisation candidate: synchro Xtream traitee par sections et insertions Room en batchs dans une transaction unique, counts/categories via SQL, tendances Home limitees, splash sans prechargement complet Movies/Series.
- Capture Firestick apres optimisation: `diagnostics/firestick-sync-20260701-123758`, deux synchros reussies sans OOM/ANR sur `21769 Live / 104005 Movies / 24325 Series`; pic `SVSyncMemory` observe a environ `59 Mo`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code/outillage concernes:
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/SVDatabase.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaDao.kt`
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `scripts/capture_firestick_xtream_sync.ps1`

## 2026-07-01 - M3U source-aware, EPG badge et UI player slim

Type:
- android
- ui
- documentation

Resume:
- Le splash et le popup manuel de synchronisation tiennent compte de la source active: M3U affiche le lien M3U et ne presente plus Films/Series comme sections chargees.
- Live TV accepte un lien M3U comme source jouable et Movies/Series affichent un etat vide explicite quand M3U est actif.
- Ajout d'un badge bleu `E` sur les lignes Live TV qui ont des programmes EPG locaux.
- Info compte est compacte: badge usage dans Licence, expiration Xtream dans Info compte, identifiants Xtream sur une ligne, boutons d'actions en icones et bascules plus petites.
- L'overlay player Live/Films/Series devient plus slim sans toucher YouTube.
- L'initialisation lourde de l'application est differee pour reduire l'ecran noir avant le splash, et les etats transitoires n'affichent plus l'ancien visuel splash.
- Release prod publiee en `0.1.70` / `versionCode 73` avec APK `smartvision-tv-v73-5ab10617.apk`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SVPlayerApplication.kt`
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/build.gradle.kts`

## 2026-07-01 - Splash video Compose Media3

Type:
- android
- ui
- documentation

Resume:
- `SplashActivity` remplace l'ancien fond image par `splash_wave_animation.mp4` lu avec Media3 `ExoPlayer` dans un ecran Compose plein ecran.
- La video est muette, bouclee, sans controles et non focusable; le fond noir sert de securite.
- Le logo, la progress bar, les statuts startup et les prechargements Home / Live TV / Films / Series restent dans le meme splash unique.
- Le theme `Theme.SVPlayer.Splash` utilise maintenant un `windowBackground` noir pour eviter l'ancien visuel avant l'ecran video.
- Release prod publiee en `0.1.69` / `versionCode 72` avec APK `smartvision-tv-v72-a30ec7bf.apk`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/res/values/styles.xml`
- `app/build.gradle.kts`

## 2026-07-01 - Source M3U active et EPG Live TV

Type:
- android
- ui
- backend
- documentation

Resume:
- Info compte compacte les cartes source et ajoute le lien M3U avec bascules exclusives Xtream/M3U ON vert / OFF rouge.
- Le catalogue supporte une source active unique: Xtream conserve le comportement existant, M3U alimente Live TV via URL directe et vide Movies/Series.
- Ajout du parser M3U, du cache EPG XMLTV et de l'affichage EPG scrollable dans l'apercu Live TV.
- Le Splash affiche les statuts de synchronisation/chargement M3U et EPG; le backend considere `m3u_url` comme playlist configuree sans marquer Xtream configure.
- Release prod publiee en `0.1.68` / `versionCode 71` avec APK `smartvision-tv-v71-44e92a70.apk`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/decisions/2026-07-01-exclusive-playlist-source.md`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/core/config/XtreamAccountManager.kt`
- `app/src/main/java/com/smartvision/svplayer/data/playlist/*`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/*`
- `server/public_html/api/device_status.php`
- `server/public_html/api/save_playlist_config.php`

## 2026-07-01 - Correctifs Home YouTube, Playlist et Info compte pilote

Type:
- android
- ui
- backend
- siteweb
- documentation

Resume:
- Correction du flash Home ou YouTube apparaissait verrouille avec couronne pendant le chargement initial de la config distante.
- Les pushs playlist depuis `/playlist/` ou le QR Xtream creent maintenant une notification d'information ciblee sur la TV, non cliquable dans l'app.
- `/playlist/` passe en interface mobile-first a onglets `Code Xtream`, `Lien M3U`, `Lien EPG`; le header web est stabilise et le payload chiffre accepte aussi `m3u_url`.
- Info compte reutilise le header principal, reduit le menu gauche, agrandit/compacte le panneau droit, retire les cadres des icones de lignes/actions EPG et donne le focus initial a `Licence SmartVision`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/notifications/NotificationsScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/data/activation/ActivationApiService.kt`
- `server/public_html/api/helpers.php`
- `server/public_html/api/save_playlist_config.php`
- `server/public_html/playlist/index.php`
- `server/public_html/assets/site.css`

## 2026-07-01 - Info compte, URL EPG et page Playlist

Type:
- android
- ui
- backend
- siteweb
- documentation

Resume:
- `Profil client` est renomme `Info compte`; la section Xtream est reorganisee en cartes selon le modele fourni, avec actions Modifier par QR / Modifier / Supprimer et icones blanches.
- Ajout d'une section URL EPG avec affichage, popup d'edition locale et bouton QR.
- Le payload playlist chiffre accepte `epg_url`; `device_status.php` renvoie l'URL EPG a la TV sans marquer Xtream comme configure si les identifiants sont absents.
- Ajout de la page publique `/playlist/` dans le header/footer pour envoyer par code TV des identifiants Xtream et/ou une URL EPG vers la TV.
- Release prod publiee en `0.1.67` / `versionCode 70` avec APK `smartvision-tv-v70-50bfb24e.apk`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/core/config/XtreamAccountManager.kt`
- `app/src/main/java/com/smartvision/svplayer/data/activation/*`
- `server/public_html/api/device_status.php`
- `server/public_html/api/save_playlist_config.php`
- `server/public_html/playlist/index.php`
- `server/public_html/xtream/index.php`
- `scripts/deploy_activation_phase1.ps1`

## 2026-07-01 - Ajustement splash et suppression flash activation

Type:
- android
- ui
- documentation

Resume:
- `SplashActivity` garde le splash natif unique et augmente le ratio du logo a `0.54f` et de la progress bar a `0.36f`.
- `ActivationViewModel` expose `localStateReady` apres lecture du cache activation local.
- `AppNavigation` affiche seulement le fond plein ecran tant que l'etat local activation n'est pas pret, au lieu de rendre temporairement `ActivationScreen`.
- Release prod publiee en `0.1.66` / `versionCode 69` avec APK `smartvision-tv-v69-cb3e3030.apk`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/activation/ActivationViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/build.gradle.kts`

## 2026-07-01 - Splash unique et prechargement Home/catalogues au demarrage

Type:
- android
- ui
- performance
- documentation

Resume:
- Suppression du panneau `StartupVerificationPanel` Compose pour eliminer le second splash et sa seconde progress bar.
- `SplashActivity` redevient l'unique splash visuel, au format du premier modele, avec progression par statuts startup.
- Le splash verifie activation/Xtream, decide si la derniere synchronisation est OK ou KO, synchronise si necessaire, puis precharge Home / Live TV / Films / Series.
- Home utilise les caches memoire de slides, progression recente enrichie et snapshots Movies / Series pour eviter le premier chargement visible.
- Release prod publiee en `0.1.65` / `versionCode 68` avec APK `smartvision-tv-v68-e4732ab9.apk`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/decisions/2026-06-30-xtream-startup-gating-local-catalog.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/activation/ActivationScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/data/home/HomeSlidesRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/UserContentRepository.kt`
- `app/build.gradle.kts`

## 2026-06-30 - Popup manuel de synchronisation Xtream dans Profil

Type:
- android
- ui
- catalogue
- documentation

Resume:
- Profil > Identifiants Xtream affiche maintenant la derniere synchronisation sous le bouton Synchroniser.
- Le bouton ouvre un popup dedie avec cards Live TV / Films / Series, infos compte masquees et focus initial sur Lancer la synchronisation.
- Pendant `SyncStatus.Running`, le popup bloque Back/D-pad, desactive les boutons, affiche un loader et met a jour compteurs/progress bars par section.
- Apres succes ou erreur, le focus passe sur Retour; la fermeture ouvre Appareil et catalogue et y remet le focus.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/domain/model/Models.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`

## 2026-06-30 - Splash unique, gating Xtream sans flicker et cache catalogue local

Type:
- android
- ui
- performance
- documentation

Resume:
- Splash natif agrandi pour garder un seul visuel de chargement pendant les statuts startup.
- `ActivationViewModel` conserve l'acces local actif pendant le refresh serveur afin d'eviter un second loader Compose apres le splash.
- `CONNECTED + checking` ne bloque plus Home/Header; les overlays Xtream restent reserves aux etats inconnus ou en erreur.
- `XtreamConnectionManager` expose une validation connectee recente pour eviter une verification startup immediate en double apres le splash.
- Ajout d'un cache memoire de snapshots catalogue locaux reutilise par Live TV, Movies et Series pour reduire les loaders lors des retours d'ecran.
- Ajout de tests unitaires pour le gating Xtream et l'invalidation du cache catalogue.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/data/xtream/XtreamConnectionManager.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/LocalCatalogSnapshotCache.kt`
- `app/src/main/java/com/smartvision/svplayer/domain/repository/CatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/activation/ActivationViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesViewModel.kt`

## 2026-06-30 - Correction detection Xtream avec ancien cache local

Type:
- android
- diagnostic
- documentation

Resume:
- Diagnostic du cas TV `LAUU9M`: le serveur Xtream de test timeout sur `player_api.php`, donc l'app devait detecter une erreur reseau.
- Correction du splash: lecture de `device_status.php` avant verification Xtream pour importer la playlist serveur la plus recente avant de tester.
- Correction navigation: verification Xtream obligatoire au premier affichage actif, sans forcer une resynchronisation catalogue si elle n'est pas due.
- Correction UI: etat `checking` considere bloquant et routes detail/player bloquees si Xtream est indisponible.
- Correction lecteur: un buffering persistant ne reste plus en spinner infini; il met l'etat Xtream en erreur, affiche un message et envoie `XTREAM_FAILED`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`
- `TROUBLESHOOTING.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/data/xtream/XtreamConnectionManager.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`

## 2026-06-30 - Diagnostics admin et gating Xtream startup

Type:
- fonctionnalite
- backend
- android
- documentation

Resume:
- Refonte de `Diagnostics` admin en onglets Synthese, AutoSync, Anomalies App, Info Serveur et Journal.
- Ajout d'un etat Xtream central avec verification rapide au splash, classification des erreurs, popup TV, notification locale et anti-doublon d'anomalies.
- Blocage Home/Header des entrees Live TV / Movies / Series et des reprises de lecture quand Xtream est indisponible.
- Navigation catalogue basculee vers les donnees locales Room via `CatalogRepository`.
- Synchronisation complete durcie: verification Xtream avant sync, progression `SyncStatus.Running`, remplacement local apres recuperation complete des reponses principales.
- AutoSync retente automatiquement seulement les erreurs reseau.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/decisions/2026-06-30-xtream-startup-gating-local-catalog.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/data/xtream/*`
- `app/src/main/java/com/smartvision/svplayer/data/remote/XtreamApiClient.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/*`
- `app/src/main/java/com/smartvision/svplayer/ui/live/*`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/*`
- `app/src/main/java/com/smartvision/svplayer/ui/series/*`
- `server/public_html/admin/index.php`
- `server/public_html/api/anomaly_service.php`
- `server/public_html/sql/init_activation_tables.sql`

## 2026-06-30 - Release Android 0.1.61 versionCode 64

Type:
- android
- youtube
- release

Resume:
- Bump Android de `0.1.60 (63)` vers `0.1.61 (64)`.
- YouTube WebView: changement video via `loadVideoById` sans recharger toute la page HTML, bridge Compose actualise et qualite de lecture ciblee `medium`.
- YouTube autoplay: la liste de suggestions est consommee progressivement et maintenue autour de 20 videos pour eviter les arrets apres quelques lectures.
- YouTube UI: header mini-player corrige avec favoris visible et bouton parametres persistant; popup parametres sans arreter la lecture; nettoyage recherche, historique et favoris.
- YouTube overlay: progress bar avec temps consomme/duree totale, bouton retour fullscreen masque avec le bandeau ou affichable seul via DPAD Haut.
- Recommandations YouTube: algorithme de dossiers enrichi avec mix historique, memes chaines, meme style et decouverte.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`
- `../TROUBLESHOOTING.md`

Fichiers code concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeWebPlayer.kt`
- `app/src/main/java/com/smartvision/svplayer/data/youtube/YoutubeRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/YoutubeDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/FavoriteDao.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`

## 2026-06-30 - Release Android 0.1.60 versionCode 63

Type:
- android
- ui
- youtube
- release
- deploy

Resume:
- Bump Android de `0.1.59 (62)` vers `0.1.60 (63)`.
- YouTube mini/fullscreen: le bandeau ne s'affiche plus a l'ouverture, seulement quand la lecture demarre reellement, puis auto-hide apres 7 secondes d'inactivite.
- Ajout progress bar non focusable, bouton reload, previous base sur l'historique, bouton retour plein ecran vers le mini lecteur et reprise de position lors des transitions mini/fullscreen.
- Ecran YouTube: favoris locaux, dossier Favoris entre History et Trending, bouton coeur dans le header, suggestions recherche limitees a 3, miniatures allegees hors focus et animation "now playing" sur la video en cours.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeWebPlayer.kt`
- `app/src/main/java/com/smartvision/svplayer/data/youtube/YoutubeRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/FavoriteDao.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`

## 2026-06-30 - Release Android 0.1.59 versionCode 62

Type:
- android
- ui
- release
- deploy

Resume:
- Bump Android de `0.1.58 (61)` vers `0.1.59 (62)`.
- Correction definitive des boutons du bandeau YouTube: le conteneur player ne consomme plus DPAD/OK quand un bouton du bandeau est focus.
- Le focus gauche/droite reste borne dans le bandeau et OK/Enter atteint maintenant le handler du bouton focus.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`

## 2026-06-30 - Release Android 0.1.58 versionCode 61

Type:
- android
- ui
- release
- deploy

Resume:
- Bump Android de `0.1.57 (60)` vers `0.1.58 (61)`.
- Correction critique des controles YouTube: le bandeau parent ne consomme plus OK/Enter avant les boutons.
- Correction focus YouTube: le WebView ne reprend plus automatiquement le focus quand l'overlay SmartVision Compose controle la lecture.
- Conservation du routage DPAD gauche/droite dans les boutons et des commandes JS play/pause.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeWebPlayer.kt`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`

## 2026-06-30 - Release Android 0.1.57 versionCode 60

Type:
- android
- ui
- release
- deploy

Resume:
- Bump Android de `0.1.56 (59)` vers `0.1.57 (60)`.
- Correction du bandeau de controle YouTube: DPAD gauche/droite route manuellement dans les boutons, OK/Enter declenche l'action focus, Haut/Back masque le bandeau et rend le focus au player.
- YouTube: suppression du bandeau haut sur le mini-lecteur, retrait des libelles sous les icones, bandeau bas plus compact, fond bleu tres transparent, bordures/glow neon renforces.
- Live TV / Films / Series: bandeau haut sans nom de dossier/categorie, bandeau bas plus compact, boutons mieux distingues, bordures et glow neon renforces.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`

## 2026-06-30 - Release Android 0.1.56 versionCode 59

Type:
- android
- ui
- release
- deploy

Resume:
- Bump Android de `0.1.55 (58)` vers `0.1.56 (59)`.
- Refonte overlay plein ecran Live TV / Films / Series en style glassmorphism: bandeau haut avec logo, badge, titre, categorie et meta droite.
- Remplacement du bloc de controles par un bandeau bas transparent avec boutons circulaires; barre de progression conservee uniquement pour Films/Series.
- Application du meme langage visuel au player YouTube avec bandeau haut/bas, boutons ronds, glow bleu et focus Play/Pause.
- Correction de la navigation telecommande du bandeau YouTube: Bas/OK affiche les controles et focus Play/Pause; gauche/droite restent dans le bandeau; Haut/Back masque le bandeau et rend le focus au player.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`

## 2026-06-30 - Release Android 0.1.55 versionCode 58

Type:
- android
- backend
- admin
- release
- deploy

Resume:
- Bump Android de `0.1.54 (57)` vers `0.1.55 (58)`.
- Enrichissement du tracking YouTube: titre video dans `Media`, source de lancement, categorie interpretee depuis titre/description/tags/categoryId, duree et score engagement.
- Suppression de `PLAYER_READY` du tracking accepte cote app et API; conservation de `VIDEO_OPENED` et deduplication `VIDEO_COMPLETED`.
- Amelioration autoplay YouTube: si les suggestions arrivent apres la fin de lecture, la video suivante est lancee des disponibilite.
- Ajout d'un player YouTube plein ecran applicatif avec bandeau de controles SmartVision et controles iframe YouTube masques.
- Traduction incrementale YouTube: categories visibles anglais/francais, anglais par defaut.
- Correction affichage categorie admin pour eviter les codes numeriques YouTube et afficher une categorie exploitable.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/data/local/SVDatabase.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/entity/YoutubeEntities.kt`
- `app/src/main/java/com/smartvision/svplayer/data/youtube/YoutubeBehaviorReporter.kt`
- `app/src/main/java/com/smartvision/svplayer/data/youtube/YoutubeRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeWebPlayer.kt`
- `server/public_html/api/behavior_service.php`

## 2026-06-30 - Release Android 0.1.54 versionCode 57

Type:
- android
- backend
- admin
- release
- deploy

Resume:
- Bump Android de `0.1.52 (55)` vers `0.1.54 (57)`.
- Correction du fallback de titre Live player/tracking via le catalogue local quand le cache Xtream memoire est absent.
- Suppression du tracking `CATEGORY_OPENED` et `PLAYBACK_STARTED` cote app et rejet serveur pour anciennes APK.
- Normalisation admin des types licence comportementaux en `free_ads`, `trial_demo`, `premium`.
- Reorganisation de l'ecran Segmentation en onglets et enrichissement de la lecture rapide.
- Correction du tableau Diagnostics admin: suppression de la reference invalide a `devices.license_type`, jointure activation/code et schema SQL `app_device_diagnostics`.
- Correction Back player Live: Back quitte le player hors pub meme si l'overlay est visible, puis le focus revient sur la chaine selectionnee/focalisee.
- Build release direct via `:app:assembleRelease`, deploiement complet backend/APK et verification publique de `0.1.54 (57)`.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/data/behavior/BehaviorReporter.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `server/public_html/api/behavior_service.php`
- `server/public_html/admin/index.php`
- `server/public_html/assets/admin.js`
- `server/public_html/assets/admin-overrides.css`
- `server/public_html/sql/init_activation_tables.sql`

## 2026-06-30 - Release Android 0.1.52 versionCode 55

Type:
- release
- deploy

Resume:
- Bump Android de `0.1.51 (54)` vers `0.1.52 (55)`.
- Build release direct via `:app:assembleRelease`.
- Deploiement complet backend/APK via `scripts/deploy_activation_phase1.ps1 -SkipInstall`.
- Verification publique de `api/app_update.php`, `downloads/smartvision-tv.version.json`, APK versionne, APK stable et assets admin `admin-overrides.css?v=8` / `admin.js?v=4`.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/build/outputs/apk/release/output-metadata.json`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`

## 2026-06-30 - Update settings, segmentation enrichie et onglets appareil

Type:
- android
- backend
- admin
- documentation

Resume:
- Ajout de `Last update` / `Derniere mise a jour` dans Settings > Updates.
- Suppression de l'ouverture automatique du popup update apres check silencieux.
- Ajout du `contentTitle` dans le tracking Android et stockage backend `content_title`.
- Inference backend region, pays, langue et centres d'interet depuis categories, medias et tags.
- Enrichissement admin Segmentation avec regions, pays, langues, interets et colonnes recentes demandees.
- Correction JS/CSS des onglets du popup detail appareil et cache-busting assets.
- Documentation de la regle langue: UI Android en anglais par defaut avec traduction francaise.

Fichiers concernes:
- `app/src/main/java/com/smartvision/svplayer/ui/update/AppUpdateViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`
- `app/src/main/java/com/smartvision/svplayer/data/behavior/BehaviorReporter.kt`
- `server/public_html/api/behavior_service.php`
- `server/public_html/admin/index.php`
- `server/public_html/assets/admin.js`
- `server/public_html/assets/admin-overrides.css`

## 2026-06-30 - Release Android 0.1.51 versionCode 54

Type:
- release
- deploy

Resume:
- Bump Android de `0.1.50 (53)` vers `0.1.51 (54)`.
- Build release direct via `:app:assembleRelease`.
- Validation du garde-fou version avant et apres build.
- Deploiement backend/APK via `scripts/deploy_activation_phase1.ps1 -SkipInstall`.
- Verification publique de `api/app_update.php`, `downloads/smartvision-tv.version.json`, APK versionne et APK stable.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/build/outputs/apk/release/output-metadata.json`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`

## 2026-06-30 - Garde-fou version release et regle deploy backend

Type:
- tooling
- documentation

Resume:
- Ajout de `scripts/guard_release_version.ps1` pour comparer `versionCode` local, metadata APK, manifests production et appareils ADB connectes.
- Documentation de la cause du conflit `0.1.50 (53)`: numero deja publie et regenere localement sans increment.
- Ajout de la regle Knowledge System: apres chaque nouveau build release livrable, deployer le backend.

Fichiers concernes:
- `scripts/guard_release_version.ps1`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/AGENT_WORKFLOW.md`
- `docs/ai-knowledge/ROOT.md`
- `TROUBLESHOOTING.md`

## 2026-06-30 - Implementation tracking comportemental et segmentation admin

Type:
- code
- backend
- android
- admin

Resume:
- Extension de `app_behavior_events` pour contenus Live TV, Films, Series, Episodes et YouTube.
- Ajout des agrégats `user_behavior_daily` et des segments `user_segments`.
- Ajout du reporter Android generique `BehaviorReporter` et instrumentation non bloquante des ecrans Live, Films, Series, details et player.
- Ajout du menu admin `Segmentation`.
- Refonte du popup detail appareil en onglets avec Tracking et Analyse comportementale.

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/data/behavior/BehaviorReporter.kt`
- `server/public_html/api/behavior_service.php`
- `server/public_html/admin/index.php`
- `server/public_html/assets/admin.js`
- `server/public_html/assets/admin-overrides.css`
- `server/public_html/sql/init_activation_tables.sql`

## 2026-06-30 - Verification code et amelioration continue du Knowledge System

Type:
- documentation
- verification

Resume:
- Reverification du Knowledge System contre le code Android/PHP/script reel.
- Ajout d'un protocole d'auto-amelioration continue.
- Clarification des routes `continue_watching` et `trending`.
- Clarification des routes Android `api/app/*` extensionless, des rewrites `.htaccess` et de l'exception `device-diagnostics.php`.
- Clarification de la spec segmentation comme cible future, distincte du tracking YouTube deja implemente.
- Aucun changement code applicatif.

Fichiers MD mis a jour:
- `docs/ai-knowledge/CONTINUOUS_IMPROVEMENT.md`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/AGENT_WORKFLOW.md`
- `docs/ai-knowledge/PROJECT_OVERVIEW.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/features/user-behavior-segmentation-ads-v1.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/KNOWLEDGE_SYSTEM_TEST_REPORT.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- aucun

## 2026-06-30 - Spec V1 tracking comportemental et segmentation publicitaire

Type:
- documentation
- specification

Resume:
- Ajout d'une spec V1 detaillee pour tracking comportemental, segmentation utilisateur et ciblage publicitaire.
- Reference de la spec dans le domaine monetisation/tracking et dans le root ai-knowledge.
- Aucun changement code.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/user-behavior-segmentation-ads-v1.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- aucun

## 2026-06-29 - Creation du systeme ai-knowledge

Type:
- documentation
- decision

Resume:
- Creation du routeur `ROOT.md`.
- Creation de fichiers specialises par domaine fonctionnel, UI/UX et technique.
- Conservation des MD legacy comme sources historiques.
- Ajout de decisions structurantes.
- Ajout du rapport de migration et du rapport de test par simulations.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/PROJECT_OVERVIEW.md`
- `docs/ai-knowledge/AGENT_WORKFLOW.md`
- `docs/ai-knowledge/LEGACY_SOURCES.md`
- `docs/ai-knowledge/MIGRATION_REPORT.md`
- `docs/ai-knowledge/KNOWLEDGE_SYSTEM_TEST_REPORT.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/decisions/2026-06-29-ai-knowledge-router.md`
- `docs/ai-knowledge/decisions/2026-06-29-native-tv-player.md`
- `docs/ai-knowledge/decisions/2026-06-29-documentation-update-policy.md`

Fichiers code concernes:
- aucun
# 2026-06-30 - Release prod 0.1.62 / versionCode 65

Type:
- release
- deploy
- documentation

Resume:
- Incrementation Android de `0.1.61` / `64` vers `0.1.62` / `65`.
- Build `:app:assembleRelease` reussi et publication prod app + serveur.
- Alignement du test admin du script de deploy sur le menu `Diagnostics` apres centralisation de Journal dans Diagnostics.
- Verification publique du manifeste, du version gate `app_update.php`, du hash APK versionne, de `app_config.php` et de la route `api/app/behavior-events`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`
- `TROUBLESHOOTING.md`

Fichiers code concernes:
- `app/build.gradle.kts`
- `scripts/deploy_activation_phase1.ps1`
# 2026-07-09 - Isolation Room multi-profils et ecran choix profil

Type:
- bugfix
- fonctionnalite
- documentation

Resume:
- Correction de l'ecrasement des nouveaux profils: `PlaylistProfile` porte maintenant un avatar couleur et la sauvegarde n'active plus automatiquement un profil existant.
- Ajout de l'isolation Room par `profileId` pour catalogue IPTV, sync_state, favoris, historiques/reprises, tendances Home, cache preview Home et mapping Xtream -> TMDB.
- Le changement de profil invalide les caches memoire et recharge les ecrans via `catalogRevision`, sans vider Room ni resynchroniser si le catalogue du profil est deja local.
- Info compte remplace le popup detail profil par un panneau de cartes source; OK sur la ligne selectionne, le toggle ON/OFF focusable active le profil.
- Ajout de `ProfilePickerScreen` apres splash et avant Home pour choisir le profil a regarder.

Verification:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain` : succes.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/decisions/2026-07-09-multi-profile-room-isolation.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/core/config/XtreamAccountManager.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/SVDatabase.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/*`
- `app/src/main/java/com/smartvision/svplayer/data/repository/*`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfilePickerScreen.kt`

# 2026-07-09 - Correction affichage catalogue apres activation d'un profil

Type:
- bugfix
- documentation

Resume:
- Ajout de `CatalogRepository.catalogRevision` pour notifier Live TV / Films / Series apres nettoyage catalogue ou synchronisation reussie.
- Les ViewModels Live TV, Films et Series rechargent leurs categories/items quand la revision catalogue change, afin de ne plus garder une ancienne playlist en memoire apres changement de profil.
- Films et Series ouvrent maintenant `ALL` ou une categorie avec contenu quand l'historique est vide, cas frequent apres activation d'un nouveau profil.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/domain/repository/CatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesViewModel.kt`

# 2026-07-09 - Multi-profils playlist locaux et correction compilation player

Type:
- bugfix
- fonctionnalite
- documentation

Resume:
- Correction compilation `FullScreenPlayerScreen.kt` apres extraction de l'overlay Live: imports `focusRequester`, `onFocusChanged` et `WbSunny`.
- Ajout de profils playlist locaux dans `XtreamAccountManager`, migration douce des anciennes sources vers `Profil principal`, selection active et projection vers les anciens flows Xtream/M3U/EPG.
- Ajout de la section `Profils` dans Info compte avec ajout, detail, activation, modification, synchronisation et suppression confirmee.
- Nettoyage du catalogue Room local avant resynchronisation lors d'un changement de profil pour eviter d'afficher l'ancien catalogue sous une nouvelle source.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/core/config/XtreamAccountManager.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/domain/repository/CatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`

## 2026-07-10 - Selection profil instantanee et progression catalogue par section

- Who's watching n'execute plus la synchronisation catalogue avant Home: la selection active le profil, invalide les caches memoire et ouvre Home immediatement.
- Home force la premiere synchronisation d'un profil sans catalogue local, meme en frequence manuelle/jamais, et conserve la progression classique sur les cards Live TV / Films / Series.
- Un nouveau profil `PlaylistWeb` ne remplace plus automatiquement le profil actif existant; les profils deja synchronises restent cache-first lors des changements.
- La synchronisation Xtream ne garde plus une transaction Room ouverte pendant les appels reseau. Les remplacements locaux sont transactionnels par section et publient une progression par lots importes.
- Info compte > Appareil et catalogue et le popup de synchronisation affichent des cards Live TV / Films / Series avec compteur, phase, pourcentage, barre et revelation visuelle progressive.
## 2026-07-10 - Restauration focus player et fluidite Home

- Retour player: Live TV restaure la chaine ouverte sans repasser par Categories; Movies et Series restaurent la ligne source par identifiant et scroll local protege.
- Navigation: Settings/Profile/Notifications restaurent leur controle d'origine dans le header Home.
- Home: hero texte-only non focusable, marge haute stabilisee, luminosite relevee et cards Xtream sans agrandissement au focus.
- Performance: suppression du re-ancrage horizontal anime a chaque card et creation de l'animation infinie GoldSweep uniquement pour ce style.
- QA: `compileReleaseKotlin` et `assembleRelease` valides; APK installe uniquement sur `emulator-5554`. Retour Live et Movies, focus Settings et hero Home verifies visuellement sans crash. Series compilee et parcourue, mais aucun episode du premier item test n'etait disponible pour ouvrir le player.

## 2026-07-13 - Progression Kids, Home, focus, filtres, tris et backend Playlist

- Corrige le HTTP 500 de session Playlist cause par la reinsertion d'un `token_hash` unique.
- Ajoute la progression Kids reelle par lots et les compteurs partages Home/Info compte.
- Stabilise scroll/focus Parametres, pastille parentale et focus Who's Watching.
- Finalise les cartes Home 16:9, overlays, premiere frame, audio progressif et segments Tendances.
- Integre l'icone AR fournie et les tris stables D-pad Live TV/Films/Series.
## 2026-07-14 - Popup PIN TV compact et navigation D-pad

- `NumericPinDialog` suit le modele vertical compact: en-tete centre, indicateurs PIN circulaires, separateur cyan, clavier 3 x 4 centre et actions de largeur egale.
- Les dimensions sont centralisees; le quatrieme chiffre focalise `Apply`, et chaque erreur vide le PIN, rejoue un shake court sur les seuls indicateurs puis restaure le focus sur `1`.

## 2026-07-17 - Metadonnees des lignes Films et saisons Series

- Films enrichit toutes les lignes chargees de tous les dossiers avec TMDB, applique le backdrop paysage aux vignettes et aligne l'ordre note/genre/details sur les lignes Series.
- La preview Films reutilise les metadonnees enrichies de la ligne selectionnee; les annulations normales de changement de dossier ne produisent plus d'erreur fugace au centre.
- Series stocke `number_of_seasons` dans le cache TMDB Room v19 et affiche le nombre de saisons a droite du titre de chaque ligne enrichie.
- Home separe durablement le header du viewport par un espace non scrollable de `24 dp` et clippe le contenu vertical, afin que le hero ne puisse plus passer sous le header pendant les restaurations de scroll/focus.
- Les rangees Historique et Tendances portent leur padding horizontal interne a `20 dp`, ce qui laisse dessiner entierement le halo de la premiere card sans perdre les cinq cards paysage visibles.
## 2026-07-20 - Header principal PNG transparents

- Remplace les glyphes Canvas du header principal par six PNG `header_icon_*` regeneres et detoures en alpha reel.
- Supprime le rail lumineux sous les onglets centraux.
- Retablit le focus standard `tvFocusTarget` de l'application sur les onglets.
- Agrandit les icones a `44 dp` hors focus; au focus/selection, l'icone passe a `32 dp` pour afficher le libelle sans augmenter la hauteur du header.

## 2026-07-21 - Films/Series fullscreen direct et skeleton VOD

- Films et Series n'envoient plus les bounds du mini-player Preview vers le player fullscreen; l'ouverture VOD devient directe, sans animation mini-player vers fullscreen.
- Home > Continue Watching force aussi les titres `SERIE` sur une seule ligne avec ellipsis.
- Le skeleton VOD corrige la coupure en bas du panneau droit Preview en supprimant l'espacement apres le dernier placeholder.

## 2026-07-21 - Resilience des dossiers catalogue

- Protege les dossiers Room Live TV, Films et Series contre un snapshot de categories Xtream vide ou inexploitable lorsque les medias de la section sont encore presents.
- Traite un catalogue avec medias sans dossiers comme incomplet afin de relancer la synchronisation automatique et reparer les profils deja affectes.

## 2026-07-22 - Snapshot catalogue Xtream atomique

- Live TV, Films et Series construisent et valident le snapshot categories + medias avant toute mutation Room, puis remplacent les deux tables dans une transaction unique par section.

## 2026-07-22 - Telemetrie diagnostic categories

- Ajout du tag Logcat sanitise `SVCatalogCategories`: lecture cache/Room, ecriture atomique, rejet de snapshot et projection ViewModel des categories Live TV, Films et Series.
- Les compteurs permettent de localiser sans exposer de donnees fournisseur une disparition entre persistance, cache, filtrage parental/dossiers speciaux et liste rendue.
- Extension aux groupes plateformes VOD et a la pagination `ALL`, afin de verifier que les sous-categories regroupes et l'ordre des elements suivent la liste de dossiers reelle.
- Une relecture immediate de l'endpoint categories est tentee lorsqu'il ne fournit aucun identifiant exploitable; si le snapshot reste invalide, l'import est annule, les donnees locales coherentes sont conservees et le statut de synchronisation signale l'erreur precise.
- Le tri `ALL`, les dossiers marques Films/Series et la regle de masquage des Favoris/Historique vides ne sont pas modifies.

## 2026-07-22 - Restauration des dossiers categories

- Correction du recepteur Kotlin dans `withSpecialCategories` Live TV, Films et Series : les categories autorisees etaient bien lues depuis Room, mais `filterNot` etait appele dans `buildList` et reutilisait le builder deja compose des seuls dossiers speciaux.
- Les groupes plateformes et le tri `ALL` retrouvent ainsi la liste complete de categories fournisseurs; la telemetrie conserve les compteurs de validation sur appareil.
## 2026-07-22 - Header navigation 2D and Media relocation

- Header tabs now use flat, labelled 2D cards with a unified filled-icon family; YouTube retains its original red icon.
- The existing right action controls and shared focus style are preserved in the order Notifications, Profile, Settings.
- Media was removed from the header and added to the Profile left menu between Synchronization and Help.
## 2026-07-22 - Header compact alignment corrections

- Central navigation cards now use the same `38 dp` visual height as the global header action buttons and wrap their icon plus label instead of using a fixed width.
- Central icons match the unfocused global-action gray and YouTube retains its original red artwork; the active-route blue underline was removed.
- The global action order is Notifications, Settings, Profile, with the existing shared focus behavior preserved.
## 2026-07-24 - Alignement visuel Activation / Xtream

- Ancrage des logos Activation et Xtream dans une colonne gauche de hauteur fixe.
- Uniformisation du voile splash et de l opacite de la carte Xtream avec l activation.
- Ajout d une icone vectorielle d entree au bouton principal Xtream sans modifier son focus ni ses dimensions.
