interface GrowthPoint {
  ageDays: number;
  weightKg: number;
}

interface BreedCurve {
  key: string;
  displayName: string;
  points: GrowthPoint[];
}

const LargeWhite: BreedCurve = {
  key: "Large White",
  displayName: "Large White",
  points: [
    { ageDays: 0, weightKg: 1.26 },
    { ageDays: 28, weightKg: 7.20 },
    { ageDays: 56, weightKg: 19.80 },
    { ageDays: 84, weightKg: 39.15 },
    { ageDays: 112, weightKg: 63.90 },
    { ageDays: 140, weightKg: 87.75 },
    { ageDays: 168, weightKg: 105.75 },
    { ageDays: 252, weightKg: 144.00 },
    { ageDays: 364, weightKg: 180.00 }
  ]
};

const Landrace: BreedCurve = {
  key: "Landrace",
  displayName: "Landrace",
  points: [
    { ageDays: 0, weightKg: 1.22 },
    { ageDays: 28, weightKg: 6.93 },
    { ageDays: 56, weightKg: 19.35 },
    { ageDays: 84, weightKg: 37.80 },
    { ageDays: 112, weightKg: 62.10 },
    { ageDays: 140, weightKg: 85.50 },
    { ageDays: 168, weightKg: 103.50 },
    { ageDays: 252, weightKg: 139.50 },
    { ageDays: 364, weightKg: 171.00 }
  ]
};

const Duroc: BreedCurve = {
  key: "Duroc",
  displayName: "Duroc",
  points: [
    { ageDays: 0, weightKg: 1.31 },
    { ageDays: 28, weightKg: 7.47 },
    { ageDays: 56, weightKg: 21.60 },
    { ageDays: 84, weightKg: 41.40 },
    { ageDays: 112, weightKg: 67.50 },
    { ageDays: 140, weightKg: 91.80 },
    { ageDays: 168, weightKg: 112.50 },
    { ageDays: 252, weightKg: 153.00 },
    { ageDays: 364, weightKg: 189.00 }
  ]
};

const Hampshire: BreedCurve = {
  key: "Hampshire",
  displayName: "Hampshire",
  points: [
    { ageDays: 0, weightKg: 1.17 },
    { ageDays: 28, weightKg: 6.75 },
    { ageDays: 56, weightKg: 18.00 },
    { ageDays: 84, weightKg: 36.00 },
    { ageDays: 112, weightKg: 58.50 },
    { ageDays: 140, weightKg: 81.00 },
    { ageDays: 168, weightKg: 99.00 },
    { ageDays: 252, weightKg: 130.50 },
    { ageDays: 364, weightKg: 162.00 }
  ]
};

const Berkshire: BreedCurve = {
  key: "Berkshire",
  displayName: "Berkshire",
  points: [
    { ageDays: 0, weightKg: 1.13 },
    { ageDays: 28, weightKg: 6.30 },
    { ageDays: 56, weightKg: 17.10 },
    { ageDays: 84, weightKg: 34.20 },
    { ageDays: 112, weightKg: 54.00 },
    { ageDays: 140, weightKg: 76.50 },
    { ageDays: 168, weightKg: 94.50 },
    { ageDays: 252, weightKg: 126.00 },
    { ageDays: 364, weightKg: 153.00 }
  ]
};

const LocalHeritage: BreedCurve = {
  key: "Local / Heritage",
  displayName: "Local / Heritage",
  points: [
    { ageDays: 0, weightKg: 0.81 },
    { ageDays: 28, weightKg: 4.50 },
    { ageDays: 56, weightKg: 9.90 },
    { ageDays: 84, weightKg: 17.10 },
    { ageDays: 112, weightKg: 27.00 },
    { ageDays: 140, weightKg: 36.90 },
    { ageDays: 168, weightKg: 46.80 },
    { ageDays: 252, weightKg: 67.50 },
    { ageDays: 364, weightKg: 90.00 }
  ]
};

export function interpolateCurve(points: GrowthPoint[], ageDays: number): number {
  if (points.length === 0) return 0;
  const sorted = [...points].sort((a, b) => a.ageDays - b.ageDays);

  if (ageDays <= sorted[0].ageDays) return sorted[0].weightKg;
  if (ageDays >= sorted[sorted.length - 1].ageDays) return sorted[sorted.length - 1].weightKg;

  for (let i = 0; i < sorted.length - 1; i++) {
    const p1 = sorted[i];
    const p2 = sorted[i + 1];
    if (ageDays >= p1.ageDays && ageDays <= p2.ageDays) {
      const ratio = (ageDays - p1.ageDays) / (p2.ageDays - p1.ageDays);
      return p1.weightKg + ratio * (p2.weightKg - p1.weightKg);
    }
  }
  return sorted[sorted.length - 1].weightKg;
}

function blendCurves(curves: BreedCurve[], displayName: string, vigorFactor = 1.0): BreedCurve {
  const standardAges = [0, 28, 56, 84, 112, 140, 168, 252, 364];
  const blendedPoints = standardAges.map((age) => {
    let sumWeights = 0;
    curves.forEach((curve) => {
      sumWeights += interpolateCurve(curve.points, age);
    });
    const avgWeight = sumWeights / curves.length;
    return {
      ageDays: age,
      weightKg: Math.round(avgWeight * vigorFactor * 100) / 100,
    };
  });
  return {
    key: displayName,
    displayName,
    points: blendedPoints,
  };
}

function findBaseBreed(token: string): BreedCurve | null {
  const t = token.toLowerCase();
  if (t.includes("duroc")) return Duroc;
  if (t.includes("landrace")) return Landrace;
  if (t.includes("large white") || t.includes("yorkshire") || t.includes("white")) return LargeWhite;
  if (t.includes("hampshire") || t.includes("hamp")) return Hampshire;
  if (t.includes("berkshire") || t.includes("berk")) return Berkshire;
  if (
    t.includes("local") ||
    t.includes("heritage") ||
    t.includes("indigenous") ||
    t.includes("native") ||
    t.includes("kolbroek") ||
    t.includes("mukota")
  ) {
    return LocalHeritage;
  }
  return null;
}

export function resolveBreedCurve(breedInput: string): BreedCurve {
  const normalized = breedInput.trim().toLowerCase();

  if (!normalized || normalized === "other" || normalized === "none" || normalized === "unknown") {
    return LocalHeritage;
  }

  // Predefined crosses
  if (normalized.includes("duroc") && normalized.includes("large white") && normalized.includes("landrace")) {
    return blendCurves([Duroc, LargeWhite, Landrace], "Duroc x (Large White x Landrace)", 1.08);
  }
  if (normalized.includes("large white") && normalized.includes("landrace")) {
    return blendCurves([LargeWhite, Landrace], "Large White x Landrace (F1)", 1.05);
  }

  // Separators checking
  const separators = /[x/+]|\bcross\b|\bhybrid\b/g;
  const parts = normalized.split(separators).map((s) => s.trim()).filter((s) => s.length > 0);

  if (parts.length > 1) {
    const matchedCurves: BreedCurve[] = [];
    parts.forEach((p) => {
      const b = findBaseBreed(p);
      if (b) matchedCurves.push(b);
    });
    if (matchedCurves.length > 0) {
      const crossName = matchedCurves.map((c) => c.displayName).join(" x ");
      return blendCurves(matchedCurves, crossName, 1.05);
    }
  }

  const singleMatch = findBaseBreed(normalized);
  if (singleMatch) return singleMatch;

  return LocalHeritage;
}

export function evaluatePerformance(breed: string, ageDays: number, actualWeight: number): string {
  if (actualWeight <= 0 || !actualWeight) {
    return "Blank";
  }
  const safeAgeDays = ageDays < 0 ? 0 : ageDays;
  const curve = resolveBreedCurve(breed);
  const expectedWeight = interpolateCurve(curve.points, safeAgeDays);
  const safeExpectedWeight = expectedWeight <= 0
    ? (curve.points[0]?.weightKg || 1.0)
    : expectedWeight;

  const ratio = actualWeight / safeExpectedWeight;

  if (safeAgeDays > 270) {
    if (ratio >= 1.35) return "Excellent";
    if (ratio >= 0.85) return "Good";
    if (ratio >= 0.70) return "Caution";
    return "Poor";
  } else {
    if (ratio >= 1.15) return "Excellent";
    if (ratio >= 0.90) return "Good";
    if (ratio >= 0.75) return "Caution";
    return "Poor";
  }
}

export function parseSwineDate(dateStr?: string): Date | null {
  if (!dateStr) return null;
  const trimmed = dateStr.trim();

  // 1. Check if format is yyyy-mm-dd (e.g. 2026-05-10)
  if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
    const parts = trimmed.split("-").map(Number);
    const d = new Date(parts[0], parts[1] - 1, parts[2]);
    if (!isNaN(d.getTime())) return d;
  }

  // 2. Check if format is dd/mm/yyyy (e.g. 10/05/2026 or 10-05-2026)
  if (/^\d{2}[/-]\d{2}[/-]\d{4}$/.test(trimmed)) {
    const separator = trimmed.includes("/") ? "/" : "-";
    const parts = trimmed.split(separator).map(Number);
    const d = new Date(parts[2], parts[1] - 1, parts[0]);
    if (!isNaN(d.getTime())) return d;
  }

  // 3. Fallback to standard new Date()
  const d = new Date(trimmed);
  return isNaN(d.getTime()) ? null : d;
}

export function calculateAgeDays(birthDateStr?: string): number {
  if (!birthDateStr) return 0;
  try {
    const birth = parseSwineDate(birthDateStr);
    if (!birth) return 0;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const birthMidnight = new Date(birth);
    birthMidnight.setHours(0, 0, 0, 0);
    const diffTime = today.getTime() - birthMidnight.getTime();
    return Math.floor(diffTime / (1000 * 60 * 60 * 24));
  } catch (e) {
    return 0;
  }
}

export function calculateAgeMonths(birthDateStr?: string): number {
  if (!birthDateStr) return 0;
  try {
    const birth = parseSwineDate(birthDateStr);
    if (!birth) return 0;
    const today = new Date();

    let months = (today.getFullYear() - birth.getFullYear()) * 12;
    months += today.getMonth() - birth.getMonth();

    if (today.getDate() < birth.getDate()) {
      months--;
    }
    return Math.max(0, months);
  } catch (e) {
    return 0;
  }
}
