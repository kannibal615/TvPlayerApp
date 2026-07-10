# Decision - Roles visuels de focus TV

Date: 2026-07-10

## Contexte

Les ecrans utilisaient souvent les memes couleurs en dur pour un element focalise, selectionne, actif ou parent. Ces etats ont pourtant des significations differentes au D-pad.

## Decision

`TvFocusStyle` est la source partagee pour quatre roles:

- `Focused`: cible courante de la telecommande;
- `Selected`: contenu ou choix memorise;
- `Active`: source, profil ou action actuellement active;
- `Parent`: categorie contenant l'element actif.

`PlayerSettings` persiste les couleurs Selected, Active et Parent. `Focused` continue d'utiliser style, couleur, effet et fond existants. Les composants reutilisables doivent consommer ces tokens avant d'introduire une couleur locale.

## Consequences

- les couleurs metier Danger/Success restent prioritaires;
- un ecran peut garder une geometrie specifique, mais pas redefinir arbitrairement les couleurs de role;
- toute nouvelle surface TV doit distinguer le focus courant de la selection persistante.
