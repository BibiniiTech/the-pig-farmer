import { NextRequest, NextResponse } from "next/server";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const { variantId, userId, userEmail, userName } = body;

    if (!variantId || !userId) {
      return NextResponse.json(
        { error: "variantId and userId are required" },
        { status: 400 }
      );
    }

    const apiKey = process.env.LEMONSQUEEZY_API_KEY;
    const storeId = process.env.LEMONSQUEEZY_STORE_ID || "409123";

    if (!apiKey) {
      console.error("LEMONSQUEEZY_API_KEY is not configured.");
      return NextResponse.json(
        { error: "Billing service is not properly configured" },
        { status: 500 }
      );
    }

    // Dynamically detect origin so users return to whichever domain they were browsing (smartswine.app or smartswine.vercel.app)
    const appUrl =
      req.headers.get("origin") ||
      req.nextUrl.origin ||
      process.env.NEXT_PUBLIC_APP_URL ||
      "https://smartswine.app";

    const response = await fetch("https://api.lemonsqueezy.com/v1/checkouts", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/vnd.api+json",
        Accept: "application/vnd.api+json",
      },
      body: JSON.stringify({
        data: {
          type: "checkouts",
          attributes: {
            checkout_data: {
              email: userEmail ? String(userEmail).trim() : undefined,
              name: userName ? String(userName).trim() : undefined,
              custom: {
                user_id: String(userId),
              },
            },
            checkout_options: {
              embed: true,
              media: true,
              logo: true,
            },
            product_options: {
              redirect_url: `${appUrl}/dashboard/billing?status=success`,
            },
          },
          relationships: {
            store: {
              data: {
                type: "stores",
                id: String(storeId),
              },
            },
            variant: {
              data: {
                type: "variants",
                id: String(variantId),
              },
            },
          },
        },
      }),
    });

    const data = await response.json();

    if (!response.ok || !data?.data?.attributes?.url) {
      console.error("Lemon Squeezy checkout creation error:", data);
      const errorMessage =
        data?.errors?.[0]?.detail ||
        data?.message ||
        "Failed to create checkout session with Lemon Squeezy";
      return NextResponse.json({ error: errorMessage }, { status: 400 });
    }

    return NextResponse.json({
      success: true,
      url: data.data.attributes.url,
    });
  } catch (error: any) {
    console.error("Error creating Lemon Squeezy checkout:", error);
    return NextResponse.json(
      { error: error.message || "Internal server error" },
      { status: 500 }
    );
  }
}
