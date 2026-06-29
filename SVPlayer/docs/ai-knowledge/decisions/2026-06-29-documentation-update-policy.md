# Decision - Documentation Update Policy

## Decision

Apres chaque intervention future, Codex doit verifier si la documentation specialisee doit etre mise a jour et ajouter une entree dans `docs/ai-knowledge/worklog/AI_CHANGELOG.md`.

## Contexte

Le projet change souvent cote Android, backend, admin, release et production. Une documentation non mise a jour devient vite dangereuse pour les agents suivants.

## Raisons

- Eviter les analyses basees sur une architecture obsolete.
- Garder les dependances entre domaines visibles.
- Documenter les decisions structurantes au moment ou elles sont prises.
- Garder `TROUBLESHOOTING.md` utile pour les erreurs recurrentes.

## Consequences

- Tout changement de fonctionnalite, ecran, endpoint, table ou workflow doit mettre a jour le fichier MD concerne.
- Les grosses decisions doivent aller dans `docs/ai-knowledge/decisions/`.
- Les erreurs reutilisables doivent aller dans `TROUBLESHOOTING.md`.

## A ne pas refaire

- Corriger le code et laisser les docs obsoletes.
- Ajouter seulement un long log brut.
- Documenter des secrets ou valeurs sensibles.
