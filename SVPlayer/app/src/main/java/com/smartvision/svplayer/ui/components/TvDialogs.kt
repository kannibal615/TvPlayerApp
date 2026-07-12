package com.smartvision.svplayer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay

enum class TvDialogTone {
    Default,
    Warning,
    Destructive,
}

@Composable
fun TvDialogSurface(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 560.dp,
    tone: TvDialogTone = TvDialogTone.Default,
    icon: ImageVector? = null,
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val accent = tone.accentColor()
    val resolvedIcon = icon ?: tone.defaultIcon()
    val shape = RoundedCornerShape(SmartVisionDimensions.CardRadius)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = modifier
                    .width(width)
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                SmartVisionColors.SurfaceElevated.copy(alpha = 0.98f),
                                SmartVisionColors.Surface.copy(alpha = 0.99f),
                            ),
                        ),
                    )
                    .border(BorderStroke(1.5.dp, accent.copy(alpha = 0.78f)), shape)
                    .padding(horizontal = 28.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f))
                            .border(BorderStroke(1.dp, accent.copy(alpha = 0.48f)), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = resolvedIcon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = title,
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.TitleS,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(18.dp))
                content()
            }
        }
    }
}

@Composable
fun TvConfirmationDialog(
    title: String,
    confirmText: String,
    cancelText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    itemLabel: String? = null,
    tone: TvDialogTone = TvDialogTone.Destructive,
    icon: ImageVector? = null,
    confirmEnabled: Boolean = true,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        delay(80)
        runCatching { cancelFocusRequester.requestFocus() }
    }

    TvDialogSurface(
        title = title,
        onDismiss = onDismiss,
        modifier = modifier,
        tone = tone,
        icon = icon,
    ) {
        if (!itemLabel.isNullOrBlank()) {
            Text(
                text = itemLabel,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SmartVisionDimensions.ButtonRadius))
                    .background(SmartVisionColors.Background.copy(alpha = 0.62f))
                    .border(
                        BorderStroke(1.dp, SmartVisionColors.Border),
                        RoundedCornerShape(SmartVisionDimensions.ButtonRadius),
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
            if (!message.isNullOrBlank()) Spacer(Modifier.height(12.dp))
        }
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Body,
                textAlign = TextAlign.Start,
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!secondaryText.isNullOrBlank() && onSecondary != null) {
                TvButton(
                    text = secondaryText,
                    onClick = onSecondary,
                    variant = TvButtonVariant.Primary,
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    modifier = Modifier.height(44.dp),
                )
                Spacer(Modifier.width(12.dp))
            }
            TvButton(
                text = cancelText,
                onClick = onDismiss,
                focusRequester = cancelFocusRequester,
                variant = TvButtonVariant.Secondary,
                contentPadding = PaddingValues(horizontal = 22.dp),
                modifier = Modifier.height(44.dp),
            )
            Spacer(Modifier.width(12.dp))
            TvButton(
                text = confirmText,
                onClick = onConfirm,
                enabled = confirmEnabled,
                leadingIcon = icon ?: tone.defaultIcon(),
                variant = when (tone) {
                    TvDialogTone.Destructive -> TvButtonVariant.Danger
                    TvDialogTone.Warning -> TvButtonVariant.Exit
                    TvDialogTone.Default -> TvButtonVariant.Primary
                },
                contentPadding = PaddingValues(horizontal = 22.dp),
                modifier = Modifier.height(44.dp),
            )
        }
    }
}

private fun TvDialogTone.accentColor(): Color = when (this) {
    TvDialogTone.Default -> SmartVisionColors.CyanAccent
    TvDialogTone.Warning -> SmartVisionColors.Warning
    TvDialogTone.Destructive -> SmartVisionColors.Error
}

private fun TvDialogTone.defaultIcon(): ImageVector = when (this) {
    TvDialogTone.Default -> Icons.Default.Info
    TvDialogTone.Warning -> Icons.Default.Warning
    TvDialogTone.Destructive -> Icons.Default.Delete
}
