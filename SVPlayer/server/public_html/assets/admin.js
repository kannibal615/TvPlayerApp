(() => {
    document.querySelectorAll('form[data-confirm]').forEach((form) => {
        form.addEventListener('submit', (event) => {
            if (!window.confirm(form.dataset.confirm || 'Confirmer cette action ?')) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll('[data-copy-target]').forEach((button) => {
        button.addEventListener('click', async () => {
            const target = document.getElementById(button.dataset.copyTarget || '');
            if (!target) return;
            try {
                await navigator.clipboard.writeText(target.textContent || '');
                button.textContent = 'Copie';
            } catch (_) {
                button.textContent = 'Copie impossible';
            }
        });
    });

    document.querySelectorAll('[data-license-generator]').forEach((form) => {
        const durationInput = form.querySelector('[data-duration-days]');
        const validUntilInput = form.querySelector('[data-valid-until]');
        const syncValidUntil = () => {
            if (!durationInput || !validUntilInput) return;
            const days = Number.parseInt(durationInput.value || '0', 10);
            if (!Number.isFinite(days) || days < 1) return;
            const date = new Date();
            date.setDate(date.getDate() + days);
            validUntilInput.value = date.toISOString().slice(0, 10);
        };
        durationInput?.addEventListener('input', syncValidUntil);
    });
})();
