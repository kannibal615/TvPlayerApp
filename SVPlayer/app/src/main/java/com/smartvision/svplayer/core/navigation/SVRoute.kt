package com.smartvision.svplayer.core.navigation

enum class SVRoute(val route: String) {
    Home("home"),
    Live("live"),
    Movies("movies"),
    Series("series"),
    Account("account"),
    Settings("settings"),
    Player("player/{kind}/{id}");

    val topLevelRoute: String
        get() = when (this) {
            Player -> Home.route
            else -> route
        }

    companion object {
        fun fromRoute(route: String?): SVRoute =
            entries.firstOrNull { route?.startsWith(it.route.substringBefore("/{")) == true } ?: Home

        fun player(kind: String, id: String): String = "player/$kind/$id"
    }
}
