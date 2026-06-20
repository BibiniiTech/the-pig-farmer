"use client";

import React, { useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { doc, onSnapshot, updateDoc, collection, setDoc, deleteDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import UserProfileDropdown from "@/components/UserProfileDropdown";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import HerdReport from "@/components/reports/HerdReport";
import { evaluatePerformance } from "@/lib/swineGrowthDatabase";
import { ExportPdfIcon } from "@/components/icons/DashboardIcons";
import { useTranslations } from "next-intl";

interface Pig {
  id: string;
  tagNumber: string;
  birthDate: string;
  breed: string;
  gender: string;
  weight: number;
  purpose: string;
  sowTag: string;
  boarTag: string;
  location: string;
  source: string;
  status: string;
  notes: string;
  castrated?: boolean;
  castrationDate?: string;
  teethClipped?: boolean;
  tailDocked?: boolean;
  ironInjections?: number;
  weaned?: boolean;
}

interface HealthRecord {
  id: string;
  date: string;
  type: string;
  description: string;
}

export default function PigProfilePage() {
  const t = useTranslations("PigProfile");
  const th = useTranslations("Herd");
  const { user, userProfile, activeFarmUid, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();
  const params = useParams();
  const pigId = params?.id as string;

  const [pig, setPig] = useState<Pig | null>(null);
  const [healthRecords, setHealthRecords] = useState<HealthRecord[]>([]);
  const [dataLoading, setDataLoading] = useState(true);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showRecordModal, setShowRecordModal] = useState(false);

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
  const calculateAgeDays = (birthDateStr: string) => {
    if (!birthDateStr) return 0;
    const birthDate = new Date(birthDateStr);
    if (isNaN(birthDate.getTime())) return 0;
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    const birth = new Date(birthDate);
    birth.setHours(0, 0, 0, 0);
    const diffTime = now.getTime() - birth.getTime();
    return Math.floor(diffTime / (1000 * 60 * 60 * 24));
  };

  const weightRecords = healthRecords.filter(r => r.type === "Weight Check");
  const latestWeightUpdateMs = weightRecords.length > 0
    ? Math.max(...weightRecords.map(r => new Date(r.date).getTime()))
    : null;

  const ageDays = pig ? calculateAgeDays(pig.birthDate) : 0;
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

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!activeFarmUid || !pigId) return;

    setDataLoading(true);

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
      } else {
        setPig(null);
      }
    }, (error) => console.error("Error fetching pig details:", error));

    // 2. Listen to Pig Health Records Subcollection
    const recordsRef = collection(db, "users", activeFarmUid, "pigs", pigId, "health_records");
    const unsubscribeRecords = onSnapshot(recordsRef, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as HealthRecord));
      setHealthRecords(list.sort((a, b) => b.date.localeCompare(a.date)));
      setDataLoading(false);
    }, (error) => {
      console.error("Error fetching health records:", error);
      setDataLoading(false);
    });

    return () => {
      unsubscribePig();
      unsubscribeRecords();
    };
  }, [activeFarmUid, pigId]);

  const handleUpdatePig = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !pigId || !pig) return;

    try {
      const pigDocRef = doc(db, "users", activeFarmUid, "pigs", pigId);
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
        const recordsRef = collection(db, "users", activeFarmUid, "pigs", pigId, "health_records");
        const newRef = doc(recordsRef);
        await setDoc(newRef, {
          id: newRef.id,
          date: new Date().toISOString().split("T")[0],
          type: "Weight Check",
          description: t("manualWeightLog")
        });
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
      const recordsRef = collection(db, "users", activeFarmUid, "pigs", pigId, "health_records");
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
      });

      if (recordType === "Weight Check" && recordWeight) {
        const pigDocRef = doc(db, "users", activeFarmUid, "pigs", pigId);
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

  const handleDeletePig = async () => {
    if (!activeFarmUid || !pigId || !confirm(t("confirmDelete"))) return;
    try {
      const pigDocRef = doc(db, "users", activeFarmUid, "pigs", pigId);
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

        <main className="flex-1 max-w-4xl w-full mx-auto px-4 py-8 grid grid-cols-1 md:grid-cols-12 gap-8">
          {/* Left Side: Summary Card */}
          <div className="md:col-span-5 space-y-6">
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

            <div className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-4 relative overflow-hidden">
              <div className="absolute top-0 right-0 h-16 w-16 rounded-full bg-emerald-500/5 blur-lg pointer-events-none" />
              <div className="flex justify-between items-start gap-4 relative z-10">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="text-xs font-semibold text-zinc-400 font-mono uppercase">{t("statusLocation")}</p>
                    {performance !== "Blank" && (
                      <span className={`text-[10px] font-black px-2 py-0.5 rounded-full uppercase tracking-wider shadow-sm border border-black/5 ${performanceBadgeColor}`}>
                        {performance}
                      </span>
                    )}
                  </div>
                  <h2 className="text-xl font-bold text-zinc-900 mt-1 truncate">{pig.tagNumber}</h2>
                  <p className="text-sm text-zinc-500 mt-0.5 truncate">{pig.breed}</p>
                </div>
                <button
                  onClick={() => {
                    const isPremium = userProfile?.isPremium || userProfile?.isAdmin;
                    if (!isPremium) {
                      alert("Exporting PDF reports is a Premium feature. Upgrade to unlock!");
                      router.push("/dashboard/billing");
                      return;
                    }
                    window.print();
                  }}
                  className={`flex-shrink-0 p-2 rounded-lg border transition shadow-sm ${
                    userProfile?.isPremium || userProfile?.isAdmin
                      ? "border-zinc-200 bg-zinc-50/50 text-zinc-500 hover:bg-zinc-100"
                      : "border-amber-200 bg-amber-50 text-amber-700 hover:bg-amber-100"
                  }`}
                  title={userProfile?.isPremium || userProfile?.isAdmin ? th("exportPdf") : th("exportPdfPremium")}
                >
                  <ExportPdfIcon className="h-4 w-4" />
                </button>
              </div>

              <div className="divide-y divide-zinc-100 text-sm relative z-10">
                <div className="py-1.5 flex justify-between">
                  <span className="text-zinc-500">{t("gender")}</span>
                  <span className="font-semibold">{pig.gender}</span>
                </div>
                <div className="py-1.5 flex justify-between">
                  <span className="text-zinc-500">{t("purpose")}</span>
                  <span className="font-semibold">{pig.purpose}</span>
                </div>
                <div className="py-1.5 flex justify-between">
                  <span className="text-zinc-500">{t("currentStatus")}</span>
                  <span className="font-semibold text-emerald-700">{pig.status}</span>
                </div>
                <div className="py-1.5 flex justify-between">
                  <span className="text-zinc-500">{t("weight")}</span>
                  <span className="font-semibold">{pig.weight} kg</span>
                </div>
                <div className="py-1.5 flex justify-between">
                  <span className="text-zinc-500">{t("location")}</span>
                  <span className="font-semibold">{pig.location || t("unassigned")}</span>
                </div>
                <div className="py-1.5 flex justify-between">
                  <span className="text-zinc-500">{t("birthDate")}</span>
                  <span className="font-semibold">{pig.birthDate}</span>
                </div>
                <div className="py-1.5 flex justify-between">
                  <span className="text-zinc-500">{t("sowTag")}</span>
                  <span className="font-semibold font-mono">{pig.sowTag || "N/A"}</span>
                </div>
                <div className="py-1.5 flex justify-between">
                  <span className="text-zinc-500">{t("boarTag")}</span>
                  <span className="font-semibold font-mono">{pig.boarTag || "N/A"}</span>
                </div>
              </div>

              {pig.notes && (
                <div className="bg-zinc-50/70 p-3 rounded-lg border border-zinc-150 text-xs text-zinc-600">
                  <p className="font-bold text-zinc-500 uppercase text-[9px] mb-1">{t("notes")}</p>
                  {pig.notes}
                </div>
              )}

              <button
                onClick={handleDeletePig}
                className="w-full text-center text-xs font-semibold text-rose-600 hover:text-rose-700 pt-2 border-t border-zinc-100 hover:underline relative z-10"
              >
                {t("deleteProfile")}
              </button>

              <button
                onClick={() => setShowEditModal(true)}
                className="w-full mt-2 rounded-lg border border-zinc-200 bg-zinc-50/50 px-4 py-2.5 text-xs font-bold text-zinc-650 hover:bg-zinc-100 hover:text-zinc-950 transition-all active:scale-95 relative z-10"
              >
                {t("editDetails")}
              </button>
            </div>
          </div>

          {/* Right Side: Health Timeline */}
          <div className="md:col-span-7 space-y-6">
            <div className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-bold text-zinc-900">{t("healthHistory")}</h3>
                <button
                  onClick={() => setShowRecordModal(true)}
                  className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white shadow-lg shadow-emerald-600/10 transition-all active:scale-95"
                >
                  {t("logHealth")}
                </button>
              </div>

              {healthRecords.length === 0 ? (
                <p className="text-sm text-zinc-500 text-center py-12">{t("noHealthRecords")}</p>
              ) : (
                <div className="relative border-l border-zinc-200 pl-4 ml-2 space-y-6">
                  {healthRecords.map((record) => (
                    <div key={record.id} className="relative">
                      {/* Timeline dot */}
                      <span className="absolute -left-[21px] top-1.5 h-3.5 w-3.5 rounded-full border-2 border-emerald-500 bg-white" />
                      <div>
                        <div className="flex justify-between items-start">
                          <p className="text-sm font-bold text-zinc-800">{record.type}</p>
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
          </div>
        </main>
      </div>

      <HerdReport
        pigs={[pig]}
        title={t("reportTitle", { tag: pig.tagNumber })}
        includeSummary={false}
      />

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
    </div>
  );
}
