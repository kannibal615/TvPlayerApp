# Monetisation, Consentement, Tracking et Publicite

Derniere mise a jour: 2026-06-30.

## 1. Objectif

Documenter les modes d'acces monetises, le consentement TV, les feature flags, les publicites VAST, les evenements pub, les anomalies, le comportement utilisateur et les diagnostics device.

## 2. Fonctionnement actuel

L'application distingue premium, essai, essai expire et gratuit avec pubs. Les fonctionnalites peuvent etre autorisees selon le statut via `api/app_config.php`. Le mode gratuit avec pubs utilise une logique player-only avec configuration distante et fallback de lecture si la pub echoue.

Le tracking couvre:
- pub: `ads_events`;
- comportement: `app_behavior_events`;
- anomalies: endpoint `api/app/anomaly-events.php`;
- diagnostics device: `api/app/device-diagnostics.php`;
- consentement TV: `app_consent_receipts`.

Spec d'evolution:
- `docs/ai-knowledge/features/user-behavior-segmentation-ads-v1.md` formalise la V1 de tracking comportemental, segmentation utilisateur et ciblage publicitaire.

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
- `data/diagnostics/DeviceDiagnosticsReporter.kt`
- `data/youtube/YoutubeBehaviorReporter.kt`

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
- `GET api/app/ads-config.php`
- `GET api/app/ads-vast.php`
- `POST api/app/ads-events.php`
- `POST api/app/anomaly-events.php`
- `POST api/app/behavior-events.php`
- `POST api/app/device-diagnostics.php`

Tables ou settings:
- `app_settings`
- `app_feature_access`
- `app_consent_receipts`
- `ads_settings`
- `ads_events`
- `app_behavior_events`

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

Ne pas lire ce fichier si la demande concerne uniquement:
- grille Movies sans lock;
- build release sans pub/config;
- code activation sans mode gratuit avec pubs.

## 12. Historique court

- 2026-06-29: migration vers documentation specialisee.
- 2026-06-29: ajout des mots-cles `ads-vast`, `app_feature_access`, `behavior-events`, `anomaly-events`.
- 2026-06-30: ajout de la spec V1 segmentation utilisateur et ciblage publicitaire.
