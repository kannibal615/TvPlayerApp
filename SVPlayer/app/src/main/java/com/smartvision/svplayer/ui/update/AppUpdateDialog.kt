package com.smartvision.svplayer.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.smartvision.svplayer.data.update.AppUpdateInfo
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.TvDialogSurface
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay

@Composable
fun AppUpdateDialog(
    update: AppUpdateInfo,
    installing: Boolean,
    errorMessage: String?,
    strings: SmartVisionStrings,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val installFocusRequester = remember { FocusRequester() }

    LaunchedEffect(update.versionCode, installing) {
        if (!installing) {
            withFrameNanos { }
            delay(80)
            runCatching { installFocusRequester.requestFocus() }
        }
    }

    TvDialogSurface(
        title = strings.updateAvailableTitle,
        onDismiss = { if (!update.mandatory && !installing) onDismiss() },
        width = 560.dp,
        icon = Icons.Default.SystemUpdate,
        dismissOnClickOutside = !update.mandatory && !installing,
    ) {
            Text(
                text = strings.updateDialogMessage.format(update.versionName),
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Body,
            )
            if (!update.releaseNotes.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = update.releaseNotes,
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Label,
                )
            }
            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = errorMessage,
                    color = SmartVisionColors.Warning,
                    style = SmartVisionType.Label,
                )
            }
            Spacer(Modifier.height(26.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TvButton(
                    text = if (installing) strings.downloadInProgress else strings.installUpdate,
                    onClick = onInstall,
                    enabled = !installing,
                    leadingIcon = Icons.Default.SystemUpdate,
                    focusRequester = installFocusRequester,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 22.dp),
                )
                if (!update.mandatory) {
                    TvButton(
                        text = strings.later,
                        onClick = onDismiss,
                        enabled = !installing,
                        variant = TvButtonVariant.Secondary,
                        leadingIcon = Icons.Default.Close,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 22.dp),
                    )
                }
            }
    }
}
