package com.example.smartfinance

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class SmartFinanceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("SmartFinancePrefs", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("THEME_DARK", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}