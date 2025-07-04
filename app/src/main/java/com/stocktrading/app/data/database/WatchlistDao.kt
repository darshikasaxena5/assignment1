package com.stocktrading.app.data.database

import androidx.room.*
import com.stocktrading.app.data.models.Watchlist
import com.stocktrading.app.data.models.WatchlistStock
import com.stocktrading.app.data.models.WatchlistWithStocks
import com.stocktrading.app.data.models.Stock
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    
    // Watchlist operations
    @Query("SELECT * FROM watchlists ORDER BY createdAt DESC")
    fun getAllWatchlists(): Flow<List<Watchlist>>
    
    @Query("SELECT * FROM watchlists WHERE id = :id")
    suspend fun getWatchlist(id: Long): Watchlist?
    
    @Query("SELECT * FROM watchlists WHERE id = :id")
    fun getWatchlistFlow(id: Long): Flow<Watchlist?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(watchlist: Watchlist): Long
    
    @Update
    suspend fun updateWatchlist(watchlist: Watchlist)
    
    @Delete
    suspend fun deleteWatchlist(watchlist: Watchlist)
    
    @Query("DELETE FROM watchlists WHERE id = :id")
    suspend fun deleteWatchlistById(id: Long)
    
    // WatchlistStock operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistStock(watchlistStock: WatchlistStock)
    
    @Query("DELETE FROM watchlist_stocks WHERE watchlistId = :watchlistId AND stockSymbol = :stockSymbol")
    suspend fun removeStockFromWatchlist(watchlistId: Long, stockSymbol: String)
    
    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_stocks WHERE watchlistId = :watchlistId AND stockSymbol = :stockSymbol)")
    suspend fun isStockInWatchlist(watchlistId: Long, stockSymbol: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_stocks WHERE stockSymbol = :stockSymbol)")
    suspend fun isStockInAnyWatchlist(stockSymbol: String): Boolean
    
    // Complex queries for watchlist with stocks
    @Transaction
    @Query("SELECT * FROM watchlists WHERE id = :watchlistId")
    suspend fun getWatchlistWithStocks(watchlistId: Long): WatchlistWithStocksResult?
    
    @Transaction
    @Query("SELECT * FROM watchlists ORDER BY createdAt DESC")
    fun getAllWatchlistsWithStocks(): Flow<List<WatchlistWithStocksResult>>
    
    @Query("""
        SELECT s.* FROM stocks s 
        INNER JOIN watchlist_stocks ws ON s.symbol = ws.stockSymbol 
        WHERE ws.watchlistId = :watchlistId 
        ORDER BY ws.addedAt DESC
    """)
    fun getStocksInWatchlist(watchlistId: Long): Flow<List<Stock>>
    
    @Query("""
        SELECT w.* FROM watchlists w 
        INNER JOIN watchlist_stocks ws ON w.id = ws.watchlistId 
        WHERE ws.stockSymbol = :stockSymbol
    """)
    suspend fun getWatchlistsContainingStock(stockSymbol: String): List<Watchlist>
    
    // Update stock count in watchlist
    @Query("""
        UPDATE watchlists 
        SET stockCount = (
            SELECT COUNT(*) FROM watchlist_stocks 
            WHERE watchlistId = watchlists.id
        ) 
        WHERE id = :watchlistId
    """)
    suspend fun updateWatchlistStockCount(watchlistId: Long)
    
    @Query("""
        UPDATE watchlists 
        SET stockCount = (
            SELECT COUNT(*) FROM watchlist_stocks 
            WHERE watchlistId = watchlists.id
        )
    """)
    suspend fun updateAllWatchlistStockCounts()
}

// Helper data class for Room relations
data class WatchlistWithStocksResult(
    @Embedded val watchlist: Watchlist,
    @Relation(
        parentColumn = "id",
        entityColumn = "symbol",
        associateBy = Junction(
            WatchlistStock::class,
            parentColumn = "watchlistId",
            entityColumn = "stockSymbol"
        )
    )
    val stocks: List<Stock>
) {
    fun toWatchlistWithStocks(): WatchlistWithStocks {
        return WatchlistWithStocks(watchlist, stocks)
    }
}