"use client";

import React, { useEffect, useState, useMemo, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot, doc, setDoc, deleteDoc, writeBatch } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import UserProfileDropdown from "@/components/UserProfileDropdown";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import FinancialReport from "@/components/reports/FinancialReport";
import { ExportPdfIcon } from "@/components/icons/DashboardIcons";
import { useTranslations } from "next-intl";
import { FinancialRecord, Pig } from "@/lib/types";
import { TierLimiter } from "@/lib/tierLimiter";
import NativeAdBanner from "@/components/ads/NativeAdBanner";
import RewardedPassModal from "@/components/ads/RewardedPassModal";

function FinancialsContent() {
  const t = useTranslations("Financials");
  const tNav = useTranslations("Navigation");
  const tCommon = useTranslations("Common");
  const tHr = useTranslations("HR");
  const searchParams = useSearchParams();
  const shouldAdd = searchParams.get("add") === "true";

  const translateCategory = (cat: string, type: string) => {
    if (!cat) return "";
    const incomeKeys: Record<string, string> = {
      "Pig Sale": "incomeCategories.pigSale",
      "Manure Sale": "incomeCategories.manureSale",
      "Breeding Service": "incomeCategories.breedingService",
      "Equipment Sale": "incomeCategories.equipmentSale",
      "Other": "incomeCategories.other",
    };
    const expenseKeys: Record<string, string> = {
      "Feed": "expenseCategories.feed",
      "Vet/Medication": "expenseCategories.vet",
      "Vet": "expenseCategories.vet",
      "Labor/Salary": "expenseCategories.labor",
      "Labor": "expenseCategories.labor",
      "Equipment": "expenseCategories.equipment",
      "Transport": "expenseCategories.transport",
      "Rent": "expenseCategories.rent",
      "Utility": "expenseCategories.utility",
      "Other": "expenseCategories.other",
    };
    const key = type === "Income" ? incomeKeys[cat] : expenseKeys[cat];
    return key ? t(key) : cat;
  };
  const { user, userProfile, activeFarmUid, isFinancialsRestricted, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  useEffect(() => {
    if (!loading && isFinancialsRestricted) {
      router.replace("/dashboard");
    }
  }, [loading, isFinancialsRestricted, router]);

  const currencySymbol = userProfile?.settings?.currencySymbol || "$";

  const [records, setRecords] = useState<FinancialRecord[]>([]);
  const [pigs, setPigs] = useState<Pig[]>([]);
  const [dataLoading, setDataLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(shouldAdd);
  const [showRewardedPassModal, setShowRewardedPassModal] = useState(false);

  useEffect(() => {
    if (searchParams.get("add") === "true") {
      setShowAddModal(true);
    }
  }, [searchParams]);

  // Form states
  const [type, setType] = useState("Expense");
  const [date, setDate] = useState(new Date().toISOString().split("T")[0]);
  const [category, setCategory] = useState("Feed");
  const [customCategory, setCustomCategory] = useState("");
  const [amount, setAmount] = useState(0);
  const [description, setDescription] = useState("");
  const [selectedPigIds, setSelectedPigIds] = useState<string[]>([]);

  const isPremium = Boolean(userProfile?.isPremium || userProfile?.isAdmin);
  const recordLimitReached = !isPremium && records.length >= TierLimiter.FREE_MAX_FINANCIAL_RECORDS;

  const categories = {
    Income: [
      { key: "Pig Sale", label: t("incomeCategories.pigSale") },
      { key: "Manure Sale", label: t("incomeCategories.manureSale") },
      { key: "Breeding Service", label: t("incomeCategories.breedingService") },
      { key: "Equipment Sale", label: t("incomeCategories.equipmentSale") },
      { key: "Other", label: t("incomeCategories.other") }
    ],
    Expense: [
      { key: "Feed", label: t("expenseCategories.feed") },
      { key: "Vet/Medication", label: t("expenseCategories.vet") },
      { key: "Salary", label: t("expenseCategories.labor") },
      { key: "Equipment", label: t("expenseCategories.equipment") },
      { key: "Transport", label: t("expenseCategories.transport") },
      { key: "Rent", label: t("expenseCategories.rent") },
      { key: "Utility", label: t("expenseCategories.utility") },
      { key: "Other", label: t("expenseCategories.other") }
    ]
  };

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!activeFarmUid) return;

    setDataLoading(true);

    // 1. Listen to Financial Records
    const finRef = collection(db, "users", activeFarmUid, "financials");
    const unsubscribeFin = onSnapshot(finRef, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as FinancialRecord));
      setRecords(list.sort((a, b) => b.date.localeCompare(a.date)));
      setDataLoading(false);
    }, (err) => {
      console.error(err);
      setDataLoading(false);
    });

    // 2. Listen to Pigs for linking transactions
    const pigsRef = collection(db, "users", activeFarmUid, "pigs");
    const unsubscribePigs = onSnapshot(pigsRef, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Pig));
      setPigs(list.sort((a, b) => (a.tagNumber || a.id).localeCompare(b.tagNumber || b.id)));
    });

    return () => {
      unsubscribeFin();
      unsubscribePigs();
    };
  }, [activeFarmUid]);

  const handleAddTransaction = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || amount <= 0) return;

    if (recordLimitReached) {
      alert(`Financial record limit of ${TierLimiter.FREE_MAX_FINANCIAL_RECORDS} reached for free tier. Please upgrade to add more.`);
      router.push("/dashboard/billing");
      setShowAddModal(false);
      return;
    }

    try {
      const finCollection = collection(db, "users", activeFarmUid, "financials");
      const newRef = doc(finCollection);

      const finalCategory = (category === "Other" && customCategory.trim()) ? customCategory.trim() : category;
      const record: FinancialRecord = {
        id: newRef.id,
        date,
        type,
        category: finalCategory,
        amount,
        description
      };
      const isPigSale = type === "Income" && category === "Pig Sale" && selectedPigIds.length > 0;
      if (isPigSale) {
        if (selectedPigIds.length === 1) {
          record.pigId = selectedPigIds[0];
        }
        record.pigIds = selectedPigIds;
      }

      if (isPigSale && selectedPigIds.length > 0) {
        const batch = writeBatch(db);
        batch.set(newRef, record);

        for (const pid of selectedPigIds) {
          const soldPig = pigs.find((p) => p.id === pid);
          if (soldPig) {
            const pigRef = doc(db, "users", activeFarmUid, "pigs", pid);
            const archiveRef = doc(db, "users", activeFarmUid, "archived_pigs", pid);

            const archivedPig: Pig = {
              ...soldPig,
              status: "Archived (Sold)",
              location: "Archived",
              notes: (soldPig.notes ? soldPig.notes + "\n" : "") + `Archived on: ${date || new Date().toISOString().split("T")[0]} Reason: Sold`,
            };

            batch.set(archiveRef, archivedPig);
            batch.delete(pigRef);
          }
        }
        await batch.commit();
      } else {
        await setDoc(newRef, record);
      }

      // Reset
      setAmount(0);
      setDescription("");
      setCustomCategory("");
      setSelectedPigIds([]);
      setShowAddModal(false);
    } catch (err) {
      console.error("Failed to log transaction:", err);
    }
  };

  const handleDeleteRecord = async (id: string) => {
    if (!activeFarmUid || !confirm(t("confirmDelete"))) return;
    try {
      await deleteDoc(doc(db, "users", activeFarmUid, "financials", id));
    } catch (err) {
      console.error(err);
    }
  };

  // Calculations
  const totalIncome = records.filter(r => r.type === "Income").reduce((sum, r) => sum + r.amount, 0);
  const totalExpense = records.filter(r => r.type === "Expense").reduce((sum, r) => sum + r.amount, 0);
  const netBalance = totalIncome - totalExpense;

  // Unit Economics & Break-even Calculations (Commercial 180-day grow-out cycle scoping)
  const activePigs = useMemo(() => {
    return pigs.filter(
      (p) => p.location !== "Archived" && !p.status?.toLowerCase().startsWith("archived")
    );
  }, [pigs]);

  const totalLiveHerdWeightKg = useMemo(() => {
    return activePigs.reduce((sum, p) => sum + (p.weight || 0), 0);
  }, [activePigs]);

  const { cycleExpenses, isScopedToCycle } = useMemo(() => {
    const cycleCutoff = new Date();
    cycleCutoff.setDate(cycleCutoff.getDate() - 180);

    const expenseRecords = records.filter((r) => r.type === "Expense");
    const recentExpenses = expenseRecords.filter((r) => {
      const d = new Date(r.date);
      return !isNaN(d.getTime()) && d >= cycleCutoff;
    });

    if (recentExpenses.length > 0) {
      return {
        cycleExpenses: recentExpenses.reduce((sum, r) => sum + (r.amount || 0), 0),
        isScopedToCycle: true,
      };
    }
    return {
      cycleExpenses: expenseRecords.reduce((sum, r) => sum + (r.amount || 0), 0),
      isScopedToCycle: false,
    };
  }, [records]);

  const costOfProductionPerKg = totalLiveHerdWeightKg > 0 ? cycleExpenses / totalLiveHerdWeightKg : 0;
  const breakEvenPerFinisher = costOfProductionPerKg * 90.0;
  const targetSellingPrice20Margin = costOfProductionPerKg * 1.2;
  const targetSellingPriceFinisher = breakEvenPerFinisher * 1.2;

  // Expense Distribution & Benchmarks Calculations
  const expenseRecords = useMemo(() => records.filter((r) => r.type === "Expense"), [records]);
  const feedTotal = useMemo(
    () => expenseRecords.filter((r) => r.category?.toLowerCase() === "feed").reduce((sum, r) => sum + (r.amount || 0), 0),
    [expenseRecords]
  );
  const medicineTotal = useMemo(
    () =>
      expenseRecords
        .filter((r) =>
          ["vet", "vet/medication", "medicine", "vaccination", "medication"].includes(r.category?.toLowerCase())
        )
        .reduce((sum, r) => sum + (r.amount || 0), 0),
    [expenseRecords]
  );
  const laborTotal = useMemo(
    () =>
      expenseRecords
        .filter((r) => ["labor", "labor/salary", "salary"].includes(r.category?.toLowerCase()))
        .reduce((sum, r) => sum + (r.amount || 0), 0),
    [expenseRecords]
  );
  const otherTotal = Math.max(0, totalExpense - feedTotal - medicineTotal - laborTotal);

  const feedPct = totalExpense > 0 ? (feedTotal / totalExpense) * 100 : 0;
  const healthPct = totalExpense > 0 ? (medicineTotal / totalExpense) * 100 : 0;
  const laborPct = totalExpense > 0 ? (laborTotal / totalExpense) * 100 : 0;
  const otherPct = totalExpense > 0 ? (otherTotal / totalExpense) * 100 : 0;

  const benchmarkAssessment = useMemo(() => {
    if (totalExpense === 0) {
      return {
        text: "No expense records yet.",
        color: "text-zinc-500",
      };
    }
    if (feedPct >= 65 && feedPct <= 75) {
      return {
        text: `Optimal: Feed is ${feedPct.toFixed(1)}% of total expenses (target 65-75%).`,
        color: "text-emerald-700",
      };
    }
    if (feedPct > 75) {
      return {
        text: `Alert: Feed is ${feedPct.toFixed(1)}% of total expenses. Higher than recommended 75% benchmark.`,
        color: "text-rose-700",
      };
    }
    return {
      text: `Feed is ${feedPct.toFixed(1)}% of total expenses (below standard 65-75% range).`,
      color: "text-amber-700",
    };
  }, [totalExpense, feedPct]);

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-[#F8FAF9] dark:bg-[#121212] text-zinc-900 dark:text-zinc-100">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen bg-[#F8FAF9] dark:bg-[#121212] text-zinc-900 dark:text-zinc-100 flex flex-col font-sans overflow-x-hidden">
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
        {!isMobile && (
          <DesktopHeader
            showBack
            backPath="/dashboard"
            label={tNav("financials") || "FINANCIALS"}
            labelColor="text-[#00796B] dark:text-[#4DB6AC]"
          />
        )}

        <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8 space-y-6">
          {/* Top Bar with Back Button and Heading */}
          <div className="flex items-center justify-between gap-4">
            <button
              type="button"
              onClick={() => router.push("/dashboard")}
              className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-white dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700 hover:text-zinc-900 transition font-bold text-xs shadow-xs"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
              <span>{tCommon("back") || "Back"}</span>
            </button>
            <h1 className="text-xl sm:text-2xl font-black text-[#00796B] dark:text-[#4DB6AC] text-center flex-1">
              {tNav("financials") || "Financials"}
            </h1>
            <div className="w-16" />
          </div>

          {/* Dashboard balance summaries */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
            {[
              { label: t("totalRevenue"), amount: totalIncome, color: "text-emerald-700 bg-emerald-50/50 border-emerald-100" },
              { label: t("totalExpenses"), amount: totalExpense, color: "text-rose-700 bg-rose-50/50 border-rose-100" },
              { label: t("netCashflow"), amount: netBalance, color: netBalance >= 0 ? "text-emerald-800 bg-emerald-100/30 border-emerald-200" : "text-rose-800 bg-rose-100/30 border-rose-200" }
            ].map((stat, i) => (
              <div key={i} className={`backdrop-blur-md border rounded-2xl p-6 shadow-sm bg-white/60 ${stat.color}`}>
                <p className="text-xs font-bold uppercase tracking-wider">{stat.label}</p>
                <p className="text-3xl font-black mt-2">{currencySymbol}{stat.amount.toFixed(2)}</p>
              </div>
            ))}
          </div>

          {/* Unit Economics & Breakeven Card */}
          <div className="bg-emerald-50/60 border border-teal-200/80 rounded-2xl p-5 sm:p-6 shadow-xs space-y-4">
            <div className="flex items-center justify-between flex-wrap gap-2">
              <div className="flex items-center gap-2 text-teal-800">
                <span className="p-1.5 rounded-lg bg-teal-100 text-teal-800">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 14h-2v-1c-1.66 0-3-1.34-3-3h2c0 .55.45 1 1 1h2c.55 0 1-.45 1-1v-2c0-.55-.45-1-1-1h-2c-1.66 0-3-1.34-3-3s1.34-3 3-3V3h2v1c1.66 0 3 1.34 3 3h-2c0-.55-.45-1-1-1h-2c-.55 0-1 .45-1 1v2c0 .55.45 1 1 1h2c1.66 0 3 1.34 3 3v2c0 .55-.45 1-1 1z" />
                  </svg>
                </span>
                <h3 className="text-base font-bold text-teal-950">Unit Economics & Breakeven</h3>
              </div>
              <div className="px-2.5 py-1 rounded-lg bg-teal-100/80 border border-teal-200 text-teal-900 text-xs font-bold">
                {isScopedToCycle ? (
                  <span>
                    {activePigs.length} {tHr("pigs") || "pigs"} ({totalLiveHerdWeightKg.toFixed(0)} kg) • 180d cycle
                  </span>
                ) : (
                  <span>
                    {activePigs.length} {tHr("pigs") || "pigs"} ({totalLiveHerdWeightKg.toFixed(0)} kg)
                  </span>
                )}
              </div>
            </div>

            <div className="border-t border-teal-200/50 pt-3 grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="space-y-1">
                <p className="text-xs text-zinc-500 font-medium">Cost of Production / kg</p>
                <p className="text-xl font-black text-zinc-900">
                  {currencySymbol}{costOfProductionPerKg.toFixed(2)} / kg
                </p>
              </div>

              <div className="space-y-1">
                <p className="text-xs text-zinc-500 font-medium">Finisher Breakeven Price (90kg)</p>
                <p className="text-xl font-black text-rose-700">
                  {currencySymbol}{breakEvenPerFinisher.toFixed(2)}
                </p>
              </div>

              <div className="space-y-1">
                <p className="text-xs text-zinc-500 font-medium">Target Selling Price (+20% Margin)</p>
                <p className="text-xl font-black text-emerald-700">
                  {currencySymbol}{targetSellingPriceFinisher.toFixed(2)}{" "}
                  <span className="text-xs font-bold text-emerald-800">
                    ({currencySymbol}{targetSellingPrice20Margin.toFixed(2)}/kg)
                  </span>
                </p>
              </div>
            </div>
          </div>

          {/* Expense Distribution & Financial Benchmarks Card */}
          <div className="bg-teal-50/70 border border-teal-200/80 rounded-2xl p-5 sm:p-6 shadow-xs space-y-4">
            <div className="flex items-center justify-between flex-wrap gap-2">
              <div className="flex items-center gap-2 text-teal-800">
                <span className="p-1.5 rounded-lg bg-teal-100 text-teal-800">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                    <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z" />
                  </svg>
                </span>
                <h3 className="text-base font-bold text-teal-950">Expense Distribution & Financial Benchmarks</h3>
              </div>
            </div>

            <div className="border-t border-teal-200/50 pt-3 space-y-3">
              {/* Feed Distribution */}
              <div className="space-y-1">
                <div className="flex justify-between text-xs font-semibold text-zinc-700">
                  <span>Feed ({feedPct.toFixed(1)}%):</span>
                  <span>{currencySymbol}{feedTotal.toFixed(2)}</span>
                </div>
                <div className="w-full h-2 bg-zinc-200 rounded-full overflow-hidden">
                  <div
                    className={`h-full transition-all duration-500 ${feedPct > 75 ? "bg-rose-600" : "bg-emerald-600"}`}
                    style={{ width: `${Math.min(feedPct, 100)}%` }}
                  />
                </div>
              </div>

              {/* Health / Vet */}
              <div className="flex justify-between text-xs text-zinc-600 font-medium">
                <span>Health / Veterinary (Target: 5–10% • {healthPct.toFixed(1)}%):</span>
                <span className="font-bold text-zinc-800">{currencySymbol}{medicineTotal.toFixed(2)}</span>
              </div>

              {/* Labor & Operations */}
              <div className="flex justify-between text-xs text-zinc-600 font-medium">
                <span>Labor & Operations (Target: 10–15% • {laborPct.toFixed(1)}%):</span>
                <span className="font-bold text-zinc-800">{currencySymbol}{laborTotal.toFixed(2)}</span>
              </div>

              {/* Other Expenses */}
              {otherTotal > 0 && (
                <div className="flex justify-between text-xs text-zinc-500 font-medium">
                  <span>Other Operations ({otherPct.toFixed(1)}%):</span>
                  <span className="font-semibold text-zinc-700">{currencySymbol}{otherTotal.toFixed(2)}</span>
                </div>
              )}

              {/* Benchmark Assessment Note */}
              <div className="pt-2 border-t border-teal-200/40">
                <p className={`text-xs font-bold ${benchmarkAssessment.color}`}>
                  • {benchmarkAssessment.text}
                </p>
              </div>
            </div>
          </div>

          {/* Ledger Table */}
          <div className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4 flex-wrap gap-4">
              <h2 className="text-lg font-bold text-zinc-900">{t("transactionsCount", { count: records.length })}</h2>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => {
                    if (!isPremium) {
                      setShowRewardedPassModal(true);
                      return;
                    }
                    window.print();
                  }}
                  className={`rounded-lg border px-3 py-2 text-xs font-semibold transition shadow-sm flex items-center gap-1.5 ${
                    isPremium
                      ? "border-zinc-200 bg-zinc-50/50 text-zinc-650 hover:bg-zinc-100"
                      : "border-amber-200 bg-amber-50 text-amber-700 hover:bg-amber-100"
                  }`}
                >
                  <ExportPdfIcon className="h-3.5 w-3.5 opacity-80" />
                  <span>{isPremium ? t("exportPdf") : t("exportPdfPremium")}</span>
                </button>
                <button
                  onClick={() => {
                    if (recordLimitReached) {
                      alert(`Financial record limit of ${TierLimiter.FREE_MAX_FINANCIAL_RECORDS} reached for free tier. Please upgrade to add more.`);
                      router.push("/dashboard/billing");
                      return;
                    }
                    setShowAddModal(true);
                  }}
                  className={`rounded-lg px-4 py-2 text-xs font-bold text-white shadow transition active:scale-95 ${
                    recordLimitReached
                      ? "bg-amber-600 hover:bg-amber-700 shadow-amber-600/10"
                      : "bg-teal-600 hover:bg-teal-700 shadow-teal-600/10"
                  }`}
                >
                  {recordLimitReached
                    ? `Limit Reached (${TierLimiter.FREE_MAX_FINANCIAL_RECORDS}) - Upgrade`
                    : `+ ${t("logTransaction")}`}
                </button>
              </div>
            </div>
            {dataLoading ? (
              <div className="space-y-3">
                {[...Array(4)].map((_, i) => (
                  <div key={i} className="h-10 bg-zinc-100 animate-pulse rounded-lg" />
                ))}
              </div>
            ) : records.length === 0 ? (
              <p className="text-sm text-zinc-500 text-center py-12">{t("noTransactions")}</p>
            ) : (
              <>
                {/* Desktop Table View */}
                <div className="hidden sm:block overflow-x-auto">
                  <table className="min-w-full divide-y divide-zinc-200 text-sm">
                    <thead>
                      <tr className="text-left text-xs font-semibold text-zinc-500 uppercase tracking-wider border-b border-zinc-200">
                        <th className="pb-3">{t("date")}</th>
                        <th className="pb-3">{t("type")}</th>
                        <th className="pb-3">{t("category")}</th>
                        <th className="pb-3">{t("description")}</th>
                        <th className="pb-3">{t("linkedPig")}</th>
                        <th className="pb-3 text-right">{t("amount")}</th>
                        <th className="pb-3 text-right">{t("actions")}</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-150">
                      {records.map(record => {
                        const linkedPig = pigs.find(p => p.id === record.pigId);
                        return (
                          <tr key={record.id}>
                            <td className="py-4 font-mono text-zinc-500">{record.date}</td>
                            <td className="py-4">
                              <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                                record.type === "Income" ? "bg-emerald-50 text-emerald-800 border border-emerald-100" : "bg-rose-50 text-rose-800 border border-rose-100"
                              }`}>
                                {record.type === "Income" ? t("income") : t("expense")}
                              </span>
                            </td>
                            <td className="py-4 font-semibold text-zinc-800">{translateCategory(record.category, record.type)}</td>
                            <td className="py-4 text-zinc-500">{record.description}</td>
                            <td className="py-4 text-zinc-600 font-mono">
                              {record.pigIds && record.pigIds.length > 1 ? (
                                <span className="text-xs font-semibold text-zinc-700">
                                  {record.pigIds.length} Pigs ({record.pigIds.map(id => pigs.find(p => p.id === id)?.tagNumber || id).slice(0, 2).join(", ")}{record.pigIds.length > 2 ? "..." : ""})
                                </span>
                              ) : linkedPig ? (
                                <Link href={`/dashboard/herd/${record.pigId || (record.pigIds && record.pigIds[0])}`} className="text-emerald-700 hover:underline">
                                  {linkedPig.tagNumber}
                                </Link>
                              ) : t("none")}
                            </td>
                            <td className={`py-4 text-right font-bold font-mono ${
                              record.type === "Income" ? "text-emerald-700" : "text-rose-700"
                            }`}>
                              {record.type === "Income" ? "+" : "-"}{currencySymbol}{record.amount.toFixed(2)}
                            </td>
                            <td className="py-4 text-right font-medium">
                              <button onClick={() => handleDeleteRecord(record.id)} className="text-xs text-rose-600 hover:underline">
                                {t("delete")}
                              </button>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                {/* Mobile Card View */}
                <div className="sm:hidden space-y-4">
                  {records.map(record => {
                    const linkedPig = pigs.find(p => p.id === record.pigId || (record.pigIds && record.pigIds.includes(p.id)));
                    return (
                      <div key={record.id} className="p-4 rounded-xl border border-zinc-100 bg-zinc-50/50 space-y-3 relative overflow-hidden">
                        <div className="flex justify-between items-start">
                          <div>
                            <p className="text-[10px] font-bold text-zinc-400 font-mono">{record.date}</p>
                            <h4 className="font-bold text-zinc-900">{translateCategory(record.category, record.type)}</h4>
                          </div>
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                            record.type === "Income" ? "bg-emerald-50 text-emerald-800 border border-emerald-100" : "bg-rose-50 text-rose-800 border border-rose-100"
                          }`}>
                            {record.type === "Income" ? t("income") : t("expense")}
                          </span>
                        </div>

                        <p className="text-xs text-zinc-600 leading-relaxed">{record.description}</p>

                        <div className="flex justify-between items-end border-t border-zinc-100 pt-3">
                          <div className="text-[10px]">
                            <span className="text-zinc-400 font-semibold uppercase">{t("linkedPig")}: </span>
                            {record.pigIds && record.pigIds.length > 1 ? (
                              <span className="text-zinc-700 font-bold">
                                {record.pigIds.length} Pigs
                              </span>
                            ) : linkedPig ? (
                              <Link href={`/dashboard/herd/${record.pigId || (record.pigIds && record.pigIds[0])}`} className="text-emerald-700 font-bold hover:underline">
                                {linkedPig.tagNumber}
                              </Link>
                            ) : <span className="text-zinc-500 font-bold">{t("none")}</span>}
                          </div>
                          <div className="text-right">
                             <p className={`text-sm font-black font-mono ${record.type === "Income" ? "text-emerald-700" : "text-rose-700"}`}>
                                {record.type === "Income" ? "+" : "-"}{currencySymbol}{record.amount.toFixed(2)}
                             </p>
                             <button onClick={() => handleDeleteRecord(record.id)} className="text-[10px] font-bold text-rose-500 mt-1 uppercase tracking-tight">
                                {t("deleteEntry")}
                             </button>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </>
            )}
          </div>

          {/* Sponsored Ad Banner for Free Users */}
          <NativeAdBanner />
        </main>
      </div>

      <RewardedPassModal
        isOpen={showRewardedPassModal}
        onClose={() => setShowRewardedPassModal(false)}
        title="Unlock Financial Ledger PDF Report"
        description="Watch a short video ad to unlock executive cashflow reports, ledger exports, and all premium financial tools for 3 hours!"
        onSuccess={() => {
          setTimeout(() => {
            window.print();
          }, 500);
        }}
      />

      <FinancialReport
        records={records}
        pigs={pigs}
        currencySymbol={currencySymbol}
      />

      {/* Log Transaction Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-teal-50/95 border border-teal-200 rounded-2xl w-full max-w-md p-6 space-y-6 shadow-2xl text-teal-950 max-h-[90vh] overflow-y-auto no-scrollbar">
            <h3 className="text-lg font-bold text-teal-950 border-b border-teal-200/80 pb-3">{t("logTransaction")}</h3>
            <form onSubmit={handleAddTransaction} className="space-y-4">
              <div className="flex gap-2 p-1 bg-teal-100/70 border border-teal-200 rounded-lg">
                <button
                  type="button"
                  onClick={() => { setType("Expense"); setCategory("Feed"); setCustomCategory(""); }}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-md transition ${type === "Expense" ? "bg-white text-teal-900 shadow" : "text-teal-700"}`}
                >
                  {t("expense")}
                </button>
                <button
                  type="button"
                  onClick={() => { setType("Income"); setCategory("Pig Sale"); setCustomCategory(""); }}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-md transition ${type === "Income" ? "bg-white text-teal-900 shadow" : "text-teal-700"}`}
                >
                  {t("income")}
                </button>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("date")}</label>
                  <input
                    type="date"
                    required
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("category")}</label>
                  <select
                    value={category}
                    onChange={(e) => { setCategory(e.target.value); setCustomCategory(""); }}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    {(type === "Income" ? categories.Income : categories.Expense).map(cat => (
                      <option key={cat.key} value={cat.key}>{cat.label}</option>
                    ))}
                  </select>
                </div>
              </div>

              {category === "Other" && (
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Custom Category Name</label>
                  <input
                    type="text"
                    required
                    value={customCategory}
                    onChange={(e) => setCustomCategory(e.target.value)}
                    placeholder="e.g. Disinfectant, Transport, Utilities, etc."
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("amountWithSymbol", { symbol: currencySymbol })}</label>
                <input
                  type="number"
                  step="any"
                  required
                  value={amount}
                  onChange={(e) => setAmount(parseFloat(e.target.value) || 0)}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                />
              </div>

              {type === "Income" && category === "Pig Sale" && (
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="block text-xs font-semibold text-zinc-500">
                      Select Pigs Sold ({selectedPigIds.length})
                    </label>
                    {selectedPigIds.length > 0 && (
                      <button
                        type="button"
                        onClick={() => setSelectedPigIds([])}
                        className="text-[11px] text-zinc-400 hover:text-zinc-600 underline"
                      >
                        Clear All
                      </button>
                    )}
                  </div>
                  <div className="border border-zinc-200 rounded-lg max-h-40 overflow-y-auto p-2.5 space-y-1.5 bg-white shadow-sm">
                    {pigs.filter(p => !p.status?.startsWith("Archived")).length === 0 ? (
                      <p className="text-xs text-zinc-400 italic">No active pigs available</p>
                    ) : (
                      pigs.filter(p => !p.status?.startsWith("Archived")).map(p => (
                        <label key={p.id} className="flex items-center justify-between text-xs text-zinc-700 hover:bg-zinc-50 p-1.5 rounded cursor-pointer transition">
                          <div className="flex items-center gap-2">
                            <input
                              type="checkbox"
                              checked={selectedPigIds.includes(p.id)}
                              onChange={(e) => {
                                if (e.target.checked) {
                                  setSelectedPigIds(prev => [...prev, p.id]);
                                } else {
                                  setSelectedPigIds(prev => prev.filter(id => id !== p.id));
                                }
                              }}
                              className="h-4 w-4 rounded border-zinc-300 text-teal-600 focus:ring-teal-500"
                            />
                            <span className="font-semibold text-zinc-900">{p.tagNumber || p.id}</span>
                            <span className="text-[10px] text-zinc-400">({p.status || "Unknown"})</span>
                          </div>
                          {p.weight && p.weight > 0 && (
                            <span className="text-[11px] text-zinc-500 font-mono">{p.weight} kg</span>
                          )}
                        </label>
                      ))
                    )}
                  </div>
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("description")}</label>
                <textarea
                  required
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder={t("descriptionPlaceholder")}
                  rows={2}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-teal-200/80">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="rounded-lg border border-teal-300 bg-white/80 px-4 py-2 text-xs font-semibold text-teal-800 hover:bg-teal-100 transition"
                >
                  {t("cancel")}
                </button>
                <button type="submit" className="rounded-lg bg-teal-600 hover:bg-teal-700 px-4 py-2 text-xs font-bold text-white transition shadow-sm">
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

export default function FinancialsPage() {
  return (
    <Suspense fallback={<div className="p-6">Loading...</div>}>
      <FinancialsContent />
    </Suspense>
  );
}
