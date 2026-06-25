(() => {
    document.querySelectorAll('[data-copy]').forEach((button) => {
        button.addEventListener('click', async () => {
            try {
                await navigator.clipboard.writeText(button.dataset.copy || '');
                const initial = button.textContent;
                button.textContent = 'Copie';
                window.setTimeout(() => { button.textContent = initial; }, 1600);
            } catch (_) {
                button.textContent = 'Copie impossible';
            }
        });
    });

})();
