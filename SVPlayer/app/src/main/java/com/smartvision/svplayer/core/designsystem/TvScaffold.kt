package com.smartvision.svplayer.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.core.navigation.SVRoute
import com.smartvision.svplayer.domain.model.SyncStatus

@Composable
fun TvScaffold(
    currentRoute: SVRoute,
    syncStatus: SyncStatus,
    onNavigate: (SVRoute) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    val navItems = listOf(
        NavItem(SVRoute.Home, Icons.Default.Home, "Accueil"),
        NavItem(SVRoute.Live, Icons.Default.Tv, "Live TV"),
        NavItem(SVRoute.Movies, Icons.Default.Movie, "Films"),
        NavItem(SVRoute.Series, Icons.Default.VideoLibrary, "Séries"),
        NavItem(SVRoute.Account, Icons.Default.Person, "Mon compte"),
        NavItem(SVRoute.Settings, Icons.Default.Settings, "Paramètres"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0B2844), SVColors.Background, SVColors.BackgroundDeep),
                    radius = 1600f,
                ),
            )
            .padding(horizontal = 48.dp, vertical = 24.dp),
    ) {
        TopHeader(
            currentRoute = currentRoute,
            items = navItems,
            onNavigate = onNavigate,
            syncStatus = syncStatus,
            onSync = onSync,
            onSettings = onSettings,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
        ) {
            content()
        }
    }
}
