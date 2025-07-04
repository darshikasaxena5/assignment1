package com.stocktrading.app.ui.watchlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stocktrading.app.data.models.Stock
import com.stocktrading.app.ui.explore.ExploreViewModel

@Composable
fun AddStockToWatchlistDialog(
    watchlistId: Long,
    watchlistName: String,
    onDismiss: () -> Unit,
    onAddStock: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val exploreViewModel: ExploreViewModel = hiltViewModel()
    val uiState by exploreViewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by exploreViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by exploreViewModel.errorMessage.collectAsStateWithLifecycle()

    // Load real stock data when dialog opens
    LaunchedEffect(Unit) {
        exploreViewModel.onRefresh()
    }

    // Get all available stocks from the explore data (already converted to Stock objects)
    val availableStocks = remember(uiState.topGainers, uiState.topLosers, uiState.mostActive) {
        val stockList = mutableListOf<Stock>()

        // Add from top gainers (already Stock objects)
        stockList.addAll(uiState.topGainers)

        // Add from top losers (already Stock objects)
        stockList.addAll(uiState.topLosers)

        // Add from most active (already Stock objects)
        stockList.addAll(uiState.mostActive)

        stockList.distinctBy { it.symbol } // Remove duplicates
    }

    // Only show fallback stocks if API data is completely unavailable
    val finalStocks = if (availableStocks.isEmpty() && !isLoading) {
        listOf(
            Stock("AAPL", "Apple Inc.", "150.25", "+2.34", "+1.58%", "45.2M"),
            Stock("MSFT", "Microsoft Corp.", "305.18", "+5.67", "+1.89%", "32.1M"),
            Stock("GOOGL", "Alphabet Inc.", "2750.80", "+45.20", "+1.67%", "28.5M"),
            Stock("TSLA", "Tesla Inc.", "890.45", "+23.15", "+2.67%", "55.8M"),
            Stock("NVDA", "NVIDIA Corp.", "420.75", "+8.90", "+2.16%", "41.2M"),
            Stock("META", "Meta Platforms", "320.45", "-4.25", "-1.31%", "38.7M"),
            Stock("AMZN", "Amazon.com Inc.", "3200.15", "-35.80", "-1.11%", "43.2M"),
            Stock("NFLX", "Netflix Inc.", "450.30", "+8.70", "+1.90%", "22.4M")
        )
    } else {
        availableStocks
    }

    val filteredStocks = remember(searchQuery, finalStocks) {
        if (searchQuery.isBlank()) {
            finalStocks
        } else {
            finalStocks.filter { stock ->
                stock.symbol.contains(searchQuery, ignoreCase = true) ||
                        stock.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Add Stock to $watchlistName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search stocks...") },
                    placeholder = { Text("Enter stock symbol") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Show data source indicator
                Text(
                    text = if (availableStocks.isNotEmpty()) {
                        "Live data from Alpha Vantage"
                    } else if (isLoading) {
                        "Loading live data..."
                    } else {
                        "Sample data (API unavailable)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Loading stocks...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    errorMessage != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error loading stocks",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "Unknown error occurred",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { exploreViewModel.onRefresh() }
                            ) {
                                Text("Retry")
                            }
                        }
                    }

                    filteredStocks.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "No stocks available" else "No stocks found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredStocks) { stock ->
                                StockDialogItem(
                                    stock = stock,
                                    onAddClick = {
                                        onAddStock(stock.symbol)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun StockDialogItem(
    stock: Stock,
    onAddClick: () -> Unit
) {
    val changeValue = stock.changePercent.replace("%", "").replace("+", "").toFloatOrNull() ?: 0f
    val isPositive = changeValue >= 0
    val changeColor = if (isPositive)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                Text(
                    text = "${stock.change} (${stock.changePercent})",
                    style = MaterialTheme.typography.bodySmall,
                    color = changeColor
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${stock.price}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = onAddClick,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Add")
                }
            }
        }
    }
}