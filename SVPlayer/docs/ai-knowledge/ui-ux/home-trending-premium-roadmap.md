# HOME Tendances et previews Xtream

Derniere mise a jour: 2026-07-17.

## Etat actuel

- Continue watching, Tendances films et Tendances series utilisent les memes cards paysage 16:9 fixes; cette intervention ne modifie ni leur taille ni leur espacement horizontal.
- Le focus conserve la largeur et le ratio; la card passe de `1.04` a `1.2` uniquement apres la premiere image video.
- Les metadonnees preview sont preparees par fenetre visible avec concurrence bornee. Apres `550 ms` de focus stable, un unique `HomePreviewController` peut lire l'URL Xtream reconstruite en memoire; le focus seul ne lance aucune requete.
- `item.id` reste la cle Lazy et `item.imageUrl` l'unique URL d'image rendue par la card. Les backdrops caches sont hydrates avant emission; les cinq premieres Series sont preparees avec concurrence `2` avant retrait du skeleton. Un enrichissement ulterieur ne remplace jamais l'image uniquement au focus/recomposition.
- Apres la premiere frame, la surface video repose sur un fond noir plein format afin que le poster ne reste pas visible dans les zones transparentes ou non couvertes du mini-player. Avant la premiere frame, le poster conserve son role de transition.
- Une perte de focus, une autre preview, une sortie Home, un changement de profil ou `ON_STOP` arrete et libere le player.
- Les previews ne passent jamais par le lecteur principal et n'ecrivent ni progression, ni historique, ni date de derniere lecture.
- Films: lien Xtream du film avec extension locale.
- Series: premier episode Xtream disponible, stocke comme id/extension de sample sans URL brute.
- Live dans l'historique: flux Xtream/M3U existant, logo centre avec rendu `Fit`.
- YouTube/TMDB trailer n'est plus une source de preview Home.

## Tendances

- `HomeTrendingPolicy.SectionLimit = 10` centralise la taille par section.
- La selection est unique par identifiant de contenu et par URL de poster normalisee (casse, query et fragment ignores). Un doublon visuel est saute au profit du candidat suivant; une entree sans poster reste eligible. Ce controle s'applique aux Films et Series et reconstruit aussi les selections Room deja persistees lors de la prochaine lecture Home.
- La selection est calculee uniquement apres import Live/Films/Series pendant `CatalogRepository.synchronize(profileId)`.
- Films conservent la selection Room existante. Series normalise toute note sur `5`, forme un pool recent borne, exclut les notes inferieures a `3/5`, trie par note/date d'ajout/annee/id puis complete uniquement avec les meilleures Series globales a au moins `3/5`.
- Les doublons et marqueurs adultes sont retires avant persistance.
- `trending_media` et `home_trending_preview_cache` sont scopes par `profileId`.
- A la lecture Series, la selection bornee est comparee a `trending_media` et remplacee transactionnellement si elle est obsolete, sans changement de schema Room.
- Les cards affichent un titre sur une ligne, une note numerique avec etoile jaune separee et un badge haut gauche pour duree Film ou `xS xxE`; une duree nulle/invalide est masquee.

## Regles a ne pas casser

- Ne jamais stocker une URL Xtream credentialisee dans Room.
- Ne jamais creer un player par card.
- Ne jamais demarrer une requete uniquement a cause du focus; le prechargement borne des items visibles reste autorise.
- Ne jamais reutiliser les donnees ou players d'un autre profil.
- Ne pas reintroduire les routes ou boutons `View all` supprimes.

## Livraison 2026-07-13

- Historique et Tendances: espacement vertical commun `10dp`; dimensions et espacement horizontal des cards preserves.
- Tendances: URL de card stable avant/pendant/apres focus; la preparation preview ne change que les metadonnees et la video.
- Preview partagee: un controller, fondu audio 0-100 % sur deux secondes et boucle VOD Tendances 15/35/55/70/90 %, segments bornes a 25 secondes.
