package com.smartvision.svplayer.feature.movies

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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Theaters
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
fun MoviesRoute(openPlayer: (Int) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: MoviesViewModel = viewModel(
        factory = viewModelFactory { MoviesViewModel(container.catalogRepository) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MoviesScreen(
        state = state,
        onCategory = viewModel::selectCategory,
        onMovie = viewModel::selectMovie,
        onFavorite = { state.selectedMovie?.let(viewModel::toggleFavorite) },
        onPlay = { state.selectedMovie?.streamId?.let(openPlayer) },
    )
}

@Composable
private fun MoviesScreen(
    state: MoviesUiState,
    onCategory: (com.smartvision.svplayer.domain.model.Category) -> Unit,
    onMovie: (com.smartvision.svplayer.domain.model.Movie) -> Unit,
    onFavorite: () -> Unit,
    onPlay: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Movie, contentDescription = null, tint = SVColors.Purple)
            Text(
                text = "Films",
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
                                icon = Icons.Default.Theaters,
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
                        Icon(Icons.Default.Theaters, contentDescription = null, tint = SVColors.Purple)
                        Text(
                            text = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name ?: "Films",
                            color = SVColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                        Text(
                            text = "   ${state.movies.size} films",
                            color = SVColors.TextSecondary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.movies, key = { it.streamId }) { movie ->
                            MediaListItem(
                                number = movie.number.toString().padStart(3, '0'),
                                title = movie.title,
                                subtitle = listOfNotNull(movie.year, movie.genre, movie.duration).joinToString(" | ").ifBlank { movie.categoryName },
                                imageUrl = movie.posterUrl,
                                selected = movie.streamId == state.selectedMovie?.streamId,
                                onClick = {
                                    onMovie(movie)
                                    onPlay()
                                },
                                accent = SVColors.Purple,
                                trailing = movie.rating,
                            )
                        }
                    }
                }
            }
            val selected = state.selectedMovie
            if (selected != null) {
                PreviewPanel(
                    modifier = Modifier
                        .weight(4f)
                        .fillMaxHeight(),
                    title = selected.title,
                    subtitle = listOfNotNull(selected.year, selected.genre).joinToString(" | ").ifBlank { selected.categoryName },
                    description = selected.plot ?: "Resume indisponible pour le moment. La fiche sera enrichie apres synchronisation Xtream.",
                    imageUrl = selected.posterUrl,
                    status = selected.rating ?: "VOD",
                    primaryAction = "Lire",
                    onPrimary = onPlay,
                    onFavorite = onFavorite,
                    accent = SVColors.Purple,
                    metadata = listOf(
                        Icons.Default.Movie to (selected.year ?: "Film"),
                        Icons.Default.Schedule to (selected.duration ?: "Duree N/A"),
                        Icons.Default.Star to (selected.rating ?: "Note N/A"),
                    ),
                )
            }
        }
    }
}
