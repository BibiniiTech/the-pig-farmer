import { NextRequest, NextResponse } from "next/server";
import crypto from "crypto";
import { dbAdmin } from "@/lib/firebaseAdmin";

export async function POST(req: NextRequest) {
  try {
    const rawBody = await req.text();
    const signature = req.headers.get("x-paystack-signature") || "";
    const secret = process.env.PAYSTACK_SECRET_KEY;

    if (!secret) {
      console.error("PAYSTACK_SECRET_KEY is not configured.");
      return new NextResponse("Webhook secret missing", { status: 500 });
    }

    // Verify signature using HMAC SHA512
    const hmac = crypto.createHmac("sha512", secret);
    const digest = hmac.update(rawBody).digest("hex");

    if (digest !== signature) {
      console.warn("Paystack Webhook signature verification failed.");
      return new NextResponse("Invalid signature", { status: 401 });
    }

    // Parse payload
    const payload = JSON.parse(rawBody);
    const eventName = payload.event;
    const customData = payload.data?.metadata?.custom_fields || payload.data?.metadata;
    
    // Extract userId (Paystack sometimes nests it depending on how frontend passes metadata)
    let userId = null;
    if (customData?.user_id) {
      userId = customData.user_id;
    } else if (Array.isArray(customData)) {
       const userField = customData.find((f: any) => f.variable_name === "user_id");
       if (userField) userId = userField.value;
    }

    if (!userId) {
      console.warn("No user_id found in Paystack webhook metadata.");
      return NextResponse.json({ received: true, warning: "No user_id" });
    }

    const userRef = dbAdmin.collection("users").doc(userId);

    console.log(`Processing Paystack event "${eventName}" for user: ${userId}`);

    if (eventName === "charge.success") {
      await userRef.set(
        {
          isPremium: true,
          subscriptionSource: "paystack",
        },
        { merge: true }
      );
      console.log(`Premium enabled for user ${userId} (charge.success)`);
    } else if (eventName === "subscription.create") {
      await userRef.set(
        {
          isPremium: true,
          subscriptionSource: "paystack",
        },
        { merge: true }
      );
      console.log(`Premium enabled for user ${userId} (subscription.create)`);
    } else if (eventName === "subscription.disable" || eventName === "subscription.not_renew") {
      await userRef.set(
        {
          isPremium: false,
          subscriptionSource: "",
        },
        { merge: true }
      );
      console.log(`Premium disabled for user ${userId} (cancelled/disabled)`);
    } else {
      console.log(`Ignored unhandled event: ${eventName}`);
    }

    return NextResponse.json({ received: true });
  } catch (error: any) {
    console.error("Error handling Paystack webhook:", error);
    return new NextResponse(
      JSON.stringify({ error: error.message }),
      {
        status: 500,
        headers: { "Content-Type": "application/json" },
      }
    );
  }
}
