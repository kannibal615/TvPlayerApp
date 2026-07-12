# HOME Tendances et previews Xtream

Derniere mise a jour: 2026-07-12.

## Etat actuel

- Continue watching, Tendances films et Tendances series utilisent des cards paysage 16:9 de `220,4 x 124 dp` des le premier rendu, afin d'en afficher au moins cinq sur une ligne en 1280 px.
- Le focus applique un scale `1.04` au conteneur complet sans changement de largeur ni de ratio.
- Apres `550 ms` de focus stable, un unique `HomePreviewController` cree un player Media3 muet et lit uniquement une URL Xtream reconstruite en memoire.
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
- Ne jamais demarrer une preparation reseau avant le delai de focus.
- Ne jamais reutiliser les donnees ou players d'un autre profil.
- Ne pas reintroduire les routes ou boutons `View all` supprimes.
