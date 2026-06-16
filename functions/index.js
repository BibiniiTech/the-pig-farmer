const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const { google } = require("googleapis");
const logger = require("firebase-functions/logger");

admin.initializeApp();

// Load the Google Play developer service account keys
let serviceAccount;
try {
  serviceAccount = require("./service-account.json");
} catch (e) {
  logger.error("Missing service-account.json file! Please place it in the functions folder.");
}

let playDeveloperApi;
if (serviceAccount) {
  const auth = new google.auth.JWT(
    serviceAccount.client_email,
    null,
    serviceAccount.private_key,
    ["https://www.googleapis.com/auth/androidpublisher"]
  );

  playDeveloperApi = google.androidpublisher({
    version: "v3",
    auth: auth
  });
}

/**
 * Cloud Function to verify Google Play Store purchase tokens server-side.
 * Updates the Firestore document under 'users/{uid}' securely.
 */
exports.verifyPurchase = onCall({ maxInstances: 10 }, async (request) => {
  // Verify user authentication
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication required");
  }

  const { purchaseToken, productId, packageName } = request.data || {};
  if (!purchaseToken || !productId || !packageName) {
    throw new HttpsError("invalid-argument", "Missing required parameters: purchaseToken, productId, or packageName");
  }

  if (!playDeveloperApi) {
    throw new HttpsError("failed-precondition", "Billing verification service account is not configured.");
  }

  try {
    // Query Google Play Developer API
    const response = await playDeveloperApi.purchases.subscriptions.get({
      packageName: packageName,
      subscriptionId: productId,
      token: purchaseToken
    });

    const paymentState = response.data.paymentState; // 1 = Payment received
    const expiryTimeMillis = parseInt(response.data.expiryTimeMillis || "0");
    const now = Date.now();

    // Check if the subscription is active
    const isActive = paymentState === 1 && expiryTimeMillis > now;

    // Securely update the user's Firestore document
    const uid = request.auth.uid;
    await admin.firestore().collection("users").doc(uid).update({
      isPremium: isActive,
      premiumExpiry: expiryTimeMillis
    });

    logger.info(`Subscription verification for user ${uid}: active=${isActive}, expiry=${expiryTimeMillis}`);
    return { success: true, isPremium: isActive };
  } catch (error) {
    logger.error("Google Play Developer API request failed:", error);
    throw new HttpsError("internal", "Google Play verification failed: " + error.message);
  }
});
