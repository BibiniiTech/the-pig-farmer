import createMiddleware from 'next-intl/middleware';

export default createMiddleware({
  // A list of all locales that are supported
  locales: ['en', 'fr', 'zh', 'es', 'tl', 'vi', 'th', 'pt', 'hi'],

  // Used when no locale matches
  defaultLocale: 'en',

  // Don't use localized hrefs (e.g. /en/dashboard)
  localePrefix: 'never'
});

export const config = {
  // Match only internationalized pathnames
  matcher: ['/', '/(de|en)/:path*']
};
