# Decision - Standard visuel des dialogues SmartVision v1

Date: 2026-07-22

## Decision

Le dialogue `Identifiants Xtream`, valide a partir de la maquette utilisateur `Generated image 2.png`, devient la premiere reference visuelle pour la migration progressive des dialogues Android TV SmartVision.

## Contrat visuel

- utiliser le fond officiel du splash `startup_cinema_background` pour les dialogues immersifs d activation, de licence et de configuration;
- garder une surface centree, compacte et nettement eloignee des bords;
- utiliser une surface bleu-noir en degrade, rayon `18 dp` et bordure bleu ardoise fine;
- conserver un halo bleu electrique et un cadre blanc comme focus D-pad principal;
- afficher un seul bouton d action primaire par surface lorsque le workflow le permet;
- respecter le rythme d espacement `8 / 12 / 14 / 24 dp` et des controles d au moins `48 dp` de hauteur;
- adapter la grille au contenu: les deux colonnes sont requises uniquement pour les dialogues qui combinent formulaire et QR.

## Reference Xtream

- gabarit avant mise a l echelle: `680 x 410 dp`;
- panneau QR: `220 x 340 dp`;
- QR: `174 dp`;
- marges de securite: `64 dp` horizontales et `40 dp` verticales;
- implementation: `app/src/main/java/com/smartvision/svplayer/ui/activation/XtreamQrSetupPanel.kt`.

## Consequences

Les prochaines refontes de dialogues doivent reutiliser ce vocabulaire visuel et les memes contrats de focus, sans dupliquer automatiquement la disposition Xtream. La logique metier, le comportement Back, le focus initial et la restauration du focus appelant restent propres a chaque dialogue.
