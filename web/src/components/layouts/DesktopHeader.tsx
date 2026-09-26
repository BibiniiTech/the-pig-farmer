"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import { doc, setDoc, collection, query, where, onSnapshot } from "firebase/firestore";
import { signOut } from "firebase/auth";
import { auth, db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import NotificationDrawer from "@/components/NotificationDrawer";
import TaskCompletionModal from "@/components/TaskCompletionModal";
import ManageSubscriptionModal from "@/components/ManageSubscriptionModal";
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
  ShieldCheckIcon,
} from "@/components/icons/DashboardIcons";
import {
  groupTasks,
  calculateWeightAlerts,
  calculateStockAlerts,
  TaskGroupItem,
  FeedStockAlertItem,
} from "@/lib/notificationUtils";
import { Pig, TaskItem } from "@/lib/types";
import { useTranslations } from "next-intl";
import SettingsModal from "@/components/SettingsModal";

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

export default function DesktopHeader({
  label,
  showBack,
  backPath,
  labelColor,
}: {
  label?: string;
  showBack?: boolean;
  backPath?: string;
  labelColor?: string;
}) {
  const t = useTranslations("Dashboard");
  const tCommon = useTranslations("Common");
  const { user, userProfile, activeFarmUid, isFinancialsRestricted } = useAuth();
  const [selectedLang, setSelectedLang] = useState("en");
  const [isLangDropdownOpen, setIsLangDropdownOpen] = useState(false);

  // Notification Drawer & Modals state
  const [isNotificationDrawerOpen, setIsNotificationDrawerOpen] = useState(false);
  const [isCompletionModalOpen, setIsCompletionModalOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const [tasksToEdit, setTasksToEdit] = useState<TaskItem[]>([]);
  const [allPigs, setAllPigs] = useState<Pig[]>([]);
  const [rawTasks, setRawTasks] = useState<TaskItem[]>([]);
  const [groupedTasks, setGroupedTasks] = useState<TaskGroupItem[]>([]);
  const [weightAlerts, setWeightAlerts] = useState<Pig[]>([]);
  const [stockAlerts, setStockAlerts] = useState<FeedStockAlertItem[]>([]);

  const router = useRouter();
  const pathname = usePathname();
  const tNav = useTranslations("Navigation");
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [isManageModalOpen, setIsManageModalOpen] = useState(false);

  const handleSignOut = async () => {
    try {
      setDrawerOpen(false);
      await signOut(auth);
      router.push("/login");
    } catch (err) {
      console.error("Logout failed:", err);
    }
  };

  useEffect(() => {
    if (userProfile?.appLanguage) {
      setSelectedLang(userProfile.appLanguage);
    }
  }, [userProfile?.appLanguage]);

  useEffect(() => {
    if (!activeFarmUid) return;

    // 1. Task Listener
    const tasksQuery = query(
      collection(db, "users", activeFarmUid, "tasks"),
      where("completed", "==", false)
    );
    const unsubscribeTasks = onSnapshot(tasksQuery, (snapshot) => {
      const tasks = snapshot.docs.map((docSnap) => ({
        id: docSnap.id,
        ...docSnap.data(),
      })) as TaskItem[];
      setRawTasks(tasks);
    });

    // 2. Pig Listener for Weight Reminders & Female filters
    const pigsQuery = collection(db, "users", activeFarmUid, "pigs");
    const unsubscribePigs = onSnapshot(pigsQuery, (snapshot) => {
      const pigs = snapshot.docs.map((docSnap) => ({
        id: docSnap.id,
        ...docSnap.data(),
      })) as Pig[];
      setAllPigs(pigs);
      setWeightAlerts(calculateWeightAlerts(pigs));
    });

    // 3. Feed Listener for Stock Alerts
    const feedQuery = collection(db, "users", activeFarmUid, "feed_inventory");
    const unsubscribeFeed = onSnapshot(feedQuery, (snapshot) => {
      const items = snapshot.docs.map((docSnap) => ({
        id: docSnap.id,
        ...docSnap.data(),
      }));
      setStockAlerts(calculateStockAlerts(items));
    });

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

  const handleSelectTaskGroup = (tasks: TaskItem[]) => {
    setTasksToEdit(tasks);
    setIsCompletionModalOpen(true);
    setIsNotificationDrawerOpen(false);
  };

  return (
    <>
      <header className="border-b border-zinc-200 dark:border-zinc-800 bg-white/80 dark:bg-[#1E1E1E]/90 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-4">
            {showBack && (
              <button
                onClick={() => router.push(backPath || "/dashboard")}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-zinc-250 dark:border-zinc-700 bg-white/90 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-700 hover:text-zinc-900 transition-colors shadow-2xs font-bold text-xs"
                aria-label="Go back"
                title="Go back"
              >
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
                </svg>
                <span className="hidden sm:inline">{tCommon("back") || "Back"}</span>
              </button>
            )}
            <button
              type="button"
              onClick={() => setIsSettingsModalOpen(true)}
              className="flex items-center gap-3 hover:opacity-85 transition-opacity cursor-pointer text-left select-none"
              title="Farm Settings & Profile"
            >
              <div className="h-9 w-9 rounded-xl bg-emerald-50 border border-emerald-500/30 p-0.5 flex items-center justify-center overflow-hidden shadow-xs flex-shrink-0">
                <img
                  src={userProfile?.farmLogo || "/app_logo.png"}
                  alt="Farm Logo"
                  className="h-full w-full object-cover rounded-lg"
                  onError={(e) => {
                    (e.currentTarget as HTMLImageElement).src = "/app_logo.png";
                  }}
                />
              </div>
              <div className="flex flex-col min-w-0">
                <span className="font-bold text-sm bg-gradient-to-r from-emerald-700 via-emerald-600 to-green-500 bg-clip-text text-transparent truncate max-w-[200px] leading-tight">
                  {userProfile?.farmName || "SmartSwine"}
                </span>
                <span className="text-[11px] font-semibold text-emerald-700 truncate leading-none mt-0.5">
                  {userProfile?.firstName ? `Welcome Farmer ${userProfile.firstName}` : "Welcome Farmer"}
                </span>
              </div>
            </button>
          </div>

          <div className="flex items-center gap-3">
            {label && (
              <h1 className={`hidden md:block text-xs font-black tracking-wider mr-2 uppercase ${labelColor || "text-zinc-500 dark:text-zinc-400"}`}>
                {label}
              </h1>
            )}
            {/* Notification Bell */}
            <div className="relative">
              <button
                onClick={() => setIsNotificationDrawerOpen(true)}
                className="relative p-2 text-zinc-650 dark:text-zinc-300 hover:text-emerald-600 dark:hover:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-950/40 rounded-lg transition duration-200 focus:outline-none"
                aria-label={t("upcomingActivities")}
              >
                <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
                {totalNotifications > 0 && (
                  <span className="absolute top-1 right-1 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[11px] font-bold text-white ring-2 ring-white dark:ring-[#1E1E1E]">
                    {totalNotifications > 9 ? "9+" : totalNotifications}
                  </span>
                )}
              </button>
            </div>

            {/* How-To Guide Button */}
            <Link
              href="/dashboard/guide"
              className="p-2 text-zinc-650 dark:text-zinc-300 hover:text-emerald-600 dark:hover:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-950/40 rounded-lg transition duration-200 focus:outline-none"
              aria-label="How-To Guide"
            >
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="9" />
                <path strokeLinecap="round" strokeLinejoin="round" d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3m.08 4h.01" />
              </svg>
            </Link>

            {/* Language Selector Dropdown */}
            <div className="relative inline-block text-left">
              <button
                onClick={() => setIsLangDropdownOpen(!isLangDropdownOpen)}
                className="inline-flex items-center justify-center gap-1.5 h-10 px-3 bg-zinc-100 dark:bg-[#252525] hover:bg-zinc-200 dark:hover:bg-zinc-700 border border-zinc-200 dark:border-zinc-700 rounded-xl text-xs font-semibold text-zinc-650 dark:text-zinc-300 hover:text-zinc-950 dark:hover:text-white transition duration-350 shadow-sm focus:outline-none select-none"
                aria-label="Select Language"
              >
                <span className="text-base leading-none">
                  {LANGUAGES.find((l) => l.code === selectedLang)?.flag || "🇺🇸"}
                </span>
                <svg
                  className={`h-3 w-3 text-zinc-500 transition-transform duration-300 ${
                    isLangDropdownOpen ? "rotate-180" : ""
                  }`}
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  strokeWidth="2.5"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              {isLangDropdownOpen && (
                <div
                  className="fixed inset-0 z-30 cursor-default"
                  onClick={() => setIsLangDropdownOpen(false)}
                />
              )}

              {isLangDropdownOpen && (
                <div className="absolute right-0 mt-2 w-48 rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white/95 dark:bg-[#1E1E1E]/95 backdrop-blur-md shadow-xl z-40 py-1.5 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200 origin-top-right">
                  <div className="px-3 py-1 text-[10px] font-bold text-zinc-400 uppercase tracking-wider border-b border-zinc-100 dark:border-zinc-800 mb-1">
                    {t("language")}
                  </div>
                  <div className="max-h-[240px] overflow-y-auto no-scrollbar">
                    {LANGUAGES.map((lang) => {
                      const isSelected = lang.code === selectedLang;
                      return (
                        <button
                          key={lang.code}
                          onClick={() => handleLanguageChange(lang.code)}
                          className={`w-full flex items-center justify-between px-4 py-2 text-xs font-bold transition duration-200 ${
                            isSelected
                              ? "text-emerald-700 dark:text-emerald-400 bg-emerald-50/50 dark:bg-emerald-950/40"
                              : "text-zinc-650 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 hover:text-zinc-900 dark:hover:text-white"
                          }`}
                        >
                          <div className="flex items-center gap-2">
                            <span className="text-sm">{lang.flag}</span>
                            <span>{lang.displayName}</span>
                          </div>
                          {isSelected && (
                            <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                          )}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>

            <div className="h-6 w-px bg-zinc-200 dark:bg-zinc-700 mx-1" />

            {/* Hamburger Menu Button - Replaces old owner card dropdown */}
            <button
              id="desktop-menu-toggle"
              onClick={() => setDrawerOpen(true)}
              aria-label="Open navigation menu"
              title="Menu"
              className="p-2 -mr-1 rounded-xl text-zinc-700 dark:text-zinc-200 hover:text-emerald-700 dark:hover:text-emerald-400 hover:bg-emerald-50/70 dark:hover:bg-zinc-800 transition-colors"
            >
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>
          </div>
        </div>
      </header>

      {/* ── Slide-in Navigation Drawer ── */}
      {drawerOpen && (
        <div
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm animate-in fade-in duration-300"
          onClick={() => setDrawerOpen(false)}
          aria-hidden="true"
        />
      )}

      <aside
        className={`fixed top-0 right-0 z-[60] h-full w-[85%] max-w-[340px] flex flex-col bg-gradient-to-b from-[#0E3820] via-[#092214] to-[#04120A] text-white shadow-2xl transition-transform duration-300 ease-out rounded-l-3xl border-l border-emerald-700/30 overflow-hidden ${
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
                  {userProfile?.firstName ? `Welcome Farmer ${userProfile.firstName}` : "SmartSwine Manager"}
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
            <span className={`h-2 w-2 rounded-full ${userProfile?.isPremium ? "bg-emerald-400 animate-pulse" : "bg-amber-400"}`} />
            <span className={userProfile?.isPremium ? "text-emerald-300" : "text-amber-300"}>
              {userProfile?.isPremium ? "Premium Tier" : "Free Tier"}
            </span>
            <svg className="h-2.5 w-2.5 text-white/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </div>

        {/* Scrollable Navigation Body */}
        <div className="flex-1 overflow-y-auto custom-scrollbar p-3 space-y-2">
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

          {/* Navigation Items */}
          {[
            { key: "herd", label: tNav("herd"), path: "/dashboard/herd", icon: HerdDataIcon },
            { key: "feed", label: tNav("feed"), path: "/dashboard/feed", icon: FeedManagementIcon },
            { key: "activities", label: tNav("activities"), path: "/dashboard/activities", icon: HerdActivitiesIcon },
            { key: "financials", label: tNav("financials"), path: "/dashboard/financials", icon: FinancialsIcon },
            { key: "hr", label: tNav("hr"), path: "/dashboard?section=hr", icon: HumanResourcesIcon },
            { key: "hub", label: tNav("hub"), path: "/dashboard?section=hub", icon: LocalHubIcon },
            { key: "symptoms", label: tNav("symptoms"), path: "/dashboard?section=symptoms", icon: SymptomsAnalyzerIcon },
            { key: "weight", label: tNav("weight"), path: "/dashboard?section=weight", icon: WeightCheckerIcon },
            { key: "training", label: tNav("training"), path: "/dashboard?section=training", icon: TrainingTipsIcon },
          ]
            .filter((item) => !isFinancialsRestricted || (item.key !== "financials" && item.key !== "hr"))
            .map((item) => {
            const Icon = item.icon;
            const isSelected = pathname === item.path;
            return (
              <Link
                key={item.key}
                href={item.path}
                onClick={() => setDrawerOpen(false)}
                className={`flex items-center justify-between px-3.5 py-2 rounded-xl text-sm font-semibold transition-all duration-200 ${
                  isSelected
                    ? "bg-[#1B5530] text-white border border-emerald-400/40 shadow-sm"
                    : "text-white/90 hover:bg-white/10 hover:text-white"
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className={`h-8 w-8 rounded-lg flex items-center justify-center shrink-0 ${
                    isSelected ? "bg-emerald-400/25 text-emerald-300" : "bg-white/10 text-white/80"
                  }`}>
                    <Icon className="h-4.5 w-4.5" />
                  </div>
                  <span>{item.label}</span>
                </div>
              </Link>
            );
          })}

          {/* How-To Guide */}
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

          {/* Settings button */}
          <button
            onClick={() => {
              setDrawerOpen(false);
              setIsSettingsModalOpen(true);
            }}
            className="flex w-full items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold text-white/90 hover:bg-white/10 hover:text-white transition-all duration-200"
          >
            <div className="flex items-center gap-3">
              <div className="h-8 w-8 rounded-lg bg-white/10 flex items-center justify-center text-white/80">
                <svg className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </div>
              <span>{tNav("settings")}</span>
            </div>
          </button>

          {/* Sign Out */}
          <button
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

      <ManageSubscriptionModal
        isOpen={isManageModalOpen}
        onClose={() => setIsManageModalOpen(false)}
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

      {/* Settings Modal */}
      <SettingsModal
        isOpen={isSettingsModalOpen}
        onClose={() => setIsSettingsModalOpen(false)}
      />
    </>
  );
}
