# Audit cible - changement de profil SmartVision

Le changement depuis Info profil contourne le contrat requestId + Home prete. Home peut alors rendre un HomeUiState appartenant a l'ancien profil, puis Who's Watching reste bloque en attendant une Home prete qui ne sera jamais publiee.

## Contexte du test

| Champ | Valeur |
|---|---|
| Date | 2026-07-17 |
| Device | TCL Smart TV |
| Serial | local-adb-tv |
| Android Version | 11 |
| Package | com.smartvision.svplayer |
| App Version | 0.1.120 (189) |
| Build Variant | release |
| Commit | working-tree |
| Run Count | 2 |

## Synthèse

| Gravité | Nombre |
|---|---:|
| Bloquant | 0 |
| Critique | 1 |
| Majeur | 0 |
| Mineur | 0 |
| Amélioration | 0 |

## Couverture

| Écran/parcours | État | Notes |
|---|---|---|
| Who's Watching | covered | Selection Kids et MARWEN, attente Home et blocage du loader observes. |
| Info profil - Changer de profil | covered | MARWEN vers Kids reproduit deux fois avec donnees Home de l'ancien profil. |
| Home | covered | Etats MARWEN, Kids stale et Kids apres redemarrage a froid compares. |
| Room / persistance | partially covered | Build release non debuggable; la comparaison apres mort du processus prouve toutefois que Room Kids contient des donnees correctes. |

## Backlog priorisé

| ID | Gravité | Catégorie | Écran | Problème |
|---|---|---|---|---|
| [SV-PROFILE-001](#sv-profile-001-lancien-home-reste-visible-apres-activation-kids-et-bloque-le-changement-suivant) | critique | fonctionnel | Info profil / Home / Who's Watching | L'ancien Home reste visible apres activation Kids et bloque le changement suivant |

## Détails

### SV-PROFILE-001 — L'ancien Home reste visible apres activation Kids et bloque le changement suivant

- **Gravité :** critique
- **Catégorie :** fonctionnel
- **Écran :** Info profil / Home / Who's Watching
- **Reproductibilité :** 2/2 pour les donnees stale; 1/1 pour le loader suivant bloque plus de 30 secondes
- **Confiance :** high

**Étapes de reproduction**

1. Ouvrir MARWEN puis Home.
2. Ouvrir l'avatar du header puis Info profil.
3. Choisir Changer de profil puis Kids.
4. Observer Home pendant au moins 30 secondes.
5. Depuis Kids, ouvrir Who's Watching et choisir MARWEN.

**Résultat actuel :** Le theme et l'avatar passent en Kids, mais les compteurs MARWEN 5393 / 41258 / 16342 et les reprises MARWEN restent visibles. Le changement suivant vers MARWEN peut rester indefiniment sur le loader de l'avatar.

**Résultat attendu :** Aucune donnee de l'ancien profil ne doit etre rendue. Home doit montrer un etat vide/loading scope Kids, charger 254 / 732 / 1076 et ses tendances, puis toute selection suivante doit terminer sa transition.

**Impact :** Fuite visuelle entre profils, contournement des attentes Kids et blocage d'un parcours principal jusqu'au redemarrage du processus.

**Preuves**

- `checkpoints/20260717-202615-138999-marwen-home-run1/screenshot.png`
- `checkpoints/20260717-202824-158135-kids-home-after-info-change-run1/screenshot.png`
- `checkpoints/20260717-203052-760140-kids-home-cold-process-run1/screenshot.png`
- `checkpoints/20260717-203651-712423-kids-home-after-info-change-run2-plus30s/screenshot.png`
- `checkpoints/20260717-204124-045372-picker-marwen-stuck-plus30s/screenshot.png`
- `checkpoints/20260717-202824-158135-kids-home-after-info-change-run1/logcat.txt`

**Zone de code suspectée**

- AppNavigation.kt: activateProfileForSession contourne ProfileSelectionRequest
- AppNavigation.kt: garde de route Kids revient immediatement sur Home
- HomeScreen.kt: homeContentReady controle le callback mais ne masque pas un state.profileId obsolete
- HomeViewModel.kt: HomeUiState conserve dans le ViewModel de la back stack

**Correction proposée :** Unifier le dialogue Info profil avec le meme coordinateur de changement que Who's Watching. Introduire une requete profileId + requestId pour tout point d'entree, remettre Home en etat cible avant navigation, et interdire le rendu de listes/compteurs si HomeUiState.profileId differe du profil actif. Ajouter un test d'integration du ViewModel conserve dans la back stack.

**Critères d’acceptation**

- MARWEN vers Kids affiche immediatement zero/skeleton cible et jamais une ligne MARWEN
- Kids charge 254 / 732 / 1076 ou un etat d'erreur terminal sans skeleton infini
- Le changement suivant Kids vers MARWEN termine l'animation
- Les deux parcours Info profil et Who's Watching utilisent le meme contrat
- Le scenario complet reussit trois fois sans redemarrage du processus

**Notes :** Le redemarrage du seul processus restaure les bonnes donnees Kids, ce qui exclut une contamination persistante Room comme cause principale. Gfxinfo de cette TV rapporte des valeurs GPU non fiables; elles ne sont pas utilisees pour attribuer le bug.
