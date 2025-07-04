package com.stocktrading.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StockTradingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}