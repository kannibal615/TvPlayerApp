# HOME Tendances Premium Roadmap

Derniere mise a jour: 2026-07-07.

## Objectif

Encadrer l'amelioration progressive des sections `Trending movies` et `Trending series` de HOME sans refonte globale de Home ni modification de `Continue watching`.

Le rendu cible est une card TV premium: portrait par defaut, focus reactif, stabilisation avant transformation, passage paysage 16:9, backdrop TMDB si disponible, fallback propre si absent, texte discret et mini-preview trailer YouTube TMDB controle.

## Etat actuel implemente

- Les lignes tendances restent chargees rapidement depuis Room via `HomeContentRepository`.
- Les details Xtream ne servent pas au premier rendu: ils restent fallback pour les champs que TMDB ne fournit pas.
- Le cache persistant `home_trending_preview_cache` stocke seulement des metadonnees: poster, backdrop, duree, cle trailer YouTube TMDB, position de depart et etats de disponibilite.
- Les URLs de lecture ne sont pas stockees en base et les tendances ne jouent plus les flux Xtream d'origine en preview.
- La migration Room `11 -> 12` neutralise les anciens caches preview `movie`/`episode`; le repository les ignore aussi a la lecture.
- `TrendingContentRow` remplace `ContinueWatchingRow` uniquement pour `Trending movies` et `Trending series`.

## State machine card

1. Normal: card portrait, poster local, aucun player.
2. Focus court: scale/glow discret, preparation item et voisins proches.
3. Focus stabilise: apres `1,3s`, la card est ancree a gauche avec `LazyListState.animateScrollToItem`.
4. Transformation: largeur animee vers 16:9, crossfade poster vers backdrop.
5. Paysage pret: titre a gauche, duree ou sample a droite.
6. Preview actif: mini-player YouTube en mode preview sur le trailer TMDB si disponible.
7. Perte focus: annulation Compose des delais/jobs UI, liberation du player, retour portrait.

## Blocs techniques

- Donnees: `HomeTrendingPreviewCacheEntity`, migrations Room `8 -> 9` puis `11 -> 12`, methodes DAO dediees.
- Repository: `HomeContentRepository.prepareTrendingPreview(...)` utilise TMDB via `TmdbRepository` pour poster/backdrop/duree/trailer et garde Xtream comme fallback de details.
- ViewModel: `HomeViewModel.prepareTrendingPreview` deduplique les jobs et borne la concurrence a `2`.
- UI: `TrendingContentRow.kt` gere prefetch visible + `3` ahead, focus stable, fallback backdrop et mini-player YouTube.

## Criteres d'acceptation

- D-pad rapide: aucune transformation ni preview avant `1,3s`.
- D-pad stable: card focussee a gauche, transformation fluide, pas de saut vertical.
- Film avec backdrop: paysage propre, titre + duree, trailer TMDB si disponible.
- Film sans backdrop: poster non etire, fond floute, pas de flash blanc.
- Serie: poster/backdrop TMDB si disponible, trailer TMDB si disponible, label duree si connu.
- Focus perdu: preview arretee et player libere.
- Xtream indisponible: overlays/blocages Home existants conserves.

## Points a surveiller

- Les backdrops/trailers dependent de la qualite du matching et des donnees TMDB; certains contenus resteront poster-only.
- Si TMDB ne fournit pas de trailer YouTube exploitable, la card reste poster/backdrop-only.
- Ne pas remettre de prechargement details tendances dans `MainActivity` ou le splash.
