<?php
declare(strict_types=1);

$legalPage = [
    'title' => 'Contact — SmartVision',
    'description' => 'Contacter SmartVision pour activation, licence ou assistance technique.',
    'heading' => 'Contact',
    'intro' => 'Pour toute question concernant SmartVision, vous pouvez nous contacter.',
    'sections' => [
        [
            'title' => 'Nous contacter',
            'paragraphs' => [
                'Email principal : <a class="text-link" href="mailto:contact%40app.smartvisions.net"><span>contact</span><span>@</span><span>app.smartvisions.net</span></a>',
                'Support : <a class="text-link" href="mailto:support%40app.smartvisions.net"><span>support</span><span>@</span><span>app.smartvisions.net</span></a>',
            ],
        ],
        [
            'title' => 'Sujets recommandés',
            'items' => [
                'Activation',
                'Licence',
                'Support technique',
                'Confidentialité',
                'Signalement d’usage abusif',
            ],
        ],
        [
            'title' => 'Formulaire',
            'paragraphs' => [
                'Aucun formulaire de contact n’est actuellement publié sur ce site. Utilisez les liens email ci-dessus pour contacter SmartVision.',
                '<!-- TODO: intégrer un backend email avant d’ajouter un formulaire fonctionnel. -->',
            ],
        ],
    ],
];

require dirname(__DIR__) . '/legal_page.php';
