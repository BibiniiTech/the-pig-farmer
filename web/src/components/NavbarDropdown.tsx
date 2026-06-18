"use client";

import React, { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
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
  { key: "feed", label: "Feed", path: "/dashboard/feed", icon: FeedManagementIcon },
  { key: "activities", label: "Herd Activities", path: "/dashboard/activities", icon: HerdActivitiesIcon },
  { key: "financials", label: "Financials", path: "/dashboard/financials", icon: FinancialsIcon },
  { key: "hr", label: "Human Resources", path: "/dashboard/hr", icon: HumanResourcesIcon },
  { key: "hub", label: "Local Hub", path: "/dashboard/hub", icon: LocalHubIcon },
  { key: "symptoms", label: "Symptoms Analyzer", path: "/dashboard/symptoms", icon: SymptomsAnalyzerIcon },
  { key: "weight", label: "Weight Checker", path: "/dashboard/weight", icon: WeightCheckerIcon },
  { key: "training", label: "Training", path: "/dashboard/training", icon: TrainingTipsIcon },
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
  const pathname = usePathname();
  const { userProfile } = useAuth();

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

  return (
    <div className="relative inline-block text-left">
      {/* Dropdown Toggle Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="inline-flex items-center gap-2 px-3 py-1.5 bg-zinc-150/70 hover:bg-zinc-200/80 border border-zinc-200 rounded-xl text-xs font-bold text-zinc-800 transition duration-300 shadow-sm focus:outline-none select-none"
      >
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
      </button>

      {/* Click Outside Overlay */}
      {isOpen && (
        <div 
          className="fixed inset-0 z-30 cursor-default" 
          onClick={() => setIsOpen(false)} 
        />
      )}

      {/* Dropdown Options List */}
      {isOpen && (
        <div className="absolute left-0 mt-2 w-56 rounded-xl border border-zinc-200 bg-white/95 backdrop-blur-md shadow-xl z-40 py-1.5 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200 origin-top-left">
          <div className="px-3 py-1 text-[10px] font-bold text-zinc-400 uppercase tracking-wider border-b border-zinc-100 mb-1">
            Navigate to
          </div>
          <div className="max-h-[320px] overflow-y-auto pr-0.5 no-scrollbar">
            {options.map((opt) => {
              const isSelected = opt.key === currentOption.key;
              return (
                <Link
                  key={opt.key}
                  href={opt.path}
                  onClick={() => setIsOpen(false)}
                  className={`flex items-center justify-between px-4 py-2.5 text-xs font-bold transition duration-200 ${
                    isSelected
                      ? "text-emerald-700 bg-emerald-50/50"
                      : "text-zinc-650 hover:bg-zinc-100 hover:text-zinc-900"
                  }`}
                >
                  <div className="flex items-center gap-2.5">
                    <opt.icon className={`h-4 w-4 ${isSelected ? "text-emerald-700" : "text-zinc-500"}`} />
                    <span>{opt.label}</span>
                  </div>
                  {isSelected && (
                    <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                  )}
                </Link>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
