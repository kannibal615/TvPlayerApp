# Plan d'integration TMDB complet

Derniere mise a jour: 2026-07-07.

## 1. Objectif produit

Integrer TMDB comme source de metadonnees enrichies pour les films et series du catalogue Xtream SmartVision, sans fournir de contenu, sans remplacer la lecture Xtream principale et sans bloquer l'interface Android TV.

Objectifs visibles:
- enrichir les fiches films avec posters/backdrops TMDB, synopsis, genres, duree, casting, realisation, note, certification et providers JustWatch quand disponibles;
- enrichir les fiches films/series avec trailers/teasers YouTube TMDB, casting/realisation/createurs avec photos, recommandations et note utilisateur locale;
- associer automatiquement les films et series Xtream a un `tmdb_id`;
- utiliser la langue reglee dans l'application pour les appels TMDB;
- ajouter une attribution TMDB/JustWatch dans un nouveau menu Settings;
- garder un cache Room local comme source prioritaire apres le premier enrichissement;
- aligner les donnees adultes avec le controle parental et les marqueurs adultes existants;
- preparer l'extension future vers series, episodes, Home, recherche enrichie et backend.

Non-objectifs du MVP:
- ne pas remplacer les URLs Xtream pour la lecture du film/episode principal;
- ne pas stocker de flux video TMDB;
- ne pas ajouter de validation admin obligatoire;
- ne pas bloquer Home, Movies ou Series pendant l'enrichissement;
- ne pas exposer de cle API, bearer token ou credentials Xtream dans les logs, la base, les docs ou le code versionne.

## 2. Contraintes et decisions

Decisions utilisateur:
- licence TMDB: licence personnelle pendant le developpement, a remplacer par une licence adaptee avant commercialisation;
- langue TMDB: derivee du reglage app (`English` -> `en-US`, `Francais` -> `fr-FR`);
- attribution: nouveau menu dans Settings;
- cache prioritaire: Room local;
- matching: automatique, sans validation admin;
- providers JustWatch: inclus dans le MVP;
- donnees adultes: filtre TMDB aligne avec le controle parental actuel.

Contraintes techniques:
- les secrets TMDB restent dans `local.properties`, jamais dans Git;
- `BuildConfig.TMDB_READ_ACCESS_TOKEN` expose uniquement une valeur locale au runtime;
- si le token est absent, TMDB est desactive proprement et l'UI garde les donnees Xtream;
- les appels TMDB passent par Retrofit/OkHttp et Network Activity;
- le cache TMDB est separe des tables catalogue Xtream pour eviter de casser les synchronisations;
- les erreurs reseau TMDB sont silencieuses cote utilisateur sauf diagnostic reseau; elles ne doivent pas empecher la lecture.

## 3. Architecture cible

Flux cible fiche film:
1. la route detail charge les infos Xtream existantes;
2. le ViewModel demande un enrichissement TMDB en arriere-plan;
3. `TmdbRepository` regarde d'abord Room;
4. si aucune association fiable n'existe, le repository nettoie le titre Xtream et interroge `search/movie`;
5. le meilleur candidat recoit un score de confiance;
6. au-dessus du seuil, l'association `contentType=movie/contentId=streamId/tmdbId` est stockee;
7. les details TMDB sont charges avec `append_to_response`;
8. les metadonnees sont stockees dans Room par langue;
9. l'UI affiche TMDB en priorite et garde Xtream en fallback champ par champ.

Flux cible fiche serie:
1. la route detail charge details/episodes Xtream;
2. le ViewModel demande une association TMDB en arriere-plan;
3. `TmdbRepository` utilise `search/tv` puis stocke le `tmdb_id`;
4. les details serie TMDB sont mis en cache Room;
5. le MVP garde l'UI serie majoritairement Xtream, mais le socle est pret pour enrichissement visuel futur.

Tables Room cible:
- `tmdb_content_mapping`: association Xtream -> TMDB, score, source, langue de matching, statut erreur;
- `tmdb_movie_metadata`: details film TMDB par `tmdbId` et langue;
- `tmdb_series_metadata`: details serie TMDB par `tmdbId` et langue.

Services:
- `TmdbApiService`: endpoints Retrofit TMDB v3;
- `TmdbRepository`: matching, cache, details, mapping;
- `TmdbMatcher`: normalisation titre/annee et scoring;
- `TmdbImageResolver`: generation d'URLs image TMDB avec fallback local.

## 4. Endpoints TMDB prevus

Configuration:
- `GET /configuration`

Recherche:
- `GET /search/movie`
- `GET /search/tv`

Details:
- `GET /movie/{movie_id}` avec `append_to_response=credits,videos,images,release_dates,watch/providers,recommendations`
- `GET /tv/{series_id}` avec `append_to_response=credits,videos,images,content_ratings,watch/providers,recommendations`

Parametres communs:
- `language`: issu du reglage SmartVision;
- `include_adult`: `false` quand le controle parental est actif, autorise seulement si la configuration locale le permet;
- `primary_release_year` ou `first_air_date_year`: utilise quand l'annee Xtream est exploitable.

## 5. Matching automatique

Nettoyage titre:
- supprimer tags qualite (`4K`, `FHD`, `HD`, `UHD`, `HEVC`, `x264`, `x265`);
- supprimer suffixes/brackets techniques (`[FR]`, `(MULTI)`, annees, codecs);
- normaliser espaces, ponctuation et casse;
- garder une version originale pour les comparaisons secondaires.

Scoring:
- titre normalise exact: score fort;
- titre original TMDB exact: score fort;
- tokens communs titre Xtream/TMDB: score progressif;
- annee exacte: bonus important;
- annee proche: petit bonus;
- popularite/votes: bonus faible, jamais suffisant seul;
- adulte exclu si controle parental actif;
- seuil MVP: association automatique seulement si confiance suffisante.

Fallback:
- recherche avec annee;
- recherche sans annee;
- comparaison sur `title`, `original_title`, `name`, `original_name`;
- si aucun score fiable, stocker l'erreur courte sans casser l'UI.

## 6. Lots de developpement

### Lot 1 - Socle donnees et cache local

Objectif:
- ajouter les entites Room TMDB et la migration de base.

Taches:
- ajouter `TmdbContentMappingEntity`;
- ajouter `TmdbMovieMetadataEntity`;
- ajouter `TmdbSeriesMetadataEntity`;
- etendre `MediaDao` avec lectures/upserts TMDB;
- migrer `SVDatabase` de version 10 a 11;
- documenter le cache local et la separation Xtream/TMDB.

Validation:
- compilation Kotlin/Room;
- migration SQL compatible avec une base existante;
- aucun impact sur synchronisation Xtream.

### Lot 2 - Prototype recherche et API TMDB

Objectif:
- ajouter les appels TMDB et le matching automatique.

Taches:
- ajouter `TMDB_READ_ACCESS_TOKEN` dans `BuildConfig`;
- ajouter le placeholder dans `local.properties.example`;
- creer `TmdbApiService` et DTOs;
- creer `TmdbRepository`;
- ajouter `TmdbMatcher`;
- ajouter `TmdbImageResolver`;
- brancher OkHttp/Retrofit dans `AppContainer`;
- faire echouer silencieusement TMDB si token absent ou appel en erreur.

Validation:
- compilation;
- aucune cle exposee;
- Network Activity identifie les appels TMDB.

### Lot 3 - Association films/series avec `tmdb_id`

Objectif:
- associer automatiquement les fiches detail Xtream a TMDB.

Taches:
- appeler l'enrichissement film depuis `MovieDetailViewModel`;
- appeler l'association serie depuis `SeriesDetailViewModel`;
- utiliser la langue app et le controle parental pour les parametres TMDB;
- stocker mapping et metadonnees en Room;
- conserver les donnees Xtream en fallback.

Validation:
- ouverture detail film/serie sans token: aucun crash;
- ouverture detail avec token: mapping stocke si confiance suffisante;
- erreurs TMDB non bloquantes.

### Lot 4 - Enrichissement visuel fiches films

Objectif:
- afficher les metadonnees TMDB sur la fiche film.

Taches:
- ajouter les champs enrichis dans `MovieDetailUiState`;
- prioriser backdrop/poster/synopsis/genres/duree/casting/realisateur/note TMDB;
- afficher un indicateur source TMDB discret;
- afficher les providers JustWatch si presents;
- garder extension/favori/lecture Xtream inchanges;
- ajouter le menu Settings d'attribution TMDB/JustWatch.

Validation:
- fiche film lisible en fallback Xtream;
- fiche film enrichie si TMDB disponible;
- les boutons `Regarder` et `Favori` gardent leur focus et comportement.

### Lot 5 - Enrichissement visuel series

Objectif:
- appliquer aux fiches series les metadonnees TMDB deja cachees.

Taches:
- utiliser poster/backdrop/synopsis/genres/cast TMDB sur `SeriesDetailScreen`;
- afficher certification/ratings si disponible;
- garder saisons/episodes Xtream comme source de lecture.

Validation:
- aucune regression saison/episode;
- matching serie reutilise depuis Room.

Statut 2026-07-07:
- implemente dans `SeriesDetailScreen.kt`;
- la fiche serie priorise TMDB pour titre, poster, backdrop, synopsis, genre, date, note, duree episode, casting, createurs, certification, providers, videos et recommandations;
- la fiche serie est scrollable et ajoute trailers/teasers dans un mini-player YouTube, casting/createurs avec photos, note utilisateur locale et recommandations TMDB;
- les saisons/episodes, ids et URLs de lecture restent Xtream;
- fallback Xtream conserve si TMDB est absent ou non configure.

### Lot 6 - Home et catalogues enrichis

Objectif:
- utiliser les images/metadonnees TMDB dans les surfaces de browsing sans ralentir l'ouverture.

Taches:
- enrichir seulement les items visibles/proches;
- ne pas reintroduire de snapshot catalogue complet;
- reutiliser Room et pagination;
- eviter tout appel TMDB pendant le splash.

Validation:
- Home reste immediatement navigable;
- pas de chargement massif ni OOM Firestick.

Statut 2026-07-07:
- implemente sans appel TMDB au splash;
- Home enrichit les cards Tendances seulement pendant `prepareTrendingPreview`, deja declenche sur items visibles/proches et borne par `HomeViewModel`;
- Movies et Series relisent seulement le cache TMDB Room local sur les premiers items charges, sans recherche reseau par item;
- les grilles restent paginees Room et gardent les images/titres Xtream en fallback.
- valide en release `0.1.109` / `versionCode 113`: prod publiee, APK installe sur Firestick, Home rendu, `TMDB token` actif dans Settings, et appel `TMDB: search tv` HTTP 200 visible dans Network Activity.
- extension lots 7/8: les tendances utilisent maintenant les posters/backdrops TMDB et, si disponible, le trailer YouTube TMDB comme preview; elles ne lancent plus la video Xtream d'origine comme preview. Les anciens caches `movie`/`episode` sont neutralises en migration `11 -> 12` et ignores a la lecture.
- Home affiche des skeleton cards non focusables pour `Continue watching`, `Trending movies` et `Trending series` tant que les donnees necessaires ne sont pas chargees.

### Lot 7 - Maintenance, refresh et observabilite

Objectif:
- maitriser la fraicheur et les erreurs TMDB.

Taches:
- ajouter TTL cache par langue;
- ajouter retry/backoff simple;
- ajouter nettoyage des metadonnees orphelines;
- exposer les appels dans Network Activity sans query sensible;
- documenter les problemes recurrents dans troubleshooting si necessaire.

Validation:
- pas de spam reseau;
- erreurs visibles seulement en diagnostic.

Statut 2026-07-07:
- implemente cote Room/repository avec TTL metadonnees TMDB par langue: cache frais pendant 7 jours, fallback stale autorise si le reseau echoue, retention locale 90 jours;
- nettoyage borne des metadonnees TMDB obsoletes via `deleteStaleTmdbMovieMetadata` et `deleteStaleTmdbSeriesMetadata`;
- la preparation Home trends reste bornee par `HomeViewModel` et dedupliquee par item;
- les appels TMDB restent dans Network Activity via OkHttp sans exposer token, query sensible ou secret;
- la migration Room passe a `SVDatabase` version `12` pour stocker cast/director/createurs/videos/recommandations et neutraliser les anciens previews Xtream en cache.

### Lot 8 - Preparation commercialisation

Objectif:
- rendre l'usage TMDB conforme avant distribution commerciale.

Taches:
- verifier licence commerciale TMDB;
- verifier attribution officielle dans Settings/About/Credits;
- ajouter politique de cache/rafraichissement conforme;
- revoir usage JustWatch selon conditions applicables;
- deplacer eventuellement certains appels cote backend si necessaire.

Validation:
- revue juridique/licence effectuee avant publication commerciale.

Statut 2026-07-07:
- attribution TMDB/JustWatch deja visible dans Settings, avec statut configure/non configure sans exposer le token;
- politique locale documentee: Room prioritaire, TTL 7 jours, retention 90 jours, fallback Xtream si TMDB absent/echec;
- les providers JustWatch restent affiches uniquement comme metadonnees disponibles;
- prerequis non code avant commercialisation: remplacer la licence personnelle par une licence/autorisation TMDB adaptee et revalider les conditions TMDB/JustWatch applicables a la distribution commerciale.

## 7. Etat final MVP TMDB au 2026-07-07

Lots 1 a 8 cote application Android:
- Room: mapping Xtream -> TMDB, metadonnees films/series enrichies, cache Home trends, schema `12`;
- API: recherche et details TMDB avec credits, videos, images, certifications, providers et recommandations;
- Details films/series: ecrans scrollables, TMDB prioritaire champ par champ, posters/backdrops, synopsis, genres, durees, note TMDB, note utilisateur locale, casting/realisation/createurs avec photos, trailers/teasers YouTube en mini-player, recommandations;
- Home: tendances avec images TMDB et trailer YouTube TMDB en preview; aucun lancement du flux Xtream d'origine pour les previews tendances;
- Loading: skeleton cards visibles pour Continue watching et tendances pendant les chargements initiaux;
- Compliance technique: token local uniquement, attribution Settings, cache local documente, fallback non bloquant.

## 8. Regles a ne pas casser

- Ne jamais bloquer la lecture ou l'ouverture detail si TMDB echoue.
- Ne jamais ecrire les secrets dans Git, docs, logs ou Room.
- Ne jamais stocker d'URL de lecture Xtream dans les tables TMDB.
- Ne jamais declencher un enrichissement massif au demarrage.
- Ne jamais remplacer les ids Xtream par les ids TMDB pour la lecture.
- Garder `MovieDetailScreen` et `SeriesDetailScreen` utilisables au D-pad.
- Garder le controle parental prioritaire sur les donnees adultes.
- Garder JustWatch comme metadonnee d'information, pas comme source de lecture SmartVision.

## 9. Fichiers principaux

Documentation:
- `docs/ai-knowledge/features/tmdb-integration-plan.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Android:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/data/tmdb/*`
- `app/src/main/java/com/smartvision/svplayer/data/local/entity/MediaEntities.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/SVDatabase.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/detail/MovieDetailScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/detail/SeriesDetailScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/detail/TmdbDetailRichContent.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/TrendingContentRow.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeSkeletonRow.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`

## 10. Definition de termine MVP Lots 1-4

Le MVP Lots 1-4 est termine quand:
- la documentation A-Z existe et est routee depuis `ROOT.md`;
- Room contient les tables TMDB et migre proprement;
- l'application compile;
- TMDB est optionnel si le token local est absent;
- l'ouverture d'une fiche film tente un enrichissement non bloquant;
- l'ouverture d'une fiche serie tente au moins l'association/cache;
- la fiche film utilise les champs TMDB en priorite quand ils existent;
- Settings contient une entree d'attribution TMDB/JustWatch;
- les docs et le changelog IA refletent les changements.

## 11. Definition de termine Lots 5-6

Les lots 5-6 sont termines quand:
- la fiche serie affiche les donnees TMDB disponibles avec fallback Xtream;
- les saisons/episodes series restent source Xtream;
- Home n'utilise TMDB que dans la preparation preview non bloquante;
- Movies/Series reutilisent le cache TMDB local sans recherche reseau massive;
- `:app:compileReleaseKotlin` passe;
- les docs et le changelog IA sont a jour.

## 12. Definition de termine Lots 7-8

Les lots 7-8 sont termines cote application quand:
- le cache TMDB applique une politique de fraicheur et retention documentee;
- les metadonnees stale restent utilisables si le reseau echoue;
- les anciennes previews Home Xtream sont neutralisees et les nouvelles previews tendances utilisent seulement les trailers YouTube TMDB;
- les ecrans detail films/series exposent les videos, personnes, note utilisateur et recommandations;
- Settings affiche l'attribution TMDB/JustWatch sans secret;
- `:app:compileReleaseKotlin` et `:app:assembleRelease` passent.

Avant commercialisation, il reste obligatoire de remplacer la licence personnelle TMDB par une licence/autorisation adaptee et de revalider les conditions TMDB/JustWatch applicables.
