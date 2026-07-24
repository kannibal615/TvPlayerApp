# Decision - Visibilite admin controlee des identifiants Xtream

Date: 2026-07-24

## Decision

La TV publie en HTTPS un inventaire de profils v2 authentifie par `device_id` et `device_token`. Les identifiants partages sont resolus localement avant l'envoi. Le serveur chiffre un payload par profil dans `device_playlist_profiles`.

Les host, username, password et EPG en texte integral ne sont dechiffres que pendant le rendu d'une session admin authentifiee. La reponse admin impose `Cache-Control: no-store`. Aucun endpoint public, log applicatif ou compte rendu de livraison ne doit contenir ces valeurs.

Les profils Kids sont synchronises pour l'inventaire admin et le calcul du statut appareil, mais restent exclus des cibles publiques PlaylistWeb.

## Raisons

- aligner l'etat admin avec les comptes reellement presents sur la TV;
- permettre le support appareil sans exposer les secrets sur une API publique;
- conserver la compatibilite des APK v1 sans effacer les payloads v2 existants.

## Consequences

- `DeviceProfilesRequest.capability_version = 2`;
- migration SQL additive et relancable;
- toute nouvelle vue des secrets exige authentification admin et reponse `no-store`;
- les logs doivent rester limites aux identifiants techniques non sensibles.
