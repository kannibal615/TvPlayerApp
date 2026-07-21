# Mesures de performance et stabilite

- Appareil: Amazon Fire TV AFTSSS, Android 9 / API 28, 1920x1080.
- Application: `com.smartvision.svplayer` 0.1.120 (215).
- Demarrage force (`am start -S -W`) sur trois executions:
  - TotalTime: 1984 ms, 1977 ms, 1856 ms (mediane 1977 ms).
  - WaitTime: 2927 ms, 2695 ms, 2665 ms (mediane 2695 ms).
  - Le selecteur de profil etait visible au controle effectue 8 secondes apres chaque lancement.
- Memoire apres exploration des parcours: 273146 KB PSS, dont 65190 KB graphiques; SwapPss 76053 KB.
- Stockage `/data`: 4.9 GB, 3.8 GB utilises, 987 MB libres (81 % utilise).
- Mesure `gfxinfo` remise a zero pendant une navigation D-pad sur Live avec apercu actif:
  - 1359 frames rendues; 1359 janky (100 %).
  - p50 57 ms; p90 73 ms; p95 81 ms; p99 97 ms.
  - 772 vsync manques; 975 frames avec UI thread lent; 975 deadlines manquees.
- Instantane CPU pendant l'apercu Live: 148 % pour le processus (mesure ponctuelle, a confirmer par trace longue).
- Aucun crash dans le buffer `logcat -b crash`; aucun ANR releve pendant la session.
- `simpleperf` indisponible sur cet appareil; `perfetto` disponible mais aucune trace systeme intrusive n'a ete lancee.

Les statistiques de frames incluent le rendu continu de l'apercu video Live. Elles prouvent une degradation visible sur ce parcours, mais une trace Perfetto ciblee reste necessaire pour attribuer precisement le cout entre Compose, Media3, animations et composition Fire OS.
