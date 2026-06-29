# Decision - Native TV App and Player

## Decision

Garder les ecrans applicatifs Android TV en Compose natif et la lecture video en AndroidX Media3 ExoPlayer.

## Contexte

SmartVision est une application Android TV pilotee au D-pad. La qualite du focus, la stabilite TV et la lecture native sont centrales.

## Raisons

- Les ecrans natifs gerent mieux le focus TV que des pages WebView.
- Media3 ExoPlayer est le chemin de lecture actuel pour Live, films et episodes.
- Les URL Xtream sont construites localement et passent par la logique native.
- Les controles TV doivent rester coherents avec le reste de l'app.

## Consequences

- Ne pas utiliser WebView pour les ecrans natifs ou la lecture video principale.
- Les corrections player doivent passer par `FullScreenPlayerScreen.kt`, les repositories et Media3.
- Les overlays ne doivent pas voler le focus sans demande explicite.

## A ne pas refaire

- Remplacer le player par une WebView.
- Ajouter des controles Compose superposes qui bloquent les controles attendus sans specification.
- Modifier une ancienne architecture sans verifier la route active.
