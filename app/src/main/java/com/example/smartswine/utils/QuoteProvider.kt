package com.example.smartswine.utils

import java.util.Calendar

object QuoteProvider {
    private val quoteKeys = (1..70).map { "quote_$it" }

    fun getQuoteOfDay(languageCode: String): String {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        
        // Use a consistent index per day
        val index = (dayOfYear + year) % quoteKeys.size
        return Translator.getString(quoteKeys[index], languageCode)
    }
}

