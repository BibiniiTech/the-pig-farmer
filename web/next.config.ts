import type { NextConfig } from "next";
import withPWAInit from "@ducanh2912/next-pwa";

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
};

export default withPWA(nextConfig);
