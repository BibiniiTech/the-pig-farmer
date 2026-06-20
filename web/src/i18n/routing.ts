import {defineRouting} from 'next-intl/routing';

export const routing = defineRouting({
  // A list of all locales that are supported
  locales: ['en', 'es', 'fr', 'hi', 'pt', 'th', 'tl', 'vi', 'zh', 'sw', 'id', 'ht', 'my'],

  // Used when no locale matches
  defaultLocale: 'en',

  // Don't use localized hrefs (e.g. /en/dashboard)
  localePrefix: 'never'
});
