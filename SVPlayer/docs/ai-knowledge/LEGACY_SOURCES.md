# Legacy Sources

Derniere mise a jour: 2026-06-29.

## Objectif

Conserver les anciens fichiers MD comme sources historiques sans les relire par defaut. Le nouveau point d'entree est `docs/ai-knowledge/ROOT.md`.

## Fichiers legacy trouves

## `AGENTS.md`

Role probable:
- briefing rapide agent IA;
- resume produit, routes, ecrans, backend, build/deploiement, validation.

Informations migrees:
- objectif produit;
- regles non negociables;
- racines importantes;
- stack technique;
- entree app et routes;
- flux activation/Xtream;
- ecrans Android actifs;
- endpoints backend;
- build/deploiement;
- contraintes AVD.

Informations non migrees:
- anciennes valeurs de release publiee devenues potentiellement obsoletes;
- details longs de validation manuelle deja couverts par domaines specialises.

Informations ambiguues ou contradictoires:
- version release mentionnee plus ancienne que `app/build.gradle.kts` constate le 2026-06-29.

Recommandation:
- conserver comme source legacy et briefing humain; ne pas lire par defaut.

## `PROJECT_NOTES.md`

Role probable:
- documentation historique detaillee du projet.

Informations migrees:
- resume executif;
- tooling Android;
- architecture;
- AppContainer;
- navigation;
- activation;
- catalogue;
- Room;
- ecrans;
- backend;
- APIs;
- deploiement;
- points de vigilance.

Informations non migrees:
- details exhaustifs anciens qui doivent etre confirmes dans le code avant intervention;
- release 0.1.23/26, obsolete par rapport au Gradle local actuel.

Informations ambiguues ou contradictoires:
- `versionCode` et release connue ne correspondent pas au Gradle actuel.
- certaines mentions d'ancienne architecture `feature/*` doivent etre verifiees localement avant modification.

Recommandation:
- conserver comme historique dense; lire seulement si un fichier specialise manque une information.

## `TROUBLESHOOTING.md`

Role probable:
- memoire technique des erreurs et solutions reutilisables.

Informations migrees:
- references aux erreurs recurrentes dans le workflow agent;
- timeout Gradle release;
- PHP service non upload;
- app_config et feature flags prod;
- scripts de maintenance PHP.

Informations non migrees:
- details de commandes de diagnostic longues, a garder dans le fichier original.

Informations ambiguues ou contradictoires:
- aucune contradiction detectee pendant cette migration.

Recommandation:
- lire avant de refaire une longue recherche sur une erreur deja rencontree.

## `TRANSLATION_PROGRESS.md`

Role probable:
- suivi incremental i18n English/French.

Informations migrees:
- politique langue: English par defaut, Francais comme alternative visible;
- zones encore a externaliser.

Informations non migrees:
- historique complet des blocs 1 a 7.

Informations ambiguues ou contradictoires:
- aucune contradiction detectee.

Recommandation:
- lire seulement pour une demande de traduction/i18n.

## `job_progress.md`

Role probable:
- tracker temporaire de lots de travail SmartVision.

Informations migrees:
- rappel que le tracker est utile pour gros lots;
- etat de certaines zones sensibles: focus, consentement, YouTube, parental, admin, release.

Informations non migrees:
- checklist complete de validation manuelle ancienne.

Informations ambiguues ou contradictoires:
- le fichier contient des releases intermediaires; verifier toujours le Gradle et la prod avant action release.

Recommandation:
- lire seulement si l'utilisateur demande de continuer un lot ou d'utiliser le tracker.

## Regle d'usage futur

Les agents IA doivent:
- commencer par `docs/ai-knowledge/ROOT.md`;
- lire les fichiers specialises;
- utiliser les legacy uniquement en secours;
- mettre a jour le nouveau systeme apres intervention;
- ne jamais supprimer ces fichiers legacy sans demande explicite.
