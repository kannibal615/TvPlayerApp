package com.smartvision.svplayer.ui.profile

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.ProfileType
import com.smartvision.svplayer.core.config.canonicalProfileAvatarId

private const val ProfileAvatarCornerFraction = 0.16f
internal val ProfileAvatarShape = RoundedCornerShape(16)

/** Picker avatar driven only by profile metadata, never by the displayed profile name. */
@Composable
fun ProfilePickerAvatar(
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
fun ProfileAvatarImage(
    avatarId: String,
    profileType: ProfileType,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(avatarDrawableResource(avatarId, profileType)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(ProfileAvatarShape)
            .fillMaxSize(),
    )
}

@Composable
fun ProfileAvatarLoadingIndicator(
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 3.25.dp,
) {
    val transition = rememberInfiniteTransition(label = "profile-avatar-loader")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "profile-avatar-loader-phase",
    )
    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val inset = strokePx / 2f
        val loaderWidth = (size.width - strokePx).coerceAtLeast(1f)
        val loaderHeight = (size.height - strokePx).coerceAtLeast(1f)
        val radius = minOf(loaderWidth, loaderHeight) * ProfileAvatarCornerFraction
        val perimeter = 2f * (loaderWidth + loaderHeight - 4f * radius) +
            2f * Math.PI.toFloat() * radius
        val segment = perimeter * 0.60f
        val gap = (perimeter - segment).coerceAtLeast(1f)
        drawRoundRect(
            color = color.copy(alpha = 0.16f),
            topLeft = Offset(inset, inset),
            size = Size(loaderWidth, loaderHeight),
            cornerRadius = CornerRadius(radius),
            style = Stroke(width = strokePx * 0.72f, cap = StrokeCap.Round),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(loaderWidth, loaderHeight),
            cornerRadius = CornerRadius(radius),
            style = Stroke(
                width = strokePx,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(segment, gap),
                    phase = -phase * perimeter,
                ),
            ),
        )
    }
}

private fun avatarDrawableResource(avatarId: String, profileType: ProfileType): Int =
    when (canonicalProfileAvatarId(avatarId, profileType)) {
        "admin" -> R.drawable.avatar_admin
        "kid_robot" -> R.drawable.avatar_kid_robot
        "kid_bear" -> R.drawable.avatar_kid_bear
        "kid_moon" -> R.drawable.avatar_kid_moon
        "kid_rainbow" -> R.drawable.avatar_kid_rainbow
        "kid_dinosaur" -> R.drawable.avatar_kid_dinosaur
        "kid_star" -> R.drawable.avatar_kid_star
        "kid_sun" -> R.drawable.avatar_kid_sun
        "kid_cube" -> R.drawable.avatar_kid_cube
        "classic_mountain" -> R.drawable.avatar_classic_mountain
        "classic_wave" -> R.drawable.avatar_classic_wave
        "classic_leaf" -> R.drawable.avatar_classic_leaf
        "classic_planet" -> R.drawable.avatar_classic_planet
        "classic_lighthouse" -> R.drawable.avatar_classic_lighthouse
        "classic_owl" -> R.drawable.avatar_classic_owl
        "classic_sunrise" -> R.drawable.avatar_classic_sunrise
        else -> R.drawable.avatar_classic_compass
    }
