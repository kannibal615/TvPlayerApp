# Audit Android TV — Lot 1B Performance Home

- Date : 2026-07-15 (Europe/Paris)
- Appareil : Amazon Fire TV `192.168.1.33:5555`, Fire OS 7 / Android 9
- Application : `com.smartvision.svplayer` 0.1.120 (168)
- Affichage mesuré : 1920 × 1080 à 59,94 Hz, budget d’une frame ≈ 16,68 ms
- Modifications de l’application et du skill : aucune
- État final : Home stable, onglet Home du header au focus

## Verdict

`LOT1-PERF-001` est **confirmé** sur le Firestick testé. Contrairement au compteur cumulatif du Lot 1, les trois mesures du Lot 1B ont chacune été précédées immédiatement d’un reset `gfxinfo`. Les trois répétitions montrent le même défaut de frame timing.

La fréquence des GC est mesurée, mais son impact causal sur le jank reste **non établi** : les GC sont concurrents, leurs pauses applicatives observées sont submilliseconde, et aucune longue désactivation du thread principal n’apparaît dans la trace système complémentaire.

## Préparation et parcours reproductible

Le Home a été vérifié stable avant la mesure : route Home, onglet Home du header au focus, aucune boîte de dialogue ni lecteur. Les composants déjà analysés pour le Lot 1 restent `ui/home/TvHeader.kt` et `ui/home/HomeScreen.kt`.

Le même parcours de 18 touches, envoyé une touche à la fois avec 600 ms entre les commandes, a été exécuté trois fois :

`RIGHT, LEFT, DOWN, RIGHT, LEFT, DOWN, RIGHT, LEFT, DOWN, RIGHT, LEFT, DOWN, RIGHT, LEFT, UP, UP, UP, UP`

Il couvre le header, les catégories, Continue Watching, les tendances films et les tendances séries, puis restaure le focus sur Home dans le header. Le détail annoté est dans `sequence.txt`.

## Mesures gfxinfo isolées

| Exécution | Frames | Janky | p50 | p90 | p95 | p99 | Slow UI thread | Slow draw | Deadline missed |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 868 | 868 (100 %) | 81 ms | 97 ms | 101 ms | 150 ms | 816 | 815 | 816 |
| 2 | 884 | 884 (100 %) | 81 ms | 97 ms | 109 ms | 150 ms | 835 | 834 | 835 |
| 3 | 888 | 888 (100 %) | 81 ms | 97 ms | 109 ms | 150 ms | 843 | 842 | 843 |

Mesure réelle : ces valeurs proviennent uniquement des fenêtres post-reset. Les compteurs HWC globaux affichés par `gfxinfo` ne sont pas utilisés dans le verdict.

## Trace temporelle de l’exécution 3

La capture Perfetto demandée n’a pas pu démarrer sur ce Fire OS : le binaire ancien n’accepte pas `--txt`, ne peut pas lire la configuration dans les chemins accessibles à `adb shell`, et échoue également à ouvrir un flux de sortie utilisable. Tous les essais ont échoué **avant** le reset et avant l’envoi des touches ; aucun parcours n’a été contaminé et aucun outil n’a été installé.

Une trace système `atrace` a été capturée en complément sur l’exécution 3. Elle ne doit pas être présentée comme une trace Perfetto et elle ne contient pas de sections Compose.

| Slice / attente | Nombre | p50 | p95 | Maximum | Dépassement du budget |
|---|---:|---:|---:|---:|---:|
| Main `Choreographer#doFrame` | 744 | 31,98 ms | 38,98 ms | 75,69 ms | 744 / 744 |
| RenderThread `DrawFrame` | 744 | 34,22 ms | 38,25 ms | 61,50 ms | 712 / 744 |
| Main hors CPU | 2 614 | — | 11,25 ms | 17,70 ms | aucun stall > 50 ms |
| RenderThread hors CPU | 961 | — | 23,22 ms | 27,43 ms | aucun stall > 50 ms |

Interprétation : le travail du thread principal et du RenderThread dépasse durablement le budget de 16,68 ms. La trace ne montre pas de long blocage scheduler du thread principal. Une activité Binder existe, mais aucun stall Binder ne peut être attribué avec cette trace ; le simple nombre de transactions n’est pas une preuve.

## GC et mémoire

| Exécution | GC concurrents dans la fenêtre | Durée totale moyenne du cycle |
|---|---:|---:|
| 1 | 20 | 287,9 ms |
| 2 | 21 | 275,2 ms |
| 3 | 22 | 297,7 ms |

La trace de l’exécution 3 contient 18 slices `Background concurrent copying GC` sur le thread GC, p50 275,14 ms et maximum 500,73 ms. Ces durées décrivent le travail concurrent total, pas une pause du thread principal. Les pauses relevées dans logcat restent inférieures à 1 ms. Hypothèse seulement : ces cycles peuvent contribuer à la pression CPU, mais les preuves ne permettent pas de les désigner comme cause du jank.

| Meminfo | Initial | Final | Delta |
|---|---:|---:|---:|
| Total PSS | 173 462 Ko | 181 577 Ko | +8 115 Ko |
| Swap PSS | 30 387 Ko | 43 917 Ko | +13 530 Ko |
| Java Heap | 25 896 Ko | 25 512 Ko | -384 Ko |
| Native Heap | 18 568 Ko | 13 912 Ko | -4 656 Ko |
| Graphics | 47 134 Ko | 47 134 Ko | 0 Ko |

Les vues (491), activités (1), contextes applicatifs (6) et compteurs Binder (18/38) sont inchangés. Le PSS total augmente de 8,1 Mo, mais sans croissance retenue du Java Heap, du Native Heap ou du Graphics. Avec un seul couple avant/après, aucune fuite mémoire n’est confirmée.

## Anomalie performance confirmée

### LOT1-PERF-001 — frame timing du Home durablement hors budget

- Gravité : majeure sur le Firestick testé
- Reproductibilité : 3/3
- Actuel : 100 % des frames `gfxinfo` classées janky après trois resets indépendants ; p50 81 ms. La trace système confirme des `doFrame` et `DrawFrame` durablement supérieurs au budget 60 Hz.
- Attendu : navigation du Home dans le budget de 16,68 ms, sans jank durable lors des déplacements DPAD.
- Impact : fluidité et réactivité perçue du focus dégradées.
- Correction recommandée, pour une phase ultérieure : profiler le travail exact du main thread et du RenderThread dans un build diagnostic compatible, puis réduire les invalidations, recompositions ou travaux synchrones par frame. Ne pas cibler les GC sans preuve supplémentaire.
- Preuves : `runs/run-1/gfxinfo-framestats.txt`, `runs/run-2/gfxinfo-framestats.txt`, `runs/run-3/gfxinfo-framestats.txt`, `runs/run-3/atrace.txt`, `analysis-summary.json`.

## Anomalies visuelles Home à conserver pour correction

### LOT1-UI-001 — le header masque le haut de l’image Hero

- Gravité : mineure
- Reproductibilité : état Home stable
- Actuel : la partie supérieure de l’illustration Hero est coupée au contact du header ; le cercle de pourcentage visible à droite est tronqué.
- Attendu : le contenu significatif du Hero reste intégralement lisible sous le header.
- Impact : composition visuelle tronquée et impression de recouvrement.
- Correction recommandée : revoir l’inset ou l’alignement vertical du Hero ainsi que le recadrage de l’image sous le header.
- Preuves : `sequence-start-home-header.png`, `home-stable.png`, `home-final.png`.

### LOT1-UI-002 — padding haut/bas du header asymétrique

- Gravité : mineure
- Reproductibilité : état Home stable
- Actuel : les éléments du header sont beaucoup plus proches du bord supérieur que de sa limite inférieure ; l’espace libre est plus grand en bas qu’en haut.
- Attendu : padding vertical visuellement équilibré et hauteur utile cohérente.
- Impact : header déséquilibré, hauteur perçue excessive et perte d’espace pour le Hero.
- Correction recommandée : normaliser le padding vertical et la hauteur du conteneur, puis réaligner logo, onglets et actions.
- Preuves : `sequence-start-home-header.png`, `home-stable.png`, `home-final.png`.

## Continuité avec le Lot 1

- `LOT1-FOCUS-001` reste une anomalie confirmée, reproductible 3/3. Elle n’a pas été retestée dans ce lot consacré uniquement à la performance Home.
- L’ancien signal cumulatif de `LOT1-PERF-001` a été explicitement reclassé comme piste d’investigation dans le rapport du Lot 1. Le présent verdict repose seulement sur les trois mesures post-reset du Lot 1B.

## Limites

1. Pas de trace Perfetto exploitable sur cette configuration Fire OS ; `atrace` est une preuve complémentaire moins riche.
2. Aucune section Compose n’est présente, donc aucune attribution à une fonction Compose précise.
3. Les captures fixes prouvent les états visuels et le focus final, mais ne mesurent pas à elles seules la perception continue du mouvement.
4. La causalité GC, Binder ou scheduler n’est pas établie ; le défaut confirmé porte sur le frame timing global.
5. Aucun lecteur, PIN, réglage, synchronisation, installation, désinstallation, effacement de données ou correction n’a été effectué.

## Conclusion

Le Home présente un problème de fluidité **reproductible et mesuré** sur ce Firestick : `LOT1-PERF-001` est confirmé. Les GC sont fréquents, mais leur rôle causal reste indéterminé. Aucune fuite mémoire n’est démontrée. Les deux défauts visuels signalés — Hero masqué en haut et padding vertical asymétrique du header — sont également consignés pour une correction ultérieure. Aucune correction n’a été appliquée.
