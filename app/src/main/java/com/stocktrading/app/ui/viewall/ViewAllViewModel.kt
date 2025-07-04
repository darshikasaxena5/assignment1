package com.stocktrading.app.ui.viewall

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
class ViewAllViewModel @Inject constructor(
    private val stockRepository: StockRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ViewAllUiState())
    val uiState: StateFlow<ViewAllUiState> = _uiState.asStateFlow()

    private var currentSection: String = ""

    fun loadStocks(section: String, forceRefresh: Boolean = false) {
        if (currentSection == section && !forceRefresh && _uiState.value.stocks.isNotEmpty()) {
            return // Already loaded this section
        }

        currentSection = section
        setLoading(true)
        clearError()

        viewModelScope.launch {
            stockRepository.getTopGainersAndLosers(forceRefresh)
                .collect { result ->
                    handleNetworkResult(
                        result = result,
                        onSuccess = { data ->
                            val stocks = when (section.lowercase()) {
                                "gainers" -> data.topGainers.map { it.toStock() }
                                "losers" -> data.topLosers.map { it.toStock() }
                                "active" -> data.mostActivelyTraded.map { it.toStock() }
                                else -> data.mostActivelyTraded.map { it.toStock() }
                            }

                            _uiState.value = _uiState.value.copy(
                                stocks = stocks,
                                section = section,
                                lastUpdated = data.lastUpdated
                            )

                            // Load company names for better display
                            loadCompanyNames(stocks.take(10)) // Load names for first 10 stocks
                        },
                        onError = { errorMessage ->
                            setError(errorMessage)

                            // Show fallback data if no stocks are loaded
                            if (_uiState.value.stocks.isEmpty()) {
                                loadFallbackData(section)
                            }
                        }
                    )
                }
        }
    }

    private fun loadFallbackData(section: String) {
        // Fallback data when API fails
        val fallbackStocks = when (section.lowercase()) {
            "gainers" -> listOf(
                Stock("AAPL", "Apple Inc.", "150.25", "+2.34", "+1.58%", "45.2M"),
                Stock("MSFT", "Microsoft Corp.", "305.18", "+5.67", "+1.89%", "32.1M"),
                Stock("GOOGL", "Alphabet Inc.", "2750.80", "+45.20", "+1.67%", "28.5M"),
                Stock("TSLA", "Tesla Inc.", "890.45", "+23.15", "+2.67%", "55.8M"),
                Stock("NVDA", "NVIDIA Corp.", "420.75", "+8.90", "+2.16%", "41.2M")
            )
            "losers" -> listOf(
                Stock("META", "Meta Platforms", "320.45", "-4.25", "-1.31%", "38.7M"),
                Stock("NFLX", "Netflix Inc.", "450.30", "-8.70", "-1.90%", "22.4M"),
                Stock("AMZN", "Amazon.com Inc.", "3200.15", "-35.80", "-1.11%", "43.2M"),
                Stock("AMD", "Advanced Micro", "95.60", "-2.40", "-2.45%", "67.3M"),
                Stock("INTC", "Intel Corp.", "52.30", "-1.20", "-2.24%", "78.9M")
            )
            else -> listOf(
                Stock("SPY", "SPDR S&P 500", "420.50", "+1.25", "+0.30%", "125.6M"),
                Stock("QQQ", "Invesco QQQ", "350.75", "+2.10", "+0.60%", "89.4M"),
                Stock("IWM", "iShares Russell", "195.30", "-0.85", "-0.43%", "95.2M"),
                Stock("VTI", "Vanguard Total", "230.45", "+0.95", "+0.41%", "72.8M"),
                Stock("VOO", "Vanguard S&P 500", "385.20", "+1.15", "+0.30%", "68.1M")
            )
        }

        _uiState.value = _uiState.value.copy(
            stocks = fallbackStocks,
            section = section,
            lastUpdated = "Fallback data (API unavailable)"
        )
        setLoading(false)
    }

    private fun loadCompanyNames(stocks: List<Stock>) {
        viewModelScope.launch {
            stocks.forEach { stock ->
                stockRepository.getCompanyOverview(stock.symbol)
                    .collect { result ->
                        if (result is NetworkResult.Success) {
                            val companyData = result.data
                            updateStockName(stock.symbol, companyData.name)
                        }
                    }
            }
        }
    }

    private fun updateStockName(symbol: String, name: String) {
        val currentStocks = _uiState.value.stocks
        val updatedStocks = currentStocks.map { stock ->
            if (stock.symbol == symbol) {
                stock.copy(name = name)
            } else {
                stock
            }
        }
        _uiState.value = _uiState.value.copy(stocks = updatedStocks)
    }

    fun onRefresh() {
        loadStocks(currentSection, forceRefresh = true)
    }

    fun onRetry() {
        loadStocks(currentSection, forceRefresh = true)
    }
}

data class ViewAllUiState(
    val stocks: List<Stock> = emptyList(),
    val section: String = "",
    val lastUpdated: String = ""
)