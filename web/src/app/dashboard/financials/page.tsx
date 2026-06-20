"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot, doc, setDoc, deleteDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";

interface FinancialRecord {
  id: string;
  date: string;
  type: string; // "Income" or "Expense"
  category: string;
  amount: number;
  description: string;
  pigId?: string;
}

interface Pig {
  id: string;
  tagNumber: string;
}

export default function FinancialsPage() {
  const { user, userProfile, activeFarmUid, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  const currencySymbol = userProfile?.settings?.currencySymbol || "$";

  const [records, setRecords] = useState<FinancialRecord[]>([]);
  const [pigs, setPigs] = useState<Pig[]>([]);
  const [dataLoading, setDataLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);

  // Form states
  const [type, setType] = useState("Expense");
  const [date, setDate] = useState(new Date().toISOString().split("T")[0]);
  const [category, setCategory] = useState("Feed");
  const [amount, setAmount] = useState(0);
  const [description, setDescription] = useState("");
  const [selectedPigId, setSelectedPigId] = useState("");

  const categories = {
    Income: ["Pig Sale", "Manure Sale", "Breeding Service", "Equipment Sale", "Other"],
    Expense: ["Feed", "Vet/Medication", "Labor/Salary", "Equipment", "Transport", "Rent", "Utility", "Other"]
  };

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!activeFarmUid) return;

    setDataLoading(true);

    // 1. Listen to Financial Records
    const finRef = collection(db, "users", activeFarmUid, "financials");
    const unsubscribeFin = onSnapshot(finRef, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as FinancialRecord));
      setRecords(list.sort((a, b) => b.date.localeCompare(a.date)));
      setDataLoading(false);
    }, (err) => {
      console.error(err);
      setDataLoading(false);
    });

    // 2. Listen to Pigs for linking transactions
    const pigsRef = collection(db, "users", activeFarmUid, "pigs");
    const unsubscribePigs = onSnapshot(pigsRef, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, tagNumber: doc.data().tagNumber || doc.id } as Pig));
      setPigs(list.sort((a, b) => a.tagNumber.localeCompare(b.tagNumber)));
    });

    return () => {
      unsubscribeFin();
      unsubscribePigs();
    };
  }, [activeFarmUid]);

  const handleAddTransaction = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || amount <= 0) return;

    try {
      const finCollection = collection(db, "users", activeFarmUid, "financials");
      const newRef = doc(finCollection);

      const record: FinancialRecord = {
        id: newRef.id,
        date,
        type,
        category,
        amount,
        description
      };
      if (type === "Income" && category === "Pig Sale" && selectedPigId) {
        record.pigId = selectedPigId;
      }

      await setDoc(newRef, record);

      // Reset
      setAmount(0);
      setDescription("");
      setSelectedPigId("");
      setShowAddModal(false);
    } catch (err) {
      console.error("Failed to log transaction:", err);
    }
  };

  const handleDeleteRecord = async (id: string) => {
    if (!activeFarmUid || !confirm("Are you sure you want to delete this transaction record?")) return;
    try {
      await deleteDoc(doc(db, "users", activeFarmUid, "financials", id));
    } catch (err) {
      console.error(err);
    }
  };

  // Calculations
  const totalIncome = records.filter(r => r.type === "Income").reduce((sum, r) => sum + r.amount, 0);
  const totalExpense = records.filter(r => r.type === "Expense").reduce((sum, r) => sum + r.amount, 0);
  const netBalance = totalIncome - totalExpense;

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-white text-zinc-900">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen bg-white text-zinc-900 flex flex-col font-sans overflow-hidden">
      {/* Watermark Logo Background */}
      {!isMobile && (
        <div className="fixed inset-0 z-0 flex items-center justify-center opacity-[0.15] pointer-events-none select-none">
          <img
            src="/app_logo.png"
            alt="Watermark Background Logo"
            className="w-full max-w-[1100px] max-h-[85vh] object-contain"
          />
        </div>
      )}

      <div className="relative z-10 flex flex-col min-h-screen">
        {!isMobile && (
          <header className="border-b border-zinc-200 bg-white/80 backdrop-blur-md sticky top-0 z-50">
            <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
              <Link href="/dashboard" className="flex items-center gap-3 hover:opacity-80 transition-opacity cursor-pointer">
                <img src="/app_logo.png" alt="SmartSwine Logo" className="h-8 w-8 object-contain rounded-md" />
                <span className="font-bold text-sm bg-gradient-to-r from-emerald-700 via-emerald-600 to-green-500 bg-clip-text text-transparent mr-2">
                  SmartSwine
                </span>
              </Link>

              <div className="flex items-center gap-2">
                <NavbarDropdown />
              </div>
            </div>
          </header>
        )}

        <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8 space-y-6">
          {/* Dashboard balance summaries */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
            {[
              { label: "Total Revenue", amount: totalIncome, color: "text-emerald-700 bg-emerald-50/50 border-emerald-100" },
              { label: "Total Expenses", amount: totalExpense, color: "text-rose-700 bg-rose-50/50 border-rose-100" },
              { label: "Net Cashflow", amount: netBalance, color: netBalance >= 0 ? "text-emerald-800 bg-emerald-100/30 border-emerald-200" : "text-rose-800 bg-rose-100/30 border-rose-200" }
            ].map((stat, i) => (
              <div key={i} className={`backdrop-blur-md border rounded-2xl p-6 shadow-sm bg-white/60 ${stat.color}`}>
                <p className="text-xs font-bold uppercase tracking-wider">{stat.label}</p>
                <p className="text-3xl font-black mt-2">{currencySymbol}{stat.amount.toFixed(2)}</p>
              </div>
            ))}
          </div>

          {/* Ledger Table */}
          <div className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4 flex-wrap gap-4">
              <h2 className="text-lg font-bold text-zinc-900">Farm Cashflow Ledger</h2>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => window.print()}
                  className="rounded-lg border border-zinc-200 bg-zinc-50/50 px-3 py-2 text-xs font-semibold text-zinc-650 hover:bg-zinc-100 transition shadow-sm flex items-center gap-1.5"
                >
                  <svg className="h-3.5 w-3.5 text-zinc-500" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M20 2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-8 11c0 .55-.45 1-1 1H9v2H7.5v-5h3c.55 0 1 .45 1 1v1zm5 2c0 .55-.45 1-1 1h-2.5v-5H16c.55 0 1 .45 1 1v3zm-5.5-4H10v1.5h.5V11zm4.5 1h-.5v2h.5v-2zm2.5 1h-2v-1h2v-1h-2v-1h3.5v5H19v-2z" />
                  </svg>
                  <span>Export PDF</span>
                </button>
                <button
                  onClick={() => setShowAddModal(true)}
                  className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white shadow shadow-emerald-600/10 transition active:scale-95"
                >
                  + Log Transaction
                </button>
              </div>
            </div>
            {dataLoading ? (
              <div className="space-y-3">
                {[...Array(4)].map((_, i) => (
                  <div key={i} className="h-10 bg-zinc-100 animate-pulse rounded-lg" />
                ))}
              </div>
            ) : records.length === 0 ? (
              <p className="text-sm text-zinc-500 text-center py-12">No transactions recorded yet.</p>
            ) : (
              <>
                {/* Desktop Table View */}
                <div className="hidden sm:block overflow-x-auto">
                  <table className="min-w-full divide-y divide-zinc-200 text-sm">
                    <thead>
                      <tr className="text-left text-xs font-semibold text-zinc-500 uppercase tracking-wider border-b border-zinc-200">
                        <th className="pb-3">Date</th>
                        <th className="pb-3">Type</th>
                        <th className="pb-3">Category</th>
                        <th className="pb-3">Description</th>
                        <th className="pb-3">Linked Pig</th>
                        <th className="pb-3 text-right">Amount</th>
                        <th className="pb-3 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-150">
                      {records.map(record => {
                        const linkedPig = pigs.find(p => p.id === record.pigId);
                        return (
                          <tr key={record.id}>
                            <td className="py-4 font-mono text-zinc-500">{record.date}</td>
                            <td className="py-4">
                              <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                                record.type === "Income" ? "bg-emerald-50 text-emerald-800 border border-emerald-100" : "bg-rose-50 text-rose-800 border border-rose-100"
                              }`}>
                                {record.type}
                              </span>
                            </td>
                            <td className="py-4 font-semibold text-zinc-800">{record.category}</td>
                            <td className="py-4 text-zinc-500">{record.description}</td>
                            <td className="py-4 text-zinc-600 font-mono">
                              {linkedPig ? (
                                <Link href={`/dashboard/herd/${record.pigId}`} className="text-emerald-700 hover:underline">
                                  {linkedPig.tagNumber}
                                </Link>
                              ) : "N/A"}
                            </td>
                            <td className={`py-4 text-right font-bold font-mono ${
                              record.type === "Income" ? "text-emerald-700" : "text-rose-700"
                            }`}>
                              {record.type === "Income" ? "+" : "-"}{currencySymbol}{record.amount.toFixed(2)}
                            </td>
                            <td className="py-4 text-right font-medium">
                              <button onClick={() => handleDeleteRecord(record.id)} className="text-xs text-rose-600 hover:underline">
                                Delete
                              </button>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                {/* Mobile Card View */}
                <div className="sm:hidden space-y-4">
                  {records.map(record => {
                    const linkedPig = pigs.find(p => p.id === record.pigId);
                    return (
                      <div key={record.id} className="p-4 rounded-xl border border-zinc-100 bg-zinc-50/50 space-y-3 relative overflow-hidden">
                        <div className="flex justify-between items-start">
                          <div>
                            <p className="text-[10px] font-bold text-zinc-400 font-mono">{record.date}</p>
                            <h4 className="font-bold text-zinc-900">{record.category}</h4>
                          </div>
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                            record.type === "Income" ? "bg-emerald-50 text-emerald-800 border border-emerald-100" : "bg-rose-50 text-rose-800 border border-rose-100"
                          }`}>
                            {record.type}
                          </span>
                        </div>

                        <p className="text-xs text-zinc-600 leading-relaxed">{record.description}</p>

                        <div className="flex justify-between items-end border-t border-zinc-100 pt-3">
                          <div className="text-[10px]">
                            <span className="text-zinc-400 font-semibold uppercase">Linked Pig: </span>
                            {linkedPig ? (
                              <Link href={`/dashboard/herd/${record.pigId}`} className="text-emerald-700 font-bold hover:underline">
                                {linkedPig.tagNumber}
                              </Link>
                            ) : <span className="text-zinc-500 font-bold">N/A</span>}
                          </div>
                          <div className="text-right">
                             <p className={`text-sm font-black font-mono ${record.type === "Income" ? "text-emerald-700" : "text-rose-700"}`}>
                                {record.type === "Income" ? "+" : "-"}{currencySymbol}{record.amount.toFixed(2)}
                             </p>
                             <button onClick={() => handleDeleteRecord(record.id)} className="text-[10px] font-bold text-rose-500 mt-1 uppercase tracking-tight">
                                Delete Entry
                             </button>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </>
            )}
          </div>
        </main>
      </div>

      {/* Log Transaction Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-md p-6 space-y-6 shadow-2xl">
            <h3 className="text-lg font-bold text-zinc-900">Log Transaction</h3>
            <form onSubmit={handleAddTransaction} className="space-y-4">
              <div className="flex gap-2 p-1 bg-zinc-100 rounded-lg">
                <button
                  type="button"
                  onClick={() => { setType("Expense"); setCategory("Feed"); }}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-md transition ${type === "Expense" ? "bg-white text-zinc-800 shadow" : "text-zinc-500"}`}
                >
                  Expense
                </button>
                <button
                  type="button"
                  onClick={() => { setType("Income"); setCategory("Pig Sale"); }}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-md transition ${type === "Income" ? "bg-white text-zinc-800 shadow" : "text-zinc-500"}`}
                >
                  Income
                </button>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Date</label>
                  <input
                    type="date"
                    required
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Category</label>
                  <select
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    {((type === "Income" ? categories.Income : categories.Expense)).map(cat => (
                      <option key={cat}>{cat}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className={type === "Income" && category === "Pig Sale" ? "grid grid-cols-2 gap-4" : "grid grid-cols-1 gap-4"}>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Amount ({currencySymbol})</label>
                  <input
                    type="number"
                    step="any"
                    required
                    value={amount}
                    onChange={(e) => setAmount(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
                {type === "Income" && category === "Pig Sale" && (
                  <div>
                    <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Link Pig (Optional)</label>
                    <select
                      value={selectedPigId}
                      onChange={(e) => setSelectedPigId(e.target.value)}
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                    >
                      <option value="">None</option>
                      {pigs.map(p => (
                        <option key={p.id} value={p.id}>{p.tagNumber}</option>
                      ))}
                    </select>
                  </div>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Description</label>
                <textarea
                  required
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="e.g. Purchased 10 bags of starter feed from feed shop."
                  rows={2}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:bg-zinc-100 transition"
                >
                  Cancel
                </button>
                <button type="submit" className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition">
                  Save
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
