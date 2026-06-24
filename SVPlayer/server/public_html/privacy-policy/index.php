<?php
declare(strict_types=1);

$legalPage = [
    'title' => 'Politique de confidentialité — SmartVision',
    'description' => 'Politique de confidentialité de SmartVision, lecteur IPTV Android TV.',
    'heading' => 'Politique de confidentialité',
    'intro' => 'SmartVision respecte la confidentialité de ses utilisateurs et limite les traitements de données aux besoins liés au fonctionnement, à la sécurité et à l’amélioration de l’application.',
    'sections' => [
        [
            'title' => 'Introduction',
            'paragraphs' => [
                'La présente politique explique quelles données peuvent être traitées lors de l’utilisation de SmartVision, lecteur IPTV Android TV. SmartVision ne fournit aucun contenu audiovisuel et fonctionne avec les identifiants ou playlists autorisées ajoutés par l’utilisateur.',
            ],
        ],
        [
            'title' => 'Données collectées',
            'paragraphs' => [
                'L’application peut collecter certaines données techniques strictement nécessaires au fonctionnement du service.',
            ],
            'items' => [
                'identifiant technique de l’appareil',
                'statut d’activation',
                'statut de licence',
                'statut d’essai',
                'informations nécessaires au bon fonctionnement de l’application',
                'logs techniques',
                'données liées aux publicités si la version avec publicités est utilisée',
            ],
        ],
        [
            'title' => 'Utilisation des données',
            'paragraphs' => [
                'Ces données peuvent être utilisées pour assurer les fonctions essentielles de SmartVision.',
            ],
            'items' => [
                'activer une licence',
                'gérer l’essai',
                'sécuriser le service',
                'améliorer la stabilité de l’application',
                'afficher des publicités vidéo dans la version avec publicités',
            ],
        ],
        [
            'title' => 'Publicités',
            'paragraphs' => [
                'La version avec publicités peut afficher des publicités vidéo raisonnables uniquement dans le lecteur vidéo. Aucune publicité ne doit être affichée dans les menus, la page d’accueil ou les paramètres de l’application.',
                'Des partenaires publicitaires peuvent traiter certaines données techniques selon leurs propres règles de confidentialité.',
            ],
        ],
        [
            'title' => 'Données IPTV',
            'paragraphs' => [
                'Les identifiants Xtream Codes API ou playlists personnelles sont fournis par l’utilisateur. SmartVision ne fournit aucun contenu audiovisuel, aucun flux vidéo et aucun service de diffusion propriétaire.',
            ],
        ],
        [
            'title' => 'Partage des données',
            'paragraphs' => [
                'SmartVision ne vend pas les données personnelles des utilisateurs. Certaines données techniques peuvent être transmises à des prestataires uniquement lorsque cela est nécessaire au fonctionnement, à la sécurité, au support ou à l’affichage des publicités dans la version concernée.',
            ],
        ],
        [
            'title' => 'Droits de l’utilisateur',
            'paragraphs' => [
                'L’utilisateur peut demander des informations sur les données associées à son utilisation de SmartVision ou contacter le support pour toute question liée à la confidentialité.',
            ],
        ],
        [
            'title' => 'Contact',
            'paragraphs' => [
                'Email principal : <a class="text-link" href="mailto:contact%40smartvisions.net"><span>contact</span><span>@</span><span>smartvisions.net</span></a>',
                'Support : <a class="text-link" href="mailto:support%40smartvisions.net"><span>support</span><span>@</span><span>smartvisions.net</span></a>',
            ],
        ],
        [
            'title' => 'Mise à jour',
            'paragraphs' => [
                'Cette politique peut être mise à jour pour refléter les évolutions de l’application ou des obligations légales.',
            ],
        ],
    ],
];

require dirname(__DIR__) . '/legal_page.php';
