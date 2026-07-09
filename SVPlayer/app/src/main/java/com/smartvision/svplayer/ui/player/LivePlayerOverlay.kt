package com.smartvision.svplayer.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import kotlinx.coroutines.delay

@Composable
internal fun LiveTvFullscreenOverlay(
    playback: FullScreenPlayback,
    errorText: String?,
    firstActionFocusRequester: FocusRequester,
    brightnessFocusRequester: FocusRequester,
    settingsFocusRequester: FocusRequester,
    brightnessMode: Boolean,
    brightnessValue: Float,
    settingsActive: Boolean,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBrightness: () -> Unit,
    onChangeBrightness: (Float) -> Unit,
    onCloseBrightness: () -> Unit,
    onBackToList: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (playback.overlayRightText.isNotBlank()) {
            Text(
                text = playback.overlayRightText,
                color = Color.White,
                style = PlayerTitleStyle.copy(fontSize = 48.sp, lineHeight = 54.sp),
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 36.dp, top = 20.dp),
            )
        }

        errorText?.let { message ->
            Text(
                text = message,
                color = Color.White,
                style = PlayerMetaStyle.copy(fontSize = 15.sp, lineHeight = 19.sp),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.64f))
                    .border(BorderStroke(1.dp, SmartVisionColors.Error.copy(alpha = 0.82f)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }

        LiveTvBottomGlassBanner(
            playback = playback,
            firstActionFocusRequester = firstActionFocusRequester,
            brightnessFocusRequester = brightnessFocusRequester,
            settingsFocusRequester = settingsFocusRequester,
            brightnessMode = brightnessMode,
            brightnessValue = brightnessValue,
            settingsActive = settingsActive,
            isFavorite = isFavorite,
            onFavorite = onFavorite,
            onOpenSettings = onOpenSettings,
            onOpenBrightness = onOpenBrightness,
            onChangeBrightness = onChangeBrightness,
            onCloseBrightness = onCloseBrightness,
            onBackToList = onBackToList,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun LiveTvBottomGlassBanner(
    playback: FullScreenPlayback,
    firstActionFocusRequester: FocusRequester,
    brightnessFocusRequester: FocusRequester,
    settingsFocusRequester: FocusRequester,
    brightnessMode: Boolean,
    brightnessValue: Float,
    settingsActive: Boolean,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBrightness: () -> Unit,
    onChangeBrightness: (Float) -> Unit,
    onCloseBrightness: () -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(85.dp)
            .background(PlayerOverlaySurface.copy(alpha = 0.65f))
            .padding(start = 12.dp, end = 18.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveTvChannelLogo(
            title = playback.title,
            imageUrl = playback.imageUrl,
            modifier = Modifier.size(width = 76.dp, height = 42.dp),
        )
        Box(
            modifier = Modifier
                .padding(start = 13.dp, end = 20.dp)
                .width(1.dp)
                .height(46.dp)
                .background(Color.White.copy(alpha = 0.20f)),
        )
        LiveTvChannelInfo(
            playback = playback,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        if (brightnessMode) {
            PlayerBrightnessSlider(
                value = brightnessValue,
                onChange = onChangeBrightness,
                onClose = onCloseBrightness,
                modifier = Modifier.width(260.dp),
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveTvActionButton(
                    label = if (isFavorite) "Remove favorite" else "Favorite",
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    focusRequester = firstActionFocusRequester,
                    selected = isFavorite,
                    onClick = onFavorite,
                )
                LiveTvActionButton(
                    label = "Brightness",
                    icon = Icons.Outlined.WbSunny,
                    focusRequester = brightnessFocusRequester,
                    active = brightnessMode,
                    onClick = onOpenBrightness,
                )
                LiveTvActionButton(
                    label = "Settings",
                    icon = Icons.Outlined.Settings,
                    focusRequester = settingsFocusRequester,
                    active = settingsActive,
                    onClick = onOpenSettings,
                )
                LiveTvActionButton(
                    label = "Exit fullscreen",
                    icon = Icons.Outlined.FullscreenExit,
                    onClick = onBackToList,
                )
            }
        }
    }
}

@Composable
private fun LiveTvChannelLogo(
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(end = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun LiveTvChannelInfo(
    playback: FullScreenPlayback,
    modifier: Modifier = Modifier,
) {
    val currentProgram = playback.epgPrograms.firstOrNull { it.isCurrent }
    val secondary = currentProgram
        ?.let { listOf(it.timeRange, it.title).filter { value -> value.isNotBlank() }.joinToString("  |  ") }
        ?: playback.status.takeUnless { it.equals("Direct", ignoreCase = true) }
        ?: playback.subtitle

    Column(modifier = modifier) {
        Text(
            text = playback.title,
            color = Color.White,
            style = PlayerTitleStyle.copy(fontSize = 28.sp, lineHeight = 34.sp),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = secondary,
            color = Color.White.copy(alpha = 0.76f),
            style = PlayerMetaStyle.copy(fontSize = 12.sp, lineHeight = 15.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LiveTvActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    selected: Boolean = false,
    active: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val iconColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.White.copy(alpha = 0.28f)
            selected -> PlayerFavoriteRed
            focused || active -> PlayerNeonBlue
            else -> Color.White
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "liveTvActionIcon",
    )
    val glowColor = if (selected) PlayerFavoriteRed else PlayerNeonBlue
    val requesterModifier = focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier

    Box(
        modifier = modifier
            .size(48.dp)
            .then(requesterModifier)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .focusProperties { canFocus = enabled }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(enabled = enabled, interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        if (enabled && (focused || active || selected)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = glowColor.copy(alpha = if (selected) 0.34f else 0.42f),
                modifier = Modifier.size(42.dp),
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
internal fun LiveTvEpgSidePanel(
    programs: List<FullScreenEpgProgram>,
    onClose: () -> Unit,
) {
    val closeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        closeFocusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(430.dp)
                .padding(top = 96.dp, end = 30.dp, bottom = 174.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.42f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(18.dp))
                .padding(20.dp),
        ) {
            Text(
                text = "EPG",
                color = Color.White,
                style = PlayerTitleStyle.copy(fontSize = 22.sp, lineHeight = 27.sp),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                programs.forEach { program ->
                    LiveTvEpgProgramRow(program)
                }
            }
            Spacer(Modifier.height(14.dp))
            TvButton(
                text = "Close",
                onClick = onClose,
                focusRequester = closeFocusRequester,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
            )
        }
    }
}

@Composable
private fun LiveTvEpgProgramRow(program: FullScreenEpgProgram) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (program.isCurrent) PlayerNeonBlue.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f))
            .border(
                BorderStroke(1.dp, if (program.isCurrent) PlayerNeonBlue.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.08f)),
                RoundedCornerShape(10.dp),
            )
            .padding(12.dp),
    ) {
        Text(
            text = program.timeRange,
            color = if (program.isCurrent) PlayerNeonBlue else Color.White.copy(alpha = 0.62f),
            style = PlayerMetaStyle.copy(fontSize = 12.sp, lineHeight = 15.sp),
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = program.title,
            color = Color.White,
            style = PlayerTitleStyle.copy(fontSize = 15.sp, lineHeight = 19.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (program.description.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = program.description,
                color = Color.White.copy(alpha = 0.68f),
                style = PlayerMetaStyle.copy(fontSize = 12.sp, lineHeight = 15.sp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun LiveTvSettingsPanel(
    selectedAspectMode: LiveAspectMode,
    audioSummary: String,
    subtitleSummary: String,
    onSelectAspectMode: (LiveAspectMode) -> Unit,
    onOpenSubtitles: () -> Unit,
    onClose: () -> Unit,
) {
    val autoFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { autoFocusRequester.requestFocus() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(500.dp)
                .height(150.dp)
                .padding(end = 35.dp, bottom = 36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.Black.copy(alpha = 0.62f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(9.dp))
                .padding(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(horizontal = 12.dp, vertical = 15.dp),
                verticalArrangement = Arrangement.spacedBy(17.dp),
            ) {
                LiveTvSettingsSectionLabel(icon = Icons.Default.Tv, text = "Image")
                LiveTvSettingsSectionLabel(icon = Icons.Default.VolumeUp, text = "Audio")
                LiveTvSettingsSectionLabel(icon = Icons.Default.List, text = "Subtitles")
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(PlayerOverlaySurface.copy(alpha = 0.62f))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LiveAspectModes.forEach { mode ->
                        SettingsControlButton(
                            text = mode.label,
                            selected = mode.label == selectedAspectMode.label,
                            focusRequester = autoFocusRequester.takeIf { mode.label == "Auto" },
                            onClick = { onSelectAspectMode(mode) },
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp),
                        )
                    }
                }
                SettingsValueRow(
                    label = "Audio track",
                    value = audioSummary,
                    onClick = { },
                )
                SettingsValueRow(
                    label = "Subtitle track",
                    value = subtitleSummary,
                    onClick = onOpenSubtitles,
                )
            }
        }
    }
}

@Composable
private fun LiveTvSettingsSectionLabel(
    icon: ImageVector,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(12.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.92f),
            style = PlayerMetaStyle.copy(fontSize = 8.sp, lineHeight = 10.sp),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SettingsControlButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> PlayerNeonBlue.copy(alpha = 0.74f)
            selected -> PlayerNeonBlue.copy(alpha = 0.42f)
            else -> Color.White.copy(alpha = 0.10f)
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "settingsControlBackground",
    )
    Box(
        modifier = modifier
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.02f,
                glowColor = PlayerNeonBlue,
                cornerRadius = 4.dp,
            )
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .border(
                BorderStroke(1.dp, if (focusState.isFocused || selected) PlayerNeonBlue else Color.White.copy(alpha = 0.12f)),
                RoundedCornerShape(4.dp),
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = PlayerMetaStyle.copy(fontSize = 7.sp, lineHeight = 9.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingsValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(27.dp)
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                focusedScale = 1.015f,
                glowColor = PlayerNeonBlue,
                cornerRadius = 5.dp,
            )
            .clip(RoundedCornerShape(5.dp))
            .background(Color.White.copy(alpha = if (focusState.isFocused) 0.18f else 0.10f))
            .border(
                BorderStroke(1.dp, if (focusState.isFocused) PlayerNeonBlue else Color.White.copy(alpha = 0.12f)),
                RoundedCornerShape(5.dp),
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color.White,
            style = PlayerMetaStyle.copy(fontSize = 8.sp, lineHeight = 10.sp),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.72f),
            style = PlayerMetaStyle.copy(fontSize = 7.sp, lineHeight = 9.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = " >",
            color = Color.White.copy(alpha = 0.82f),
            style = PlayerMetaStyle.copy(fontSize = 10.sp, lineHeight = 10.sp),
            fontWeight = FontWeight.Bold,
        )
    }
}

