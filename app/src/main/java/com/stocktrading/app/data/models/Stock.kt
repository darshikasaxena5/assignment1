package com.stocktrading.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "stocks")
data class Stock(
    @PrimaryKey val symbol: String,
    val name: String = "",
    val price: String = "0.00",
    val change: String = "0.00",
    val changePercent: String = "0.00%",
    val volume: String = "0",
    val marketCap: String = "",
    val sector: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val isInWatchlist: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class TopGainersLosersResponse(
    @SerializedName("top_gainers")
    val topGainers: List<StockQuote> = emptyList(),

    @SerializedName("top_losers")
    val topLosers: List<StockQuote> = emptyList(),

    @SerializedName("most_actively_traded")
    val mostActivelyTraded: List<StockQuote> = emptyList(),

    @SerializedName("last_updated")
    val lastUpdated: String = ""
) : Parcelable

@Parcelize
data class StockQuote(
    @SerializedName("ticker") val ticker: String,
    @SerializedName("price") val price: String,
    @SerializedName("change_amount") val changeAmount: String,
    @SerializedName("change_percentage") val changePercentage: String,
    @SerializedName("volume") val volume: String
) : Parcelable {
    fun toStock(): Stock = Stock(
        symbol = ticker,
        name = ticker,
        price = price,
        change = changeAmount,
        changePercent = changePercentage,
        volume = volume
    )
}

@Parcelize
data class CompanyOverview(
    @SerializedName("Symbol") val symbol: String = "",
    @SerializedName("Name") val name: String = "",
    @SerializedName("Description") val description: String = "",
    @SerializedName("Sector") val sector: String = "",
    @SerializedName("Industry") val industry: String = "",
    @SerializedName("MarketCapitalization") val marketCapitalization: String = "",
    @SerializedName("52WeekHigh") val weekHigh52: String = "",
    @SerializedName("52WeekLow") val weekLow52: String = "",
    @SerializedName("PERatio") val peRatio: String = "",
    @SerializedName("DividendYield") val dividendYield: String = "",
    @SerializedName("EPS") val eps: String = "",
    @SerializedName("RevenuePerShareTTM") val revenuePerShare: String = ""
) : Parcelable

@Parcelize
data class TimeSeriesResponse(
    @SerializedName("Meta Data") val metaData: MetaData? = null,
    @SerializedName("Time Series (Daily)") val timeSeries: Map<String, DailyData>? = null
) : Parcelable

@Parcelize
data class MetaData(
    @SerializedName("1. Information") val information: String = "",
    @SerializedName("2. Symbol") val symbol: String = "",
    @SerializedName("3. Last Refreshed") val lastRefreshed: String = "",
    @SerializedName("4. Output Size") val outputSize: String = "",
    @SerializedName("5. Time Zone") val timeZone: String = ""
) : Parcelable

@Parcelize
data class DailyData(
    @SerializedName("1. open") val open: String = "",
    @SerializedName("2. high") val high: String = "",
    @SerializedName("3. low") val low: String = "",
    @SerializedName("4. close") val close: String = "",
    @SerializedName("5. volume") val volume: String = ""
) : Parcelable

@Parcelize
data class ChartPoint(
    val date: String,
    val price: Float,
    val timestamp: Long = 0L
) : Parcelable

@Parcelize
data class SearchResponse(
    @SerializedName("bestMatches")
    val bestMatches: List<SearchResult> = emptyList()
) : Parcelable

@Parcelize
data class SearchResult(
    @SerializedName("1. symbol") val symbol: String = "",
    @SerializedName("2. name") val name: String = "",
    @SerializedName("3. type") val type: String = "",
    @SerializedName("4. region") val region: String = "",
    @SerializedName("8. currency") val currency: String = ""
) : Parcelable