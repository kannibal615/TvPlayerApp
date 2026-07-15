# SmartVision Android TV audit - Preflight only

- Date locale: 2026-07-15 (Europe/Paris)
- Scope: preflight uniquement; aucune navigation, saisie de PIN, installation/desinstallation APK, reinitialisation ou modification de donnees.
- Projet: `SVPlayer`, branche `main`, commit `fa6a2813dc27eec4716739eeb7f83625d617a34a`

## Appareil et ADB

- ADB: connecte et autorise, etat `device`, serial `192.168.1.33:5555`
- Appareil: Amazon AFTSSS (`sheldonp`)
- OS: Fire OS 7.7.1.4 / Android 9 / API 28
- Affichage: 1920 x 1080, densite 320

## Application et alignement source

- Module Android: `app`
- Package source et installe: `com.smartvision.svplayer`
- Activite launcher source et resolue: `com.smartvision.svplayer/.MainActivity`
- Version source: `0.1.120` (`versionCode 168`)
- Version installee: `0.1.120` (`versionCode 168`)
- Resultat: correspondance exacte versionName/versionCode.
- Variante installee: famille release non debuggable et sans suffixe `-diag`; le buildType exact (`release`, `releaseFast` ou `releaseOptimized`) n'est pas distinguable depuis les seules metadonnees installees.

## Outils de mesure

| Outil | Etat preflight | Limite |
|---|---|---|
| `dumpsys gfxinfo` | Disponible; la sonde package retourne des donnees | Pas de capture de parcours ni de reset framestats pendant ce preflight |
| Perfetto | Disponible et executable dans `/system/bin/perfetto` | Aucune trace capturee pendant ce preflight |
| Simpleperf | Indisponible sur l'appareil (`/system/bin/simpleperf` absent) | Le package installe est aussi non debuggable; le profil CPU applicatif requerrait un build compatible installe lors d'une phase ulterieure autorisee |
| `dumpsys meminfo` | Disponible; la sonde package retourne des donnees | Un seul instantane preflight, sans boucle de navigation ni analyse de croissance |

## Inventaire source des ecrans et parcours

Tous les elements sont marques `not reached` conformement au scope preflight.

| Domaine | Ecrans / parcours trouves dans le code | Couverture runtime |
|---|---|---|
| Demarrage | Splash/loading -> consentement si requis -> activation/essai/free-with-ads -> configuration Xtream si aucune source -> selecteur de profil -> Home | not reached |
| Navigation principale | Home, Live TV, Movies, Series, Media, YouTube | not reached |
| Actions globales | Info compte/Profile, Settings, Notifications; raccourci telecommande Settings/Menu | not reached |
| Home | Hero, cartes Live/Movies/Series, Continue Watching, tendances films et series, etats de synchronisation | not reached |
| Live TV | Categories/dossiers, chaines, preview/mini-player, EPG, lecteur live et Recorder | not reached |
| Movies | Categories/liste/preview, fiche film, lecteur film | not reached |
| Series | Categories/liste/preview, fiche serie/saisons/episodes, lecteur episode | not reached |
| Media Center | Bibliotheque locale, import/export telephone, detail media prive, lecteurs local/prive, transfert/renommage/deplacement/suppression | not reached |
| YouTube | Recherche/suggestions/favoris, parametres, player | not reached |
| Profile | Info compte, controle parental, synchronisation, historique, aide, raccourci Parametres, edition/selection de profils | not reached |
| Settings | Licence, preferences, activite reseau, attribution TMDB, personnalisation, mises a jour, donnees locales | not reached |
| Notifications | Toutes, mise a jour, playlist, information importante, historique, detail notification | not reached |
| Routes profondes | `player/{channelId}`, `movie_detail/{movieId}`, `movie_player/{movieId}`, `series_detail/{seriesId}`, `episode_player/{episodeId}`, `media_player/{mediaFileId}`, private media detail/player | not reached |
| Etats transverses | Dialogues sortie, licence QR, connexion/edition Xtream, update, PIN parental, consentement; placeholders et gates Kids/Premium/Xtream | not reached |

## Artefacts

- `device.json`: metadonnees ADB/appareil/package verifiees
- `package.txt`: dump package installe
- `launcher-activity.txt`: activite launcher resolue
- `activity-preflight.txt`: etat ActivityManager en lecture seule
- `window-preflight.txt`: etat WindowManager en lecture seule

## Limites et blocages

1. Simpleperf est absent du Firestick. Aucun profil CPU Simpleperf ne sera possible avec cette configuration installee.
2. Le helper fourni utilisait `sh -c` d'une facon incompatible avec le passage d'arguments Windows/Fire OS. Il a d'abord produit des champs errones; ces fichiers ont ete supprimes et regeneres avec le meme module et une adaptation temporaire en memoire. Aucun fichier de la competence ou du projet n'a ete modifie.
3. `docs/ai-knowledge/ROOT.md` annonce encore `versionCode 151`, alors que `app/build.gradle.kts` et l'installation sont en `168`. Cette derive documentaire n'a pas ete corrigee a cause de la contrainte de lecture seule.

