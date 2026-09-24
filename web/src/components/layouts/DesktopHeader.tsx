"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { doc, setDoc, collection, query, where, onSnapshot } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import UserProfileDropdown from "@/components/UserProfileDropdown";
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
import { useTranslations } from "next-intl";

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

export default function DesktopHeader({ label, showBack, backPath }: { label?: string; showBack?: boolean; backPath?: string }) {
  const t = useTranslations("Dashboard");
  const { user, userProfile, activeFarmUid } = useAuth();
  const [selectedLang, setSelectedLang] = useState("en");
  const [isLangDropdownOpen, setIsLangDropdownOpen] = useState(false);

  // Notification Drawer & Modals state
  const [isNotificationDrawerOpen, setIsNotificationDrawerOpen] = useState(false);
  const [isCompletionModalOpen, setIsCompletionModalOpen] = useState(false);
  const [tasksToEdit, setTasksToEdit] = useState<TaskItem[]>([]);
  const [allPigs, setAllPigs] = useState<Pig[]>([]);
  const [rawTasks, setRawTasks] = useState<TaskItem[]>([]);
  const [groupedTasks, setGroupedTasks] = useState<TaskGroupItem[]>([]);
  const [weightAlerts, setWeightAlerts] = useState<Pig[]>([]);
  const [stockAlerts, setStockAlerts] = useState<FeedStockAlertItem[]>([]);

  const router = useRouter();

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
      <header className="border-b border-zinc-200 bg-white/80 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-4">
            {showBack && (
              <button
                onClick={() => router.push(backPath || "/dashboard")}
                className="p-2 hover:bg-zinc-100 rounded-lg transition-colors text-zinc-600"
                aria-label="Go back"
              >
                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
                </svg>
              </button>
            )}
            <Link href="/dashboard" className="flex items-center gap-3 hover:opacity-80 transition-opacity cursor-pointer">
              <div className="h-8 w-8 rounded-lg bg-emerald-50 border border-emerald-500/30 p-0.5 flex items-center justify-center overflow-hidden shadow-xs flex-shrink-0">
                <img
                  src={userProfile?.farmLogo || "/app_logo.png"}
                  alt="Farm Logo"
                  className="h-full w-full object-cover rounded-md"
                  onError={(e) => {
                    (e.currentTarget as HTMLImageElement).src = "/app_logo.png";
                  }}
                />
              </div>
              <span className="font-bold text-sm bg-gradient-to-r from-emerald-700 via-emerald-600 to-green-500 bg-clip-text text-transparent mr-2 truncate max-w-[200px]">
                {userProfile?.farmName || "SmartSwine"}
              </span>
            </Link>
          </div>

          <div className="flex items-center gap-3">
            {label && (
              <h1 className="hidden md:block text-[10px] font-black text-zinc-400 tracking-widest mr-2 uppercase">
                {label}
              </h1>
            )}
            {/* Notification Bell */}
            <div className="relative">
              <button
                onClick={() => setIsNotificationDrawerOpen(true)}
                className="relative p-2 text-zinc-650 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition duration-200 focus:outline-none"
                aria-label={t("upcomingActivities")}
              >
                <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
                {totalNotifications > 0 && (
                  <span className="absolute top-1 right-1 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[11px] font-bold text-white ring-2 ring-white">
                    {totalNotifications > 9 ? "9+" : totalNotifications}
                  </span>
                )}
              </button>
            </div>

            {/* How-To Guide Button */}
            <Link
              href="/dashboard/guide"
              className="p-2 text-zinc-650 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition duration-200 focus:outline-none"
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
                className="inline-flex items-center justify-center gap-1.5 h-10 px-3 bg-zinc-100 hover:bg-zinc-200 border border-zinc-200 rounded-xl text-xs font-semibold text-zinc-650 hover:text-zinc-950 transition duration-350 shadow-sm focus:outline-none select-none"
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
                <div className="absolute right-0 mt-2 w-48 rounded-xl border border-zinc-200 bg-white/95 backdrop-blur-md shadow-xl z-40 py-1.5 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200 origin-top-right">
                  <div className="px-3 py-1 text-[10px] font-bold text-zinc-400 uppercase tracking-wider border-b border-zinc-100 mb-1">
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
                              ? "text-emerald-700 bg-emerald-50/50"
                              : "text-zinc-650 hover:bg-zinc-100 hover:text-zinc-900"
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

            <div className="h-6 w-px bg-zinc-200 mx-1" />

            <NavbarDropdown />
            <UserProfileDropdown />
          </div>
        </div>
      </header>

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
    </>
  );
}
