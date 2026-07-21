# Lots de correction pour Codex

Traiter un lot à la fois. Inspecter les preuves et le code avant toute modification, préserver les changements existants, puis exécuter les tests ciblés et le scénario de validation Android TV.

## Lot 1 — priorité-haute / internationalisation

```text
Corrige les anomalies Android TV suivantes : TV-I18N-001.

Contraintes :
- Analyse d'abord les captures, logs, traces et zones de code indiquées dans le rapport d'audit.
- Corrige les causes racines sans refonte hors périmètre.
- Préserve la navigation complète à la télécommande et la cohérence visuelle existante.
- Ajoute ou adapte les tests ciblés lorsque cela est pertinent.
- Rejoue les critères d'acceptation sur l'appareil Android TV et rapporte les résultats.

Anomalies :
- TV-I18N-001 — Global — La langue anglaise laisse de nombreux libelles francais dans l'interface
  Actuel : Des chaines de texte francaises coexistent avec les titres et actions anglais dans un meme ecran.
  Attendu : Toutes les chaines visibles de l'application doivent provenir de SmartVisionStrings et suivre la langue active, hors clavier/systeme Fire OS.
  Correction proposée : Inventorier les literals UI restants, les remplacer par SmartVisionStrings et ajouter une verification des ecrans cles dans chaque langue supportee. Ne pas localiser les noms de contenus fournisseur.
  Preuves : checkpoints/20260721-200839-378913-live-mini-player-first-ok/screenshot.png; checkpoints/20260721-201507-158718-movies-first-item-ok/screenshot.png; checkpoints/20260721-201942-187284-movie-detail-via-info/screenshot.png
  Validation : Aucun libelle francais applicatif ne reste en mode anglais sur les parcours audites.; Le mode francais conserve les traductions attendues.; Les categories speciales, apercus, details, erreurs et dialogues sont couverts.; Les textes fournisseur et le clavier systeme ne sont pas alteres.
```

## Lot 2 — priorité-haute / navigation-focus

```text
Corrige les anomalies Android TV suivantes : TV-NAV-004, TV-NAV-001, TV-PROFILE-001, TV-NAV-002, TV-NAV-003.

Contraintes :
- Analyse d'abord les captures, logs, traces et zones de code indiquées dans le rapport d'audit.
- Corrige les causes racines sans refonte hors périmètre.
- Préserve la navigation complète à la télécommande et la cohérence visuelle existante.
- Ajoute ou adapte les tests ciblés lorsque cela est pertinent.
- Rejoue les critères d'acceptation sur l'appareil Android TV et rapporte les résultats.

Anomalies :
- TV-NAV-004 — Live TV — Haut depuis un controle superieur Live saute vers YouTube au lieu de l'onglet Live
  Actuel : Le focus saute sur YouTube alors que Live reste seulement marque comme route active.
  Attendu : Haut depuis les controles superieurs Live doit revenir sur l'onglet Live du header, conformement au routage explicite des ecrans principaux.
  Correction proposée : Relier explicitement la propriete up des boutons recherche/filtre Live au currentTabFocusRequester Live, sans laisser la recherche spatiale choisir l'onglet aligne horizontalement.
  Preuves : checkpoints/20260721-201318-107902-live-filter-up/screenshot.png
  Validation : Haut depuis chaque controle superieur Live cible l'onglet Live.; Bas depuis Live restaure la cible de contenu attendue.; Movies et Series respectent la meme regle avec leur propre onglet.; La boucle horizontale du header reste intacte.
- TV-NAV-001 — Notifications / Settings / navigation globale — Les BackHandler imbriques rendent le retour non deterministe
  Actuel : Le retour Notifications peut demander deux appuis puis ouvrir immediatement le dialogue de sortie de Home. Dans Settings, un Back a egalement ete consomme sans retour visible.
  Attendu : Un seul Back depuis Notifications ou Settings doit revenir a Home, restaurer le focus sur l'icone d'origine et ne jamais ouvrir le dialogue de sortie dans le meme geste.
  Correction proposée : Definir un seul proprietaire du Back par niveau de route. Supprimer la concurrence entre BackHandler d'ecran et BackHandler global, puis centraliser le retour top-level vers Home avec une requete de focus unique.
  Preuves : checkpoints/20260721-204428-458483-notifications-back/screenshot.png; checkpoints/20260721-204540-074014-notifications-back-second/screenshot.png; checkpoints/20260721-204200-890571-settings-back-home/screenshot.png
  Validation : Un Back unique depuis Notifications, Settings et Profil revient a Home.; Le dialogue Exit n'apparait que depuis Home sur un nouvel appui distinct.; Le focus revient respectivement sur Notifications, Settings ou Profil.; Les Back des lecteurs et dialogues restent inchanges.
- TV-PROFILE-001 — Profil > Manage profiles — Depuis Add kids profile, Bas cible Edit sur le profil precedent
  Actuel : Le focus quitte la carte Add kids profile et arrive sur Edit dans le panneau d'actions du dernier profil existant; son detail et Delete restent visibles pendant que la carte d'ajout etait la cible precedente.
  Attendu : Une carte d'ajout ne doit jamais router vers les actions d'un autre profil. Bas doit etre bloque, rester sur la carte ou cibler une action de creation clairement associee.
  Correction proposée : Representer explicitement la cible de focus courante (profil ou action d'ajout), masquer/desactiver le panneau d'actions quand une carte add est focussee et definir les focusProperties down des deux cartes d'ajout.
  Preuves : Capture locale protegee non exportee car elle contient des noms de profils; ui/profile/ProfileAreaScreen.kt:534-548, 582-617; ui/profile/ProfileManagementPolicy.kt:39-48
  Validation : Bas depuis Add kids/Add profile ne cible jamais Edit/Lock/Delete d'un profil existant.; Le panneau de detail correspond toujours a la cible focussee ou est clairement neutralise.; Les actions destructives restent inaccessibles tant qu'un profil n'est pas explicitement selectionne.; Le retour de focus apres edition/suppression reste coherent.
- TV-NAV-002 — Series detail / episode player — Le retour d'un episode restaure la saison au lieu de l'episode lance
  Actuel : Le detail revient correctement mais le focus est place sur Saison 1, pas sur la ligne d'episode lancee.
  Attendu : Le retour doit restaurer la saison, le scroll et le focus exact de l'episode source.
  Correction proposée : Memoriser l'episode source et son index/season avant navigation, exposer un FocusRequester par episode visible et restaurer le scroll avant la demande de focus apres popBackStack.
  Preuves : checkpoints/20260721-202455-412695-episode-back-detail-focus/screenshot.png
  Validation : Back restaure exactement l'episode lance, meme hors premiere ligne.; Le scroll et la saison selectionnee sont conserves.; Le focus initial d'une ouverture fraiche reste coherent.; La reprise et le redemarrage d'episode ne regressent pas.
- TV-NAV-003 — YouTube — Back du lecteur YouTube perd la video source et remet le focus sur la categorie
  Actuel : Le focus revient sur Trending au lieu de la video lancee; la grille est rechargee/reordonnee et son nombre d'elements est passe de 50 a 83 pendant le parcours.
  Attendu : Back doit revenir a la meme video, au meme scroll et a une grille stable, sauf rafraichissement explicitement demande.
  Correction proposée : Conserver le videoId et l'index/scroll source. A la fermeture du player, restaurer ce requester precis et ne pas forcer simultanement le requester de categorie Trending. Separer chargement incremental et rafraichissement de grille.
  Preuves : checkpoints/20260721-202820-926751-youtube-first-video-ok/screenshot.png; checkpoints/20260721-202932-768235-youtube-player-after-18s/screenshot.png; checkpoints/20260721-203116-345624-youtube-back-grid/screenshot.png
  Validation : Back remet le focus sur la video lancee.; Le scroll horizontal/vertical et l'ordre visible sont conserves.; Le chargement incremental n'ecrase pas la liste courante.; Les retours depuis suggestions et plein ecran suivent la meme regle.
```

## Lot 3 — priorité-haute / fonctionnel

```text
Corrige les anomalies Android TV suivantes : TV-FUNC-001.

Contraintes :
- Analyse d'abord les captures, logs, traces et zones de code indiquées dans le rapport d'audit.
- Corrige les causes racines sans refonte hors périmètre.
- Préserve la navigation complète à la télécommande et la cohérence visuelle existante.
- Ajoute ou adapte les tests ciblés lorsque cela est pertinent.
- Rejoue les critères d'acceptation sur l'appareil Android TV et rapporte les résultats.

Anomalies :
- TV-FUNC-001 — Live TV / Movies / Series — Les categories fournisseur disparaissent dans les trois catalogues
  Actuel : Live n'affiche que ALL, Favoris et Historique; Movies et Series n'affichent que ALL et Historique. Aucune categorie reelle du fournisseur n'est accessible malgre plus de 52000 contenus charges.
  Attendu : Les categories fournisseur associees aux contenus doivent etre visibles, comptabilisees et focusables dans chaque catalogue, en plus des categories speciales locales.
  Correction proposée : Tracer les nombres de categories recues, acceptees et inserees par section et profil. Corriger la correspondance entre categoryId des medias et id fournisseur avant le delete/upsert atomique; ne pas remplacer un jeu de categories valide par un resultat filtre anormalement vide.
  Preuves : checkpoints/20260721-200839-378913-live-mini-player-first-ok/screenshot.png; checkpoints/20260721-201507-158718-movies-first-item-ok/screenshot.png; checkpoints/20260721-202138-320246-series-first-item-preview/screenshot.png
  Validation : Chaque ecran affiche toutes les categories fournisseur non vides avec un compteur coherent.; Selectionner une categorie reduit la liste aux contenus correspondants et conserve le focus.; Une synchronisation puis un redemarrage ne suppriment plus ces categories.; ALL, Favoris et Historique continuent de fonctionner sans regression.
```

## Lot 4 — priorité-haute / performance

```text
Corrige les anomalies Android TV suivantes : TV-PERF-001.

Contraintes :
- Analyse d'abord les captures, logs, traces et zones de code indiquées dans le rapport d'audit.
- Corrige les causes racines sans refonte hors périmètre.
- Préserve la navigation complète à la télécommande et la cohérence visuelle existante.
- Ajoute ou adapte les tests ciblés lorsque cela est pertinent.
- Rejoue les critères d'acceptation sur l'appareil Android TV et rapporte les résultats.

Anomalies :
- TV-PERF-001 — Live TV avec apercu actif — La navigation Live presente un jank massif et une forte charge sur le Fire TV
  Actuel : Sur la fenetre controlee, 1359/1359 frames sont classees janky, avec p50 57 ms, p90 73 ms, 772 vsync manques et 975 deadlines manquees. Le processus atteint 273 MB PSS et un instantane CPU a 148 % pendant l'apercu.
  Attendu : La navigation D-pad doit rester fluide et reactive pendant l'apercu, avec un budget de frame stable et sans surcharge CPU durable.
  Correction proposée : Capturer une trace Perfetto ciblee avant modification, puis reduire les recompositions et allocations liees au focus, stabiliser les cles/listes et isoler le rendu Media3. Comparer avec apercu desactive pour separer UI et video.
  Preuves : performance-summary.md; checkpoints/20260721-200839-378913-live-mini-player-first-ok/gfxinfo-framestats.txt
  Validation : Une campagne D-pad equivalente ne montre plus 100 % de frames janky.; Les percentiles et le temps de reponse focus s'ameliorent nettement sur le meme appareil.; L'apercu continue de demarrer/s'arreter correctement sans fuite de player.; La memoire se stabilise apres plusieurs allers-retours Live/Home.
```

## Lot 5 — priorité-haute / lecture

```text
Corrige les anomalies Android TV suivantes : TV-PLAY-001.

Contraintes :
- Analyse d'abord les captures, logs, traces et zones de code indiquées dans le rapport d'audit.
- Corrige les causes racines sans refonte hors périmètre.
- Préserve la navigation complète à la télécommande et la cohérence visuelle existante.
- Ajoute ou adapte les tests ciblés lorsque cela est pertinent.
- Rejoue les critères d'acceptation sur l'appareil Android TV et rapporte les résultats.

Anomalies :
- TV-PLAY-001 — Movies / lecteur plein ecran — Une erreur decodeur/source laisse le lecteur sur un spinner noir sans message
  Actuel : Le lecteur reste noir avec un spinner. Logcat signale un HEVC 3840x2080 depassant les capacites du decodeur OMX.MTK puis des reponses HTTP 551; aucune erreur ni action de retour/reessai n'est affichee a l'utilisateur.
  Attendu : Une erreur terminale de compatibilite ou de source doit arreter le spinner, expliquer que le contenu ne peut pas etre lu et proposer Retour/Reessayer ou une variante compatible disponible.
  Correction proposée : Distinguer buffering, erreur recuperable et erreur terminale. Apres echec decodeur ou retries source bornes, afficher un panneau d'erreur focusable et liberer la boucle de chargement. Ajouter un precontrole des capacites pour les variantes 4K HEVC lorsque les metadonnees sont disponibles.
  Preuves : checkpoints/20260721-201654-686516-movie-player-after-17s/screenshot.png; checkpoints/20260721-201654-686516-movie-player-after-17s/logcat.txt
  Validation : Le spinner ne reste jamais indefiniment apres une erreur terminale.; Le message et les boutons sont lisibles et utilisables au D-pad.; Back revient au film source avec son focus restaure.; Les flux compatibles continuent de demarrer normalement.
```

## Lot 6 — finition / compatibilite-stabilite

```text
Corrige les anomalies Android TV suivantes : TV-STAB-001.

Contraintes :
- Analyse d'abord les captures, logs, traces et zones de code indiquées dans le rapport d'audit.
- Corrige les causes racines sans refonte hors périmètre.
- Préserve la navigation complète à la télécommande et la cohérence visuelle existante.
- Ajoute ou adapte les tests ciblés lorsque cela est pertinent.
- Rejoue les critères d'acceptation sur l'appareil Android TV et rapporte les résultats.

Anomalies :
- TV-STAB-001 — Demarrage / diagnostics reseau — Android 9 journalise des echecs repetes autour de java.lang.ClassValue
  Actuel : Chaque demarrage rejoue plusieurs traces Rejecting re-init et ClassNotFoundException pour java.lang.ClassValue depuis kotlinx.coroutines.internal.ClassValueCtorCache. L'application continue et aucun crash n'est observe.
  Attendu : La pile release API 28 doit utiliser un chemin de compatibilite silencieux et les diagnostics ne doivent pas generer de traces d'echec repetees.
  Correction proposée : Identifier l'appel qui initialise ce cache, verifier les versions de dependances et la compatibilite API 28, puis confirmer si les requetes de diagnostics aboutissent. Eviter de masquer l'erreur sans prouver le chemin de fallback.
  Preuves : checkpoints/20260721-210204-939718-startup-run3-after-8s/logcat.txt; checkpoints/20260721-195908-826244-startup-cold-initial/logcat.txt
  Validation : Aucun NoClassDefFoundError ClassValue au demarrage sur API 28.; Les diagnostics/requetes concernes reussissent ou echouent avec un message maitrise.; Les versions Android plus recentes ne regressent pas.; Le buffer crash reste vide sur les trois demarrages de controle.
```

## Lot 7 — finition / visuel-focus

```text
Corrige les anomalies Android TV suivantes : TV-VIS-001.

Contraintes :
- Analyse d'abord les captures, logs, traces et zones de code indiquées dans le rapport d'audit.
- Corrige les causes racines sans refonte hors périmètre.
- Préserve la navigation complète à la télécommande et la cohérence visuelle existante.
- Ajoute ou adapte les tests ciblés lorsque cela est pertinent.
- Rejoue les critères d'acceptation sur l'appareil Android TV et rapporte les résultats.

Anomalies :
- TV-VIS-001 — Header global — La route active ressemble a un second focus
  Actuel : La route active conserve fond, bordure et label presque comme une cible focussee, tandis que le vrai focus est en or ailleurs; deux elements semblent actifs simultanement.
  Attendu : La route active doit rester identifiable mais suffisamment secondaire pour qu'un seul element lise visuellement comme le focus telecommande.
  Correction proposée : Conserver un indicateur actif plus discret (teinte d'icone ou soulignement fin) et reserver fond, cadre fort, label/scale complets au focus reel. Respecter la personnalisation active/selected sans supprimer l'information de route.
  Preuves : checkpoints/20260721-200138-678927-home-header-right-live/screenshot.png; checkpoints/20260721-200158-970500-home-header-down-content/screenshot.png
  Validation : A tout instant, la cible du D-pad est visuellement non ambigue.; La route active reste reconnaissable sans imiter le focus.; Les couleurs de personnalisation et le mode Kids restent compatibles.; Notifications, avatar et Settings ne regressent pas.
```

## Lot 8 — finition / visuel

```text
Corrige les anomalies Android TV suivantes : TV-VIS-002.

Contraintes :
- Analyse d'abord les captures, logs, traces et zones de code indiquées dans le rapport d'audit.
- Corrige les causes racines sans refonte hors périmètre.
- Préserve la navigation complète à la télécommande et la cohérence visuelle existante.
- Ajoute ou adapte les tests ciblés lorsque cela est pertinent.
- Rejoue les critères d'acceptation sur l'appareil Android TV et rapporte les résultats.

Anomalies :
- TV-VIS-002 — Settings > Personalization — Des libelles d'options sont tronques dans les cartes de personnalisation
  Actuel : Les options affichent des formes tronquees telles que Transpar/Transpare, sans ellipsis ni libelle complet.
  Attendu : Chaque variante doit etre lisible en entier ou disposer d'un traitement d'ellipsis/tooltip coherent pour un ecran 1080p.
  Correction proposée : Ajuster largeur, taille ou libelles localises des boutons afin d'afficher les variantes completement a 1080p, puis verifier les langues les plus longues.
  Preuves : checkpoints/20260721-204046-945877-settings-personalization-right/screenshot.png
  Validation : Tous les libelles sont complets a 1080p.; Le focus et l'etat selectionne restent distincts.; Le scroll vertical continue d'amener les cartes basses a l'ecran.; Aucune option n'est decalee ou chevauchee.
```
