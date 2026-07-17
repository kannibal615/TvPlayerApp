(() => {
    'use strict';

    const form = document.getElementById('playlist-form');
    if (!form) return;

    const codeInput = document.getElementById('playlist-device');
    const codeStatus = document.getElementById('playlist-code-status');
    const targets = document.getElementById('playlist-targets');
    const profileOptions = document.getElementById('playlist-profile-options');
    const targetError = document.getElementById('playlist-target-error');
    const createNew = document.getElementById('playlist-create-new');
    const newOption = document.getElementById('playlist-new-profile-option');
    const newHelp = document.getElementById('playlist-new-profile-help');
    const newNameWrap = document.getElementById('playlist-new-profile-name-wrap');
    const newName = document.getElementById('playlist-new-profile-name');
    const configType = document.getElementById('playlist-config-type');
    const submit = document.getElementById('playlist-submit');
    const selectedTargetIds = new Set(JSON.parse(form.dataset.selectedTargets || '[]'));
    const restoreCreateNew = form.dataset.createNew === '1';
    let eligibleProfiles = [];
    let codeReady = false;
    let validationRequest = null;

    const normalizedCode = (value) => value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6);
    const activeType = () => document.querySelector('[data-config-tab]:checked')?.value || 'xtream';

    function setCodeStatus(message, state = '') {
        codeStatus.textContent = message;
        codeStatus.className = `field-status${state ? ` is-${state}` : ''}`;
    }

    function profileSelectionCount() {
        return profileOptions.querySelectorAll('input[type="checkbox"]:checked').length + (createNew.checked ? 1 : 0);
    }

    function updateSubmitState() {
        const hasDestination = profileSelectionCount() > 0;
        const nameValid = !createNew.checked || newName.value.trim().length > 0;
        submit.disabled = !codeReady || !hasDestination || !nameValid;
        targetError.textContent = codeReady && !hasDestination ? 'Sélectionnez au moins un profil destinataire.' : '';
    }

    function updateNewProfileState() {
        const epgOnly = activeType() === 'epg';
        createNew.disabled = epgOnly;
        newOption.classList.toggle('is-disabled', epgOnly);
        newHelp.hidden = !epgOnly;
        if (epgOnly) createNew.checked = false;
        newNameWrap.hidden = !createNew.checked || epgOnly;
        newName.required = createNew.checked && !epgOnly;
        updateSubmitState();
    }

    function updatePanels() {
        const type = activeType();
        configType.value = type;
        document.querySelectorAll('[data-config-panel]').forEach((panel) => {
            const active = panel.dataset.configPanel === type;
            panel.querySelectorAll('input').forEach((input) => {
                input.disabled = !active;
                if (input.id === 'playlist-host' || input.id === 'playlist-username' || input.id === 'playlist-password' || input.id === 'playlist-m3u') {
                    input.required = active;
                }
            });
        });
        const epgInput = document.getElementById('playlist-epg');
        const clearEpg = form.querySelector('input[name="clear_epg"]');
        if (epgInput && clearEpg) epgInput.required = type === 'epg' && !clearEpg.checked;
        updateNewProfileState();
    }

    function renderProfiles(profiles) {
        eligibleProfiles = profiles;
        profileOptions.replaceChildren();
        profiles.forEach((profile) => {
            const label = document.createElement('label');
            label.className = 'playlist-profile-option';
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.name = 'target_profile_ids[]';
            checkbox.value = profile.id;
            checkbox.checked = selectedTargetIds.has(profile.id);
            checkbox.addEventListener('change', updateSubmitState);
            const avatar = document.createElement('span');
            avatar.className = 'playlist-profile-avatar';
            avatar.textContent = (profile.name || '?').trim().charAt(0).toUpperCase();
            const copy = document.createElement('span');
            const title = document.createElement('strong');
            title.textContent = profile.name;
            const type = document.createElement('small');
            type.textContent = profile.type === 'admin' ? 'Administrateur' : 'Profil normal';
            copy.append(title, type);
            label.append(checkbox, avatar, copy);
            profileOptions.append(label);
        });
        if (profiles.length === 0) {
            const empty = document.createElement('p');
            empty.className = 'playlist-inline-help';
            empty.textContent = 'Aucun profil existant éligible. Vous pouvez créer un nouveau profil.';
            profileOptions.append(empty);
        }
        createNew.checked = restoreCreateNew && activeType() !== 'epg';
        targets.hidden = false;
        updateNewProfileState();
    }

    async function validateCode() {
        const code = normalizedCode(codeInput.value);
        codeInput.value = code;
        validationRequest?.abort();
        validationRequest = null;
        codeReady = false;
        eligibleProfiles = [];
        profileOptions.replaceChildren();
        targets.hidden = true;
        updateSubmitState();
        if (code.length < 6) {
            setCodeStatus(code.length === 0 ? '' : `${code.length}/6`);
            return;
        }
        setCodeStatus('Vérification…', 'loading');
        validationRequest = new AbortController();
        try {
            const response = await fetch('/api/playlist_targets.php', {
                method: 'POST',
                headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
                body: JSON.stringify({device: code}),
                signal: validationRequest.signal,
            });
            const payload = await response.json();
            if (!response.ok || !payload.success || !payload.valid) {
                setCodeStatus(payload.error || 'Code TV invalide.', 'error');
                return;
            }
            if (!payload.ready) {
                setCodeStatus(payload.message || 'Ouvrez SmartVision sur la TV.', 'error');
                return;
            }
            codeReady = true;
            setCodeStatus('Code valide', 'success');
            renderProfiles(Array.isArray(payload.profiles) ? payload.profiles : []);
        } catch (error) {
            if (error.name !== 'AbortError') setCodeStatus('Vérification indisponible. Réessayez.', 'error');
        }
    }

    codeInput.addEventListener('input', validateCode);
    document.querySelectorAll('[data-config-tab]').forEach((tab) => tab.addEventListener('change', updatePanels));
    createNew.addEventListener('change', updateNewProfileState);
    newName.addEventListener('input', () => {
        const collision = eligibleProfiles.some((profile) => profile.name.localeCompare(newName.value.trim(), undefined, {sensitivity: 'accent'}) === 0);
        newName.setCustomValidity(collision ? 'Un profil porte déjà ce nom.' : '');
        updateSubmitState();
    });
    form.querySelector('input[name="clear_epg"]')?.addEventListener('change', updatePanels);
    form.addEventListener('submit', (event) => {
        updateSubmitState();
        if (submit.disabled || !form.checkValidity()) {
            event.preventDefault();
            form.reportValidity();
            return;
        }
        submit.disabled = true;
        submit.textContent = 'Envoi en cours…';
    });

    updatePanels();
    if (normalizedCode(codeInput.value).length === 6) validateCode();
})();

