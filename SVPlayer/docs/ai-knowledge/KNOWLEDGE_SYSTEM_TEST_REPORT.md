# Knowledge System Test Report

Date: 2026-06-29.

## Verdict final

Nouveau systeme utilisable tel quel pour orienter Codex, avec un point a surveiller: les informations de production live et les numeros de release doivent toujours etre verifies avant build/deploiement.

## Simulation 1 - Activation, licence et essai gratuit

Demande simulee:
Corriger le flux ou les 7 jours d'essai doivent commencer apres validation des identifiants Xtream.

### A. Ancien systeme documentaire

Fichiers legacy qui auraient ete lus:
- `AGENTS.md`
- `PROJECT_NOTES.md`
- possiblement `job_progress.md`

Informations utiles trouvees:
- flux activation;
- endpoints device_status, start_trial, create_playlist_setup_session;
- AppNavigation et ActivationViewModel.

Informations inutiles lues:
- sections Home, player, deploiement ancien, anciennes releases.

Risques de confusion:
- versions de release anciennes;
- trop d'informations ecrans non liees.

Informations manquantes:
- routage direct vers `trial_pending_xtream`.

Reponse probable de Codex:
- lire beaucoup de contexte, puis inspecter `ActivationViewModel`, `AppNavigation`, API devices.

Fichiers de code probablement concernes:
- `ActivationViewModel.kt`
- `ActivationRepository.kt`
- `AppNavigation.kt`
- `api/devices/start_trial.php`
- `api/create_playlist_setup_session.php`
- `api/device_status.php`
- `xtream/index.php`

### B. Nouveau systeme documentaire

Fichier racine lu:
- `docs/ai-knowledge/ROOT.md`

Fichiers specialises selectionnes:
- `features/activation-license-trial-xtream.md`
- `technical/backend-admin-api-deploy.md`
- `features/monetization-consent-tracking.md` si free_with_ads impacte.

Pourquoi:
- mots-cles `essai gratuit`, `trial_pending_xtream`, `device_status`, `playlist_config`.

Informations utiles trouvees:
- workflow exact activation -> Xtream -> finalisation essai;
- fichiers Android/API concernes;
- regles a ne pas casser.

Informations inutiles evitees:
- docs UI generales, release legacy, anciens trackers.

Informations manquantes:
- schema SQL exact a confirmer dans code si modification DB.

Risques restants:
- etat prod live a verifier.

Reponse probable de Codex:
- inspecter directement les fichiers activation/API puis faire une correction ciblee.

Fichiers de code probablement concernes:
- ceux listes dans la section A.

Fichiers MD a mettre a jour apres intervention:
- `features/activation-license-trial-xtream.md`
- `technical/backend-admin-api-deploy.md` si endpoint change
- `worklog/AI_CHANGELOG.md`

### C. Verdict de comparaison

- Gain de precision: fort.
- Reduction de lecture inutile: forte.
- Reduction du risque d'erreur: forte.
- Defaut detecte: le root devait inclure explicitement `trial_pending_xtream`.
- Correction appliquee: mot-cle ajoute au root et au fichier specialise.

## Simulation 2 - Interface TV, focus et navigation telecommande

Demande simulee:
Corriger un crash `FocusRequester is not initialized` quand on quitte Live TV ou YouTube avec la telecommande.

### A. Ancien systeme documentaire

Fichiers legacy qui auraient ete lus:
- `AGENTS.md`
- `PROJECT_NOTES.md`
- `TROUBLESHOOTING.md`

Informations utiles trouvees:
- regle UI actionnable focusable;
- routes actives;
- notes crash/focus dans troubleshooting si presentes.

Informations inutiles lues:
- backend, release, activation, SQL, commerce.

Risques de confusion:
- corriger un symptome UI sans verifier la route active ou la stack fatale.

Informations manquantes:
- route courte vers `ui/focus`, `FocusModifiers`, `TvFocusStyle`.

Reponse probable de Codex:
- lire des sections longues puis rechercher `FocusRequester`.

Fichiers de code probablement concernes:
- `LiveTvScreen.kt`
- `YoutubeScreen.kt`
- `FocusModifiers.kt`
- `TvFocusStyle.kt`
- `AppNavigation.kt`

### B. Nouveau systeme documentaire

Fichier racine lu:
- `ROOT.md`

Fichiers specialises selectionnes:
- `ui-ux/tv-navigation-focus.md`
- `ui-ux/screens-home-profile-settings.md`
- `features/catalog-playback.md` si Live/player implique.

Pourquoi:
- mots-cles `FocusRequester`, `D-pad`, `navigation telecommande`, `YouTube`.

Informations utiles trouvees:
- cause probable FocusRequester hors composition;
- fichiers focus/UI;
- regles a ne pas casser.

Informations inutiles evitees:
- backend/admin/release.

Informations manquantes:
- stack fatale exacte necessaire avant correction crash.

Risques restants:
- sans logcat/anomalie, risque d'hypothese incomplete.

Reponse probable de Codex:
- recuperer evidence crash, inspecter focus wiring, appliquer correction ciblee.

Fichiers de code probablement concernes:
- ceux listes dans la section A.

Fichiers MD a mettre a jour apres intervention:
- `ui-ux/tv-navigation-focus.md`
- `features/catalog-playback.md` si player impacte
- `worklog/AI_CHANGELOG.md`

### C. Verdict de comparaison

- Gain de precision: fort.
- Reduction de lecture inutile: forte.
- Reduction du risque d'erreur: moyenne a forte.
- Defaut detecte: `FocusRequester` devait etre dans les mots-cles du root.
- Correction appliquee: mot-cle ajoute.

## Simulation 3 - Lecteur video, playlist et ExoPlayer

Demande simulee:
Corriger un probleme ou le Live TV plein ecran echoue sur `.ts` mais devrait essayer `.m3u8`.

### A. Ancien systeme documentaire

Fichiers legacy qui auraient ete lus:
- `AGENTS.md`
- `PROJECT_NOTES.md`

Informations utiles trouvees:
- Media3 ExoPlayer;
- URL Xtream live `.ts` et fallback `.m3u8`;
- chemins player.

Informations inutiles lues:
- activation web, admin, releases, site public.

Risques de confusion:
- modifier le mauvais package si ancienne architecture mentionnee.

Informations manquantes:
- liste courte des fichiers de playback.

Reponse probable de Codex:
- inspecter AppNavigation, FullScreenPlayer, XtreamUrlFactory.

Fichiers de code probablement concernes:
- `FullScreenPlayerScreen.kt`
- `XtreamUrlFactory.kt`
- `DefaultCatalogRepository.kt`
- `UseCases.kt`

### B. Nouveau systeme documentaire

Fichier racine lu:
- `ROOT.md`

Fichiers specialises selectionnes:
- `features/catalog-playback.md`
- `technical/android-architecture-build-release.md` si build requis.

Pourquoi:
- mots-cles `ExoPlayer`, `Live TV`, `.m3u8`, `playlist`.

Informations utiles trouvees:
- chemin de lecture;
- URL live/fallback;
- regles a ne pas casser.

Informations inutiles evitees:
- admin PHP et docs i18n.

Informations manquantes:
- stack runtime et logs player exacts.

Risques restants:
- fallback peut dependre du fournisseur Xtream.

Reponse probable de Codex:
- inspecter construction de PlaybackRequest et gestion d'erreur ExoPlayer.

Fichiers de code probablement concernes:
- ceux listes dans la section A.

Fichiers MD a mettre a jour apres intervention:
- `features/catalog-playback.md`
- `worklog/AI_CHANGELOG.md`

### C. Verdict de comparaison

- Gain de precision: fort.
- Reduction de lecture inutile: forte.
- Reduction du risque d'erreur: forte.
- Defaut detecte: aucun bloquant apres ajout des chemins player.
- Correction appliquee: chemins player et URL fallback inclus dans le fichier specialise.

## Simulation 4 - Panel admin, serveur et gestion des fonctionnalites

Demande simulee:
Ajouter une nouvelle fonctionnalite premium dans l'admin et s'assurer que l'app la recoit via `api/app_config.php`.

### A. Ancien systeme documentaire

Fichiers legacy qui auraient ete lus:
- `PROJECT_NOTES.md`
- `TROUBLESHOOTING.md`
- `AGENTS.md`

Informations utiles trouvees:
- backend PHP;
- app_config;
- deploy cPanel;
- probleme connu feature flags stockes.

Informations inutiles lues:
- architecture Android complete, player, catalogues.

Risques de confusion:
- oublier d'ajouter un nouveau PHP au script de deploy si fichier cree;
- supposer que les defaults PHP suffisent alors que prod stocke `app_feature_access`.

Informations manquantes:
- route directe vers admin/app_config/deploy.

Reponse probable de Codex:
- inspecter admin/index.php, app_config.php et deploy script apres lecture longue.

Fichiers de code probablement concernes:
- `server/public_html/admin/index.php`
- `server/public_html/api/app_config.php`
- `scripts/deploy_activation_phase1.ps1`
- `AppConfigRepository.kt`
- `AppNavigation.kt`

### B. Nouveau systeme documentaire

Fichier racine lu:
- `ROOT.md`

Fichiers specialises selectionnes:
- `technical/backend-admin-api-deploy.md`
- `features/monetization-consent-tracking.md`
- `ui-ux/screens-home-profile-settings.md` si lock UI.

Pourquoi:
- mots-cles `admin`, `app_config`, `app_feature_access`, `feature flags`.

Informations utiles trouvees:
- endpoint et table/settings;
- regle nouveau PHP = deploy script;
- prod stored flags a verifier.

Informations inutiles evitees:
- player et sync catalogue.

Informations manquantes:
- champs exacts du formulaire admin a confirmer dans `admin/index.php`.

Risques restants:
- prod peut avoir un etat stocke different.

Reponse probable de Codex:
- modifier PHP/admin de facon ciblee, verifier API publique apres deploy demande.

Fichiers de code probablement concernes:
- ceux listes dans la section A.

Fichiers MD a mettre a jour apres intervention:
- `technical/backend-admin-api-deploy.md`
- `features/monetization-consent-tracking.md`
- `worklog/AI_CHANGELOG.md`

### C. Verdict de comparaison

- Gain de precision: fort.
- Reduction de lecture inutile: forte.
- Reduction du risque d'erreur: forte.
- Defaut detecte: le root devait exposer `app_feature_access`.
- Correction appliquee: mot-cle ajoute aux domaines backend et monetisation.

## Simulation 5 - Tracking, comportement utilisateurs et futur systeme de publicite

Demande simulee:
Analyser pourquoi des pubs sont autorisees mais ne demarrent pas et preparer une evolution tracking.

### A. Ancien systeme documentaire

Fichiers legacy qui auraient ete lus:
- `PROJECT_NOTES.md`
- `TROUBLESHOOTING.md`
- possiblement anciens trackers.

Informations utiles trouvees:
- ads config;
- VAST proxy;
- anomaly/behavior events;
- regle fail-open.

Informations inutiles lues:
- activation detaillee, Home, settings, releases non liees.

Risques de confusion:
- diagnostiquer eligibility au lieu de reachability VAST;
- oublier les endpoints `ads-vast` et `ads-events`.

Informations manquantes:
- vue consolidee ads + behavior + anomaly + diagnostics.

Reponse probable de Codex:
- chercher plusieurs fois dans data/monetization puis backend.

Fichiers de code probablement concernes:
- `MonetizationManager.kt`
- `IdleVastAdLoader.kt`
- `AdsEventReporter.kt`
- `api/ads_service.php`
- `api/app/ads-config.php`
- `api/app/ads-vast.php`
- `api/app/ads-events.php`
- `api/app/behavior-events.php`
- `api/app/anomaly-events.php`

### B. Nouveau systeme documentaire

Fichier racine lu:
- `ROOT.md`

Fichiers specialises selectionnes:
- `features/monetization-consent-tracking.md`
- `technical/backend-admin-api-deploy.md`
- `features/catalog-playback.md` si impact player.

Pourquoi:
- mots-cles `ads`, `VAST`, `behavior-events`, `anomaly-events`, `future systeme de pub`.

Informations utiles trouvees:
- endpoints tracking;
- regle player-only/fail-open;
- proxy VAST stable;
- fichiers Android et PHP pertinents.

Informations inutiles evitees:
- i18n, release legacy, catalogues non player.

Informations manquantes:
- donnees prod recentes a interroger.

Risques restants:
- sans telemetry live, cause non certaine.

Reponse probable de Codex:
- verifier ads-config, ads-vast, events recents, puis corriger le chemin le plus court.

Fichiers de code probablement concernes:
- ceux listes dans la section A.

Fichiers MD a mettre a jour apres intervention:
- `features/monetization-consent-tracking.md`
- `technical/backend-admin-api-deploy.md`
- `worklog/AI_CHANGELOG.md`

### C. Verdict de comparaison

- Gain de precision: fort.
- Reduction de lecture inutile: forte.
- Reduction du risque d'erreur: forte.
- Defaut detecte: les mots-cles `ads-vast`, `behavior-events`, `anomaly-events` devaient etre visibles au root.
- Correction appliquee: mots-cles ajoutes.

## Corrections appliquees apres simulations

- Ajout de `trial_pending_xtream` au root et au domaine activation.
- Ajout de `FocusRequester` au root et au domaine UI/focus.
- Ajout de `app_feature_access` au root et aux domaines backend/monetisation.
- Ajout de `ads-vast`, `behavior-events`, `anomaly-events` au root et au domaine monetisation.
- Ajout des fichiers MD a mettre a jour apres chaque type d'intervention.
- Clarification que les versions/release legacy sont obsoletes et doivent etre verifiees.

## Corrections restantes

- Aucune correction structurelle bloquante.
- Ajouter plus tard un fichier dedie SQL si le schema backend devient un sujet frequent.

## Conclusion

Le nouveau systeme reduit fortement la lecture inutile et route mieux Codex vers les fichiers de code utiles. Il reste volontairement compact; les fichiers legacy ne servent plus qu'en secours.
