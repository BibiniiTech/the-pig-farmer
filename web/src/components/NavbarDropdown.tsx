"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { signOut } from "firebase/auth";
import { doc, setDoc } from "firebase/firestore";
import { auth, db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import SettingsModal from "@/components/SettingsModal";
import ManageSubscriptionModal from "@/components/ManageSubscriptionModal";
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
  CalculateIcon,
  ScienceIcon,
  AnalyticsIcon,
} from "@/components/icons/DashboardIcons";

interface AppLanguageOption {
  code: string;
  displayName: string;
  flag: string;
}

const LANGUAGES: AppLanguageOption[] = [
  { code: "en", displayName: "English", flag: "🇺🇸" },
  { code: "fr", displayName: "Français", flag: "🇫🇷" },
  { code: "zh", displayName: "中文", flag: "🇨🇳" },
  { code: "es", displayName: "Español", flag: "🇲🇽" },
  { code: "es-es", displayName: "Español (Castellano)", flag: "🇪🇸" },
  { code: "es-do", displayName: "Español (Dominicano)", flag: "🇩🇴" },
  { code: "de", displayName: "Deutsch", flag: "🇩🇪" },
  { code: "ja", displayName: "日本語", flag: "🇯🇵" },
  { code: "pt", displayName: "Português", flag: "🇵🇹" },
  { code: "tl", displayName: "Filipino", flag: "🇵🇭" },
  { code: "vi", displayName: "Tiếng Việt", flag: "🇻🇳" },
  { code: "th", displayName: "ไทย", flag: "🇹🇭" },
  { code: "id", displayName: "Bahasa Indonesia", flag: "🇮🇩" },
  { code: "hi", displayName: "हिन्दी", flag: "🇮🇳" },
  { code: "sw", displayName: "Kiswahili", flag: "🇰🇪" },
  { code: "lg", displayName: "Oluganda", flag: "🇺🇬" },
  { code: "rw", displayName: "Ikinyarwanda", flag: "🇷🇼" },
  { code: "ht", displayName: "Kreyòl Ayisyen", flag: "🇭🇹" },
  { code: "my", displayName: "မြန်မာ", flag: "🇲🇲" },
  { code: "tpi", displayName: "Tok Pisin", flag: "🇵🇬" },
  { code: "zgh", displayName: "Tamaziɣt", flag: "🇲🇦" },
  { code: "af", displayName: "Afrikaans", flag: "🇿🇦" },
];

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
  const [isManageModalOpen, setIsManageModalOpen] = useState(false);
  const [selectedLang, setSelectedLang] = useState("en");
  const [isLangDropdownOpen, setIsLangDropdownOpen] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(false);
  const pathname = usePathname();
  const router = useRouter();
  const { user, userProfile, isPassActive, passTimeRemaining } = useAuth();

  useEffect(() => {
    if (userProfile?.appLanguage) {
      setSelectedLang(userProfile.appLanguage);
    }
  }, [userProfile?.appLanguage]);

  useEffect(() => {
    const isDark = document.documentElement.classList.contains("dark") ||
                   localStorage.getItem("theme") === "dark";
    setIsDarkMode(isDark);
    if (isDark) {
      document.documentElement.classList.add("dark");
    }
  }, []);

  const toggleDarkMode = () => {
    const next = !isDarkMode;
    setIsDarkMode(next);
    if (next) {
      document.documentElement.classList.add("dark");
      localStorage.setItem("theme", "dark");
    } else {
      document.documentElement.classList.remove("dark");
      localStorage.setItem("theme", "light");
    }
  };

  const handleLanguageChange = async (langCode: string) => {
    setSelectedLang(langCode);
    setIsLangDropdownOpen(false);
    document.cookie = `NEXT_LOCALE=${langCode}; path=/; max-age=31536000`;
    if (user) {
      try {
        const userDocRef = doc(db, "users", user.uid);
        await setDoc(userDocRef, { appLanguage: langCode }, { merge: true });
      } catch (err) {
        console.error("Failed to update language:", err);
      }
    }
    window.location.reload();
  };

  const CORE_MANAGEMENT_OPTIONS: NavOption[] = [
    { key: "home", label: t("home"), path: "/dashboard", icon: HomeIcon, description: t("homeDesc") },
    { key: "herd", label: t("herd"), path: "/dashboard/herd", icon: HerdDataIcon, description: t("herdDesc") },
    { key: "feed", label: t("feed"), path: "/dashboard/feed", icon: FeedManagementIcon, description: t("feedDesc") },
    { key: "activities", label: t("activities"), path: "/dashboard/activities", icon: HerdActivitiesIcon, description: t("activitiesDesc") },
    { key: "financials", label: t("financials"), path: "/dashboard/financials", icon: FinancialsIcon, description: t("financialsDesc") },
  ];

  const OPERATIONS_OPTIONS: NavOption[] = [
    { key: "hr", label: t("hr"), path: "/dashboard?section=hr", icon: HumanResourcesIcon, description: t("hrDesc") },
    { key: "hub", label: t("hub"), path: "/dashboard?section=hub", icon: LocalHubIcon, description: t("hubDesc") },
    { key: "symptoms", label: t("symptoms"), path: "/dashboard?section=symptoms", icon: SymptomsAnalyzerIcon, description: t("symptomsDesc") },
    { key: "weight", label: t("weight"), path: "/dashboard?section=weight", icon: WeightCheckerIcon, description: t("weightDesc") },
    { key: "training", label: t("training"), path: "/dashboard?section=training", icon: TrainingTipsIcon, description: t("trainingDesc") },
    { key: "guide", label: t("howToGuide") || "How-To Guide", path: "/dashboard/guide", icon: TrainingTipsIcon, description: "Step-by-step feature guides" },
    { key: "billing", label: t("billing"), path: "/dashboard/billing", icon: PremiumIcon, description: t("billingDesc") },
  ];

  const ADMIN_NAV_OPTION: NavOption = {
    key: "admin",
    label: t("admin"),
    path: "/admin",
    icon: ShieldCheckIcon,
    description: "System administration",
  };

  const allOptions = userProfile?.isAdmin
    ? [...CORE_MANAGEMENT_OPTIONS, ...OPERATIONS_OPTIONS, ADMIN_NAV_OPTION]
    : [...CORE_MANAGEMENT_OPTIONS, ...OPERATIONS_OPTIONS];

  // Determine current option based on path
  const currentOption = allOptions.find(
    (opt) =>
      opt.path === pathname || 
      (opt.path !== "/dashboard" && pathname.startsWith(opt.path))
  ) || allOptions[0];

  const handleSignOut = async () => {
    try {
      setIsOpen(false);
      await signOut(auth);
      router.push("/login");
    } catch (err) {
      console.error("Logout failed:", err);
    }
  };

  const renderMobileNavItem = (opt: NavOption) => {
    const isSelected = opt.key === currentOption.key;
    return (
      <Link
        key={opt.key}
        href={opt.path}
        onClick={() => setIsOpen(false)}
        className={`flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition-all duration-200 ${
          isSelected
            ? "bg-[#1B5530] text-white border border-emerald-400/40 shadow-sm"
            : "text-white/85 hover:bg-white/10 hover:text-white"
        }`}
      >
        <div className="flex items-center gap-3">
          <div className={`h-7 w-7 rounded-lg flex items-center justify-center shrink-0 ${
            isSelected ? "bg-emerald-400/25 text-emerald-300" : "bg-white/10 text-white/80"
          }`}>
            <opt.icon className="h-4 w-4" />
          </div>
          <span>{opt.label}</span>
        </div>
        {isSelected && (
          <span className="h-2 w-2 rounded-full bg-emerald-400 shadow-sm" />
        )}
      </Link>
    );
  };

  const renderDesktopNavItem = (opt: NavOption) => {
    const isSelected = opt.key === currentOption.key;
    return (
      <Link
        key={opt.key}
        href={opt.path}
        onClick={() => setIsOpen(false)}
        className={`flex items-start gap-3.5 p-2.5 rounded-xl transition duration-200 group ${
          isSelected
            ? "bg-emerald-50 border border-emerald-200/60"
            : "hover:bg-zinc-50"
        }`}
      >
        <div className={`p-2 rounded-lg shrink-0 transition-colors ${
          isSelected ? "bg-emerald-100 text-emerald-700" : "bg-zinc-100 text-zinc-500 group-hover:bg-emerald-50 group-hover:text-emerald-600"
        }`}>
          <opt.icon className="h-4.5 w-4.5" />
        </div>
        <div className="min-w-0">
          <p className={`text-xs font-bold truncate ${isSelected ? "text-emerald-800" : "text-zinc-800"}`}>
            {opt.label}
          </p>
          <p className="text-[10px] text-zinc-400 font-medium truncate">
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

        {/* ─── DESKTOP DROPDOWN ───────────────────────────────────── */}
        {isOpen && (
          <>
            <div
              className="hidden sm:block fixed inset-0 z-30 cursor-default"
              onClick={() => setIsOpen(false)}
            />
            <div className="hidden sm:block absolute right-0 mt-2 w-80 rounded-2xl border border-zinc-200 bg-white shadow-2xl z-40 py-2.5 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200 origin-top-right">
              <div className="max-h-[520px] overflow-y-auto custom-scrollbar px-2 space-y-3">
                {/* Section 1: Core Management */}
                <div>
                  <div className="px-3 py-1 text-[10px] font-bold text-zinc-400 uppercase tracking-wider">
                    Core Management
                  </div>
                  <div className="space-y-0.5 mt-1">
                    {CORE_MANAGEMENT_OPTIONS.map((opt) => renderDesktopNavItem(opt))}
                  </div>
                </div>

                {/* Section 2: Operations & Tools */}
                <div>
                  <div className="px-3 py-1 text-[10px] font-bold text-zinc-400 uppercase tracking-wider border-t border-zinc-100 pt-2">
                    Operations & Tools
                  </div>
                  <div className="space-y-0.5 mt-1">
                    {OPERATIONS_OPTIONS.map((opt) => renderDesktopNavItem(opt))}
                  </div>
                </div>

                {/* Section 3: Admin (if admin) */}
                {userProfile?.isAdmin && (
                  <div>
                    <div className="px-3 py-1 text-[10px] font-bold text-zinc-400 uppercase tracking-wider border-t border-zinc-100 pt-2">
                      Administration
                    </div>
                    <div className="space-y-0.5 mt-1">
                      {renderDesktopNavItem(ADMIN_NAV_OPTION)}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </>
        )}

        {/* ─── MOBILE RIGHT-SIDE SLIDE-IN DRAWER ─────────────────────── */}
        {isOpen && (
          <div className="sm:hidden">
            {/* Backdrop */}
            <div
              className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 animate-in fade-in duration-300"
              onClick={() => setIsOpen(false)}
            />
            
            {/* Drawer Sheet sliding from RIGHT edge */}
            <div className="fixed inset-y-0 right-0 w-[82%] max-w-[320px] bg-gradient-to-b from-[#0E3820] via-[#092214] to-[#04120A] text-white shadow-2xl z-50 flex flex-col animate-in slide-in-from-right duration-300 rounded-l-3xl border-l border-emerald-700/30 overflow-hidden">
              {/* Specced Up Header */}
              <div className="bg-[#134226] border-b border-emerald-600/30 p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="h-11 w-11 rounded-xl bg-white/10 border border-emerald-400/40 p-1 flex items-center justify-center shrink-0 shadow-md overflow-hidden">
                      <img
                        src={userProfile?.farmLogo || "/app_logo.png"}
                        alt="Farm Logo"
                        className="h-full w-full object-cover rounded-lg"
                        onError={(e) => {
                          (e.currentTarget as HTMLImageElement).src = "/app_logo.png";
                        }}
                      />
                    </div>
                    <div className="min-w-0">
                      <h2 className="text-sm font-bold text-white truncate">
                        {userProfile?.farmName || "SmartSwine"}
                      </h2>
                      <p className="text-xs text-emerald-300 truncate font-medium">
                        {userProfile?.firstName ? `Farmer ${userProfile.firstName}` : "SmartSwine Manager"}
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => setIsOpen(false)}
                    className="p-1.5 rounded-lg text-white/70 hover:text-white hover:bg-white/10 transition-colors"
                    aria-label="Close menu"
                  >
                    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>

                {/* Pro Active / Standard Badge */}
                <button
                  type="button"
                  onClick={() => {
                    setIsOpen(false);
                    if (userProfile?.isPremium) {
                      setIsManageModalOpen(true);
                    } else {
                      router.push("/dashboard/billing");
                    }
                  }}
                  className="mt-3 inline-flex items-center gap-2 rounded-lg bg-[#092917] hover:bg-[#0f3d23] px-2.5 py-1 text-[10px] font-extrabold tracking-wider uppercase border border-emerald-500/20 transition cursor-pointer select-none active:scale-95"
                  title={userProfile?.isPremium ? "Manage Premium Subscription" : "Upgrade to Premium"}
                >
                  <span className={`h-2 w-2 rounded-full ${userProfile?.isPremium ? "bg-emerald-400 animate-pulse" : isPassActive ? "bg-amber-400 animate-pulse" : "bg-zinc-400"}`} />
                  <span className={userProfile?.isPremium ? "text-emerald-300" : isPassActive ? "text-amber-300" : "text-zinc-300"}>
                    {userProfile?.isPremium ? "Premium Tier" : isPassActive ? `3h Pass (${passTimeRemaining || "Active"})` : "Free Tier"}
                  </span>
                  <svg className="h-2.5 w-2.5 text-white/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M9 5l7 7-7 7" />
                  </svg>
                </button>
              </div>

              {/* Scrollable Categorized Navigation Sections */}
              <div className="flex-1 overflow-y-auto custom-scrollbar p-3 space-y-3">
                {/* 1. Home */}
                <Link
                  href="/dashboard"
                  onClick={() => setIsOpen(false)}
                  className={`flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition-all duration-200 ${
                    pathname === "/dashboard"
                      ? "bg-[#1B5530] text-white border border-emerald-400/40 shadow-sm"
                      : "text-white/90 hover:bg-white/10 hover:text-white"
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className={`h-8 w-8 rounded-lg flex items-center justify-center shrink-0 ${
                      pathname === "/dashboard" ? "bg-emerald-400/25 text-emerald-300" : "bg-white/10 text-white/80"
                    }`}>
                      <HomeIcon className="h-4.5 w-4.5" />
                    </div>
                    <span>{t("home")}</span>
                  </div>
                  {pathname === "/dashboard" && (
                    <span className="h-2 w-2 rounded-full bg-emerald-400 shadow-sm" />
                  )}
                </Link>

                {/* 2. Dark theme toggle + Language flag side-by-side on same line */}
                <div className="grid grid-cols-2 gap-2 pt-0.5">
                  {/* Dark theme toggle pill */}
                  <button
                    onClick={toggleDarkMode}
                    className="flex items-center justify-between px-3 py-2 rounded-xl bg-[#134226] border border-emerald-500/30 text-xs font-semibold text-white hover:bg-emerald-800/40 transition-colors"
                  >
                    <div className="flex items-center gap-2">
                      {isDarkMode ? (
                        <svg className="h-4 w-4 text-amber-300" fill="currentColor" viewBox="0 0 24 24">
                          <path d="M12 3a9 9 0 109 9c0-.46-.04-.92-.1-1.36a5.389 5.389 0 01-4.4 2.26 5.403 5.403 0 01-3.14-9.8c-.44-.06-.9-.1-1.36-.1z" />
                        </svg>
                      ) : (
                        <svg className="h-4 w-4 text-amber-400" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                          <circle cx="12" cy="12" r="5" />
                          <path strokeLinecap="round" d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
                        </svg>
                      )}
                      <span className="font-bold text-xs">{isDarkMode ? t("dark") : t("light")}</span>
                    </div>
                    <div className={`w-7 h-4 rounded-full transition-colors flex items-center p-0.5 ${isDarkMode ? "bg-emerald-500 justify-end" : "bg-zinc-600 justify-start"}`}>
                      <div className="w-3 h-3 rounded-full bg-white shadow-sm" />
                    </div>
                  </button>

                  {/* Language flag dropdown */}
                  <div className="relative">
                    <button
                      onClick={() => setIsLangDropdownOpen(!isLangDropdownOpen)}
                      className="w-full flex items-center justify-between px-3 py-2 rounded-xl bg-[#134226] border border-emerald-500/30 text-xs font-semibold text-white hover:bg-emerald-800/40 transition-colors"
                    >
                      <div className="flex items-center gap-1.5 min-w-0">
                        <span className="text-base leading-none">{LANGUAGES.find((l) => l.code === selectedLang)?.flag || "🇺🇸"}</span>
                        <span className="font-bold text-xs text-emerald-300 uppercase truncate">
                          {selectedLang}
                        </span>
                      </div>
                      <svg className={`h-3.5 w-3.5 text-white/70 transition-transform ${isLangDropdownOpen ? "rotate-180" : ""}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M19 9l-7 7-7-7" />
                      </svg>
                    </button>

                    {isLangDropdownOpen && (
                      <>
                        <div className="fixed inset-0 z-30" onClick={() => setIsLangDropdownOpen(false)} />
                        <div className="absolute right-0 mt-1 w-44 rounded-xl border border-emerald-700/50 bg-[#0A2616] text-white shadow-2xl z-40 py-1.5 overflow-hidden animate-in fade-in duration-200">
                          <div className="max-h-[220px] overflow-y-auto">
                            {LANGUAGES.map((lang) => {
                              const isSelected = lang.code === selectedLang;
                              return (
                                <button
                                  key={lang.code}
                                  onClick={() => handleLanguageChange(lang.code)}
                                  className={`w-full flex items-center justify-between px-3 py-2 text-xs font-semibold ${
                                    isSelected ? "text-emerald-300 bg-emerald-900/50" : "text-white/80 hover:bg-white/10"
                                  }`}
                                >
                                  <div className="flex items-center gap-2">
                                    <span>{lang.flag}</span>
                                    <span className="truncate">{lang.displayName}</span>
                                  </div>
                                  {isSelected && <div className="h-1.5 w-1.5 rounded-full bg-emerald-400" />}
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      </>
                    )}
                  </div>
                </div>

                {/* 3. Quick Access Heading */}
                <div className="pt-2">
                  <div className="px-3 py-1.5 text-xs font-black text-emerald-400 uppercase tracking-wider">
                    {t("quickAccess")}
                  </div>
                  <div className="space-y-1 mt-1">
                    {/* Add pigs */}
                    <Link
                      href="/dashboard/herd?action=add"
                      onClick={() => setIsOpen(false)}
                      className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
                    >
                      <div className="flex items-center gap-3">
                        <div className="h-8 w-8 rounded-lg bg-emerald-500/20 text-emerald-300 flex items-center justify-center shrink-0">
                          <svg className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                            <circle cx="12" cy="12" r="9" />
                            <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v8m-4-4h8" />
                          </svg>
                        </div>
                        <span>{t("addPigs")}</span>
                      </div>
                    </Link>

                    {/* Calculate feed (renamed from calculator) */}
                    <Link
                      href="/dashboard/feed/calculator"
                      onClick={() => setIsOpen(false)}
                      className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
                    >
                      <div className="flex items-center gap-3">
                        <div className="h-8 w-8 rounded-lg bg-amber-500/20 text-amber-300 flex items-center justify-center shrink-0">
                          <CalculateIcon className="h-4.5 w-4.5" />
                        </div>
                        <span>{t("calculateFeed")}</span>
                      </div>
                    </Link>

                    {/* Mix feed */}
                    <Link
                      href="/dashboard/feed/mix"
                      onClick={() => setIsOpen(false)}
                      className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
                    >
                      <div className="flex items-center gap-3">
                        <div className="h-8 w-8 rounded-lg bg-amber-500/20 text-amber-300 flex items-center justify-center shrink-0">
                          <ScienceIcon className="h-4.5 w-4.5" />
                        </div>
                        <span>{t("mixFeed")}</span>
                      </div>
                    </Link>

                    {/* Analyze feed */}
                    <Link
                      href="/dashboard/feed/analyze"
                      onClick={() => setIsOpen(false)}
                      className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
                    >
                      <div className="flex items-center gap-3">
                        <div className="h-8 w-8 rounded-lg bg-sky-500/20 text-sky-300 flex items-center justify-center shrink-0">
                          <AnalyticsIcon className="h-4.5 w-4.5" />
                        </div>
                        <span>{t("analyzeFeed")}</span>
                      </div>
                    </Link>

                    {/* Add employee */}
                    <Link
                      href="/dashboard?section=hr&action=add"
                      onClick={() => setIsOpen(false)}
                      className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
                    >
                      <div className="flex items-center gap-3">
                        <div className="h-8 w-8 rounded-lg bg-purple-500/20 text-purple-300 flex items-center justify-center shrink-0">
                          <svg className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                          </svg>
                        </div>
                        <span>{t("addEmployee")}</span>
                      </div>
                    </Link>

                    {/* Find disease */}
                    <Link
                      href="/dashboard?section=symptoms"
                      onClick={() => setIsOpen(false)}
                      className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
                    >
                      <div className="flex items-center gap-3">
                        <div className="h-8 w-8 rounded-lg bg-pink-500/20 text-pink-300 flex items-center justify-center shrink-0">
                          <SymptomsAnalyzerIcon className="h-4.5 w-4.5" />
                        </div>
                        <span>{t("findDisease")}</span>
                      </div>
                    </Link>

                    {/* Billing & Subscription */}
                    <Link
                      href="/dashboard/billing"
                      onClick={() => setIsOpen(false)}
                      className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
                    >
                      <div className="flex items-center gap-3">
                        <div className="h-8 w-8 rounded-lg bg-emerald-500/20 text-emerald-300 flex items-center justify-center shrink-0">
                          <PremiumIcon className="h-4.5 w-4.5" />
                        </div>
                        <span>{t("billing")}</span>
                      </div>
                    </Link>
                  </div>
                </div>

                <div className="border-t border-emerald-700/30 my-2" />

                {/* How-To Guide */}
                <Link
                  href="/dashboard/guide"
                  onClick={() => setIsOpen(false)}
                  className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
                >
                  <div className="flex items-center gap-3">
                    <div className="h-8 w-8 rounded-lg bg-sky-500/20 text-sky-300 flex items-center justify-center shrink-0">
                      <svg className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                      </svg>
                    </div>
                    <span>How-To Guide</span>
                  </div>
                </Link>

                {/* 4. Settings */}
                <button
                  onClick={() => {
                    setIsOpen(false);
                    setIsSettingsModalOpen(true);
                  }}
                  className="flex w-full items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 transition-colors"
                >
                  <div className="h-8 w-8 rounded-lg bg-white/10 flex items-center justify-center text-zinc-300">
                    <svg className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                  </div>
                  <span>{t("settings")}</span>
                </button>

                {/* Admin Panel (if admin) */}
                {userProfile?.isAdmin && (
                  <Link
                    href="/admin"
                    onClick={() => setIsOpen(false)}
                    className={`flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold transition-colors ${
                      pathname === "/admin" ? "bg-[#1B5530] text-white border border-emerald-400/40" : "text-white/90 hover:bg-white/10"
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <div className="h-8 w-8 rounded-lg bg-amber-400/20 flex items-center justify-center text-amber-300">
                        <ShieldCheckIcon className="h-4.5 w-4.5" />
                      </div>
                      <span>{t("admin")}</span>
                    </div>
                    {pathname === "/admin" && <span className="h-2 w-2 rounded-full bg-emerald-400" />}
                  </Link>
                )}

                {/* 5. Sign Out */}
                <button
                  onClick={handleSignOut}
                  className="flex w-full items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-semibold text-red-300 hover:bg-red-500/20 transition-colors"
                >
                  <div className="h-8 w-8 rounded-lg bg-red-500/20 flex items-center justify-center text-red-400">
                    <svg className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                    </svg>
                  </div>
                  <span>{t("signOut")}</span>
                </button>
              </div>

              {/* Footer */}
              <div className="bg-[#06180C] border-t border-white/10 px-4 py-2.5 flex items-center justify-between text-[11px]">
                <span className="font-bold text-white/70">SmartSwine • Pro Edition</span>
                <span className="font-semibold text-emerald-400">v2.4</span>
              </div>
            </div>
          </div>
        )}
      </div>

      <SettingsModal
        isOpen={isSettingsModalOpen}
        onClose={() => setIsSettingsModalOpen(false)}
      />

      <ManageSubscriptionModal
        isOpen={isManageModalOpen}
        onClose={() => setIsManageModalOpen(false)}
      />
    </>
  );
}
