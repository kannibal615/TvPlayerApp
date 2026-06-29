# Knowledge System Continuous Improvement

Derniere mise a jour: 2026-06-30.

## 1. Objectif

Mettre en place une boucle simple pour que le Knowledge System s'ameliore au fil des interventions, sans devenir un nouveau fichier legacy trop long.

## 2. Principe

Chaque intervention Codex doit produire au moins une verification documentaire rapide:
- le domaine lu correspond-il bien a la demande ?
- une information du MD est-elle contredite par le code ?
- une information utile trouvee dans le code manque-t-elle au MD ?
- une decision risque-t-elle d'etre oubliee par un futur agent ?

Si oui, corriger le MD specialise immediatement ou ajouter une note `A_VERIFIER` si la preuve manque.

## 3. Boucle automatique par intervention

Avant de modifier du code ou une doc:
1. Lire `ROOT.md`.
2. Lire uniquement les MD specialises utiles.
3. Verifier les affirmations importantes dans le code ou les scripts reels.
4. Noter les ecarts doc/code.

Pendant l'intervention:
1. Si un fait documentaire est faux, corriger le MD concerne.
2. Si un nouveau workflow est identifie, l'ajouter dans le domaine specialise.
3. Si une nouvelle regle durable apparait, creer ou completer une decision.

Apres l'intervention:
1. Mettre a jour `worklog/AI_CHANGELOG.md`.
2. Ajouter une note dans `TROUBLESHOOTING.md` seulement pour les erreurs reutilisables.
3. Ne pas recopier de logs longs; garder la cause, le contexte et la solution.

## 4. Regles anti-derive

- Le root reste court: il route, il ne remplace pas les domaines.
- Les domaines restent operationnels: objectif, fonctionnement, workflows, fichiers, API, regles, problemes connus.
- Les specs futures doivent etre marquees comme `cible`, `proposition` ou `future`, jamais comme implementation actuelle.
- Les numeros de version, endpoints publics et etats production doivent etre verifies avant release ou deploy.
- Les anciens MD restent legacy et ne doivent pas redevenir la source par defaut.

## 5. Quand creer une decision

Creer un fichier dans `docs/ai-knowledge/decisions/` si:
- un choix technique empeche une future mauvaise direction;
- un comportement produit est volontaire et pourrait etre reinterprete;
- un compromis build/deploy/UX/backend est durable;
- une regle de securite ou confidentialite est importante.

## 6. Drift audit rapide

Faire un audit de derive documentaire quand:
- un domaine a ete modifie plusieurs fois;
- une nouvelle release change les routes, endpoints, tables ou version Gradle;
- une simulation Knowledge System detecte une confusion;
- un agent doit lire un fichier legacy pour comprendre une demande.

Checklist:
- `ROOT.md` pointe vers le bon domaine;
- le domaine liste les fichiers de code reels;
- les routes/endpoints existent encore;
- les specs futures sont separees de l'etat actuel;
- `AI_CHANGELOG.md` contient l'entree courte.

## 7. Mesure d'efficacite

Pour comparer ancien et nouveau systeme, estimer:
- nombre de MD lus avant de trouver le bon domaine;
- nombre de recherches `rg` necessaires;
- nombre de fichiers de code ouverts;
- presence ou absence d'informations obsoletes;
- temps/tokens avant premiere action utile.

Objectif moyen:
- lire `ROOT.md` + 1 a 3 domaines specialises;
- eviter `PROJECT_NOTES.md` sauf besoin historique;
- garder les mises a jour documentaires petites et verifiables.
