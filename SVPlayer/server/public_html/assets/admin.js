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

    document.querySelectorAll('.notification-form').forEach((form) => {
        const scope = form.querySelector('[data-notification-scope]');
        const targets = form.querySelector('input[name="target_value"]');
        const syncTargets = () => {
            if (!scope || !targets) return;
            const isAll = scope.value === 'all';
            targets.disabled = isAll;
            targets.required = !isAll;
            if (isAll) targets.value = '';
        };
        scope?.addEventListener('change', syncTargets);
        syncTargets();
    });

    const closeModal = (modal) => {
        if (!modal) return;
        modal.hidden = true;
        document.body.classList.remove('admin-modal-open');
    };

    const openModal = (id) => {
        const modal = document.getElementById(id || '');
        if (!modal) return;
        modal.hidden = false;
        document.body.classList.add('admin-modal-open');
        modal.querySelector('[data-modal-close]')?.focus?.();
    };

    document.querySelectorAll('[data-modal-target]').forEach((row) => {
        row.addEventListener('click', (event) => {
            if (event.target.closest('a,button,input,select,textarea,form,label')) return;
            openModal(row.dataset.modalTarget || '');
        });
        row.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                openModal(row.dataset.modalTarget || '');
            }
        });
    });

    document.querySelectorAll('[data-modal-close]').forEach((button) => {
        button.addEventListener('click', () => closeModal(button.closest('.admin-modal')));
    });

    const activateModalTab = (button) => {
        const modal = button.closest('.admin-modal');
        const target = document.getElementById(button.dataset.tabTarget || '');
        if (!modal || !target) return;
        modal.querySelectorAll('[data-tab-target]').forEach((tab) => {
            const active = tab === button;
            tab.classList.toggle('active', active);
            tab.setAttribute('aria-selected', active ? 'true' : 'false');
        });
        modal.querySelectorAll('.admin-tab-panel').forEach((panel) => {
            const active = panel === target;
            panel.classList.toggle('active', active);
            panel.hidden = !active;
        });
    };

    document.addEventListener('click', (event) => {
        const button = event.target.closest('[data-tab-target]');
        if (!button) return;
        event.preventDefault();
        activateModalTab(button);
    });

    document.querySelectorAll('.admin-modal').forEach((modal) => {
        const activeButton = modal.querySelector('[data-tab-target].active') || modal.querySelector('[data-tab-target]');
        if (activeButton) {
            activateModalTab(activeButton);
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            document.querySelectorAll('.admin-modal:not([hidden])').forEach(closeModal);
        }
    });
})();
