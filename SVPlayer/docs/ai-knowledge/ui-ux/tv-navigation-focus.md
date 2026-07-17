# UI TV, Focus et Navigation Telecommande

Derniere mise a jour: 2026-07-17.

## Profils Kids et dialogues

- Le picker utilise des cles `profile.id` stables dans une `LazyRow`. Le focus initial cible le profil actif, sinon ADMIN, sinon le premier profil; Compose revele automatiquement les cartes hors ecran.
- Les actions verrouillees ouvrent un dialogue PIN a quatre chiffres; annuler retourne au picker sans navigation anticipee.
- Tous les flux Create/Enter/Confirm PIN reutilisent `NumericPinDialog`: popup vertical compact centre, grille 3 x 4 a dimensions fixes, focus initial sur `1`, focus automatique sur `Apply` au quatrieme chiffre et shake rejouable limite aux quatre indicateurs en cas de refus. La derniere ligne route D-pad Bas vers les actions et les actions remontent vers la derniere ligne du clavier.
- Le formulaire profil est borne en hauteur, scrollable, applique `imePadding` et demande `bringIntoView()` lorsque le clavier TV ouvre un champ.
- Le formulaire partage depuis le picker et Info compte une mise en page TV compacte: nom et avatars en tete, sources Xtream/M3U fermees par defaut dans un accordeon exclusif, contenu scrollable et footer fixe. Depuis une source fermee ou le dernier champ visible, D-pad Bas cible `Enregistrer`.
- La rangee d'avatars est horizontale et scrollable au focus; ADMIN accepte son avatar dedie et tous les avatars CLASSIC.
- La selection de profil garde une validation visuelle courte sur la carte puis active le profil et purge les caches memoire. Le picker ne lance aucune synchronisation reseau; Home en reste proprietaire apres son rendu.
- Sur une carte de profil reelle, Bas cible le crayon discret et Haut retourne a la carte. La fermeture du PIN ou du formulaire redemande le focus a la cible d'origine avec un `FocusRequester` stable.
- Dans `Info profil`, Droite depuis le menu entre sur `Changer de profil`; Bas relie cette action a `Synchroniser ce profil`, Haut fait le chemin inverse et Gauche revient au menu. Le focus d'une carte du selecteur ne change jamais le profil actif: seul OK appelle l'activation.
- Dans `Gerer les profils`, Gauche/Droite parcourt la `LazyRow`; Bas ou OK sur un profil selectionne la cible d'administration puis focalise `Modifier`. Haut depuis les actions retourne a la carte. Actif, selection detail et focus sont trois etats independants, et toute fermeture de dialogue redemande la carte d'origine ou son voisin apres suppression.
- Dans `Controle parental`, Droite depuis le menu cible Activation; Droite ou OK entre dans la section et focalise le toggle global. OK/Enter modifie les toggles global et par profil, et Gauche depuis le global revient au cadre Activation puis au menu.

## 1. Objectif

Documenter les regles UI/UX Android TV, le focus Compose, la navigation D-pad et les contraintes visuelles pour eviter les regressions.

## 2. Fonctionnement actuel

L'interface est Compose native, optimisee TV: fond sombre, grands composants, focus visible, navigation au D-pad. Les surfaces actives utilisent principalement `ui/*`, avec des composants partages sous `ui/components`, `ui/focus` et certains elements historiques sous `core/designsystem`.

Le focus global est derive des settings utilisateur:
- style;
- couleur;
- effet;
- background.

`AppNavigation.kt` construit un `TvFocusStyle` et le propage aux ecrans.

## 3. Workflow utilisateur

- L'utilisateur navigue avec la telecommande.
- Les actions doivent etre focusables et pas seulement clickables.
- Home, Live TV, Movies, Series, Media, YouTube, Settings, Profile et Notifications partagent une logique de header.
- En profil Kids, les headers Home/Live TV et catalogue Movies/Series/YouTube masquent les actions Licence/Premium, Notifications et Parametres; l'avatar de profil et la date/heure restent visibles.
- Le header principal affiche aussi une date/heure non focusable a droite (`HH:mm:ss` puis `dd/MM/yyyy`), separee des boutons globaux par un trait vertical discret.
- Les formulaires doivent permettre Haut/Bas entre champs.
- Les popups doivent etre navigables au D-pad.
- Controle parental utilise un focus a deux niveaux: Activation, puis Mots-cles, puis Resultats. `Change PIN` est le seul bouton du header droit. OK sur Activation entre dans la section sans changer l'etat; le toggle global applique ON/OFF, puis Bas entre dans la rangee horizontale des toggles profils. Haut revient au toggle global puis au bouton PIN; Gauche/Droite parcourt les profils. Les toggles profils restent visibles mais non focusables quand le global est OFF. Back revient d'abord a la carte, puis au bouton Controle parental du menu gauche.

## 4. Workflow technique

Fichiers centraux:
- `ui/focus/TvFocusStyle.kt`
- `ui/focus/FocusModifiers.kt`
- `ui/components/TvButton.kt`
- `ui/components/TvCard.kt`
- `ui/components/TvHeader.kt`
- `ui/home/TvHeader.kt`
- `ui/navigation/AppNavigation.kt`
- `domain/model/Models.kt` pour `PlayerSettings`
- `data/repository/DefaultSettingsRepository.kt`

Attention:
- Un `FocusRequester` peut crasher si demande de focus avant initialisation ou apres disparition du composable.
- Dans Controle parental, le focus d'une carte declenche `LazyListState.animateScrollToItem()` dans la seule zone droite. Cette `LazyColumn` garde un padding bas afin de rendre la section Resultats entierement visible; ses deux listes internes scrollent et paginent pres de leur fin. Resultats entre sur le premier dossier disponible, sinon le premier element. Apres suppression d'un mot-cle, le focus cible le suivant, le precedent, ou le champ si la liste devient vide.
- Ne pas affecter `left`/`right`/`up`/`down` dans `focusProperties` vers un `FocusRequester` porte par un item `LazyColumn`/`LazyVerticalGrid` qui peut ne pas etre compose. Preferer la recherche spatiale Compose ou un routage explicite par evenement avec `runCatching`.
- Sur Home, D-pad bas depuis Live TV / Movies / Series doit cibler le premier item de la prochaine ligne disponible; D-pad bas depuis Continue watching cible le premier item `Trending movies`; D-pad bas depuis `Trending movies` cible le premier item `Trending series`.
- Sur Home, ce routage D-pad bas/haut annule le job vertical precedent, remet la `LazyRow` cible a l'index `0`, execute un unique `ScrollState.animateScrollTo()` vers une position calculee, puis demande le focus sur le premier item apres la fin du scroll. Ne pas reutiliser `BringIntoViewRequester` pour ces transitions verticales.
- Sur Home, le focus horizontal dans Continue watching / Trending movies / Trending series doit ancrer l'item focus en premiere position visible via `LazyListState.animateScrollToItem(index)` tant qu'il reste assez d'items a droite; en fin de liste, utiliser le dernier premier index utile pour eviter un blanc a droite.
- Sur Home, Continue watching et Tendances sont toujours en 16:9, avec une base `220,4 x 124 dp` qui garde au moins cinq cards visibles sur une surface TV 1280 px. Le focus applique un scale conteneur `1.04` sans animation de largeur; le preview Xtream partage demarre apres `550 ms` de focus stable. Si le focus part avant, aucun player ni appel de preparation n'est cree. Sa surface Media3 est un `TextureView` pour respecter le clipping Compose sur les anciennes versions Android TV.
- Sur Home, ne pas restaurer un ancien `LazyListState` sauvegarde pour les lignes tendances: chaque nouveau jeu d'items doit partir a l'index `0`, sinon l'ouverture peut commencer sur un ancien item milieu de liste.
- Sur Home, les cibles D-pad doivent tenir compte des lignes réellement visibles. Si Continue watching ou une ligne Trending est vide, ne pas consommer Haut/Bas vers un `FocusRequester` absent.
- Les logs diagnostics Home focus utilisent le tag `SVHomeFocus`; les logs du handoff splash/MainActivity utilisent `SVStartup`.
- `MainActivity.dispatchKeyEvent()` absorbe par securite le crash Compose `FocusRequester is not initialized` pendant une recherche D-pad, mais cette protection ne doit pas remplacer un routage de focus propre.
- Depuis le 2026-07-06, `MainActivity.dispatchKeyEvent()` intercepte uniquement `KEYCODE_SETTINGS`, `KEYCODE_MENU` et `KEYCODE_MEDIA_TOP_MENU`: `ACTION_DOWN` est consomme sans action, `ACTION_UP` demande l'ouverture de Settings via `RemoteSettingsNavigation`, puis toutes les autres touches retournent a `super.dispatchKeyEvent(event)`.
- Les handlers D-pad doivent tenir compte des surfaces visibles.
- Depuis le 2026-07-10, le retour du player restaure explicitement la ligne de contenu source: Live TV attend que la chaine soit composee avant de la focaliser et neutralise le focus initial Categories; Movies et Series transportent respectivement le `streamId` ou `seriesId`, scrollent la liste vers l'item puis demandent son focus. Le retour Settings -> Home cible l'icone Settings du header; Profile et Notifications utilisent le meme contrat avec leur controle d'origine.
- Notifications initialise le focus sur `Mises a jour`. Haut depuis cette premiere entree cible explicitement l'icone Notifications du header et Bas revient a la categorie selectionnee. Droite entre dans la liste seulement si elle contient une notification; l'etat vide est informatif et non focusable. Gauche revient a la categorie selectionnee et Haut depuis la premiere carte atteint `Actualiser`. Apres ouverture, le focus vise la carte suivante, la precedente si necessaire, ou la categorie lorsque la liste devient vide.
- Le hero Home est strictement informatif: texte et pagination uniquement, sans CTA ni cible focusable. L'espace header/hero partage les `14 dp` du shell principal et tout retour cible vers le header remet le scroll Home en haut afin d'eviter une bordure hero masquee.
- Home, Live TV, Films, Series, Media, YouTube, Notifications, Profile et Settings reutilisent `ui/home/TvHeader.kt` dans un shell commun: `34 dp` horizontaux, `18 dp` verticaux et `14 dp` avant le contenu. Les `FocusRequester` propres a chaque ecran restent branches sur cette instance unique.
- Les trois cards Xtream Home gardent une taille stable au focus (`focusedScale = 1f`): leur cadre/glow commun exprime le focus sans recouvrir ni deplacer les autres elements.
- La navigation horizontale Continue watching/Tendances laisse `LazyRow` faire la revelation minimale du prochain enfant. Ne pas reintroduire un `animateScrollToItem(index)` a chaque changement de focus: il entrait en concurrence avec le mouvement D-pad et l'animation de largeur premium.
- `tvFocusTarget` ne doit creer l'animation infinie de balayage que lorsque `TvFocusEffect.GoldSweep` est actif. Les styles Frame/Neon ne doivent pas conserver une infinite transition par composant.
- `Info profil` desactive son bouton pendant `SyncStatus.Running`; le repository reste l'unique arbitre anti-double lancement. Back ferme d'abord les dialogues, puis `profile/manage` revient a `profile` et `profile` revient a Home sur l'avatar.
- Depuis le 2026-07-05, dans Live TV, D-pad gauche depuis une chaine revient explicitement vers le dossier ouvert/selectionne avec `runCatching` pour proteger les `FocusRequester`. La liste chaines ancre le focus autour de la 3e ligne via `LazyListState.animateScrollToItem(index - 2)` quand possible, avec marge haute pour eviter le clipping.
- Depuis le 2026-07-05, le header Apercu Live TV porte les actions `Regarder`, `Favori` et `Supprimer` en boutons carres icon-only. D-pad droite depuis une chaine cible ces actions d'apercu; les lignes EPG sous le mini-player sont elles-memes focusables et OK ouvre/ferme le detail en rideau.
- Depuis le 2026-07-06, `ui/home/TvHeader.kt` accepte un `FocusRequester` pour l'onglet courant et une cible D-pad bas vers le contenu. Live TV branche l'onglet `Live TV` et la categorie selectionnee: D-pad haut depuis la premiere ligne/action de contenu remonte vers l'onglet actif, et D-pad bas depuis le header revient uniquement aux categories. Les actions du header Apercu et les lignes EPG routent D-pad gauche vers la chaine selectionnee, avec fallback sur la premiere chaine composee.
- Depuis le 2026-07-06, Live TV evite les restaurations de focus directes vers des items Lazy potentiellement hors composition. Les transitions critiques header -> categories, categories -> chaines, chaines -> categories, chaines -> recherche -> header et apercu/EPG -> chaines passent par un scroll `LazyListState.scrollToItem(...)`, une attente de presence dans `visibleItemsInfo`, puis seulement un `requestFocus()` protege. `TvHeader` expose aussi un callback optionnel `onContentDown` pour laisser l'ecran restaurer lui-meme la bonne cible composee.
- Depuis le 2026-07-07, la liste Categories Live TV demande aussi le focus sur la categorie selectionnee uniquement apres avoir scrolle l'index cible et confirme sa presence visible, ce qui synchronise selection, focus et scroll lors de l'ouverture. Dans l'apercu Live TV, D-pad haut depuis la premiere ligne EPG cible maintenant le premier bouton du header Apercu (`Regarder`) au lieu de remonter directement au header principal; l'icone EPG a ete deplacee dans le titre de section et reste non focusable.
- Depuis le 2026-07-07, les fiches detail Films/Series ramenent les actions, saisons, episodes et etoiles de note dans la zone visible quand elles recoivent le focus via `BringIntoViewRequester`. Les `requestFocus()` initiaux des fiches detail sont proteges par delai court et `runCatching` pour eviter de cibler un composable sorti de composition.
- Depuis le 2026-07-07, les trois overlays fullscreen Live/Film/Serie partagent le meme style visuel: overlay sombre/gradient, icones utilitaires outline sans labels, focus discret bleu/blanc et slider luminosite integre a la place des icones. Le player plein ecran gere ses surfaces secondaires dans l'ordre Back attendu: panneau `Autres episodes`, slider luminosite et card `Episode suivant` se ferment avant la sortie du player. Quand le panneau episodes est ouvert, l'overlay principal n'est plus visible ni focusable. Depuis le 2026-07-08, l'overlay Live cible uniquement l'ordre `Favori`, `Luminosite`, `Parametres`, `Exit fullscreen`; `Record` et `Info` ne doivent pas etre ajoutes a ce bandeau sans nouvelle specification.
- Depuis le 2026-07-09, Movies et Series suivent le modele 3 colonnes de Live TV. La colonne gauche garde les categories sans champ recherche. La colonne centrale est une liste verticale paginee avec recherche contenu Room et lignes VOD 16:9. Le premier OK sur une ligne selectionne le contenu, lance le mini-preview a droite et garde le focus sur la ligne; un deuxieme OK sur la meme ligne ouvre le fullscreen. Le panneau Preview porte `Play`, `Details`, `Favori` en boutons carres icon-only; dans `Historique`, `Favori` est remplace par `Supprimer`. La suppression historique ne doit plus etre inline dans les lignes Movies/Series.
- Depuis le 2026-07-09, les chargements Movies/Series restent non focusables et affichent un skeleton equivalent au layout reel, comme Live TV. Quand aucun contenu n'est selectionne, le panneau Preview reste non focusable sauf actions visibles; en mode `FREE_WITH_ADS`, il peut lire une pub idle dans le mini-player et afficher le bloc Premium sous le player. La section detail sous le mini-player est focusable et scrollable au D-pad. Les fiches detail Films/Series scrollent explicitement en haut avant le focus initial: le focus cible `Regarder`/`Reprendre` sans `bringIntoView` initial, pas la premiere ligne episode, afin d'eviter un affichage partiel decale. Le fullscreen Films/Series garde les controles focusables autour de la progressbar, avec favori, luminosite, parametres et plein ecran dans le groupe utilitaire.
- Depuis le 2026-07-10, dans Movies/Series, D-pad bas depuis le header Preview cible le mini-player avant la section details, et D-pad haut depuis le mini-player revient au bouton `Play`. Le premier OK sur une ligne garde toujours le focus sur la ligne; seul D-pad droite/bas entre dans le panneau Preview.
- Depuis le 2026-07-10, l'overlay fullscreen Films/Series est separe du Live et du Media local: `VodFullscreenControlsOverlay` garde exactement l'ordre horizontal `Luminosite -> Precedent -> -10 s -> Lecture/Pause -> +10 s -> Suivant -> Sortie fullscreen`. Le focus initial et le retour apres auto-masquage ciblent Lecture/Pause; seul un halo bleu diffus se deplace, sans cercle, cadre, label ni changement de geometrie.
- Depuis le 2026-07-10, OK sur une categorie Live/Movies/Series focalise explicitement la premiere ligne apres chargement, sans la selectionner. Live ne lance plus automatiquement le mini-player; le premier OK contenu reste le seul declencheur de preview.
- Depuis le 2026-07-10, les roles visuels sont separes dans `TvFocusStyle`: `Focused`, `Selected`, `Active`, `Parent`. Les composants partages catalogue, boutons et overlays utilisent ces tokens; Settings > Personnalisation persiste les trois couleurs de role et affiche un apercu en direct.
- Depuis le 2026-07-06, quand la liste chaines Live TV est vide, D-pad droite depuis les categories cible le champ de recherche au lieu de tenter un focus sur une ligne chaine inexistante.
- Depuis le 2026-07-05, Live TV > Historique ne possede plus de bouton supprimer inline dans les lignes. La suppression passe uniquement par le header Apercu, garde le dialog de confirmation focusable, puis restaure la selection sur la chaine suivante visible, sinon precedente.
- Les mini-players Home Continue watching, Home Tendances et Live TV apercu ont un fade-in audio local: volume player `0f`, attente 1 seconde apres `play()`, puis montee a `1f` sur 1 seconde. Cette logique ne doit pas modifier le focus, le D-pad, le fallback video ni les routes plein ecran. Le job audio doit rester vivant jusqu'a disparition/changement du composable; ne pas remettre `volume = 0f` juste apres la premiere frame.
- Depuis le 2026-07-05, le player plein ecran Live TV a un overlay dedie. Depuis le 2026-07-08, D-pad haut/bas zappe chaine precedente/suivante uniquement quand l'overlay Live est masque; si le bandeau, le slider luminosite, EPG ou Parametres est visible, Haut/Bas reste dans la surface active et ne zappe pas. Gauche/droite navigue les boutons du bandeau bas (`Favori`, `Luminosite`, `Parametres`, `Exit fullscreen`); Back ferme d'abord les surfaces secondaires puis revient a la liste Live TV. A la sortie du fullscreen Live, la liste restaure le focus sur la derniere chaine ouverte, y compris apres zapping.
- Depuis le 2026-07-05, la route `Media` utilise `MediaCatalogHeader` et un ecran 3 zones connecte au stockage local. Les proportions sont alignees sur Live TV: zones gauche/liste/apercu en ratios `0.24 / 0.42 / 0.34`. Le panneau gauche est compact, sans hero premium ni sous-titre snapshot; `Telephone -> TV` et `TV -> Phone` sont des tuiles focusables visibles, sans bouton interne cache. Le panneau central ne montre plus les cards stats colorees ni la ligne `Folders`. Le panneau apercu lance un mini-player local ExoPlayer quand le fichier selectionne est une video, fond noir et remplissage type Live TV; `Lire`, `Renommer` et `Supprimer` restent sur la premiere rangee d'actions avec icones. `Supprimer` ouvre un dialog focusable et cible le fichier par id explicite.
- Depuis le 2026-07-07, la colonne gauche de `Media` est hierarchique: `Media local` est un parent expandable avec `All files`, `Recordings`, `Imports`, `Transfers`, puis les actions QR `Importer tel.` et `Exporter tel.` en lignes compactes; `Media prives` est une categorie principale separee avec sous-dossiers backend compacts. Le focus initial reste sur `Media local`, DPAD droite depuis la liste cible la preview seulement si un item local/prive est selectionne, et la liste privee garde les etats loading/disabled/empty sans essayer de focus sur un item absent.

## 5. Ecrans concernes

- Home
- Live TV
- Movies
- Series
- YouTube
- Media Center
- Settings
- Profile
- Notifications
- ConsentDialog
- ActivationScreen
- Xtream setup
- Player overlay
- Badge EPG Live TV

## 6. Fichiers de code concernes

- `app/src/main/java/com/smartvision/svplayer/ui/focus/*`
- `app/src/main/java/com/smartvision/svplayer/ui/components/*`
- `app/src/main/java/com/smartvision/svplayer/ui/home/*`
- `app/src/main/java/com/smartvision/svplayer/ui/live/*`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/*`
- `app/src/main/java/com/smartvision/svplayer/ui/series/*`
- `app/src/main/java/com/smartvision/svplayer/ui/media/*`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/*`
- `app/src/main/java/com/smartvision/svplayer/ui/settings/*`
- `app/src/main/java/com/smartvision/svplayer/ui/profile/*`
- `app/src/main/java/com/smartvision/svplayer/ui/appconfig/ConsentDialog.kt`

## 7. Donnees / API / Backend / Admin

Parametres locaux:
- `PlayerSettings.focusStyle`
- `PlayerSettings.focusColor`
- `PlayerSettings.focusEffect`
- `PlayerSettings.focusBackground`
- `PlayerSettings.language`
- parental settings si ecran Settings concerne.

Backend indirect:
- `api/app_config.php` peut bloquer certaines features.
- `api/notifications.php` alimente notifications.

## 8. Dependances

- Catalogue/playback pour les listes et player overlay.
- Activation/licence pour QR et popups premium.
- Monetisation/consentement pour ConsentDialog et locks.
- i18n via `ui/i18n/SmartVisionStrings.kt`.

## 9. Regles a ne pas casser

- Tout element actionnable doit rester focusable.
- Info compte doit donner le focus initial au menu `Licence SmartVision`; le panneau `Info compte` reutilise le header principal et ses boutons globaux.
- Les titres principaux Profile et Settings restent hors du cadre droit; ce changement de composition ne modifie aucun `FocusRequester`, ordre D-pad, toggle ou controle actionnable. Les sections sans en-tete conservent leurs cadres comme cibles visuelles non focusables.
- Home conserve un padding non scrollable de `12 dp` entre le header partage et le hero afin que le focus automatique ne puisse pas le supprimer. Les cards categories, Continue Watching et Tendances reutilisent le halo bleu `25 dp` de Who's Watching sans changer leur routage D-pad.
- Settings reutilise aussi le header principal; son focus initial cible le premier item du menu. Back depuis Settings ou Info compte doit naviguer vers Home et demander le focus sur l'onglet Home du header, pas revenir au dernier ecran catalogue.
- Dans Info compte, le contenu `Appareil et catalogue` integre en bas de `Info compte` est une cible focusable non cliquable pour permettre le scroll D-pad; le menu gauche dedie n'existe plus.
- Dans Profile > Synchronisation, DPAD droite depuis l'entree menu cible le toggle de demarrage. Les toggles routent gauche vers le menu et droite vers la rangee de frequence correspondante; la grille `24h / 48h / A chaque demarrage / Manuel / Jamais` utilise des cibles explicites pour eviter les sorties de panneau.
- Depuis Synchronisation, OK/Enter sur `Info profil` met a jour la destination Compose locale meme lorsque la route NavHost reste `profile`; le focus seul ne doit jamais etre confondu avec la selection active.
- L'uniformisation visuelle des sous-sections Profile/Settings reutilise une primitive Compose commune mais conserve les `FocusRequester`, `focusProperties`, handlers OK/Enter et zones scrollables propres a chaque ecran.
- Ne pas remplacer les controles natifs attendus par des overlays qui volent le focus sans demande explicite.
- Si un overlay custom YouTube est explicitement demande, le bandeau doit borner le focus gauche/droite avec routage DPAD explicite entre boutons; OK/Enter doit declencher l'action du bouton focus; Haut ou Back doit masquer le bandeau et rendre le focus au conteneur player. Le parent du bandeau ne doit pas consommer OK/Enter, et le WebView ne doit pas reprendre automatiquement le focus pendant l'affichage des controles Compose.
- Le bandeau YouTube custom ne doit pas s'afficher a l'ouverture du player: attendre le premier etat `PLAYING`, puis masquer completement le bandeau apres timeout meme si un bouton garde le focus.
- YouTube mini/fullscreen: DPAD Haut en fullscreen affiche le bouton retour seul si le bandeau est masque; le bouton retour doit disparaitre avec le meme timeout que le bandeau.
- YouTube mini-player: le header doit garder recherche, favoris et parametres focusables; DPAD Haut depuis la premiere suggestion va vers le bouton retour du header mini-player.
- Ne pas hardcoder un focus cyan si un style global existe.
- Ne pas auto-focus un bouton qui doit rester accessible par scroll D-pad.
- Ne pas changer l'ordre D-pad sans valider les surfaces adjacentes.
- Ne pas etendre l'interception globale Settings/Menu aux touches D-pad, OK, Back ou media: ces touches doivent rester gerees par les ecrans/player ou par Android via `super.dispatchKeyEvent(event)`.
- Les transitions entre une liste de categories et une liste/grille de contenus ne doivent pas pointer directement vers le premier item lazy via `focusProperties`; si l'item sort de composition, DPAD droite/gauche peut crasher.
- `LazyListState.awaitItemVisible()` est le helper partage Live/Movies/Series: scroller la cible, attendre sa composition, puis demander le focus.
- Live/Movies/Series suivent le meme contrat explicite `categories <-> liste centrale <-> preview`; Movies/Series restaurent le dernier item focusse et selectionnent la ligne avant d'entrer dans Preview.
- Media selectionne explicitement le fichier/item prive avant le transfert DPAD droite, puis rend DPAD gauche a la liste ou a la bibliotheque si la liste est vide.
- Live TV Categories: OK applique le filtre au premier clic. `All` reste premier, le filtre actif passe en deuxieme position avec cle stable, puis la categorie `All` filtree est selectionnee et recoit le focus. La fleche droite est supprimee; les chips gardent leur largeur et passent a `30.dp` de haut.
- Movies/Series 3 colonnes: DPAD droite depuis une ligne contenu peut cibler le bouton `Play` du panneau preview, mais le premier OK ne doit plus transferer automatiquement le focus vers `Play`; les restaurations initiales doivent rester protegees par `withFrameNanos`/delai court et `runCatching`.
- Le texte doit rester lisible a distance TV.
- Les boutons icones d'Info compte et de l'overlay player doivent rester focusables et conserver une cible D-pad suffisante meme si le rendu visuel est compact.
- Media prives: le dossier parent `Media prives` expand/collapse ses sous-categories administrables; DPAD droite depuis une categorie charge la liste, premier OK sur un item charge le player inline dans la section principale, second OK ouvre le plein ecran. Le champ recherche remplace refresh et doit rester focusable/cliquable pour ouvrir le clavier TV via OK/clic. Les lignes enfants sous `Media local` et `Media prives` doivent rester plus basses que leurs parents pour eviter la confusion visuelle. En mode prive, le rendu s'aligne sur YouTube: liste compacte au centre, player/miniature large a droite, WebView embed focusable au D-pad et controls natifs visibles pour HLS/MP4. Depuis le 2026-07-08, le wrapper Compose ne consomme pas OK/Enter sur un embed prive: il rend le focus au WebView puis laisse `PrivateMediaTvWebView` et les controles provider traiter la touche. Les focus Media utilisent `LocalTvFocusStyle`.

## 10. Problemes connus

- Uniformisation focus restante: le socle partage `TvFocusStyle` / `tvFocusTarget` couvre deja Home, catalogues, Media, YouTube, profil et plusieurs dialogs, mais des exceptions codent encore leur couleur/echelle localement. Les priorites de migration sont `detail/TmdbDetailRichContent.kt`, `detail/SeriesDetailScreen.kt`, `player/LivePlayerOverlay.kt`, `player/FullScreenPlayerScreen.kt` et certains controles `appconfig/ConsentDialog.kt`. Conserver les distinctions semantiques selection/courant/erreur, mais prendre `LocalTvFocusStyle.current.accent`, `background` et `borderWidth` pour l'etat focus.

- `FocusRequester is not initialized` est souvent lie a une cible sortie de composition.
- Les popups de consentement doivent garder le scroll en controle jusqu'au bas.
- Les suggestions YouTube peuvent rester visibles si suppression/focus non coordonnes.
- Les champs Firestick peuvent flicker si le buffer local n'est pas stabilise.

## 11. Quand lire ce fichier ?

Lire ce fichier si la demande concerne:
- focus;
- D-pad;
- telecommande;
- popup;
- consentement visuel;
- header;
- Settings fields;
- YouTube navigation;
- personnalisation du focus;
- traduction visible d'ecrans Compose.

Ne pas lire ce fichier si la demande concerne uniquement:
- endpoint PHP pur;
- build release sans UI;
- SQL admin sans impact ecran.

## 12. Historique court

- 2026-06-29: migration vers documentation specialisee.
- 2026-06-29: ajout des signaux `FocusRequester`, `D-pad`, `focusBackground`.
- 2026-06-30: ajout de la regle focus pour overlay YouTube custom demande explicitement: bandeau focusable borne, retour Haut/Back vers le player.
- 2026-06-30: renforcement overlay YouTube: routage manuel gauche/droite, OK/Enter intercepte par bouton, pas de bandeau haut sur mini-lecteur.
- 2026-06-30: correction overlay YouTube: suppression de l'interception OK/Enter par le parent et des reprises de focus automatiques du WebView en mode controle Compose.
- 2026-06-30: correction overlay YouTube: le conteneur player ne traite DPAD/OK que lorsqu'il a lui-meme le focus; si un bouton du bandeau est focus, les touches doivent etre laissees au bouton.
- 2026-06-30: amelioration overlay YouTube: affichage seulement au demarrage reel de lecture, auto-hide 7s, progress bar, bouton reload, previous via historique et retour full screen vers mini lecteur avec reprise de position.
- 2026-06-30: performance/UX YouTube: WebView conserve entre videos avec `loadVideoById`, queue autoplay maintenue autour de 20 suggestions, qualite cible `medium`, bouton retour fullscreen masque avec l'overlay et popup parametres sans stopper la lecture.
- 2026-06-30: ajout popup de synchronisation Xtream avec focus initial sur lancement, blocage telecommande pendant synchro active et retour focus vers Appareil et catalogue apres fermeture.
- 2026-07-01: Info compte ajoute une section URL EPG avec boutons icones focusables Modifier et Modifier par QR.
- 2026-07-01: Info compte devient l'ecran pilote visuel: header principal, menu gauche plus etroit, icones de lignes sans cadres, focus initial sur Licence SmartVision, et badge usage dans l'en-tete du panneau droit.
- 2026-07-05: ajout de la route/header `Media`, verrou/couronne via `media_center`, alignement du header Detail, puis ecran Media Center connecte au stockage local avec listes fichiers/dossiers focusables, panneau apercu et dialogues Renommer/Deplacer/Supprimer.
- 2026-07-05: Media Center ajoute la lecture locale video/audio/photo, un viewer photo plein ecran, un focus DPAD droite liste -> apercu, et un agencement plus compact; le header reduit encore les espacements et positionne la couronne au-dessus du label locked.
- 2026-07-05: Media Center ajoute Importer tel. et Exporter tel. sous gate `media_phone_transfer`. Ces actions ouvrent un dialog QR focusable; la session reseau local s'arrete a la fermeture du dialog ou a la destruction du ViewModel.
- 2026-07-13: Media Center selectionne d'abord la ligne lors de DPAD droite, attend l'etat selectionne compose puis cible la premiere action d'apercu; DPAD gauche restaure explicitement liste/bibliotheque.
- 2026-07-05: Media Center rend `Supprimer` accessible au meme niveau que `Lire` et `Renommer`; le dialog de suppression prend le focus TV et les actions fichier utilisent l'id du fichier du dialog.
- 2026-07-05: Media Center remplace le bouton lateral d'import telephone par un bloc focusable `Telephone -> TV`: `Recevoir fichier` quand le transfert est autorise, `Debloquer` avec message Premium/essai quand la feature `media_phone_transfer` est verrouillee.
- 2026-07-05: Media Center adopte un agencement premium: colonne gauche studio/compteurs/stockage/hub transfert, stats en tete de liste, lignes fichier avec badges/pills et panneau apercu hero.
- 2026-07-05: Media Center compact aligne sur Live TV: ratios `0.24 / 0.42 / 0.34`, suppression hero/snapshot/stats colorees, suppression de la ligne `Folders`, hubs `Telephone -> TV` et `TV -> Phone` en tuiles focusables, mini-player local video dans l'apercu avec remplissage du cadre, details fichier reduits, actions avec icones, et anti-flash couronne Media pendant `appConfigState.loading` comme YouTube.
- 2026-07-01: Info compte renforce l'approche icones focusables et l'overlay Live/Films/Series devient plus slim sans changer les actions lecteur.
- 2026-07-02: Home corrige le D-pad bas vers Continue watching / Trending movies / Trending series avec bring-into-view avant `requestFocus`.
- 2026-07-02: Home renforce le routage vertical avec reset horizontal vers le premier item, padding interne de `LazyRow` pour eviter le clipping gauche, et logs `SVHomeFocus`.
- 2026-07-02: Home demande maintenant le focus avant le recentrage vertical pour eviter que l'ecran scrolle avant l'arrivee visuelle du focus.
- 2026-07-02: Home remplace le recentrage vertical par `BringIntoViewRequester` par un scroll vertical deterministe annulable, focus apres scroll et hauteurs fixes Continue/Trending pour stopper les corrections continues liees a l'animation de largeur.
- 2026-07-02: Home ajoute l'ancrage horizontal des cards focussees avec `LazyListState.animateScrollToItem`, borne en fin de liste pour conserver les items restants visibles.
- 2026-07-03: Home Tendances separe `TrendingContentRow` de `ContinueWatchingRow`; les previews tendances sont declenchees apres focus stable `1,3s`, avec ancrage gauche et annulation propre au changement de focus.
- 2026-07-03: Live TV rend le panneau details EPG sous mini-player focusable et scrollable au D-pad, avec routage bas vers `Regarder`.
- 2026-07-05: Home Tendances aligne sa transition de focus visuelle sur Continue watching et les mini-players Home/Live TV ajoutent le fade-in audio 1s + 1s sans changer la navigation.
- 2026-07-05: correction du cycle de vie audio Home: les players Continue watching LiveImmediate et Tendances ne coupent plus le fade-in juste apres demarrage.
- 2026-07-05: remplacement et affinage de l'overlay plein ecran Live TV par un bandeau bas rectangulaire glassmorphism compact, boutons focusables, zapping haut/bas prioritaire, EPG lateral, panneau Settings aspect ratio enrichi et retour focus liste sur la chaine ouverte.
- 2026-07-05: le header principal ajoute date/heure non focusables a droite et compacte legerement logo, onglets et boutons icones sans changer le routage D-pad.
- 2026-07-05: Live TV compacte Categories/Chaines/Apercu, ancre le focus chaines autour de la 3e ligne, route D-pad gauche vers le dossier selectionne, deplace `Regarder`/`Favori`/`Supprimer` dans le header Apercu et remplace l'ancien bloc `En cours` par des lignes EPG focusables.
- 2026-07-07: Media prives ajoute sous-dossiers expandable sous le parent, recherche focusable dans la colonne liste, mini-player prive au premier OK et route fullscreen au second OK.
- 2026-07-07: Media deplace les actions QR `Importer tel.` / `Exporter tel.` sous `Media local` en lignes compactes; les sous-dossiers prives deviennent gerables depuis le backend et restent visuellement plus bas que leurs dossiers parents.
- 2026-07-07: Media corrige le champ recherche TV pour ouvrir le clavier, applique le style de focus parametre aux lignes Media, compacte le mini-player prive 16:9 et durcit le WebView prive Android TV.
- 2026-07-07: Media prives adopte un fonctionnement proche de YouTube: lancement video dans la section principale, player embed focusable, controls natifs Media3 visibles et overlay fullscreen bas.
- 2026-07-09: Info compte ajoute une section `Profils` dans le panneau `Info compte`: profil actif, liste focusable, ajout, detail, activation, modification, synchronisation et suppression avec confirmation. Les formulaires reutilisent les champs TV existants et le password est masque dans la fiche detail.
- 2026-07-09: apres changement de profil playlist, Live TV / Films / Series rechargent leurs donnees via `catalogRevision`. Films et Series n'ouvrent plus sur `Historique` vide quand un profil neuf vient d'etre synchronise; ils ciblent `ALL` ou une categorie avec contenu.
- 2026-07-09: Info compte separe selection et activation des profils. OK sur une ligne profil affiche ses infos dans le panneau de cartes source existant en bas, sans popup; le toggle ON/OFF a droite est focusable au D-pad et un seul profil peut etre ON. Les avatars profils affichent la premiere lettre sur une couleur stable.
- 2026-07-09: apres splash et avant Home, `ProfilePickerScreen` demande quel profil regarder quand des profils configures existent. OK sur un avatar active ce profil, reutilise son catalogue local s'il existe deja et lance la synchro seulement si le profil n'a pas encore de catalogue local.
- 2026-07-09: les lignes profil routent explicitement D-pad droite vers le toggle ON/OFF et D-pad gauche du toggle vers la ligne. Dans `ProfilePickerScreen`, le focus initial vise le profil actif et Add/Manage naviguent vers Info compte seulement apres sortie du picker pour eviter un crash de navigation avant `NavHost`.
- 2026-07-09: Movies/Series adoptent le layout 3 colonnes Live TV avec liste centrale, preview a droite, premier OK preview en gardant le focus ligne, deuxieme OK fullscreen, et suppression Historique dans le header Preview.
- 2026-07-10: `ProfilePickerScreen` supprime `Manage profiles`; `Add Profile` ouvre le formulaire profil directement, le stylo du nom ouvre le meme formulaire pre-rempli, et OK sur un profil affiche une progress bar jusqu'a Home. Le multi-profil est gatee par `multi_profile`: Premium et essai actif autorises, Free Ads verrouille avec couronne sur l'ajout.
- 2026-07-10: Info compte > Profils affiche/masque le detail par OK sur la ligne. L'avatar du header detail est focusable et ouvre un popup de 10 avatars standards; apres selection, le focus passe sur `Enregistrer`, puis le retour se fait vers la photo apres fermeture. Le nom se modifie via le stylo du header.
- 2026-07-10: Settings adopte le header principal, focus initial sur le premier menu, et Back Settings/Info compte retourne sur Home avec focus header Home. Info compte supprime le menu `Appareil et catalogue` mais garde son contenu focusable en bas du panneau. Movies/Series rendent le mini-player Preview focusable entre les boutons header et les details.
- 2026-07-11: les overlays fullscreen Films/Series et Live partagent le meme contrat D-pad. Bas masque l'overlay et rend le focus au `PlayerView`; Gauche/Droite parcourent uniquement les commandes actives; Haut cible la barre seulement si Media3 autorise le seek; les sliders progression/luminosite gardent un `FocusRequester` stable et le halo radial bleu commun. Quand l'overlay Live est masque, Haut/Bas conservent le zapping historique; quand il est visible, Bas ne zappe jamais.
- 2026-07-12: la rangee de filtres Live est compactee et sa fleche droite fait defiler le contenu. Le picker applique le fond Kids au focus d'un profil Kids et affiche un badge ourson PNG. Le header commun conserve date/heure en mode Kids. Le dialogue de sortie actif propose aussi `Change profile` / `Changer de profil`.
