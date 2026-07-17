# Decision: ciblage multi-profils depuis Playlist

Date: 2026-07-17

## Decision

La page publique Playlist cible les profils locaux par identifiant opaque. La TV publie au serveur uniquement les profils Admin et Normal sous la forme id, nom et type; aucun credential ni URL n'est publie. Kids est refuse aux deux frontieres.

Une livraison peut viser plusieurs profils existants et creer simultanement un profil Normal. Un profil partageant les credentials Admin devient `CUSTOM` en copiant localement la source resolue avant modification; l'Admin reste inchange. Le profil actif n'est jamais change par une livraison web.

Chaque payload chiffre porte un `config_id`. La TV memorise le dernier identifiant applique pour rendre les controles `device_status` repetes idempotents. EPG seul ne peut pas creer un nouveau profil, car il ne fournit aucune source jouable.

## Consequences

- Une version compatible doit avoir ete ouverte pour rendre son inventaire disponible sur le site.
- Les cibles supprimees ou devenues Kids entre validation et livraison sont ignorees sans fallback vers `PlaylistWeb`.
- Les versions anciennes conservent le flux legacy uniquement pour les payloads sans metadonnees de ciblage.
