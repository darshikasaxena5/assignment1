package com.stocktrading.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "watchlists")
data class Watchlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val stockCount: Int = 0
) : Parcelable

@Parcelize
@Entity(
    tableName = "watchlist_stocks",
    primaryKeys = ["watchlistId", "stockSymbol"],
    foreignKeys = [
        ForeignKey(
            entity = Watchlist::class,
            parentColumns = ["id"],
            childColumns = ["watchlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Stock::class,
            parentColumns = ["symbol"],
            childColumns = ["stockSymbol"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WatchlistStock(
    val watchlistId: Long,
    val stockSymbol: String,
    val addedAt: Long = System.currentTimeMillis()
) : Parcelable

// Data class for displaying watchlist with stocks
@Parcelize
data class WatchlistWithStocks(
    val watchlist: Watchlist,
    val stocks: List<Stock>
) : Parcelable