# Decision: profils ADMIN, NORMAL et KIDS

Date: 2026-07-12

## Decision

Etendre `PlaylistProfile` et `XtreamAccountManager` au lieu de creer une architecture parallele. Le premier profil conserve son ID et devient l'unique ADMIN. Les profils partages resolvent les credentials ADMIN a la lecture; les secrets custom sont separes dans Android Keystore.

Le filtrage Kids est fail-closed et execute pendant la synchronisation avant Room. `AppNavigation` derive header et routes de `ProfilePermissions`; masquer un bouton seul n'est pas considere comme une protection.

## Consequences

- Les donnees Room deja profilees restent rattachees au meme ID pendant la migration.
- Une mise a jour des credentials ADMIN est visible immediatement par les profils `SHARED_WITH_ADMIN`.
- Un changement du dictionnaire Kids requiert une nouvelle synchronisation du profil Kids.
- Les donnees non identifiees ne sont pas utilisees pour remplir une liste Kids vide.
