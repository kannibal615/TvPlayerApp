# Ecrans Home, Catalogues, Profile, Settings et YouTube

Derniere mise a jour: 2026-07-05.

## 1. Objectif

Cartographier les principaux ecrans utilisateur et les chemins de code pour orienter rapidement les interventions UI.

## 2. Fonctionnement actuel

Les ecrans actifs sont routes depuis `ui/navigation/AppNavigation.kt`. Le header principal expose Home, Live TV, Movies, Series et YouTube, les actions globales, puis la date/heure non focusable a droite sur deux lignes (`HH:mm:ss`, `dd/MM/yyyy`). Le bouton licence apparait selon le statut. Les ecrans de contenu affichent un QR Xtream si aucun compte n'est configure. Si un compte Xtream existe mais que la verification rapide echoue, Home reste accessible et les entrees Live TV / Movies / Series sont bloquees par popup et overlay. Pendant une verification Xtream en cours, la navigation catalogue peut rester temporairement protegee si aucun etat connecte valide n'est deja connu, mais Home et le header ne doivent afficher les warnings/overlays "Connexion indisponible" qu'apres un echec Xtream confirme.

## 3. Workflow utilisateur

- Home: hero, cartes Live/Movies/Series, continue watching si disponible, `Trending movies`, `Trending series`, notifications/profil/settings.
- Quand l'utilisateur est deja sur Home, le bouton HOME du header ne doit pas relancer la navigation vers la meme route: il rafraichit localement slides/tendances, remet les listes horizontales a l'index `0`, remonte l'ecran et redonne le focus aux categories.
- Home affiche un overlay "Connexion indisponible" sur les cartes et reprises de lecture quand Xtream est indisponible avec echec confirme, pas pendant le simple etat `checking`.
- Home ne doit pas afficher cet overlay pendant un simple refresh rapide si la derniere verification Xtream est encore `CONNECTED`.
- Home affiche ses donnees a partir de petits jeux locaux: slides caches, historique recent limite a `10`, et tendances films/series `10 + 10` aleatoires depuis Room, hors adulte. Le premier rendu tendances reste local et leger; les metadonnees premium des cards visibles/proches ou focussees sont preparees ensuite en arriere-plan. Le splash ne bloque plus sur les details Xtream/backdrops, ne precharge plus Home et ne publie plus de chargement catalogue; Home relance un refresh tendances cache-first apres sa premiere frame.
- Depuis le 2026-07-03, Home decide la synchronisation automatique apres son premier rendu. `Synchronize` lance une synchro catalogue complete sur Home; `LoadLocal` n'est plus utilise pour bloquer ou charger l'interface depuis le splash. Pendant une vraie synchro, Home intercepte Back/D-pad/OK, empeche la navigation/clics et affiche un overlay sombre progressif sur les cards Live TV / Films / Series.
- Les overlays Home agissent comme une progression inversee pendant `Synchronize`: la card commence sombre, puis la partie gauche redevient normale selon le pourcentage de section. Les cards terminees perdent leur overlay; les cards en attente restent sombres. Les phases visibles viennent de `SyncStatus.SyncSectionPhase`: `WAITING`, `RUNNING`, `IMPORTING`, `COMPLETED`, `ERROR`; `LOADING_TRENDS` n'est plus declenche par la synchro allegee.
- `HomeViewModel` ne rafraichit plus automatiquement les tendances lourdes a l'initialisation. Les tendances Home sont tirees aleatoirement depuis Room via `HomeContentRepository`, sans recalcul `trending_media`, sans appels details Xtream et sans validation URL.
- Sur Home, les cards tendances films/series utilisent `TrendingContentRow`: portrait par defaut, focus court sans player, stabilisation `1s`, ancrage gauche de la card focussee, transformation 16:9 avec la meme transition visuelle que Continue watching, crossfade vers backdrop, fallback poster floute si aucun backdrop, puis mini-player Media3 apres transformation. Le preview demarre a environ 15% de la duree connue et tente 30% si la premiere frame n'arrive pas. Le son du mini-player demarre a `0f`, attend 1 seconde apres lancement, puis monte progressivement a `1f` sur 1 seconde; l'effet reste vivant jusqu'a disparition/changement du composable.
- Sur Home, Continue watching utilise aussi le mini-player Media3 au focus: le mini-player est cree seulement apres un focus stable court, le poster/thumbnail reste visible jusqu'a `onRenderedFirstFrame`, puis Films et Episodes lisent depuis la position de reprise; la boucle reste de 20 secondes avec overlay i18n `Resume playback` / `Reprendre la lecture`. Le son suit le meme fade-in local: 1 seconde muet, puis montee de 1 seconde vers le volume player complet. Pour les apercus LiveImmediate, l'effet audio ne doit pas etre annule juste apres `play()`. Les URLs preview qui echouent avec un conteneur Media3 non supporte sont ignorees en poster-only pour le reste de la session.
- Sur Home, les tendances Films/Series utilisent les posters locaux disponibles pour construire les lignes. Les details Xtream, backdrops, durees et samples series ne sont demandes que pour les items visibles/proches ou focussees, avec cache local `home_trending_preview_cache`.
- Sur Home, les lignes Continue watching / Trending movies / Trending series ont un padding interne horizontal pour que la premiere card, sa bordure focus et le mini-player ne soient pas tronques a gauche. Les slots `LazyRow` gardent une largeur stable quand les previews sont activees; le focus ne doit plus animer la largeur d'item lazy. Le scroll automatique horizontal est limite aux cas ou la card focussee n'est pas deja entierement visible.
- Les `LazyListState` des lignes Home ne sont pas sauvegardes/restaures entre jeux de donnees; ils sont recrees par signature de liste et remis a l'index `0`, afin que les tendances ne s'ouvrent pas sur un ancien item comme le 14e.
- Le D-pad bas/haut sur Home force la ligne cible a revenir a son premier item, annule tout scroll vertical Home deja lance, execute un seul scroll vertical deterministe, puis demande le focus apres la fin du scroll. Les lignes Continue watching / Trending movies / Trending series gardent une hauteur fixe pour que l'animation de largeur de la card focussee ne relance pas de corrections verticales.
- Les sections Continue watching / Trending movies / Trending series ne sont rendues que si elles contiennent des items; les cibles D-pad haut/bas tiennent compte de ces lignes absentes pour eviter un focus mort.
- Home routes secondaires: `continue_watching` et `trending` via `HomeCollectionsScreen`; la route `trending` lance aussi un refresh tendances cache-first si elle est ouverte directement.
- Live TV: categories, chaines, apercu puis plein ecran. Le mini-player d'apercu Live TV demarre muet, attend 1 seconde apres lancement, puis augmente progressivement le volume player a `1f` sur 1 seconde; le fallback `.m3u8`, le buffering et les erreurs restent inchanges.
- Movies: grille de films, detail, lecture.
- Series: grille, detail, saisons/episodes, lecture episode.
- Live TV / Movies / Series affichent les categories puis chargent les contenus par pages Room locales pendant le scroll, sans reconstruire toute la playlist en RAM.
- Live TV / Movies / Series chargent d'abord `20` categories Room maximum, puis completent discretement la liste complete dans leur ViewModel. Aucune categorie ni premiere page catalogue n'est prechauffee dans le splash.
- Les logos Live TV et images Films/Series affiches dans les listes, details, Home et historiques viennent des champs normalises en repository. Ne pas corriger les images directement dans chaque composable sauf besoin UI specifique; les ecrans doivent consommer les URL catalogue deja nettoyees.
- Info compte: licence, device, mode d'utilisation, expiration, compte Xtream, lien M3U, URL EPG, source active exclusive, QR achat/config. L'ecran reutilise le header principal de l'application, donne le focus initial au menu `Licence SmartVision`, et garde la section droite plus large/compacte pour servir de modele aux futurs ecrans profil/parametres. Dans Info compte, le bouton Synchroniser ouvre un popup de confirmation avec compteurs Live TV / Films / Series; apres succes ou erreur, Retour ferme le popup, ouvre Appareil et catalogue et y remet le focus.
- Info compte compacte la section source: icone utilisateur bleue pour le panneau, badge d'usage dans l'en-tete Licence SmartVision, expiration Xtream dans l'en-tete Info compte, identifiants Xtream sur une ligne, boutons Xtream en icones et bascules source plus petites.
- Quand M3U est actif, Movies et Series affichent un etat vide source-aware au lieu d'une erreur Xtream.
- Live TV affiche un badge bleu `E` a droite des lignes de chaines qui ont des programmes EPG locaux, y compris dans le dossier Historique quand la chaine existe encore en Room. Les dossiers Live TV avec EPG affichent leur compteur dans un cadre bleu au lieu d'ajouter un badge EPG separe. Le panneau details EPG sous le mini-player est focusable et scrollable au D-pad.
- Settings: experience video, personnalisation focus, langue, synchro, activite reseau, comptes Xtream, parental.
- Settings > Activite reseau affiche un panneau compact alimente par `NetworkActivityTracker`: travaux actifs, historique recent, progression, debit, taille de donnees, duree, source/section et erreurs. Le panneau droit est focusable et scrollable au D-pad pour voir toutes les activites. Les activites instrumentees incluent synchro catalogue Live/Films/Series, M3U, EPG, Home slides/tendances, verification Xtream, update APK et requetes HTTP SmartVision/Xtream sans exposer les query params ni secrets.
- YouTube: recherche/suggestions/player, favoris, parametres YouTube et queue autoplay, soumis a feature lock.
- Notifications: liste et ouverture du popup update si notification release.

## 4. Workflow technique

Navigation:
- `AppNavigation.kt` cree les repositories/viewmodels communs.
- `headerTabs()` definit les onglets.
- `navigateSingleTop()` evite les doublons.
- Les routes player sont separees par type de contenu.

Etat:
- `ActivationViewModel` pour acces;
- `ActivationViewModel.localStateReady` empeche le rendu temporaire de l'ecran activation avant la lecture du cache local;
- `AppConfigViewModel` pour feature flags et consent;
- `AppUpdateViewModel` pour update;
- `SettingsRepository` pour preferences.
- `NetworkActivityTracker` est cree dans `AppContainer` et survit aux changements d'ecran; `SettingsScreen` consomme son `StateFlow` pour afficher les travaux reseau en cours et recents.
- `HomeViewModel` seme son etat initial depuis `HomeSlidesRepository`, `UserContentRepository` et le cache `HomeContentRepository`; il recharge les tendances apres le rendu initial seulement si ce cache est absent ou si un refresh force est demande.
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
- `ui/settings/SettingsScreen.kt`
- `ui/settings/ParentalContentFilter.kt`
- `data/network/NetworkActivityTracker.kt`
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
- `NetworkActivityInterceptor` mesure les requetes OkHttp avec titre sanitise host/chemin uniquement; ne jamais afficher les query params Xtream, tokens, mots de passe ou URLs brutes de lecture.

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
- Garder les actions TV focusables.
- YouTube: ne pas recharger toute la liste de suggestions a chaque video; consommer la queue courante, enlever la video lancee et recharger en arriere-plan seulement les elements manquants.

## 10. Problemes connus

- Certaines traductions visibles restent a externaliser selon `TRANSLATION_PROGRESS.md`.
- Les releases anciennes dans les trackers ne sont pas la source actuelle.
- Les notifications update doivent ouvrir le popup update, pas seulement Home.
- Les notifications d'information, comme une configuration Playlist poussee depuis le site, restent non cliquables; seules les notifications de mise a jour ouvrent une action.
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
