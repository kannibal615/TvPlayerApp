package com.smartvision.svplayer.feature.series

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.designsystem.CategoryRow
import com.smartvision.svplayer.core.designsystem.FocusableButton
import com.smartvision.svplayer.core.designsystem.GlassPanel
import com.smartvision.svplayer.core.designsystem.MediaListItem
import com.smartvision.svplayer.core.designsystem.MediaThumb
import com.smartvision.svplayer.core.designsystem.SVColors
import com.smartvision.svplayer.core.designsystem.SearchFieldPlaceholder
import com.smartvision.svplayer.core.ui.viewModelFactory

@Composable
fun SeriesRoute(openPlayer: (Int) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: SeriesViewModel = viewModel(
        factory = viewModelFactory { SeriesViewModel(container.catalogRepository) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SeriesScreen(
        state = state,
        onCategory = viewModel::selectCategory,
        onSeries = {
            viewModel.selectSeries(it)
            viewModel.loadEpisodes(it)
        },
        onLoadEpisodes = { state.selectedSeries?.let(viewModel::loadEpisodes) },
        onFavorite = { state.selectedSeries?.let(viewModel::toggleFavorite) },
        onEpisode = openPlayer,
    )
}

@Composable
private fun SeriesScreen(
    state: SeriesUiState,
    onCategory: (com.smartvision.svplayer.domain.model.Category) -> Unit,
    onSeries: (com.smartvision.svplayer.domain.model.TvSeries) -> Unit,
    onLoadEpisodes: () -> Unit,
    onFavorite: () -> Unit,
    onEpisode: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = SVColors.Blue)
            Text(
                text = "Series",
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
                                icon = Icons.Default.VideoLibrary,
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
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = SVColors.Blue)
                        Text(
                            text = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name ?: "Series",
                            color = SVColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                        Text(
                            text = "   ${state.series.size} series",
                            color = SVColors.TextSecondary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.series, key = { it.seriesId }) { item ->
                            MediaListItem(
                                number = item.number.toString().padStart(3, '0'),
                                title = item.title,
                                subtitle = listOfNotNull(item.year, item.genre, item.seasonsCount?.let { "$it saisons" }).joinToString(" | ").ifBlank { item.categoryName },
                                imageUrl = item.posterUrl,
                                selected = item.seriesId == state.selectedSeries?.seriesId,
                                onClick = { onSeries(item) },
                                accent = SVColors.Blue,
                                trailing = item.rating,
                            )
                        }
                    }
                }
            }
            SeriesPreviewPanel(
                state = state,
                onLoadEpisodes = onLoadEpisodes,
                onFavorite = onFavorite,
                onEpisode = onEpisode,
                modifier = Modifier
                    .weight(4f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun SeriesPreviewPanel(
    state: SeriesUiState,
    onLoadEpisodes: () -> Unit,
    onFavorite: () -> Unit,
    onEpisode: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedSeries
    GlassPanel(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
        ) {
            if (selected == null) {
                Text("Selectionnez une serie", color = SVColors.TextSecondary)
                return@GlassPanel
            }
            MediaThumb(
                imageUrl = selected.posterUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                selected.title,
                color = SVColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(selected.year, selected.genre, selected.rating).joinToString(" | ").ifBlank { selected.categoryName },
                color = SVColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                selected.plot ?: "Description indisponible. Les saisons et episodes sont charges via Xtream lorsque la serie est ouverte.",
                color = SVColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FocusableButton(
                    text = if (state.loadingEpisodes) "Chargement..." else "Voir saisons",
                    icon = Icons.Default.VideoLibrary,
                    onClick = onLoadEpisodes,
                    accent = SVColors.Blue,
                    modifier = Modifier.weight(1f),
                )
                FocusableButton(
                    text = "Favori",
                    icon = Icons.Default.Favorite,
                    onClick = onFavorite,
                    accent = SVColors.Purple,
                    modifier = Modifier.weight(1f),
                )
                FocusableButton(
                    text = "Infos",
                    icon = Icons.Default.Info,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Episodes",
                color = SVColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (state.episodes.isEmpty()) {
                    item {
                        Text(
                            text = if (state.loadingEpisodes) "Chargement des episodes..." else "Ouvrez la serie pour charger les saisons.",
                            color = SVColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                } else {
                    items(state.episodes, key = { it.episodeId }) { episode ->
                        MediaListItem(
                            number = "S${episode.seasonNumber}E${episode.episodeNumber}",
                            title = episode.title,
                            subtitle = episode.duration ?: "Episode",
                            imageUrl = null,
                            selected = false,
                            onClick = { onEpisode(episode.episodeId) },
                            accent = SVColors.Blue,
                            trailing = "Lire",
                        )
                    }
                }
            }
        }
    }
}
