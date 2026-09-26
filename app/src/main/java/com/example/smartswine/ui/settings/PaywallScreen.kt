package com.example.smartswine.ui.settings

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibiniitech.smartswine.R
import com.example.smartswine.data.BillingManager
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.utils.findActivity
import com.example.smartswine.utils.stringResource
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.LocalAppLanguage

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
                val isMonthly = offer.basePlanId.contains("monthly", ignoreCase = true) || product.productId.contains("monthly", ignoreCase = true)
                val planKey = if (isMonthly) "monthly_plan" else "annual_plan"
                
                val priceDisplay = if (formattedPrice.isNotEmpty()) {
                    formattedPrice
                } else {
                    if (isMonthly) "$2.00" else "$9.99"
                }
                
                val periodDisplay = if (isMonthly) Translator.getString("paywall_per_month", currentLanguageCode) else Translator.getString("paywall_per_year", currentLanguageCode)
                val subPrice = if (isMonthly) {
                    Translator.getString("paywall_subprice_monthly", currentLanguageCode)
                } else {
                    Translator.getString("paywall_subprice_annual", currentLanguageCode)
                }
                val badge = if (isMonthly) null else Translator.getString("paywall_badge_best_value", currentLanguageCode)

                result.add(
                    PaywallPlan(
                        productId = offer.basePlanId,
                        titleKey = planKey,
                        title = Translator.getString(planKey, currentLanguageCode),
                        price = priceDisplay,
                        periodDisplay = periodDisplay,
                        subPriceDisplay = subPrice,
                        badge = badge,
                        isAnnual = !isMonthly,
                        productDetails = product
                    )
                )
            }
            
            // Fallback for one-time products or simple subs without offer details
            if (product.subscriptionOfferDetails.isNullOrEmpty()) {
                val formattedPrice = product.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                val isMonthly = product.productId.contains("monthly", ignoreCase = true)
                val planKey = if (isMonthly) "monthly_plan" else "annual_plan"
                val priceDisplay = formattedPrice.ifEmpty { if (isMonthly) "$2.00" else "$9.99" }
                val periodDisplay = if (isMonthly) Translator.getString("paywall_per_month", currentLanguageCode) else Translator.getString("paywall_per_year", currentLanguageCode)
                val subPrice = if (isMonthly) Translator.getString("paywall_subprice_monthly", currentLanguageCode) else Translator.getString("paywall_subprice_annual", currentLanguageCode)
                val badge = if (isMonthly) null else Translator.getString("paywall_badge_best_value", currentLanguageCode)
                result.add(
                    PaywallPlan(
                        productId = product.productId,
                        titleKey = planKey,
                        title = Translator.getString(planKey, currentLanguageCode),
                        price = priceDisplay,
                        periodDisplay = periodDisplay,
                        subPriceDisplay = subPrice,
                        badge = badge,
                        isAnnual = !isMonthly,
                        productDetails = product
                    )
                )
            }
        }
        result.distinctBy { it.productId }
    }

    PaywallContent(
        plans = plans.ifEmpty {
            listOf(
                PaywallPlan(
                    productId = "premium-annual",
                    titleKey = "annual_plan",
                    title = Translator.getString("annual_plan", currentLanguageCode),
                    price = "$9.99",
                    periodDisplay = Translator.getString("paywall_per_year", currentLanguageCode),
                    subPriceDisplay = Translator.getString("paywall_subprice_annual", currentLanguageCode),
                    badge = Translator.getString("paywall_badge_best_value", currentLanguageCode),
                    isAnnual = true
                ),
                PaywallPlan(
                    productId = "premium-monthly",
                    titleKey = "monthly_plan",
                    title = Translator.getString("monthly_plan", currentLanguageCode),
                    price = "$2.00",
                    periodDisplay = Translator.getString("paywall_per_month", currentLanguageCode),
                    subPriceDisplay = Translator.getString("paywall_subprice_monthly", currentLanguageCode),
                    badge = null,
                    isAnnual = false
                )
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
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    // Sort plans so Annual is prioritized first
    val sortedPlans = remember(plans) {
        plans.sortedByDescending { it.isAnnual }
    }
    
    // Default selection is the Annual plan (or the first plan available)
    var selectedPlanId by remember(sortedPlans) {
        mutableStateOf(sortedPlans.find { it.isAnnual }?.productId ?: sortedPlans.firstOrNull()?.productId ?: "")
    }
    val selectedPlan = sortedPlans.find { it.productId == selectedPlanId } ?: sortedPlans.firstOrNull()

    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF071B11),
                Color(0xFF0C2417),
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.background
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE8F5E9),
                Color(0xFFF1F8E9),
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.background
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(40.dp)
                                .background(
                                    color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource("close"),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = onRestore,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = stringResource("restore_purchase"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── HERO BADGE & HEADLINE ──
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFB300).copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF133E25) else Color(0xFFE8F5E9),
                        border = BorderStroke(2.dp, Color(0xFFFFB300)),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource("smartswine_premium"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource("paywall_hero_subtitle"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── FEATURE MATRIX CARD ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDark) Color(0xFF142419) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF2B4D36) else Color(0xFFE0E0E0)
                    ),
                    shadowElevation = if (isDark) 0.dp else 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = stringResource("everything_included"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        PaywallFeatureItem(
                            icon = Icons.AutoMirrored.Filled.List,
                            title = stringResource("unlimited_pigs"),
                            description = stringResource("unlimited_pigs_desc")
                        )
                        PaywallFeatureItem(
                            icon = Icons.Default.PictureAsPdf,
                            title = stringResource("export_any_report"),
                            description = stringResource("export_any_report_desc")
                        )
                        PaywallFeatureItem(
                            icon = Icons.Default.Science,
                            title = stringResource("mix_balanced_feed"),
                            description = stringResource("mix_balanced_feed_desc")
                        )
                        PaywallFeatureItem(
                            icon = Icons.Default.Groups,
                            title = stringResource("staff_management"),
                            description = stringResource("staff_management_desc")
                        )
                        PaywallFeatureItem(
                            iconResId = R.drawable.ic_symptoms_analyzer,
                            title = stringResource("find_diseases_treatment"),
                            description = stringResource("find_diseases_treatment_desc")
                        )
                        PaywallFeatureItem(
                            icon = Icons.Default.Block,
                            title = stringResource("ad_free_experience"),
                            description = stringResource("ad_free_experience_desc")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── PLAN SELECTION CARDS ──
                Text(
                    text = stringResource("choose_your_plan"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                sortedPlans.forEach { plan ->
                    val isSelected = plan.productId == selectedPlanId
                    PaywallPlanCard(
                        plan = plan,
                        isSelected = isSelected,
                        onClick = { selectedPlanId = plan.productId },
                        isDark = isDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource("syncing_play_store_prices"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ── PRIMARY CTA BUTTON ──
                val ctaText = if (selectedPlan?.isAnnual == true) {
                    stringResource("continue_with_annual")
                } else {
                    stringResource("continue_with_monthly")
                }

                Button(
                    onClick = {
                        selectedPlan?.let { onPlanClick(it) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = ctaText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── TRUST PILLS ROW ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrustBadge(icon = Icons.Default.Lock, text = stringResource("google_play_secure"))
                    Text("•", color = MaterialTheme.colorScheme.outlineVariant)
                    TrustBadge(icon = Icons.Default.CheckCircle, text = stringResource("instant_access"))
                    Text("•", color = MaterialTheme.colorScheme.outlineVariant)
                    TrustBadge(icon = Icons.Default.Cancel, text = stringResource("cancel_anytime"))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── LEGAL & DISCLAIMER ──
                Text(
                    text = stringResource("auto_renewal_cancel_desc"),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun PaywallPlanCard(
    plan: PaywallPlan,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean
) {
    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 1.dp,
        label = "cardElevation"
    )

    val borderColor = if (isSelected) {
        Color(0xFFFFB300)
    } else {
        if (isDark) Color(0xFF2B4D36) else Color(0xFFE0E0E0)
    }

    val cardBg = if (isSelected) {
        if (isDark) Color(0xFF193B27) else Color(0xFFE8F5E9)
    } else {
        if (isDark) Color(0xFF142419) else Color.White
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(animatedElevation, RoundedCornerShape(20.dp), spotColor = if (isSelected) Color(0xFFFFB300) else Color.Transparent)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
    ) {
        // Clean golden badge positioned at top-right corner
        if (plan.badge != null) {
            Surface(
                color = Color(0xFFFFB300),
                shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 20.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = plan.badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = if (plan.badge != null) 24.dp else 18.dp,
                    bottom = 18.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Radio indicator + Plan title & Subtitle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onClick,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFFFFB300),
                            unselectedColor = if (isDark) Color(0xFF81C784).copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                    Column {
                        Text(
                            text = plan.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = plan.subPriceDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) (if (isDark) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Price display
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = plan.price,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = plan.periodDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaywallFeatureItem(
    icon: ImageVector? = null,
    iconResId: Int? = null,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (iconResId != null) {
                    Icon(
                        painter = painterResource(id = iconResId),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(18.dp)
                .align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun TrustBadge(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class PaywallPlan(
    val productId: String,
    val titleKey: String,
    val title: String,
    val price: String,
    val periodDisplay: String,
    val subPriceDisplay: String,
    val badge: String? = null,
    val isAnnual: Boolean = false,
    val productDetails: com.android.billingclient.api.ProductDetails? = null
)

@Preview(showBackground = true)
@Composable
fun PaywallScreenPreview() {
    SmartSwineTheme {
        PaywallContent(
            plans = listOf(
                PaywallPlan(
                    productId = "premium-annual",
                    titleKey = "annual_plan",
                    title = "Annual Plan",
                    price = "$9.99",
                    periodDisplay = "/ yr",
                    subPriceDisplay = "Just $0.83/month • Save 58%",
                    badge = "BEST VALUE • SAVE 58%",
                    isAnnual = true
                ),
                PaywallPlan(
                    productId = "premium-monthly",
                    titleKey = "monthly_plan",
                    title = "Monthly Plan",
                    price = "$2.00",
                    periodDisplay = "/ mo",
                    subPriceDisplay = "Flexible billing • Cancel anytime",
                    badge = null,
                    isAnnual = false
                )
            ),
            isLoading = false,
            onBack = {},
            onPlanClick = {},
            onRestore = {}
        )
    }
}

