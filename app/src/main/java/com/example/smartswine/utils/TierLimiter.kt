package com.example.smartswine.utils

object TierLimiter {
    const val FREE_MAX_PIGS = 20
    const val FREE_MAX_FINANCIAL_RECORDS = 20
    const val FREE_MAX_STAFF = 3
    const val FREE_MAX_FEED_INGREDIENTS = 2
    const val PASS_DURATION_MILLIS = 3 * 60 * 60 * 1000L // 3 Hours

    fun isPassOrPaidActive(userDocIsPremium: Boolean = false): Boolean {
        if (userDocIsPremium) return true
        return com.example.smartswine.SmartSwineApplication.appContext?.let { context ->
            com.example.smartswine.data.AdRewardManager.getInstance(context).isPassActive.value
        } ?: false
    }
}
