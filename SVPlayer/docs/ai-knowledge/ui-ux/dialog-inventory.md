# Inventaire des dialogues Android TV

Derniere mise a jour: 2026-07-24.

## Objectif et perimetre

Cet inventaire couvre les surfaces modales Compose du chemin actif `MainActivity -> AppNavigation -> ui/*`. La recherche a combine les appels `Dialog(...)`, les composants dont le nom contient `Dialog`, les appels du shell partage `TvDialogSurface` / `TvConfirmationDialog` et leurs etats d ouverture. Les definitions sans appel actif sont marquees `dormant` et ne doivent pas etre confondues avec un ecran actuellement joignable.

Les ecrans de fin d essai et d activation pleine page ne sont pas des dialogues Compose. Ils sont listes a part pour eviter de les perdre pendant une future uniformisation de toutes les surfaces transitoires.

## Composants de base existants

| Composant | Fonction | Fichier | Etat visuel |
|---|---|---|---|
| `TvDialogSurface` | Shell modal generique: scrim, cadre, icone, titre, message, focus initial et fermeture Back. | `ui/components/TvDialogs.kt` | Meilleure base commune actuelle. |
| `TvConfirmationDialog` | Confirmation standard, destructive ou informative construite sur `TvDialogSurface`. | `ui/components/TvDialogs.kt` | Deja reutilisee dans la majorite des suppressions. |
| `NumericPinDialog` | Saisie/creation/verification de PIN parental au D-pad. | `ui/components/NumericPinDialog.kt` | Specialise, clavier numerique et focus propres. |
| `SmartVisionQrDialog` | Route vers un QR seul ou le dialogue Premium avec QR + code licence. | `ui/profile/ProfileScreen.kt` | Style Premium specifique, non aligne sur `TvDialogSurface`. |

## Inventaire des dialogues actifs

| Dialogue / variante | Fonction et declencheur | Ecran lie | Fichier principal | Construction actuelle | Point de standardisation |
|---|---|---|---|---|---|
| Conditions d utilisation / consentement | Affiche le texte legal distant; impose le scroll jusqu en bas avant acceptation. | Demarrage global avant navigation | `ui/appconfig/ConsentDialog.kt` | `Dialog` custom plein, scroll et focus specialises. | Conserver sa logique de lecture obligatoire, normaliser cadre/titre/boutons. |
| PIN parental - deverrouillage | Verifie le PIN avant Profil admin, Controle parental ou profil verrouille. | Who s Watching, Profil, Gestion profils, Controle parental | `ui/components/NumericPinDialog.kt`; appels dans `ProfilePickerScreen.kt`, `ProfileScreen.kt`, `ProfileAreaScreen.kt`, `ParentalControlPanel.kt` | Dialogue specialise partage. | Unifier seulement l enveloppe; conserver clavier, erreur et shake. |
| PIN parental - creation/changement | Saisie puis confirmation du nouveau PIN. | Profil et Controle parental | memes fichiers que ci-dessus | Meme composant avec mode `Create`. | Ajouter titres/aides et etats d erreur communs. |
| QR Premium / Remove ads global | Achat Premium depuis une feature verrouillee ou une action Licence. Peut aussi accepter un code licence. | Global `AppNavigation`, Parametres Licence, Profil historique | `ui/profile/ProfileScreen.kt` (`SmartVisionQrDialog`, `PremiumQrOnlyDialog`, `PremiumLicenseDialog`) | Deux grands dialogues Premium custom. | Une seule famille Premium; QR, code TV, saisie code optionnelle et fermeture coherents. |
| Connexion Xtream indisponible | Avertit que le catalogue est bloque; propose reessayer/configurer. | Home, Live TV, Movies, Series, YouTube/Media gates | `ui/navigation/AppNavigation.kt` (`XtreamConnectionAlertDialog`) | `TvDialogSurface`. | Garder comme reference pour alertes reseau. |
| Configuration Xtream par QR | Saisie les identifiants sur la TV ou affiche le QR direct vers `/playlist/` avec le code TV persistant pre-rempli. | Global, Home sans source, Live TV, Movies, Series | `ui/activation/XtreamQrSetupPanel.kt`; ouverture modale dans `ui/navigation/AppNavigation.kt` | Premier dialogue conforme au standard visuel SmartVision v1; aucun shortcode temporaire dans le QR Xtream. | Reference de proportions, fond, surface, focus et composition pour les prochaines migrations. |
| Sort catalogue | Liste les choix de tri et restaure le focus sur le bouton. | Movies / Series via composant catalogue | `ui/catalog/CatalogSortButton.kt` | `Dialog` local compact. | Migrer vers un menu modal standard compact. |
| Reprendre la lecture | Demande reprise ou lecture depuis le debut pour VOD/episode. | Player plein ecran | `ui/player/FullScreenPlayerScreen.kt` (`ResumePlaybackDialog`) | `Dialog` custom. | Normaliser bouton principal/secondaire et focus par defaut. |
| Synchronisation Xtream | Montre progression, compteurs, phases et erreurs pendant la synchro. | Profil > Synchronisation | `ui/profile/ProfileScreen.kt` (`XtreamSynchronizationDialog`) | `Dialog` custom non dismissable pendant travail. | Creer variante `Progress` du shell commun. |
| Editeur de profil playlist | Cree/modifie profil NORMAL/KIDS, avatar, filtres et identifiants Xtream/M3U. | Who s Watching, Gerer les profils | `ui/profile/ProfileScreen.kt` (`PlaylistProfileEditorDialog`) | Grande surface construite sur `TvDialogSurface`. | Reference pour formulaires modaux; clarifier footer fixe et parcours D-pad. |
| Choix avatar | Choisit l avatar autorise selon le type de profil. | Editeur de profil | `ui/profile/ProfileScreen.kt` (`ProfileAvatarPickerDialog`) | `TvDialogSurface`. | Reutiliser grille/focus commun pour tous les pickers. |
| Suppression profil playlist | Confirme la suppression definitive d un profil. | Gerer les profils | `ui/profile/ProfileScreen.kt` (`ConfirmPlaylistProfileDeleteDialog`) | `TvConfirmationDialog`. | Deja standard; conserver ton destructif. |
| Suppression profil depuis Profile Area | Meme action depuis l autre surface de gestion. | Profile Area | `ui/profile/ProfileAreaScreen.kt` | `TvConfirmationDialog`. | Fusionner le texte et le contrat avec la variante precedente. |
| Editeur URL EPG/M3U | Modifie une URL avec champ TV puis valide. | Profil > Info compte | `ui/profile/ProfileScreen.kt` (`UrlEditorDialog`) | `TvDialogSurface`. | Creer variante formulaire simple partagee. |
| Edition mot-cle parental | Ajoute ou modifie un mot-cle filtre. | Controle parental | `ui/profile/ParentalControlPanel.kt` (`KeywordEditDialog`) | `Dialog` custom. | Migrer vers le formulaire simple partage. |
| Suppression mot-cle parental | Confirme la suppression du mot-cle. | Controle parental | `ui/profile/ParentalControlPanel.kt` | `TvConfirmationDialog`. | Deja standard. |
| Suppression historique Live | Supprime une chaine de l historique. | Live TV > Historique | `ui/live/LiveTvScreen.kt` | `TvConfirmationDialog`. | Deja standard, meme vocabulaire que Movies/Series. |
| Suppression historique Movie | Supprime un film de l historique. | Movies > Historique | `ui/movies/MoviesScreen.kt` | `TvConfirmationDialog`. | Deja standard. |
| Suppression historique Series | Supprime une serie de l historique. | Series > Historique | `ui/series/SeriesScreen.kt` | `TvConfirmationDialog`. | Deja standard. |
| Nettoyage YouTube | Confirme suppression recherche, historique video ou favoris. | YouTube > Parametres | `ui/youtube/YoutubeScreen.kt` | `TvConfirmationDialog`. | Deja standard; retour vers le dialogue Parametres a conserver. |
| Parametres YouTube | Choix internes et acces aux nettoyages. | YouTube | `ui/youtube/YoutubeScreen.kt` (`YoutubeSettingsDialog`) | `Dialog` custom. | Migrer vers shell menu compact. |
| Nettoyage donnees locales | Supprime reglages/cache/catalogue local selon le workflow actuel. | Parametres > Donnees | `ui/settings/SettingsScreen.kt` | `TvConfirmationDialog`. | Deja standard, verifier la precision du texte destructif. |
| Actions en masse Notifications | Confirme `Tout marquer vu` ou `Effacer historique`. | Notifications | `ui/notifications/NotificationsScreen.kt` | `TvConfirmationDialog`, ton adapte. | Bon exemple de confirmation informative/destructive. |
| Detail notification | Affiche titre, message et details du payload admin. | Notifications | `ui/notifications/NotificationsScreen.kt` (`NotificationDetailsDialog`) | `TvDialogSurface`. | Reference pour dialogue informatif. |
| Mise a jour application | Affiche version, progression, telechargement/installation et erreurs. | Global, Parametres/notification update | `ui/update/AppUpdateDialog.kt` | `TvDialogSurface`. | Creer variante `Progress/Update` standard. |
| Quitter l application | Confirme la fermeture depuis Back aux niveaux racine. | Home, Activation et navigation racine | `ui/navigation/AppNavigation.kt` (`ExitConfirmationDialog`) | `TvConfirmationDialog`. | Deja standard; focus initial sur Annuler recommande. |
| Transfert Media telephone/TV | Affiche QR, statut serveur de transfert et instructions. | Media Center | `ui/media/MediaScreen.kt` (`MediaTransferDialog`) | `TvDialogSurface` via `MediaDialogPanel`. | Variante QR/progress a rapprocher du shell QR commun. |
| Renommer Media | Edite le nom d un fichier/dossier. | Media Center | `ui/media/MediaScreen.kt` (`MediaRenameDialog`) | `TvDialogSurface`. | Utiliser formulaire simple commun. |
| Deplacer Media | Choisit le dossier destination. | Media Center | `ui/media/MediaScreen.kt` (`MediaMoveDialog`) | `TvDialogSurface`. | Utiliser picker modal commun. |
| Supprimer Media | Confirme suppression fichier/dossier. | Media Center | `ui/media/MediaScreen.kt` (`MediaDeleteDialog`) | `TvConfirmationDialog`. | Deja standard. |

## Definitions presentes mais non joignables dans le chemin actif

| Definition | Fichier | Observation |
|---|---|---|
| `AccountEditorDialog` | `ui/settings/SettingsScreen.kt` | Definition sans appel actif; ancien CRUD compte local. |
| `LiveRecordDialog` | `ui/player/FullScreenPlayerScreen.kt` | Definition sans appel direct trouve dans le code actuel; verifier avant standardisation Recorder. |

## Surfaces proches a inclure dans l etude visuelle

- `ActivationScreen`: surface pleine page unifiee, pas un `Dialog` Compose. Depuis le 2026-07-24, activation initiale, `TRIAL_EXPIRED` et `LICENSE_EXPIRED` partagent la meme carte; seule l action verte varie selon `ActivationOfferMode`.
- le QR Premium, le code TV et la saisie licence sont integres directement dans cette carte. L ancien `ActivationPurchaseDialog` imbrique est supprime afin qu une seule surface d activation soit visible.
- `XtreamQrSetupPanel`: panneau reutilise dans des ecrans et dans le dialogue global.
- overlays du player: controles, panneau episodes, luminosite et parametres sont des surfaces transitoires internes, mais pas des `Dialog` Compose.

Depuis le 2026-07-23, l ancien `XtreamSetupDialog` dormant est supprime. `XtreamQrSetupPanel` est l unique formulaire Xtream actif et le handoff du demarrage attend la resolution d acces avant de le reveler.

## Proposition de standardisation

1. Conserver `TvDialogSurface` comme unique enveloppe: scrim, rayon, bordure, largeur, titre, icone, Back et focus initial.
2. Definir cinq variantes: `Information`, `Confirmation`, `Form`, `Qr`, `Progress`.
3. Conserver trois composants specialises au-dessus de ce shell: `NumericPinDialog`, consentement scroll obligatoire et editeur de profil complexe.
4. Fusionner les familles dupliquees: Premium QR/licence, QR Xtream/Media, formulaires URL/nom/mot-cle et suppressions profil.
5. Pour chaque variante, imposer: focus initial visible, ordre D-pad, bouton Annuler accessible, Back ferme la surface la plus proche, action destructive rouge, et restauration du focus appelant.

## Standard visuel SmartVision v1 - dialogue Xtream

Le dialogue `XtreamQrSetupPanel` est la premiere reference approuvee pour l uniformisation progressive des dialogues. La maquette de validation est `Generated image 2.png` fournie le 2026-07-22.

Regles visuelles a reutiliser:

- fond plein ecran: ressource officielle `R.drawable.startup_cinema_background`, recouverte d un voile bleu-noir leger;
- surface centree: gabarit virtuel `680 x 410 dp` avant `ScaledActivationLayout(0.88)`, soit environ 60 % de la largeur et 65 % de la hauteur sur la cible TV de reference;
- marges de securite: `64 dp` horizontales et `40 dp` verticales autour de la zone centree;
- surface principale: rayon `18 dp`, bordure bleu ardoise fine, degrade `#0A1A31 -> #030D1C`;
- composition Xtream: colonne formulaire flexible, espace `24 dp`, panneau QR fixe de `220 x 340 dp`;
- hierarchie: logo officiel, titre court, trois champs, une seule action principale; supprimer les explications et etapes secondaires non indispensables;
- focus TV: halo bleu electrique externe, cadre blanc interne de `2 dp`, fond bleu sombre sans changement de dimensions;
- QR: carte blanche `174 dp`, bordure bleu clair, code court de session affiche dans un cartouche distinct;
- rythme: espacements bases sur `8 / 12 / 14 / 24 dp`, textes principaux blancs et textes secondaires gris bleute;
- les prochains dialogues doivent reprendre le fond, la surface, le focus, les marges et la hierarchie, mais adapter leur composition au contenu au lieu de recopier deux colonnes quand elles ne sont pas utiles.

Depuis le 2026-07-24, `ActivationScreen` est la deuxieme implementation de reference: meme gabarit `680 x 410 dp`, QR `174 dp`, panneau droit `220 dp`, fond splash et focus bleu/blanc. Elle valide aussi le contrat de fusion: une seule surface Compose pour plusieurs etats metier, avec contenu stable et action primaire variable.

## Priorite de revue recommandee

- P1: Premium/QR/licence, consentement, PIN et Xtream, car ils bloquent activation/acces.
- P2: confirmations destructives et progression update/synchronisation.
- P3: formulaires profil/URL/Media et menus compacts YouTube/tri.
- P4: overlays player non-Dialog, dans un lot distinct pour ne pas melanger les contrats Back.
