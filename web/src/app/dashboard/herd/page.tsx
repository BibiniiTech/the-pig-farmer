"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot, doc, setDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import UserProfileDropdown from "@/components/UserProfileDropdown";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import HerdReport from "@/components/reports/HerdReport";
import { evaluatePerformance, calculateAgeMonths, calculateAgeDays, formatSwineAge } from "@/lib/swineGrowthDatabase";
import { ExportPdfIcon, HerdDataIcon } from "@/components/icons/DashboardIcons";
import { useTranslations } from "next-intl";
import { Pig } from "@/lib/types";
import { TierLimiter } from "@/lib/tierLimiter";
import NativeAdBanner from "@/components/ads/NativeAdBanner";
import RewardedPassModal from "@/components/ads/RewardedPassModal";

const STANDARD_BREEDS = [
  "Large White",
  "Landrace",
  "Duroc",
  "Hampshire",
  "Berkshire",
  "Large White x Landrace (F1)",
  "Duroc x (Large White x Landrace)",
  "Duroc x Landrace",
  "Duroc x Large White",
  "Hampshire x Landrace",
  "Hampshire x Large White",
  "Local / Heritage",
  "Other"
];

const ALL_STATUSES = [
  "Piglet",
  "Starter",
  "Grower",
  "Finisher",
  "Boar",
  "Gilt",
  "Sow",
  "Barrow",
  "Pregnant",
  "Lactating"
];

const PURPOSES = ["Breeder", "Porker"];

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

export default function HerdPage() {
  const t = useTranslations("Herd");
  const td = useTranslations("Dashboard");
  const tHr = useTranslations("HR");
  const { user, userProfile, activeFarmUid, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();
  const isPremium = Boolean(userProfile?.isPremium || userProfile?.isAdmin);

  const [pigs, setPigs] = useState<Pig[]>([]);
  const [archivedPigs, setArchivedPigs] = useState<Pig[]>([]);
  const [viewingArchived, setViewingArchived] = useState(false);
  const [dataLoading, setDataLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showRewardedPassModal, setShowRewardedPassModal] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [purposeFilter, setPurposeFilter] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string | null>(null);

  const [herdStats, setHerdStats] = useState({
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

  // Form states
  const [isMultiple, setIsMultiple] = useState(false);
  const [birthDate, setBirthDate] = useState(new Date().toISOString().split("T")[0]);
  const [breed, setBreed] = useState("Large White");
  const [selectedBreed, setSelectedBreed] = useState("Large White");
  const [customBreed, setCustomBreed] = useState("");
  const [purpose, setPurpose] = useState("Porker");
  const [sowTag, setSowTag] = useState("");
  const [boarTag, setBoarTag] = useState("");
  const [source, setSource] = useState("Born on farm");
  const [notes, setNotes] = useState("");
  
  // Single mode inputs
  const [tagNumber, setTagNumber] = useState("");
  const [gender, setGender] = useState("Male");
  const [weight, setWeight] = useState(0);
  const [location, setLocation] = useState("");
  const [purchasePrice, setPurchasePrice] = useState(0);

  interface MultiPigEntry {
    tagNumber: string;
    weight: number;
    location: string;
  }

  // Batch mode inputs
  const [maleQty, setMaleQty] = useState(0);
  const [femaleQty, setFemaleQty] = useState(0);
  const [malePigs, setMalePigs] = useState<MultiPigEntry[]>([]);
  const [femalePigs, setFemalePigs] = useState<MultiPigEntry[]>([]);

  const handleMaleQtyChange = (qty: number) => {
    setMaleQty(qty);
    setMalePigs((prev) => {
      const next = [...prev];
      if (qty > next.length) {
        for (let i = next.length; i < qty; i++) {
          next.push({ tagNumber: "", weight: 0, location: "" });
        }
      } else if (qty < next.length) {
        next.splice(qty);
      }
      return next;
    });
  };

  const handleFemaleQtyChange = (qty: number) => {
    setFemaleQty(qty);
    setFemalePigs((prev) => {
      const next = [...prev];
      if (qty > next.length) {
        for (let i = next.length; i < qty; i++) {
          next.push({ tagNumber: "", weight: 0, location: "" });
        }
      } else if (qty < next.length) {
        next.splice(qty);
      }
      return next;
    });
  };

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (typeof window !== "undefined") {
      const params = new URLSearchParams(window.location.search);
      if (params.get("action") === "add" || params.get("add") === "true") {
        setShowAddModal(true);
      }
    }
  }, []);

  useEffect(() => {
    if (!activeFarmUid) return;

    setDataLoading(true);
    const pigsQuery = collection(db, "users", activeFarmUid, "pigs");
    const unsubscribe = onSnapshot(pigsQuery, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Pig));
      setPigs(list.sort((a, b) => a.tagNumber.localeCompare(b.tagNumber)));
      
      const breeders = list.filter(p => p.purpose === "Breeder");
      const porkers = list.filter(p => p.purpose === "Porker");

      setHerdStats({
        total: list.length,
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

      setDataLoading(false);
    }, (error) => {
      console.error("Error listening to pigs:", error);
      setDataLoading(false);
    });

    const archivedQuery = collection(db, "users", activeFarmUid, "archived_pigs");
    const unsubscribeArchived = onSnapshot(archivedQuery, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Pig));
      setArchivedPigs(list.sort((a, b) => a.tagNumber.localeCompare(b.tagNumber)));
    }, (error) => {
      console.error("Error listening to archived pigs:", error);
    });

    return () => {
      unsubscribe();
      unsubscribeArchived();
    };
  }, [activeFarmUid]);

  const handleAddPig = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid) return;

    const isPremium = userProfile?.isPremium || userProfile?.isAdmin;
    const additionalPigs = isMultiple ? malePigs.filter(p => p.tagNumber.trim() !== "").length + femalePigs.filter(p => p.tagNumber.trim() !== "").length : 1;

    if (!isPremium && herdStats.total + additionalPigs > TierLimiter.FREE_MAX_PIGS) {
      alert(t("limitReached"));
      router.push("/dashboard/billing");
      setShowAddModal(false);
      return;
    }

    try {
      const pigsCollection = collection(db, "users", activeFarmUid, "pigs");

      const todayStr = new Date().toISOString().split("T")[0];

      if (!isMultiple) {
        // Single Add
        if (!tagNumber.trim()) return;
        const newRef = doc(pigsCollection);
        const newPig: Pig = {
          id: newRef.id,
          tagNumber: tagNumber.trim(),
          birthDate,
          breed,
          gender,
          weight,
          lastWeightDate: weight > 0 ? todayStr : "",
          purpose,
          sowTag,
          boarTag,
          location,
          source,
          status: purpose === "Breeder" ? (gender === "Male" ? "Boar" : "Sow") : "Piglet",
          notes
        };
        await setDoc(newRef, newPig, { merge: true });

        if (weight > 0) {
          const healthRef = doc(collection(newRef, "health_records"));
          await setDoc(healthRef, {
            id: healthRef.id,
            date: todayStr,
            type: "Weight Check",
            description: "Initial weight record.",
          });
        }

        if (source === "Brought to farm" && purchasePrice > 0) {
          const finRef = doc(collection(db, "users", activeFarmUid, "financials"));
          await setDoc(finRef, {
            id: finRef.id,
            date: todayStr,
            type: "Expense",
            category: "Livestock Purchase",
            amount: purchasePrice,
            description: `Purchase of pig with Tag: ${tagNumber.trim()}`,
            pigId: newRef.id
          });
        }
      } else {
        // Batch Add
        const validMales = malePigs.filter((p) => p.tagNumber.trim() !== "");
        const validFemales = femalePigs.filter((p) => p.tagNumber.trim() !== "");

        for (const entry of validMales) {
          const newRef = doc(pigsCollection);
          const newPig: Pig = {
            id: newRef.id,
            tagNumber: entry.tagNumber.trim(),
            birthDate,
            breed,
            gender: "Male",
            weight: entry.weight,
            lastWeightDate: entry.weight > 0 ? todayStr : "",
            purpose,
            sowTag,
            boarTag,
            location: entry.location,
            source,
            status: purpose === "Breeder" ? "Boar" : "Piglet",
            notes: notes || "Batch addition (Male)"
          };
          await setDoc(newRef, newPig, { merge: true });

          if (entry.weight > 0) {
            const healthRef = doc(collection(newRef, "health_records"));
            await setDoc(healthRef, {
              id: healthRef.id,
              date: todayStr,
              type: "Weight Check",
              description: "Initial weight record.",
            });
          }
        }

        for (const entry of validFemales) {
          const newRef = doc(pigsCollection);
          const newPig: Pig = {
            id: newRef.id,
            tagNumber: entry.tagNumber.trim(),
            birthDate,
            breed,
            gender: "Female",
            weight: entry.weight,
            lastWeightDate: entry.weight > 0 ? todayStr : "",
            purpose,
            sowTag,
            boarTag,
            location: entry.location,
            source,
            status: purpose === "Breeder" ? "Sow" : "Piglet",
            notes: notes || "Batch addition (Female)"
          };
          await setDoc(newRef, newPig, { merge: true });

          if (entry.weight > 0) {
            const healthRef = doc(collection(newRef, "health_records"));
            await setDoc(healthRef, {
              id: healthRef.id,
              date: todayStr,
              type: "Weight Check",
              description: "Initial weight record.",
            });
          }
        }

        if (source === "Brought to farm" && purchasePrice > 0) {
          const finRef = doc(collection(db, "users", activeFarmUid, "financials"));
          await setDoc(finRef, {
            id: finRef.id,
            date: new Date().toISOString().split("T")[0],
            type: "Expense",
            category: "Livestock Purchase",
            amount: purchasePrice,
            description: `Purchase of batch of pigs (Qty: ${validMales.length + validFemales.length})`,
          });
        }
      }

      // Reset Form
      setTagNumber("");
      setNotes("");
      setWeight(0);
      setLocation("");
      setPurchasePrice(0);
      setSource("Born on farm");
      setMaleQty(0);
      setFemaleQty(0);
      setMalePigs([]);
      setFemalePigs([]);
      setBreed("Large White");
      setSelectedBreed("Large White");
      setCustomBreed("");
      setShowAddModal(false);
    } catch (err) {
      console.error("Failed to add pig:", err);
    }
  };

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-white text-zinc-900">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen bg-white text-zinc-900 flex flex-col font-sans overflow-x-hidden">
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

      <div className="relative z-10 flex flex-col min-h-screen print:hidden">
        {!isMobile && <DesktopHeader />}

        <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8 space-y-6">
          {/* Top Actions & Heading Bar */}
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => router.push("/dashboard")}
                className="p-2 hover:bg-zinc-100 rounded-xl transition-colors text-zinc-600 border border-zinc-200 bg-white shadow-xs flex items-center justify-center shrink-0"
                aria-label="Back to dashboard"
                title="Back to dashboard"
              >
                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
                </svg>
              </button>
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-xl bg-emerald-50 border border-emerald-200/60 flex items-center justify-center flex-shrink-0">
                  <HerdDataIcon className="h-5 w-5 text-emerald-600" />
                </div>
                <div>
                  <h1 className="text-lg sm:text-2xl font-black text-emerald-600">
                    {t("title") || "Herd Data"}
                  </h1>
                  <p className="text-[11px] sm:text-xs text-zinc-500">{t("desc") || "Manage individual pigs, track health, and view records"}</p>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-2 flex-wrap">
              <button
                onClick={() => setViewingArchived(false)}
                className={`px-3 sm:px-3.5 py-1.5 sm:py-2 rounded-xl text-xs font-bold transition shadow-sm ${
                  !viewingArchived
                    ? "bg-emerald-600 text-white"
                    : "bg-zinc-100 text-zinc-600 hover:bg-zinc-200"
                }`}
              >
                Active Herd ({pigs.length})
              </button>
              <button
                onClick={() => setViewingArchived(true)}
                className={`px-3 sm:px-3.5 py-1.5 sm:py-2 rounded-xl text-xs font-bold transition flex items-center gap-1.5 shadow-sm ${
                  viewingArchived
                    ? "bg-emerald-600 text-white"
                    : "bg-zinc-100 text-zinc-600 hover:bg-zinc-200"
                }`}
              >
                <ArchiveIcon className="h-4 w-4" />
                Archived ({archivedPigs.length})
              </button>
              <button
                onClick={() => {
                  const isPremium = userProfile?.isPremium || userProfile?.isAdmin;
                  if (!isPremium && herdStats.total >= TierLimiter.FREE_MAX_PIGS) {
                    alert(t("limitReached"));
                    router.push("/dashboard/billing");
                  } else {
                    setShowAddModal(true);
                  }
                }}
                className="rounded-xl bg-emerald-600 hover:bg-emerald-700 px-4 sm:px-6 py-2 sm:py-2.5 text-xs font-bold text-white shadow-lg shadow-emerald-600/20 transition-all active:scale-95"
              >
                {t("addPigs")}
              </button>
            </div>
          </div>

          {/* Herd List Grid Card with Compact Search */}
          <div className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-5">
            <div className="flex items-center justify-between gap-4 flex-wrap">
              {/* Compact Search Bar with reduced size */}
              <div className="relative flex-1 min-w-[220px] max-w-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-zinc-400">
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </div>
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Tag / Location / Breed"
                  className="w-full pl-9 pr-4 py-1.5 bg-white border border-zinc-200 rounded-xl text-xs sm:text-sm text-zinc-900 placeholder:text-zinc-400 focus:outline-none focus:ring-1 focus:ring-emerald-500 focus:border-emerald-500 shadow-sm"
                />
              </div>

              <button
                onClick={() => {
                  if (!isPremium) {
                    setShowRewardedPassModal(true);
                    return;
                  }
                  window.print();
                }}
                className={`rounded-lg border px-3 py-1.5 text-xs font-semibold transition shadow-sm flex items-center gap-1.5 ${
                  isPremium
                    ? "border-zinc-200 bg-zinc-50/50 text-zinc-650 hover:bg-zinc-100"
                    : "border-amber-200 bg-amber-50 text-amber-700 hover:bg-amber-100"
                }`}
              >
                <ExportPdfIcon className="h-3.5 w-3.5 opacity-80" />
                <span>{isPremium ? t("exportPdf") : t("exportPdfPremium")}</span>
              </button>
            </div>

            {/* Filter Chips Bar (Matching Android HerdDataScreen) */}
            <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-1">
              <button
                type="button"
                onClick={() => {
                  setPurposeFilter(null);
                  setStatusFilter(null);
                }}
                className={`px-3 py-1.5 rounded-full text-xs font-bold whitespace-nowrap transition-all ${
                  purposeFilter === null && statusFilter === null
                    ? "bg-emerald-600 text-white shadow-xs"
                    : "bg-zinc-100 text-zinc-600 hover:bg-zinc-200"
                }`}
              >
                All
              </button>

              {PURPOSES.map((p) => {
                const isSelected = purposeFilter === p;
                return (
                  <button
                    key={p}
                    type="button"
                    onClick={() => setPurposeFilter(isSelected ? null : p)}
                    className={`px-3 py-1.5 rounded-full text-xs font-bold whitespace-nowrap transition-all ${
                      isSelected
                        ? "bg-emerald-600 text-white shadow-xs"
                        : "bg-zinc-100 text-zinc-600 hover:bg-zinc-200"
                    }`}
                  >
                    {p}
                  </button>
                );
              })}

              <div className="h-4 w-px bg-zinc-200 shrink-0 mx-1" />

              {ALL_STATUSES.map((st) => {
                const isSelected = statusFilter === st;
                return (
                  <button
                    key={st}
                    type="button"
                    onClick={() => setStatusFilter(isSelected ? null : st)}
                    className={`px-3 py-1.5 rounded-full text-xs font-bold whitespace-nowrap transition-all ${
                      isSelected
                        ? "bg-emerald-600 text-white shadow-xs"
                        : "bg-zinc-100 text-zinc-600 hover:bg-zinc-200"
                    }`}
                  >
                    {st}
                  </button>
                );
              })}
            </div>

            {dataLoading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                {[...Array(6)].map((_, i) => (
                  <div key={i} className="h-28 bg-zinc-100 animate-pulse rounded-xl" />
                ))}
              </div>
            ) : (() => {
              const sourcePigs = viewingArchived ? archivedPigs : pigs;
              const filteredPigs = sourcePigs.filter((pig) => {
                if (purposeFilter && pig.purpose !== purposeFilter) return false;
                if (statusFilter && pig.status !== statusFilter) return false;
                if (!searchQuery.trim()) return true;
                const q = searchQuery.toLowerCase().trim();
                return (
                  (pig.tagNumber && pig.tagNumber.toLowerCase().includes(q)) ||
                  (pig.location && pig.location.toLowerCase().includes(q)) ||
                  (pig.breed && pig.breed.toLowerCase().includes(q))
                );
              });

              if (filteredPigs.length === 0) {
                return (
                  <p className="text-sm text-zinc-500 text-center py-12">
                    {searchQuery.trim()
                      ? `No pigs found matching "${searchQuery}".`
                      : viewingArchived
                      ? t("noArchived")
                      : t("noPigs")}
                  </p>
                );
              }

              return (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4">
                  {filteredPigs.map((pig) => {
                    const ageDays = calculateAgeDays(pig.birthDate);
                    const performance = evaluatePerformance(pig.breed, ageDays, pig.weight);

                    let performanceBg = "bg-[#E0E0E0] text-[#616161]";
                    if (performance === "Excellent") performanceBg = "bg-[#FEF3C7] text-[#B45309]";
                    else if (performance === "Good") performanceBg = "bg-[#C8E6C9] text-[#2E7D32]";
                    else if (performance === "Caution") performanceBg = "bg-[#FFF9C4] text-[#F57F17]";
                    else if (performance === "Poor") performanceBg = "bg-[#FFCDD2] text-[#C62828]";

                    const genderDisplay = (pig.gender === "Male" ? (t("male") || "Male") : (t("female") || "Female")) +
                      (pig.gender === "Male" && pig.castrated ? ` (${t("castrated_label") || "Castrated"})` : "") +
                      (pig.gender === "Female" && pig.parity !== undefined && pig.parity > 0 ? ` P${pig.parity}` : "");

                    const isWithdrawalActive = pig.activeWithdrawalUntil && pig.activeWithdrawalUntil >= new Date().toISOString().split("T")[0];

                    return (
                      <Link
                        href={`/dashboard/herd/${pig.id}`}
                        key={pig.id}
                        className="bg-[#E8F5E9] hover:bg-[#E0F2E9] border border-[#C8E6C9] rounded-2xl p-4 transition-all shadow-xs block group"
                      >
                        <div className="flex items-center justify-between gap-2">
                          <h3 className="text-base sm:text-lg font-bold text-[#1B5E20] group-hover:underline truncate">
                            {t("tag") || "Tag"}: {pig.tagNumber}
                          </h3>
                          <span className={`text-xs font-bold px-2.5 py-1 rounded-md shrink-0 ${performanceBg}`}>
                            {performance ? (t(performance.toLowerCase()) || performance) : ""}
                          </span>
                        </div>

                        <div className="flex items-center gap-4 text-xs sm:text-sm mt-2">
                          <div className="flex items-center gap-1 min-w-0">
                            <span className="font-bold text-[#2E7D32]">{t("age") || "Age"}:</span>
                            <span className="text-[#1B5E20] truncate">{formatSwineAge(pig.birthDate, true)}</span>
                          </div>
                          <div className="flex items-center gap-1 min-w-0">
                            <span className="font-bold text-[#2E7D32]">{t("gender") || "Gender"}:</span>
                            <span className="text-[#1B5E20] truncate">{genderDisplay}</span>
                          </div>
                        </div>

                        <div className="flex items-center gap-4 text-xs sm:text-sm mt-1">
                          <div className="flex items-center gap-1 min-w-0">
                            <span className="font-bold text-[#2E7D32]">{t("weight") || "Weight"}:</span>
                            <span className="text-[#1B5E20] truncate">{pig.weight} kg</span>
                          </div>
                          <div className="flex items-center gap-1 min-w-0">
                            <span className="font-bold text-[#2E7D32]">{t("location") || "Pen"}:</span>
                            <span className="text-[#1B5E20] truncate">{pig.location || "N/A"}</span>
                          </div>
                        </div>

                        {isWithdrawalActive && (
                          <div className="mt-2.5 bg-[#FFCDD2] text-[#B71C1C] rounded-md px-2 py-1 text-[11px] font-bold flex items-center gap-1">
                            <span>⚠️ WITHDRAWAL ACTIVE: UNTIL {pig.activeWithdrawalUntil}</span>
                          </div>
                        )}

                        {pig.status?.startsWith("Archived") && (
                          <div className="mt-1.5 text-[11px] font-bold text-red-600">
                            {pig.status}
                          </div>
                        )}
                      </Link>
                    );
                  })}
                </div>
              );
            })()}
          </div>

          {/* Sponsored Ad Banner for Free Users */}
          <NativeAdBanner />
        </main>
      </div>

      <RewardedPassModal
        isOpen={showRewardedPassModal}
        onClose={() => setShowRewardedPassModal(false)}
        title="Unlock Herd PDF Report"
        description="Watch a short video ad to unlock executive PDF reports and all premium herd tools for 3 hours!"
        onSuccess={() => {
          setTimeout(() => {
            window.print();
          }, 500);
        }}
      />

      {isPremium && (
        <HerdReport
          pigs={viewingArchived ? archivedPigs : pigs}
          title={viewingArchived ? t("titleArchived") : t("title")}
        />
      )}

      {/* Add Pigs Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-lg p-6 space-y-6 shadow-2xl max-h-[90vh] overflow-y-auto no-scrollbar">
            <h3 className="text-lg font-bold text-zinc-900">{t("addNewPigs")}</h3>

            {/* Mode selection */}
            <div className="flex gap-2 p-1 bg-zinc-100 rounded-lg">
              <button
                type="button"
                onClick={() => setIsMultiple(false)}
                className={`flex-1 py-1.5 text-xs font-bold rounded-md transition ${!isMultiple ? "bg-white text-zinc-800 shadow" : "text-zinc-500"}`}
              >
                {t("singlePig")}
              </button>
              <button
                type="button"
                onClick={() => setIsMultiple(true)}
                className={`flex-1 py-1.5 text-xs font-bold rounded-md transition ${isMultiple ? "bg-white text-zinc-800 shadow" : "text-zinc-500"}`}
              >
                {t("multipleBatch")}
              </button>
            </div>

            <form onSubmit={handleAddPig} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("breed")}</label>
                  <select
                    value={selectedBreed}
                    onChange={(e) => {
                      const val = e.target.value;
                      setSelectedBreed(val);
                      if (val !== "Other") {
                        setBreed(val);
                      } else {
                        setBreed(customBreed);
                      }
                    }}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  >
                    {STANDARD_BREEDS.map((b) => (
                      <option key={b}>{b}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("purpose")}</label>
                  <select
                    value={purpose}
                    onChange={(e) => setPurpose(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  >
                    <option value="Porker">{t("porker")}</option>
                    <option value="Breeder">{t("breeder")}</option>
                  </select>
                </div>
              </div>

              {selectedBreed === "Other" && (
                <div className="animate-fade-in">
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("specifyBreed")}</label>
                  <input
                    type="text"
                    required
                    value={customBreed}
                    onChange={(e) => {
                      const val = e.target.value;
                      setCustomBreed(val);
                      setBreed(val);
                    }}
                    placeholder={t("breedPlaceholder")}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
              )}

              {!isMultiple ? (
                <>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("tagNumber")}</label>
                      <input
                        type="text"
                        required
                        value={tagNumber}
                        onChange={(e) => setTagNumber(e.target.value)}
                        placeholder="e.g. SW-001"
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("gender")}</label>
                      <select
                        value={gender}
                        onChange={(e) => setGender(e.target.value)}
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                      >
                        <option>Male</option>
                        <option>Female</option>
                      </select>
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("weightKg")}</label>
                      <input
                        type="number"
                        step="any"
                        value={weight}
                        onChange={(e) => setWeight(parseFloat(e.target.value) || 0)}
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("locationPen")}</label>
                      <input
                        type="text"
                        value={location}
                        onChange={(e) => setLocation(e.target.value)}
                        placeholder="e.g. Pen A"
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                      />
                    </div>
                  </div>
                </>
              ) : (
                <>
                  {/* Males Section */}
                  <div className="space-y-3">
                    <div className="flex items-center justify-between border-b border-zinc-150 pb-2">
                      <h4 className="text-sm font-bold text-zinc-800">{t("males")}</h4>
                      <div className="flex items-center gap-2">
                        <label className="text-xs font-semibold text-zinc-500">{t("qty")}</label>
                        <input
                          type="number"
                          min="0"
                          value={maleQty || ""}
                          onChange={(e) => handleMaleQtyChange(parseInt(e.target.value) || 0)}
                          className="w-16 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 text-center"
                        />
                      </div>
                    </div>

                    {maleQty > 0 && (
                      <div className="space-y-2 max-h-[200px] overflow-y-auto pr-1 no-scrollbar">
                        <div className="grid grid-cols-12 gap-2 text-[10px] font-bold text-zinc-500 px-1">
                          <div className="col-span-4">{t("tagNumber")}</div>
                          <div className="col-span-4">{t("weightKg")}</div>
                          <div className="col-span-4">{t("locationPen")}</div>
                        </div>
                        {malePigs.map((pig, idx) => (
                          <div key={idx} className="grid grid-cols-12 gap-2 items-center">
                            <input
                              type="text"
                              required
                              placeholder={`Male Tag ${idx + 1}`}
                              value={pig.tagNumber}
                              onChange={(e) => {
                                const next = [...malePigs];
                                next[idx] = { ...next[idx], tagNumber: e.target.value };
                                setMalePigs(next);
                              }}
                              className="col-span-4 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                            <input
                              type="number"
                              step="any"
                              placeholder="0"
                              value={pig.weight || ""}
                              onChange={(e) => {
                                const next = [...malePigs];
                                next[idx] = { ...next[idx], weight: parseFloat(e.target.value) || 0 };
                                setMalePigs(next);
                              }}
                              className="col-span-4 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                            <input
                              type="text"
                              placeholder="Pen"
                              value={pig.location}
                              onChange={(e) => {
                                const next = [...malePigs];
                                next[idx] = { ...next[idx], location: e.target.value };
                                setMalePigs(next);
                              }}
                              className="col-span-4 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Females Section */}
                  <div className="space-y-3">
                    <div className="flex items-center justify-between border-b border-zinc-150 pb-2">
                      <h4 className="text-sm font-bold text-zinc-800">{t("females")}</h4>
                      <div className="flex items-center gap-2">
                        <label className="text-xs font-semibold text-zinc-500">{t("qty")}</label>
                        <input
                          type="number"
                          min="0"
                          value={femaleQty || ""}
                          onChange={(e) => handleFemaleQtyChange(parseInt(e.target.value) || 0)}
                          className="w-16 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 text-center"
                        />
                      </div>
                    </div>

                    {femaleQty > 0 && (
                      <div className="space-y-2 max-h-[200px] overflow-y-auto pr-1 no-scrollbar">
                        <div className="grid grid-cols-12 gap-2 text-[10px] font-bold text-zinc-500 px-1">
                          <div className="col-span-4">{t("tagNumber")}</div>
                          <div className="col-span-4">{t("weightKg")}</div>
                          <div className="col-span-4">{t("locationPen")}</div>
                        </div>
                        {femalePigs.map((pig, idx) => (
                          <div key={idx} className="grid grid-cols-12 gap-2 items-center">
                            <input
                              type="text"
                              required
                              placeholder={`Female Tag ${idx + 1}`}
                              value={pig.tagNumber}
                              onChange={(e) => {
                                const next = [...femalePigs];
                                next[idx] = { ...next[idx], tagNumber: e.target.value };
                                setFemalePigs(next);
                              }}
                              className="col-span-4 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                            <input
                              type="number"
                              step="any"
                              placeholder="0"
                              value={pig.weight || ""}
                              onChange={(e) => {
                                const next = [...femalePigs];
                                next[idx] = { ...next[idx], weight: parseFloat(e.target.value) || 0 };
                                setFemalePigs(next);
                              }}
                              className="col-span-4 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                            <input
                              type="text"
                              placeholder="Pen"
                              value={pig.location}
                              onChange={(e) => {
                                const next = [...femalePigs];
                                next[idx] = { ...next[idx], location: e.target.value };
                                setFemalePigs(next);
                              }}
                              className="col-span-4 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </>
              )}

              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("birthDate")}</label>
                  <input
                    type="date"
                    required
                    value={birthDate}
                    onChange={(e) => setBirthDate(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("sowTag")}</label>
                  <input
                    type="text"
                    value={sowTag}
                    onChange={(e) => setSowTag(e.target.value)}
                    placeholder={t("motherTag")}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("boarTag")}</label>
                  <input
                    type="text"
                    value={boarTag}
                    onChange={(e) => setBoarTag(e.target.value)}
                    placeholder={t("fatherTag")}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("source")}</label>
                <select
                  value={source}
                  onChange={(e) => setSource(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                >
                  <option value="Born on farm">{t("bornOnFarm")}</option>
                  <option value="Brought to farm">{t("broughtToFarm")}</option>
                </select>
              </div>

              {source === "Brought to farm" && (
                <div className="bg-emerald-50/50 border border-emerald-200/50 rounded-xl p-4 space-y-2 animate-fade-in shadow-sm">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-bold text-emerald-800">{t("purchasePrice")}</label>
                    <span className="text-[10px] font-semibold text-emerald-600 bg-emerald-100/80 px-2 py-0.5 rounded-full">{t("financialExpense")}</span>
                  </div>
                  <input
                    type="number"
                    step="any"
                    required
                    value={purchasePrice === 0 ? "" : purchasePrice}
                    onChange={(e) => setPurchasePrice(parseFloat(e.target.value) || 0)}
                    placeholder={t("purchasePricePlaceholder")}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("notes")}</label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder={t("notesPlaceholder")}
                  rows={2}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100 transition"
                >
                  {t("cancel")}
                </button>
                <button
                  type="submit"
                  className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition"
                >
                  {t("save")}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
