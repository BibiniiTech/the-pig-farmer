"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot, doc, setDoc, deleteDoc, updateDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import UserProfileDropdown from "@/components/UserProfileDropdown";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import NativeAdBanner from "@/components/ads/NativeAdBanner";
import RewardedPassModal from "@/components/ads/RewardedPassModal";
import { useTranslations } from "next-intl";
import {
  InventoryIcon,
  ScienceIcon,
  CalculateIcon,
  AnalyticsIcon,
  ExportPdfIcon,
} from "@/components/icons/DashboardIcons";
import {
  formulateFeed,
  FormulationResult,
  analyzeFeedMix,
  FeedNutrientProfile,
} from "@/lib/feedCalculator";
import { FeedIngredient, NutritionalRequirement, FeedInventoryItem, FeedInventoryTransaction } from "@/lib/types";
import PremiumWrapper from "@/components/PremiumWrapper";

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

const ArrowLeftIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <line x1="19" y1="12" x2="5" y2="12" />
    <polyline points="12 19 5 12 12 5" />
  </svg>
);

const PlusIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.5"
    strokeLinecap="round"
    strokeLinejoin="round"
    {...props}
  >
    <line x1="12" y1="5" x2="12" y2="19" />
    <line x1="5" y1="12" x2="19" y2="12" />
  </svg>
);

const ChevronDownIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <polyline points="6 9 12 15 18 9" />
  </svg>
);

const defaultRequirements: NutritionalRequirement[] = [
  { stage: "Starter", digestibleProtein: 17.0, metabolizableEnergy: 3350.0, calcium: 0.90, phosphorus: 0.75, lysine: 7.90, methionineCystine: 5.20, tryptophan: 1.25, crudeFiber: 3.0, minDailyFeed: 0.35, maxDailyFeed: 0.85 },
  { stage: "Grower", digestibleProtein: 14.5, metabolizableEnergy: 3300.0, calcium: 0.75, phosphorus: 0.50, lysine: 6.10, methionineCystine: 4.00, tryptophan: 1.10, crudeFiber: 5.0, minDailyFeed: 0.75, maxDailyFeed: 1.50 },
  { stage: "Finisher", digestibleProtein: 13.0, metabolizableEnergy: 3300.0, calcium: 0.75, phosphorus: 0.50, lysine: 5.70, methionineCystine: 3.00, tryptophan: 1.00, crudeFiber: 6.0, minDailyFeed: 1.50, maxDailyFeed: 2.50 }
];

export default function FeedPage() {
  const t = useTranslations("Feed");
  
  const translateIngredientName = (name: string) => {
    if (!name) return "";
    const key = name.toLowerCase().replace(/[\s\(\),&\-\/]/g, "_").replace(/_+/g, "_").replace(/_$/, "");
    try {
      const translation = t(`ingredientsList.${key}`);
      if (translation && !translation.startsWith("ingredientsList.")) {
        return translation;
      }
    } catch {}
    return name;
  };

  const translateNutrientLabel = (label: string) => {
    const keys: Record<string, string> = {
      "Digestible Protein (%)": "nutrients.digestibleProtein",
      "Crude Fiber (%)": "nutrients.crudeFiber",
      "Metabolizable Energy (kcal/kg)": "nutrients.metabolizableEnergy",
      "Calcium (%)": "nutrients.calcium",
      "Phosphorus (%)": "nutrients.phosphorus",
      "Lysine (%)": "nutrients.lysine",
      "Methionine + Cystine (%)": "nutrients.methionineCystine",
    };
    const key = keys[label];
    return key ? t(key) : label;
  };

  const translateCategoryGroup = (cat: string) => {
    const keys: Record<string, string> = {
      "Energy": "groups.energy",
      "Protein": "groups.protein",
      "Vitamins, Minerals & Salt": "groups.vitaminsMineralsSalt",
    };
    const key = keys[cat];
    return key ? t(key) : cat;
  };

  const { user, activeFarmUid, loading, userProfile } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  // ==================== INVENTORY STATES ====================
  const [items, setItems] = useState<FeedInventoryItem[]>([]);
  const [transactions, setTransactions] = useState<FeedInventoryTransaction[]>([]);
  const [dataLoading, setDataLoading] = useState(true);
  const [isTxHistoryOpen, setIsTxHistoryOpen] = useState(false);

  // Form modals
  const [showAddModal, setShowAddModal] = useState(false);
  const [showRestockModal, setShowRestockModal] = useState(false);
  const [showUsageModal, setShowUsageModal] = useState(false);
  const [selectedItemId, setSelectedItemId] = useState<string | null>(null);
  const [showRewardedPassModal, setShowRewardedPassModal] = useState(false);

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

  // Single accordion open at a time for available ingredients
  const [expandedCategory, setExpandedCategory] = useState<string | null>(null);

  const toggleCategory = (cat: string) => {
    setExpandedCategory(prev => prev === cat ? null : cat);
  };

  // ==================== ANALYZE FEED STATES ====================
  const [analyzePercentageMode, setAnalyzePercentageMode] = useState(false);
  const [analyzeItems, setAnalyzeItems] = useState<{ ingredient: FeedIngredient; quantityStr: string }[]>([]);
  const [analyzeTargetStage, setAnalyzeTargetStage] = useState("Grower");
  const [showAddIngredientModal, setShowAddIngredientModal] = useState(false);
  const [analyzeSearchQuery, setAnalyzeSearchQuery] = useState("");

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
      rangeLabel = t("currentMonth");
    } else if (exportRange === "Last 3 Months") {
      const pastDate = new Date();
      pastDate.setMonth(today.getMonth() - 3);
      startDateStr = pastDate.toISOString().split("T")[0];
      rangeLabel = t("last3Months");
    } else {
      startDateStr = exportFromDate || "1970-01-01";
      endDateStr = exportToDate || todayStr;
      rangeLabel = `${t("custom")} (${startDateStr} to ${endDateStr})`;
    }

    // Filter transactions
    const filteredTx = transactions.filter(tx => {
      const txDate = tx.date.split("T")[0];
      return txDate >= startDateStr && txDate <= endDateStr;
    });

    setPrintData({
      type: "inventory",
      title: t("reportTitle", { range: rangeLabel }),
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
      title: t("feedFormulaMix", { stage: selectedStage }),
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
      title: t("projectedResults"),
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
      
      // Auto-prune and filter out duplicate/redundant ingredients (parity with Android)
      const REDUNDANT_NAMES = ["barleyb", "dried brewers grain", "full fat soybean"];
      rawList.forEach(ing => {
        if (REDUNDANT_NAMES.includes(ing.name.trim().toLowerCase())) {
          deleteDoc(doc(db, "users", activeFarmUid, "feed_ingredients", ing.id)).catch(console.error);
        }
      });
      const validRaw = rawList.filter(ing => !REDUNDANT_NAMES.includes(ing.name.trim().toLowerCase()));

      // Deduplicate by name, preferring "Vitamins, Minerals & Salt" or "Energy" over "Protein" if name is duplicated
      const uniqueMap = new Map<string, FeedIngredient>();
      validRaw.forEach(ing => {
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
      Sows: { rate: 2.2, label: t("sowsCount") },
      Boars: { rate: 2.2, label: t("boarsCount") },
      Gilts: { rate: 2.2, label: t("giltsCount") },
      Pregnant: { rate: 2.2, label: t("pregnantSows") },
      Lactating: { rate: 5.5, label: t("lactatingSows") },
      Starter: { rate: 0.7, label: t("starterPiglets") },
      Grower: { rate: 1.8, label: t("growers") },
      Finisher: { rate: 2.5, label: t("finishers") }
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

  // ==================== ANALYZE FEED CALCULATIONS ====================
  const analyzeParsedItems = React.useMemo(() => {
    return analyzeItems.map(item => {
      const q = parseFloat(item.quantityStr) || 0;
      return { ingredient: item.ingredient, quantity: q };
    }).filter(i => i.quantity > 0);
  }, [analyzeItems]);

  const analyzeProfile = React.useMemo(() => {
    return analyzeFeedMix(analyzeParsedItems, analyzePercentageMode);
  }, [analyzeParsedItems, analyzePercentageMode]);

  const analyzeTotalEntered = React.useMemo(() => {
    return analyzeItems.reduce((acc, item) => acc + (parseFloat(item.quantityStr) || 0), 0);
  }, [analyzeItems]);

  const benchmarkReq = requirements.find(r => r.stage.toLowerCase() === analyzeTargetStage.toLowerCase()) || defaultRequirements[1];

  // Helper render for collapsible category lists in Formulator (single open category at a time)
  const renderGroupList = (category: string) => {
    const list = ingredients.filter(ing => ing.mainCategory === category);
    const isExpanded = expandedCategory === category;

    return (
      <div className="bg-zinc-50/75 backdrop-blur-md border border-zinc-200 rounded-xl overflow-hidden shadow-sm">
        <button
          type="button"
          onClick={() => toggleCategory(category)}
          className="w-full bg-zinc-100/60 px-4 py-3 flex items-center justify-between border-b border-zinc-200/50 hover:bg-zinc-200/60 transition-colors"
        >
          <div className="flex items-center gap-2 font-bold text-zinc-800 text-xs">
            <span className="text-amber-600">{isExpanded ? "▼" : "▶"}</span>
            <span>{translateCategoryGroup(category)} ({list.length})</span>
          </div>
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
                    <span>{translateIngredientName(ing.name)}</span>
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
        {!isMobile && <DesktopHeader />}

        <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8 space-y-12 print:p-0">

          {/* Header Bar */}
          <div className="flex items-center justify-between flex-wrap gap-4 border-b border-zinc-200 pb-4">
            <div className="flex items-center gap-3">
              <Link
                href="/dashboard?section=feed"
                className="p-2 rounded-xl border border-zinc-200 text-zinc-600 hover:bg-zinc-100 transition"
              >
                <ArrowLeftIcon className="h-5 w-5" />
              </Link>
              <div className="h-10 w-10 rounded-xl bg-amber-50 border border-amber-200/60 flex items-center justify-center flex-shrink-0">
                <InventoryIcon className="h-5 w-5 text-amber-600" />
              </div>
              <div>
                <h1 className="text-xl sm:text-2xl font-black text-amber-600">
                  {t("inventory") || "Feed Inventory"}
                </h1>
                <p className="text-xs text-zinc-500">{t("inventoryDesc") || "Manage feed stock, restock bags, and log usage"}</p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <Link
                href="/dashboard/feed/calculator"
                className="px-3.5 py-1.5 rounded-xl text-xs font-bold text-zinc-700 bg-zinc-100 hover:bg-zinc-200 transition"
              >
                Calculator
              </Link>
              <Link
                href="/dashboard/feed/mix"
                className="px-3.5 py-1.5 rounded-xl text-xs font-bold text-zinc-700 bg-zinc-100 hover:bg-zinc-200 transition"
              >
                Mix Feed
              </Link>
              <Link
                href="/dashboard/feed/analyze"
                className="px-3.5 py-1.5 rounded-xl text-xs font-bold text-zinc-700 bg-zinc-100 hover:bg-zinc-200 transition"
              >
                Analyze Feed
              </Link>
            </div>
          </div>

          {/* ==================== 4. FEED INVENTORY ==================== */}
          <div className="space-y-6">
            {items.some(item => item.quantity < item.minThreshold) && (
              <div className="rounded-xl border border-amber-200 bg-amber-50/60 p-4 text-xs text-amber-800 flex items-center gap-3 print:hidden shadow-sm">
                <WarningIcon className="h-5 w-5 text-amber-600 shrink-0" />
                <div>
                  <p className="font-bold">{t("lowStockWarning")}</p>
                  <p className="text-zinc-650 mt-0.5">{t("lowStockDesc")}</p>
                </div>
              </div>
            )}

            <div id="inventory" className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm overflow-hidden print:border-none print:p-0 scroll-mt-24">
              <div className="flex items-center justify-between mb-5 flex-wrap gap-4">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-xl bg-amber-50 border border-amber-200/60 flex items-center justify-center flex-shrink-0">
                    <InventoryIcon className="h-5 w-5 text-amber-600" />
                  </div>
                  <div>
                    <h2 className="text-sm font-black text-zinc-900 print:text-black">{t("inventory")}</h2>
                    <p className="text-xs text-zinc-500 mt-0.5">{t("inventoryDesc")}</p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <PremiumWrapper fallback={
                    <button
                      onClick={() => setShowRewardedPassModal(true)}
                      className="rounded-lg border border-amber-200 bg-amber-50/50 px-4 py-2 text-xs font-semibold text-amber-650 hover:bg-amber-100 transition shadow-sm flex items-center gap-1.5"
                    >
                      <ExportPdfIcon className="h-3.5 w-3.5 text-amber-500" />
                      <span>{t("exportPdfPremium")}</span>
                    </button>
                  }>
                    <button
                      onClick={() => setShowExportModal(true)}
                      className="rounded-lg border border-zinc-200 bg-zinc-50/50 px-4 py-2 text-xs font-semibold text-zinc-650 hover:bg-zinc-100 transition shadow-sm flex items-center gap-1.5"
                    >
                      <ExportPdfIcon className="h-3.5 w-3.5 text-zinc-500" />
                      <span>{t("exportPdf")}</span>
                    </button>
                  </PremiumWrapper>
                  <button
                    onClick={() => setShowAddModal(true)}
                    className="rounded-lg bg-amber-600 hover:bg-amber-700 px-4 py-2 text-xs font-bold text-white shadow shadow-amber-600/10 transition active:scale-95"
                  >
                    {t("addFeedItem")}
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
                <div className="h-64 flex flex-col items-center justify-center border border-dashed border-zinc-200 rounded-2xl text-center p-6 bg-zinc-50/40 backdrop-blur-sm shadow-inner">
                  <InventoryIcon className="h-10 w-10 text-zinc-400 mb-3" />
                  <p className="font-semibold text-zinc-500 text-sm">{t("noFeedInventoryItems")}</p>
                  <p className="text-xs text-zinc-400 mt-1">{t("addFirstFeedItem")}</p>
                  <button
                    onClick={() => setShowAddModal(true)}
                    className="mt-4 rounded-lg bg-amber-600 hover:bg-amber-700 px-4 py-2 text-xs font-bold text-white shadow shadow-amber-600/10 transition active:scale-95"
                  >
                    {t("addFirstItem")}
                  </button>
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
                  {items.map((item) => {
                    const isLow = item.quantity < item.minThreshold;
                    return (
                      <div
                        key={item.id}
                        className={`bg-white border rounded-2xl p-5 shadow-xs flex flex-col justify-between transition-all hover:shadow-md ${
                          isLow ? "border-amber-300 ring-1 ring-amber-300/40" : "border-zinc-200/80"
                        }`}
                      >
                        <div>
                          <div className="flex justify-between items-start mb-2">
                            <div className="min-w-0 pr-2">
                              <h3 className="font-black text-zinc-900 text-base truncate">{item.name}</h3>
                              <span className="inline-block mt-0.5 px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 text-amber-800 border border-amber-200/50">
                                {item.feedType}
                              </span>
                            </div>
                            <div className="flex items-center gap-1">
                              <button
                                onClick={() => handleDeleteItem(item.id)}
                                className="p-1 text-zinc-400 hover:text-rose-600 transition"
                                title="Delete item"
                              >
                                <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
                                  <path strokeLinecap="round" strokeLinejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                </svg>
                              </button>
                            </div>
                          </div>

                          <div className="mt-4 flex items-baseline gap-2">
                            <span className={`text-3xl font-black ${isLow ? "text-amber-600" : "text-zinc-900"}`}>
                              {item.quantity}
                            </span>
                            <span className="text-xs font-bold text-zinc-500 uppercase">{t(item.unit as any)}</span>
                            <span className="text-xs text-zinc-400">({(item.quantity * item.unitWeight).toLocaleString()} kg)</span>
                          </div>

                          {isLow && (
                            <p className="mt-1 text-[11px] font-bold text-amber-600">
                              Low stock! Below {item.minThreshold} {t(item.unit as any)} threshold
                            </p>
                          )}
                        </div>

                        <div className="mt-5 pt-4 border-t border-zinc-100 flex items-center gap-2">
                          <button
                            onClick={() => {
                              setSelectedItemId(item.id);
                              setShowRestockModal(true);
                            }}
                            className="flex-1 rounded-xl border border-zinc-200 bg-zinc-50/60 hover:bg-zinc-100 py-2 text-xs font-bold text-zinc-700 transition active:scale-95 text-center"
                          >
                            {t("restock")}
                          </button>
                          <button
                            onClick={() => {
                              setSelectedItemId(item.id);
                              setShowUsageModal(true);
                            }}
                            className="flex-1 rounded-xl bg-amber-600 hover:bg-amber-700 py-2 text-xs font-bold text-white shadow shadow-amber-600/10 transition active:scale-95 text-center"
                          >
                            Log Usage
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              {transactions.length > 0 && (
                <div className="mt-8 pt-6 border-t border-zinc-200">
                  <button
                    type="button"
                    onClick={() => setIsTxHistoryOpen((prev) => !prev)}
                    className="w-full flex items-center justify-between py-2 text-left group hover:opacity-80 transition cursor-pointer"
                  >
                    <div className="flex items-center gap-2.5">
                      <h3 className="text-xs font-bold text-zinc-600 uppercase tracking-wider group-hover:text-zinc-900 transition-colors">
                        {t("recentMovements") || "Recent Inventory Movements"}
                      </h3>
                      <span className="text-[11px] font-bold bg-zinc-100 text-zinc-700 px-2 py-0.5 rounded-full border border-zinc-200">
                        {transactions.length}
                      </span>
                    </div>
                    <ChevronDownIcon
                      className={`h-4 w-4 text-zinc-400 transition-transform duration-200 ${
                        isTxHistoryOpen ? "rotate-180 text-zinc-700" : ""
                      }`}
                    />
                  </button>

                  {isTxHistoryOpen && (
                    <div className="mt-3 divide-y divide-zinc-100 max-h-80 overflow-y-auto pr-1 animate-in fade-in duration-200">
                      {transactions.slice(0, 10).map((tx) => {
                        const isRestock = tx.type === "Restock";
                        return (
                          <div key={tx.id} className="py-2.5 flex items-center justify-between gap-3 text-xs">
                            <div className="flex items-center gap-2.5 min-w-0">
                              <span className={`h-2 w-2 rounded-full ${isRestock ? "bg-emerald-500" : "bg-amber-500"}`} />
                              <div className="truncate">
                                <span className="font-bold text-zinc-900">{tx.itemName}</span>
                                <span className="text-zinc-400 ml-1.5 text-[11px]">
                                  {new Date(tx.date).toLocaleDateString()}
                                </span>
                              </div>
                            </div>
                            <div className="text-right font-mono font-bold flex-shrink-0">
                              <span className={isRestock ? "text-emerald-600" : "text-amber-600"}>
                                {isRestock ? "+" : "-"}{tx.quantity} {t(tx.unit as any)}
                              </span>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* Sponsored Ad Banner for Free Users */}
          <NativeAdBanner />
        </main>
      </div>

      {/* 1. Add Item Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-md p-6 space-y-6 shadow-2xl">
            <h3 className="text-lg font-bold text-zinc-900">{t("addNewItem")}</h3>
            <form onSubmit={handleAddItem} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("name")}</label>
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
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("feedType")}</label>
                  <select
                    value={newFeedType}
                    onChange={(e) => setNewFeedType(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    <option value="Starter">{t("starter")}</option>
                    <option value="Grower">{t("grower")}</option>
                    <option value="Finisher">{t("finisher")}</option>
                    <option value="Sow">{t("sow")}</option>
                    <option value="Boar">{t("boar")}</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("baseUnit")}</label>
                  <select
                    value={newUnit}
                    onChange={(e) => setNewUnit(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    <option value="bags">{t("bags")}</option>
                    <option value="kg">{t("kg")}</option>
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("initQty")}</label>
                  <input
                    type="number"
                    step="any"
                    value={newQty}
                    onChange={(e) => setNewQty(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("bagWt")}</label>
                  <input
                    type="number"
                    step="any"
                    value={newWeight}
                    onChange={(e) => setNewWeight(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("minAlert")}</label>
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
                  {t("cancel")}
                </button>
                <button type="submit" className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition">
                  {t("save")}
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
            <h3 className="text-lg font-bold text-zinc-900">{t("restockItem")}</h3>
            <form onSubmit={handleRestock} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("qtyAdded")}</label>
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
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("unit")}</label>
                  <select
                    value={restockUnit}
                    onChange={(e) => setRestockUnit(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    <option value="bags">{t("bags")}</option>
                    <option value="kg">{t("kg")}</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("totalCost", { symbol: "$" })}</label>
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
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("notes")}</label>
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
                  {t("cancel")}
                </button>
                <button type="submit" className="rounded-lg bg-amber-600 hover:bg-amber-700 px-4 py-2 text-xs font-bold text-white transition">
                  {t("addStock")}
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
            <h3 className="text-lg font-bold text-zinc-900">{t("logFeedUsage")}</h3>
            <form onSubmit={handleUseFeed} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("qtyUsed")}</label>
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
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("unit")}</label>
                  <select
                    value={usageUnit}
                    onChange={(e) => setUsageUnit(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    <option value="bags">{t("bags")}</option>
                    <option value="kg">{t("kg")}</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("notes")}</label>
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
                  {t("cancel")}
                </button>
                <button type="submit" className="rounded-lg bg-amber-600 hover:bg-amber-700 px-4 py-2 text-xs font-bold text-white transition">
                  {t("logUsage")}
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
            <h3 className="text-lg font-bold text-zinc-900">{t("exportData")}</h3>
            <div className="space-y-4">
              <label className="block text-xs font-bold text-zinc-500 uppercase tracking-wider">{t("selectRange")}</label>
              <div className="space-y-2">
                {(["Current Month", "Last 3 Months", "Custom"] as const).map((range) => (
                  <label key={range} className="flex items-center gap-2.5 cursor-pointer text-sm text-zinc-800">
                    <input
                      type="radio"
                      name="exportRange"
                      checked={exportRange === range}
                      onChange={() => setExportRange(range)}
                      className="h-4 w-4 border-zinc-300 text-amber-600 focus:ring-amber-500"
                    />
                    <span>{range === "Current Month" ? t("currentMonth") : range === "Last 3 Months" ? t("last3Months") : t("custom")}</span>
                  </label>
                ))}
              </div>

              {exportRange === "Custom" && (
                <div className="grid grid-cols-2 gap-4 animate-fade-in">
                  <div>
                    <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("fromDate")}</label>
                    <input
                      type="date"
                      required
                      value={exportFromDate}
                      onChange={(e) => setExportFromDate(e.target.value)}
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("toDate")}</label>
                    <input
                      type="date"
                      required
                      value={exportToDate}
                      onChange={(e) => setExportToDate(e.target.value)}
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm"
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
                {t("cancel")}
              </button>
              <button
                onClick={handleExportFeedInventoryPdf}
                className="rounded-lg bg-amber-600 hover:bg-amber-700 px-4 py-2 text-xs font-bold text-white transition"
              >
                {t("export")}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 5. Add Ingredient Modal for Analyze Feed */}
      {showAddIngredientModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fadeIn">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-zinc-200 space-y-4 max-h-[85vh] flex flex-col">
            <div className="flex justify-between items-center pb-2 border-b border-zinc-100">
              <h3 className="text-base font-bold text-zinc-900">Select Ingredient to Add</h3>
              <button
                type="button"
                onClick={() => setShowAddIngredientModal(false)}
                className="text-zinc-400 hover:text-zinc-700 p-1 text-sm font-bold"
              >
                ✕
              </button>
            </div>
            <input
              type="text"
              value={analyzeSearchQuery}
              onChange={(e) => setAnalyzeSearchQuery(e.target.value)}
              placeholder="Search available ingredients..."
              className="w-full rounded-xl border border-zinc-200 px-3 py-2 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500"
            />
            <div className="flex-1 overflow-y-auto space-y-1.5 pr-1 divide-y divide-zinc-100">
              {ingredients
                .filter(ing => !analyzeItems.some(item => item.ingredient.id === ing.id))
                .filter(ing => ing.name.toLowerCase().includes(analyzeSearchQuery.toLowerCase()))
                .map(ing => (
                  <button
                    key={ing.id}
                    type="button"
                    onClick={() => {
                      setAnalyzeItems(prev => [...prev, { ingredient: ing, quantityStr: "" }]);
                      setShowAddIngredientModal(false);
                      setAnalyzeSearchQuery("");
                    }}
                    className="w-full pt-2.5 first:pt-0 flex items-center justify-between text-left hover:bg-amber-50/70 p-2 rounded-xl transition"
                  >
                    <div>
                      <p className="text-xs font-bold text-zinc-900">{translateIngredientName(ing.name)}</p>
                      <p className="text-[10px] text-zinc-400">{ing.mainCategory}</p>
                    </div>
                    <span className="text-[10px] font-mono text-amber-600 font-bold">CP: {ing.crudeProtein}%</span>
                  </button>
                ))}
              {ingredients
                .filter(ing => !analyzeItems.some(item => item.ingredient.id === ing.id))
                .filter(ing => ing.name.toLowerCase().includes(analyzeSearchQuery.toLowerCase())).length === 0 && (
                <p className="text-xs text-zinc-400 italic text-center py-6">No matching ingredients found.</p>
              )}
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
              unit: item.unit === "bags" ? t("bags") : t("kg"),
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
                <p className="text-[10px] text-zinc-400 mt-1">{t("generatedOn", { date: generatedOn })}</p>
              </div>

              {/* Report Body */}
              {printData.type === "inventory" && printData.details && (
                <div className="space-y-6">
                  <table className="min-w-full divide-y divide-zinc-200 text-xs border border-zinc-200">
                    <thead className="bg-zinc-100">
                      <tr className="text-left font-bold text-zinc-700 uppercase tracking-wider">
                        <th className="p-2 border">{t("feedName")}</th>
                        <th className="p-2 border">{t("feedType")}</th>
                        <th className="p-2 border">{t("unit")}</th>
                        <th className="p-2 border text-right">{t("addition")}</th>
                        <th className="p-2 border text-right">{t("usage")}</th>
                        <th className="p-2 border text-right">{t("inStock")}</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-200 bg-white">
                      {inventoryRows.map((row: any, index: number) => (
                        <tr key={index} className="text-zinc-800">
                          <td className="p-2 border font-medium">{row.name}</td>
                          <td className="p-2 border">{t(row.feedType as any)}</td>
                          <td className="p-2 border">{row.unit}</td>
                          <td className="p-2 border text-right">{row.addition.toFixed(1)}</td>
                          <td className="p-2 border text-right">{row.usage.toFixed(1)}</td>
                          <td className="p-2 border text-right">{row.inStock.toFixed(1)}</td>
                        </tr>
                      ))}
                      <tr className="bg-zinc-50 font-bold text-zinc-900 border-t-2 border-zinc-400">
                        <td className="p-2 border">{t("totalKg")}</td>
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
                    <h3 className="text-sm font-bold text-zinc-700 mb-2 uppercase tracking-wide">{t("ingredientsComposition")}</h3>
                    <table className="min-w-full divide-y divide-zinc-200 text-xs border border-zinc-200">
                      <thead className="bg-zinc-100">
                        <tr className="text-left font-bold text-zinc-700 uppercase tracking-wider">
                          <th className="p-2 border">{t("ingredient")}</th>
                          <th className="p-2 border text-right">{t("rationPercentage")}</th>
                          <th className="p-2 border text-right">{t("perTonMixKg")}</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-zinc-200 bg-white">
                        {Object.entries(printData.details.formulation.ingredients).map(([id, percent]: any) => {
                          const ing = printData.details.ingredients.find((i: any) => i.id === id || i.name === id);
                          const name = ing ? translateIngredientName(ing.name) : translateIngredientName(id);
                          return (
                            <tr key={id} className="text-zinc-800">
                              <td className="p-2 border">{name}</td>
                              <td className="p-2 border text-right">{percent.toFixed(1)}%</td>
                              <td className="p-2 border text-right font-mono">{(percent * 10).toFixed(1)} kg</td>
                            </tr>
                          );
                        })}
                        <tr className="bg-zinc-50 font-bold text-zinc-900 border-t-2 border-zinc-400">
                          <td className="p-2 border">{t("totalMix")}</td>
                          <td className="p-2 border text-right">{printData.details.formulation.totalPercentage.toFixed(1)}%</td>
                          <td className="p-2 border text-right font-mono">1000.0 kg</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>

                  {printData.details.formulation.totalPercentage < 99.9 && (
                    <div className="text-xs font-bold text-rose-600">
                      {t("formulaIncomplete", { percent: printData.details.formulation.totalPercentage.toFixed(1) })}
                    </div>
                  )}

                  <div>
                    <h3 className="text-sm font-bold text-zinc-700 mb-2 uppercase tracking-wide">{t("nutritionalAnalysis")}</h3>
                    <table className="min-w-full divide-y divide-zinc-200 text-xs border border-zinc-200">
                      <thead className="bg-zinc-100">
                        <tr className="text-left font-bold text-zinc-700 uppercase tracking-wider">
                          <th className="p-2 border">{t("nutrient")}</th>
                          <th className="p-2 border text-right">{t("target")}</th>
                          <th className="p-2 border text-right">{t("actual")}</th>
                          <th className="p-2 border text-right">{t("status")}</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-zinc-200 bg-white">
                        {printData.details.formulation.nutritionalComparison.map((nutrient: any) => (
                          <tr key={nutrient.label} className="text-zinc-800">
                            <td className="p-2 border font-medium">{translateNutrientLabel(nutrient.label)}</td>
                            <td className="p-2 border text-right font-mono text-zinc-500">{nutrient.target.toFixed(2)}</td>
                            <td className={`p-2 border text-right font-mono font-bold ${nutrient.isDeficient ? "text-rose-600" : "text-emerald-600"}`}>
                              {nutrient.actual.toFixed(2)}
                            </td>
                            <td className={`p-2 border text-right font-semibold ${nutrient.isDeficient ? "text-rose-600" : "text-emerald-600"}`}>
                              {nutrient.isDeficient ? t("deficient") : t("ok")}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  <div className="text-center font-bold text-rose-600 text-[11px] pt-4 leading-relaxed">
                    {t("disclaimer")}
                  </div>
                </div>
              )}

              {printData.type === "calculator" && printData.details && (
                <div className="space-y-6">
                  <div>
                    <table className="min-w-full divide-y divide-zinc-200 text-xs border border-zinc-200">
                      <thead className="bg-zinc-100">
                        <tr className="text-left font-bold text-zinc-700 uppercase tracking-wider">
                          <th className="p-2 border">{t("growthStage")}</th>
                          <th className="p-2 border text-right">{t("dailyKg")}</th>
                          <th className="p-2 border text-right">
                            {printData.details.calcResults.days > 1
                              ? t("days", { count: printData.details.calcResults.days })
                              : t("dailyKg")}
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
                    {t("disclaimer")}
                  </div>
                </div>
              )}

              {/* Footer */}
              <div className="border-t pt-4 mt-8 text-center text-[10px] text-zinc-400">
                <p>{t("managementSystemReport")}</p>
                <p>Copyright 2026. Developed by Goshen AgriFirm & Bibinii Tech</p>
              </div>
            </div>
          </div>
        );
      })()}

      <RewardedPassModal
        isOpen={showRewardedPassModal}
        onClose={() => setShowRewardedPassModal(false)}
        featureName="Feed Inventory & Export"
      />
    </div>
  );
}
