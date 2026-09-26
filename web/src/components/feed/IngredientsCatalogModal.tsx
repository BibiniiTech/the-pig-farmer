"use client";

import React, { useState } from "react";
import { db } from "@/lib/firebase";
import { doc, collection, setDoc, updateDoc, deleteDoc, serverTimestamp } from "firebase/firestore";
import { FeedIngredient } from "@/lib/types";
import AddEditIngredientModal from "./AddEditIngredientModal";

interface IngredientsCatalogModalProps {
  isOpen: boolean;
  onClose: () => void;
  ingredients: FeedIngredient[];
  activeFarmUid: string | null;
  currencySymbol?: string;
  isPremium?: boolean;
  onRequestRewardedPass?: () => void;
}

export default function IngredientsCatalogModal({
  isOpen,
  onClose,
  ingredients,
  activeFarmUid,
  currencySymbol = "$",
  isPremium = false,
  onRequestRewardedPass,
}: IngredientsCatalogModalProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState<string>("All");
  const [editingIngredient, setEditingIngredient] = useState<FeedIngredient | null>(null);
  const [isAddEditOpen, setIsAddEditOpen] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  if (!isOpen) return null;

  const categories = ["All", "Protein", "Energy", "Vitamins, Minerals & Salt"];

  const filtered = ingredients.filter((ing) => {
    const matchesSearch =
      ing.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (ing.category && ing.category.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (ing.mainCategory && ing.mainCategory.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchesCategory =
      selectedCategory === "All" ||
      ing.mainCategory?.toLowerCase() === selectedCategory.toLowerCase() ||
      ing.category?.toLowerCase() === selectedCategory.toLowerCase();

    return matchesSearch && matchesCategory;
  });

  const handleOpenAdd = () => {
    setEditingIngredient(null);
    setIsAddEditOpen(true);
  };

  const handleOpenEdit = (ing: FeedIngredient) => {
    setEditingIngredient(ing);
    setIsAddEditOpen(true);
  };

  const handleDelete = async (id: string, name: string) => {
    if (!activeFarmUid) return;
    if (!window.confirm(`Are you sure you want to delete "${name}" from your farm's ingredient catalog?`)) {
      return;
    }

    setDeletingId(id);
    try {
      await deleteDoc(doc(db, "users", activeFarmUid, "feed_ingredients", id));
    } catch (err) {
      console.error("Error deleting ingredient:", err);
      alert("Failed to delete ingredient. Please try again.");
    } finally {
      setDeletingId(null);
    }
  };

  const handleSaveIngredient = async (
    ingredientData: Omit<FeedIngredient, "id">,
    id?: string
  ) => {
    if (!activeFarmUid) return;

    if (id) {
      // Update existing
      const ingRef = doc(db, "users", activeFarmUid, "feed_ingredients", id);
      await updateDoc(ingRef, {
        ...ingredientData,
        updatedAt: serverTimestamp(),
      });
    } else {
      // Create new
      const colRef = collection(db, "users", activeFarmUid, "feed_ingredients");
      const newDocRef = doc(colRef);
      await setDoc(newDocRef, {
        ...ingredientData,
        id: newDocRef.id,
        farmId: activeFarmUid,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    }
  };

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-zinc-950/70 backdrop-blur-sm animate-in fade-in duration-200">
        <div className="relative w-full max-w-4xl max-h-[90vh] bg-white rounded-2xl shadow-2xl border border-zinc-200 flex flex-col overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-zinc-200 bg-zinc-50/50">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-xl bg-amber-500/10 text-amber-600 flex items-center justify-center font-bold">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  strokeWidth={2}
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
                  />
                </svg>
              </div>
              <div>
                <h2 className="text-lg font-bold text-zinc-900">
                  Farm Ingredients Catalog
                </h2>
                <p className="text-xs text-zinc-500">
                  Manage custom local ingredients and nutritional specifications
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleOpenAdd}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-bold text-white bg-amber-600 hover:bg-amber-700 active:scale-95 rounded-xl shadow-sm transition"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-4 w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  strokeWidth={2.5}
                >
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
                </svg>
                <span>Add Ingredient</span>
              </button>
              <button
                type="button"
                onClick={onClose}
                className="p-2 text-zinc-400 hover:text-zinc-600 rounded-lg hover:bg-zinc-100 transition"
                aria-label="Close modal"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  strokeWidth={2}
                >
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>

          {/* Filter and Search Bar */}
          <div className="p-4 border-b border-zinc-200 bg-white flex flex-col sm:flex-row gap-3 items-center justify-between">
            <div className="relative w-full sm:w-72">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="absolute left-3 top-2.5 h-4 w-4 text-zinc-400"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth={2}
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
              <input
                type="text"
                placeholder="Search ingredients..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-1.5 text-xs rounded-xl border border-zinc-200 bg-zinc-50/50 text-zinc-800 placeholder-zinc-400 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 transition"
              />
            </div>

            {/* Category Pills */}
            <div className="flex items-center gap-1.5 overflow-x-auto w-full sm:w-auto pb-1 sm:pb-0 scrollbar-none">
              {categories.map((cat) => (
                <button
                  key={cat}
                  type="button"
                  onClick={() => setSelectedCategory(cat)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition ${
                    selectedCategory === cat
                      ? "bg-amber-100 text-amber-800 border border-amber-300"
                      : "bg-zinc-100 text-zinc-600 hover:bg-zinc-200 border border-transparent"
                  }`}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>

          {/* Ingredients Grid / List */}
          <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-3">
            {filtered.length === 0 ? (
              <div className="py-16 text-center text-zinc-400 flex flex-col items-center justify-center">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-12 w-12 text-zinc-300 mb-3"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  strokeWidth={1.5}
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
                  />
                </svg>
                <p className="text-sm font-semibold text-zinc-600">No ingredients found</p>
                <p className="text-xs text-zinc-400 mt-1 max-w-sm">
                  {searchQuery
                    ? "Try adjusting your search or category filter"
                    : "Add your first custom farm ingredient to tailor feed mixing and nutrient analysis"}
                </p>
                {!searchQuery && (
                  <button
                    type="button"
                    onClick={handleOpenAdd}
                    className="mt-4 px-4 py-2 text-xs font-bold text-white bg-amber-600 hover:bg-amber-700 rounded-xl shadow-sm transition"
                  >
                    Add Your First Ingredient
                  </button>
                )}
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                {filtered.map((ing) => (
                  <div
                    key={ing.id}
                    className="group border border-zinc-200 hover:border-amber-400/60 rounded-xl p-4 bg-white hover:bg-amber-50/20 transition-all shadow-xs flex flex-col justify-between"
                  >
                    <div>
                      <div className="flex items-start justify-between gap-2 mb-2">
                        <div>
                          <h3 className="font-bold text-sm text-zinc-900 group-hover:text-amber-700 transition">
                            {ing.name}
                          </h3>
                          <div className="flex items-center gap-1.5 mt-0.5">
                            <span className="text-[10px] px-2 py-0.5 rounded-md font-semibold bg-zinc-100 text-zinc-700">
                              {ing.mainCategory || "Uncategorized"}
                            </span>
                            {ing.category && ing.category !== ing.mainCategory && (
                              <span className="text-[10px] text-zinc-400">
                                • {ing.category}
                              </span>
                            )}
                          </div>
                        </div>

                        {/* Actions */}
                        <div className="flex items-center gap-1 shrink-0">
                          <button
                            type="button"
                            onClick={() => handleOpenEdit(ing)}
                            title="Edit Ingredient"
                            className="p-1.5 text-zinc-400 hover:text-amber-600 hover:bg-amber-50 rounded-lg transition"
                          >
                            <svg
                              xmlns="http://www.w3.org/2000/svg"
                              className="h-4 w-4"
                              fill="none"
                              viewBox="0 0 24 24"
                              stroke="currentColor"
                              strokeWidth={2}
                            >
                              <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                              />
                            </svg>
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDelete(ing.id, ing.name)}
                            disabled={deletingId === ing.id}
                            title="Delete Ingredient"
                            className="p-1.5 text-zinc-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition disabled:opacity-50"
                          >
                            <svg
                              xmlns="http://www.w3.org/2000/svg"
                              className="h-4 w-4"
                              fill="none"
                              viewBox="0 0 24 24"
                              stroke="currentColor"
                              strokeWidth={2}
                            >
                              <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                              />
                            </svg>
                          </button>
                        </div>
                      </div>

                      {/* Nutrient Matrix Chips */}
                      <div className="grid grid-cols-3 sm:grid-cols-4 gap-1.5 mt-3 pt-3 border-t border-zinc-100 text-[11px]">
                        <div className="bg-zinc-50 rounded px-2 py-1">
                          <span className="text-zinc-400 block text-[9px]">CP</span>
                          <span className="font-bold text-zinc-800">
                            {ing.crudeProtein?.toFixed(1) ?? "0"}%
                          </span>
                        </div>
                        <div className="bg-zinc-50 rounded px-2 py-1">
                          <span className="text-zinc-400 block text-[9px]">ME</span>
                          <span className="font-bold text-zinc-800">
                            {Math.round(ing.metabolizableEnergy || 0)} <span className="text-[9px] font-normal">kcal</span>
                          </span>
                        </div>
                        <div className="bg-zinc-50 rounded px-2 py-1">
                          <span className="text-zinc-400 block text-[9px]">Lysine</span>
                          <span className="font-bold text-zinc-800">
                            {ing.lysine?.toFixed(2) ?? "0"}%
                          </span>
                        </div>
                        <div className="bg-zinc-50 rounded px-2 py-1">
                          <span className="text-zinc-400 block text-[9px]">Dry Matter</span>
                          <span className="font-bold text-zinc-800">
                            {ing.dryMatter?.toFixed(1) ?? "0"}%
                          </span>
                        </div>
                        <div className="bg-zinc-50 rounded px-2 py-1">
                          <span className="text-zinc-400 block text-[9px]">Calcium</span>
                          <span className="font-bold text-zinc-800">
                            {ing.calcium?.toFixed(2) ?? "0"}%
                          </span>
                        </div>
                        <div className="bg-zinc-50 rounded px-2 py-1">
                          <span className="text-zinc-400 block text-[9px]">Phosphorus</span>
                          <span className="font-bold text-zinc-800">
                            {ing.phosphorus?.toFixed(2) ?? "0"}%
                          </span>
                        </div>
                        <div className="bg-zinc-50 rounded px-2 py-1">
                          <span className="text-zinc-400 block text-[9px]">Crude Fiber</span>
                          <span className="font-bold text-zinc-800">
                            {ing.crudeFiber?.toFixed(1) ?? "0"}%
                          </span>
                        </div>
                        <div className="bg-amber-50/70 border border-amber-200/50 rounded px-2 py-1">
                          <span className="text-amber-700 block text-[9px]">Cost / kg</span>
                          <span className="font-bold text-amber-900">
                            {currencySymbol}{ing.costPerKg?.toFixed(2) ?? "0.00"}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="px-6 py-3 border-t border-zinc-200 bg-zinc-50 flex items-center justify-between text-xs text-zinc-500">
            <span>
              Total Ingredients: <strong>{filtered.length}</strong>
            </span>
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-1.5 text-xs font-semibold text-zinc-600 hover:text-zinc-900 hover:bg-zinc-200/60 rounded-lg transition"
            >
              Close
            </button>
          </div>
        </div>
      </div>

      {/* Add / Edit Modal */}
      <AddEditIngredientModal
        isOpen={isAddEditOpen}
        onClose={() => setIsAddEditOpen(false)}
        existingIngredient={editingIngredient}
        currencySymbol={currencySymbol}
        onSave={handleSaveIngredient}
      />
    </>
  );
}
