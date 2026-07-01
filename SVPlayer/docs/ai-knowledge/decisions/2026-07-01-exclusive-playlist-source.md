# Decision 2026-07-01 - Source playlist exclusive

## Contexte

SmartVision peut recevoir des identifiants Xtream et un lien M3U pour la meme TV. L'application ne doit pas melanger deux catalogues en meme temps.

## Decision

La source catalogue active est une preference locale Android exclusive: `xtream` ou `m3u`. Activer M3U desactive Xtream comme source active, et inversement. Le backend peut conserver les deux configurations, mais il ne decide pas automatiquement laquelle est active quand les deux existent.

## Consequences

- Xtream reste la source complete Live TV / Films / Series.
- M3U alimente Live TV uniquement via URL directe.
- Movies et Series restent vides quand M3U est actif.
- Les refreshs `device_status.php` ne doivent pas ecraser la source active locale si Xtream et M3U sont tous les deux presents.
- L'EPG XMLTV est un enrichissement commun a la source active Live TV.
