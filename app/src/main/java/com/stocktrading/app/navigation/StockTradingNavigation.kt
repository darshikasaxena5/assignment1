package com.stocktrading.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.stocktrading.app.ui.explore.ExploreScreen
import com.stocktrading.app.ui.explore.ExploreViewModel
import com.stocktrading.app.ui.watchlist.WatchlistScreen
import com.stocktrading.app.ui.product.ProductScreen
import com.stocktrading.app.data.models.Stock

@Composable
fun StockTradingNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = "explore"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("explore") {
            ExploreScreen(
                onNavigateToProduct = { symbol ->
                    navController.navigate("product/$symbol")
                },
                onNavigateToViewAll = { section ->
                    navController.navigate("viewall/$section")
                }
            )
        }

        composable("watchlist") {
            WatchlistScreen(
                onNavigateToProduct = { symbol ->
                    navController.navigate("product/$symbol")
                }
            )
        }

        composable("product/{symbol}") { backStackEntry ->
            val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
            ProductScreen(
                symbol = symbol,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("viewall/{section}") { backStackEntry ->
            val section = backStackEntry.arguments?.getString("section") ?: ""
            ViewAllScreen(
                section = section,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProduct = { symbol ->
                    navController.navigate("product/$symbol")
                }
            )
        }
    }
}

// Helper function to handle explore navigation from anywhere
fun navigateToExplore(navController: NavHostController) {
    navController.navigate("explore") {
        // Clear all back stack entries up to explore
        popUpTo("explore") {
            inclusive = false
        }
        // Avoid multiple copies of the same destination
        launchSingleTop = true
        // Don't restore state when reselecting a previously selected item
        restoreState = false
    }
}

// Helper function to handle watchlist navigation
fun navigateToWatchlist(navController: NavHostController) {
    navController.navigate("watchlist") {
        // Pop up to the start destination but keep the back stack
        popUpTo(navController.graph.startDestinationId) {
            saveState = true
        }
        // Avoid multiple copies of the same destination
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewAllScreen(
    section: String,
    onNavigateBack: () -> Unit,
    onNavigateToProduct: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)

    // Load data when screen opens
    LaunchedEffect(Unit) {
        viewModel.onRefresh()
    }

    // Get the appropriate stock list based on section (already Stock objects from ExploreViewModel)
    val stocks = when (section.lowercase()) {
        "gainers" -> uiState.topGainers
        "losers" -> uiState.topLosers
        "active" -> uiState.mostActive
        else -> uiState.mostActive
    }

    // Only use fallback data if API data is completely unavailable and not loading
    val finalStocks = if (stocks.isNotEmpty()) {
        stocks
    } else if (!isLoading) {
        when (section.lowercase()) {
            "gainers" -> listOf(
                Stock("AAPL", "Apple Inc.", "150.25", "+2.34", "+1.58%", "45.2M"),
                Stock("MSFT", "Microsoft Corp.", "305.18", "+5.67", "+1.89%", "32.1M"),
                Stock("GOOGL", "Alphabet Inc.", "2750.80", "+45.20", "+1.67%", "28.5M"),
                Stock("TSLA", "Tesla Inc.", "890.45", "+23.15", "+2.67%", "55.8M"),
                Stock("NVDA", "NVIDIA Corp.", "420.75", "+8.90", "+2.16%", "41.2M"),
                Stock("AMD", "Advanced Micro", "95.60", "+2.40", "+2.45%", "67.3M"),
                Stock("NFLX", "Netflix Inc.", "450.30", "+8.70", "+1.90%", "22.4M")
            )
            "losers" -> listOf(
                Stock("META", "Meta Platforms", "320.45", "-4.25", "-1.31%", "38.7M"),
                Stock("AMZN", "Amazon.com Inc.", "3200.15", "-35.80", "-1.11%", "43.2M"),
                Stock("INTC", "Intel Corp.", "52.30", "-1.20", "-2.24%", "78.9M"),
                Stock("IBM", "IBM Corp.", "140.25", "-3.45", "-2.40%", "12.5M"),
                Stock("ORCL", "Oracle Corp.", "88.90", "-2.10", "-2.31%", "18.7M")
            )
            else -> listOf(
                Stock("SPY", "SPDR S&P 500", "420.50", "+1.25", "+0.30%", "125.6M"),
                Stock("QQQ", "Invesco QQQ", "350.75", "+2.10", "+0.60%", "89.4M"),
                Stock("IWM", "iShares Russell", "195.30", "-0.85", "-0.43%", "95.2M"),
                Stock("VTI", "Vanguard Total", "230.45", "+0.95", "+0.41%", "72.8M"),
                Stock("VOO", "Vanguard S&P 500", "385.20", "+1.15", "+0.30%", "68.1M")
            )
        }
    } else {
        stocks
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Top ${section.replaceFirstChar { it.uppercase() }}",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.onRefresh() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
        )

        // Error handling
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.onRefresh() }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

        // Content with pull-to-refresh
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.onRefresh() }
        ) {
            when {
                isLoading && finalStocks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading ${section.lowercase()} stocks...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header showing data source
                        item {
                            Text(
                                text = if (stocks.isNotEmpty()) {
                                    "Showing live data • Last updated: ${uiState.lastUpdated}"
                                } else {
                                    "Showing sample data • Pull to refresh for live data"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(finalStocks) { stock ->
                            StockListItem(
                                stock = stock,
                                onClick = { onNavigateToProduct(stock.symbol) }
                            )
                        }

                        // Footer
                        item {
                            Text(
                                text = "Showing ${finalStocks.size} stocks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockListItem(
    stock: Stock,
    onClick: () -> Unit
) {
    val changeValue = stock.changePercent.replace("%", "").replace("+", "").toFloatOrNull() ?: 0f
    val isPositive = changeValue >= 0
    val changeColor = if (isPositive) androidx.compose.ui.graphics.Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color(0xFFF44336)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stock.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (stock.name.isNotEmpty() && stock.name != stock.symbol) {
                        stock.name
                    } else {
                        stock.symbol
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (stock.volume.isNotEmpty()) {
                    Text(
                        text = "Volume: ${stock.volume}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${stock.price}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${if (isPositive && !stock.change.startsWith("+")) "+" else ""}${stock.change} (${stock.changePercent})",
                    style = MaterialTheme.typography.bodySmall,
                    color = changeColor
                )
            }
        }
    }
}