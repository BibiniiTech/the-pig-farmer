"use client";

import React, { useState, useEffect } from "react";
import { doc, updateDoc, setDoc, collection } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { Pig } from "@/lib/types";

const ChevronDownIcon = ({ className = "h-5 w-5", ...props }: React.SVGProps<SVGSVGElement>) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.5"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={className}
    {...props}
  >
    <polyline points="6 9 12 15 18 9" />
  </svg>
);

const ScaleIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <path d="M12 3v18" />
    <path d="M6 8l6-5 6 5" />
    <path d="M3 13l3-5 3 5a3 3 0 0 1-6 0z" />
    <path d="M15 13l3-5 3 5a3 3 0 0 1-6 0z" />
  </svg>
);

const StraightenIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <rect x="2" y="7" width="20" height="10" rx="2" />
    <line x1="6" y1="7" x2="6" y2="11" />
    <line x1="10" y1="7" x2="10" y2="13" />
    <line x1="14" y1="7" x2="14" y2="11" />
    <line x1="18" y1="7" x2="18" y2="13" />
  </svg>
);

const CalculateIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <rect x="4" y="2" width="16" height="20" rx="3" />
    <line x1="8" y1="6" x2="16" y2="6" />
    <line x1="16" y1="14" x2="16" y2="18" />
    <circle cx="8.5" cy="10.5" r="1" fill="currentColor" />
    <circle cx="12" cy="10.5" r="1" fill="currentColor" />
    <circle cx="15.5" cy="10.5" r="1" fill="currentColor" />
    <circle cx="8.5" cy="14" r="1" fill="currentColor" />
    <circle cx="12" cy="14" r="1" fill="currentColor" />
    <circle cx="8.5" cy="17.5" r="1" fill="currentColor" />
    <circle cx="12" cy="17.5" r="1" fill="currentColor" />
  </svg>
);

const TrendingUpIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" />
    <polyline points="17 6 23 6 23 12" />
  </svg>
);

interface InlineWeightSectionProps {
  pigs: Pig[];
  initialSubOption?: string | null;
  initialPigTag?: string | null;
}

export default function InlineWeightSection({
  pigs,
  initialSubOption,
  initialPigTag
}: InlineWeightSectionProps) {
  const { activeFarmUid } = useAuth();

  // Single-open accordion: "convert" | "tape" | "scale" | "carcass" | null
  const [openSubOption, setOpenSubOption] = useState<string | null>(initialSubOption || null);

  const scrollToSubOption = (key: string, delayMs = 150) => {
    if (typeof window === "undefined") return;
    setTimeout(() => {
      const el = document.getElementById(`suboption-${key}`);
      if (el) {
        el.scrollIntoView({ behavior: "smooth", block: "start" });
      }
    }, delayMs);
    setTimeout(() => {
      const el = document.getElementById(`suboption-${key}`);
      if (el) {
        el.scrollIntoView({ behavior: "smooth", block: "start" });
      }
    }, delayMs + 220);
  };

  useEffect(() => {
    if (initialSubOption) {
      setOpenSubOption(initialSubOption);
      scrollToSubOption(initialSubOption, 250);
    }
  }, [initialSubOption]);

  // Sub-option 1: Converter states
  const [convLbs, setConvLbs] = useState("");
  const [convKg, setConvKg] = useState("");

  // Sub-option 2: Tape states
  const [unit, setUnit] = useState<"in" | "cm">("in");
  const [girth, setGirth] = useState("");
  const [length, setLength] = useState("");
  const [estLiveLbs, setEstLiveLbs] = useState<number | null>(null);
  const [estLiveKg, setEstLiveKg] = useState<number | null>(null);
  const [estCarcassKg, setEstCarcassKg] = useState<number | null>(null);

  // Pig save state
  const [selectedPigId, setSelectedPigId] = useState("");
  const [saveWeightInput, setSaveWeightInput] = useState("");
  const [saveLoading, setSaveLoading] = useState(false);
  const [saveMessage, setSaveMessage] = useState("");

  // Sub-option 3: Scale states
  const [scaleUnit, setScaleUnit] = useState<"kg" | "lbs">("kg");
  const [scaleWeight, setScaleWeight] = useState("");
  const [scalePigId, setScalePigId] = useState("");
  const [scalePigSearch, setScalePigSearch] = useState("");
  const [scaleSaveLoading, setScaleSaveLoading] = useState(false);
  const [scaleSaveMessage, setScaleSaveMessage] = useState("");

  useEffect(() => {
    if (initialPigTag && pigs.length > 0) {
      const clean = initialPigTag.trim().toLowerCase();
      const found = pigs.find(
        (p) => p.tagNumber?.trim().toLowerCase() === clean || p.id.toLowerCase() === clean
      );
      if (found) {
        setSelectedPigId(found.id);
        setScalePigId(found.id);
        setScalePigSearch(found.tagNumber || found.id);
        if (found.weight) {
          setSaveWeightInput(found.weight.toString());
        }
      }
    }
  }, [initialPigTag, pigs]);

  // Sub-option 4: Carcass Weight states
  const [liveWeightKg, setLiveWeightKg] = useState("100");
  const [dressingType, setDressingType] = useState<"head_on" | "head_off">("head_on");
  const [showPrimalCuts, setShowPrimalCuts] = useState(false);

  const toggleSubOption = (key: string) => {
    setOpenSubOption((prev) => {
      const next = prev === key ? null : key;
      if (next) {
        scrollToSubOption(next, 150);
      }
      return next;
    });
  };

  // Convert handlers
  const handleLbsChange = (val: string) => {
    setConvLbs(val);
    if (val === "") {
      setConvKg("");
    } else {
      const num = parseFloat(val);
      if (!isNaN(num)) {
        setConvKg((num / 2.20462).toFixed(2));
      }
    }
  };

  const handleKgChange = (val: string) => {
    setConvKg(val);
    if (val === "") {
      setConvLbs("");
    } else {
      const num = parseFloat(val);
      if (!isNaN(num)) {
        setConvLbs((num * 2.20462).toFixed(2));
      }
    }
  };

  // Tape calculation trigger
  useEffect(() => {
    const girthNum = parseFloat(girth);
    const lengthNum = parseFloat(length);

    if (isNaN(girthNum) || isNaN(lengthNum) || girthNum <= 0 || lengthNum <= 0) {
      setEstLiveLbs(null);
      setEstLiveKg(null);
      setEstCarcassKg(null);
      return;
    }

    let girthInches = girthNum;
    let lengthInches = lengthNum;
    if (unit === "cm") {
      girthInches = girthNum / 2.54;
      lengthInches = lengthNum / 2.54;
    }

    // Formula: Live Lbs = (girth_inches^2 * length_inches) / 400
    let liveLbs = (Math.pow(girthInches, 2) * lengthInches) / 400;
    if (liveLbs < 150) {
      liveLbs += 7;
    }

    const liveKg = liveLbs / 2.20462;
    const carcassKg = liveKg * 0.72;

    const roundedKg = Math.round(liveKg * 10) / 10;
    setEstLiveLbs(Math.round(liveLbs * 10) / 10);
    setEstLiveKg(roundedKg);
    setEstCarcassKg(Math.round(carcassKg * 10) / 10);
    setSaveWeightInput(roundedKg.toString());
  }, [girth, length, unit]);

  // Save to pig document
  const handleSaveToProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !selectedPigId) return;

    const weightNum = parseFloat(saveWeightInput);
    if (isNaN(weightNum) || weightNum <= 0) {
      alert("Please enter a valid weight in kg.");
      return;
    }

    setSaveLoading(true);
    setSaveMessage("");
    try {
      const pigRef = doc(db, "users", activeFarmUid, "pigs", selectedPigId);
      const todayStr = new Date().toLocaleDateString("en-GB");

      await updateDoc(pigRef, {
        weight: weightNum,
        lastWeightDate: todayStr,
      });

      // Also create health record entry
      const logCollection = collection(db, "users", activeFarmUid, "pigs", selectedPigId, "health_records");
      const logRef = doc(logCollection);
      await setDoc(logRef, {
        id: logRef.id,
        type: "Weight Check",
        weight: weightNum,
        notes: `Estimated weight via tape: ${weightNum} kg (Girth: ${girth}${unit}, Length: ${length}${unit})`,
        date: todayStr,
        createdAt: new Date().toISOString(),
      });

      setSaveMessage("Weight successfully saved to pig record!");
      setTimeout(() => setSaveMessage(""), 4000);
    } catch (err) {
      console.error("Failed to save pig weight:", err);
      alert("Failed to save weight. Please check connection.");
    } finally {
      setSaveLoading(false);
    }
  };

  // Scale Calculations
  const directWeightNum = parseFloat(scaleWeight) || 0;
  const scaleLiveKg = scaleUnit === "kg" ? directWeightNum : directWeightNum * 0.453592;
  const scaleLiveLbs = scaleUnit === "kg" ? directWeightNum / 0.453592 : directWeightNum;
  const roundedScaleKg = Math.round(scaleLiveKg * 10) / 10;
  const roundedScaleLbs = Math.round(scaleLiveLbs * 10) / 10;
  const scaleCarcassKg = Math.round(roundedScaleKg * 0.72 * 10) / 10;
  const scaleCarcassLbs = Math.round(roundedScaleLbs * 0.72 * 10) / 10;

  const scaleSelectedPig = pigs.find((p) => p.id === scalePigId);

  // Growth metrics for selected pig
  const growthMetrics = (() => {
    if (!scaleSelectedPig || directWeightNum <= 0) return null;
    let ageDays = 0;
    if (scaleSelectedPig.birthDate) {
      const birth = new Date(scaleSelectedPig.birthDate);
      if (!isNaN(birth.getTime())) {
        ageDays = Math.max(0, Math.floor((Date.now() - birth.getTime()) / (1000 * 60 * 60 * 24)));
      }
    }
    const birthWeightKg = 1.3;
    const lifetimeAdgGrams = ageDays > 0 ? Math.max(0, ((roundedScaleKg - birthWeightKg) / ageDays) * 1000) : 0;
    const targetWeightKg = 90.0;
    const weightRemainingKg = Math.max(0, targetWeightKg - roundedScaleKg);
    const daysToMarket = lifetimeAdgGrams > 50 && weightRemainingKg > 0 ? Math.round(weightRemainingKg / (lifetimeAdgGrams / 1000)) : null;

    let ratingText = "Low ADG";
    let ratingClass = "bg-amber-100 text-amber-800 border-amber-200";
    if (lifetimeAdgGrams >= 700) {
      ratingText = "Optimal Growth";
      ratingClass = "bg-emerald-100 text-emerald-800 border-emerald-200";
    } else if (lifetimeAdgGrams >= 500) {
      ratingText = "Normal Growth";
      ratingClass = "bg-blue-100 text-blue-800 border-blue-200";
    }

    return {
      ageDays,
      lifetimeAdgGrams: Math.round(lifetimeAdgGrams),
      daysToMarket,
      isMarketReady: roundedScaleKg >= 90,
      ratingText,
      ratingClass,
    };
  })();

  const handleSaveScaleWeight = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !scalePigId) return;

    if (directWeightNum <= 0) {
      alert("Please enter a valid scale weight reading.");
      return;
    }

    setScaleSaveLoading(true);
    setScaleSaveMessage("");
    try {
      const pigRef = doc(db, "users", activeFarmUid, "pigs", scalePigId);
      const todayStr = new Date().toLocaleDateString("en-GB");

      await updateDoc(pigRef, {
        weight: roundedScaleKg,
        lastWeightDate: todayStr,
      });

      const logCollection = collection(db, "users", activeFarmUid, "pigs", scalePigId, "health_records");
      const logRef = doc(logCollection);
      await setDoc(logRef, {
        id: logRef.id,
        type: "Weight Check",
        weight: roundedScaleKg,
        notes: `Measured via Scale: ${directWeightNum} ${scaleUnit} (${roundedScaleKg} kg)`,
        date: todayStr,
        createdAt: new Date().toISOString(),
      });

      const pig = pigs.find((p) => p.id === scalePigId);
      setScaleSaveMessage(`Weight saved successfully for pig ${pig?.tagNumber || scalePigId}!`);
      setTimeout(() => setScaleSaveMessage(""), 4000);
    } catch (err) {
      console.error("Failed to save scale weight:", err);
      alert("Failed to save weight. Please check connection.");
    } finally {
      setScaleSaveLoading(false);
    }
  };

  // Carcass Calculations
  const carcassMetrics = (() => {
    const liveWeight = parseFloat(liveWeightKg) || 0;
    const dressingPercent = dressingType === "head_on" ? 74 : 68;
    const carcassKg = Math.round(liveWeight * (dressingPercent / 100) * 10) / 10;
    const carcassLbs = Math.round(carcassKg * 2.20462 * 10) / 10;
    const usableMeatKg = Math.round(carcassKg * 0.78 * 10) / 10;
    const usableMeatLbs = Math.round(usableMeatKg * 2.20462 * 10) / 10;
    const visceraKg = Math.round(Math.max(0, liveWeight - carcassKg) * 10) / 10;
    const visceraLbs = Math.round(visceraKg * 2.20462 * 10) / 10;

    return {
      liveWeight,
      dressingPercent,
      carcassKg,
      carcassLbs,
      usableMeatKg,
      usableMeatLbs,
      visceraKg,
      visceraLbs,
      cuts: [
        { name: "Ham / Leg (Hindquarter)", percent: 24, kg: Math.round(carcassKg * 0.24 * 10) / 10 },
        { name: "Loin / Chops / Roast", percent: 18, kg: Math.round(carcassKg * 0.18 * 10) / 10 },
        { name: "Belly / Bacon", percent: 16, kg: Math.round(carcassKg * 0.16 * 10) / 10 },
        { name: "Shoulder / Boston Butt", percent: 16, kg: Math.round(carcassKg * 0.16 * 10) / 10 },
        { name: "Spare Ribs", percent: 5, kg: Math.round(carcassKg * 0.05 * 10) / 10 },
        { name: "Trimmings / Sausage Meat", percent: 12, kg: Math.round(carcassKg * 0.12 * 10) / 10 },
        { name: "Bone, Fat & Trim Loss", percent: 9, kg: Math.round(carcassKg * 0.09 * 10) / 10 },
      ],
    };
  })();

  return (
    <div className="space-y-3">
      {/* Sub-option 1: Convert Weight */}
      <div id="suboption-convert" className="scroll-mt-20 bg-white border border-slate-200 rounded-xl overflow-hidden shadow-xs">
        <button
          type="button"
          onClick={() => toggleSubOption("convert")}
          className="w-full p-4 flex items-center justify-between text-left hover:bg-slate-50/50 transition-colors"
        >
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 rounded-lg bg-slate-100 text-slate-700 border border-slate-300 flex items-center justify-center shrink-0">
              <ScaleIcon className="h-5 w-5" />
            </div>
            <div>
              <span className="text-sm font-bold text-zinc-900">Convert Weight</span>
              <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-800">
                lbs ↔ kg
              </span>
            </div>
          </div>
          <ChevronDownIcon
            className={`h-5 w-5 text-zinc-400 transform transition-transform duration-300 ${
              openSubOption === "convert" ? "rotate-180 text-slate-700" : ""
            }`}
          />
        </button>

        {openSubOption === "convert" && (
          <div className="border-t border-slate-100 p-4 bg-zinc-50/40 space-y-4 animate-fadeIn">
            <p className="text-xs text-zinc-500 font-medium">
              Convert immediately between Imperial pounds (lbs) and Metric kilograms (kg).
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-w-lg mx-auto">
              <div className="bg-white p-3.5 rounded-xl border border-zinc-200 shadow-xs space-y-1">
                <label className="text-[10px] font-black uppercase text-zinc-400">Pounds (lbs)</label>
                <input
                  type="number"
                  step="any"
                  value={convLbs}
                  onChange={(e) => handleLbsChange(e.target.value)}
                  placeholder="e.g. 220"
                  className="w-full text-base font-bold text-zinc-900 bg-zinc-50 border border-zinc-200 rounded-lg p-2 focus:outline-none focus:ring-2 focus:ring-slate-500/20"
                />
              </div>

              <div className="bg-white p-3.5 rounded-xl border border-zinc-200 shadow-xs space-y-1">
                <label className="text-[10px] font-black uppercase text-zinc-400">Kilograms (kg)</label>
                <input
                  type="number"
                  step="any"
                  value={convKg}
                  onChange={(e) => handleKgChange(e.target.value)}
                  placeholder="e.g. 100"
                  className="w-full text-base font-bold text-zinc-900 bg-zinc-50 border border-zinc-200 rounded-lg p-2 focus:outline-none focus:ring-2 focus:ring-slate-500/20"
                />
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Sub-option 2: Weigh with Tape */}
      <div id="suboption-tape" className="scroll-mt-20 bg-white border border-slate-200 rounded-xl overflow-hidden shadow-xs">
        <button
          type="button"
          onClick={() => toggleSubOption("tape")}
          className="w-full p-4 flex items-center justify-between text-left hover:bg-slate-50/50 transition-colors"
        >
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 rounded-lg bg-slate-100 text-slate-700 border border-slate-300 flex items-center justify-center shrink-0">
              <StraightenIcon className="h-5 w-5" />
            </div>
            <div>
              <span className="text-sm font-bold text-zinc-900">Weigh with Tape</span>
              <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-800">
                Heart Girth & Length
              </span>
            </div>
          </div>
          <ChevronDownIcon
            className={`h-5 w-5 text-zinc-400 transform transition-transform duration-300 ${
              openSubOption === "tape" ? "rotate-180 text-slate-700" : ""
            }`}
          />
        </button>

        {openSubOption === "tape" && (
          <div className="border-t border-slate-100 p-4 bg-zinc-50/40 space-y-4 animate-fadeIn">
            <div className="flex items-center justify-between">
              <p className="text-xs text-zinc-500 font-medium">
                Measure Heart Girth (Point B) and Body Length (Point A).
              </p>
              {/* Unit Toggle */}
              <div className="flex rounded-lg border border-zinc-300 bg-white p-0.5">
                <button
                  type="button"
                  onClick={() => setUnit("in")}
                  className={`px-2.5 py-1 text-[11px] font-bold rounded-md transition ${
                    unit === "in" ? "bg-slate-700 text-white" : "text-zinc-600 hover:text-zinc-900"
                  }`}
                >
                  Inches
                </button>
                <button
                  type="button"
                  onClick={() => setUnit("cm")}
                  className={`px-2.5 py-1 text-[11px] font-bold rounded-md transition ${
                    unit === "cm" ? "bg-slate-700 text-white" : "text-zinc-600 hover:text-zinc-900"
                  }`}
                >
                  Centimeters
                </button>
              </div>
            </div>

            {/* Pig Diagram & Instructions */}
            <div className="bg-white p-4 rounded-xl border border-zinc-200 space-y-3">
              <div className="relative w-full max-w-sm mx-auto flex items-center justify-center p-2 bg-slate-50/50 rounded-lg border border-slate-100">
                <img
                  src="/ic_pig_scale.png"
                  alt="Pig measurement diagram"
                  className="max-h-52 w-auto object-contain rounded"
                />
              </div>
              <div className="bg-amber-50/80 border border-amber-200/80 rounded-lg p-3 text-xs text-amber-950 space-y-1.5">
                <div className="font-bold flex items-center gap-1.5 text-amber-900">
                  <svg className="w-4 h-4 text-amber-700 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span>Important Notes</span>
                </div>
                <ul className="list-disc list-inside space-y-1 text-amber-900/90 text-[11px] leading-relaxed">
                  <li><strong>Point A (Body Length):</strong> Measure from the base of the ears to the base of the tail making sure the tape is firm on the body and not loose.</li>
                  <li><strong>Point B (Heart Girth):</strong> Measure the thorax (just behind the front legs) making sure the tape fits snugly along the body and meets tightly.</li>
                  <li>Ensure the pig is standing squarely on level ground for the most accurate result.</li>
                </ul>
              </div>
            </div>

            {/* Inputs */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="bg-white p-3 rounded-xl border border-zinc-200 space-y-1">
                <label className="text-[10px] font-black uppercase text-zinc-400">
                  Heart Girth ({unit})
                </label>
                <input
                  type="number"
                  step="any"
                  value={girth}
                  onChange={(e) => setGirth(e.target.value)}
                  placeholder={`e.g. ${unit === "in" ? "42" : "106"}`}
                  className="w-full text-sm font-bold text-zinc-900 bg-zinc-50 border border-zinc-200 rounded-lg p-2 focus:outline-none focus:ring-2 focus:ring-slate-500/20"
                />
                <p className="text-[10px] text-zinc-400">Circumference directly behind front legs.</p>
              </div>

              <div className="bg-white p-3 rounded-xl border border-zinc-200 space-y-1">
                <label className="text-[10px] font-black uppercase text-zinc-400">
                  Body Length ({unit})
                </label>
                <input
                  type="number"
                  step="any"
                  value={length}
                  onChange={(e) => setLength(e.target.value)}
                  placeholder={`e.g. ${unit === "in" ? "40" : "101"}`}
                  className="w-full text-sm font-bold text-zinc-900 bg-zinc-50 border border-zinc-200 rounded-lg p-2 focus:outline-none focus:ring-2 focus:ring-slate-500/20"
                />
                <p className="text-[10px] text-zinc-400">From base of ears to base of tail.</p>
              </div>
            </div>

            {/* Estimation Results Card */}
            {estLiveKg !== null && (
              <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 space-y-3">
                <h4 className="text-xs font-black uppercase tracking-wider text-slate-800">
                  Estimated Weights
                </h4>
                <div className="grid grid-cols-3 gap-2 text-center">
                  <div className="bg-white p-2.5 rounded-lg border border-slate-200">
                    <p className="text-[10px] text-zinc-400 uppercase font-bold">Live Weight</p>
                    <p className="text-base sm:text-lg font-black text-slate-900">{estLiveKg} kg</p>
                  </div>
                  <div className="bg-white p-2.5 rounded-lg border border-slate-200">
                    <p className="text-[10px] text-zinc-400 uppercase font-bold">In Pounds</p>
                    <p className="text-base sm:text-lg font-black text-slate-900">{estLiveLbs} lbs</p>
                  </div>
                  <div className="bg-white p-2.5 rounded-lg border border-slate-200">
                    <p className="text-[10px] text-zinc-400 uppercase font-bold">Carcass (72%)</p>
                    <p className="text-base sm:text-lg font-black text-emerald-700">{estCarcassKg} kg</p>
                  </div>
                </div>

                {/* Save to Profile Form */}
                <form onSubmit={handleSaveToProfile} className="pt-2 border-t border-slate-200 space-y-2">
                  <h5 className="text-[11px] font-bold text-slate-800">Save Weight to Pig Profile:</h5>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    <select
                      value={selectedPigId}
                      onChange={(e) => setSelectedPigId(e.target.value)}
                      required
                      className="text-xs p-2 rounded-lg border border-zinc-200 bg-white font-medium"
                    >
                      <option value="">-- Select Pig Tag --</option>
                      {pigs.map((p) => (
                        <option key={p.id} value={p.id}>
                          Tag: {p.tagNumber || p.id} ({p.breed || "Pig"})
                        </option>
                      ))}
                    </select>
                    <input
                      type="number"
                      step="0.1"
                      value={saveWeightInput}
                      onChange={(e) => setSaveWeightInput(e.target.value)}
                      required
                      placeholder="Weight in kg"
                      className="text-xs p-2 rounded-lg border border-zinc-200 bg-white font-bold"
                    />
                  </div>
                  <button
                    type="submit"
                    disabled={saveLoading || !selectedPigId}
                    className="w-full py-2 rounded-lg bg-slate-800 hover:bg-slate-900 text-white text-xs font-bold transition disabled:opacity-40"
                  >
                    {saveLoading ? "Saving weight..." : "Save to Pig Profile"}
                  </button>
                  {saveMessage && (
                    <p className="text-xs font-bold text-emerald-700 text-center animate-pulse">
                      {saveMessage}
                    </p>
                  )}
                </form>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Sub-option 3: Weigh with Scale */}
      <div id="suboption-scale" className="scroll-mt-20 bg-white border border-slate-200 rounded-xl overflow-hidden shadow-xs">
        <button
          type="button"
          onClick={() => toggleSubOption("scale")}
          className="w-full p-4 flex items-center justify-between text-left hover:bg-slate-50/50 transition-colors"
        >
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 rounded-lg bg-slate-100 text-slate-700 border border-slate-300 flex items-center justify-center shrink-0">
              <ScaleIcon className="h-5 w-5" />
            </div>
            <div>
              <span className="text-sm font-bold text-zinc-900">Weigh with Scale</span>
              <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-800">
                Direct Scale Reading
              </span>
            </div>
          </div>
          <ChevronDownIcon
            className={`h-5 w-5 text-zinc-400 transform transition-transform duration-300 ${
              openSubOption === "scale" ? "rotate-180 text-slate-700" : ""
            }`}
          />
        </button>

        {openSubOption === "scale" && (
          <div className="border-t border-slate-100 p-4 bg-zinc-50/40 space-y-4 animate-fadeIn">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <p className="text-xs text-zinc-500 font-medium">
                Enter numeric scale reading directly from hanging balance or platform scale.
              </p>
              {/* Unit Toggle */}
              <div className="flex self-start sm:self-auto rounded-lg border border-zinc-300 bg-white p-0.5">
                <button
                  type="button"
                  onClick={() => setScaleUnit("lbs")}
                  className={`px-2.5 py-1 text-[11px] font-bold rounded-md transition ${
                    scaleUnit === "lbs" ? "bg-slate-700 text-white" : "text-zinc-600 hover:text-zinc-900"
                  }`}
                >
                  lbs
                </button>
                <button
                  type="button"
                  onClick={() => setScaleUnit("kg")}
                  className={`px-2.5 py-1 text-[11px] font-bold rounded-md transition ${
                    scaleUnit === "kg" ? "bg-slate-700 text-white" : "text-zinc-600 hover:text-zinc-900"
                  }`}
                >
                  kg
                </button>
              </div>
            </div>

            {/* Direct Numeric Input */}
            <div className="max-w-md mx-auto bg-white p-4 rounded-xl border border-zinc-200 shadow-xs space-y-2">
              <label className="block text-center text-[10px] font-black uppercase text-zinc-400">
                Scale Reading ({scaleUnit})
              </label>
              <input
                type="number"
                step="any"
                value={scaleWeight}
                onChange={(e) => setScaleWeight(e.target.value)}
                placeholder="0.0"
                className="w-full text-center text-2xl font-black text-zinc-900 bg-zinc-50 border border-zinc-200 rounded-lg p-3 focus:outline-none focus:ring-2 focus:ring-slate-500/20"
              />
            </div>

            {/* Results when scaleWeight > 0 */}
            {directWeightNum > 0 && (
              <div className="space-y-3">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-center">
                  <div className="bg-white p-3 rounded-xl border border-slate-200 shadow-xs">
                    <p className="text-[10px] text-zinc-400 uppercase font-bold">Estimated Live Weight</p>
                    <p className="text-lg font-black text-slate-900">
                      {roundedScaleKg} kg <span className="text-xs text-zinc-500 font-semibold">({roundedScaleLbs} lbs)</span>
                    </p>
                  </div>
                  <div className="bg-white p-3 rounded-xl border border-slate-200 shadow-xs">
                    <p className="text-[10px] text-zinc-400 uppercase font-bold">Estimated Carcass Weight (72%)</p>
                    <p className="text-lg font-black text-slate-800">
                      {scaleCarcassKg} kg <span className="text-xs text-zinc-500 font-semibold">({scaleCarcassLbs} lbs)</span>
                    </p>
                  </div>
                </div>

                {/* Growth Intelligence Card (if pig selected) */}
                {growthMetrics && (
                  <div className="bg-emerald-50/70 border border-emerald-200 rounded-xl p-3.5 space-y-2">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-1.5 text-emerald-900 font-bold text-xs">
                        <TrendingUpIcon className="h-4 w-4 text-emerald-700" />
                        <span>Growth Intelligence</span>
                      </div>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${growthMetrics.ratingClass}`}>
                        {growthMetrics.ratingText}
                      </span>
                    </div>
                    <ul className="text-xs text-emerald-950 space-y-1">
                      <li>
                        • Lifetime ADG: <strong>{growthMetrics.lifetimeAdgGrams} g/day</strong> (Age: {growthMetrics.ageDays} days)
                      </li>
                      {growthMetrics.isMarketReady ? (
                        <li className="font-bold text-emerald-800">
                          • 🎉 Pig has reached prime market weight (≥ 90 kg)!
                        </li>
                      ) : growthMetrics.daysToMarket !== null ? (
                        <li>
                          • Projected Market Date: <strong>~{growthMetrics.daysToMarket} days remaining</strong> to reach 90 kg
                        </li>
                      ) : null}
                    </ul>
                  </div>
                )}

                {/* Pig Profile Assignment & Save */}
                <form onSubmit={handleSaveScaleWeight} className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-3">
                  <h5 className="text-xs font-bold text-slate-800 text-center">Save to Pig Profile</h5>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    <div className="relative">
                      <input
                        type="text"
                        value={scalePigSearch}
                        onChange={(e) => {
                          setScalePigSearch(e.target.value);
                          if (scalePigId) setScalePigId("");
                        }}
                        placeholder="Filter pig tag..."
                        className="w-full text-xs p-2.5 rounded-lg border border-zinc-200 bg-zinc-50 font-medium focus:outline-none focus:ring-2 focus:ring-slate-500/20"
                      />
                      {scalePigSearch && (
                        <button
                          type="button"
                          onClick={() => {
                            setScalePigSearch("");
                            setScalePigId("");
                          }}
                          className="absolute right-2 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-600 text-xs font-bold"
                        >
                          ✕
                        </button>
                      )}
                    </div>

                    <select
                      value={scalePigId}
                      onChange={(e) => {
                        setScalePigId(e.target.value);
                        const p = pigs.find((pig) => pig.id === e.target.value);
                        if (p) setScalePigSearch(p.tagNumber || p.id);
                      }}
                      className="text-xs p-2.5 rounded-lg border border-zinc-200 bg-zinc-50 font-medium focus:outline-none focus:ring-2 focus:ring-slate-500/20"
                    >
                      <option value="">-- Select Pig Tag (Optional) --</option>
                      {pigs
                        .filter(
                          (p) =>
                            !scalePigSearch ||
                            (p.tagNumber && p.tagNumber.toLowerCase().includes(scalePigSearch.toLowerCase())) ||
                            p.id.toLowerCase().includes(scalePigSearch.toLowerCase())
                        )
                        .map((p) => (
                          <option key={p.id} value={p.id}>
                            Tag: {p.tagNumber || p.id} ({p.breed || "Pig"})
                          </option>
                        ))}
                    </select>
                  </div>

                  {scaleSelectedPig && (
                    <div className="text-[11px] text-zinc-500 flex items-center justify-between pt-1 border-t border-zinc-100">
                      <span>
                        Current logged weight: <strong className="text-zinc-800">{scaleSelectedPig.weight ? `${scaleSelectedPig.weight} kg` : "None"}</strong>
                      </span>
                      {scaleSelectedPig.breed && <span className="text-zinc-400">Breed: {scaleSelectedPig.breed}</span>}
                    </div>
                  )}

                  <button
                    type="submit"
                    disabled={scaleSaveLoading || !scalePigId}
                    className="w-full py-2.5 rounded-lg bg-slate-800 hover:bg-slate-900 text-white text-xs font-bold transition disabled:opacity-40 shadow-xs"
                  >
                    {scaleSaveLoading
                      ? "Saving weight..."
                      : scalePigId
                      ? `Save ${roundedScaleKg} kg to Pig #${scaleSelectedPig?.tagNumber || scalePigId}`
                      : "Select a pig above to save"}
                  </button>

                  {scaleSaveMessage && (
                    <p className="text-xs font-bold text-emerald-700 text-center animate-pulse">
                      {scaleSaveMessage}
                    </p>
                  )}
                </form>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Sub-option 4: Find Carcass Weight */}
      <div id="suboption-carcass" className="scroll-mt-20 bg-white border border-slate-200 rounded-xl overflow-hidden shadow-xs">
        <button
          type="button"
          onClick={() => toggleSubOption("carcass")}
          className="w-full p-4 flex items-center justify-between text-left hover:bg-slate-50/50 transition-colors"
        >
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 rounded-lg bg-slate-100 text-slate-700 border border-slate-300 flex items-center justify-center shrink-0">
              <CalculateIcon className="h-5 w-5" />
            </div>
            <div>
              <span className="text-sm font-bold text-zinc-900">Find Carcass Weight</span>
              <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-800">
                Meat Yield Breakdown
              </span>
            </div>
          </div>
          <ChevronDownIcon
            className={`h-5 w-5 text-zinc-400 transform transition-transform duration-300 ${
              openSubOption === "carcass" ? "rotate-180 text-slate-700" : ""
            }`}
          />
        </button>

        {openSubOption === "carcass" && (
          <div className="border-t border-slate-100 p-4 bg-zinc-50/40 space-y-4 animate-fadeIn">
            <p className="text-xs text-zinc-500 font-medium text-center">
              Calculate hot/cold carcass dressing weight and estimated primal cut yields based on live weight.
            </p>

            {/* Inputs: Live Weight & Dressing Type Buttons */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-w-lg mx-auto">
              <div className="bg-white p-3 rounded-xl border border-zinc-200 space-y-1">
                <label className="text-[10px] font-black uppercase text-zinc-400">
                  Live Pig Weight (kg)
                </label>
                <input
                  type="number"
                  step="any"
                  value={liveWeightKg}
                  onChange={(e) => setLiveWeightKg(e.target.value)}
                  placeholder="e.g. 100"
                  className="w-full text-base font-bold text-zinc-900 bg-zinc-50 border border-zinc-200 rounded-lg p-2 focus:outline-none focus:ring-2 focus:ring-slate-500/20"
                />
              </div>

              <div className="bg-white p-3 rounded-xl border border-zinc-200 space-y-1 flex flex-col justify-between">
                <label className="text-[10px] font-black uppercase text-zinc-400">
                  Dressing Type
                </label>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setDressingType("head_on")}
                    className={`flex-1 py-2 text-xs font-bold rounded-lg transition border ${
                      dressingType === "head_on"
                        ? "bg-emerald-700 border-emerald-800 text-white shadow-xs"
                        : "bg-zinc-100 border-zinc-200 text-zinc-700 hover:bg-zinc-200"
                    }`}
                  >
                    Head-On (~74%)
                  </button>
                  <button
                    type="button"
                    onClick={() => setDressingType("head_off")}
                    className={`flex-1 py-2 text-xs font-bold rounded-lg transition border ${
                      dressingType === "head_off"
                        ? "bg-emerald-700 border-emerald-800 text-white shadow-xs"
                        : "bg-zinc-100 border-zinc-200 text-zinc-700 hover:bg-zinc-200"
                    }`}
                  >
                    Head-Off (~68%)
                  </button>
                </div>
              </div>
            </div>

            {/* 3 Result Cards */}
            {carcassMetrics.liveWeight > 0 && (
              <div className="space-y-3">
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  {/* Card 1: Estimated Carcass Weight (Clickable) */}
                  <div
                    onClick={() => setShowPrimalCuts((prev) => !prev)}
                    className="cursor-pointer bg-white p-4 rounded-xl border-2 border-emerald-500/50 hover:border-emerald-600 transition shadow-xs space-y-1 relative group"
                    title="Click to toggle Estimated Primal Cuts Yield breakdown"
                  >
                    <div className="flex items-center justify-between">
                      <p className="text-[11px] font-bold text-zinc-600">Estimated Carcass Weight</p>
                      <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-1.5 py-0.5 rounded border border-emerald-200">
                        {showPrimalCuts ? "Hide Cuts ▲" : "View Cuts ▼"}
                      </span>
                    </div>
                    <p className="text-[10px] text-zinc-400">{carcassMetrics.dressingPercent}% dressing</p>
                    <p className="text-lg font-black text-emerald-700">
                      {carcassMetrics.carcassKg} kg{" "}
                      <span className="text-xs font-semibold text-zinc-500">({carcassMetrics.carcassLbs} lbs)</span>
                    </p>
                  </div>

                  {/* Card 2: Estimated Usable Meat */}
                  <div className="bg-white p-4 rounded-xl border border-zinc-200 shadow-xs space-y-1">
                    <p className="text-[11px] font-bold text-zinc-600">Estimated Usable Meat</p>
                    <p className="text-[10px] text-zinc-400">~78% of carcass weight</p>
                    <p className="text-lg font-black text-blue-700">
                      {carcassMetrics.usableMeatKg} kg{" "}
                      <span className="text-xs font-semibold text-zinc-500">({carcassMetrics.usableMeatLbs} lbs)</span>
                    </p>
                  </div>

                  {/* Card 3: Viscera & Offal */}
                  <div className="bg-white p-4 rounded-xl border border-zinc-200 shadow-xs space-y-1">
                    <p className="text-[11px] font-bold text-zinc-600">Viscera & Offal</p>
                    <p className="text-[10px] text-zinc-400">Blood, viscera & gut fill</p>
                    <p className="text-lg font-black text-rose-700">
                      -{carcassMetrics.visceraKg} kg{" "}
                      <span className="text-xs font-semibold text-zinc-500">({carcassMetrics.visceraLbs} lbs)</span>
                    </p>
                  </div>
                </div>

                {/* Primal Cuts Breakdown (Shown when showPrimalCuts is true) */}
                {showPrimalCuts && (
                  <div className="bg-white border border-slate-200 rounded-xl p-4 space-y-3 animate-fadeIn">
                    <div className="flex items-center justify-between border-b border-zinc-100 pb-2">
                      <h4 className="text-xs font-black uppercase tracking-wider text-slate-900">
                        Estimated Primal Cuts Yield Breakdown
                      </h4>
                      <span className="text-[11px] font-medium text-zinc-500">
                        Basis: {carcassMetrics.carcassKg} kg carcass
                      </span>
                    </div>
                    <div className="divide-y divide-zinc-100 text-xs">
                      {carcassMetrics.cuts.map((cut, i) => (
                        <div key={i} className="py-2 flex items-center justify-between">
                          <span className="font-semibold text-zinc-800">{cut.name}</span>
                          <div className="flex items-center gap-3">
                            <span className="text-[11px] text-zinc-400 font-medium">{cut.percent}%</span>
                            <span className="font-bold text-slate-900 min-w-[55px] text-right">{cut.kg} kg</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
