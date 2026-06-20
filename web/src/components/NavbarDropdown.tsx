"use client";

import React, { useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { signOut } from "firebase/auth";
import { auth } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import SettingsModal from "@/components/SettingsModal";
import { useTranslations } from "next-intl";
import {
  HomeIcon,
  HerdDataIcon,
  FeedManagementIcon,
  HerdActivitiesIcon,
  FinancialsIcon,
  HumanResourcesIcon,
  LocalHubIcon,
  SymptomsAnalyzerIcon,
  WeightCheckerIcon,
  TrainingTipsIcon,
  PremiumIcon,
  ShieldCheckIcon,
} from "@/components/icons/DashboardIcons";

interface NavOption {
  key: string;
  label: string;
  path: string;
  icon: React.ComponentType<any>;
  description?: string;
}

export default function NavbarDropdown() {
  const t = useTranslations("Navigation");
  const [isOpen, setIsOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const pathname = usePathname();
  const router = useRouter();
  const { userProfile } = useAuth();
  const { isMobile } = useDevice();

  const BASE_NAV_OPTIONS: NavOption[] = [
    { key: "home", label: t("home"), path: "/dashboard", icon: HomeIcon, description: t("homeDesc") },
    { key: "herd", label: t("herd"), path: "/dashboard/herd", icon: HerdDataIcon, description: t("herdDesc") },
    { key: "feed", label: t("feed"), path: "/dashboard/feed", icon: FeedManagementIcon, description: t("feedDesc") },
    { key: "activities", label: t("activities"), path: "/dashboard/activities", icon: HerdActivitiesIcon, description: t("activitiesDesc") },
    { key: "financials", label: t("financials"), path: "/dashboard/financials", icon: FinancialsIcon, description: t("financialsDesc") },
    { key: "hr", label: t("hr"), path: "/dashboard/hr", icon: HumanResourcesIcon, description: t("hrDesc") },
    { key: "hub", label: t("hub"), path: "/dashboard/hub", icon: LocalHubIcon, description: t("hubDesc") },
    { key: "symptoms", label: t("symptoms"), path: "/dashboard/symptoms", icon: SymptomsAnalyzerIcon, description: t("symptomsDesc") },
    { key: "weight", label: t("weight"), path: "/dashboard/weight", icon: WeightCheckerIcon, description: t("weightDesc") },
    { key: "training", label: t("training"), path: "/dashboard/training", icon: TrainingTipsIcon, description: t("trainingDesc") },
    { key: "billing", label: t("billing"), path: "/dashboard/billing", icon: PremiumIcon, description: t("billingDesc") },
  ];

  const ADMIN_NAV_OPTION: NavOption = {
    key: "admin",
    label: t("admin"),
    path: "/admin",
    icon: ShieldCheckIcon,
  };

  // Insert Admin Panel immediately after Billing & Premium (same order as Android app)
  const options = userProfile?.isAdmin
    ? [...BASE_NAV_OPTIONS, ADMIN_NAV_OPTION]
    : [...BASE_NAV_OPTIONS];

  // Determine current option based on path
  const currentOption = options.find(
    (opt) =>
      opt.path === pathname || 
      (opt.path !== "/dashboard" && pathname.startsWith(opt.path))
  ) || options[0];

  const handleSignOut = async () => {
    try {
      setIsOpen(false);
      await signOut(auth);
      router.push("/login");
    } catch (err) {
      console.error("Logout failed:", err);
    }
  };

  return (
    <>
      <div className="relative inline-block text-left">
        {/* Dropdown Toggle Button / Hamburger */}
        <button
          onClick={() => setIsOpen(!isOpen)}
          className="inline-flex items-center justify-center gap-2 h-10 transition duration-300 shadow-sm focus:outline-none select-none rounded-xl border border-zinc-200 w-10 sm:w-auto sm:px-4 bg-white sm:bg-zinc-150/70 hover:bg-zinc-50 sm:hover:bg-zinc-200/80 text-xs font-bold text-zinc-800 lg:min-w-[160px]"
          aria-label="Navigation menu"
        >
          {/* Hamburger Icon (Visible on mobile) */}
          <div className="sm:hidden">
            <svg className="h-6 w-6 text-zinc-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </div>

          {/* Desktop View (Icon + Label + Arrow) */}
          <div className="hidden sm:flex items-center gap-2">
            <currentOption.icon className="h-4 w-4 text-zinc-650" />
            <span>{currentOption.label}</span>
            <svg
              className={`h-3 w-3 text-zinc-500 transition-transform duration-300 ${isOpen ? "rotate-180" : ""}`}
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth="2.5"
            >
              <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
            </svg>
          </div>
        </button>

        {/* Click Outside Overlay */}
        {isOpen && (
          <div
            className="fixed inset-0 z-30 cursor-default bg-black/5 sm:bg-transparent"
            onClick={() => setIsOpen(false)}
          />
        )}

        {/* Dropdown Options List */}
        {isOpen && (
          <div className="absolute right-0 sm:left-0 mt-2 w-64 sm:w-80 rounded-2xl border border-zinc-200 bg-white shadow-2xl z-40 py-2 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200 origin-top-right sm:origin-top-left">
            <div className="px-4 py-2 text-[10px] font-bold text-zinc-400 uppercase tracking-widest border-b border-zinc-100 mb-1">
              {t("navigateTo")}
            </div>

            {/* Desktop Modern List Layout */}
            <div className="hidden sm:block p-2 max-h-[500px] overflow-y-auto custom-scrollbar">
              <div className="space-y-1">
                {options.map((opt) => {
                  const isSelected = opt.key === currentOption.key;
                  return (
                    <Link
                      key={opt.key}
                      href={opt.path}
                      onClick={() => setIsOpen(false)}
                      className={`flex items-start gap-4 p-3 rounded-xl transition duration-200 group ${
                        isSelected
                          ? "bg-emerald-50"
                          : "hover:bg-zinc-50"
                      }`}
                    >
                      <div className={`p-2 rounded-lg shrink-0 transition-colors ${
                        isSelected ? "bg-emerald-100 text-emerald-700" : "bg-zinc-100 text-zinc-500 group-hover:bg-emerald-50 group-hover:text-emerald-600"
                      }`}>
                        <opt.icon className="h-5 w-5" />
                      </div>
                      <div className="min-w-0">
                        <p className={`text-sm font-bold truncate ${isSelected ? "text-emerald-800" : "text-zinc-800"}`}>
                          {opt.label}
                        </p>
                        <p className="text-[10px] text-zinc-400 font-medium">
                          {opt.description}
                        </p>
                      </div>
                      {isSelected && (
                        <div className="ml-auto self-center">
                          <div className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                        </div>
                      )}
                    </Link>
                  );
                })}
              </div>
            </div>

            {/* Mobile List Layout (Untouched) */}
            <div className="sm:hidden max-h-[360px] overflow-y-auto pr-0.5 custom-scrollbar">
              {options.map((opt) => {
                const isSelected = opt.key === currentOption.key;
                return (
                  <Link
                    key={opt.key}
                    href={opt.path}
                    onClick={() => setIsOpen(false)}
                    className={`flex items-center justify-between px-4 py-3 text-sm font-bold transition duration-200 ${
                      isSelected
                        ? "text-emerald-700 bg-emerald-50/50"
                        : "text-zinc-650 hover:bg-zinc-100 hover:text-zinc-900"
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <opt.icon className={`h-5 w-5 ${isSelected ? "text-emerald-700" : "text-zinc-500"}`} />
                      <span>{opt.label}</span>
                    </div>
                    {isSelected && (
                      <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                    )}
                  </Link>
                );
              })}
            </div>

            {/* Bottom Section - Settings/SignOut (Mobile ONLY) */}
            <div className="sm:hidden">
              {/* Separator */}
              <div className="my-1 border-t border-zinc-100" />

              {/* Settings Link */}
              <button
                onClick={() => {
                  setIsOpen(false);
                  setIsSettingsModalOpen(true);
                }}
                className="flex w-full items-center gap-3 px-4 py-3 text-sm font-bold text-zinc-650 hover:bg-zinc-100 hover:text-zinc-900 transition duration-200"
              >
                <svg className="h-5 w-5 text-zinc-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                <span>{t("settings")}</span>
              </button>

              {/* Sign Out Link */}
              <button
                onClick={handleSignOut}
                className="flex w-full items-center gap-3 px-4 py-3 text-sm font-bold text-red-600 hover:bg-red-50 transition duration-200"
              >
                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                </svg>
                <span>{t("signOut")}</span>
              </button>
            </div>
          </div>
        )}
      </div>

      <SettingsModal
        isOpen={isSettingsModalOpen}
        onClose={() => setIsSettingsModalOpen(false)}
      />
    </>
  );
}
