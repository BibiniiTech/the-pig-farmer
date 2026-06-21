import { NextRequest, NextResponse } from "next/server";

export function proxy(request: NextRequest) {
  // Perform device detection
  const ua = request.headers.get("user-agent") || "";
  const isMobile = /Mobile|Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Windows Phone/i.test(ua);
  const deviceType = isMobile ? "mobile" : "desktop";

  const response = NextResponse.next();

  response.headers.set("x-device-type", deviceType);
  response.cookies.set("device-type", deviceType, {
    path: "/",
    httpOnly: false,
    sameSite: "lax",
    maxAge: 60 * 60 * 24,
  });

  // If the NEXT_LOCALE cookie is not set, detect from Accept-Language header
  if (!request.cookies.has("NEXT_LOCALE")) {
    const acceptLanguage = request.headers.get("accept-language") || "";
    // Supported locales in the app
    const locales = ['en', 'es', 'fr', 'hi', 'pt', 'th', 'tl', 'vi', 'zh', 'sw', 'id', 'ht', 'my'];
    let detectedLocale = 'en';

    for (const lang of locales) {
      if (
        acceptLanguage.toLowerCase().startsWith(lang) || 
        acceptLanguage.toLowerCase().includes(`,${lang}`) || 
        acceptLanguage.toLowerCase().includes(`;${lang}`)
      ) {
        detectedLocale = lang;
        break;
      }
    }

    response.cookies.set("NEXT_LOCALE", detectedLocale, {
      path: "/",
      maxAge: 60 * 60 * 24 * 365, // 1 year
    });
  }

  return response;
}

export const config = {
  matcher: ['/((?!api|_next|.*\\..*).*)']
};
