# Audit refonte profils - 2026-07-15

## Cible

- Fire TV `AFTSSS`, Android 9 / API 28.
- Resolution native restauree: `1920x1080`, densite `320`.
- Package `com.smartvision.svplayer`, release locale `0.1.120` / code `168`.
- Installation `adb install -r` reussie; certificat identique a l'application presente.

## Valide

- Migration Room 17 -> 18 au demarrage sans erreur de validation.
- Aucun crash, ANR ou echec Room cible dans Logcat.
- `Info profil`: profil actif, compteurs reels, elements masques, derniere synchronisation et action unique de synchronisation visibles.
- Routage D-pad menu -> Changer -> Synchroniser et retour visuellement verifie.
- `Gerer les profils`: route distincte, liste profils puis ajouts Kids/standard, detail non sensible, actions admin protegees.
- ADMIN non supprimable; lock et edition visibles sans manipulation du PIN ni des donnees.

## Limites

- Aucun profil QA temporaire n'a ete cree: le PIN du profil ADMIN reel n'a ete ni demande, ni devine, ni modifie.
- Creation, modification, lock/unlock et suppression n'ont donc pas ete valides jusqu'a persistance sur la Fire TV.
- Les captures 720p et 4K synthetiques ont ete realisees sur le picker. L'override 4K a provoque des frames transitoires corrompues du compositeur Fire OS; `wm size reset` a restaure `1920x1080` et un mouvement D-pad a force une frame propre.

## Remarques utilisateur a traiter en fin de revue

- Home: l'image du hero ne s'affiche plus alors que le cadre et le texte restent visibles.

## Captures utiles

- `checkpoints/20260715-051652-294481-06-info-profile/screenshot.png`
- `checkpoints/20260715-051818-242021-08-info-sync-focus-stable/screenshot.png`
- `checkpoints/20260715-051939-851221-10-manage-profiles-stable/screenshot.png`
- `checkpoints/20260715-053325-906554-13-picker-recompose/screenshot.png`
