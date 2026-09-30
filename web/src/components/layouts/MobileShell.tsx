"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { signOut } from "firebase/auth";
import { doc, setDoc, collection, query, where, onSnapshot } from "firebase/firestore";
import { auth, db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import SettingsModal from "@/components/SettingsModal";
import ManageSubscriptionModal from "@/components/ManageSubscriptionModal";
import { useTranslations } from "next-intl";
import NotificationDrawer from "@/components/NotificationDrawer";
import TaskCompletionModal from "@/components/TaskCompletionModal";
import {
  groupTasks,
  calculateWeightAlerts,
  calculateStockAlerts,
  TaskGroupItem,
  FeedStockAlertItem,
} from "@/lib/notificationUtils";
import { Pig, TaskItem } from "@/lib/types";
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

interface MobileShellProps {
  children: React.ReactNode;
}

export default function MobileShell({
  children,
}: MobileShellProps) {
  const tNav = useTranslations("Navigation");
  const tDash = useTranslations("Dashboard");
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedLang, setSelectedLang] = useState("en");
  const [isLangDropdownOpen, setIsLangDropdownOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const [isManageModalOpen, setIsManageModalOpen] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(false);

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

  // Notification Drawer & Modals state
  const [isNotificationDrawerOpen, setIsNotificationDrawerOpen] = useState(false);
  const [isCompletionModalOpen, setIsCompletionModalOpen] = useState(false);
  const [tasksToEdit, setTasksToEdit] = useState<TaskItem[]>([]);
  const [allPigs, setAllPigs] = useState<Pig[]>([]);
  const [rawTasks, setRawTasks] = useState<TaskItem[]>([]);
  const [groupedTasks, setGroupedTasks] = useState<TaskGroupItem[]>([]);
  const [weightAlerts, setWeightAlerts] = useState<Pig[]>([]);
  const [stockAlerts, setStockAlerts] = useState<FeedStockAlertItem[]>([]);

  const pathname = usePathname();
  const router = useRouter();
  const { user, userProfile, activeFarmUid, isStaff, isFinancialsRestricted, isPassActive, passTimeRemaining } = useAuth();

  const isHome = pathname === "/dashboard";

  const BASE_NAV_OPTIONS: NavOption[] = [
    { key: "home", label: tNav("home"), path: "/dashboard", icon: HomeIcon },
    { key: "herd", label: tNav("herd"), path: "/dashboard/herd", icon: HerdDataIcon },
    { key: "feed", label: tNav("feed"), path: "/dashboard/feed", icon: FeedManagementIcon },
    { key: "activities", label: tNav("activities"), path: "/dashboard/activities", icon: HerdActivitiesIcon },
    { key: "financials", label: tNav("financials"), path: "/dashboard/financials", icon: FinancialsIcon },
    { key: "hr", label: tNav("hr"), path: "/dashboard?section=hr", icon: HumanResourcesIcon },
    { key: "hub", label: tNav("hub"), path: "/dashboard?section=hub", icon: LocalHubIcon },
    { key: "symptoms", label: tNav("symptoms"), path: "/dashboard?section=symptoms", icon: SymptomsAnalyzerIcon },
    { key: "weight", label: tNav("weight"), path: "/dashboard?section=weight", icon: WeightCheckerIcon },
    { key: "training", label: tNav("training"), path: "/dashboard?section=training", icon: TrainingTipsIcon },
    { key: "billing", label: tNav("billing"), path: "/dashboard/billing", icon: PremiumIcon },
  ].filter((o) => !isFinancialsRestricted || (o.key !== "financials" && o.key !== "hr"));

  const ADMIN_NAV_OPTION: NavOption = {
    key: "admin",
    label: tNav("admin"),
    path: "/admin",
    icon: ShieldCheckIcon,
  };

  useEffect(() => {
    if (userProfile?.appLanguage) {
      setSelectedLang(userProfile.appLanguage);
    }
  }, [userProfile?.appLanguage]);

  // Reactive listeners for tasks, weight checks, and inventory alerts
  useEffect(() => {
    if (!activeFarmUid) return;

    // 1. Task Listener
    const tasksQuery = query(
      collection(db, "users", activeFarmUid, "tasks"),
      where("completed", "==", false)
    );
    const unsubscribeTasks = onSnapshot(
      tasksQuery,
      (snapshot) => {
        const tasks = snapshot.docs.map((docSnap) => ({
          id: docSnap.id,
          ...docSnap.data(),
        })) as TaskItem[];
        setRawTasks(tasks);
      },
      (error) => console.error("Error querying tasks in MobileShell:", error)
    );

    // 2. Pig Listener for Weight Reminders & Female filters
    const pigsQuery = collection(db, "users", activeFarmUid, "pigs");
    const unsubscribePigs = onSnapshot(
      pigsQuery,
      (snapshot) => {
        const pigs = snapshot.docs.map((docSnap) => ({
          id: docSnap.id,
          ...docSnap.data(),
        })) as Pig[];
        setAllPigs(pigs);
        setWeightAlerts(calculateWeightAlerts(pigs));
      },
      (error) => console.error("Error querying pigs in MobileShell:", error)
    );

    // 3. Feed Listener for Stock Alerts
    const feedQuery = collection(db, "users", activeFarmUid, "feed_inventory");
    const unsubscribeFeed = onSnapshot(
      feedQuery,
      (snapshot) => {
        const items = snapshot.docs.map((docSnap) => ({
          id: docSnap.id,
          ...docSnap.data(),
        }));
        setStockAlerts(calculateStockAlerts(items));
      },
      (error) => console.error("Error querying feed in MobileShell:", error)
    );

    return () => {
      unsubscribeTasks();
      unsubscribePigs();
      unsubscribeFeed();
    };
  }, [activeFarmUid]);

  // Re-group tasks whenever raw tasks or pigs update
  useEffect(() => {
    setGroupedTasks(groupTasks(rawTasks, allPigs));
  }, [rawTasks, allPigs]);

  const totalNotifications = groupedTasks.length + weightAlerts.length + stockAlerts.length;

  const handleLanguageChange = async (langCode: string) => {
    setSelectedLang(langCode);
    setIsLangDropdownOpen(false);

    // Set cookie and reload to apply new locale
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

  const handleSelectTaskGroup = (tasks: TaskItem[]) => {
    setTasksToEdit(tasks);
    setIsCompletionModalOpen(true);
    setIsNotificationDrawerOpen(false);
  };

  return (
    <div className="relative flex flex-col h-[100dvh] bg-[#F8FAF9] dark:bg-[#121212] text-zinc-900 dark:text-zinc-100 font-sans overflow-x-hidden">
      {/* ── Top App Bar ── */}
      <header className="sticky top-0 z-50 flex items-center justify-between h-14 px-4 bg-white/90 dark:bg-[#1E1E1E]/90 backdrop-blur-md border-b border-zinc-100 dark:border-zinc-800 shadow-sm">
        {/* Upper Left: Farm Logo + Farm Name (Clickable to open Settings/Profile) */}
        <button
          id="mobile-farm-header-button"
          onClick={() => setIsSettingsModalOpen(true)}
          className="flex items-center gap-2.5 text-left select-none hover:opacity-85 active:scale-95 transition-all max-w-[65%]"
          title="Profile & Settings"
        >
          <div className="relative h-11 w-11 rounded-xl ring-1.5 ring-emerald-500/25 bg-emerald-50/80 dark:bg-emerald-950/40 flex items-center justify-center overflow-hidden flex-shrink-0 shadow-xs">
            <img
              src={userProfile?.farmLogo || "/app_logo.png"}
              alt="Farm Logo"
              className="h-full w-full object-cover p-0.5"
              onError={(e) => {
                (e.currentTarget as HTMLImageElement).src = "/app_logo.png";
              }}
            />
          </div>
          <div className="flex flex-col min-w-0">
            <span className="font-black text-sm text-zinc-900 dark:text-zinc-100 truncate leading-tight">
              {userProfile?.farmName || "SmartSwine"}
            </span>
            <span className="text-[11px] font-semibold text-emerald-600 dark:text-emerald-400 truncate leading-none mt-0.5">
              {userProfile?.firstName ? `Welcome Farmer ${userProfile.firstName}` : "Welcome Farmer"}
            </span>
          </div>
        </button>

        {/* Right Actions: Notification Bell + Help Guide + Hamburger Menu */}
        <div className="flex items-center gap-1">
          {/* Notification bell */}
          <button
            onClick={() => setIsNotificationDrawerOpen(true)}
            aria-label={tDash("upcomingActivities")}
            className="relative p-2 rounded-xl text-zinc-600 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors active:scale-95"
          >
            <svg className="h-6 w-6 text-emerald-700 dark:text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            {totalNotifications > 0 && (
              <span className="absolute top-1.5 right-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-black text-white ring-2 ring-white dark:ring-zinc-900">
                {totalNotifications > 9 ? "9+" : totalNotifications}
              </span>
            )}
          </button>

          {/* How-To Guide Help Button */}
          <Link
            href="/dashboard/guide"
            aria-label="How-To Guide"
            className="p-2 rounded-xl text-emerald-700 dark:text-emerald-400 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors active:scale-95"
          >
            <svg className="h-5.5 w-5.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
              <circle cx="12" cy="12" r="9" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3m.08 4h.01" />
            </svg>
          </Link>

          {/* Hamburger - Always on Right */}
          <button
            id="mobile-menu-toggle"
            onClick={() => setDrawerOpen(true)}
            aria-label="Open navigation menu"
            className="p-2 -mr-1 rounded-xl text-zinc-700 dark:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
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
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm animate-in fade-in duration-300"
          onClick={() => setDrawerOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* ── Drawer Panel ── */}
      <aside
        className={`fixed top-0 right-0 z-[60] h-full w-[82%] max-w-[320px] flex flex-col bg-gradient-to-b from-[#0E3820] via-[#092214] to-[#04120A] text-white shadow-2xl transition-transform duration-300 ease-out rounded-l-3xl border-l border-emerald-700/30 overflow-hidden ${
          drawerOpen ? "translate-x-0" : "translate-x-full"
        }`}
        aria-label="Navigation drawer"
      >
        {/* Drawer Header */}
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
              onClick={() => setDrawerOpen(false)}
              className="p-1.5 rounded-lg text-white/70 hover:text-white hover:bg-white/10 transition-colors"
              aria-label="Close menu"
            >
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {/* Status Pill Badge */}
          <button
            type="button"
            onClick={() => {
              setDrawerOpen(false);
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

        {/* Scrollable Navigation Body */}
        <div className="flex-1 overflow-y-auto custom-scrollbar p-3 space-y-3">
          {/* 1. Home */}
          <Link
            href="/dashboard"
            onClick={() => setDrawerOpen(false)}
            className={`flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold transition-all duration-200 ${
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
              <span>{tNav("home")}</span>
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
                <span className="font-bold text-xs">{isDarkMode ? tNav("dark") : tNav("light")}</span>
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
              {tNav("quickAccess")}
            </div>
            <div className="space-y-1 mt-1">
              {/* Add pigs */}
              <Link
                href="/dashboard/herd?action=add"
                onClick={() => setDrawerOpen(false)}
                className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
              >
                <div className="flex items-center gap-3">
                  <div className="h-8 w-8 rounded-lg bg-emerald-500/20 text-emerald-300 flex items-center justify-center shrink-0">
                    <svg className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                      <circle cx="12" cy="12" r="9" />
                      <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v8m-4-4h8" />
                    </svg>
                  </div>
                  <span>{tNav("addPigs")}</span>
                </div>
              </Link>

              {/* Calculate feed (renamed from calculator) */}
              <Link
                href="/dashboard/feed/calculator"
                onClick={() => setDrawerOpen(false)}
                className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
              >
                <div className="flex items-center gap-3">
                  <div className="h-8 w-8 rounded-lg bg-amber-500/20 text-amber-300 flex items-center justify-center shrink-0">
                    <CalculateIcon className="h-4.5 w-4.5" />
                  </div>
                  <span>{tNav("calculateFeed")}</span>
                </div>
              </Link>

              {/* Mix feed */}
              <Link
                href="/dashboard/feed/mix"
                onClick={() => setDrawerOpen(false)}
                className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
              >
                <div className="flex items-center gap-3">
                  <div className="h-8 w-8 rounded-lg bg-amber-500/20 text-amber-300 flex items-center justify-center shrink-0">
                    <ScienceIcon className="h-4.5 w-4.5" />
                  </div>
                  <span>{tNav("mixFeed")}</span>
                </div>
              </Link>

              {/* Analyze feed */}
              <Link
                href="/dashboard/feed/analyze"
                onClick={() => setDrawerOpen(false)}
                className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
              >
                <div className="flex items-center gap-3">
                  <div className="h-8 w-8 rounded-lg bg-sky-500/20 text-sky-300 flex items-center justify-center shrink-0">
                    <AnalyticsIcon className="h-4.5 w-4.5" />
                  </div>
                  <span>{tNav("analyzeFeed")}</span>
                </div>
              </Link>

              {/* Add employee */}
              <Link
                href="/dashboard?section=hr&action=add"
                onClick={() => setDrawerOpen(false)}
                className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
              >
                <div className="flex items-center gap-3">
                  <div className="h-8 w-8 rounded-lg bg-purple-500/20 text-purple-300 flex items-center justify-center shrink-0">
                    <svg className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                    </svg>
                  </div>
                  <span>{tNav("addEmployee")}</span>
                </div>
              </Link>

              {/* Find disease */}
              <Link
                href="/dashboard?section=symptoms"
                onClick={() => setDrawerOpen(false)}
                className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
              >
                <div className="flex items-center gap-3">
                  <div className="h-8 w-8 rounded-lg bg-indigo-500/20 text-indigo-300 flex items-center justify-center shrink-0">
                    <SymptomsAnalyzerIcon className="h-4.5 w-4.5" />
                  </div>
                  <span>{tNav("findDisease")}</span>
                </div>
              </Link>

              {/* Billing & Subscription */}
              <Link
                href="/dashboard/billing"
                onClick={() => setDrawerOpen(false)}
                className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
              >
                <div className="flex items-center gap-3">
                  <div className="h-8 w-8 rounded-lg bg-emerald-500/20 text-emerald-300 flex items-center justify-center shrink-0">
                    <PremiumIcon className="h-4.5 w-4.5" />
                  </div>
                  <span>{tNav("billing")}</span>
                </div>
              </Link>
            </div>
          </div>

          <div className="border-t border-emerald-700/30 my-2" />

          {/* 4. How-To Guide */}
          <Link
            href="/dashboard/guide"
            onClick={() => setDrawerOpen(false)}
            className="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 transition-colors"
          >
            <div className="h-8 w-8 rounded-lg bg-sky-500/20 flex items-center justify-center text-sky-300">
              <svg className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="9" />
                <path strokeLinecap="round" strokeLinejoin="round" d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3m.08 4h.01" />
              </svg>
            </div>
            <span>{tNav("howToGuide") || "How-To Guide"}</span>
          </Link>

          {/* 5. Settings */}
          <button
            onClick={() => {
              setDrawerOpen(false);
              setIsSettingsModalOpen(true);
            }}
            className="flex w-full items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 transition-colors"
          >
            <div className="h-8 w-8 rounded-lg bg-white/10 flex items-center justify-center text-zinc-300">
              <svg className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37.996.608 2.296.07 2.572-1.065z" />
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </div>
            <span>{tNav("settings")}</span>
          </button>

          {/* Admin Panel (if admin) */}
          {userProfile?.isAdmin && (
            <Link
              href="/admin"
              onClick={() => setDrawerOpen(false)}
              className={`flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold transition-colors ${
                pathname === "/admin" ? "bg-[#1B5530] text-white border border-emerald-400/40" : "text-white/90 hover:bg-white/10"
              }`}
            >
              <div className="flex items-center gap-3">
                <div className="h-8 w-8 rounded-lg bg-amber-400/20 flex items-center justify-center text-amber-300">
                  <ShieldCheckIcon className="h-4.5 w-4.5" />
                </div>
                <span>{tNav("admin")}</span>
              </div>
              {pathname === "/admin" && <span className="h-2 w-2 rounded-full bg-emerald-400" />}
            </Link>
          )}

          {/* 5. Sign Out */}
          <button
            id="mobile-signout-button"
            onClick={handleSignOut}
            className="flex w-full items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-semibold text-red-300 hover:bg-red-500/20 transition-colors"
          >
            <div className="h-8 w-8 rounded-lg bg-red-500/20 flex items-center justify-center text-red-400">
              <svg className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
            </div>
            <span>{tNav("signOut")}</span>
          </button>
        </div>

        {/* Footer */}
        <div className="bg-[#06180C] border-t border-white/10 px-4 py-2.5 flex items-center justify-between text-[11px]">
          <span className="font-bold text-white/70">SmartSwine • Pro Edition</span>
          <span className="font-semibold text-emerald-400">v2.4</span>
        </div>
      </aside>

      {/* ── Page Content ── */}
      <main className="flex-1 flex flex-col overflow-y-auto overflow-x-hidden pb-24 touch-pan-y">
        {children}
      </main>

      <SettingsModal
        isOpen={isSettingsModalOpen}
        onClose={() => setIsSettingsModalOpen(false)}
      />

      {/* Slide-Over Notification Drawer */}
      <NotificationDrawer
        isOpen={isNotificationDrawerOpen}
        onClose={() => setIsNotificationDrawerOpen(false)}
        groupedTasks={groupedTasks}
        weightAlerts={weightAlerts}
        stockAlerts={stockAlerts}
        onSelectTaskGroup={handleSelectTaskGroup}
      />

      {/* Task Completion Modal */}
      {activeFarmUid && (
        <TaskCompletionModal
          isOpen={isCompletionModalOpen}
          onClose={() => setIsCompletionModalOpen(false)}
          tasksToEdit={tasksToEdit}
          allPigs={allPigs}
          activeFarmUid={activeFarmUid}
        />
      )}

      {/* Subscription Management Modal */}
      <ManageSubscriptionModal
        isOpen={isManageModalOpen}
        onClose={() => setIsManageModalOpen(false)}
      />
    </div>
  );
}
