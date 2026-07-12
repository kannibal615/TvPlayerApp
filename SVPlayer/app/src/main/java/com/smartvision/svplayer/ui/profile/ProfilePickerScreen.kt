package com.smartvision.svplayer.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.ProfileType
import com.smartvision.svplayer.domain.access.PremiumFeatureGateResult
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.TvDialogSurface
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay

@Composable
fun ProfilePickerScreen(
    profiles: List<PlaylistProfile>,
    activeProfileId: String?,
    onSelectProfile: (String) -> Unit,
    onSaveProfile: (PlaylistProfile) -> Unit,
    multiProfileAccess: PremiumFeatureGateResult,
    selectionLoading: Boolean,
    onLockedFeature: () -> Unit,
    onVerifyPin: (String) -> Boolean,
) {
    val firstProfileFocus = remember { FocusRequester() }
    var profileToEdit by remember { mutableStateOf<PlaylistProfile?>(null) }
    var showProfileEditor by remember { mutableStateOf(false) }
    var createProfileType by remember { mutableStateOf<ProfileType?>(null) }
    var protectedAction by remember { mutableStateOf<PickerProtectedAction?>(null) }
    var backgroundTarget by remember { mutableStateOf(PickerBackgroundTarget.Standard) }
    val orderedProfiles = remember(profiles) {
        profiles.sortedWith(compareBy<PlaylistProfile> { if (it.type == ProfileType.ADMIN) 0 else 1 }.thenBy { it.createdAt })
    }
    val adminProfile = orderedProfiles.firstOrNull { it.type == ProfileType.ADMIN }
    val initialFocusProfileId = activeProfileId
        ?.takeIf { id -> orderedProfiles.any { it.id == id } }
        ?: orderedProfiles.firstOrNull()?.id

    fun performAction(action: PickerProtectedAction) {
        when (action) {
            is PickerProtectedAction.Select -> onSelectProfile(action.profile.id)
            is PickerProtectedAction.Edit -> {
                profileToEdit = action.profile
                createProfileType = null
                showProfileEditor = true
            }
            is PickerProtectedAction.Add -> {
                profileToEdit = null
                createProfileType = action.type
                showProfileEditor = true
            }
        }
    }

    fun requestAction(action: PickerProtectedAction, locked: Boolean) {
        if (locked) protectedAction = action else performAction(action)
    }

    LaunchedEffect(profiles, initialFocusProfileId, selectionLoading) {
        if (profiles.isNotEmpty() && !selectionLoading) {
            delay(180)
            runCatching { firstProfileFocus.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = backgroundTarget,
            animationSpec = tween(320),
            label = "profile-picker-background",
            modifier = Modifier.fillMaxSize(),
        ) { target ->
            Box(Modifier.fillMaxSize()) {
                if (target == PickerBackgroundTarget.Kids) {
                    Image(
                        painter = painterResource(R.drawable.kids_home_hero),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(target.backgroundBrush()),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 56.dp, vertical = 44.dp),
        ) {
            Text(
                text = "Who's watching?",
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleL,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(34.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.Top) {
                orderedProfiles.forEach { profile ->
                    ProfilePickerCard(
                        profile = profile,
                        active = profile.id == activeProfileId,
                        focusRequester = if (profile.id == initialFocusProfileId) firstProfileFocus else null,
                        enabled = !selectionLoading,
                        editEnabled = multiProfileAccess.allowed && !selectionLoading,
                        onFocused = { backgroundTarget = PickerBackgroundTarget.Standard },
                        onClick = { if (!selectionLoading) requestAction(PickerProtectedAction.Select(profile), profile.isLocked) },
                        onEdit = {
                            if (multiProfileAccess.allowed) {
                                requestAction(PickerProtectedAction.Edit(profile), adminProfile?.isLocked == true)
                            } else {
                                onLockedFeature()
                            }
                        },
                    )
                }
                AddProfileCard(
                    enabled = multiProfileAccess.allowed && !selectionLoading,
                    locked = !multiProfileAccess.allowed || adminProfile?.isLocked == true,
                    label = "Add Kids Profile",
                    kids = true,
                    onFocused = { backgroundTarget = PickerBackgroundTarget.Kids },
                    onClick = {
                        if (multiProfileAccess.allowed) {
                            requestAction(PickerProtectedAction.Add(ProfileType.KIDS), adminProfile?.isLocked == true)
                        } else {
                            onLockedFeature()
                        }
                    },
                )
                AddProfileCard(
                    enabled = multiProfileAccess.allowed && !selectionLoading,
                    locked = !multiProfileAccess.allowed || adminProfile?.isLocked == true,
                    label = "Add Normal Profile",
                    kids = false,
                    onFocused = { backgroundTarget = PickerBackgroundTarget.Normal },
                    onClick = {
                        if (multiProfileAccess.allowed) {
                            requestAction(PickerProtectedAction.Add(ProfileType.NORMAL), adminProfile?.isLocked == true)
                        } else {
                            onLockedFeature()
                        }
                    },
                )
            }
            Spacer(Modifier.height(54.dp))
            ProfileSelectionLoadingIndicator(
                visible = selectionLoading,
                modifier = Modifier
                    .width(260.dp)
                    .height(46.dp),
            )
        }
    }

    if (showProfileEditor) {
        PlaylistProfileEditorDialog(
            initial = profileToEdit,
            createType = createProfileType,
            adminProfile = adminProfile,
            existingNames = profiles
                .filterNot { it.id == profileToEdit?.id }
                .map { it.name },
            onDismiss = {
                showProfileEditor = false
                profileToEdit = null
            },
            onSave = { profile ->
                showProfileEditor = false
                profileToEdit = null
                onSaveProfile(profile)
            },
        )
    }

    protectedAction?.let { action ->
        ProfilePickerPinDialog(
            onDismiss = { protectedAction = null },
            onVerify = { pin ->
                if (onVerifyPin(pin)) {
                    protectedAction = null
                    performAction(action)
                    true
                } else false
            },
        )
    }
}

private sealed interface PickerProtectedAction {
    data class Select(val profile: PlaylistProfile) : PickerProtectedAction
    data class Edit(val profile: PlaylistProfile) : PickerProtectedAction
    data class Add(val type: ProfileType) : PickerProtectedAction
}

private enum class PickerBackgroundTarget { Standard, Normal, Kids }

private fun PickerBackgroundTarget.backgroundBrush(): Brush = when (this) {
    PickerBackgroundTarget.Standard -> Brush.radialGradient(
        listOf(SmartVisionColors.Primary.copy(alpha = 0.34f), Color(0xFF060B15), Color(0xFF01040C)),
        radius = 1400f,
    )
    PickerBackgroundTarget.Normal -> Brush.radialGradient(
        listOf(Color(0xFF244F80), Color(0xFF071426), Color(0xFF01040C)),
        radius = 1350f,
    )
    PickerBackgroundTarget.Kids -> Brush.radialGradient(
        listOf(Color(0xFF784ED8).copy(alpha = 0.18f), Color(0xFF061126).copy(alpha = 0.34f), Color(0xFF01040C).copy(alpha = 0.78f)),
        radius = 1450f,
    )
}

@Composable
private fun ProfilePickerPinDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Boolean,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val pinFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        runCatching { pinFocusRequester.requestFocus() }
    }

    TvDialogSurface(
        title = "Enter administrator PIN",
        onDismiss = onDismiss,
        width = 460.dp,
        icon = Icons.Default.Lock,
    ) {
        OutlinedTextField(
            value = pin,
            onValueChange = {
                pin = it.filter(Char::isDigit).take(4)
                error = false
            },
            modifier = Modifier.fillMaxWidth().focusRequester(pinFocusRequester),
            singleLine = true,
            label = { Text("4-digit PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            isError = error,
        )
        if (error) {
            Spacer(Modifier.height(6.dp))
            Text("Incorrect PIN. Try again.", color = SmartVisionColors.Error, style = SmartVisionType.Caption)
        }
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TvButton(text = "Cancel", onClick = onDismiss, variant = TvButtonVariant.Secondary)
            Spacer(Modifier.width(10.dp))
            TvButton(
                text = "Unlock",
                enabled = pin.length == 4,
                onClick = { error = !onVerify(pin) },
            )
        }
    }
}

@Composable
private fun ProfilePickerCard(
    profile: PlaylistProfile,
    active: Boolean,
    focusRequester: FocusRequester?,
    enabled: Boolean,
    editEnabled: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusStyle = LocalTvFocusStyle.current
    var focused by remember { mutableStateOf(false) }
    val avatarColor = remember(profile.avatarColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(profile.avatarColorHex)) }
            .getOrDefault(SmartVisionColors.Primary)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp)) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(avatarColor)
                .border(
                    BorderStroke(
                        if (focused) focusStyle.borderWidth else 2.dp,
                        when {
                            focused -> focusStyle.accent
                            active -> Color(0xFF20D46B)
                            else -> Color.Black.copy(alpha = 0.54f)
                        },
                    ),
                    RoundedCornerShape(8.dp),
                )
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                }
                .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
                .focusable(enabled = enabled, interactionSource = interactionSource),
            contentAlignment = Alignment.Center,
        ) {
            PlaylistProfileAvatar(profile = profile, modifier = Modifier.matchParentSize())
            if (profile.isLocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(19.dp),
                )
            }
            if (profile.type == ProfileType.ADMIN) {
                Text(
                    text = "ADMIN",
                    color = Color.White,
                    style = SmartVisionType.Caption,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PickerEditButton(enabled = editEnabled, onClick = onEdit)
            Spacer(Modifier.width(5.dp))
            Text(
                text = profile.name,
                color = if (focused) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
                style = SmartVisionType.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AddProfileCard(
    enabled: Boolean,
    locked: Boolean,
    label: String,
    kids: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val focusStyle = LocalTvFocusStyle.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp)) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (kids) Color(0xFF6D48C8).copy(alpha = 0.34f) else Color.White.copy(alpha = 0.10f))
                .border(
                    BorderStroke(if (focused) focusStyle.borderWidth else 1.dp, if (focused) focusStyle.accent else SmartVisionColors.Border),
                    RoundedCornerShape(8.dp),
                )
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                }
                .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
                .focusable(enabled = enabled, interactionSource = interactionSource),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = if (enabled) Color.White else Color.White.copy(alpha = 0.38f), modifier = Modifier.size(58.dp))
            if (locked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            color = if (!enabled) SmartVisionColors.TextSecondary.copy(alpha = 0.45f) else if (focused) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            style = SmartVisionType.Body,
            maxLines = 2,
        )
    }
}

@Composable
private fun PickerEditButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (focused) SmartVisionColors.Primary.copy(alpha = 0.34f) else Color.Transparent)
            .border(BorderStroke(if (focused) 1.dp else 0.dp, if (focused) SmartVisionColors.CyanAccent else Color.Transparent), RoundedCornerShape(5.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(enabled = enabled, interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = "Edit profile",
            tint = if (enabled) SmartVisionColors.TextSecondary else SmartVisionColors.TextSecondary.copy(alpha = 0.32f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun ProfileSelectionLoadingIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (visible) {
            CircularProgressIndicator(
                color = SmartVisionColors.CyanAccent,
                strokeWidth = 4.dp,
                modifier = Modifier.size(38.dp),
            )
        }
    }
}
