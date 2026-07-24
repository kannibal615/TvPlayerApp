package com.smartvision.svplayer.ui.profile

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.CredentialsMode
import com.smartvision.svplayer.core.config.ProfileType
import com.smartvision.svplayer.core.config.PlaylistProfileStatus
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.AdminProfileAvatarId
import com.smartvision.svplayer.core.config.ProfileAvatarPresetIds
import com.smartvision.svplayer.core.config.KidsProfileAvatarPresetIds
import com.smartvision.svplayer.core.config.XtreamAccount
import com.smartvision.svplayer.core.config.canonicalProfileAvatarId
import com.smartvision.svplayer.core.config.defaultProfileAvatarId
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.data.xtream.XtreamCredentialsValidationResult
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.activation.ActivationException
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.domain.model.AccountProfile
import com.smartvision.svplayer.domain.model.ParentalControlScope
import com.smartvision.svplayer.domain.model.SyncStatus
import com.smartvision.svplayer.domain.profile.ContentPrefixPolicy
import com.smartvision.svplayer.domain.access.PremiumFeatureGateResult
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.NumericPinDialog
import com.smartvision.svplayer.ui.components.PremiumPreviewQr
import com.smartvision.svplayer.ui.components.TvConfirmationDialog
import com.smartvision.svplayer.ui.components.TvDialogSurface
import com.smartvision.svplayer.ui.activation.ScaledActivationLayout
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.home.HomeVisualBackground
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import com.smartvision.svplayer.ui.home.TvHeader
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.settings.SynchronizationPreferencesContent
import com.smartvision.svplayer.startup.BackgroundSyncScheduler
import com.smartvision.svplayer.sync.SyncFrequencyPolicy
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


internal enum class ProfileSection(
    val label: String,
    val icon: ImageVector,
) {
    Xtream("Info compte", Icons.Default.Person),
    Parental("Controle parental", Icons.Default.Lock),
    Sync("Synchronisation", Icons.Default.CloudSync),
}

@Composable
internal fun XtreamPanel(
    strings: SmartVisionStrings,
    state: ProfileUiState,
    syncStatus: SyncStatus,
    multiProfileAccess: PremiumFeatureGateResult,
    onLockedFeature: () -> Unit,
    onShowXtreamSetupQr: () -> Unit,
    onOpenSyncDialog: () -> Unit,
    onSaveXtreamAccount: (XtreamAccount) -> Unit,
    onSaveEpgUrl: (String) -> Unit,
    onSaveM3uUrl: (String) -> Unit,
    onSelectPlaylistSource: (PlaylistSource) -> Unit,
    onSavePlaylistProfile: (PlaylistProfile) -> Unit,
    onSelectPlaylistProfile: (String) -> Unit,
    onDeletePlaylistProfile: (String) -> Unit,
    onSynchronizePlaylistProfile: (String) -> Unit,
    onDeleteXtreamAccount: (String) -> Unit,
    sectionFocusRequester: FocusRequester,
    deviceCatalogFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    var profileToEdit by remember { mutableStateOf<PlaylistProfile?>(null) }
    var selectedProfileId by remember { mutableStateOf<String?>(null) }
    var profileDetailVisible by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<PlaylistProfile?>(null) }
    var showProfileEditor by remember { mutableStateOf(false) }
    var profileAvatarToEdit by remember { mutableStateOf<PlaylistProfile?>(null) }
    var showEpgEditor by remember { mutableStateOf(false) }
    var showM3uEditor by remember { mutableStateOf(false) }
    var deletedProfileIdAwaitingFocus by remember { mutableStateOf<String?>(null) }
    val profileListFocusRequester = remember { FocusRequester() }
    val selectedProfile = state.playlistProfiles.firstOrNull { it.id == selectedProfileId }
        ?: state.playlistProfiles.firstOrNull { it.id == state.activePlaylistProfileId }
        ?: state.playlistProfiles.firstOrNull()

    LaunchedEffect(deletedProfileIdAwaitingFocus, state.playlistProfiles) {
        val deletedId = deletedProfileIdAwaitingFocus ?: return@LaunchedEffect
        if (state.playlistProfiles.any { it.id == deletedId }) return@LaunchedEffect
        delay(ProfileFocusRequestDelayMillis)
        if (state.playlistProfiles.isNotEmpty() || multiProfileAccess.allowed) {
            runCatching { profileListFocusRequester.requestFocus() }
        } else {
            runCatching { sectionFocusRequester.requestFocus() }
        }
        deletedProfileIdAwaitingFocus = null
    }

    ProfilePanel(
        title = "Info compte",
        icon = Icons.Default.Person,
        modifier = modifier,
        trailingContent = {
            ExpirationPill(state.xtreamExpiresAt.ifBlank { "Expiration non disponible" })
        },
    ) {
        PlaylistProfilesSection(
            profiles = state.playlistProfiles,
            activeProfileId = state.activePlaylistProfileId,
            onAdd = {
                if (multiProfileAccess.allowed) {
                    profileToEdit = null
                    showProfileEditor = true
                } else {
                    onLockedFeature()
                }
            },
            multiProfileLocked = !multiProfileAccess.allowed,
            selectedProfileId = selectedProfile?.id,
            onOpen = { profile ->
                val sameProfile = selectedProfileId == profile.id
                selectedProfileId = profile.id
                profileDetailVisible = !(sameProfile && profileDetailVisible)
            },
            onSelect = onSelectPlaylistProfile,
            onEdit = { profile ->
                if (multiProfileAccess.allowed) {
                    profileToEdit = profile
                    showProfileEditor = true
                } else {
                    onLockedFeature()
                }
            },
            restoreFocusRequester = profileListFocusRequester,
        )
        if (profileDetailVisible) selectedProfile?.let { profile ->
            Spacer(Modifier.height(8.dp))
            PlaylistProfileDetailsPanel(
                profile = profile,
                active = profile.id == state.activePlaylistProfileId,
                syncStatus = syncStatus,
                onEditProfile = {
                    if (multiProfileAccess.allowed) {
                        profileToEdit = profile
                        showProfileEditor = true
                    } else {
                        onLockedFeature()
                    }
                },
                onEditAvatar = {
                    if (multiProfileAccess.allowed) {
                        profileAvatarToEdit = profile
                    } else {
                        onLockedFeature()
                    }
                },
                onShowXtreamSetupQr = onShowXtreamSetupQr,
                onSelectSource = { source ->
                    onSavePlaylistProfile(profile.copy(source = source))
                    selectedProfileId = profile.id
                },
                onEditM3u = { showM3uEditor = true },
                onEditEpg = { showEpgEditor = true },
                onSynchronize = {
                    selectedProfileId = profile.id
                    onSynchronizePlaylistProfile(profile.id)
                },
                onToggleLock = {
                    if (container.profilePinManager.hasPin()) {
                        onSavePlaylistProfile(profile.copy(isLocked = !profile.isLocked))
                        selectedProfileId = profile.id
                    }
                },
                pinConfigured = container.profilePinManager.hasPin(),
                onDelete = {
                    if (profile.type != ProfileType.ADMIN) {
                        selectedProfileId = profile.id
                        profileToDelete = profile
                    }
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        DeviceCatalogInlineSection(
            state = state,
            syncStatus = syncStatus,
            focusRequester = deviceCatalogFocusRequester,
        )
    }

    profileAvatarToEdit?.let { profile ->
        ProfileAvatarPickerDialog(
            initialAvatarId = profile.avatarId,
            profileType = profile.type,
            onDismiss = { profileAvatarToEdit = null },
            onSave = { avatarId ->
                profileAvatarToEdit = null
                onSavePlaylistProfile(profile.copy(avatarId = avatarId))
                selectedProfileId = profile.id
                profileDetailVisible = true
            },
        )
    }

    if (showProfileEditor) {
        PlaylistProfileEditorDialog(
            strings = strings,
            initial = profileToEdit,
            adminProfile = state.playlistProfiles.firstOrNull { it.type == ProfileType.ADMIN },
            existingNames = state.playlistProfiles
                .filterNot { it.id == profileToEdit?.id }
                .map { it.name },
            onDismiss = {
                showProfileEditor = false
                profileToEdit = null
            },
            onSave = { profile ->
                showProfileEditor = false
                profileToEdit = null
                onSavePlaylistProfile(profile)
            },
        )
    }

    profileToDelete?.let { profile ->
        ConfirmPlaylistProfileDeleteDialog(
            profile = profile,
            strings = strings,
            onDismiss = { profileToDelete = null },
            onConfirm = {
                profileToDelete = null
                val deletedIndex = state.playlistProfiles.indexOfFirst { it.id == profile.id }
                selectedProfileId = state.playlistProfiles.getOrNull(deletedIndex + 1)?.id
                    ?: state.playlistProfiles.getOrNull(deletedIndex - 1)?.id
                profileDetailVisible = false
                deletedProfileIdAwaitingFocus = profile.id
                onDeletePlaylistProfile(profile.id)
            },
        )
    }

    if (showEpgEditor) {
        val profile = selectedProfile
        UrlEditorDialog(
            title = "Modifier URL EPG",
            invalidMessage = "URL EPG invalide.",
            initialUrl = profile?.epgUrl ?: state.epgUrl,
            onDismiss = { showEpgEditor = false },
            onSave = { url ->
                showEpgEditor = false
                if (profile != null) {
                    onSavePlaylistProfile(profile.copy(epgUrl = url))
                    selectedProfileId = profile.id
                } else {
                    onSaveEpgUrl(url)
                }
            },
        )
    }

    if (showM3uEditor) {
        val profile = selectedProfile
        UrlEditorDialog(
            title = "Modifier lien M3U",
            invalidMessage = "Lien M3U invalide.",
            initialUrl = profile?.m3uUrl ?: state.m3uUrl,
            onDismiss = { showM3uEditor = false },
            onSave = { url ->
                showM3uEditor = false
                if (profile != null) {
                    onSavePlaylistProfile(profile.copy(m3uUrl = url))
                    selectedProfileId = profile.id
                } else {
                    onSaveM3uUrl(url)
                }
            },
        )
    }
}

@Composable
private fun PlaylistProfilesSection(
    profiles: List<PlaylistProfile>,
    activeProfileId: String,
    selectedProfileId: String?,
    onAdd: () -> Unit,
    multiProfileLocked: Boolean,
    onOpen: (PlaylistProfile) -> Unit,
    onSelect: (String) -> Unit,
    onEdit: (PlaylistProfile) -> Unit,
    restoreFocusRequester: FocusRequester,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.62f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.82f)), RoundedCornerShape(7.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            AccountInfoLine(
                icon = Icons.Default.Person,
                label = "Profils",
                value = profiles.firstOrNull { it.id == activeProfileId }?.name ?: "Aucun profil actif",
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            TvButton(
                text = "Ajouter",
                onClick = onAdd,
                leadingIcon = Icons.Default.Add,
                enabled = !multiProfileLocked,
                focusRequester = restoreFocusRequester.takeIf { profiles.isEmpty() },
                modifier = Modifier
                    .width(150.dp)
                    .height(40.dp),
            )
            if (multiProfileLocked) {
                Spacer(Modifier.width(8.dp))
                Image(
                    painter = painterResource(R.drawable.premium_crown),
                    contentDescription = "Premium",
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (profiles.isEmpty()) {
            Text(
                text = "Aucun profil. Ajoutez une source Xtream ou M3U pour configurer ce compte.",
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
            )
        } else {
            profiles.forEach { profile ->
                PlaylistProfileRow(
                    profile = profile,
                    active = profile.id == activeProfileId,
                    selected = profile.id == selectedProfileId,
                    onOpen = { onOpen(profile) },
                    onSelect = { onSelect(profile.id) },
                    onEdit = { onEdit(profile) },
                    externalFocusRequester = restoreFocusRequester.takeIf { profile.id == selectedProfileId },
                )
                Spacer(Modifier.height(7.dp))
            }
        }
    }
}

@Composable
private fun PlaylistProfileRow(
    profile: PlaylistProfile,
    active: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    externalFocusRequester: FocusRequester? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val internalRowFocusRequester = remember { FocusRequester() }
    val rowFocusRequester = externalFocusRequester ?: internalRowFocusRequester
    val toggleFocusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val focusStyle = LocalTvFocusStyle.current
    val borderColor = when {
        focused -> focusStyle.accent
        selected -> SmartVisionColors.CyanAccent.copy(alpha = 0.72f)
        active -> Color(0xFF20D46B).copy(alpha = 0.68f)
        else -> SmartVisionColors.Border.copy(alpha = 0.55f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    focused -> focusStyle.background
                    selected -> SmartVisionColors.Primary.copy(alpha = 0.20f)
                    active -> Color(0xFF0F3728).copy(alpha = 0.66f)
                    else -> Color.White.copy(alpha = 0.035f)
                },
            )
            .border(
                BorderStroke(if (focused) focusStyle.borderWidth else 1.dp, borderColor),
                RoundedCornerShape(6.dp),
            )
            .focusRequester(rowFocusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight && profile.isConfigured) {
                    runCatching { toggleFocusRequester.requestFocus() }
                    true
                } else {
                    false
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onOpen)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaylistProfileAvatar(profile = profile, modifier = Modifier.size(42.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileInlineEditIcon(onClick = onEdit)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = profile.name,
                    color = SmartVisionColors.TextPrimary,
                    style = SmartVisionType.Label,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${profile.source.displayLabel()}  |  ${profile.status.displayLabel()}",
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        SourceToggleButton(
            active = active,
            enabled = profile.isConfigured,
            onClick = onSelect,
            modifier = Modifier
                .focusRequester(toggleFocusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                        runCatching { rowFocusRequester.requestFocus() }
                        true
                    } else {
                        false
                    }
                },
        )
    }
}

@Composable
private fun ProfileInlineEditIcon(
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (focused) SmartVisionColors.Primary.copy(alpha = 0.32f) else Color.Transparent)
            .border(
                BorderStroke(if (focused) 1.dp else 0.dp, if (focused) SmartVisionColors.CyanAccent else Color.Transparent),
                RoundedCornerShape(5.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Modifier le profil",
            tint = if (focused) Color.White else SmartVisionColors.TextSecondary,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
fun PlaylistProfileAvatar(
    profile: PlaylistProfile,
    modifier: Modifier = Modifier,
) {
    ProfileAvatarImage(
        avatarId = profile.avatarId,
        profileType = profile.type,
        modifier = modifier,
    )
}

@Composable
private fun ProfileAvatarEditButton(
    profile: PlaylistProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) SmartVisionColors.CyanAccent else Color.White.copy(alpha = 0.22f)),
                RoundedCornerShape(7.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        PlaylistProfileAvatar(profile = profile, modifier = Modifier.matchParentSize())
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Modifier la photo",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .background(Color.Black.copy(alpha = 0.52f), RoundedCornerShape(50))
                .padding(2.dp),
        )
    }
}

@Composable
private fun PlaylistProfileDetailsPanel(
    profile: PlaylistProfile,
    active: Boolean,
    syncStatus: SyncStatus,
    onEditProfile: () -> Unit,
    onEditAvatar: () -> Unit,
    onShowXtreamSetupQr: () -> Unit,
    onSelectSource: (PlaylistSource) -> Unit,
    onEditM3u: () -> Unit,
    onEditEpg: () -> Unit,
    onSynchronize: () -> Unit,
    onToggleLock: () -> Unit,
    pinConfigured: Boolean,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.72f)), RoundedCornerShape(7.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatarEditButton(
                profile = profile,
                onClick = onEditAvatar,
                modifier = Modifier.size(50.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.Label,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    ProfileInlineEditIcon(onClick = onEditProfile)
                }
                Text(
                    text = if (active) "Profil actif" else "Profil selectionne",
                    color = if (active) Color(0xFF20D46B) else SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(SmartVisionColors.Surface.copy(alpha = 0.58f))
                .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.78f)), RoundedCornerShape(7.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceToggleButton(active = profile.isLocked, enabled = pinConfigured, onClick = onToggleLock)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Profile lock", color = SmartVisionColors.TextPrimary, style = SmartVisionType.Label)
                Text(
                    if (pinConfigured) "Uses the administrator parental PIN" else "Configure the parental PIN first",
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        XtreamAccountCard(
            account = profile.toXtreamAccountOrNull(),
            active = profile.source == PlaylistSource.Xtream,
            editEnabled = true,
            deleteEnabled = profile.type != ProfileType.ADMIN,
            onToggleSource = { onSelectSource(PlaylistSource.Xtream) },
            onEdit = onEditProfile,
            onEditQr = onShowXtreamSetupQr,
            onDelete = onDelete,
        )
        Spacer(Modifier.height(8.dp))
        M3uUrlCard(
            m3uUrl = profile.m3uUrl,
            active = profile.source == PlaylistSource.M3u,
            onToggleSource = { onSelectSource(PlaylistSource.M3u) },
            onEdit = onEditM3u,
        )
        Spacer(Modifier.height(8.dp))
        EpgUrlCard(
            epgUrl = profile.epgUrl,
            onEdit = onEditEpg,
            onEditQr = onShowXtreamSetupQr,
        )
        Spacer(Modifier.height(8.dp))
        SynchronizationCard(
            lastSync = profile.lastSyncAt.asProfileDate(),
            syncStatus = syncStatus,
            onOpenSyncDialog = onSynchronize,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun XtreamAccountCard(
    account: XtreamAccount?,
    active: Boolean,
    editEnabled: Boolean = account != null,
    deleteEnabled: Boolean = account != null,
    onToggleSource: () -> Unit,
    onEdit: () -> Unit,
    onEditQr: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.58f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.78f)), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceToggleButton(active = active, enabled = account != null, onClick = onToggleSource)
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = account?.name?.ifBlank { "Compte Xtream" } ?: "Compte Xtream",
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            XtreamCredentialsLine(account)
        }
        Spacer(Modifier.width(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ProfileIconTileButton(
                icon = Icons.Default.QrCode2,
                contentDescription = "Modifier par QR",
                onClick = onEditQr,
            )
            ProfileIconTileButton(
                icon = Icons.Default.Edit,
                contentDescription = "Modifier",
                onClick = onEdit,
                enabled = editEnabled,
            )
            ProfileIconTileButton(
                icon = Icons.Default.Delete,
                contentDescription = "Supprimer",
                onClick = onDelete,
                enabled = deleteEnabled,
            )
        }
    }
}

@Composable
private fun XtreamCredentialsLine(account: XtreamAccount?) {
    Text(
        text = listOf(
            "URL ${account?.host?.ifBlank { "Non configure" } ?: "Non configure"}",
            "User ${account?.username?.ifBlank { "Non configure" } ?: "Non configure"}",
            "Pass ${if (account?.password.isNullOrBlank()) "Non configure" else "********"}",
        ).joinToString("  |  "),
        color = SmartVisionColors.TextSecondary,
        style = SmartVisionType.Caption,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun M3uUrlCard(
    m3uUrl: String,
    active: Boolean,
    onToggleSource: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.58f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.78f)), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceToggleButton(active = active, enabled = m3uUrl.isNotBlank(), onClick = onToggleSource)
        Spacer(Modifier.width(12.dp))
        AccountInfoLine(
            icon = Icons.Default.Devices,
            label = "Lien M3U",
            value = m3uUrl.ifBlank { "Non configure" },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        ProfileIconTileButton(
            icon = Icons.Default.Edit,
            contentDescription = "Modifier lien M3U",
            onClick = onEdit,
        )
    }
}

@Composable
private fun EpgUrlCard(
    epgUrl: String,
    onEdit: () -> Unit,
    onEditQr: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.58f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.78f)), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountInfoLine(
            icon = Icons.Default.Devices,
            label = "URL EPG",
            value = epgUrl.ifBlank { "Non configure" },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(18.dp))
        ProfileIconTileButton(
            icon = Icons.Default.Edit,
            contentDescription = "Modifier URL EPG",
            onClick = onEdit,
        )
        Spacer(Modifier.width(12.dp))
        ProfileIconTileButton(
            icon = Icons.Default.QrCode2,
            contentDescription = "Modifier URL EPG par QR",
            onClick = onEditQr,
        )
    }
}

@Composable
private fun SourceToggleButton(
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val color = if (active) Color(0xFF20D46B) else Color(0xFFE33A3A)
    Box(
        modifier = modifier
            .size(width = 54.dp, height = 26.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = if (enabled) 0.88f else 0.32f))
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color.White else color.copy(alpha = 0.9f)),
                RoundedCornerShape(50),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .padding(horizontal = 6.dp),
        contentAlignment = if (active) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            text = if (active) "ON" else "OFF",
            color = Color.White,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun AccountInfoLine(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProfileIconTileButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (focused) SmartVisionColors.CyanAccent else Color.Transparent
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 38.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (focused) SmartVisionColors.SurfaceElevated.copy(alpha = 0.9f) else Color.Transparent)
            .border(BorderStroke(if (focused) 1.dp else 0.dp, borderColor), RoundedCornerShape(7.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(enabled = enabled, interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.34f),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun UrlEditorDialog(
    title: String,
    invalidMessage: String,
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    var error by remember { mutableStateOf<String?>(null) }
    val urlFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(ProfileFocusRequestDelayMillis)
        runCatching { urlFocusRequester.requestFocus() }
    }

    TvDialogSurface(
        title = title,
        onDismiss = onDismiss,
        width = 600.dp,
        icon = Icons.Default.Edit,
    ) {
            ProfileEditTextField(
                label = "URL",
                value = url,
                onValueChange = {
                    url = it
                    error = null
                },
                focusRequester = urlFocusRequester,
                nextFocusRequester = saveFocusRequester,
            )
            error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = SmartVisionColors.Error, style = SmartVisionType.Caption)
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TvButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(42.dp),
                )
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = "Enregistrer",
                    onClick = {
                        val normalized = url.trim()
                        if (normalized.isNotBlank() && !normalized.startsWith("http://") && !normalized.startsWith("https://")) {
                            error = invalidMessage
                        } else {
                            onSave(normalized)
                        }
                    },
                    focusRequester = saveFocusRequester,
                    modifier = Modifier.height(42.dp),
                )
            }
    }
}

@Composable
private fun DeviceCatalogInlineSection(
    state: ProfileUiState,
    syncStatus: SyncStatus,
    focusRequester: FocusRequester,
) {
    var focused by remember { mutableStateOf(false) }
    val focusStyle = LocalTvFocusStyle.current
    val shape = RoundedCornerShape(7.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clip(shape)
            .background(if (focused) focusStyle.background else SmartVisionColors.Surface.copy(alpha = 0.58f))
            .border(
                BorderStroke(
                    if (focused) focusStyle.borderWidth else 1.dp,
                    if (focused) focusStyle.accent else SmartVisionColors.Border.copy(alpha = 0.78f),
                ),
                shape,
            )
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Devices, contentDescription = null, tint = SmartVisionColors.CyanAccent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Appareil et catalogue", color = SmartVisionColors.TextPrimary, style = SmartVisionType.Label, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            DeviceCatalogSyncHeaderStatus(syncStatus)
        }
        Spacer(Modifier.height(12.dp))
        DeviceCatalogContent(state, syncStatus)
    }
}

@Composable
private fun DeviceCatalogContent(
    state: ProfileUiState,
    syncStatus: SyncStatus,
) {
    val catalogProfileName = state.playlistProfiles
        .firstOrNull { it.id == state.activePlaylistProfileId }
        ?.name
        ?.trim()
        ?.ifBlank { null }
        ?: "Profil actif"
    val progress = syncStatus.catalogProgressOrDefault(state.account)
    val showProgress = syncStatus is SyncStatus.Running || syncStatus is SyncStatus.Error
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        XtreamSyncCountCard(
            "Live TV",
            progress.live,
            showProgress,
            visualStyle = HomeVisualStyle.Signal,
            modifier = Modifier.weight(1f),
        )
        XtreamSyncCountCard(
            "Films",
            progress.movies,
            showProgress,
            enabled = state.activePlaylistSource == PlaylistSource.Xtream,
            visualStyle = HomeVisualStyle.Cinema,
            modifier = Modifier.weight(1f),
        )
        XtreamSyncCountCard(
            "Series",
            progress.series,
            showProgress,
            enabled = state.activePlaylistSource == PlaylistSource.Xtream,
            visualStyle = HomeVisualStyle.Series,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
        ProfileInfoRow("Code TV", state.tvCode, modifier = Modifier.weight(1f))
        ProfileInfoRow("Profil catalogue", catalogProfileName, modifier = Modifier.weight(1f))
    }
    if (state.errorMessage != null) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = state.errorMessage,
            color = SmartVisionColors.Error,
            style = SmartVisionType.Caption,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DeviceCatalogSyncHeaderStatus(syncStatus: SyncStatus) {
    when (syncStatus) {
        is SyncStatus.Running -> {
            Text(
                text = syncStatus.message,
                color = SmartVisionColors.CyanAccent,
                style = SmartVisionType.Caption,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        is SyncStatus.Success -> {
            Text(
                text = syncStatus.message,
                color = SmartVisionColors.Success,
                style = SmartVisionType.Caption,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        is SyncStatus.Error -> {
            Text(
                text = syncStatus.message,
                color = SmartVisionColors.Error,
                style = SmartVisionType.Caption,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        else -> Unit
    }
}

@Composable
internal fun ProfilePanel(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xEA0B1526),
                        Color(0xF207101E),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = SmartVisionColors.CyanAccent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(9.dp))
            Text(title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            trailingContent?.invoke()
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
internal fun ProfileMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = SmartVisionColors.TextPrimary,
) {
    Column(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.72f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.78f)), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            color = accent,
            style = SmartVisionType.Label,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ProfileInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun UsageStep(
    title: String,
    text: String,
    active: Boolean,
    color: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(if (active) color.copy(alpha = 0.16f) else SmartVisionColors.Surface.copy(alpha = 0.42f))
            .border(BorderStroke(1.dp, if (active) color.copy(alpha = 0.68f) else SmartVisionColors.Border), RoundedCornerShape(7.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = if (active) Icons.Default.CheckCircle else Icons.Default.CreditCard,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Label, fontWeight = FontWeight.Bold)
            Text(text, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
    Spacer(Modifier.height(9.dp))
}

@Composable
internal fun StatusPill(label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.18f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.72f)), RoundedCornerShape(100.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, style = SmartVisionType.Caption, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExpirationPill(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(SmartVisionColors.Primary.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, SmartVisionColors.CyanAccent.copy(alpha = 0.62f)), RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = SmartVisionColors.CyanAccent,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlaylistProfile.toXtreamAccountOrNull(): XtreamAccount? {
    if (xtreamHost.isBlank() && xtreamUsername.isBlank() && xtreamPassword.isBlank()) return null
    return XtreamAccount(
        id = id,
        name = "Compte SmartVision",
        host = xtreamHost,
        username = xtreamUsername,
        password = xtreamPassword,
        epgUrl = epgUrl,
    )
}

private fun PlaylistSource.displayLabel(): String =
    when (this) {
        PlaylistSource.Xtream -> "Xtream Codes"
        PlaylistSource.M3u -> "Playlist M3U"
    }

internal fun UsageMode.localizedPrimaryCta(strings: SmartVisionStrings): String = when (this) {
    UsageMode.Trial -> strings.usageUpgradePremium
    UsageMode.Premium -> strings.usageExtend
    UsageMode.FreeAds -> strings.usageRemoveAds
    UsageMode.Unknown -> strings.usageBuyLicense
}

private fun PlaylistProfileStatus.displayLabel(): String =
    when (this) {
        PlaylistProfileStatus.Active -> "Actif"
        PlaylistProfileStatus.Inactive -> "Inactif"
        PlaylistProfileStatus.Error -> "Erreur"
        PlaylistProfileStatus.NotConfigured -> "Non configure"
    }

internal fun String.looksLikeHttpUrl(): Boolean {
    val value = trim()
    return value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)
}

internal fun String.looksLikeUrlHost(): Boolean {
    val value = trim()
    return value.isNotBlank() && (value.looksLikeHttpUrl() || "." in value)
}

internal fun Long?.asProfileDate(): String =
    this?.let { timestamp ->
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    } ?: "Jamais"

internal fun String.safeServerHost(fallback: String): String =
    runCatching { java.net.URI(trim()).host }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: fallback
