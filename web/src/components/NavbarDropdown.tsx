"use client";

import React, { useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { signOut } from "firebase/auth";
import { auth } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import SettingsModal from "@/components/SettingsModal";
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
}

const BASE_NAV_OPTIONS: NavOption[] = [
  { key: "home", label: "Home", path: "/dashboard", icon: HomeIcon },
  { key: "herd", label: "Herd Data", path: "/dashboard/herd", icon: HerdDataIcon },
  { key: "feed", label: "Feed Management", path: "/dashboard/feed", icon: FeedManagementIcon },
  { key: "activities", label: "Herd Activities", path: "/dashboard/activities", icon: HerdActivitiesIcon },
  { key: "financials", label: "Financials", path: "/dashboard/financials", icon: FinancialsIcon },
  { key: "hr", label: "Human Resources", path: "/dashboard/hr", icon: HumanResourcesIcon },
  { key: "hub", label: "Local Hub", path: "/dashboard/hub", icon: LocalHubIcon },
  { key: "symptoms", label: "Symptoms Analyzer", path: "/dashboard/symptoms", icon: SymptomsAnalyzerIcon },
  { key: "weight", label: "Weight Checker", path: "/dashboard/weight", icon: WeightCheckerIcon },
  { key: "training", label: "Training Tips", path: "/dashboard/training", icon: TrainingTipsIcon },
  { key: "billing", label: "Billing & Premium", path: "/dashboard/billing", icon: PremiumIcon },
];

const ADMIN_NAV_OPTION: NavOption = {
  key: "admin",
  label: "Admin Panel",
  path: "/admin",
  icon: ShieldCheckIcon,
};

export default function NavbarDropdown() {
  const [isOpen, setIsOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const pathname = usePathname();
  const router = useRouter();
  const { userProfile } = useAuth();
  const { isMobile } = useDevice();

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
          className={`inline-flex items-center justify-center gap-2 h-10 transition duration-300 shadow-sm focus:outline-none select-none rounded-xl border border-zinc-200 ${
            isMobile
              ? "w-10 bg-white hover:bg-zinc-50"
              : "px-4 bg-zinc-150/70 hover:bg-zinc-200/80 text-xs font-bold text-zinc-800 lg:min-w-[160px]"
          }`}
          aria-label={isMobile ? "Open menu" : "Navigation menu"}
        >
          {isMobile ? (
            <svg className="h-6 w-6 text-zinc-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          ) : (
            <>
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
            </>
          )}
        </button>

        {/* Click Outside Overlay */}
        {isOpen && (
          <div
            className="fixed inset-0 z-30 cursor-default bg-black/5 lg:bg-transparent"
            onClick={() => setIsOpen(false)}
          />
        )}

        {/* Dropdown Options List */}
        {isOpen && (
          <div className={`absolute right-0 mt-2 w-64 rounded-2xl border border-zinc-200 bg-white shadow-2xl z-40 py-2 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200 origin-top-right ${isMobile ? "fixed top-14 right-4 mt-0 max-h-[calc(100vh-80px)]" : ""}`}>
            <div className="px-4 py-2 text-[10px] font-bold text-zinc-400 uppercase tracking-widest border-b border-zinc-100 mb-1">
              Navigate to
            </div>
            <div className="max-h-[360px] lg:max-h-[400px] overflow-y-auto pr-0.5 custom-scrollbar">
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
              <span>Settings</span>
            </button>

            {/* Sign Out Link */}
            <button
              onClick={handleSignOut}
              className="flex w-full items-center gap-3 px-4 py-3 text-sm font-bold text-red-600 hover:bg-red-50 transition duration-200"
            >
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
              <span>Sign Out</span>
            </button>
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
