# Decision: pagination Room pour les catalogues TV

Date: 2026-07-01.

## Statut

Acceptee.

## Contexte

Les gros comptes Xtream peuvent contenir plusieurs dizaines de milliers de films et series. Charger un snapshot complet Room -> RAM pour ouvrir Live TV, Movies ou Series provoque des loaders longs et augmente le risque OOM sur Firestick.

## Decision

Room reste le stockage persistant complet du catalogue sur l'appareil. Les ecrans Live TV / Movies / Series ne doivent pas charger tout le catalogue en memoire pour s'ouvrir: ils affichent d'abord categories/counts puis consomment les contenus par pages locales `LIMIT/OFFSET`.

## Consequences

- Le backend ne stocke pas de mini catalogue utilisateur pour cette optimisation.
- `SplashActivity` reste limite aux verifications et categories/counts.
- Les caches RAM applicatifs ne doivent conserver que des petits jeux deja affiches ou utiles.
- Toute future recherche globale devra utiliser une requete Room dediee, pas filtrer un snapshot complet.
