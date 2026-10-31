"use client";

import React, { useEffect, useState } from "react";
import { collection, doc, writeBatch, getDocs, query, where } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useTranslations } from "next-intl";
import { calculateAgeMonths } from "@/lib/swineGrowthDatabase";
import { Pig } from "@/lib/types";

const addDays = (dateStr: string, days: number) => {
  const result = new Date(dateStr);
  result.setDate(result.getDate() + days);
  return result.toISOString().split("T")[0];
};

interface ActivityModalProps {
  activity: any | null;
  onClose: () => void;
  pigs: Pig[];
  activeFarmUid: string | null;
  currencySymbol?: string;
  onSuccess?: () => void;
}

export default function ActivityModal({
  activity,
  onClose,
  pigs,
  activeFarmUid,
  currencySymbol = "$",
  onSuccess,
}: ActivityModalProps) {
  const t = useTranslations("Activities");
  const tCommon = useTranslations("Common");

  const [selectedPigIds, setSelectedPigIds] = useState<string[]>([]);
  const [logDate, setLogDate] = useState(new Date().toISOString().split("T")[0]);
  const [notes, setNotes] = useState("");

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

  const [pigWeaningLocations, setPigWeaningLocations] = useState<Record<string, string>>({});
  const [pigWeights, setPigWeights] = useState<Record<string, string>>({});
  const [pigSalePrices, setPigSalePrices] = useState<Record<string, string>>({});

  const [medicationName, setMedicationName] = useState("");
  const [medicationDosage, setMedicationDosage] = useState("");
  const [scheduleSecondIron, setScheduleSecondIron] = useState(false);
  const [showAllPigsForIron, setShowAllPigsForIron] = useState(false);
  const [withdrawalDays, setWithdrawalDays] = useState("0");
  const [withdrawalWarningPig, setWithdrawalWarningPig] = useState<Pig | null>(null);

  const [stillbornCount, setStillbornCount] = useState("0");
  const [mummiesCount, setMummiesCount] = useState("0");
  const [litterBirthWeight, setLitterBirthWeight] = useState("");

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
    setWithdrawalDays("0");
    setWithdrawalWarningPig(null);
    setStillbornCount("0");
    setMummiesCount("0");
    setLitterBirthWeight("");
    setScheduleSecondIron(false);
    setShowAllPigsForIron(false);
  }, [activity]);

  if (!activity) return null;

  const getFilteredPigs = () => {
    const key = activity.key;
    switch (key) {
      case "Heat Detection":
      case "Breeding/Mating":
        return pigs.filter(
          (p) =>
            p.gender?.toLowerCase() === "female" &&
            ["sow", "gilt", "pregnant", "lactating", "nursing", "finisher"].includes(p.status?.toLowerCase())
        );

      case "Confirm Pregnancy":
        return pigs.filter(
          (p) =>
            p.gender?.toLowerCase() === "female" &&
            ["sow", "gilt", "pregnant", "lactating", "nursing", "finisher"].includes(p.status?.toLowerCase()) &&
            p.lastBreedingDate
        );

      case "Farrowing":
        return pigs.filter(
          (p) => p.status?.toLowerCase() === "pregnant" && p.gender?.toLowerCase() === "female"
        );

      case "Weaning":
        return pigs.filter((p) => !p.weaned && p.status?.toLowerCase() !== "sow" && p.status?.toLowerCase() !== "boar");

      case "Castration":
        return pigs.filter(
          (p) => p.gender?.toLowerCase() === "male" && !p.castrated && calculateAgeMonths(p.birthDate) < 3
        );

      case "Teeth Clipping":
        return pigs.filter((p) => !p.teethClipped && calculateAgeMonths(p.birthDate) < 2);

      case "Tail Docking":
        return pigs.filter((p) => !p.tailDocked && calculateAgeMonths(p.birthDate) < 2);

      case "Iron Injection":
        if (showAllPigsForIron) return pigs;
        return pigs.filter((p) => calculateAgeMonths(p.birthDate) < 2);

      default:
        return pigs;
    }
  };

  const boarsList = pigs.filter((p) => p.gender?.toLowerCase() === "male" && p.status?.toLowerCase() === "boar");
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
    snapshot.docs.forEach((docSnap) => {
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
    const pigTags = pigIds.map((id) => pigs.find((p) => p.id === id)?.tagNumber || id);

    snapshot.docs.forEach((docSnap) => {
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
              const newName =
                remainingTags.length === 1
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

  const handleTogglePigSelection = (pigId: string) => {
    setSelectedPigIds((prev) =>
      prev.includes(pigId) ? prev.filter((id) => id !== pigId) : [...prev, pigId]
    );
  };

  const handleSelectAllFiltered = () => {
    if (selectedPigIds.length === filteredPigs.length) {
      setSelectedPigIds([]);
    } else {
      setSelectedPigIds(filteredPigs.map((p) => p.id));
    }
  };

  const handleLogActivity = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || selectedPigIds.length === 0) return;

    if (activity.key === "Culling") {
      const nowStr = new Date().toISOString().split("T")[0];
      const blockedPig = selectedPigIds
        .map((id) => pigs.find((p) => p.id === id))
        .find((p) => p && p.activeWithdrawalUntil && p.activeWithdrawalUntil >= nowStr);

      if (blockedPig) {
        setWithdrawalWarningPig(blockedPig);
        return;
      }
    }

    try {
      const batch = writeBatch(db);
      const activityType = activity.key;

      for (const pigId of selectedPigIds) {
        const pig = pigs.find((p) => p.id === pigId);
        if (!pig) continue;

        const isFuture = isFutureDate(logDate);
        const pigRef = doc(db, "users", activeFarmUid, "pigs", pigId);
        let finalDescription = notes;
        let taskId: string | null = null;

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
            completed: false,
          });
        }

        if (!isFuture) {
          if (activityType === "Breeding/Mating") {
            finalDescription = `${notes}\nMated Sow ${pig.tagNumber} with Boar ${boarTag}\nOutcome: ${matingOutcome}`.trim();
            if (pig.gender === "Female") {
              batch.update(pigRef, {
                lastBreedingDate: logDate,
                lastBoarTag: boarTag,
                purpose: "Breeder",
              });
              if (matingOutcome === "Successful") {
                const day110 = addDays(logDate, 110);
                const day114 = addDays(logDate, 114);
                batch.update(pigRef, {
                  status: "Pregnant",
                  expectedFarrowingDate: day114,
                  farrowingPenMoveDate: day110,
                });

                const tHeat = doc(collection(db, "users", activeFarmUid, "tasks"));
                batch.set(tHeat, {
                  id: tHeat.id,
                  name: `Check Return-to-Heat / Estrus: Pig ${pig.tagNumber}`,
                  date: addDays(logDate, 21),
                  notes: `Check if sow returns to heat 18-24 days post-mating on ${logDate}`,
                  pigIds: [pigId],
                  completed: false,
                });

                const tPen = doc(collection(db, "users", activeFarmUid, "tasks"));
                batch.set(tPen, {
                  id: tPen.id,
                  name: `Move to Farrowing Pen & Disinfect: Pig ${pig.tagNumber}`,
                  date: day110,
                  notes: `Sow ${pig.tagNumber} is due on ${day114}. Move to sanitized farrowing crate.`,
                  pigIds: [pigId],
                  completed: false,
                });

                const tFar = doc(collection(db, "users", activeFarmUid, "tasks"));
                batch.set(tFar, {
                  id: tFar.id,
                  name: `Expected Farrowing Date: Pig ${pig.tagNumber}`,
                  date: day114,
                  notes: `Expected farrowing date for Sow ${pig.tagNumber} (Boar: ${boarTag})`,
                  pigIds: [pigId],
                  completed: false,
                });
              }
            }
          } else if (activityType === "Confirm Pregnancy") {
            finalDescription = `${notes}\nPregnancy Check Outcome: ${pregnancyOutcome}`.trim();
            if (pregnancyOutcome === "Successful") {
              const sowBreedingDate = pig.lastBreedingDate || logDate;
              const day110 = addDays(sowBreedingDate, 110);
              const day114 = addDays(sowBreedingDate, 114);
              batch.update(pigRef, {
                status: "Pregnant",
                expectedFarrowingDate: day114,
                farrowingPenMoveDate: day110,
              });
            } else {
              batch.update(pigRef, {
                status: "Sow",
                expectedFarrowingDate: "",
                farrowingPenMoveDate: "",
                lastBreedingDate: "",
              });
            }
          } else if (activityType === "Farrowing") {
            const mCount = parseInt(numMales) || 0;
            const fCount = parseInt(numFemales) || 0;
            const sCount = parseInt(stillbornCount) || 0;
            const mumCount = parseInt(mummiesCount) || 0;
            const totalBornAlive = mCount + fCount;
            const totalBorn = totalBornAlive + sCount + mumCount;
            const birthWt = parseFloat(litterBirthWeight) || 0;

            finalDescription = `${notes}\nTotal Born: ${totalBorn} | Alive: ${totalBornAlive} (Males: ${mCount}, Females: ${fCount}) | Stillborn: ${sCount} | Mummies: ${mumCount}${
              birthWt > 0 ? ` | Litter Weight: ${birthWt} kg` : ""
            }`.trim();

            batch.update(pigRef, {
              status: "Lactating",
              parity: (pig.parity || 0) + 1,
              expectedFarrowingDate: "",
              farrowingPenMoveDate: "",
            });

            // Schedule Iron Shots on Day 3 and Day 14
            const tIron = doc(collection(db, "users", activeFarmUid, "tasks"));
            batch.set(tIron, {
              id: tIron.id,
              name: `Administer Iron Injection: Piglet Litter of Sow ${pig.tagNumber}`,
              date: addDays(logDate, 3),
              notes: `Inject 200mg Iron Dextran into piglet neck muscle at 3 days old`,
              pigIds: [pigId],
              completed: false,
            });

            const tWean = doc(collection(db, "users", activeFarmUid, "tasks"));
            batch.set(tWean, {
              id: tWean.id,
              name: `Wean Piglets & Dry Off: Sow ${pig.tagNumber}`,
              date: addDays(logDate, 28),
              notes: `Scheduled weaning 28 days post-farrowing. Move piglets to nursery/starter pen.`,
              pigIds: [pigId],
              completed: false,
            });
          } else if (activityType === "Weaning") {
            const newLoc = pigWeaningLocations[pigId] || pig.location || "";
            finalDescription = `${notes}\nWeaned on ${logDate}${newLoc ? ` to Pen: ${newLoc}` : ""}`.trim();
            batch.update(pigRef, {
              weaned: true,
              weaningDate: logDate,
              location: newLoc,
              status: "Starter",
            });
          } else if (activityType === "Castration") {
            batch.update(pigRef, { castrated: true });
          } else if (activityType === "Teeth Clipping") {
            batch.update(pigRef, { teethClipped: true });
          } else if (activityType === "Tail Docking") {
            batch.update(pigRef, { tailDocked: true });
          } else if (activityType === "Culling") {
            finalDescription = `${notes}\nCulling Reason: ${cullingReason}`.trim();
            batch.update(pigRef, {
              status: `Archived (${cullingReason})`,
              culledDate: logDate,
              culledReason: cullingReason,
            });

            if (cullingReason === "Sold") {
              const amount = parseFloat(pigSalePrices[pigId] || salePrice) || 0;
              if (amount > 0) {
                const finRef = doc(collection(db, "users", activeFarmUid, "financials"));
                batch.set(finRef, {
                  id: finRef.id,
                  type: "Income",
                  category: "Pig Sale",
                  amount,
                  date: logDate,
                  description: `Pig Sale: Tag ${pig.tagNumber}`,
                  pigId,
                  pigIds: [pigId],
                });
              }
            }
          }
        }

        const histRef = doc(collection(db, "users", activeFarmUid, "pigs", pigId, "history"));
        batch.set(histRef, {
          id: histRef.id,
          activityType: activityType === "Custom" ? customName || "Custom" : activityType,
          date: logDate,
          description: finalDescription,
          notes,
          isFuture,
          taskId: taskId || null,
        });

        if (["Medication", "Vaccination", "Deworming"].includes(activityType)) {
          const healthRef = doc(collection(db, "users", activeFarmUid, "pigs", pigId, "health_records"));
          batch.set(healthRef, {
            id: healthRef.id,
            type: activityType,
            date: logDate,
            description: finalDescription,
            medication: medicationName || activityType,
            dosage: medicationDosage || "",
          });
        }
      }

      await batch.commit();

      if (!isFutureDate(logDate)) {
        const cleanupBatch = writeBatch(db);
        for (const pId of selectedPigIds) {
          const pig = pigs.find((p) => p.id === pId);
          if (activity.key === "Culling") {
            await cleanupTasksForPig(cleanupBatch, pId, pig?.tagNumber || "");
          }
        }
        await autoCompleteUpcomingTasks(cleanupBatch, selectedPigIds, activity.key);
        await cleanupBatch.commit();
      }

      onClose();
      if (onSuccess) onSuccess();
    } catch (err) {
      console.error("Error logging activity:", err);
    }
  };

  const Icon = activity.icon;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in duration-200"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="bg-white dark:bg-[#1E1E1E] border border-cyan-200 dark:border-cyan-800 rounded-2xl w-full max-w-lg p-5 sm:p-6 space-y-5 shadow-2xl max-h-[90vh] overflow-y-auto no-scrollbar text-zinc-900 dark:text-zinc-100">
        <div className="flex items-center justify-between border-b border-cyan-100 dark:border-cyan-900/60 pb-3">
          <h3 className="text-lg font-bold text-cyan-950 dark:text-cyan-200 flex items-center gap-2.5">
            <span className="p-1.5 rounded-lg bg-cyan-100 dark:bg-cyan-950 text-cyan-700 dark:text-cyan-300">
              {Icon && <Icon className="h-5 w-5" />}
            </span>
            <span>{t("logActivity", { type: activity.type || activity.name })}</span>
          </h3>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition"
            aria-label="Close"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleLogActivity} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-zinc-600 dark:text-zinc-400 mb-1.5">
              {t("procedureDate")}
            </label>
            <input
              type="date"
              required
              value={logDate}
              onChange={(e) => setLogDate(e.target.value)}
              className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-3 py-2 text-sm text-zinc-900 dark:text-zinc-100 focus:outline-hidden focus:border-cyan-500 shadow-xs"
            />
          </div>

          {/* Breeding/Mating Dropdowns */}
          {activity.key === "Breeding/Mating" && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 dark:text-zinc-400 mb-1.5">
                    {t("chooseSow")}
                  </label>
                  <select
                    required
                    value={selectedPigIds[0] || ""}
                    onChange={(e) => setSelectedPigIds([e.target.value])}
                    className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-3 py-2 text-sm text-zinc-900 dark:text-zinc-100 focus:outline-hidden focus:border-cyan-500 shadow-xs"
                  >
                    <option value="">{t("selectSow")}</option>
                    {filteredPigs.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.tagNumber} ({p.status})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-zinc-600 dark:text-zinc-400 mb-1.5">
                    {t("chooseBoar")}
                  </label>
                  <select
                    required
                    value={boarTag}
                    onChange={(e) => setBoarTag(e.target.value)}
                    className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-3 py-2 text-sm text-zinc-900 dark:text-zinc-100 focus:outline-hidden focus:border-cyan-500 shadow-xs"
                  >
                    <option value="">{t("selectBoar")}</option>
                    {boarsList.map((p) => (
                      <option key={p.id} value={p.tagNumber}>
                        {p.tagNumber}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-600 dark:text-zinc-400 mb-1.5">
                  {t("matingOutcome")}
                </label>
                <select
                  value={matingOutcome}
                  onChange={(e) => setMatingOutcome(e.target.value)}
                  className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-3 py-2 text-sm text-zinc-900 dark:text-zinc-100 focus:outline-hidden focus:border-cyan-500 shadow-xs"
                >
                  <option value="Successful">{t("successful")}</option>
                  <option value="Unsuccessful">{t("unsuccessful")}</option>
                </select>
              </div>
            </div>
          )}

          {/* Confirm Pregnancy Dropdowns */}
          {activity.key === "Confirm Pregnancy" && (
            <div>
              <label className="block text-xs font-semibold text-zinc-600 dark:text-zinc-400 mb-1.5">
                {t("pregnancyOutcome")}
              </label>
              <select
                value={pregnancyOutcome}
                onChange={(e) => setPregnancyOutcome(e.target.value)}
                className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-3 py-2 text-sm text-zinc-900 dark:text-zinc-100 focus:outline-hidden focus:border-cyan-500 shadow-xs"
              >
                <option value="Successful">{t("confirmedPregnant")}</option>
                <option value="Unsuccessful">{t("notPregnant")}</option>
              </select>
            </div>
          )}

          {/* Farrowing Dropdowns */}
          {activity.key === "Farrowing" && (
            <div className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 dark:text-zinc-400 mb-1">
                    {t("numMales")}
                  </label>
                  <input
                    type="number"
                    min="0"
                    placeholder="0"
                    value={numMales}
                    onChange={(e) => setNumMales(e.target.value)}
                    className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-3 py-2 text-sm text-zinc-900 dark:text-zinc-100 focus:outline-hidden focus:border-cyan-500 shadow-xs"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 dark:text-zinc-400 mb-1">
                    {t("numFemales")}
                  </label>
                  <input
                    type="number"
                    min="0"
                    placeholder="0"
                    value={numFemales}
                    onChange={(e) => setNumFemales(e.target.value)}
                    className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-3 py-2 text-sm text-zinc-900 dark:text-zinc-100 focus:outline-hidden focus:border-cyan-500 shadow-xs"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 dark:text-zinc-400 mb-1">
                    {t("stillbornCount") || "Stillborn"}
                  </label>
                  <input
                    type="number"
                    min="0"
                    value={stillbornCount}
                    onChange={(e) => setStillbornCount(e.target.value)}
                    className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-3 py-2 text-sm text-zinc-900 dark:text-zinc-100 focus:outline-hidden focus:border-cyan-500 shadow-xs"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 dark:text-zinc-400 mb-1">
                    {t("mummiesCount") || "Mummified"}
                  </label>
                  <input
                    type="number"
                    min="0"
                    value={mummiesCount}
                    onChange={(e) => setMummiesCount(e.target.value)}
                    className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-3 py-2 text-sm text-zinc-900 dark:text-zinc-100 focus:outline-hidden focus:border-cyan-500 shadow-xs"
                  />
                </div>
              </div>
            </div>
          )}

          {/* Target Pigs List for activities other than Breeding/Mating */}
          {activity.key !== "Breeding/Mating" && (
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-semibold text-zinc-600 dark:text-zinc-400">
                  {t("selectTargetPigs", { count: selectedPigIds.length })}
                </label>
                {filteredPigs.length > 1 && activity.key !== "Farrowing" && (
                  <button
                    type="button"
                    onClick={handleSelectAllFiltered}
                    className="text-xs font-bold text-cyan-600 hover:text-cyan-700"
                  >
                    {selectedPigIds.length === filteredPigs.length ? tCommon("cancel") : t("selectAll") || "Select All"}
                  </button>
                )}
              </div>
              <div className="border border-zinc-200 dark:border-zinc-700 rounded-xl max-h-36 overflow-y-auto p-2.5 space-y-1.5 bg-zinc-50/50 dark:bg-zinc-800/40">
                {filteredPigs.length === 0 ? (
                  <p className="text-xs text-zinc-400 py-3 text-center">{t("noEligiblePigs")}</p>
                ) : (
                  filteredPigs.map((pig) => (
                    <label
                      key={pig.id}
                      className="flex items-center gap-2 p-1.5 rounded-lg hover:bg-white dark:hover:bg-zinc-700/60 cursor-pointer text-xs"
                    >
                      <input
                        type={activity.key === "Farrowing" ? "radio" : "checkbox"}
                        name="activity-pig-select"
                        checked={selectedPigIds.includes(pig.id)}
                        onChange={() => {
                          if (activity.key === "Farrowing") {
                            setSelectedPigIds([pig.id]);
                          } else {
                            handleTogglePigSelection(pig.id);
                          }
                        }}
                        className="rounded text-cyan-600 focus:ring-cyan-500"
                      />
                      <span className="font-semibold text-zinc-800 dark:text-zinc-200">
                        {pig.tagNumber} ({pig.status})
                      </span>
                    </label>
                  ))
                )}
              </div>
            </div>
          )}

          {/* Notes */}
          <div>
            <label className="block text-xs font-semibold text-zinc-600 dark:text-zinc-400 mb-1.5">
              {t("notes") || "Notes"}
            </label>
            <textarea
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Add details, medication batch numbers, or instructions..."
              className="w-full rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-3 py-2 text-sm text-zinc-900 dark:text-zinc-100 focus:outline-hidden focus:border-cyan-500 shadow-xs"
            />
          </div>

          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-zinc-100 dark:border-zinc-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-bold text-zinc-600 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-xl transition"
            >
              {tCommon("cancel") || "Cancel"}
            </button>
            <button
              type="submit"
              disabled={selectedPigIds.length === 0}
              className="px-5 py-2 text-xs font-bold text-white bg-cyan-600 hover:bg-cyan-700 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl shadow-xs transition"
            >
              {t("confirmAndLog") || "Save & Log"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
