# Spec cible V1 - Tracking Comportemental, Segmentation Utilisateur et Ciblage Publicitaire

Derniere mise a jour: 2026-06-30.

## 1. Objectif

Construire une cible V1 simple et fiable pour comprendre les comportements utilisateurs dans SmartVision, produire des segments exploitables par le futur systeme publicitaire et garder une approche privacy-first.

La V1 ne doit pas faire de machine learning. Elle doit collecter des evenements normalises, agreger des signaux par appareil, calculer des segments par regles et exposer ces segments au backend publicitaire.

Important: ce document decrit la V1 maintenant partiellement implementee. L'etat actuel inclut le tracking comportemental generique, les agregats, les segments admin et l'inference region/pays/langue/interets depuis categories et medias. Le ciblage publicitaire base sur ces segments reste l'etape suivante.

## 2. Etat actuel

Deja en place:
- tracking publicitaire dans `ads_events`;
- tracking comportemental Live TV, Movies, Series, Episodes et YouTube dans `app_behavior_events`;
- endpoint Android `POST api/app/behavior-events`;
- endpoint Android `POST api/app/ads-events`;
- table `user_behavior_daily`;
- table `user_segments`;
- menu admin `Segmentation`;
- ecran admin `Segmentation` organise en onglets Vue, Segments, Interpretation et Evenements;
- popup detail appareil avec onglets Tracking et Analyse;
- inference backend des regions, pays, langues et centres d'interet depuis `category_label`, `content_title` et `tags`;
- normalisation admin des types licence comportementaux en `free_ads`, `trial_demo`, `premium`;
- diagnostics admin autostart/auto-sync via `app_device_diagnostics`;
- historique local YouTube avec video, channel, category, tags, duree, source de lancement et champs comportementaux enrichis;
- historique local de lecture, favoris, progression, categories Live/Films/Series;
- donnees device: pays, version app, statut licence, essai, free_ads, xtream_status.

Evenements comportementaux actuellement acceptes par `behavior_service.php`:
- `VIDEO_OPENED`;
- `PLAY_PAUSE`;
- `VIDEO_COMPLETED`;
- `SUGGESTION_OPENED`;
- `CONTENT_OPENED`;
- `PLAYBACK_PROGRESS`;
- `PLAYBACK_COMPLETED`;
- `FAVORITE_ADDED`;
- `FAVORITE_REMOVED`;
- `SEARCH_PERFORMED`;
- `PLAYER_ERROR`.

Evenements explicitement retires du tracking comportemental:
- `CATEGORY_OPENED`;
- `PLAYBACK_STARTED`.
- `PLAYER_READY`.

Limites actuelles:
- le ciblage publicitaire ne consomme pas encore `user_segments`;
- l'inference pays/langue/interets est basee sur regles lexicales, a enrichir progressivement avec les vrais noms categories/medias observes;
- le stockage local offline/batch durable Android n'est pas encore implemente;
- la retention avancee et les seuils configurables restent a finaliser.

## 3. Donnees V1 a exploiter

### 3.1 Signaux contenus

Collecter uniquement des signaux utiles au ciblage, sans secrets ni contenu sensible brut.

Types de contenu:
- `LIVE_TV`;
- `MOVIE`;
- `SERIES`;
- `EPISODE`;
- `YOUTUBE`.

Evenements V1:
- `CONTENT_OPENED`: contenu ouvert ou chaine selectionnee;
- `PLAYBACK_PROGRESS`: progression significative, limitee en frequence;
- `PLAYBACK_COMPLETED`: contenu termine;
- `FAVORITE_ADDED`;
- `FAVORITE_REMOVED`;
- `SEARCH_PERFORMED`: requete normalisee ou hash selon contexte;
- `SUGGESTION_OPENED`;
- `AD_REQUESTED`;
- `AD_STARTED`;
- `AD_FAILED`;
- `AD_COMPLETED` si disponible plus tard.

Metadonnees exploitables:
- type de contenu;
- category_id;
- category_name normalisee si disponible;
- genre;
- pays ou langue deduits de categorie/titre si non sensible;
- duree du contenu ou bucket de duree;
- position et pourcentage de completion;
- tags YouTube normalises;
- channel_id YouTube;
- source YouTube de lancement: `SEARCH`, `SUGGESTION`, `HISTORY`, `TRENDING`, `MUSIC`, `SPORT`, `GAMING`, `NEWS`, `DOCUMENTARIES`, `KIDS`, `AUTOPLAY`, `PREVIOUS`, `NEXT`;
- categorie YouTube interpretee depuis titre, description, tags ou categoryId YouTube: `music`, `news`, `sports`, `tutorial`, `kids`, `cinema`, `documentaries`, `gaming`, `technology`, etc.;
- statut utilisateur: premium, trial, free_ads, expired;
- version app, plateforme, pays device.

### 3.2 Signaux a eviter

Ne pas envoyer:
- identifiants Xtream;
- URL de lecture;
- username/password;
- titre exact long ou sensible; la V1 stocke un `content_title` court nettoye pour permettre l'analyse admin et l'inference, sans URL ni secret Xtream;
- adresse IP brute;
- device_id brut hors endpoint deja hash cote serveur;
- donnees personnelles declarees par l'utilisateur;
- contenu adulte explicite sous forme de texte brut.

Quand un identifiant contenu est necessaire, utiliser un hash ou un ID technique deja non personnel.

## 4. Modele de donnees cible

### 4.1 Table evenement comportemental etendue

Objectif: etendre ou versionner `app_behavior_events`.

Champs V1 recommandes:
- `id`;
- `device_id_hash`;
- `app_version`;
- `platform`;
- `user_status`;
- `event_type`;
- `content_type`;
- `content_id_hash`;
- `category_id`;
- `category_name`;
- `genre`;
- `language_hint`;
- `country_hint`;
- `duration_bucket`;
- `completion_percent`;
- `source_surface`;
- `metadata_json`;
- `created_at`.

Index minimum:
- `created_at`;
- `device_id_hash, created_at`;
- `content_type, event_type`;
- `category_id`;
- `user_status`;

### 4.2 Table segments utilisateur

Nouvelle table recommandee: `user_segments`.

Champs:
- `id`;
- `device_id_hash`;
- `segment_key`;
- `score`;
- `confidence`;
- `source`;
- `window_days`;
- `created_at`;
- `updated_at`;
- `expires_at`.

Regle d'unicite:
- `device_id_hash + segment_key + window_days`.

### 4.3 Table agregats quotidiens

Nouvelle table recommandee: `user_behavior_daily`.

Champs:
- `device_id_hash`;
- `event_date`;
- `content_type`;
- `category_id`;
- `genre`;
- `language_hint`;
- `country_hint`;
- `opens_count`;
- `playback_seconds`;
- `completed_count`;
- `favorites_count`;
- `ads_started_count`;
- `ads_failed_count`;
- `updated_at`.

Cette table evite de recalculer les segments depuis tous les evenements bruts.

## 5. Segments V1

Les segments doivent etre explicables, recalculables et utilisables sans decision obscure.

Segments contenus:
- `live_heavy`: forte consommation Live TV;
- `movies_heavy`: forte consommation films;
- `series_heavy`: forte consommation series/episodes;
- `youtube_heavy`: forte consommation YouTube;
- `kids_interest`: categories/tags enfants;
- `sports_interest`: categories/tags sport;
- `news_interest`: categories/tags actualites;
- `music_interest`: YouTube musique ou categories musique;
- `documentary_interest`: documentaires;
- `cinema_interest`: films, cinema, genres associes.

Segments langue/pays:
- `french_content_interest`;
- `arabic_content_interest`;
- `english_content_interest`;
- `country_fr_interest`;
- `country_ma_interest`;
- autres pays uniquement si le signal est fiable.

Segments engagement:
- `high_engagement`: usage recent frequent;
- `low_engagement`: usage rare;
- `binge_viewer`: plusieurs episodes ou longues sessions;
- `short_session_viewer`: sessions courtes recurrentes;
- `favorites_user`: usage significatif des favoris.

Segments pub:
- `ad_eligible_free_ads`;
- `ad_started_recently`;
- `ad_failures_recent`;
- `ad_tolerant`: pubs demarrees sans abandon detecte;
- `ad_sensitive`: echecs ou abandons apres pub si mesurable.

## 6. Regles de scoring V1

Fenetre par defaut:
- 7 jours pour comportement recent;
- 30 jours pour interets stables;
- expiration segment apres 45 jours sans signal.

Score:
- entier de 0 a 100;
- score >= 60: segment actif;
- score 40-59: segment faible, visible admin mais non utilise pour ciblage strict;
- score < 40: segment ignore.

Exemples de regles:
- `movies_heavy`: au moins 5 lectures films sur 7 jours ou 40% du temps de lecture recent;
- `series_heavy`: au moins 6 episodes ou 3 series distinctes sur 7 jours;
- `youtube_heavy`: au moins 8 videos ouvertes ou 30% des ouvertures recentes;
- `sports_interest`: au moins 3 signaux sport sur 30 jours;
- `kids_interest`: au moins 3 signaux enfants sur 30 jours, avec prudence publicitaire;
- `high_engagement`: activite sur au moins 4 jours dans les 7 derniers jours;
- `ad_failures_recent`: au moins 2 `AD_FAILED` dans les 7 derniers jours.

Les seuils doivent rester configurables cote backend pour ajustement sans release Android.

## 7. API et flux technique

### 7.1 Android vers backend

Ajouter un reporter comportemental generique, distinct du reporter YouTube actuel ou en evolution de celui-ci.

Flux:
1. L'app cree un evenement local quand l'utilisateur interagit avec un contenu.
2. L'evenement est stocke localement si offline.
3. Un batch limite est envoye a `POST api/app/behavior-events`.
4. Le serveur valide, nettoie, rate-limit et stocke.
5. Les evenements envoyes sont marques comme synchronises.

Contraintes:
- batch maximum V1: 40 evenements;
- pas d'envoi bloquant la lecture;
- fail silent cote app si le tracking echoue;
- rate limit par appareil cote serveur;
- pas de log de payload sensible.

### 7.2 Backend aggregation

Traitement recommande:
- job admin manuel V1 ou cron plus tard;
- lire les evenements recents;
- mettre a jour `user_behavior_daily`;
- recalculer `user_segments`;
- supprimer ou archiver les evenements bruts selon politique de retention.

### 7.3 Publicite

Le systeme publicitaire ne doit pas lire l'historique brut. Il doit consommer:
- `device_id_hash`;
- liste des `segment_key`;
- score;
- user_status;
- content_type courant;
- pays/langue si disponible.

Decision V1:
- ciblage par segments simples;
- fallback pub generale si aucun segment fiable;
- aucune pub plus intrusive pour les segments sensibles.

## 8. Admin V1

Ajouter plus tard une page ou un bloc admin "Segmentation".

Vues minimales:
- nombre d'appareils segmentes;
- top segments 7j/30j;
- repartition par statut utilisateur;
- segments avec taux `AD_STARTED` / `AD_FAILED`;
- evenements comportementaux recents masques;
- fiche device: segments actifs, score, derniere activite, sans historique brut detaille par defaut.

Actions admin:
- recalculer segments;
- desactiver temporairement un segment;
- ajuster seuils de scoring;
- exporter uniquement des statistiques agregees.

## 9. Consentement, confidentialite et retention

Principes:
- informer l'utilisateur que les donnees d'utilisation peuvent servir a ameliorer l'app et la publicite;
- ne pas collecter de secret;
- hasher les identifiants device et contenu quand possible;
- garder les donnees brutes peu longtemps;
- garder les segments/agregats plus longtemps mais avec expiration.

Retention recommandee V1:
- evenements bruts: 90 jours maximum;
- agregats quotidiens: 180 jours;
- segments: expiration apres 45 jours sans signal;
- logs techniques: duree minimale utile au diagnostic.

Point important:
- les segments `kids_interest` ou contenus sensibles doivent etre traites en mode protection, pas pour augmenter la pression publicitaire.

## 10. Plan de livraison V1

Phase 1 - Documentation et validation:
- valider cette spec;
- choisir les segments V1 actifs;
- valider consentement et retention;
- confirmer si `app_behavior_events` est etendue ou si une nouvelle table est creee.

Phase 2 - Backend:
- schema SQL implemente;
- validation payload implemente;
- batch endpoint implemente;
- agregation quotidienne implemente;
- calcul segments implemente;
- tests PHP.

Phase 3 - Android:
- reporter generique;
- stockage local des evenements;
- hooks Live TV, films, series, episodes, YouTube;
- envoi batch non bloquant;
- tests unitaires si possible.

Phase 4 - Admin:
- dashboard segments implemente;
- top segments implemente;
- fiche device implemente avec onglets;
- controles de seuils.

Phase 5 - Publicite:
- lecture des segments;
- mapping segment -> campagne/provider/config;
- fallback general;
- monitoring `AD_STARTED`, `AD_FAILED`, revenus estimes.

## 11. Criteres d'acceptation V1

La V1 est acceptable si:
- un utilisateur Live/Film/Serie/YouTube genere des evenements normalises;
- aucun secret Xtream n'est envoye;
- les evenements sont rates limites;
- les segments sont recalcules depuis des donnees agregees;
- au moins 8 segments V1 sont visibles en admin;
- le systeme pub peut recuperer les segments actifs d'un appareil;
- la lecture continue meme si le tracking echoue;
- les donnees brutes ont une politique de retention documentee;
- les tests backend valident payload invalide, rate limit, stockage et scoring.

## 12. Hors scope V1

Ne pas faire en V1:
- machine learning;
- profilage individuel nominatif;
- export de donnees brutes utilisateur;
- ciblage base sur secrets Xtream;
- personnalisation intrusive de l'interface;
- modification du player pour forcer plus de pubs;
- dashboard revenu avance.

## 13. Questions a trancher avant implementation

- Etendre `app_behavior_events` ou creer `app_content_events` dediee ?
- Garder `POST api/app/behavior-events` comme endpoint unique ou ajouter `POST api/app/behavior-events/batch` ?
- Quels segments sont autorises pour le ciblage publicitaire des le lancement ?
- Le consentement actuel couvre-t-il explicitement la personnalisation publicitaire ?
- Quelle duree de retention doit etre appliquee en production ?
