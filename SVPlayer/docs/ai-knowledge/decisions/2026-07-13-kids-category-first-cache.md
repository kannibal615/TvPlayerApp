# Decision - Filtrage Kids category-first partage

Date: 2026-07-13

## Decision

Le statut Kids est calcule au niveau de la source et non du profil. Chaque categorie est evaluee une fois; une categorie sure transmet son statut a ses items sans score individuel complet. Les categories incertaines utilisent ensuite le score local de chaque item.

Les decisions sont persistees dans Room v16 avec une cle `sourceKey + contentType + categoryId/contentId`, une empreinte des metadonnees et `KidsContentFilter.RuleVersion`. `sourceKey` est une empreinte sanitisee et ne contient ni mot de passe ni URL credentialisee.

## Raisons

- eviter les parcours complets repetes pour chaque profil Kids;
- garantir l'heritage de grandes categories explicites comme `KIDS France 150` et `USAKIDS 95`;
- ne recalculer que les noms/metadonnees ou versions de regles modifies;
- garder les exclusions adultes prioritaires sans TMDB ni requete par item;
- rendre les decisions et mesures auditables.

## Consequences

- les profils standards gardent leur import existant;
- les profils Kids partagent le cache de classification mais conservent leurs catalogues, favoris et historiques isoles par `profileId`;
- toute modification des regles doit incrementer `RuleVersion`;
- les logs doivent rester agreges et sanitises.
