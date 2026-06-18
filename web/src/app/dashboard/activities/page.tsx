"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot, doc, setDoc, writeBatch, getDocs, query, where } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import NavbarDropdown from "@/components/NavbarDropdown";
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

const activityCategories = [
  { type: "Heat Detection", desc: "Identify sows or gilts showing signs of standing heat.", icon: HeatIcon },
  { type: "Breeding/Mating", desc: "Record sow mating dates with a specific boar tag.", icon: BreedingIcon },
  { type: "Confirm Pregnancy", desc: "Perform ultrasound or manual checks (21-30 days post-mating).", icon: PregnancyCheckIcon },
  { type: "Farrowing", desc: "Log farrowing events and auto-register new born piglets.", icon: FarrowingIcon },
  { type: "Weaning", desc: "Record weaning event, change parent status, and move starter piglets.", icon: WeaningIcon },
  { type: "Castration", desc: "Log castration details for male piglets.", icon: CastrationIcon },
  { type: "Teeth Clipping", desc: "Log teeth clipping to prevent udder injuries.", icon: TeethClippingIcon },
  { type: "Tail Docking", desc: "Record tail docking for cannibalism prevention.", icon: TailDockingIcon },
  { type: "Deworming", desc: "Log routine anthelmintic medication.", icon: DewormingIcon },
  { type: "Iron Injection", desc: "Log iron injections given to newborn piglets.", icon: IronIcon },
  { type: "Vaccination", desc: "Log parvovirus, erysipelas, or FMD vaccines.", icon: VaccinationIcon },
  { type: "Medication", desc: "Log standard antibiotics or vet treatments.", icon: MedicationIcon },
  { type: "Weight Check", desc: "Record current weight check metric values.", icon: WeightCheckerIcon },
  { type: "Culling", desc: "Record sell or mortality culls to archive the pig.", icon: CullingIcon },
  { type: "Custom", desc: "Log any customized farm actions not listed.", icon: NoteAddIcon }
];

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
  const { user, activeFarmUid, loading } = useAuth();
  const router = useRouter();

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
    const type = selectedActivity.type;

    switch (type) {
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
      const activityType = selectedActivity.type;

      // Handle Future Date: Schedule a Task only
      if (isFutureDate(logDate)) {
        const taskRef = doc(collection(db, "users", activeFarmUid, "tasks"));
        const tags = selectedPigIds.map(id => pigs.find(p => p.id === id)?.tagNumber || id);
        const taskName = `${activityType === "Custom" ? customName || "Custom" : activityType}: Pig ${tags.join(", ")}`;

        await setDoc(taskRef, {
          id: taskRef.id,
          name: taskName,
          date: logDate,
          notes: notes || "Scheduled future activity from operations log screen",
          pigIds: selectedPigIds,
          completed: false
        });

        setSelectedPigIds([]);
        setNotes("");
        setSelectedActivity(null);
        return;
      }

      // Logging Immediate Batch Changes
      for (const pigId of selectedPigIds) {
        const pig = pigs.find(p => p.id === pigId);
        if (!pig) continue;

        const pigRef = doc(db, "users", activeFarmUid, "pigs", pigId);
        const healthRecordsRef = doc(collection(db, "users", activeFarmUid, "pigs", pigId, "health_records"));
        let finalDescription = notes;

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
                pigIds: [pigId]
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
              pigIds: [pigId]
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
              pigIds: [pigId]
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
                pigIds: [pigId]
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
            finalDescription = `${notes}\nCulled (Sold) for: Ksh ${individualPrice}`.trim();
          } else {
            finalDescription = `${notes}\nCulled reason: ${cullingReason}`.trim();
          }
          await cleanupTasksForPig(batch, pigId, pig.tagNumber);
        }

        // Save health record
        const actName = activityType === "Custom" ? customName || "Custom" : activityType;
        const hrRef = activityType === "Culling"
          ? doc(collection(db, "users", activeFarmUid, "archived_pigs", pigId, "health_records"))
          : doc(collection(db, "users", activeFarmUid, "pigs", pigId, "health_records"));

        batch.set(hrRef, {
          id: hrRef.id,
          date: logDate,
          type: actName,
          description: finalDescription
        });
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
        Expected Farrowing Date: {expected}
      </div>
    );
  };

  return (
    <div className="relative min-h-screen bg-white text-zinc-900 flex flex-col font-sans overflow-hidden">
      {/* Watermark Logo Background */}
      <div className="fixed inset-0 z-0 flex items-center justify-center opacity-[0.15] pointer-events-none select-none">
        <img
          src="/app_logo.png"
          alt="Watermark Background Logo"
          className="w-full max-w-[1100px] max-h-[85vh] object-contain"
        />
      </div>

      <div className="relative z-10 flex flex-col min-h-screen">
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

        <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8 space-y-6">
          <div className="space-y-1">
            <h2 className="text-2xl font-bold text-zinc-900">Execute Herd Procedure</h2>
            <p className="text-sm text-zinc-500">Select an activity to update pig statuses, record health logs, or schedule future tasks.</p>
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
              <span>Log Activity: {selectedActivity.type}</span>
            </h3>

            <form onSubmit={handleLogActivity} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Procedure Date</label>
                <input
                  type="date"
                  required
                  value={logDate}
                  onChange={(e) => setLogDate(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                />
              </div>

              {/* Breeding/Mating Dropdowns */}
              {selectedActivity.type === "Breeding/Mating" && (
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Select Sow (Female)</label>
                      <select
                        required
                        value={selectedPigIds[0] || ""}
                        onChange={(e) => setSelectedPigIds([e.target.value])}
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                      >
                        <option value="">-- Choose Sow --</option>
                        {filteredPigs.map(p => (
                          <option key={p.id} value={p.id}>{p.tagNumber} ({p.status})</option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Select Boar (Male)</label>
                      <select
                        required
                        value={boarTag}
                        onChange={(e) => setBoarTag(e.target.value)}
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                      >
                        <option value="">-- Choose Boar --</option>
                        {boarsList.map(p => (
                          <option key={p.id} value={p.tagNumber}>{p.tagNumber}</option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Mating Outcome</label>
                    <select
                      value={matingOutcome}
                      onChange={(e) => setMatingOutcome(e.target.value)}
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                    >
                      <option value="Successful">Mating Successful</option>
                      <option value="Unsuccessful">Mating Failed</option>
                    </select>
                  </div>
                </div>
              )}

              {/* Heat Detection */}
              {selectedActivity.type === "Heat Detection" && (
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Heat Outcome</label>
                  <select
                    value={heatOutcome}
                    onChange={(e) => setHeatOutcome(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    <option value="Heat Detected">Heat Detected</option>
                    <option value="No Heat Detected">No Heat Detected</option>
                  </select>
                </div>
              )}

              {/* Confirm Pregnancy Radio Buttons */}
              {selectedActivity.type === "Confirm Pregnancy" && (
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Pregnancy Confirmed?</label>
                  <div className="flex gap-4">
                    <label className="flex items-center gap-2 cursor-pointer text-sm font-semibold">
                      <input
                        type="radio"
                        checked={pregnancyOutcome === "Successful"}
                        onChange={() => setPregnancyOutcome("Successful")}
                        className="h-4 w-4 border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                      />
                      <span>Yes (Pregnant)</span>
                    </label>
                    <label className="flex items-center gap-2 cursor-pointer text-sm font-semibold">
                      <input
                        type="radio"
                        checked={pregnancyOutcome === "Failed"}
                        onChange={() => setPregnancyOutcome("Failed")}
                        className="h-4 w-4 border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                      />
                      <span>No (Failed)</span>
                    </label>
                  </div>
                </div>
              )}

              {/* Farrowing Inputs */}
              {selectedActivity.type === "Farrowing" && (
                <div className="space-y-4">
                  {renderFarrowingExpectedDate()}
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Male Piglets</label>
                      <input
                        type="number"
                        min="0"
                        value={numMales}
                        onChange={(e) => setNumMales(e.target.value)}
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Female Piglets</label>
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
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Male Tags (Comma Separated)</label>
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
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Female Tags (Comma Separated)</label>
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
              {["Iron Injection", "Deworming", "Vaccination", "Medication"].includes(selectedActivity.type) && (
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Medication Name</label>
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
                    <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Dosage</label>
                    <input
                      type="text"
                      required
                      value={medicationDosage}
                      onChange={(e) => setMedicationDosage(e.target.value)}
                      placeholder="e.g. 2 ml"
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                    />
                  </div>

                  {selectedActivity.type === "Iron Injection" && (
                    <div className="col-span-2 space-y-3">
                      <label className="flex items-center gap-2 cursor-pointer text-sm font-semibold">
                        <input
                          type="checkbox"
                          checked={scheduleSecondIron}
                          onChange={(e) => setScheduleSecondIron(e.target.checked)}
                          className="h-4 w-4 rounded border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                        />
                        <span>Schedule 2nd Iron Injection (7 days later)?</span>
                      </label>
                      <button
                        type="button"
                        onClick={() => setShowAllPigsForIron(!showAllPigsForIron)}
                        className="text-xs font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 px-3 py-1 rounded"
                      >
                        {showAllPigsForIron ? "Show under 2 months only" : "Show All Pigs"}
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* Custom Activity Name */}
              {selectedActivity.type === "Custom" && (
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Custom Activity Name</label>
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
              {selectedActivity.type === "Culling" && (
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Reason</label>
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
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Total Sale Price ($)</label>
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
              {selectedActivity.type !== "Breeding/Mating" && (
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">
                    Select Target Pigs ({selectedPigIds.length})
                  </label>
                  <div className="border border-zinc-200 rounded-lg max-h-36 overflow-y-auto p-3 space-y-2 bg-zinc-50/50">
                    {filteredPigs.length === 0 ? (
                      <p className="text-xs text-zinc-400 italic">No pigs match the target growth stage or status rules.</p>
                    ) : (
                      filteredPigs.map(pig => (
                        <label key={pig.id} className="flex items-center gap-2 cursor-pointer text-xs text-zinc-700">
                          <input
                            type="checkbox"
                            checked={selectedPigIds.includes(pig.id)}
                            onChange={() => {
                              // Farrowing: limit to 1 pig
                              if (selectedActivity.type === "Farrowing") {
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
              {selectedActivity.type === "Weaning" && selectedPigIds.length > 0 && (
                <div className="space-y-2.5 border-t border-zinc-100 pt-3">
                  <h4 className="text-xs font-bold text-zinc-700">Weaning Locations</h4>
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
              {selectedActivity.type === "Weight Check" && selectedPigIds.length > 0 && (
                <div className="space-y-2.5 border-t border-zinc-100 pt-3">
                  <h4 className="text-xs font-bold text-zinc-700">Weights (kg)</h4>
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
              {selectedActivity.type === "Culling" && cullingReason === "Sold" && selectedPigIds.length > 1 && (
                <div className="space-y-2.5 border-t border-zinc-100 pt-3">
                  <h4 className="text-xs font-bold text-zinc-700">Sale Prices ($)</h4>
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
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Description Notes</label>
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
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={selectedPigIds.length === 0}
                  className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition disabled:opacity-50"
                >
                  Log Action
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
