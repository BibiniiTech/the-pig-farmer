"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { signOut } from "firebase/auth";
import { doc, setDoc, collection, query, where, onSnapshot } from "firebase/firestore";
import { auth, db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
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
  icon: React.ComponentType<React.SVGProps<SVGSVGElement>>;
}

interface AppLanguageOption {
  code: string;
  displayName: string;
  flag: string;
}

const LANGUAGES: AppLanguageOption[] = [
  { code: "en", displayName: "English", flag: "🇺🇸" },
  { code: "fr", displayName: "Français", flag: "🇫🇷" },
  { code: "zh", displayName: "中文", flag: "🇨🇳" },
  { code: "es", displayName: "Español", flag: "🇪🇸" },
  { code: "tl", displayName: "Filipino", flag: "🇵🇭" },
  { code: "vi", displayName: "Tiếng Việt", flag: "🇻🇳" },
  { code: "th", displayName: "ไทย", flag: "🇹🇭" },
  { code: "pt", displayName: "Português", flag: "🇵🇹" },
  { code: "hi", displayName: "हिन्दी", flag: "🇮🇳" },
];

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

interface MobileShellProps {
  children: React.ReactNode;
}

export default function MobileShell({
  children,
}: MobileShellProps) {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedLang, setSelectedLang] = useState("en");
  const [isLangDropdownOpen, setIsLangDropdownOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const [taskCount, setTaskCount] = useState(0);

  const pathname = usePathname();
  const router = useRouter();
  const { user, userProfile, activeFarmUid, isStaff } = useAuth();

  useEffect(() => {
    if (userProfile?.appLanguage) {
      setSelectedLang(userProfile.appLanguage);
    }
  }, [userProfile?.appLanguage]);

  // Unified Task Listener for the notification bell
  useEffect(() => {
    if (!activeFarmUid) return;
    const tasksQuery = query(
      collection(db, "users", activeFarmUid, "tasks"),
      where("completed", "==", false)
    );
    const unsubscribe = onSnapshot(tasksQuery, (snapshot) => {
      setTaskCount(snapshot.size);
    }, (error) => console.error("Error querying tasks in MobileShell:", error));
    return () => unsubscribe();
  }, [activeFarmUid]);

  const handleLanguageChange = async (langCode: string) => {
    setSelectedLang(langCode);
    setIsLangDropdownOpen(false);
    if (user) {
      try {
        const userDocRef = doc(db, "users", user.uid);
        await setDoc(userDocRef, { appLanguage: langCode }, { merge: true });
      } catch (err) {
        console.error("Failed to update language:", err);
      }
    }
  };

  const navOptions = userProfile?.isAdmin
    ? [...BASE_NAV_OPTIONS, ADMIN_NAV_OPTION]
    : [...BASE_NAV_OPTIONS];

  const currentOption =
    navOptions.find(
      (opt) =>
        opt.path === pathname ||
        (opt.path !== "/dashboard" && pathname.startsWith(opt.path))
    ) || navOptions[0];

  const handleSignOut = async () => {
    try {
      setDrawerOpen(false);
      await signOut(auth);
      router.push("/login");
    } catch (err) {
      console.error("Logout failed:", err);
    }
  };

  return (
    <div className="relative flex flex-col min-h-screen bg-white text-zinc-900 font-sans overflow-hidden">
      {/* ── Top App Bar ── */}
      <header className="sticky top-0 z-50 flex items-center justify-between h-14 px-4 bg-white/90 backdrop-blur-md border-b border-zinc-100 shadow-sm">
        {/* Logo + App name (Left) */}
        <Link href="/dashboard" className="flex items-center gap-2 select-none">
          <img src="/app_logo.png" alt="SmartSwine" className="h-7 w-7 object-contain rounded-md" />
          <span className="font-extrabold text-sm bg-gradient-to-r from-emerald-700 via-emerald-600 to-green-500 bg-clip-text text-transparent">
            SmartSwine
          </span>
        </Link>

        {/* Right Actions */}
        <div className="flex items-center gap-0.5">
          {/* Notification bell */}
          <button
            onClick={() => router.push("/dashboard")}
            aria-label="Upcoming Activities"
            className="relative p-2 rounded-xl text-zinc-600 hover:bg-zinc-100 transition-colors"
          >
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            {taskCount > 0 && (
              <span className="absolute top-1.5 right-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white ring-2 ring-white">
                {taskCount > 9 ? "9+" : taskCount}
              </span>
            )}
          </button>

          {/* Language flag */}
          <div className="relative">
            <button
              onClick={() => setIsLangDropdownOpen(!isLangDropdownOpen)}
              className="p-2 rounded-xl text-lg hover:bg-zinc-100 transition-colors"
              aria-label="Select Language"
            >
              {LANGUAGES.find((l) => l.code === selectedLang)?.flag || "🇺🇸"}
            </button>

            {isLangDropdownOpen && (
              <>
                <div
                  className="fixed inset-0 z-30"
                  onClick={() => setIsLangDropdownOpen(false)}
                />
                <div className="absolute right-0 mt-1 w-48 rounded-xl border border-zinc-200 bg-white shadow-xl z-40 py-1.5 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
                  <div className="px-3 py-1 text-[10px] font-bold text-zinc-400 uppercase tracking-wider border-b border-zinc-100 mb-1">
                    Language
                  </div>
                  <div className="max-h-[240px] overflow-y-auto">
                    {LANGUAGES.map((lang) => {
                      const isSelected = lang.code === selectedLang;
                      return (
                        <button
                          key={lang.code}
                          onClick={() => handleLanguageChange(lang.code)}
                          className={`w-full flex items-center justify-between px-4 py-2 text-sm font-semibold ${
                            isSelected
                              ? "text-emerald-700 bg-emerald-50"
                              : "text-zinc-700 hover:bg-zinc-50"
                          }`}
                        >
                          <div className="flex items-center gap-2">
                            <span>{lang.flag}</span>
                            <span>{lang.displayName}</span>
                          </div>
                          {isSelected && <div className="h-1.5 w-1.5 rounded-full bg-emerald-500" />}
                        </button>
                      );
                    })}
                  </div>
                </div>
              </>
            )}
          </div>

          {/* Hamburger */}
          <button
            id="mobile-menu-toggle"
            onClick={() => setDrawerOpen(true)}
            aria-label="Open navigation menu"
            className="p-2 -mr-1 rounded-xl text-zinc-600 hover:bg-zinc-100 transition-colors"
          >
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
        </div>
      </header>

      {/* ── Slide-in Drawer Overlay ── */}
      {drawerOpen && (
        <div
          className="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm"
          onClick={() => setDrawerOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* ── Drawer Panel ── */}
      <aside
        className={`fixed top-0 left-0 z-[60] h-full w-72 flex flex-col bg-white shadow-2xl transition-transform duration-300 ease-out ${
          drawerOpen ? "translate-x-0" : "-translate-x-full"
        }`}
        aria-label="Navigation drawer"
      >
        {/* Drawer Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-zinc-100 bg-gradient-to-r from-emerald-50 to-white">
          <div className="flex items-center gap-3">
            <img src="/app_logo.png" alt="SmartSwine" className="h-9 w-9 rounded-xl object-contain shadow-sm" />
            <div>
              <p className="font-extrabold text-sm text-zinc-900 leading-tight">SmartSwine</p>
              <p className="text-[11px] font-semibold text-emerald-700 tracking-wide uppercase leading-none mt-0.5">
                {userProfile?.farmName || "Piggery Manager"}
              </p>
            </div>
          </div>
          <button
            onClick={() => setDrawerOpen(false)}
            aria-label="Close menu"
            className="p-1.5 rounded-lg text-zinc-400 hover:bg-zinc-100 transition-colors"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* User info strip */}
        <div className="flex items-center gap-3 px-5 py-3 bg-zinc-50 border-b border-zinc-100">
          <div className="h-9 w-9 rounded-full bg-gradient-to-br from-emerald-400 to-emerald-700 flex items-center justify-center text-white font-bold text-sm shadow">
            {(userProfile?.firstName?.[0] || "U").toUpperCase()}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-xs font-bold text-zinc-800 truncate">
              {userProfile?.firstName} {userProfile?.lastName}
            </p>
            <p className="text-[11px] text-zinc-500 truncate">
              {isStaff ? "Staff Member" : "Farm Owner"}
              {userProfile?.isPremium && " · Premium"}
            </p>
          </div>
          {userProfile?.isPremium && (
            <span className="flex-shrink-0 text-emerald-500">
              <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
              </svg>
            </span>
          )}
        </div>

        {/* Navigation Items */}
        <nav className="flex-1 overflow-y-auto py-2 px-2">
          <p className="px-3 py-1.5 text-[10px] font-bold text-zinc-400 uppercase tracking-widest">
            Navigate to
          </p>
          {navOptions.map((opt) => {
            const isActive = opt.key === currentOption.key;
            return (
              <Link
                key={opt.key}
                href={opt.path}
                onClick={() => setDrawerOpen(false)}
                className={`flex items-center gap-3 px-3 py-3 rounded-xl mb-0.5 transition-all duration-150 ${
                  isActive
                    ? "bg-emerald-50 text-emerald-700 font-semibold"
                    : "text-zinc-700 hover:bg-zinc-50 hover:text-zinc-900 font-medium"
                }`}
              >
                <opt.icon
                  className={`h-5 w-5 flex-shrink-0 ${
                    isActive ? "text-emerald-600" : "text-zinc-400"
                  }`}
                />
                <span className="text-sm">{opt.label}</span>
                {isActive && (
                  <span className="ml-auto h-2 w-2 rounded-full bg-emerald-500" />
                )}
              </Link>
            );
          })}

          <button
            onClick={() => {
              setDrawerOpen(false);
              setIsSettingsModalOpen(true);
            }}
            className="flex w-full items-center gap-3 px-3 py-3 rounded-xl text-zinc-700 hover:bg-zinc-50 hover:text-zinc-900 font-medium transition-all duration-150"
          >
            <svg className="h-5 w-5 flex-shrink-0 text-zinc-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
              <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <span className="text-sm">Settings</span>
          </button>

          {/* Sign Out as the last item in the list */}
          <button
            id="mobile-signout-button"
            onClick={handleSignOut}
            className="flex w-full items-center gap-3 px-3 py-3 rounded-xl text-red-600 hover:bg-red-50 transition-all duration-150 font-semibold mt-4 border-t border-zinc-50 pt-4"
          >
            <svg className="h-5 w-5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
              <path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            <span className="text-sm">Sign Out</span>
          </button>
        </nav>

      </aside>

      {/* ── Page Content ── */}
      <main className="flex-1 flex flex-col overflow-y-auto">
        {children}
      </main>

      <SettingsModal
        isOpen={isSettingsModalOpen}
        onClose={() => setIsSettingsModalOpen(false)}
      />
    </div>
  );
}
