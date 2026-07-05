# Decision: Gate premium central pour Recorder et Media Center

Date: 2026-07-05.

## Contexte

Recorder et Media Center doivent etre reutilisables sur plusieurs surfaces: header Media, bouton Record, ecran Media, actions fichiers, transfert telephone/TV et futures sources compatibles. Les regles Premium, essai actif, Free avec pubs, licence expiree et source non supportee doivent rester coherentes avec les feature flags existants.

## Decision

Ajouter `domain/access/PremiumFeatureGate.kt` comme point de decision central pour les futures surfaces Recorder/Media.

Le gate:
- lit `AppRuntimeConfig.features`;
- consomme `MonetizationStatus` deja resolu par la logique monetisation existante;
- retourne un resultat typable par feature;
- distingue `Allowed`, `LockedPremiumVisible`, `BlockedExpired`, `SourceUnsupported` et `ConfigDisabled`;
- conserve des defaults locaux pour les nouvelles cles si la config distante n'est pas encore a jour.

Les nouvelles cles feature sont:
- `recorder`;
- `media_center`;
- `media_file_management`;
- `media_phone_transfer`.

Defaults:
- Premium: oui;
- Trial: oui;
- Free Ads: non.

## Raisons

- Eviter de dupliquer la logique d'acces Premium dans chaque ecran.
- Garder Free Ads affichable mais verrouille avec couronne/popup.
- Bloquer les licences et essais expires sans supprimer les fichiers locaux.
- Permettre a l'admin/API de couper une surface sans modifier le code Android.
- Prepararer les lots suivants sans brancher prematurement l'UI Media ou le service Recorder.

## Consequences

- Les prochains lots doivent consommer `PremiumFeatureGateResult` au lieu de recalculer l'acces.
- Les headers, boutons et popups doivent mapper `LockedPremiumVisible` vers une UI grisee avec couronne.
- `SourceUnsupported` doit etre utilise pour YouTube et les sources non enregistrables.
- Aucun enregistrement ni fichier local ne doit etre supprime a cause d'un statut expire.
