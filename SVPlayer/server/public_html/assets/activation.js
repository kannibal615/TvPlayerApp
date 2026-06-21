(() => {
    const deviceId = document.getElementById('device-id')?.value || new URLSearchParams(location.search).get('device_id') || '';
    const shortCode = document.getElementById('short-code')?.value || new URLSearchParams(location.search).get('code') || '';
    const activationStep = document.getElementById('activation-step');
    const playlistStep = document.getElementById('playlist-step');
    const successStep = document.getElementById('success-step');

    const request = async (url, payload) => {
        const response = await fetch(url, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
        const data = await response.json().catch(() => ({}));
        if (!response.ok || data.success !== true) throw new Error(data.error || 'Opération impossible.');
        return data;
    };

    const showPlaylist = () => {
        activationStep.hidden = true;
        playlistStep.hidden = false;
        document.getElementById('xtream-host')?.focus();
    };

    document.getElementById('license-form')?.addEventListener('submit', async (event) => {
        event.preventDefault();
        const message = document.getElementById('activation-message');
        const button = document.getElementById('license-button');
        const activationCode = document.getElementById('activation-code').value.trim();
        if (activationCode.length < 6) { message.textContent = 'Saisissez un code SmartVision valide.'; return; }
        button.disabled = true; message.textContent = '';
        try {
            await request('/api/validate_activation.php', {device_id: deviceId, short_code: shortCode, activation_code: activationCode});
            showPlaylist();
        } catch (error) { message.textContent = error.message; button.disabled = false; }
    });

    document.getElementById('trial-button')?.addEventListener('click', async (event) => {
        const button = event.currentTarget;
        const message = document.getElementById('activation-message');
        button.disabled = true; message.textContent = '';
        try {
            await request('/api/start_trial.php', {device_id: deviceId, short_code: shortCode});
            showPlaylist();
        } catch (error) { message.textContent = error.message; button.disabled = false; }
    });

    document.getElementById('playlist-form')?.addEventListener('submit', async (event) => {
        event.preventDefault();
        const button = document.getElementById('playlist-button');
        const message = document.getElementById('playlist-message');
        const payload = {
            device_id: deviceId, short_code: shortCode,
            host: document.getElementById('xtream-host').value.trim(),
            username: document.getElementById('xtream-username').value.trim(),
            password: document.getElementById('xtream-password').value,
        };
        if (!payload.host || !payload.username || !payload.password) { message.textContent = 'Complétez les trois champs Xtream.'; return; }
        button.disabled = true; message.textContent = '';
        try {
            await request('/api/save_playlist_config.php', payload);
            playlistStep.hidden = true; successStep.hidden = false;
        } catch (error) { message.textContent = error.message; button.disabled = false; }
    });

    document.getElementById('skip-button')?.addEventListener('click', () => {
        playlistStep.hidden = true;
        successStep.hidden = false;
    });
})();
