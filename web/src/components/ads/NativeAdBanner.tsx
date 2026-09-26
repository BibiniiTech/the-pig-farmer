"use client";

import React from "react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";

interface NativeAdBannerProps {
  slotId?: string;
  className?: string;
}

export default function NativeAdBanner({ slotId, className = "" }: NativeAdBannerProps) {
  const { userProfile, isPassActive } = useAuth();

  // If user is paid premium or holding an active 3-hour pass, hide all ads
  if (userProfile?.isPremium || isPassActive) {
    return null;
  }

  return (
    <div
      className={`w-full my-6 rounded-2xl border border-zinc-200/90 dark:border-zinc-800 bg-zinc-50/80 dark:bg-zinc-900/60 p-4 transition shadow-sm overflow-hidden print:hidden ${className}`}
    >
      {/* Header Tag */}
      <div className="flex items-center justify-between mb-3">
        <span className="inline-block px-2 py-0.5 rounded-md bg-emerald-100 dark:bg-emerald-950/60 text-[10px] font-black tracking-wider text-emerald-800 dark:text-emerald-300 uppercase">
          Sponsored
        </span>
        <Link
          href="/dashboard/billing"
          className="text-[11px] font-medium text-zinc-400 hover:text-emerald-600 transition"
        >
          Remove ads with Premium ⚡
        </Link>
      </div>

      {/* Google AdSense Container if slot provided */}
      {slotId && (
        <div className="w-full min-h-[90px] flex items-center justify-center my-2">
          <ins
            className="adsbygoogle"
            style={{ display: "block", textAlign: "center" }}
            data-ad-layout="in-article"
            data-ad-format="fluid"
            data-ad-client="ca-pub-4097392441181162"
            data-ad-slot={slotId}
          />
        </div>
      )}

      {/* Native Sponsored Card matching Android NativeAdCard */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pt-1">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-emerald-600 to-green-500 flex items-center justify-center text-white text-xl shadow-md shrink-0">
            🌾
          </div>
          <div>
            <h4 className="text-sm font-bold text-zinc-900 dark:text-zinc-100">
              SmartSwine Farming Hub & Nutrition
            </h4>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 line-clamp-2">
              Connect with certified veterinarians, quality feed suppliers, and commercial pig buyers across the region.
            </p>
          </div>
        </div>

        <Link
          href="/dashboard/billing"
          className="shrink-0 w-full sm:w-auto text-center px-4 py-2.5 rounded-xl bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 text-xs font-bold hover:bg-emerald-700 dark:hover:bg-emerald-500 hover:text-white transition shadow-sm"
        >
          Go Ad-Free
        </Link>
      </div>
    </div>
  );
}
