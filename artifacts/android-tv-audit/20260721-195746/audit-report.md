# Audit general approfondi SmartVision sur Fire TV

Audit en lecture seule de la version installee 0.1.120 (215). Douze anomalies reproductibles ont ete retenues: neuf majeures et trois mineures. Aucun crash ni ANR n'a ete observe, mais les categories fournisseur sont absentes dans les trois catalogues, un film incompatible reste bloque sur un spinner, la navigation Live mesuree est fortement janky et plusieurs retours D-pad ne restaurent pas le bon contexte.

## Contexte du test

| Champ | Valeur |
|---|---|
| Date | 2026-07-21 |
| Device | Amazon Fire TV AFTSSS (identifiant ADB masque) |
| Android | Android 9 / API 28 |
| Display | 1920x1080, densite 320 dpi |
| Package | com.smartvision.svplayer |
| Installed Version | 0.1.120 (215) |
| Method | ADB, captures, logcat, dumpsys gfxinfo/meminfo, lecture source ciblee |
| Safety | Aucun changement de code, build, installation, deploiement ou mutation volontaire des donnees |

## Synthèse

| Gravité | Nombre |
|---|---:|
| Bloquant | 0 |
| Critique | 0 |
| Majeur | 9 |
| Mineur | 3 |
| Amélioration | 0 |

## Couverture

| Écran/parcours | État | Notes |
|---|---|---|
| Demarrage / selecteur de profil | partiel | 3 lancements forces mesures; selecteur visible a 8 s; selection du profil existant validee. Creation/edition non soumise. |
| Home / header | couvert | Chargement, compteurs, hero, cartes, retour header/contenu, boucle horizontale et etats visuels. |
| Live TV | couvert coeur | Liste, apercu, plein ecran, overlay, Back, historique, recherche ouverte/fermee. Favori et filtres persistants non modifies. |
| Movies | partiel | Liste, apercu, detail, lancement plein ecran et erreur lecteur sur un film. Echantillon contenu limite a un film. |
| Series | partiel | Liste, apercu, detail, saison, dialogue reprise, lecture episode et retour. Echantillon limite a une serie/episode. |
| Media Center | partiel bloque | Navigation des zones et etat vide valides. Lecture, import, export, renommage, deplacement et suppression bloques par l'absence de fichiers. |
| YouTube | couvert coeur | Grille, lancement, chargement, overlay, lecture et retour valides sur une video. |
| Settings | couvert lecture seule | Licence, preferences, personnalisation, mises a jour et donnees locales parcourues sans changer d'option ni lancer d'action destructive. |
| Notifications | couvert etat vide | Quatre categories et retour testes; aucune notification disponible pour tester ouverture, marquage ou purge. |
| Profil / gestion / controle parental | partiel | Navigation, gestion et dialogue PIN testes sans saisir de PIN ni modifier, verrouiller ou supprimer un profil. |
| Activation / onboarding Xtream / consentement | non atteint | Session deja active; deconnexion ou effacement non autorises dans cet audit. |
| Mode Kids / changement de profil | non execute | Evite pour ne pas changer le profil actif ni declencher de synchronisation persistante. |
| Offline / reseau degrade / endurance | non execute | Pas de coupure reseau ni de campagne longue afin de ne pas modifier l'environnement du Fire TV. |

## Backlog priorisé

| ID | Gravité | Catégorie | Écran | Problème |
|---|---|---|---|---|
| [TV-I18N-001](#tv-i18n-001-la-langue-anglaise-laisse-de-nombreux-libelles-francais-dans-linterface) | majeur | internationalisation | Global | La langue anglaise laisse de nombreux libelles francais dans l'interface |
| [TV-NAV-004](#tv-nav-004-haut-depuis-un-controle-superieur-live-saute-vers-youtube-au-lieu-de-longlet-live) | majeur | navigation-focus | Live TV | Haut depuis un controle superieur Live saute vers YouTube au lieu de l'onglet Live |
| [TV-FUNC-001](#tv-func-001-les-categories-fournisseur-disparaissent-dans-les-trois-catalogues) | majeur | fonctionnel | Live TV / Movies / Series | Les categories fournisseur disparaissent dans les trois catalogues |
| [TV-PERF-001](#tv-perf-001-la-navigation-live-presente-un-jank-massif-et-une-forte-charge-sur-le-fire-tv) | majeur | performance | Live TV avec apercu actif | La navigation Live presente un jank massif et une forte charge sur le Fire TV |
| [TV-PLAY-001](#tv-play-001-une-erreur-decodeursource-laisse-le-lecteur-sur-un-spinner-noir-sans-message) | majeur | lecture | Movies / lecteur plein ecran | Une erreur decodeur/source laisse le lecteur sur un spinner noir sans message |
| [TV-NAV-001](#tv-nav-001-les-backhandler-imbriques-rendent-le-retour-non-deterministe) | majeur | navigation-focus | Notifications / Settings / navigation globale | Les BackHandler imbriques rendent le retour non deterministe |
| [TV-PROFILE-001](#tv-profile-001-depuis-add-kids-profile-bas-cible-edit-sur-le-profil-precedent) | majeur | navigation-focus | Profil > Manage profiles | Depuis Add kids profile, Bas cible Edit sur le profil precedent |
| [TV-NAV-002](#tv-nav-002-le-retour-dun-episode-restaure-la-saison-au-lieu-de-lepisode-lance) | majeur | navigation-focus | Series detail / episode player | Le retour d'un episode restaure la saison au lieu de l'episode lance |
| [TV-NAV-003](#tv-nav-003-back-du-lecteur-youtube-perd-la-video-source-et-remet-le-focus-sur-la-categorie) | majeur | navigation-focus | YouTube | Back du lecteur YouTube perd la video source et remet le focus sur la categorie |
| [TV-STAB-001](#tv-stab-001-android-9-journalise-des-echecs-repetes-autour-de-javalangclassvalue) | mineur | compatibilite-stabilite | Demarrage / diagnostics reseau | Android 9 journalise des echecs repetes autour de java.lang.ClassValue |
| [TV-VIS-001](#tv-vis-001-la-route-active-ressemble-a-un-second-focus) | mineur | visuel-focus | Header global | La route active ressemble a un second focus |
| [TV-VIS-002](#tv-vis-002-des-libelles-doptions-sont-tronques-dans-les-cartes-de-personnalisation) | mineur | visuel | Settings > Personalization | Des libelles d'options sont tronques dans les cartes de personnalisation |

## Détails

### TV-I18N-001 — La langue anglaise laisse de nombreux libelles francais dans l'interface

- **Gravité :** majeur
- **Catégorie :** internationalisation
- **Écran :** Global
- **Reproductibilité :** systematique sur plusieurs ecrans
- **Confiance :** elevee

**Étapes de reproduction**

1. Utiliser l'application avec les textes principaux en anglais.
2. Ouvrir Live, Movies, Series, les apercus et les details.
3. Comparer les titres anglais avec Favoris, Historique, Apercu, Passer Premium ou Regarder.

**Résultat actuel :** Des chaines de texte francaises coexistent avec les titres et actions anglais dans un meme ecran.

**Résultat attendu :** Toutes les chaines visibles de l'application doivent provenir de SmartVisionStrings et suivre la langue active, hors clavier/systeme Fire OS.

**Impact :** Experience incoherente, comprehension reduite et impression de produit non finalise pour les utilisateurs non francophones.

**Preuves**

- `checkpoints/20260721-200839-378913-live-mini-player-first-ok/screenshot.png`
- `checkpoints/20260721-201507-158718-movies-first-item-ok/screenshot.png`
- `checkpoints/20260721-201942-187284-movie-detail-via-info/screenshot.png`

**Zone de code suspectée**

- ui/live/LiveTvViewModel.kt, ui/movies/MoviesViewModel.kt, ui/series/SeriesViewModel.kt
- ui/detail/MovieDetailScreen.kt:459-471
- composants VodThreePane: chaines Apercu/Passer Premium

**Correction proposée :** Inventorier les literals UI restants, les remplacer par SmartVisionStrings et ajouter une verification des ecrans cles dans chaque langue supportee. Ne pas localiser les noms de contenus fournisseur.

**Critères d’acceptation**

- Aucun libelle francais applicatif ne reste en mode anglais sur les parcours audites.
- Le mode francais conserve les traductions attendues.
- Les categories speciales, apercus, details, erreurs et dialogues sont couverts.
- Les textes fournisseur et le clavier systeme ne sont pas alteres.

**Notes :** Priorite P1 qualite globale.

### TV-NAV-004 — Haut depuis un controle superieur Live saute vers YouTube au lieu de l'onglet Live

- **Gravité :** majeur
- **Catégorie :** navigation-focus
- **Écran :** Live TV
- **Reproductibilité :** 1/1 sequence
- **Confiance :** elevee

**Étapes de reproduction**

1. Ouvrir Live TV et placer le focus sur le controle filtre/tri en haut de la colonne centrale.
2. Appuyer sur Haut.
3. Observer l'onglet du header qui recoit le focus.

**Résultat actuel :** Le focus saute sur YouTube alors que Live reste seulement marque comme route active.

**Résultat attendu :** Haut depuis les controles superieurs Live doit revenir sur l'onglet Live du header, conformement au routage explicite des ecrans principaux.

**Impact :** Un appui directionnel banal cible une destination sans rapport et peut provoquer une navigation accidentelle.

**Preuves**

- `checkpoints/20260721-201318-107902-live-filter-up/screenshot.png`

**Zone de code suspectée**

- ui/live/LiveTvScreen.kt focusProperties des controles superieurs
- ui/home/TvHeader.kt requesters de l'onglet courant

**Correction proposée :** Relier explicitement la propriete up des boutons recherche/filtre Live au currentTabFocusRequester Live, sans laisser la recherche spatiale choisir l'onglet aligne horizontalement.

**Critères d’acceptation**

- Haut depuis chaque controle superieur Live cible l'onglet Live.
- Bas depuis Live restaure la cible de contenu attendue.
- Movies et Series respectent la meme regle avec leur propre onglet.
- La boucle horizontale du header reste intacte.

**Notes :** Priorite P1.

### TV-FUNC-001 — Les categories fournisseur disparaissent dans les trois catalogues

- **Gravité :** majeur
- **Catégorie :** fonctionnel
- **Écran :** Live TV / Movies / Series
- **Reproductibilité :** 3/3 ecrans, 1 session complete
- **Confiance :** elevee sur le symptome; moyenne sur la cause

**Étapes de reproduction**

1. Ouvrir Live TV depuis le header et attendre la fin du chargement des 13726 chaines.
2. Observer la colonne Categories, puis refaire le controle dans Movies (26933) et Series (12028).
3. Verifier que seuls ALL et les categories speciales locales restent visibles.

**Résultat actuel :** Live n'affiche que ALL, Favoris et Historique; Movies et Series n'affichent que ALL et Historique. Aucune categorie reelle du fournisseur n'est accessible malgre plus de 52000 contenus charges.

**Résultat attendu :** Les categories fournisseur associees aux contenus doivent etre visibles, comptabilisees et focusables dans chaque catalogue, en plus des categories speciales locales.

**Impact :** La navigation et le filtrage deviennent pratiquement inutilisables sur des catalogues de plusieurs dizaines de milliers d'elements.

**Preuves**

- `checkpoints/20260721-200839-378913-live-mini-player-first-ok/screenshot.png`
- `checkpoints/20260721-201507-158718-movies-first-item-ok/screenshot.png`
- `checkpoints/20260721-202138-320246-series-first-item-preview/screenshot.png`

**Zone de code suspectée**

- data/repository/DefaultCatalogRepository.kt:1229-1269, 1341-1344, 1512-1515, 1675-1678
- ui/live/LiveTvViewModel.kt, ui/movies/MoviesViewModel.kt, ui/series/SeriesViewModel.kt

**Correction proposée :** Tracer les nombres de categories recues, acceptees et inserees par section et profil. Corriger la correspondance entre categoryId des medias et id fournisseur avant le delete/upsert atomique; ne pas remplacer un jeu de categories valide par un resultat filtre anormalement vide.

**Critères d’acceptation**

- Chaque ecran affiche toutes les categories fournisseur non vides avec un compteur coherent.
- Selectionner une categorie reduit la liste aux contenus correspondants et conserve le focus.
- Une synchronisation puis un redemarrage ne suppriment plus ces categories.
- ALL, Favoris et Historique continuent de fonctionner sans regression.

**Notes :** Priorite P0 fonctionnelle. Verifier aussi le payload Xtream reel avant de conclure a une cause uniquement locale.

### TV-PERF-001 — La navigation Live presente un jank massif et une forte charge sur le Fire TV

- **Gravité :** majeur
- **Catégorie :** performance
- **Écran :** Live TV avec apercu actif
- **Reproductibilité :** 1 mesure controlee apres une longue session; jank visuellement perceptible
- **Confiance :** elevee sur les mesures; moyenne sur l'attribution exacte

**Étapes de reproduction**

1. Ouvrir Live TV et demarrer l'apercu de la premiere chaine.
2. Remettre dumpsys gfxinfo a zero.
3. Parcourir la liste au D-pad vers le bas puis vers le haut et relever gfxinfo/meminfo/top.

**Résultat actuel :** Sur la fenetre controlee, 1359/1359 frames sont classees janky, avec p50 57 ms, p90 73 ms, 772 vsync manques et 975 deadlines manquees. Le processus atteint 273 MB PSS et un instantane CPU a 148 % pendant l'apercu.

**Résultat attendu :** La navigation D-pad doit rester fluide et reactive pendant l'apercu, avec un budget de frame stable et sans surcharge CPU durable.

**Impact :** Latence de focus, animation saccadee et risque accru de chauffe, swap ou fermeture sous pression memoire sur un Fire Stick limite.

**Preuves**

- `performance-summary.md`
- `checkpoints/20260721-200839-378913-live-mini-player-first-ok/gfxinfo-framestats.txt`

**Zone de code suspectée**

- ui/live/LiveTvScreen.kt et composants trois panneaux
- cycle de vie de l'apercu Media3, animations de focus et recompositions

**Correction proposée :** Capturer une trace Perfetto ciblee avant modification, puis reduire les recompositions et allocations liees au focus, stabiliser les cles/listes et isoler le rendu Media3. Comparer avec apercu desactive pour separer UI et video.

**Critères d’acceptation**

- Une campagne D-pad equivalente ne montre plus 100 % de frames janky.
- Les percentiles et le temps de reponse focus s'ameliorent nettement sur le meme appareil.
- L'apercu continue de demarrer/s'arreter correctement sans fuite de player.
- La memoire se stabilise apres plusieurs allers-retours Live/Home.

**Notes :** Priorite P0/P1. Les statistiques incluent le rendu video continu; confirmer la cause avec Perfetto.

### TV-PLAY-001 — Une erreur decodeur/source laisse le lecteur sur un spinner noir sans message

- **Gravité :** majeur
- **Catégorie :** lecture
- **Écran :** Movies / lecteur plein ecran
- **Reproductibilité :** 1/1 contenu teste; depend du flux
- **Confiance :** elevee

**Étapes de reproduction**

1. Ouvrir Movies et lancer le premier film teste depuis l'apercu.
2. Attendre au moins 17 secondes sur le lecteur plein ecran.
3. Observer l'ecran et les erreurs Media3 dans logcat.

**Résultat actuel :** Le lecteur reste noir avec un spinner. Logcat signale un HEVC 3840x2080 depassant les capacites du decodeur OMX.MTK puis des reponses HTTP 551; aucune erreur ni action de retour/reessai n'est affichee a l'utilisateur.

**Résultat attendu :** Une erreur terminale de compatibilite ou de source doit arreter le spinner, expliquer que le contenu ne peut pas etre lu et proposer Retour/Reessayer ou une variante compatible disponible.

**Impact :** L'utilisateur pense que l'application est bloquee et ne sait pas si le probleme vient du reseau, du contenu ou de l'appareil.

**Preuves**

- `checkpoints/20260721-201654-686516-movie-player-after-17s/screenshot.png`
- `checkpoints/20260721-201654-686516-movie-player-after-17s/logcat.txt`

**Zone de code suspectée**

- ui/player/FullScreenPlayerScreen.kt
- gestion d'erreur et strategie de retry Media3

**Correction proposée :** Distinguer buffering, erreur recuperable et erreur terminale. Apres echec decodeur ou retries source bornes, afficher un panneau d'erreur focusable et liberer la boucle de chargement. Ajouter un precontrole des capacites pour les variantes 4K HEVC lorsque les metadonnees sont disponibles.

**Critères d’acceptation**

- Le spinner ne reste jamais indefiniment apres une erreur terminale.
- Le message et les boutons sont lisibles et utilisables au D-pad.
- Back revient au film source avec son focus restaure.
- Les flux compatibles continuent de demarrer normalement.

**Notes :** Priorite P0 lecteur. Le test ne permet pas de generaliser l'echec a tous les films.

### TV-NAV-001 — Les BackHandler imbriques rendent le retour non deterministe

- **Gravité :** majeur
- **Catégorie :** navigation-focus
- **Écran :** Notifications / Settings / navigation globale
- **Reproductibilité :** Notifications 1/1 sequence; Settings 1/1 sequence
- **Confiance :** elevee

**Étapes de reproduction**

1. Depuis Home, ouvrir Notifications et descendre sur Notification history.
2. Appuyer sur Back: le premier appui peut rester visuellement sur place.
3. Appuyer de nouveau sur Back: Home apparait directement avec le dialogue Exit SmartVision.
4. Dans Settings > Personalization, entrer dans les controles puis appuyer sur Back et constater que l'ecran peut rester ouvert.

**Résultat actuel :** Le retour Notifications peut demander deux appuis puis ouvrir immediatement le dialogue de sortie de Home. Dans Settings, un Back a egalement ete consomme sans retour visible.

**Résultat attendu :** Un seul Back depuis Notifications ou Settings doit revenir a Home, restaurer le focus sur l'icone d'origine et ne jamais ouvrir le dialogue de sortie dans le meme geste.

**Impact :** Risque de sortie accidentelle et perte de confiance dans la navigation de base a la telecommande.

**Preuves**

- `checkpoints/20260721-204428-458483-notifications-back/screenshot.png`
- `checkpoints/20260721-204540-074014-notifications-back-second/screenshot.png`
- `checkpoints/20260721-204200-890571-settings-back-home/screenshot.png`

**Zone de code suspectée**

- ui/notifications/NotificationsScreen.kt:169
- ui/settings/SettingsScreen.kt:152
- ui/navigation/AppNavigation.kt:1235-1247

**Correction proposée :** Definir un seul proprietaire du Back par niveau de route. Supprimer la concurrence entre BackHandler d'ecran et BackHandler global, puis centraliser le retour top-level vers Home avec une requete de focus unique.

**Critères d’acceptation**

- Un Back unique depuis Notifications, Settings et Profil revient a Home.
- Le dialogue Exit n'apparait que depuis Home sur un nouvel appui distinct.
- Le focus revient respectivement sur Notifications, Settings ou Profil.
- Les Back des lecteurs et dialogues restent inchanges.

**Notes :** Priorite P0 navigation. Tester chaque route top-level apres correction.

### TV-PROFILE-001 — Depuis Add kids profile, Bas cible Edit sur le profil precedent

- **Gravité :** majeur
- **Catégorie :** navigation-focus
- **Écran :** Profil > Manage profiles
- **Reproductibilité :** 1/1 sequence
- **Confiance :** elevee

**Étapes de reproduction**

1. Ouvrir Profil puis Manage profiles.
2. Parcourir les profils existants jusqu'a Add kids profile.
3. Appuyer sur Bas sans valider la creation.

**Résultat actuel :** Le focus quitte la carte Add kids profile et arrive sur Edit dans le panneau d'actions du dernier profil existant; son detail et Delete restent visibles pendant que la carte d'ajout etait la cible precedente.

**Résultat attendu :** Une carte d'ajout ne doit jamais router vers les actions d'un autre profil. Bas doit etre bloque, rester sur la carte ou cibler une action de creation clairement associee.

**Impact :** Risque d'editer, verrouiller ou supprimer le mauvais profil, particulierement grave avec une telecommande et des profils enfants.

**Preuves**

- `Capture locale protegee non exportee car elle contient des noms de profils`
- `ui/profile/ProfileAreaScreen.kt:534-548, 582-617`
- `ui/profile/ProfileManagementPolicy.kt:39-48`

**Zone de code suspectée**

- ui/profile/ProfileAreaScreen.kt: focusedProfileId n'est pas efface sur les cartes add et aucun down explicite n'est defini

**Correction proposée :** Representer explicitement la cible de focus courante (profil ou action d'ajout), masquer/desactiver le panneau d'actions quand une carte add est focussee et definir les focusProperties down des deux cartes d'ajout.

**Critères d’acceptation**

- Bas depuis Add kids/Add profile ne cible jamais Edit/Lock/Delete d'un profil existant.
- Le panneau de detail correspond toujours a la cible focussee ou est clairement neutralise.
- Les actions destructives restent inaccessibles tant qu'un profil n'est pas explicitement selectionne.
- Le retour de focus apres edition/suppression reste coherent.

**Notes :** Priorite P0 UX/securite fonctionnelle.

### TV-NAV-002 — Le retour d'un episode restaure la saison au lieu de l'episode lance

- **Gravité :** majeur
- **Catégorie :** navigation-focus
- **Écran :** Series detail / episode player
- **Reproductibilité :** 1/1 episode teste
- **Confiance :** elevee

**Étapes de reproduction**

1. Ouvrir une serie, choisir la saison et lancer le premier episode.
2. Valider Resume dans le dialogue et attendre la lecture.
3. Appuyer une fois sur Back pour revenir au detail.

**Résultat actuel :** Le detail revient correctement mais le focus est place sur Saison 1, pas sur la ligne d'episode lancee.

**Résultat attendu :** Le retour doit restaurer la saison, le scroll et le focus exact de l'episode source.

**Impact :** Dans une saison longue, l'utilisateur perd sa position et doit reparcourir la liste apres chaque lecture.

**Preuves**

- `checkpoints/20260721-202455-412695-episode-back-detail-focus/screenshot.png`

**Zone de code suspectée**

- ui/detail/SeriesDetailScreen.kt:450-464, 724-728
- ui/navigation/AppNavigation.kt route episode_player

**Correction proposée :** Memoriser l'episode source et son index/season avant navigation, exposer un FocusRequester par episode visible et restaurer le scroll avant la demande de focus apres popBackStack.

**Critères d’acceptation**

- Back restaure exactement l'episode lance, meme hors premiere ligne.
- Le scroll et la saison selectionnee sont conserves.
- Le focus initial d'une ouverture fraiche reste coherent.
- La reprise et le redemarrage d'episode ne regressent pas.

**Notes :** Priorite P1.

### TV-NAV-003 — Back du lecteur YouTube perd la video source et remet le focus sur la categorie

- **Gravité :** majeur
- **Catégorie :** navigation-focus
- **Écran :** YouTube
- **Reproductibilité :** 1/1 video testee
- **Confiance :** elevee

**Étapes de reproduction**

1. Ouvrir YouTube, aller de Trending vers la premiere video et lancer la lecture.
2. Attendre le demarrage puis appuyer sur Back.
3. Comparer la grille et le focus avec l'etat avant lecture.

**Résultat actuel :** Le focus revient sur Trending au lieu de la video lancee; la grille est rechargee/reordonnee et son nombre d'elements est passe de 50 a 83 pendant le parcours.

**Résultat attendu :** Back doit revenir a la meme video, au meme scroll et a une grille stable, sauf rafraichissement explicitement demande.

**Impact :** L'utilisateur perd le contexte et ne peut pas reprendre facilement sa navigation dans les suggestions.

**Preuves**

- `checkpoints/20260721-202820-926751-youtube-first-video-ok/screenshot.png`
- `checkpoints/20260721-202932-768235-youtube-player-after-18s/screenshot.png`
- `checkpoints/20260721-203116-345624-youtube-back-grid/screenshot.png`

**Zone de code suspectée**

- ui/youtube/YoutubeScreen.kt:173-175, 247, 267-281

**Correction proposée :** Conserver le videoId et l'index/scroll source. A la fermeture du player, restaurer ce requester precis et ne pas forcer simultanement le requester de categorie Trending. Separer chargement incremental et rafraichissement de grille.

**Critères d’acceptation**

- Back remet le focus sur la video lancee.
- Le scroll horizontal/vertical et l'ordre visible sont conserves.
- Le chargement incremental n'ecrase pas la liste courante.
- Les retours depuis suggestions et plein ecran suivent la meme regle.

**Notes :** Priorite P1. Le demarrage initial de la video a aussi demande environ 18 s lors de ce test unique.

### TV-STAB-001 — Android 9 journalise des echecs repetes autour de java.lang.ClassValue

- **Gravité :** mineur
- **Catégorie :** compatibilite-stabilite
- **Écran :** Demarrage / diagnostics reseau
- **Reproductibilité :** 3/3 demarrages forces
- **Confiance :** moyenne; impact fonctionnel non prouve

**Étapes de reproduction**

1. Vider logcat et forcer l'arret/demarrage de l'application sur le Fire TV API 28.
2. Attendre le selecteur de profil puis Home.
3. Rechercher NoClassDefFoundError et ClassValueCtorCache dans les logs.

**Résultat actuel :** Chaque demarrage rejoue plusieurs traces Rejecting re-init et ClassNotFoundException pour java.lang.ClassValue depuis kotlinx.coroutines.internal.ClassValueCtorCache. L'application continue et aucun crash n'est observe.

**Résultat attendu :** La pile release API 28 doit utiliser un chemin de compatibilite silencieux et les diagnostics ne doivent pas generer de traces d'echec repetees.

**Impact :** Bruit de logs, cout de demarrage possible et risque que certains rapports reseau/diagnostics echouent silencieusement sur Android 9.

**Preuves**

- `checkpoints/20260721-210204-939718-startup-run3-after-8s/logcat.txt`
- `checkpoints/20260721-195908-826244-startup-cold-initial/logcat.txt`

**Zone de code suspectée**

- versions kotlinx-coroutines / Retrofit et configuration R8-desugaring API 28
- DeviceDiagnosticsReporter au demarrage

**Correction proposée :** Identifier l'appel qui initialise ce cache, verifier les versions de dependances et la compatibilite API 28, puis confirmer si les requetes de diagnostics aboutissent. Eviter de masquer l'erreur sans prouver le chemin de fallback.

**Critères d’acceptation**

- Aucun NoClassDefFoundError ClassValue au demarrage sur API 28.
- Les diagnostics/requetes concernes reussissent ou echouent avec un message maitrise.
- Les versions Android plus recentes ne regressent pas.
- Le buffer crash reste vide sur les trois demarrages de controle.

**Notes :** Priorite P2 diagnostic.


### TV-VIS-001 — La route active ressemble a un second focus

- **Gravité :** mineur
- **Catégorie :** visuel-focus
- **Écran :** Header global
- **Reproductibilité :** systematique
- **Confiance :** elevee

**Étapes de reproduction**

1. Sur Home, descendre le focus sur une carte de contenu.
2. Observer que Home reste encadre en cyan avec son label.
3. Remonter au header et deplacer le focus sur Live TV.

**Résultat actuel :** La route active conserve fond, bordure et label presque comme une cible focussee, tandis que le vrai focus est en or ailleurs; deux elements semblent actifs simultanement.

**Résultat attendu :** La route active doit rester identifiable mais suffisamment secondaire pour qu'un seul element lise visuellement comme le focus telecommande.

**Impact :** Ambiguite immediate de focus, particulierement sur la nouvelle barre d'onglets.

**Preuves**

- `checkpoints/20260721-200138-678927-home-header-right-live/screenshot.png`
- `checkpoints/20260721-200158-970500-home-header-down-content/screenshot.png`

**Zone de code suspectée**

- ui/home/TvHeader.kt:595-624, 698-712

**Correction proposée :** Conserver un indicateur actif plus discret (teinte d'icone ou soulignement fin) et reserver fond, cadre fort, label/scale complets au focus reel. Respecter la personnalisation active/selected sans supprimer l'information de route.

**Critères d’acceptation**

- A tout instant, la cible du D-pad est visuellement non ambigue.
- La route active reste reconnaissable sans imiter le focus.
- Les couleurs de personnalisation et le mode Kids restent compatibles.
- Notifications, avatar et Settings ne regressent pas.

**Notes :** Priorite P2 finition, mais directement liee au signalement de double focus.

### TV-VIS-002 — Des libelles d'options sont tronques dans les cartes de personnalisation

- **Gravité :** mineur
- **Catégorie :** visuel
- **Écran :** Settings > Personalization
- **Reproductibilité :** systematique a 1920x1080
- **Confiance :** elevee

**Étapes de reproduction**

1. Ouvrir Settings puis Personalization.
2. Observer la ligne Focus background et ses trois options.
3. Verifier le texte des variantes Transparent.

**Résultat actuel :** Les options affichent des formes tronquees telles que Transpar/Transpare, sans ellipsis ni libelle complet.

**Résultat attendu :** Chaque variante doit etre lisible en entier ou disposer d'un traitement d'ellipsis/tooltip coherent pour un ecran 1080p.

**Impact :** L'utilisateur ne peut pas distinguer clairement les choix de personnalisation.

**Preuves**

- `checkpoints/20260721-204046-945877-settings-personalization-right/screenshot.png`

**Zone de code suspectée**

- ui/settings/SettingsScreen.kt composants de Personalization

**Correction proposée :** Ajuster largeur, taille ou libelles localises des boutons afin d'afficher les variantes completement a 1080p, puis verifier les langues les plus longues.

**Critères d’acceptation**

- Tous les libelles sont complets a 1080p.
- Le focus et l'etat selectionne restent distincts.
- Le scroll vertical continue d'amener les cartes basses a l'ecran.
- Aucune option n'est decalee ou chevauchee.

**Notes :** Priorite P2.
## Analyse transversale

### Focus et navigation D-pad

- Le focus est généralement visible en or et les parcours de base Home, listes et dialogues restent accessibles.
- Les principales ruptures concernent le propriétaire de Back, la restauration du contexte après un player et certains liens verticaux laissés à la recherche spatiale.
- Les retours Live plein écran et Movie vers la liste restaurent correctement le contenu source; Series et YouTube ne restaurent pas la cible exacte.
- La gestion des profils nécessite une correction prioritaire: une carte d'ajout peut envoyer Bas vers les actions du profil précédent.

### Cohérence visuelle

- La grille, les trois panneaux catalogue, les états vides et les cartes principales sont lisibles à 1080p.
- L'état `active` cyan du header ressemble trop au focus réel, ce qui produit la perception de double focus signalée.
- Les écrans utilisent encore plusieurs systèmes de focus (or, cyan, blanc) et des libellés de personnalisation sont tronqués.
- L'interface anglaise reste mélangée avec des chaînes françaises applicatives.

### Performance et chargement

- Les trois lancements forcés donnent une médiane `TotalTime` de 1977 ms et une médiane `WaitTime` de 2695 ms; le sélecteur de profil était prêt lors du contrôle à 8 secondes.
- Le premier démarrage YouTube testé a demandé environ 18 secondes avant lecture; un échantillon plus large est nécessaire avant d'en faire une anomalie autonome.
- Le parcours Live avec aperçu actif est le point critique mesuré: 100 % de frames janky dans la fenêtre contrôlée, p50 57 ms et p95 81 ms.
- La base locale est volumineuse et le processus atteint 273 MB PSS après exploration; `/data` ne dispose plus que de 987 MB libres.

### Stabilité

- Aucun crash ni ANR n'a été observé pendant la campagne et le buffer crash est resté vide.
- Live et un épisode Series ont été lus avec succès; le film HEVC 4K testé a échoué proprement côté Media3 mais pas côté interface utilisateur.
- Les traces `ClassValue` sont répétées sur Android 9 sans crash; leur impact réel sur les diagnostics réseau reste à confirmer.

## Plan d'action recommandé

1. **Lot 0 — intégrité fonctionnelle et sécurité de navigation**: TV-FUNC-001, TV-PLAY-001, TV-NAV-001 et TV-PROFILE-001. Corriger séparément puis rejouer les trois catalogues, les erreurs lecteur, tous les Back top-level et la gestion des profils.
2. **Lot 1 — restauration de focus**: TV-NAV-002, TV-NAV-003 et TV-NAV-004. Introduire des cibles de retour explicites sans modifier les routes ni les comportements player déjà valides.
3. **Lot 2 — performance Fire TV**: TV-PERF-001. Capturer d'abord une trace Perfetto comparative aperçu actif/inactif, corriger la cause dominante, puis refaire exactement la mesure `gfxinfo`.
4. **Lot 3 — cohérence UI et compatibilité**: TV-I18N-001, TV-VIS-001, TV-VIS-002 et TV-STAB-001. Centraliser les chaînes, différencier route active/focus, corriger les contraintes de texte et vérifier API 28.

Chaque lot doit être validé sur le même Fire TV, avec build release uniquement au moment où une correction sera autorisée. Aucun de ces lots n'a été commencé pendant l'audit.
