package com.example.smartswine.ui.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibiniitech.smartswine.R
import com.example.smartswine.data.BillingManager
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.utils.findActivity
import com.example.smartswine.utils.stringResource
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.StylishDivider

@Composable
fun PaywallScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val billingManager = remember { BillingManager.getInstance(context) }
    val productDetails by billingManager.productDetails.collectAsStateWithLifecycle()
    val isPremium by billingManager.isPremium.collectAsStateWithLifecycle()

    LaunchedEffect(isPremium) {
        if (isPremium == true) {
            onBack()
        }
    }

    val currentLanguageCode = LocalAppLanguage.current.code

    val plans = remember(productDetails, currentLanguageCode) {
        val result = mutableListOf<PaywallPlan>()
        productDetails.forEach { product ->
            product.subscriptionOfferDetails?.forEach { offer ->
                val formattedPrice = offer.pricingPhases.pricingPhaseList.firstOrNull()?.formattedPrice ?: ""
                
                // Identify plan type based on basePlanId or productId
                val isMonthly = offer.basePlanId.contains("monthly") || product.productId.contains("monthly")
                val planKey = if (isMonthly) "monthly_plan" else "annual_plan"
                
                val priceDisplay = if (formattedPrice.isNotEmpty()) {
                    val formatKey = if (isMonthly) "price_per_month" else "price_per_year"
                    Translator.getString(formatKey, currentLanguageCode, formattedPrice)
                } else {
                    val fallbackKey = if (isMonthly) "default_monthly_price" else "default_annual_price"
                    Translator.getString(fallbackKey, currentLanguageCode)
                }
                
                // Use basePlanId as the unique ID for this specific offer
                result.add(PaywallPlan(offer.basePlanId, planKey, priceDisplay, product))
            }
            
            // Fallback for one-time products or simple subs without offer details
            if (product.subscriptionOfferDetails.isNullOrEmpty()) {
                val formattedPrice = product.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                result.add(PaywallPlan(product.productId, "premium_access", formattedPrice, product))
            }
        }
        result.distinctBy { it.productId } // Ensure no duplicates
    }

    PaywallContent(
        plans = plans.ifEmpty {
            listOf(
                PaywallPlan("premium-monthly", "monthly_plan", Translator.getString("default_monthly_price", currentLanguageCode)),
                PaywallPlan("premium-annual", "annual_plan", Translator.getString("default_annual_price", currentLanguageCode))
            )
        },
        isLoading = productDetails.isEmpty(),
        onBack = onBack,
        onPlanClick = { plan ->
            if (plan.productDetails != null) {
                context.findActivity()?.let { activity ->
                    billingManager.launchBillingFlow(activity, plan.productDetails, plan.productId)
                }
            } else {
                android.util.Log.e("Paywall", "Cannot launch purchase: productDetails is null for ${plan.productId}")
            }
        },
        onRestore = { billingManager.queryPurchases() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaywallContent(
    plans: List<PaywallPlan>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onPlanClick: (PaywallPlan) -> Unit,
    onRestore: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.05f),
            contentScale = ContentScale.Crop
        )
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource("go_premium"),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        // Empty spacer to balance the close button
                        Spacer(modifier = Modifier.width(48.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            StylishDivider(modifier = Modifier.padding(bottom = 8.dp))
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource("premium_benefits"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource("gives_you"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            BenefitItem(icon = Icons.AutoMirrored.Filled.List, text = stringResource("unlimited_pigs"))
            BenefitItem(icon = Icons.Default.PictureAsPdf, text = stringResource("export_any_report"))
            BenefitItem(icon = Icons.Default.Science, text = stringResource("advanced_feed_formulator"))
            BenefitItem(icon = Icons.Default.Groups, text = stringResource("staff_management"))
            BenefitItem(iconResId = R.drawable.ic_symptoms_analyzer, text = stringResource("ai_symptoms_analyzer"))

            Spacer(modifier = Modifier.height(32.dp))

            val monthlyPlan = plans.find { it.productId == "premium-monthly" }
            val annualPlan = plans.find { it.productId == "premium-annual" }

            // Divider above monthly card
            StylishDivider(modifier = Modifier.padding(vertical = 12.dp))

            monthlyPlan?.let { plan ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPlanClick(plan)
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.White.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            )
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = stringResource(plan.titleKey),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = plan.price,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowCircleRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Divider after monthly card (and above annual card)
            StylishDivider(modifier = Modifier.padding(vertical = 12.dp))

            annualPlan?.let { plan ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPlanClick(plan)
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.White.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            )
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = stringResource(plan.titleKey),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = plan.price,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowCircleRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Best Value Badge
                    Surface(
                        color = Color(0xFFFFD700), // Gold / Amber
                        shape = RoundedCornerShape(topEnd = 20.dp, bottomStart = 12.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = stringResource("best_value"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Divider after annual card
            StylishDivider(modifier = Modifier.padding(vertical = 12.dp))

            if (isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connecting to Google Play Store...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (plans.isEmpty()) {
                Text(
                    text = "No subscription plans available at the moment. Please try again later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onRestore) {
                Text(stringResource("restore_purchase"))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource("auto_renewal_cancel_desc"),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
}

@Composable
fun BenefitItem(icon: ImageVector? = null, iconResId: Int? = null, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else if (iconResId != null) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

private data class PaywallPlan(
    val productId: String,
    val titleKey: String,
    val price: String,
    val productDetails: com.android.billingclient.api.ProductDetails? = null
)

@Preview(showBackground = true)
@Composable
fun PaywallScreenPreview() {
    SmartSwineTheme {
        PaywallContent(
            plans = listOf(
                PaywallPlan("premium-monthly", "monthly_plan", "Pay $4.99 to save up to x20 off less productive herd"),
                PaywallPlan("premium-annual", "annual_plan", "Pay $49.99 to save over x30 off commercial feed")
            ),
            isLoading = false,
            onBack = {},
            onPlanClick = {},
            onRestore = {}
        )
    }
}
