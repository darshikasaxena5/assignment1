package com.stocktrading.app.utils

import com.stocktrading.app.data.models.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import kotlin.random.Random

class SmartStockSimulator {
    
    companion object {
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        
        // Real stock symbols with their typical price ranges and sectors
        private val stockDatabase = mapOf(
            // Technology Stocks (Higher volatility)
            "AAPL" to StockInfo("Apple Inc.", 150.0, 200.0, "Technology", 0.02, true),
            "MSFT" to StockInfo("Microsoft Corp.", 300.0, 400.0, "Technology", 0.018, true),
            "GOOGL" to StockInfo("Alphabet Inc.", 2500.0, 3200.0, "Technology", 0.025, true),
            "AMZN" to StockInfo("Amazon.com Inc.", 3000.0, 3500.0, "Technology", 0.022, true),
            "TSLA" to StockInfo("Tesla Inc.", 200.0, 300.0, "Automotive", 0.05, true),
            "NVDA" to StockInfo("NVIDIA Corp.", 400.0, 900.0, "Technology", 0.035, true),
            "META" to StockInfo("Meta Platforms", 300.0, 500.0, "Technology", 0.028, true),
            "NFLX" to StockInfo("Netflix Inc.", 400.0, 600.0, "Technology", 0.025, true),
            "AMD" to StockInfo("Advanced Micro", 90.0, 150.0, "Technology", 0.035, true),
            "CRM" to StockInfo("Salesforce Inc.", 180.0, 250.0, "Technology", 0.03, true),
            "ORCL" to StockInfo("Oracle Corp.", 90.0, 130.0, "Technology", 0.02, false),
            
            // Traditional Stocks (Lower volatility)
            "JPM" to StockInfo("JPMorgan Chase", 140.0, 180.0, "Financial", 0.015, false),
            "JNJ" to StockInfo("Johnson & Johnson", 160.0, 180.0, "Healthcare", 0.012, false),
            "PG" to StockInfo("Procter & Gamble", 140.0, 160.0, "Consumer Goods", 0.01, false),
            "KO" to StockInfo("Coca-Cola Co.", 55.0, 65.0, "Consumer Goods", 0.008, false),
            "WMT" to StockInfo("Walmart Inc.", 140.0, 160.0, "Retail", 0.012, false),
            "DIS" to StockInfo("Walt Disney Co.", 90.0, 120.0, "Entertainment", 0.02, false),
            "MCD" to StockInfo("McDonald's Corp.", 250.0, 300.0, "Consumer Goods", 0.015, false),
            "VZ" to StockInfo("Verizon Communications", 35.0, 45.0, "Telecom", 0.012, false),
            
            // High Activity ETFs and Popular Stocks
            "SPY" to StockInfo("SPDR S&P 500", 400.0, 450.0, "ETF", 0.008, false),
            "QQQ" to StockInfo("Invesco QQQ", 350.0, 400.0, "ETF", 0.012, true),
            "IWM" to StockInfo("iShares Russell 2000", 180.0, 220.0, "ETF", 0.015, false),
            "INTC" to StockInfo("Intel Corp.", 50.0, 70.0, "Technology", 0.02, false),
            
            // Meme/Popular Stocks (High volatility)
            "GME" to StockInfo("GameStop Corp.", 15.0, 25.0, "Retail", 0.08, true),
            "AMC" to StockInfo("AMC Entertainment", 3.0, 8.0, "Entertainment", 0.06, true),
            "PLTR" to StockInfo("Palantir Tech", 15.0, 25.0, "Technology", 0.04, true),
            "BB" to StockInfo("BlackBerry Ltd.", 4.0, 8.0, "Technology", 0.03, true),
            "RIVN" to StockInfo("Rivian Automotive", 12.0, 25.0, "Automotive", 0.06, true),
            "LCID" to StockInfo("Lucid Group Inc.", 5.0, 15.0, "Automotive", 0.07, true),
            
            // Energy & Finance
            "XOM" to StockInfo("Exxon Mobil", 95.0, 120.0, "Energy", 0.025, false),
            "BAC" to StockInfo("Bank of America", 28.0, 40.0, "Financial", 0.018, false),
            "F" to StockInfo("Ford Motor Co.", 10.0, 15.0, "Automotive", 0.03, false),
            "GE" to StockInfo("General Electric", 90.0, 120.0, "Industrial", 0.025, false),
            "CVX" to StockInfo("Chevron Corp.", 140.0, 180.0, "Energy", 0.02, false),
            "WFC" to StockInfo("Wells Fargo", 35.0, 50.0, "Financial", 0.02, false)
        )
    }
    
    data class StockInfo(
        val name: String,
        val minPrice: Double,
        val maxPrice: Double,
        val sector: String,
        val volatility: Double,
        val isTechStock: Boolean
    )
    
    fun generateTopGainersLosersResponse(): TopGainersLosersResponse {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        // More frequent updates - every 3 minutes for more dynamic feel
        val microSeed = (hour * 60 + minute / 3) 
        // Daily trending seed for consistent daily direction
        val dailySeed = dayOfYear.toLong()
        // Intraday movement seed for realistic price action
        val intradaySeed = (System.currentTimeMillis() / (3 * 60 * 1000)) // Changes every 3 minutes
        
        val random = Random(dailySeed + microSeed + intradaySeed)
        
        // Market conditions with breaking news effects
        val marketSentiment = getMarketSentiment(dayOfYear, hour, minute)
        val breakingNewsEffect = getBreakingNewsEffect(random, hour)
        
        val gainers = generateDynamicGainers(random, marketSentiment, breakingNewsEffect, hour, minute)
        val losers = generateDynamicLosers(random, marketSentiment, breakingNewsEffect, hour, minute)
        val mostActive = generateDynamicMostActive(random, marketSentiment, breakingNewsEffect, hour, minute)
        
        return TopGainersLosersResponse(
            topGainers = gainers,
            topLosers = losers,
            mostActivelyTraded = mostActive,
            lastUpdated = getCurrentTimestamp()
        )
    }
    
    private fun getMarketSentiment(dayOfYear: Int, hour: Int, minute: Int): MarketSentiment {
        return when {
            // Simulate market hours (9:30 AM - 4:00 PM EST)
            hour in 9..16 -> {
                when {
                    // Market opening surge/drop
                    hour == 9 && minute < 45 -> if (dayOfYear % 3 == 0) MarketSentiment.VOLATILE else MarketSentiment.BULLISH
                    // Lunch lull
                    hour == 12 -> MarketSentiment.NEUTRAL
                    // Afternoon power hour
                    hour == 15 -> MarketSentiment.VOLATILE
                    // Day patterns
                    dayOfYear % 7 == 0 -> MarketSentiment.WEEKEND
                    dayOfYear % 7 == 6 -> MarketSentiment.WEEKEND
                    dayOfYear % 7 == 1 -> MarketSentiment.BULLISH // Monday rally
                    dayOfYear % 7 == 5 -> MarketSentiment.VOLATILE // Friday volatility
                    else -> if (dayOfYear % 3 == 0) MarketSentiment.BEARISH else MarketSentiment.BULLISH
                }
            }
            hour in 17..23 || hour in 0..8 -> MarketSentiment.AFTER_HOURS
            else -> MarketSentiment.NEUTRAL
        }
    }
    
    private fun getBreakingNewsEffect(random: Random, hour: Int): BreakingNewsEffect {
        // Simulate random news events that affect markets
        val newsChance = random.nextDouble()
        return when {
            hour in 9..10 && newsChance < 0.1 -> BreakingNewsEffect.EARNINGS_BEAT // Morning earnings
            hour in 14..15 && newsChance < 0.05 -> BreakingNewsEffect.FED_ANNOUNCEMENT // Afternoon fed news
            hour in 11..12 && newsChance < 0.03 -> BreakingNewsEffect.TECH_BREAKTHROUGH // Midday tech news
            newsChance < 0.02 -> BreakingNewsEffect.SECTOR_ROTATION // Random sector news
            else -> BreakingNewsEffect.NONE
        }
    }
    
    private fun generateDynamicGainers(random: Random, sentiment: MarketSentiment, newsEffect: BreakingNewsEffect, hour: Int, minute: Int): List<StockQuote> {
        // Dynamic selection based on time and market conditions
        val gainersPool = when (newsEffect) {
            BreakingNewsEffect.EARNINGS_BEAT -> stockDatabase.filter { it.value.isTechStock }.keys.toList()
            BreakingNewsEffect.TECH_BREAKTHROUGH -> stockDatabase.filter { it.value.sector == "Technology" }.keys.toList()
            else -> stockDatabase.filter { 
                it.value.isTechStock || sentiment == MarketSentiment.BULLISH 
            }.keys.toList()
        }
        
        // Time-based selection for more realistic rotation
        val timeRotation = (hour * 4 + minute / 15) % gainersPool.size
        val rotatedPool = gainersPool.drop(timeRotation) + gainersPool.take(timeRotation)
        
        return rotatedPool.take(10).map { symbol ->
            val stockInfo = stockDatabase[symbol]!!
            
            // More realistic intraday price calculation
            val basePrice = getIntradayPrice(stockInfo, hour, minute, random)
            val gainMultiplier = when {
                newsEffect == BreakingNewsEffect.EARNINGS_BEAT && stockInfo.isTechStock -> 2.5
                newsEffect == BreakingNewsEffect.TECH_BREAKTHROUGH && stockInfo.sector == "Technology" -> 2.0
                sentiment == MarketSentiment.BULLISH -> 1.5
                sentiment == MarketSentiment.VOLATILE -> 2.0
                sentiment == MarketSentiment.WEEKEND -> 0.3
                else -> 1.0
            }
            
            // Realistic change calculation with intraday momentum
            val baseChangePercent = (0.5 + random.nextDouble() * 4.0) * gainMultiplier
            val momentumEffect = getIntradayMomentum(hour, minute, random, true)
            val changePercent = baseChangePercent * momentumEffect
            
            val changeAmount = basePrice * (changePercent / 100)
            val currentPrice = basePrice + changeAmount
            
            StockQuote(
                ticker = symbol,
                price = String.format("%.2f", currentPrice),
                changeAmount = "+${String.format("%.2f", changeAmount)}",
                changePercentage = "+${String.format("%.2f", changePercent)}%",
                volume = generateDynamicVolume(random, stockInfo, sentiment, hour, minute)
            )
        }.sortedByDescending { 
            it.changePercentage.replace("+", "").replace("%", "").toDoubleOrNull() ?: 0.0 
        }.take(8) // Keep top 8 for better performance
    }
    
    private fun generateDynamicLosers(random: Random, sentiment: MarketSentiment, newsEffect: BreakingNewsEffect, hour: Int, minute: Int): List<StockQuote> {
        val losersPool = when (newsEffect) {
            BreakingNewsEffect.SECTOR_ROTATION -> stockDatabase.filter { !it.value.isTechStock }.keys.toList()
            else -> stockDatabase.filter { 
                !it.value.isTechStock || sentiment == MarketSentiment.BEARISH 
            }.keys.toList()
        }
        
        val timeRotation = (hour * 3 + minute / 20) % losersPool.size
        val rotatedPool = losersPool.drop(timeRotation) + losersPool.take(timeRotation)
        
        return rotatedPool.take(10).map { symbol ->
            val stockInfo = stockDatabase[symbol]!!
            val basePrice = getIntradayPrice(stockInfo, hour, minute, random)
            
            val lossMultiplier = when {
                newsEffect == BreakingNewsEffect.SECTOR_ROTATION && !stockInfo.isTechStock -> 1.8
                sentiment == MarketSentiment.BEARISH -> 1.5
                sentiment == MarketSentiment.VOLATILE -> 2.0
                sentiment == MarketSentiment.WEEKEND -> 0.3
                else -> 1.0
            }
            
            val baseChangePercent = -(0.3 + random.nextDouble() * 3.5) * lossMultiplier
            val momentumEffect = getIntradayMomentum(hour, minute, random, false)
            val changePercent = baseChangePercent * momentumEffect
            
            val changeAmount = basePrice * (changePercent / 100)
            val currentPrice = basePrice + changeAmount
            
            StockQuote(
                ticker = symbol,
                price = String.format("%.2f", currentPrice),
                changeAmount = String.format("%.2f", changeAmount),
                changePercentage = "${String.format("%.2f", changePercent)}%",
                volume = generateDynamicVolume(random, stockInfo, sentiment, hour, minute)
            )
        }.sortedBy { 
            it.changePercentage.replace("%", "").toDoubleOrNull() ?: 0.0 
        }.take(8)
    }
    
    private fun generateDynamicMostActive(random: Random, sentiment: MarketSentiment, newsEffect: BreakingNewsEffect, hour: Int, minute: Int): List<StockQuote> {
        // Most active changes throughout the day based on volume patterns
        val activeStocks = listOf("AAPL", "TSLA", "SPY", "QQQ", "MSFT", "AMZN", "NVDA", "META", "AMD", "GME", "GOOGL", "F", "BAC", "XOM")
        
        // Rotate active stocks based on time for realistic market activity
        val timeRotation = (hour * 2 + minute / 30) % activeStocks.size
        val rotatedActive = activeStocks.drop(timeRotation) + activeStocks.take(timeRotation)
        
        return rotatedActive.take(10).map { symbol ->
            val stockInfo = stockDatabase[symbol]!!
            val basePrice = getIntradayPrice(stockInfo, hour, minute, random)
            
            // Most active can be gainers or losers with time-based bias
            val timeBasedBias = sin((hour * 60 + minute) * PI / 720) // Sine wave throughout day
            val isGainer = (random.nextDouble() + timeBasedBias * 0.3) > 0.5
            
            val changePercent = if (isGainer) {
                (0.2 + random.nextDouble() * 2.5) * getIntradayMomentum(hour, minute, random, true)
            } else {
                -(0.2 + random.nextDouble() * 2.0) * getIntradayMomentum(hour, minute, random, false)
            }
            
            val changeAmount = basePrice * (changePercent / 100)
            val currentPrice = basePrice + changeAmount
            
            StockQuote(
                ticker = symbol,
                price = String.format("%.2f", currentPrice),
                changeAmount = "${if (changeAmount >= 0) "+" else ""}${String.format("%.2f", changeAmount)}",
                changePercentage = "${if (changePercent >= 0) "+" else ""}${String.format("%.2f", changePercent)}%",
                volume = generateHighDynamicVolume(random, stockInfo, hour, minute)
            )
        }.sortedByDescending { 
            it.volume.replace(",", "").replace("M", "000000").replace("K", "000").toDoubleOrNull() ?: 0.0 
        }.take(8)
    }
    
    private fun getIntradayPrice(stockInfo: StockInfo, hour: Int, minute: Int, random: Random): Double {
        // Simulate realistic intraday price movement
        val timeProgress = (hour * 60 + minute) / 1440.0 // Progress through the day
        val openPrice = stockInfo.minPrice + (stockInfo.maxPrice - stockInfo.minPrice) * random.nextDouble()
        
        // Add intraday volatility with realistic patterns
        val volatilityFactor = when (hour) {
            9 -> 1.5 // Opening volatility
            10, 11 -> 1.2 // Morning activity
            12, 13 -> 0.8 // Lunch lull
            14, 15 -> 1.3 // Afternoon activity
            16 -> 1.4 // Closing volatility
            else -> 0.5 // After hours
        }
        
        val intradayMove = sin(timeProgress * PI * 2) * stockInfo.volatility * volatilityFactor * openPrice
        val randomNoise = (random.nextDouble() - 0.5) * stockInfo.volatility * openPrice * 0.5
        
        return openPrice + intradayMove + randomNoise
    }
    
    private fun getIntradayMomentum(hour: Int, minute: Int, random: Random, isPositive: Boolean): Double {
        // Simulate momentum effects that change throughout the day
        val timeOfDay = hour * 60 + minute
        val momentumPattern = when {
            timeOfDay < 600 -> 0.7 // Pre-market low momentum
            timeOfDay < 630 -> 1.3 // Opening surge
            timeOfDay < 720 -> 1.1 // Morning activity
            timeOfDay < 780 -> 0.9 // Late morning
            timeOfDay < 840 -> 0.8 // Lunch lull
            timeOfDay < 900 -> 1.2 // Afternoon pickup
            timeOfDay < 960 -> 1.4 // Power hour
            else -> 0.6 // After hours
        }
        
        val randomFactor = 0.8 + random.nextDouble() * 0.4 // 0.8 to 1.2
        return momentumPattern * randomFactor
    }
    
    private fun generateDynamicVolume(random: Random, stockInfo: StockInfo, sentiment: MarketSentiment, hour: Int, minute: Int): String {
        val baseVolume = when {
            stockInfo.name.contains("Apple") -> 45_000_000
            stockInfo.name.contains("Tesla") -> 25_000_000
            stockInfo.name.contains("Microsoft") -> 30_000_000
            stockInfo.name.contains("SPDR") -> 80_000_000
            stockInfo.sector == "ETF" -> 60_000_000
            stockInfo.isTechStock -> 20_000_000
            else -> 15_000_000
        }
        
        // Volume changes throughout the day realistically
        val timeMultiplier = when (hour) {
            9 -> 2.5 + random.nextDouble() * 0.5 // Opening spike
            10, 11 -> 1.5 + random.nextDouble() * 0.5 // Morning activity
            12, 13 -> 0.6 + random.nextDouble() * 0.3 // Lunch dip
            14, 15 -> 1.3 + random.nextDouble() * 0.4 // Afternoon pickup
            16 -> 2.0 + random.nextDouble() * 0.5 // Closing volume
            else -> 0.3 + random.nextDouble() * 0.2 // After hours
        }
        
        val sentimentMultiplier = when (sentiment) {
            MarketSentiment.VOLATILE -> 1.8 + random.nextDouble() * 0.7
            MarketSentiment.BULLISH, MarketSentiment.BEARISH -> 1.3 + random.nextDouble() * 0.5
            MarketSentiment.WEEKEND -> 0.2 + random.nextDouble() * 0.2
            else -> 0.9 + random.nextDouble() * 0.3
        }
        
        // Add minute-based micro fluctuations
        val microFluctuation = 1.0 + sin((minute * PI) / 30) * 0.1
        
        val volume = (baseVolume * timeMultiplier * sentimentMultiplier * microFluctuation).toLong()
        
        return when {
            volume >= 1_000_000 -> "${String.format("%.1f", volume / 1_000_000.0)}M"
            volume >= 1_000 -> "${String.format("%.1f", volume / 1_000.0)}K"
            else -> volume.toString()
        }
    }
    
    private fun generateHighDynamicVolume(random: Random, stockInfo: StockInfo, hour: Int, minute: Int): String {
        val baseVolume = when {
            stockInfo.name.contains("Apple") -> 85_000_000
            stockInfo.name.contains("Tesla") -> 55_000_000
            stockInfo.name.contains("SPDR") -> 150_000_000
            stockInfo.sector == "ETF" -> 120_000_000
            else -> 45_000_000
        }
        
        val timeMultiplier = when (hour) {
            9 -> 3.0 + random.nextDouble() // Massive opening volume
            10, 11 -> 2.0 + random.nextDouble() * 0.8
            15, 16 -> 2.5 + random.nextDouble() * 0.7 // Power hour
            else -> 1.2 + random.nextDouble() * 0.8
        }
        
        val volume = (baseVolume * timeMultiplier).toLong()
        
        return when {
            volume >= 1_000_000 -> "${String.format("%.1f", volume / 1_000_000.0)}M"
            volume >= 1_000 -> "${String.format("%.1f", volume / 1_000.0)}K"
            else -> volume.toString()
        }
    }
    
    private fun getCurrentTimestamp(): String {
        val calendar = Calendar.getInstance()
        return when {
            calendar.get(Calendar.HOUR_OF_DAY) in 9..16 -> {
                "🟢 Market Open - ${dateFormatter.format(calendar.time)} EST"
            }
            calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY -> {
                "🔴 Weekend - Last Updated ${dateFormatter.format(calendar.time)} EST"
            }
            else -> {
                "🟡 After Hours - ${dateFormatter.format(calendar.time)} EST"
            }
        }
    }
    
    fun generateCompanyOverview(symbol: String): CompanyOverview? {
        val stockInfo = stockDatabase[symbol] ?: return null
        val random = Random(symbol.hashCode().toLong())
        
        val currentPrice = stockInfo.minPrice + (stockInfo.maxPrice - stockInfo.minPrice) * random.nextDouble()
        val marketCap = (currentPrice * (500_000_000 + random.nextDouble() * 2_000_000_000)).toLong()
        
        return CompanyOverview(
            symbol = symbol,
            name = stockInfo.name,
            description = generateDescription(stockInfo),
            sector = stockInfo.sector,
            industry = generateIndustry(stockInfo.sector),
            marketCapitalization = marketCap.toString(),
            weekHigh52 = String.format("%.2f", stockInfo.maxPrice * (0.95 + random.nextDouble() * 0.1)),
            weekLow52 = String.format("%.2f", stockInfo.minPrice * (0.9 + random.nextDouble() * 0.1)),
            peRatio = String.format("%.1f", 15 + random.nextDouble() * 25),
            dividendYield = if (stockInfo.isTechStock) "0.00" else String.format("%.2f", random.nextDouble() * 3),
            eps = String.format("%.2f", 2 + random.nextDouble() * 8),
            revenuePerShare = String.format("%.2f", 10 + random.nextDouble() * 50)
        )
    }
    
    private fun generateDescription(stockInfo: StockInfo): String {
        return when {
            stockInfo.name.contains("Apple") -> "Apple Inc. designs, manufactures, and markets smartphones, personal computers, tablets, wearables, and accessories worldwide."
            stockInfo.name.contains("Tesla") -> "Tesla, Inc. designs, develops, manufactures, leases, and sells electric vehicles, and energy generation and storage systems."
            stockInfo.name.contains("Microsoft") -> "Microsoft Corporation develops, licenses, and supports software, services, devices, and solutions worldwide."
            stockInfo.name.contains("Amazon") -> "Amazon.com, Inc. engages in the retail sale of consumer products and subscriptions through online and physical stores."
            stockInfo.sector == "ETF" -> "Exchange-traded fund that tracks the performance of selected market indices."
            else -> "${stockInfo.name} operates in the ${stockInfo.sector.lowercase()} sector, providing various products and services to customers worldwide."
        }
    }
    
    private fun generateIndustry(sector: String): String {
        return when (sector) {
            "Technology" -> listOf("Software", "Semiconductors", "Internet Services", "Computer Hardware").random()
            "Financial" -> listOf("Banking", "Investment Services", "Insurance").random()
            "Healthcare" -> listOf("Pharmaceuticals", "Medical Devices", "Biotechnology").random()
            "Energy" -> listOf("Oil & Gas", "Renewable Energy", "Utilities").random()
            "Automotive" -> listOf("Auto Manufacturing", "Electric Vehicles", "Auto Parts").random()
            else -> sector
        }
    }
    
    enum class MarketSentiment {
        BULLISH, BEARISH, NEUTRAL, VOLATILE, WEEKEND, AFTER_HOURS
    }
    
    enum class BreakingNewsEffect {
        NONE, EARNINGS_BEAT, FED_ANNOUNCEMENT, TECH_BREAKTHROUGH, SECTOR_ROTATION
    }
} 