package com.example.smartswine.ui.components

import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.bibiniitech.smartswine.BuildConfig
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
const val PROD_NATIVE_AD_UNIT_ID = "ca-app-pub-4097392441181162/5339981183"

val DEFAULT_NATIVE_AD_UNIT_ID: String
    get() = if (BuildConfig.DEBUG) TEST_NATIVE_AD_UNIT_ID else PROD_NATIVE_AD_UNIT_ID

@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = DEFAULT_NATIVE_AD_UNIT_ID
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var adFailedToLoad by remember { mutableStateOf(false) }

    DisposableEffect(adUnitId) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
                adFailedToLoad = false
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    adFailedToLoad = true
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAd?.destroy()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "SPONSORED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val currentAd = nativeAd
            if (currentAd != null) {
                AndroidView(
                    factory = { ctx ->
                        NativeAdView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )

                            val container = LinearLayout(ctx).apply {
                                orientation = LinearLayout.VERTICAL
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            }

                            // Headline
                            val headlineView = TextView(ctx).apply {
                                text = currentAd.headline ?: "Recommended Farm Partner"
                                textSize = 15f
                                setTypeface(null, android.graphics.Typeface.BOLD)
                                setTextColor(android.graphics.Color.DKGRAY)
                            }
                            container.addView(headlineView)
                            this.headlineView = headlineView

                            // Body
                            currentAd.body?.let { bodyText ->
                                val bodyView = TextView(ctx).apply {
                                    text = bodyText
                                    textSize = 12f
                                    setTextColor(android.graphics.Color.GRAY)
                                    setPadding(0, 6, 0, 8)
                                }
                                container.addView(bodyView)
                                this.bodyView = bodyView
                            }

                            // Call to Action
                            currentAd.callToAction?.let { ctaText ->
                                val ctaButton = Button(ctx).apply {
                                    text = ctaText
                                    textSize = 12f
                                }
                                container.addView(ctaButton)
                                this.callToActionView = ctaButton
                            }

                            addView(container)
                            setNativeAd(currentAd)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Fallback graceful card while loading or if offline
                Text(
                    text = "SmartSwine Agricultural Partner",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Find high quality swine genetics, automated pig feeders, and premium feed ingredients.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
