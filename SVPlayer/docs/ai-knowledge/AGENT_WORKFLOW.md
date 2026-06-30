# Agent Workflow

Derniere mise a jour: 2026-06-30.

## 1. Objectif

Definir la procedure obligatoire pour les futures interventions Codex sur SmartVision afin de limiter les lectures inutiles, garder la documentation a jour et eviter les regressions.

## 2. Workflow standard

1. Lire `docs/ai-knowledge/ROOT.md`.
2. Comprendre la demande utilisateur.
3. Identifier les domaines concernes avec les mots-cles du root.
4. Lire uniquement les fichiers MD specialises utiles.
5. Lire uniquement les fichiers de code necessaires.
6. Appliquer la correction ou la modification demandee.
7. Tester si possible et selon la demande.
8. Mettre a jour les fichiers MD concernes.
9. Ajouter une entree courte dans `docs/ai-knowledge/worklog/AI_CHANGELOG.md`.
10. Appliquer la boucle `docs/ai-knowledge/CONTINUOUS_IMPROVEMENT.md` si un ecart doc/code est trouve.
11. Signaler toute documentation insuffisante, obsolete ou contradictoire.

## 3. Regles de lecture legacy

Ne pas lire `PROJECT_NOTES.md` ou `AGENTS.md` par defaut. Les utiliser seulement si:
- le root et les fichiers specialises ne suffisent pas;
- une information historique est necessaire;
- une ancienne procedure doit etre comparee;
- un conflit documentaire doit etre tranche.

`TROUBLESHOOTING.md` doit etre consulte avant de refaire une longue recherche sur une erreur deja rencontree.

## 4. Mise a jour documentaire obligatoire

Apres toute intervention future, verifier:
- fonctionnalite changee;
- workflow utilisateur change;
- regle metier changee;
- dependance changee;
- ecran change;
- endpoint, table ou parametre admin change;
- fichier MD specialise a mettre a jour;
- decision structurante a documenter.

Si oui:
- mettre a jour le ou les fichiers concernes;
- ajouter un changelog court;
- creer une decision si le choix empeche une future mauvaise direction;
- documenter les erreurs reutilisables dans `TROUBLESHOOTING.md`.

## 5. Release Android

Quand l'utilisateur demande une release:
- verifier le `versionCode` live et local si necessaire;
- incrementer `versionCode` avant de produire un nouvel APK si une release est demandee;
- lancer `.\scripts\guard_release_version.ps1` avant le build pour bloquer un numero deja publie ou installe;
- bypass automatiquement `compileDebugKotlin` et `testDebugUnitTest`;
- lancer directement `.\gradlew.bat assembleRelease`;
- utiliser un timeout de 15 minutes minimum;
- si le build timeout, verifier les processus Java/Gradle et les artefacts avant de relancer;
- relancer `.\scripts\guard_release_version.ps1 -RequireBuildMetadata` apres le build pour verifier `output-metadata.json`;
- publier avec `scripts/deploy_activation_phase1.ps1 -SkipInstall` sauf consigne contraire;
- toujours deployer le backend apres un nouveau build release livrable, sauf demande explicite de build local uniquement;
- verifier `api/app_update.php`, `downloads/smartvision-tv.version.json`, APK versionne et stable URL cache-busted.

## 6. Tests selon domaine

- Documentation only: verifier existence, liens et coherence des MD; ne pas build.
- PHP: `php -l` sur les fichiers touches, puis test endpoint si deploiement demande.
- Android code: build approprie; pour release, pas de debug build sauf demande explicite.
- UI TV: installer sur `SmartVision_TV_720p_Light` seulement si demande ou si validation UI requise.
- Production: ne pas deployer sans demande explicite.

## 7. Erreurs recurrentes

Avant plusieurs essais:
- lire le message exact;
- chercher dans `TROUBLESHOOTING.md`;
- appliquer la correction la plus simple;
- ne pas repeter la meme commande sans changement d'approche;
- ajouter une note si la solution est reutilisable.

## 8. Secrets et securite

Ne jamais afficher:
- contenu de `local.properties`;
- cle cPanel;
- identifiants MySQL;
- mots de passe Xtream;
- keystore ou mots de passe de signature;
- tokens temporaires encore valides.

Les docs doivent decrire les cles attendues sans valeur reelle.

## 9. Auto-amelioration du Knowledge System

Apres chaque intervention, verifier rapidement:
- le root a-t-il route vers le bon domaine ?
- le MD specialise contenait-il une information fausse, incomplete ou trop vague ?
- un fichier de code important manquait-il dans la liste ?
- une spec future a-t-elle ete confondue avec l'etat actuel ?

Si oui:
- corriger le MD specialise;
- ajouter une entree courte dans `worklog/AI_CHANGELOG.md`;
- creer une decision seulement si le choix est structurant;
- marquer `A_VERIFIER` quand la preuve code/prod manque.

Pour les audits plus larges, lire `docs/ai-knowledge/CONTINUOUS_IMPROVEMENT.md`.
