"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Script from "next/script";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import ManageSubscriptionModal from "@/components/ManageSubscriptionModal";
import { useTranslations } from "next-intl";

export default function BillingPage() {
  const t = useTranslations("Billing");
  const tCommon = useTranslations("Common");
  const { user, userProfile, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();
  const [billingCycle, setBillingCycle] = useState<"monthly" | "annual">("monthly");
  const [isManageModalOpen, setIsManageModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [verificationSuccess, setVerificationSuccess] = useState(false);

  // User billing details state (editable before payment)
  const [billingEmail, setBillingEmail] = useState("");
  const [billingPhone, setBillingPhone] = useState("");
  const [billingFirstName, setBillingFirstName] = useState("");
  const [billingLastName, setBillingLastName] = useState("");
  const [emailTouched, setEmailTouched] = useState(false);

  // Sync initial user details when user/profile loads
  useEffect(() => {
    if (userProfile || user) {
      setBillingEmail((prev) => prev || userProfile?.email || user?.email || "");
      setBillingFirstName((prev) => prev || userProfile?.firstName || "");
      setBillingLastName((prev) => prev || userProfile?.lastName || "");
      setBillingPhone((prev) => prev || (user as any)?.phoneNumber || "");
    }
  }, [user, userProfile]);

  // Check for successful checkout redirect
  useEffect(() => {
    if (typeof window !== "undefined") {
      const params = new URLSearchParams(window.location.search);
      if (params.get("status") === "success") {
        setVerificationSuccess(true);
      }
    }
  }, []);

  const MONTHLY_VARIANT_ID =
    process.env.NEXT_PUBLIC_LEMONSQUEEZY_MONTHLY_VARIANT_ID || "2179203";
  const ANNUAL_VARIANT_ID =
    process.env.NEXT_PUBLIC_LEMONSQUEEZY_ANNUAL_VARIANT_ID || "2179200";

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-[#F8FAF9] dark:bg-[#121212] text-zinc-900 dark:text-zinc-100">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  const userUid = user.uid;
  const trimmedEmail = billingEmail.trim();
  const isEmailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail);
  const isFormValid = isEmailValid && trimmedEmail.length > 0;

  const handleCheckout = async () => {
    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const selectedVariantId =
        billingCycle === "monthly" ? MONTHLY_VARIANT_ID : ANNUAL_VARIANT_ID;

      const res = await fetch("/api/billing/create-checkout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          variantId: selectedVariantId,
          userId: userUid,
          userEmail: trimmedEmail,
          userName: `${billingFirstName.trim()} ${billingLastName.trim()}`.trim() || undefined,
        }),
      });

      const data = await res.json();
      if (!res.ok || !data.url) {
        throw new Error(data.error || "Failed to create checkout session");
      }

      // Open in Lemon Squeezy overlay modal if Lemon.js is ready, else redirect
      if ((window as any).LemonSqueezy) {
        (window as any).LemonSqueezy.Url.Open(data.url);
      } else {
        window.location.href = data.url;
      }
    } catch (err: any) {
      console.error("Checkout error:", err);
      setErrorMessage(
        err.message || "Failed to initiate checkout. Please try again or check your connection."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const features = [
    { name: t("features.unlimitedPigs"), free: t("features.pigsLimit"), premium: t("features.unlimited") },
    { name: t("features.growthTracking"), free: t("features.pigsLimit"), premium: t("features.unlimited") },
    { name: t("features.feedFormulation"), free: t("features.unavailable"), premium: t("features.feedDescription") },
    { name: t("features.addEmployee"), free: t("features.unavailable"), premium: t("features.employeeDescription") },
    { name: t("features.pdfReports"), free: t("features.unavailable"), premium: t("features.pdfDescription") },
    { name: t("features.symptomsAnalyzer"), free: t("features.unavailable"), premium: t("features.fullAccess") },
  ];

  return (
    <div className="relative min-h-screen bg-[#F8FAF9] dark:bg-[#121212] text-zinc-900 dark:text-zinc-100 flex flex-col font-sans overflow-hidden">
      {/* Load Lemon.js for seamless overlay checkout */}
      <Script
        src="https://assets.lemonsqueezy.com/lemon.js"
        strategy="lazyOnload"
        onLoad={() => {
          (window as any).createLemonSqueezy?.();
        }}
      />

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
        {!isMobile && (
          <DesktopHeader
            showBack
            backPath="/dashboard"
            label={t("smartSwinePremium") || "SMARTSWINE PREMIUM"}
            labelColor="text-[#00796B] dark:text-[#4DB6AC]"
          />
        )}

        <main className="flex-1 max-w-5xl w-full mx-auto px-4 py-8 space-y-8">
          {/* Top Bar with Back Button */}
          <div className="flex items-center justify-between gap-4">
            <button
              type="button"
              onClick={() => router.push("/dashboard")}
              className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-white dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700 hover:text-zinc-900 transition font-bold text-xs shadow-xs"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
              <span>{tCommon("back") || "Back"}</span>
            </button>
            <div className="w-16" />
          </div>

          {/* Top Info */}
          <div className="text-center max-w-2xl mx-auto space-y-4">
            <h1 className="text-3xl md:text-4xl font-extrabold tracking-tight bg-gradient-to-r from-emerald-800 via-teal-700 to-green-600 bg-clip-text text-transparent">
              {t("title")}
            </h1>
            <p className="text-zinc-600 text-sm md:text-base font-medium">
              {t("description")}
            </p>
          </div>

          {/* Premium Active User Banner */}
          {userProfile?.isPremium ? (
            <div className="bg-gradient-to-r from-emerald-50/40 via-emerald-100/5 to-transparent backdrop-blur-md border border-emerald-200 rounded-2xl p-6 text-center space-y-4 relative overflow-hidden shadow-sm">
              <div className="absolute top-0 right-0 h-32 w-32 rounded-full bg-emerald-500/5 blur-2xl animate-pulse" />
              <span className="text-4xl">🎉</span>
              <h2 className="text-xl font-bold text-zinc-900">{t("premiumActive")}</h2>
              <p className="text-sm text-zinc-600 max-w-md mx-auto">
                {t("premiumBenefits")}
              </p>
              <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-3.5 py-1.5 text-xs font-semibold text-emerald-800 border border-emerald-200">
                  {t("billingSource", { source: userProfile?.subscriptionSource || "lemonsqueezy" })}
                </span>
                <button
                  type="button"
                  onClick={() => setIsManageModalOpen(true)}
                  className="inline-flex items-center gap-2 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white px-4 py-1.5 text-xs font-bold transition shadow-sm active:scale-95"
                >
                  <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="3" />
                    <path strokeLinecap="round" strokeLinejoin="round" d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
                  </svg>
                  <span>Manage Subscription</span>
                </button>
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
                    {t("monthlyBilling")}
                  </button>
                  <button
                    onClick={() => setBillingCycle("annual")}
                    className={`px-4 py-2 rounded-lg text-xs font-bold transition-all relative flex items-center gap-1.5 ${
                      billingCycle === "annual"
                        ? "bg-white text-zinc-800 shadow-sm border border-zinc-200/50"
                        : "text-zinc-500 hover:text-zinc-850"
                    }`}
                  >
                    {t("annualBilling")}
                    <span className="bg-emerald-600 text-[10px] text-white font-bold px-1.5 py-0.5 rounded-full">
                      {t("save33")}
                    </span>
                  </button>
                </div>
              </div>

              {/* Pricing Cards Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-3xl mx-auto relative z-10">
                {/* Free Plan */}
                <div className="bg-zinc-50/60 backdrop-blur-sm border border-zinc-200 rounded-2xl p-8 space-y-6 flex flex-col justify-between hover:border-zinc-300 transition duration-300 shadow-sm">
                  <div className="space-y-4">
                    <h3 className="text-lg font-bold text-zinc-500">{t("basicPlan")}</h3>
                    <div className="flex items-baseline gap-1">
                      <span className="text-4xl font-extrabold text-zinc-900">$0</span>
                      <span className="text-zinc-500 text-sm">/ {t("forever")}</span>
                    </div>
                    <p className="text-xs text-zinc-600">
                      {t("basicDescription")}
                    </p>
                  </div>
                  <button
                    disabled
                    className="w-full rounded-xl border border-zinc-200 bg-zinc-100 py-3 text-xs font-bold text-zinc-400 cursor-not-allowed text-center transition"
                  >
                    {t("currentFreePlan")}
                  </button>
                </div>

                {/* Premium Plan */}
                <div className="bg-white/60 backdrop-blur-md border-2 border-emerald-500/40 rounded-2xl p-8 space-y-6 flex flex-col justify-between relative overflow-hidden group hover:border-emerald-500 transition duration-300 shadow-xl shadow-emerald-500/5">
                  <div className="absolute top-0 right-0 bg-gradient-to-r from-emerald-600 to-green-500 text-white text-[10px] font-black px-3 py-1.5 rounded-bl-xl tracking-wider uppercase">
                    Highly Recommended
                  </div>
                  <div className="space-y-4">
                    <h3 className="text-lg font-bold text-zinc-900 flex items-center gap-2">
                      💎 {t("smartSwinePremium")}
                    </h3>
                    <div className="space-y-1">
                      <div className="flex items-baseline gap-1.5">
                        <span className="text-4xl font-extrabold text-zinc-900">
                          {billingCycle === "monthly" ? "US$2.00" : "US$10.00"}
                        </span>
                        <span className="text-zinc-500 text-sm">
                          / {billingCycle === "monthly" ? t("perMonth") : "year"}
                        </span>
                      </div>
                      <p className="text-xs font-semibold text-emerald-700">
                        {billingCycle === "monthly"
                          ? "Billed monthly via Lemon Squeezy (US$2.00/mo)"
                          : "Save US$14.00 yearly (US$10.00/yr ~ US$0.83/mo)"}
                      </p>
                    </div>
                    <p className="text-xs text-zinc-600">
                      {t("premiumDescription")}
                    </p>
                  </div>

                  {/* Feedback Status */}
                  {verificationSuccess && (
                    <div className="rounded-xl bg-emerald-100 border border-emerald-300 p-3 text-center">
                      <p className="text-xs font-bold text-emerald-800">
                        🎉 Payment successful! Premium features are active.
                      </p>
                    </div>
                  )}

                  {errorMessage && (
                    <div className="rounded-xl bg-red-50 border border-red-200 p-3 text-center space-y-1">
                      <p className="text-xs font-bold text-red-800">Checkout Error</p>
                      <p className="text-[11px] text-red-600">{errorMessage}</p>
                    </div>
                  )}

                  {/* Billing Contact Form */}
                  <div className="space-y-3 pt-3 border-t border-emerald-100">
                    <div className="flex items-center justify-between">
                      <label className="text-[11px] font-bold text-zinc-700 uppercase tracking-wider">
                        {t("billingDetails")}
                      </label>
                      <span className="text-[10px] text-zinc-400">
                        {t("billingDetailsSub")}
                      </span>
                    </div>

                    <div className="space-y-2.5">
                      <div>
                        <label className="block text-[11px] font-semibold text-zinc-600 mb-1">
                          {t("emailAddress")} <span className="text-red-500">*</span>
                        </label>
                        <input
                          type="email"
                          required
                          value={billingEmail}
                          onChange={(e) => {
                            setBillingEmail(e.target.value);
                            setEmailTouched(true);
                          }}
                          onBlur={() => setEmailTouched(true)}
                          placeholder="e.g. farmer@example.com"
                          className={`w-full rounded-lg border px-3 py-2 text-xs text-zinc-900 bg-white placeholder-zinc-400 focus:outline-none transition ${
                            emailTouched && !isEmailValid
                              ? "border-red-400 focus:border-red-500 focus:ring-1 focus:ring-red-400"
                              : "border-zinc-300 focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500"
                          }`}
                        />
                        {emailTouched && !isEmailValid && (
                          <p className="text-[10px] text-red-500 mt-1">
                            {trimmedEmail.length === 0 ? t("enterEmailPrompt") : t("invalidEmail")}
                          </p>
                        )}
                      </div>

                      <div className="grid grid-cols-2 gap-2">
                        <div>
                          <label className="block text-[11px] font-semibold text-zinc-600 mb-1">
                            {t("firstName")}
                          </label>
                          <input
                            type="text"
                            value={billingFirstName}
                            onChange={(e) => setBillingFirstName(e.target.value)}
                            placeholder="First name"
                            className="w-full rounded-lg border border-zinc-300 px-3 py-2 text-xs text-zinc-900 bg-white placeholder-zinc-400 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition"
                          />
                        </div>
                        <div>
                          <label className="block text-[11px] font-semibold text-zinc-600 mb-1">
                            {t("lastName")}
                          </label>
                          <input
                            type="text"
                            value={billingLastName}
                            onChange={(e) => setBillingLastName(e.target.value)}
                            placeholder="Last name"
                            className="w-full rounded-lg border border-zinc-300 px-3 py-2 text-xs text-zinc-900 bg-white placeholder-zinc-400 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition"
                          />
                        </div>
                      </div>

                      <div>
                        <label className="block text-[11px] font-semibold text-zinc-600 mb-1">
                          {t("phone")} (Optional)
                        </label>
                        <input
                          type="tel"
                          value={billingPhone}
                          onChange={(e) => setBillingPhone(e.target.value)}
                          placeholder="e.g. +1 555 123 4567"
                          className="w-full rounded-lg border border-zinc-300 px-3 py-2 text-xs text-zinc-900 bg-white placeholder-zinc-400 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition"
                        />
                      </div>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <button
                      type="button"
                      disabled={isSubmitting || !isFormValid}
                      onClick={handleCheckout}
                      className={`w-full rounded-xl py-3.5 text-xs font-bold text-white text-center shadow-lg transition duration-300 transform active:scale-95 flex items-center justify-center gap-2 ${
                        isSubmitting || !isFormValid
                          ? "bg-zinc-400 cursor-not-allowed opacity-60"
                          : "bg-gradient-to-r from-emerald-600 to-green-500 hover:from-emerald-700 hover:to-green-600 shadow-emerald-600/10 cursor-pointer"
                      }`}
                    >
                      {isSubmitting ? (
                        <>
                          <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                          <span>Preparing Secure Checkout...</span>
                        </>
                      ) : !trimmedEmail ? (
                        t("enterEmailPrompt")
                      ) : !isEmailValid ? (
                        t("enterValidEmail")
                      ) : billingCycle === "monthly" ? (
                        "Upgrade to Premium — $2.00 / month"
                      ) : (
                        "Upgrade to Premium — $10.00 / year"
                      )}
                    </button>
                    <p className="text-[11px] text-zinc-400 text-center leading-tight">
                      Secured by Lemon Squeezy. Visa, Mastercard, Apple Pay, Google Pay, and PayPal accepted.
                    </p>
                  </div>
                </div>
              </div>
            </>
          )}

          {/* Feature Comparison Table */}
          <div className="bg-zinc-50/70 backdrop-blur-sm border border-zinc-200 rounded-2xl p-6 overflow-hidden shadow-sm relative z-10">
            <h3 className="text-lg font-bold text-zinc-900 mb-6">{t("planComparison")}</h3>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-zinc-200 text-sm">
                <thead>
                  <tr className="text-left text-zinc-500 font-semibold border-b border-zinc-200">
                    <th className="pb-3 pr-4">{t("feature")}</th>
                    <th className="pb-3 text-center">{t("basicFree")}</th>
                    <th className="pb-3 text-center text-emerald-700">{t("premium")}</th>
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

      <ManageSubscriptionModal
        isOpen={isManageModalOpen}
        onClose={() => setIsManageModalOpen(false)}
      />
    </div>
  );
}
