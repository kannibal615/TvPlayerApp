package com.smartvision.svplayer.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvision.svplayer.core.navigation.SVRoute

data class NavItem(
    val route: SVRoute,
    val icon: ImageVector,
    val label: String,
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
            .width(104.dp)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF020612), Color(0xFF03101E), Color(0xFF01040C)),
                ),
            )
            .border(BorderStroke(1.dp, SVColors.Border.copy(alpha = 0.55f))),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        SmartVisionMark(Modifier.size(42.dp))
        Spacer(Modifier.height(22.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.forEach { item ->
                val selected = item.route.topLevelRoute == currentRoute.topLevelRoute
                SideNavItem(
                    item = item,
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                )
            }
        }
    }
}

@Composable
private fun SideNavItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.035f else 1f, label = "navScale")
    val container by animateColorAsState(
        when {
            selected -> Color(0xFF042E36).copy(alpha = 0.82f)
            focused -> Color(0xFF0A2536).copy(alpha = 0.96f)
            else -> Color.Transparent
        },
        label = "navContainer",
    )
    val border by animateColorAsState(
        when {
            selected || focused -> SVColors.Cyan
            else -> Color.Transparent
        },
        label = "navBorder",
    )
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = Modifier
            .width(98.dp)
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                if (selected || focused) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(SVColors.Cyan.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.78f,
                        ),
                    )
                }
            }
            .clip(shape)
            .background(container)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, border), shape)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .focusable()
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (selected || focused) Color.White else SVColors.TextPrimary.copy(alpha = 0.92f),
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = item.label,
            color = if (selected || focused) SVColors.Cyan else SVColors.TextPrimary.copy(alpha = 0.92f),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp, lineHeight = 14.sp),
            fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun SmartVisionMark(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF101827), Color(0xFF050713), Color(0xFF11182A)),
                ),
            )
            .border(BorderStroke(1.dp, SVColors.Border.copy(alpha = 0.95f)), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = buildAnnotatedString {
                append("S")
                withStyle(
                    SpanStyle(
                        brush = Brush.linearGradient(listOf(SVColors.Cyan, SVColors.Purple)),
                    ),
                ) {
                    append("V")
                }
            },
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 21.sp, lineHeight = 24.sp),
            fontWeight = FontWeight.Black,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .size(4.dp)
                .background(SVColors.Purple, RoundedCornerShape(50)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 13.dp, end = 5.dp)
                .size(3.dp)
                .background(SVColors.Cyan, RoundedCornerShape(50)),
        )
    }
}
