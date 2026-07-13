# Design QA - Refonte du demarrage

Final result: passed

## Evidence

- Source picker: `C:/Users/ONEDEV/Desktop/IPTV APP NATIVE ANDROID/assets/refonte design ecrans/demarrage/3-3.png`
- Render final Firestick: `screenshots/codex/startup-redesign/profile-picker-final-1920x1080.png`
- Focus crayon: `screenshots/codex/startup-redesign/profile-picker-edit-final-1920x1080.png`
- Appareil: Firestick AFTSSS, viewport physique `1920x1080`, densite `320`.
- Verification native Android par ADB; le navigateur web n'est pas applicable a cette interface Jetpack Compose TV.

## Comparaison

1. Le fond navy, le halo bleu central et les traces du symbole conservent la direction visuelle de la maquette.
2. Le logo reste dans la zone sure superieure gauche du picker.
3. Titre et sous-titre restent centres avec la meme hierarchie visuelle.
4. Les quatre cartes visibles sont centrees comme un groupe; une `LazyRow` prend le relais quand la liste depasse la largeur disponible.
5. Le focus conserve bordure, halo et scale sans modifier la disposition des cartes voisines.
6. Tous les avatars utilisent le meme diametre; les visuels de profils dependent du type et de `avatarId`, jamais du nom affiche.
7. Le crayon est hors de la carte et le routage D-pad Bas/Haut a ete verifie sur l'appareil.

## Ecarts intentionnels

- Les cartes finales sont plus compactes que la maquette a la demande de l'utilisateur.
- Les noms `Walid` et `Nouran` proviennent des donnees reelles; `Admin` et `PlaylistWeb` ne sont pas fabriques par l'UI.
- Les avatars de maquette ne sont pas utilises comme assets de production.
- Les crayons ont ete deplaces sous les cartes a la demande de l'utilisateur.

## Copy diff

- La capture utilise l'anglais configure sur l'appareil; les textes francais equivalents sont fournis par `SmartVisionStrings`.
- Les noms de profils sont volontairement differents de la maquette car ils proviennent de `PlaylistProfile.name`.
