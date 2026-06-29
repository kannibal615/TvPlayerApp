# Auto Trial Rollout

Date: 2026-06-29

## Objectif

Simplifier l'acces a SmartVision: apres consentement, l'app demarre automatiquement un essai gratuit, affiche directement la configuration Xtream, puis lance les 7 jours reels seulement apres validation des identifiants Xtream.

## Regles retenues

- Ne plus afficher l'ecran d'activation au premier acces eligible.
- Garder l'ecran d'expiration existant apres fin d'essai.
- Garder l'achat de licence et le mode gratuit avec pubs existants.
- Ne pas recreer d'essai si l'appareil a deja utilise son essai.
- Les 7 jours commencent quand les identifiants Xtream sont enregistres ou deja presents.

## Points de vigilance

- Ne pas toucher aux changements utilisateur deja presents dans le repo.
- Ne pas deployer si le build release echoue.
- Verifier que `pending_xtream` ne reste pas bloque si un compte Xtream local existe deja.
- Verifier les routes backend de trial, playlist setup, device status et free ads.

## Suivi

- [x] Modifier l'orchestration Android pour demarrer l'essai automatiquement.
- [x] Finaliser l'essai apres presence d'un compte Xtream.
- [x] Verifier les tests backend.
- [x] Verifier la compilation release.
- [x] Bumper la version avant publication.
- [x] Deployer APK + backend et verifier les endpoints publics.

## Validation

- PHP lint OK sur les endpoints trial/status/setup Xtream.
- `server/tests/monetization_rules_test.php` OK.
- APK release genere en `48 / 0.1.45`.
- Signature APK verifiee avec `apksigner` v1/v2.
- Production verifiee: `api/app_update.php`, `downloads/smartvision-tv.version.json`, APK versionne, APK stable, `ads-config`, `ads-vast.php`, `/xtream/`.
