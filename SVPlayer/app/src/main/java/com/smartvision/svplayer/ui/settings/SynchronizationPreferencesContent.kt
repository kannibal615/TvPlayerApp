package com.smartvision.svplayer.ui.settings

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.TvSectionCard
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType

@Composable
internal fun SynchronizationPreferencesContent(
    settings: PlayerSettings,
    activeProfileName: String,
    activeServer: String,
    lastSynchronization: String,
    strings: SmartVisionStrings,
    firstControlFocusRequester: FocusRequester,
    menuFocusRequester: FocusRequester,
    onSetAutostartEnabled: (Boolean) -> Unit,
    onSetBackgroundSyncEnabled: (Boolean) -> Unit,
    onSetSyncFrequency: (String) -> Unit,
) {
    val backgroundFocusRequester = remember { FocusRequester() }
    val frequencyRequesters = remember { List(5) { FocusRequester() } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TvSectionCard(
            title = strings.syncGeneralOptions,
            icon = Icons.Default.Settings,
            modifier = Modifier
                .weight(1.08f)
                .fillMaxHeight(),
        ) {
            SyncPreferenceToggleRow(
                title = strings.launchOnStartup,
                subtitle = strings.launchOnStartupDescription,
                checked = settings.autostartEnabled,
                focusRequester = firstControlFocusRequester,
                modifier = Modifier.focusProperties {
                    left = menuFocusRequester
                    right = frequencyRequesters[0]
                    down = backgroundFocusRequester
                },
                onCheckedChange = onSetAutostartEnabled,
            )
            SyncDivider()
            SyncPreferenceToggleRow(
                title = strings.backgroundSync,
                subtitle = strings.backgroundSyncDescription,
                checked = settings.backgroundSyncEnabled,
                focusRequester = backgroundFocusRequester,
                modifier = Modifier.focusProperties {
                    left = menuFocusRequester
                    right = frequencyRequesters[2]
                    up = firstControlFocusRequester
                },
                onCheckedChange = onSetBackgroundSyncEnabled,
            )
        }

        TvSectionCard(
            title = strings.syncFrequencyTitle,
            icon = Icons.Default.Schedule,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            val options = synchronizationFrequencyOptions(strings)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FrequencyButton(
                    option = options[0],
                    selected = settings.syncFrequency == options[0].first,
                    focusRequester = frequencyRequesters[0],
                    modifier = Modifier.weight(1f).focusProperties {
                        left = firstControlFocusRequester
                        right = frequencyRequesters[1]
                        down = frequencyRequesters[2]
                    },
                    onSelected = onSetSyncFrequency,
                )
                FrequencyButton(
                    option = options[1],
                    selected = settings.syncFrequency == options[1].first,
                    focusRequester = frequencyRequesters[1],
                    modifier = Modifier.weight(1f).focusProperties {
                        left = frequencyRequesters[0]
                        down = frequencyRequesters[3]
                    },
                    onSelected = onSetSyncFrequency,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FrequencyButton(
                    option = options[2],
                    selected = settings.syncFrequency == options[2].first,
                    focusRequester = frequencyRequesters[2],
                    modifier = Modifier.weight(1f).focusProperties {
                        left = backgroundFocusRequester
                        right = frequencyRequesters[3]
                        up = frequencyRequesters[0]
                        down = frequencyRequesters[4]
                    },
                    onSelected = onSetSyncFrequency,
                )
                FrequencyButton(
                    option = options[3],
                    selected = settings.syncFrequency == options[3].first,
                    focusRequester = frequencyRequesters[3],
                    modifier = Modifier.weight(1f).focusProperties {
                        left = frequencyRequesters[2]
                        up = frequencyRequesters[1]
                        down = frequencyRequesters[4]
                    },
                    onSelected = onSetSyncFrequency,
                )
            }
            Spacer(Modifier.height(10.dp))
            FrequencyButton(
                option = options[4],
                selected = settings.syncFrequency == options[4].first,
                focusRequester = frequencyRequesters[4],
                modifier = Modifier.fillMaxWidth().focusProperties {
                    left = menuFocusRequester
                    up = frequencyRequesters[2]
                },
                onSelected = onSetSyncFrequency,
            )
        }
    }

    Spacer(Modifier.height(10.dp))

    TvSectionCard(
        title = strings.syncSummary,
        icon = Icons.Default.Sync,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            SyncSummaryItem(
                icon = Icons.Default.Schedule,
                label = strings.currentFrequency,
                value = settings.syncFrequency.localizedSynchronizationFrequency(strings),
                modifier = Modifier.weight(1f),
            )
            SyncSummaryItem(
                icon = Icons.Default.Check,
                label = strings.lastSynchronization,
                value = lastSynchronization,
                valueColor = SmartVisionColors.Success,
                modifier = Modifier.weight(1f),
            )
        }
        SyncDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            SyncSummaryItem(
                icon = Icons.Default.Person,
                label = strings.activeAccount,
                value = activeProfileName,
                modifier = Modifier.weight(1f),
            )
            SyncSummaryItem(
                icon = Icons.Default.Dns,
                label = strings.activeServer,
                value = activeServer,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SyncPreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Body, fontWeight = FontWeight.Medium, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        SyncToggle(
            checked = checked,
            focusRequester = focusRequester,
            modifier = modifier,
            onClick = { onCheckedChange(!checked) },
        )
    }
}

@Composable
private fun SyncToggle(
    checked: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val color = if (checked) Color(0xFF20D46B) else Color(0xFFE33A3A)
    Box(
        modifier = modifier
            .size(width = 54.dp, height = 28.dp)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                glowColor = color,
                cornerRadius = 50.dp,
            )
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.9f))
            .border(
                BorderStroke(if (focusState.isFocused) focusStyle.borderWidth else 1.dp, if (focusState.isFocused) focusStyle.accent else color),
                RoundedCornerShape(50),
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 6.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            text = if (checked) "ON" else "OFF",
            color = Color.White,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FrequencyButton(
    option: Pair<String, String>,
    selected: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier,
    onSelected: (String) -> Unit,
) {
    TvButton(
        text = option.second,
        onClick = { onSelected(option.first) },
        selected = selected,
        variant = if (selected) TvButtonVariant.Primary else TvButtonVariant.Secondary,
        leadingIcon = if (selected) Icons.Default.Check else null,
        focusRequester = focusRequester,
        modifier = modifier.height(44.dp),
    )
}

@Composable
private fun SyncSummaryItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = SmartVisionColors.CyanAccent,
) {
    Row(modifier = modifier.height(43.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = SmartVisionColors.TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            color = valueColor,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SyncDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SmartVisionColors.Border.copy(alpha = 0.72f)),
    )
}

private fun synchronizationFrequencyOptions(strings: SmartVisionStrings): List<Pair<String, String>> = listOf(
    "24h" to strings.sync24h,
    "48h" to strings.sync48h,
    "A chaque demarrage" to strings.syncOnStartup,
    "Manuelle" to strings.syncManual,
    "Jamais" to strings.syncNever,
)

private fun String.localizedSynchronizationFrequency(strings: SmartVisionStrings): String =
    synchronizationFrequencyOptions(strings).firstOrNull { it.first == this }?.second ?: this
