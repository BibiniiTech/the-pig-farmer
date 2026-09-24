import { Pig, TaskItem } from "@/lib/types";

export interface TaskGroupItem {
  activity: string;
  target: string;
  date: string;
  isOverdue: boolean;
  originalTasks: TaskItem[];
}

export interface FeedStockAlertItem {
  id: string;
  name: string;
  quantity: number;
  minThreshold: number;
  unit: string;
}

export function parseTaskDate(dateStr: string): Date | null {
  if (!dateStr) return null;
  if (dateStr.match(/^\d{4}-\d{2}-\d{2}$/)) {
    return new Date(dateStr);
  }
  const parts = dateStr.split(/\s+/);
  if (parts.length >= 2) {
    const monthStr = parts[0].toLowerCase();
    const day = parseInt(parts[1], 10);
    if (!isNaN(day)) {
      const months = ["jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"];
      const monthIdx = months.findIndex((m) => monthStr.startsWith(m));
      if (monthIdx !== -1) {
        const now = new Date();
        const d = new Date(now.getFullYear(), monthIdx, day);
        const diffMs = d.getTime() - now.getTime();
        const diffDays = diffMs / (1000 * 60 * 60 * 24);
        if (diffDays > 180) {
          d.setFullYear(d.getFullYear() - 1);
        } else if (diffDays < -180) {
          d.setFullYear(d.getFullYear() + 1);
        }
        return d;
      }
    }
  }
  const parsed = new Date(dateStr);
  return isNaN(parsed.getTime()) ? null : parsed;
}

export function isTaskOverdue(dateStr: string): boolean {
  if (!dateStr || dateStr === "Today" || dateStr === "Tomorrow") return false;
  const taskDate = parseTaskDate(dateStr);
  if (!taskDate) return false;

  const now = new Date();
  taskDate.setHours(23, 59, 59, 999);
  return taskDate < now;
}

export function formatToTaskDate(date: Date): string {
  const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  return `${months[date.getMonth()]} ${date.getDate()}`;
}

export function convertToTaskDate(dateStr: string): string {
  if (!dateStr) return "";
  if (dateStr.match(/^\d{4}-\d{2}-\d{2}$/)) {
    const d = new Date(dateStr);
    if (!isNaN(d.getTime())) return formatToTaskDate(d);
  }
  const ddmmyyyy = dateStr.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (ddmmyyyy) {
    const day = parseInt(ddmmyyyy[1], 10);
    const month = parseInt(ddmmyyyy[2], 10) - 1;
    const year = parseInt(ddmmyyyy[3], 10);
    const d = new Date(year, month, day);
    if (!isNaN(d.getTime())) return formatToTaskDate(d);
  }
  return dateStr;
}

export function isTaskDueOrOverdue(dateStr: string): boolean {
  if (!dateStr) return false;
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const taskDate = parseTaskDate(dateStr);
  if (!taskDate) return false;
  taskDate.setHours(0, 0, 0, 0);

  const diffDays = Math.ceil((taskDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  // Include all overdue tasks (diffDays < 0) and tasks due within next 2 days (diffDays <= 2)
  return diffDays <= 2;
}

export function groupTasks(tasks: TaskItem[], allPigs: Pig[] = []): TaskGroupItem[] {
  const now = Date.now();
  const filteredTasks = tasks.filter((task) => {
    if (task.completed) return false;
    if (task.snoozeUntil && task.snoozeUntil > now) return false;
    if (!isTaskDueOrOverdue(task.date)) return false;

    const name = task.name || "";
    const isFemaleSpecific = ["heat detection", "breeding/mating", "confirm pregnancy", "farrowing"].some((prefix) =>
      name.toLowerCase().startsWith(prefix)
    );
    if (isFemaleSpecific) {
      const parts = name.split(":");
      const rawTarget = parts.length > 1
        ? parts.slice(1).join(":")
            .replace(/pigs?/gi, "")
            .replace(/tags?:?/gi, "")
            .trim()
        : "";
      const pig = allPigs.find((p) => p.id === rawTarget || p.tagNumber.toLowerCase() === rawTarget.toLowerCase()) ||
                  (task.pigIds && task.pigIds.length > 0 ? allPigs.find((p) => task.pigIds.includes(p.id)) : undefined);
      return !pig || pig.gender?.toLowerCase() === "female";
    }
    return true;
  });

  const groups: Record<string, TaskItem[]> = {};
  filteredTasks.forEach((task) => {
    const name = task.name || "";
    const activity = name.includes(":") ? name.split(":")[0].trim() : name.trim();
    const key = `${activity}_${task.date}`;
    if (!groups[key]) {
      groups[key] = [];
    }
    groups[key].push(task);
  });

  const taskGroups: TaskGroupItem[] = Object.values(groups).map((group) => {
    const first = group[0];
    const name = first.name || "";
    const activity = name.includes(":") ? name.split(":")[0].trim() : name.trim();

    let target = "General";
    if (group.length > 1) {
      const tags = group
        .map((t) => {
          if (t.pigIds && t.pigIds.length > 0) {
            const pig = allPigs.find((p) => t.pigIds.includes(p.id));
            if (pig) return pig.tagNumber;
          }
          const parts = t.name.split(":");
          const rawTarget = parts.length > 1
            ? parts.slice(1).join(":")
                .replace(/pigs?/gi, "")
                .replace(/tags?:?/gi, "")
                .trim()
            : "";
          if (!rawTarget) return "General";
          const pig = allPigs.find((p) => p.id === rawTarget || p.tagNumber.toLowerCase() === rawTarget.toLowerCase());
          return pig ? pig.tagNumber : rawTarget;
        })
        .filter((t) => t !== "");
      const uniqueTags = Array.from(new Set(tags));
      target = uniqueTags.join(", ");
    } else {
      if (first.pigIds && first.pigIds.length > 0) {
        const pig = allPigs.find((p) => first.pigIds.includes(p.id));
        if (pig) target = pig.tagNumber;
      }
      if (target === "General") {
        const parts = first.name.split(":");
        const rawTarget = parts.length > 1
          ? parts.slice(1).join(":")
              .replace(/pigs?/gi, "")
              .replace(/tags?:?/gi, "")
              .trim()
          : "";
        if (rawTarget && rawTarget.toLowerCase() !== "general") {
          const pig = allPigs.find((p) => p.id === rawTarget || p.tagNumber.toLowerCase() === rawTarget.toLowerCase());
          target = pig ? pig.tagNumber : rawTarget;
        }
      }
    }

    const isOverdueVal = isTaskOverdue(first.date);
    const convertedDate = convertToTaskDate(first.date);

    return {
      activity,
      target,
      date: convertedDate,
      isOverdue: isOverdueVal,
      originalTasks: group,
    };
  });

  return taskGroups.sort((a, b) => {
    if (a.isOverdue && !b.isOverdue) return -1;
    if (!a.isOverdue && b.isOverdue) return 1;
    return 0;
  });
}

export function calculateWeightAlerts(
  pigs: Pig[],
  snoozedAlerts: Record<string, number> = {},
  dismissedAlerts: string[] = []
): Pig[] {
  const now = Date.now();
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  return pigs.filter((pig) => {
    if (dismissedAlerts.includes(pig.id)) return false;
    const snoozeUntil = snoozedAlerts[pig.id];
    if (snoozeUntil && snoozeUntil > now) return false;
    const lastWeight = (pig.lastWeightDate && pig.lastWeightDate.trim().length > 0)
      ? pig.lastWeightDate
      : (pig.weight <= 0 ? pig.birthDate : null);
    if (!lastWeight) return false;

    let lastDate: Date;
    if (lastWeight.includes("/")) {
      const parts = lastWeight.split("/");
      lastDate = new Date(parseInt(parts[2]), parseInt(parts[1]) - 1, parseInt(parts[0]));
    } else {
      lastDate = new Date(lastWeight);
    }

    if (isNaN(lastDate.getTime())) return false;
    const diffDays = Math.floor((today.getTime() - lastDate.getTime()) / (1000 * 60 * 60 * 24));
    return diffDays >= 30;
  });
}

export function calculateStockAlerts(
  inventory: any[],
  snoozedAlerts: Record<string, number> = {},
  dismissedAlerts: string[] = []
): FeedStockAlertItem[] {
  const now = Date.now();
  return inventory
    .filter((item) => {
      if (dismissedAlerts.includes(item.id)) return false;
      const snoozeUntil = snoozedAlerts[item.id];
      if (snoozeUntil && snoozeUntil > now) return false;
      return item.quantity <= item.minThreshold && item.minThreshold > 0;
    })
    .map((item) => ({
      id: item.id,
      name: item.name,
      quantity: item.quantity,
      minThreshold: item.minThreshold,
      unit: item.unit || "kg",
    }));
}
