package com.stocktrading.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.stocktrading.app.data.models.Stock
import com.stocktrading.app.data.models.Watchlist
import com.stocktrading.app.data.models.WatchlistStock

@Database(
    entities = [
        Stock::class,
        Watchlist::class,
        WatchlistStock::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StockDatabase : RoomDatabase() {
    
    abstract fun stockDao(): StockDao
    abstract fun watchlistDao(): WatchlistDao
    
    companion object {
        const val DATABASE_NAME = "stock_database"
        
        @Volatile
        private var INSTANCE: StockDatabase? = null
        
        fun getDatabase(context: Context): StockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration() // For development only
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}