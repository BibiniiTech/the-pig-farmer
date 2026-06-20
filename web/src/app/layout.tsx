import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/context/AuthContext";
import { DeviceProvider } from "@/context/DeviceContext";
import Footer from "@/components/layouts/Footer";
import StaffLockoutWrapper from "@/components/StaffLockoutWrapper";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "SmartSwine - Farm Management Simplified",
  description: "Optimize your pig farm operations with our premium feed formulators, herd tracking, task management, and financial summaries.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col bg-white text-zinc-900 font-sans">
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
      </body>
    </html>
  );
}
