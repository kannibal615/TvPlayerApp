# AGENTS.md — SmartVision Android TV

## Objectif global

SmartVision est une application Android TV native en Kotlin + Jetpack Compose for TV.
Priorité actuelle : fidélité visuelle, fluidité TV, navigation D-pad et rendu premium.
Ne pas utiliser WebView, HTML ou CSS pour les écrans natifs.

## Règle absolue UI

Pour chaque écran, respecter d’abord :

1. la hiérarchie visuelle ;
2. les proportions ;
3. la navigation D-pad ;
4. le focus TV ;
5. la capture de validation.

Ne jamais compresser toute la page pour faire rentrer trop de sections dans le premier viewport.

## Écran Home — règles obligatoires

Le menu principal est horizontal en haut.
Ne jamais créer de menu latéral gauche pour l’écran Home.

Structure obligatoire du premier viewport :

1. Header horizontal
2. Hero compact
3. Trois cartes principales : Live TV, Films, Séries
4. Titre “Reprendre la lecture”
5. Row “Reprendre la lecture”

La section “Tendances” doit être sous le fold.
Elle ne doit pas être visible comme section active au premier affichage.

## Dimensions Home

Créer ou utiliser un fichier centralisé :

* HomeDimens.kt

Toutes les tailles Home doivent être regroupées dedans :

* screen padding ;
* header height ;
* hero height ;
* main cards height ;
* continue row height ;
* section gaps ;
* card radius ;
* focus scale ;
* border width.

Ne pas disperser les dimensions dans plusieurs composants.

## Focus Android TV

Tous les éléments actionnables doivent être focusable.
Ne jamais rendre un élément clickable sans focusable.

Focus initial obligatoire sur la carte Live TV.

État focus :

* scale : 1.04 à 1.08 ;
* border : 2dp blanc/bleu ;
* glow léger ;
* zIndex supérieur ;
* animation : 120 à 180ms.

## Navigation D-pad

* Haut depuis cartes principales : header.
* Bas depuis cartes principales : Reprendre la lecture.
* Gauche/Droite dans les cartes : Live TV → Films → Séries.
* Gauche/Droite dans header : Accueil → Live TV → Films → Séries → Synchroniser → Paramètres.
* OK Live TV : route live_tv.
* OK Films : route movies.
* OK Séries : route series.
* OK Paramètres : route settings.

## Assets

Ne pas bloquer si les images finales manquent.
Créer des placeholders propres, locaux, premium et neutres.
Ne pas utiliser d’images distantes codées en dur.
Ne pas intégrer d’affiches ou images protégées comme assets finaux.

## Méthode de travail obligatoire

Avant de modifier :

1. inspecter la capture objectif ;
2. lister les écarts visuels ;
3. proposer un plan court ;
4. modifier uniquement les fichiers nécessaires ;
5. compiler ;
6. générer une capture réelle ;
7. comparer la capture au modèle ;
8. corriger les écarts restants.

## Validation obligatoire

Une tâche UI n’est terminée que si une capture réelle est produite ou si l’environnement empêche la capture.

Chemin attendu :
screenshots/codex/home_screen.png

Commandes recommandées :
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.smartvision/.MainActivity
adb exec-out screencap -p > screenshots/codex/home_screen.png

## Critère de validation Home

Valider uniquement si :

* header horizontal en haut ;
* hero compact ;
* 3 cartes principales grandes et dominantes ;
* Live TV focus initial ;
* Reprendre la lecture visible ;
* Tendances sous le fold ;
* aucun WebView ;
* aucune API branchée pendant la phase UI mock.

3. Outils / librairies à demander à Codex
À utiliser
Jetpack Compose for TV : base UI Android TV moderne.
androidx.tv.material3 : composants adaptés TV, plus cohérents pour focus et navigation TV.
FocusRequester + onFocusChanged + focusable : obligatoire pour contrôler le focus initial et les états visuels.
LazyRow / LazyColumn maîtrisés : uniquement si le scroll est contrôlé.
Compose Preview Screenshot Testing ou capture ADB systématique : pour comparer le rendu réel au modèle.
À éviter
UI mobile Compose classique sans logique TV.
Material3 mobile uniquement pour les éléments focusables TV.
Dimensions codées directement dans chaque composant.
Trop de sections visibles dans le premier viewport.
Création d’une UI “inspirée” au lieu d’une UI “reproduite”.
4. Technique qui améliorera fortement le rendement

La meilleure solution : imposer à Codex une boucle courte :

1 correction = 1 capture = 1 comparaison = 1 ajustement.

Exemple :

Ajuster uniquement la hauteur du hero.
Générer capture.
Comparer avec le modèle.
Ajuster uniquement les cartes.
Générer capture.
Ajuster uniquement la row Reprendre.