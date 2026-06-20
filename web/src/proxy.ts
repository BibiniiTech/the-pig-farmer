import { NextRequest, NextResponse } from "next/server";

// Patterns that identify mobile / tablet user-agents
const MOBILE_UA_REGEX =
  /Mobile|Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Windows Phone/i;

export function proxy(request: NextRequest) {
  const ua = request.headers.get("user-agent") || "";
  const deviceType = MOBILE_UA_REGEX.test(ua) ? "mobile" : "desktop";

  const response = NextResponse.next();

  // Forward as a request header so server components can read it
  response.headers.set("x-device-type", deviceType);

  // Set a cookie so client components can read it without hydration mismatch
  response.cookies.set("device-type", deviceType, {
    path: "/",
    httpOnly: false,   // Needs to be readable by client JS
    sameSite: "lax",
    maxAge: 60 * 60 * 24, // 24 hours
  });

  return response;
}

export const config = {
  // Run on all pages except Next.js internals, static files, and API routes
  matcher: ["/((?!_next/static|_next/image|favicon.ico|icons|api).*)"],
};
