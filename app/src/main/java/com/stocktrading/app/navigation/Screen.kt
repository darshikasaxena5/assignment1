package com.stocktrading.app.navigation

sealed class Screen(val route: String) {
    object Explore : Screen("explore")
    object Watchlist : Screen("watchlist")
    object Product : Screen("product")
    object ViewAll : Screen("viewall")
}

enum class BottomNavItem(
    val screen: Screen,
    val title: String
) {
    EXPLORE(Screen.Explore, "Explore"),
    WATCHLIST(Screen.Watchlist, "Watchlist")
}