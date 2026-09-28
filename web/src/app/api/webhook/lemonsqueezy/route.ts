import { NextRequest, NextResponse } from "next/server";
import crypto from "crypto";
import { dbAdmin } from "@/lib/firebaseAdmin";

export async function POST(req: NextRequest) {
  try {
    const rawBody = await req.text();
    const signature = req.headers.get("x-signature") || "";
    const secret = process.env.LEMONSQUEEZY_WEBHOOK_SECRET;

    if (!secret) {
      console.error("LEMONSQUEEZY_WEBHOOK_SECRET is not configured.");
      return new NextResponse("Webhook secret missing", { status: 500 });
    }

    // Verify signature using HMAC SHA-256 with timing-safe comparison
    const hmac = crypto.createHmac("sha256", secret);
    const digest = Buffer.from(hmac.update(rawBody).digest("hex"), "utf8");
    const signatureBuffer = Buffer.from(signature, "utf8");

    if (
      digest.length !== signatureBuffer.length ||
      !crypto.timingSafeEqual(digest, signatureBuffer)
    ) {
      console.warn("Lemon Squeezy Webhook signature verification failed.");
      return new NextResponse("Invalid signature", { status: 401 });
    }

    const payload = JSON.parse(rawBody);
    const eventName = payload.meta?.event_name;
    const customData = payload.meta?.custom_data;
    let userId: string | null = customData?.user_id ? String(customData.user_id) : null;

    const data = payload.data || {};
    const attributes = data.attributes || {};
    const subscriptionId = String(data.id || attributes.subscription_id || "");
    const customerId = String(attributes.customer_id || "");
    const variantId = String(attributes.variant_id || "");
    const customerEmail = attributes.user_email?.toLowerCase()?.trim();
    const portalUrl = attributes.urls?.customer_portal || null;
    const status = attributes.status; // "active", "on_trial", "past_due", "paused", "cancelled", "expired", "unpaid"
    const renewsAt = attributes.renews_at || null;
    const endsAt = attributes.ends_at || null;

    let userDocRef: FirebaseFirestore.DocumentReference | null = null;

    if (userId) {
      userDocRef = dbAdmin.collection("users").doc(userId);
    } else {
      // Find user by subscriptionId, customerId, or email
      if (subscriptionId) {
        const snap = await dbAdmin
          .collection("users")
          .where("lemonSqueezySubscriptionId", "==", subscriptionId)
          .limit(1)
          .get();
        if (!snap.empty) {
          userDocRef = snap.docs[0].ref;
          userId = snap.docs[0].id;
        }
      }

      if (!userDocRef && customerId) {
        const snap = await dbAdmin
          .collection("users")
          .where("lemonSqueezyCustomerId", "==", customerId)
          .limit(1)
          .get();
        if (!snap.empty) {
          userDocRef = snap.docs[0].ref;
          userId = snap.docs[0].id;
        }
      }

      if (!userDocRef && customerEmail) {
        const snap = await dbAdmin
          .collection("users")
          .where("email", "==", customerEmail)
          .limit(1)
          .get();
        if (!snap.empty) {
          userDocRef = snap.docs[0].ref;
          userId = snap.docs[0].id;
        }
      }
    }

    if (!userDocRef || !userId) {
      console.warn(
        `No user resolved for Lemon Squeezy event "${eventName}". Subscription: ${subscriptionId}, Customer: ${customerId}`
      );
      return NextResponse.json({ received: true, warning: "User not found" });
    }

    console.log(`Processing Lemon Squeezy event "${eventName}" for user: ${userId}, status: ${status}`);

    const isSubscriptionActive =
      status === "active" ||
      status === "on_trial" ||
      status === "paid" ||
      eventName === "subscription_payment_success";

    if (
      eventName === "subscription_created" ||
      eventName === "subscription_updated" ||
      eventName === "subscription_resumed" ||
      eventName === "subscription_unpaused" ||
      eventName === "subscription_plan_changed" ||
      eventName === "subscription_payment_success"
    ) {
      const updates: Record<string, any> = {
        isPremium: isSubscriptionActive,
        subscriptionSource: "lemonsqueezy",
        updatedAt: Date.now(),
      };

      if (customerId) updates.lemonSqueezyCustomerId = customerId;
      if (subscriptionId) updates.lemonSqueezySubscriptionId = subscriptionId;
      if (variantId) updates.lemonSqueezyVariantId = variantId;
      if (renewsAt) updates.lemonSqueezyRenewsAt = renewsAt;
      if (endsAt) updates.lemonSqueezyEndsAt = endsAt;
      if (portalUrl) {
        updates.lemonSqueezyCustomerPortalUrl = portalUrl;
      }

      await userDocRef.set(updates, { merge: true });
      console.log(`Updated subscription for user ${userId}: isPremium=${isSubscriptionActive}`);
    } else if (eventName === "subscription_cancelled") {
      // In Lemon Squeezy, cancelled means it won't renew, but remains active until ends_at
      const isStillActive = endsAt ? new Date(endsAt).getTime() > Date.now() : true;
      const updates: Record<string, any> = {
        isPremium: isStillActive,
        updatedAt: Date.now(),
      };
      if (endsAt) updates.lemonSqueezyEndsAt = endsAt;
      if (portalUrl) {
        updates.lemonSqueezyCustomerPortalUrl = portalUrl;
      }
      await userDocRef.set(updates, { merge: true });
      console.log(`Subscription marked as cancelled for user ${userId}, active=${isStillActive}, ends at: ${endsAt}`);
    } else if (
      eventName === "subscription_expired" ||
      status === "expired" ||
      status === "unpaid"
    ) {
      const currentDocSnap = await userDocRef.get();
      const currentData = currentDocSnap.data() || {};

      // Protect non-Lemon Squeezy users (e.g. Google Play Store or Admins)
      if (
        currentData.subscriptionSource &&
        currentData.subscriptionSource !== "lemonsqueezy"
      ) {
        console.log(
          `Skipping revocation for user ${userId}: active source is "${currentData.subscriptionSource}"`
        );
        return NextResponse.json({
          received: true,
          note: "Different subscription source",
        });
      }

      if (currentData.admin === true || currentData.isKofisPerson === true) {
        console.log(
          `Skipping revocation for user ${userId}: account has admin privileges`
        );
        return NextResponse.json({
          received: true,
          note: "Admin account protected",
        });
      }

      await userDocRef.set(
        {
          isPremium: false,
          subscriptionSource: "",
          updatedAt: Date.now(),
        },
        { merge: true }
      );
      console.log(`Premium revoked for user ${userId} (${eventName})`);
    } else {
      console.log(`Unhandled or informational Lemon Squeezy event: ${eventName}`);
    }

    return NextResponse.json({ received: true });
  } catch (error: any) {
    console.error("Error handling Lemon Squeezy webhook:", error);
    return new NextResponse(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }
}
