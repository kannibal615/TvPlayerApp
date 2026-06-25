(() => {
    const button = document.querySelector('[data-site-menu-toggle]');
    const nav = document.querySelector('[data-site-menu]');
    if (!button || !nav) return;

    const setOpen = (open) => {
        button.setAttribute('aria-expanded', open ? 'true' : 'false');
        button.setAttribute('aria-label', open ? 'Fermer le menu' : 'Ouvrir le menu');
        button.classList.toggle('is-open', open);
        nav.classList.toggle('open', open);
        document.documentElement.classList.toggle('site-menu-open', open);
    };

    button.addEventListener('click', () => {
        setOpen(button.getAttribute('aria-expanded') !== 'true');
    });

    nav.querySelectorAll('a').forEach((link) => {
        link.addEventListener('click', () => setOpen(false));
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            setOpen(false);
        }
    });

    document.addEventListener('click', (event) => {
        if (!nav.classList.contains('open')) return;
        if (nav.contains(event.target) || button.contains(event.target)) return;
        setOpen(false);
    });
})();
