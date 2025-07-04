package com.stocktrading.app.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Import your models - adjust package if needed
import com.stocktrading.app.data.models.TopGainersLosersResponse
import com.stocktrading.app.data.models.CompanyOverview
import com.stocktrading.app.data.models.TimeSeriesResponse
import com.stocktrading.app.data.models.SearchResponse

interface AlphaVantageApi {

    /**
     * Test endpoint to verify API key is working
     * Using a simple GLOBAL_QUOTE call for AAPL
     */
    @GET("query")
    suspend fun testApiKey(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String = "AAPL",
        @Query("apikey") apiKey: String
    ): Response<Map<String, Any>>

    /**
     * Get top gainers, losers, and most active stocks
     * Endpoint: TOP_GAINERS_LOSERS
     */
    @GET("query")
    suspend fun getTopGainersLosers(
        @Query("function") function: String = "TOP_GAINERS_LOSERS",
        @Query("apikey") apiKey: String
    ): Response<TopGainersLosersResponse>

    /**
     * Get company overview/fundamental data
     * Endpoint: OVERVIEW
     */
    @GET("query")
    suspend fun getCompanyOverview(
        @Query("function") function: String = "OVERVIEW",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): Response<CompanyOverview>

    /**
     * Get daily time series data for charts
     * Endpoint: TIME_SERIES_DAILY
     */
    @GET("query")
    suspend fun getDailyTimeSeries(
        @Query("function") function: String = "TIME_SERIES_DAILY",
        @Query("symbol") symbol: String,
        @Query("outputsize") outputSize: String = "compact", // compact or full
        @Query("apikey") apiKey: String
    ): Response<TimeSeriesResponse>

    /**
     * Search for stocks/ETFs
     * Endpoint: SYMBOL_SEARCH
     */
    @GET("query")
    suspend fun searchSymbols(
        @Query("function") function: String = "SYMBOL_SEARCH",
        @Query("keywords") keywords: String,
        @Query("apikey") apiKey: String
    ): Response<SearchResponse>

    /**
     * Get global quote (current price and basic stats)
     * Endpoint: GLOBAL_QUOTE
     */
    @GET("query")
    suspend fun getGlobalQuote(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol")   symbol:   String,
        @Query("apikey")   apiKey:   String
    ): Response<Map<String, Map<String, String>>>  // <<-- nested Map!
}