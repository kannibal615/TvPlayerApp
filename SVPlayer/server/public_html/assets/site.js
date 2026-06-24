(() => {
    const button = document.querySelector('[data-site-menu-toggle]');
    const nav = document.getElementById('site-nav');
    if (!button || !nav) return;

    button.addEventListener('click', () => {
        const expanded = button.getAttribute('aria-expanded') === 'true';
        button.setAttribute('aria-expanded', expanded ? 'false' : 'true');
        nav.classList.toggle('open', !expanded);
    });

    nav.querySelectorAll('a').forEach((link) => {
        link.addEventListener('click', () => {
            button.setAttribute('aria-expanded', 'false');
            nav.classList.remove('open');
        });
    });
})();
