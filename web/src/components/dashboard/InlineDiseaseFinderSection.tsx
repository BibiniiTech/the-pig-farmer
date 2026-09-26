"use client";

import React, { useState, useMemo, useEffect } from "react";
import { useTranslations } from "next-intl";
import { collection, onSnapshot, doc, setDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { Pig } from "@/lib/types";
import PremiumWrapper from "@/components/PremiumWrapper";
import Link from "next/link";
import {
  SYMPTOM_GROUPS,
  DISEASES,
  DiseaseItem,
  SeverityLevel,
  PigStage,
  PIG_STAGE_OPTIONS,
  getStageFromPigStatus,
  isPathognomonic,
  getSymptomSubtitle,
  diagnoseSwineDiseases,
  DiagnosisMatchResult,
} from "@/lib/swineDiseasesData";

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

const SearchIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <circle cx="11" cy="11" r="8" />
    <line x1="21" y1="21" x2="16.65" y2="16.65" />
  </svg>
);

const BookOpenIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
    <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
  </svg>
);

const WarningIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
    <line x1="12" y1="9" x2="12" y2="13" />
    <line x1="12" y1="17" x2="12.01" y2="17" />
  </svg>
);

const StarIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" {...props}>
    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
  </svg>
);

const NoteAddIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
    <polyline points="14 2 14 8 20 8" />
    <line x1="12" y1="18" x2="12" y2="12" />
    <line x1="9" y1="15" x2="15" y2="15" />
  </svg>
);

const ShareIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <circle cx="18" cy="5" r="3" />
    <circle cx="6" cy="12" r="3" />
    <circle cx="18" cy="19" r="3" />
    <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
    <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
  </svg>
);

const CheckIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <polyline points="20 6 9 17 4 12" />
  </svg>
);

const CloseIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <line x1="18" y1="6" x2="6" y2="18" />
    <line x1="6" y1="6" x2="18" y2="18" />
  </svg>
);

export default function InlineDiseaseFinderSection() {
  const t = useTranslations("Symptoms");
  const td = useTranslations("Dashboard");
  const { activeFarmUid } = useAuth();

  // Pigs in farm
  const [allPigs, setAllPigs] = useState<Pig[]>([]);
  const [selectedPigId, setSelectedPigId] = useState<string | null>(null);
  const [showPigSelector, setShowPigSelector] = useState(false);
  const [pigFilterQuery, setPigFilterQuery] = useState("");

  const selectedPig = useMemo(() => {
    return allPigs.find((p) => p.id === selectedPigId) || null;
  }, [allPigs, selectedPigId]);

  // Listen to Pigs
  useEffect(() => {
    if (!activeFarmUid) return;
    const pigsRef = collection(db, "users", activeFarmUid, "pigs");
    const unsubscribe = onSnapshot(
      pigsRef,
      (snapshot) => {
        const list = snapshot.docs
          .map((d) => ({ id: d.id, ...d.data() } as Pig))
          .filter((p) => p.location !== "Archived" && !p.status?.toLowerCase().startsWith("archived"))
          .sort((a, b) => (a.tagNumber || a.id).localeCompare(b.tagNumber || b.id));
        setAllPigs(list);
      },
      (err) => console.error("Error loading pigs for disease finder:", err)
    );
    return () => unsubscribe();
  }, [activeFarmUid]);

  // Production Stage
  const [selectedStage, setSelectedStage] = useState<PigStage>("stage_all");

  // Sub-options: "symptoms" | "diseases" | null
  const [openSubOption, setOpenSubOption] = useState<string | null>(null);

  // Sub-option 1: Symptoms diagnosis states
  const [selectedSymptoms, setSelectedSymptoms] = useState<Set<string>>(new Set());
  const [symptomSearchQuery, setSymptomSearchQuery] = useState("");
  const [openSymptomCategory, setOpenSymptomCategory] = useState<string | null>("sg_skin_coat");
  const [diagnosisResults, setDiagnosisResults] = useState<DiagnosisMatchResult[]>([]);
  const [hasDiagnosed, setHasDiagnosed] = useState(false);
  const [expandedResultId, setExpandedResultId] = useState<string | null>(null);

  // Action toasts / feedback
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 4000);
  };

  // Sub-option 2: Common Diseases states
  const [openCommonCategory, setOpenCommonCategory] = useState<string | null>(null);
  const [expandedCommonDiseaseId, setExpandedCommonDiseaseId] = useState<string | null>(null);

  const scrollToSubOption = (key: string, delayMs = 150) => {
    if (typeof window === "undefined") return;
    setTimeout(() => {
      const el = document.getElementById(`suboption-${key}`);
      if (el) {
        el.scrollIntoView({ behavior: "smooth", block: "start" });
      }
    }, delayMs);
  };

  const toggleSubOption = (key: string) => {
    setOpenSubOption((prev) => {
      const next = prev === key ? null : key;
      if (next) {
        scrollToSubOption(next, 150);
      }
      return next;
    });
  };

  const handleSelectPig = (pig: Pig | null) => {
    if (!pig) {
      setSelectedPigId(null);
      setSelectedStage("stage_all");
    } else {
      setSelectedPigId(pig.id);
      setSelectedStage(getStageFromPigStatus(pig.status, pig.purpose));
    }
    setShowPigSelector(false);
  };

  const toggleSymptom = (key: string) => {
    setSelectedSymptoms((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  const handleAnalyze = () => {
    if (selectedSymptoms.size === 0) return;
    const results = diagnoseSwineDiseases(selectedSymptoms, selectedStage);
    setDiagnosisResults(results);
    setHasDiagnosed(true);
    if (results.length > 0) {
      setExpandedResultId(results[0].disease.id);
    }
  };

  const handleReset = () => {
    setSelectedSymptoms(new Set());
    setDiagnosisResults([]);
    setHasDiagnosed(false);
    setExpandedResultId(null);
    setSelectedPigId(null);
    setSelectedStage("stage_all");
    setSymptomSearchQuery("");
  };

  const getSeverityBadge = (severity: SeverityLevel) => {
    switch (severity) {
      case "CRITICAL":
        return "bg-red-100 text-red-800 border-red-200";
      case "HIGH":
        return "bg-orange-100 text-orange-800 border-orange-200";
      case "MODERATE":
        return "bg-amber-100 text-amber-800 border-amber-200";
      case "LOW":
      default:
        return "bg-emerald-100 text-emerald-800 border-emerald-200";
    }
  };

  const renderSymptomName = (sKey: string) => {
    try {
      return t(`list.${sKey}`);
    } catch {
      return sKey.replace("sym_", "").replace(/_/g, " ");
    }
  };

  const renderGroupName = (gKey: string) => {
    try {
      return t(`groups.${gKey}`);
    } catch {
      return gKey.replace("sg_", "").replace(/_/g, " ");
    }
  };

  const renderDiseaseName = (id: string) => {
    try {
      return t(`diseases.${id}.name`);
    } catch {
      return id.replace("dis_", "").replace(/_/g, " ");
    }
  };

  const renderDiseaseScientificName = (id: string) => {
    try {
      return t(`diseases.${id}.scientificName`);
    } catch {
      return "";
    }
  };

  const renderDiseaseDescription = (id: string) => {
    try {
      return t(`diseases.${id}.description`);
    } catch {
      return "";
    }
  };

  const renderDiseasePrevention = (id: string) => {
    try {
      return t(`diseases.${id}.prevention`);
    } catch {
      return "";
    }
  };

  // Log to Herd Records
  const handleLogToHerd = async (match: DiagnosisMatchResult) => {
    if (!selectedPig) {
      setShowPigSelector(true);
      showToast("Please select a pig from your herd to log this health record.");
      return;
    }
    if (!activeFarmUid) return;

    try {
      const today = new Date().toISOString().split("T")[0];
      const diseaseName = renderDiseaseName(match.disease.id);
      const symptomList = Array.from(selectedSymptoms).map(renderSymptomName).join(", ");
      const desc = `Suspected diagnosis: ${diseaseName} (${match.matchPercent}% match) based on: ${symptomList}`;
      const recType =
        match.disease.isNotifiable || match.disease.severity === "CRITICAL"
          ? "Quarantine"
          : "Diagnosis / Treatment";
      const medication = renderDiseasePrevention(match.disease.id) || "";

      const healthRecCol = collection(db, "users", activeFarmUid, "pigs", selectedPig.id, "health_records");
      const newRecRef = doc(healthRecCol);

      await setDoc(newRecRef, {
        id: newRecRef.id,
        date: today,
        type: recType,
        description: desc,
        medication,
      });

      showToast(`Logged to Health Records for Pig Tag ${selectedPig.tagNumber}!`);
    } catch (err) {
      console.error("Failed to log health record:", err);
      showToast("Failed to save health record. Please try again.");
    }
  };

  // Telehealth Report Sharing
  const handleExportTelehealth = async (match: DiagnosisMatchResult) => {
    const today = new Date().toISOString().replace("T", " ").substring(0, 16);
    const targetText = selectedPig
      ? `Tag ${selectedPig.tagNumber} (${selectedPig.status || "Unknown"}, ${selectedPig.gender || ""}${
          selectedPig.location ? `, Pen ${selectedPig.location}` : ""
        })`
      : "General Pen / Unassigned Group";

    const stageObj = PIG_STAGE_OPTIONS.find((s) => s.key === selectedStage);
    const stageLabel = stageObj ? stageObj.label : "All Stages";

    const observedSigns = Array.from(selectedSymptoms)
      .map((s) => {
        const isHallmark = isPathognomonic(s);
        return `• ${renderSymptomName(s)}${isHallmark ? " [⭐ Hallmark Sign]" : ""}`;
      })
      .join("\n");

    const hallmarkList =
      match.matchedPathognomonic.length > 0
        ? `\n• Hallmark Signs: ${match.matchedPathognomonic.map(renderSymptomName).join(", ")}`
        : "";

    const preventionText = renderDiseasePrevention(match.disease.id);

    const report = [
      "📋 SWINE CLINICAL TELEHEALTH REPORT",
      "------------------------------------",
      `Date: ${today}`,
      `Subject: ${targetText}`,
      `Production Stage: ${stageLabel}`,
      "",
      `OBSERVED SIGNS (${selectedSymptoms.size}):`,
      observedSigns,
      "",
      "SUSPECTED CLINICAL DIAGNOSIS:",
      `• Disease: ${renderDiseaseName(match.disease.id)} (${renderDiseaseScientificName(match.disease.id)})`,
      `• Match Confidence: ${match.matchPercent}%`,
      `• Severity: ${match.disease.severity}${hallmarkList}`,
      "",
      "RECOMMENDED ACTION / INTERVENTION:",
      preventionText || "Consult a certified veterinary practitioner immediately.",
      "",
      match.disease.isNotifiable
        ? "⚠️ EMERGENCY BIOSECURITY PROTOCOL:\nStrict quarantine required. Stop all movements of pigs, feed, and equipment. Alert veterinary authority immediately."
        : "",
      "Generated via SmartSwine Disease Diagnostics & Piggery Management System",
    ]
      .filter(Boolean)
      .join("\n");

    try {
      if (typeof navigator !== "undefined" && navigator.share) {
        await navigator.share({
          title: `Swine Telehealth Report - ${renderDiseaseName(match.disease.id)}`,
          text: report,
        });
      } else if (typeof navigator !== "undefined" && navigator.clipboard) {
        await navigator.clipboard.writeText(report);
        showToast("Telehealth report copied to clipboard! Ready to send to veterinarian.");
      }
    } catch {
      // Fallback
      if (typeof navigator !== "undefined" && navigator.clipboard) {
        await navigator.clipboard.writeText(report);
        showToast("Telehealth report copied to clipboard!");
      }
    }
  };

  // Filter symptoms for instant search
  const allSymptomKeys = useMemo(() => {
    return Array.from(new Set(SYMPTOM_GROUPS.flatMap((g) => g.symptoms)));
  }, []);

  const matchingSymptoms = useMemo(() => {
    if (!symptomSearchQuery.trim()) return [];
    const q = symptomSearchQuery.toLowerCase().trim();
    return allSymptomKeys.filter((k) => {
      const name = renderSymptomName(k).toLowerCase();
      const subtitle = (getSymptomSubtitle(k) || "").toLowerCase();
      return name.includes(q) || subtitle.includes(q) || k.toLowerCase().includes(q);
    });
  }, [symptomSearchQuery, allSymptomKeys]);

  const notifiableAlert = useMemo(() => {
    return diagnosisResults.find((r) => r.isNotifiableAlert) || null;
  }, [diagnosisResults]);

  return (
    <PremiumWrapper fallback={
      <div className="p-8 md:p-12 text-center border border-pink-200 rounded-3xl bg-pink-50/50 backdrop-blur-sm space-y-5 max-w-2xl mx-auto shadow-sm my-4">
        <div className="h-16 w-16 mx-auto rounded-2xl bg-pink-100 text-pink-600 flex items-center justify-center shadow-inner">
          <svg className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
            <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
          </svg>
        </div>
        <div className="space-y-2">
          <h2 className="text-xl sm:text-2xl font-black text-zinc-900">Symptoms & Veterinary Diagnostics is a Premium Feature</h2>
          <p className="text-xs sm:text-sm text-zinc-600 max-w-md mx-auto leading-relaxed">
            Upgrade to SmartSwine Premium to unlock differential swine disease matching, clinical signs analysis, hallmark symptoms, and vet-approved interventions.
          </p>
        </div>
        <Link
          href="/dashboard/billing"
          className="inline-block px-7 py-3 bg-gradient-to-r from-emerald-600 to-green-600 hover:from-emerald-700 hover:to-green-700 text-white text-xs font-bold rounded-xl shadow-lg shadow-emerald-600/20 transition-all active:scale-95"
        >
          Upgrade to Premium
        </Link>
      </div>
    }>
      <div className="space-y-3">
      {/* Toast Feedback */}
      {toastMessage && (
        <div className="fixed bottom-20 left-1/2 -translate-x-1/2 z-50 px-4 py-2.5 bg-zinc-900 text-white text-xs font-bold rounded-xl shadow-xl border border-zinc-700 animate-bounce">
          {toastMessage}
        </div>
      )}

      {/* Pig Selector Modal */}
      {showPigSelector && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-5 space-y-4 shadow-2xl border border-zinc-200 animate-fadeIn">
            <div className="flex items-center justify-between pb-2 border-b border-zinc-100">
              <h3 className="text-sm font-bold text-zinc-900">Select Affected Pig</h3>
              <button
                type="button"
                onClick={() => setShowPigSelector(false)}
                className="p-1 rounded-lg text-zinc-400 hover:text-zinc-600 hover:bg-zinc-100"
              >
                <CloseIcon className="h-5 w-5" />
              </button>
            </div>

            <div className="relative">
              <SearchIcon className="h-4 w-4 absolute left-3 top-3 text-zinc-400" />
              <input
                type="text"
                placeholder="Search tag number, breed, or pen..."
                value={pigFilterQuery}
                onChange={(e) => setPigFilterQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-xs bg-zinc-50 border border-zinc-200 rounded-xl focus:outline-hidden focus:border-indigo-500"
              />
            </div>

            <div className="max-h-60 overflow-y-auto space-y-1.5 pr-1">
              <button
                type="button"
                onClick={() => handleSelectPig(null)}
                className="w-full p-2.5 rounded-xl border border-dashed border-zinc-300 text-left text-xs font-medium text-zinc-500 hover:bg-zinc-50 flex items-center justify-between"
              >
                <span>Clear Pig (Unassigned Herd Group)</span>
                <CloseIcon className="h-4 w-4 text-zinc-400" />
              </button>

              {allPigs
                .filter((p) => {
                  const q = pigFilterQuery.toLowerCase();
                  return (
                    p.tagNumber?.toLowerCase().includes(q) ||
                    p.breed?.toLowerCase().includes(q) ||
                    p.location?.toLowerCase().includes(q) ||
                    p.status?.toLowerCase().includes(q)
                  );
                })
                .map((pig) => {
                  const isSelected = selectedPigId === pig.id;
                  return (
                    <button
                      key={pig.id}
                      type="button"
                      onClick={() => handleSelectPig(pig)}
                      className={`w-full p-3 rounded-xl border text-left flex items-center justify-between transition ${
                        isSelected
                          ? "bg-indigo-50 border-indigo-300 text-indigo-950 font-bold"
                          : "bg-white border-zinc-200 text-zinc-800 hover:bg-zinc-50"
                      }`}
                    >
                      <div>
                        <div className="text-xs font-bold flex items-center gap-1.5">
                          <span>Tag {pig.tagNumber}</span>
                          <span className="text-[10px] px-1.5 py-0.5 rounded bg-zinc-100 text-zinc-600 font-normal">
                            {pig.status}
                          </span>
                        </div>
                        <p className="text-[11px] text-zinc-400 font-normal">
                          {pig.gender} • {pig.breed || "Crossbreed"} {pig.location ? `• Pen ${pig.location}` : ""}
                        </p>
                      </div>
                      {isSelected && <CheckIcon className="h-4 w-4 text-indigo-600" />}
                    </button>
                  );
                })}
            </div>
          </div>
        </div>
      )}

      {/* Sub-option 1: Find with Symptoms */}
      <div id="suboption-symptoms" className="scroll-mt-20 bg-white border border-indigo-100 rounded-xl overflow-hidden shadow-xs">
        <button
          type="button"
          onClick={() => toggleSubOption("symptoms")}
          className="w-full p-4 flex items-center justify-between text-left hover:bg-indigo-50/30 transition-colors"
        >
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 rounded-lg bg-indigo-50 text-indigo-600 border border-indigo-200/60 flex items-center justify-center shrink-0">
              <SearchIcon className="h-5 w-5" />
            </div>
            <div>
              <span className="text-sm font-bold text-zinc-900">{td("findWithSymptoms") || "Find with Symptoms"}</span>
              {selectedSymptoms.size > 0 && (
                <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-800">
                  {selectedSymptoms.size} Selected
                </span>
              )}
            </div>
          </div>
          <ChevronDownIcon
            className={`h-5 w-5 text-zinc-400 transform transition-transform duration-300 ${
              openSubOption === "symptoms" ? "rotate-180 text-indigo-600" : ""
            }`}
          />
        </button>

        {openSubOption === "symptoms" && (
          <div className="border-t border-indigo-100 p-4 space-y-4 bg-zinc-50/40 animate-fadeIn">
            {/* 1. Affected Pig Tagging Banner */}
            <div
              className={`p-3 rounded-xl border flex items-center justify-between gap-3 ${
                selectedPig
                  ? "bg-indigo-50/70 border-indigo-200 text-indigo-950"
                  : "bg-white border-zinc-200/80 text-zinc-700"
              }`}
            >
              <div className="space-y-0.5 min-w-0">
                <p className="text-xs font-bold truncate">
                  {selectedPig ? (
                    <span>Affected Pig: Tag {selectedPig.tagNumber}</span>
                  ) : (
                    <span>Herd Link: Unassigned</span>
                  )}
                </p>
                <p className="text-[11px] text-zinc-500">
                  {selectedPig
                    ? `${selectedPig.status || "Pig"} • ${selectedPig.gender || ""} ${
                        selectedPig.location ? `• Pen ${selectedPig.location}` : ""
                      }`
                    : "Select a pig to auto-filter clinical stage and log medical records directly to animal history."}
                </p>
              </div>

              <div className="flex items-center gap-1.5 shrink-0">
                {selectedPig && (
                  <button
                    type="button"
                    onClick={() => handleSelectPig(null)}
                    className="p-1 rounded-lg text-red-500 hover:bg-red-50"
                    title="Clear selected pig"
                  >
                    <CloseIcon className="h-4 w-4" />
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => setShowPigSelector(true)}
                  className="px-2.5 py-1 text-xs font-bold rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 transition"
                >
                  {selectedPig ? "Change" : "Select Pig"}
                </button>
              </div>
            </div>

            {/* 2. Production Stage Filter Chips */}
            <div className="space-y-1.5">
              <label className="text-[11px] font-bold text-zinc-600 uppercase tracking-wider block">
                Production Stage Filter
              </label>
              <div className="flex items-center gap-1.5 overflow-x-auto pb-1 no-scrollbar">
                {PIG_STAGE_OPTIONS.map((st) => {
                  const isSelected = selectedStage === st.key;
                  return (
                    <button
                      key={st.key}
                      type="button"
                      onClick={() => setSelectedStage(st.key)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-bold whitespace-nowrap transition ${
                        isSelected
                          ? "bg-indigo-600 text-white shadow-xs"
                          : "bg-white border border-zinc-200 text-zinc-700 hover:bg-zinc-100"
                      }`}
                    >
                      {st.label}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* 3. Active Selected Symptoms Bar */}
            {selectedSymptoms.size > 0 && (
              <div className="p-3 bg-white border border-indigo-200/80 rounded-xl space-y-2 shadow-xs">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-indigo-950">
                    Selected Symptoms ({selectedSymptoms.size})
                  </span>
                  <button
                    type="button"
                    onClick={handleReset}
                    className="text-[11px] font-bold text-red-600 hover:text-red-700"
                  >
                    Clear All
                  </button>
                </div>
                <div className="flex flex-wrap gap-1.5">
                  {Array.from(selectedSymptoms).map((symKey) => {
                    const isHallmark = isPathognomonic(symKey);
                    return (
                      <span
                        key={symKey}
                        className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium cursor-pointer transition ${
                          isHallmark
                            ? "bg-amber-100 text-amber-900 border border-amber-300"
                            : "bg-indigo-100 text-indigo-900 border border-indigo-200"
                        }`}
                        onClick={() => toggleSymptom(symKey)}
                      >
                        {isHallmark && <span title="Hallmark / Pathognomonic Sign">⭐</span>}
                        <span>{renderSymptomName(symKey)}</span>
                        <CloseIcon className="h-3 w-3 ml-0.5 opacity-70 hover:opacity-100" />
                      </span>
                    );
                  })}
                </div>
              </div>
            )}

            {/* 4. Instant Symptom Search Bar */}
            <div className="relative">
              <SearchIcon className="h-4 w-4 absolute left-3 top-3 text-zinc-400" />
              <input
                type="text"
                placeholder="Search symptom (e.g. fever, blisters, coughing)..."
                value={symptomSearchQuery}
                onChange={(e) => setSymptomSearchQuery(e.target.value)}
                className="w-full pl-9 pr-8 py-2 text-xs bg-white border border-zinc-200 rounded-xl focus:outline-hidden focus:border-indigo-500 shadow-xs"
              />
              {symptomSearchQuery && (
                <button
                  type="button"
                  onClick={() => setSymptomSearchQuery("")}
                  className="absolute right-2.5 top-2.5 text-zinc-400 hover:text-zinc-600"
                >
                  <CloseIcon className="h-4 w-4" />
                </button>
              )}
            </div>

            {/* 5. Symptom Selection List: Search Mode or Group Accordion Mode */}
            {symptomSearchQuery.trim() ? (
              <div className="space-y-1.5">
                <p className="text-[11px] font-bold text-zinc-500">
                  Matching Symptoms ({matchingSymptoms.length}):
                </p>
                {matchingSymptoms.length === 0 ? (
                  <p className="p-3 text-center text-xs text-zinc-400 bg-white rounded-xl border border-dashed border-zinc-200">
                    No symptoms match &quot;{symptomSearchQuery}&quot;
                  </p>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {matchingSymptoms.map((symKey) => {
                      const isSelected = selectedSymptoms.has(symKey);
                      const isHallmark = isPathognomonic(symKey);
                      const subtitle = getSymptomSubtitle(symKey);
                      return (
                        <button
                          key={symKey}
                          type="button"
                          onClick={() => toggleSymptom(symKey)}
                          className={`p-2.5 rounded-xl border text-left transition flex items-start justify-between gap-2 ${
                            isSelected
                              ? "bg-indigo-50/80 border-indigo-300 text-indigo-950 font-bold"
                              : isHallmark
                              ? "bg-amber-50/40 border-amber-200 text-zinc-800 hover:bg-amber-50"
                              : "bg-white border-zinc-200/80 text-zinc-700 hover:bg-zinc-50"
                          }`}
                        >
                          <div className="min-w-0">
                            <div className="text-xs flex items-center gap-1.5">
                              <span>{renderSymptomName(symKey)}</span>
                              {isHallmark && (
                                <span className="text-[10px] px-1 py-0.2 rounded bg-amber-100 text-amber-900 border border-amber-300 font-bold flex items-center gap-0.5">
                                  ⭐ Hallmark
                                </span>
                              )}
                            </div>
                            {subtitle && (
                              <p className="text-[10px] text-zinc-400 font-normal mt-0.5">{subtitle}</p>
                            )}
                          </div>
                          {isSelected && <CheckIcon className="h-4 w-4 text-indigo-600 shrink-0 mt-0.5" />}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            ) : (
              /* Group Accordion Mode */
              <div className="space-y-2">
                {SYMPTOM_GROUPS.map((group) => {
                  const isCatOpen = openSymptomCategory === group.id;
                  const selectedInGroup = group.symptoms.filter((sk) => selectedSymptoms.has(sk)).length;

                  return (
                    <div key={group.id} className="border border-zinc-200/80 rounded-xl bg-white overflow-hidden shadow-2xs">
                      <button
                        type="button"
                        onClick={() => setOpenSymptomCategory(isCatOpen ? null : group.id)}
                        className="w-full p-3.5 flex items-center justify-between text-left hover:bg-zinc-50 transition-colors"
                      >
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold text-zinc-800 uppercase tracking-tight">
                            {renderGroupName(group.id)}
                          </span>
                          {selectedInGroup > 0 && (
                            <span className="px-1.5 py-0.2 rounded-full text-[10px] font-bold bg-indigo-600 text-white">
                              {selectedInGroup}
                            </span>
                          )}
                        </div>
                        <ChevronDownIcon
                          className={`h-4 w-4 text-zinc-400 transform transition-transform ${
                            isCatOpen ? "rotate-180 text-indigo-600" : ""
                          }`}
                        />
                      </button>

                      {isCatOpen && (
                        <div className="p-3 border-t border-zinc-100 grid grid-cols-1 sm:grid-cols-2 gap-2 bg-zinc-50/30">
                          {group.symptoms.map((symptomKey) => {
                            const isSelected = selectedSymptoms.has(symptomKey);
                            const isHallmark = isPathognomonic(symptomKey);
                            const subtitle = getSymptomSubtitle(symptomKey);

                            return (
                              <button
                                key={symptomKey}
                                type="button"
                                onClick={() => toggleSymptom(symptomKey)}
                                className={`p-2.5 rounded-xl border text-left transition flex items-start justify-between gap-2 ${
                                  isSelected
                                    ? "bg-indigo-50/80 border-indigo-300 text-indigo-950 font-bold"
                                    : isHallmark
                                    ? "bg-amber-50/30 border-amber-200 text-zinc-800 hover:bg-amber-50"
                                    : "bg-white border-zinc-200/80 text-zinc-700 hover:bg-zinc-50"
                                }`}
                              >
                                <div className="min-w-0">
                                  <div className="text-xs flex items-center gap-1.5">
                                    <span>{renderSymptomName(symptomKey)}</span>
                                    {isHallmark && (
                                      <span className="text-[10px] px-1 py-0.2 rounded bg-amber-100 text-amber-900 border border-amber-300 font-bold flex items-center gap-0.5">
                                        ⭐ Hallmark
                                      </span>
                                    )}
                                  </div>
                                  {subtitle && (
                                    <p className="text-[10px] text-zinc-400 font-normal mt-0.5">{subtitle}</p>
                                  )}
                                </div>
                                {isSelected && <CheckIcon className="h-4 w-4 text-indigo-600 shrink-0 mt-0.5" />}
                              </button>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}

            {/* 6. Action Buttons */}
            <div className="flex items-center gap-3 pt-2">
              <button
                type="button"
                onClick={handleAnalyze}
                disabled={selectedSymptoms.size === 0}
                className="flex-1 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold shadow-sm transition disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                <SearchIcon className="h-4 w-4" />
                <span>Analyze Symptoms ({selectedSymptoms.size})</span>
              </button>

              {(selectedSymptoms.size > 0 || hasDiagnosed) && (
                <button
                  type="button"
                  onClick={handleReset}
                  className="px-4 py-2.5 rounded-xl border border-zinc-300 text-xs font-bold text-zinc-700 hover:bg-zinc-100 transition"
                >
                  Reset
                </button>
              )}
            </div>

            {/* 7. Diagnostic Results */}
            {hasDiagnosed && (
              <div className="pt-3 border-t border-zinc-200 space-y-3">
                {/* Critical Biosecurity Emergency Banner */}
                {notifiableAlert && (
                  <div className="p-4 bg-red-50 border-2 border-red-500 rounded-xl space-y-2.5 shadow-sm text-red-950">
                    <div className="flex items-center gap-2 text-red-700 font-black text-sm">
                      <WarningIcon className="h-5 w-5 text-red-600 shrink-0" />
                      <span>{t("criticalBiosecurityAlert")}</span>
                    </div>
                    <p className="text-xs font-bold text-red-900">
                      {t("criticalBiosecurityDesc", {
                        disease: renderDiseaseName(notifiableAlert.disease.id),
                        percent: notifiableAlert.matchPercent,
                      })}
                    </p>
                    <div className="border-t border-red-200 pt-2 text-xs text-red-800 space-y-1">
                      <p className="font-black uppercase tracking-wider text-[11px] text-red-950">
                        {t("emergencyContainmentProtocolTitle")}
                      </p>
                      <div className="whitespace-pre-line text-[11px] font-medium leading-relaxed">
                        {t("emergencyContainmentProtocolSteps")}
                      </div>
                    </div>
                  </div>
                )}

                <h4 className="text-xs font-black uppercase tracking-wider text-indigo-950">
                  {t("clinicalDiagnosisMatches")} ({diagnosisResults.length})
                </h4>

                {diagnosisResults.length === 0 ? (
                  <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl text-center text-xs text-amber-800">
                    {t("noMatches") || "No matching diseases found for the selected combination and stage. Try adjusting your symptoms or stage filter."}
                  </div>
                ) : (
                  <div className="space-y-2.5">
                    {diagnosisResults.map((match) => {
                      const { disease, matches, matchPercent, totalSymptoms, matchedPathognomonic } = match;
                      const isExpanded = expandedResultId === disease.id;
                      const confidenceColor =
                        matchPercent >= 70
                          ? "bg-emerald-100 text-emerald-800 border-emerald-300"
                          : matchPercent >= 45
                          ? "bg-amber-100 text-amber-800 border-amber-300"
                          : "bg-orange-100 text-orange-800 border-orange-300";

                      return (
                        <div
                          key={disease.id}
                          className={`bg-white border rounded-xl overflow-hidden shadow-xs transition ${
                            match.isNotifiableAlert ? "border-red-400" : "border-zinc-200"
                          }`}
                        >
                          <button
                            type="button"
                            onClick={() => setExpandedResultId(isExpanded ? null : disease.id)}
                            className="w-full p-3.5 flex items-start justify-between text-left hover:bg-zinc-50 transition"
                          >
                            <div className="space-y-1 min-w-0 pr-2">
                              <div className="flex flex-wrap items-center gap-2">
                                <h5 className="font-bold text-xs text-zinc-900">{renderDiseaseName(disease.id)}</h5>
                                {matchedPathognomonic.length > 0 && (
                                  <span className="text-[9px] font-bold px-1.5 py-0.5 rounded bg-amber-100 text-amber-900 border border-amber-300 flex items-center gap-0.5">
                                    ⭐ {t("hallmarkBadge")}
                                  </span>
                                )}
                                <span className={`text-[9px] font-black px-1.5 py-0.5 rounded border ${getSeverityBadge(disease.severity)}`}>
                                  {disease.severity}
                                </span>
                              </div>
                              <p className="text-[11px] text-zinc-400 italic">
                                {renderDiseaseScientificName(disease.id)}
                              </p>
                              <div className="flex items-center gap-2 pt-0.5">
                                <span className={`text-[10px] font-bold px-2 py-0.5 rounded-md border ${confidenceColor}`}>
                                  {t("clinicalMatchPercent", { percent: matchPercent })}
                                </span>
                                <span className="text-[11px] text-zinc-500">
                                  {t("matchedSignsCount", { matched: matches, total: totalSymptoms })}
                                </span>
                              </div>
                            </div>
                            <ChevronDownIcon
                              className={`h-4 w-4 text-zinc-400 shrink-0 transform transition-transform ${
                                isExpanded ? "rotate-180 text-indigo-600" : ""
                              }`}
                            />
                          </button>

                          {/* Hallmark Notice Banner */}
                          {matchedPathognomonic.length > 0 && (
                            <div className="px-3.5 py-1.5 bg-amber-50/80 border-t border-b border-amber-200/60 text-[11px] text-amber-900 flex items-center gap-1.5 font-bold">
                              <span>⭐</span>
                              <span>
                                {(() => {
                                  const signs = matchedPathognomonic.map(renderSymptomName).join(", ");
                                  try {
                                    return t("hallmarkSignPresent", { sign: signs });
                                  } catch {
                                    return `Hallmark sign detected: ${signs}`;
                                  }
                                })()}
                              </span>
                            </div>
                          )}

                          {isExpanded && (
                            <div className="border-t border-zinc-100 p-3.5 bg-zinc-50/50 space-y-3 text-xs text-zinc-700">
                              <div>
                                <strong className="text-zinc-900">Description: </strong>
                                <span>{renderDiseaseDescription(disease.id)}</span>
                              </div>

                              <div>
                                <strong className="text-zinc-900">Observed Symptoms: </strong>
                                <div className="flex flex-wrap gap-1 mt-1">
                                  {disease.symptomKeys.map((sk) => {
                                    const matched = selectedSymptoms.has(sk);
                                    return (
                                      <span
                                        key={sk}
                                        className={`px-2 py-0.5 rounded text-[10px] ${
                                          matched
                                            ? "bg-indigo-100 text-indigo-800 font-bold border border-indigo-200"
                                            : "bg-zinc-100 text-zinc-600"
                                        }`}
                                      >
                                        {renderSymptomName(sk)}
                                      </span>
                                    );
                                  })}
                                </div>
                              </div>

                              {disease.symptomKeys.filter((sk) => !selectedSymptoms.has(sk)).length > 0 && (
                                <div>
                                  <strong className="text-amber-900">{t("otherSignsWatch")} </strong>
                                  <div className="flex flex-wrap gap-1 mt-1">
                                    {disease.symptomKeys
                                      .filter((sk) => !selectedSymptoms.has(sk))
                                      .map((sk) => (
                                        <span
                                          key={sk}
                                          className="px-2 py-0.5 rounded text-[10px] bg-amber-50 text-amber-800 border border-amber-200"
                                        >
                                          {renderSymptomName(sk)}
                                        </span>
                                      ))}
                                  </div>
                                </div>
                              )}

                              {renderDiseasePrevention(disease.id) && (
                                <div className="p-2.5 bg-indigo-50/60 rounded-lg border border-indigo-100 text-indigo-900">
                                  <strong className="block text-[11px] uppercase tracking-wider mb-0.5">
                                    Prevention & Treatment:
                                  </strong>
                                  <span>{renderDiseasePrevention(disease.id)}</span>
                                </div>
                              )}

                              {disease.isNotifiable && (
                                <div className="p-3 bg-red-50/80 rounded-xl border border-red-300 text-red-900 text-xs space-y-1">
                                  <div className="flex items-center gap-1.5 font-bold text-red-700">
                                    <WarningIcon className="h-4 w-4 shrink-0" />
                                    <span>{t("emergencyBiosecurityProtocolHeader")}</span>
                                  </div>
                                  <p className="leading-relaxed font-medium">
                                    {(() => {
                                      if (disease.biosecurityProtocolKey) {
                                        try {
                                          return t(disease.biosecurityProtocolKey);
                                        } catch {
                                          return null;
                                        }
                                      }
                                      return null;
                                    })() || "Immediate herd quarantine required. Contact livestock authorities before animal movement."}
                                  </p>
                                </div>
                              )}

                              {/* Parity Action Buttons: Log to Herd & Share with Vet */}
                              <div className="pt-2 border-t border-zinc-200 flex flex-wrap items-center gap-2">
                                <button
                                  type="button"
                                  onClick={() => handleLogToHerd(match)}
                                  className="flex-1 min-w-[130px] py-2 px-3 bg-white border border-indigo-300 text-indigo-700 hover:bg-indigo-50 font-bold rounded-lg text-xs flex items-center justify-center gap-1.5 transition shadow-2xs"
                                >
                                  <NoteAddIcon className="h-4 w-4 text-indigo-600" />
                                  <span>Log to Herd</span>
                                </button>

                                <button
                                  type="button"
                                  onClick={() => handleExportTelehealth(match)}
                                  className="flex-1 min-w-[130px] py-2 px-3 bg-indigo-600 text-white hover:bg-indigo-700 font-bold rounded-lg text-xs flex items-center justify-center gap-1.5 transition shadow-2xs"
                                >
                                  <ShareIcon className="h-4 w-4" />
                                  <span>Share with Vet</span>
                                </button>
                              </div>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}

                {/* Veterinary Advice Disclaimer */}
                <div className="p-3 bg-amber-50/70 border border-amber-200/70 rounded-xl flex items-start gap-2 text-[11px] text-amber-900">
                  <WarningIcon className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
                  <p>
                    <strong>Disclaimer:</strong> This diagnostic tool provides reference insights for educational purposes. Always consult a certified veterinarian before initiating pharmaceutical treatments or herd interventions.
                  </p>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Sub-option 2: Common Pig Diseases */}
      <div id="suboption-diseases" className="scroll-mt-20 bg-white border border-indigo-100 rounded-xl overflow-hidden shadow-xs">
        <button
          type="button"
          onClick={() => toggleSubOption("diseases")}
          className="w-full p-4 flex items-center justify-between text-left hover:bg-indigo-50/30 transition-colors"
        >
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 rounded-lg bg-indigo-50 text-indigo-600 border border-indigo-200/60 flex items-center justify-center shrink-0">
              <BookOpenIcon className="h-5 w-5" />
            </div>
            <div>
              <span className="text-sm font-bold text-zinc-900">{td("commonPigDiseases") || "Common Pig Diseases"}</span>
              <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-800">
                {DISEASES.length} Illnesses
              </span>
            </div>
          </div>
          <ChevronDownIcon
            className={`h-5 w-5 text-zinc-400 transform transition-transform duration-300 ${
              openSubOption === "diseases" ? "rotate-180 text-indigo-600" : ""
            }`}
          />
        </button>

        {openSubOption === "diseases" && (
          <div className="border-t border-indigo-100 p-4 bg-zinc-50/40 space-y-3 animate-fadeIn">
            <p className="text-xs text-zinc-500 font-medium">
              Browse common swine diseases grouped under the 6 clinical symptom categories.
            </p>

            <div className="space-y-2">
              {SYMPTOM_GROUPS.map((group) => {
                const isCatOpen = openCommonCategory === group.id;
                const catDiseases = DISEASES.filter((d) =>
                  d.symptomKeys.some((sKey) => group.symptoms.includes(sKey))
                ).sort((a, b) => renderDiseaseName(a.id).localeCompare(renderDiseaseName(b.id)));

                return (
                  <div key={group.id} className="border border-zinc-200/80 rounded-xl bg-white overflow-hidden">
                    <button
                      type="button"
                      onClick={() => setOpenCommonCategory(isCatOpen ? null : group.id)}
                      className="w-full p-3.5 flex items-center justify-between text-left hover:bg-zinc-50 transition-colors"
                    >
                      <div className="flex items-center gap-2.5">
                        <span className="text-xs font-bold text-zinc-800 uppercase tracking-tight">
                          {renderGroupName(group.id)}
                        </span>
                        <span className="px-1.5 py-0.5 rounded-full text-[10px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
                          {catDiseases.length}
                        </span>
                      </div>
                      <ChevronDownIcon
                        className={`h-4 w-4 text-zinc-400 transform transition-transform ${
                          isCatOpen ? "rotate-180 text-indigo-600" : ""
                        }`}
                      />
                    </button>

                    {isCatOpen && (
                      <div className="p-3 border-t border-zinc-100 space-y-2 bg-zinc-50/30">
                        {catDiseases.map((disease) => {
                          const isDiseaseExpanded = expandedCommonDiseaseId === disease.id;
                          return (
                            <div
                              key={disease.id}
                              className="bg-white border border-zinc-200/80 rounded-xl overflow-hidden"
                            >
                              <button
                                type="button"
                                onClick={() =>
                                  setExpandedCommonDiseaseId(isDiseaseExpanded ? null : disease.id)
                                }
                                className="w-full p-3 flex items-start justify-between text-left hover:bg-zinc-50 transition"
                              >
                                <div className="space-y-0.5 pr-2">
                                  <div className="flex items-center gap-2">
                                    <h5 className="font-bold text-xs text-zinc-900">
                                      {renderDiseaseName(disease.id)}
                                    </h5>
                                    <span
                                      className={`text-[9px] font-black px-1.5 py-0.2 rounded border ${getSeverityBadge(
                                        disease.severity
                                      )}`}
                                    >
                                      {disease.severity}
                                    </span>
                                  </div>
                                  <p className="text-[10px] text-zinc-400 italic">
                                    {renderDiseaseScientificName(disease.id)}
                                  </p>
                                </div>
                                <ChevronDownIcon
                                  className={`h-3.5 w-3.5 text-zinc-400 shrink-0 transform transition-transform ${
                                    isDiseaseExpanded ? "rotate-180 text-indigo-600" : ""
                                  }`}
                                />
                              </button>

                              {isDiseaseExpanded && (
                                <div className="border-t border-zinc-100 p-3 bg-zinc-50/50 space-y-2 text-xs text-zinc-700">
                                  <div>
                                    <strong className="text-zinc-900">Description: </strong>
                                    <span>{renderDiseaseDescription(disease.id)}</span>
                                  </div>
                                  <div>
                                    <strong className="text-zinc-900">Key Symptoms: </strong>
                                    <div className="flex flex-wrap gap-1 mt-1">
                                      {disease.symptomKeys.map((sk) => (
                                        <span
                                          key={sk}
                                          className="px-2 py-0.5 rounded text-[10px] bg-zinc-100 text-zinc-700"
                                        >
                                          {renderSymptomName(sk)}
                                        </span>
                                      ))}
                                    </div>
                                  </div>
                                  {renderDiseasePrevention(disease.id) && (
                                    <div className="p-2.5 bg-indigo-50/60 rounded-lg border border-indigo-100 text-indigo-900">
                                      <strong className="block text-[11px] uppercase tracking-wider mb-0.5">
                                        Prevention & Treatment:
                                      </strong>
                                      <span>{renderDiseasePrevention(disease.id)}</span>
                                    </div>
                                  )}
                                </div>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
    </PremiumWrapper>
  );
}
