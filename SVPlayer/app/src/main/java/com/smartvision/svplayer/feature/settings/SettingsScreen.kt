package com.smartvision.svplayer.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.designsystem.FocusableButton
import com.smartvision.svplayer.core.designsystem.GlassPanel
import com.smartvision.svplayer.core.designsystem.SVColors
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.domain.model.PlayerSettings

@Composable
fun SettingsRoute() {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory { SettingsViewModel(container.settingsRepository) },
    )
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsScreen(
        settings = settings,
        onDisplay = viewModel::setDisplaySize,
        onAnimations = viewModel::setAnimationsEnabled,
        onRatio = viewModel::setVideoRatio,
        onBuffer = viewModel::setBufferMode,
        onRetry = viewModel::setRetryEnabled,
        onClear = viewModel::clearLocalData,
    )
}

@Composable
private fun SettingsScreen(
    settings: PlayerSettings,
    onDisplay: (String) -> Unit,
    onAnimations: (Boolean) -> Unit,
    onRatio: (String) -> Unit,
    onBuffer: (String) -> Unit,
    onRetry: (Boolean) -> Unit,
    onClear: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = SVColors.Cyan)
            Text(
                text = "Parametres",
                color = SVColors.TextPrimary,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxSize()) {
            SettingsSection(title = "Interface", modifier = Modifier.weight(1f)) {
                OptionRow("Taille affichage", listOf("Normal", "Grand"), settings.displaySize, onDisplay)
                OptionRow("Animations", listOf("Activees", "Reduites"), if (settings.animationsEnabled) "Activees" else "Reduites") {
                    onAnimations(it == "Activees")
                }
                StaticRow("Theme", "Sombre")
            }
            SettingsSection(title = "Player", modifier = Modifier.weight(1f)) {
                OptionRow("Ratio video", listOf("Fit", "Fill", "Zoom"), settings.videoRatio, onRatio)
                OptionRow("Buffer", listOf("Standard", "Eleve"), settings.bufferMode, onBuffer)
                OptionRow("Retry auto", listOf("Active", "Desactive"), if (settings.retryEnabled) "Active" else "Desactive") {
                    onRetry(it == "Active")
                }
            }
            SettingsSection(title = "Synchronisation", modifier = Modifier.weight(1f)) {
                FocusableButton("Synchroniser maintenant", onClick = {}, icon = Icons.Default.Sync, modifier = Modifier.fillMaxWidth())
                FocusableButton("Vider base locale", onClick = onClear, icon = Icons.Default.Delete, accent = SVColors.Danger, modifier = Modifier.fillMaxWidth())
                StaticRow("Environnement", if (BuildConfig.DEBUG) "Debug" else "Release")
                StaticRow("Version", BuildConfig.VERSION_NAME)
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassPanel(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = SVColors.Cyan)
                Text(title, color = SVColors.TextPrimary, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 10.dp))
            }
            content()
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    values: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Column {
        Text(label, color = SVColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value ->
                FocusableButton(
                    text = value,
                    onClick = { onSelected(value) },
                    selected = value == selected,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StaticRow(label: String, value: String) {
    GlassPanel(modifier = Modifier.fillMaxWidth().height(70.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = SVColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
            Text(value, color = SVColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
        }
    }
}
