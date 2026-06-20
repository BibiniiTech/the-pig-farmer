"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { collection, onSnapshot, doc, updateDoc, setDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import UserProfileDropdown from "@/components/UserProfileDropdown";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import { ScaleIcon, InfoIcon } from "@/components/icons/DashboardIcons";
import { useTranslations } from "next-intl";

interface Pig {
  id: string;
  tagNumber: string;
  breed: string;
  gender: string;
  status: string;
  weight?: number;
  location: string;
}

export default function WeightCheckerPage() {
  const t = useTranslations("Weight");
  const { user, activeFarmUid, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  const [pigs, setPigs] = useState<Pig[]>([]);
  const [dataLoading, setDataLoading] = useState(true);
  const [saveLoading, setSaveLoading] = useState(false);

  // Unit converter states
  const [convLbs, setConvLbs] = useState("");
  const [convKg, setConvKg] = useState("");

  // Tape calculator states
  const [unit, setUnit] = useState<"in" | "cm">("in");
  const [girth, setGirth] = useState("");
  const [length, setLength] = useState("");
  
  // Results of tape estimation
  const [estLiveLbs, setEstLiveLbs] = useState<number | null>(null);
  const [estLiveKg, setEstLiveKg] = useState<number | null>(null);
  const [estCarcassLbs, setEstCarcassLbs] = useState<number | null>(null);
  const [estCarcassKg, setEstCarcassKg] = useState<number | null>(null);

  // Profile save states
  const [selectedPigId, setSelectedPigId] = useState("");
  const [customSaveWeight, setCustomSaveWeight] = useState(""); // Default to estimated, but editable

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  // Listen to Pigs
  useEffect(() => {
    if (!activeFarmUid) return;

    setDataLoading(true);
    const pigsQuery = collection(db, "users", activeFarmUid, "pigs");
    const unsubscribe = onSnapshot(
      pigsQuery,
      (snapshot) => {
        const list = snapshot.docs.map((doc) => ({
          id: doc.id,
          ...doc.data(),
        })) as Pig[];
        setPigs(list.sort((a, b) => a.tagNumber.localeCompare(b.tagNumber)));
        setDataLoading(false);
      },
      (error) => {
        console.error("Error fetching pigs:", error);
        setDataLoading(false);
      }
    );

    return () => unsubscribe();
  }, [activeFarmUid]);

  // Converter Handlers
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

  // Tape Calculator trigger
  useEffect(() => {
    const girthNum = parseFloat(girth);
    const lengthNum = parseFloat(length);

    if (isNaN(girthNum) || isNaN(lengthNum) || girthNum <= 0 || lengthNum <= 0) {
      setEstLiveLbs(null);
      setEstLiveKg(null);
      setEstCarcassLbs(null);
      setEstCarcassKg(null);
      return;
    }

    // Convert to inches if CM
    let girthInches = girthNum;
    let lengthInches = lengthNum;
    if (unit === "cm") {
      girthInches = girthNum / 2.54;
      lengthInches = lengthNum / 2.54;
    }

    // Formula: Live Lbs = (girth_inches^2 * length_inches) / 400
    let liveLbs = (Math.pow(girthInches, 2) * lengthInches) / 400;

    // Adjustments: if live weight < 150 lbs, add 7 lbs
    if (liveLbs < 150) {
      liveLbs += 7;
    }

    const liveKg = liveLbs / 2.20462;

    // Carcass is ~72% of live weight
    const carcassLbs = liveLbs * 0.72;
    const carcassKg = liveKg * 0.72;

    setEstLiveLbs(Math.round(liveLbs * 10) / 10);
    setEstLiveKg(Math.round(liveKg * 10) / 10);
    setEstCarcassLbs(Math.round(carcassLbs * 10) / 10);
    setEstCarcassKg(Math.round(carcassKg * 10) / 10);

    // Auto-update target save weight input with the estimated Kg
    setCustomSaveWeight((Math.round(liveKg * 10) / 10).toString());
  }, [girth, length, unit]);

  // Save to pig document
  const handleSaveToProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !selectedPigId) return;

    const weightNum = parseFloat(customSaveWeight);
    if (isNaN(weightNum) || weightNum <= 0) {
      alert(t("validWeightAlert"));
      return;
    }

    setSaveLoading(true);
    try {
      const today = new Date().toISOString().split("T")[0];
      const pigRef = doc(db, "users", activeFarmUid, "pigs", selectedPigId);
      
      // Update primary pig document weight
      await updateDoc(pigRef, { weight: weightNum });

      // Add a health log entry
      const logCollection = collection(db, "users", activeFarmUid, "pigs", selectedPigId, "health_records");
      const logRef = doc(logCollection);

      // Create details message
      let desc = t("weightCheckedManually", { weight: weightNum });
      if (estLiveKg && Math.abs(estLiveKg - weightNum) < 0.2) {
        desc = t("tapeEstimationLog", {
          weight: weightNum,
          lbs: estLiveLbs,
          girth,
          unit: unit === "in" ? t("inches") : t("centimeters"),
          length
        });
      }

      await setDoc(logRef, {
        id: logRef.id,
        date: today,
        type: "Weight Check",
        description: desc,
      });

      alert(t("saveSuccess"));
      // Reset profile select
      setSelectedPigId("");
      setCustomSaveWeight("");
    } catch (err) {
      console.error("Failed to save pig weight:", err);
      alert(t("saveError"));
    } finally {
      setSaveLoading(false);
    }
  };

  if (loading) {
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
        {/* Header */}
        {!isMobile && <DesktopHeader />}

        {/* Content Body */}
        <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
          <div className="bg-white/60 backdrop-blur-sm border border-zinc-200 rounded-2xl p-6 shadow-sm">
            <h2 className="text-xl font-bold text-zinc-900">{t("title")}</h2>
            <p className="text-sm text-zinc-500 mt-1">{t("description")}</p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Left Column: Tape Calculator (Span 7) */}
            <div className="lg:col-span-7 bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-6">
              <div className="flex flex-col gap-4">
                <h3 className="text-base font-bold text-zinc-900 flex items-center gap-2">
                  <span>📏</span> {t("weighWithTape")}
                </h3>
                {/* Unit selector */}
                <div className="inline-flex w-fit rounded-lg border border-zinc-200 bg-zinc-50 p-1">
                  <button
                    onClick={() => setUnit("in")}
                    className={`px-3 py-1 text-xs font-bold rounded-md transition ${
                      unit === "in" ? "bg-white text-teal-700 shadow-sm" : "text-zinc-500 hover:text-zinc-800"
                    }`}
                  >
                    {t("inches")}
                  </button>
                  <button
                    onClick={() => setUnit("cm")}
                    className={`px-3 py-1 text-xs font-bold rounded-md transition ${
                      unit === "cm" ? "bg-white text-teal-700 shadow-sm" : "text-zinc-500 hover:text-zinc-800"
                    }`}
                  >
                    {t("centimeters")}
                  </button>
                </div>
              </div>

              {/* Form Inputs */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-zinc-650 mb-1.5">
                    {t("bodyLength", { unit: unit === "in" ? t("inches") : t("centimeters") })}
                  </label>
                  <input
                    type="number"
                    step="any"
                    placeholder={`e.g. ${unit === "in" ? "38" : "96"}`}
                    value={length}
                    onChange={(e) => setLength(e.target.value)}
                    className="w-full rounded-xl border border-zinc-200 bg-white/70 px-4 py-2.5 text-xs font-semibold focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
                  />
                  <p className="text-[10px] text-zinc-400 mt-1">{t("bodyLengthDesc")}</p>
                </div>

                <div>
                  <label className="block text-xs font-bold text-zinc-650 mb-1.5">
                    {t("heartGirth", { unit: unit === "in" ? t("inches") : t("centimeters") })}
                  </label>
                  <input
                    type="number"
                    step="any"
                    placeholder={`e.g. ${unit === "in" ? "42" : "106"}`}
                    value={girth}
                    onChange={(e) => setGirth(e.target.value)}
                    className="w-full rounded-xl border border-zinc-200 bg-white/70 px-4 py-2.5 text-xs font-semibold focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
                  />
                  <p className="text-[10px] text-zinc-400 mt-1">{t("heartGirthDesc")}</p>
                </div>
              </div>

              {/* Simple illustrative diagram of a pig showing girth & length */}
              <div className="relative rounded-xl border border-zinc-150 bg-zinc-50/50 p-4 flex flex-col items-center">
                <img
                  src="/ic_pig_scale.png"
                  alt="Pig Measurement Guide"
                  className={`w-full max-w-[320px] h-auto object-contain transition-opacity duration-300 ${
                    girth || length ? "opacity-100" : "opacity-50"
                  }`}
                />
                <div className="text-[10px] text-zinc-500 font-bold text-center mt-2">
                  {t("measurementGuide")}
                </div>
              </div>

              {/* Tape Estimation Results */}
              <div className="border-t border-zinc-200 pt-6 space-y-4">
                <h4 className="text-xs font-bold text-zinc-500 uppercase tracking-wider">{t("estimatedResults")}</h4>
                {estLiveLbs !== null ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="bg-teal-50/40 border border-teal-200/50 rounded-xl p-4 space-y-1">
                      <span className="text-[10px] font-bold text-teal-800 uppercase">{t("estLiveWeight")}</span>
                      <div className="text-2xl font-extrabold text-teal-900">
                        {estLiveKg} kg <span className="text-sm font-semibold text-teal-700">/ {estLiveLbs} lbs</span>
                      </div>
                    </div>

                    <div className="bg-zinc-50/70 border border-zinc-200 rounded-xl p-4 space-y-1">
                      <span className="text-[10px] font-bold text-zinc-500 uppercase">{t("estCarcassWeight")}</span>
                      <div className="text-2xl font-extrabold text-zinc-800">
                        {estCarcassKg} kg <span className="text-sm font-semibold text-zinc-500">/ {estCarcassLbs} lbs</span>
                      </div>
                    </div>
                  </div>
                ) : (
                  <p className="text-xs text-zinc-400 font-semibold text-center py-4 bg-zinc-50/30 rounded-xl border border-dashed border-zinc-200">
                    {t("enterPositiveValues")}
                  </p>
                )}
              </div>
            </div>

            {/* Right Column: Weight Converter & Profile Sync (Span 5) */}
            <div className="lg:col-span-5 space-y-8">
              {/* Unit Converter Card */}
              <div className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-4">
                <h3 className="text-base font-bold text-zinc-900 flex items-center gap-2">
                  <ScaleIcon className="h-5 w-5 text-teal-600" /> {t("weightConverter")}
                </h3>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-zinc-650 mb-1.5">{t("pounds")}</label>
                    <input
                      type="number"
                      placeholder="e.g. 180"
                      value={convLbs}
                      onChange={(e) => handleLbsChange(e.target.value)}
                      className="w-full rounded-xl border border-zinc-200 bg-white/70 px-4 py-2.5 text-xs font-semibold focus:border-teal-500 focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-zinc-650 mb-1.5">{t("kilograms")}</label>
                    <input
                      type="number"
                      placeholder="e.g. 81.6"
                      value={convKg}
                      onChange={(e) => handleKgChange(e.target.value)}
                      className="w-full rounded-xl border border-zinc-200 bg-white/70 px-4 py-2.5 text-xs font-semibold focus:border-teal-500 focus:outline-none"
                    />
                  </div>
                </div>
              </div>

              {/* Save weight to pig card */}
              <div className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-4">
                <h3 className="text-base font-bold text-zinc-900 flex items-center gap-2">
                  <span>💾</span> {t("saveToProfile")}
                </h3>

                {dataLoading ? (
                  <div className="h-28 bg-zinc-50 animate-pulse rounded-xl" />
                ) : pigs.length === 0 ? (
                  <p className="text-xs text-zinc-500 text-center py-4">{t("noPigs")}</p>
                ) : (
                  <form onSubmit={handleSaveToProfile} className="space-y-4">
                    <div>
                      <label className="block text-xs font-bold text-zinc-650 mb-1.5">{t("selectPigTag")}</label>
                      <select
                        value={selectedPigId}
                        onChange={(e) => setSelectedPigId(e.target.value)}
                        required
                        className="w-full rounded-xl border border-zinc-200 bg-white/70 px-4 py-2.5 text-xs font-semibold focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
                      >
                        <option value="">{t("choosePigTag")}</option>
                        {pigs.map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.tagNumber} ({p.gender} • {p.status})
                          </option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-zinc-650 mb-1.5">{t("weightToSave")}</label>
                      <input
                        type="number"
                        step="any"
                        required
                        placeholder="e.g. 80.5"
                        value={customSaveWeight}
                        onChange={(e) => setCustomSaveWeight(e.target.value)}
                        className="w-full rounded-xl border border-zinc-200 bg-white/70 px-4 py-2.5 text-xs font-semibold focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
                      />
                    </div>

                    <button
                      type="submit"
                      disabled={saveLoading || !selectedPigId}
                      className="w-full py-2.5 rounded-xl bg-teal-600 hover:bg-teal-700 disabled:bg-zinc-200 disabled:text-zinc-450 text-xs font-bold text-white shadow transition"
                    >
                      {saveLoading ? t("savingWeight") : t("saveWeight")}
                    </button>
                  </form>
                )}
              </div>

              {/* Important Notes Card */}
              <div className="bg-zinc-50/80 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-4">
                <div className="flex items-center justify-center gap-2 text-teal-700">
                  <InfoIcon className="h-5 w-5" />
                  <h3 className="text-sm font-bold uppercase tracking-wider">{t("importantNotes")}</h3>
                </div>
                <div className="space-y-3 text-xs text-zinc-600 leading-relaxed text-justify">
                  <p>
                    <span className="font-bold text-zinc-800">{t("noteBodyLength")}</span>
                  </p>
                  <p>
                    <span className="font-bold text-zinc-800">{t("noteHeartGirth")}</span>
                  </p>
                  <p>
                    {t("noteSquarely")}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
