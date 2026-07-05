# Monetisation, Consentement, Tracking et Publicite

Derniere mise a jour: 2026-07-05.

## 1. Objectif

Documenter les modes d'acces monetises, le consentement TV, les feature flags, les publicites VAST, les evenements pub, les anomalies, le comportement utilisateur et les diagnostics device.

## 2. Fonctionnement actuel

L'application distingue premium, essai, essai expire et gratuit avec pubs. Les fonctionnalites peuvent etre autorisees selon le statut via `api/app_config.php`. Le mode gratuit avec pubs utilise une logique player-only avec configuration distante et fallback de lecture si la pub echoue.

Depuis le 2026-07-05, les nouvelles surfaces Recorder et Media Center doivent passer par `domain/access/PremiumFeatureGate.kt`. Ce gate centralise les decisions Premium/Trial/Free Ads/Expired a partir de `AppRuntimeConfig.features` et `MonetizationStatus`, et renvoie un etat reutilisable par l'UI: autorise, verrou Premium visible, expire, source non supportee ou desactive par config. La route/header `Media` consomme maintenant le flag `media_center`: visible verrouille avec couronne si upgrade requis, masque si desactive par config.

Le tracking actuellement implemente couvre:
- pub: `ads_events`;
- comportement generique Live TV, Movies, Series, Episodes et YouTube: `app_behavior_events`;
- segmentation utilisateur: `user_segments`;
- agregats comportementaux quotidiens: `user_behavior_daily`;
- anomalies: endpoint `api/app/anomaly-events.php`;
- diagnostics device: `api/app/device-diagnostics.php`;
- consentement TV: `app_consent_receipts`.

Depuis le 2026-06-30, les erreurs de connexion Xtream sont remontees comme anomalies `XTREAM_FAILED` avec anti-doublon cote Android. Le payload inclut le code TV public quand disponible, la version app, le type d'erreur et un contexte non secret.

Segmentation:
- `docs/ai-knowledge/features/user-behavior-segmentation-ads-v1.md` decrit maintenant la V1 en cours: tracking contenu consomme, interpretation region/pays/langue/interets et dashboard admin Segmentation.

## 3. Workflow utilisateur

- Au premier usage ou quand la version change, le consentement TV peut etre affiche.
- Certaines fonctionnalites comme YouTube ou parental control peuvent etre bloquees selon le statut.
- En mode gratuit avec pubs, une pub peut se lancer avant contenu player, selon frequence et limites.
- Si la pub echoue et que la politique le permet, la lecture ne doit pas etre bloquee.

## 4. Workflow technique

Android:
- `data/monetization/MonetizationManager.kt`
- `data/monetization/MonetizationStore.kt`
- `data/monetization/IdleVastAdLoader.kt`
- `data/monetization/AdsEventReporter.kt`
- `data/monetization/AdConfig.kt`
- `data/appconfig/AppConfigRepository.kt`
- `ui/appconfig/ConsentDialog.kt`
- `data/anomaly/AnomalyReporter.kt`
- `data/xtream/XtreamConnectionManager.kt`
- `data/diagnostics/DeviceDiagnosticsReporter.kt`
- `data/behavior/BehaviorReporter.kt`
- `domain/access/PremiumFeatureGate.kt`

Backend:
- `api/app_config.php`
- `api/ads_service.php`
- `api/app/ads-config.php`
- `api/app/ads-events.php`
- `api/app/ads-vast.php`
- `api/app/anomaly-events.php`
- `api/app/behavior-events.php`
- `api/app/device-diagnostics.php`
- `api/behavior_service.php`
- `api/anomaly_service.php`
- `api/device_diagnostics_service.php`

## 5. Ecrans concernes

- Consentement TV popup
- Header et placeholder YouTube premium
- Header et ecran Media Center MVP
- Profil licence/acces
- Lecteur plein ecran avec preroll
- Admin > Fonctionnalites / Consentement / Publicites / diagnostics

## 6. Fichiers de code concernes

- `app/src/main/java/com/smartvision/svplayer/data/monetization/*`
- `app/src/main/java/com/smartvision/svplayer/ui/appconfig/*`
- `app/src/main/java/com/smartvision/svplayer/data/appconfig/*`
- `app/src/main/java/com/smartvision/svplayer/data/anomaly/*`
- `app/src/main/java/com/smartvision/svplayer/data/diagnostics/*`
- `app/src/main/java/com/smartvision/svplayer/data/youtube/*Behavior*`
- `server/public_html/api/ads_service.php`
- `server/public_html/api/app_config.php`
- `server/public_html/admin/index.php`
- `docs/ai-knowledge/features/user-behavior-segmentation-ads-v1.md`

## 7. Donnees / API / Backend / Admin

Endpoints:
- `GET api/app_config.php`
- `POST api/app_config.php` pour consent receipt
- Android appelle `GET api/app/ads-config`, reecrit vers `api/app/ads-config.php`
- `vastTagUrl` expose par ads-config pointe vers `api/app/ads-vast`, reecrit vers `api/app/ads-vast.php`
- Android appelle `POST api/app/ads-events`, reecrit vers `api/app/ads-events.php`
- Android appelle `POST api/app/anomaly-events`, reecrit vers `api/app/anomaly-events.php`
- Android appelle `POST api/app/behavior-events`, reecrit vers `api/app/behavior-events.php`
- Android appelle actuellement `POST api/app/device-diagnostics.php` directement

Evenements comportementaux actuellement acceptes cote serveur:
- `VIDEO_OPENED`
- `PLAYER_READY`
- `PLAY_PAUSE`
- `VIDEO_COMPLETED`
- `SUGGESTION_OPENED`
- `CONTENT_OPENED`
- `PLAYBACK_STARTED`
- `PLAYBACK_PROGRESS`
- `PLAYBACK_COMPLETED`
- `FAVORITE_ADDED`
- `FAVORITE_REMOVED`
- `SEARCH_PERFORMED`
- `CATEGORY_OPENED`
- `PLAYER_ERROR`

Tables ou settings:
- `app_settings`
- `app_feature_access`
- `app_feature_access` inclut les defaults Recorder/Media: `recorder`, `media_center`, `media_file_management`, `media_phone_transfer` avec Premium oui, Trial oui, Free Ads non.
- `app_consent_receipts`
- `ads_settings`
- `ads_events`
- `app_behavior_events`
- `user_behavior_daily`
- `user_segments`
- `app_anomaly_events.public_device_code`

## 8. Dependances

- Activation/licence pour statut utilisateur.
- Backend/admin/API pour flags et tableaux de bord.
- Lecteur plein ecran pour preroll.
- UI TV/focus pour consentement et lock premium.

## 9. Regles a ne pas casser

- Les pubs doivent rester dans le lecteur, pas dans toute la navigation.
- La lecture doit rester fail-open quand la politique le permet.
- Ne pas exposer les tags VAST prives ni les cles provider.
- Les feature flags prod stockes peuvent override les defaults: verifier `api/app_config.php`.
- Ne pas rendre l'admin dependent d'un service optionnel qui peut faire HTTP 500.
- Garder les evenements rates limites pour eviter le bruit.
- Garder l'anti-doublon Xtream cote Android pour eviter les boucles d'anomalies identiques.
- Un flux media bloque en buffering doit remonter une anomalie Xtream non secrete (`XTREAM_FAILED`) en plus des evenements player techniques, afin que le panel admin affiche le probleme par code TV.
- Ne pas stocker de nouveaux evenements comportementaux avec `content_type = UNKNOWN`; les lignes UNKNOWN sont purgees par le service comportement.
- Les segments region/pays/langue/interets doivent rester explicables par evidence textuelle nettoyee, pas par modele opaque.

## 10. Problemes connus

- `app_feature_access` stocke en prod peut conserver d'anciennes valeurs.
- Une URL VAST directe peut etre inaccessible depuis device; le proxy `api/app/ads-vast.php` est le chemin stable.
- Nouveaux services PHP non ajoutes au script de deploy peuvent casser l'admin en prod.

## 11. Quand lire ce fichier ?

Lire ce fichier si la demande concerne:
- free with ads;
- publicite;
- VAST;
- consentement TV;
- YouTube bloque premium;
- parental control bloque par statut;
- app_config;
- behavior events;
- segmentation utilisateur;
- ciblage publicitaire;
- anomaly events;
- diagnostics device;
- future systeme de pub.
- Recorder ou Media Center avec verrou Premium.

Ne pas lire ce fichier si la demande concerne uniquement:
- grille Movies sans lock;
- build release sans pub/config;
- code activation sans mode gratuit avec pubs.

## 12. Historique court

- 2026-06-29: migration vers documentation specialisee.
- 2026-06-29: ajout des mots-cles `ads-vast`, `app_feature_access`, `behavior-events`, `anomaly-events`.
- 2026-06-30: ajout de la spec V1 segmentation utilisateur et ciblage publicitaire.
- 2026-06-30: clarification etat actuel vs cible future, routes extensionless et exception `device-diagnostics.php`.
- 2026-06-30: mise a jour etat actuel apres implementation segmentation admin et inference region/pays/langue/interets.
- 2026-06-30: anomalies Xtream `XTREAM_FAILED` ajoutees avec code TV public et classification simple.
- 2026-06-30: les blocages buffering du lecteur Xtream basculent maintenant l'etat connexion en erreur et declenchent une anomalie `XTREAM_FAILED`.
- 2026-07-05: ajout de `PremiumFeatureGate` pour Recorder/Media Center et defaults `app_feature_access` associes.
- 2026-07-05: route/header `Media` branchee sur `media_center` avec couronne et popup Premium.
