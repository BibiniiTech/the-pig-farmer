"use client";

import React, { useEffect, useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { signOut } from "firebase/auth";
import { collection, query, limit, onSnapshot, doc, setDoc, where } from "firebase/firestore";
import InlineHRSection from "@/components/dashboard/InlineHRSection";
import InlineMarketSection from "@/components/dashboard/InlineMarketSection";
import InlineDiseaseFinderSection from "@/components/dashboard/InlineDiseaseFinderSection";
import InlineWeightSection from "@/components/dashboard/InlineWeightSection";
import InlineTipsSection from "@/components/dashboard/InlineTipsSection";
import { auth, db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import quotesData from "@/lib/quotes.json";
import NavbarDropdown from "@/components/NavbarDropdown";
import UserProfileDropdown from "@/components/UserProfileDropdown";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import TaskCompletionModal from "@/components/TaskCompletionModal";
import SettingsModal from "@/components/SettingsModal";
import NotificationDrawer from "@/components/NotificationDrawer";
import PassCountdownBanner from "@/components/ads/PassCountdownBanner";
import NativeAdBanner from "@/components/ads/NativeAdBanner";
import { useTranslations, useLocale } from "next-intl";
import { Pig, TaskItem, StaffMember } from "@/lib/types";
import { evaluatePerformance, calculateAgeMonths, calculateAgeDays, formatSwineAge } from "@/lib/swineGrowthDatabase";
import { getCurrencyByCountry } from "@/lib/currencyUtils";
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
  InventoryIcon,
  ScienceIcon,
  CalculateIcon,
  AnalyticsIcon,
} from "@/components/icons/DashboardIcons";
import {
  HeatIcon,
  BreedingIcon,
  PregnancyCheckIcon,
  FarrowingIcon,
  WeaningIcon,
  CastrationIcon,
  TeethClippingIcon,
  TailDockingIcon,
  DewormingIcon,
  IronIcon,
  VaccinationIcon,
  MedicationIcon,
  WeightCheckerIcon as ActWeightIcon,
  CullingIcon,
  NoteAddIcon,
} from "@/components/icons/HerdActivityIcons";

const ChevronDownIcon = ({ className = "h-5 w-5", ...props }: React.SVGProps<SVGSVGElement>) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.5"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={className}
    {...props}
  >
    <polyline points="6 9 12 15 18 9" />
  </svg>
);

const PlusIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.5"
    strokeLinecap="round"
    strokeLinejoin="round"
    {...props}
  >
    <line x1="12" y1="5" x2="12" y2="19" />
    <line x1="5" y1="12" x2="19" y2="12" />
  </svg>
);

const ArrowRightIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.5"
    strokeLinecap="round"
    strokeLinejoin="round"
    {...props}
  >
    <line x1="5" y1="12" x2="19" y2="12" />
    <polyline points="12 5 19 12 12 19" />
  </svg>
);

function CompactPigDropdownCard({ pig }: { pig: Pig }) {
  const ageDays = calculateAgeDays(pig.birthDate);
  const performance = evaluatePerformance(pig.breed, ageDays, pig.weight);

  let performanceBadge = "bg-zinc-100 text-zinc-600 border-zinc-200";
  if (performance === "Excellent") performanceBadge = "bg-amber-50 text-amber-700 border-amber-200/60";
  else if (performance === "Good") performanceBadge = "bg-emerald-50 text-emerald-700 border-emerald-200/60";
  else if (performance === "Caution") performanceBadge = "bg-yellow-50 text-yellow-700 border-yellow-200/60";
  else if (performance === "Poor") performanceBadge = "bg-rose-50 text-rose-700 border-rose-200/60";

  const ageDisplay = formatSwineAge(pig.birthDate, true);

  const genderDisplay =
    (pig.gender === "Female" ? "Female" : "Male") +
    (pig.gender === "Male" && pig.castrated ? " (Castrated)" : "");

  return (
    <Link
      href={`/dashboard/herd/${pig.id}`}
      className="p-3 rounded-xl bg-white border border-zinc-200 hover:border-emerald-400/80 hover:shadow-sm transition-all flex items-center justify-between group"
    >
      <div className="space-y-1 min-w-0">
        <span className="text-xs font-bold text-zinc-900 group-hover:text-emerald-700 transition truncate block">
          Tag: {pig.tagNumber || pig.id}
        </span>
        <div className="flex items-center gap-2 text-[11px] text-zinc-500">
          <span>Age: {ageDisplay}</span>
          <span className="text-zinc-300">•</span>
          <span>Gender: {genderDisplay}</span>
        </div>
      </div>
      <div className="flex items-center gap-2 shrink-0 ml-2">
        {performance && performance !== "Blank" && (
          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${performanceBadge}`}>
            {performance}
          </span>
        )}
        <span className="text-zinc-400 group-hover:text-emerald-600 transition">
          <ArrowRightIcon className="h-4 w-4" />
        </span>
      </div>
    </Link>
  );
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

interface FeedInventoryItem {
  id: string;
  name: string;
  quantity: number;
  minThreshold: number;
  unit: string;
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

function DashboardContent() {
  const t = useTranslations("Dashboard");
  const tNotif = useTranslations("Notifications");
  const tAct = useTranslations("Activities");
  const locale = useLocale();

  const getActivityTranslation = (activityName: string): string => {
    const lower = (activityName || "").toLowerCase().trim();
    if (lower.startsWith("heat")) return tAct("categories.heat.type");
    if (lower.startsWith("breeding") || lower.startsWith("mating")) return tAct("categories.breeding.type");
    if (lower.startsWith("confirm") || lower.startsWith("pregnancy")) return tAct("categories.pregnancy.type");
    if (lower.startsWith("farrow")) return tAct("categories.farrowing.type");
    if (lower.startsWith("wean")) return tAct("categories.weaning.type");
    if (lower.startsWith("castrat")) return tAct("categories.castration.type");
    if (lower.startsWith("teeth")) return tAct("categories.teeth.type");
    if (lower.startsWith("tail")) return tAct("categories.tail.type");
    if (lower.startsWith("deworm")) return tAct("categories.deworming.type");
    if (lower.startsWith("iron")) return tAct("categories.iron.type");
    if (lower.startsWith("vaccin")) return tAct("categories.vaccination.type");
    if (lower.startsWith("medicat")) return tAct("categories.medication.type");
    if (lower.startsWith("weight")) return tAct("categories.weight.type");
    if (lower.startsWith("cull")) return tAct("categories.culling.type");
    if (lower.startsWith("custom")) return tAct("categories.custom.type");
    return activityName;
  };
  const { user, userProfile, activeFarmUid, isStaff, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();
  const searchParams = useSearchParams();
  const rawSection = searchParams.get("section");
  const normalizeSection = (sec: string | null): string | null => {
    if (!sec) return null;
    const s = sec.toLowerCase().trim();
    if (s === "human_resources" || s === "human-resources" || s === "hr") return "hr";
    if (s === "symptoms_analyzer" || s === "disease_finder" || s === "disease-finder" || s === "symptoms") return "symptoms";
    if (s === "weight_checker" || s === "weigh_pigs" || s === "weight-checker" || s === "weight") return "weight";
    if (s === "market" || s === "market_access" || s === "market-hub" || s === "hub") return "hub";
    if (s === "production_activities" || s === "herd_activities" || s === "activities") return "activities";
    if (s === "herd" || s === "herd_data") return "herd_data";
    if (s === "feed") return "feed";
    if (s === "financials") return "financials";
    if (s === "training") return "training";
    return s;
  };
  const initialSection = normalizeSection(rawSection);
  const initialSubOption = searchParams.get("subOption");
  const initialPigTag = searchParams.get("tag");
  const initialAction = searchParams.get("action");
  const shouldOpenAddStaff = (initialSection === "hr" || rawSection === "human_resources") && (initialAction === "add" || initialSubOption === "add");

  const [pigCount, setPigCount] = useState<number>(0);
  const [feedCount, setFeedCount] = useState<number>(0);
  const [recentFinancials, setRecentFinancials] = useState<any[]>([]);
  const [statsLoading, setStatsLoading] = useState(true);
  const [expandedSection, setExpandedSection] = useState<string | null>(initialSection || null);
  const [weightSubOption, setWeightSubOption] = useState<string | null>(initialSubOption || null);
  const [weightPigTag, setWeightPigTag] = useState<string | null>(initialPigTag || null);

  const scrollToElementId = (id: string, delayMs = 150) => {
    if (typeof window === "undefined") return;
    setTimeout(() => {
      const el = document.getElementById(id);
      if (el) {
        el.scrollIntoView({ behavior: "smooth", block: "start" });
      }
    }, delayMs);
    // Secondary adjustment after collapsing transitions finish (~300ms)
    setTimeout(() => {
      const el = document.getElementById(id);
      if (el) {
        el.scrollIntoView({ behavior: "smooth", block: "start" });
      }
    }, delayMs + 220);
  };

  useEffect(() => {
    if (initialSection) {
      setExpandedSection(initialSection);
    }
  }, [initialSection]);

  useEffect(() => {
    if (initialSubOption) {
      setWeightSubOption(initialSubOption);
    }
  }, [initialSubOption]);

  useEffect(() => {
    if (initialPigTag) {
      setWeightPigTag(initialPigTag);
    }
  }, [initialPigTag]);

  useEffect(() => {
    if (initialSubOption && initialSubOption !== "add") {
      scrollToElementId(`suboption-${initialSubOption}`, 300);
    } else if (initialSection) {
      scrollToElementId(`accordion-${initialSection}`, 250);
    }
  }, [initialSection, initialSubOption]);

  const toggleSection = (sectionId: string) => {
    setExpandedSection((prev) => {
      const next = prev === sectionId ? null : sectionId;
      if (next) {
        scrollToElementId(`accordion-${next}`, 150);
      }
      return next;
    });
  };

  const [allPigs, setAllPigs] = useState<Pig[]>([]);
  const [staff, setStaff] = useState<StaffMember[]>([]);
  const [activityPage, setActivityPage] = useState<number>(0);
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [weightAlerts, setWeightAlerts] = useState<Pig[]>([]);
  const [stockAlerts, setStockAlerts] = useState<FeedInventoryItem[]>([]);
  const [isNotificationDrawerOpen, setIsNotificationDrawerOpen] = useState(false);
  const [isCompletionModalOpen, setIsCompletionModalOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const [tasksToEdit, setTasksToEdit] = useState<TaskItem[]>([]);

  const herdActivitiesList = [
    { name: "Heat Detection", key: "Heat Detection", icon: HeatIcon },
    { name: "Breeding/Mating", key: "Breeding/Mating", icon: BreedingIcon },
    { name: "Confirm Pregnancy", key: "Confirm Pregnancy", icon: PregnancyCheckIcon },
    { name: "Farrowing", key: "Farrowing", icon: FarrowingIcon },
    { name: "Weaning", key: "Weaning", icon: WeaningIcon },
    { name: "Castration", key: "Castration", icon: CastrationIcon },
    { name: "Teeth Clipping", key: "Teeth Clipping", icon: TeethClippingIcon },
    { name: "Tail Docking", key: "Tail Docking", icon: TailDockingIcon },
    { name: "Deworming", key: "Deworming", icon: DewormingIcon },
    { name: "Iron Injection", key: "Iron Injection", icon: IronIcon },
    { name: "Vaccination", key: "Vaccination", icon: VaccinationIcon },
    { name: "Medication", key: "Medication", icon: MedicationIcon },
    { name: "Weight Check", key: "Weight Check", icon: ActWeightIcon },
    { name: "Culling", key: "Culling", icon: CullingIcon },
    { name: "Custom Activity", key: "Custom", icon: NoteAddIcon }
  ];

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
    if (!activeFarmUid) return;

    setStatsLoading(true);

    // 1. Listen to Pigs for Detailed Stats
    const pigsQuery = collection(db, "users", activeFarmUid, "pigs");
    const unsubscribePigs = onSnapshot(pigsQuery, (snapshot) => {
      const pigList = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Pig));
      setAllPigs(pigList);

      const today = new Date();
      today.setHours(0,0,0,0);
      const alerts = pigList.filter(pig => {
        const lastWeight = pig.lastWeightDate || pig.birthDate;
        if (!lastWeight) return false;
        let lastDate: Date;
        if (lastWeight.includes('/')) {
          const parts = lastWeight.split('/');
          lastDate = new Date(parseInt(parts[2]), parseInt(parts[1]) - 1, parseInt(parts[0]));
        } else {
          lastDate = new Date(lastWeight);
        }
        if (isNaN(lastDate.getTime())) return false;
        const diffDays = Math.floor((today.getTime() - lastDate.getTime()) / (1000 * 60 * 60 * 24));
        return diffDays >= 30;
      });
      setWeightAlerts(alerts);

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
      const feedList = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as FeedInventoryItem));
      setFeedCount(snapshot.size);
      setStockAlerts(feedList.filter(item => item.quantity <= item.minThreshold && item.minThreshold > 0));
    }, (error) => console.error("Error querying feed:", error));

    // 3. Listen to Financial Transactions (All records for accurate totals)
    const financialsQuery = collection(db, "users", activeFarmUid, "financials");
    const unsubscribeFinancials = onSnapshot(financialsQuery, (snapshot) => {
      const records = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      setRecentFinancials(records.sort((a: any, b: any) => (b.date || "").localeCompare(a.date || "")));
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

    const staffQuery = collection(db, "users", activeFarmUid, "staff");
    const unsubscribeStaff = onSnapshot(staffQuery, (snapshot) => {
      const staffList = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as StaffMember));
      setStaff(staffList);
    }, (error) => console.error("Error querying staff:", error));

    return () => {
      unsubscribePigs();
      unsubscribeFeed();
      unsubscribeFinancials();
      unsubscribeTasks();
      unsubscribeStaff();
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

  const dailyQuote = getQuoteOfDay(locale);
  const groupedTasks = groupTasks(tasks, allPigs);

  const totalIncome = recentFinancials.filter((r: any) => r.type === "Income").reduce((sum: number, r: any) => sum + (Number(r.amount) || 0), 0);
  const totalExpenses = recentFinancials.filter((r: any) => r.type === "Expense").reduce((sum: number, r: any) => sum + (Number(r.amount) || 0), 0);
  const netProfit = totalIncome - totalExpenses;
  const currencySymbol = userProfile?.settings?.currencySymbol || (userProfile?.country ? getCurrencyByCountry(userProfile.country).symbol : "$");

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-white text-zinc-900">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen bg-white text-zinc-900 flex flex-col font-sans overflow-hidden">
      {/* Fixed SmartSwine Watermark Background Logo */}
      <div className="fixed inset-0 pointer-events-none flex items-center justify-center -z-0 select-none overflow-hidden">
        <img
          src="/app_logo.png"
          alt="SmartSwine Watermark"
          className="w-[380px] max-w-[70vw] max-h-[70vh] opacity-[0.12] object-contain"
        />
      </div>

      <div className="relative z-10 flex flex-col min-h-screen">
        {/* Navbar Header */}
        {!isMobile && <DesktopHeader />}

        {/* Main Body */}
        <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
          <PassCountdownBanner />

          {/* Header Welcome Bar */}
          <div className="flex flex-col items-center text-center sm:flex-row sm:justify-between sm:items-center sm:text-left gap-4 bg-zinc-50/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm">
            <div className="flex flex-col gap-1">
              <h1 className="text-xl sm:text-2xl font-bold text-zinc-900">
                {t("welcome", { farmName: userProfile?.farmName || "SmartSwine" })}
              </h1>
              <div className="hidden sm:block">
                <p className="text-xs text-zinc-500">
                  {isStaff ? t("staffMember") : t("farmOwner")} {userProfile?.isPremium && `• ${t("premium")}`}
                </p>
              </div>
            </div>
            {userProfile?.isPremium ? (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-800 border border-emerald-200/50">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse" />
                {t("premiumAccessActive")}
              </span>
            ) : (
              <div className="flex items-center gap-3">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-zinc-100 px-3 py-1 text-xs font-semibold text-zinc-600 border border-zinc-200">
                  {t("freeTier")}
                </span>
                <Link href="/dashboard/billing" className="inline-flex items-center rounded-lg bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 px-3 py-1 text-xs font-bold text-emerald-700 transition-all">
                  {t("upgrade")}
                </Link>
              </div>
            )}
          </div>

          {/* Collapsible Accordion Navigation Options (Opens One at a Time) */}
          <div className="space-y-4">
            {/* 1. HERD DATA ACCORDION */}
            <div id="accordion-herd_data" className="scroll-mt-20 bg-white/80 backdrop-blur-md border border-zinc-200/90 rounded-2xl shadow-sm overflow-hidden transition-all duration-300">
              <button
                type="button"
                onClick={() => toggleSection("herd_data")}
                className="w-full flex items-center justify-between p-5 sm:p-6 text-left hover:bg-zinc-50/80 transition-colors"
              >
                <div className="flex items-center gap-4 sm:gap-5 min-w-0">
                  <div className="h-14 w-14 rounded-2xl bg-emerald-50 text-emerald-600 border border-emerald-200/60 flex items-center justify-center flex-shrink-0 shadow-sm">
                    <HerdDataIcon className="h-7 w-7" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h2 className="text-xl sm:text-2xl font-black text-zinc-900 truncate">{t("herdData")}</h2>
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800">
                        {herdStats.total} {t("totalPigs", { count: herdStats.total }).split(":")[0] || "Pigs"}
                      </span>
                    </div>
                  </div>
                </div>
                <div className="ml-3 flex-shrink-0 text-zinc-400">
                  <ChevronDownIcon
                    className={`h-6 w-6 sm:h-7 sm:w-7 transform transition-transform duration-300 ${
                      expandedSection === "herd_data" ? "rotate-180 text-emerald-600" : ""
                    }`}
                  />
                </div>
              </button>

              {expandedSection === "herd_data" && (
                <div className="border-t border-zinc-150 p-4 sm:p-6 bg-zinc-50/40 space-y-6 animate-fadeIn">
                  {/* Rotating Herd Summary Stats Card (StatsRibbon) */}
                  <div
                    onClick={handleCardClick}
                    className="cursor-pointer bg-emerald-50/70 border border-emerald-200/80 rounded-2xl p-6 relative overflow-hidden group hover:border-emerald-500/50 hover:bg-emerald-50/90 transition-all duration-300 shadow-sm min-h-[140px] flex flex-col justify-between"
                  >
                    <div className="absolute top-0 right-0 h-32 w-32 rounded-full bg-emerald-500/5 blur-2xl pointer-events-none" />
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
                                {t("totalPigs", { count: herdStats.total })}
                              </h3>
                              <p className="text-xs sm:text-sm font-medium text-zinc-500">
                                {t("breedersPorkers", { breeders: herdStats.breeders_count, porkers: herdStats.porkers_count })}
                              </p>
                            </>
                          )}

                          {currentSlide === 1 && (
                            <>
                              <h3 className="text-lg sm:text-xl font-bold text-zinc-800 tracking-tight">
                                {t("totalBreeders", { count: herdStats.breeders_count })}
                              </h3>
                              <p className="text-[11px] sm:text-xs font-medium text-zinc-500 leading-relaxed max-w-2xl">
                                {t("piglet")}: <span className="font-semibold text-zinc-700">{herdStats.breeders_piglets}</span> | {t("starter")}: <span className="font-semibold text-zinc-700">{herdStats.breeders_starter}</span> | {t("grower")}: <span className="font-semibold text-zinc-700">{herdStats.breeders_grower}</span> | {t("boar")}: <span className="font-semibold text-zinc-700">{herdStats.boars}</span> | {t("gilt")}: <span className="font-semibold text-zinc-700">{herdStats.gilts}</span>
                                <span className="block mt-0.5">
                                  {t("pregnant")}: <span className="font-semibold text-zinc-700">{herdStats.Pregnant}</span> | {t("lactating")}: <span className="font-semibold text-zinc-700">{herdStats.Lactating}</span> | {t("sow")}: <span className="font-semibold text-zinc-700">{herdStats.sows}</span>
                                </span>
                              </p>
                            </>
                          )}

                          {currentSlide === 2 && (
                            <>
                              <h3 className="text-lg sm:text-xl font-bold text-zinc-800 tracking-tight">
                                {t("totalPorkers", { count: herdStats.porkers_count })}
                              </h3>
                              <p className="text-xs sm:text-sm font-medium text-zinc-500 leading-relaxed">
                                {t("starter")}: <span className="font-semibold text-zinc-700">{herdStats.Starter}</span> | {t("grower")}: <span className="font-semibold text-zinc-700">{herdStats.Grower}</span> | {t("finisher")}: <span className="font-semibold text-zinc-700">{herdStats.Finisher}</span>
                              </p>
                            </>
                          )}

                          {currentSlide === 3 && (
                            <>
                              <div className="flex items-center gap-2 text-emerald-700">
                                <ArchiveIcon className="h-5 w-5" />
                                <h3 className="text-lg sm:text-xl font-bold text-zinc-800 tracking-tight">
                                  {t("archivedPigs")}
                                </h3>
                              </div>
                              <p className="text-xs sm:text-sm font-medium text-zinc-500">
                                {t("viewCulled")}
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
                            e.stopPropagation();
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

                  {/* Actions Row */}
                  <div className="flex flex-wrap items-center gap-3">
                    <Link
                      href="/dashboard/herd"
                      className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-md shadow-emerald-600/20 transition"
                    >
                      <PlusIcon className="h-4 w-4" />
                      Add Pig
                    </Link>
                    <Link
                      href="/dashboard/herd"
                      className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-emerald-600 text-emerald-700 hover:bg-emerald-50 text-xs font-bold transition bg-white shadow-sm"
                    >
                      View Full Herd ({herdStats.total} Pigs)
                      <ArrowRightIcon className="h-3.5 w-3.5" />
                    </Link>
                  </div>

                  {/* Pigs List Preview (up to 10 active pigs, excluding archived) */}
                  {(() => {
                    const activePigs = allPigs
                      .filter((p) => !p.status?.toLowerCase().startsWith("archived"))
                      .slice(0, 10);

                    return (
                      <div className="space-y-3">
                        <div className="flex items-center justify-between">
                          <h4 className="text-xs font-bold text-zinc-600 uppercase tracking-wider">
                            Active Herd ({activePigs.length})
                          </h4>
                          <span className="text-[11px] text-zinc-400">Showing up to 10 pigs</span>
                        </div>

                        {activePigs.length === 0 ? (
                          <div className="p-6 bg-white rounded-xl border border-zinc-200 text-center text-xs text-zinc-500">
                            No active pigs in your herd. Click &quot;+ Add Pig&quot; to get started!
                          </div>
                        ) : (
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                            {activePigs.map((pig) => (
                              <CompactPigDropdownCard key={pig.id} pig={pig} />
                            ))}
                          </div>
                        )}
                      </div>
                    );
                  })()}
                </div>
              )}
            </div>

            {/* 2. FEED MANAGEMENT ACCORDION */}
            <div id="accordion-feed" className="scroll-mt-20 bg-white/80 backdrop-blur-md border border-zinc-200/90 rounded-2xl shadow-sm overflow-hidden transition-all duration-300">
              <button
                type="button"
                onClick={() => toggleSection("feed")}
                className="w-full flex items-center justify-between p-5 sm:p-6 text-left hover:bg-zinc-50/80 transition-colors"
              >
                <div className="flex items-center gap-4 sm:gap-5 min-w-0">
                  <div className="h-14 w-14 rounded-2xl bg-orange-50 text-orange-600 border border-orange-200/60 flex items-center justify-center flex-shrink-0 shadow-sm">
                    <FeedManagementIcon className="h-7 w-7" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h2 className="text-xl sm:text-2xl font-black text-zinc-900 truncate">{t("feedManagement")}</h2>
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-orange-100 text-orange-800">
                        {feedCount} {t("items") || "Items"}
                      </span>
                    </div>
                  </div>
                </div>
                <div className="ml-3 flex-shrink-0 text-zinc-400">
                  <ChevronDownIcon
                    className={`h-6 w-6 sm:h-7 sm:w-7 transform transition-transform duration-300 ${
                      expandedSection === "feed" ? "rotate-180 text-orange-600" : ""
                    }`}
                  />
                </div>
              </button>

              {expandedSection === "feed" && (
                <div className="border-t border-zinc-150 p-4 sm:p-6 bg-zinc-50/40 space-y-4 animate-fadeIn">
                  <div className="grid grid-cols-2 sm:grid-cols-2 md:grid-cols-4 gap-3 sm:gap-4">
                    {/* Option 1: Feed Inventory */}
                    <Link
                      href="/dashboard/feed"
                      className="bg-white border border-zinc-200/90 rounded-2xl p-5 flex flex-col items-center justify-center text-center gap-3 hover:border-orange-400 hover:bg-orange-50/40 hover:shadow-md transition-all duration-200 group"
                    >
                      <div className="h-12 w-12 rounded-xl bg-orange-50 text-orange-600 border border-orange-200/60 flex items-center justify-center group-hover:scale-105 transition-transform">
                        <InventoryIcon className="h-6 w-6" />
                      </div>
                      <h3 className="text-sm sm:text-base font-black text-zinc-900 group-hover:text-orange-700 transition-colors">
                        {t("inventory") || "Feed Inventory"}
                      </h3>
                    </Link>

                    {/* Option 2: Calculate Feed */}
                    <Link
                      href="/dashboard/feed/calculator"
                      className="bg-white border border-zinc-200/90 rounded-2xl p-5 flex flex-col items-center justify-center text-center gap-3 hover:border-orange-400 hover:bg-orange-50/40 hover:shadow-md transition-all duration-200 group"
                    >
                      <div className="h-12 w-12 rounded-xl bg-orange-50 text-orange-600 border border-orange-200/60 flex items-center justify-center group-hover:scale-105 transition-transform">
                        <CalculateIcon className="h-6 w-6" />
                      </div>
                      <h3 className="text-sm sm:text-base font-black text-zinc-900 group-hover:text-orange-700 transition-colors">
                        {t("calculator") || "Calculate Feed"}
                      </h3>
                    </Link>

                    {/* Option 3: Mix Feed */}
                    <Link
                      href="/dashboard/feed/mix"
                      className="bg-white border border-zinc-200/90 rounded-2xl p-5 flex flex-col items-center justify-center text-center gap-3 hover:border-orange-400 hover:bg-orange-50/40 hover:shadow-md transition-all duration-200 group"
                    >
                      <div className="h-12 w-12 rounded-xl bg-orange-50 text-orange-600 border border-orange-200/60 flex items-center justify-center group-hover:scale-105 transition-transform">
                        <ScienceIcon className="h-6 w-6" />
                      </div>
                      <h3 className="text-sm sm:text-base font-black text-zinc-900 group-hover:text-orange-700 transition-colors">
                        {t("mixFeed") || "Mix Feed"}
                      </h3>
                    </Link>

                    {/* Option 4: Analyze Feed */}
                    <Link
                      href="/dashboard/feed/analyze"
                      className="bg-white border border-zinc-200/90 rounded-2xl p-5 flex flex-col items-center justify-center text-center gap-3 hover:border-orange-400 hover:bg-orange-50/40 hover:shadow-md transition-all duration-200 group"
                    >
                      <div className="h-12 w-12 rounded-xl bg-orange-50 text-orange-600 border border-orange-200/60 flex items-center justify-center group-hover:scale-105 transition-transform">
                        <AnalyticsIcon className="h-6 w-6" />
                      </div>
                      <h3 className="text-sm sm:text-base font-black text-zinc-900 group-hover:text-orange-700 transition-colors">
                        {t("analyzeFeed") || "Analyze Feed"}
                      </h3>
                    </Link>
                  </div>
                </div>
              )}
            </div>

            {/* 3. WEIGH PIGS ACCORDION */}
            <div id="accordion-weight" className="scroll-mt-20 bg-white/80 backdrop-blur-md border border-zinc-200/90 rounded-2xl shadow-sm overflow-hidden transition-all duration-300">
              <button
                type="button"
                onClick={() => toggleSection("weight")}
                className="w-full flex items-center justify-between p-5 sm:p-6 text-left hover:bg-zinc-50/80 transition-colors"
              >
                <div className="flex items-center gap-4 sm:gap-5 min-w-0">
                  <div className="h-14 w-14 rounded-2xl bg-slate-100 text-slate-700 border border-slate-300 flex items-center justify-center flex-shrink-0 shadow-sm">
                    <WeightCheckerIcon className="h-7 w-7" />
                  </div>
                  <div className="min-w-0">
                    <h2 className="text-xl sm:text-2xl font-black text-zinc-900 truncate">{t("weightChecker") || "Weigh Pigs"}</h2>
                  </div>
                </div>
                <div className="ml-3 flex-shrink-0 text-zinc-400">
                  <ChevronDownIcon
                    className={`h-6 w-6 sm:h-7 sm:w-7 transform transition-transform duration-300 ${
                      expandedSection === "weight" ? "rotate-180 text-slate-700" : ""
                    }`}
                  />
                </div>
              </button>

              {expandedSection === "weight" && (
                <div className="border-t border-zinc-150 p-4 sm:p-6 bg-zinc-50/40 animate-fadeIn">
                  <InlineWeightSection
                    pigs={allPigs}
                    initialSubOption={weightSubOption}
                    initialPigTag={weightPigTag}
                  />
                </div>
              )}
            </div>

            {/* 4. DISEASE FINDER ACCORDION */}
            <div id="accordion-symptoms" className="scroll-mt-20 bg-white/80 backdrop-blur-md border border-zinc-200/90 rounded-2xl shadow-sm overflow-hidden transition-all duration-300">
              <button
                type="button"
                onClick={() => toggleSection("symptoms")}
                className="w-full flex items-center justify-between p-5 sm:p-6 text-left hover:bg-zinc-50/80 transition-colors"
              >
                <div className="flex items-center gap-4 sm:gap-5 min-w-0">
                  <div className="h-14 w-14 rounded-2xl bg-indigo-50 text-indigo-600 border border-indigo-200/60 flex items-center justify-center flex-shrink-0 shadow-sm">
                    <SymptomsAnalyzerIcon className="h-7 w-7" />
                  </div>
                  <div className="min-w-0">
                    <h2 className="text-xl sm:text-2xl font-black text-zinc-900 truncate">{t("symptomsAnalyzer") || "Disease Finder"}</h2>
                  </div>
                </div>
                <div className="ml-3 flex-shrink-0 text-zinc-400">
                  <ChevronDownIcon
                    className={`h-6 w-6 sm:h-7 sm:w-7 transform transition-transform duration-300 ${
                      expandedSection === "symptoms" ? "rotate-180 text-indigo-600" : ""
                    }`}
                  />
                </div>
              </button>

              {expandedSection === "symptoms" && (
                <div className="border-t border-zinc-150 p-4 sm:p-6 bg-zinc-50/40 animate-fadeIn">
                  <InlineDiseaseFinderSection />
                </div>
              )}
            </div>

            {/* 5. HERD ACTIVITIES ACCORDION */}
            <div id="accordion-activities" className="scroll-mt-20 bg-white/80 backdrop-blur-md border border-zinc-200/90 rounded-2xl shadow-sm overflow-hidden transition-all duration-300">
              <button
                type="button"
                onClick={() => toggleSection("activities")}
                className="w-full flex items-center justify-between p-5 sm:p-6 text-left hover:bg-zinc-50/80 transition-colors"
              >
                <div className="flex items-center gap-4 sm:gap-5 min-w-0">
                  <div className="h-14 w-14 rounded-2xl bg-cyan-50 text-cyan-600 border border-cyan-200/60 flex items-center justify-center flex-shrink-0 shadow-sm">
                    <HerdActivitiesIcon className="h-7 w-7" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h2 className="text-xl sm:text-2xl font-black text-zinc-900 truncate">{t("herdActivities")}</h2>
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-cyan-100 text-cyan-800">
                        {groupedTasks.length} {t("pending") || "Pending"}
                      </span>
                    </div>
                  </div>
                </div>
                <div className="ml-3 flex-shrink-0 text-zinc-400">
                  <ChevronDownIcon
                    className={`h-6 w-6 sm:h-7 sm:w-7 transform transition-transform duration-300 ${
                      expandedSection === "activities" ? "rotate-180 text-cyan-600" : ""
                    }`}
                  />
                </div>
              </button>

              {expandedSection === "activities" && (
                <div className="border-t border-zinc-150 p-4 sm:p-6 bg-zinc-50/40 space-y-6 animate-fadeIn">
                  {/* 1-Column Herd Activities with 8 items per page */}
                  <div>
                    <div className="flex items-center justify-between mb-3">
                      <h3 className="text-xs font-bold text-zinc-600 uppercase tracking-wider">
                        {t("logActivitiesEvents") || "Log Activities & Events"}
                      </h3>
                      <Link
                        href="/dashboard/activities"
                        className="text-xs font-bold text-cyan-700 hover:text-cyan-800 inline-flex items-center gap-1"
                      >
                        {t("openActivityCalendar") || "Open Activity Calendar →"}
                      </Link>
                    </div>

                    <div className="space-y-2">
                      {(activityPage === 0 ? herdActivitiesList.slice(0, 8) : herdActivitiesList.slice(8)).map((activity, idx) => {
                        const IconComponent = activity.icon;
                        return (
                          <Link
                            key={`act-item-${idx}`}
                            href={`/dashboard/activities?activity=${encodeURIComponent(activity.name)}`}
                            className="p-3 rounded-xl bg-white border border-cyan-100 hover:border-cyan-400/80 hover:shadow-sm transition flex items-center justify-between group"
                          >
                            <div className="flex items-center gap-3">
                              <div className="h-10 w-10 rounded-xl bg-cyan-50 text-cyan-700 group-hover:bg-cyan-100 transition flex items-center justify-center flex-shrink-0">
                                <IconComponent className="h-5 w-5" />
                              </div>
                              <span className="text-sm font-bold text-zinc-800 group-hover:text-cyan-800 transition">
                                {getActivityTranslation(activity.name)}
                              </span>
                            </div>
                            <span className="text-cyan-500 group-hover:translate-x-0.5 transition-transform">
                              <ArrowRightIcon className="h-4 w-4" />
                            </span>
                          </Link>
                        );
                      })}

                      {activityPage === 0 ? (
                        <div className="flex justify-end pt-2">
                          <button
                            type="button"
                            onClick={() => setActivityPage(1)}
                            className="text-xs font-bold text-cyan-700 hover:text-cyan-800 inline-flex items-center gap-1.5 py-1 px-3 rounded-lg hover:bg-cyan-50 transition"
                          >
                            {t("nextPage") || "Next Page →"}
                          </button>
                        </div>
                      ) : (
                        <div className="flex justify-start pt-2">
                          <button
                            type="button"
                            onClick={() => setActivityPage(0)}
                            className="text-xs font-bold text-cyan-700 hover:text-cyan-800 inline-flex items-center gap-1.5 py-1 px-3 rounded-lg hover:bg-cyan-50 transition"
                          >
                            {t("prevPage") || "← Previous Page"}
                          </button>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Upcoming Pending Activities */}
                  {groupedTasks.length > 0 && (
                    <div className="space-y-3 pt-2">
                      <h3 className="text-xs font-bold text-zinc-600 uppercase tracking-wider">
                        {t("upcomingActivities") || "Upcoming Pending Activities"} ({groupedTasks.length})
                      </h3>
                      <div className="space-y-2">
                        {groupedTasks.slice(0, 4).map((taskGroup, idx) => (
                          <div
                            key={`act-${idx}`}
                            onClick={() => {
                              setTasksToEdit(taskGroup.originalTasks);
                              setIsCompletionModalOpen(true);
                            }}
                            className={`p-3.5 rounded-xl border transition cursor-pointer flex items-center justify-between ${
                              taskGroup.isOverdue ? "bg-red-50 border-red-200" : "bg-white border-zinc-200 hover:bg-zinc-50"
                            }`}
                          >
                            <div>
                              <p className="text-xs font-bold text-zinc-900">{getActivityTranslation(taskGroup.activity)}</p>
                              <p className="text-[11px] text-zinc-500">{t("target") || "Target"}: {taskGroup.target}</p>
                            </div>
                            <span className={`text-[10px] font-bold px-2.5 py-1 rounded-full ${
                              taskGroup.isOverdue ? "bg-red-100 text-red-800" : "bg-zinc-100 text-zinc-700"
                            }`}>
                              {taskGroup.date}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* 4. FINANCIALS ACCORDION */}
            <div id="accordion-financials" className="scroll-mt-20 bg-white/80 backdrop-blur-md border border-zinc-200/90 rounded-2xl shadow-sm overflow-hidden transition-all duration-300">
              <button
                type="button"
                onClick={() => toggleSection("financials")}
                className="w-full flex items-center justify-between p-5 sm:p-6 text-left hover:bg-zinc-50/80 transition-colors"
              >
                <div className="flex items-center gap-4 sm:gap-5 min-w-0">
                  <div className="h-14 w-14 rounded-2xl bg-teal-50 text-teal-600 border border-teal-200/60 flex items-center justify-center flex-shrink-0 shadow-sm">
                    <FinancialsIcon className="h-7 w-7" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h2 className="text-xl sm:text-2xl font-black text-zinc-900 truncate">{t("financials")}</h2>
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-teal-100 text-teal-800">
                        {t("cashflowRecords") || "Cashflow & Records"}
                      </span>
                    </div>
                  </div>
                </div>
                <div className="ml-3 flex-shrink-0 text-zinc-400">
                  <ChevronDownIcon
                    className={`h-6 w-6 sm:h-7 sm:w-7 transform transition-transform duration-300 ${
                      expandedSection === "financials" ? "rotate-180 text-teal-600" : ""
                    }`}
                  />
                </div>
              </button>

              {expandedSection === "financials" && (
                <div className="border-t border-zinc-150 p-4 sm:p-6 bg-zinc-50/40 space-y-5 animate-fadeIn">
                  {/* Add Entry Button */}
                  <Link
                    href="/dashboard/financials?add=true"
                    className="w-full py-3 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-xs font-bold shadow-sm transition flex items-center justify-center gap-2"
                  >
                    <PlusIcon className="h-4 w-4" />
                    {t("addEntry") || "Add Entry"}
                  </Link>

                  {/* Financial Summary Card */}
                  <div className="bg-white border border-teal-100 rounded-2xl p-5 shadow-sm space-y-4">
                    <h3 className="text-sm font-bold text-zinc-900">
                      {t("financialSummary") || "Financial Summary"}
                    </h3>
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                      <div className="p-3.5 rounded-xl bg-emerald-50/70 border border-emerald-100">
                        <p className="text-[11px] font-semibold text-emerald-700 uppercase tracking-wider">{t("totalIncome") || "Total Income"}</p>
                        <p className="text-lg sm:text-xl font-bold text-emerald-800 mt-0.5">
                          {currencySymbol}{totalIncome.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </p>
                      </div>
                      <div className="p-3.5 rounded-xl bg-rose-50/70 border border-rose-100">
                        <p className="text-[11px] font-semibold text-rose-700 uppercase tracking-wider">{t("totalExpenses") || "Total Expenses"}</p>
                        <p className="text-lg sm:text-xl font-bold text-rose-800 mt-0.5">
                          {currencySymbol}{totalExpenses.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </p>
                      </div>
                      <div className={`p-3.5 rounded-xl border ${netProfit >= 0 ? "bg-teal-50/70 border-teal-100" : "bg-red-50/70 border-red-100"}`}>
                        <p className={`text-[11px] font-semibold uppercase tracking-wider ${netProfit >= 0 ? "text-teal-700" : "text-red-700"}`}>{t("netProfit") || "Net Profit"}</p>
                        <p className={`text-lg sm:text-xl font-bold mt-0.5 ${netProfit >= 0 ? "text-teal-800" : "text-red-800"}`}>
                          {currencySymbol}{netProfit.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between">
                    <h3 className="text-xs font-bold text-zinc-600 uppercase tracking-wider">
                      {t("recentTransactions")}
                    </h3>
                    <Link
                      href="/dashboard/financials"
                      className="text-xs font-bold text-teal-700 hover:text-teal-800 inline-flex items-center gap-1"
                    >
                      {t("viewFullAccounting") || "View Full Accounting →"}
                    </Link>
                  </div>

                  {recentFinancials.length === 0 ? (
                    <div className="p-5 bg-white rounded-xl border border-zinc-200 text-center text-xs text-zinc-500">
                      {t("noTransactions")}
                    </div>
                  ) : (
                    <div className="bg-white rounded-xl border border-teal-100 divide-y divide-zinc-100 overflow-hidden shadow-sm">
                      {recentFinancials.slice(0, 10).map((record) => (
                        <div key={record.id} className="p-3.5 flex justify-between items-center text-xs">
                          <div>
                            <p className="font-semibold text-zinc-800">{record.category}</p>
                            <p className="text-[11px] text-zinc-500">{record.date} • {record.description}</p>
                          </div>
                          <span className={`font-bold ${record.type === "Income" ? "text-emerald-600" : "text-rose-600"}`}>
                            {record.type === "Income" ? "+" : "-"}{currencySymbol}{Number(record.amount || 0).toFixed(2)}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}

                  <Link
                    href="/dashboard/financials"
                    className="w-full py-2.5 rounded-xl border border-teal-600 text-teal-700 hover:bg-teal-50 text-xs font-bold transition flex items-center justify-center gap-2"
                  >
                    {t("viewAll") || "View All"}
                    <ArrowRightIcon className="h-3.5 w-3.5" />
                  </Link>
                </div>
              )}
            </div>

            {/* 5. HUMAN RESOURCES ACCORDION */}
            <div id="accordion-hr" className="scroll-mt-20 bg-white/80 backdrop-blur-md border border-zinc-200/90 rounded-2xl shadow-sm overflow-hidden transition-all duration-300">
              <button
                type="button"
                onClick={() => toggleSection("hr")}
                className="w-full flex items-center justify-between p-5 sm:p-6 text-left hover:bg-zinc-50/80 transition-colors"
              >
                <div className="flex items-center gap-4 sm:gap-5 min-w-0">
                  <div className="h-14 w-14 rounded-2xl bg-purple-50 text-purple-700 border border-purple-200/60 flex items-center justify-center flex-shrink-0 shadow-sm">
                    <HumanResourcesIcon className="h-7 w-7" />
                  </div>
                  <div className="min-w-0">
                    <h2 className="text-xl sm:text-2xl font-black text-zinc-900 truncate">{t("hr")}</h2>
                  </div>
                </div>
                <div className="ml-3 flex-shrink-0 text-zinc-400">
                  <ChevronDownIcon
                    className={`h-6 w-6 sm:h-7 sm:w-7 transform transition-transform duration-300 ${
                      expandedSection === "hr" ? "rotate-180 text-purple-700" : ""
                    }`}
                  />
                </div>
              </button>

              {expandedSection === "hr" && (
                <div className="border-t border-zinc-150 p-4 sm:p-6 bg-zinc-50/40 animate-fadeIn">
                  <InlineHRSection
                    staff={staff}
                    currencySymbol={currencySymbol}
                    activeFarmUid={activeFarmUid || ""}
                    initialShowAdd={shouldOpenAddStaff}
                  />
                </div>
              )}
            </div>

            {/* 6. MARKET HUB ACCORDION */}
            <div id="accordion-hub" className="scroll-mt-20 bg-white/80 backdrop-blur-md border border-zinc-200/90 rounded-2xl shadow-sm overflow-hidden transition-all duration-300">
              <button
                type="button"
                onClick={() => toggleSection("hub")}
                className="w-full flex items-center justify-between p-5 sm:p-6 text-left hover:bg-zinc-50/80 transition-colors"
              >
                <div className="flex items-center gap-4 sm:gap-5 min-w-0">
                  <div className="h-14 w-14 rounded-2xl bg-pink-50 text-pink-700 border border-pink-200/60 flex items-center justify-center flex-shrink-0 shadow-sm">
                    <LocalHubIcon className="h-7 w-7" />
                  </div>
                  <div className="min-w-0">
                    <h2 className="text-xl sm:text-2xl font-black text-zinc-900 truncate">{t("marketHub") || t("localHub") || "Market Hub"}</h2>
                  </div>
                </div>
                <div className="ml-3 flex-shrink-0 text-zinc-400">
                  <ChevronDownIcon
                    className={`h-6 w-6 sm:h-7 sm:w-7 transform transition-transform duration-300 ${
                      expandedSection === "hub" ? "rotate-180 text-pink-700" : ""
                    }`}
                  />
                </div>
              </button>

              {expandedSection === "hub" && (
                <div className="border-t border-zinc-150 p-4 sm:p-6 bg-zinc-50/40 animate-fadeIn">
                  <InlineMarketSection />
                </div>
              )}
            </div>

            {/* 9. PIG FARMING TIPS ACCORDION */}
            <div id="accordion-training" className="scroll-mt-20 bg-white/80 backdrop-blur-md border border-zinc-200/90 rounded-2xl shadow-sm overflow-hidden transition-all duration-300">
              <button
                type="button"
                onClick={() => toggleSection("training")}
                className="w-full flex items-center justify-between p-5 sm:p-6 text-left hover:bg-zinc-50/80 transition-colors"
              >
                <div className="flex items-center gap-4 sm:gap-5 min-w-0">
                  <div className="h-14 w-14 rounded-2xl bg-[#EFEBE9] text-[#5D4037] border border-[#5D4037]/20 flex items-center justify-center flex-shrink-0 shadow-sm">
                    <TrainingTipsIcon className="h-7 w-7" />
                  </div>
                  <div className="min-w-0">
                    <h2 className="text-xl sm:text-2xl font-black text-zinc-900 truncate">{t("trainingTips") || "Pig Farming Tips"}</h2>
                  </div>
                </div>
                <div className="ml-3 flex-shrink-0 text-zinc-400">
                  <ChevronDownIcon
                    className={`h-6 w-6 sm:h-7 sm:w-7 transform transition-transform duration-300 ${
                      expandedSection === "training" ? "rotate-180 text-[#5D4037]" : ""
                    }`}
                  />
                </div>
              </button>

              {expandedSection === "training" && (
                <div className="border-t border-zinc-150 p-4 sm:p-6 bg-zinc-50/40 animate-fadeIn">
                  <InlineTipsSection />
                </div>
              )}
            </div>

            {/* 10. ADMIN PANEL ACCORDION (If Admin) */}
            {userProfile?.isAdmin && (
              <div id="accordion-admin" className="scroll-mt-20 bg-emerald-50/30 backdrop-blur-md border border-emerald-200 rounded-2xl shadow-sm overflow-hidden transition-all duration-300">
                <button
                  type="button"
                  onClick={() => toggleSection("admin")}
                  className="w-full flex items-center justify-between p-5 sm:p-6 text-left hover:bg-emerald-50/60 transition-colors"
                >
                  <div className="flex items-center gap-4 sm:gap-5 min-w-0">
                    <div className="h-14 w-14 rounded-2xl bg-emerald-100 text-emerald-700 border border-emerald-300/60 flex items-center justify-center flex-shrink-0 shadow-sm">
                      <LocalHubIcon className="h-7 w-7" />
                    </div>
                    <div className="min-w-0">
                      <h2 className="text-xl sm:text-2xl font-black text-emerald-950 truncate">{t("adminPanel")}</h2>
                    </div>
                  </div>
                  <div className="ml-3 flex-shrink-0 text-emerald-600">
                    <ChevronDownIcon
                      className={`h-6 w-6 sm:h-7 sm:w-7 transform transition-transform duration-300 ${
                        expandedSection === "admin" ? "rotate-180" : ""
                      }`}
                    />
                  </div>
                </button>

                {expandedSection === "admin" && (
                  <div className="border-t border-emerald-200/60 p-4 sm:p-6 bg-emerald-50/50 space-y-4 animate-fadeIn">
                    <p className="text-xs text-emerald-900 leading-relaxed">
                      {t("adminPanelDesc") || "Manage global ingredients, verify provider suggestions, and update training video tutorials."}
                    </p>
                    <Link
                      href="/admin"
                      className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold shadow-sm transition"
                    >
                      {t("adminPanel")}
                      <ArrowRightIcon className="h-3.5 w-3.5" />
                    </Link>
                  </div>
                )}
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
              &ldquo;{dailyQuote}&rdquo;
            </p>
          </div>

          {/* Sponsored Ad Banner for Free Users */}
          <NativeAdBanner />

          {/* Copyright Notice */}
          <div className="pt-2 pb-6 text-center">
            <p className="text-xs font-medium text-zinc-400 select-none">
              © SmartSwine 2026
            </p>
          </div>
        </main>
      </div>

      {/* Notification Drawer */}
      <NotificationDrawer
        isOpen={isNotificationDrawerOpen}
        onClose={() => setIsNotificationDrawerOpen(false)}
        groupedTasks={groupedTasks}
        weightAlerts={weightAlerts}
        stockAlerts={stockAlerts}
        onSelectTaskGroup={(tasks) => {
          setTasksToEdit(tasks);
          setIsCompletionModalOpen(true);
          setIsNotificationDrawerOpen(false);
        }}
        onSelectWeightAlert={(pig) => {
          setExpandedSection("weight");
          setWeightSubOption("tape");
          setWeightPigTag(pig.tagNumber || pig.id);
          setIsNotificationDrawerOpen(false);
          scrollToElementId("suboption-tape", 250);
        }}
      />

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

export default function DashboardPage() {
  return (
    <Suspense
      fallback={
        <div className="flex h-screen items-center justify-center bg-white text-zinc-900">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
        </div>
      }
    >
      <DashboardContent />
    </Suspense>
  );
}

