(() => {
    const planInputs = [...document.querySelectorAll('.account-plan input[type="radio"]')];
    const summaryPlan = document.getElementById('summary-plan');
    const summaryPrice = document.getElementById('summary-price');

    const updatePlan = (input) => {
        document.querySelectorAll('.account-plan').forEach((plan) => {
            plan.classList.toggle('selected', plan.contains(input));
        });
        if (summaryPlan) summaryPlan.textContent = input.dataset.planLabel || '';
        if (summaryPrice) summaryPrice.textContent = input.dataset.planPrice || '';
        const url = new URL(window.location.href);
        url.searchParams.set('plan', input.value);
        history.replaceState(null, '', url);
    };

    planInputs.forEach((input) => input.addEventListener('change', () => updatePlan(input)));

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

    document.getElementById('checkout-form')?.addEventListener('submit', (event) => {
        const button = event.submitter;
        if (!button || button.value !== 'test_payment') return;
        button.disabled = true;
        button.textContent = 'Validation en cours...';
    });
})();
