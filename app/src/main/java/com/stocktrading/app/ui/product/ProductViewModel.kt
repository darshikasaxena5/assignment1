package com.stocktrading.app.ui.product

import androidx.lifecycle.viewModelScope
import com.stocktrading.app.data.models.ChartPoint
import com.stocktrading.app.data.models.NetworkResult
import com.stocktrading.app.data.models.Stock
import com.stocktrading.app.data.models.StockQuote
import com.stocktrading.app.data.repository.StockRepository
import com.stocktrading.app.data.repository.WatchlistRepository
import com.stocktrading.app.ui.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val watchlistRepository: WatchlistRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    private var currentSymbol: String = ""

    fun loadStockData(symbol: String) {
        if (currentSymbol != symbol) {
            currentSymbol = symbol
            _uiState.value = ProductUiState()
        }

        setLoading(true)
        clearError()

        viewModelScope.launch {
            var dataLoaded = false

            // First check if it's a fallback stock
            val fallbackStock = getFallbackStock(symbol)
            if (fallbackStock != null) {
                android.util.Log.d("ProductViewModel", "Using fallback data for $symbol")
                _uiState.value = _uiState.value.copy(
                    stock = fallbackStock,
                    isInWatchlist = watchlistRepository.isStockInAnyWatchlist(symbol)
                )
                dataLoaded = true
                // Generate mock chart data for fallback stocks
                _uiState.value = _uiState.value.copy(
                    chartData = generateMockChartData(fallbackStock)
                )
            } else {
                // Try to load from API
                // 1) Try GLOBAL_QUOTE for this symbol
                withTimeoutOrNull(10_000) {
                    stockRepository.getGlobalQuote(symbol).collect { result ->
                        when (result) {
                            is NetworkResult.Loading -> {
                                android.util.Log.d("ProductViewModel", "Loading quote for $symbol")
                            }
                            is NetworkResult.Success -> {
                                android.util.Log.d("ProductViewModel", "Got quote for $symbol from API")
                                val quote = result.data
                                _uiState.value = _uiState.value.copy(
                                    stock = quote.toStock(),
                                    isInWatchlist = watchlistRepository.isStockInAnyWatchlist(symbol)
                                )
                                dataLoaded = true
                            }
                            is NetworkResult.Error -> {
                                android.util.Log.e("ProductViewModel", "Quote API error for $symbol: ${result.message}")
                            }
                        }
                    }
                }

                // 2) If GLOBAL_QUOTE failed, try other methods
                if (!dataLoaded) {
                    android.util.Log.d("ProductViewModel", "GLOBAL_QUOTE failed, trying other methods for $symbol")
                    loadStockDataFromMultipleSources(symbol)
                    dataLoaded = _uiState.value.stock != null
                }
            }

            if (!dataLoaded) {
                setError("Stock '$symbol' not found. Please check the symbol and try again.")
            } else {
                // Load additional data (chart, watchlist status)
                loadAdditionalData(symbol)
            }

            setLoading(false)
        }
    }

    private suspend fun loadStockDataFromMultipleSources(symbol: String) {
        try {
            // 1) Check if it exists in top gainers/losers
            val topGainersLosersResult = stockRepository.getTopGainersAndLosers().first()
            if (topGainersLosersResult is NetworkResult.Success) {
                val data = topGainersLosersResult.data
                val allStocks = data.topGainers + data.topLosers + data.mostActivelyTraded
                val foundQuote = allStocks.find { it.ticker.equals(symbol, ignoreCase = true) }

                if (foundQuote != null) {
                    android.util.Log.d("ProductViewModel", "Found $symbol in top gainers/losers")
                    _uiState.value = _uiState.value.copy(
                        stock = foundQuote.toStock(),
                        isInWatchlist = watchlistRepository.isStockInAnyWatchlist(symbol)
                    )
                    return
                }
            }

            // 2) Check cached data in database
            val cachedStock = stockRepository.getStock(symbol).first()
            if (cachedStock != null) {
                android.util.Log.d("ProductViewModel", "Found $symbol in cache")
                _uiState.value = _uiState.value.copy(
                    stock = cachedStock,
                    isInWatchlist = cachedStock.isInWatchlist
                )
                return
            }

            // 3) Try to get company overview
            val overviewResult = stockRepository.getCompanyOverview(symbol).first()
            if (overviewResult is NetworkResult.Success) {
                val overview = overviewResult.data
                if (overview.name.isNotEmpty()) {
                    android.util.Log.d("ProductViewModel", "Found $symbol via company overview")
                    val stock = Stock(
                        symbol = symbol,
                        name = overview.name,
                        price = "Loading...", // Price will be updated separately
                        change = "0.00",
                        changePercent = "0.00%",
                        volume = "0",
                        marketCap = overview.marketCapitalization,
                        sector = overview.sector,
                        description = overview.description
                    )
                    _uiState.value = _uiState.value.copy(
                        stock = stock,
                        isInWatchlist = watchlistRepository.isStockInAnyWatchlist(symbol)
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ProductViewModel", "Error loading stock data for $symbol", e)
        }
    }

    private fun loadAdditionalData(symbol: String) {
        viewModelScope.launch {
            // Load chart data
            try {
                stockRepository.getDailyTimeSeries(symbol).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                chartData = result.data.takeLast(30)
                            )
                        }
                        is NetworkResult.Error -> {
                            // If API fails, generate mock chart data
                            _uiState.value.stock?.let { stock ->
                                _uiState.value = _uiState.value.copy(
                                    chartData = generateMockChartData(stock)
                                )
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductViewModel", "Error loading chart data", e)
            }

            // Update watchlist status
            val isInWatchlist = watchlistRepository.isStockInAnyWatchlist(symbol)
            _uiState.value = _uiState.value.copy(isInWatchlist = isInWatchlist)
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            val stock = _uiState.value.stock ?: return@launch
            val currentlyInWatchlist = _uiState.value.isInWatchlist

            try {
                if (currentlyInWatchlist) {
                    // Remove from all watchlists
                    watchlistRepository.removeStockFromAllWatchlists(stock.symbol)
                    stockRepository.updateWatchlistStatus(stock.symbol, false)
                    _uiState.value = _uiState.value.copy(isInWatchlist = false)
                    setSuccess("${stock.symbol} removed from watchlist")
                } else {
                    // Get all watchlists
                    val watchlists = watchlistRepository.getAllWatchlists().first()

                    if (watchlists.isEmpty()) {
                        // Create a default watchlist if none exist
                        val watchlistId = watchlistRepository.createWatchlist("My Watchlist")
                        watchlistRepository.addStockToWatchlist(watchlistId, stock.symbol)
                        _uiState.value = _uiState.value.copy(isInWatchlist = true)
                        setSuccess("${stock.symbol} added to My Watchlist")
                    } else {
                        // Add to the first watchlist (you can later implement a selection dialog)
                        val firstWatchlist = watchlists.first()
                        watchlistRepository.addStockToWatchlist(firstWatchlist.id, stock.symbol)
                        _uiState.value = _uiState.value.copy(isInWatchlist = true)
                        setSuccess("${stock.symbol} added to ${firstWatchlist.name}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductViewModel", "Error toggling watchlist", e)
                setError("Failed to update watchlist: ${e.message}")
            }
        }
    }

    private fun getFallbackStock(symbol: String): Stock? {
        return when (symbol.uppercase()) {
            // Fallback gainers
            "AAPL" -> Stock("AAPL", "Apple Inc.", "150.25", "+2.34", "+1.58%", "45.2M")
            "MSFT" -> Stock("MSFT", "Microsoft Corp.", "305.18", "+5.67", "+1.89%", "32.1M")
            "GOOGL" -> Stock("GOOGL", "Alphabet Inc.", "2750.80", "+45.20", "+1.67%", "28.5M")
            "TSLA" -> Stock("TSLA", "Tesla Inc.", "890.45", "+23.15", "+2.67%", "55.8M")
            "NVDA" -> Stock("NVDA", "NVIDIA Corp.", "420.75", "+8.90", "+2.16%", "41.2M")

            // Fallback losers
            "META" -> Stock("META", "Meta Platforms", "320.45", "-4.25", "-1.31%", "38.7M")
            "NFLX" -> Stock("NFLX", "Netflix Inc.", "450.30", "-8.70", "-1.90%", "22.4M")
            "AMZN" -> Stock("AMZN", "Amazon.com Inc.", "3200.15", "-35.80", "-1.11%", "43.2M")
            "AMD" -> Stock("AMD", "Advanced Micro", "95.60", "-2.40", "-2.45%", "67.3M")
            "INTC" -> Stock("INTC", "Intel Corp.", "52.30", "-1.20", "-2.24%", "78.9M")
            "IBM" -> Stock("IBM", "IBM Corp.", "140.25", "-3.45", "-2.40%", "12.5M")
            "ORCL" -> Stock("ORCL", "Oracle Corp.", "88.90", "-2.10", "-2.31%", "18.7M")

            // Fallback most active
            "SPY" -> Stock("SPY", "SPDR S&P 500", "420.50", "+1.25", "+0.30%", "125.6M")
            "QQQ" -> Stock("QQQ", "Invesco QQQ", "350.75", "+2.10", "+0.60%", "89.4M")
            "IWM" -> Stock("IWM", "iShares Russell", "195.30", "-0.85", "-0.43%", "95.2M")
            "VTI" -> Stock("VTI", "Vanguard Total", "230.45", "+0.95", "+0.41%", "72.8M")
            "VOO" -> Stock("VOO", "Vanguard S&P 500", "385.20", "+1.15", "+0.30%", "68.1M")

            else -> null
        }
    }

    private fun generateMockChartData(stock: Stock): List<ChartPoint> {
        val currentPrice = stock.price.toFloatOrNull() ?: 100f
        val changePercent = stock.changePercent
            .replace("%", "")
            .replace("+", "")
            .toFloatOrNull() ?: 0f

        val chartPoints = mutableListOf<ChartPoint>()
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -30)

        for (i in 0 until 30) {
            val dayProgress = i.toFloat() / 29f
            val trendComponent = (changePercent / 100f) * currentPrice * dayProgress
            val randomVariation = (kotlin.random.Random.nextFloat() - 0.5f) * currentPrice * 0.02f

            val price = currentPrice - (changePercent / 100f * currentPrice) + trendComponent + randomVariation

            chartPoints.add(ChartPoint(
                date = "${calendar.get(java.util.Calendar.YEAR)}-${String.format("%02d", calendar.get(java.util.Calendar.MONTH) + 1)}-${String.format("%02d", calendar.get(java.util.Calendar.DAY_OF_MONTH))}",
                price = price.coerceAtLeast(0.01f),
                timestamp = calendar.timeInMillis
            ))

            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        return chartPoints
    }

    fun clearMessages() {
        clearError()
        clearSuccess()
    }

    override fun onCleared() {
        super.onCleared()
        clearMessages()
    }
}

data class ProductUiState(
    val stock: Stock? = null,
    val chartData: List<ChartPoint> = emptyList(),
    val isInWatchlist: Boolean = false
)