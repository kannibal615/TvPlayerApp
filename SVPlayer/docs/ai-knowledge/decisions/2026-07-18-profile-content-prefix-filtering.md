# Decision - Filtrage de contenu par prefixe de profil

Date: 2026-07-18

## Decision

Le filtre pays/langue est une propriete du profil local (`selectedContentPrefixes`) et agit pendant l'import, uniquement sur le debut du nom ou titre du contenu. Les categories ne participent jamais a la detection.

## Regles stables

- syntaxes reconnues: `FR -`, `AR:`, `EN |` et `[UK]`, avec code majuscule de 2-3 lettres; le format entre crochets accepte aussi le code special d'une lettre `[X]` ou `[x]`, normalise en `X`;
- l'editeur affiche seulement le code reel, place les selections actives en tete et permet d'ajouter manuellement un code de 1-3 lettres; un code manuel rejoint la meme selection locale et n'introduit aucune autre regle de filtrage;
- aucune selection conserve tout le catalogue, pour compatibilite avec les profils existants;
- plusieurs codes utilisent un OU; un titre sans prefixe est exclu si le filtre est actif;
- une serie est acceptee par son titre puis garde ses episodes;
- le filtre Kids est applique apres le filtre de prefixe;
- les categories d'origine sont conservees seulement si elles contiennent encore un contenu;
- l'empreinte de catalogue utilise la source resolue, le type, les filtres et la version de politique. Les identifiants restent seulement dans l'entree SHA-256 et ne sont pas affiches.

## Consequence

Une modification de filtre ou d'identifiants Admin invalide le catalogue attendu. Le profil actif resynchronise immediatement; un profil inactif, notamment Kids partage avec Admin, resynchronise lors de sa prochaine activation.
