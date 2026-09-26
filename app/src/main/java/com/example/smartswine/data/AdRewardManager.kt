package com.example.smartswine.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bibiniitech.smartswine.BuildConfig
import com.example.smartswine.utils.TierLimiter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AdRewardManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isPassActive = MutableStateFlow(false)
    val isPassActive: StateFlow<Boolean> = _isPassActive.asStateFlow()

    private val _passTimeRemainingFormatted = MutableStateFlow("")
    val passTimeRemainingFormatted: StateFlow<String> = _passTimeRemainingFormatted.asStateFlow()

    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    private var rewardedAd: RewardedAd? = null
    private var isLoadingAd = false

    private val tickerHandler = Handler(Looper.getMainLooper())
    private val tickerRunnable = object : Runnable {
        override fun run() {
            updatePassState()
            tickerHandler.postDelayed(this, 1000L)
        }
    }

    init {
        updatePassState()
        tickerHandler.post(tickerRunnable)
        loadRewardedAd()
    }

    fun loadRewardedAd() {
        if (isLoadingAd || rewardedAd != null) return
        isLoadingAd = true

        try {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                context,
                REWARDED_AD_UNIT_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Log.d("AdRewardManager", "RewardedAd loaded successfully.")
                        rewardedAd = ad
                        isLoadingAd = false
                        _isAdLoaded.value = true
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.w("AdRewardManager", "RewardedAd failed to load: ${loadAdError.message}")
                        rewardedAd = null
                        isLoadingAd = false
                        _isAdLoaded.value = false
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("AdRewardManager", "Failed to load RewardedAd", e)
            isLoadingAd = false
            _isAdLoaded.value = false
        }
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        val currentAd = rewardedAd
        if (currentAd == null) {
            // Fallback: If ad isn't loaded (e.g. poor network), grant pass to not punish genuine user, but log
            Log.w("AdRewardManager", "No ad ready. Granting pass as fallback.")
            activate3HourPass()
            onRewardEarned()
            loadRewardedAd()
            return
        }

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d("AdRewardManager", "Ad was dismissed.")
                rewardedAd = null
                _isAdLoaded.value = false
                loadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e("AdRewardManager", "Ad failed to show: ${adError.message}")
                rewardedAd = null
                _isAdLoaded.value = false
                loadRewardedAd()
                onFailure(adError.message)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("AdRewardManager", "Ad showed fullscreen content.")
            }
        }

        currentAd.show(activity) { _ ->
            Log.d("AdRewardManager", "User earned reward. Activating 3-hour pass.")
            activate3HourPass()
            onRewardEarned()
        }
    }

    fun activate3HourPass() {
        val expiresAt = System.currentTimeMillis() + TierLimiter.PASS_DURATION_MILLIS
        prefs.edit().putLong(KEY_PASS_EXPIRES_AT, expiresAt).apply()
        updatePassState()
    }

    fun updatePassState() {
        val expiresAt = prefs.getLong(KEY_PASS_EXPIRES_AT, 0L)
        val now = System.currentTimeMillis()
        val diff = expiresAt - now

        if (diff > 0) {
            _isPassActive.value = true
            val totalSeconds = diff / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            _passTimeRemainingFormatted.value = String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
        } else {
            _isPassActive.value = false
            _passTimeRemainingFormatted.value = ""
        }
    }

    companion object {
        private const val PREFS_NAME = "smart_swine_ad_rewards"
        private const val KEY_PASS_EXPIRES_AT = "key_pass_expires_at"

        // Official Google AdMob sample test ID for Rewarded Ads
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        // Production Rewarded Ad Unit ID
        const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-4097392441181162/6569302378"

        val REWARDED_AD_UNIT_ID: String
            get() = if (BuildConfig.DEBUG) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID

        @Volatile
        private var instance: AdRewardManager? = null

        fun getInstance(context: Context): AdRewardManager {
            return instance ?: synchronized(this) {
                instance ?: AdRewardManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
