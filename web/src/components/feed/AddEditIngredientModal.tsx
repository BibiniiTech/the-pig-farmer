"use client";

import React, { useState, useEffect } from "react";
import { FeedIngredient } from "@/lib/types";

interface AddEditIngredientModalProps {
  isOpen: boolean;
  onClose: () => void;
  existingIngredient?: FeedIngredient | null;
  currencySymbol?: string;
  onSave: (ingredient: Omit<FeedIngredient, "id">, id?: string) => Promise<void>;
}

export default function AddEditIngredientModal({
  isOpen,
  onClose,
  existingIngredient,
  currencySymbol = "$",
  onSave,
}: AddEditIngredientModalProps) {
  const [name, setName] = useState("");
  const [mainCategory, setMainCategory] = useState("Protein");
  const [category, setCategory] = useState("Oilseed Byproducts");
  const [dryMatter, setDryMatter] = useState("88");
  const [crudeProtein, setCrudeProtein] = useState("18.0");
  const [metabolizableEnergy, setMetabolizableEnergy] = useState("2800");
  const [lysine, setLysine] = useState("0.85");
  const [calcium, setCalcium] = useState("0.25");
  const [phosphorus, setPhosphorus] = useState("0.45");
  const [crudeFiber, setCrudeFiber] = useState("6.5");
  const [costPerKg, setCostPerKg] = useState("0.00");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      if (existingIngredient) {
        setName(existingIngredient.name || "");
        setMainCategory(existingIngredient.mainCategory || "Protein");
        setCategory(existingIngredient.category || "");
        setDryMatter(String(existingIngredient.dryMatter ?? 88));
        setCrudeProtein(String(existingIngredient.crudeProtein ?? 18));
        setMetabolizableEnergy(String(existingIngredient.metabolizableEnergy ?? 2800));
        setLysine(String(existingIngredient.lysine ?? 0.85));
        setCalcium(String(existingIngredient.calcium ?? 0.25));
        setPhosphorus(String(existingIngredient.phosphorus ?? 0.45));
        setCrudeFiber(String(existingIngredient.crudeFiber ?? 6.5));
        setCostPerKg(String(existingIngredient.costPerKg ?? 0));
      } else {
        setName("");
        setMainCategory("Protein");
        setCategory("Oilseed Byproducts");
        setDryMatter("88");
        setCrudeProtein("18.0");
        setMetabolizableEnergy("2800");
        setLysine("0.85");
        setCalcium("0.25");
        setPhosphorus("0.45");
        setCrudeFiber("6.5");
        setCostPerKg("0.00");
      }
      setError(null);
    }
  }, [isOpen, existingIngredient]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError("Please provide a name for this ingredient.");
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const payload: Omit<FeedIngredient, "id"> = {
        name: name.trim(),
        mainCategory,
        category: category.trim() || mainCategory,
        dryMatter: parseFloat(dryMatter) || 88,
        crudeProtein: parseFloat(crudeProtein) || 0,
        metabolizableEnergy: parseFloat(metabolizableEnergy) || 0,
        lysine: parseFloat(lysine) || 0,
        calcium: parseFloat(calcium) || 0,
        phosphorus: parseFloat(phosphorus) || 0,
        crudeFiber: parseFloat(crudeFiber) || 0,
        costPerKg: parseFloat(costPerKg) || 0,
        description: existingIngredient?.description || `${name.trim()} - Farm Custom Ingredient`,
        quantity: existingIngredient?.quantity ?? 0,
        unit: existingIngredient?.unit || "kg",
        visible: existingIngredient?.visible ?? true,
        fat: existingIngredient?.fat ?? 0,
        sodium: existingIngredient?.sodium ?? 0,
        chloride: existingIngredient?.chloride ?? 0,
        potassium: existingIngredient?.potassium ?? 0,
        sulfur: existingIngredient?.sulfur ?? 0,
        methionine: existingIngredient?.methionine ?? 0,
        cystine: existingIngredient?.cystine ?? 0,
        threonine: existingIngredient?.threonine ?? 0,
        tryptophan: existingIngredient?.tryptophan ?? 0,
        arginine: existingIngredient?.arginine ?? 0,
        maxStarter: existingIngredient?.maxStarter ?? 30,
        maxGrower: existingIngredient?.maxGrower ?? 40,
        maxFinisher: existingIngredient?.maxFinisher ?? 50,
      };

      await onSave(payload, existingIngredient?.id);
      onClose();
    } catch (err: any) {
      console.error("Failed to save ingredient:", err);
      setError(err.message || "Failed to save feed ingredient. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div
        className="w-full max-w-xl bg-white dark:bg-zinc-900 rounded-3xl border border-zinc-200 dark:border-zinc-800 shadow-2xl flex flex-col max-h-[92vh] overflow-hidden text-zinc-900 dark:text-zinc-100"
        role="dialog"
        aria-modal="true"
      >
        <div className="p-5 sm:p-6 border-b border-zinc-150 dark:border-zinc-800 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-amber-50 dark:bg-amber-950/60 border border-amber-200 dark:border-amber-800/60 flex items-center justify-center text-amber-600 dark:text-amber-400">
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
              </svg>
            </div>
            <div>
              <h3 className="text-lg font-bold tracking-tight">
                {existingIngredient ? "Edit Farm Ingredient" : "New Farm Feed Ingredient"}
              </h3>
              <p className="text-xs text-zinc-500 dark:text-zinc-400">
                Nutritional values per 1 kg / % as-fed
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-zinc-400 hover:text-zinc-700 dark:hover:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-800"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-5 sm:p-6 space-y-4">
          {error && (
            <div className="p-3 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/60 text-xs font-semibold text-rose-700 dark:text-rose-300">
              {error}
            </div>
          )}

          <div>
            <label className="block text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 mb-1">
              Ingredient Name *
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Palm Kernel Cake, Brewery Spent Grain"
              className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800 px-3.5 py-2.5 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-amber-500 transition"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 mb-1">
                Main Category *
              </label>
              <select
                value={mainCategory}
                onChange={(e) => setMainCategory(e.target.value)}
                className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800 px-3.5 py-2.5 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-amber-500 transition"
              >
                <option value="Protein">Protein Source</option>
                <option value="Energy">Energy Source</option>
                <option value="Vitamins, Minerals & Salt">Vitamins, Minerals & Salt</option>
                <option value="Roughages">Roughages / Fiber</option>
              </select>
            </div>
            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 mb-1">
                Cost Per Kg ({currencySymbol})
              </label>
              <input
                type="number"
                step="any"
                value={costPerKg}
                onChange={(e) => setCostPerKg(e.target.value)}
                placeholder="0.00"
                className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800 px-3.5 py-2.5 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-amber-500 transition"
              />
            </div>
          </div>

          {/* Nutritional Profile Grid */}
          <div className="border border-zinc-200 dark:border-zinc-800 rounded-2xl p-4 bg-zinc-50/60 dark:bg-zinc-850/40 space-y-3">
            <span className="text-[11px] font-black uppercase tracking-wider text-amber-700 dark:text-amber-400 block">
              Nutritional Content (NRC Standard)
            </span>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-[10px] font-bold uppercase text-zinc-500 mb-1">
                  Crude Protein (CP %) *
                </label>
                <input
                  type="number"
                  step="any"
                  required
                  value={crudeProtein}
                  onChange={(e) => setCrudeProtein(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-2.5 py-1.5 text-xs font-bold text-amber-700 dark:text-amber-400 focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
              </div>

              <div>
                <label className="block text-[10px] font-bold uppercase text-zinc-500 mb-1">
                  Energy (ME kcal/kg)
                </label>
                <input
                  type="number"
                  step="any"
                  value={metabolizableEnergy}
                  onChange={(e) => setMetabolizableEnergy(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-2.5 py-1.5 text-xs font-bold focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
              </div>

              <div>
                <label className="block text-[10px] font-bold uppercase text-zinc-500 mb-1">
                  Dry Matter (DM %)
                </label>
                <input
                  type="number"
                  step="any"
                  value={dryMatter}
                  onChange={(e) => setDryMatter(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-2.5 py-1.5 text-xs font-bold focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
              </div>

              <div>
                <label className="block text-[10px] font-bold uppercase text-zinc-500 mb-1">
                  Lysine (%)
                </label>
                <input
                  type="number"
                  step="any"
                  value={lysine}
                  onChange={(e) => setLysine(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-2.5 py-1.5 text-xs font-bold focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
              </div>

              <div>
                <label className="block text-[10px] font-bold uppercase text-zinc-500 mb-1">
                  Calcium (Ca %)
                </label>
                <input
                  type="number"
                  step="any"
                  value={calcium}
                  onChange={(e) => setCalcium(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-2.5 py-1.5 text-xs font-bold focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
              </div>

              <div>
                <label className="block text-[10px] font-bold uppercase text-zinc-500 mb-1">
                  Phosphorus (P %)
                </label>
                <input
                  type="number"
                  step="any"
                  value={phosphorus}
                  onChange={(e) => setPhosphorus(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-2.5 py-1.5 text-xs font-bold focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
              </div>

              <div className="col-span-2 sm:col-span-1">
                <label className="block text-[10px] font-bold uppercase text-zinc-500 mb-1">
                  Crude Fiber (CF %)
                </label>
                <input
                  type="number"
                  step="any"
                  value={crudeFiber}
                  onChange={(e) => setCrudeFiber(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-2.5 py-1.5 text-xs font-bold focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
              </div>
            </div>
          </div>

          <div className="flex items-center justify-end gap-2 pt-3 border-t border-zinc-150 dark:border-zinc-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-zinc-200 dark:border-zinc-700 text-xs font-semibold text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-5 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold shadow-md transition disabled:opacity-60 flex items-center gap-1.5 active:scale-95"
            >
              {submitting ? "Saving..." : existingIngredient ? "Update Ingredient" : "Add Ingredient"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
