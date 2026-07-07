# UI TV, Focus et Navigation Telecommande

Derniere mise a jour: 2026-07-07.

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
- Le header principal affiche aussi une date/heure non focusable a droite (`HH:mm:ss` puis `dd/MM/yyyy`), separee des boutons globaux par un trait vertical discret.
- Les formulaires doivent permettre Haut/Bas entre champs.
- Les popups doivent etre navigables au D-pad.

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
- Ne pas affecter `left`/`right`/`up`/`down` dans `focusProperties` vers un `FocusRequester` porte par un item `LazyColumn`/`LazyVerticalGrid` qui peut ne pas etre compose. Preferer la recherche spatiale Compose ou un routage explicite par evenement avec `runCatching`.
- Sur Home, D-pad bas depuis Live TV / Movies / Series doit cibler le premier item de la prochaine ligne disponible; D-pad bas depuis Continue watching cible le premier item `Trending movies`; D-pad bas depuis `Trending movies` cible le premier item `Trending series`.
- Sur Home, ce routage D-pad bas/haut annule le job vertical precedent, remet la `LazyRow` cible a l'index `0`, execute un unique `ScrollState.animateScrollTo()` vers une position calculee, puis demande le focus sur le premier item apres la fin du scroll. Ne pas reutiliser `BringIntoViewRequester` pour ces transitions verticales.
- Sur Home, le focus horizontal dans Continue watching / Trending movies / Trending series doit ancrer l'item focus en premiere position visible via `LazyListState.animateScrollToItem(index)` tant qu'il reste assez d'items a droite; en fin de liste, utiliser le dernier premier index utile pour eviter un blanc a droite.
- Sur Home, les lignes Tendances utilisent un composant dedie: la transformation 16:9 et le mini-preview ne demarrent qu'apres `1s` de focus stable. La transition visuelle de focus doit rester alignee sur Continue watching: scale `1.0f` et animation largeur courte via `SmartVisionDimensions.FocusAnimationMillis`. Si le focus change avant ce delai, la card reste portrait et aucun player n'est cree.
- Sur Home, ne pas restaurer un ancien `LazyListState` sauvegarde pour les lignes tendances: chaque nouveau jeu d'items doit partir a l'index `0`, sinon l'ouverture peut commencer sur un ancien item milieu de liste.
- Sur Home, les cibles D-pad doivent tenir compte des lignes réellement visibles. Si Continue watching ou une ligne Trending est vide, ne pas consommer Haut/Bas vers un `FocusRequester` absent.
- Les logs diagnostics Home focus utilisent le tag `SVHomeFocus`; les logs du handoff splash/MainActivity utilisent `SVStartup`.
- `MainActivity.dispatchKeyEvent()` absorbe par securite le crash Compose `FocusRequester is not initialized` pendant une recherche D-pad, mais cette protection ne doit pas remplacer un routage de focus propre.
- Depuis le 2026-07-06, `MainActivity.dispatchKeyEvent()` intercepte uniquement `KEYCODE_SETTINGS`, `KEYCODE_MENU` et `KEYCODE_MEDIA_TOP_MENU`: `ACTION_DOWN` est consomme sans action, `ACTION_UP` demande l'ouverture de Settings via `RemoteSettingsNavigation`, puis toutes les autres touches retournent a `super.dispatchKeyEvent(event)`.
- Les handlers D-pad doivent tenir compte des surfaces visibles.
- Le popup manuel Info compte > Synchroniser bloque Back/D-pad uniquement pendant `SyncStatus.Running`; a la fin ou en erreur, le focus va sur `Retour`.
- Depuis le 2026-07-05, dans Live TV, D-pad gauche depuis une chaine revient explicitement vers le dossier ouvert/selectionne avec `runCatching` pour proteger les `FocusRequester`. La liste chaines ancre le focus autour de la 3e ligne via `LazyListState.animateScrollToItem(index - 2)` quand possible, avec marge haute pour eviter le clipping.
- Depuis le 2026-07-05, le header Apercu Live TV porte les actions `Regarder`, `Favori` et `Supprimer` en boutons carres icon-only. D-pad droite depuis une chaine cible ces actions d'apercu; les lignes EPG sous le mini-player sont elles-memes focusables et OK ouvre/ferme le detail en rideau.
- Depuis le 2026-07-06, `ui/home/TvHeader.kt` accepte un `FocusRequester` pour l'onglet courant et une cible D-pad bas vers le contenu. Live TV branche l'onglet `Live TV` et la categorie selectionnee: D-pad haut depuis la premiere ligne/action de contenu remonte vers l'onglet actif, et D-pad bas depuis le header revient uniquement aux categories. Les actions du header Apercu et les lignes EPG routent D-pad gauche vers la chaine selectionnee, avec fallback sur la premiere chaine composee.
- Depuis le 2026-07-06, Live TV evite les restaurations de focus directes vers des items Lazy potentiellement hors composition. Les transitions critiques header -> categories, categories -> chaines, chaines -> categories, chaines -> recherche -> header et apercu/EPG -> chaines passent par un scroll `LazyListState.scrollToItem(...)`, une attente de presence dans `visibleItemsInfo`, puis seulement un `requestFocus()` protege. `TvHeader` expose aussi un callback optionnel `onContentDown` pour laisser l'ecran restaurer lui-meme la bonne cible composee.
- Depuis le 2026-07-07, la liste Categories Live TV demande aussi le focus sur la categorie selectionnee uniquement apres avoir scrolle l'index cible et confirme sa presence visible, ce qui synchronise selection, focus et scroll lors de l'ouverture. Dans l'apercu Live TV, D-pad haut depuis la premiere ligne EPG cible maintenant le premier bouton du header Apercu (`Regarder`) au lieu de remonter directement au header principal; l'icone EPG a ete deplacee dans le titre de section et reste non focusable.
- Depuis le 2026-07-07, les fiches detail Films/Series ramenent les actions, saisons, episodes et etoiles de note dans la zone visible quand elles recoivent le focus via `BringIntoViewRequester`. Les `requestFocus()` initiaux des fiches detail sont proteges par delai court et `runCatching` pour eviter de cibler un composable sorti de composition.
- Depuis le 2026-07-07, les trois overlays fullscreen Live/Film/Serie partagent le meme style visuel: overlay sombre/gradient, icones utilitaires outline sans labels, focus discret bleu/blanc et slider luminosite integre a la place des icones. Le player plein ecran gere ses surfaces secondaires dans l'ordre Back attendu: panneau `Autres episodes`, slider luminosite et card `Episode suivant` se ferment avant la sortie du player. Quand le panneau episodes est ouvert, l'overlay principal n'est plus visible ni focusable.
- Depuis le 2026-07-06, quand la liste chaines Live TV est vide, D-pad droite depuis les categories cible le champ de recherche au lieu de tenter un focus sur une ligne chaine inexistante.
- Depuis le 2026-07-05, Live TV > Historique ne possede plus de bouton supprimer inline dans les lignes. La suppression passe uniquement par le header Apercu, garde le dialog de confirmation focusable, puis restaure la selection sur la chaine suivante visible, sinon precedente.
- Les mini-players Home Continue watching, Home Tendances et Live TV apercu ont un fade-in audio local: volume player `0f`, attente 1 seconde apres `play()`, puis montee a `1f` sur 1 seconde. Cette logique ne doit pas modifier le focus, le D-pad, le fallback video ni les routes plein ecran. Le job audio doit rester vivant jusqu'a disparition/changement du composable; ne pas remettre `volume = 0f` juste apres la premiere frame.
- Depuis le 2026-07-05, le player plein ecran Live TV a un overlay dedie: D-pad haut/bas zappe chaine precedente/suivante uniquement quand aucun panneau Live n'est ouvert; si EPG ou Settings est ouvert, le panneau garde la priorite de navigation/focus. Gauche/droite navigue les boutons du bandeau bas (`EPG`, `Settings`, `Record`, `Back to List`); Back ferme d'abord EPG/Settings puis revient a la liste Live TV. A la sortie du fullscreen Live, la liste restaure le focus sur la derniere chaine ouverte, y compris apres zapping.
- Depuis le 2026-07-05, la route `Media` utilise `MediaCatalogHeader` et un ecran 3 zones connecte au stockage local. Les proportions sont alignees sur Live TV: zones gauche/liste/apercu en ratios `0.24 / 0.42 / 0.34`. Le panneau gauche est compact, sans hero premium ni sous-titre snapshot; `Telephone -> TV` et `TV -> Phone` sont des tuiles focusables visibles, sans bouton interne cache. Le panneau central ne montre plus les cards stats colorees ni la ligne `Folders`. Le panneau apercu lance un mini-player local ExoPlayer quand le fichier selectionne est une video, fond noir et remplissage type Live TV; `Lire`, `Renommer` et `Supprimer` restent sur la premiere rangee d'actions avec icones. `Supprimer` ouvre un dialog focusable et cible le fichier par id explicite.
- Depuis le 2026-07-07, la colonne gauche de `Media` est hierarchique: `Media local` est un parent expandable avec `All files`, `Recordings`, `Imports`, `Transfers`; `Media prives` est une categorie principale separee. Le focus initial reste sur `Media local`, DPAD droite depuis la liste cible la preview seulement si un item local/prive est selectionne, et la liste privee garde les etats loading/disabled/empty sans essayer de focus sur un item absent.

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
- Le texte doit rester lisible a distance TV.
- Les boutons icones d'Info compte et de l'overlay player doivent rester focusables et conserver une cible D-pad suffisante meme si le rendu visuel est compact.

## 10. Problemes connus

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
- 2026-07-05: stabilisation Media Center Lot 14: DPAD droite depuis la liste ne demande l'apercu que si un fichier est selectionne, et le focus cible le premier bouton d'apercu reellement actif (`Lire`, `Renommer` ou `Exporter tel.`). Les etats import/export telephone affichent une preparation visible.
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
