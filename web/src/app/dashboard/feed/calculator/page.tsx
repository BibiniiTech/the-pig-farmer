"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import { useTranslations } from "next-intl";
import { CalculateIcon, ExportPdfIcon } from "@/components/icons/DashboardIcons";
import FeedRequirementsReport from "@/components/reports/FeedRequirementsReport";
import { Pig } from "@/lib/types";
import PremiumWrapper from "@/components/PremiumWrapper";
import NativeAdBanner from "@/components/ads/NativeAdBanner";
import RewardedPassModal from "@/components/ads/RewardedPassModal";

const ArrowLeftIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <line x1="19" y1="12" x2="5" y2="12" />
    <polyline points="12 19 5 12 12 5" />
  </svg>
);

export default function FeedCalculatorPage() {
  const t = useTranslations("Feed");
  const tCommon = useTranslations("Common");
  const { user, activeFarmUid, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  const [sowsCount, setSowsCount] = useState<number>(0);
  const [boarsCount, setBoarsCount] = useState<number>(0);
  const [giltsCount, setGiltsCount] = useState<number>(0);
  const [pregnantCount, setPregnantCount] = useState<number>(0);
  const [lactatingCount, setLactatingCount] = useState<number>(0);
  const [breedersStarterCount, setBreedersStarterCount] = useState<number>(0);
  const [breedersGrowerCount, setBreedersGrowerCount] = useState<number>(0);
  const [starterCount, setStarterCount] = useState<number>(0);
  const [growerCount, setGrowerCount] = useState<number>(0);
  const [finisherCount, setFinisherCount] = useState<number>(0);
  const [calcDays, setCalcDays] = useState<number>(1);
  const [calculationBreakdown, setCalculationBreakdown] = useState<any[]>([]);
  const [totalDailyReq, setTotalDailyReq] = useState<number>(0);
  const [totalPeriodReq, setTotalPeriodReq] = useState<number>(0);
  const [showRewardedPassModal, setShowRewardedPassModal] = useState<boolean>(false);

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!activeFarmUid) return;
    const pigsQuery = collection(db, "users", activeFarmUid, "pigs");
    const unsub = onSnapshot(pigsQuery, (snapshot) => {
      const pigList = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Pig)).filter(p => !p.archived && !p.status?.toLowerCase().startsWith("archived"));
      
      const breeders = pigList.filter(p => p.purpose === "Breeder");
      const porkers = pigList.filter(p => p.purpose === "Porker");

      setSowsCount(breeders.filter(p => p.status === "Sow").length);
      setBoarsCount(breeders.filter(p => p.status === "Boar").length);
      setGiltsCount(breeders.filter(p => p.status === "Gilt").length);
      setPregnantCount(breeders.filter(p => p.status === "Pregnant").length);
      setLactatingCount(breeders.filter(p => p.status === "Lactating").length);
      setBreedersStarterCount(breeders.filter(p => p.status === "Starter" || p.status === "Piglet").length);
      setBreedersGrowerCount(breeders.filter(p => p.status === "Grower").length);
      setStarterCount(porkers.filter(p => p.status === "Starter" || p.status === "Piglet").length);
      setGrowerCount(porkers.filter(p => p.status === "Grower").length);
      setFinisherCount(porkers.filter(p => p.status === "Finisher").length);
    });

    return () => unsub();
  }, [activeFarmUid]);

  useEffect(() => {
    const rates = {
      Sows: { rate: 2.2, label: t("sowsCount") || "Sows" },
      Boars: { rate: 2.2, label: t("boarsCount") || "Boars" },
      Gilts: { rate: 2.2, label: t("giltsCount") || "Gilts" },
      Pregnant: { rate: 2.2, label: t("pregnantSows") || "Pregnant Sows" },
      Lactating: { rate: 5.5, label: t("lactatingSows") || "Lactating Sows" },
      BreedersStarter: { rate: 0.7, label: "Young Breeders (Starter)" },
      BreedersGrower: { rate: 1.8, label: "Young Breeders (Grower)" },
      Starter: { rate: 0.7, label: t("starterPiglets") || "Starter Piglets (Porker)" },
      Grower: { rate: 1.8, label: t("growers") || "Growers (Porker)" },
      Finisher: { rate: 2.5, label: t("finishers") || "Finishers" }
    };

    const breakdown: any[] = [];
    let grandDailyTotal = 0;

    const inputs = [
      { key: "Sows", count: sowsCount },
      { key: "Boars", count: boarsCount },
      { key: "Gilts", count: giltsCount },
      { key: "Pregnant", count: pregnantCount },
      { key: "Lactating", count: lactatingCount },
      { key: "BreedersStarter", count: breedersStarterCount },
      { key: "BreedersGrower", count: breedersGrowerCount },
      { key: "Starter", count: starterCount },
      { key: "Grower", count: growerCount },
      { key: "Finisher", count: finisherCount }
    ];

    inputs.forEach(inp => {
      if (inp.count > 0) {
        const rateObj = (rates as any)[inp.key];
        const dailyReq = inp.count * rateObj.rate;
        grandDailyTotal += dailyReq;
        breakdown.push({
          category: rateObj.label,
          count: inp.count,
          rate: rateObj.rate,
          dailyTotal: dailyReq,
          periodTotal: dailyReq * calcDays
        });
      }
    });

    setCalculationBreakdown(breakdown);
    setTotalDailyReq(grandDailyTotal);
    setTotalPeriodReq(grandDailyTotal * calcDays);
  }, [sowsCount, boarsCount, giltsCount, pregnantCount, lactatingCount, breedersStarterCount, breedersGrowerCount, starterCount, growerCount, finisherCount, calcDays, t]);

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-[#F8FAF9] dark:bg-[#121212] text-zinc-900 dark:text-zinc-100">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-amber-500 border-t-transparent"></div>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen bg-[#F8FAF9] dark:bg-[#121212] text-zinc-900 dark:text-zinc-100 flex flex-col font-sans overflow-x-hidden">
      <div className="relative z-10 flex flex-col min-h-screen print:hidden">
        {!isMobile && (
          <DesktopHeader
            showBack
            backPath="/dashboard?section=feed"
            label={t("calculator") || "FEED CALCULATOR"}
            labelColor="text-[#E65100] dark:text-[#FFB74D]"
          />
        )}

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8 space-y-6">
        <div className="flex items-center justify-between flex-wrap gap-4 border-b border-zinc-200 dark:border-zinc-800 pb-4">
          <div className="flex items-center gap-3">
            <Link
              href="/dashboard?section=feed"
              className="inline-flex items-center gap-2 px-3 py-2 rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-700 transition font-bold text-xs shadow-xs"
            >
              <ArrowLeftIcon className="h-4 w-4" />
              <span>{tCommon("back") || "Back"}</span>
            </Link>
            <div className="h-10 w-10 rounded-xl bg-[#FFF3E0] dark:bg-[#E65100]/30 border border-[#E65100]/30 flex items-center justify-center flex-shrink-0">
              <CalculateIcon className="h-5 w-5 text-[#E65100] dark:text-[#FFB74D]" />
            </div>
            <div>
              <h1 className="text-xl sm:text-2xl font-black text-[#E65100] dark:text-[#FFB74D]">
                {t("calculator") || "Calculator"}
              </h1>
              <p className="text-xs text-zinc-500 dark:text-zinc-400">{t("calculatorDesc") || "Calculate daily and periodic feed requirements"}</p>
            </div>
          </div>
        </div>

        {/* Mobile Quick Summary for Instant Visibility */}
        <div className="lg:hidden bg-amber-600 text-white rounded-2xl p-4 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-wider text-amber-100">Total Feed Needed ({calcDays} {calcDays === 1 ? "Day" : "Days"})</p>
            <p className="text-2xl font-black">{calcDays > 1 ? `${totalPeriodReq.toFixed(1)} kg` : `${totalDailyReq.toFixed(1)} kg`}</p>
          </div>
          <span className="text-xs bg-white/20 px-3 py-1.5 rounded-xl font-bold">
            {totalDailyReq.toFixed(1)} kg/day
          </span>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          <div className="lg:col-span-5 bg-amber-50/50 backdrop-blur-md border border-amber-200/80 rounded-2xl p-6 shadow-sm space-y-4">
            <h3 className="text-lg font-bold text-zinc-900">{t("numberOfPigs") || "Number of Pigs"}</h3>
            <p className="text-xs text-zinc-500">{t("calculatorPrompt") || "Pigs counts are preloaded from your herd database. Adjust if necessary."}</p>

            <div className="space-y-3">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-zinc-500 mb-1">{t("sowsCount") || "Sows"}</label>
                  <input type="number" min="0" value={sowsCount} onChange={(e) => setSowsCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm" />
                </div>
                <div>
                  <label className="block text-xs font-bold text-zinc-500 mb-1">{t("boarsCount") || "Boars"}</label>
                  <input type="number" min="0" value={boarsCount} onChange={(e) => setBoarsCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm" />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-zinc-500 mb-1">{t("giltsCount") || "Gilts"}</label>
                  <input type="number" min="0" value={giltsCount} onChange={(e) => setGiltsCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm" />
                </div>
                <div>
                  <label className="block text-xs font-bold text-zinc-500 mb-1">{t("pregnantSows") || "Pregnant Sows"}</label>
                  <input type="number" min="0" value={pregnantCount} onChange={(e) => setPregnantCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm" />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-zinc-500 mb-1">{t("lactatingSows") || "Lactating Sows"}</label>
                  <input type="number" min="0" value={lactatingCount} onChange={(e) => setLactatingCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm" />
                </div>
                <div>
                  <label className="block text-xs font-bold text-zinc-500 mb-1">Breeder Starters</label>
                  <input type="number" min="0" value={breedersStarterCount} onChange={(e) => setBreedersStarterCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm" />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-zinc-500 mb-1">Breeder Growers</label>
                  <input type="number" min="0" value={breedersGrowerCount} onChange={(e) => setBreedersGrowerCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm" />
                </div>
                <div>
                  <label className="block text-xs font-bold text-zinc-500 mb-1">{t("starterPiglets") || "Porker Starters"}</label>
                  <input type="number" min="0" value={starterCount} onChange={(e) => setStarterCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm" />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-zinc-500 mb-1">{t("growers") || "Growers"}</label>
                  <input type="number" min="0" value={growerCount} onChange={(e) => setGrowerCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm" />
                </div>
                <div>
                  <label className="block text-xs font-bold text-zinc-500 mb-1">{t("finishers") || "Finishers"}</label>
                  <input type="number" min="0" value={finisherCount} onChange={(e) => setFinisherCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm" />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-zinc-500 mb-1">{t("projectionPeriod") || "Duration (Days)"}</label>
                <input type="number" min="1" value={calcDays} onChange={(e) => setCalcDays(parseInt(e.target.value) || 1)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm" />
              </div>
            </div>
          </div>

          <div className="lg:col-span-7 bg-amber-50/50 backdrop-blur-md border border-amber-200/80 rounded-2xl p-6 shadow-sm flex flex-col justify-between">
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-bold text-zinc-900">{t("quantityNeeded") || "Quantity Needed"}</h3>
                <span className="text-xs bg-amber-50 text-amber-700 font-bold px-2.5 py-1 rounded-full border border-amber-200">
                  {calcDays} {calcDays === 1 ? "Day" : "Days"}
                </span>
              </div>

              {calculationBreakdown.length === 0 ? (
                <div className="py-12 text-center text-zinc-400">
                  <p className="text-sm">No pigs entered to calculate requirements.</p>
                </div>
              ) : (
                <div className="overflow-x-auto -mx-6 sm:mx-0">
                  <div className="inline-block min-w-full align-middle sm:px-0 px-6">
                    <table className="min-w-full divide-y divide-zinc-200 text-sm">
                      <thead>
                        <tr className="text-left text-xs font-semibold text-zinc-500 uppercase tracking-wider">
                          <th className="pb-3">{t("category") || "Category"}</th>
                          <th className="pb-3 text-right">{t("headCount") || "Head"}</th>
                          <th className="pb-3 text-right">{t("dailyIntake") || "Rate"}</th>
                          <th className="pb-3 text-right">{t("dailyTotal") || "Daily"}</th>
                          {calcDays > 1 && <th className="pb-3 text-right">{t("periodTotal") || `Total (${calcDays}d)`}</th>}
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-zinc-150">
                        {calculationBreakdown.map((row) => (
                          <tr key={row.category}>
                            <td className="py-3 font-semibold text-zinc-800">{row.category}</td>
                            <td className="py-3 text-right font-mono text-zinc-600">{row.count}</td>
                            <td className="py-3 text-right font-mono text-zinc-500">{row.rate.toFixed(1)} kg</td>
                            <td className="py-3 text-right font-mono font-bold text-amber-600">{row.dailyTotal.toFixed(1)} kg</td>
                            {calcDays > 1 && (
                              <td className="py-3 text-right font-mono font-bold text-zinc-900">{row.periodTotal.toFixed(1)} kg</td>
                            )}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>

            <div className="mt-8 pt-4 border-t border-zinc-200 flex items-center justify-between flex-wrap gap-4">
              <div>
                <p className="text-xs text-zinc-500 uppercase tracking-wider font-semibold">Grand Total</p>
                <p className="text-2xl font-black text-amber-600">
                  {calcDays > 1 ? `${totalPeriodReq.toFixed(1)} kg` : `${totalDailyReq.toFixed(1)} kg`}
                </p>
                {calcDays > 1 && (
                  <p className="text-xs text-zinc-500">({totalDailyReq.toFixed(1)} kg / day)</p>
                )}
              </div>

              <PremiumWrapper fallback={
                <button
                  onClick={() => setShowRewardedPassModal(true)}
                  className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-2.5 text-xs font-bold text-amber-800 shadow-sm flex items-center gap-2 hover:bg-amber-100 transition"
                >
                  <ExportPdfIcon className="h-4 w-4 text-amber-600" />
                  Export PDF (Premium)
                </button>
              }>
                <button
                  onClick={() => window.print()}
                  className="rounded-xl bg-amber-600 hover:bg-amber-700 px-5 py-2.5 text-xs font-bold text-white shadow-sm transition flex items-center gap-2"
                >
                  <ExportPdfIcon className="h-4 w-4" />
                  Print / Save PDF
                </button>
              </PremiumWrapper>
            </div>
          </div>
        </div>

        <NativeAdBanner />
      </main>
      </div>

      <RewardedPassModal
        isOpen={showRewardedPassModal}
        onClose={() => setShowRewardedPassModal(false)}
        featureName="Feed Calculator & PDF Export"
        onSuccess={() => {
          setTimeout(() => {
            window.print();
          }, 500);
        }}
      />

      <FeedRequirementsReport
        days={calcDays}
        breakdown={calculationBreakdown}
        totalDaily={totalDailyReq}
        totalPeriod={totalPeriodReq}
      />
    </div>
  );
}
