# Decision - Cycle de vie des notifications

Date: 2026-07-14

## Decision

Une notification ciblee est non lue jusqu'a son ouverture sur la TV. L'ouverture cree ou met a jour son recu avec `seen_at`; elle quitte les vues non lues et reste dans Historique, meme apres expiration de la source.

La purge admin marque les recus vus avec `purged_at` au lieu de les supprimer. Ce tombstone garantit qu'une notification purgee ne reapparait jamais comme non lue. Les recus non lus ne sont pas touches.

Les details sensibles `playlist_added` restent chiffres dans `payload_ciphertext` et ne sont dechiffres que pour un `device_token` valide correspondant a l'appareil cible.

## Consequences

- `app_update` reste actionne par le flux canonique `app_update.php` et les versions deja installees sont masquees.
- Les anciennes APK conservent la reponse minimale historique.
- Une suppression individuelle de la source supprime egalement ses recus pour eviter les historiques orphelins.
