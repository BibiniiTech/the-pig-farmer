"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import {
  formulateFeed,
  FeedIngredient,
  NutritionalRequirement,
  FormulationResult
} from "@/lib/feedCalculator";

const defaultRequirements: NutritionalRequirement[] = [
  { stage: "Starter", digestibleProtein: 17.0, metabolizableEnergy: 3350.0, calcium: 0.90, phosphorus: 0.75, lysine: 7.90, methionineCystine: 5.20, tryptophan: 1.25, crudeFiber: 3.0, minDailyFeed: 0.35, maxDailyFeed: 0.85 },
  { stage: "Grower", digestibleProtein: 14.5, metabolizableEnergy: 3300.0, calcium: 0.75, phosphorus: 0.50, lysine: 6.10, methionineCystine: 4.00, tryptophan: 1.10, crudeFiber: 5.0, minDailyFeed: 0.75, maxDailyFeed: 1.50 },
  { stage: "Finisher", digestibleProtein: 13.0, metabolizableEnergy: 3300.0, calcium: 0.75, phosphorus: 0.50, lysine: 5.70, methionineCystine: 3.00, tryptophan: 1.00, crudeFiber: 6.0, minDailyFeed: 1.50, maxDailyFeed: 2.50 }
];

export default function FeedFormulatorPage() {
  const { user, activeFarmUid, loading } = useAuth();
  const router = useRouter();

  const [ingredients, setIngredients] = useState<FeedIngredient[]>([]);
  const [requirements, setRequirements] = useState<NutritionalRequirement[]>(defaultRequirements);
  const [dataLoading, setDataLoading] = useState(true);

  // Formulator state
  const [selectedStage, setSelectedStage] = useState("Starter");
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [formulation, setFormulation] = useState<FormulationResult | null>(null);
  const [formulatorError, setFormulatorError] = useState<string | null>(null);

  // Group collapses
  const [energyCollapsed, setEnergyCollapsed] = useState(false);
  const [proteinCollapsed, setProteinCollapsed] = useState(false);
  const [mineralsCollapsed, setMineralsCollapsed] = useState(false);

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!activeFarmUid) return;

    setDataLoading(true);

    // 1. Listen to Ingredients
    const ingredientsQuery = collection(db, "users", activeFarmUid, "feed_ingredients");
    const unsubscribeIngredients = onSnapshot(ingredientsQuery, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as FeedIngredient));
      // Sort: Visible first, then by name
      setIngredients(list.sort((a, b) => a.name.localeCompare(b.name)));
    });

    // 2. Listen to Requirements
    const reqQuery = collection(db, "users", activeFarmUid, "nutritional_requirements");
    const unsubscribeReq = onSnapshot(reqQuery, (snapshot) => {
      if (!snapshot.empty) {
        const list = snapshot.docs.map(doc => doc.data() as NutritionalRequirement);
        setRequirements(list);
      }
      setDataLoading(false);
    });

    return () => {
      unsubscribeIngredients();
      unsubscribeReq();
    };
  }, [activeFarmUid]);

  const handleToggleSelect = (id: string) => {
    setSelectedIds(prev =>
      prev.includes(id) ? prev.filter(item => item !== id) : [...prev, id]
    );
  };

  const handleSelectAll = (category: string) => {
    const catIds = ingredients
      .filter(ing => ing.mainCategory === category)
      .map(ing => ing.id);
    
    const allSelected = catIds.every(id => selectedIds.includes(id));
    if (allSelected) {
      setSelectedIds(prev => prev.filter(id => !catIds.includes(id)));
    } else {
      setSelectedIds(prev => Array.from(new Set([...prev, ...catIds])));
    }
  };

  const handleFormulate = () => {
    setFormulatorError(null);
    const targetReq = requirements.find(req => req.stage === selectedStage);
    if (!targetReq) {
      setFormulatorError("Target stage configuration not found.");
      return;
    }

    const result = formulateFeed(targetReq, ingredients, selectedIds);
    setFormulation(result);
    if (result.error) {
      setFormulatorError(result.error);
    }
  };

  const renderGroupList = (category: string, collapsed: boolean, setCollapsed: (val: boolean) => void) => {
    const list = ingredients.filter(ing => ing.mainCategory === category);
    const catIds = list.map(ing => ing.id);
    const allSelected = catIds.length > 0 && catIds.every(id => selectedIds.includes(id));

    return (
      <div className="bg-zinc-900/40 border border-zinc-900 rounded-2xl overflow-hidden">
        <div className="bg-zinc-900/60 px-5 py-4 flex items-center justify-between">
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="flex items-center gap-3 font-bold text-white text-sm"
          >
            <span>{collapsed ? "▶" : "▼"}</span>
            <span>{category} ({list.length})</span>
          </button>

          <button
            onClick={() => handleSelectAll(category)}
            className="text-xs font-semibold text-emerald-400 hover:text-emerald-300"
          >
            {allSelected ? "Deselect All" : "Select All"}
          </button>
        </div>

        {!collapsed && (
          <div className="divide-y divide-zinc-900/50 p-4 space-y-2">
            {list.length === 0 ? (
              <p className="text-xs text-zinc-500 italic py-2">No ingredients configured in this group.</p>
            ) : (
              list.map((ing) => (
                <div key={ing.id} className="flex items-center justify-between py-2.5">
                  <label className="flex items-center gap-3 cursor-pointer select-none text-sm text-zinc-300">
                    <input
                      type="checkbox"
                      checked={selectedIds.includes(ing.id)}
                      onChange={() => handleToggleSelect(ing.id)}
                      className="h-4.5 w-4.5 rounded border-zinc-700 bg-zinc-950 text-emerald-500 focus:ring-emerald-500 focus:ring-offset-zinc-950"
                    />
                    <span>{ing.name}</span>
                  </label>
                  <span className="text-xs text-zinc-500 font-mono">CP: {ing.crudeProtein.toFixed(1)}%</span>
                </div>
              ))
            )}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 flex flex-col font-sans">
      <header className="border-b border-zinc-900 bg-zinc-900/40 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href="/dashboard" className="text-zinc-400 hover:text-white transition">
              ← Dashboard
            </Link>
            <span className="text-zinc-700">|</span>
            <span className="font-bold text-lg text-white flex items-center gap-2">
              🧪 Feed Formulator
            </span>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => window.print()}
              disabled={!formulation}
              className="rounded-lg border border-zinc-800 bg-zinc-900/40 px-4 py-2 text-xs font-semibold text-zinc-300 hover:bg-zinc-800 hover:text-white transition disabled:opacity-50"
            >
              🖨️ Print Formula
            </button>
            <button
              onClick={handleFormulate}
              className="rounded-lg bg-emerald-500 hover:bg-emerald-600 px-6 py-2 text-xs font-bold text-white shadow-lg shadow-emerald-500/15 transition"
            >
              Formulate Feed
            </button>
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8 grid grid-cols-1 lg:grid-cols-12 gap-8 print:p-0 print:bg-white print:text-black">
        {/* Left Side: Setup Panel */}
        <div className="lg:col-span-5 space-y-6 print:hidden">
          {/* Stage selection */}
          <div className="bg-zinc-900/30 border border-zinc-900 rounded-2xl p-6 space-y-3">
            <h3 className="text-sm font-bold text-zinc-400 uppercase tracking-wider">Target Growth Stage</h3>
            <select
              value={selectedStage}
              onChange={(e) => setSelectedStage(e.target.value)}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2.5 text-sm text-white focus:border-emerald-500 focus:outline-none"
            >
              {requirements.map((req) => (
                <option key={req.stage} value={req.stage}>
                  {req.stage} (CP target: {req.digestibleProtein}%)
                </option>
              ))}
            </select>
          </div>

          {/* Group lists */}
          {dataLoading ? (
            <div className="space-y-4">
              <div className="h-16 bg-zinc-900 animate-pulse rounded-2xl" />
              <div className="h-16 bg-zinc-900 animate-pulse rounded-2xl" />
            </div>
          ) : (
            <div className="space-y-4">
              <h3 className="text-sm font-bold text-zinc-400 uppercase tracking-wider px-2">Select Ingredients</h3>
              {renderGroupList("Energy", energyCollapsed, setEnergyCollapsed)}
              {renderGroupList("Protein", proteinCollapsed, setProteinCollapsed)}
              {renderGroupList("Vitamins, Minerals & Salt", mineralsCollapsed, setMineralsCollapsed)}
            </div>
          )}
        </div>

        {/* Right Side: Results Display */}
        <div className="lg:col-span-7 space-y-6 print:w-full print:block">
          {formulatorError && (
            <div className="rounded-xl border border-red-500/20 bg-red-500/5 p-4 text-sm text-red-400 print:hidden">
              {formulatorError}
            </div>
          )}

          {!formulation ? (
            <div className="h-96 flex flex-col items-center justify-center border border-dashed border-zinc-800 rounded-2xl text-center p-6 print:hidden">
              <span className="text-3xl mb-3">🧪</span>
              <p className="font-semibold text-zinc-400 text-sm">No active formulation</p>
              <p className="text-xs text-zinc-500 max-w-xs mt-1">
                Select your ingredients on the left and click &quot;Formulate Feed&quot; to compute balances.
              </p>
            </div>
          ) : (
            <div className="space-y-6 print:text-black">
              {/* Formula Composition */}
              <div className="bg-zinc-900/30 border border-zinc-900 rounded-2xl p-6 print:border-none print:p-0">
                <h3 className="text-lg font-bold text-white mb-4 print:text-black">
                  Feed Formula: {selectedStage} Mix
                </h3>
                <div className="space-y-4">
                  <div className="divide-y divide-zinc-900">
                    {Object.entries(formulation.ingredients).map(([id, percent]) => {
                      const ing = ingredients.find(i => i.id === id || i.name === id);
                      const displayName = ing ? ing.name : id;
                      const batchWeight = (percent / 100.0) * 1000; // kg in a 1-ton batch
                      return (
                        <div key={id} className="py-3 flex justify-between text-sm">
                          <span className="text-zinc-300 font-medium print:text-black">{displayName}</span>
                          <div className="space-x-6 text-right font-mono">
                            <span className="text-white print:text-black">{percent.toFixed(1)}%</span>
                            <span className="text-zinc-500 print:text-black">{batchWeight.toFixed(1)} kg</span>
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  <div className="pt-4 border-t border-zinc-900 flex justify-between font-bold text-sm">
                    <span className="text-white print:text-black">Total Mix</span>
                    <div className="space-x-6 text-right font-mono text-white print:text-black">
                      <span>{formulation.totalPercentage.toFixed(1)}%</span>
                      <span>1000.0 kg</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Nutritional Analysis */}
              <div className="bg-zinc-900/30 border border-zinc-900 rounded-2xl p-6 print:border-none print:p-0">
                <h3 className="text-lg font-bold text-white mb-4 print:text-black">Nutritional Analysis</h3>
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-zinc-900 text-sm print:text-black">
                    <thead>
                      <tr className="text-left text-xs font-semibold text-zinc-400 uppercase tracking-wider print:text-black">
                        <th className="pb-3">Nutrient</th>
                        <th className="pb-3 text-right">Target</th>
                        <th className="pb-3 text-right">Actual</th>
                        <th className="pb-3 text-right">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-900/50">
                      {formulation.nutritionalComparison.map((nutrient) => (
                        <tr key={nutrient.label}>
                          <td className="py-3 font-medium text-zinc-300 print:text-black">{nutrient.label}</td>
                          <td className="py-3 text-right font-mono text-zinc-400 print:text-black">
                            {nutrient.target.toFixed(2)}
                          </td>
                          <td className="py-3 text-right font-mono">
                            <span className={nutrient.isDeficient ? "text-red-500" : "text-emerald-500"}>
                              {nutrient.actual.toFixed(2)}
                            </span>
                          </td>
                          <td className="py-3 text-right font-semibold">
                            <span className={nutrient.isDeficient ? "text-red-500" : "text-emerald-500"}>
                              {nutrient.isDeficient ? "Deficient" : "OK"}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
