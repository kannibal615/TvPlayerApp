package com.smartvision.svplayer.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.core.config.XtreamAccount
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import com.smartvision.svplayer.ui.update.AppUpdateUiState
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    updateState: AppUpdateUiState,
    onCheckForUpdate: () -> Unit,
    onSyncCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val accounts by container.accountManager.accounts.collectAsStateWithLifecycle()
    val activeAccountId by container.accountManager.activeAccountId.collectAsStateWithLifecycle()
    val activeAccount = accounts.firstOrNull { it.id == activeAccountId } ?: accounts.firstOrNull()
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = com.smartvision.svplayer.domain.model.PlayerSettings(),
    )
    val scope = rememberCoroutineScope()
    var selectedSection by remember { mutableStateOf(SettingsSection.Preferences) }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        SmartVisionColors.PrimaryDark.copy(alpha = 0.4f),
                        SmartVisionColors.Background,
                        Color(0xFF01040C),
                    ),
                    radius = 1500f,
                ),
            )
            .padding(horizontal = 34.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TvButton(
                text = "Retour",
                leadingIcon = Icons.Default.ArrowBack,
                onClick = onBack,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier.height(42.dp),
            )
            Spacer(Modifier.width(20.dp))
            Text(
                text = "Parametres",
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleL,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(22.dp))

        SettingsMenuLayout(
            selectedSection = selectedSection,
            onSectionSelected = { selectedSection = it },
            settings = settings,
            accountsCount = accounts.size,
            activeAccount = activeAccount,
            updateState = updateState,
            onCheckForUpdate = onCheckForUpdate,
            onSyncCatalog = onSyncCatalog,
            onSetLanguage = { value -> scope.launch { container.settingsRepository.setLanguage(value) } },
            onSetSyncFrequency = { value -> scope.launch { container.settingsRepository.setSyncFrequency(value) } },
            onSetVideoRatio = { value -> scope.launch { container.settingsRepository.setVideoRatio(value) } },
            onSetAnimations = { value -> scope.launch { container.settingsRepository.setAnimationsEnabled(value) } },
            onSetRetry = { value -> scope.launch { container.settingsRepository.setRetryEnabled(value) } },
            onSetParentalEnabled = { value -> scope.launch { container.settingsRepository.setParentalControlEnabled(value) } },
            onSetParentalPin = { value -> scope.launch { container.settingsRepository.setParentalPin(value) } },
            onSetParentalKeywords = { value -> scope.launch { container.settingsRepository.setParentalKeywords(value) } },
            onClearLocalData = { scope.launch { container.settingsRepository.clearLocalData() } },
            modifier = Modifier.fillMaxSize(),
        )
        return@Column

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SettingsPanel(
                title = "Preferences generales",
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
            ) {
                SettingsChoice(
                    label = "Langue",
                    values = listOf("Francais", "English", "Espanol", "العربية"),
                    selected = settings.language,
                    onSelected = { value -> scope.launch { container.settingsRepository.setLanguage(value) } },
                )
                SettingsChoice(
                    label = "Synchronisation automatique",
                    values = listOf("24h", "48h", "A chaque demarrage", "Manuelle", "Jamais"),
                    selected = settings.syncFrequency,
                    onSelected = { value -> scope.launch { container.settingsRepository.setSyncFrequency(value) } },
                )
                SettingsChoice(
                    label = "Format video",
                    values = listOf("Fit", "Fill", "Zoom"),
                    selected = settings.videoRatio,
                    onSelected = { scope.launch { container.settingsRepository.setVideoRatio(it) } },
                )
                SettingsChoice(
                    label = "Animations",
                    values = listOf("Activees", "Reduites"),
                    selected = if (settings.animationsEnabled) "Activees" else "Reduites",
                    onSelected = { value -> scope.launch { container.settingsRepository.setAnimationsEnabled(value == "Activees") } },
                )
                SettingsChoice(
                    label = "Reconnexion automatique",
                    values = listOf("Activee", "Desactivee"),
                    selected = if (settings.retryEnabled) "Activee" else "Desactivee",
                    onSelected = { value -> scope.launch { container.settingsRepository.setRetryEnabled(value == "Activee") } },
                )
                SettingsInfoRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                SettingsInfoRow("Compte Xtream actif", activeAccount?.name ?: "Aucun")
                SettingsInfoRow("Serveur actif", activeAccount?.host ?: "Non configure")
                Spacer(Modifier.height(12.dp))
                TvButton(
                    text = if (updateState.checking) "Recherche..." else "Chercher une mise a jour",
                    leadingIcon = Icons.Default.Refresh,
                    onClick = onCheckForUpdate,
                    enabled = !updateState.checking && !updateState.installing,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                )
                updateState.errorMessage?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        color = SmartVisionColors.Error,
                        style = SmartVisionType.Caption,
                        maxLines = 2,
                    )
                }
                if (updateState.checkedOnce && updateState.update == null && updateState.errorMessage == null && !updateState.checking) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Application a jour.",
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Caption,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(18.dp))
                TvButton(
                    text = "Vider les donnees locales",
                    leadingIcon = Icons.Default.Refresh,
                    onClick = { scope.launch { container.settingsRepository.clearLocalData() } },
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                )
            }

            SettingsPanel(
                title = "Maintenance et donnees",
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
            ) {
                SettingsInfoRow("Frequence actuelle", settings.syncFrequency)
                SettingsInfoRow("Compte actif", activeAccount?.let { "${it.name} - ${it.username}" } ?: "Aucun")
                SettingsInfoRow("Mode buffer", settings.bufferMode)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Les identifiants Xtream se gerent maintenant depuis Compte utilisateur afin de separer les reglages de l'application et les donnees client.",
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Body,
                )
                Spacer(Modifier.height(16.dp))
                TvButton(
                    text = "Verifier les mises a jour",
                    leadingIcon = Icons.Default.Refresh,
                    onClick = onCheckForUpdate,
                    enabled = !updateState.checking && !updateState.installing,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                )
                Spacer(Modifier.height(10.dp))
                TvButton(
                    text = "Vider les donnees locales",
                    leadingIcon = Icons.Default.Delete,
                    onClick = { scope.launch { container.settingsRepository.clearLocalData() } },
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuLayout(
    selectedSection: SettingsSection,
    onSectionSelected: (SettingsSection) -> Unit,
    settings: PlayerSettings,
    accountsCount: Int,
    activeAccount: XtreamAccount?,
    updateState: AppUpdateUiState,
    onCheckForUpdate: () -> Unit,
    onSyncCatalog: () -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetSyncFrequency: (String) -> Unit,
    onSetVideoRatio: (String) -> Unit,
    onSetAnimations: (Boolean) -> Unit,
    onSetRetry: (Boolean) -> Unit,
    onSetParentalEnabled: (Boolean) -> Unit,
    onSetParentalPin: (String) -> Unit,
    onSetParentalKeywords: (String) -> Unit,
    onClearLocalData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(
            modifier = Modifier
                .width(292.dp)
                .fillMaxHeight()
                .background(Color(0xD9091424), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsSection.entries.forEach { section ->
                TvButton(
                    text = section.label,
                    leadingIcon = section.icon,
                    selected = selectedSection == section,
                    variant = if (selectedSection == section) TvButtonVariant.Primary else TvButtonVariant.Text,
                    onClick = { onSectionSelected(section) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                )
            }
        }

        SettingsPanel(
            title = selectedSection.label,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            when (selectedSection) {
                SettingsSection.Preferences -> {
                    SettingsChoice(
                        label = "Langue",
                        values = listOf("English", "Francais"),
                        selected = settings.language,
                        onSelected = onSetLanguage,
                    )
                    SettingsChoice(
                        label = "Format video",
                        values = listOf("Fit", "Fill", "Zoom"),
                        selected = settings.videoRatio,
                        onSelected = onSetVideoRatio,
                    )
                    SettingsChoice(
                        label = "Animations",
                        values = listOf("Activees", "Reduites"),
                        selected = if (settings.animationsEnabled) "Activees" else "Reduites",
                        onSelected = { value -> onSetAnimations(value == "Activees") },
                    )
                    SettingsChoice(
                        label = "Reconnexion automatique",
                        values = listOf("Activee", "Desactivee"),
                        selected = if (settings.retryEnabled) "Activee" else "Desactivee",
                        onSelected = { value -> onSetRetry(value == "Activee") },
                    )
                }
                SettingsSection.Sync -> {
                    SettingsChoice(
                        label = "Synchronisation automatique",
                        values = listOf("24h", "48h", "A chaque demarrage", "Manuelle", "Jamais"),
                        selected = settings.syncFrequency,
                        onSelected = onSetSyncFrequency,
                    )
                    SettingsInfoRow("Frequence actuelle", settings.syncFrequency)
                    SettingsInfoRow("Compte actif", activeAccount?.let { "${it.name} - ${it.username}" } ?: "Aucun")
                    SettingsInfoRow("Serveur actif", activeAccount?.host ?: "Non configure")
                }
                SettingsSection.Updates -> {
                    SettingsInfoRow("Version installee", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    SettingsInfoRow("Portail", BuildConfig.ACTIVATION_BASE_URL.removeSuffix("/"))
                    Spacer(Modifier.height(14.dp))
                    TvButton(
                        text = if (updateState.checking) "Recherche..." else "Chercher une mise a jour",
                        leadingIcon = Icons.Default.Refresh,
                        onClick = onCheckForUpdate,
                        enabled = !updateState.checking && !updateState.installing,
                        variant = TvButtonVariant.Secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                    )
                    updateState.errorMessage?.let { message ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = message,
                            color = SmartVisionColors.Error,
                            style = SmartVisionType.Caption,
                            maxLines = 2,
                        )
                    }
                    if (updateState.checkedOnce && updateState.update == null && updateState.errorMessage == null && !updateState.checking) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Application a jour.",
                            color = SmartVisionColors.TextSecondary,
                            style = SmartVisionType.Caption,
                            maxLines = 1,
                        )
                    }
                }
                SettingsSection.Data -> {
                    SettingsInfoRow("Mode buffer", settings.bufferMode)
                    SettingsInfoRow("Comptes Xtream locaux", accountsCount.toString())
                    SettingsInfoRow("Compte actif", activeAccount?.name ?: "Aucun")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Les identifiants Xtream se gerent depuis Profil client pour separer les reglages de l'application et les donnees client.",
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Body,
                    )
                    Spacer(Modifier.height(16.dp))
                    TvButton(
                        text = "Vider les donnees locales",
                        leadingIcon = Icons.Default.Delete,
                        onClick = onClearLocalData,
                        variant = TvButtonVariant.Secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                    )
                }
                SettingsSection.Parental -> {
                    val pinFocusRequester = remember { FocusRequester() }
                    val keywordsFocusRequester = remember { FocusRequester() }
                    SettingsChoice(
                        label = "Controle parental",
                        values = listOf("Active", "Desactive"),
                        selected = if (settings.parentalControlEnabled) "Active" else "Desactive",
                        onSelected = { value -> onSetParentalEnabled(value == "Active") },
                    )
                    SettingsTextField(
                        label = "Code PIN",
                        value = settings.parentalPin,
                        onValueChange = onSetParentalPin,
                        focusRequester = pinFocusRequester,
                        nextFocusRequester = keywordsFocusRequester,
                        password = true,
                    )
                    SettingsTextField(
                        label = "Mots cles masques",
                        value = settings.parentalKeywords,
                        onValueChange = onSetParentalKeywords,
                        focusRequester = keywordsFocusRequester,
                        previousFocusRequester = pinFocusRequester,
                    )
                    Text(
                        text = "Separez les mots par virgule, point-virgule ou retour a la ligne. Le filtrage s applique aux titres, descriptions et categories Live, Films et Series.",
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Caption,
                    )
                }
            }
        }
    }
}

private enum class SettingsSection(
    val label: String,
    val icon: ImageVector,
) {
    Preferences("Preferences generales", Icons.Default.Settings),
    Sync("Synchronisation", Icons.Default.CloudSync),
    Updates("Mises a jour", Icons.Default.Refresh),
    Parental("Controle parental", Icons.Default.Person),
    Data("Donnees locales", Icons.Default.Delete),
}

@Composable
private fun SettingsPanel(
    title: String,
    modifier: Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(Color(0xE60A1424), RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.height(18.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            content()
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingsChoice(
    label: String,
    values: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Label)
    Spacer(Modifier.height(7.dp))
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        values.chunked(3).forEach { rowValues ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                rowValues.forEach { value ->
                    TvButton(
                        text = value,
                        onClick = { onSelected(value) },
                        selected = value == selected,
                        variant = if (value == selected) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                    )
                }
                repeat(3 - rowValues.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
        Spacer(Modifier.weight(1f))
        Text(value, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Caption, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AccountRow(
    account: XtreamAccount,
    active: Boolean,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(if (active) SmartVisionColors.PrimaryDark.copy(alpha = 0.5f) else SmartVisionColors.Surface, RoundedCornerShape(7.dp))
            .border(BorderStroke(1.dp, if (active) SmartVisionColors.Primary else SmartVisionColors.Border), RoundedCornerShape(7.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (active) Icons.Default.CheckCircle else Icons.Default.Person,
            contentDescription = null,
            tint = if (active) SmartVisionColors.CyanAccent else SmartVisionColors.TextSecondary,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(account.name, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Label, fontWeight = FontWeight.Bold)
            Text("${account.host}  |  ${account.username}", color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
        }
        if (!active) {
            TvButton("Connecter", onClick = onConnect, modifier = Modifier.height(38.dp))
            Spacer(Modifier.width(7.dp))
        }
        TvButton("Modifier", onClick = onEdit, leadingIcon = Icons.Default.Edit, variant = TvButtonVariant.Secondary, modifier = Modifier.height(38.dp))
        Spacer(Modifier.width(7.dp))
        TvButton("Supprimer", onClick = onDelete, leadingIcon = Icons.Default.Delete, variant = TvButtonVariant.Secondary, modifier = Modifier.height(38.dp))
    }
}

@Composable
private fun AccountEditorDialog(
    initial: XtreamAccount,
    onDismiss: () -> Unit,
    onSave: (XtreamAccount) -> Unit,
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var host by remember(initial.id) { mutableStateOf(initial.host) }
    var username by remember(initial.id) { mutableStateOf(initial.username) }
    var password by remember(initial.id) { mutableStateOf(initial.password) }
    var error by remember { mutableStateOf<String?>(null) }
    val nameFocusRequester = remember { FocusRequester() }
    val hostFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val cancelFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        nameFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(570.dp)
                .background(Color(0xFF0A1425), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, SmartVisionColors.Primary), RoundedCornerShape(8.dp))
                .padding(24.dp),
        ) {
            Text("Compte Xtream", color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            SettingsTextField(
                label = "Nom du compte",
                value = name,
                onValueChange = { name = it },
                focusRequester = nameFocusRequester,
                nextFocusRequester = hostFocusRequester,
            )
            SettingsTextField(
                label = "Hote",
                value = host,
                onValueChange = { host = it },
                focusRequester = hostFocusRequester,
                previousFocusRequester = nameFocusRequester,
                nextFocusRequester = usernameFocusRequester,
            )
            SettingsTextField(
                label = "Utilisateur",
                value = username,
                onValueChange = { username = it },
                focusRequester = usernameFocusRequester,
                previousFocusRequester = hostFocusRequester,
                nextFocusRequester = passwordFocusRequester,
            )
            SettingsTextField(
                label = "Mot de passe",
                value = password,
                onValueChange = { password = it },
                focusRequester = passwordFocusRequester,
                previousFocusRequester = usernameFocusRequester,
                nextFocusRequester = saveFocusRequester,
                password = true,
            )
            error?.let {
                Text(it, color = SmartVisionColors.Error, style = SmartVisionType.Caption)
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TvButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    focusRequester = cancelFocusRequester,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(42.dp),
                )
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = "Enregistrer et connecter",
                    focusRequester = saveFocusRequester,
                    onClick = {
                        if (host.isBlank() || username.isBlank() || password.isBlank()) {
                            error = "Hote, utilisateur et mot de passe sont obligatoires."
                        } else {
                            onSave(initial.copy(name = name, host = host, username = username, password = password))
                        }
                    },
                    modifier = Modifier.height(42.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    previousFocusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    password: Boolean = false,
) {
    Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
    Spacer(Modifier.height(5.dp))
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = SmartVisionType.Body.copy(color = SmartVisionColors.TextPrimary),
        cursorBrush = SolidColor(SmartVisionColors.CyanAccent),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        nextFocusRequester?.requestFocus()
                        nextFocusRequester != null
                    }
                    Key.DirectionUp -> {
                        previousFocusRequester?.requestFocus()
                        previousFocusRequester != null
                    }
                    else -> false
                }
            }
            .fillMaxWidth()
            .height(44.dp)
            .background(SmartVisionColors.Surface, RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
    Spacer(Modifier.height(12.dp))
}
