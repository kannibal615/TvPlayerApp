package com.smartvision.svplayer.ui.profile

import androidx.compose.foundation.BorderStroke
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
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay

@Composable
fun ProfilePickerScreen(
    profiles: List<PlaylistProfile>,
    activeProfileId: String?,
    onSelectProfile: (String) -> Unit,
    onManageProfiles: () -> Unit,
) {
    val firstProfileFocus = remember { FocusRequester() }
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
                        onClick = { onSelectProfile(profile.id) },
                    )
                }
                AddProfileCard(onClick = onManageProfiles)
            }
            Spacer(Modifier.height(54.dp))
            TvButton(
                text = "Manage profiles",
                onClick = onManageProfiles,
                leadingIcon = Icons.Default.Edit,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier
                    .width(260.dp)
                    .height(46.dp),
            )
        }
    }
}

@Composable
private fun ProfilePickerCard(
    profile: PlaylistProfile,
    active: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
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
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .focusable(interactionSource = interactionSource),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = profile.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                style = SmartVisionType.TitleL,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = profile.name,
            color = if (focused) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            style = SmartVisionType.Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddProfileCard(onClick: () -> Unit) {
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
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .focusable(interactionSource = interactionSource),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(58.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Add Profile",
            color = if (focused) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            style = SmartVisionType.Body,
            maxLines = 1,
        )
    }
}
