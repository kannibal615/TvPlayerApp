# Decision - Portee du controle parental par profil

Date: 2026-07-15
Statut: accepte

## Contexte

Le controle parental etait un booleen global. Tous les consommateurs lisaient `PlayerSettings.parentalControlEnabled`, sans permettre a l'Admin de cibler certains profils.

## Decision

- Conserver l'activation globale existante et persister seulement les IDs des profils explicitement desactives.
- Exposer `ParentalControlScope` et calculer `PlayerSettings.parentalControlEnabled` pour le profil actif.
- Inclure Admin parmi les profils ciblables.
- Considerer les profils existants et nouveaux comme actives par defaut tant qu'ils ne sont pas exclus.
- Conserver les exclusions quand le global passe OFF; `resetParentalControl()` efface global, PIN, mots-cles et exclusions.
- Faire observer le scope brut au panneau Admin et au garde de feature, afin qu'un Admin exclu puisse encore administrer et reinitialiser la fonctionnalite.

## Consequences

- Aucun changement backend ni migration Room.
- Les ecrans existants qui filtrent via `PlayerSettings` deviennent automatiquement sensibles au profil actif.
- La suppression d'un profil peut laisser un ID sans effet dans le scope; un nouvel ID reste ON par defaut.
