"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot, doc, addDoc, deleteDoc, query, orderBy } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import { useTranslations } from "next-intl";
import { ScienceIcon, ExportPdfIcon } from "@/components/icons/DashboardIcons";
import { FeedIngredient, NutritionalRequirement, SavedFeedRecipe } from "@/lib/types";
import { formulateFeed, FormulationResult } from "@/lib/feedCalculator";
import PremiumWrapper from "@/components/PremiumWrapper";
import NativeAdBanner from "@/components/ads/NativeAdBanner";
import SavedRecipesModal from "@/components/feed/SavedRecipesModal";
import SaveRecipeModal from "@/components/feed/SaveRecipeModal";
import IngredientsCatalogModal from "@/components/feed/IngredientsCatalogModal";
import BatchMixModal from "@/components/feed/BatchMixModal";

const ArrowLeftIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <line x1="19" y1="12" x2="5" y2="12" />
    <polyline points="12 19 5 12 12 5" />
  </svg>
);

const ChevronDownIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <polyline points="6 9 12 15 18 9" />
  </svg>
);

const defaultRequirements: NutritionalRequirement[] = [
  { stage: "Starter", digestibleProtein: 17.0, metabolizableEnergy: 3350.0, calcium: 0.90, phosphorus: 0.75, lysine: 7.90, methionineCystine: 5.20, tryptophan: 1.25, crudeFiber: 3.0, minDailyFeed: 0.35, maxDailyFeed: 0.85 },
  { stage: "Grower", digestibleProtein: 14.5, metabolizableEnergy: 3300.0, calcium: 0.75, phosphorus: 0.50, lysine: 6.10, methionineCystine: 4.00, tryptophan: 1.10, crudeFiber: 5.0, minDailyFeed: 0.75, maxDailyFeed: 1.50 },
  { stage: "Finisher", digestibleProtein: 13.0, metabolizableEnergy: 3300.0, calcium: 0.75, phosphorus: 0.50, lysine: 5.70, methionineCystine: 3.00, tryptophan: 1.00, crudeFiber: 6.0, minDailyFeed: 1.50, maxDailyFeed: 2.50 }
];

export default function MixFeedPage() {
  const t = useTranslations("Feed");
  const tCommon = useTranslations("Common");
  const { user, userProfile, activeFarmUid, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  const [ingredients, setIngredients] = useState<FeedIngredient[]>([]);
  const [requirements, setRequirements] = useState<NutritionalRequirement[]>(defaultRequirements);
  const [selectedStage, setSelectedStage] = useState("Grower");
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [formulation, setFormulation] = useState<FormulationResult | null>(null);
  const [formulatorError, setFormulatorError] = useState<string | null>(null);
  const [expandedCategory, setExpandedCategory] = useState<string | null>("Energy");

  // Saved Recipes states
  const [savedRecipes, setSavedRecipes] = useState<SavedFeedRecipe[]>([]);
  const [isSavedModalOpen, setIsSavedModalOpen] = useState(false);
  const [isSaveModalOpen, setIsSaveModalOpen] = useState(false);
  const [isBatchModalOpen, setIsBatchModalOpen] = useState(false);
  const [isCatalogOpen, setIsCatalogOpen] = useState(false);
  const [saveToast, setSaveToast] = useState<string | null>(null);

  const toggleCategory = (cat: string) => {
    setExpandedCategory(prev => (prev === cat ? null : cat));
  };

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
      
      const normalized = list.map(ing => {
        let cat = (ing.mainCategory || ing.category || "Uncategorized").trim();
        const l = cat.toLowerCase();
        if (l === "protein" || l === "proteins") cat = "Protein";
        else if (l === "energy" || l === "energies" || l === "energy source") cat = "Energy";
        else if (l.includes("vitamins") || l.includes("minerals") || l.includes("salt")) cat = "Vitamins, Minerals & Salt";
        return { ...ing, mainCategory: cat };
      });

      setIngredients(normalized);
    });

    return () => unsub();
  }, [activeFarmUid]);

  // Subscribe to saved recipes in real-time
  useEffect(() => {
    if (!activeFarmUid) return;
    const q = query(
      collection(db, "users", activeFarmUid, "saved_feed_recipes"),
      orderBy("timestamp", "desc")
    );
    const unsub = onSnapshot(q, (snapshot) => {
      const list = snapshot.docs.map((d) => ({
        id: d.id,
        ...d.data(),
      } as SavedFeedRecipe));
      setSavedRecipes(list);
    }, (err) => {
      console.warn("Error listening to saved recipes:", err);
    });

    return () => unsub();
  }, [activeFarmUid]);

  const calculatedCostPerKg = React.useMemo(() => {
    if (!formulation) return 0;
    const ings = formulation.ingredients || formulation.proportions || {};
    let totalCost = 0;
    let totalPct = 0;
    Object.entries(ings).forEach(([id, pct]) => {
      const ing = ingredients.find(i => i.id === id);
      if (ing && ing.costPerKg > 0) {
        totalCost += (pct / 100) * ing.costPerKg;
      }
      totalPct += pct;
    });
    return totalPct > 0 ? totalCost / (totalPct / 100) : 0;
  }, [formulation, ingredients]);

  const handleSaveRecipe = async (data: { name: string; notes: string; targetBatchKg: number }) => {
    if (!activeFarmUid || !formulation) return;
    const ingredientsMap = formulation.ingredients || formulation.proportions || {};
    const newRecipe = {
      name: data.name,
      stage: selectedStage,
      dateCreated: new Date().toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" }),
      ingredients: ingredientsMap,
      isPercentage: true,
      costPerKg: calculatedCostPerKg,
      targetBatchKg: data.targetBatchKg,
      notes: data.notes,
      timestamp: Date.now(),
    };

    await addDoc(collection(db, "users", activeFarmUid, "saved_feed_recipes"), newRecipe);
    setSaveToast(`Formulation "${data.name}" saved successfully!`);
    setTimeout(() => setSaveToast(null), 4000);
  };

  const handleDeleteRecipe = async (recipeId: string) => {
    if (!activeFarmUid || !recipeId) return;
    await deleteDoc(doc(db, "users", activeFarmUid, "saved_feed_recipes", recipeId));
  };

  const handleLoadRecipe = (recipe: SavedFeedRecipe) => {
    if (recipe.stage) {
      setSelectedStage(recipe.stage);
    }
    const ingIds = Object.keys(recipe.ingredients || {});
    if (ingIds.length > 0) {
      setSelectedIds(ingIds);
      const targetReq =
        requirements.find(
          req => req.stage.toLowerCase() === (recipe.stage || "Grower").toLowerCase()
        ) || defaultRequirements[1];
      const result = formulateFeed(targetReq, ingredients, ingIds);
      setFormulation(result);
      if (result.error) setFormulatorError(result.error);
      else setFormulatorError(null);
    }
    setSaveToast(`Loaded formulation "${recipe.name}"`);
    setTimeout(() => setSaveToast(null), 4000);
  };

  const handleToggleSelect = (id: string) => {
    setSelectedIds(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  };

  const handleFormulate = () => {
    setFormulatorError(null);
    const targetReq = requirements.find(req => req.stage.toLowerCase() === selectedStage.toLowerCase()) || defaultRequirements[1];
    const result = formulateFeed(targetReq, ingredients, selectedIds);
    setFormulation(result);
    if (result.error) setFormulatorError(result.error);
  };

  const renderAccordionGroup = (categoryName: string, list: FeedIngredient[]) => {
    const isExpanded = expandedCategory === categoryName;
    const selectedCount = list.filter(ing => selectedIds.includes(ing.id)).length;

    return (
      <div key={categoryName} className="border border-zinc-200 rounded-xl overflow-hidden bg-white/70 shadow-xs">
        <button
          type="button"
          onClick={() => toggleCategory(categoryName)}
          className="w-full flex items-center justify-between p-3.5 text-left bg-zinc-50/70 hover:bg-zinc-100/80 transition-colors"
        >
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-zinc-900">{categoryName}</span>
            <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-50 text-amber-800 border border-amber-200">
              {selectedCount} / {list.length}
            </span>
          </div>
          <ChevronDownIcon className={`h-4 w-4 text-zinc-500 transform transition-transform ${isExpanded ? "rotate-180" : ""}`} />
        </button>

        {isExpanded && (
          <div className="p-3 space-y-2 divide-y divide-zinc-100">
            {list.length === 0 ? (
              <p className="text-[11px] text-zinc-400 italic">No ingredients in this group.</p>
            ) : (
              list.map((ing) => (
                <div key={ing.id} className="flex items-center justify-between pt-2 first:pt-0">
                  <label className="flex items-center gap-2.5 cursor-pointer text-xs text-zinc-700">
                    <input
                      type="checkbox"
                      checked={selectedIds.includes(ing.id)}
                      onChange={() => handleToggleSelect(ing.id)}
                      className="h-4 w-4 rounded border-zinc-300 text-amber-600 focus:ring-amber-500"
                    />
                    <span>{ing.name}</span>
                  </label>
                  <span className="text-[10px] text-zinc-500 font-mono">CP: {ing.crudeProtein.toFixed(1)}%</span>
                </div>
              ))
            )}
          </div>
        )}
      </div>
    );
  };

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-[#F8FAF9] dark:bg-[#121212] text-zinc-900 dark:text-zinc-100">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-amber-500 border-t-transparent"></div>
      </div>
    );
  }

  const energyList = ingredients.filter(i => i.mainCategory === "Energy");
  const proteinList = ingredients.filter(i => i.mainCategory === "Protein");
  const supplementalList = ingredients.filter(i => i.mainCategory === "Vitamins, Minerals & Salt");

  return (
    <div className="relative min-h-screen bg-[#F8FAF9] dark:bg-[#121212] text-zinc-900 dark:text-zinc-100 flex flex-col font-sans overflow-x-hidden">
      {!isMobile && (
        <DesktopHeader
          showBack
          backPath="/dashboard?section=feed"
          label={t("mixFeed") || "FEED FORMULATOR"}
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
              <ScienceIcon className="h-5 w-5 text-[#E65100] dark:text-[#FFB74D]" />
            </div>
            <div>
              <h1 className="text-xl sm:text-2xl font-black text-[#E65100] dark:text-[#FFB74D]">
                {t("mixFeed") || "Mix Feed"}
              </h1>
              <p className="text-xs text-zinc-500 dark:text-zinc-400">{t("mixFeedDesc") || "Pearson square multi-nutrient balanced feed formulation"}</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setIsCatalogOpen(true)}
              className="px-3.5 py-2.5 rounded-xl bg-white hover:bg-zinc-50 border border-zinc-200 text-zinc-700 text-xs font-bold transition flex items-center gap-2 shadow-2xs active:scale-95"
            >
              <ScienceIcon className="h-4 w-4 text-amber-600" />
              <span>Ingredients ({ingredients.length})</span>
            </button>
            <button
              type="button"
              onClick={() => setIsSavedModalOpen(true)}
              className="px-4 py-2.5 rounded-xl bg-amber-50 hover:bg-amber-100 border border-amber-200/80 text-amber-900 text-xs font-bold transition flex items-center gap-2 shadow-2xs active:scale-95"
            >
              <svg className="h-4 w-4 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
              </svg>
              <span>Saved Formulations</span>
              {savedRecipes.length > 0 && (
                <span className="h-5 min-w-5 px-1.5 rounded-full bg-amber-600 text-white font-black text-[10px] flex items-center justify-center">
                  {savedRecipes.length}
                </span>
              )}
            </button>
          </div>
        </div>

        <PremiumWrapper fallback={
          <div className="p-12 text-center border border-zinc-200 rounded-2xl bg-zinc-50 space-y-4">
            <ScienceIcon className="h-12 w-12 text-amber-600 mx-auto opacity-70" />
            <h2 className="text-xl font-bold text-zinc-900">Mix Feed is a Premium Feature</h2>
            <p className="text-xs text-zinc-500 max-w-md mx-auto">Upgrade to SmartSwine Premium to generate balanced rations with automated Pearson square feed balancing.</p>
            <Link href="/dashboard/billing" className="inline-block px-6 py-2.5 bg-amber-600 text-white text-xs font-bold rounded-xl shadow">Upgrade to Premium</Link>
          </div>
        }>
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
            <div className="lg:col-span-5 bg-amber-50/50 backdrop-blur-md border border-amber-200/80 rounded-2xl p-6 shadow-sm space-y-6">
              <div>
                <label className="block text-xs font-bold text-zinc-500 uppercase tracking-wider mb-2">
                  Target Swine Stage
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {["Starter", "Grower", "Finisher"].map((stage) => (
                    <button
                      key={stage}
                      type="button"
                      onClick={() => setSelectedStage(stage)}
                      className={`py-2 text-xs font-bold rounded-xl border transition-all ${
                        selectedStage.toLowerCase() === stage.toLowerCase()
                          ? "bg-amber-600 text-white border-amber-600 shadow-sm"
                          : "bg-white text-zinc-700 border-zinc-200 hover:bg-zinc-100"
                      }`}
                    >
                      {stage}
                    </button>
                  ))}
                </div>
              </div>

              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <label className="block text-xs font-bold text-zinc-500 uppercase tracking-wider">
                    Available Ingredients ({selectedIds.length} selected)
                  </label>
                  <button
                    type="button"
                    onClick={() => setSelectedIds([])}
                    className="text-[11px] font-semibold text-amber-700 hover:underline"
                  >
                    Clear All
                  </button>
                </div>
                
                {renderAccordionGroup("Energy", energyList)}
                {renderAccordionGroup("Protein", proteinList)}
                {renderAccordionGroup("Vitamins, Minerals & Salt", supplementalList)}
              </div>

              <button
                type="button"
                onClick={handleFormulate}
                disabled={selectedIds.length === 0}
                className="w-full py-3 rounded-xl bg-amber-600 hover:bg-amber-700 disabled:opacity-50 text-white font-bold text-sm shadow transition"
              >
                {t("runFormulator") || "Calculate Formulation"}
              </button>

              {formulatorError && (
                <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl font-medium">
                  {formulatorError}
                </div>
              )}
            </div>

            <div className="lg:col-span-7 bg-amber-50/50 backdrop-blur-md border border-amber-200/80 rounded-2xl p-6 shadow-sm space-y-6">
              <div className="flex items-center justify-between flex-wrap gap-2">
                <h3 className="text-lg font-bold text-zinc-900">{t("mixingResults") || "Mixing Results"}</h3>
                {formulation && (
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
                      onClick={() => setIsSaveModalOpen(true)}
                      className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl flex items-center gap-1.5 shadow-xs transition active:scale-95"
                    >
                      <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
                      </svg>
                      <span>Save Formulation</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => window.print()}
                      className="px-3.5 py-1.5 bg-white border border-amber-200 text-xs font-bold text-amber-900 rounded-xl hover:bg-amber-50 flex items-center gap-1.5 shadow-xs transition"
                    >
                      <ExportPdfIcon className="h-3.5 w-3.5 text-amber-600" />
                      Print Recipe
                    </button>
                  </div>
                )}
              </div>

              {!formulation ? (
                <div className="py-16 text-center text-zinc-400">
                  <ScienceIcon className="h-10 w-10 mx-auto mb-2 opacity-50 text-amber-600" />
                  <p className="text-sm font-medium">Select ingredients and click &quot;Calculate Formulation&quot;.</p>
                </div>
              ) : (
                <div className="space-y-6">
                  <div className="bg-white/95 rounded-2xl border border-amber-200/80 p-5 space-y-3 shadow-xs">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-amber-900">Ingredient Inclusions (per 100 kg batch)</h4>
                    <div className="divide-y divide-zinc-100">
                      {Object.entries(formulation.ingredients || formulation.proportions || {}).map(([id, percent]) => {
                        const ing = ingredients.find(i => i.id === id);
                        return (
                          <div key={id} className="py-2.5 flex justify-between items-center text-xs">
                            <span className="font-semibold text-zinc-800">{ing?.name || id}</span>
                            <div className="flex items-center gap-3">
                              <span className="font-mono font-bold text-amber-600">{percent.toFixed(2)}%</span>
                              <span className="text-zinc-500 font-mono">({(percent * 10).toFixed(1)} kg/ton)</span>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>

                  <div className="bg-white/95 rounded-2xl border border-amber-200/80 p-5 space-y-3 shadow-xs">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-amber-900">Nutritional Comparison</h4>
                    <table className="min-w-full divide-y divide-zinc-200 text-xs">
                      <thead>
                        <tr className="text-left font-semibold text-zinc-500">
                          <th className="pb-2">Nutrient</th>
                          <th className="pb-2 text-right">Actual</th>
                          <th className="pb-2 text-right">Target</th>
                          <th className="pb-2 text-right">Status</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-zinc-150">
                        {formulation.nutritionalComparison.map(n => (
                          <tr key={n.label}>
                            <td className="py-2 font-medium text-zinc-700">{n.label}</td>
                            <td className="py-2 text-right font-mono font-bold text-zinc-900">{n.actual.toFixed(2)}</td>
                            <td className="py-2 text-right font-mono text-zinc-500">{n.target.toFixed(2)}</td>
                            <td className="py-2 text-right">
                              <span className={`font-bold ${n.isDeficient ? "text-rose-600" : "text-emerald-600"}`}>
                                {n.isDeficient ? "Deficient" : "OK"}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          </div>
        </PremiumWrapper>

        <NativeAdBanner />

        {/* Saved Recipes Browser Modal */}
        <SavedRecipesModal
          isOpen={isSavedModalOpen}
          onClose={() => setIsSavedModalOpen(false)}
          savedRecipes={savedRecipes}
          allIngredients={ingredients}
          currencySymbol={userProfile?.settings?.currencySymbol || "$"}
          onLoadRecipe={handleLoadRecipe}
          onDeleteRecipe={handleDeleteRecipe}
        />

        {/* Save Recipe Input Modal */}
        <SaveRecipeModal
          isOpen={isSaveModalOpen}
          onClose={() => setIsSaveModalOpen(false)}
          stage={selectedStage}
          ingredientsCount={Object.keys(formulation?.ingredients || formulation?.proportions || {}).length}
          costPerKg={calculatedCostPerKg}
          currencySymbol={userProfile?.settings?.currencySymbol || "$"}
          onSave={handleSaveRecipe}
        />

        {/* Ingredients Catalog Modal */}
        <IngredientsCatalogModal
          isOpen={isCatalogOpen}
          onClose={() => setIsCatalogOpen(false)}
          ingredients={ingredients}
          activeFarmUid={activeFarmUid}
          currencySymbol={userProfile?.settings?.currencySymbol || "$"}
          isPremium={Boolean(userProfile?.isPremium || userProfile?.isAdmin)}
        />

        {/* Batch Mix Modal */}
        <BatchMixModal
          isOpen={isBatchModalOpen}
          onClose={() => setIsBatchModalOpen(false)}
          recipeName={`${selectedStage} Feed Mix`}
          stage={selectedStage}
          defaultBatchKg={1000}
          requiredIngredients={Object.entries(formulation?.ingredients || formulation?.proportions || {}).map(([id, pct]) => {
            const ing = ingredients.find(i => i.id === id);
            return {
              id,
              name: ing?.name || id,
              percent: pct,
              costPerKg: ing?.costPerKg || 0,
            };
          })}
          activeFarmUid={activeFarmUid || ""}
          onSuccess={(msg) => {
            setSaveToast(msg);
            setTimeout(() => setSaveToast(null), 5000);
          }}
        />

        {/* Toast confirmation */}
        {saveToast && (
          <div className="fixed bottom-6 right-6 z-50 py-3 px-4.5 bg-zinc-900 text-white text-xs font-bold rounded-2xl shadow-2xl flex items-center gap-2.5 animate-in fade-in slide-in-from-bottom-2 duration-200">
            <svg className="h-4 w-4 text-emerald-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M5 13l4 4L19 7" />
            </svg>
            <span>{saveToast}</span>
          </div>
        )}
      </main>
    </div>
  );
}
