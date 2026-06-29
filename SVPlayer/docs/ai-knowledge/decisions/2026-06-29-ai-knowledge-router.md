# Decision - AI Knowledge Router

## Decision

Creer `docs/ai-knowledge/ROOT.md` comme routeur principal pour les agents IA. Les anciens gros MD restent conserves mais ne sont plus lus par defaut.

## Contexte

Le projet contient plusieurs fichiers MD denses. Les agents lisent trop souvent des informations non liees a la demande, ce qui augmente le temps, le risque de confusion et les tokens consommes.

## Raisons

- Orienter rapidement vers le bon domaine.
- Eviter de relire toute la documentation legacy.
- Garder une structure maintenable.
- Distinguer sources actives et sources historiques.

## Consequences

- Toute future intervention commence par `docs/ai-knowledge/ROOT.md`.
- Les fichiers specialises deviennent les sources de reference courantes.
- Les legacy restent disponibles en secours.
- Les fichiers specialises doivent etre mis a jour apres changement.

## A ne pas refaire

- Ajouter un nouveau gros fichier unique qui regroupe tout.
- Lire `PROJECT_NOTES.md` integralement pour une correction locale simple.
- Creer des doublons `final`, `new`, `v2`, `copy`.
