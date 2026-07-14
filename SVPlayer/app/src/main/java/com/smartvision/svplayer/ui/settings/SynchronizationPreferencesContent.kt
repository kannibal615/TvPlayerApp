package com.smartvision.svplayer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import com.smartvision.svplayer.core.config.XtreamAccount
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType

@Composable
internal fun SynchronizationPreferencesContent(
    settings: PlayerSettings,
    activeAccount: XtreamAccount?,
    strings: SmartVisionStrings,
    onSetAutostartEnabled: (Boolean) -> Unit,
    onSetBackgroundSyncEnabled: (Boolean) -> Unit,
    onSetSyncFrequency: (String) -> Unit,
) {
    Text(strings.launchOnStartup, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Label)
    Spacer(Modifier.height(7.dp))
    SyncPreferenceChoice(
        values = listOf("enabled" to strings.enabled, "disabled" to strings.disabled),
        selected = if (settings.autostartEnabled) "enabled" else "disabled",
        onSelected = { onSetAutostartEnabled(it == "enabled") },
    )
    Text(strings.backgroundSync, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Label)
    Spacer(Modifier.height(7.dp))
    SyncPreferenceChoice(
        values = listOf("enabled" to strings.enabled, "disabled" to strings.disabled),
        selected = if (settings.backgroundSyncEnabled) "enabled" else "disabled",
        onSelected = { onSetBackgroundSyncEnabled(it == "enabled") },
    )
    Text(strings.automaticSync, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Label)
    Spacer(Modifier.height(7.dp))
    SyncPreferenceChoice(
        values = synchronizationFrequencyOptions(strings),
        selected = settings.syncFrequency,
        onSelected = onSetSyncFrequency,
    )
    SyncInfoRow(strings.currentFrequency, settings.syncFrequency.localizedSynchronizationFrequency(strings))
    SyncInfoRow(strings.activeAccount, activeAccount?.let { "${it.name} - ${it.username}" } ?: strings.none)
    SyncInfoRow(strings.activeServer, activeAccount?.host ?: strings.notConfigured)
}

@Composable
private fun SyncPreferenceChoice(
    values: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        values.chunked(3).forEach { rowValues ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowValues.forEach { (value, label) ->
                    TvButton(
                        text = label,
                        onClick = { onSelected(value) },
                        selected = value == selected,
                        variant = if (value == selected) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                        modifier = Modifier.weight(1f).height(40.dp),
                    )
                }
                repeat(3 - rowValues.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    Spacer(Modifier.height(18.dp))
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

@Composable
private fun SyncInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, modifier = Modifier.weight(1f))
        Text(value, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Caption, fontWeight = FontWeight.SemiBold)
    }
}
