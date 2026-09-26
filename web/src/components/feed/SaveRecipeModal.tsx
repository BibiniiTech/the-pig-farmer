"use client";

import React, { useState, useEffect } from "react";

interface SaveRecipeModalProps {
  isOpen: boolean;
  onClose: () => void;
  stage: string;
  ingredientsCount: number;
  costPerKg: number;
  currencySymbol?: string;
  onSave: (data: { name: string; notes: string; targetBatchKg: number }) => Promise<void>;
}

export default function SaveRecipeModal({
  isOpen,
  onClose,
  stage,
  ingredientsCount,
  costPerKg,
  currencySymbol = "$",
  onSave,
}: SaveRecipeModalProps) {
  const [recipeName, setRecipeName] = useState(`${stage} Balanced Mix`);
  const [batchKg, setBatchKg] = useState("1000");
  const [notes, setNotes] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setRecipeName(`${stage} Balanced Mix`);
      setBatchKg("1000");
      setNotes("");
      setError(null);
    }
  }, [isOpen, stage]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!recipeName.trim()) {
      setError("Please provide a name for this recipe.");
      return;
    }

    const batch = parseFloat(batchKg) || 1000;
    setSaving(true);
    setError(null);

    try {
      await onSave({
        name: recipeName.trim(),
        notes: notes.trim(),
        targetBatchKg: batch,
      });
      onClose();
    } catch (err: any) {
      console.error("Failed to save recipe:", err);
      setError(err.message || "Failed to save formulation. Please check connection.");
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div
        className="w-full max-w-md bg-white dark:bg-zinc-900 rounded-3xl border border-zinc-200 dark:border-zinc-800 shadow-2xl p-6 sm:p-7 space-y-5 text-zinc-900 dark:text-zinc-100"
        role="dialog"
        aria-modal="true"
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-emerald-50 dark:bg-emerald-950/60 border border-emerald-200 dark:border-emerald-800/60 flex items-center justify-center text-emerald-600 dark:text-emerald-400">
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
              </svg>
            </div>
            <div>
              <h3 className="text-lg font-bold tracking-tight">Save Formulation</h3>
              <p className="text-xs text-zinc-500 dark:text-zinc-400">
                Store recipe for quick reuse & batch mixing
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

        {/* Quick summary strip */}
        <div className="grid grid-cols-3 gap-2 bg-zinc-50 dark:bg-zinc-800/60 border border-zinc-200/80 dark:border-zinc-700/60 rounded-xl p-3 text-center text-xs">
          <div>
            <span className="text-[10px] text-zinc-400 font-bold uppercase block">Stage</span>
            <span className="font-bold text-emerald-600 dark:text-emerald-400">{stage}</span>
          </div>
          <div>
            <span className="text-[10px] text-zinc-400 font-bold uppercase block">Ingredients</span>
            <span className="font-bold text-zinc-800 dark:text-zinc-200">{ingredientsCount} items</span>
          </div>
          <div>
            <span className="text-[10px] text-zinc-400 font-bold uppercase block">Est. Cost</span>
            <span className="font-bold text-zinc-800 dark:text-zinc-200">
              {currencySymbol}{costPerKg > 0 ? costPerKg.toFixed(2) : "0.00"}/kg
            </span>
          </div>
        </div>

        {error && (
          <div className="p-3 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/60 text-xs font-semibold text-rose-700 dark:text-rose-300">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 mb-1">
              Recipe Name *
            </label>
            <input
              type="text"
              required
              value={recipeName}
              onChange={(e) => setRecipeName(e.target.value)}
              placeholder="e.g. High Protein Grower Mix"
              className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800 px-3.5 py-2.5 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-emerald-500 transition"
            />
          </div>

          <div>
            <label className="block text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 mb-1">
              Target Batch Size (kg)
            </label>
            <input
              type="number"
              step="any"
              value={batchKg}
              onChange={(e) => setBatchKg(e.target.value)}
              placeholder="1000"
              className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800 px-3.5 py-2.5 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-emerald-500 transition"
            />
          </div>

          <div>
            <label className="block text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 mb-1">
              Notes (Optional)
            </label>
            <textarea
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Add mixing instructions, premix additives, or seasonality notes..."
              className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800 px-3.5 py-2 text-xs font-medium focus:outline-none focus:ring-2 focus:ring-emerald-500 transition resize-none"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-zinc-200 dark:border-zinc-700 text-xs font-semibold text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-md transition disabled:opacity-60 flex items-center gap-1.5 active:scale-95"
            >
              {saving ? "Saving..." : "Save Recipe"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
