"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { signOut } from "firebase/auth";
import { collection, query, limit, onSnapshot, doc, setDoc, where } from "firebase/firestore";
import { auth, db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import quotesData from "@/lib/quotes.json";
import NavbarDropdown from "@/components/NavbarDropdown";
import TaskCompletionModal from "@/components/TaskCompletionModal";
import SettingsModal from "@/components/SettingsModal";
import {
  HerdDataIcon,
  FeedManagementIcon,
  HerdActivitiesIcon,
  FinancialsIcon,
  HumanResourcesIcon,
  LocalHubIcon,
  SymptomsAnalyzerIcon,
  WeightCheckerIcon,
  TrainingTipsIcon,
} from "@/components/icons/DashboardIcons";

interface Pig {
  id: string;
  tagNumber: string;
  birthDate: string;
  breed: string;
  gender: string;
  weight: number;
  purpose: string;
  sowTag: string;
  boarTag: string;
  location: string;
  source: string;
  status: string;
  notes: string;
}

interface HerdStats {
  total: number;
  breeders_count: number;
  porkers_count: number;
  breeders_piglets: number;
  breeders_starter: number;
  breeders_grower: number;
  boars: number;
  gilts: number;
  Pregnant: number;
  Lactating: number;
  sows: number;
  Starter: number;
  Grower: number;
  Finisher: number;
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

interface TaskItem {
  id: string;
  name: string;
  date: string;
  notes: string;
  pigIds: string[];
  completed?: boolean;
}

interface TaskGroup {
  activity: string;
  target: string;
  date: string;
  isOverdue: boolean;
  originalTasks: TaskItem[];
}

function parseTaskDate(dateStr: string): Date | null {
  if (!dateStr) return null;
  if (dateStr.match(/^\d{4}-\d{2}-\d{2}$/)) {
    return new Date(dateStr);
  }
  const parts = dateStr.split(/\s+/);
  if (parts.length >= 2) {
    const monthStr = parts[0].toLowerCase();
    const day = parseInt(parts[1], 10);
    if (!isNaN(day)) {
      const months = ["jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"];
      const monthIdx = months.findIndex(m => monthStr.startsWith(m));
      if (monthIdx !== -1) {
        const now = new Date();
        const d = new Date(now.getFullYear(), monthIdx, day);
        const diffMs = d.getTime() - now.getTime();
        const diffDays = diffMs / (1000 * 60 * 60 * 24);
        if (diffDays > 180) {
          d.setFullYear(d.getFullYear() - 1);
        } else if (diffDays < -180) {
          d.setFullYear(d.getFullYear() + 1);
        }
        return d;
      }
    }
  }
  const parsed = new Date(dateStr);
  return isNaN(parsed.getTime()) ? null : parsed;
}

function isTaskOverdue(dateStr: string): boolean {
  if (!dateStr || dateStr === "Today" || dateStr === "Tomorrow") return false;
  const taskDate = parseTaskDate(dateStr);
  if (!taskDate) return false;
  
  const now = new Date();
  taskDate.setHours(23, 59, 59, 999);
  return taskDate < now;
}

function formatToTaskDate(date: Date): string {
  const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  return `${months[date.getMonth()]} ${date.getDate()}`;
}

function convertToTaskDate(dateStr: string): string {
  if (!dateStr) return "";
  if (dateStr.match(/^\d{4}-\d{2}-\d{2}$/)) {
    const d = new Date(dateStr);
    if (!isNaN(d.getTime())) return formatToTaskDate(d);
  }
  const ddmmyyyy = dateStr.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (ddmmyyyy) {
    const day = parseInt(ddmmyyyy[1], 10);
    const month = parseInt(ddmmyyyy[2], 10) - 1;
    const year = parseInt(ddmmyyyy[3], 10);
    const d = new Date(year, month, day);
    if (!isNaN(d.getTime())) return formatToTaskDate(d);
  }
  return dateStr;
}

function groupTasks(tasks: TaskItem[], allPigs: Pig[]): TaskGroup[] {
  const filteredTasks = tasks.filter(task => {
    const name = task.name || "";
    const isFemaleSpecific = ["heat detection", "breeding/mating", "confirm pregnancy", "farrowing"].some(prefix =>
      name.toLowerCase().startsWith(prefix)
    );
    if (isFemaleSpecific) {
      const parts = name.split(":");
      const rawTarget = parts.length > 1 ? parts[1].replace(/pigs?/i, "").trim() : "";
      const pig = allPigs.find(p => p.id === rawTarget || p.tagNumber === rawTarget);
      return !pig || pig.gender?.toLowerCase() === "female";
    }
    return true;
  });

  const groups: Record<string, TaskItem[]> = {};
  filteredTasks.forEach(task => {
    const name = task.name || "";
    const activity = name.includes(":") ? name.split(":")[0].trim() : name.trim();
    const key = `${activity}_${task.date}`;
    if (!groups[key]) {
      groups[key] = [];
    }
    groups[key].push(task);
  });

  const taskGroups: TaskGroup[] = Object.values(groups).map(group => {
    const first = group[0];
    const name = first.name || "";
    const activity = name.includes(":") ? name.split(":")[0].trim() : name.trim();
    
    let target = "General";
    if (group.length > 1) {
      const tags = group.map(t => {
        const parts = t.name.split(":");
        const rawTarget = parts.length > 1 ? parts[1].replace(/pigs?/i, "").trim() : "";
        if (!rawTarget) return "General";
        const pig = allPigs.find(p => p.id === rawTarget || p.tagNumber === rawTarget);
        return pig ? pig.tagNumber : rawTarget;
      }).filter(t => t !== "");
      const uniqueTags = Array.from(new Set(tags));
      target = uniqueTags.join(", ");
    } else {
      const parts = first.name.split(":");
      const rawTarget = parts.length > 1 ? parts[1].replace(/pigs?/i, "").trim() : "";
      if (rawTarget && rawTarget !== "General") {
        const pig = allPigs.find(p => p.id === rawTarget || p.tagNumber === rawTarget);
        target = pig ? pig.tagNumber : rawTarget;
      }
    }

    const isOverdueVal = isTaskOverdue(first.date);
    const convertedDate = convertToTaskDate(first.date);

    return {
      activity,
      target,
      date: convertedDate,
      isOverdue: isOverdueVal,
      originalTasks: group
    };
  });

  return taskGroups.sort((a, b) => {
    if (a.isOverdue && !b.isOverdue) return -1;
    if (!a.isOverdue && b.isOverdue) return 1;
    return 0;
  });
}

const ArchiveIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.4"
    strokeLinecap="round"
    strokeLinejoin="round"
    {...props}
  >
    <polyline points="21 8 21 21 3 21 3 8" />
    <rect x="1" y="3" width="22" height="5" rx="1" />
    <line x1="10" y1="12" x2="14" y2="12" />
  </svg>
);

export default function DashboardPage() {
  const { user, userProfile, activeFarmUid, isStaff, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  const [pigCount, setPigCount] = useState<number>(0);
  const [feedCount, setFeedCount] = useState<number>(0);
  const [recentFinancials, setRecentFinancials] = useState<any[]>([]);
  const [statsLoading, setStatsLoading] = useState(true);
  const [greeting, setGreeting] = useState("Good Day");

  const [selectedLang, setSelectedLang] = useState("en");
  const [isLangDropdownOpen, setIsLangDropdownOpen] = useState(false);

  const [allPigs, setAllPigs] = useState<Pig[]>([]);
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [isNotificationDrawerOpen, setIsNotificationDrawerOpen] = useState(false);
  const [isCompletionModalOpen, setIsCompletionModalOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const [tasksToEdit, setTasksToEdit] = useState<TaskItem[]>([]);

  useEffect(() => {
    if (userProfile?.appLanguage) {
      setSelectedLang(userProfile.appLanguage);
    }
  }, [userProfile?.appLanguage]);

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

  const [herdStats, setHerdStats] = useState<HerdStats>({
    total: 0,
    breeders_count: 0,
    porkers_count: 0,
    breeders_piglets: 0,
    breeders_starter: 0,
    breeders_grower: 0,
    boars: 0,
    gilts: 0,
    Pregnant: 0,
    Lactating: 0,
    sows: 0,
    Starter: 0,
    Grower: 0,
    Finisher: 0,
  });

  const [currentSlide, setCurrentSlide] = useState<number>(0);
  const timerRef = React.useRef<NodeJS.Timeout | null>(null);

  const resetTimer = React.useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
    }
    timerRef.current = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % 4);
    }, 5000);
  }, []);

  useEffect(() => {
    resetTimer();
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [resetTimer]);

  const handleSlideChange = (index: number) => {
    setCurrentSlide(index);
    resetTimer();
  };

  const handleCardClick = () => {
    if (currentSlide === 3) {
      router.push("/dashboard/herd");
    } else {
      handleSlideChange((currentSlide + 1) % 4);
    }
  };

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    const hr = new Date().getHours();
    if (hr < 12) setGreeting("Good Morning");
    else if (hr < 17) setGreeting("Good Afternoon");
    else setGreeting("Good Evening");
  }, []);

  useEffect(() => {
    if (!activeFarmUid) return;

    setStatsLoading(true);

    // 1. Listen to Pigs for Detailed Stats
    const pigsQuery = collection(db, "users", activeFarmUid, "pigs");
    const unsubscribePigs = onSnapshot(pigsQuery, (snapshot) => {
      const pigList = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Pig));
      setAllPigs(pigList);
      
      const breeders = pigList.filter(p => p.purpose === "Breeder");
      const porkers = pigList.filter(p => p.purpose === "Porker");

      setHerdStats({
        total: pigList.length,
        breeders_count: breeders.length,
        porkers_count: porkers.length,
        
        breeders_piglets: breeders.filter(p => p.status === "Piglet").length,
        breeders_starter: breeders.filter(p => p.status === "Starter").length,
        breeders_grower: breeders.filter(p => p.status === "Grower").length,
        boars: breeders.filter(p => p.status === "Boar").length,
        gilts: breeders.filter(p => p.status === "Gilt").length,
        Pregnant: breeders.filter(p => p.status === "Pregnant").length,
        Lactating: breeders.filter(p => p.status === "Lactating").length,
        sows: breeders.filter(p => p.status === "Sow").length,

        Starter: porkers.filter(p => p.status === "Starter").length,
        Grower: porkers.filter(p => p.status === "Grower").length,
        Finisher: porkers.filter(p => p.status === "Finisher").length,
      });

      setPigCount(pigList.length);
    }, (error) => console.error("Error querying pigs:", error));


    // 2. Listen to Feed Inventory Count
    const feedQuery = collection(db, "users", activeFarmUid, "feed_inventory");
    const unsubscribeFeed = onSnapshot(feedQuery, (snapshot) => {
      setFeedCount(snapshot.size);
    }, (error) => console.error("Error querying feed:", error));

    // 3. Listen to Recent Financial Transactions
    const financialsQuery = query(
      collection(db, "users", activeFarmUid, "financials"),
      limit(5)
    );
    const unsubscribeFinancials = onSnapshot(financialsQuery, (snapshot) => {
      const records = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      setRecentFinancials(records);
      setStatsLoading(false);
    }, (error) => {
      console.error("Error querying financials:", error);
      setStatsLoading(false);
    });

    // 4. Listen to Tasks
    const tasksQuery = query(
      collection(db, "users", activeFarmUid, "tasks"),
      where("completed", "==", false)
    );
    const unsubscribeTasks = onSnapshot(tasksQuery, (snapshot) => {
      const taskList = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as TaskItem));
      setTasks(taskList);
    }, (error) => console.error("Error querying tasks:", error));

    return () => {
      unsubscribePigs();
      unsubscribeFeed();
      unsubscribeFinancials();
      unsubscribeTasks();
    };
  }, [activeFarmUid]);

  const handleSignOut = async () => {
    try {
      await signOut(auth);
      router.push("/login");
    } catch (err) {
      console.error("Logout failed:", err);
    }
  };

  const getQuoteOfDay = (langCode: string) => {
    const now = new Date();
    const year = now.getFullYear();
    const start = new Date(year, 0, 0);
    const diff = (now.getTime() - start.getTime()) + ((start.getTimezoneOffset() - now.getTimezoneOffset()) * 60 * 1000);
    const oneDay = 1000 * 60 * 60 * 24;
    const dayOfYear = Math.floor(diff / oneDay);
    const index = (dayOfYear + year) % 70;
    const quoteKey = `quote_${index + 1}`;
    
    const quotesMap = quotesData as Record<string, Record<string, string>>;
    const langQuotes = quotesMap[langCode] || quotesMap["en"];
    return langQuotes[quoteKey] || langQuotes["quote_1"] || "";
  };

  const dailyQuote = getQuoteOfDay(selectedLang);
  const groupedTasks = groupTasks(tasks, allPigs);

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-white text-zinc-900">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen bg-white text-zinc-900 flex flex-col font-sans overflow-hidden">
      {/* Watermark Logo Background */}
      {!isMobile && (
        <div className="fixed inset-0 z-0 flex items-center justify-center opacity-[0.15] pointer-events-none select-none">
          <img
            src="/app_logo.png"
            alt="Watermark Background Logo"
            className="w-full max-w-[1100px] max-h-[85vh] object-contain"
          />
        </div>
      )}

      <div className="relative z-10 flex flex-col min-h-screen">
        {/* Navbar Header */}
        {!isMobile && (
          <header className="border-b border-zinc-200 bg-white/80 backdrop-blur-md sticky top-0 z-50">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
              <Link href="/dashboard" className="flex items-center gap-3 hover:opacity-80 transition-opacity cursor-pointer">
                <img src="/app_logo.png" alt="SmartSwine Logo" className="h-8 w-8 object-contain rounded-md" />
                <span className="font-bold text-sm bg-gradient-to-r from-emerald-700 via-emerald-600 to-green-500 bg-clip-text text-transparent mr-2 inline-block">
                  SmartSwine
                </span>
              </Link>

              <div className="flex items-center gap-4">
                <NavbarDropdown />
                {/* Notification Bell */}
                {/* Notification Bell */}
                <div className="relative">
                  <button
                    onClick={() => setIsNotificationDrawerOpen(!isNotificationDrawerOpen)}
                    className="relative p-2 text-zinc-650 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition duration-200 focus:outline-none"
                    aria-label="Upcoming Activities"
                  >
                    <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                    </svg>
                    {groupedTasks.length > 0 && (
                      <span className="absolute top-1 right-1 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[11px] font-bold text-white ring-2 ring-white">
                        {groupedTasks.length}
                      </span>
                    )}
                  </button>
                </div>

                <div className="text-right hidden sm:block lg:hidden">
                  <p className="text-sm font-semibold text-zinc-800">
                    {greeting}, Farmer {userProfile?.firstName || ""}
                  </p>
                  <p className="text-xs text-zinc-500">
                    {isStaff ? "Staff Member" : "Farm Owner"} {userProfile?.isPremium && "• Premium"}
                  </p>
                </div>
                {/* Language Selector Dropdown */}
                <div className="relative inline-block text-left">
                  <button
                    onClick={() => setIsLangDropdownOpen(!isLangDropdownOpen)}
                    className="inline-flex items-center justify-center gap-1.5 h-10 px-4 bg-zinc-100 hover:bg-zinc-200 border border-zinc-200 rounded-lg text-xs font-semibold text-zinc-650 hover:text-zinc-950 transition duration-350 shadow-sm focus:outline-none select-none min-w-[120px]"
                    aria-label="Select Language"
                  >
                    <span className="text-sm">
                      {LANGUAGES.find((l) => l.code === selectedLang)?.flag || "🇺🇸"}
                    </span>
                    <span className="hidden md:inline">
                      {LANGUAGES.find((l) => l.code === selectedLang)?.displayName || "English"}
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

                  {/* Click Outside Overlay */}
                  {isLangDropdownOpen && (
                    <div
                      className="fixed inset-0 z-30 cursor-default"
                      onClick={() => setIsLangDropdownOpen(false)}
                    />
                  )}

                  {/* Dropdown Options */}
                  {isLangDropdownOpen && (
                    <div className="absolute right-0 mt-2 w-48 rounded-xl border border-zinc-200 bg-white/95 backdrop-blur-md shadow-xl z-40 py-1.5 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200 origin-top-right">
                      <div className="px-3 py-1 text-[12px] font-bold text-zinc-400 uppercase tracking-wider border-b border-zinc-100 mb-1">
                        Preferred Language
                      </div>
                      <div className="max-h-[240px] overflow-y-auto no-scrollbar">
                        {LANGUAGES.map((lang) => {
                          const isSelected = lang.code === selectedLang;
                          return (
                            <button
                              key={lang.code}
                              onClick={() => handleLanguageChange(lang.code)}
                              className={`w-full flex items-center justify-between px-4 py-2.5 text-xs font-bold transition duration-200 ${
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

                <button
                  onClick={() => setIsSettingsModalOpen(true)}
                  className="hidden lg:flex items-center justify-center gap-2 h-10 px-4 rounded-lg border border-zinc-200 bg-zinc-100 text-xs font-semibold text-zinc-600 hover:bg-zinc-200 hover:text-zinc-900 transition-all min-w-[120px]"
                >
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  Settings
                </button>

                <button
                  id="logout-button"
                  onClick={handleSignOut}
                  className="rounded-lg border border-zinc-200 bg-zinc-100 h-10 px-4 py-2 text-xs font-semibold text-zinc-600 hover:bg-zinc-200 hover:text-zinc-900 transition-all min-w-[120px]"
                >
                  Sign Out
                </button>
              </div>
            </div>
          </header>
        )}

        {/* Main Body */}
        <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
          <div className="flex flex-col items-center text-center sm:flex-row sm:justify-between sm:items-center sm:text-left gap-4 bg-zinc-50/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm">
            <div className="flex flex-col gap-1">
              <h1 className="text-xl sm:text-2xl font-bold text-zinc-900">
                Welcome to {userProfile?.farmName || "SmartSwine"}
              </h1>
              <div className="hidden sm:block">
                <p className="text-sm font-bold text-emerald-800">
                  {greeting}, Farmer {userProfile?.firstName || ""}
                </p>
                <p className="text-xs text-zinc-500">
                  {isStaff ? "Staff Member" : "Farm Owner"} {userProfile?.isPremium && "• Premium"}
                </p>
              </div>
            </div>
            {userProfile?.isPremium ? (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-800 border border-emerald-200/50">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse" />
                Premium Access Active
              </span>
            ) : (
              <div className="flex items-center gap-3">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-zinc-100 px-3 py-1 text-xs font-semibold text-zinc-600 border border-zinc-200">
                  Free Tier
                </span>
                <Link href="/dashboard/billing" className="inline-flex items-center rounded-lg bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 px-3 py-1 text-xs font-bold text-emerald-700 transition-all">
                  💎 Upgrade
                </Link>
              </div>
            )}
          </div>

          {/* Herd Summary Stats Card (StatsRibbon) */}
          <div
            onClick={handleCardClick}
            className="cursor-pointer bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 relative overflow-hidden group hover:border-emerald-500/30 transition-all duration-300 shadow-sm min-h-[148px] flex flex-col justify-between"
          >
            {/* Background design elements */}
            <div className="absolute top-0 right-0 h-32 w-32 rounded-full bg-emerald-500/5 blur-2xl group-hover:bg-emerald-500/10 transition-all duration-300 pointer-events-none" />
            <div className="absolute bottom-0 left-0 h-24 w-24 rounded-full bg-teal-500/5 blur-xl pointer-events-none" />

            <div className="flex-1 flex flex-col items-center justify-center text-center px-4">
              {statsLoading ? (
                <div className="space-y-2 flex flex-col items-center">
                  <div className="h-6 w-48 bg-zinc-200 animate-pulse rounded" />
                  <div className="h-4 w-64 bg-zinc-100 animate-pulse rounded" />
                </div>
              ) : (
                <div
                  key={currentSlide}
                  className="animate-slide-in flex flex-col items-center text-center space-y-2 select-none"
                >
                  {currentSlide === 0 && (
                    <>
                      <h3 className="text-lg sm:text-xl font-bold text-zinc-800 tracking-tight">
                        Total number of Pigs: <span className="text-xl sm:text-2xl font-extrabold text-emerald-600">{herdStats.total}</span>
                      </h3>
                      <p className="text-xs sm:text-sm font-medium text-zinc-500">
                        Breeders: <span className="font-semibold text-zinc-700">{herdStats.breeders_count}</span> | Porkers: <span className="font-semibold text-zinc-700">{herdStats.porkers_count}</span>
                      </p>
                    </>
                  )}

                  {currentSlide === 1 && (
                    <>
                      <h3 className="text-lg sm:text-xl font-bold text-zinc-800 tracking-tight">
                        Total Number of Breeders: <span className="text-xl sm:text-2xl font-extrabold text-emerald-600">{herdStats.breeders_count}</span>
                      </h3>
                      <p className="text-[11px] sm:text-xs font-medium text-zinc-500 leading-relaxed max-w-2xl">
                        Piglet: <span className="font-semibold text-zinc-700">{herdStats.breeders_piglets}</span> | Starter: <span className="font-semibold text-zinc-700">{herdStats.breeders_starter}</span> | Grower: <span className="font-semibold text-zinc-700">{herdStats.breeders_grower}</span> | Boar: <span className="font-semibold text-zinc-700">{herdStats.boars}</span> | Gilt: <span className="font-semibold text-zinc-700">{herdStats.gilts}</span>
                        <span className="block mt-0.5">
                          Pregnant: <span className="font-semibold text-zinc-700">{herdStats.Pregnant}</span> | Lactating: <span className="font-semibold text-zinc-700">{herdStats.Lactating}</span> | Sow: <span className="font-semibold text-zinc-700">{herdStats.sows}</span>
                        </span>
                      </p>
                    </>
                  )}

                  {currentSlide === 2 && (
                    <>
                      <h3 className="text-lg sm:text-xl font-bold text-zinc-800 tracking-tight">
                        Total Number of Porkers: <span className="text-xl sm:text-2xl font-extrabold text-emerald-600">{herdStats.porkers_count}</span>
                      </h3>
                      <p className="text-xs sm:text-sm font-medium text-zinc-500 leading-relaxed">
                        Starter: <span className="font-semibold text-zinc-700">{herdStats.Starter}</span> | Grower: <span className="font-semibold text-zinc-700">{herdStats.Grower}</span> | Finisher: <span className="font-semibold text-zinc-700">{herdStats.Finisher}</span>
                      </p>
                    </>
                  )}

                  {currentSlide === 3 && (
                    <>
                      <div className="flex items-center gap-2 text-emerald-700">
                        <ArchiveIcon className="h-5 w-5" />
                        <h3 className="text-lg sm:text-xl font-bold text-zinc-800 tracking-tight">
                          Archived Pigs
                        </h3>
                      </div>
                      <p className="text-xs sm:text-sm font-medium text-zinc-500">
                        View culled or sold animals
                      </p>
                    </>
                  )}
                </div>
              )}
            </div>

            {/* Slide Pagination Indicator Dots */}
            <div className="flex justify-center gap-2 mt-2 z-20">
              {[0, 1, 2, 3].map((index) => (
                <button
                  key={index}
                  onClick={(e) => {
                    e.stopPropagation(); // prevent card click handler from firing
                    handleSlideChange(index);
                  }}
                  className={`h-1.5 rounded-full transition-all duration-300 ${
                    currentSlide === index ? "w-5 bg-emerald-500" : "w-1.5 bg-zinc-300 hover:bg-zinc-400"
                  }`}
                  aria-label={`Go to slide ${index + 1}`}
                />
              ))}
            </div>
          </div>

          {/* Quick Tools / Navigation Section */}
          <div className="bg-zinc-50/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm">
            <h3 className="text-lg font-bold text-zinc-900 mb-4">Farm Operations</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              <Link href="/dashboard/herd" className="flex items-center sm:items-start gap-4 p-4 rounded-xl border border-zinc-200 bg-white/60 hover:bg-zinc-50/60 backdrop-blur-md transition duration-300 group shadow-sm">
                <span className="p-3 bg-emerald-50 rounded-xl text-emerald-600 group-hover:scale-110 transition duration-300">
                  <HerdDataIcon className="h-8 w-8" />
                </span>
                <div>
                  <h4 className="text-base font-bold text-zinc-900 group-hover:text-emerald-700 transition">Herd Data</h4>
                  <p className="hidden sm:block text-sm text-zinc-500 mt-1">Manage active/archived pigs, register piglets/breeders, track locations, and view individual pig profiles.</p>
                </div>
              </Link>

              <Link href="/dashboard/feed" className="flex items-center sm:items-start gap-4 p-4 rounded-xl border border-zinc-200 bg-white/60 hover:bg-zinc-50/60 backdrop-blur-md transition duration-300 group shadow-sm">
                <span className="p-3 bg-emerald-50 rounded-xl text-emerald-600 group-hover:scale-110 transition duration-300">
                  <FeedManagementIcon className="h-8 w-8" />
                </span>
                <div>
                  <h4 className="text-base font-bold text-zinc-900 group-hover:text-emerald-700 transition">Feed Management</h4>
                  <p className="hidden sm:block text-sm text-zinc-500 mt-1">Calculate projected herd feed demand, formulate stage rations, and log inventory usage/restocks.</p>
                </div>
              </Link>

              <Link href="/dashboard/activities" className="flex items-center sm:items-start gap-4 p-4 rounded-xl border border-zinc-200 bg-white/60 hover:bg-zinc-50/60 backdrop-blur-md transition duration-300 group shadow-sm">
                <span className="p-3 bg-amber-50 rounded-xl text-amber-600 group-hover:scale-110 transition duration-300">
                  <HerdActivitiesIcon className="h-8 w-8" />
                </span>
                <div>
                  <h4 className="text-base font-bold text-zinc-900 group-hover:text-amber-700 transition">Herd Activities</h4>
                  <p className="hidden sm:block text-sm text-zinc-500 mt-1">Log mating, pregnancy confirmation, farrowing births, weaning moves, castration, iron injections, and deworming.</p>
                </div>
              </Link>

              <Link href="/dashboard/financials" className="flex items-center sm:items-start gap-4 p-4 rounded-xl border border-zinc-200 bg-white/60 hover:bg-zinc-50/60 backdrop-blur-md transition duration-300 group shadow-sm">
                <span className="p-3 bg-blue-50 rounded-xl text-blue-600 group-hover:scale-110 transition duration-300">
                  <FinancialsIcon className="h-8 w-8" />
                </span>
                <div>
                  <h4 className="text-base font-bold text-zinc-900 group-hover:text-blue-700 transition">Financials</h4>
                  <p className="hidden sm:block text-sm text-zinc-500 mt-1">Record and categorize incomes and expenses, monitor profit margins, and track transactions tied to specific pigs.</p>
                </div>
              </Link>

              <Link href="/dashboard/hr" className="flex items-center sm:items-start gap-4 p-4 rounded-xl border border-zinc-200 bg-white/60 hover:bg-zinc-50/60 backdrop-blur-md transition duration-300 group shadow-sm">
                <span className="p-3 bg-indigo-50 rounded-xl text-indigo-600 group-hover:scale-110 transition duration-300">
                  <HumanResourcesIcon className="h-8 w-8" />
                </span>
                <div>
                  <h4 className="text-base font-bold text-zinc-900 group-hover:text-indigo-700 transition">Human Resources</h4>
                  <p className="hidden sm:block text-sm text-zinc-500 mt-1">Register farm staff members, log payroll details, manage active/leave statuses, and enable application login access.</p>
                </div>
              </Link>

              <Link href="/dashboard/hub" className="flex items-center sm:items-start gap-4 p-4 rounded-xl border border-zinc-200 bg-white/60 hover:bg-zinc-50/60 backdrop-blur-md transition duration-300 group shadow-sm">
                <span className="p-3 bg-purple-50 rounded-xl text-purple-600 group-hover:scale-110 transition duration-300">
                  <LocalHubIcon className="h-8 w-8" />
                </span>
                <div>
                  <h4 className="text-base font-bold text-zinc-900 group-hover:text-purple-700 transition">Local Hub</h4>
                  <p className="hidden sm:block text-sm text-zinc-500 mt-1">Connect with verified vet doctors, feed sellers, and pig buyers, and publish local listing suggestions.</p>
                </div>
              </Link>

              <Link href="/dashboard/symptoms" className="flex items-center sm:items-start gap-4 p-4 rounded-xl border border-zinc-200 bg-white/60 hover:bg-zinc-50/60 backdrop-blur-md transition duration-300 group shadow-sm">
                <span className="p-3 bg-rose-50 rounded-xl text-rose-600 group-hover:scale-110 transition duration-300">
                  <SymptomsAnalyzerIcon className="h-8 w-8" />
                </span>
                <div>
                  <h4 className="text-base font-bold text-zinc-900 group-hover:text-rose-700 transition">Symptoms Analyzer</h4>
                  <p className="hidden sm:block text-sm text-zinc-500 mt-1">Analyze symptoms (skin, stool, breathing) against a swine disease engine to identify issues and treatments.</p>
                </div>
              </Link>

              <Link href="/dashboard/weight" className="flex items-center sm:items-start gap-4 p-4 rounded-xl border border-zinc-200 bg-white/60 hover:bg-zinc-50/60 backdrop-blur-md transition duration-300 group shadow-sm">
                <span className="p-3 bg-teal-50 rounded-xl text-teal-600 group-hover:scale-110 transition duration-300">
                  <WeightCheckerIcon className="h-8 w-8" />
                </span>
                <div>
                  <h4 className="text-base font-bold text-zinc-900 group-hover:text-teal-700 transition">Weight Checker</h4>
                  <p className="hidden sm:block text-sm text-zinc-500 mt-1">Estimate pig live and carcass weights using length and girth tape measurements. Save direct to profiles.</p>
                </div>
              </Link>

              <Link href="/dashboard/training" className="flex items-center sm:items-start gap-4 p-4 rounded-xl border border-zinc-200 bg-white/60 hover:bg-zinc-50/60 backdrop-blur-md transition duration-300 group shadow-sm">
                <span className="p-3 bg-violet-50 rounded-xl text-violet-600 group-hover:scale-110 transition duration-300">
                  <TrainingTipsIcon className="h-8 w-8" />
                </span>
                <div>
                  <h4 className="text-base font-bold text-zinc-900 group-hover:text-violet-700 transition">Training Tips</h4>
                  <p className="hidden sm:block text-sm text-zinc-500 mt-1">Access detailed husbandry guidelines on weaning, feeding, and breeding, and watch expert video tutorials.</p>
                </div>
              </Link>

              {userProfile?.isAdmin && (
                <Link href="/admin" className="flex items-center sm:items-start gap-4 p-4 rounded-xl border border-emerald-200 bg-emerald-50/30 hover:bg-emerald-50/50 backdrop-blur-md transition duration-300 group shadow-sm">
                  <span className="p-3 bg-emerald-100 rounded-xl text-emerald-600 group-hover:scale-110 transition duration-300">
                    <LocalHubIcon className="h-8 w-8" />
                  </span>
                  <div>
                    <h4 className="text-base font-bold text-emerald-900 group-hover:text-emerald-700 transition">Admin Panel</h4>
                    <p className="hidden sm:block text-sm text-emerald-700/70 mt-1">Manage global ingredients, verify provider suggestions, and update training video tutorials.</p>
                  </div>
                </Link>
              )}
            </div>
          </div>

          {/* Recent Financials Section */}
          <div className="hidden sm:block bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm">
            <h3 className="text-lg font-bold text-zinc-900 mb-4">Recent Transactions</h3>
            {statsLoading ? (
              <div className="space-y-3">
                {[...Array(3)].map((_, i) => (
                  <div key={i} className="h-12 bg-zinc-100 animate-pulse rounded-lg" />
                ))}
              </div>
            ) : recentFinancials.length === 0 ? (
              <p className="text-sm text-zinc-500 text-center py-4">No recent financial transactions found.</p>
            ) : (
              <div className="divide-y divide-zinc-100">
                {recentFinancials.map((record) => (
                  <div key={record.id} className="py-3 flex justify-between items-center">
                    <div>
                      <p className="text-sm font-semibold text-zinc-800">{record.category}</p>
                      <p className="text-xs text-zinc-500">{record.date} • {record.description}</p>
                    </div>
                    <span className={`text-sm font-semibold ${record.type === "Income" ? "text-emerald-600" : "text-rose-600"}`}>
                      {record.type === "Income" ? "+" : "-"}${record.amount.toFixed(2)}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Daily Quote / Inspiration Card */}
          <div className="bg-emerald-50/40 backdrop-blur-md border border-emerald-100 rounded-2xl p-6 shadow-sm relative overflow-hidden flex flex-col items-center text-center">
            {/* Watermark design elements */}
            <div className="absolute top-0 right-0 h-24 w-24 rounded-full bg-emerald-500/5 blur-xl pointer-events-none" />
            <div className="absolute bottom-0 left-0 h-20 w-20 rounded-full bg-teal-500/5 blur-xl pointer-events-none" />
            
            <svg
              className="h-8 w-8 text-emerald-600 mb-3"
              fill="currentColor"
              viewBox="0 0 24 24"
            >
              <path d="M14.017 21v-7.391c0-5.704 3.731-9.57 8.983-10.609l.995 2.151c-2.432.917-3.995 3.638-3.995 5.849h4v10h-9.983zm-14.017 0v-7.391c0-5.704 3.748-9.57 9-10.609l.996 2.151c-2.433.917-3.996 3.638-3.996 5.849h3.983v10h-9.983z" />
            </svg>
            <p className="text-sm font-medium italic text-zinc-700 max-w-3xl leading-relaxed select-none">
              "{dailyQuote}"
            </p>
          </div>
        </main>
      </div>

      {/* Notification Drawer / Panel */}
      {isNotificationDrawerOpen && (
        <div className="fixed inset-0 z-50 overflow-hidden">
          {/* Backdrop */}
          <div
            className="absolute inset-0 bg-black/40 backdrop-blur-sm transition-opacity"
            onClick={() => setIsNotificationDrawerOpen(false)}
          />
          <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
            <div className="w-screen max-w-md bg-white border-l border-zinc-200 shadow-2xl flex flex-col transition duration-300">
              <div className="p-6 border-b border-zinc-150 flex items-center justify-between">
                <h2 className="text-lg font-bold text-zinc-900">Upcoming Activities</h2>
                <button
                  onClick={() => setIsNotificationDrawerOpen(false)}
                  className="p-1 rounded-lg text-zinc-400 hover:text-zinc-650 hover:bg-zinc-100 transition"
                >
                  <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              
              <div className="flex-1 overflow-y-auto p-6 space-y-4 no-scrollbar">
                {groupedTasks.length === 0 ? (
                  <div className="flex flex-col items-center justify-center h-48 text-center">
                    <p className="text-sm font-semibold text-zinc-400">No upcoming activities</p>
                    <p className="text-xs text-zinc-400 mt-1">All tasks are completed!</p>
                  </div>
                ) : (
                  groupedTasks.map((taskGroup, index) => (
                    <div
                      key={index}
                      onClick={() => {
                        setTasksToEdit(taskGroup.originalTasks);
                        setIsCompletionModalOpen(true);
                        setIsNotificationDrawerOpen(false);
                      }}
                      className={`p-4 rounded-xl border transition cursor-pointer flex items-center justify-between ${
                        taskGroup.isOverdue
                          ? "bg-red-50 border-red-200 hover:bg-red-100/50"
                          : "bg-zinc-50 border-zinc-200 hover:bg-zinc-100"
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <span className={`p-2 rounded-lg ${taskGroup.isOverdue ? "bg-red-100 text-red-700" : "bg-emerald-50 text-emerald-700"}`}>
                          <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
                          </svg>
                        </span>
                        <div>
                          <h4 className="font-bold text-zinc-900 text-sm">{taskGroup.activity}</h4>
                          <p className={`text-xs mt-0.5 ${taskGroup.isOverdue ? "text-red-700/80" : "text-zinc-500"}`}>
                            {taskGroup.target === "General" ? "General Task" : taskGroup.target}
                          </p>
                        </div>
                      </div>
                      <span className={`text-[10px] font-bold px-2 py-1 rounded-full ${taskGroup.isOverdue ? "bg-red-200 text-red-800" : "bg-zinc-200 text-zinc-600"}`}>
                        {taskGroup.date}
                      </span>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {activeFarmUid && (
        <TaskCompletionModal
          isOpen={isCompletionModalOpen}
          onClose={() => setIsCompletionModalOpen(false)}
          tasksToEdit={tasksToEdit}
          allPigs={allPigs}
          activeFarmUid={activeFarmUid}
        />
      )}

      <SettingsModal
        isOpen={isSettingsModalOpen}
        onClose={() => setIsSettingsModalOpen(false)}
      />
    </div>
  );
}
