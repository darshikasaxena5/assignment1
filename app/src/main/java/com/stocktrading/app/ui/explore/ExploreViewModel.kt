package com.stocktrading.app.ui.explore

import androidx.lifecycle.viewModelScope
import com.stocktrading.app.data.models.Stock
import com.stocktrading.app.data.models.NetworkResult
import com.stocktrading.app.data.repository.StockRepository
import com.stocktrading.app.ui.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val stockRepository: StockRepository
) : BaseViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadStockData()
    }

    fun testApiConnection() {
        viewModelScope.launch {
            stockRepository.testApiConnection()
                .collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            android.util.Log.d("ExploreViewModel", "Testing API connection...")
                        }
                        is NetworkResult.Success -> {
                            android.util.Log.d("ExploreViewModel", "API Test Success: ${result.data}")
                            setSuccess("API connection successful!")
                        }
                        is NetworkResult.Error -> {
                            android.util.Log.e("ExploreViewModel", "API Test Error: ${result.message}")
                            setError("API Test Failed: ${result.message}")
                        }
                    }
                }
        }
    }

    fun onRefresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadStockData(forceRefresh = true)
    }

    private fun loadStockData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            setLoading(true)
            clearError()

            stockRepository.getTopGainersAndLosers(forceRefresh)
                .collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            setLoading(true)
                        }

                        is NetworkResult.Success -> {
                            val data = result.data
                            android.util.Log.d("ExploreViewModel", "📊 Data received - Gainers: ${data.topGainers.size}, Losers: ${data.topLosers.size}, Active: ${data.mostActivelyTraded.size}")

                            // Convert API data to Stock objects
                            val gainers = data.topGainers.map { quote ->
                                Stock(
                                    symbol = quote.ticker,
                                    name = getStockName(quote.ticker),
                                    price = cleanPrice(quote.price),
                                    change = cleanChange(quote.changeAmount),
                                    changePercent = cleanPercentage(quote.changePercentage),
                                    volume = quote.volume
                                )
                            }

                            val losers = data.topLosers.map { quote ->
                                Stock(
                                    symbol = quote.ticker,
                                    name = getStockName(quote.ticker),
                                    price = cleanPrice(quote.price),
                                    change = cleanChange(quote.changeAmount),
                                    changePercent = cleanPercentage(quote.changePercentage),
                                    volume = quote.volume
                                )
                            }

                            val mostActive = data.mostActivelyTraded.map { quote ->
                                Stock(
                                    symbol = quote.ticker,
                                    name = getStockName(quote.ticker),
                                    price = cleanPrice(quote.price),
                                    change = cleanChange(quote.changeAmount),
                                    changePercent = cleanPercentage(quote.changePercentage),
                                    volume = quote.volume
                                )
                            }

                            // All data from smart simulator is already valid, no need for extensive filtering
                            android.util.Log.d("ExploreViewModel", "✅ Processed - Gainers: ${gainers.size}, Losers: ${losers.size}, Active: ${mostActive.size}")

                            _uiState.value = _uiState.value.copy(
                                topGainers = gainers,
                                topLosers = losers,
                                mostActive = mostActive,
                                lastUpdated = data.lastUpdated,
                                isRefreshing = false,
                                dataSource = determineDataSource(data.lastUpdated)
                            )

                            setLoading(false)

                            // Load company names for better display (async, non-blocking)
                            if (gainers.isNotEmpty() || losers.isNotEmpty() || mostActive.isNotEmpty()) {
                                loadCompanyNamesAsync(gainers + losers + mostActive)
                            }
                        }

                        is NetworkResult.Error -> {
                            android.util.Log.e("ExploreViewModel", "❌ API Error: ${result.message}")
                            
                            // This should rarely happen now with smart fallback
                            setError("Unable to load stock data: ${result.message}")
                            setLoading(false)
                            _uiState.value = _uiState.value.copy(isRefreshing = false)

                            // Show static fallback only as last resort
                            if (_uiState.value.topGainers.isEmpty() && _uiState.value.topLosers.isEmpty() && _uiState.value.mostActive.isEmpty()) {
                                loadStaticFallbackData()
                            }
                        }
                    }
                }
        }
    }

    private fun getStockName(symbol: String): String {
        return when (symbol.uppercase()) {
            "AAPL" -> "Apple Inc."
            "MSFT" -> "Microsoft Corp."
            "GOOGL" -> "Alphabet Inc."
            "AMZN" -> "Amazon.com Inc."
            "TSLA" -> "Tesla Inc."
            "NVDA" -> "NVIDIA Corp."
            "META" -> "Meta Platforms"
            "NFLX" -> "Netflix Inc."
            "JPM" -> "JPMorgan Chase"
            "JNJ" -> "Johnson & Johnson"
            "PG" -> "Procter & Gamble"
            "KO" -> "Coca-Cola Co."
            "WMT" -> "Walmart Inc."
            "DIS" -> "Walt Disney Co."
            "SPY" -> "SPDR S&P 500"
            "QQQ" -> "Invesco QQQ"
            "AMD" -> "Advanced Micro"
            "INTC" -> "Intel Corp."
            "GME" -> "GameStop Corp."
            "AMC" -> "AMC Entertainment"
            "PLTR" -> "Palantir Tech"
            "BB" -> "BlackBerry Ltd."
            "XOM" -> "Exxon Mobil"
            "BAC" -> "Bank of America"
            "F" -> "Ford Motor Co."
            "GE" -> "General Electric"
            else -> symbol // Return symbol if name not found
        }
    }

    private fun cleanPrice(price: String): String {
        return price.replace("$", "").trim()
    }

    private fun cleanChange(change: String): String {
        return change.replace("$", "").trim()
    }

    private fun cleanPercentage(percentage: String): String {
        return percentage.trim()
    }

    private fun determineDataSource(lastUpdated: String): DataSource {
        return when {
            lastUpdated.contains("Market Open", ignoreCase = true) -> DataSource.LIVE_SIMULATION
            lastUpdated.contains("After Hours", ignoreCase = true) -> DataSource.AFTER_HOURS_SIMULATION
            lastUpdated.contains("Weekend", ignoreCase = true) -> DataSource.WEEKEND_SIMULATION
            lastUpdated.contains("Cached", ignoreCase = true) -> DataSource.CACHED
            else -> DataSource.SIMULATION
        }
    }

    private fun loadCompanyNamesAsync(stocks: List<Stock>) {
        viewModelScope.launch {
            // Load company names for the first few stocks to improve UI (non-blocking)
            val symbolsToLoad = stocks.take(6).map { it.symbol }

            symbolsToLoad.forEach { symbol ->
                // Only load if we don't already have a proper name
                if (symbol == getStockName(symbol)) {
                    stockRepository.getCompanyOverview(symbol)
                        .collect { result ->
                            if (result is NetworkResult.Success) {
                                val companyData = result.data
                                if (companyData.name.isNotEmpty() && companyData.name != symbol) {
                                    updateStockName(symbol, companyData.name)
                                }
                            }
                        }
                }
            }
        }
    }

    private fun updateStockName(symbol: String, name: String) {
        val currentState = _uiState.value

        val updatedGainers = currentState.topGainers.map { stock ->
            if (stock.symbol == symbol) stock.copy(name = name) else stock
        }

        val updatedLosers = currentState.topLosers.map { stock ->
            if (stock.symbol == symbol) stock.copy(name = name) else stock
        }

        val updatedActive = currentState.mostActive.map { stock ->
            if (stock.symbol == symbol) stock.copy(name = name) else stock
        }

        _uiState.value = currentState.copy(
            topGainers = updatedGainers,
            topLosers = updatedLosers,
            mostActive = updatedActive
        )
    }

    private fun loadStaticFallbackData() {
        android.util.Log.d("ExploreViewModel", "📋 Loading static fallback data")
        _uiState.value = _uiState.value.copy(
            topGainers = getStaticGainers(),
            topLosers = getStaticLosers(),
            mostActive = getStaticActive(),
            lastUpdated = "📊 Demo Data (No internet connection)",
            isRefreshing = false,
            dataSource = DataSource.STATIC_DEMO
        )
    }

    private fun getStaticGainers(): List<Stock> {
        return listOf(
            Stock("AAPL", "Apple Inc.", "175.25", "+2.34", "+1.36%", "45.2M"),
            Stock("MSFT", "Microsoft Corp.", "362.18", "+5.67", "+1.59%", "32.1M"),
            Stock("GOOGL", "Alphabet Inc.", "2847.80", "+35.20", "+1.25%", "28.5M"),
            Stock("TSLA", "Tesla Inc.", "258.45", "+6.15", "+2.44%", "55.8M"),
            Stock("NVDA", "NVIDIA Corp.", "678.75", "+12.90", "+1.94%", "41.2M")
        )
    }

    private fun getStaticLosers(): List<Stock> {
        return listOf(
            Stock("META", "Meta Platforms", "398.45", "-4.25", "-1.06%", "38.7M"),
            Stock("NFLX", "Netflix Inc.", "456.30", "-8.70", "-1.87%", "22.4M"),
            Stock("INTC", "Intel Corp.", "58.30", "-1.20", "-2.02%", "78.9M"),
            Stock("F", "Ford Motor Co.", "12.85", "-0.34", "-2.58%", "95.3M"),
            Stock("GE", "General Electric", "105.60", "-2.40", "-2.22%", "67.3M")
        )
    }

    private fun getStaticActive(): List<Stock> {
        return listOf(
            Stock("SPY", "SPDR S&P 500", "422.50", "+1.25", "+0.30%", "125.6M"),
            Stock("QQQ", "Invesco QQQ", "368.75", "+2.10", "+0.57%", "89.4M"),
            Stock("AMD", "Advanced Micro", "127.30", "+2.85", "+2.29%", "95.2M"),
            Stock("GME", "GameStop Corp.", "18.45", "+0.95", "+5.43%", "72.8M"),
            Stock("AMC", "AMC Entertainment", "5.20", "+0.15", "+2.97%", "68.1M")
        )
    }

    override fun onCleared() {
        super.onCleared()
        clearError()
        clearSuccess()
    }
}

data class ExploreUiState(
    val topGainers: List<Stock> = emptyList(),
    val topLosers: List<Stock> = emptyList(),
    val mostActive: List<Stock> = emptyList(),
    val isRefreshing: Boolean = false,
    val lastUpdated: String = "",
    val dataSource: DataSource = DataSource.LOADING
)

enum class DataSource {
    LOADING,
    LIVE_SIMULATION,
    AFTER_HOURS_SIMULATION,
    WEEKEND_SIMULATION,
    CACHED,
    SIMULATION,
    STATIC_DEMO
}