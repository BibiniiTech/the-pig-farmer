"use client";

import React, { useState, useEffect } from "react";
import { useAuth } from "@/context/AuthContext";
import { SUPPORTED_COUNTRIES, getCurrencyByCountry, getNormalizedCountryName } from "@/lib/currencyUtils";

export default function CompleteProfileModal() {
  const { user, userProfile, isStaff, isProfileComplete, loading, createGoogleUserProfile } = useAuth();

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [farmName, setFarmName] = useState("");
  const [country, setCountry] = useState("United States");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Prepopulate names from Google account
  useEffect(() => {
    if (user?.displayName) {
      const parts = user.displayName.split(" ");
      setFirstName(parts[0] || "");
      setLastName(parts.slice(1).join(" ") || "");
    }
  }, [user]);

  // If loading or not logged in, or already complete, don't show
  if (loading || !user || isProfileComplete || isStaff) {
    return null;
  }

  // Also don't show if profile already has farmName & country
  if (userProfile?.farmName && userProfile?.country) {
    return null;
  }

  const derivedCurrency = getCurrencyByCountry(country);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!firstName.trim() || !lastName.trim() || !farmName.trim() || !country.trim()) {
      setError("Please fill in all required fields to continue.");
      return;
    }

    setSubmitting(true);
    try {
      await createGoogleUserProfile({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        farmName: farmName.trim(),
        country: country.trim(),
      });
    } catch (err: any) {
      console.error("Failed to complete profile:", err);
      setError(err.message || "Failed to create your farm profile. Please try again.");
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md p-4 animate-in fade-in duration-300">
      <div
        className="w-full max-w-md bg-white dark:bg-zinc-900 rounded-3xl border border-zinc-200 dark:border-zinc-800 shadow-2xl p-6 sm:p-8 space-y-6 text-zinc-900 dark:text-zinc-100"
        role="dialog"
        aria-modal="true"
      >
        <div className="text-center flex flex-col items-center space-y-3">
          <div className="h-16 w-16 rounded-2xl bg-white dark:bg-zinc-800 border border-zinc-200/80 dark:border-zinc-700/80 p-2 shadow-md flex items-center justify-center">
            <img
              src="/app_logo.png"
              alt="SmartSwine Logo"
              className="h-full w-full object-contain"
            />
          </div>
          <div className="space-y-1">
            <h2 className="text-2xl font-black tracking-tight bg-gradient-to-r from-emerald-800 via-emerald-600 to-green-600 bg-clip-text text-transparent">
              Complete Your Profile
            </h2>
            <p className="text-xs sm:text-sm text-zinc-500 dark:text-zinc-400">
              Set up your farm profile to get started with SmartSwine
            </p>
          </div>
        </div>

        {error && (
          <div className="p-3 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/60 text-xs font-semibold text-rose-700 dark:text-rose-300">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 mb-1">
                First Name *
              </label>
              <input
                type="text"
                required
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                placeholder="John"
                className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800 px-3.5 py-2.5 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-emerald-500 transition"
              />
            </div>
            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 mb-1">
                Last Name *
              </label>
              <input
                type="text"
                required
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                placeholder="Doe"
                className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800 px-3.5 py-2.5 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-emerald-500 transition"
              />
            </div>
          </div>

          <div>
            <label className="block text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 mb-1">
              Email Address
            </label>
            <input
              type="email"
              disabled
              value={user.email || ""}
              className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-zinc-100 dark:bg-zinc-800/60 px-3.5 py-2.5 text-sm font-medium text-zinc-500 dark:text-zinc-400 cursor-not-allowed select-none"
            />
          </div>

          <div>
            <label className="block text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 mb-1">
              Farm Name *
            </label>
            <input
              type="text"
              required
              value={farmName}
              onChange={(e) => setFarmName(e.target.value)}
              placeholder="e.g. Green Valley Piggery"
              className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800 px-3.5 py-2.5 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-emerald-500 transition"
            />
          </div>

          <div>
            <label className="block text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 mb-1">
              Country *
            </label>
            <select
              required
              value={getNormalizedCountryName(country)}
              onChange={(e) => setCountry(e.target.value)}
              className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800 px-3.5 py-2.5 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-emerald-500 transition"
            >
              {SUPPORTED_COUNTRIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>

          {/* Auto-selected currency indication banner */}
          <div className="flex items-center justify-between p-3 rounded-xl bg-emerald-50/80 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800/60">
            <div className="flex items-center gap-2">
              <span className="text-base">💱</span>
              <span className="text-xs font-semibold text-emerald-800 dark:text-emerald-300">
                Primary Currency (Auto-Selected)
              </span>
            </div>
            <span className="px-2.5 py-1 rounded-lg bg-emerald-600 text-white font-black text-xs shadow-xs">
              {derivedCurrency.code} ({derivedCurrency.symbol})
            </span>
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full mt-2 py-3 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 active:scale-98 text-white font-bold text-sm shadow-md transition disabled:opacity-60 flex items-center justify-center gap-2"
          >
            {submitting ? (
              <>
                <div className="h-4 w-4 rounded-full border-2 border-white border-t-transparent animate-spin" />
                <span>Setting up Farm Profile...</span>
              </>
            ) : (
              <span>Finish Registration & Enter Dashboard</span>
            )}
          </button>
        </form>
      </div>
    </div>
  );
}
