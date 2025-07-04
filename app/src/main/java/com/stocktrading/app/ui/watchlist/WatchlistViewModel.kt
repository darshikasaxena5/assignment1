package com.stocktrading.app.ui.watchlist

import androidx.lifecycle.viewModelScope
import com.stocktrading.app.data.models.NetworkResult
import com.stocktrading.app.data.models.Stock
import com.stocktrading.app.data.models.Watchlist
import com.stocktrading.app.data.models.WatchlistWithStocks
import com.stocktrading.app.data.repository.StockRepository
import com.stocktrading.app.data.repository.WatchlistRepository
import com.stocktrading.app.ui.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val stockRepository: StockRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    fun loadWatchlists() {
        viewModelScope.launch {
            setLoading(true)
            try {
                watchlistRepository.getAllWatchlistsWithStocks().collect { watchlists ->
                    _uiState.value = _uiState.value.copy(watchlists = watchlists)
                    setLoading(false)
                }
            } catch (e: Exception) {
                setError("Failed to load watchlists: ${e.message}")
                setLoading(false)
            }
        }
    }

    fun createWatchlist(name: String) {
        viewModelScope.launch {
            try {
                val watchlistId = watchlistRepository.createWatchlist(name)
                if (watchlistId > 0) {
                    setSuccess("Watchlist '$name' created successfully")
                    loadWatchlists()
                } else {
                    setError("Failed to create watchlist")
                }
            } catch (e: Exception) {
                setError("Failed to create watchlist: ${e.message}")
            }
        }
    }

    fun addStockToWatchlist(watchlistId: Long, symbol: String) {
        viewModelScope.launch {
            try {
                setLoading(true)
                android.util.Log.d("WatchlistViewModel", "Adding stock $symbol to watchlist $watchlistId")

                // Basic format validation
                if (!isValidSymbolFormat(symbol)) {
                    setError("Invalid symbol format. Please enter a valid ticker symbol.")
                    setLoading(false)
                    return@launch
                }

                // Check for obviously invalid symbols
                if (isInvalidSymbol(symbol)) {
                    setError("Cannot add invalid stock symbol '$symbol' to watchlist")
                    setLoading(false)
                    return@launch
                }

                // Check if stock already exists in this watchlist
                val watchlist = watchlistRepository.getWatchlistWithStocks(watchlistId)
                if (watchlist != null) {
                    val existingStock = watchlist.stocks.find { it.symbol.equals(symbol, ignoreCase = true) }
                    if (existingStock != null) {
                        setError("$symbol is already in this watchlist")
                        setLoading(false)
                        return@launch
                    }
                }

                // Strict validation: Must exist in current API data
                val isValidStock = validateStockWithAPI(symbol)
                if (!isValidStock) {
                    setError("Cannot add invalid stock symbol '$symbol' to watchlist")
                    setLoading(false)
                    return@launch
                }

                // Add the stock
                val success = watchlistRepository.addStockToWatchlist(watchlistId, symbol)
                if (success) {
                    setSuccess("Added $symbol to watchlist")
                    loadWatchlists()
                } else {
                    setError("Failed to add $symbol to watchlist")
                }
                setLoading(false)
            } catch (e: Exception) {
                setError("Failed to add stock: ${e.message}")
                setLoading(false)
            }
        }
    }

    private fun isValidSymbolFormat(symbol: String): Boolean {
        // Basic symbol format validation
        val symbolRegex = "^[A-Za-z0-9.\\-+]{1,10}$".toRegex()
        return symbol.matches(symbolRegex)
    }

    private fun isInvalidSymbol(symbol: String): Boolean {
        // List of known invalid symbols
        val invalidSymbols = setOf(
            "DARSHI", "INVALID", "TEST", "MOCK", "DUMMY", "SAMPLE", "EXAMPLE", "FAKE", "NULL", "UNDEFINED"
        )
        return invalidSymbols.contains(symbol.uppercase())
    }

    private suspend fun validateStockWithAPI(symbol: String): Boolean {
        return try {
            val upperSymbol = symbol.uppercase()
            var isValid = false

            android.util.Log.d("WatchlistViewModel", "Validating stock $upperSymbol with API")

            // Check in current API data with timeout
            val result = withTimeoutOrNull(10000) {
                stockRepository.getTopGainersAndLosers(forceRefresh = true).first()
            }

            when (result) {
                is NetworkResult.Success -> {
                    val allStocks = result.data.topGainers + result.data.topLosers + result.data.mostActivelyTraded
                    isValid = allStocks.any { it.ticker.equals(upperSymbol, ignoreCase = true) }

                    android.util.Log.d("WatchlistViewModel", "API validation result for $upperSymbol: $isValid")
                    android.util.Log.d("WatchlistViewModel", "Available symbols: ${allStocks.map { it.ticker }.take(10)}")
                }
                is NetworkResult.Error -> {
                    android.util.Log.e("WatchlistViewModel", "API Error during validation: ${result.message}")
                }
                else -> {
                    android.util.Log.w("WatchlistViewModel", "API call failed or timed out for validation")
                }
            }

            // If not found in market data, try company overview as fallback
            if (!isValid) {
                android.util.Log.d("WatchlistViewModel", "Trying company overview for $upperSymbol")
                val companyResult = withTimeoutOrNull(10000) {
                    stockRepository.getCompanyOverview(upperSymbol).first()
                }

                when (companyResult) {
                    is NetworkResult.Success -> {
                        val companyData = companyResult.data
                        isValid = companyData.name.isNotEmpty() &&
                                companyData.name != upperSymbol &&
                                companyData.name != "N/A" &&
                                companyData.symbol.isNotEmpty()
                        android.util.Log.d("WatchlistViewModel", "Company overview validation for $upperSymbol: $isValid")
                    }
                    else -> {
                        android.util.Log.w("WatchlistViewModel", "Company overview failed for $upperSymbol")
                    }
                }
            }

            android.util.Log.d("WatchlistViewModel", "Final validation result for $upperSymbol: $isValid")
            isValid
        } catch (e: Exception) {
            android.util.Log.e("WatchlistViewModel", "Error validating symbol $symbol", e)
            false // Be strict on errors
        }
    }

    fun removeStockFromWatchlist(watchlistId: Long, symbol: String) {
        viewModelScope.launch {
            try {
                val success = watchlistRepository.removeStockFromWatchlist(watchlistId, symbol)
                if (success) {
                    setSuccess("Removed $symbol from watchlist")
                    loadWatchlists()
                } else {
                    setError("Failed to remove $symbol from watchlist")
                }
            } catch (e: Exception) {
                setError("Failed to remove stock: ${e.message}")
            }
        }
    }

    fun deleteWatchlist(watchlist: Watchlist) {
        viewModelScope.launch {
            try {
                watchlistRepository.deleteWatchlist(watchlist)
                setSuccess("Deleted watchlist '${watchlist.name}'")
                loadWatchlists()
            } catch (e: Exception) {
                setError("Failed to delete watchlist: ${e.message}")
            }
        }
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

data class WatchlistUiState(
    val watchlists: List<WatchlistWithStocks> = emptyList()
)