# HOME Tendances et previews Xtream

Derniere mise a jour: 2026-07-13.

## Etat actuel

- Continue watching, Tendances films et Tendances series utilisent les memes cards paysage 16:9 fixes; cette intervention ne modifie ni leur taille ni leur espacement horizontal.
- Le focus applique un scale `1.04` au conteneur complet sans changement de largeur ni de ratio.
- Les metadonnees preview sont preparees par fenetre visible avec concurrence bornee. Apres `550 ms` de focus stable, un unique `HomePreviewController` peut lire l'URL Xtream reconstruite en memoire; le focus seul ne lance aucune requete.
- `item.id` reste la cle Lazy et `item.imageUrl` l'unique URL d'image de card. Un backdrop de preview peut etre mis en cache mais ne remplace jamais l'image au focus/recomposition.
- Une perte de focus, une autre preview, une sortie Home, un changement de profil ou `ON_STOP` arrete et libere le player.
- Les previews ne passent jamais par le lecteur principal et n'ecrivent ni progression, ni historique, ni date de derniere lecture.
- Films: lien Xtream du film avec extension locale.
- Series: premier episode Xtream disponible, stocke comme id/extension de sample sans URL brute.
- Live dans l'historique: flux Xtream/M3U existant, logo centre avec rendu `Fit`.
- YouTube/TMDB trailer n'est plus une source de preview Home.

## Tendances

- `HomeTrendingPolicy.SectionLimit = 10` centralise la taille par section.
- La selection est calculee uniquement apres import Live/Films/Series pendant `CatalogRepository.synchronize(profileId)`.
- Priorite: note valide decroissante, date d'ajout, annee, id stable; fallback vers categories nouveautes normalisees, sinon date d'ajout.
- Les doublons et marqueurs adultes sont retires avant persistance.
- `trending_media` et `home_trending_preview_cache` sont scopes par `profileId`.
- Home relit la selection persistante et ne la recalcule pas lors d'une ouverture, recomposition ou action du header.

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
