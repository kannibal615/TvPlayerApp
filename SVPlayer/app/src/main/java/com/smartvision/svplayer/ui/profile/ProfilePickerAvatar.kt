package com.smartvision.svplayer.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.ProfileType
import com.smartvision.svplayer.ui.theme.SmartVisionColors

/** Picker avatar driven only by profile metadata, never by the displayed profile name. */
@Composable
fun ProfilePickerAvatar(
    profile: PlaylistProfile,
    modifier: Modifier = Modifier,
) {
    val visual = remember(profile.avatarId, profile.avatarColorHex, profile.type) {
        profilePickerAvatarVisual(profile)
    }
    Box(
        modifier = modifier
            .background(Brush.radialGradient(listOf(visual.highlight, visual.primary, visual.secondary)))
            .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.68f)
                .background(Color.Black.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.94f),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(17.dp),
            )
        }
    }
}

private data class ProfilePickerAvatarVisual(
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val icon: ImageVector,
)

private fun profilePickerAvatarVisual(profile: PlaylistProfile): ProfilePickerAvatarVisual {
    val fallback = runCatching { Color(android.graphics.Color.parseColor(profile.avatarColorHex)) }
        .getOrDefault(SmartVisionColors.Primary)
    val colors = when (profile.avatarId) {
        "aurora" -> Color(0xFF00D4FF) to Color(0xFF1439D6)
        "ocean" -> Color(0xFF0077B6) to Color(0xFF001D4F)
        "sunset" -> Color(0xFFFF7A1A) to Color(0xFFD7263D)
        "emerald" -> Color(0xFF00A86B) to Color(0xFF073B3A)
        "violet" -> Color(0xFF8A45FF) to Color(0xFF2B0A5B)
        "coral" -> Color(0xFFFF5E5B) to Color(0xFF7A1E3A)
        "steel" -> Color(0xFF64748B) to Color(0xFF0F172A)
        "gold" -> Color(0xFFFFB703) to Color(0xFF8A5A00)
        "rose" -> Color(0xFFFF4D8D) to Color(0xFF7F1D5A)
        "midnight" -> Color(0xFF1D4ED8) to Color(0xFF020617)
        "kids_sky" -> Color(0xFF38BDF8) to Color(0xFF4F46E5)
        "kids_star" -> Color(0xFFFBBF24) to Color(0xFFF97316)
        "kids_mint" -> Color(0xFF34D399) to Color(0xFF0EA5E9)
        "kids_coral" -> Color(0xFFFB7185) to Color(0xFFA855F7)
        "kids_sun" -> Color(0xFFFDE047) to Color(0xFFEC4899)
        else -> fallback to fallback.copy(alpha = 0.68f)
    }
    val icon = when (profile.type) {
        ProfileType.ADMIN -> Icons.Default.AdminPanelSettings
        ProfileType.KIDS -> Icons.Default.ChildCare
        ProfileType.NORMAL -> Icons.Default.Person
    }
    return ProfilePickerAvatarVisual(
        primary = colors.first,
        secondary = colors.second,
        highlight = colors.first.copy(alpha = 0.72f),
        icon = icon,
    )
}
