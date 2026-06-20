package com.smartvision.svplayer.data.mock

enum class HomeCategoryType {
    Live,
    Movies,
    Series,
}

enum class HomeVisualStyle {
    Signal,
    Cinema,
    Series,
    Sport,
    Nature,
    People,
    Desert,
    City,
    Fire,
    Mystery,
}

data class HomeCategory(
    val id: String,
    val type: HomeCategoryType,
    val badge: String,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val visualStyle: HomeVisualStyle,
)

data class ContinueItem(
    val id: String,
    val title: String,
    val meta: String,
    val remaining: String,
    val progress: Float,
    val visualStyle: HomeVisualStyle,
    val imageUrl: String? = null,
)

object MockHomeData {
    val categories = listOf(
        HomeCategory(
            id = "live",
            type = HomeCategoryType.Live,
            badge = "LIVE",
            title = "Live TV",
            subtitle = "Regardez vos chaînes en direct",
            actionLabel = "Voir maintenant",
            visualStyle = HomeVisualStyle.Signal,
        ),
        HomeCategory(
            id = "movies",
            type = HomeCategoryType.Movies,
            badge = "VOD",
            title = "Films",
            subtitle = "Découvrez des films incontournables",
            actionLabel = "Explorer",
            visualStyle = HomeVisualStyle.Cinema,
        ),
        HomeCategory(
            id = "series",
            type = HomeCategoryType.Series,
            badge = "SÉRIES",
            title = "Séries",
            subtitle = "Suivez vos séries préférées",
            actionLabel = "Explorer",
            visualStyle = HomeVisualStyle.Series,
        ),
    )

    val continueWatching = listOf(
        ContinueItem(
            id = "continue-1",
            title = "The Last Signal",
            meta = "S1 E5",
            remaining = "45 min",
            progress = 0.22f,
            visualStyle = HomeVisualStyle.People,
        ),
        ContinueItem(
            id = "continue-2",
            title = "Velocity 10",
            meta = "Film",
            remaining = "1 h 12",
            progress = 0.34f,
            visualStyle = HomeVisualStyle.City,
        ),
        ContinueItem(
            id = "continue-3",
            title = "Desert Path",
            meta = "Partie 2",
            remaining = "53 min",
            progress = 0.18f,
            visualStyle = HomeVisualStyle.Desert,
        ),
        ContinueItem(
            id = "continue-4",
            title = "Night Lights",
            meta = "S2 E4",
            remaining = "32 min",
            progress = 0.48f,
            visualStyle = HomeVisualStyle.Mystery,
        ),
        ContinueItem(
            id = "continue-5",
            title = "Mountain Quest",
            meta = "Film",
            remaining = "1 h 5",
            progress = 0.63f,
            visualStyle = HomeVisualStyle.Nature,
        ),
    )

    val trending = listOf(
        ContinueItem(
            id = "trend-1",
            title = "Blue Frontier",
            meta = "Tendance",
            remaining = "Nouveau",
            progress = 0f,
            visualStyle = HomeVisualStyle.Nature,
        ),
        ContinueItem(
            id = "trend-2",
            title = "Urban Run",
            meta = "Action",
            remaining = "Populaire",
            progress = 0f,
            visualStyle = HomeVisualStyle.City,
        ),
        ContinueItem(
            id = "trend-3",
            title = "House of Embers",
            meta = "Série",
            remaining = "Top 10",
            progress = 0f,
            visualStyle = HomeVisualStyle.Fire,
        ),
        ContinueItem(
            id = "trend-4",
            title = "Silent Mission",
            meta = "Film",
            remaining = "Recommandé",
            progress = 0f,
            visualStyle = HomeVisualStyle.Mystery,
        ),
        ContinueItem(
            id = "trend-5",
            title = "After Rain",
            meta = "Série",
            remaining = "Nouveau",
            progress = 0f,
            visualStyle = HomeVisualStyle.People,
        ),
    )
}
