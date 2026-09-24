import type { NextConfig } from "next";
import withPWAInit from "@ducanh2912/next-pwa";
import createNextIntlPlugin from 'next-intl/plugin';

const withNextIntl = createNextIntlPlugin();

const withPWA = withPWAInit({
  dest: "public",
  cacheOnFrontEndNav: true,
  aggressiveFrontEndNavCaching: true,
  reloadOnOnline: true,
  // Show a custom offline page when the user has no connection
  fallbacks: {
    document: "/offline",
  },
  workboxOptions: {
    disableDevLogs: true,
  },
  // Disable PWA in development to avoid service worker conflicts
  disable: process.env.NODE_ENV === "development",
});

const nextConfig: NextConfig = {
  // Silence Turbopack vs webpack warning from next-pwa
  turbopack: {},
  async redirects() {
    return [
      { source: '/dashboard/hr', destination: '/dashboard?section=hr', permanent: false },
      { source: '/dashboard/human-resources', destination: '/dashboard?section=hr', permanent: false },
      { source: '/dashboard/hub', destination: '/dashboard?section=hub', permanent: false },
      { source: '/dashboard/market', destination: '/dashboard?section=hub', permanent: false },
      { source: '/dashboard/symptoms', destination: '/dashboard?section=symptoms', permanent: false },
      { source: '/dashboard/disease-finder', destination: '/dashboard?section=symptoms', permanent: false },
      { source: '/dashboard/symptoms-analyzer', destination: '/dashboard?section=symptoms', permanent: false },
      { source: '/dashboard/weight', destination: '/dashboard?section=weight', permanent: false },
      { source: '/dashboard/weight-checker', destination: '/dashboard?section=weight', permanent: false },
      { source: '/dashboard/training', destination: '/dashboard?section=training', permanent: false },
    ];
  },
};

export default withNextIntl(withPWA(nextConfig));
