import type { Metadata } from "next";
import "./globals.css";
import { AuthProvider } from "@/context/AuthContext";
import { DeviceProvider } from "@/context/DeviceContext";
import Footer from "@/components/layouts/Footer";
import StaffLockoutWrapper from "@/components/StaffLockoutWrapper";
import { NextIntlClientProvider } from "next-intl";
import { getMessages, getLocale } from "next-intl/server";
import Script from "next/script";

// Font variable definitions with standard system font fallbacks
const geistSans = { variable: "font-sans" };
const geistMono = { variable: "font-mono" };

export const metadata: Metadata = {
  title: "SmartSwine - Pig Farm Management",
  description: "Optimize your pig farm operations with our premium feed formulators, herd tracking, task management, and financial summaries.",
  icons: {
    icon: [
      { url: "/icons/icon-192x192.png", sizes: "192x192", type: "image/png" },
      { url: "/icons/icon-96x96.png", sizes: "96x96", type: "image/png" },
      { url: "/favicon.ico", sizes: "any" },
    ],
    apple: [
      { url: "/icons/apple-touch-icon.png", sizes: "180x180", type: "image/png" },
    ],
    shortcut: "/favicon.ico",
  },
};

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const locale = await getLocale();
  const messages = await getMessages();

  return (
    <html
      lang={locale}
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col bg-white text-zinc-900 font-sans">
        <Script
          async
          src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-4097392441181162"
          crossOrigin="anonymous"
          strategy="lazyOnload"
        />
        {/* Monetag Push Notification Tag (Zone 11929267) */}
        <Script
          src="https://5gvci.com/act/files/tag.min.js?z=11929267"
          data-cfasync="false"
          strategy="afterInteractive"
        />
        {/* Monetag Vignette / Rewarded Tag (Zone 11929302) */}
        <Script id="monetag-vignette" strategy="afterInteractive">
          {`(function(s){s.dataset.zone='11929302',s.src='https://n6wxm.com/vignette.min.js'})([document.documentElement, document.body].filter(Boolean).pop().appendChild(document.createElement('script')));`}
        </Script>
        <NextIntlClientProvider messages={messages} locale={locale}>
          <DeviceProvider>
            <AuthProvider>
              <StaffLockoutWrapper>
                <div className="flex flex-col min-h-screen">
                  <main className="flex-grow">
                    {children}
                  </main>
                  <Footer />
                </div>
              </StaffLockoutWrapper>
            </AuthProvider>
          </DeviceProvider>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
