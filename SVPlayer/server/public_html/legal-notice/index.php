<?php
declare(strict_types=1);

$legalPage = [
    'title' => 'Mentions légales — SmartVision',
    'description' => 'Mentions légales du site SmartVision.',
    'heading' => 'Mentions légales',
    'intro' => 'Informations légales relatives au site officiel SmartVision.',
    'sections' => [
        [
            'title' => 'Site',
            'items' => [
                'Nom du site : SmartVision',
                'Domaine : app.smartvisions.net',
                'Pays : France',
            ],
        ],
        [
            'title' => 'Éditeur',
            'items' => [
                'Éditeur : smartvisions',
                'Responsable de publication : Alain COLIN',
                'Email principal : <a class="text-link" href="mailto:contact%40app.smartvisions.net"><span>contact</span><span>@</span><span>app.smartvisions.net</span></a>',
                'Email support : <a class="text-link" href="mailto:support%40app.smartvisions.net"><span>support</span><span>@</span><span>app.smartvisions.net</span></a>',
            ],
        ],
        [
            'title' => 'Hébergement',
            'items' => [
                'Hébergeur : hostcreed',
                'Adresse de l’hébergeur : 10 Majaro Street, Onike, 100213, Lagos, Nigeria',
            ],
        ],
    ],
];

require dirname(__DIR__) . '/legal_page.php';
