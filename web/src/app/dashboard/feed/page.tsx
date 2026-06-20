"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot, doc, setDoc, deleteDoc, updateDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import {
  InventoryIcon,
  ScienceIcon,
  CalculateIcon,
} from "@/components/icons/DashboardIcons";
import {
  formulateFeed,
  FeedIngredient,
  NutritionalRequirement,
  FormulationResult
} from "@/lib/feedCalculator";

// SVG Icons matching Android Material Icons
const PrintIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" {...props}>
    <path d="M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z" />
  </svg>
);

const WarningIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" {...props}>
    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z" />
  </svg>
);

const PdfIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" {...props}>
    <path d="M20 2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-8 11c0 .55-.45 1-1 1H9v2H7.5v-5h3c.55 0 1 .45 1 1v1zm5 2c0 .55-.45 1-1 1h-2.5v-5H16c.55 0 1 .45 1 1v3zm-5.5-4H10v1.5h.5V11zm4.5 1h-.5v2h.5v-2zm2.5 1h-2v-1h2v-1h-2v-1h3.5v5H19v-2z" />
  </svg>
);

const TABS_CONFIG = [
  { id: "inventory", label: "Feed Inventory", icon: InventoryIcon },
  { id: "formulator", label: "Feed Formulator", icon: ScienceIcon },
  { id: "calculator", label: "Feed Calculator", icon: CalculateIcon }
] as const;

// Interfaces
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

const defaultRequirements: NutritionalRequirement[] = [
  { stage: "Starter", digestibleProtein: 17.0, metabolizableEnergy: 3350.0, calcium: 0.90, phosphorus: 0.75, lysine: 7.90, methionineCystine: 5.20, tryptophan: 1.25, crudeFiber: 3.0, minDailyFeed: 0.35, maxDailyFeed: 0.85 },
  { stage: "Grower", digestibleProtein: 14.5, metabolizableEnergy: 3300.0, calcium: 0.75, phosphorus: 0.50, lysine: 6.10, methionineCystine: 4.00, tryptophan: 1.10, crudeFiber: 5.0, minDailyFeed: 0.75, maxDailyFeed: 1.50 },
  { stage: "Finisher", digestibleProtein: 13.0, metabolizableEnergy: 3300.0, calcium: 0.75, phosphorus: 0.50, lysine: 5.70, methionineCystine: 3.00, tryptophan: 1.00, crudeFiber: 6.0, minDailyFeed: 1.50, maxDailyFeed: 2.50 }
];

export default function FeedPage() {
  const { user, activeFarmUid, loading, userProfile } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  // Tab State: "inventory" | "formulator" | "calculator"
  const [activeTab, setActiveTab] = useState<"inventory" | "formulator" | "calculator">("inventory");

  // ==================== INVENTORY STATES ====================
  const [items, setItems] = useState<FeedInventoryItem[]>([]);
  const [transactions, setTransactions] = useState<FeedInventoryTransaction[]>([]);
  const [dataLoading, setDataLoading] = useState(true);

  // Form modals
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

  // ==================== FORMULATOR STATES ====================
  const [ingredients, setIngredients] = useState<FeedIngredient[]>([]);
  const [requirements, setRequirements] = useState<NutritionalRequirement[]>(defaultRequirements);
  const [selectedStage, setSelectedStage] = useState("Starter");
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [formulation, setFormulation] = useState<FormulationResult | null>(null);
  const [formulatorError, setFormulatorError] = useState<string | null>(null);

  // Collapses
  const [energyCollapsed, setEnergyCollapsed] = useState(true);
  const [proteinCollapsed, setProteinCollapsed] = useState(true);
  const [mineralsCollapsed, setMineralsCollapsed] = useState(true);

  // ==================== CALCULATOR STATES ====================
  const [sowsCount, setSowsCount] = useState(0);
  const [boarsCount, setBoarsCount] = useState(0);
  const [giltsCount, setGiltsCount] = useState(0);
  const [pregnantCount, setPregnantCount] = useState(0);
  const [lactatingCount, setLactatingCount] = useState(0);
  const [starterCount, setStarterCount] = useState(0);
  const [growerCount, setGrowerCount] = useState(0);
  const [finisherCount, setFinisherCount] = useState(0);
  const [calcDays, setCalcDays] = useState(1);
  const [calcResults, setCalcResults] = useState<any | null>(null);

  // ==================== PDF EXPORT & PRINT STATES ====================
  const [printData, setPrintData] = useState<{
    type: "inventory" | "formulator" | "calculator";
    title: string;
    details: any;
  } | null>(null);

  const [showExportModal, setShowExportModal] = useState(false);
  const [exportRange, setExportRange] = useState<"Current Month" | "Last 3 Months" | "Custom">("Current Month");
  const [exportFromDate, setExportFromDate] = useState("");
  const [exportToDate, setExportToDate] = useState("");

  const handleExportFeedInventoryPdf = () => {
    const today = new Date();
    const todayStr = today.toISOString().split("T")[0];
    let startDateStr = "";
    let endDateStr = todayStr;
    let rangeLabel = "";

    if (exportRange === "Current Month") {
      const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
      startDateStr = firstDay.toISOString().split("T")[0];
      rangeLabel = "Current Month";
    } else if (exportRange === "Last 3 Months") {
      const pastDate = new Date();
      pastDate.setMonth(today.getMonth() - 3);
      startDateStr = pastDate.toISOString().split("T")[0];
      rangeLabel = "Last 3 Months";
    } else {
      startDateStr = exportFromDate || "1970-01-01";
      endDateStr = exportToDate || todayStr;
      rangeLabel = `Custom (${startDateStr} to ${endDateStr})`;
    }

    // Filter transactions
    const filteredTx = transactions.filter(tx => {
      const txDate = tx.date.split("T")[0];
      return txDate >= startDateStr && txDate <= endDateStr;
    });

    setPrintData({
      type: "inventory",
      title: `Feed Inventory Report - ${rangeLabel}${exportRange === "Custom" ? ` (${startDateStr} to ${endDateStr})` : ""}`,
      details: {
        items,
        transactions: filteredTx,
        startDate: startDateStr,
        endDate: endDateStr
      }
    });

    setShowExportModal(false);

    setTimeout(() => {
      window.print();
    }, 150);
  };

  const handleExportFormulationPdf = () => {
    if (!formulation) return;
    setPrintData({
      type: "formulator",
      title: `Feed Formulation Report - Formulation for ${selectedStage}`,
      details: {
        stage: selectedStage,
        formulation,
        ingredients
      }
    });

    setTimeout(() => {
      window.print();
    }, 150);
  };

  const handleExportCalculatorPdf = () => {
    if (!calcResults) return;
    setPrintData({
      type: "calculator",
      title: "Feed Requirements Report",
      details: {
        calcResults
      }
    });

    setTimeout(() => {
      window.print();
    }, 150);
  };

  // ==================== EFFECT LISTENERS ====================
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

    // 3. Listen to Ingredients for Formulator
    const ingredientsQuery = collection(db, "users", activeFarmUid, "feed_ingredients");
    const unsubscribeIngredients = onSnapshot(ingredientsQuery, (snapshot) => {
      const rawList = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as FeedIngredient));
      
      // Deduplicate by name, preferring "Vitamins, Minerals & Salt" or "Energy" over "Protein" if name is duplicated
      const uniqueMap = new Map<string, FeedIngredient>();
      rawList.forEach(ing => {
        const key = ing.name.trim().toLowerCase();
        const existing = uniqueMap.get(key);
        if (!existing) {
          uniqueMap.set(key, ing);
        } else {
          const newCat = ing.mainCategory;
          const oldCat = existing.mainCategory;
          if (
            (newCat === "Vitamins, Minerals & Salt" || newCat === "Energy") &&
            oldCat === "Protein"
          ) {
            uniqueMap.set(key, ing);
          }
        }
      });
      
      const list = Array.from(uniqueMap.values());
      setIngredients(list.sort((a, b) => a.name.localeCompare(b.name)));
    });

    // 4. Listen to Requirements for Formulator
    const reqQuery = collection(db, "users", activeFarmUid, "nutritional_requirements");
    const unsubscribeReq = onSnapshot(reqQuery, (snapshot) => {
      if (!snapshot.empty) {
        const list = snapshot.docs.map(doc => doc.data() as NutritionalRequirement);
        setRequirements(list);
      }
    });

    return () => {
      unsubscribeItems();
      unsubscribeTx();
      unsubscribeIngredients();
      unsubscribeReq();
    };
  }, [activeFarmUid]);

  // ==================== INVENTORY ACTIONS ====================
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

      if (newQty > 0) {
        const txCollection = collection(db, "users", activeFarmUid, "feed_inventory_transactions");
        const txRef = doc(txCollection);
        await setDoc(txRef, {
          id: txRef.id,
          itemId: newItemRef.id,
          itemName: newName,
          type: "Restock",
          quantity: newQty,
          unit: newUnit,
          cost: 0,
          date: dateStr,
          notes: "Initial stock entry"
        });
      }

      setNewName("");
      setNewQty(0);
      setShowAddModal(false);
    } catch (err) {
      console.error(err);
    }
  };

  const handleRestock = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !selectedItemId) return;
    const item = items.find(i => i.id === selectedItemId);
    if (!item) return;

    try {
      const convertedAdded = restockUnit === item.unit
        ? restockQty
        : restockUnit === "bags" && item.unit === "kg"
        ? restockQty * item.unitWeight
        : restockQty / item.unitWeight;

      const dateStr = new Date().toISOString();
      const updatedQty = Math.max(0, item.quantity + convertedAdded);
      const costPerUnit = restockQty > 0 ? restockCost / restockQty : item.costPerUnit;

      await updateDoc(doc(db, "users", activeFarmUid, "feed_inventory", item.id), {
        quantity: updatedQty,
        costPerUnit,
        lastUpdated: dateStr
      });

      await setDoc(doc(collection(db, "users", activeFarmUid, "feed_inventory_transactions")), {
        id: doc(collection(db, "users", activeFarmUid, "feed_inventory_transactions")).id,
        itemId: item.id,
        itemName: item.name,
        type: "Restock",
        quantity: restockQty,
        unit: restockUnit,
        cost: restockCost,
        date: dateStr,
        notes: restockNotes
      });

      if (restockCost > 0) {
        const finRef = doc(collection(db, "users", activeFarmUid, "financials"));
        await setDoc(finRef, {
          id: finRef.id,
          date: new Date().toISOString().split("T")[0],
          type: "Expense",
          category: "Feed",
          amount: restockCost,
          description: `Purchased Feed: ${item.name} (${restockQty} ${restockUnit})`
        });
      }

      setRestockQty(0);
      setRestockCost(0);
      setRestockNotes("");
      setShowRestockModal(false);
    } catch (err) {
      console.error(err);
    }
  };

  const handleUseFeed = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !selectedItemId) return;
    const item = items.find(i => i.id === selectedItemId);
    if (!item) return;

    try {
      const convertedUsed = usageUnit === item.unit
        ? usageQty
        : usageUnit === "bags" && item.unit === "kg"
        ? usageQty * item.unitWeight
        : usageQty / item.unitWeight;

      const dateStr = new Date().toISOString();
      const updatedQty = Math.max(0, item.quantity - convertedUsed);

      await updateDoc(doc(db, "users", activeFarmUid, "feed_inventory", item.id), {
        quantity: updatedQty,
        lastUpdated: dateStr
      });

      await setDoc(doc(collection(db, "users", activeFarmUid, "feed_inventory_transactions")), {
        id: doc(collection(db, "users", activeFarmUid, "feed_inventory_transactions")).id,
        itemId: item.id,
        itemName: item.name,
        type: "Usage",
        quantity: usageQty,
        unit: usageUnit,
        cost: 0,
        date: dateStr,
        notes: usageNotes
      });

      setUsageQty(0);
      setUsageNotes("");
      setShowUsageModal(false);
    } catch (err) {
      console.error(err);
    }
  };

  const handleDeleteItem = async (itemId: string) => {
    if (!activeFarmUid || !confirm("Are you sure you want to delete this item?")) return;
    try {
      await deleteDoc(doc(db, "users", activeFarmUid, "feed_inventory", itemId));
    } catch (err) {
      console.error(err);
    }
  };

  // ==================== FORMULATOR ACTIONS ====================
  const handleToggleSelect = (id: string) => {
    setSelectedIds(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  };

  const handleSelectAll = (category: string) => {
    const catIds = ingredients.filter(ing => ing.mainCategory === category).map(ing => ing.id);
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
      setFormulatorError("Target stage config not found.");
      return;
    }
    const result = formulateFeed(targetReq, ingredients, selectedIds);
    setFormulation(result);
    if (result.error) setFormulatorError(result.error);
  };

  // ==================== CALCULATOR ACTIONS ====================
  const handleCalculateFeedProjections = () => {
    // intake values (kg per animal per day)
    const rates = {
      Sows: { rate: 2.2, label: "Sows (dry/breeders)" },
      Boars: { rate: 2.2, label: "Boars" },
      Gilts: { rate: 2.2, label: "Gilts" },
      Pregnant: { rate: 2.2, label: "Pregnant Sows" },
      Lactating: { rate: 5.5, label: "Lactating Sows" },
      Starter: { rate: 0.7, label: "Starter Piglets" },
      Grower: { rate: 1.8, label: "Grower Pigs" },
      Finisher: { rate: 2.5, label: "Finisher Pigs" }
    };

    const breakdown: any[] = [];
    let grandDailyTotal = 0;

    const inputs = [
      { key: "Sows", count: sowsCount },
      { key: "Boars", count: boarsCount },
      { key: "Gilts", count: giltsCount },
      { key: "Pregnant", count: pregnantCount },
      { key: "Lactating", count: lactatingCount },
      { key: "Starter", count: starterCount },
      { key: "Grower", count: growerCount },
      { key: "Finisher", count: finisherCount }
    ];

    inputs.forEach(inp => {
      if (inp.count > 0) {
        const rateObj = (rates as any)[inp.key];
        const dailyReq = inp.count * rateObj.rate;
        grandDailyTotal += dailyReq;
        breakdown.push({
          category: rateObj.label,
          count: inp.count,
          rate: rateObj.rate,
          dailyTotal: dailyReq,
          periodTotal: dailyReq * calcDays
        });
      }
    });

    setCalcResults({
      breakdown,
      dailyTotal: grandDailyTotal,
      periodTotal: grandDailyTotal * calcDays,
      days: calcDays
    });
  };

  // Helper render for collapsible category lists in Formulator
  const renderGroupList = (category: string, collapsed: boolean, setCollapsed: (val: boolean) => void) => {
    const list = ingredients.filter(ing => ing.mainCategory === category);

    return (
      <div className="bg-zinc-50/75 backdrop-blur-md border border-zinc-200 rounded-xl overflow-hidden shadow-sm">
        <button
          type="button"
          onClick={() => setCollapsed(!collapsed)}
          className="w-full bg-zinc-100/60 px-4 py-3 flex items-center justify-between border-b border-zinc-200/50 hover:bg-zinc-200/60 transition-colors"
        >
          <div className="flex items-center gap-2 font-bold text-zinc-800 text-xs">
            <span>{collapsed ? "▶" : "▼"}</span>
            <span>{category} ({list.length})</span>
          </div>
        </button>
        {!collapsed && (
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
                      className="h-4 w-4 rounded border-zinc-300 text-emerald-600 focus:ring-emerald-500"
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

      <div className="relative z-10 flex flex-col min-h-screen print:hidden">
        {!isMobile && (
          <header className="border-b border-zinc-200 bg-white/80 backdrop-blur-md sticky top-0 z-50">
            <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
              <Link href="/dashboard" className="flex items-center gap-3 hover:opacity-80 transition-opacity cursor-pointer">
                <img src="/app_logo.png" alt="SmartSwine Logo" className="h-8 w-8 object-contain rounded-md" />
                <span className="font-bold text-sm bg-gradient-to-r from-emerald-700 via-emerald-600 to-green-500 bg-clip-text text-transparent mr-2 inline-block">
                  SmartSwine
                </span>
              </Link>

              <div className="flex items-center gap-2">
                <NavbarDropdown />
              </div>
            </div>
          </header>
        )}

        <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8 space-y-12 print:p-0">

          {/* ==================== SECTION 1: INVENTORY ==================== */}
          <div className="space-y-6">
            {items.some(item => item.quantity < item.minThreshold) && (
              <div className="rounded-xl border border-amber-200 bg-amber-50/60 p-4 text-xs text-amber-800 flex items-center gap-3 print:hidden shadow-sm">
                <WarningIcon className="h-5 w-5 text-amber-600 shrink-0" />
                <div>
                  <p className="font-bold">Low Stock Warning</p>
                  <p className="text-zinc-650 mt-0.5">Some feed items have fallen below their safety threshold. Please restock.</p>
                </div>
              </div>
            )}

            <div className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm overflow-hidden print:border-none print:p-0">
              {/* Section header — Android style */}
              <div className="flex items-center justify-between mb-5 flex-wrap gap-4">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-xl bg-emerald-50 flex items-center justify-center flex-shrink-0">
                    <InventoryIcon className="h-5 w-5 text-emerald-600" />
                  </div>
                  <div>
                    <h2 className="text-sm font-black text-zinc-900 print:text-black">Feed Inventory</h2>
                    <p className="text-xs text-zinc-500 mt-0.5">Track and manage your feed stock</p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setShowExportModal(true)}
                    className="rounded-lg border border-zinc-200 bg-zinc-50/50 px-4 py-2 text-xs font-semibold text-zinc-650 hover:bg-zinc-100 transition shadow-sm flex items-center gap-1.5"
                  >
                    <PdfIcon className="h-3.5 w-3.5 text-zinc-500" />
                    <span>Export PDF</span>
                  </button>
                  <button
                    onClick={() => setShowAddModal(true)}
                    className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white shadow shadow-emerald-600/10 transition active:scale-95"
                  >
                    + Add Feed Item
                  </button>
                </div>
              </div>

              {dataLoading ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
                  {[...Array(3)].map((_, i) => (
                    <div key={i} className="h-44 bg-zinc-100 animate-pulse rounded-2xl" />
                  ))}
                </div>
              ) : items.length === 0 ? (
                <p className="text-sm text-zinc-500 text-center py-8">No feed items registered yet.</p>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
                  {items.map((item) => {
                    const isLow = item.quantity < item.minThreshold;
                    const stockPct = item.minThreshold > 0
                      ? Math.min(100, (item.quantity / (item.minThreshold * 3)) * 100)
                      : 100;
                    return (
                      <div key={item.id} className={`bg-white/80 border rounded-2xl p-5 shadow-sm flex flex-col gap-3 transition ${isLow ? "border-amber-300 shadow-amber-100" : "border-zinc-200"}`}>
                        {/* Card Header */}
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex-1 min-w-0">
                            <p className="font-bold text-zinc-900 text-sm truncate">{item.name}</p>
                            <span className="mt-1 inline-block px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[10px] font-bold border border-emerald-100">{item.feedType}</span>
                          </div>
                          <div className="flex items-center gap-1.5 flex-shrink-0">
                            {isLow && (
                              <span className="px-1.5 py-0.5 rounded-full bg-amber-100 text-amber-700 text-[9px] font-black uppercase tracking-wide">Low</span>
                            )}
                            <button
                              onClick={() => handleDeleteItem(item.id)}
                              className="text-zinc-300 hover:text-rose-500 transition p-1 rounded-lg hover:bg-rose-50"
                              title="Delete item"
                            >
                              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                                <path strokeLinecap="round" strokeLinejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                              </svg>
                            </button>
                          </div>
                        </div>

                        {/* Quantity */}
                        <div>
                          <div className="flex items-baseline justify-between mb-1.5">
                            <span className={`text-3xl font-black tracking-tight ${isLow ? "text-amber-600" : "text-zinc-900"}`}>
                              {item.quantity.toFixed(1)}
                            </span>
                            <span className="text-xs text-zinc-500 font-semibold">{item.unit}</span>
                          </div>
                          <div className="h-1.5 rounded-full bg-zinc-100 overflow-hidden">
                            <div
                              className={`h-full rounded-full transition-all duration-500 ${isLow ? "bg-amber-400" : "bg-emerald-500"}`}
                              style={{ width: `${stockPct}%` }}
                            />
                          </div>
                          <p className="text-[10px] text-zinc-400 mt-1">Min threshold: {item.minThreshold} {item.unit}</p>
                        </div>

                        {/* Actions */}
                        <div className="flex gap-2 mt-auto pt-1">
                          <button
                            onClick={() => { setSelectedItemId(item.id); setUsageUnit(item.unit); setShowUsageModal(true); }}
                            className="flex-1 py-2 text-xs font-bold text-violet-700 border border-violet-200 rounded-xl hover:bg-violet-50 transition active:scale-95"
                          >
                            Log Usage
                          </button>
                          <button
                            onClick={() => { setSelectedItemId(item.id); setRestockUnit(item.unit); setShowRestockModal(true); }}
                            className="flex-1 py-2 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-xl transition active:scale-95"
                          >
                            Restock
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            <div className="bg-zinc-50/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm">
              {/* Section header — Android style */}
              <div className="flex items-center gap-3 mb-5">
                <div className="h-10 w-10 rounded-xl bg-zinc-100 flex items-center justify-center flex-shrink-0">
                  <svg className="h-5 w-5 text-zinc-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
                  </svg>
                </div>
                <div>
                  <h3 className="text-sm font-black text-zinc-900">Transaction History</h3>
                  <p className="text-xs text-zinc-500 mt-0.5">Recent inventory movements</p>
                </div>
              </div>

              {dataLoading ? (
                <div className="h-10 bg-zinc-150 animate-pulse rounded-lg" />
              ) : transactions.length === 0 ? (
                <p className="text-sm text-zinc-550 text-center py-6">No inventory transactions logged.</p>
              ) : (
                <div className="space-y-3">
                  {transactions.slice(0, 10).map(tx => {
                    const isRestock = tx.type === "Restock";
                    return (
                      <div key={tx.id} className="flex items-start gap-3 bg-white/80 border border-zinc-100 rounded-xl p-4 shadow-sm">
                        {/* Icon */}
                        <div className={`flex-shrink-0 h-9 w-9 rounded-full flex items-center justify-center ${isRestock ? "bg-emerald-50" : "bg-violet-50"}`}>
                          {isRestock ? (
                            <svg className="h-4 w-4 text-emerald-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.5}>
                              <path strokeLinecap="round" strokeLinejoin="round" d="M5 10l7-7m0 0l7 7m-7-7v18" />
                            </svg>
                          ) : (
                            <svg className="h-4 w-4 text-violet-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.5}>
                              <path strokeLinecap="round" strokeLinejoin="round" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
                            </svg>
                          )}
                        </div>
                        {/* Content */}
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 flex-wrap">
                            <p className="font-bold text-zinc-800 text-sm">{tx.itemName}</p>
                            <span className={`text-[10px] font-black px-1.5 py-0.5 rounded-full ${isRestock ? "bg-emerald-100 text-emerald-700" : "bg-violet-100 text-violet-700"}`}>
                              {tx.type}
                            </span>
                          </div>
                          {tx.notes && <p className="text-xs text-zinc-500 mt-0.5 truncate">{tx.notes}</p>}
                          <p className="text-[10px] text-zinc-400 mt-1">{new Date(tx.date).toLocaleString()}</p>
                        </div>
                        {/* Right */}
                        <div className="text-right flex-shrink-0">
                          <p className={`font-bold text-sm ${isRestock ? "text-emerald-600" : "text-violet-600"}`}>
                            {isRestock ? "+" : "-"}{tx.quantity} {tx.unit}
                          </p>
                          {tx.cost > 0 && <p className="text-[10px] text-zinc-400 mt-0.5">${tx.cost.toFixed(2)}</p>}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          <hr className="border-zinc-200" />

          {/* ==================== SECTION 2: FORMULATOR ==================== */}
          <div className="space-y-6">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-xl bg-purple-50 flex items-center justify-center flex-shrink-0">
                <ScienceIcon className="h-5 w-5 text-purple-600" />
              </div>
              <div>
                <h2 className="text-sm font-black text-zinc-900">Feed Formulator</h2>
                <p className="text-xs text-zinc-500 mt-0.5">Scientific ration balancing</p>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
              <div className="lg:col-span-5 space-y-6">
                <div className="bg-zinc-50/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 space-y-3 shadow-sm">
                  <h3 className="text-xs font-bold text-zinc-500 uppercase tracking-wider">Target Growth Stage</h3>
                  <select
                    value={selectedStage}
                    onChange={(e) => setSelectedStage(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2.5 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  >
                    {requirements
                      .filter((req) => ["Starter", "Grower", "Finisher"].includes(req.stage))
                      .map((req) => (
                        <option key={req.stage} value={req.stage}>
                          {req.stage} (CP target: {req.digestibleProtein}%)
                        </option>
                      ))}
                  </select>
                </div>

                <div className="space-y-4">
                  <h3 className="text-xs font-bold text-zinc-500 uppercase tracking-wider px-2">Select Ingredients</h3>
                  {renderGroupList("Energy", energyCollapsed, setEnergyCollapsed)}
                  {renderGroupList("Protein", proteinCollapsed, setProteinCollapsed)}
                  {renderGroupList("Vitamins, Minerals & Salt", mineralsCollapsed, setMineralsCollapsed)}
                </div>

                <button
                  onClick={handleFormulate}
                  className="w-full rounded-lg bg-emerald-600 hover:bg-emerald-700 py-3 text-xs font-bold text-white shadow shadow-emerald-600/10 transition active:scale-95"
                >
                  Formulate Ration
                </button>
              </div>

              <div className="lg:col-span-7 space-y-6">
                {formulatorError && (
                  <div className="rounded-xl border border-rose-200 bg-rose-50/80 p-4 text-xs text-rose-800 font-medium shadow-sm">
                    {formulatorError}
                  </div>
                )}

                {!formulation ? (
                  <div className="h-96 flex flex-col items-center justify-center border border-dashed border-zinc-200 rounded-2xl text-center p-6 bg-zinc-50/40 backdrop-blur-sm shadow-inner">
                    <ScienceIcon className="h-10 w-10 text-zinc-400 mb-3" />
                    <p className="font-semibold text-zinc-500 text-sm">No Active Formulation</p>
                    <p className="text-xs text-zinc-400 mt-1">Select growth stage, check ingredients, and formulate.</p>
                  </div>
                ) : (
                  <div className="space-y-6">
                    <div className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm">
                      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-4 gap-4">
                        <h3 className="text-lg font-bold text-zinc-900">Feed Formula: {selectedStage} Mix</h3>
                        <button
                          onClick={handleExportFormulationPdf}
                          className="w-full sm:w-auto rounded-lg border border-zinc-200 bg-zinc-50/50 px-3 py-1.5 text-xs font-semibold text-zinc-650 hover:bg-zinc-100 transition shadow-sm flex items-center justify-center gap-1.5"
                        >
                          <PdfIcon className="h-3.5 w-3.5 text-zinc-500" />
                          <span>Export PDF</span>
                        </button>
                      </div>
                      <div className="space-y-3 divide-y divide-zinc-100">
                        {Object.entries(formulation.ingredients).map(([id, percent]) => {
                          const ing = ingredients.find(i => i.id === id || i.name === id);
                          const name = ing ? ing.name : id;
                          return (
                            <div key={id} className="pt-2.5 first:pt-0 flex justify-between items-center text-sm">
                              <span className="text-zinc-700 font-medium">{name}</span>
                              <div className="flex flex-col items-end">
                                <span className="font-mono font-bold text-zinc-900">{percent.toFixed(1)}%</span>
                                <span className="text-[10px] text-zinc-500 font-semibold">{(percent * 10).toFixed(1)} kg/ton</span>
                              </div>
                            </div>
                          );
                        })}
                        <div className="pt-3 flex justify-between items-center font-bold text-sm text-zinc-900">
                          <span>Total mix</span>
                          <div className="flex flex-col items-end">
                            <span className="font-mono">{formulation.totalPercentage.toFixed(1)}%</span>
                            <span className="text-[10px] text-zinc-500 font-semibold">1000 kg</span>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm">
                      <h3 className="text-lg font-bold text-zinc-900 mb-4">Nutritional Analysis</h3>
                      <div className="overflow-x-auto -mx-6 sm:mx-0">
                        <div className="inline-block min-w-full align-middle sm:px-0 px-6">
                          <table className="min-w-full divide-y divide-zinc-200 text-sm">
                            <thead>
                              <tr className="text-left text-xs font-semibold text-zinc-500 uppercase tracking-wider">
                                <th className="pb-3">Nutrient</th>
                                <th className="pb-3 text-right">Target</th>
                                <th className="pb-3 text-right">Actual</th>
                                <th className="pb-3 text-right">Status</th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-zinc-150">
                              {formulation.nutritionalComparison.map((nutrient) => (
                                <tr key={nutrient.label}>
                                  <td className="py-3 font-semibold text-zinc-700 whitespace-nowrap pr-4">{nutrient.label}</td>
                                  <td className="py-3 text-right font-mono text-zinc-500">{nutrient.target.toFixed(2)}</td>
                                  <td className="py-3 text-right font-mono">
                                    <span className={nutrient.isDeficient ? "text-rose-600 font-bold" : "text-emerald-600 font-bold"}>
                                      {nutrient.actual.toFixed(2)}
                                    </span>
                                  </td>
                                  <td className="py-3 text-right font-semibold">
                                    <span className={nutrient.isDeficient ? "text-rose-600" : "text-emerald-600"}>
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
                  </div>
                )}
              </div>
            </div>
          </div>

          <hr className="border-zinc-200" />

          {/* ==================== SECTION 3: CALCULATOR ==================== */}
          <div className="space-y-6">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-xl bg-blue-50 flex items-center justify-center flex-shrink-0">
                <CalculateIcon className="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <h2 className="text-sm font-black text-zinc-900">Feed Calculator</h2>
                <p className="text-xs text-zinc-500 mt-0.5">Projected herd demand</p>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
              <div className="lg:col-span-5 bg-zinc-50/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-4">
                <h3 className="text-lg font-bold text-zinc-900">Feed Demand Calculator</h3>
                <p className="text-xs text-zinc-500">Verify your herd counts and enter the calculation projection period.</p>

                <div className="space-y-3">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold text-zinc-500 mb-1">Sows count</label>
                      <input type="number" min="0" value={sowsCount} onChange={(e) => setSowsCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm" />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-zinc-500 mb-1">Boars count</label>
                      <input type="number" min="0" value={boarsCount} onChange={(e) => setBoarsCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm" />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold text-zinc-500 mb-1">Gilts count</label>
                      <input type="number" min="0" value={giltsCount} onChange={(e) => setGiltsCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm" />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-zinc-500 mb-1">Pregnant Sows</label>
                      <input type="number" min="0" value={pregnantCount} onChange={(e) => setPregnantCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm" />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold text-zinc-500 mb-1">Lactating Sows</label>
                      <input type="number" min="0" value={lactatingCount} onChange={(e) => setLactatingCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm" />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-zinc-500 mb-1">Starter piglets</label>
                      <input type="number" min="0" value={starterCount} onChange={(e) => setStarterCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm" />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold text-zinc-500 mb-1">Growers</label>
                      <input type="number" min="0" value={growerCount} onChange={(e) => setGrowerCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm" />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-zinc-500 mb-1">Finishers</label>
                      <input type="number" min="0" value={finisherCount} onChange={(e) => setFinisherCount(parseInt(e.target.value) || 0)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm" />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-zinc-500 mb-1">Projection Period (Days)</label>
                    <input type="number" min="1" value={calcDays} onChange={(e) => setCalcDays(parseInt(e.target.value) || 1)} className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm" />
                  </div>
                </div>

                <button
                  onClick={handleCalculateFeedProjections}
                  className="w-full rounded-lg bg-emerald-600 hover:bg-emerald-700 py-3 text-xs font-bold text-white shadow shadow-emerald-600/10 transition active:scale-95"
                >
                  Generate Projection Report
                </button>
              </div>

              <div className="lg:col-span-7">
                {!calcResults ? (
                  <div className="h-96 flex flex-col items-center justify-center border border-dashed border-zinc-200 rounded-2xl text-center p-6 bg-zinc-50/40 backdrop-blur-sm shadow-inner">
                    <CalculateIcon className="h-10 w-10 text-zinc-400 mb-3" />
                    <p className="font-semibold text-zinc-500 text-sm">No Active Calculation</p>
                    <p className="text-xs text-zinc-400 mt-1">Configure counts on the left and click calculate.</p>
                  </div>
                ) : (
                  <div className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-6">
                    <div className="flex justify-between items-center">
                      <h3 className="text-lg font-bold text-zinc-900">Projected Feed Demand Results</h3>
                      <button
                        onClick={handleExportCalculatorPdf}
                        className="rounded-lg border border-zinc-200 bg-zinc-50/50 px-3 py-1.5 text-xs font-semibold text-zinc-650 hover:bg-zinc-100 transition shadow-sm flex items-center gap-1.5"
                      >
                        <PdfIcon className="h-3.5 w-3.5 text-zinc-500" />
                        <span>Export PDF</span>
                      </button>
                    </div>

                    <div className="divide-y divide-zinc-100">
                      {calcResults.breakdown.map((row: any, i: number) => (
                        <div key={i} className="py-3 flex justify-between text-sm">
                          <div>
                            <p className="font-bold text-zinc-800">{row.category}</p>
                            <p className="text-xs text-zinc-400">
                              {row.count} heads × {row.rate} kg/head/day
                            </p>
                          </div>
                          <div className="text-right font-mono font-bold text-zinc-900">
                            <p>{row.dailyTotal.toFixed(1)} kg/day</p>
                            <p className="text-xs text-zinc-500">Period Total: {row.periodTotal.toFixed(1)} kg</p>
                          </div>
                        </div>
                      ))}
                    </div>

                    <div className="pt-4 border-t border-zinc-200 flex justify-between font-extrabold text-sm text-zinc-900">
                      <span>Grand Total for {calcResults.days} Days</span>
                      <div className="text-right font-mono">
                        <p>{calcResults.dailyTotal.toFixed(1)} kg/day</p>
                        <p className="text-xs text-emerald-700">Projected Total: {calcResults.periodTotal.toFixed(1)} kg</p>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </main>
      </div>

      {/* 1. Add Item Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-md p-6 space-y-6 shadow-2xl">
            <h3 className="text-lg font-bold text-zinc-900">Add New Feed Item</h3>
            <form onSubmit={handleAddItem} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Name</label>
                <input
                  type="text"
                  required
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  placeholder="e.g. Broiler Finisher"
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Feed Type</label>
                  <select
                    value={newFeedType}
                    onChange={(e) => setNewFeedType(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    <option>Starter</option>
                    <option>Grower</option>
                    <option>Finisher</option>
                    <option>Sow</option>
                    <option>Boar</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Base Unit</label>
                  <select
                    value={newUnit}
                    onChange={(e) => setNewUnit(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    <option value="bags">Bags</option>
                    <option value="kg">kg</option>
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Init Qty</label>
                  <input
                    type="number"
                    step="any"
                    value={newQty}
                    onChange={(e) => setNewQty(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Bag Wt (kg)</label>
                  <input
                    type="number"
                    step="any"
                    value={newWeight}
                    onChange={(e) => setNewWeight(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Min Alert</label>
                  <input
                    type="number"
                    step="any"
                    value={newThreshold}
                    onChange={(e) => setNewThreshold(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
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

      {/* 2. Restock Modal */}
      {showRestockModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-md p-6 space-y-6 shadow-2xl">
            <h3 className="text-lg font-bold text-zinc-900">Restock Feed Item</h3>
            <form onSubmit={handleRestock} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Qty Added</label>
                  <input
                    type="number"
                    step="any"
                    required
                    value={restockQty}
                    onChange={(e) => setRestockQty(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Unit</label>
                  <select
                    value={restockUnit}
                    onChange={(e) => setRestockUnit(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    <option value="bags">Bags</option>
                    <option value="kg">kg</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Total Cost ($)</label>
                <input
                  type="number"
                  step="any"
                  value={restockCost}
                  onChange={(e) => setRestockCost(parseFloat(e.target.value) || 0)}
                  placeholder="0.00"
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Notes</label>
                <input
                  type="text"
                  value={restockNotes}
                  onChange={(e) => setRestockNotes(e.target.value)}
                  placeholder="e.g. Purchased from store"
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                />
              </div>
              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
                <button type="button" onClick={() => setShowRestockModal(false)} className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:bg-zinc-100 transition">
                  Cancel
                </button>
                <button type="submit" className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition">
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
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-md p-6 space-y-6 shadow-2xl">
            <h3 className="text-lg font-bold text-zinc-900">Log Feed Usage</h3>
            <form onSubmit={handleUseFeed} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Qty Used</label>
                  <input
                    type="number"
                    step="any"
                    required
                    value={usageQty}
                    onChange={(e) => setUsageQty(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Unit</label>
                  <select
                    value={usageUnit}
                    onChange={(e) => setUsageUnit(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    <option value="bags">Bags</option>
                    <option value="kg">kg</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Notes</label>
                <input
                  type="text"
                  value={usageNotes}
                  onChange={(e) => setUsageNotes(e.target.value)}
                  placeholder="e.g. Fed to Grower Pen 1"
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                />
              </div>
              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
                <button type="button" onClick={() => setShowUsageModal(false)} className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:bg-zinc-100 transition">
                  Cancel
                </button>
                <button type="submit" className="rounded-lg bg-violet-600 hover:bg-violet-700 px-4 py-2 text-xs font-bold text-white transition">
                  Log Usage
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 4. Export Feed Data Modal */}
      {showExportModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-md p-6 space-y-6 shadow-2xl">
            <h3 className="text-lg font-bold text-zinc-900">Export Feed Data</h3>
            <div className="space-y-4">
              <label className="block text-xs font-bold text-zinc-500 uppercase tracking-wider">Select Range</label>
              <div className="space-y-2">
                {(["Current Month", "Last 3 Months", "Custom"] as const).map((range) => (
                  <label key={range} className="flex items-center gap-2.5 cursor-pointer text-sm text-zinc-800">
                    <input
                      type="radio"
                      name="exportRange"
                      checked={exportRange === range}
                      onChange={() => setExportRange(range)}
                      className="h-4 w-4 border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                    />
                    <span>{range === "Current Month" ? "Current Month" : range === "Last 3 Months" ? "Last 3 Months" : "Custom"}</span>
                  </label>
                ))}
              </div>

              {exportRange === "Custom" && (
                <div className="grid grid-cols-2 gap-4 animate-fade-in">
                  <div>
                    <label className="block text-xs font-semibold text-zinc-500 mb-1.5">From Date</label>
                    <input
                      type="date"
                      required
                      value={exportFromDate}
                      onChange={(e) => setExportFromDate(e.target.value)}
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-zinc-500 mb-1.5">To Date</label>
                    <input
                      type="date"
                      required
                      value={exportToDate}
                      onChange={(e) => setExportToDate(e.target.value)}
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                    />
                  </div>
                </div>
              )}
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
              <button
                type="button"
                onClick={() => setShowExportModal(false)}
                className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:bg-zinc-100 transition"
              >
                Cancel
              </button>
              <button
                onClick={handleExportFeedInventoryPdf}
                className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition"
              >
                Export
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Print View */}
      {printData && (() => {
        const d = new Date();
        const pad = (n: number) => String(n).padStart(2, '0');
        const generatedOn = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;

        // Compute Feed Inventory Data if type === "inventory"
        let inventoryRows: any[] = [];
        let totalAdditionInKg = 0;
        let totalUsageInKg = 0;
        let totalInStockInKg = 0;

        if (printData.type === "inventory" && printData.details) {
          const itemsList = printData.details.items || [];
          const txsList = printData.details.transactions || [];

          inventoryRows = itemsList.map((item: any) => {
            const itemTransactions = txsList.filter((tx: any) => tx.itemId === item.id);

            const getConvertedQty = (tx: any) => {
              if (tx.unit === item.unit) return tx.quantity;
              if (tx.unit === "bags") {
                return item.unit === "kg" ? tx.quantity * item.unitWeight : tx.quantity;
              }
              if (tx.unit === "kg") {
                return (item.unit === "bags" && item.unitWeight > 0) ? tx.quantity / item.unitWeight : tx.quantity;
              }
              return tx.quantity;
            };

            const addition = itemTransactions
              .filter((tx: any) => tx.type === "Restock")
              .reduce((sum: number, tx: any) => sum + getConvertedQty(tx), 0);

            const usage = itemTransactions
              .filter((tx: any) => tx.type === "Usage")
              .reduce((sum: number, tx: any) => sum + getConvertedQty(tx), 0);

            const inStock = item.quantity;

            const additionInKg = addition * (item.unit === "bags" ? item.unitWeight : 1.0);
            const usageInKg = usage * (item.unit === "bags" ? item.unitWeight : 1.0);
            const inStockInKg = inStock * (item.unit === "bags" ? item.unitWeight : 1.0);

            totalAdditionInKg += additionInKg;
            totalUsageInKg += usageInKg;
            totalInStockInKg += inStockInKg;

            return {
              name: item.name,
              feedType: item.feedType,
              unit: item.unit === "bags" ? "Bags" : "kg",
              addition,
              usage,
              inStock
            };
          });
        }

        return (
          <div className="hidden print:block p-8 space-y-6 text-zinc-900 font-sans w-full relative min-h-screen">
            {/* Watermark logo */}
            <div className="fixed inset-0 flex items-center justify-center opacity-[0.12] pointer-events-none z-0">
              <img
                src="/app_logo.png"
                alt="Watermark Background Logo"
                className="w-[300px] h-[300px] object-contain"
              />
            </div>

            <div className="relative z-10 space-y-6">
              {/* Header */}
              <div className="relative border-b pb-4 mb-6 text-center">
                <div className="absolute right-0 top-0">
                  <img src="/app_logo.png" alt="SmartSwine Logo" className="h-12 w-12 object-contain" />
                </div>
                <h1 className="text-[22px] font-bold text-black leading-tight">SmartSwine</h1>
                <p className="text-[12px] italic text-zinc-500 mt-0.5 leading-tight">
                  The Only Tool a Pig Farmer Needs.<br />Farm Smarter, Not Harder.
                </p>
                <h2 className="text-[18px] font-bold text-black mt-3">{printData.title}</h2>
                <p className="text-[10px] text-zinc-400 mt-1">Generated on: {generatedOn}</p>
              </div>

              {/* Report Body */}
              {printData.type === "inventory" && printData.details && (
                <div className="space-y-6">
                  <table className="min-w-full divide-y divide-zinc-200 text-xs border border-zinc-200">
                    <thead className="bg-zinc-100">
                      <tr className="text-left font-bold text-zinc-700 uppercase tracking-wider">
                        <th className="p-2 border">Feed Name</th>
                        <th className="p-2 border">Feed Type</th>
                        <th className="p-2 border">Unit</th>
                        <th className="p-2 border text-right">Addition</th>
                        <th className="p-2 border text-right">Usage</th>
                        <th className="p-2 border text-right">In Stock</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-200 bg-white">
                      {inventoryRows.map((row: any, index: number) => (
                        <tr key={index} className="text-zinc-800">
                          <td className="p-2 border font-medium">{row.name}</td>
                          <td className="p-2 border">{row.feedType}</td>
                          <td className="p-2 border">{row.unit}</td>
                          <td className="p-2 border text-right">{row.addition.toFixed(1)}</td>
                          <td className="p-2 border text-right">{row.usage.toFixed(1)}</td>
                          <td className="p-2 border text-right">{row.inStock.toFixed(1)}</td>
                        </tr>
                      ))}
                      <tr className="bg-zinc-50 font-bold text-zinc-900 border-t-2 border-zinc-400">
                        <td className="p-2 border">Total (kg)</td>
                        <td className="p-2 border"></td>
                        <td className="p-2 border"></td>
                        <td className="p-2 border text-right">{totalAdditionInKg.toFixed(1)}</td>
                        <td className="p-2 border text-right">{totalUsageInKg.toFixed(1)}</td>
                        <td className="p-2 border text-right">{totalInStockInKg.toFixed(1)}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              )}

              {printData.type === "formulator" && printData.details && (
                <div className="space-y-6">
                  <div>
                    <h3 className="text-sm font-bold text-zinc-700 mb-2 uppercase tracking-wide">Ingredients Composition</h3>
                    <table className="min-w-full divide-y divide-zinc-200 text-xs border border-zinc-200">
                      <thead className="bg-zinc-100">
                        <tr className="text-left font-bold text-zinc-700 uppercase tracking-wider">
                          <th className="p-2 border">Ingredient</th>
                          <th className="p-2 border text-right">Ration Percentage (%)</th>
                          <th className="p-2 border text-right">Per Ton Mix (kg)</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-zinc-200 bg-white">
                        {Object.entries(printData.details.formulation.ingredients).map(([id, percent]: any) => {
                          const ing = printData.details.ingredients.find((i: any) => i.id === id || i.name === id);
                          const name = ing ? ing.name : id;
                          return (
                            <tr key={id} className="text-zinc-800">
                              <td className="p-2 border">{name}</td>
                              <td className="p-2 border text-right">{percent.toFixed(1)}%</td>
                              <td className="p-2 border text-right font-mono">{(percent * 10).toFixed(1)} kg</td>
                            </tr>
                          );
                        })}
                        <tr className="bg-zinc-50 font-bold text-zinc-900 border-t-2 border-zinc-400">
                          <td className="p-2 border">Total mix</td>
                          <td className="p-2 border text-right">{printData.details.formulation.totalPercentage.toFixed(1)}%</td>
                          <td className="p-2 border text-right font-mono">1000.0 kg</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>

                  {printData.details.formulation.totalPercentage < 99.9 && (
                    <div className="text-xs font-bold text-rose-600">
                      Formula incomplete ({printData.details.formulation.totalPercentage.toFixed(1)}%)
                    </div>
                  )}

                  <div>
                    <h3 className="text-sm font-bold text-zinc-700 mb-2 uppercase tracking-wide">Nutritional Analysis</h3>
                    <table className="min-w-full divide-y divide-zinc-200 text-xs border border-zinc-200">
                      <thead className="bg-zinc-100">
                        <tr className="text-left font-bold text-zinc-700 uppercase tracking-wider">
                          <th className="p-2 border">Nutrient</th>
                          <th className="p-2 border text-right">Target</th>
                          <th className="p-2 border text-right">Actual</th>
                          <th className="p-2 border text-right">Status</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-zinc-200 bg-white">
                        {printData.details.formulation.nutritionalComparison.map((nutrient: any) => (
                          <tr key={nutrient.label} className="text-zinc-800">
                            <td className="p-2 border font-medium">{nutrient.label}</td>
                            <td className="p-2 border text-right font-mono text-zinc-500">{nutrient.target.toFixed(2)}</td>
                            <td className={`p-2 border text-right font-mono font-bold ${nutrient.isDeficient ? "text-rose-600" : "text-emerald-600"}`}>
                              {nutrient.actual.toFixed(2)}
                            </td>
                            <td className={`p-2 border text-right font-semibold ${nutrient.isDeficient ? "text-rose-600" : "text-emerald-600"}`}>
                              {nutrient.isDeficient ? "Deficient" : "OK"}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  <div className="text-center font-bold text-rose-600 text-[11px] pt-4 leading-relaxed">
                    Disclaimer: Always consult a qualified animal nutritionist or veterinarian before making significant changes to your herd's diet.
                  </div>
                </div>
              )}

              {printData.type === "calculator" && printData.details && (
                <div className="space-y-6">
                  <div>
                    <table className="min-w-full divide-y divide-zinc-200 text-xs border border-zinc-200">
                      <thead className="bg-zinc-100">
                        <tr className="text-left font-bold text-zinc-700 uppercase tracking-wider">
                          <th className="p-2 border">Growth Stage</th>
                          <th className="p-2 border text-right">Daily (kg)</th>
                          <th className="p-2 border text-right">
                            {printData.details.calcResults.days > 1
                              ? `${printData.details.calcResults.days} Days`
                              : "Daily (kg)"}
                          </th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-zinc-200 bg-white">
                        {printData.details.calcResults.breakdown.map((row: any, i: number) => (
                          <tr key={i} className="text-zinc-800">
                            <td className="p-2 border font-medium">
                              {row.category} ({row.count})
                            </td>
                            <td className="p-2 border text-right">{row.dailyTotal.toFixed(1)}</td>
                            <td className="p-2 border text-right font-mono">{row.periodTotal.toFixed(1)}</td>
                          </tr>
                        ))}
                        <tr className="bg-zinc-50 font-bold text-zinc-900 border-t-2 border-zinc-400">
                          <td className="p-2 border">Total</td>
                          <td className="p-2 border text-right">
                            {printData.details.calcResults.dailyTotal.toFixed(1)}
                          </td>
                          <td className="p-2 border text-right font-mono">
                            {printData.details.calcResults.periodTotal.toFixed(1)}
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>

                  <div className="text-center font-bold text-rose-600 text-[11px] pt-4 leading-relaxed">
                    Disclaimer: Always consult a qualified animal nutritionist or veterinarian before making significant changes to your herd's diet.
                  </div>
                </div>
              )}

              {/* Footer */}
              <div className="border-t pt-4 mt-8 text-center text-[10px] text-zinc-400">
                <p>SmartSwine Management System Report</p>
                <p>Copyright 2026. Developed by Goshen AgriFirm & Bibinii Tech</p>
              </div>
            </div>
          </div>
        );
      })()}
    </div>
  );
}
