"use client";

import React from "react";
import { useAuth } from "@/context/AuthContext";

interface ManageSubscriptionModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const WorkspacePremiumIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" {...props}>
    <path d="M9.68 13.69L12 11.93l2.31 1.76-.88-2.85L15.75 9h-2.84L12 6.19 11.09 9H8.25l2.31 1.84-.88 2.85zM20 10c0-4.42-3.58-8-8-8s-8 3.58-8 8c0 2.03.76 3.87 2 5.28V23l6-2 6 2v-7.72c1.24-1.41 2-3.25 2-5.28zm-8-6c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6 2.69-6 6-6z" />
  </svg>
);

const CheckCircleIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" {...props}>
    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
  </svg>
);

const SupportAgentIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <path d="M3 18v-6a9 9 0 0 1 18 0v6" />
    <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z" />
  </svg>
);

const SettingsIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <circle cx="12" cy="12" r="3" />
    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
  </svg>
);

const BENEFITS = [
  "Unlimited Herd Capacity & Genealogy Tracking",
  "All Executive PDF Reports (Herd, Feed, HR, Finances)",
  "Mix Balanced Feed & Custom Ingredient Formulator",
  "Staff Logins, Permissions & Payroll Records",
  "Smart Veterinary Disease & Symptom Matching",
  "100% Ad-Free Experience & Priority Sync"
];

export default function ManageSubscriptionModal({ isOpen, onClose }: ManageSubscriptionModalProps) {
  const { user, userProfile, activeFarmUid } = useAuth();

  if (!isOpen) return null;

  const isPlayStore = userProfile?.subscriptionSource === "play_store";
  const portalUrl = userProfile?.lemonSqueezyCustomerPortalUrl;
  const manageHref = isPlayStore
    ? "https://play.google.com/store/account/subscriptions?package=com.bibiniitech.smartswine"
    : portalUrl || "https://smartswine.lemonsqueezy.com/billing";

  const supportHref = `mailto:bibiniitech@gmail.com?subject=SmartSwine%20Premium%20Support%20Inquiry&body=Hello%20SmartSwine%20Support%20Team%2C%0D%0A%0D%0AI%20am%20a%20Premium%20subscriber%20and%20have%20a%20question%20regarding%3A%0D%0A%0D%0A`;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div
        className="bg-white dark:bg-zinc-900 border border-emerald-500/30 rounded-3xl w-full max-w-md p-6 sm:p-7 space-y-5 shadow-2xl relative overflow-hidden text-center max-h-[92vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Shimmering Golden/Emerald Crown Emblem */}
        <div className="mx-auto w-[76px] h-[76px] rounded-full bg-gradient-to-tr from-amber-400/30 via-emerald-600/25 to-transparent flex items-center justify-center p-2 shadow-inner">
          <div className="w-14 h-14 rounded-full bg-[#133E25] border-2 border-[#FFB300] flex items-center justify-center shadow-lg">
            <WorkspacePremiumIcon className="w-8 h-8 text-[#FFB300]" />
          </div>
        </div>

        {/* Header */}
        <div className="space-y-2">
          <h2 className="text-xl sm:text-2xl font-black text-zinc-900 dark:text-zinc-100 tracking-tight">
            Thank You for Your Support!
          </h2>
          <div>
            <span className="inline-block px-3 py-1 rounded-lg bg-emerald-700/10 dark:bg-emerald-500/15 border border-emerald-700/30 dark:border-emerald-500/40 text-[11px] font-black text-emerald-800 dark:text-emerald-300 tracking-wider">
              SMARTSWINE PREMIUM MEMBER
            </span>
          </div>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 max-w-sm mx-auto leading-relaxed">
            You have unlocked the full suite of farm management, precision feeding, health diagnostics, and team collaboration tools.
          </p>
        </div>

        {/* Active Benefits Card */}
        <div className="bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200/80 dark:border-zinc-700/50 rounded-2xl p-4 text-left space-y-2.5 shadow-sm">
          {BENEFITS.map((benefit, idx) => (
            <div key={idx} className="flex items-start gap-2.5">
              <CheckCircleIcon className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
              <span className="text-xs font-medium text-zinc-800 dark:text-zinc-200 leading-snug">
                {benefit}
              </span>
            </div>
          ))}
        </div>

        {/* Manage Subscription Button */}
        <a
          href={manageHref}
          target="_blank"
          rel="noopener noreferrer"
          className="w-full h-12 flex items-center justify-center gap-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-sm transition-all shadow-md active:scale-[0.98]"
        >
          <SettingsIcon className="w-4 h-4" />
          <span>
            {isPlayStore
              ? "Manage Subscription on Google Play"
              : "Manage Subscription"}
          </span>
        </a>

        {/* Support and Close Buttons */}
        <div className="flex gap-2.5 pt-1">
          <a
            href={supportHref}
            className="flex-1 h-11 flex items-center justify-center gap-1.5 rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 hover:bg-zinc-50 dark:hover:bg-zinc-750 text-xs font-bold text-zinc-700 dark:text-zinc-300 transition-colors shadow-sm"
          >
            <SupportAgentIcon className="w-4 h-4 text-zinc-500" />
            <span>Support</span>
          </a>

          <button
            type="button"
            onClick={onClose}
            className="flex-1 h-11 flex items-center justify-center rounded-xl bg-zinc-100 hover:bg-zinc-200 dark:bg-zinc-800 dark:hover:bg-zinc-700 text-xs font-bold text-zinc-800 dark:text-zinc-200 transition-colors shadow-sm"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
}
