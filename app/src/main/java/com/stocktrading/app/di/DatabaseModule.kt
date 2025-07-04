package com.stocktrading.app.di

import android.content.Context
import androidx.room.Room
import com.stocktrading.app.data.database.StockDatabase
import com.stocktrading.app.data.database.StockDao
import com.stocktrading.app.data.database.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides Room database instance
     */
    @Provides
    @Singleton
    fun provideStockDatabase(@ApplicationContext context: Context): StockDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            StockDatabase::class.java,
            StockDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration() // For development - remove in production
        .build()
    }
    
    /**
     * Provides StockDao
     */
    @Provides
    @Singleton
    fun provideStockDao(database: StockDatabase): StockDao {
        return database.stockDao()
    }
    
    /**
     * Provides WatchlistDao
     */
    @Provides
    @Singleton
    fun provideWatchlistDao(database: StockDatabase): WatchlistDao {
        return database.watchlistDao()
    }
}