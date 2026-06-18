package com.smartvision.svplayer.data.mock

data class MockHomeCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val countLabel: String,
)

data class MockStatusItem(
    val label: String,
    val value: String,
)

object MockData {
    val homeCards = listOf(
        MockHomeCard(
            id = "live",
            title = "Live TV",
            subtitle = "Categories et chaines en direct seront branchees apres validation UI.",
            badge = "DIRECT",
            countLabel = "8 categories mock",
        ),
        MockHomeCard(
            id = "movies",
            title = "Films",
            subtitle = "Rails VOD, filtres et details arriveront ecran par ecran.",
            badge = "VOD",
            countLabel = "6 categories mock",
        ),
        MockHomeCard(
            id = "series",
            title = "Series",
            subtitle = "Catalogue series temporaire, sans API Xtream.",
            badge = "SERIES",
            countLabel = "6 categories mock",
        ),
    )

    val statusItems = listOf(
        MockStatusItem("Mode", "UI mock"),
        MockStatusItem("API", "Non branchee"),
        MockStatusItem("Player", "Non integre"),
        MockStatusItem("Storage", "Room inactive"),
    )

    val liveNotes = listOf(
        "Layout cible: categories, chaines, apercu.",
        "Chargement categories d'abord, chaines apres OK.",
        "Focus visible sur chaque item D-pad.",
    )

    val moviesNotes = listOf(
        "Rails horizontaux et filtres a definir dans la prochaine tache.",
        "Cartes 16:9 avec badge optionnel.",
        "Aucune recuperation VOD pour ce socle.",
    )

    val seriesNotes = listOf(
        "Structure series/saisons/episodes gardee pour plus tard.",
        "Rows horizontales scrollables a venir.",
        "Focus initial a definir par ecran final.",
    )

    val settingsNotes = listOf(
        "Parametres placeholders uniquement.",
        "Pas de synchro, pas de compte, pas de persistance.",
        "Les actions restent compatibles telecommande.",
    )
}
