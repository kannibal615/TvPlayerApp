# Lots de correction pour Codex

Traiter un lot à la fois. Inspecter les preuves et le code avant toute modification, préserver les changements existants, puis exécuter les tests ciblés et le scénario de validation Android TV.

## Lot 1 — priorité-haute / fonctionnel

```text
Corrige les anomalies Android TV suivantes : SV-PROFILE-001.

Contraintes :
- Analyse d'abord les captures, logs, traces et zones de code indiquées dans le rapport d'audit.
- Corrige les causes racines sans refonte hors périmètre.
- Préserve la navigation complète à la télécommande et la cohérence visuelle existante.
- Ajoute ou adapte les tests ciblés lorsque cela est pertinent.
- Rejoue les critères d'acceptation sur l'appareil Android TV et rapporte les résultats.

Anomalies :
- SV-PROFILE-001 — Info profil / Home / Who's Watching — L'ancien Home reste visible apres activation Kids et bloque le changement suivant
  Actuel : Le theme et l'avatar passent en Kids, mais les compteurs MARWEN 5393 / 41258 / 16342 et les reprises MARWEN restent visibles. Le changement suivant vers MARWEN peut rester indefiniment sur le loader de l'avatar.
  Attendu : Aucune donnee de l'ancien profil ne doit etre rendue. Home doit montrer un etat vide/loading scope Kids, charger 254 / 732 / 1076 et ses tendances, puis toute selection suivante doit terminer sa transition.
  Correction proposée : Unifier le dialogue Info profil avec le meme coordinateur de changement que Who's Watching. Introduire une requete profileId + requestId pour tout point d'entree, remettre Home en etat cible avant navigation, et interdire le rendu de listes/compteurs si HomeUiState.profileId differe du profil actif. Ajouter un test d'integration du ViewModel conserve dans la back stack.
  Preuves : checkpoints/20260717-202615-138999-marwen-home-run1/screenshot.png; checkpoints/20260717-202824-158135-kids-home-after-info-change-run1/screenshot.png; checkpoints/20260717-203052-760140-kids-home-cold-process-run1/screenshot.png; checkpoints/20260717-203651-712423-kids-home-after-info-change-run2-plus30s/screenshot.png; checkpoints/20260717-204124-045372-picker-marwen-stuck-plus30s/screenshot.png; checkpoints/20260717-202824-158135-kids-home-after-info-change-run1/logcat.txt
  Validation : MARWEN vers Kids affiche immediatement zero/skeleton cible et jamais une ligne MARWEN; Kids charge 254 / 732 / 1076 ou un etat d'erreur terminal sans skeleton infini; Le changement suivant Kids vers MARWEN termine l'animation; Les deux parcours Info profil et Who's Watching utilisent le meme contrat; Le scenario complet reussit trois fois sans redemarrage du processus
```
