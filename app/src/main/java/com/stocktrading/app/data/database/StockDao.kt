package com.stocktrading.app.data.database

import androidx.room.*
import androidx.paging.PagingSource
import com.stocktrading.app.data.models.Stock
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    
    @Query("SELECT * FROM stocks ORDER BY symbol ASC")
    fun getAllStocks(): Flow<List<Stock>>
    


    @Query("SELECT * FROM stocks WHERE symbol = :symbol")
    suspend fun getStock(symbol: String): Stock?
    
    @Query("SELECT * FROM stocks WHERE symbol = :symbol")
    fun getStockFlow(symbol: String): Flow<Stock?>
    
    @Query("SELECT * FROM stocks WHERE isInWatchlist = 1")
    fun getWatchlistedStocks(): Flow<List<Stock>>
    
    @Query("SELECT * FROM stocks WHERE symbol LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%'")
    suspend fun searchStocks(query: String): List<Stock>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: Stock)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<Stock>)
    
    @Update
    suspend fun updateStock(stock: Stock)
    
    @Query("UPDATE stocks SET isInWatchlist = :isInWatchlist WHERE symbol = :symbol")
    suspend fun updateWatchlistStatus(symbol: String, isInWatchlist: Boolean)
    
    @Delete
    suspend fun deleteStock(stock: Stock)
    
    @Query("DELETE FROM stocks WHERE symbol = :symbol")
    suspend fun deleteStockBySymbol(symbol: String)
    
    @Query("DELETE FROM stocks")
    suspend fun deleteAllStocks()
    
    // Cache management
    @Query("SELECT COUNT(*) FROM stocks")
    suspend fun getStockCount(): Int
    
    @Query("SELECT lastUpdated FROM stocks ORDER BY lastUpdated DESC LIMIT 1")
    suspend fun getLastUpdateTime(): Long?
    
    @Query("DELETE FROM stocks WHERE lastUpdated < :timestamp")
    suspend fun deleteOldStocks(timestamp: Long)
}