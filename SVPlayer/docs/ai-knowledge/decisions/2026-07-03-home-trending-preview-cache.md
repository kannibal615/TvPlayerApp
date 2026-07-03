# Decision: Cache metadata-only pour previews HOME Tendances

Date: 2026-07-03.

## Contexte

Les cards `Trending movies` et `Trending series` de HOME doivent afficher un rendu premium avec backdrop, duree et mini-preview, sans bloquer le premier rendu Home et sans relancer un traitement massif du catalogue.

## Decision

Ajouter un cache Room dedie `home_trending_preview_cache` pour stocker uniquement les metadonnees de preview:
- type/id contenu;
- poster/backdrop;
- duree label/ms;
- type/id/extension du sample preview;
- position de depart 15%;
- etats backdrop/preview;
- `preparedAt` et `lastSync`.

Les URLs de lecture brutes ne sont pas stockees en base. Elles sont reconstruites en memoire via `XtreamUrlFactory` au moment d'afficher la card.

## Raisons

- Eviter d'exposer ou persister des URLs contenant les credentials Xtream.
- Conserver un premier rendu HOME rapide depuis Room locale.
- Permettre un enrichissement progressif limite aux cards visibles/proches ou focussees.
- Eviter de remettre du prechargement details dans `MainActivity` ou le splash.

## Consequences

- Le cache doit etre invalide par `lastSync`.
- Une card peut rester poster-only si le fournisseur ne fournit pas de backdrop ou si le preview Media3 echoue.
- Les futures optimisations doivent respecter la regle metadata-only.
