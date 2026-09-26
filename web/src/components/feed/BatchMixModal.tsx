"use client";

import React, { useState, useEffect } from "react";
import { collection, doc, getDocs, writeBatch, serverTimestamp } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { FeedInventoryItem } from "@/lib/types";

interface BatchMixModalProps {
  isOpen: boolean;
  onClose: () => void;
  recipeName: string;
  stage: string;
  defaultBatchKg?: number;
  requiredIngredients: { id: string; name: string; percent: number; costPerKg?: number }[];
  activeFarmUid: string;
  onSuccess?: (msg: string) => void;
}

export default function BatchMixModal({
  isOpen,
  onClose,
  recipeName,
  stage,
  defaultBatchKg = 1000.0,
  requiredIngredients,
  activeFarmUid,
  onSuccess,
}: BatchMixModalProps) {
  const [batchKg, setBatchKg] = useState<number>(defaultBatchKg);
  const [inventoryItems, setInventoryItems] = useState<FeedInventoryItem[]>([]);
  const [loadingInventory, setLoadingInventory] = useState<boolean>(true);
  const [isMixing, setIsMixing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setBatchKg(defaultBatchKg);
  }, [defaultBatchKg]);

  useEffect(() => {
    if (!isOpen || !activeFarmUid) return;

    const fetchInventory = async () => {
      try {
        setLoadingInventory(true);
        const snap = await getDocs(collection(db, "users", activeFarmUid, "feed_inventory"));
        const items = snap.docs.map(d => ({ id: d.id, ...d.data() } as FeedInventoryItem));
        setInventoryItems(items);
      } catch (err: any) {
        console.error("Error fetching feed inventory:", err);
      } finally {
        setLoadingInventory(false);
      }
    };

    fetchInventory();
  }, [isOpen, activeFarmUid]);

  if (!isOpen) return null;

  // Identify out-of-stock ingredients
  const outOfStockIngredients = requiredIngredients.filter(req => {
    const cleanName = req.name.trim().toLowerCase();
    const invItem = inventoryItems.find(
      i =>
        i.name.trim().toLowerCase() === cleanName ||
        i.id.trim().toLowerCase() === req.id.trim().toLowerCase() ||
        (i.feedType && i.feedType.trim().toLowerCase() === cleanName)
    );
    return !invItem || invItem.quantity <= 0;
  });

  const handleExecuteMix = async () => {
    if (!activeFarmUid || batchKg <= 0 || isMixing) return;
    setIsMixing(true);
    setError(null);

    try {
      const nowIso = new Date().toISOString();
      const displayDate = new Date().toLocaleDateString("en-GB"); // dd/mm/yyyy
      let totalBatchCost = 0.0;

      const batch = writeBatch(db);

      // 1. Process each ingredient and deduct stock
      requiredIngredients.forEach(req => {
        const neededKg = (req.percent / 100.0) * batchKg;
        if (neededKg > 0.0001) {
          const ingCost = req.costPerKg ?? 0.0;
          const lineCost = ingCost * neededKg;
          totalBatchCost += lineCost;

          const cleanName = req.name.trim().toLowerCase();
          const matchingInv = inventoryItems.find(
            i =>
              i.name.trim().toLowerCase() === cleanName ||
              i.id.trim().toLowerCase() === req.id.trim().toLowerCase() ||
              (i.feedType && i.feedType.trim().toLowerCase() === cleanName)
          );

          if (matchingInv) {
            const convertedUsed =
              matchingInv.unit === "bags" && matchingInv.unitWeight > 0
                ? neededKg / matchingInv.unitWeight
                : neededKg;

            const updatedQty = Math.max(0, matchingInv.quantity - convertedUsed);
            const invDocRef = doc(db, "users", activeFarmUid, "feed_inventory", matchingInv.id);
            batch.update(invDocRef, {
              quantity: updatedQty,
              lastUpdated: nowIso,
            });
          }

          // Record usage transaction
          const usageTxRef = doc(collection(db, "users", activeFarmUid, "feed_inventory_transactions"));
          batch.set(usageTxRef, {
            itemId: matchingInv?.id || req.id,
            itemName: req.name,
            type: "Usage",
            quantity: neededKg,
            unit: "kg",
            cost: lineCost,
            date: nowIso,
            notes: `Mixed into ${batchKg} kg of ${stage} feed (${recipeName})`,
          });
        }
      });

      // 2. Restock or create the finished mixed feed item
      const finishedName = recipeName.trim() || `${stage} Feed (Mixed)`;
      const existingFinished = inventoryItems.find(
        i => i.name.trim().toLowerCase() === finishedName.toLowerCase()
      );
      const finalCostPerKg = batchKg > 0 ? totalBatchCost / batchKg : 0.0;

      let finishedId = "";
      if (existingFinished) {
        finishedId = existingFinished.id;
        const addedQty =
          existingFinished.unit === "bags" && existingFinished.unitWeight > 0
            ? batchKg / existingFinished.unitWeight
            : batchKg;

        const finishedDocRef = doc(db, "users", activeFarmUid, "feed_inventory", existingFinished.id);
        batch.update(finishedDocRef, {
          quantity: existingFinished.quantity + addedQty,
          costPerUnit: finalCostPerKg > 0 ? finalCostPerKg : existingFinished.costPerUnit,
          lastUpdated: nowIso,
        });
      } else {
        const newFinishedRef = doc(collection(db, "users", activeFarmUid, "feed_inventory"));
        finishedId = newFinishedRef.id;
        batch.set(newFinishedRef, {
          name: finishedName,
          feedType: stage,
          itemCategory: "Complete Feed",
          quantity: batchKg,
          unit: "kg",
          unitWeight: 50.0,
          minThreshold: 100.0,
          costPerUnit: finalCostPerKg,
          lastUpdated: nowIso,
        });
      }

      // Record finished feed restock transaction
      const restockTxRef = doc(collection(db, "users", activeFarmUid, "feed_inventory_transactions"));
      batch.set(restockTxRef, {
        itemId: finishedId,
        itemName: finishedName,
        type: "Restock",
        quantity: batchKg,
        unit: "kg",
        cost: totalBatchCost,
        date: nowIso,
        notes: `Finished farm mix: ${recipeName} (${stage})`,
      });

      // 3. Auto-post batch expense to financials
      if (totalBatchCost > 0) {
        const financialRef = doc(collection(db, "users", activeFarmUid, "financials"));
        batch.set(financialRef, {
          date: displayDate,
          type: "Expense",
          category: "Feed",
          amount: totalBatchCost,
          description: `Batch Mix: ${recipeName} (${stage}, ${batchKg}kg)`,
          createdAt: serverTimestamp(),
        });
      }

      // Commit atomic batch
      await batch.commit();

      if (onSuccess) {
        onSuccess(`Successfully mixed ${batchKg} kg of ${recipeName}! Inventory updated.`);
      }
      onClose();
    } catch (err: any) {
      console.error("Batch mix execution failed:", err);
      setError(err?.message || "Failed to execute batch mix. Please try again.");
    } finally {
      setIsMixing(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-xl border border-zinc-200 animate-fadeIn">
        <div className="flex items-center justify-between pb-3 border-b border-zinc-150">
          <div>
            <h3 className="text-base font-black text-amber-600">Execute Batch Mix</h3>
            <p className="text-xs text-zinc-500">Produce finished complete feed and deduct ingredients from inventory.</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isMixing}
            className="text-zinc-400 hover:text-zinc-600 p-1"
          >
            ✕
          </button>
        </div>

        <div className="space-y-3">
          <div>
            <label className="block text-xs font-bold text-zinc-700 mb-1">
              Batch Size (kg)
            </label>
            <input
              type="number"
              min="1"
              step="1"
              value={batchKg}
              disabled={isMixing}
              onChange={(e) => setBatchKg(Math.max(1, parseFloat(e.target.value) || 0))}
              className="w-full px-3 py-2 border border-zinc-200 rounded-xl text-sm font-bold text-zinc-900 focus:outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>

          {/* Quick presets */}
          <div className="flex items-center gap-2">
            {[100, 250, 500, 1000].map(size => (
              <button
                key={size}
                type="button"
                disabled={isMixing}
                onClick={() => setBatchKg(size)}
                className={`flex-1 py-1.5 rounded-lg text-xs font-bold border transition ${
                  batchKg === size
                    ? "bg-amber-600 text-white border-amber-600"
                    : "bg-zinc-50 text-zinc-700 border-zinc-200 hover:bg-zinc-100"
                }`}
              >
                {size} kg
              </button>
            ))}
          </div>

          {/* Out of Stock Warning */}
          {outOfStockIngredients.length > 0 && (
            <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-xs space-y-1">
              <div className="flex items-center gap-1.5 font-bold text-rose-800">
                <span>⚠️</span>
                <span>Ingredients Out of Stock in Feed Inventory:</span>
              </div>
              <p className="text-zinc-600 text-[11px]">
                The following required ingredients have 0 kg in stock. Mixing will deduct remaining items and leave negative stock tracking if needed:
              </p>
              <ul className="list-disc list-inside text-rose-700 font-semibold space-y-0.5">
                {outOfStockIngredients.map(ing => (
                  <li key={ing.id}>{ing.name}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Breakdown Preview */}
          <div className="bg-zinc-50 border border-zinc-200 rounded-xl p-3 max-h-44 overflow-y-auto space-y-1.5">
            <p className="text-[11px] font-bold text-zinc-600 uppercase tracking-wider">Required For {batchKg} kg Batch:</p>
            {requiredIngredients.map(req => {
              const reqKg = (req.percent / 100.0) * batchKg;
              return (
                <div key={req.id} className="flex justify-between text-xs text-zinc-700">
                  <span className="truncate pr-2">{req.name} ({req.percent.toFixed(1)}%)</span>
                  <span className="font-mono font-bold text-zinc-900 shrink-0">{reqKg.toFixed(1)} kg</span>
                </div>
              );
            })}
          </div>

          {error && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl font-medium">
              {error}
            </div>
          )}
        </div>

        <div className="flex items-center justify-end gap-2 pt-3 border-t border-zinc-150">
          <button
            type="button"
            onClick={onClose}
            disabled={isMixing}
            className="px-4 py-2 text-xs font-bold text-zinc-600 hover:text-zinc-800"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleExecuteMix}
            disabled={isMixing || batchKg <= 0}
            className="px-5 py-2 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold shadow transition flex items-center gap-2 disabled:opacity-50"
          >
            {isMixing ? (
              <>
                <div className="h-3 w-3 animate-spin rounded-full border-2 border-white border-t-transparent" />
                <span>Mixing Batch...</span>
              </>
            ) : (
              <span>Mix & Deduct Stock</span>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
