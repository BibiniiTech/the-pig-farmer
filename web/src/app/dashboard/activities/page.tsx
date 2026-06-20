"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot, doc, setDoc, writeBatch, getDocs, query, where } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import UserProfileDropdown from "@/components/UserProfileDropdown";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import { useTranslations } from "next-intl";
import {
  HeatIcon,
  BreedingIcon,
  PregnancyCheckIcon,
  FarrowingIcon,
  WeaningIcon,
  CastrationIcon,
  TeethClippingIcon,
  TailDockingIcon,
  DewormingIcon,
  IronIcon,
  VaccinationIcon,
  MedicationIcon,
  WeightCheckerIcon,
  CullingIcon,
  NoteAddIcon
} from "@/components/icons/HerdActivityIcons";

interface Pig {
  id: string;
  tagNumber: string;
  gender: string;
  breed: string;
  status: string;
  location: string;
  weight: number;
  ironInjections?: number;
  birthDate?: string;
  castrated?: boolean;
  teethClipped?: boolean;
  tailDocked?: boolean;
  weaned?: boolean;
  lastBreedingDate?: string;
  lastBoarTag?: string;
  sowTag?: string;
}

const addDays = (dateStr: string, days: number) => {
  const result = new Date(dateStr);
  result.setDate(result.getDate() + days);
  return result.toISOString().split("T")[0];
};

const calculateAgeMonths = (birthDateStr?: string) => {
  if (!birthDateStr) return 0;
  const birthDate = new Date(birthDateStr);
  if (isNaN(birthDate.getTime())) return 0;
  const now = new Date();
  const diffTime = now.getTime() - birthDate.getTime();
  return diffTime / (1000 * 60 * 60 * 24 * 30.417);
};

export default function HerdActivitiesPage() {
  const t = useTranslations("Activities");
  const { user, userProfile, activeFarmUid, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  const activityCategories = [
    { type: t("categories.heat.type"), key: "Heat Detection", desc: t("categories.heat.desc"), icon: HeatIcon },
    { type: t("categories.breeding.type"), key: "Breeding/Mating", desc: t("categories.breeding.desc"), icon: BreedingIcon },
    { type: t("categories.pregnancy.type"), key: "Confirm Pregnancy", desc: t("categories.pregnancy.desc"), icon: PregnancyCheckIcon },
    { type: t("categories.farrowing.type"), key: "Farrowing", desc: t("categories.farrowing.desc"), icon: FarrowingIcon },
    { type: t("categories.weaning.type"), key: "Weaning", desc: t("categories.weaning.desc"), icon: WeaningIcon },
    { type: t("categories.castration.type"), key: "Castration", desc: t("categories.castration.desc"), icon: CastrationIcon },
    { type: t("categories.teeth.type"), key: "Teeth Clipping", desc: t("categories.teeth.desc"), icon: TeethClippingIcon },
    { type: t("categories.tail.type"), key: "Tail Docking", desc: t("categories.tail.desc"), icon: TailDockingIcon },
    { type: t("categories.deworming.type"), key: "Deworming", desc: t("categories.deworming.desc"), icon: DewormingIcon },
    { type: t("categories.iron.type"), key: "Iron Injection", desc: t("categories.iron.desc"), icon: IronIcon },
    { type: t("categories.vaccination.type"), key: "Vaccination", desc: t("categories.vaccination.desc"), icon: VaccinationIcon },
    { type: t("categories.medication.type"), key: "Medication", desc: t("categories.medication.desc"), icon: MedicationIcon },
    { type: t("categories.weight.type"), key: "Weight Check", desc: t("categories.weight.desc"), icon: WeightCheckerIcon },
    { type: t("categories.culling.type"), key: "Culling", desc: t("categories.culling.desc"), icon: CullingIcon },
    { type: t("categories.custom.type"), key: "Custom", desc: t("categories.custom.desc"), icon: NoteAddIcon }
  ];

  const currencySymbol = userProfile?.settings?.currencySymbol || "$";

  const [pigs, setPigs] = useState<Pig[]>([]);
  const [dataLoading, setDataLoading] = useState(true);
  const [selectedActivity, setSelectedActivity] = useState<any | null>(null);

  // General log variables
  const [selectedPigIds, setSelectedPigIds] = useState<string[]>([]);
  const [logDate, setLogDate] = useState(new Date().toISOString().split("T")[0]);
  const [notes, setNotes] = useState("");

  // Dynamic fields
  const [customName, setCustomName] = useState("");
  const [boarTag, setBoarTag] = useState("");
  const [matingOutcome, setMatingOutcome] = useState("Successful");
  const [pregnancyOutcome, setPregnancyOutcome] = useState("Successful");
  const [heatOutcome, setHeatOutcome] = useState("Heat Detected");
  const [cullingReason, setCullingReason] = useState("Sold");
  const [salePrice, setSalePrice] = useState("");
  const [numMales, setNumMales] = useState("");
  const [numFemales, setNumFemales] = useState("");
  const [maleTags, setMaleTags] = useState("");
  const [femaleTags, setFemaleTags] = useState("");

  // Location/Weights maps
  const [pigWeaningLocations, setPigWeaningLocations] = useState<Record<string, string>>({});
  const [pigWeights, setPigWeights] = useState<Record<string, string>>({});
  const [pigSalePrices, setPigSalePrices] = useState<Record<string, string>>({});

  // Medication inputs
  const [medicationName, setMedicationName] = useState("");
  const [medicationDosage, setMedicationDosage] = useState("");
  const [scheduleSecondIron, setScheduleSecondIron] = useState(false);
  const [showAllPigsForIron, setShowAllPigsForIron] = useState(false);

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!activeFarmUid) return;

    const pigsQuery = collection(db, "users", activeFarmUid, "pigs");
    const unsubscribe = onSnapshot(pigsQuery, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Pig));
      setPigs(list.sort((a, b) => a.tagNumber.localeCompare(b.tagNumber)));
      setDataLoading(false);
    });

    return () => unsubscribe();
  }, [activeFarmUid]);

  // Reset inputs when activity changes
  useEffect(() => {
    setSelectedPigIds([]);
    setNotes("");
    setCustomName("");
    setBoarTag("");
    setMatingOutcome("Successful");
    setPregnancyOutcome("Successful");
    setHeatOutcome("Heat Detected");
    setCullingReason("Sold");
    setSalePrice("");
    setNumMales("");
    setNumFemales("");
    setMaleTags("");
    setFemaleTags("");
    setPigWeaningLocations({});
    setPigWeights({});
    setPigSalePrices({});
    setMedicationName("");
    setMedicationDosage("");
    setScheduleSecondIron(false);
    setShowAllPigsForIron(false);
  }, [selectedActivity]);

  const getFilteredPigs = () => {
    if (!selectedActivity) return [];
    const key = selectedActivity.key;

    switch (key) {
      case "Heat Detection":
        return pigs.filter(p =>
          p.gender?.toLowerCase() === "female" &&
          ["sow", "gilt", "pregnant", "lactating", "nursing", "finisher"].includes(p.status?.toLowerCase())
        );

      case "Breeding/Mating":
        return pigs.filter(p =>
          p.gender?.toLowerCase() === "female" &&
          ["sow", "gilt", "pregnant", "lactating", "nursing", "finisher"].includes(p.status?.toLowerCase())
        );

      case "Confirm Pregnancy":
        return pigs.filter(p =>
          p.gender?.toLowerCase() === "female" &&
          ["sow", "gilt", "pregnant", "lactating", "nursing", "finisher"].includes(p.status?.toLowerCase()) &&
          p.lastBreedingDate
        );

      case "Farrowing":
        return pigs.filter(p =>
          p.status?.toLowerCase() === "pregnant" &&
          p.gender?.toLowerCase() === "female"
        );

      case "Weaning":
        return pigs.filter(p => !p.weaned && p.status?.toLowerCase() !== "sow" && p.status?.toLowerCase() !== "boar");

      case "Castration":
        return pigs.filter(p =>
          p.gender?.toLowerCase() === "male" &&
          !p.castrated &&
          calculateAgeMonths(p.birthDate) < 3
        );

      case "Teeth Clipping":
        return pigs.filter(p => !p.teethClipped && calculateAgeMonths(p.birthDate) < 2);

      case "Tail Docking":
        return pigs.filter(p => !p.tailDocked && calculateAgeMonths(p.birthDate) < 2);

      case "Iron Injection":
        if (showAllPigsForIron) return pigs;
        return pigs.filter(p => calculateAgeMonths(p.birthDate) < 2);

      default:
        return pigs;
    }
  };

  const boarsList = pigs.filter(p => p.gender?.toLowerCase() === "male" && p.status?.toLowerCase() === "boar");
  const filteredPigs = getFilteredPigs();

  const isFutureDate = (dStr: string) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const log = new Date(dStr);
    return log > today;
  };

  const cleanupTasksForPig = async (batch: any, pigId: string, pigTag: string) => {
    if (!activeFarmUid) return;
    const tasksRef = collection(db, "users", activeFarmUid, "tasks");
    const q = query(tasksRef, where("pigIds", "array-contains", pigId));
    const snapshot = await getDocs(q);
    snapshot.docs.forEach(docSnap => {
      const task = docSnap.data();
      const pigIds = task.pigIds || [];
      if (pigIds.length <= 1) {
        batch.delete(docSnap.ref);
      } else {
        const newPigIds = pigIds.filter((id: string) => id !== pigId);
        let newName = task.name;
        if (pigTag) {
          newName = task.name.replace(pigTag, "").replace(/,\s*,/g, ",").trim().replace(/^,|,$/g, "");
        }
        batch.update(docSnap.ref, { pigIds: newPigIds, name: newName });
      }
    });
  };

  const autoCompleteUpcomingTasks = async (batch: any, pigIds: string[], activityName: string) => {
    if (!activeFarmUid) return;
    const tasksRef = collection(db, "users", activeFarmUid, "tasks");
    const q = query(tasksRef, where("completed", "==", false));
    const snapshot = await getDocs(q);
    const pigTags = pigIds.map(id => pigs.find(p => p.id === id)?.tagNumber || id);

    snapshot.docs.forEach(docSnap => {
      const task = docSnap.data();
      const taskName = task.name || "";
      const taskActivity = taskName.split(":")[0].trim();
      
      let isMatch = false;
      switch (activityName) {
        case "Breeding/Mating":
          isMatch = taskActivity.toLowerCase().includes("breeding") || taskActivity.toLowerCase().includes("mating");
          break;
        case "Confirm Pregnancy":
          isMatch = taskActivity.toLowerCase().includes("pregnancy");
          break;
        case "Farrowing":
          isMatch = taskActivity.toLowerCase().includes("farrowing");
          break;
        case "Weaning":
          isMatch = taskActivity.toLowerCase().includes("weaning");
          break;
        case "Iron Injection":
          isMatch = taskActivity.toLowerCase().includes("iron");
          break;
        case "Heat Detection":
          isMatch = taskActivity.toLowerCase().includes("heat");
          break;
        case "Weight Check":
          isMatch = taskActivity.toLowerCase().includes("weight");
          break;
        case "Castration":
          isMatch = taskActivity.toLowerCase().includes("castration");
          break;
        case "Teeth Clipping":
          isMatch = taskActivity.toLowerCase().includes("teeth");
          break;
        case "Tail Docking":
          isMatch = taskActivity.toLowerCase().includes("tail");
          break;
        case "Deworming":
          isMatch = taskActivity.toLowerCase().includes("deworming");
          break;
        case "Vaccination":
          isMatch = taskActivity.toLowerCase().includes("vaccination");
          break;
        case "Medication":
          isMatch = taskActivity.toLowerCase().includes("medication");
          break;
        case "Culling":
          isMatch = taskActivity.toLowerCase().includes("culling");
          break;
        default:
          isMatch = taskActivity.toLowerCase() === activityName.toLowerCase();
      }

      if (isMatch) {
        const taskPigPart = taskName.includes(":") ? taskName.split(":")[1].trim() : "";
        const tagsInTask = taskPigPart
          .replace(/Pig\s+/i, "")
          .replace(/Pigs\s+/i, "")
          .split(",")
          .map((t: string) => t.trim())
          .filter((t: string) => t.length > 0);

        if (tagsInTask.length === 0 && taskPigPart === "") {
          batch.update(docSnap.ref, { completed: true });
        } else {
          const matchingTags = tagsInTask.filter((t: string) => pigTags.includes(t));
          if (matchingTags.length > 0) {
            if (matchingTags.length === tagsInTask.length) {
              batch.update(docSnap.ref, { completed: true });
            } else {
              const remainingTags = tagsInTask.filter((t: string) => !matchingTags.includes(t));
              const newName = remainingTags.length === 1 
                ? `${taskActivity}: Pig ${remainingTags[0]}` 
                : `${taskActivity}: Pigs ${remainingTags.join(", ")}`;
              const remainingPigIds = (task.pigIds || []).filter((id: string) => !pigIds.includes(id));
              batch.update(docSnap.ref, { name: newName, pigIds: remainingPigIds });
            }
          }
        }
      }
    });
  };

  const handleLogActivity = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || selectedPigIds.length === 0) return;

    try {
      const batch = writeBatch(db);
      const activityType = selectedActivity.key;

      // Logging Batch Changes
      for (const pigId of selectedPigIds) {
        const pig = pigs.find(p => p.id === pigId);
        if (!pig) continue;

        const isFuture = isFutureDate(logDate);
        const pigRef = doc(db, "users", activeFarmUid, "pigs", pigId);
        const healthRecordsRef = doc(collection(db, "users", activeFarmUid, "pigs", pigId, "health_records"));
        let finalDescription = notes;
        let taskId: string | null = null;

        // If Future Date: Create a Task and link it
        if (isFuture) {
          const taskRef = doc(collection(db, "users", activeFarmUid, "tasks"));
          taskId = taskRef.id;
          const taskName = `${activityType === "Custom" ? customName || "Custom" : activityType}: Pig ${pig.tagNumber}`;

          batch.set(taskRef, {
            id: taskId,
            name: taskName,
            date: logDate,
            notes: notes || "Scheduled future activity from operations log screen",
            pigIds: [pigId],
            completed: false
          });
        }

        // Logic for specialized activities (mostly for non-future logs)
        if (!isFuture) {
          // Breeding/Mating
          if (activityType === "Breeding/Mating") {
            finalDescription = `${notes}\nMated Sow ${pig.tagNumber} with Boar ${boarTag}\nOutcome: ${matingOutcome}`.trim();
            if (pig.gender === "Female") {
              batch.update(pigRef, {
                lastBreedingDate: logDate,
                lastBoarTag: boarTag,
                purpose: "Breeder"
              });
              if (matingOutcome === "Successful") {
                const taskRef = doc(collection(db, "users", activeFarmUid, "tasks"));
                batch.set(taskRef, {
                  id: taskRef.id,
                  name: `Confirm Pregnancy: Pig ${pig.tagNumber}`,
                  date: addDays(logDate, 21),
                  notes: `Scheduled 21 days after mating on ${logDate}`,
                  pigIds: [pigId],
                  completed: false
                });
              }
            }
          }

          // Heat Detection
          if (activityType === "Heat Detection") {
            finalDescription = `${notes}\nOutcome: ${heatOutcome}`.trim();
            if (heatOutcome === "Heat Detected") {
              const taskRef = doc(collection(db, "users", activeFarmUid, "tasks"));
              batch.set(taskRef, {
                id: taskRef.id,
                name: `Heat Detection: Pig ${pig.tagNumber}`,
                date: addDays(logDate, 21),
                notes: `Auto-created 21 days after heat detection on ${logDate}`,
                pigIds: [pigId],
                completed: false
              });
            }
          }

          // Confirm Pregnancy
          if (activityType === "Confirm Pregnancy") {
            if (pregnancyOutcome === "Successful") {
              batch.update(pigRef, { status: "Pregnant", purpose: "Breeder" });
              const sowBreedingDate = pig.lastBreedingDate || logDate;
              const taskRef = doc(collection(db, "users", activeFarmUid, "tasks"));
              batch.set(taskRef, {
                id: taskRef.id,
                name: `Farrowing: Pig ${pig.tagNumber}`,
                date: addDays(sowBreedingDate, 114),
                notes: `Scheduled 114 days after mating on ${sowBreedingDate}`,
                pigIds: [pigId],
                completed: false
              });
              finalDescription = `${notes}\nPregnancy Confirmed. Farrowing scheduled.`.trim();
            } else {
              finalDescription = `${notes}\nPregnancy check failed.`.trim();
            }
          }

          // Farrowing
          if (activityType === "Farrowing") {
            const malesCount = parseInt(numMales) || 0;
            const femalesCount = parseInt(numFemales) || 0;
            finalDescription = `${notes}\nFarrowed: ${malesCount} Males, ${femalesCount} Females`.trim();
            batch.update(pigRef, { status: "Lactating", hasFarrowed: true, weaned: false, purpose: "Breeder" });

            const maleTagsArr = maleTags.split(",").map(t => t.trim()).filter(t => t.length > 0);
            for (let i = 0; i < malesCount; i++) {
              const kidRef = doc(collection(db, "users", activeFarmUid, "pigs"));
              const tag = maleTagsArr[i] || `${pig.tagNumber}-M${i + 1}`;
              batch.set(kidRef, {
                id: kidRef.id,
                tagNumber: tag,
                gender: "Male",
                breed: pig.breed || "",
                birthDate: logDate,
                status: "Piglet",
                sowTag: pig.tagNumber,
                boarTag: pig.lastBoarTag || "",
                location: pig.location || "",
                source: "Born on farm",
                weight: 1.5
              });
            }

            const femaleTagsArr = femaleTags.split(",").map(t => t.trim()).filter(t => t.length > 0);
            for (let i = 0; i < femalesCount; i++) {
              const kidRef = doc(collection(db, "users", activeFarmUid, "pigs"));
              const tag = femaleTagsArr[i] || `${pig.tagNumber}-F${i + 1}`;
              batch.set(kidRef, {
                id: kidRef.id,
                tagNumber: tag,
                gender: "Female",
                breed: pig.breed || "",
                birthDate: logDate,
                status: "Piglet",
                sowTag: pig.tagNumber,
                boarTag: pig.lastBoarTag || "",
                location: pig.location || "",
                source: "Born on farm",
                weight: 1.5
              });
            }
          }

          // Weaning
          if (activityType === "Weaning") {
            const targetLocation = pigWeaningLocations[pigId] || pig.location || "";
            batch.update(pigRef, { status: pig.status === "Lactating" ? "Sow" : "Starter", weaned: true });
            if (targetLocation) {
              batch.update(pigRef, { location: targetLocation });
            }
            finalDescription = `${notes}\nWeaned and moved to location: ${targetLocation}`.trim();

            if (pig.status === "Lactating" || pig.status === "Nursing") {
              batch.update(pigRef, { status: "Sow" });
            } else {
              const sowTagVal = pig.sowTag || "";
              if (sowTagVal) {
                const otherOffspringQuery = query(
                  collection(db, "users", activeFarmUid, "pigs"),
                  where("sowTag", "==", sowTagVal),
                  where("weaned", "==", false)
                );
                const otherSnap = await getDocs(otherOffspringQuery);
                const allOffspringCompleted = otherSnap.docs.every(docSnap =>
                  docSnap.id === pigId || selectedPigIds.includes(docSnap.id)
                );
                if (allOffspringCompleted) {
                  const sowQuery = query(collection(db, "users", activeFarmUid, "pigs"), where("tagNumber", "==", sowTagVal));
                  const sowSnap = await getDocs(sowQuery);
                  if (!sowSnap.empty) {
                    batch.update(sowSnap.docs[0].ref, { status: "Sow" });
                  }
                }
              }
            }
          }

          // Castration
          if (activityType === "Castration" && pig.gender === "Male") {
            batch.update(pigRef, { castrated: true, castrationDate: logDate });
          }

          // Teeth Clipping
          if (activityType === "Teeth Clipping") {
            batch.update(pigRef, { teethClipped: true });
          }

          // Tail Docking
          if (activityType === "Tail Docking") {
            batch.update(pigRef, { tailDocked: true });
          }

          // Iron Injection / Medication
          if (["Iron Injection", "Deworming", "Vaccination", "Medication"].includes(activityType)) {
            finalDescription = `${notes}\nMedication/Vaccine: ${medicationName || activityType}, Dosage: ${medicationDosage || "N/A"}`.trim();
            if (activityType === "Iron Injection") {
              const currentCount = pig.ironInjections || 0;
              batch.update(pigRef, { ironInjections: currentCount + 1 });
              if (scheduleSecondIron && currentCount === 0) {
                const taskRef = doc(collection(db, "users", activeFarmUid, "tasks"));
                batch.set(taskRef, {
                  id: taskRef.id,
                  name: `2nd Iron Injection: Pig ${pig.tagNumber}`,
                  date: addDays(logDate, 7),
                  notes: "Scheduled 7 days after first injection",
                  pigIds: [pigId],
                  completed: false
                });
              }
            }
          }

          // Weight Check
          if (activityType === "Weight Check") {
            const pigWeight = parseFloat(pigWeights[pigId]) || 0;
            if (pigWeight > 0) {
              batch.update(pigRef, { weight: pigWeight });
              finalDescription = `${notes}\nWeight updated to: ${pigWeight} kg`.trim();
            }
          }

          // Culling
          if (activityType === "Culling") {
            const individualPrice = parseFloat(pigSalePrices[pigId]) || (parseFloat(salePrice) || 0) / selectedPigIds.length;
            const archiveRef = doc(db, "users", activeFarmUid, "archived_pigs", pigId);

            batch.set(archiveRef, {
              ...pig,
              status: `Culled (${cullingReason})`,
              cullingReason,
              salePrice: cullingReason === "Sold" ? individualPrice : 0,
              culledDate: logDate
            });
            batch.delete(pigRef);

            if (cullingReason === "Sold" && individualPrice > 0) {
              const finRef = doc(collection(db, "users", activeFarmUid, "financials"));
              batch.set(finRef, {
                id: finRef.id,
                type: "Income",
                category: "Pig Sale",
                amount: individualPrice,
                date: logDate,
                description: `Sold Culled Pig: ${pig.tagNumber}`
              });
              finalDescription = `${notes}\nCulled (Sold) for: ${currencySymbol} ${individualPrice}`.trim();
            } else {
              finalDescription = `${notes}\nCulled reason: ${cullingReason}`.trim();
            }
            await cleanupTasksForPig(batch, pigId, pig.tagNumber);
          }
        }

        // Save health record (For both immediate and future activities)
        const actName = activityType === "Custom" ? customName || "Custom" : activityType;

        // If it's a future task, or not a culling, it goes into the active pig's records.
        // Immediate culling is the only case where it goes to archived_pigs.
        const hrRef = (activityType === "Culling" && !isFuture)
          ? doc(collection(db, "users", activeFarmUid, "archived_pigs", pigId, "health_records"))
          : doc(collection(db, "users", activeFarmUid, "pigs", pigId, "health_records"));

        const hrData: any = {
          id: hrRef.id,
          date: logDate,
          type: actName,
          description: finalDescription
        };
        if (taskId) {
          hrData.taskId = taskId;
        }

        batch.set(hrRef, hrData);
      }

      // Auto-complete matching outstanding tasks for selected pigs
      await autoCompleteUpcomingTasks(batch, selectedPigIds, activityType);

      await batch.commit();

      setSelectedPigIds([]);
      setNotes("");
      setSelectedActivity(null);
    } catch (err) {
      console.error("Failed to batch log activity:", err);
    }
  };

  const handleTogglePigSelection = (id: string) => {
    setSelectedPigIds(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  };

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-white text-zinc-900">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  const renderFarrowingExpectedDate = () => {
    if (selectedPigIds.length !== 1) return null;
    const pig = pigs.find(p => p.id === selectedPigIds[0]);
    if (!pig || !pig.lastBreedingDate) return null;
    const expected = addDays(pig.lastBreedingDate, 114);
    return (
      <div className="p-3 bg-emerald-50 border border-emerald-250 rounded-lg text-xs font-semibold text-emerald-800">
        {t("expectedFarrowing", { date: expected })}
      </div>
    );
  };

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
        {!isMobile && <DesktopHeader />}

        <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8 space-y-6">
          <div className="space-y-1">
            <h2 className="text-2xl font-bold text-zinc-900">{t("title")}</h2>
            <p className="text-sm text-zinc-500">{t("description")}</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {activityCategories.map((act, i) => (
              <button
                key={i}
                onClick={() => setSelectedActivity(act)}
                className="bg-white/60 hover:bg-zinc-50/60 backdrop-blur-md border border-zinc-200 hover:border-emerald-500/40 rounded-xl p-5 text-left transition shadow-sm flex items-start gap-4 group"
              >
                <span className="p-3 bg-zinc-100 rounded-xl group-hover:scale-110 transition duration-300 text-emerald-600">
                  <act.icon className="h-8 w-8" />
                </span>
                <div>
                  <h4 className="font-bold text-zinc-900 text-sm group-hover:text-emerald-700 transition">
                    {act.type}
                  </h4>
                  <p className="text-xs text-zinc-500 mt-1">{act.desc}</p>
                </div>
              </button>
            ))}
          </div>
        </main>
      </div>

      {/* Log Activity Modal */}
      {selectedActivity && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-lg p-6 space-y-6 shadow-2xl max-h-[90vh] overflow-y-auto no-scrollbar">
            <h3 className="text-lg font-bold text-zinc-900 flex items-center gap-2 border-b border-zinc-100 pb-3">
              <span className="text-emerald-600"><selectedActivity.icon className="h-6 w-6" /></span>
              <span>{t("logActivity", { type: selectedActivity.type })}</span>
            </h3>

            <form onSubmit={handleLogActivity} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("procedureDate")}</label>
                <input
                  type="date"
                  required
                  value={logDate}
                  onChange={(e) => setLogDate(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                />
              </div>

              {/* Breeding/Mating Dropdowns */}
              {selectedActivity.key === "Breeding/Mating" && (
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("chooseSow")}</label>
                      <select
                        required
                        value={selectedPigIds[0] || ""}
                        onChange={(e) => setSelectedPigIds([e.target.value])}
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                      >
                        <option value="">{t("selectSow")}</option>
                        {filteredPigs.map(p => (
                          <option key={p.id} value={p.id}>{p.tagNumber} ({p.status})</option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("chooseBoar")}</label>
                      <select
                        required
                        value={boarTag}
                        onChange={(e) => setBoarTag(e.target.value)}
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                      >
                        <option value="">{t("selectBoar")}</option>
                        {boarsList.map(p => (
                          <option key={p.id} value={p.tagNumber}>{p.tagNumber}</option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("matingOutcome")}</label>
                    <select
                      value={matingOutcome}
                      onChange={(e) => setMatingOutcome(e.target.value)}
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                    >
                      <option value="Successful">{t("successful")}</option>
                      <option value="Unsuccessful">{t("unsuccessful")}</option>
                    </select>
                  </div>
                </div>
              )}

              {/* Heat Detection */}
              {selectedActivity.key === "Heat Detection" && (
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("heatOutcome")}</label>
                  <select
                    value={heatOutcome}
                    onChange={(e) => setHeatOutcome(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    <option value="Heat Detected">{t("heatDetected")}</option>
                    <option value="No Heat Detected">{t("noHeatDetected")}</option>
                  </select>
                </div>
              )}

              {/* Confirm Pregnancy Radio Buttons */}
              {selectedActivity.key === "Confirm Pregnancy" && (
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("pregnancyConfirmed")}</label>
                  <div className="flex gap-4">
                    <label className="flex items-center gap-2 cursor-pointer text-sm font-semibold">
                      <input
                        type="radio"
                        checked={pregnancyOutcome === "Successful"}
                        onChange={() => setPregnancyOutcome("Successful")}
                        className="h-4 w-4 border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                      />
                      <span>{t("yes")} (Pregnant)</span>
                    </label>
                    <label className="flex items-center gap-2 cursor-pointer text-sm font-semibold">
                      <input
                        type="radio"
                        checked={pregnancyOutcome === "Failed"}
                        onChange={() => setPregnancyOutcome("Failed")}
                        className="h-4 w-4 border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                      />
                      <span>{t("no")} (Failed)</span>
                    </label>
                  </div>
                </div>
              )}

              {/* Farrowing Inputs */}
              {selectedActivity.key === "Farrowing" && (
                <div className="space-y-4">
                  {renderFarrowingExpectedDate()}
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("malePiglets")}</label>
                      <input
                        type="number"
                        min="0"
                        value={numMales}
                        onChange={(e) => setNumMales(e.target.value)}
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("femalePiglets")}</label>
                      <input
                        type="number"
                        min="0"
                        value={numFemales}
                        onChange={(e) => setNumFemales(e.target.value)}
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                      />
                    </div>
                  </div>

                  {(parseInt(numMales) || 0) > 0 && (
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("maleTags")}</label>
                      <input
                        type="text"
                        value={maleTags}
                        onChange={(e) => setMaleTags(e.target.value)}
                        placeholder="e.g. tag1, tag2"
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                      />
                    </div>
                  )}

                  {(parseInt(numFemales) || 0) > 0 && (
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("femaleTags")}</label>
                      <input
                        type="text"
                        value={femaleTags}
                        onChange={(e) => setFemaleTags(e.target.value)}
                        placeholder="e.g. tag1, tag2"
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                      />
                    </div>
                  )}
                </div>
              )}

              {/* Medication, Deworming, Vaccination, Iron */}
              {["Iron Injection", "Deworming", "Vaccination", "Medication"].includes(selectedActivity.key) && (
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("medicationName")}</label>
                    <input
                      type="text"
                      required
                      value={medicationName}
                      onChange={(e) => setMedicationName(e.target.value)}
                      placeholder="e.g. Iron Dextran"
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("dosage")}</label>
                    <input
                      type="text"
                      required
                      value={medicationDosage}
                      onChange={(e) => setMedicationDosage(e.target.value)}
                      placeholder="e.g. 2 ml"
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                    />
                  </div>

                  {selectedActivity.key === "Iron Injection" && (
                    <div className="col-span-2 space-y-3">
                      <label className="flex items-center gap-2 cursor-pointer text-sm font-semibold">
                        <input
                          type="checkbox"
                          checked={scheduleSecondIron}
                          onChange={(e) => setScheduleSecondIron(e.target.checked)}
                          className="h-4 w-4 rounded border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                        />
                        <span>{t("scheduleSecondIron")}</span>
                      </label>
                      <button
                        type="button"
                        onClick={() => setShowAllPigsForIron(!showAllPigsForIron)}
                        className="text-xs font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 px-3 py-1 rounded"
                      >
                        {showAllPigsForIron ? t("showUnder2Months") : t("showAllPigs")}
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* Custom Activity Name */}
              {selectedActivity.key === "Custom" && (
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("customName")}</label>
                  <input
                    type="text"
                    required
                    value={customName}
                    onChange={(e) => setCustomName(e.target.value)}
                    placeholder="e.g. Hoof Trimming"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
              )}

              {/* Culling Reasons */}
              {selectedActivity.key === "Culling" && (
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("reason")}</label>
                    <select
                      value={cullingReason}
                      onChange={(e) => setCullingReason(e.target.value)}
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                    >
                      <option value="Natural Causes">Natural Causes</option>
                      <option value="Disease">Disease</option>
                      <option value="Sold">Sold</option>
                    </select>
                  </div>
                  {cullingReason === "Sold" && selectedPigIds.length > 0 && (
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("totalSalePrice", { symbol: currencySymbol })}</label>
                      <input
                        type="number"
                        step="any"
                        value={salePrice}
                        onChange={(e) => setSalePrice(e.target.value)}
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                      />
                    </div>
                  )}
                </div>
              )}

              {/* Pig Selection Checkbox Checklist */}
              {selectedActivity.key !== "Breeding/Mating" && (
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">
                    {t("selectTargetPigs", { count: selectedPigIds.length })}
                  </label>
                  <div className="border border-zinc-200 rounded-lg max-h-36 overflow-y-auto p-3 space-y-2 bg-zinc-50/50">
                    {filteredPigs.length === 0 ? (
                      <p className="text-xs text-zinc-400 italic">{t("noPigsMatch")}</p>
                    ) : (
                      filteredPigs.map(pig => (
                        <label key={pig.id} className="flex items-center gap-2 cursor-pointer text-xs text-zinc-700">
                          <input
                            type="checkbox"
                            checked={selectedPigIds.includes(pig.id)}
                            onChange={() => {
                              // Farrowing: limit to 1 pig
                              if (selectedActivity.key === "Farrowing") {
                                setSelectedPigIds([pig.id]);
                              } else {
                                handleTogglePigSelection(pig.id);
                              }
                            }}
                            className="h-4 w-4 rounded border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                          />
                          <span>{pig.tagNumber} ({pig.gender} • {pig.status})</span>
                        </label>
                      ))
                    )}
                  </div>
                </div>
              )}

              {/* Individual Weaning Locations Input Fields */}
              {selectedActivity.key === "Weaning" && selectedPigIds.length > 0 && (
                <div className="space-y-2.5 border-t border-zinc-100 pt-3">
                  <h4 className="text-xs font-bold text-zinc-700">{t("weaningLocations")}</h4>
                  {selectedPigIds.map(pigId => {
                    const pig = pigs.find(p => p.id === pigId);
                    if (!pig) return null;
                    return (
                      <div key={pigId} className="flex items-center gap-3 justify-between">
                        <span className="text-xs text-zinc-650">{pig.tagNumber} location:</span>
                        <input
                          type="text"
                          required
                          value={pigWeaningLocations[pigId] || ""}
                          onChange={(e) => setPigWeaningLocations(prev => ({ ...prev, [pigId]: e.target.value }))}
                          placeholder="e.g. Starter Pen A"
                          className="w-1/2 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none shadow-sm"
                        />
                      </div>
                    );
                  })}
                </div>
              )}

              {/* Individual Weight Check Input Fields */}
              {selectedActivity.key === "Weight Check" && selectedPigIds.length > 0 && (
                <div className="space-y-2.5 border-t border-zinc-100 pt-3">
                  <h4 className="text-xs font-bold text-zinc-700">{t("weights")}</h4>
                  {selectedPigIds.map(pigId => {
                    const pig = pigs.find(p => p.id === pigId);
                    if (!pig) return null;
                    return (
                      <div key={pigId} className="flex items-center gap-3 justify-between">
                        <span className="text-xs text-zinc-650">{pig.tagNumber} weight:</span>
                        <input
                          type="number"
                          step="any"
                          required
                          value={pigWeights[pigId] || ""}
                          onChange={(e) => setPigWeights(prev => ({ ...prev, [pigId]: e.target.value }))}
                          placeholder="e.g. 15.5"
                          className="w-1/2 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none shadow-sm"
                        />
                      </div>
                    );
                  })}
                </div>
              )}

              {/* Individual Sale Price Inputs for Culling */}
              {selectedActivity.key === "Culling" && cullingReason === "Sold" && selectedPigIds.length > 1 && (
                <div className="space-y-2.5 border-t border-zinc-100 pt-3">
                  <h4 className="text-xs font-bold text-zinc-700">{t("salePrices", { symbol: currencySymbol })}</h4>
                  {selectedPigIds.map(pigId => {
                    const pig = pigs.find(p => p.id === pigId);
                    if (!pig) return null;
                    return (
                      <div key={pigId} className="flex items-center gap-3 justify-between">
                        <span className="text-xs text-zinc-650">{pig.tagNumber} sale price:</span>
                        <input
                          type="number"
                          step="any"
                          value={pigSalePrices[pigId] || ""}
                          onChange={(e) => setPigSalePrices(prev => ({ ...prev, [pigId]: e.target.value }))}
                          placeholder="Individual price (optional)"
                          className="w-1/2 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none shadow-sm"
                        />
                      </div>
                    );
                  })}
                </div>
              )}

              {/* Description / Notes */}
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("descriptionNotes")}</label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Enter details..."
                  rows={2}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                />
              </div>

              {/* Actions */}
              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
                <button
                  type="button"
                  onClick={() => setSelectedActivity(null)}
                  className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100 transition"
                >
                  {t("cancel")}
                </button>
                <button
                  type="submit"
                  disabled={selectedPigIds.length === 0}
                  className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition disabled:opacity-50"
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
