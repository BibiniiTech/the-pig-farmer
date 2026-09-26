"use client";

import React, { useState } from "react";
import { SavedFeedRecipe, FeedIngredient } from "@/lib/types";

interface SavedRecipesModalProps {
  isOpen: boolean;
  onClose: () => void;
  savedRecipes: SavedFeedRecipe[];
  allIngredients: FeedIngredient[];
  currencySymbol?: string;
  onLoadRecipe: (recipe: SavedFeedRecipe) => void;
  onDeleteRecipe: (recipeId: string) => Promise<void>;
}

export default function SavedRecipesModal({
  isOpen,
  onClose,
  savedRecipes,
  allIngredients,
  currencySymbol = "$",
  onLoadRecipe,
  onDeleteRecipe,
}: SavedRecipesModalProps) {
  const [recipeToDelete, setRecipeToDelete] = useState<SavedFeedRecipe | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleDeleteConfirm = async () => {
    if (!recipeToDelete) return;
    setDeletingId(recipeToDelete.id);
    try {
      await onDeleteRecipe(recipeToDelete.id);
      setRecipeToDelete(null);
    } catch (err) {
      console.error("Failed to delete recipe:", err);
      alert("Failed to delete recipe. Please try again.");
    } finally {
      setDeletingId(null);
    }
  };

  const getIngredientDisplayName = (idOrKey: string) => {
    const found = allIngredients.find((ing) => ing.id === idOrKey || ing.name.toLowerCase() === idOrKey.toLowerCase());
    return found ? found.name : idOrKey;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div
        className="w-full max-w-2xl bg-white dark:bg-zinc-900 rounded-3xl border border-zinc-200 dark:border-zinc-800 shadow-2xl flex flex-col max-h-[88vh] overflow-hidden text-zinc-900 dark:text-zinc-100"
        role="dialog"
        aria-modal="true"
      >
        {/* Header */}
        <div className="p-5 sm:p-6 border-b border-zinc-150 dark:border-zinc-800 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-11 w-11 rounded-2xl bg-amber-50 dark:bg-amber-950/60 border border-amber-200 dark:border-amber-800/60 flex items-center justify-center text-amber-600 dark:text-amber-400">
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
              </svg>
            </div>
            <div>
              <h2 className="text-lg sm:text-xl font-bold tracking-tight">Saved Formulations</h2>
              <p className="text-xs text-zinc-500 dark:text-zinc-400">
                {savedRecipes.length} {savedRecipes.length === 1 ? "recipe" : "recipes"} saved on farm
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-xl text-zinc-400 hover:text-zinc-700 dark:hover:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition"
            aria-label="Close"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Content list */}
        <div className="flex-1 overflow-y-auto p-5 sm:p-6 space-y-4">
          {savedRecipes.length === 0 ? (
            <div className="p-12 text-center space-y-3">
              <div className="h-16 w-16 mx-auto rounded-full bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center text-zinc-400">
                <svg className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                </svg>
              </div>
              <h3 className="text-base font-bold text-zinc-700 dark:text-zinc-300">No Saved Recipes Yet</h3>
              <p className="text-xs text-zinc-500 max-w-sm mx-auto leading-relaxed">
                Formulate a balanced feed mix using the Pearson Square calculator and tap "Save Formulation" to store your recipes here.
              </p>
            </div>
          ) : (
            savedRecipes.map((recipe) => {
              const ingEntries = Object.entries(recipe.ingredients || {});
              return (
                <div
                  key={recipe.id}
                  className="bg-zinc-50/80 dark:bg-zinc-800/50 border border-zinc-200/80 dark:border-zinc-700/60 rounded-2xl p-4 sm:p-5 space-y-3 hover:border-amber-400/50 transition-all shadow-xs"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <h3 className="font-bold text-base text-zinc-900 dark:text-white">
                          {recipe.name}
                        </h3>
                        <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300 border border-emerald-300/40">
                          {recipe.stage || "Custom"}
                        </span>
                      </div>
                      <p className="text-[11px] text-zinc-500 dark:text-zinc-400">
                        Created: {recipe.dateCreated || new Date(recipe.timestamp || Date.now()).toLocaleDateString()}
                      </p>
                    </div>

                    <div className="text-right shrink-0">
                      <span className="text-xs text-zinc-400 font-medium block">Est. Cost</span>
                      <span className="text-sm font-black text-emerald-600 dark:text-emerald-400">
                        {currencySymbol}{recipe.costPerKg > 0 ? recipe.costPerKg.toFixed(2) : "0.00"}/kg
                      </span>
                    </div>
                  </div>

                  {/* Ingredients breakdown pills */}
                  <div className="bg-white dark:bg-zinc-850 rounded-xl p-3 border border-zinc-150 dark:border-zinc-750/50 space-y-1.5">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-zinc-400 block">
                      Ingredients & Proportions
                    </span>
                    <div className="flex flex-wrap gap-2">
                      {ingEntries.map(([ingId, percent]) => (
                        <span
                          key={ingId}
                          className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-zinc-100 dark:bg-zinc-800 text-xs font-semibold text-zinc-700 dark:text-zinc-300 border border-zinc-200 dark:border-zinc-700"
                        >
                          <span>{getIngredientDisplayName(ingId)}:</span>
                          <span className="text-emerald-600 dark:text-emerald-400 font-bold">
                            {Number(percent).toFixed(1)}%
                          </span>
                        </span>
                      ))}
                    </div>
                  </div>

                  {recipe.notes && (
                    <p className="text-xs text-zinc-600 dark:text-zinc-400 italic">
                      "{recipe.notes}"
                    </p>
                  )}

                  {/* Card Actions */}
                  <div className="flex items-center justify-between pt-1 border-t border-zinc-200/60 dark:border-zinc-700/40">
                    <button
                      type="button"
                      onClick={() => setRecipeToDelete(recipe)}
                      className="text-xs font-semibold text-rose-600 hover:text-rose-700 hover:underline flex items-center gap-1 py-1"
                    >
                      <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                      <span>Delete</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => {
                        onLoadRecipe(recipe);
                        onClose();
                      }}
                      className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition shadow-xs flex items-center gap-1.5 active:scale-95"
                    >
                      <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
                      </svg>
                      <span>Load into Formulator</span>
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Delete Confirmation Alert */}
        {recipeToDelete && (
          <div className="absolute inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-10 animate-in fade-in duration-150">
            <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-2xl p-6 max-w-sm w-full space-y-4 shadow-xl">
              <h4 className="font-bold text-base text-zinc-900 dark:text-white">Delete Recipe?</h4>
              <p className="text-xs text-zinc-600 dark:text-zinc-400">
                Are you sure you want to delete <strong className="text-zinc-900 dark:text-zinc-100">{recipeToDelete.name}</strong>? This action cannot be undone.
              </p>
              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  disabled={deletingId !== null}
                  onClick={() => setRecipeToDelete(null)}
                  className="px-3.5 py-1.5 rounded-lg border border-zinc-200 dark:border-zinc-700 text-xs font-semibold text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  disabled={deletingId !== null}
                  onClick={handleDeleteConfirm}
                  className="px-4 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold shadow-xs"
                >
                  {deletingId ? "Deleting..." : "Delete Recipe"}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
