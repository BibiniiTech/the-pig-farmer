package com.example.smartswine.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(private val context: Context) {
    private val functions = FirebaseFunctions.getInstance()
    private val billingClient: BillingClient

    private val _isPremium = MutableStateFlow<Boolean?>(null)
    val isPremium: StateFlow<Boolean?> = _isPremium.asStateFlow()

    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if ((billingResult.responseCode == BillingClient.BillingResponseCode.OK) && (purchases != null)) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    init {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(pendingPurchasesParams)
            .enableAutoServiceReconnection()
            .build()
        
        startConnection()
    }

    private fun startConnection() {
        Log.d("BillingManager", "Starting connection to BillingClient...")
        billingClient.startConnection(
            object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d("BillingManager", "Billing setup finished. Response code: ${billingResult.responseCode}, Message: ${billingResult.debugMessage}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("BillingManager", "Billing service disconnected. Attempting to reconnect...")
                startConnection()
            }
        },
        )
    }

    private fun queryProductDetails() {
        Log.d("BillingManager", "Querying product details for [premium-monthly, premium-annual, premium_access]...")
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium-monthly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium-annual")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_access")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            val responseCode = billingResult.responseCode
            Log.d("BillingManager", "Query finished. Response code: $responseCode")
            if (responseCode == BillingClient.BillingResponseCode.OK) {
                val details = queryProductDetailsResult.productDetailsList
                Log.d("BillingManager", "Found ${details.size} products")
                _productDetails.value = details
            } else {
                Log.e("BillingManager", "Error querying products: ${billingResult.debugMessage}")
            }
        }
    }

    fun queryPurchases() {
        Log.d("BillingManager", "Querying purchases...")
        if (!billingClient.isReady) {
            Log.w("BillingManager", "BillingClient is not ready for queryPurchases")
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            Log.d("BillingManager", "Purchases query finished. Response code: ${billingResult.responseCode}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d("BillingManager", "Found ${purchases.size} purchases")
                checkPremiumStatus(purchases)
                for (purchase in purchases) {
                    if ((purchase.purchaseState == Purchase.PurchaseState.PURCHASED) && !purchase.isAcknowledged) {
                        Log.d("BillingManager", "Acknowledging unacknowledged purchase: ${purchase.orderId}")
                        handlePurchase(purchase)
                    }
                }
            } else {
                Log.e("BillingManager", "Error querying purchases: ${billingResult.debugMessage}")
            }
        }
    }

    private fun checkPremiumStatus(purchases: List<Purchase>) {
        val activePurchases = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (activePurchases.isEmpty()) {
            _isPremium.value = false
            return
        }

        var checkedCount = 0
        var anyPremium = false
        for (purchase in activePurchases) {
            verifyPurchaseOnServer(purchase) { isValid ->
                checkedCount++
                if (isValid) {
                    anyPremium = true
                }
                if (checkedCount == activePurchases.size) {
                    _isPremium.value = anyPremium
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        Log.d("BillingManager", "Handling purchase: ${purchase.orderId}, state: ${purchase.purchaseState}, acknowledged: ${purchase.isAcknowledged}")
        verifyPurchaseOnServer(purchase) { verified ->
            if (!verified) {
                Log.e("BillingManager", "Purchase verification failed for purchase: ${purchase.orderId}")
                return@verifyPurchaseOnServer
            }
            if ((purchase.purchaseState == Purchase.PurchaseState.PURCHASED) && !purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    Log.d("BillingManager", "Acknowledge purchase finished. Response code: ${billingResult.responseCode}")
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _isPremium.value = true
                    }
                }
            } else if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                _isPremium.value = true
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails, planId: String) {
        // Find the specific offer token for the requested base plan
        val offerDetails = productDetails.subscriptionOfferDetails
        val offerToken = offerDetails?.find { it.basePlanId == planId }?.offerToken
            ?: offerDetails?.firstOrNull()?.offerToken

        if (offerToken == null) {
            Log.e("BillingManager", "Could not find offer token for $planId in ${productDetails.productId}")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build(),
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun verifyPurchaseOnServer(purchase: Purchase, onComplete: (Boolean) -> Unit) {
        val data = hashMapOf(
            "purchaseToken" to purchase.purchaseToken,
            "productId" to (purchase.products.firstOrNull() ?: ""),
            "packageName" to context.packageName
        )

        functions
            .getHttpsCallable("verifyPurchase")
            .call(data)
            .addOnSuccessListener { taskResult ->
                val result = taskResult.data as? Map<*, *>
                val success = result?.get("success") as? Boolean == true
                val isPremium = result?.get("isPremium") as? Boolean == true
                onComplete(success && isPremium)
            }
            .addOnFailureListener { e ->
                Log.e("BillingManager", "Server-side purchase verification failed: ${e.message}")
                onComplete(verifyPurchaseSignature(purchase))
            }
    }

    private fun verifyPurchaseSignature(purchase: Purchase): Boolean {
        return com.example.smartswine.utils.SecurityUtils.verifyPurchase(
            BASE64_PUBLIC_KEY,
            purchase.originalJson,
            purchase.signature,
        )
    }

    companion object {
        private const val KEY_PART_1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoTAsgYu08GhqgegWTW+Fc2JeNThWbZFgEkwHRY4hEaMexThzNRL9drH6/1G8OrGaRUk"
        private const val KEY_PART_2 = "QLy3uoDJqSi5zAPOG/QC2wvpzjG99ntnIzvItFdQZijYByUR7KwXeb8ArOgBlNFosKAJkG3BU8F5gIknSYGsXwtHGZQiqpbbOb2Y2u0ACKcpz"
        private const val KEY_PART_3 = "dsr9sbYozXdleuRNAmoINRuoFR2TQAUE2RlPD4547wiCX4uXMarG8fWYO5fgi4gmp0DoSvCm3jDK4xPO6RpwJBtzHlIUXD8JpeaRLKSWj6iG"
        private const val KEY_PART_4 = "uCuEkSVt2UN5knQzQ0z2/IhQgy6nhy3s974vSvPEP0W70GRLeUPEmxQCaQIDAQAB"

        val BASE64_PUBLIC_KEY: String
            get() = KEY_PART_1 + KEY_PART_2 + KEY_PART_3 + KEY_PART_4

        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
