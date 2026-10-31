"use client";

import React, { useState, useEffect } from "react";

const TARGET_URL = "https://www.smartswine.app";
const REDIRECT_DELAY_SECONDS = 5;

export default function MigrationNoticeContent() {
  const [countdown, setCountdown] = useState(REDIRECT_DELAY_SECONDS);

  useEffect(() => {
    // Decrement countdown every 1 second
    const interval = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          // Trigger redirect
          if (typeof window !== "undefined") {
            window.location.replace(TARGET_URL);
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, []);

  const progressPercentage = (countdown / REDIRECT_DELAY_SECONDS) * 100;

  return (
    <div className="fixed inset-0 z-[99999] overflow-y-auto bg-gradient-to-br from-emerald-50 via-zinc-50 to-white flex items-center justify-center p-4 sm:p-6 font-sans">
      <div className="w-full max-w-xl mx-auto bg-white rounded-3xl shadow-2xl shadow-emerald-950/10 border border-zinc-200/80 p-6 sm:p-10 text-center relative overflow-hidden">
        
        {/* Subtle decorative background glow */}
        <div className="absolute -top-24 -left-24 w-48 h-48 bg-emerald-100 rounded-full blur-3xl opacity-60 pointer-events-none" />
        <div className="absolute -bottom-24 -right-24 w-48 h-48 bg-emerald-100 rounded-full blur-3xl opacity-60 pointer-events-none" />

        {/* Status Badge */}
        <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-emerald-100 text-emerald-800 text-xs sm:text-sm font-semibold mb-6">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-600"></span>
          </span>
          Platform Relocation Notice
        </div>

        {/* 1. App Logo - Clickable */}
        <div className="mb-4">
          <a
            href={TARGET_URL}
            className="group inline-block transition-transform duration-200 hover:scale-105 active:scale-95 focus:outline-none"
            title="Click to visit www.smartswine.app"
          >
            <div className="relative inline-block">
              <img
                src="/app_logo.png"
                alt="SmartSwine App Logo"
                className="h-24 w-24 sm:h-28 sm:w-28 object-contain rounded-2xl shadow-md border border-emerald-200/80 bg-white p-2 group-hover:shadow-xl group-hover:border-emerald-400 transition-all duration-200"
              />
              <span className="absolute -bottom-2 -right-2 bg-emerald-600 text-white rounded-full p-1.5 shadow-md">
                <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
                </svg>
              </span>
            </div>
          </a>
        </div>

        {/* 2. App Name - Clickable */}
        <div className="mb-2">
          <a
            href={TARGET_URL}
            className="inline-block text-3xl sm:text-4xl font-extrabold text-zinc-900 tracking-tight hover:text-emerald-600 transition-colors duration-150 focus:outline-none"
            title="Click to visit www.smartswine.app"
          >
            SmartSwine
          </a>
          <p className="text-xs sm:text-sm font-bold text-emerald-800 tracking-wider uppercase mt-1">
            Pig Farm Management System
          </p>
        </div>

        {/* 3. Note that we have moved to www.smartswine.app - Clickable */}
        <div className="mt-6 mb-6">
          <a
            href={TARGET_URL}
            className="group block w-full bg-emerald-50/90 hover:bg-emerald-100/90 border-2 border-emerald-500/40 hover:border-emerald-500 rounded-2xl p-5 sm:p-6 transition-all duration-200 shadow-sm hover:shadow-md text-left sm:text-center focus:outline-none"
            title="Click to visit www.smartswine.app"
          >
            <div className="flex items-center justify-center gap-2 text-lg sm:text-2xl font-extrabold text-emerald-700 group-hover:text-emerald-800 transition-colors">
              <span>We have moved to www.smartswine.app</span>
              <svg
                className="w-5 h-5 sm:w-6 sm:h-6 shrink-0 transition-transform duration-200 group-hover:translate-x-1"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth="2.5"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M13.5 6H5.25A2.25 2.25 0 003 8.25v10.5A2.25 2.25 0 005.25 21h10.5A2.25 2.25 0 0018 18.75V10.5m-10.5 6L21 3m0 0h-5.25M21 3v5.25"
                />
              </svg>
            </div>
            <p className="mt-2 text-xs sm:text-sm text-zinc-600 group-hover:text-zinc-900 transition-colors">
              Our official web platform has relocated to our dedicated custom domain.
              Click anywhere on this note, the logo, or the name above to go directly to{" "}
              <span className="font-semibold text-emerald-700 underline underline-offset-2">www.smartswine.app</span>.
            </p>
          </a>
        </div>

        {/* Auto-redirect Countdown Notice & Progress Bar */}
        <div className="mb-6 space-y-2">
          <p className="text-xs sm:text-sm text-zinc-500">
            {countdown > 0 ? (
              <>
                Redirecting automatically in{" "}
                <span className="font-bold text-emerald-600 text-base">{countdown}</span>{" "}
                second{countdown !== 1 ? "s" : ""}...
              </>
            ) : (
              <span className="font-semibold text-emerald-600">Redirecting now...</span>
            )}
          </p>
          <div className="w-full bg-zinc-200/80 rounded-full h-1.5 overflow-hidden">
            <div
              className="bg-emerald-600 h-full transition-all duration-1000 ease-linear rounded-full"
              style={{ width: `${progressPercentage}%` }}
            />
          </div>
        </div>

        {/* Primary Action Button */}
        <div>
          <a
            href={TARGET_URL}
            className="inline-flex items-center justify-center gap-2.5 w-full sm:w-auto px-8 py-3.5 bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white font-bold text-base rounded-xl shadow-lg shadow-emerald-600/20 hover:shadow-xl hover:shadow-emerald-600/30 transition-all duration-150 transform hover:-translate-y-0.5 focus:outline-none"
          >
            <span>Go to www.smartswine.app</span>
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
            </svg>
          </a>
        </div>

        {/* Bookmark suggestion */}
        <p className="mt-6 text-[11px] sm:text-xs text-zinc-400">
          Tip: Please bookmark <strong className="text-zinc-600 font-semibold">https://www.smartswine.app</strong> for future access.
        </p>
      </div>
    </div>
  );
}
