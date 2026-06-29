# Migration Report - AI Knowledge System

Date: 2026-06-29.

## 1. Anciens fichiers MD analyses

- `AGENTS.md`
- `PROJECT_NOTES.md`
- `TROUBLESHOOTING.md`
- `TRANSLATION_PROGRESS.md`
- `job_progress.md`

Aucun fichier legacy n'a ete supprime ou deplace.

## 2. Nouvelle structure creee

```text
docs/ai-knowledge/
  ROOT.md
  PROJECT_OVERVIEW.md
  AGENT_WORKFLOW.md
  LEGACY_SOURCES.md
  MIGRATION_REPORT.md
  KNOWLEDGE_SYSTEM_TEST_REPORT.md
  features/
    activation-license-trial-xtream.md
    catalog-playback.md
    monetization-consent-tracking.md
  ui-ux/
    tv-navigation-focus.md
    screens-home-profile-settings.md
  technical/
    android-architecture-build-release.md
    backend-admin-api-deploy.md
  decisions/
    2026-06-29-ai-knowledge-router.md
    2026-06-29-native-tv-player.md
    2026-06-29-documentation-update-policy.md
  worklog/
    AI_CHANGELOG.md
```

## 3. Logique de decoupage choisie

Le decoupage suit les axes qui reviennent dans les interventions SmartVision:
- acces produit et Xtream;
- catalogue et playback;
- UI TV et focus;
- backend/admin/API;
- monetisation/tracking;
- build/release;
- workflow agent et legacy.

Le root reste court et sert de routeur. Les fichiers detailles suivent un modele stable en 12 sections.

## 4. Domaines fonctionnels identifies

- Activation, licence, essai, mode gratuit avec pubs et configuration Xtream.
- Catalogue Xtream, Room, synchro, favoris, historique, progression, player.
- UI Android TV, focus, D-pad, popups, headers, i18n.
- Backend PHP, admin, site, account, paiement, email, APIs.
- Publicite VAST, consentement, feature flags, anomalies, comportement, diagnostics.
- Build Gradle, release APK, deploiement et update in-app.

## 5. Informations migrees

- Routes actives de `AppNavigation.kt`.
- Entree app `SplashActivity -> MainActivity`.
- Stack technique Kotlin/Compose/Room/DataStore/Retrofit/Media3.
- Endpoints activation et Xtream setup.
- Endpoints app config, ads, behavior, anomaly, diagnostics.
- Regles release: `assembleRelease`, `versionCode`, deploy script, update manifest.
- Regles focus TV et D-pad.
- Politique i18n: English par defaut, Francais comme alternative visible.
- Erreurs recurrentes references depuis `TROUBLESHOOTING.md`.

## 6. Informations ambiguues ou contradictoires

- `PROJECT_NOTES.md` et `AGENTS.md` contiennent des numeros de release anciens.
- `app/build.gradle.kts` lu le 2026-06-29 indique `versionCode 53` et `versionName 0.1.50`.
- Les trackers historiques contiennent plusieurs releases intermediaires; ils ne doivent pas etre utilises comme source live.
- Les feature flags prod peuvent differer des defaults PHP stockes dans le code.

## 7. Informations manquantes

- Schema MySQL complet non recopie; lire `server/public_html/sql/init_activation_tables.sql` et les services PHP si besoin.
- Etat production live non verifie pendant cette mission documentation.
- Captures UI non regenerees.
- Liste exhaustive de tous les textes a traduire non migree; utiliser `TRANSLATION_PROGRESS.md` pour i18n.

## 8. Anciens fichiers conserves

- `AGENTS.md`
- `PROJECT_NOTES.md`
- `TROUBLESHOOTING.md`
- `TRANSLATION_PROGRESS.md`
- `job_progress.md`

Ils restent dans leur emplacement initial.

## 9. Recommandations

- Ajouter une entree changelog apres chaque intervention.
- Mettre a jour le fichier specialise concerne des qu'un ecran, endpoint ou workflow change.
- Laisser `PROJECT_NOTES.md` comme archive ou le reduire plus tard seulement si demande explicite.
- Ajouter une decision separee pour tout choix produit durable.
- Pour chaque release, verifier la prod actuelle avant d'utiliser un numero de version issu des docs.

## 10. Limites de la migration

- Mission realisee sans build ni tests applicatifs, conformement au mode documentation only.
- Les simulations sont documentaires et ne prouvent pas un comportement runtime.
- Les informations live prod peuvent avoir change; elles doivent etre verifiees avant release/deploy.
