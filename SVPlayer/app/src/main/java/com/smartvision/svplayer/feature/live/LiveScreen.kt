package com.smartvision.svplayer.feature.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tv
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
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.designsystem.CategoryRow
import com.smartvision.svplayer.core.designsystem.GlassPanel
import com.smartvision.svplayer.core.designsystem.MediaListItem
import com.smartvision.svplayer.core.designsystem.PreviewPanel
import com.smartvision.svplayer.core.designsystem.SVColors
import com.smartvision.svplayer.core.designsystem.SearchFieldPlaceholder
import com.smartvision.svplayer.core.ui.viewModelFactory

@Composable
fun LiveRoute(openPlayer: (Int) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: LiveViewModel = viewModel(
        factory = viewModelFactory { LiveViewModel(container.catalogRepository) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LiveScreen(
        state = state,
        onCategory = viewModel::selectCategory,
        onChannel = viewModel::selectChannel,
        onFavorite = { state.selectedChannel?.let(viewModel::toggleFavorite) },
        onWatch = { state.selectedChannel?.streamId?.let(openPlayer) },
    )
}

@Composable
private fun LiveScreen(
    state: LiveUiState,
    onCategory: (com.smartvision.svplayer.domain.model.Category) -> Unit,
    onChannel: (com.smartvision.svplayer.domain.model.LiveChannel) -> Unit,
    onFavorite: () -> Unit,
    onWatch: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tv, contentDescription = null, tint = SVColors.Cyan)
            Spacer(Modifier.height(1.dp))
            Text(
                text = "Live TV",
                color = SVColors.TextPrimary,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            GlassPanel(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    SearchFieldPlaceholder("Rechercher une categorie", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.categories, key = { it.id }) { category ->
                            CategoryRow(
                                category = category,
                                selected = category.id == state.selectedCategoryId,
                                icon = Icons.Default.EmojiEvents,
                                onClick = { onCategory(category) },
                            )
                        }
                    }
                }
            }

            GlassPanel(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxHeight(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SVColors.Cyan)
                        Text(
                            text = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name ?: "Sport",
                            color = SVColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                        Text(
                            text = "   ${state.channels.size} chaines",
                            color = SVColors.TextSecondary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.channels, key = { it.streamId }) { channel ->
                            MediaListItem(
                                number = channel.number.toString().padStart(3, '0'),
                                title = channel.name,
                                subtitle = channel.currentProgram ?: channel.categoryName,
                                imageUrl = channel.logoUrl,
                                selected = channel.streamId == state.selectedChannel?.streamId,
                                onClick = {
                                    onChannel(channel)
                                    onWatch()
                                },
                                trailing = if (channel.streamId == state.selectedChannel?.streamId) "EN COURS" else channel.timeRange,
                            )
                        }
                    }
                }
            }

            val selected = state.selectedChannel
            if (selected != null) {
                PreviewPanel(
                    modifier = Modifier
                        .weight(4f)
                        .fillMaxHeight(),
                    title = selected.name,
                    subtitle = selected.currentProgram ?: selected.categoryName,
                    description = "Suivez cette chaine en direct avec une lecture native Android TV. Les informations EPG seront enrichies lorsque la source les fournit.",
                    imageUrl = selected.logoUrl,
                    status = "EN COURS",
                    primaryAction = "Regarder",
                    onPrimary = onWatch,
                    onFavorite = onFavorite,
                    metadata = listOf(
                        Icons.Default.CalendarToday to "Aujourd'hui",
                        Icons.Default.Schedule to (selected.timeRange ?: "Live"),
                        Icons.Default.Tv to "HD",
                    ),
                )
            }
        }
    }
}
