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
import { AnalyticsIcon, ScienceIcon, ExportPdfIcon } from "@/components/icons/DashboardIcons";
import { FeedIngredient, NutritionalRequirement } from "@/lib/types";
import { analyzeFeedMix, checkIngredientSafety, FeedNutrientProfile, InclusionSafetyAlert } from "@/lib/feedCalculator";
import { TierLimiter } from "@/lib/tierLimiter";
import NativeAdBanner from "@/components/ads/NativeAdBanner";
import RewardedPassModal from "@/components/ads/RewardedPassModal";
import IngredientsCatalogModal from "@/components/feed/IngredientsCatalogModal";
import BatchMixModal from "@/components/feed/BatchMixModal";

const ArrowLeftIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <line x1="19" y1="12" x2="5" y2="12" />
    <polyline points="12 19 5 12 12 5" />
  </svg>
);

const PlusIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <line x1="12" y1="5" x2="12" y2="19" />
    <line x1="5" y1="12" x2="19" y2="12" />
  </svg>
);

const TrashIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <polyline points="3 6 5 6 21 6" />
    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
  </svg>
);

const defaultRequirements: Record<string, NutritionalRequirement> = {
  starter: { stage: "Starter", digestibleProtein: 17.0, metabolizableEnergy: 3350.0, calcium: 0.90, phosphorus: 0.75, lysine: 7.90, methionineCystine: 5.20, tryptophan: 1.25, crudeFiber: 3.0, minDailyFeed: 0.35, maxDailyFeed: 0.85 },
  grower: { stage: "Grower", digestibleProtein: 14.5, metabolizableEnergy: 3300.0, calcium: 0.75, phosphorus: 0.50, lysine: 6.10, methionineCystine: 4.00, tryptophan: 1.10, crudeFiber: 5.0, minDailyFeed: 0.75, maxDailyFeed: 1.50 },
  finisher: { stage: "Finisher", digestibleProtein: 13.0, metabolizableEnergy: 3300.0, calcium: 0.75, phosphorus: 0.50, lysine: 5.70, methionineCystine: 3.00, tryptophan: 1.00, crudeFiber: 6.0, minDailyFeed: 1.50, maxDailyFeed: 2.50 },
  sow: { stage: "Sow", digestibleProtein: 13.0, metabolizableEnergy: 3100.0, calcium: 0.85, phosphorus: 0.65, lysine: 6.00, methionineCystine: 3.50, tryptophan: 1.10, crudeFiber: 7.0, minDailyFeed: 2.00, maxDailyFeed: 2.50 },
  lactating: { stage: "Lactating", digestibleProtein: 16.5, metabolizableEnergy: 3400.0, calcium: 0.95, phosphorus: 0.75, lysine: 8.50, methionineCystine: 5.00, tryptophan: 1.30, crudeFiber: 5.0, minDailyFeed: 4.50, maxDailyFeed: 6.50 },
  boar: { stage: "Boar", digestibleProtein: 13.5, metabolizableEnergy: 3150.0, calcium: 0.85, phosphorus: 0.65, lysine: 6.20, methionineCystine: 3.60, tryptophan: 1.10, crudeFiber: 7.0, minDailyFeed: 2.00, maxDailyFeed: 2.50 }
};

export default function AnalyzeFeedPage() {
  const t = useTranslations("Feed");
  const tCommon = useTranslations("Common");
  const { user, userProfile, activeFarmUid, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  const isPremium = Boolean(userProfile?.isPremium || userProfile?.isAdmin);

  const [ingredients, setIngredients] = useState<FeedIngredient[]>([]);
  const [analyzePercentageMode, setAnalyzePercentageMode] = useState<boolean>(false);
  const [analyzeTargetStage, setAnalyzeTargetStage] = useState<string>("Grower");
  const [analyzeItems, setAnalyzeItems] = useState<{ ingredient: FeedIngredient; quantity: number }[]>([]);
  const [showAddIngredientModal, setShowAddIngredientModal] = useState<boolean>(false);
  const [ingredientSearchQuery, setIngredientSearchQuery] = useState<string>("");
  const [showRewardedPassModal, setShowRewardedPassModal] = useState<boolean>(false);
  const [isCatalogOpen, setIsCatalogOpen] = useState<boolean>(false);
  const [isBatchModalOpen, setIsBatchModalOpen] = useState<boolean>(false);
  const [batchToast, setBatchToast] = useState<string | null>(null);

  const ingredientLimitReached = !isPremium && analyzeItems.length >= TierLimiter.FREE_MAX_FEED_INGREDIENTS;

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!activeFarmUid) return;
    const ingQuery = collection(db, "users", activeFarmUid, "feed_ingredients");
    const unsub = onSnapshot(ingQuery, (snapshot) => {
      const redundantNames = ["barleyb", "dried brewers grain", "full fat soybean"];
      const list = snapshot.docs
        .map(doc => ({ id: doc.id, ...doc.data() } as FeedIngredient))
        .filter(ing => !redundantNames.includes(ing.name.toLowerCase().trim()));
      setIngredients(list);
    });

    return () => unsub();
  }, [activeFarmUid]);

  const handleAddIngredient = (ing: FeedIngredient) => {
    if (analyzeItems.some(i => i.ingredient.id === ing.id)) return;
    if (ingredientLimitReached) {
      setShowAddIngredientModal(false);
      setShowRewardedPassModal(true);
      return;
    }
    const initialQty = analyzePercentageMode ? 10 : 50;
    setAnalyzeItems(prev => [...prev, { ingredient: ing, quantity: initialQty }]);
    setShowAddIngredientModal(false);
  };

  const handleUpdateQuantity = (id: string, qty: number) => {
    setAnalyzeItems(prev =>
      prev.map(item => (item.ingredient.id === id ? { ...item, quantity: Math.max(0, qty) } : item))
    );
  };

  const handleRemoveIngredient = (id: string) => {
    setAnalyzeItems(prev => prev.filter(item => item.ingredient.id !== id));
  };

  const analyzeProfile = analyzeFeedMix(
    analyzeItems.map(item => ({ ingredient: item.ingredient, quantity: item.quantity })),
    analyzePercentageMode
  );

  const safetyAlerts = checkIngredientSafety(
    analyzeItems.map(item => ({ ingredient: item.ingredient, quantity: item.quantity })),
    analyzeTargetStage
  );

  const benchmarkReq = defaultRequirements[analyzeTargetStage.toLowerCase()] || defaultRequirements.grower;

  const totalInputQty = analyzeItems.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0);

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-[#F8FAF9] dark:bg-[#121212] text-zinc-900 dark:text-zinc-100">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-amber-500 border-t-transparent"></div>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen bg-[#F8FAF9] dark:bg-[#121212] text-zinc-900 dark:text-zinc-100 flex flex-col font-sans overflow-x-hidden">
      {!isMobile && (
        <DesktopHeader
          showBack
          backPath="/dashboard?section=feed"
          label={t("analyzeFeed") || "ANALYZE FEED"}
          labelColor="text-[#E65100] dark:text-[#FFB74D]"
        />
      )}

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8 space-y-8">
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
              <AnalyticsIcon className="h-5 w-5 text-[#E65100] dark:text-[#FFB74D]" />
            </div>
            <div>
              <h1 className="text-xl sm:text-2xl font-black text-[#E65100] dark:text-[#FFB74D]">
                {t("analyzeFeed") || "Analyze Feed"}
              </h1>
              <p className="text-xs text-zinc-500 dark:text-zinc-400">{t("analyzeFeedDesc") || "Calculate resulting nutritional profile of your custom feed mix"}</p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => setIsCatalogOpen(true)}
            className="px-3.5 py-2 rounded-xl bg-amber-50 hover:bg-amber-100 border border-amber-200/80 text-amber-900 text-xs font-bold transition flex items-center gap-2 shadow-2xs active:scale-95"
          >
            <ScienceIcon className="h-4 w-4 text-amber-600" />
            <span>Ingredients ({ingredients.length})</span>
          </button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          <div className="lg:col-span-5 space-y-6">
            <div className="bg-zinc-50/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 space-y-4 shadow-sm">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-zinc-900">Input Mode</h3>
                  <p className="text-xs text-zinc-500">Weight (kg) or Percentage (%)</p>
                </div>
                <div className="inline-flex bg-zinc-200/80 p-1 rounded-xl">
                  <button
                    type="button"
                    onClick={() => setAnalyzePercentageMode(false)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
                      !analyzePercentageMode ? "bg-amber-600 text-white shadow-sm" : "text-zinc-600 hover:text-zinc-900"
                    }`}
                  >
                    Weight (kg)
                  </button>
                  <button
                    type="button"
                    onClick={() => setAnalyzePercentageMode(true)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
                      analyzePercentageMode ? "bg-amber-600 text-white shadow-sm" : "text-zinc-600 hover:text-zinc-900"
                    }`}
                  >
                    Percentage (%)
                  </button>
                </div>
              </div>

              <div className="pt-3 border-t border-zinc-200">
                <label className="block text-xs font-bold text-zinc-500 uppercase tracking-wider mb-2">
                  Benchmark Growth Stage
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {["Starter", "Grower", "Finisher", "Sow", "Lactating", "Boar"].map((stage) => (
                    <button
                      key={stage}
                      type="button"
                      onClick={() => setAnalyzeTargetStage(stage)}
                      className={`py-2 text-xs font-bold rounded-xl border transition-all ${
                        analyzeTargetStage.toLowerCase() === stage.toLowerCase()
                          ? "bg-amber-600 text-white border-amber-600 shadow-sm"
                          : "bg-white text-zinc-700 border-zinc-200 hover:bg-zinc-100"
                      }`}
                    >
                      {stage}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <div className="bg-zinc-50/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 space-y-4 shadow-sm">
              <div className="flex items-center justify-between">
                <h3 className="text-xs font-bold text-zinc-500 uppercase tracking-wider">
                  Ingredients in Mix ({analyzeItems.length})
                </h3>
                <button
                  type="button"
                  onClick={() => {
                    if (ingredientLimitReached) {
                      alert(`Ingredient limit of ${TierLimiter.FREE_MAX_FEED_INGREDIENTS} reached for free tier. Upgrade to SmartSwine Premium to analyze full feed formulas with unlimited ingredients.`);
                      router.push("/dashboard/billing");
                      return;
                    }
                    setShowAddIngredientModal(true);
                  }}
                  className={`px-3 py-1.5 text-white text-xs font-bold rounded-xl transition flex items-center gap-1 shadow-sm ${
                    ingredientLimitReached
                      ? "bg-amber-700 hover:bg-amber-800"
                      : "bg-amber-600 hover:bg-amber-700"
                  }`}
                >
                  <PlusIcon className="h-3.5 w-3.5" />
                  {ingredientLimitReached ? `Limit (${TierLimiter.FREE_MAX_FEED_INGREDIENTS}) - Upgrade` : "Add Ingredient"}
                </button>
              </div>

              {analyzeItems.length === 0 ? (
                <div className="py-10 text-center text-zinc-400">
                  <ScienceIcon className="h-10 w-10 mx-auto mb-2 opacity-50 text-amber-600" />
                  <p className="text-xs font-medium">No ingredients added yet.</p>
                  <p className="text-[11px] text-zinc-400 mt-0.5">Click &quot;+ Add Ingredient&quot; to begin analysis.</p>
                </div>
              ) : (
                <div className="space-y-2.5">
                  {analyzeItems.map((item) => (
                    <div
                      key={item.ingredient.id}
                      className="p-3 bg-white border border-zinc-200 rounded-xl flex items-center justify-between gap-3 shadow-xs"
                    >
                      <div className="min-w-0 flex-1">
                        <p className="text-xs font-bold text-zinc-900 truncate">{item.ingredient.name}</p>
                        <p className="text-[10px] text-zinc-500">CP: {item.ingredient.crudeProtein}% • ME: {item.ingredient.metabolizableEnergy} kcal</p>
                      </div>

                      <div className="flex items-center gap-2">
                        <div className="relative w-24">
                          <input
                            type="number"
                            step="0.1"
                            min="0"
                            value={item.quantity || ""}
                            onChange={(e) => handleUpdateQuantity(item.ingredient.id, parseFloat(e.target.value) || 0)}
                            className="w-full text-right pr-7 pl-2 py-1 text-xs font-bold border border-zinc-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-amber-500"
                          />
                          <span className="absolute right-2 top-1/2 -translate-y-1/2 text-[10px] text-zinc-400 font-bold">
                            {analyzePercentageMode ? "%" : "kg"}
                          </span>
                        </div>

                        <button
                          type="button"
                          onClick={() => handleRemoveIngredient(item.ingredient.id)}
                          className="p-1.5 text-zinc-400 hover:text-rose-600 rounded-lg transition"
                        >
                          <TrashIcon className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                  ))}

                  <div className="pt-3 border-t border-zinc-200 flex justify-between items-center text-xs font-bold text-zinc-900">
                    <span>Total {analyzePercentageMode ? "Percentage" : "Batch Weight"}</span>
                    <span className="font-mono text-amber-600 text-sm">
                      {totalInputQty.toFixed(1)} {analyzePercentageMode ? "%" : "kg"}
                    </span>
                  </div>
                </div>
              )}
            </div>
          </div>

          <div className="lg:col-span-7 bg-zinc-50/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-6">
            <div className="flex items-center justify-between flex-wrap gap-2">
              <div>
                <h3 className="text-lg font-bold text-zinc-900">Nutrient Breakdown</h3>
                <p className="text-xs text-zinc-500">Benchmark comparison for {analyzeTargetStage}</p>
              </div>
              {analyzeItems.length > 0 && (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setIsBatchModalOpen(true)}
                    className="px-3.5 py-1.5 bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold rounded-xl flex items-center gap-1.5 shadow-xs transition active:scale-95"
                  >
                    <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                    </svg>
                    <span>Mix Batch</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      if (!isPremium) {
                        setShowRewardedPassModal(true);
                        return;
                      }
                      window.print();
                    }}
                    className="px-3.5 py-1.5 bg-white border border-zinc-200 text-xs font-bold text-zinc-700 rounded-xl hover:bg-zinc-100 flex items-center gap-1.5 shadow-xs"
                  >
                    <ExportPdfIcon className="h-3.5 w-3.5 text-zinc-500" />
                    Print Analysis
                  </button>
                </div>
              )}
            </div>

            {/* Inclusion Safety Alerts */}
            {safetyAlerts.length > 0 && (
              <div className="space-y-2">
                {safetyAlerts.map((alert, idx) => (
                  <div key={idx} className="p-3 bg-amber-50 border border-amber-200/90 rounded-xl flex items-start gap-2.5 shadow-2xs">
                    <span className="text-base shrink-0 mt-0.5">⚠️</span>
                    <div className="text-xs">
                      <p className="font-bold text-amber-900">
                        {alert.ingredientName}: {alert.currentPercent.toFixed(1)}% (Max Recommended: {alert.maxAllowedPercent.toFixed(1)}%)
                      </p>
                      <p className="text-amber-800 text-[11px] mt-0.5">{alert.riskDescription}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {analyzeItems.length === 0 ? (
              <div className="py-20 text-center text-zinc-400">
                <AnalyticsIcon className="h-12 w-12 mx-auto mb-2 opacity-40 text-amber-600" />
                <p className="text-sm font-medium">Add ingredients on the left to see live nutritional results.</p>
              </div>
            ) : (
              <div className="overflow-x-auto -mx-6 sm:mx-0">
                <div className="inline-block min-w-full align-middle sm:px-0 px-6">
                  <table className="min-w-full divide-y divide-zinc-200 text-xs">
                    <thead>
                      <tr className="text-left font-semibold text-zinc-500">
                        <th className="pb-3">Nutrient</th>
                        <th className="pb-3 text-right">Actual Level</th>
                        <th className="pb-3 text-right">Target Level</th>
                        <th className="pb-3 text-right">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-150">
                      {[
                        { label: "Crude Protein", actual: analyzeProfile.crudeProtein, target: benchmarkReq.digestibleProtein / 0.85, unit: "%", higherBetter: true },
                        { label: "Digestible Protein", actual: analyzeProfile.digestibleProtein, target: benchmarkReq.digestibleProtein, unit: "%", higherBetter: true },
                        { label: "Metabolizable Energy (ME)", actual: analyzeProfile.metabolizableEnergy, target: benchmarkReq.metabolizableEnergy, unit: "kcal/kg", higherBetter: true },
                        { label: "Crude Fiber", actual: analyzeProfile.crudeFiber, target: benchmarkReq.crudeFiber, unit: "%", higherBetter: false },
                        { label: "Calcium (Ca)", actual: analyzeProfile.calcium, target: benchmarkReq.calcium, unit: "%", higherBetter: true },
                        { label: "Phosphorus (P)", actual: analyzeProfile.phosphorus, target: benchmarkReq.phosphorus, unit: "%", higherBetter: true },
                        { label: "Lysine", actual: analyzeProfile.lysine, target: benchmarkReq.lysine, unit: "%", higherBetter: true },
                        { label: "Methionine + Cystine", actual: analyzeProfile.methionine, target: benchmarkReq.methionineCystine, unit: "%", higherBetter: true },
                      ].map((row) => {
                        const isDeficient = row.higherBetter ? row.actual < row.target * 0.95 : row.actual > row.target * 1.05;
                        return (
                          <tr key={row.label}>
                            <td className="py-3 font-semibold text-zinc-700 whitespace-nowrap pr-4">{row.label}</td>
                            <td className="py-3 text-right font-mono font-bold text-zinc-900">
                              {row.actual.toFixed(2)} {row.unit}
                            </td>
                            <td className="py-3 text-right font-mono text-zinc-500">
                              {row.target.toFixed(2)} {row.unit}
                            </td>
                            <td className="py-3 text-right font-semibold">
                              <span className={isDeficient ? "text-rose-600 font-bold" : "text-emerald-600 font-bold"}>
                                {isDeficient ? "Deficient" : "OK"}
                              </span>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        </div>

        <NativeAdBanner />
      </main>

      {showAddIngredientModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-xl border border-zinc-200">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-zinc-900">Select Ingredient</h3>
              <button
                onClick={() => setShowAddIngredientModal(false)}
                className="text-zinc-400 hover:text-zinc-700 text-sm font-bold"
              >
                ✕
              </button>
            </div>

            <input
              type="text"
              placeholder="Search ingredient name..."
              value={ingredientSearchQuery}
              onChange={(e) => setIngredientSearchQuery(e.target.value)}
              className="w-full px-3 py-2 text-xs border border-zinc-200 rounded-xl focus:outline-none focus:ring-1 focus:ring-amber-500"
            />

            <div className="max-h-60 overflow-y-auto divide-y divide-zinc-100">
              {ingredients
                .filter(i => i.name.toLowerCase().includes(ingredientSearchQuery.toLowerCase()))
                .map(ing => {
                  const already = analyzeItems.some(i => i.ingredient.id === ing.id);
                  return (
                    <button
                      key={ing.id}
                      type="button"
                      disabled={already}
                      onClick={() => handleAddIngredient(ing)}
                      className="w-full py-2.5 px-2 flex justify-between items-center text-left hover:bg-amber-50 disabled:opacity-40 rounded-lg transition"
                    >
                      <div>
                        <p className="text-xs font-bold text-zinc-900">{ing.name}</p>
                        <p className="text-[10px] text-zinc-500">{ing.mainCategory} • CP: {ing.crudeProtein}%</p>
                      </div>
                      {already ? (
                        <span className="text-[10px] text-zinc-400 font-semibold">Added</span>
                      ) : (
                        <span className="text-xs text-amber-600 font-bold">+ Add</span>
                      )}
                    </button>
                  );
                })}
            </div>
          </div>
        </div>
      )}

      <IngredientsCatalogModal
        isOpen={isCatalogOpen}
        onClose={() => setIsCatalogOpen(false)}
        ingredients={ingredients}
        activeFarmUid={activeFarmUid}
        currencySymbol={userProfile?.settings?.currencySymbol || "$"}
        isPremium={isPremium}
        onRequestRewardedPass={() => setShowRewardedPassModal(true)}
      />

      <RewardedPassModal
        isOpen={showRewardedPassModal}
        onClose={() => setShowRewardedPassModal(false)}
        featureName="Feed Analysis & Full Formulas"
      />

      {/* Batch Mix Modal */}
      <BatchMixModal
        isOpen={isBatchModalOpen}
        onClose={() => setIsBatchModalOpen(false)}
        recipeName={`${analyzeTargetStage} Feed Mix`}
        stage={analyzeTargetStage}
        defaultBatchKg={analyzePercentageMode ? 1000 : (totalInputQty > 0 ? totalInputQty : 1000)}
        requiredIngredients={(() => {
          const sumWeight = analyzeItems.reduce((s, it) => s + it.quantity, 0) || 1;
          return analyzeItems.map(item => ({
            id: item.ingredient.id,
            name: item.ingredient.name,
            percent: analyzePercentageMode ? item.quantity : (item.quantity / sumWeight) * 100,
            costPerKg: item.ingredient.costPerKg || 0,
          }));
        })()}
        activeFarmUid={activeFarmUid || ""}
        onSuccess={(msg) => {
          setBatchToast(msg);
          setTimeout(() => setBatchToast(null), 5000);
        }}
      />

      {/* Toast confirmation */}
      {batchToast && (
        <div className="fixed bottom-6 right-6 z-50 py-3 px-4.5 bg-zinc-900 text-white text-xs font-bold rounded-2xl shadow-2xl flex items-center gap-2.5 animate-in fade-in slide-in-from-bottom-2 duration-200">
          <svg className="h-4 w-4 text-emerald-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M5 13l4 4L19 7" />
          </svg>
          <span>{batchToast}</span>
        </div>
      )}
    </div>
  );
}
