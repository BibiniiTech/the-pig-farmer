"use client";

import React, { useEffect } from "react";
import Link from "next/link";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";

export default function PrivacyPolicyPage() {
  const { isMobile } = useDevice();

  useEffect(() => {
    // Optional: Auto-redirect after a few seconds
    // const timer = setTimeout(() => {
    //   window.location.href = "https://sites.google.com/view/smartswine-privacypolicy/home";
    // }, 3000);
    // return () => clearTimeout(timer);
  }, []);

  return (
    <div className="relative min-h-screen bg-white text-zinc-900 flex flex-col font-sans overflow-hidden">
      {/* Watermark Logo Background */}
      {!isMobile && (
        <div className="fixed inset-0 z-0 flex items-center justify-center opacity-[0.15] pointer-events-none select-none">
          <img
            src="/app_logo.png"
            alt="Watermark Background Logo"
            className="w-full max-w-[1100px] max-h-[85vh] object-contain"
          />
        </div>
      )}

      <div className="relative z-10 flex flex-col min-h-screen">
        {/* Header */}
        {!isMobile && (
          <header className="border-b border-zinc-200 bg-white/80 backdrop-blur-md sticky top-0 z-50">
            <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
              <Link href="/dashboard" className="flex items-center gap-3 hover:opacity-80 transition-opacity cursor-pointer">
                <img src="/app_logo.png" alt="SmartSwine Logo" className="h-8 w-8 object-contain rounded-md" />
                <span className="font-bold text-sm bg-gradient-to-r from-emerald-700 via-emerald-600 to-green-500 bg-clip-text text-transparent mr-2 inline-block">
                  SmartSwine
                </span>
              </Link>

              <div className="flex items-center gap-2">
                <NavbarDropdown />
              </div>
            </div>
          </header>
        )}

        {/* Content Body */}
        <main className="flex-1 max-w-4xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12 space-y-12">
          <div className="text-center space-y-4">
            <h1 className="text-4xl font-extrabold text-zinc-900 tracking-tight">Privacy Policy</h1>
            <p className="text-sm text-zinc-500 font-medium">Last Updated: April 2026</p>
          </div>

          <div className="bg-emerald-50 border border-emerald-100 rounded-3xl p-8 space-y-6 shadow-sm text-center">
            <h2 className="text-2xl font-bold text-emerald-800">Your Data Privacy is Important to Us</h2>
            <p className="text-zinc-600 leading-relaxed text-lg max-w-2xl mx-auto">
              SmartSwine is committed to protecting your personal and farm data. We use industry-standard security measures to ensure your information is kept safe.
            </p>
            <div className="pt-4">
              <a
                href="https://sites.google.com/view/smartswine-privacypolicy/home"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center px-8 py-4 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-lg shadow-lg shadow-emerald-200 transition-all hover:scale-[1.02] active:scale-[0.98]"
              >
                View Full Privacy Policy ↗
              </a>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 pt-8">
            <div className="space-y-3">
              <h3 className="font-bold text-zinc-800 flex items-center gap-2">
                <span className="text-emerald-600">🛡️</span> Data Ownership
              </h3>
              <p className="text-zinc-600 leading-relaxed">
                You retain full ownership of all data you input into SmartSwine, including herd records, financials, and staff details.
              </p>
            </div>
            <div className="space-y-3">
              <h3 className="font-bold text-zinc-800 flex items-center gap-2">
                <span className="text-emerald-600">☁️</span> Cloud Security
              </h3>
              <p className="text-zinc-600 leading-relaxed">
                Your data is stored securely using Google Firebase, featuring encrypted storage and synchronization across your devices.
              </p>
            </div>
            <div className="space-y-3">
              <h3 className="font-bold text-zinc-800 flex items-center gap-2">
                <span className="text-emerald-600">🚫</span> No Third-Party Sales
              </h3>
              <p className="text-zinc-600 leading-relaxed">
                We do not sell, rent, or trade your personal or farm data to third parties for marketing purposes.
              </p>
            </div>
            <div className="space-y-3">
              <h3 className="font-bold text-zinc-800 flex items-center gap-2">
                <span className="text-emerald-600">🎯</span> Accuracy Responsibility
              </h3>
              <p className="text-zinc-600 leading-relaxed">
                You are responsible for the accuracy of the data entered. Correct data ensures precise AI diagnostic and feed calculations.
              </p>
            </div>
          </div>

          <div className="h-20" />
        </main>
      </div>
    </div>
  );
}
