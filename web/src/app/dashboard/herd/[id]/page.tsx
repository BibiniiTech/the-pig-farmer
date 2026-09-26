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
import { parseAnyDateToMs } from "@/lib/notificationUtils";

export default function PigProfilePage() {
  const t = useTranslations("PigProfile");
  const th = useTranslations("Herd");
  const tHr = useTranslations("HR");
  const tCommon = useTranslations("Common");
  
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
  const [allPigs, setAllPigs] = useState<Pig[]>([]);
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
  const [recordBoarTag, setRecordBoarTag] = useState("");
  const [recordPregnancyConfirmed, setRecordPregnancyConfirmed] = useState(true);
  const [recordWithdrawalDays, setRecordWithdrawalDays] = useState("0");

  const addDays = (dateStr: string, days: number) => {
    const result = new Date(dateStr);
    result.setDate(result.getDate() + days);
    return result.toISOString().split("T")[0];
  };

  const isFutureDate = (dateStr: string) => {
    const today = new Date().toISOString().split("T")[0];
    return dateStr > today;
  };

  // Weight Warning Logic matching Android PigProfileScreen.kt and DashboardViewModel.kt
  const weightRecords = healthRecords.filter(r => r.type === "Weight Check");
  const weightRecordDates = weightRecords
    .map(r => parseAnyDateToMs(r.date))
    .filter((ms): ms is number => ms !== null && !isNaN(ms));

  const pigLastWeightMs = parseAnyDateToMs(pig?.lastWeightDate);
  if (pigLastWeightMs !== null && !isNaN(pigLastWeightMs)) {
    weightRecordDates.push(pigLastWeightMs);
  }

  const latestWeightUpdateMs = weightRecordDates.length > 0
    ? Math.max(...weightRecordDates)
    : null;

  const ageDays = pig ? calculateAgeDays(pig.birthDate) : 0;
  const ageMonths = pig ? calculateAgeMonths(pig.birthDate) : 0;
  const performance = pig ? evaluatePerformance(pig.breed, ageDays, pig.weight) : "Blank";

  let performanceBadgeColor = "bg-[#E0E0E0] text-[#616161]";
  if (performance === "Excellent") performanceBadgeColor = "bg-[#FEF3C7] text-[#B45309]";
  else if (performance === "Good") performanceBadgeColor = "bg-[#C8E6C9] text-[#2E7D32]";
  else if (performance === "Caution") performanceBadgeColor = "bg-[#FFF9C4] text-[#F57F17]";
  else if (performance === "Poor") performanceBadgeColor = "bg-[#FFCDD2] text-[#C62828]";

  const showWeightUpdateWarning = (() => {
    if (!pig || pig.status?.startsWith("Archived")) return false;
    if (performance === "Blank" || (pig.weight <= 0 && ageDays > 25)) return true;
    if (ageDays > 25) {
      if (latestWeightUpdateMs !== null) {
        const diffMs = Date.now() - latestWeightUpdateMs;
        const daysSinceUpdate = diffMs / (1000 * 60 * 60 * 24);
        return daysSinceUpdate > 25;
      }
      // If pig already has a weight recorded (> 0) and no explicit date, do not falsely flag
      return false;
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

    const allPigsQuery = collection(db, "users", activeFarmUid, "pigs");
    const unsubscribeAllPigs = onSnapshot(allPigsQuery, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Pig));
      setAllPigs(list);
    }, () => {});

    return () => {
      unsubscribePig();
      unsubscribeRecords();
      unsubscribeAllPigs();
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

      const todayStr = new Date().toISOString().split("T")[0];
      const updateData: any = {
        breed,
        purpose,
        location,
        status,
        weight,
        notes
      };
      if (weight !== oldWeight && weight > 0) {
        updateData.lastWeightDate = todayStr;
      }

      await updateDoc(pigDocRef, updateData);

      // If weight was updated manually, add a history record for it to clear warnings
      if (weight !== oldWeight && weight > 0) {
        const recordsRef = collection(db, "users", activeFarmUid, targetColl, pigId, "health_records");
        const newRef = doc(recordsRef);
        await setDoc(newRef, {
          id: newRef.id,
          date: todayStr,
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
    if (!activeFarmUid || !pigId || !pig) return;

    try {
      const isFuture = isFutureDate(recordDate);
      const targetColl = isArchived ? "archived_pigs" : "pigs";
      const recordsRef = collection(db, "users", activeFarmUid, targetColl, pigId, "health_records");
      const newRef = doc(recordsRef);
      const pigTag = pig.tagNumber || pigId;

      let finalDesc = recordDesc;
      if (recordType === "Weight Check" && recordWeight) {
        finalDesc = t("weightCheckDesc", { notes: recordDesc, weight: recordWeight });
      }

      if (isFuture) {
        const taskRef = doc(collection(db, "users", activeFarmUid, "tasks"));
        await setDoc(taskRef, {
          id: taskRef.id,
          name: `${recordType}: Pig ${pigTag}`,
          date: recordDate,
          notes: finalDesc || recordDesc,
          pigIds: [pigId],
          completed: false
        });

        await setDoc(newRef, {
          id: newRef.id,
          date: recordDate,
          type: recordType,
          description: finalDesc,
          taskId: taskRef.id
        }, { merge: true });
      } else {
        const batch = writeBatch(db);
        const pigRef = doc(db, "users", activeFarmUid, targetColl, pigId);

        // Specialized Activity Logic matching Android HerdRepository.kt
        if (recordType === "Heat Detection") {
          const tHeatRef = doc(collection(db, "users", activeFarmUid, "tasks"));
          const heatDate = addDays(recordDate, 21);
          batch.set(tHeatRef, {
            id: tHeatRef.id,
            name: `Heat Detection: Pig ${pigTag}`,
            date: heatDate,
            notes: `Auto-created 21 days after heat detection on ${recordDate}`,
            pigIds: [pigId],
            completed: false
          });
        } else if (recordType === "Breeding" || recordType === "Breeding/Mating") {
          const day110 = addDays(recordDate, 110);
          const day114 = addDays(recordDate, 114);
          const updates: any = {
            lastBreedingDate: recordDate,
            purpose: "Breeder",
            status: "Pregnant",
            expectedFarrowingDate: day114,
            farrowingPenMoveDate: day110
          };
          if (recordBoarTag.trim()) {
            updates.lastBoarTag = recordBoarTag.trim();
          }
          batch.update(pigRef, updates);

          // 1. Day 21 Return-to-heat
          const tHeat = doc(collection(db, "users", activeFarmUid, "tasks"));
          batch.set(tHeat, {
            id: tHeat.id,
            name: `Check Return-to-Heat / Estrus: Pig ${pigTag}`,
            date: addDays(recordDate, 21),
            notes: `Check if sow returns to heat 18-24 days post-mating on ${recordDate}`,
            pigIds: [pigId],
            completed: false
          });

          // 2. Day 110 Move to crate
          const tCrate = doc(collection(db, "users", activeFarmUid, "tasks"));
          batch.set(tCrate, {
            id: tCrate.id,
            name: `Move to Farrowing Crate: Pig ${pigTag}`,
            date: day110,
            notes: `Move sow to sanitized farrowing pen & wash/deworm 4-5 days before due date`,
            pigIds: [pigId],
            completed: false
          });

          // 3. Day 114 Expected Farrowing Due Date
          const tFarrow = doc(collection(db, "users", activeFarmUid, "tasks"));
          batch.set(tFarrow, {
            id: tFarrow.id,
            name: `Farrowing: Pig ${pigTag}`,
            date: day114,
            notes: `Scheduled 114 days after mating on ${recordDate}`,
            pigIds: [pigId],
            completed: false
          });
        } else if (recordType === "Confirm Pregnancy" || recordType === "Pregnancy Confirmation") {
          if (recordPregnancyConfirmed) {
            const sowBreedingDate = pig.lastBreedingDate || recordDate;
            const day110 = addDays(sowBreedingDate, 110);
            const day114 = addDays(sowBreedingDate, 114);
            batch.update(pigRef, {
              status: "Pregnant",
              purpose: "Breeder",
              expectedFarrowingDate: day114,
              farrowingPenMoveDate: day110
            });

            const tCrate = doc(collection(db, "users", activeFarmUid, "tasks"));
            batch.set(tCrate, {
              id: tCrate.id,
              name: `Move to Farrowing Crate: Pig ${pigTag}`,
              date: day110,
              notes: `Move sow to sanitized farrowing pen & wash/deworm 4-5 days before due date`,
              pigIds: [pigId],
              completed: false
            });

            const tFarrow = doc(collection(db, "users", activeFarmUid, "tasks"));
            batch.set(tFarrow, {
              id: tFarrow.id,
              name: `Farrowing: Pig ${pigTag}`,
              date: day114,
              notes: `Scheduled 114 days after mating on ${sowBreedingDate}`,
              pigIds: [pigId],
              completed: false
            });
            finalDesc = `${recordDesc}\nPregnancy Confirmed. Due on ${day114}`.trim();
          } else {
            batch.update(pigRef, {
              status: "Sow",
              lastBreedingDate: "",
              expectedFarrowingDate: "",
              farrowingPenMoveDate: ""
            });
            const tRemate = doc(collection(db, "users", activeFarmUid, "tasks"));
            const remateDate = addDays(recordDate, 3);
            batch.set(tRemate, {
              id: tRemate.id,
              name: `Re-mate / Heat Check: Pig ${pigTag}`,
              date: remateDate,
              notes: `Conception check failed on ${recordDate}. Monitor for next estrus cycle and re-mate.`,
              pigIds: [pigId],
              completed: false
            });
            finalDesc = `${recordDesc}\nPregnancy check failed. Reset to open Sow.`.trim();
          }
        } else if (recordType === "Farrowing") {
          const currentParity = pig.parity || 0;
          batch.update(pigRef, {
            status: "Lactating",
            hasFarrowed: true,
            weaned: false,
            isWeaned: false,
            purpose: "Breeder",
            parity: currentParity + 1,
            expectedFarrowingDate: "",
            farrowingPenMoveDate: ""
          });

          const weanTask = doc(collection(db, "users", activeFarmUid, "tasks"));
          batch.set(weanTask, {
            id: weanTask.id,
            name: `Weaning: Pig ${pigTag}`,
            date: addDays(recordDate, 28),
            notes: `Weaning due 28 days after farrowing on ${recordDate}`,
            pigIds: [pigId],
            completed: false
          });

          const ironTask = doc(collection(db, "users", activeFarmUid, "tasks"));
          batch.set(ironTask, {
            id: ironTask.id,
            name: `Iron Injection: Pig ${pigTag}`,
            date: addDays(recordDate, 3),
            notes: `Administer 1st iron injection to newborn piglets (3 days post-farrowing)`,
            pigIds: [pigId],
            completed: false
          });

          const creepTask = doc(collection(db, "users", activeFarmUid, "tasks"));
          batch.set(creepTask, {
            id: creepTask.id,
            name: `Creep Feed Introduction: Pig ${pigTag}`,
            date: addDays(recordDate, 7),
            notes: `Introduce high-protein creep feed to piglets at 7-10 days of age`,
            pigIds: [pigId],
            completed: false
          });
        } else if (recordType === "Weaning") {
          if (pig.status === "Lactating" || pig.status === "Nursing" || pig.status === "Sow") {
            batch.update(pigRef, { status: "Sow" });
            const heatTask = doc(collection(db, "users", activeFarmUid, "tasks"));
            batch.set(heatTask, {
              id: heatTask.id,
              name: `Post-Weaning Heat Check: Pig ${pigTag}`,
              date: addDays(recordDate, 5),
              notes: `Wean-to-Service Interval surveillance (4-7 days expected post-weaning)`,
              pigIds: [pigId],
              completed: false
            });
          } else {
            batch.update(pigRef, { status: "Starter", weaned: true, isWeaned: true });
          }
        } else if (recordType === "Castration" && pig.gender?.toLowerCase() === "male") {
          batch.update(pigRef, { castrated: true, isCastrated: true, castrationDate: recordDate });
        } else if (recordType === "Teeth Clipping") {
          batch.update(pigRef, { teethClipped: true, isTeethClipped: true });
        } else if (recordType === "Tail Docking") {
          batch.update(pigRef, { tailDocked: true, isTailDocked: true });
        } else if (recordType === "Iron Injection") {
          const currentCount = pig.ironInjections || 0;
          batch.update(pigRef, { ironInjections: currentCount + 1 });
        } else if (["Medication", "Deworming", "Vaccination", "Treatment"].includes(recordType)) {
          const wDays = parseInt(recordWithdrawalDays, 10) || 0;
          if (wDays > 0) {
            const safeDate = addDays(recordDate, wDays);
            batch.update(pigRef, {
              activeWithdrawalUntil: safeDate,
              withdrawalMedication: recordType
            });
            const wTask = doc(collection(db, "users", activeFarmUid, "tasks"));
            batch.set(wTask, {
              id: wTask.id,
              name: `Meat Withdrawal Cleared: Pig ${pigTag}`,
              date: safeDate,
              notes: `Safe for slaughter and meat sale. Medication/Treatment: ${recordType}`,
              pigIds: [pigId],
              completed: false
            });
            finalDesc = `${finalDesc}\nDrug Withdrawal: ${wDays} days. Safe date: ${safeDate}`.trim();
          }
        } else if (recordType === "Weight Check" && recordWeight) {
          batch.update(pigRef, {
            weight: parseFloat(recordWeight) || 0,
            lastWeightDate: recordDate
          });
        }

        batch.set(newRef, {
          id: newRef.id,
          date: recordDate,
          type: recordType,
          description: finalDesc
        }, { merge: true });

        await batch.commit();
      }

      setRecordDesc("");
      setRecordWeight("");
      setRecordBoarTag("");
      setRecordPregnancyConfirmed(true);
      setRecordWithdrawalDays("0");
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
      <div className="flex h-screen items-center justify-center bg-[#F8FAF9] dark:bg-[#121212] text-zinc-900 dark:text-zinc-100">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  if (!pig) {
    return (
      <div className="flex h-screen flex-col items-center justify-center bg-[#F8FAF9] dark:bg-[#121212] text-zinc-900 dark:text-zinc-100 p-4">
        <p className="text-lg font-semibold text-zinc-500">{t("profileNotFound")}</p>
        <Link href="/dashboard/herd" className="mt-4 text-emerald-600 hover:underline">
          {t("backToHerd")}
        </Link>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen bg-[#F8FAF9] dark:bg-[#121212] text-zinc-900 dark:text-zinc-100 flex flex-col font-sans overflow-x-hidden">
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
          <DesktopHeader
            showBack
            backPath="/dashboard/herd"
            label={`${t("tag") || "TAG"}: ${pig.tagNumber}`}
            labelColor="text-[#2E7D32] dark:text-[#81C784]"
          />
        )}

        <main className="flex-1 max-w-3xl w-full mx-auto px-4 py-8 space-y-5">
          {/* Top Bar with Back Button & Actions */}
          <div className="flex items-center justify-between gap-3 flex-wrap">
            <div className="flex items-center gap-3">
              <Link
                href="/dashboard/herd"
                className="inline-flex items-center gap-2 px-3 py-2 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-xl transition-colors text-zinc-700 dark:text-zinc-300 border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 shadow-xs font-bold text-xs shrink-0"
                aria-label="Back to herd list"
                title="Back to herd list"
              >
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
                </svg>
                <span>{tCommon("back") || "Back"}</span>
              </Link>
              <h1 className="text-xl sm:text-2xl font-black text-[#2E7D32] dark:text-[#81C784] tracking-tight">
                {pig.tagNumber}
              </h1>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => setShowEditModal(true)}
                className="rounded-xl border border-zinc-200 bg-white px-3.5 py-1.5 text-xs font-bold text-zinc-700 hover:bg-zinc-50 transition-all shadow-xs"
              >
                {t("editDetails")}
              </button>

              {!isArchived ? (
                <button
                  onClick={() => setShowArchiveModal(true)}
                  className="rounded-xl border border-amber-300 bg-amber-50 px-3.5 py-1.5 text-xs font-bold text-amber-800 hover:bg-amber-100 transition-all shadow-xs flex items-center gap-1.5"
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
                  className="rounded-xl border border-emerald-300 bg-emerald-50 px-3.5 py-1.5 text-xs font-bold text-emerald-800 hover:bg-emerald-100 transition-all shadow-xs flex items-center gap-1.5"
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
                className={`p-2 rounded-xl border transition shadow-xs ${
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
            <div className="bg-[#FFF9C4] border border-[#FFF59D] rounded-xl p-3.5 sm:p-4 flex items-center gap-3 text-[#F57F17] shadow-xs">
              <svg className="h-5 w-5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <div className="space-y-0.5">
                <p className="text-xs sm:text-sm font-bold">{t("weightUpdateRequired")}</p>
                <p className="text-[11px] sm:text-xs opacity-90 leading-relaxed">
                  {t("weightUpdateDesc")}
                </p>
              </div>
            </div>
          )}

          {pig.activeWithdrawalUntil && pig.activeWithdrawalUntil >= new Date().toISOString().split("T")[0] && (
            <div className="bg-rose-50 border-2 border-rose-300 rounded-xl p-4 flex items-start gap-3 shadow-xs">
              <span className="text-2xl">⚠️</span>
              <div className="space-y-1">
                <p className="text-xs font-black text-rose-800 uppercase tracking-wide">Withdrawal Active: Safe After {pig.activeWithdrawalUntil}</p>
                <p className="text-[11px] text-rose-700 leading-relaxed">
                  This animal received {pig.withdrawalMedication || "treatment"} and cannot be culled, slaughtered, or sold for meat until <strong>{pig.activeWithdrawalUntil}</strong> ({pig.withdrawalPeriodDays || 0} days withdrawal).
                </p>
              </div>
            </div>
          )}

          {/* Bio Details Card (Matching Android PigInfoCard) */}
          <div className="bg-[#E8F5E9] border border-[#C8E6C9] rounded-2xl p-4 sm:p-5 shadow-xs space-y-3">
            <div className="flex justify-between items-center gap-3">
              <h2 className="text-base sm:text-lg font-bold text-[#1B5E20] truncate">
                {t("tag") || "Tag"}: {pig.tagNumber}
              </h2>
              <span className={`text-xs font-bold px-2.5 py-1 rounded-md shrink-0 ${performanceBadgeColor}`}>
                {performance ? (th(performance.toLowerCase()) || performance) : ""}
              </span>
            </div>

            <div className="border-t border-[#C8E6C9] pt-2 space-y-2">
              {/* DOB */}
              <div className="flex items-center justify-between text-xs sm:text-sm">
                <div className="flex items-center gap-2 text-[#2E7D32] font-semibold">
                  <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                    <line x1="16" y1="2" x2="16" y2="6"/>
                    <line x1="8" y1="2" x2="8" y2="6"/>
                    <line x1="3" y1="10" x2="21" y2="10"/>
                  </svg>
                  <span>{t("birthDate") || "Date of Birth"}</span>
                </div>
                <span className="font-semibold text-[#1B5E20]">{pig.birthDate}</span>
              </div>

              {/* Age */}
              <div className="flex items-center justify-between text-xs sm:text-sm">
                <div className="flex items-center gap-2 text-[#2E7D32] font-semibold">
                  <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 16 14"/>
                  </svg>
                  <span>{th("age") || "Age"}</span>
                </div>
                <span className="font-semibold text-[#1B5E20]">{formatSwineAge(pig.birthDate)}</span>
              </div>

              {/* Breed */}
              <div className="flex items-center justify-between text-xs sm:text-sm">
                <div className="flex items-center gap-2 text-[#2E7D32] font-semibold">
                  <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M14 9V5a3 3 0 00-3-3l-4 9v11h11.28a2 2 0 002-1.7l1.38-9a2 2 0 00-2-2.3zM7 22H4a2 2 0 01-2-2v-7a2 2 0 012-2h3"/>
                  </svg>
                  <span>{t("breed") || "Breed"}</span>
                </div>
                <span className="font-semibold text-[#1B5E20]">{pig.breed || "Not specified"}</span>
              </div>

              {/* Status */}
              <div className="flex items-center justify-between text-xs sm:text-sm">
                <div className="flex items-center gap-2 text-[#2E7D32] font-semibold">
                  <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="16" x2="12" y2="12"/>
                    <line x1="12" y1="8" x2="12.01" y2="8"/>
                  </svg>
                  <span>{t("currentStatus") || "Status"}</span>
                </div>
                <span className="font-semibold text-[#1B5E20]">{translateStatus(pig.status)}</span>
              </div>

              {/* Gender */}
              <div className="flex items-center justify-between text-xs sm:text-sm">
                <div className="flex items-center gap-2 text-[#2E7D32] font-semibold">
                  <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="4"/>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 2v6m0 8v6m-4-10H2m14 0h6"/>
                  </svg>
                  <span>{t("gender") || "Gender"}</span>
                </div>
                <span className="font-semibold text-[#1B5E20]">
                  {translateGender(pig.gender)}
                  {pig.gender === "Male" && pig.castrated ? ` (${t("castrated_label") || "Castrated"})` : ""}
                </span>
              </div>

              {/* Sow Parity */}
              {pig.gender === "Female" && pig.parity !== undefined && pig.parity > 0 && (
                <div className="flex items-center justify-between text-xs sm:text-sm">
                  <div className="flex items-center gap-2 text-[#2E7D32] font-semibold">
                    <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                    </svg>
                    <span>Parity</span>
                  </div>
                  <span className="font-semibold text-[#1B5E20]">
                    P{pig.parity} ({pig.parity} {pig.parity === 1 ? "litter" : "litters"})
                  </span>
                </div>
              )}

              {/* Due Date */}
              {pig.expectedFarrowingDate && (
                <div className="flex items-center justify-between text-xs sm:text-sm">
                  <div className="flex items-center gap-2 text-[#2E7D32] font-semibold">
                    <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                      <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                      <line x1="16" y1="2" x2="16" y2="6"/>
                      <line x1="8" y1="2" x2="8" y2="6"/>
                      <line x1="3" y1="10" x2="21" y2="10"/>
                    </svg>
                    <span>Due Date</span>
                  </div>
                  <span className="font-semibold text-[#1B5E20]">{pig.expectedFarrowingDate}</span>
                </div>
              )}

              {/* Farrowing Pen Move Date */}
              {pig.farrowingPenMoveDate && (
                <div className="flex items-center justify-between text-xs sm:text-sm">
                  <div className="flex items-center gap-2 text-[#2E7D32] font-semibold">
                    <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
                    </svg>
                    <span>Farrowing Pen Move</span>
                  </div>
                  <span className="font-semibold text-[#1B5E20]">{pig.farrowingPenMoveDate}</span>
                </div>
              )}

              {/* Weight */}
              <div className="flex items-center justify-between text-xs sm:text-sm">
                <div className="flex items-center gap-2 text-[#2E7D32] font-semibold">
                  <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M3 6l3 1m0 0l-3 9a5.002 5.002 0 006.001 0M6 7l3 9M6 7l6-2m6 2l3-1m-3 1l-3 9a5.002 5.002 0 006.001 0M18 7l3 9m-3-9l-6-2m0-2v2m0 16V5m0 16H9m3 0h3"/>
                  </svg>
                  <span>{t("weight") || "Weight"}</span>
                </div>
                <span className="font-semibold text-[#1B5E20]">{pig.weight} kg</span>
              </div>

              {/* Location */}
              <div className="flex items-center justify-between text-xs sm:text-sm">
                <div className="flex items-center gap-2 text-[#2E7D32] font-semibold">
                  <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
                  </svg>
                  <span>{t("location") || "Pen / Location"}</span>
                </div>
                <span className="font-semibold text-[#1B5E20]">{pig.location || t("unassigned") || "Unassigned"}</span>
              </div>

              {/* Source */}
              <div className="flex items-center justify-between text-xs sm:text-sm">
                <div className="flex items-center gap-2 text-[#2E7D32] font-semibold">
                  <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
                  </svg>
                  <span>{t("source") || "Source"}</span>
                </div>
                <span className="font-semibold text-[#1B5E20]">{pig.source || "Born on farm"}</span>
              </div>

              {/* Purpose */}
              <div className="flex items-center justify-between text-xs sm:text-sm">
                <div className="flex items-center gap-2 text-[#2E7D32] font-semibold">
                  <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"/>
                  </svg>
                  <span>{t("purpose") || "Purpose"}</span>
                </div>
                <span className={`text-xs font-bold px-2.5 py-0.5 rounded-md ${pig.purpose === "Breeder" ? "bg-[#C8E6C9] text-[#1B5E20]" : "bg-[#E8F5E9] text-[#2E7D32]"}`}>
                  {translatePurpose(pig.purpose)}
                </span>
              </div>

              {/* Sow Tag & Boar Tag */}
              {pig.sowTag && (
                <div className="flex items-center justify-between text-xs sm:text-sm">
                  <span className="text-[#2E7D32] font-semibold pl-6">{t("sowTag") || "Sow Tag"}</span>
                  <span className="font-mono font-semibold text-[#1B5E20]">{pig.sowTag}</span>
                </div>
              )}
              {pig.boarTag && (
                <div className="flex items-center justify-between text-xs sm:text-sm">
                  <span className="text-[#2E7D32] font-semibold pl-6">{t("boarTag") || "Boar Tag"}</span>
                  <span className="font-mono font-semibold text-[#1B5E20]">{pig.boarTag}</span>
                </div>
              )}
            </div>
          </div>

          {/* Notes Card (Matching Android Notes Card) */}
          {pig.notes && (
            <div className="bg-[#E8F5E9] border border-[#C8E6C9] rounded-2xl p-4 shadow-xs space-y-1.5">
              <h3 className="text-sm font-bold text-[#1B5E20]">{t("notes") || "Notes"}</h3>
              <p className="text-xs sm:text-sm text-[#2E7D32] whitespace-pre-line leading-relaxed">{pig.notes}</p>
            </div>
          )}

          {/* 2. Collapsible History Section (Closed by default) */}
          <div className="bg-white border border-zinc-200/90 rounded-2xl shadow-xs overflow-hidden transition-all duration-300">
            <div
              onClick={() => setIsHistoryExpanded(!isHistoryExpanded)}
              className="w-full flex items-center justify-between p-4 sm:p-5 text-left hover:bg-zinc-50/70 transition-colors cursor-pointer select-none"
            >
              <div className="flex items-center gap-3 min-w-0">
                <div className="h-10 w-10 rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-200/60 flex items-center justify-center flex-shrink-0">
                  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <h3 className="text-base font-bold text-zinc-900 truncate">{t("healthHistory")}</h3>
                    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-bold bg-zinc-100 text-zinc-700 shrink-0">
                      {healthRecords.length}
                    </span>
                  </div>
                  <p className="text-xs text-zinc-500 mt-0.5 truncate">
                    {healthRecords.length} {healthRecords.length === 1 ? "record" : "records"}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-3 shrink-0 ml-2">
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    setShowRecordModal(true);
                  }}
                  className="rounded-xl bg-emerald-600 hover:bg-emerald-700 px-3.5 py-1.5 text-xs font-bold text-white shadow-xs transition-all active:scale-95"
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
              <div className="border-t border-zinc-150 p-4 sm:p-6 bg-zinc-50/40 space-y-4 animate-fadeIn">
                {healthRecords.length === 0 ? (
                  <p className="text-sm text-zinc-500 text-center py-6">{t("noHealthRecords")}</p>
                ) : (
                  <div className="relative border-l border-emerald-300 pl-4 ml-2 space-y-5">
                    {healthRecords.map((record) => (
                      <div key={record.id} className="relative">
                        {/* Timeline dot */}
                        <span className="absolute -left-[21px] top-1.5 h-3.5 w-3.5 rounded-full border-2 border-emerald-600 bg-white" />
                        <div>
                          <div className="flex justify-between items-start gap-2">
                            <p className="text-xs sm:text-sm font-bold text-zinc-800">{translateActivityType(record.type)}</p>
                            <span className="text-xs text-zinc-400 font-mono shrink-0">{record.date}</span>
                          </div>
                          {record.description && (
                            <p className="text-xs text-zinc-600 mt-1 whitespace-pre-line leading-relaxed">{record.description}</p>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Delete Profile Action */}
          <div className="pt-2 text-center">
            <button
              onClick={handleDeletePig}
              className="text-xs font-semibold text-rose-600 hover:text-rose-700 hover:underline transition"
            >
              {t("deleteProfile")}
            </button>
          </div>
        </main>
      </div>

      {pig && (
        <HerdReport
          pigs={[pig]}
          allPigs={allPigs}
          healthRecords={{ [pig.id]: healthRecords }}
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
                  <option value="Confirm Pregnancy">{t("actionTypes.pregnancy")}</option>
                  <option value="Farrowing">Farrowing</option>
                  <option value="Weaning">Weaning</option>
                  <option value="Deworming">{t("actionTypes.deworming")}</option>
                  <option value="Iron Injection">Iron Injection</option>
                  <option value="Castration">Castration</option>
                  <option value="Teeth Clipping">Teeth Clipping</option>
                  <option value="Tail Docking">Tail Docking</option>
                  <option value="Weight Check">{t("actionTypes.weight")}</option>
                  <option value="Other">{t("actionTypes.other")}</option>
                </select>
              </div>

              {(recordType === "Breeding" || recordType === "Breeding/Mating") && (
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Boar Tag / ID</label>
                  <input
                    type="text"
                    value={recordBoarTag}
                    onChange={(e) => setRecordBoarTag(e.target.value)}
                    placeholder="e.g. Boar-01"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
              )}

              {(recordType === "Confirm Pregnancy" || recordType === "Pregnancy Confirmation") && (
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Pregnancy Confirmed?</label>
                  <div className="flex gap-4">
                    <label className="flex items-center gap-2 cursor-pointer text-sm font-semibold">
                      <input
                        type="radio"
                        checked={recordPregnancyConfirmed}
                        onChange={() => setRecordPregnancyConfirmed(true)}
                        className="h-4 w-4 border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                      />
                      <span>Confirmed (Pregnant)</span>
                    </label>
                    <label className="flex items-center gap-2 cursor-pointer text-sm font-semibold">
                      <input
                        type="radio"
                        checked={!recordPregnancyConfirmed}
                        onChange={() => setRecordPregnancyConfirmed(false)}
                        className="h-4 w-4 border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                      />
                      <span>Failed (Open / Sow)</span>
                    </label>
                  </div>
                </div>
              )}

              {["Medication", "Deworming", "Vaccination", "Treatment"].includes(recordType) && (
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Meat Withdrawal Period (Days)</label>
                  <input
                    type="number"
                    min="0"
                    value={recordWithdrawalDays}
                    onChange={(e) => setRecordWithdrawalDays(e.target.value)}
                    placeholder="e.g. 14"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
              )}

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
