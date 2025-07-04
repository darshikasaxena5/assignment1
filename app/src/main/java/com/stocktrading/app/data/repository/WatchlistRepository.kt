package com.stocktrading.app.data.repository

import com.stocktrading.app.data.database.StockDao
import com.stocktrading.app.data.database.WatchlistDao
import com.stocktrading.app.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepository @Inject constructor(
    private val watchlistDao: WatchlistDao,
    private val stockDao: StockDao
) {
    
    /**
     * Get all watchlists
     */
    fun getAllWatchlists(): Flow<List<Watchlist>> {
        return watchlistDao.getAllWatchlists()
    }
    
    /**
     * Get all watchlists with their stocks
     */
    fun getAllWatchlistsWithStocks(): Flow<List<WatchlistWithStocks>> {
        return watchlistDao.getAllWatchlistsWithStocks().map { results ->
            results.map { it.toWatchlistWithStocks() }
        }
    }
    
    /**
     * Get specific watchlist with stocks
     */
    suspend fun getWatchlistWithStocks(watchlistId: Long): WatchlistWithStocks? {
        return watchlistDao.getWatchlistWithStocks(watchlistId)?.toWatchlistWithStocks()
    }
    
    /**
     * Get stocks in a specific watchlist
     */
    fun getStocksInWatchlist(watchlistId: Long): Flow<List<Stock>> {
        return watchlistDao.getStocksInWatchlist(watchlistId)
    }
    
    /**
     * Create a new watchlist
     */
    suspend fun createWatchlist(name: String): Long {
        val watchlist = Watchlist(name = name)
        return watchlistDao.insertWatchlist(watchlist)
    }
    
    /**
     * Update watchlist
     */
    suspend fun updateWatchlist(watchlist: Watchlist) {
        watchlistDao.updateWatchlist(watchlist)
    }
    
    /**
     * Delete watchlist
     */
    suspend fun deleteWatchlist(watchlist: Watchlist) {
        watchlistDao.deleteWatchlist(watchlist)
    }
    
    /**
     * Add stock to watchlist
     */
    suspend fun addStockToWatchlist(watchlistId: Long, stockSymbol: String): Boolean {
        return try {
            // First ensure the stock exists in the stocks table
            val existingStock = stockDao.getStock(stockSymbol)
            if (existingStock == null) {
                // Create a basic stock entry if it doesn't exist
                val basicStock = Stock(
                    symbol = stockSymbol,
                    name = stockSymbol // We'll update this later when we fetch full data
                )
                stockDao.insertStock(basicStock)
            }
            
            // Add to watchlist
            val watchlistStock = WatchlistStock(
                watchlistId = watchlistId,
                stockSymbol = stockSymbol
            )
            watchlistDao.insertWatchlistStock(watchlistStock)
            
            // Update stock's watchlist status
            stockDao.updateWatchlistStatus(stockSymbol, true)
            
            // Update watchlist stock count
            watchlistDao.updateWatchlistStockCount(watchlistId)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Remove stock from watchlist
     */
    suspend fun removeStockFromWatchlist(watchlistId: Long, stockSymbol: String): Boolean {
        return try {
            watchlistDao.removeStockFromWatchlist(watchlistId, stockSymbol)
            
            // Check if stock is in any other watchlist
            val isInOtherWatchlist = watchlistDao.isStockInAnyWatchlist(stockSymbol)
            if (!isInOtherWatchlist) {
                stockDao.updateWatchlistStatus(stockSymbol, false)
            }
            
            // Update watchlist stock count
            watchlistDao.updateWatchlistStockCount(watchlistId)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if stock is in specific watchlist
     */
    suspend fun isStockInWatchlist(watchlistId: Long, stockSymbol: String): Boolean {
        return watchlistDao.isStockInWatchlist(watchlistId, stockSymbol)
    }
    
    /**
     * Check if stock is in any watchlist
     */
    suspend fun isStockInAnyWatchlist(stockSymbol: String): Boolean {
        return watchlistDao.isStockInAnyWatchlist(stockSymbol)
    }
    
    /**
     * Get all watchlists containing a specific stock
     */
    suspend fun getWatchlistsContainingStock(stockSymbol: String): List<Watchlist> {
        return watchlistDao.getWatchlistsContainingStock(stockSymbol)
    }
    
    /**
     * Remove stock from all watchlists
     */
    suspend fun removeStockFromAllWatchlists(stockSymbol: String) {
        val watchlists = getWatchlistsContainingStock(stockSymbol)
        watchlists.forEach { watchlist ->
            removeStockFromWatchlist(watchlist.id, stockSymbol)
        }
    }
    
    /**
     * Update all watchlist stock counts
     */
    suspend fun updateAllWatchlistStockCounts() {
        watchlistDao.updateAllWatchlistStockCounts()
    }
    
    /**
     * Get watchlist by ID
     */
    suspend fun getWatchlist(id: Long): Watchlist? {
        return watchlistDao.getWatchlist(id)
    }
    
    /**
     * Get watchlist as Flow
     */
    fun getWatchlistFlow(id: Long): Flow<Watchlist?> {
        return watchlistDao.getWatchlistFlow(id)
    }
}