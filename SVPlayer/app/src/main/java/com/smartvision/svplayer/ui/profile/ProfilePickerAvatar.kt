package com.smartvision.svplayer.ui.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.ProfileType
import com.smartvision.svplayer.core.config.canonicalProfileAvatarId

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
        contentScale = ContentScale.Fit,
        modifier = modifier
            .clip(CircleShape)
            .fillMaxSize(),
    )
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
