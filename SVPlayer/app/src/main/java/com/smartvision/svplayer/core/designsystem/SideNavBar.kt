package com.smartvision.svplayer.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.core.navigation.SVRoute

data class NavItem(
    val route: SVRoute,
    val icon: ImageVector,
    val contentDescription: String,
)

@Composable
fun SideNavBar(
    currentRoute: SVRoute,
    items: List<NavItem>,
    onNavigate: (SVRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(96.dp)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    listOf(SVColors.BackgroundDeep, SVColors.Surface.copy(alpha = 0.72f)),
                ),
            )
            .border(BorderStroke(1.dp, SVColors.Border.copy(alpha = 0.55f))),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(38.dp))
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(SVColors.SurfaceLight, RoundedCornerShape(10.dp))
                .border(BorderStroke(1.dp, SVColors.Border), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SmartVisionMark(Modifier.size(44.dp))
        }
        Spacer(Modifier.height(42.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.forEach { item ->
                val selected = item.route.topLevelRoute == currentRoute.topLevelRoute
                FocusableCard(
                    onClick = { onNavigate(item.route) },
                    selected = selected,
                    accent = SVColors.Cyan,
                    modifier = Modifier.size(width = 84.dp, height = 56.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { focused ->
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.contentDescription,
                        tint = if (selected || focused) SVColors.TextPrimary else SVColors.TextSecondary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SmartVisionMark(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        androidx.compose.material3.Text(
            text = "SV",
            color = SVColors.TextPrimary,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
        )
    }
}
