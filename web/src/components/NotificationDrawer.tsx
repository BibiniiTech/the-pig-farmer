"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { doc, updateDoc, deleteDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { Pig, TaskItem } from "@/lib/types";
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
import { FeedManagementIcon } from "@/components/icons/DashboardIcons";

interface TaskGroupItem {
  activity: string;
  target: string;
  date: string;
  isOverdue: boolean;
  originalTasks: TaskItem[];
}

interface FeedStockAlertItem {
  id: string;
  name: string;
  quantity: number;
  minThreshold: number;
  unit: string;
}

interface NotificationDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  groupedTasks: TaskGroupItem[];
  weightAlerts: Pig[];
  stockAlerts: FeedStockAlertItem[];
  onSelectTaskGroup?: (tasks: TaskItem[]) => void;
  onSelectWeightAlert?: (pig: Pig) => void;
  onSnoozeTasks?: (tasks: TaskItem[], hours: number) => void;
  onDeleteTasks?: (tasks: TaskItem[]) => void;
  onSnoozeWeightAlert?: (pigId: string, days: number) => void;
  onDismissWeightAlert?: (pigId: string) => void;
  onSnoozeStockAlert?: (itemId: string, hours: number) => void;
  onDismissStockAlert?: (itemId: string) => void;
}

const ClockIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <circle cx="12" cy="12" r="10" />
    <polyline points="12 6 12 12 16 14" />
  </svg>
);

const TrashIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <polyline points="3 6 5 6 21 6" />
    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
  </svg>
);

export default function NotificationDrawer({
  isOpen,
  onClose,
  groupedTasks = [],
  weightAlerts = [],
  stockAlerts = [],
  onSelectTaskGroup,
  onSelectWeightAlert,
  onSnoozeTasks,
  onDeleteTasks,
  onSnoozeWeightAlert,
  onDismissWeightAlert,
  onSnoozeStockAlert,
  onDismissStockAlert,
}: NotificationDrawerProps) {
  const t = useTranslations("Dashboard");
  const tNotif = useTranslations("Notifications");
  const tAct = useTranslations("Activities");
  const { activeFarmUid } = useAuth();
  const router = useRouter();

  const [openSnoozeId, setOpenSnoozeId] = useState<string | null>(null);
  const [deleteConfirmTarget, setDeleteConfirmTarget] = useState<{
    title: string;
    description: string;
    onConfirm: () => void;
  } | null>(null);

  const [localDismissedTaskIds, setLocalDismissedTaskIds] = useState<Set<string>>(new Set());
  const [localSnoozedTaskIds, setLocalSnoozedTaskIds] = useState<Set<string>>(new Set());

  const [localDismissedWeights, setLocalDismissedWeights] = useState<Set<string>>(() => {
    try {
      if (typeof window !== "undefined") {
        return new Set(JSON.parse(localStorage.getItem("ss_dismissed_weight") || "[]"));
      }
    } catch {}
    return new Set();
  });
  const [localSnoozedWeights, setLocalSnoozedWeights] = useState<Record<string, number>>(() => {
    try {
      if (typeof window !== "undefined") {
        return JSON.parse(localStorage.getItem("ss_snoozed_weight") || "{}");
      }
    } catch {}
    return {};
  });

  const [localDismissedStocks, setLocalDismissedStocks] = useState<Set<string>>(() => {
    try {
      if (typeof window !== "undefined") {
        return new Set(JSON.parse(localStorage.getItem("ss_dismissed_stock") || "[]"));
      }
    } catch {}
    return new Set();
  });
  const [localSnoozedStocks, setLocalSnoozedStocks] = useState<Record<string, number>>(() => {
    try {
      if (typeof window !== "undefined") {
        return JSON.parse(localStorage.getItem("ss_snoozed_stock") || "{}");
      }
    } catch {}
    return {};
  });

  if (!isOpen) return null;

  const now = Date.now();
  const visibleTasks = groupedTasks.filter(
    (g) => !g.originalTasks.some((t) => localDismissedTaskIds.has(t.id) || localSnoozedTaskIds.has(t.id))
  );
  const visibleWeights = weightAlerts.filter(
    (p) => !localDismissedWeights.has(p.id) && (!localSnoozedWeights[p.id] || localSnoozedWeights[p.id] <= now)
  );
  const visibleStocks = stockAlerts.filter(
    (s) => !localDismissedStocks.has(s.id) && (!localSnoozedStocks[s.id] || localSnoozedStocks[s.id] <= now)
  );

  const totalCount = visibleTasks.length + visibleWeights.length + visibleStocks.length;

  const handleSnoozeTasks = async (tasks: TaskItem[], hours: number) => {
    setOpenSnoozeId(null);
    setLocalSnoozedTaskIds((prev) => {
      const next = new Set(prev);
      tasks.forEach((t) => next.add(t.id));
      return next;
    });

    if (onSnoozeTasks) {
      onSnoozeTasks(tasks, hours);
      return;
    }

    if (!activeFarmUid) return;
    const snoozeUntil = Date.now() + hours * 3600 * 1000;
    for (const t of tasks) {
      if (t.id) {
        try {
          await updateDoc(doc(db, "users", activeFarmUid, "tasks", t.id), { snoozeUntil });
        } catch (e) {
          console.error("Failed to snooze task", e);
        }
      }
    }
  };

  const handleDeleteTasks = async (tasks: TaskItem[]) => {
    setOpenSnoozeId(null);
    setLocalDismissedTaskIds((prev) => {
      const next = new Set(prev);
      tasks.forEach((t) => next.add(t.id));
      return next;
    });

    if (onDeleteTasks) {
      onDeleteTasks(tasks);
      return;
    }

    if (!activeFarmUid) return;
    for (const t of tasks) {
      if (t.id) {
        try {
          await deleteDoc(doc(db, "users", activeFarmUid, "tasks", t.id));
        } catch (e) {
          console.error("Failed to delete task", e);
        }
      }
    }
  };

  const handleSnoozeWeight = (pigId: string, days: number) => {
    setOpenSnoozeId(null);
    const until = Date.now() + days * 24 * 3600 * 1000;
    const updated = { ...localSnoozedWeights, [pigId]: until };
    setLocalSnoozedWeights(updated);
    try {
      localStorage.setItem("ss_snoozed_weight", JSON.stringify(updated));
    } catch {}

    if (onSnoozeWeightAlert) {
      onSnoozeWeightAlert(pigId, days);
    }
  };

  const handleDismissWeight = (pigId: string) => {
    setOpenSnoozeId(null);
    const updated = new Set(localDismissedWeights);
    updated.add(pigId);
    setLocalDismissedWeights(updated);
    try {
      localStorage.setItem("ss_dismissed_weight", JSON.stringify(Array.from(updated)));
    } catch {}

    if (onDismissWeightAlert) {
      onDismissWeightAlert(pigId);
    }
  };

  const handleSnoozeStock = (itemId: string, hours: number) => {
    setOpenSnoozeId(null);
    const until = Date.now() + hours * 3600 * 1000;
    const updated = { ...localSnoozedStocks, [itemId]: until };
    setLocalSnoozedStocks(updated);
    try {
      localStorage.setItem("ss_snoozed_stock", JSON.stringify(updated));
    } catch {}

    if (onSnoozeStockAlert) {
      onSnoozeStockAlert(itemId, hours);
    }
  };

  const handleDismissStock = (itemId: string) => {
    setOpenSnoozeId(null);
    const updated = new Set(localDismissedStocks);
    updated.add(itemId);
    setLocalDismissedStocks(updated);
    try {
      localStorage.setItem("ss_dismissed_stock", JSON.stringify(Array.from(updated)));
    } catch {}

    if (onDismissStockAlert) {
      onDismissStockAlert(itemId);
    }
  };

  const handleWeightAlertClick = (pig: Pig) => {
    onClose();
    if (onSelectWeightAlert) {
      onSelectWeightAlert(pig);
    } else {
      router.push(`/dashboard?section=weight&subOption=tape&tag=${encodeURIComponent(pig.tagNumber || pig.id)}`);
    }
  };

  const getActivityIcon = (activityName: string) => {
    const act = activityName.toLowerCase();
    if (act.includes("heat")) return HeatIcon;
    if (act.includes("breeding") || act.includes("mating")) return BreedingIcon;
    if (act.includes("pregnancy") || act.includes("confirm")) return PregnancyCheckIcon;
    if (act.includes("farrowing")) return FarrowingIcon;
    if (act.includes("weaning")) return WeaningIcon;
    if (act.includes("castration")) return CastrationIcon;
    if (act.includes("teeth")) return TeethClippingIcon;
    if (act.includes("tail")) return TailDockingIcon;
    if (act.includes("deworming")) return DewormingIcon;
    if (act.includes("iron")) return IronIcon;
    if (act.includes("vaccination")) return VaccinationIcon;
    if (act.includes("medication")) return MedicationIcon;
    if (act.includes("weight")) return WeightCheckerIcon;
    if (act.includes("culling")) return CullingIcon;
    return FeedManagementIcon;
  };

  const formatTarget = (target: string) => {
    if (!target || target.toLowerCase() === "general") return tNotif("general") || "General Task";
    if (target.toLowerCase().startsWith("pigs ")) {
      return `${tNotif("pigs") || "Pigs"} ${target.substring(5)}`;
    }
    if (target.toLowerCase().startsWith("pig ")) {
      return `${tNotif("pig") || "Pig"} ${target.substring(4)}`;
    }
    if (target.includes(",")) {
      return `${tNotif("pigs") || "Pigs"} ${target}`;
    }
    return `${tNotif("pig") || "Pig"} ${target}`;
  };

  const getActivityTitle = (activityName: string) => {
    const act = activityName.toLowerCase();
    try {
      if (act.includes("heat")) return tAct("categories.heat.type");
      if (act.includes("breeding") || act.includes("mating")) return tAct("categories.breeding.type");
      if (act.includes("pregnancy") || act.includes("confirm")) return tAct("categories.pregnancy.type");
      if (act.includes("farrowing")) return tAct("categories.farrowing.type");
      if (act.includes("weaning")) return tAct("categories.weaning.type");
      if (act.includes("castration")) return tAct("categories.castration.type");
      if (act.includes("teeth")) return tAct("categories.teeth.type");
      if (act.includes("tail")) return tAct("categories.tail.type");
      if (act.includes("deworming")) return tAct("categories.deworming.type");
      if (act.includes("iron")) return tAct("categories.iron.type");
      if (act.includes("vaccination")) return tAct("categories.vaccination.type");
      if (act.includes("medication")) return tAct("categories.medication.type");
      if (act.includes("weight")) return tAct("categories.weight.type");
      if (act.includes("culling")) return tAct("categories.culling.type");
    } catch {}
    return activityName;
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/60 backdrop-blur-sm transition-opacity animate-in fade-in duration-300"
        onClick={onClose}
      />

      {/* Slide-Over Drawer Sheet */}
      <div className="fixed inset-y-0 right-0 max-w-full flex">
        <div className="w-screen max-w-md bg-white shadow-2xl flex flex-col animate-in slide-in-from-right duration-300 border-l border-zinc-200">
          
          {/* ─── HEADER ────────────────────────────────────────────── */}
          <div className="p-5 border-b border-zinc-150 flex items-center justify-between bg-zinc-50/80">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-xl bg-emerald-50 border border-emerald-200/60 text-emerald-700 flex items-center justify-center shrink-0">
                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
              </div>
              <div>
                <h2 className="text-base font-bold text-zinc-900">{tNotif("title")}</h2>
                <p className="text-xs text-zinc-400 font-medium">{totalCount} {tNotif("notifications") || "Notifications"}</p>
              </div>
            </div>

            <button
              onClick={onClose}
              className="p-2 rounded-xl text-zinc-400 hover:text-zinc-700 hover:bg-zinc-150 transition-colors"
              aria-label="Close notification drawer"
            >
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {/* ─── NOTIFICATION CONTENT LIST ─────────────────────────── */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4 custom-scrollbar">
            {totalCount === 0 ? (
              <div className="flex flex-col items-center justify-center h-64 text-center p-6">
                <div className="h-16 w-16 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center mb-3">
                  <svg className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                  </svg>
                </div>
                <p className="text-sm font-bold text-zinc-800">{tNotif("allCaughtUp")}</p>
                <p className="text-xs text-zinc-400 mt-1">{tNotif("noPendingNotifications")}</p>
              </div>
            ) : (
              <>
                {/* 1. Procedures & Events */}
                {visibleTasks.length > 0 && (
                  <div className="space-y-2.5">
                    <h3 className="text-[11px] font-black text-emerald-800 uppercase tracking-wider px-1">
                      {tNotif("herdProceduresTasks")} ({visibleTasks.length})
                    </h3>
                    {visibleTasks.map((taskGroup, idx) => {
                      const IconComp = getActivityIcon(taskGroup.activity);
                      const snoozeKey = `task-${idx}`;
                      const isSnoozeOpen = openSnoozeId === snoozeKey;
                      const activityDisplay = getActivityTitle(taskGroup.activity);

                      return (
                        <div
                          key={snoozeKey}
                          className={`p-3.5 rounded-2xl border transition-all flex flex-col gap-2.5 relative ${
                            taskGroup.isOverdue
                              ? "bg-red-50/70 border-red-200"
                              : "bg-zinc-50 border-zinc-200"
                          }`}
                        >
                          <div
                            onClick={() => onSelectTaskGroup?.(taskGroup.originalTasks)}
                            className="flex items-center justify-between cursor-pointer group min-w-0"
                          >
                            <div className="flex items-center gap-3 min-w-0">
                              <div className={`h-10 w-10 rounded-xl flex items-center justify-center shrink-0 ${
                                taskGroup.isOverdue ? "bg-red-100 text-red-700" : "bg-emerald-100 text-emerald-800"
                              }`}>
                                <IconComp className="h-5 w-5" />
                              </div>
                              <div className="min-w-0">
                                <h4 className="font-bold text-xs sm:text-sm text-zinc-900 truncate">
                                  {activityDisplay}
                                </h4>
                                <p className={`text-xs truncate ${taskGroup.isOverdue ? "text-red-700 font-medium" : "text-zinc-500"}`}>
                                  {formatTarget(taskGroup.target)}
                                </p>
                              </div>
                            </div>

                            <div className="flex items-center gap-2 shrink-0 ml-3">
                              <span className={`text-[10px] font-black px-2 py-0.5 rounded-md ${
                                taskGroup.isOverdue ? "bg-red-200 text-red-900" : "bg-zinc-200 text-zinc-700"
                              }`}>
                                {taskGroup.isOverdue ? (tNotif("overdue") || "OVERDUE").toUpperCase() : `${tNotif("due") || "Due"} ${taskGroup.date}`}
                              </span>
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  onSelectTaskGroup?.(taskGroup.originalTasks);
                                }}
                                className={`px-2.5 py-1 rounded-lg text-xs font-bold text-white shadow-xs transition active:scale-95 ${
                                  taskGroup.isOverdue ? "bg-red-600 hover:bg-red-700" : "bg-emerald-600 hover:bg-emerald-700"
                                }`}
                              >
                                {tNotif("record") || "Record"}
                              </button>
                            </div>
                          </div>

                          {/* Action Buttons Row: Snooze & Delete */}
                          <div className="flex items-center justify-end gap-1.5 sm:gap-2 pt-1 border-t border-zinc-200/60 flex-wrap">
                            {/* Snooze Dropdown */}
                            <div className="relative">
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setOpenSnoozeId(isSnoozeOpen ? null : snoozeKey);
                                }}
                                className="inline-flex items-center gap-1 text-[11px] font-semibold text-zinc-600 hover:text-zinc-900 bg-white/80 hover:bg-white px-2 py-1 rounded-lg border border-zinc-200 shadow-xs transition"
                              >
                                <ClockIcon className="h-3.5 w-3.5 text-zinc-500" />
                                <span>{tNotif("snooze")}</span>
                              </button>

                              {isSnoozeOpen && (
                                <div
                                  onClick={(e) => e.stopPropagation()}
                                  className="absolute right-0 bottom-full mb-1 z-30 w-44 bg-white rounded-xl shadow-lg border border-zinc-200 py-1 text-xs text-zinc-700 animate-in fade-in zoom-in-95"
                                >
                                  <div className="px-3 py-1 text-[10px] font-bold text-zinc-400 uppercase tracking-wider">
                                    {tNotif("snoozeFor") || "Snooze for"}
                                  </div>
                                  <button
                                    onClick={() => handleSnoozeTasks(taskGroup.originalTasks, 1)}
                                    className="w-full text-left px-3 py-1.5 hover:bg-zinc-100 font-medium"
                                  >
                                    {tNotif("snooze1Hour") || "1 Hour"}
                                  </button>
                                  <button
                                    onClick={() => handleSnoozeTasks(taskGroup.originalTasks, 24)}
                                    className="w-full text-left px-3 py-1.5 hover:bg-zinc-100 font-medium"
                                  >
                                    {tNotif("snooze24HoursTomorrow") || "24 Hours (Tomorrow)"}
                                  </button>
                                  <button
                                    onClick={() => handleSnoozeTasks(taskGroup.originalTasks, 72)}
                                    className="w-full text-left px-3 py-1.5 hover:bg-zinc-100 font-medium"
                                  >
                                    {tNotif("snooze3Days") || "3 Days"}
                                  </button>
                                  <button
                                    onClick={() => handleSnoozeTasks(taskGroup.originalTasks, 168)}
                                    className="w-full text-left px-3 py-1.5 hover:bg-zinc-100 font-medium"
                                  >
                                    {tNotif("snooze1Week") || "1 Week"}
                                  </button>
                                </div>
                              )}
                            </div>

                            {/* Delete Button */}
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                setDeleteConfirmTarget({
                                  title: tNotif("deleteNotificationQuestion") || "Delete Notification?",
                                  description: tNotif("deleteNotificationConfirm", { itemTitle: activityDisplay }),
                                  onConfirm: () => handleDeleteTasks(taskGroup.originalTasks)
                                });
                              }}
                              className="inline-flex items-center gap-1 text-[11px] font-semibold text-rose-600 hover:text-rose-700 bg-white/80 hover:bg-rose-50 px-2 py-1 rounded-lg border border-rose-200/60 shadow-xs transition"
                              title="Delete Task"
                            >
                              <TrashIcon className="h-3.5 w-3.5 text-rose-500" />
                              <span>{tNotif("delete")}</span>
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
                {/* 2. Weight Check Reminders */}
                {visibleWeights.length > 0 && (
                  <div className="space-y-2.5">
                    <h3 className="text-[11px] font-black text-amber-800 uppercase tracking-wider px-1 pt-2">
                      {tNotif("weightCheckReminders")} ({visibleWeights.length})
                    </h3>
                    {visibleWeights.map((pig) => {
                      const snoozeKey = `weight-${pig.id}`;
                      const isSnoozeOpen = openSnoozeId === snoozeKey;
                      const lastDate = pig.lastWeightDate || pig.birthDate;
                      let daysSince = 30;
                      if (lastDate) {
                        const d = new Date(lastDate).getTime();
                        if (!isNaN(d)) {
                          daysSince = Math.max(1, Math.floor((Date.now() - d) / (1000 * 60 * 60 * 24)));
                        }
                      }
                      const pigDisplay = `${tNotif("pig") || "Pig"} #${pig.tagNumber || pig.id}`;

                      return (
                        <div
                          key={snoozeKey}
                          className="p-3.5 rounded-2xl border border-amber-200/80 bg-amber-50/70 hover:bg-amber-100/50 transition-all flex flex-col gap-2.5 relative"
                        >
                          <div
                            onClick={() => handleWeightAlertClick(pig)}
                            className="flex items-center justify-between cursor-pointer group min-w-0"
                          >
                            <div className="flex items-center gap-3 min-w-0">
                              <div className="h-10 w-10 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center shrink-0">
                                <WeightCheckerIcon className="h-5 w-5" />
                              </div>
                              <div className="min-w-0">
                                <h4 className="font-bold text-xs sm:text-sm text-zinc-900 truncate">
                                  {tNotif("weightCheckReminder") || "Weight Check Reminder"}
                                </h4>
                                <p className="text-xs text-amber-900 font-medium truncate">
                                  {pigDisplay} ({pig.breed || "Breeder"}) • Pen: {pig.location || "General"}
                                </p>
                                <p className="text-[10px] text-amber-700 font-semibold">
                                  {tNotif("lastWeighedDaysAgo", { days: daysSince })}
                                </p>
                              </div>
                            </div>

                            <div className="shrink-0 ml-3">
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleWeightAlertClick(pig);
                                }}
                                className="px-3.5 py-1.5 rounded-xl font-bold text-xs bg-amber-600 hover:bg-amber-700 text-white shadow-xs transition active:scale-95"
                              >
                                {tNotif("weigh") || "Weigh"}
                              </button>
                            </div>
                          </div>

                          {/* Action Buttons Row: Snooze & Delete/Dismiss */}
                          <div className="flex items-center justify-end gap-1.5 sm:gap-2 pt-1 border-t border-amber-200/60 flex-wrap">
                            {/* Snooze Dropdown */}
                            <div className="relative">
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setOpenSnoozeId(isSnoozeOpen ? null : snoozeKey);
                                }}
                                className="inline-flex items-center gap-1 text-[11px] font-semibold text-amber-900 bg-white/80 hover:bg-white px-2 py-1 rounded-lg border border-amber-200 shadow-xs transition"
                              >
                                <ClockIcon className="h-3.5 w-3.5 text-amber-700" />
                                <span>{tNotif("snooze")}</span>
                              </button>

                              {isSnoozeOpen && (
                                <div
                                  onClick={(e) => e.stopPropagation()}
                                  className="absolute right-0 bottom-full mb-1 z-30 w-44 bg-white rounded-xl shadow-lg border border-zinc-200 py-1 text-xs text-zinc-700 animate-in fade-in zoom-in-95"
                                >
                                  <div className="px-3 py-1 text-[10px] font-bold text-zinc-400 uppercase tracking-wider">
                                    {tNotif("snoozeFor") || "Snooze for"}
                                  </div>
                                  <button
                                    onClick={() => handleSnoozeWeight(pig.id, 7)}
                                    className="w-full text-left px-3 py-1.5 hover:bg-amber-50 font-medium"
                                  >
                                    {tNotif("snooze1Week") || "1 Week"}
                                  </button>
                                  <button
                                    onClick={() => handleSnoozeWeight(pig.id, 14)}
                                    className="w-full text-left px-3 py-1.5 hover:bg-amber-50 font-medium"
                                  >
                                    {tNotif("snooze2Weeks") || "2 Weeks"}
                                  </button>
                                  <button
                                    onClick={() => handleSnoozeWeight(pig.id, 30)}
                                    className="w-full text-left px-3 py-1.5 hover:bg-amber-50 font-medium"
                                  >
                                    {tNotif("snooze1Month") || "1 Month"}
                                  </button>
                                </div>
                              )}
                            </div>

                            {/* Dismiss/Delete Button */}
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                setDeleteConfirmTarget({
                                  title: tNotif("deleteNotificationQuestion") || "Delete Notification?",
                                  description: tNotif("deleteNotificationConfirm", { itemTitle: pigDisplay }),
                                  onConfirm: () => handleDismissWeight(pig.id)
                                });
                              }}
                              className="inline-flex items-center gap-1 text-[11px] font-semibold text-rose-600 hover:text-rose-700 bg-white/80 hover:bg-rose-50 px-2 py-1 rounded-lg border border-rose-200/60 shadow-xs transition"
                              title="Dismiss Reminder"
                            >
                              <TrashIcon className="h-3.5 w-3.5 text-rose-500" />
                              <span>{tNotif("delete")}</span>
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}

                {/* 3. Low Feed Stock Alerts */}
                {visibleStocks.length > 0 && (
                  <div className="space-y-2.5">
                    <h3 className="text-[11px] font-black text-orange-800 uppercase tracking-wider px-1 pt-2">
                      {tNotif("lowInventoryAlerts")} ({visibleStocks.length})
                    </h3>
                    {visibleStocks.map((feedItem) => {
                      const snoozeKey = `stock-${feedItem.id}`;
                      const isSnoozeOpen = openSnoozeId === snoozeKey;

                      return (
                        <div
                          key={snoozeKey}
                          className="p-3.5 rounded-2xl border border-orange-200 bg-orange-50/70 hover:bg-orange-100/50 transition-all flex flex-col gap-2.5 relative"
                        >
                          <Link
                            href="/dashboard/feed"
                            onClick={onClose}
                            className="flex items-center justify-between cursor-pointer group min-w-0"
                          >
                            <div className="flex items-center gap-3 min-w-0">
                              <div className="h-10 w-10 rounded-xl bg-orange-100 text-orange-800 flex items-center justify-center shrink-0">
                                <FeedManagementIcon className="h-5 w-5" />
                              </div>
                              <div className="min-w-0">
                                <h4 className="font-bold text-xs sm:text-sm text-zinc-900 truncate">
                                  {tNotif("lowFeedStock") || "Low Feed Stock"}
                                </h4>
                                <p className="text-xs text-orange-900 font-medium truncate">
                                  {feedItem.name}
                                </p>
                                <p className="text-[10px] text-orange-700 font-bold">
                                  {tNotif("stockLeftMinFormat", {
                                    current: feedItem.quantity,
                                    unit: feedItem.unit,
                                    min: feedItem.minThreshold,
                                  })}
                                </p>
                              </div>
                            </div>

                            <div className="shrink-0 ml-3">
                              <span className="px-3.5 py-1.5 rounded-xl font-bold text-xs bg-orange-600 hover:bg-orange-700 text-white shadow-xs transition inline-block">
                                {tNotif("restock") || "Restock"}
                              </span>
                            </div>
                          </Link>

                          {/* Action Buttons Row: Snooze & Dismiss */}
                          <div className="flex items-center justify-end gap-1.5 sm:gap-2 pt-1 border-t border-orange-200/60 flex-wrap">
                            {/* Snooze Dropdown */}
                            <div className="relative">
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setOpenSnoozeId(isSnoozeOpen ? null : snoozeKey);
                                }}
                                className="inline-flex items-center gap-1 text-[11px] font-semibold text-orange-900 bg-white/80 hover:bg-white px-2 py-1 rounded-lg border border-orange-200 shadow-xs transition"
                              >
                                <ClockIcon className="h-3.5 w-3.5 text-orange-700" />
                                <span>{tNotif("snooze")}</span>
                              </button>

                              {isSnoozeOpen && (
                                <div
                                  onClick={(e) => e.stopPropagation()}
                                  className="absolute right-0 bottom-full mb-1 z-30 w-44 bg-white rounded-xl shadow-lg border border-zinc-200 py-1 text-xs text-zinc-700 animate-in fade-in zoom-in-95"
                                >
                                  <div className="px-3 py-1 text-[10px] font-bold text-zinc-400 uppercase tracking-wider">
                                    {tNotif("snoozeFor") || "Snooze for"}
                                  </div>
                                  <button
                                    onClick={() => handleSnoozeStock(feedItem.id, 24)}
                                    className="w-full text-left px-3 py-1.5 hover:bg-orange-50 font-medium"
                                  >
                                    {tNotif("snooze24Hours") || "24 Hours"}
                                  </button>
                                  <button
                                    onClick={() => handleSnoozeStock(feedItem.id, 72)}
                                    className="w-full text-left px-3 py-1.5 hover:bg-orange-50 font-medium"
                                  >
                                    {tNotif("snooze3Days") || "3 Days"}
                                  </button>
                                  <button
                                    onClick={() => handleSnoozeStock(feedItem.id, 168)}
                                    className="w-full text-left px-3 py-1.5 hover:bg-orange-50 font-medium"
                                  >
                                    {tNotif("snooze1Week") || "1 Week"}
                                  </button>
                                </div>
                              )}
                            </div>

                            {/* Dismiss Button */}
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                setDeleteConfirmTarget({
                                  title: tNotif("deleteNotificationQuestion") || "Delete Notification?",
                                  description: tNotif("deleteNotificationConfirm", { itemTitle: feedItem.name }),
                                  onConfirm: () => handleDismissStock(feedItem.id)
                                });
                              }}
                              className="inline-flex items-center gap-1 text-[11px] font-semibold text-rose-600 hover:text-rose-700 bg-white/80 hover:bg-rose-50 px-2 py-1 rounded-lg border border-rose-200/60 shadow-xs transition"
                              title="Dismiss Alert"
                            >
                              <TrashIcon className="h-3.5 w-3.5 text-rose-500" />
                              <span>{tNotif("delete")}</span>
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </>
            )}
          </div>

          {/* ─── FOOTER ────────────────────────────────────────────── */}
          <div className="p-4 border-t border-zinc-150 bg-zinc-50 flex items-center justify-between text-xs text-zinc-500">
            <span>SmartSwine Notification Engine</span>
            <Link href="/dashboard/activities" onClick={onClose} className="font-bold text-emerald-700 hover:underline">
              View All Activities →
            </Link>
          </div>
        </div>
      </div>

      {/* Confirmation Modal */}
      {deleteConfirmTarget && (
        <div className="fixed inset-0 z-60 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 animate-in fade-in duration-150">
          <div
            onClick={(e) => e.stopPropagation()}
            className="bg-white rounded-2xl border border-zinc-200 shadow-2xl p-5 max-w-sm w-full space-y-4 text-left"
          >
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-xl bg-rose-100 text-rose-600 flex items-center justify-center shrink-0">
                <TrashIcon className="h-5 w-5" />
              </div>
              <div>
                <h3 className="font-bold text-base text-zinc-900">{deleteConfirmTarget.title}</h3>
                <p className="text-xs text-zinc-500 mt-0.5">Please confirm your action</p>
              </div>
            </div>
            <p className="text-xs text-zinc-600 leading-relaxed">
              {deleteConfirmTarget.description}
            </p>
            <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-zinc-100">
              <button
                type="button"
                onClick={() => setDeleteConfirmTarget(null)}
                className="px-3.5 py-2 rounded-xl text-xs font-semibold text-zinc-700 hover:bg-zinc-100 transition"
              >
                {tNotif("cancel") || "Cancel"}
              </button>
              <button
                type="button"
                onClick={() => {
                  deleteConfirmTarget.onConfirm();
                  setDeleteConfirmTarget(null);
                }}
                className="px-4 py-2 rounded-xl text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 shadow-sm shadow-rose-600/20 transition active:scale-95"
              >
                {tNotif("delete") || "Delete"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
