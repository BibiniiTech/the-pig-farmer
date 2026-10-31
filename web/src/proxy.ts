import { NextRequest, NextResponse } from "next/server";

export function proxy(request: NextRequest) {
  const host = request.headers.get("x-forwarded-host") || request.headers.get("host") || request.nextUrl.hostname || "";
  const isVercelDomain = host.toLowerCase().includes(".vercel.app");
  const pathname = request.nextUrl.pathname;

  // If traffic reaches any *.vercel.app domain, rewrite to the migration screen (/moved)
  if (isVercelDomain) {
    const isExcluded = 
      pathname.startsWith("/api") || 
      pathname.startsWith("/_next") || 
      pathname === "/moved" ||
      /\.(png|jpg|jpeg|gif|svg|ico|json|js|css|woff2?|txt|map)$/i.test(pathname);

    if (!isExcluded) {
      const movedUrl = new URL("/moved", request.url);
      return NextResponse.rewrite(movedUrl);
    }
  } else if (!host.includes("localhost") && !host.includes("127.0.0.1") && pathname === "/moved") {
    // If accessing /moved on the production custom domain (www.smartswine.app), redirect home
    return NextResponse.redirect(new URL("/", request.url));
  }

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
    const locales = [
      'en', 'es', 'es-es', 'es-do', 'fr', 'hi', 'pt', 'th', 'tl', 'vi', 'zh', 'sw', 'id', 'ht', 'my',
      'de', 'ja', 'lg', 'rw', 'tpi', 'zgh', 'af'
    ];
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
