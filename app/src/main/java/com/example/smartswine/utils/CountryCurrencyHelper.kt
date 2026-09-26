package com.example.smartswine.utils

import java.util.Currency
import java.util.Locale

data class CurrencyInfo(
    val code: String,
    val symbol: String
)

object CountryCurrencyHelper {

    private val countryToCurrency: Map<String, CurrencyInfo> = mapOf(
        "ghana" to CurrencyInfo("GHS", "GH₵"),
        "nigeria" to CurrencyInfo("NGN", "₦"),
        "united states" to CurrencyInfo("USD", "$"),
        "usa" to CurrencyInfo("USD", "$"),
        "united kingdom" to CurrencyInfo("GBP", "£"),
        "uk" to CurrencyInfo("GBP", "£"),
        "canada" to CurrencyInfo("CAD", "$"),
        "kenya" to CurrencyInfo("KES", "KSh"),
        "south africa" to CurrencyInfo("ZAR", "R"),
        "india" to CurrencyInfo("INR", "₹"),
        "philippines" to CurrencyInfo("PHP", "₱"),
        "vietnam" to CurrencyInfo("VND", "₫"),
        "thailand" to CurrencyInfo("THB", "฿"),
        "china" to CurrencyInfo("CNY", "¥"),
        "spain" to CurrencyInfo("EUR", "€"),
        "france" to CurrencyInfo("EUR", "€"),
        "germany" to CurrencyInfo("EUR", "€"),
        "italy" to CurrencyInfo("EUR", "€"),
        "netherlands" to CurrencyInfo("EUR", "€"),
        "ireland" to CurrencyInfo("EUR", "€"),
        "portugal" to CurrencyInfo("EUR", "€"),
        "australia" to CurrencyInfo("AUD", "$"),
        "new zealand" to CurrencyInfo("NZD", "$"),
        "brazil" to CurrencyInfo("BRL", "R$"),
        "mexico" to CurrencyInfo("MXN", "$"),
        "uganda" to CurrencyInfo("UGX", "USh"),
        "tanzania" to CurrencyInfo("TZS", "TSh"),
        "zambia" to CurrencyInfo("ZMW", "ZK"),
        "cameroon" to CurrencyInfo("XAF", "FCFA"),
        "ivory coast" to CurrencyInfo("XOF", "CFA"),
        "senegal" to CurrencyInfo("XOF", "CFA"),
        "mali" to CurrencyInfo("XOF", "CFA"),
        "burkina faso" to CurrencyInfo("XOF", "CFA"),
        "benin" to CurrencyInfo("XOF", "CFA"),
        "togo" to CurrencyInfo("XOF", "CFA"),
        "niger" to CurrencyInfo("XOF", "CFA"),
        "gabon" to CurrencyInfo("XAF", "FCFA"),
        "congo" to CurrencyInfo("XAF", "FCFA"),
        "chad" to CurrencyInfo("XAF", "FCFA"),
        "central african republic" to CurrencyInfo("XAF", "FCFA"),
        "equatorial guinea" to CurrencyInfo("XAF", "FCFA"),
        "liberia" to CurrencyInfo("LRD", "$"),
        "sierra leone" to CurrencyInfo("SLE", "Le"),
        "gambia" to CurrencyInfo("GMD", "D"),
        "rwanda" to CurrencyInfo("RWF", "FRw"),
        "burundi" to CurrencyInfo("BIF", "FBu"),
        "ethiopia" to CurrencyInfo("ETB", "Br"),
        "malawi" to CurrencyInfo("MWK", "MK"),
        "zimbabwe" to CurrencyInfo("ZWL", "$"),
        "botswana" to CurrencyInfo("BWP", "P"),
        "namibia" to CurrencyInfo("NAD", "$"),
        "japan" to CurrencyInfo("JPY", "¥"),
        "south korea" to CurrencyInfo("KRW", "₩"),
        "korea, south" to CurrencyInfo("KRW", "₩"),
        "indonesia" to CurrencyInfo("IDR", "Rp"),
        "malaysia" to CurrencyInfo("MYR", "RM"),
        "singapore" to CurrencyInfo("SGD", "$"),
        "pakistan" to CurrencyInfo("PKR", "Rs"),
        "bangladesh" to CurrencyInfo("BDT", "৳"),
        "sri lanka" to CurrencyInfo("LKR", "Rs"),
        "nepal" to CurrencyInfo("NPR", "Rs"),
        "egypt" to CurrencyInfo("EGP", "E£"),
        "morocco" to CurrencyInfo("MAD", "DH"),
        "colombia" to CurrencyInfo("COP", "$"),
        "argentina" to CurrencyInfo("ARS", "$"),
        "chile" to CurrencyInfo("CLP", "$"),
        "peru" to CurrencyInfo("PEN", "S/"),
    )

    fun getCurrencyForCountry(countryName: String, countryCode: String = ""): CurrencyInfo {
        val normalizedName = countryName.trim().lowercase()
        countryToCurrency[normalizedName]?.let { return it }

        // Try lookup via countryCode
        if (countryCode.isNotBlank()) {
            try {
                val locale = Locale.Builder().setRegion(countryCode.trim().uppercase()).build()
                val currency = Currency.getInstance(locale)
                if (currency != null) {
                    val symbol = currency.getSymbol(Locale.US)
                    return CurrencyInfo(currency.currencyCode, symbol)
                }
            } catch (_: Exception) { }
        }

        // Try available locales matching countryName
        if (countryName.isNotBlank()) {
            try {
                val locale = Locale.getAvailableLocales().firstOrNull {
                    it.displayCountry.equals(countryName, ignoreCase = true) ||
                            it.getDisplayCountry(Locale.ENGLISH).equals(countryName, ignoreCase = true)
                }
                if (locale != null) {
                    val currency = Currency.getInstance(locale)
                    if (currency != null) {
                        val symbol = currency.getSymbol(Locale.US)
                        return CurrencyInfo(currency.currencyCode, symbol)
                    }
                }
            } catch (_: Exception) { }
        }

        // Default fallback to USD
        return CurrencyInfo("USD", "$")
    }
}
