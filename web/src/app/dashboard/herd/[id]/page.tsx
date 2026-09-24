"use client";

import React, { useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { doc, onSnapshot, updateDoc, collection, setDoc, deleteDoc, writeBatch } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import UserProfileDropdown from "@/components/UserProfileDropdown";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import HerdReport from "@/components/reports/HerdReport";
import { evaluatePerformance, calculateAgeMonths, calculateAgeDays, formatSwineAge } from "@/lib/swineGrowthDatabase";
import { ExportPdfIcon } from "@/components/icons/DashboardIcons";
import { useTranslations } from "next-intl";
import { Pig, HealthRecord } from "@/lib/types";
import RewardedPassModal from "@/components/ads/RewardedPassModal";

export default function PigProfilePage() {
  const t = useTranslations("PigProfile");
  const th = useTranslations("Herd");
  const tHr = useTranslations("HR");
  
  const translateGender = (gender: string) => {
    if (!gender) return "";
    const key = gender.toLowerCase();
    if (["male", "female"].includes(key)) return th(key);
    return gender;
  };

  const translatePurpose = (purpose: string) => {
    if (!purpose) return "";
    const key = purpose.toLowerCase();
    if (["breeder", "porker"].includes(key)) return th(key);
    return purpose;
  };

  const translateStatus = (status: string) => {
    if (!status) return "";
    const key = status.toLowerCase();
    const validKeys = ["piglet", "gilt", "sow", "boar", "barrow", "pregnant", "lactating"];
    if (validKeys.includes(key)) {
      return th(key);
    }
    return status;
  };

  const translateActivityType = (type: string) => {
    if (!type) return "";
    const key = type.toLowerCase().replace(/[\s\/]/g, "_");
    const actionKeys = [
      "vaccination", "deworming", "medication", "weight_check", "culling", 
      "teeth_clipping", "tail_docking", "iron_injection", "weaning", 
      "castration", "heat_detection", "breeding_mating", "pregnancy_check", 
      "farrowing", "custom"
    ];
    if (actionKeys.includes(key)) {
      return t(`actionTypes.${key}`);
    }
    const fallbacks: Record<string, string> = {
      "treatment": "treatment",
      "heat": "heat",
      "breeding": "breeding",
      "pregnancy": "pregnancy",
      "weight": "weight",
      "other": "other"
    };
    if (fallbacks[key]) {
      return t(`actionTypes.${fallbacks[key]}`);
    }
    return type;
  };
  const { user, userProfile, activeFarmUid, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();
  const params = useParams();
  const pigId = params?.id as string;
  const isPremium = Boolean(userProfile?.isPremium || userProfile?.isAdmin);

  const [pig, setPig] = useState<Pig | null>(null);
  const [healthRecords, setHealthRecords] = useState<HealthRecord[]>([]);
  const [dataLoading, setDataLoading] = useState(true);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showRecordModal, setShowRecordModal] = useState(false);
  const [showArchiveModal, setShowArchiveModal] = useState(false);
  const [archiveReason, setArchiveReason] = useState("Culled");
  const [customArchiveReason, setCustomArchiveReason] = useState("");
  const [showRewardedPassModal, setShowRewardedPassModal] = useState(false);
  const [isHistoryExpanded, setIsHistoryExpanded] = useState(false);

  // Edit fields
  const [breed, setBreed] = useState("");
  const [purpose, setPurpose] = useState("");
  const [location, setLocation] = useState("");
  const [status, setStatus] = useState("");
  const [weight, setWeight] = useState(0);
  const [notes, setNotes] = useState("");

  // Log Health Record fields
  const [recordDate, setRecordDate] = useState(new Date().toISOString().split("T")[0]);
  const [recordType, setRecordType] = useState("Medication");
  const [recordDesc, setRecordDesc] = useState("");
  const [recordWeight, setRecordWeight] = useState("");

  // Weight Warning Logic
  const weightRecords = healthRecords.filter(r => r.type === "Weight Check");
  const latestWeightUpdateMs = weightRecords.length > 0
    ? Math.max(...weightRecords.map(r => new Date(r.date).getTime()))
    : null;

  const ageDays = pig ? calculateAgeDays(pig.birthDate) : 0;
  const ageMonths = pig ? calculateAgeMonths(pig.birthDate) : 0;
  const performance = pig ? evaluatePerformance(pig.breed, ageDays, pig.weight) : "Blank";

  let performanceBadgeColor = "bg-zinc-200 text-zinc-600";
  if (performance === "Excellent") performanceBadgeColor = "bg-amber-100 text-amber-700";
  else if (performance === "Good") performanceBadgeColor = "bg-green-100 text-green-800";
  else if (performance === "Caution") performanceBadgeColor = "bg-yellow-100 text-yellow-800";
  else if (performance === "Poor") performanceBadgeColor = "bg-red-100 text-red-800";

  const showWeightUpdateWarning = (() => {
    if (performance === "Blank") return true;
    if (ageDays > 25) {
      if (latestWeightUpdateMs) {
        const diffMs = Date.now() - latestWeightUpdateMs;
        const daysSinceUpdate = diffMs / (1000 * 60 * 60 * 24);
        return daysSinceUpdate > 25;
      }
      return true;
    }
    return false;
  })();

  const [isArchived, setIsArchived] = useState(false);

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!activeFarmUid || !pigId) return;

    setDataLoading(true);
    let unsubArchPig: (() => void) | null = null;
    let unsubArchRecords: (() => void) | null = null;

    // 1. Listen to Pig Document
    const pigDocRef = doc(db, "users", activeFarmUid, "pigs", pigId);
    const unsubscribePig = onSnapshot(pigDocRef, (snapshot) => {
      if (snapshot.exists()) {
        const data = { id: snapshot.id, ...snapshot.data() } as Pig;
        setPig(data);
        setBreed(data.breed);
        setPurpose(data.purpose);
        setLocation(data.location || "");
        setStatus(data.status);
        setWeight(data.weight || 0);
        setNotes(data.notes || "");
        setIsArchived(false);
      } else {
        // Fallback to archived_pigs
        const archDocRef = doc(db, "users", activeFarmUid, "archived_pigs", pigId);
        unsubArchPig = onSnapshot(archDocRef, (archSnap) => {
          if (archSnap.exists()) {
            const data = { id: archSnap.id, ...archSnap.data() } as Pig;
            setPig(data);
            setBreed(data.breed);
            setPurpose(data.purpose);
            setLocation(data.location || "");
            setStatus(data.status);
            setWeight(data.weight || 0);
            setNotes(data.notes || "");
            setIsArchived(true);
          } else {
            setPig(null);
          }
        });
      }
    }, (error) => console.error("Error fetching pig details:", error));

    // 2. Listen to Pig Health Records Subcollection
    const recordsRef = collection(db, "users", activeFarmUid, "pigs", pigId, "health_records");
    const unsubscribeRecords = onSnapshot(recordsRef, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as HealthRecord));
      if (list.length > 0) {
        setHealthRecords(list.sort((a, b) => b.date.localeCompare(a.date)));
        setDataLoading(false);
      } else {
        // Fallback to archived_pigs health records
        const archRecordsRef = collection(db, "users", activeFarmUid, "archived_pigs", pigId, "health_records");
        unsubArchRecords = onSnapshot(archRecordsRef, (archSnap) => {
          const archList = archSnap.docs.map(doc => ({ id: doc.id, ...doc.data() } as HealthRecord));
          setHealthRecords(archList.sort((a, b) => b.date.localeCompare(a.date)));
          setDataLoading(false);
        }, () => setDataLoading(false));
      }
    }, (error) => {
      console.error("Error fetching health records:", error);
      setDataLoading(false);
    });

    return () => {
      unsubscribePig();
      unsubscribeRecords();
      if (unsubArchPig) unsubArchPig();
      if (unsubArchRecords) unsubArchRecords();
    };
  }, [activeFarmUid, pigId]);

  const handleUpdatePig = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !pigId || !pig) return;

    try {
      const targetColl = isArchived ? "archived_pigs" : "pigs";
      const pigDocRef = doc(db, "users", activeFarmUid, targetColl, pigId);
      const oldWeight = pig.weight || 0;

      await updateDoc(pigDocRef, {
        breed,
        purpose,
        location,
        status,
        weight,
        notes
      });

      // If weight was updated manually, add a history record for it to clear warnings
      if (weight !== oldWeight && weight > 0) {
        const recordsRef = collection(db, "users", activeFarmUid, targetColl, pigId, "health_records");
        const newRef = doc(recordsRef);
        await setDoc(newRef, {
          id: newRef.id,
          date: new Date().toISOString().split("T")[0],
          type: "Weight Check",
          description: t("manualWeightLog")
        }, { merge: true });
      }

      setShowEditModal(false);
    } catch (err) {
      console.error("Update failed:", err);
    }
  };

  const handleAddHealthRecord = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !pigId) return;

    try {
      const targetColl = isArchived ? "archived_pigs" : "pigs";
      const recordsRef = collection(db, "users", activeFarmUid, targetColl, pigId, "health_records");
      const newRef = doc(recordsRef);

      let finalDesc = recordDesc;
      if (recordType === "Weight Check" && recordWeight) {
        finalDesc = t("weightCheckDesc", { notes: recordDesc, weight: recordWeight });
      }

      await setDoc(newRef, {
        id: newRef.id,
        date: recordDate,
        type: recordType,
        description: finalDesc
      }, { merge: true });

      if (recordType === "Weight Check" && recordWeight) {
        const pigDocRef = doc(db, "users", activeFarmUid, targetColl, pigId);
        await updateDoc(pigDocRef, {
          weight: parseFloat(recordWeight) || 0
        });
      }

      setRecordDesc("");
      setRecordWeight("");
      setShowRecordModal(false);
    } catch (err) {
      console.error("Adding record failed:", err);
    }
  };

  const handleArchivePig = async () => {
    if (!activeFarmUid || !pigId || !pig) return;
    try {
      const finalReason = archiveReason === "Other" ? (customArchiveReason.trim() || "Other") : archiveReason;
      const todayStr = new Date().toISOString().split("T")[0];
      const archivedPig: Pig = {
        ...pig,
        status: `Archived (${finalReason})`,
        location: "Archived",
        notes: (pig.notes ? pig.notes + "\n" : "") + `Archived on: ${todayStr} Reason: ${finalReason}`
      };

      const batch = writeBatch(db);
      batch.set(doc(db, "users", activeFarmUid, "archived_pigs", pigId), archivedPig);
      batch.delete(doc(db, "users", activeFarmUid, "pigs", pigId));
      await batch.commit();

      setShowArchiveModal(false);
      router.push("/dashboard/herd");
    } catch (err) {
      console.error("Failed to archive pig:", err);
    }
  };

  const handleRestorePig = async () => {
    if (!activeFarmUid || !pigId || !pig) return;
    try {
      const todayStr = new Date().toISOString().split("T")[0];
      const restoredStatus = pig.purpose === "Breeder" 
        ? (pig.gender === "Male" ? "Boar" : "Sow")
        : "Grower";
      const restoredPig: Pig = {
        ...pig,
        status: restoredStatus,
        location: pig.location === "Archived" ? "" : pig.location,
        notes: (pig.notes ? pig.notes + "\n" : "") + `Restored to active herd on: ${todayStr}`
      };

      const batch = writeBatch(db);
      batch.set(doc(db, "users", activeFarmUid, "pigs", pigId), restoredPig);
      batch.delete(doc(db, "users", activeFarmUid, "archived_pigs", pigId));
      await batch.commit();

      router.push("/dashboard/herd");
    } catch (err) {
      console.error("Failed to restore pig:", err);
    }
  };

  const handleDeletePig = async () => {
    if (!activeFarmUid || !pigId || !confirm(t("confirmDelete"))) return;
    try {
      const targetColl = isArchived ? "archived_pigs" : "pigs";
      const pigDocRef = doc(db, "users", activeFarmUid, targetColl, pigId);
      await deleteDoc(pigDocRef);
      router.push("/dashboard/herd");
    } catch (err) {
      console.error("Failed to delete pig:", err);
    }
  };

  if (loading || !user || dataLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-white text-zinc-900">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  if (!pig) {
    return (
      <div className="flex h-screen flex-col items-center justify-center bg-white text-zinc-900 p-4">
        <p className="text-lg font-semibold text-zinc-500">{t("profileNotFound")}</p>
        <Link href="/dashboard/herd" className="mt-4 text-emerald-600 hover:underline">
          {t("backToHerd")}
        </Link>
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
        {!isMobile && <DesktopHeader showBack backPath="/dashboard/herd" />}

        <main className="flex-1 max-w-3xl w-full mx-auto px-4 py-8 space-y-6">
          {/* 1. Bio Section Header */}
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-black text-zinc-900 tracking-tight">Bio</h1>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setShowEditModal(true)}
                className="rounded-xl border border-zinc-200 bg-white px-3.5 py-1.5 text-xs font-bold text-zinc-700 hover:bg-zinc-50 transition-all shadow-sm"
              >
                {t("editDetails")}
              </button>

              {!isArchived ? (
                <button
                  onClick={() => setShowArchiveModal(true)}
                  className="rounded-xl border border-amber-300 bg-amber-50 px-3.5 py-1.5 text-xs font-bold text-amber-800 hover:bg-amber-100 transition-all shadow-sm flex items-center gap-1.5"
                  title="Archive Pig"
                >
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" />
                  </svg>
                  <span>Archive</span>
                </button>
              ) : (
                <button
                  onClick={handleRestorePig}
                  className="rounded-xl border border-emerald-300 bg-emerald-50 px-3.5 py-1.5 text-xs font-bold text-emerald-800 hover:bg-emerald-100 transition-all shadow-sm flex items-center gap-1.5"
                  title="Restore to Active Herd"
                >
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                  <span>Restore</span>
                </button>
              )}

              <button
                onClick={() => {
                  const isPremium = userProfile?.isPremium || userProfile?.isAdmin;
                  if (!isPremium) {
                    setShowRewardedPassModal(true);
                    return;
                  }
                  window.print();
                }}
                className={`p-2 rounded-xl border transition shadow-sm ${
                  userProfile?.isPremium || userProfile?.isAdmin
                    ? "border-zinc-200 bg-white text-zinc-600 hover:bg-zinc-50"
                    : "border-amber-200 bg-amber-50 text-amber-700 hover:bg-amber-100"
                }`}
                title={userProfile?.isPremium || userProfile?.isAdmin ? th("exportPdf") : th("exportPdfPremium")}
              >
                <ExportPdfIcon className="h-4 w-4" />
              </button>
            </div>
          </div>

          {showWeightUpdateWarning && (
            <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-start gap-3 animate-pulse shadow-sm">
              <svg className="h-5 w-5 text-amber-600 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <div className="space-y-1">
                <p className="text-xs font-bold text-amber-800">{t("weightUpdateRequired")}</p>
                <p className="text-[11px] text-amber-700 leading-relaxed">
                  {t("weightUpdateDesc")}
                </p>
              </div>
            </div>
          )}

          {pig.activeWithdrawalUntil && pig.activeWithdrawalUntil >= new Date().toISOString().split("T")[0] && (
            <div className="bg-rose-50 border-2 border-rose-300 rounded-xl p-4 flex items-start gap-3 shadow-sm">
              <span className="text-2xl">⚠️</span>
              <div className="space-y-1">
                <p className="text-xs font-black text-rose-800 uppercase tracking-wide">Withdrawal Active: Safe After {pig.activeWithdrawalUntil}</p>
                <p className="text-[11px] text-rose-700 leading-relaxed">
                  This animal received {pig.withdrawalMedication || "treatment"} and cannot be culled, slaughtered, or sold for meat until <strong>{pig.activeWithdrawalUntil}</strong> ({pig.withdrawalPeriodDays || 0} days withdrawal).
                </p>
              </div>
            </div>
          )}

          {/* Bio Details Card */}
          <div className="bg-white/70 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-4 relative overflow-hidden">
            <div className="absolute top-0 right-0 h-24 w-24 rounded-full bg-emerald-500/5 blur-xl pointer-events-none" />
            <div className="flex justify-between items-start gap-4 relative z-10">
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <p className="text-xs font-semibold text-zinc-400 font-mono uppercase">{t("statusLocation")}</p>
                  {performance !== "Blank" && (
                    <span className={`text-[10px] font-black px-2 py-0.5 rounded-full uppercase tracking-wider shadow-sm border border-black/5 ${performanceBadgeColor}`}>
                      {th(performance.toLowerCase())}
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-2 flex-wrap mt-1">
                  <h2 className="text-2xl font-black text-zinc-900 truncate">Tag: {pig.tagNumber}</h2>
                  {pig.parity !== undefined && pig.parity > 0 && (
                    <span className="text-xs font-black px-2 py-0.5 rounded-full uppercase tracking-wider bg-purple-100 text-purple-700 border border-purple-200">
                      Parity {pig.parity} (P{pig.parity})
                    </span>
                  )}
                </div>
                <p className="text-sm font-medium text-zinc-500 mt-0.5 truncate">{pig.breed}</p>
              </div>
            </div>

            <div className="divide-y divide-zinc-100 text-sm relative z-10">
              <div className="py-2 flex justify-between">
                <span className="text-zinc-500">{t("gender")}</span>
                <span className="font-semibold text-zinc-800">{translateGender(pig.gender)}</span>
              </div>
              <div className="py-2 flex justify-between">
                <span className="text-zinc-500">{t("purpose")}</span>
                <span className="font-semibold text-zinc-800">{translatePurpose(pig.purpose)}</span>
              </div>
              <div className="py-2 flex justify-between">
                <span className="text-zinc-500">{t("currentStatus")}</span>
                <span className="font-semibold text-emerald-700">{translateStatus(pig.status)}</span>
              </div>
              {pig.parity !== undefined && pig.parity > 0 && (
                <div className="py-2 flex justify-between">
                  <span className="text-zinc-500">Parity</span>
                  <span className="font-semibold text-purple-700 font-mono">P{pig.parity} ({pig.parity} {pig.parity === 1 ? "litter" : "litters"})</span>
                </div>
              )}
              {pig.activeWithdrawalUntil && (
                <div className="py-2 flex justify-between">
                  <span className="text-zinc-500">Withdrawal Safe Date</span>
                  <span className={`font-semibold font-mono ${pig.activeWithdrawalUntil >= new Date().toISOString().split("T")[0] ? "text-rose-600 font-bold" : "text-zinc-500 line-through"}`}>
                    {pig.activeWithdrawalUntil} {pig.activeWithdrawalUntil >= new Date().toISOString().split("T")[0] ? "(ACTIVE)" : "(EXPIRED)"}
                  </span>
                </div>
              )}
              <div className="py-2 flex justify-between">
                <span className="text-zinc-500">{t("weight")}</span>
                <span className="font-semibold text-zinc-800">{pig.weight} kg</span>
              </div>
              <div className="py-2 flex justify-between">
                <span className="text-zinc-500">{t("location")}</span>
                <span className="font-semibold text-zinc-800">{pig.location || t("unassigned")}</span>
              </div>
              <div className="py-2 flex justify-between">
                <span className="text-zinc-500">{t("birthDate")}</span>
                <span className="font-semibold text-zinc-800">{pig.birthDate}</span>
              </div>
              <div className="py-2 flex justify-between">
                <span className="text-zinc-500">{th("age")}</span>
                <span className="font-semibold text-zinc-800">
                  {formatSwineAge(pig.birthDate)}
                </span>
              </div>
              <div className="py-2 flex justify-between">
                <span className="text-zinc-500">{t("sowTag")}</span>
                <span className="font-semibold font-mono text-zinc-800">{pig.sowTag || "N/A"}</span>
              </div>
              <div className="py-2 flex justify-between">
                <span className="text-zinc-500">{t("boarTag")}</span>
                <span className="font-semibold font-mono text-zinc-800">{pig.boarTag || "N/A"}</span>
              </div>
            </div>

            {pig.notes && (
              <div className="bg-zinc-50/80 p-3.5 rounded-xl border border-zinc-150 text-xs text-zinc-600">
                <p className="font-bold text-zinc-500 uppercase text-[9px] mb-1">{t("notes")}</p>
                {pig.notes}
              </div>
            )}

            <button
              onClick={handleDeletePig}
              className="w-full text-center text-xs font-semibold text-rose-600 hover:text-rose-700 pt-3 border-t border-zinc-100 hover:underline relative z-10"
            >
              {t("deleteProfile")}
            </button>
          </div>

          {/* 2. Collapsible History Section (Closed by default) */}
          <div className="bg-white/70 backdrop-blur-md border border-zinc-200 rounded-2xl shadow-sm overflow-hidden transition-all duration-300">
            <div
              onClick={() => setIsHistoryExpanded(!isHistoryExpanded)}
              className="w-full flex items-center justify-between p-5 text-left hover:bg-zinc-50/70 transition-colors cursor-pointer select-none"
            >
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-200/60 flex items-center justify-center flex-shrink-0">
                  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-base font-bold text-zinc-900">{t("healthHistory")}</h3>
                    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-bold bg-zinc-100 text-zinc-700">
                      {healthRecords.length}
                    </span>
                  </div>
                  <p className="text-xs text-zinc-500 mt-0.5">Click to view health logs & medical history</p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    setShowRecordModal(true);
                  }}
                  className="rounded-xl bg-emerald-600 hover:bg-emerald-700 px-3.5 py-1.5 text-xs font-bold text-white shadow-sm transition-all active:scale-95"
                >
                  {t("logHealth")}
                </button>
                <div className="text-zinc-400">
                  <svg
                    className={`h-5 w-5 transform transition-transform duration-300 ${
                      isHistoryExpanded ? "rotate-180 text-emerald-600" : ""
                    }`}
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M19 9l-7 7-7-7" />
                  </svg>
                </div>
              </div>
            </div>

            {isHistoryExpanded && (
              <div className="border-t border-zinc-150 p-6 bg-zinc-50/40 space-y-4 animate-fadeIn">
                {healthRecords.length === 0 ? (
                  <p className="text-sm text-zinc-500 text-center py-8">{t("noHealthRecords")}</p>
                ) : (
                  <div className="relative border-l border-zinc-200 pl-4 ml-2 space-y-6">
                    {healthRecords.map((record) => (
                      <div key={record.id} className="relative">
                        {/* Timeline dot */}
                        <span className="absolute -left-[21px] top-1.5 h-3.5 w-3.5 rounded-full border-2 border-emerald-500 bg-white" />
                        <div>
                          <div className="flex justify-between items-start">
                            <p className="text-sm font-bold text-zinc-800">{translateActivityType(record.type)}</p>
                            <span className="text-xs text-zinc-400 font-mono">{record.date}</span>
                          </div>
                          {record.description && (
                            <p className="text-xs text-zinc-500 mt-1 whitespace-pre-line">{record.description}</p>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </main>
      </div>

      {isPremium && (
        <HerdReport
          pigs={[pig]}
          title={t("reportTitle", { tag: pig.tagNumber })}
          includeSummary={false}
        />
      )}

      {/* Edit Details Modal */}
      {showEditModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-md p-6 space-y-6 shadow-2xl">
            <h3 className="text-lg font-bold text-zinc-900">{t("editPigDetails")}</h3>
            <form onSubmit={handleUpdatePig} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("breed")}</label>
                <input
                  type="text"
                  required
                  value={breed}
                  onChange={(e) => setBreed(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("purpose")}</label>
                  <select
                    value={purpose}
                    onChange={(e) => setPurpose(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  >
                    <option value="Porker">{th("porker")}</option>
                    <option value="Breeder">{th("breeder")}</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("currentStatus")}</label>
                  <input
                    type="text"
                    required
                    value={status}
                    onChange={(e) => setStatus(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("weight")} (kg)</label>
                  <input
                    type="number"
                    step="any"
                    value={weight}
                    onChange={(e) => setWeight(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{th("locationPen")}</label>
                  <input
                    type="text"
                    value={location}
                    onChange={(e) => setLocation(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("notes")}</label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  rows={2}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
                <button
                  type="button"
                  onClick={() => setShowEditModal(false)}
                  className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100 transition"
                >
                  {t("cancel")}
                </button>
                <button
                  type="submit"
                  className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition"
                >
                  {t("saveChanges")}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Log Health Record Modal */}
      {showRecordModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-md p-6 space-y-6 shadow-2xl">
            <h3 className="text-lg font-bold text-zinc-900">{t("logHealthAction")}</h3>
            <form onSubmit={handleAddHealthRecord} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("actionDate")}</label>
                <input
                  type="date"
                  required
                  value={recordDate}
                  onChange={(e) => setRecordDate(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("actionType")}</label>
                <select
                  value={recordType}
                  onChange={(e) => setRecordType(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                >
                  <option value="Medication">{t("actionTypes.medication")}</option>
                  <option value="Vaccination">{t("actionTypes.vaccination")}</option>
                  <option value="Treatment">{t("actionTypes.treatment")}</option>
                  <option value="Heat Detection">{t("actionTypes.heat")}</option>
                  <option value="Breeding">{t("actionTypes.breeding")}</option>
                  <option value="Pregnancy Confirmation">{t("actionTypes.pregnancy")}</option>
                  <option value="Deworming">{t("actionTypes.deworming")}</option>
                  <option value="Weight Check">{t("actionTypes.weight")}</option>
                  <option value="Other">{t("actionTypes.other")}</option>
                </select>
              </div>

              {recordType === "Weight Check" && (
                <div className="animate-in fade-in slide-in-from-top-1 duration-200">
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("actualWeight")}</label>
                  <input
                    type="number"
                    step="any"
                    required
                    value={recordWeight}
                    onChange={(e) => setRecordWeight(e.target.value)}
                    placeholder={t("weightPlaceholder")}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("descriptionOutcome")}</label>
                <textarea
                  required
                  value={recordDesc}
                  onChange={(e) => setRecordDesc(e.target.value)}
                  placeholder={t("outcomePlaceholder")}
                  rows={3}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
                <button
                  type="button"
                  onClick={() => setShowRecordModal(false)}
                  className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100 transition"
                >
                  {t("cancel")}
                </button>
                <button
                  type="submit"
                  className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition"
                >
                  {t("logAction")}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Archive Pig Modal (Matching Android PigProfileScreen showArchiveDialog) */}
      {showArchiveModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-md p-6 space-y-5 shadow-2xl">
            <div className="space-y-1">
              <h3 className="text-lg font-bold text-zinc-900">Archive Pig #{pig.tagNumber}</h3>
              <p className="text-xs text-zinc-500">
                Select the reason for archiving this animal. Historical logs and ancestry links are preserved.
              </p>
            </div>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-zinc-600 mb-1.5">Reason for Archiving</label>
                <select
                  value={archiveReason}
                  onChange={(e) => setArchiveReason(e.target.value)}
                  className="w-full rounded-xl border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm"
                >
                  <option value="Culled">Culled</option>
                  <option value="Sold">Sold</option>
                  <option value="Died">Died</option>
                  <option value="Other">Other</option>
                </select>
              </div>

              {archiveReason === "Other" && (
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 mb-1.5">Specify Reason</label>
                  <input
                    type="text"
                    value={customArchiveReason}
                    onChange={(e) => setCustomArchiveReason(e.target.value)}
                    placeholder="e.g. Transferred to secondary farm"
                    className="w-full rounded-xl border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm"
                  />
                </div>
              )}
            </div>

            <div className="flex justify-end gap-3 pt-3 border-t border-zinc-150">
              <button
                type="button"
                onClick={() => setShowArchiveModal(false)}
                className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-600 hover:bg-zinc-100 transition"
              >
                {t("cancel")}
              </button>
              <button
                type="button"
                onClick={handleArchivePig}
                className="rounded-lg bg-amber-600 hover:bg-amber-700 px-4 py-2 text-xs font-bold text-white transition shadow-sm"
              >
                Archive Pig
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Rewarded Pass Modal for Non-Premium PDF Export */}
      <RewardedPassModal
        isOpen={showRewardedPassModal}
        onClose={() => setShowRewardedPassModal(false)}
        title={`Unlock ${pig.tagNumber} PDF Report`}
        description="Watch a short video ad to unlock comprehensive pig profile PDF reports and all premium herd tools for 3 hours!"
        onSuccess={() => {
          setTimeout(() => {
            window.print();
          }, 500);
        }}
      />
    </div>
  );
}
