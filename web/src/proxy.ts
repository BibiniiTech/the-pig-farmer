import { NextRequest, NextResponse } from "next/server";
import createMiddleware from 'next-intl/middleware';
import { routing } from './i18n/routing';

const intlMiddleware = createMiddleware(routing);

export function proxy(request: NextRequest) {
  // First, run the internationalization middleware
  const response = intlMiddleware(request);

  // Perform device detection
  const ua = request.headers.get("user-agent") || "";
  const isMobile = /Mobile|Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Windows Phone/i.test(ua);
  const deviceType = isMobile ? "mobile" : "desktop";

  // Use the response from next-intl if available
  const finalResponse = response || NextResponse.next();

  finalResponse.headers.set("x-device-type", deviceType);
  finalResponse.cookies.set("device-type", deviceType, {
    path: "/",
    httpOnly: false,
    sameSite: "lax",
    maxAge: 60 * 60 * 24,
  });

  return finalResponse;
}

export const config = {
  matcher: ['/((?!api|_next|.*\\..*).*)']
};
