# UI TV, Focus et Navigation Telecommande

Derniere mise a jour: 2026-06-30.

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
- Home, Live TV, Movies, Series, YouTube, Settings, Profile et Notifications partagent une logique de header.
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
- Les handlers D-pad doivent tenir compte des surfaces visibles.
- Le popup manuel Profil > Identifiants Xtream bloque Back/D-pad uniquement pendant `SyncStatus.Running`; a la fin ou en erreur, le focus va sur `Retour`.

## 5. Ecrans concernes

- Home
- Live TV
- Movies
- Series
- YouTube
- Settings
- Profile
- Notifications
- ConsentDialog
- ActivationScreen
- Xtream setup
- Player overlay

## 6. Fichiers de code concernes

- `app/src/main/java/com/smartvision/svplayer/ui/focus/*`
- `app/src/main/java/com/smartvision/svplayer/ui/components/*`
- `app/src/main/java/com/smartvision/svplayer/ui/home/*`
- `app/src/main/java/com/smartvision/svplayer/ui/live/*`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/*`
- `app/src/main/java/com/smartvision/svplayer/ui/series/*`
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
- Ne pas remplacer les controles natifs attendus par des overlays qui volent le focus sans demande explicite.
- Si un overlay custom YouTube est explicitement demande, le bandeau doit borner le focus gauche/droite avec routage DPAD explicite entre boutons; OK/Enter doit declencher l'action du bouton focus; Haut ou Back doit masquer le bandeau et rendre le focus au conteneur player. Le parent du bandeau ne doit pas consommer OK/Enter, et le WebView ne doit pas reprendre automatiquement le focus pendant l'affichage des controles Compose.
- Le bandeau YouTube custom ne doit pas s'afficher a l'ouverture du player: attendre le premier etat `PLAYING`, puis masquer completement le bandeau apres timeout meme si un bouton garde le focus.
- YouTube mini/fullscreen: DPAD Haut en fullscreen affiche le bouton retour seul si le bandeau est masque; le bouton retour doit disparaitre avec le meme timeout que le bandeau.
- YouTube mini-player: le header doit garder recherche, favoris et parametres focusables; DPAD Haut depuis la premiere suggestion va vers le bouton retour du header mini-player.
- Ne pas hardcoder un focus cyan si un style global existe.
- Ne pas auto-focus un bouton qui doit rester accessible par scroll D-pad.
- Ne pas changer l'ordre D-pad sans valider les surfaces adjacentes.
- Le texte doit rester lisible a distance TV.

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
