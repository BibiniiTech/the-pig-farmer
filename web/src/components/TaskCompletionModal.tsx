"use client";

import React, { useState, useEffect } from "react";
import { doc, collection, writeBatch, getDoc, getDocs, query, where } from "firebase/firestore";
import { db } from "@/lib/firebase";
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
  CullingIcon
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

interface TaskItem {
  id: string;
  name: string;
  date: string;
  notes: string;
  pigIds: string[];
  completed?: boolean;
}

interface TaskCompletionModalProps {
  isOpen: boolean;
  onClose: () => void;
  tasksToEdit: TaskItem[];
  allPigs: Pig[];
  activeFarmUid: string;
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

export default function TaskCompletionModal({
  isOpen,
  onClose,
  tasksToEdit,
  allPigs,
  activeFarmUid
}: TaskCompletionModalProps) {
  const [logDate, setLogDate] = useState(new Date().toISOString().split("T")[0]);
  const [selectedPigIds, setSelectedPigIds] = useState<string[]>([]);
  const [notes, setNotes] = useState("");

  // Medication / vaccine fields
  const [medicationName, setMedicationName] = useState("");
  const [medicationDosage, setMedicationDosage] = useState("");
  const [scheduleSecondIron, setScheduleSecondIron] = useState(false);

  // Breeding outcomes
  const [matingOutcome, setMatingOutcome] = useState("Successful");
  const [pregnancyOutcome, setPregnancyOutcome] = useState("Successful");
  const [heatOutcome, setHeatOutcome] = useState("Heat Detected");

  // Culling
  const [cullingReason, setCullingReason] = useState("Sold");
  const [salePrice, setSalePrice] = useState("");

  // Farrowing litters
  const [numMales, setNumMales] = useState("");
  const [numFemales, setNumFemales] = useState("");
  const [maleTags, setMaleTags] = useState("");
  const [femaleTags, setFemaleTags] = useState("");

  // Weaning locations & Weights maps
  const [pigWeaningLocations, setPigWeaningLocations] = useState<Record<string, string>>({});
  const [pigWeights, setPigWeights] = useState<Record<string, string>>({});
  const [pigSalePrices, setPigSalePrices] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const firstTask = tasksToEdit[0];
  const activityName = firstTask ? (firstTask.name.split(":")[0] || "").trim() : "";

  // Reset inputs when tasks change
  useEffect(() => {
    if (!firstTask) return;
    setLogDate(firstTask.date || new Date().toISOString().split("T")[0]);
    setNotes("");
    setMedicationName("");
    setMedicationDosage("");
    setScheduleSecondIron(false);
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

    // Resolve pig identifiers from task names
    const resolvedIds: string[] = [];
    tasksToEdit.forEach(task => {
      const parts = (task.name || "").split(":");
      if (parts.length > 1) {
        const identifier = parts[1].replace(/pigs/i, "").replace(/pig/i, "").trim();
        if (identifier) {
          const splitTags = identifier.split(",").map(t => t.trim());
          splitTags.forEach(tag => {
            const pig = allPigs.find(p => p.id === tag || p.tagNumber === tag);
            if (pig) {
              resolvedIds.push(pig.id);
            } else if (tag && tag !== "General") {
              resolvedIds.push(tag);
            }
          });
        }
      }
      if (task.pigIds && task.pigIds.length > 0) {
        task.pigIds.forEach(id => {
          if (!resolvedIds.includes(id)) {
            resolvedIds.push(id);
          }
        });
      }
    });

    // Filter out male pigs for female-only tasks
    const isFemaleSpecific = ["heat", "breeding", "mating", "pregnancy", "farrowing"].some(k =>
      activityName.toLowerCase().includes(k)
    );
    const finalIds = isFemaleSpecific
      ? resolvedIds.filter(id => {
          const pig = allPigs.find(p => p.id === id);
          return !pig || pig.gender?.toLowerCase() === "female";
        })
      : resolvedIds;

    setSelectedPigIds(Array.from(new Set(finalIds)));
  }, [tasksToEdit, allPigs, activityName, firstTask]);

  if (!isOpen || !firstTask) return null;

  const isHeatDetection = activityName.toLowerCase().includes("heat");
  const isBreeding = activityName.toLowerCase().includes("breeding") || activityName.toLowerCase().includes("mating");
  const isPregnancyCheck = activityName.toLowerCase().includes("pregnancy");
  const isFarrowing = activityName.toLowerCase().includes("farrowing");
  const isWeaning = activityName.toLowerCase().includes("weaning");
  const isWeightCheck = activityName.toLowerCase().includes("weight");
  const isCulling = activityName.toLowerCase().includes("culling");
  const isMedicationActivity = [
    "iron",
    "deworming",
    "vaccination",
    "medication"
  ].some(k => activityName.toLowerCase().includes(k));

  const renderIcon = () => {
    const props = { className: "h-6 w-6 text-emerald-600" };
    if (isHeatDetection) return <HeatIcon {...props} />;
    if (isBreeding) return <BreedingIcon {...props} />;
    if (isPregnancyCheck) return <PregnancyCheckIcon {...props} />;
    if (isFarrowing) return <FarrowingIcon {...props} />;
    if (isWeaning) return <WeaningIcon {...props} />;
    if (isCastration()) return <CastrationIcon {...props} />;
    if (isTeethClipping()) return <TeethClippingIcon {...props} />;
    if (isTailDocking()) return <TailDockingIcon {...props} />;
    if (isCulling) return <CullingIcon {...props} />;
    if (isWeightCheck) return <WeightCheckerIcon {...props} />;
    if (activityName.toLowerCase().includes("iron")) return <IronIcon {...props} />;
    if (activityName.toLowerCase().includes("deworming")) return <DewormingIcon {...props} />;
    if (activityName.toLowerCase().includes("vaccin")) return <VaccinationIcon {...props} />;
    return <MedicationIcon {...props} />;
  };

  function isCastration() { return activityName.toLowerCase().includes("castration"); }
  function isTeethClipping() { return activityName.toLowerCase().includes("teeth"); }
  function isTailDocking() { return activityName.toLowerCase().includes("tail"); }

  // Clean up tasks for culling
  const cleanupTasksForPig = async (batch: any, pigId: string, pigTag: string) => {
    const tasksRef = collection(db, "users", activeFarmUid, "tasks");
    const q = query(tasksRef, where("pigIds", "array-contains", pigId));
    const snapshot = await getDocs(q);
    snapshot.docs.forEach(docSnap => {
      const task = docSnap.data() as TaskItem;
      const pigIds = task.pigIds || [];
      if (pigIds.length <= 1) {
        batch.delete(docSnap.ref);
      } else {
        const newPigIds = pigIds.filter(id => id !== pigId);
        let newName = task.name;
        if (pigTag) {
          newName = task.name.replace(pigTag, "").replace(/,\s*,/g, ",").trim().replace(/^,|,$/g, "");
        }
        batch.update(docSnap.ref, { pigIds: newPigIds, name: newName });
      }
    });
  };

  // Auto-complete outstanding tasks
  const autoCompleteUpcomingTasks = async (batch: any, pigIds: string[]) => {
    const tasksRef = collection(db, "users", activeFarmUid, "tasks");
    const q = query(tasksRef, where("completed", "==", false));
    const snapshot = await getDocs(q);
    const pigTags = pigIds.map(id => allPigs.find(p => p.id === id)?.tagNumber || id);

    snapshot.docs.forEach(docSnap => {
      const task = docSnap.data() as TaskItem;
      // Skip completed tasks or tasks that are currently being edited
      if (tasksToEdit.some(t => t.id === docSnap.id)) return;

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
          .map(t => t.trim())
          .filter(t => t.length > 0);

        if (tagsInTask.length === 0 && taskPigPart === "") {
          batch.update(docSnap.ref, { completed: true });
        } else {
          const matchingTags = tagsInTask.filter(t => pigTags.includes(t));
          if (matchingTags.length > 0) {
            if (matchingTags.length === tagsInTask.length) {
              batch.update(docSnap.ref, { completed: true });
            } else {
              const remainingTags = tagsInTask.filter(t => !matchingTags.includes(t));
              const newName = remainingTags.length === 1 
                ? `${taskActivity}: Pig ${remainingTags[0]}` 
                : `${taskActivity}: Pigs ${remainingTags.join(", ")}`;
              const remainingPigIds = (task.pigIds || []).filter(id => !pigIds.includes(id));
              batch.update(docSnap.ref, { name: newName, pigIds: remainingPigIds });
            }
          }
        }
      }
    });
  };

  const handleCompleteTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedPigIds.length === 0) return;
    setIsSubmitting(true);

    try {
      const batch = writeBatch(db);

      for (const pigId of selectedPigIds) {
        const pig = allPigs.find(p => p.id === pigId);
        if (!pig) continue;

        const pigRef = doc(db, "users", activeFarmUid, "pigs", pigId);
        const healthRecordsRef = doc(collection(db, "users", activeFarmUid, "pigs", pigId, "health_records"));
        let finalDescription = notes;

        // Breeding/Mating Logic
        if (isBreeding) {
          const taskName = firstTask.name || "";
          const sowTag = taskName.includes("Sow ") ? taskName.split("Sow ")[1].split(",")[0].trim() : pig.tagNumber;
          const boarTag = taskName.includes("Boar ") ? taskName.split("Boar ")[1].trim() : "Boar";
          
          finalDescription = `${notes}\nMated Sow ${sowTag} with Boar ${boarTag}\nOutcome: ${matingOutcome}`.trim();
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
        if (isHeatDetection) {
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
        if (isPregnancyCheck) {
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
        if (isFarrowing) {
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
        if (isWeaning) {
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
        if (isCastration() && pig.gender === "Male") {
          batch.update(pigRef, { castrated: true, castrationDate: logDate });
        }

        // Teeth Clipping
        if (isTeethClipping()) {
          batch.update(pigRef, { teethClipped: true });
        }

        // Tail Docking
        if (isTailDocking()) {
          batch.update(pigRef, { tailDocked: true });
        }

        // Iron Injection / Medication
        if (isMedicationActivity) {
          finalDescription = `${notes}\nMedication/Vaccine: ${medicationName || activityName}, Dosage: ${medicationDosage || "N/A"}`.trim();
          if (activityName.toLowerCase().includes("iron")) {
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
        if (isWeightCheck) {
          const pigWeight = parseFloat(pigWeights[pigId]) || 0;
          if (pigWeight > 0) {
            batch.update(pigRef, { weight: pigWeight });
            finalDescription = `${notes}\nWeight updated to: ${pigWeight} kg`.trim();
          }
        }

        // Culling
        if (isCulling) {
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

        // Save procedure health record
        const actName = activityName;
        const hrRef = isCulling 
          ? doc(collection(db, "users", activeFarmUid, "archived_pigs", pigId, "health_records"))
          : doc(collection(db, "users", activeFarmUid, "pigs", pigId, "health_records"));
        
        batch.set(hrRef, {
          id: hrRef.id,
          date: logDate,
          type: actName,
          description: finalDescription
        });
      }

      // Mark tasks as completed
      tasksToEdit.forEach(task => {
        const docRef = doc(db, "users", activeFarmUid, "tasks", task.id);
        const taskPigIds = task.pigIds || [];
        const incompletePigIds = taskPigIds.filter(id => !selectedPigIds.includes(id));
        
        if (incompletePigIds.length === 0) {
          batch.update(docRef, { completed: true });
        } else {
          // Partial task completion
          const remainingTags = incompletePigIds.map(id => allPigs.find(p => p.id === id)?.tagNumber || id);
          const newName = remainingTags.length === 1 
            ? `${activityName}: Pig ${remainingTags[0]}` 
            : `${activityName}: Pigs ${remainingTags.join(", ")}`;
          batch.update(docRef, { pigIds: incompletePigIds, name: newName });
        }
      });

      // Task auto-completion
      await autoCompleteUpcomingTasks(batch, selectedPigIds);

      await batch.commit();
      onClose();
    } catch (err) {
      console.error("Error committing task completion batch:", err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleTogglePigSelection = (id: string) => {
    setSelectedPigIds(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-lg p-6 space-y-6 shadow-2xl max-h-[90vh] overflow-y-auto no-scrollbar">
        <h3 className="text-lg font-bold text-zinc-900 flex items-center gap-2 border-b border-zinc-100 pb-3">
          {renderIcon()}
          <span>Log Activity: {activityName}</span>
        </h3>

        <form onSubmit={handleCompleteTask} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Date Completed</label>
            <input
              type="date"
              required
              value={logDate}
              onChange={(e) => setLogDate(e.target.value)}
              className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
            />
          </div>

          {/* Activity-specific options */}
          {isHeatDetection && (
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

          {isBreeding && (
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
          )}

          {isPregnancyCheck && (
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

          {isFarrowing && (
            <div className="space-y-4">
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

          {isMedicationActivity && (
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Medication Name</label>
                <input
                  type="text"
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
                  value={medicationDosage}
                  onChange={(e) => setMedicationDosage(e.target.value)}
                  placeholder="e.g. 2 ml"
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                />
              </div>
              {activityName.toLowerCase().includes("iron") && selectedPigIds.every(id => {
                const p = allPigs.find(pig => pig.id === id);
                return !p || (p.ironInjections || 0) === 0;
              }) && (
                <div className="col-span-2">
                  <label className="flex items-center gap-2 cursor-pointer text-sm font-semibold">
                    <input
                      type="checkbox"
                      checked={scheduleSecondIron}
                      onChange={(e) => setScheduleSecondIron(e.target.checked)}
                      className="h-4 w-4 rounded border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                    />
                    <span>Schedule 2nd Iron Injection (7 days later)?</span>
                  </label>
                </div>
              )}
            </div>
          )}

          {isCulling && (
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
              {cullingReason === "Sold" && (
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

          {/* Checklist of Pigs */}
          <div>
            <label className="block text-xs font-semibold text-zinc-500 mb-1.5">
              Select Affected Pigs ({selectedPigIds.length})
            </label>
            <div className="border border-zinc-200 rounded-lg max-h-36 overflow-y-auto p-3 space-y-2 bg-zinc-50/50">
              {selectedPigIds.length === 0 ? (
                <p className="text-xs text-zinc-400 italic">No targets loaded.</p>
              ) : (
                selectedPigIds.map(id => {
                  const pig = allPigs.find(p => p.id === id);
                  return (
                    <div key={id} className="flex items-center justify-between text-xs text-zinc-700">
                      <label className="flex items-center gap-2 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={selectedPigIds.includes(id)}
                          onChange={() => handleTogglePigSelection(id)}
                          className="h-4 w-4 rounded border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                        />
                        <span>{pig?.tagNumber || id} ({pig?.status || "Unknown"})</span>
                      </label>

                      {/* Weaning pen per pig */}
                      {isWeaning && selectedPigIds.includes(id) && (
                        <input
                          type="text"
                          placeholder="Pen Location"
                          value={pigWeaningLocations[id] || ""}
                          onChange={(e) => setPigWeaningLocations(prev => ({ ...prev, [id]: e.target.value }))}
                          className="rounded border border-zinc-200 px-2 py-0.5 text-[11px] w-24 outline-none"
                        />
                      )}

                      {/* Weight check per pig */}
                      {isWeightCheck && selectedPigIds.includes(id) && (
                        <input
                          type="number"
                          step="any"
                          placeholder="Weight (kg)"
                          value={pigWeights[id] || ""}
                          onChange={(e) => setPigWeights(prev => ({ ...prev, [id]: e.target.value }))}
                          className="rounded border border-zinc-200 px-2 py-0.5 text-[11px] w-24 outline-none"
                        />
                      )}

                      {/* Sale price per pig */}
                      {isCulling && cullingReason === "Sold" && selectedPigIds.includes(id) && (
                        <input
                          type="number"
                          step="any"
                          placeholder="Price ($)"
                          value={pigSalePrices[id] || ""}
                          onChange={(e) => setPigSalePrices(prev => ({ ...prev, [id]: e.target.value }))}
                          className="rounded border border-zinc-200 px-2 py-0.5 text-[11px] w-24 outline-none"
                        />
                      )}
                    </div>
                  );
                })
              )}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Procedure Notes</label>
            <textarea
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="e.g. completed successfully"
              className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
            />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={selectedPigIds.length === 0 || isSubmitting}
              className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition disabled:opacity-50 flex items-center gap-1.5"
            >
              {isSubmitting && (
                <div className="h-3 w-3 animate-spin rounded-full border-2 border-white border-t-transparent" />
              )}
              <span>Complete Task</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
