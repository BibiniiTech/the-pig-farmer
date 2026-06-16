import { NextRequest, NextResponse } from "next/server";
import crypto from "crypto";
import { dbAdmin } from "@/lib/firebaseAdmin";

export async function POST(req: NextRequest) {
  try {
    const rawBody = await req.text();
    const signature = req.headers.get("x-signature") || "";
    const secret = process.env.LEMON_SQUEEZY_WEBHOOK_SECRET;

    if (!secret) {
      console.error("LEMON_SQUEEZY_WEBHOOK_SECRET is not configured.");
      return new NextResponse("Webhook secret missing", { status: 500 });
    }

    // Verify signature
    const hmac = crypto.createHmac("sha256", secret);
    const digest = hmac.update(rawBody).digest("hex");

    const digestBuffer = Buffer.from(digest, "hex");
    const signatureBuffer = Buffer.from(signature, "hex");

    if (
      digestBuffer.length !== signatureBuffer.length ||
      !crypto.timingSafeEqual(digestBuffer, signatureBuffer)
    ) {
      console.warn("Webhook signature verification failed.");
      return new NextResponse("Invalid signature", { status: 401 });
    }

    // Parse payload
    const payload = JSON.parse(rawBody);
    const eventName = payload.meta?.event_name;
    const customData = payload.meta?.custom_data;
    const userId = customData?.user_id || customData?.["user_id"];

    if (!userId) {
      console.warn("No user_id found in webhook custom_data.");
      // Return 200 OK so Lemon Squeezy knows we processed the request but found no target user
      return NextResponse.json({ received: true, warning: "No user_id" });
    }

    const userRef = dbAdmin.collection("users").doc(userId);

    console.log(`Processing Lemon Squeezy event "${eventName}" for user: ${userId}`);

    if (eventName === "subscription_created") {
      await userRef.set(
        {
          isPremium: true,
          subscriptionSource: "lemon_squeezy",
        },
        { merge: true }
      );
      console.log(`Premium enabled for user ${userId} (created)`);
    } else if (eventName === "subscription_updated") {
      const status = payload.data?.attributes?.status;
      const isPremiumActive = status === "active" || status === "on_trial";

      await userRef.set(
        {
          isPremium: isPremiumActive,
          subscriptionSource: isPremiumActive ? "lemon_squeezy" : "",
        },
        { merge: true }
      );
      console.log(
        `Premium status updated for user ${userId} (status: ${status}, active: ${isPremiumActive})`
      );
    } else if (eventName === "subscription_cancelled") {
      await userRef.set(
        {
          isPremium: false,
          subscriptionSource: "",
        },
        { merge: true }
      );
      console.log(`Premium disabled for user ${userId} (cancelled)`);
    } else {
      console.log(`Ignored unhandled event: ${eventName}`);
    }

    return NextResponse.json({ received: true });
  } catch (error: any) {
    console.error("Error handling Lemon Squeezy webhook:", error);
    return new NextResponse(
      JSON.stringify({ error: error.message }),
      {
        status: 500,
        headers: { "Content-Type": "application/json" },
      }
    );
  }
}
