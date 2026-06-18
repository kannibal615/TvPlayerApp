package com.smartvision.svplayer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import kotlin.math.min

@Composable
fun HomeVisualBackground(
    style: HomeVisualStyle,
    modifier: Modifier = Modifier,
) {
    val colors = style.visualColors()
    Box(
        modifier = modifier
            .background(Brush.linearGradient(colors))
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.last().copy(alpha = 0.1f), Color.Transparent),
                        center = Offset(size.width * 0.72f, size.height * 0.28f),
                        radius = size.maxDimension * 0.72f,
                    ),
                )
                when (style) {
                    HomeVisualStyle.Signal -> drawSignalVisual()
                    HomeVisualStyle.Cinema -> drawCinemaVisual()
                    HomeVisualStyle.Series -> drawSeriesVisual()
                    HomeVisualStyle.Sport -> drawSportVisual()
                    HomeVisualStyle.Nature -> drawNatureVisual()
                    HomeVisualStyle.People -> drawPeopleVisual()
                    HomeVisualStyle.Desert -> drawDesertVisual()
                    HomeVisualStyle.City -> drawCityVisual()
                    HomeVisualStyle.Fire -> drawFireVisual()
                    HomeVisualStyle.Mystery -> drawMysteryVisual()
                }
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xFF020712).copy(alpha = 0.64f)),
                    ),
                )
            },
    )
}

@Composable
fun HomeHeroMosaic(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        HomeVisualBackground(
            style = HomeVisualStyle.People,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, SmartVisionColors.Primary.copy(alpha = 0.24f)),
                    ),
                ),
        )
    }
}

private fun DrawScope.drawSignalVisual() {
    val center = Offset(size.width * 0.68f, size.height * 0.36f)
    repeat(6) { index ->
        drawCircle(
            color = SmartVisionColors.Primary.copy(alpha = 0.27f - index * 0.032f),
            radius = min(size.width, size.height) * (0.13f + index * 0.12f),
            center = center,
            style = Stroke(width = 3.2f),
        )
    }

    val towerTop = Offset(center.x, size.height * 0.38f)
    val baseY = size.height * 0.86f
    val halfBase = size.width * 0.11f
    val tower = Path().apply {
        moveTo(towerTop.x, towerTop.y)
        lineTo(center.x - halfBase, baseY)
        lineTo(center.x + halfBase, baseY)
        close()
    }
    drawPath(tower, SmartVisionColors.Primary.copy(alpha = 0.18f))
    drawLine(SmartVisionColors.CyanAccent.copy(alpha = 0.72f), towerTop, Offset(center.x - halfBase, baseY), strokeWidth = 3f)
    drawLine(SmartVisionColors.CyanAccent.copy(alpha = 0.72f), towerTop, Offset(center.x + halfBase, baseY), strokeWidth = 3f)
    repeat(3) { index ->
        val y = towerTop.y + (baseY - towerTop.y) * (0.24f + index * 0.2f)
        val span = halfBase * (0.38f + index * 0.22f)
        drawLine(Color.White.copy(alpha = 0.28f), Offset(center.x - span, y), Offset(center.x + span, y), strokeWidth = 2f)
    }
    drawCircle(SmartVisionColors.CyanAccent.copy(alpha = 0.9f), radius = 7f, center = towerTop)
    drawCitySilhouette(alpha = 0.25f)
}

private fun DrawScope.drawCinemaVisual() {
    val reelCenter = Offset(size.width * 0.78f, size.height * 0.64f)
    drawCircle(Color(0xFFE4C28B).copy(alpha = 0.35f), size.minDimension * 0.16f, reelCenter)
    drawCircle(Color(0xFF120B08).copy(alpha = 0.72f), size.minDimension * 0.07f, reelCenter)
    repeat(5) { index ->
        val angleOffset = index * size.minDimension * 0.018f
        drawCircle(
            Color.Black.copy(alpha = 0.48f),
            size.minDimension * 0.027f,
            Offset(reelCenter.x + angleOffset - size.minDimension * 0.045f, reelCenter.y - angleOffset * 0.7f),
        )
    }

    val bucketTop = Offset(size.width * 0.48f, size.height * 0.42f)
    val bucketSize = Size(size.width * 0.2f, size.height * 0.36f)
    drawRoundRect(
        color = Color(0xFFEEDFC8).copy(alpha = 0.72f),
        topLeft = bucketTop,
        size = bucketSize,
        cornerRadius = CornerRadius(8f, 8f),
    )
    repeat(3) { index ->
        drawRect(
            color = Color(0xFFD4472E).copy(alpha = 0.55f),
            topLeft = Offset(bucketTop.x + bucketSize.width * (0.16f + index * 0.27f), bucketTop.y + 4f),
            size = Size(bucketSize.width * 0.12f, bucketSize.height - 8f),
        )
    }
    repeat(7) { index ->
        drawCircle(
            color = Color(0xFFFFD37A).copy(alpha = 0.58f),
            radius = size.minDimension * (0.035f + index % 2 * 0.01f),
            center = Offset(bucketTop.x + bucketSize.width * (0.04f + index * 0.15f), bucketTop.y - size.height * 0.03f),
        )
    }
    drawClapboard()
}

private fun DrawScope.drawSeriesVisual() {
    drawCitySilhouette(alpha = 0.32f)
    drawPersonSilhouette(Offset(size.width * 0.68f, size.height * 0.38f), scale = 1.02f, alpha = 0.48f)
    drawPersonSilhouette(Offset(size.width * 0.83f, size.height * 0.42f), scale = 0.92f, alpha = 0.38f)
    drawCircle(
        color = Color(0xFF79B9FF).copy(alpha = 0.18f),
        radius = size.maxDimension * 0.38f,
        center = Offset(size.width * 0.78f, size.height * 0.34f),
    )
}

private fun DrawScope.drawSportVisual() {
    drawRoundRect(
        color = Color(0xFF0B4C70).copy(alpha = 0.36f),
        topLeft = Offset(size.width * 0.18f, size.height * 0.5f),
        size = Size(size.width * 0.72f, size.height * 0.34f),
        cornerRadius = CornerRadius(20f, 20f),
    )
    drawLine(Color.White.copy(alpha = 0.35f), Offset(size.width * 0.18f, size.height * 0.66f), Offset(size.width * 0.9f, size.height * 0.66f), strokeWidth = 2f)
    drawCircle(Color.White.copy(alpha = 0.78f), size.minDimension * 0.07f, Offset(size.width * 0.66f, size.height * 0.46f))
    drawCircle(Color.Black.copy(alpha = 0.32f), size.minDimension * 0.025f, Offset(size.width * 0.66f, size.height * 0.46f))
}

private fun DrawScope.drawNatureVisual() {
    val mountain = Path().apply {
        moveTo(size.width * 0.06f, size.height * 0.82f)
        lineTo(size.width * 0.32f, size.height * 0.36f)
        lineTo(size.width * 0.54f, size.height * 0.76f)
        lineTo(size.width * 0.7f, size.height * 0.48f)
        lineTo(size.width * 0.95f, size.height * 0.82f)
        close()
    }
    drawPath(mountain, Color(0xFFC9E8FF).copy(alpha = 0.22f))
    drawCircle(Color(0xFF8CE5FF).copy(alpha = 0.16f), size.minDimension * 0.18f, Offset(size.width * 0.82f, size.height * 0.28f))
}

private fun DrawScope.drawPeopleVisual() {
    repeat(4) { index ->
        val left = size.width * (0.42f + index * 0.12f)
        val top = size.height * (0.18f + index % 2 * 0.08f)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.1f + index * 0.025f),
            topLeft = Offset(left, top),
            size = Size(size.width * 0.13f, size.height * 0.58f),
            cornerRadius = CornerRadius(14f, 14f),
        )
    }
    drawPersonSilhouette(Offset(size.width * 0.58f, size.height * 0.46f), scale = 0.72f, alpha = 0.34f)
    drawPersonSilhouette(Offset(size.width * 0.72f, size.height * 0.5f), scale = 0.64f, alpha = 0.28f)
}

private fun DrawScope.drawDesertVisual() {
    repeat(3) { index ->
        drawCircle(
            color = Color(0xFFFFB15A).copy(alpha = 0.14f),
            radius = size.maxDimension * (0.32f + index * 0.09f),
            center = Offset(size.width * (0.7f - index * 0.06f), size.height * 0.36f),
        )
    }
    drawLine(Color(0xFFFFD28A).copy(alpha = 0.28f), Offset(size.width * 0.1f, size.height * 0.72f), Offset(size.width * 0.92f, size.height * 0.55f), strokeWidth = 4f)
}

private fun DrawScope.drawCityVisual() {
    drawCitySilhouette(alpha = 0.42f)
    repeat(4) { index ->
        drawLine(
            color = SmartVisionColors.Primary.copy(alpha = 0.2f),
            start = Offset(size.width * 0.16f, size.height * (0.78f - index * 0.1f)),
            end = Offset(size.width * 0.86f, size.height * (0.3f - index * 0.05f)),
            strokeWidth = 2f,
        )
    }
}

private fun DrawScope.drawFireVisual() {
    repeat(8) { index ->
        drawCircle(
            color = Color(0xFFFF6734).copy(alpha = 0.16f),
            radius = size.minDimension * (0.05f + index * 0.018f),
            center = Offset(size.width * (0.42f + index * 0.06f), size.height * (0.68f - index % 3 * 0.13f)),
        )
    }
    drawRoundRect(
        color = Color(0xFF2E1208).copy(alpha = 0.5f),
        topLeft = Offset(size.width * 0.3f, size.height * 0.58f),
        size = Size(size.width * 0.52f, size.height * 0.22f),
        cornerRadius = CornerRadius(20f, 20f),
    )
}

private fun DrawScope.drawMysteryVisual() {
    drawCircle(Color(0xFFBBC6FF).copy(alpha = 0.18f), size.minDimension * 0.2f, Offset(size.width * 0.78f, size.height * 0.28f))
    drawCircle(Color(0xFF05060B).copy(alpha = 0.72f), size.minDimension * 0.18f, Offset(size.width * 0.72f, size.height * 0.26f))
    drawCitySilhouette(alpha = 0.28f)
}

private fun DrawScope.drawClapboard() {
    val topLeft = Offset(size.width * 0.58f, size.height * 0.28f)
    val boardSize = Size(size.width * 0.28f, size.height * 0.16f)
    drawRoundRect(
        color = Color(0xFFE4C28B).copy(alpha = 0.34f),
        topLeft = topLeft,
        size = boardSize,
        cornerRadius = CornerRadius(7f, 7f),
    )
    repeat(4) { index ->
        drawLine(
            color = Color.Black.copy(alpha = 0.32f),
            start = Offset(topLeft.x + boardSize.width * index / 4f, topLeft.y),
            end = Offset(topLeft.x + boardSize.width * (index + 0.45f) / 4f, topLeft.y + boardSize.height),
            strokeWidth = 4f,
        )
    }
}

private fun DrawScope.drawPersonSilhouette(
    center: Offset,
    scale: Float,
    alpha: Float,
) {
    val head = size.minDimension * 0.075f * scale
    drawCircle(Color.Black.copy(alpha = alpha), head, center)
    drawRoundRect(
        color = Color.Black.copy(alpha = alpha),
        topLeft = Offset(center.x - head * 1.25f, center.y + head * 0.72f),
        size = Size(head * 2.5f, head * 3.8f),
        cornerRadius = CornerRadius(head, head),
    )
}

private fun DrawScope.drawCitySilhouette(alpha: Float) {
    val base = size.height * 0.88f
    repeat(9) { index ->
        val width = size.width * (0.035f + (index % 3) * 0.012f)
        val height = size.height * (0.12f + (index % 4) * 0.055f)
        val left = size.width * (0.44f + index * 0.055f)
        drawRect(
            color = Color.Black.copy(alpha = alpha),
            topLeft = Offset(left, base - height),
            size = Size(width, height),
        )
    }
}

fun HomeVisualStyle.visualColors(): List<Color> =
    when (this) {
        HomeVisualStyle.Signal -> listOf(Color(0xFF031328), Color(0xFF06316F), Color(0xFF020712))
        HomeVisualStyle.Cinema -> listOf(Color(0xFF110B08), Color(0xFF663015), Color(0xFF06070A))
        HomeVisualStyle.Series -> listOf(Color(0xFF061927), Color(0xFF1D3B54), Color(0xFF050A12))
        HomeVisualStyle.Sport -> listOf(Color(0xFF02182A), Color(0xFF0F4C81), Color(0xFF03111C))
        HomeVisualStyle.Nature -> listOf(Color(0xFF051A2B), Color(0xFF185A71), Color(0xFF081522))
        HomeVisualStyle.People -> listOf(Color(0xFF111827), Color(0xFF31486D), Color(0xFF050810))
        HomeVisualStyle.Desert -> listOf(Color(0xFF170E08), Color(0xFF6E3B16), Color(0xFF0B0705))
        HomeVisualStyle.City -> listOf(Color(0xFF04101F), Color(0xFF263C66), Color(0xFF05070E))
        HomeVisualStyle.Fire -> listOf(Color(0xFF100806), Color(0xFF5A1E10), Color(0xFF070507))
        HomeVisualStyle.Mystery -> listOf(Color(0xFF0A0F1D), Color(0xFF2B214B), Color(0xFF05060B))
    }
