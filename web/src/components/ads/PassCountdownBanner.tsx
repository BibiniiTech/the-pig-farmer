"use client";

import React from "react";
import { useAuth } from "@/context/AuthContext";

export default function PassCountdownBanner() {
  const { isPassActive, passTimeRemaining, isPaidPremium } = useAuth();

  // Only show to users who have an active temporary pass and are not already paid premium
  if (!isPassActive || isPaidPremium || !passTimeRemaining) {
    return null;
  }

  return (
    <div className="w-full mb-4 rounded-xl bg-emerald-50/90 dark:bg-emerald-950/40 border border-emerald-500/40 px-4 py-2.5 flex items-center justify-between shadow-sm animate-in fade-in duration-300">
      <div className="flex items-center gap-2.5">
        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-600/15 text-emerald-700 dark:text-emerald-400">
          <svg className="h-4 w-4 animate-pulse" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
        </div>
        <div>
          <span className="text-xs sm:text-sm font-black text-emerald-900 dark:text-emerald-200">
            3-Hour Universal Pass:
          </span>{" "}
          <span className="text-xs sm:text-sm font-extrabold text-emerald-700 dark:text-emerald-400 font-mono tracking-wide">
            {passTimeRemaining} remaining
          </span>
        </div>
      </div>
      <span className="hidden sm:inline-block text-[11px] font-bold text-emerald-800/80 dark:text-emerald-300/80 bg-emerald-100 dark:bg-emerald-900/60 px-2.5 py-1 rounded-md">
        Full Access Unlocked
      </span>
    </div>
  );
}
