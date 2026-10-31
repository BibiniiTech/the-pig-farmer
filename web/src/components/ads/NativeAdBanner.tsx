"use client";

import React, { useEffect, useRef } from "react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";

interface NativeAdBannerProps {
  zoneId?: string;
  className?: string;
}

export default function NativeAdBanner({ zoneId, className = "" }: NativeAdBannerProps) {
  const { userProfile } = useAuth();
  const bannerContainerRef = useRef<HTMLDivElement>(null);

  const activeZoneId =
    zoneId ||
    process.env.NEXT_PUBLIC_MONETAG_BANNER_ZONE_ID ||
    "12162725";

  const monetagDirectLink =
    process.env.NEXT_PUBLIC_MONETAG_DIRECT_LINK ||
    "https://uplcm.com/4/12162741";

  useEffect(() => {
    // If a custom Monetag in-page zone script is provided, initialize dynamically in container
    if (activeZoneId && bannerContainerRef.current) {
      try {
        const container = bannerContainerRef.current;
        container.innerHTML = "";
        const adDiv = document.createElement("div");
        adDiv.dataset.zone = activeZoneId;
        adDiv.className = "monetag-inpage-banner";
        container.appendChild(adDiv);
      } catch (e) {
        console.debug("Monetag banner init:", e);
      }
    }
  }, [activeZoneId]);

  // Only hide native ads if user has an actual paid premium subscription or is admin.
  // The 3-hour ad pass does NOT hide native banner ads.
  if (userProfile?.isPremium || userProfile?.isAdmin) {
    return null;
  }

  const handleSponsoredClick = () => {
    // Trigger Monetag vignette if available on window
    try {
      const w = window as any;
      if (typeof w.show_12162725 === "function") {
        w.show_12162725();
      }
    } catch (err) {
      console.debug("Monetag trigger:", err);
    }
  };

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

      {/* Monetag In-Page Container */}
      <div
        ref={bannerContainerRef}
        className="w-full flex items-center justify-center my-1 overflow-hidden min-h-[10px]"
      />

      {/* Monetag Native Sponsored Card */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pt-1">
        <a
          href={monetagDirectLink}
          target="_blank"
          rel="noopener noreferrer sponsored"
          onClick={handleSponsoredClick}
          className="flex items-center gap-3 group flex-1"
        >
          <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-emerald-600 to-green-500 flex items-center justify-center text-white text-xl shadow-md shrink-0 group-hover:scale-105 transition-transform">
            🌾
          </div>
          <div>
            <h4 className="text-sm font-bold text-zinc-900 dark:text-zinc-100 group-hover:text-emerald-700 dark:group-hover:text-emerald-400 transition-colors">
              SmartSwine Commercial Farm Solutions
            </h4>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 line-clamp-2">
              Connect with certified livestock veterinarians, quality bulk feed suppliers, and commercial swine buyers across the region.
            </p>
          </div>
        </a>

        <div className="flex items-center gap-2 shrink-0 w-full sm:w-auto">
          <a
            href={monetagDirectLink}
            target="_blank"
            rel="noopener noreferrer sponsored"
            onClick={handleSponsoredClick}
            className="w-1/2 sm:w-auto text-center px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition shadow-xs"
          >
            Explore Offers
          </a>
          <Link
            href="/dashboard/billing"
            className="w-1/2 sm:w-auto text-center px-4 py-2 rounded-xl bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 text-xs font-bold hover:bg-zinc-800 dark:hover:bg-zinc-200 transition shadow-xs"
          >
            Go Ad-Free
          </Link>
        </div>
      </div>
    </div>
  );
}
