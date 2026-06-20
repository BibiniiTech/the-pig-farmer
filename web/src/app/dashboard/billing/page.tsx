"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import UserProfileDropdown from "@/components/UserProfileDropdown";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import dynamic from "next/dynamic";

const PaystackBillingButton = dynamic(() => import("@/components/PaystackBillingButton"), {
  ssr: false,
});

export default function BillingPage() {
  const { user, userProfile, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();
  const [billingCycle, setBillingCycle] = useState<"monthly" | "annual">("monthly");

  const MONTHLY_PLAN_CODE = process.env.NEXT_PUBLIC_PAYSTACK_MONTHLY_PLAN_CODE || "PLN_0fhg14kc86tn8qs";
  const ANNUAL_PLAN_CODE = process.env.NEXT_PUBLIC_PAYSTACK_ANNUAL_PLAN_CODE || "PLN_sk44tcyegocprdu";
  const PUBLIC_KEY = (process.env.NEXT_PUBLIC_PAYSTACK_PUBLIC_KEY || "pk_live_80c6263d2d5499da137d63269d25aa45959b33e3").replace(/['"]/g, "").trim();

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-white text-zinc-900">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  const userUid = user.uid;
  const userEmail = userProfile?.email || user.email || "user@smartswine.app";

  const paystackConfig = {
    email: userEmail,
    amount: billingCycle === "monthly" ? 500 : 4500,
    publicKey: PUBLIC_KEY,
    plan: billingCycle === "monthly" ? MONTHLY_PLAN_CODE : ANNUAL_PLAN_CODE,
    metadata: {
      custom_fields: [
        {
          display_name: "User ID",
          variable_name: "user_id",
          value: userUid
        }
      ]
    }
  };

  const onSuccess = (reference: any) => {
    alert("Payment successful! Your premium features will be activated shortly.");
  };

  const onClose = () => {
    console.log("Paystack modal closed");
  };

  const features = [
    { name: "Unlimited Pigs & Litters", free: "Up to 20 pigs", premium: "Unlimited" },
    { name: "Feed Stock & Transactions", free: "Basic logging", premium: "Advanced + Alerts" },
    { name: "Feed Formulator (Pearson Square)", free: "Starter stage only", premium: "All stages (Starter, Grower, Finisher)" },
    { name: "PDF Report Exports", free: "❌ Unavailable", premium: "⚡ Yes, Instant Print/PDF" },
    { name: "Visual Financial Ledger charts", free: "Basic list", premium: "Advanced graphs + Filter" },
    { name: "Offline Sync Backup", free: "❌ Unavailable", premium: "⚡ Real-time cloud backup" },
    { name: "Support Priority", free: "Email support", premium: "24/7 Priority chat" },
  ];

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
        {!isMobile && <DesktopHeader />}

        <main className="flex-1 max-w-5xl w-full mx-auto px-4 py-12 space-y-12">
          {/* Top Info */}
          <div className="text-center max-w-2xl mx-auto space-y-4">
            <h1 className="text-3xl md:text-4xl font-extrabold tracking-tight bg-gradient-to-r from-emerald-800 via-emerald-600 to-green-500 bg-clip-text text-transparent">
              Unlock the Full Power of SmartSwine
            </h1>
            <p className="text-zinc-600 text-sm md:text-base font-medium">
              Optimize your pig farm operations, formulate advanced rations, export reports, and scale without limits.
            </p>
          </div>

          {/* Premium Active User Banner */}
          {userProfile?.isPremium ? (
            <div className="bg-gradient-to-r from-emerald-50/40 via-emerald-100/5 to-transparent backdrop-blur-md border border-emerald-200 rounded-2xl p-6 text-center space-y-4 relative overflow-hidden shadow-sm">
              <div className="absolute top-0 right-0 h-32 w-32 rounded-full bg-emerald-500/5 blur-2xl animate-pulse" />
              <span className="text-4xl">🎉</span>
              <h2 className="text-xl font-bold text-zinc-900">SmartSwine Premium is Active!</h2>
              <p className="text-sm text-zinc-600 max-w-md mx-auto">
                Your account has full access to the Feed Formulator, unlimited herd data sizes, and PDF downloads. Thank you for supporting our platform!
              </p>
              <div className="pt-2 flex justify-center gap-3">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-3.5 py-1 text-xs font-semibold text-emerald-800 border border-emerald-200">
                  Billing Source: {userProfile?.subscriptionSource || "paystack"}
                </span>
              </div>
            </div>
          ) : (
            <>
              {/* Pricing Cycle Toggle */}
              <div className="flex justify-center">
                <div className="bg-zinc-100 border border-zinc-200 p-1.5 rounded-xl flex gap-1 items-center shadow-sm">
                  <button
                    onClick={() => setBillingCycle("monthly")}
                    className={`px-4 py-2 rounded-lg text-xs font-bold transition-all ${
                      billingCycle === "monthly"
                        ? "bg-white text-zinc-800 shadow-sm border border-zinc-200/50"
                        : "text-zinc-500 hover:text-zinc-850"
                    }`}
                  >
                    Monthly billing
                  </button>
                  <button
                    onClick={() => setBillingCycle("annual")}
                    className={`px-4 py-2 rounded-lg text-xs font-bold transition-all relative flex items-center gap-1.5 ${
                      billingCycle === "annual"
                        ? "bg-white text-zinc-800 shadow-sm border border-zinc-200/50"
                        : "text-zinc-500 hover:text-zinc-850"
                    }`}
                  >
                    Annual billing
                    <span className="bg-emerald-600 text-[10px] text-white font-bold px-1.5 py-0.5 rounded-full">
                      Save 33%
                    </span>
                  </button>
                </div>
              </div>

              {/* Pricing Cards Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-3xl mx-auto relative z-10">
                {/* Free Plan */}
                <div className="bg-zinc-50/60 backdrop-blur-sm border border-zinc-200 rounded-2xl p-8 space-y-6 flex flex-col justify-between hover:border-zinc-300 transition duration-300 shadow-sm">
                  <div className="space-y-4">
                    <h3 className="text-lg font-bold text-zinc-500">Basic Plan</h3>
                    <div className="flex items-baseline gap-1">
                      <span className="text-4xl font-extrabold text-zinc-900">$0</span>
                      <span className="text-zinc-500 text-sm">/ forever</span>
                    </div>
                    <p className="text-xs text-zinc-600">
                      Perfect for hobbyists and starting farmers to explore tracking.
                    </p>
                  </div>
                  <button
                    disabled
                    className="w-full rounded-xl border border-zinc-200 bg-zinc-100 py-3 text-xs font-bold text-zinc-400 cursor-not-allowed text-center transition"
                  >
                    Current Free Plan
                  </button>
                </div>

                {/* Premium Plan */}
                <div className="bg-white/60 backdrop-blur-md border-2 border-emerald-500/40 rounded-2xl p-8 space-y-6 flex flex-col justify-between relative overflow-hidden group hover:border-emerald-500 transition duration-300 shadow-xl shadow-emerald-500/5">
                  <div className="absolute top-0 right-0 bg-gradient-to-r from-emerald-600 to-green-500 text-white text-[10px] font-black px-3 py-1.5 rounded-bl-xl tracking-wider uppercase">
                    Highly Recommended
                  </div>
                  <div className="space-y-4">
                    <h3 className="text-lg font-bold text-zinc-900 flex items-center gap-2">
                      💎 SmartSwine Premium
                    </h3>
                    <div className="flex items-baseline gap-1">
                      <span className="text-4xl font-extrabold text-zinc-900">
                        {billingCycle === "monthly" ? "$5.00" : "$45.00"}
                      </span>
                      <span className="text-zinc-500 text-sm">
                        / month {billingCycle === "annual" && "(billed yearly)"}
                      </span>
                    </div>
                    <p className="text-xs text-zinc-600">
                      Maximize productivity, dynamic Pearson Square feeds, and unlimited operations.
                    </p>
                  </div>
                  <PaystackBillingButton
                    config={paystackConfig}
                    onSuccess={onSuccess}
                    onClose={onClose}
                  />
                </div>
              </div>
            </>
          )}

          {/* Feature Comparison Table */}
          <div className="bg-zinc-50/70 backdrop-blur-sm border border-zinc-200 rounded-2xl p-6 overflow-hidden shadow-sm relative z-10">
            <h3 className="text-lg font-bold text-zinc-900 mb-6">Plan Comparison</h3>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-zinc-200 text-sm">
                <thead>
                  <tr className="text-left text-zinc-500 font-semibold border-b border-zinc-200">
                    <th className="pb-3 pr-4">Feature</th>
                    <th className="pb-3 text-center">Basic (Free)</th>
                    <th className="pb-3 text-center text-emerald-700">Premium</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-200/50">
                  {features.map((f) => (
                    <tr key={f.name}>
                      <td className="py-3.5 pr-4 font-medium text-zinc-700">{f.name}</td>
                      <td className="py-3.5 text-center text-zinc-500">{f.free}</td>
                      <td className="py-3.5 text-center font-semibold text-zinc-900">{f.premium}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
