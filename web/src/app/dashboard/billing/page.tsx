"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Script from "next/script";
import { useAuth } from "@/context/AuthContext";

export default function BillingPage() {
  const { user, userProfile, loading } = useAuth();
  const router = useRouter();
  const [billingCycle, setBillingCycle] = useState<"monthly" | "annual">("monthly");

  const MONTHLY_VARIANT_ID = process.env.NEXT_PUBLIC_LEMON_SQUEEZY_MONTHLY_VARIANT_ID || "333333";
  const ANNUAL_VARIANT_ID = process.env.NEXT_PUBLIC_LEMON_SQUEEZY_ANNUAL_VARIANT_ID || "444444";
  const STORE_URL = process.env.NEXT_PUBLIC_LEMON_SQUEEZY_STORE_URL || "https://smartswine.lemonsqueezy.com";

  // Re-initialize Lemon Squeezy script when layout updates to hook elements
  useEffect(() => {
    const win = window as any;
    if (typeof win.createLemonSqueezy === "function") {
      win.createLemonSqueezy();
    }
  }, [billingCycle, userProfile]);

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-zinc-950 text-zinc-100">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  const userUid = user.uid;
  const storeBase = STORE_URL.replace(/\/$/, ""); // trim trailing slash
  const monthlyUrl = `${storeBase}/checkout/buy/${MONTHLY_VARIANT_ID}?embed=1&media=0&checkout[custom][user_id]=${userUid}`;
  const annualUrl = `${storeBase}/checkout/buy/${ANNUAL_VARIANT_ID}?embed=1&media=0&checkout[custom][user_id]=${userUid}`;

  const features = [
    { name: "Unlimited Pigs & Litters", free: "Up to 50 pigs", premium: "Unlimited" },
    { name: "Feed Stock & Transactions", free: "Basic logging", premium: "Advanced + Alerts" },
    { name: "Feed Formulator (Pearson Square)", free: "Starter stage only", premium: "All stages (Starter, Grower, Finisher)" },
    { name: "PDF Report Exports", free: "❌ Unavailable", premium: "⚡ Yes, Instant Print/PDF" },
    { name: "Visual Financial Ledger charts", free: "Basic list", premium: "Advanced graphs + Filter" },
    { name: "Offline Sync Backup", free: "❌ Unavailable", premium: "⚡ Real-time cloud backup" },
    { name: "Support Priority", free: "Email support", premium: "24/7 Priority chat" },
  ];

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 flex flex-col font-sans">
      {/* Lemon Squeezy Script Integration */}
      <Script
        src="https://app.lemonsqueezy.com/js/lemon.js"
        strategy="afterInteractive"
        onLoad={() => {
          const win = window as any;
          if (typeof win.createLemonSqueezy === "function") {
            win.createLemonSqueezy();
          }
        }}
      />

      <header className="border-b border-zinc-900 bg-zinc-900/40 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href="/dashboard" className="text-zinc-400 hover:text-white transition">
              ← Dashboard
            </Link>
            <span className="text-zinc-700">|</span>
            <span className="font-bold text-lg text-white flex items-center gap-2">
              💎 Premium & Billing
            </span>
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-5xl w-full mx-auto px-4 py-12 space-y-12">
        {/* Top Info */}
        <div className="text-center max-w-2xl mx-auto space-y-4">
          <h1 className="text-3xl md:text-4xl font-extrabold text-white tracking-tight bg-gradient-to-r from-emerald-400 via-teal-400 to-violet-400 bg-clip-text text-transparent">
            Unlock the Full Power of Smart Swine
          </h1>
          <p className="text-zinc-400 text-sm md:text-base">
            Optimize your pig farm operations, formulate advanced rations, export reports, and scale without limits.
          </p>
        </div>

        {/* Premium Active User Banner */}
        {userProfile?.isPremium ? (
          <div className="bg-gradient-to-r from-emerald-500/10 via-emerald-600/5 to-transparent border border-emerald-500/20 rounded-2xl p-6 text-center space-y-4 relative overflow-hidden">
            <div className="absolute top-0 right-0 h-32 w-32 rounded-full bg-emerald-500/5 blur-2xl animate-pulse" />
            <span className="text-4xl">🎉</span>
            <h2 className="text-xl font-bold text-white">Smart Swine Premium is Active!</h2>
            <p className="text-sm text-zinc-400 max-w-md mx-auto">
              Your account has full access to the Feed Formulator, unlimited herd data sizes, and PDF downloads. Thank you for supporting our platform!
            </p>
            <div className="pt-2 flex justify-center gap-3">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-500/10 px-3.5 py-1 text-xs font-semibold text-emerald-400 border border-emerald-500/25">
                Billing Source: {userProfile?.subscriptionSource || "lemon_squeezy"}
              </span>
            </div>
          </div>
        ) : (
          <>
            {/* Pricing Cycle Toggle */}
            <div className="flex justify-center">
              <div className="bg-zinc-900 border border-zinc-800 p-1.5 rounded-xl flex gap-1 items-center">
                <button
                  onClick={() => setBillingCycle("monthly")}
                  className={`px-4 py-2 rounded-lg text-xs font-bold transition-all ${
                    billingCycle === "monthly"
                      ? "bg-zinc-800 text-white shadow-md"
                      : "text-zinc-400 hover:text-zinc-200"
                  }`}
                >
                  Monthly billing
                </button>
                <button
                  onClick={() => setBillingCycle("annual")}
                  className={`px-4 py-2 rounded-lg text-xs font-bold transition-all relative flex items-center gap-1.5 ${
                    billingCycle === "annual"
                      ? "bg-zinc-800 text-white shadow-md"
                      : "text-zinc-400 hover:text-zinc-200"
                  }`}
                >
                  Annual billing
                  <span className="bg-emerald-500 text-[10px] text-zinc-950 font-black px-1.5 py-0.5 rounded-full">
                    Save 33%
                  </span>
                </button>
              </div>
            </div>

            {/* Pricing Cards Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-3xl mx-auto">
              {/* Free Plan */}
              <div className="bg-zinc-900/30 border border-zinc-900 rounded-2xl p-8 space-y-6 flex flex-col justify-between hover:border-zinc-800 transition duration-300">
                <div className="space-y-4">
                  <h3 className="text-lg font-bold text-zinc-400">Basic Plan</h3>
                  <div className="flex items-baseline gap-1">
                    <span className="text-4xl font-extrabold text-white">$0</span>
                    <span className="text-zinc-500 text-sm">/ forever</span>
                  </div>
                  <p className="text-xs text-zinc-500">
                    Perfect for hobbyists and starting farmers to explore tracking.
                  </p>
                </div>
                <button
                  disabled
                  className="w-full rounded-xl border border-zinc-850 bg-zinc-900/40 py-3 text-xs font-bold text-zinc-500 cursor-not-allowed text-center transition"
                >
                  Current Free Plan
                </button>
              </div>

              {/* Premium Plan */}
              <div className="bg-zinc-900/50 border-2 border-emerald-500/30 rounded-2xl p-8 space-y-6 flex flex-col justify-between relative overflow-hidden group hover:border-emerald-500/50 transition duration-300 shadow-xl shadow-emerald-950/5">
                <div className="absolute top-0 right-0 bg-gradient-to-l from-emerald-500 to-teal-500 text-zinc-950 text-[10px] font-black px-3 py-1 rounded-bl-xl tracking-wider uppercase">
                  Highly Recommended
                </div>
                <div className="space-y-4">
                  <h3 className="text-lg font-bold text-white flex items-center gap-2">
                    💎 Smart Swine Premium
                  </h3>
                  <div className="flex items-baseline gap-1">
                    <span className="text-4xl font-extrabold text-white">
                      {billingCycle === "monthly" ? "$9.99" : "$6.66"}
                    </span>
                    <span className="text-zinc-500 text-sm">
                      / month {billingCycle === "annual" && "(billed yearly)"}
                    </span>
                  </div>
                  <p className="text-xs text-zinc-400">
                    Maximize productivity, dynamic Pearson Square feeds, and unlimited operations.
                  </p>
                </div>
                <a
                  href={billingCycle === "monthly" ? monthlyUrl : annualUrl}
                  className="lemonsqueezy-button w-full rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 py-3 text-xs font-black text-zinc-950 text-center shadow-lg shadow-emerald-500/10 block transition duration-300 transform active:scale-95"
                >
                  Upgrade to Premium
                </a>
              </div>
            </div>
          </>
        )}

        {/* Feature Comparison Table */}
        <div className="bg-zinc-900/20 border border-zinc-900 rounded-2xl p-6 overflow-hidden">
          <h3 className="text-lg font-bold text-white mb-6">Plan Comparison</h3>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-zinc-900 text-sm">
              <thead>
                <tr className="text-left text-zinc-400 font-semibold border-b border-zinc-900">
                  <th className="pb-3 pr-4">Feature</th>
                  <th className="pb-3 text-center">Basic (Free)</th>
                  <th className="pb-3 text-center text-emerald-400">Premium</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-900/50">
                {features.map((f) => (
                  <tr key={f.name}>
                    <td className="py-3.5 pr-4 font-medium text-zinc-300">{f.name}</td>
                    <td className="py-3.5 text-center text-zinc-500">{f.free}</td>
                    <td className="py-3.5 text-center font-semibold text-white">{f.premium}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  );
}
