package com.smartvision.svplayer.ui.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.domain.access.PremiumFeatureGateResult
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
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
    selectionProgress: Float,
    onLockedFeature: () -> Unit,
) {
    val firstProfileFocus = remember { FocusRequester() }
    var profileToEdit by remember { mutableStateOf<PlaylistProfile?>(null) }
    var showProfileEditor by remember { mutableStateOf(false) }
    val initialFocusProfileId = activeProfileId
        ?.takeIf { id -> profiles.any { it.id == id } }
        ?: profiles.firstOrNull()?.id

    LaunchedEffect(profiles, initialFocusProfileId) {
        if (profiles.isNotEmpty()) {
            delay(180)
            runCatching { firstProfileFocus.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SmartVisionColors.Primary.copy(alpha = 0.34f),
                        Color(0xFF060B15),
                        Color(0xFF01040C),
                    ),
                    radius = 1400f,
                ),
            )
            .padding(horizontal = 56.dp, vertical = 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Who's watching?",
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleL,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(34.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.Top) {
                profiles.forEach { profile ->
                    ProfilePickerCard(
                        profile = profile,
                        active = profile.id == activeProfileId,
                        focusRequester = if (profile.id == initialFocusProfileId) firstProfileFocus else null,
                        enabled = !selectionLoading,
                        editEnabled = multiProfileAccess.allowed && !selectionLoading,
                        onClick = { if (!selectionLoading) onSelectProfile(profile.id) },
                        onEdit = {
                            if (multiProfileAccess.allowed) {
                                profileToEdit = profile
                                showProfileEditor = true
                            } else {
                                onLockedFeature()
                            }
                        },
                    )
                }
                AddProfileCard(
                    enabled = multiProfileAccess.allowed && !selectionLoading,
                    locked = !multiProfileAccess.allowed,
                    onClick = {
                        if (multiProfileAccess.allowed) {
                            profileToEdit = null
                            showProfileEditor = true
                        } else {
                            onLockedFeature()
                        }
                    },
                )
            }
            Spacer(Modifier.height(54.dp))
            ProfileSelectionProgressBar(
                visible = selectionLoading,
                progress = selectionProgress,
                modifier = Modifier
                    .width(260.dp)
                    .height(46.dp),
            )
        }
    }

    if (showProfileEditor) {
        PlaylistProfileEditorDialog(
            initial = profileToEdit,
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
}

@Composable
private fun ProfilePickerCard(
    profile: PlaylistProfile,
    active: Boolean,
    focusRequester: FocusRequester?,
    enabled: Boolean,
    editEnabled: Boolean,
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
                .onFocusChanged { focused = it.isFocused }
                .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
                .focusable(enabled = enabled, interactionSource = interactionSource),
            contentAlignment = Alignment.Center,
        ) {
            PlaylistProfileAvatar(profile = profile, modifier = Modifier.matchParentSize())
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
                .background(Color.White.copy(alpha = 0.10f))
                .border(
                    BorderStroke(if (focused) focusStyle.borderWidth else 1.dp, if (focused) focusStyle.accent else SmartVisionColors.Border),
                    RoundedCornerShape(8.dp),
                )
                .onFocusChanged { focused = it.isFocused }
                .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
                .focusable(enabled = enabled, interactionSource = interactionSource),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = if (enabled) Color.White else Color.White.copy(alpha = 0.38f), modifier = Modifier.size(58.dp))
            if (locked) {
                Image(
                    painter = painterResource(R.drawable.premium_crown),
                    contentDescription = "Premium",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(26.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Add Profile",
            color = if (!enabled) SmartVisionColors.TextSecondary.copy(alpha = 0.45f) else if (focused) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            style = SmartVisionType.Body,
            maxLines = 1,
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
private fun ProfileSelectionProgressBar(
    visible: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (visible) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(50)),
                color = SmartVisionColors.CyanAccent,
                trackColor = SmartVisionColors.Surface.copy(alpha = 0.84f),
            )
        }
    }
}
