"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot, doc, setDoc, deleteDoc, updateDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";

interface FeedInventoryItem {
  id: string;
  name: string;
  feedType: string;
  quantity: number;
  unit: string;
  unitWeight: number;
  minThreshold: number;
  costPerUnit: number;
  lastUpdated: string;
}

interface FeedInventoryTransaction {
  id: string;
  itemId: string;
  itemName: string;
  type: string; // "Restock" or "Usage"
  quantity: number;
  unit: string;
  cost: number;
  date: string;
  notes: string;
}

export default function FeedInventoryPage() {
  const { user, activeFarmUid, loading } = useAuth();
  const router = useRouter();

  const [items, setItems] = useState<FeedInventoryItem[]>([]);
  const [transactions, setTransactions] = useState<FeedInventoryTransaction[]>([]);
  const [dataLoading, setDataLoading] = useState(true);

  // Form states
  const [showAddModal, setShowAddModal] = useState(false);
  const [showRestockModal, setShowRestockModal] = useState(false);
  const [showUsageModal, setShowUsageModal] = useState(false);
  const [selectedItemId, setSelectedItemId] = useState<string | null>(null);

  // Add Item inputs
  const [newName, setNewName] = useState("");
  const [newFeedType, setNewFeedType] = useState("Starter");
  const [newQty, setNewQty] = useState(0);
  const [newUnit, setNewUnit] = useState("bags");
  const [newWeight, setNewWeight] = useState(50);
  const [newThreshold, setNewThreshold] = useState(5);

  // Restock inputs
  const [restockQty, setRestockQty] = useState(0);
  const [restockUnit, setRestockUnit] = useState("bags");
  const [restockCost, setRestockCost] = useState(0);
  const [restockNotes, setRestockNotes] = useState("");

  // Usage inputs
  const [usageQty, setUsageQty] = useState(0);
  const [usageUnit, setUsageUnit] = useState("bags");
  const [usageNotes, setUsageNotes] = useState("");

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!activeFarmUid) return;

    setDataLoading(true);

    // 1. Listen to Feed Items
    const itemsQuery = collection(db, "users", activeFarmUid, "feed_inventory");
    const unsubscribeItems = onSnapshot(itemsQuery, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as FeedInventoryItem));
      setItems(list.sort((a, b) => a.name.localeCompare(b.name)));
    });

    // 2. Listen to Transactions
    const txQuery = collection(db, "users", activeFarmUid, "feed_inventory_transactions");
    const unsubscribeTx = onSnapshot(txQuery, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as FeedInventoryTransaction));
      setTransactions(list.sort((a, b) => b.date.localeCompare(a.date)));
      setDataLoading(false);
    });

    return () => {
      unsubscribeItems();
      unsubscribeTx();
    };
  }, [activeFarmUid]);

  const handleAddItem = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !newName.trim()) return;

    try {
      const itemsCollection = collection(db, "users", activeFarmUid, "feed_inventory");
      const newItemRef = doc(itemsCollection);
      const dateStr = new Date().toISOString();

      const newItem: FeedInventoryItem = {
        id: newItemRef.id,
        name: newName,
        feedType: newFeedType,
        quantity: newQty,
        unit: newUnit,
        unitWeight: newWeight,
        minThreshold: newThreshold,
        costPerUnit: 0,
        lastUpdated: dateStr
      };

      await setDoc(newItemRef, newItem);

      // Record first restock if quantity > 0
      if (newQty > 0) {
        const txCollection = collection(db, "users", activeFarmUid, "feed_inventory_transactions");
        const txRef = doc(txCollection);
        const transaction: FeedInventoryTransaction = {
          id: txRef.id,
          itemId: newItemRef.id,
          itemName: newName,
          type: "Restock",
          quantity: newQty,
          unit: newUnit,
          cost: 0,
          date: dateStr,
          notes: "Initial stock entry"
        };
        await setDoc(txRef, transaction);
      }

      // Reset
      setNewName("");
      setNewQty(0);
      setShowAddModal(false);
    } catch (err) {
      console.error("Failed to add inventory item:", err);
    }
  };

  const handleRestock = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !selectedItemId) return;
    const item = items.find(i => i.id === selectedItemId);
    if (!item) return;

    try {
      // Calculate converted quantity based on unit
      const convertedAdded = restockUnit === item.unit
        ? restockQty
        : restockUnit === "bags" && item.unit === "kg"
        ? restockQty * item.unitWeight
        : restockUnit === "kg" && item.unit === "bags"
        ? restockQty / item.unitWeight
        : restockQty;

      const dateStr = new Date().toISOString();
      const updatedQty = Math.max(0, item.quantity + convertedAdded);
      const costPerUnit = restockQty > 0 ? restockCost / restockQty : item.costPerUnit;

      // 1. Update Inventory Item
      const itemRef = doc(db, "users", activeFarmUid, "feed_inventory", item.id);
      await updateDoc(itemRef, {
        quantity: updatedQty,
        costPerUnit: costPerUnit,
        lastUpdated: dateStr
      });

      // 2. Add Stock Transaction
      const txCollection = collection(db, "users", activeFarmUid, "feed_inventory_transactions");
      const txRef = doc(txCollection);
      const transaction: FeedInventoryTransaction = {
        id: txRef.id,
        itemId: item.id,
        itemName: item.name,
        type: "Restock",
        quantity: restockQty,
        unit: restockUnit,
        cost: restockCost,
        date: dateStr,
        notes: restockNotes
      };
      await setDoc(txRef, transaction);

      // 3. Add Expense Record to Financials
      if (restockCost > 0) {
        const finCollection = collection(db, "users", activeFarmUid, "financials");
        const finRef = doc(finCollection);
        const dateOnly = new Date().toISOString().split("T")[0];
        const desc = `Purchased Feed: ${item.name} (${restockQty} ${restockUnit})`;
        await setDoc(finRef, {
          id: finRef.id,
          date: dateOnly,
          type: "Expense",
          category: "Feed",
          amount: restockCost,
          description: desc
        });
      }

      // Reset
      setRestockQty(0);
      setRestockCost(0);
      setRestockNotes("");
      setShowRestockModal(false);
    } catch (err) {
      console.error("Restock failed:", err);
    }
  };

  const handleUseFeed = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !selectedItemId) return;
    const item = items.find(i => i.id === selectedItemId);
    if (!item) return;

    try {
      // Calculate converted quantity
      const convertedUsed = usageUnit === item.unit
        ? usageQty
        : usageUnit === "bags" && item.unit === "kg"
        ? usageQty * item.unitWeight
        : usageUnit === "kg" && item.unit === "bags"
        ? usageQty / item.unitWeight
        : usageQty;

      const dateStr = new Date().toISOString();
      const updatedQty = Math.max(0, item.quantity - convertedUsed);

      // 1. Update Inventory Item
      const itemRef = doc(db, "users", activeFarmUid, "feed_inventory", item.id);
      await updateDoc(itemRef, {
        quantity: updatedQty,
        lastUpdated: dateStr
      });

      // 2. Add Stock Transaction
      const txCollection = collection(db, "users", activeFarmUid, "feed_inventory_transactions");
      const txRef = doc(txCollection);
      const transaction: FeedInventoryTransaction = {
        id: txRef.id,
        itemId: item.id,
        itemName: item.name,
        type: "Usage",
        quantity: usageQty,
        unit: usageUnit,
        cost: 0,
        date: dateStr,
        notes: usageNotes
      };
      await setDoc(txRef, transaction);

      // Reset
      setUsageQty(0);
      setUsageNotes("");
      setShowUsageModal(false);
    } catch (err) {
      console.error("Failed to log usage:", err);
    }
  };

  const handleDeleteItem = async (itemId: string) => {
    if (!activeFarmUid || !confirm("Are you sure you want to delete this item?")) return;
    try {
      await deleteDoc(doc(db, "users", activeFarmUid, "feed_inventory", itemId));
    } catch (err) {
      console.error("Delete failed:", err);
    }
  };

  const openRestock = (itemId: string) => {
    const item = items.find(i => i.id === itemId);
    if (item) {
      setSelectedItemId(itemId);
      setRestockUnit(item.unit);
      setShowRestockModal(true);
    }
  };

  const openUsage = (itemId: string) => {
    const item = items.find(i => i.id === itemId);
    if (item) {
      setSelectedItemId(itemId);
      setUsageUnit(item.unit);
      setShowUsageModal(true);
    }
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
              🌾 Feed Inventory
            </span>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => window.print()}
              className="rounded-lg border border-zinc-800 bg-zinc-900/40 px-4 py-2 text-xs font-semibold text-zinc-300 hover:bg-zinc-800 hover:text-white transition"
            >
              🖨️ Print Report
            </button>
            <button
              onClick={() => setShowAddModal(true)}
              className="rounded-lg bg-emerald-500 hover:bg-emerald-600 px-4 py-2 text-xs font-bold text-white shadow-lg shadow-emerald-500/15 transition"
            >
              + Add Feed Item
            </button>
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8 space-y-8 print:p-0 print:bg-white print:text-black">
        {/* Low Stock Alert */}
        {items.some(item => item.quantity < item.minThreshold) && (
          <div className="rounded-xl border border-amber-500/20 bg-amber-500/5 p-4 text-sm text-amber-400 flex items-center gap-3 print:hidden">
            <span className="text-xl">⚠️</span>
            <div>
              <p className="font-semibold">Low Stock warning</p>
              <p className="text-zinc-400 mt-0.5">
                Some feed items have fallen below their safety threshold. Consider restocking soon.
              </p>
            </div>
          </div>
        )}

        {/* Inventory Table Card */}
        <div className="bg-zinc-900/30 border border-zinc-900 rounded-2xl p-6 overflow-hidden print:border-none print:p-0">
          <h2 className="text-lg font-bold text-white mb-4 print:text-black">Current Stock Levels</h2>
          {dataLoading ? (
            <div className="space-y-3">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="h-12 bg-zinc-900 animate-pulse rounded-lg" />
              ))}
            </div>
          ) : items.length === 0 ? (
            <p className="text-sm text-zinc-500 text-center py-8">No feed inventory items registered yet.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-zinc-900 print:text-black">
                <thead>
                  <tr className="text-left text-xs font-semibold text-zinc-400 uppercase tracking-wider print:text-black">
                    <th className="pb-3">Name</th>
                    <th className="pb-3">Feed Type</th>
                    <th className="pb-3">Quantity</th>
                    <th className="pb-3">Unit</th>
                    <th className="pb-3">Threshold</th>
                    <th className="pb-3">Last Updated</th>
                    <th className="pb-3 text-right print:hidden">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-900/50">
                  {items.map((item) => {
                    const isLow = item.quantity < item.minThreshold;
                    return (
                      <tr key={item.id} className="text-sm">
                        <td className="py-4 font-medium text-white print:text-black">{item.name}</td>
                        <td className="py-4 text-zinc-400 print:text-black">{item.feedType}</td>
                        <td className="py-4">
                          <span className={`font-semibold ${isLow ? "text-amber-500" : "text-white print:text-black"}`}>
                            {item.quantity.toFixed(1)}
                          </span>
                        </td>
                        <td className="py-4 text-zinc-400 print:text-black">{item.unit}</td>
                        <td className="py-4 text-zinc-400 print:text-black">{item.minThreshold} {item.unit}</td>
                        <td className="py-4 text-zinc-500 print:text-black">
                          {new Date(item.lastUpdated).toLocaleDateString()}
                        </td>
                        <td className="py-4 text-right space-x-2 print:hidden">
                          <button
                            onClick={() => openRestock(item.id)}
                            className="text-xs text-emerald-400 hover:text-emerald-300 font-semibold transition"
                          >
                            Restock
                          </button>
                          <span className="text-zinc-800">|</span>
                          <button
                            onClick={() => openUsage(item.id)}
                            className="text-xs text-violet-400 hover:text-violet-300 font-semibold transition"
                          >
                            Log Usage
                          </button>
                          <span className="text-zinc-800">|</span>
                          <button
                            onClick={() => handleDeleteItem(item.id)}
                            className="text-xs text-red-400 hover:text-red-300 transition"
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Transactions Card */}
        <div className="bg-zinc-900/30 border border-zinc-900 rounded-2xl p-6 print:mt-8">
          <h2 className="text-lg font-bold text-white mb-4 print:text-black">Inventory Transactions</h2>
          {dataLoading ? (
            <div className="space-y-2">
              <div className="h-10 bg-zinc-900 animate-pulse rounded-lg" />
              <div className="h-10 bg-zinc-900 animate-pulse rounded-lg" />
            </div>
          ) : transactions.length === 0 ? (
            <p className="text-sm text-zinc-500 text-center py-6">No inventory transactions logged.</p>
          ) : (
            <div className="space-y-4">
              {transactions.map((tx) => (
                <div key={tx.id} className="flex justify-between items-start border-b border-zinc-900/50 pb-3 text-sm">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className={`h-2 w-2 rounded-full ${tx.type === "Restock" ? "bg-emerald-500" : "bg-violet-600"}`} />
                      <p className="font-semibold text-white print:text-black">{tx.itemName}</p>
                      <span className="text-xs text-zinc-500">({tx.type})</span>
                    </div>
                    {tx.notes && <p className="text-xs text-zinc-400 mt-1 print:text-black">{tx.notes}</p>}
                    <p className="text-xs text-zinc-600 mt-0.5">{new Date(tx.date).toLocaleString()}</p>
                  </div>
                  <div className="text-right">
                    <p className="font-bold text-white print:text-black">
                      {tx.type === "Restock" ? "+" : "-"}{tx.quantity} {tx.unit}
                    </p>
                    {tx.cost > 0 && (
                      <p className="text-xs text-zinc-500 mt-0.5">Cost: ${tx.cost.toFixed(2)}</p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>

      {/* 1. Add Item Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl w-full max-w-md p-6 space-y-6">
            <h3 className="text-lg font-bold text-white">Add New Feed Item</h3>
            <form onSubmit={handleAddItem} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-400 mb-1.5">Name</label>
                <input
                  type="text"
                  required
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-emerald-500 focus:outline-none"
                  placeholder="e.g. Starter Feed Mix"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-400 mb-1.5">Feed Type</label>
                  <select
                    value={newFeedType}
                    onChange={(e) => setNewFeedType(e.target.value)}
                    className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-emerald-500 focus:outline-none"
                  >
                    <option>Starter</option>
                    <option>Grower</option>
                    <option>Finisher</option>
                    <option>Sow</option>
                    <option>Boar</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-400 mb-1.5">Base Unit</label>
                  <select
                    value={newUnit}
                    onChange={(e) => setNewUnit(e.target.value)}
                    className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-emerald-500 focus:outline-none"
                  >
                    <option value="bags">Bags</option>
                    <option value="kg">kg</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-400 mb-1.5">Init Qty</label>
                  <input
                    type="number"
                    step="any"
                    value={newQty}
                    onChange={(e) => setNewQty(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-emerald-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-400 mb-1.5">Bag Wt (kg)</label>
                  <input
                    type="number"
                    step="any"
                    value={newWeight}
                    onChange={(e) => setNewWeight(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-emerald-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-400 mb-1.5">Min Alert</label>
                  <input
                    type="number"
                    step="any"
                    value={newThreshold}
                    onChange={(e) => setNewThreshold(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-800">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="rounded-lg border border-zinc-800 bg-zinc-900/60 px-4 py-2 text-xs font-semibold text-zinc-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="rounded-lg bg-emerald-500 hover:bg-emerald-600 px-4 py-2 text-xs font-bold text-white"
                >
                  Save
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 2. Restock Modal */}
      {showRestockModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl w-full max-w-md p-6 space-y-6">
            <h3 className="text-lg font-bold text-white">Restock Feed Item</h3>
            <form onSubmit={handleRestock} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-400 mb-1.5">Qty Added</label>
                  <input
                    type="number"
                    step="any"
                    required
                    value={restockQty}
                    onChange={(e) => setRestockQty(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-emerald-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-400 mb-1.5">Unit</label>
                  <select
                    value={restockUnit}
                    onChange={(e) => setRestockUnit(e.target.value)}
                    className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-emerald-500"
                  >
                    <option value="bags">Bags</option>
                    <option value="kg">kg</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-400 mb-1.5">Total Cost ($)</label>
                <input
                  type="number"
                  step="any"
                  value={restockCost}
                  onChange={(e) => setRestockCost(parseFloat(e.target.value) || 0)}
                  className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-emerald-500"
                  placeholder="0.00"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-400 mb-1.5">Notes</label>
                <input
                  type="text"
                  value={restockNotes}
                  onChange={(e) => setRestockNotes(e.target.value)}
                  className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-emerald-500"
                  placeholder="e.g. Purchased from local supplier"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-800">
                <button
                  type="button"
                  onClick={() => setShowRestockModal(false)}
                  className="rounded-lg border border-zinc-800 bg-zinc-900/60 px-4 py-2 text-xs font-semibold text-zinc-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="rounded-lg bg-emerald-500 hover:bg-emerald-600 px-4 py-2 text-xs font-bold text-white"
                >
                  Add Stock
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 3. Log Usage Modal */}
      {showUsageModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl w-full max-w-md p-6 space-y-6">
            <h3 className="text-lg font-bold text-white">Log Feed Usage</h3>
            <form onSubmit={handleUseFeed} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-400 mb-1.5">Qty Used</label>
                  <input
                    type="number"
                    step="any"
                    required
                    value={usageQty}
                    onChange={(e) => setUsageQty(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-emerald-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-400 mb-1.5">Unit</label>
                  <select
                    value={usageUnit}
                    onChange={(e) => setUsageUnit(e.target.value)}
                    className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-emerald-500"
                  >
                    <option value="bags">Bags</option>
                    <option value="kg">kg</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-400 mb-1.5">Notes</label>
                <input
                  type="text"
                  value={usageNotes}
                  onChange={(e) => setUsageNotes(e.target.value)}
                  className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-emerald-500"
                  placeholder="e.g. Fed to Finisher Pen 2"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-800">
                <button
                  type="button"
                  onClick={() => setShowUsageModal(false)}
                  className="rounded-lg border border-zinc-800 bg-zinc-900/60 px-4 py-2 text-xs font-semibold text-zinc-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="rounded-lg bg-violet-600 hover:bg-violet-700 px-4 py-2 text-xs font-bold text-white"
                >
                  Log Usage
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
