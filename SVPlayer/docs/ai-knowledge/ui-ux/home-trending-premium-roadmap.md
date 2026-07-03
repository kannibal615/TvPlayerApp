# HOME Tendances Premium Roadmap

Derniere mise a jour: 2026-07-03.

## Objectif

Encadrer l'amelioration progressive des sections `Trending movies` et `Trending series` de HOME sans refonte globale de Home ni modification de `Continue watching`.

Le rendu cible est une card TV premium: portrait par defaut, focus reactif, stabilisation avant transformation, passage paysage 16:9, backdrop si disponible, fallback propre si absent, texte discret et mini-preview muet controle.

## Etat actuel implemente

- Les lignes tendances restent chargees rapidement depuis Room via `HomeContentRepository`.
- Les details Xtream ne servent pas au premier rendu: ils sont prepares uniquement pour les cards visibles/proches ou focussees.
- Le cache persistant `home_trending_preview_cache` stocke seulement des metadonnees: poster, backdrop, duree, type de preview, id sample, extension, position de depart et etats de disponibilite.
- Les URLs de lecture ne sont pas stockees en base; elles sont reconstruites en memoire avec `XtreamUrlFactory`.
- `TrendingContentRow` remplace `ContinueWatchingRow` uniquement pour `Trending movies` et `Trending series`.

## State machine card

1. Normal: card portrait, poster local, aucun player.
2. Focus court: scale/glow discret, preparation item et voisins proches.
3. Focus stabilise: apres `1,3s`, la card est ancree a gauche avec `LazyListState.animateScrollToItem`.
4. Transformation: largeur animee vers 16:9, crossfade poster vers backdrop.
5. Paysage pret: titre a gauche, duree ou sample a droite.
6. Preview actif: player Media3 muet, depart a 15% de la duree si disponible, fallback 30% si le premier demarrage ne rend pas de frame.
7. Perte focus: annulation Compose des delais/jobs UI, liberation du player, retour portrait.

## Blocs techniques

- Donnees: `HomeTrendingPreviewCacheEntity`, migration Room `8 -> 9`, methodes DAO dediees.
- Repository: `HomeContentRepository.prepareTrendingPreview(...)` utilise `XtreamRepository.getMovieDetails`, `getSeriesDetails` et `getSeriesEpisodes`.
- ViewModel: `HomeViewModel.prepareTrendingPreview` deduplique les jobs et borne la concurrence a `2`.
- UI: `TrendingContentRow.kt` gere prefetch visible + `3` ahead, focus stable, fallback backdrop et mini-player.

## Criteres d'acceptation

- D-pad rapide: aucune transformation ni preview avant `1,3s`.
- D-pad stable: card focussee a gauche, transformation fluide, pas de saut vertical.
- Film avec backdrop: paysage propre, titre + duree, preview a 15%.
- Film sans backdrop: poster non etire, fond floute, pas de flash blanc.
- Serie: premier episode disponible comme sample preview, label duree ou `Sx Ex`.
- Focus perdu: preview arretee et player libere.
- Xtream indisponible: overlays/blocages Home existants conserves.

## Points a surveiller

- Les backdrops dependent de `get_vod_info` / `get_series_info`; certains fournisseurs Xtream n'en fournissent pas.
- Si Media3 echoue sur un conteneur preview, l'URL est ignoree en memoire pour la session et la card reste poster/backdrop-only.
- Ne pas remettre de prechargement details tendances dans `MainActivity` ou le splash.
