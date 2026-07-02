# Ecrans Home, Catalogues, Profile, Settings et YouTube

Derniere mise a jour: 2026-07-02.

## 1. Objectif

Cartographier les principaux ecrans utilisateur et les chemins de code pour orienter rapidement les interventions UI.

## 2. Fonctionnement actuel

Les ecrans actifs sont routes depuis `ui/navigation/AppNavigation.kt`. Le header principal expose Home, Live TV, Movies, Series et YouTube. Le bouton licence apparait selon le statut. Les ecrans de contenu affichent un QR Xtream si aucun compte n'est configure. Si un compte Xtream existe mais que la verification rapide echoue, Home reste accessible et les entrees Live TV / Movies / Series sont bloquees par popup et overlay. Pendant une verification Xtream en cours, les entrees catalogue ne sont bloquees que si aucun etat connecte valide n'est deja connu; un etat `CONNECTED` en cours de refresh ne doit pas afficher l'overlay d'alerte.

## 3. Workflow utilisateur

- Home: hero, cartes Live/Movies/Series, continue watching si disponible, `Trending movies`, `Trending series`, notifications/profil/settings.
- Home affiche un overlay "Connexion indisponible" sur les cartes et reprises de lecture quand Xtream est indisponible.
- Home ne doit pas afficher cet overlay pendant un simple refresh rapide si la derniere verification Xtream est encore `CONNECTED`.
- Home affiche ses donnees a partir de petits jeux locaux: slides caches, historique recent limite a `10`, tendances films `10` et tendances series `10` issues de `trending_media`. Il ne doit pas relire les snapshots complets Movies / Series.
- Sur Home, les cards tendances films/series passent en largeur 16:9 au focus en gardant la hauteur actuelle; le poster reste affiche immediatement puis un mini-player Media3 muet demarre apres environ 650 ms avec segments a 10%, 25%, 45%, 65% et 80% du media.
- Home routes secondaires: `continue_watching` et `trending` via `HomeCollectionsScreen`.
- Live TV: categories, chaines, apercu puis plein ecran.
- Movies: grille de films, detail, lecture.
- Series: grille, detail, saisons/episodes, lecture episode.
- Live TV / Movies / Series affichent les categories puis chargent les contenus par pages Room locales pendant le scroll, sans reconstruire toute la playlist en RAM.
- Info compte: licence, device, mode d'utilisation, expiration, compte Xtream, lien M3U, URL EPG, source active exclusive, QR achat/config. L'ecran reutilise le header principal de l'application, donne le focus initial au menu `Licence SmartVision`, et garde la section droite plus large/compacte pour servir de modele aux futurs ecrans profil/parametres. Dans Info compte, le bouton Synchroniser ouvre un popup de confirmation avec compteurs Live TV / Films / Series; apres succes ou erreur, Retour ferme le popup, ouvre Appareil et catalogue et y remet le focus.
- Info compte compacte la section source: icone utilisateur bleue pour le panneau, badge d'usage dans l'en-tete Licence SmartVision, expiration Xtream dans l'en-tete Info compte, identifiants Xtream sur une ligne, boutons Xtream en icones et bascules source plus petites.
- Quand M3U est actif, Movies et Series affichent un etat vide source-aware au lieu d'une erreur Xtream.
- Live TV affiche un badge bleu `E` a droite des lignes de chaines qui ont des programmes EPG locaux.
- Settings: experience video, personnalisation focus, langue, synchro, comptes Xtream, parental.
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
- `HomeViewModel` seme son etat initial depuis `HomeSlidesRepository` et `UserContentRepository`, puis charge les tendances avec des requetes limitees de `CatalogRepository`.

## 5. Ecrans concernes

- `HomeScreen.kt`
- `HomeCollectionsScreen.kt`
- `ContinueWatchingRow.kt`
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
