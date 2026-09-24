import {defineRouting} from 'next-intl/routing';

export const routing = defineRouting({
  locales: [
    'en', 'es', 'es-es', 'es-do', 'fr', 'hi', 'pt', 'th', 'tl', 'vi', 'zh', 'sw', 'id', 'ht', 'my',
    'de', 'ja', 'lg', 'rw', 'tpi', 'zgh', 'af'
  ],

  // Used when no locale matches
  defaultLocale: 'en',

  // Don't use localized hrefs (e.g. /en/dashboard)
  localePrefix: 'never'
});
