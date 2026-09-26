import { FeedIngredient, NutritionalRequirement } from "./types";

export interface FormulationResult {
  ingredients: { [id: string]: number }; // Maps ingredient ID -> percent
  proportions?: { [id: string]: number };
  targetRequirement: NutritionalRequirement;
  nutritionalComparison: Array<{
    label: string;
    target: number;
    actual: number;
    isDeficient: boolean;
  }>;
  totalPercentage: number;
  error?: string;
}

export interface InclusionSafetyAlert {
  ingredientName: string;
  currentPercent: number;
  maxAllowedPercent: number;
  stage: string;
  riskDescription: string;
  riskKey: string;
}

export interface FeedNutrientProfile {
  crudeProtein: number;
  metabolizableEnergy: number;
  digestibleProtein: number;
  crudeFiber: number;
  calcium: number;
  phosphorus: number;
  lysine: number;
  methionine: number;
  totalWeight: number;
  costPerKg?: number;
  costPer50kgBag?: number;
  totalCost?: number;
  caPRatio?: number;
}

/**
 * 100% Parity Port of the Android Kotlin Feed formulation Pearson Square & deficit balancing algorithm
 * (FeedViewModel.kt lines 670-895)
 */
export function formulateFeed(
  targetRequirement: NutritionalRequirement,
  allIngredients: FeedIngredient[],
  selectedIds: string[],
  shuffle: boolean = false
): FormulationResult {
  // Target Crude Protein: use NRC crudeProtein standard, fallback to digestibleProtein / 0.85
  const targetProtein =
    targetRequirement.crudeProtein && targetRequirement.crudeProtein > 0
      ? targetRequirement.crudeProtein
      : targetRequirement.digestibleProtein && targetRequirement.digestibleProtein > 0
      ? targetRequirement.digestibleProtein / 0.85
      : 16.0;

  const targetStage = targetRequirement.stage || "Grower";

  const selectedIngredients = allIngredients.filter(ing => selectedIds.includes(ing.id));
  if (selectedIngredients.length === 0) {
    return {
      ingredients: {},
      targetRequirement,
      nutritionalComparison: [],
      totalPercentage: 0,
      error: "Please select at least one ingredient to formulate.",
    };
  }

  const currentUsed: { [id: string]: number } = {};

  const supplementalCategory = "Vitamins, Minerals & Salt";
  const supplementalIngredients = selectedIngredients.filter(
    ing =>
      (ing.mainCategory && ing.mainCategory.toLowerCase() === supplementalCategory.toLowerCase()) ||
      ing.name.toLowerCase().includes("salt") ||
      ing.name.toLowerCase().includes("premix") ||
      ing.name.toLowerCase().includes("limestone") ||
      ing.name.toLowerCase().includes("bone meal") ||
      ing.name.toLowerCase().includes("dcp") ||
      ing.name.toLowerCase().includes("oyster") ||
      ing.name.toLowerCase().includes("lysine") ||
      ing.name.toLowerCase().includes("methionine")
  );

  const mainIngredients = selectedIngredients.filter(
    ing => !supplementalIngredients.some(s => s.id === ing.id)
  );

  // Veterinary limits based on target stage
  const limits: { [id: string]: number } = {};
  selectedIngredients.forEach(ing => {
    let limit = 100.0;
    const stageLower = targetStage.toLowerCase();
    if (stageLower === "creep" || stageLower === "pre-starter") {
      limit = Math.min(ing.maxStarter * 0.7, ing.maxStarter);
    } else if (stageLower === "weaner/starter" || stageLower === "starter" || stageLower === "weaner") {
      limit = ing.maxStarter;
    } else if (stageLower === "grower") {
      limit = ing.maxGrower;
    } else if (stageLower === "finisher") {
      limit = ing.maxFinisher;
    } else if (stageLower === "pregnant" || stageLower === "gestating") {
      limit = Math.min(ing.maxGrower, ing.maxFinisher);
    } else if (stageLower === "lactating") {
      limit = ing.maxGrower;
    } else {
      limit = Math.min(ing.maxStarter, Math.min(ing.maxGrower, ing.maxFinisher));
    }
    limits[ing.id] = limit > 0 ? limit : 50.0;
  });

  // Helper getters for Ca and P percentage (accounting for g/kg vs %)
  const getCaPercent = (ing: FeedIngredient) => (ing.calcium > 50.0 ? ing.calcium / 10.0 : ing.calcium);
  const getPPercent = (ing: FeedIngredient) => (ing.phosphorus > 50.0 ? ing.phosphorus / 10.0 : ing.phosphorus);

  let suppAllocated = 0.0;

  // STEP 1a: Essential Salt & Premix Pre-allocation
  supplementalIngredients.forEach(ing => {
    const nameLower = ing.name.toLowerCase();
    const limit = limits[ing.id] ?? 2.0;
    if (nameLower.includes("salt")) {
      const saltAmt = Math.min(0.35, limit);
      currentUsed[ing.id] = saltAmt;
      suppAllocated += saltAmt;
    } else if (nameLower.includes("premix") || nameLower.includes("vitamin")) {
      const premixAmt = Math.min(0.30, limit);
      currentUsed[ing.id] = premixAmt;
      suppAllocated += premixAmt;
    }
  });

  // STEP 1b: Calcium & Phosphorus balancing
  const mineralSources = supplementalIngredients.filter(ing => {
    const n = ing.name.toLowerCase();
    return (
      !n.includes("salt") &&
      !n.includes("premix") &&
      !n.includes("vitamin") &&
      (getCaPercent(ing) > 5.0 || getPPercent(ing) > 5.0)
    );
  });

  const sortedMinerals = [...mineralSources].sort((a, b) => getPPercent(b) - getPPercent(a));
  let currentCaFromSupp = Object.entries(currentUsed).reduce((sum, [id, pct]) => {
    const ing = selectedIngredients.find(i => i.id === id);
    return sum + (ing ? getCaPercent(ing) * (pct / 100.0) : 0);
  }, 0);
  let currentPFromSupp = Object.entries(currentUsed).reduce((sum, [id, pct]) => {
    const ing = selectedIngredients.find(i => i.id === id);
    return sum + (ing ? getPPercent(ing) * (pct / 100.0) : 0);
  }, 0);

  sortedMinerals.forEach(ing => {
    const caPurity = getCaPercent(ing) / 100.0;
    const pPurity = getPPercent(ing) / 100.0;
    const limit = limits[ing.id] ?? 3.0;

    let neededPct = 0.0;
    if (pPurity > 0.05 && currentPFromSupp < targetRequirement.phosphorus * 0.7) {
      const deficitP = targetRequirement.phosphorus * 0.7 - currentPFromSupp;
      neededPct = Math.min(deficitP / pPurity, limit);
    } else if (caPurity > 0.1 && currentCaFromSupp < targetRequirement.calcium * 0.8) {
      const deficitCa = targetRequirement.calcium * 0.8 - currentCaFromSupp;
      neededPct = Math.min(deficitCa / caPurity, limit);
    }

    if (neededPct > 0.05) {
      currentUsed[ing.id] = neededPct;
      suppAllocated += neededPct;
      currentCaFromSupp += neededPct * caPurity;
      currentPFromSupp += neededPct * pPurity;
    }
  });

  // STEP 2: BUDGET FOR MAIN INGREDIENTS
  const mainBudget = Math.max(10.0, Math.min(100.0, 100.0 - suppAllocated));
  let availablePercent = mainBudget;

  // Diversity allocation for main ingredients
  mainIngredients.forEach(ing => {
    const name = ing.name.toLowerCase();
    const minInclusion = name.includes("bran") ? 4.0 : 3.0;
    const limit = limits[ing.id] ?? 50.0;
    const safeStart = Math.min(minInclusion, limit);

    if (availablePercent >= safeStart) {
      currentUsed[ing.id] = (currentUsed[ing.id] ?? 0.0) + safeStart;
      availablePercent -= safeStart;
    }
  });

  // Calculate remaining CP required from main ingredients
  const currentCpFromAllocated = Object.entries(currentUsed).reduce((sum, [id, pct]) => {
    const ing = selectedIngredients.find(i => i.id === id);
    return sum + (ing ? ing.crudeProtein * (pct / 100.0) : 0);
  }, 0);
  const remainingCpNeeded = targetProtein - currentCpFromAllocated;
  const mainTargetCP =
    availablePercent > 0.5
      ? Math.max(5.0, Math.min(50.0, remainingCpNeeded / (availablePercent / 100.0)))
      : targetProtein;

  // Cost-Aware Least-Cost Sorting for Energy & Protein Pools
  let poolLow = mainIngredients.filter(ing => ing.crudeProtein < mainTargetCP);
  let poolHigh = mainIngredients.filter(ing => ing.crudeProtein >= mainTargetCP);

  if (shuffle) {
    poolLow = [...poolLow].sort(() => Math.random() - 0.5);
    poolHigh = [...poolHigh].sort(() => Math.random() - 0.5);
  } else {
    // Energy pool: lowest cost per unit energy first
    poolLow = [...poolLow].sort((a, b) => {
      const costRatioA = a.costPerKg > 0 && a.metabolizableEnergy > 0 ? a.costPerKg / a.metabolizableEnergy : 999.0;
      const costRatioB = b.costPerKg > 0 && b.metabolizableEnergy > 0 ? b.costPerKg / b.metabolizableEnergy : 999.0;
      if (Math.abs(costRatioA - costRatioB) > 0.0001) return costRatioA - costRatioB;
      return b.metabolizableEnergy - a.metabolizableEnergy;
    });
    // Protein pool: lowest cost per unit protein first
    poolHigh = [...poolHigh].sort((a, b) => {
      const costRatioA = a.costPerKg > 0 && a.crudeProtein > 0 ? a.costPerKg / a.crudeProtein : 999.0;
      const costRatioB = b.costPerKg > 0 && b.crudeProtein > 0 ? b.costPerKg / b.crudeProtein : 999.0;
      if (Math.abs(costRatioA - costRatioB) > 0.0001) return costRatioA - costRatioB;
      return b.crudeProtein - a.crudeProtein;
    });
  }

  let remainingTotal = availablePercent;

  // Balance Main Mix to remaining budget using Pearson Square logic
  while (remainingTotal > 0.01 && poolLow.length > 0 && poolHigh.length > 0) {
    const low = poolLow[0];
    const high = poolHigh[0];

    const rLow = Math.max(0.1, high.crudeProtein - mainTargetCP);
    const rHigh = Math.max(0.1, mainTargetCP - low.crudeProtein);

    const x = remainingTotal * (rLow / (rLow + rHigh));
    const y = remainingTotal * (rHigh / (rLow + rHigh));

    const capLow = (limits[low.id] ?? 100.0) - (currentUsed[low.id] ?? 0.0);
    const capHigh = (limits[high.id] ?? 100.0) - (currentUsed[high.id] ?? 0.0);

    const scaleX = x > 0.001 ? capLow / x : 1.0;
    const scaleY = y > 0.001 ? capHigh / y : 1.0;
    const scale = Math.min(1.0, scaleX, scaleY);

    const useX = x * scale;
    const useY = y * scale;

    currentUsed[low.id] = (currentUsed[low.id] ?? 0.0) + useX;
    currentUsed[high.id] = (currentUsed[high.id] ?? 0.0) + useY;
    remainingTotal -= useX + useY;

    if ((currentUsed[low.id] ?? 0.0) >= (limits[low.id] ?? 100.0) - 0.01) {
      poolLow.shift();
    }
    if ((currentUsed[high.id] ?? 0.0) >= (limits[high.id] ?? 100.0) - 0.01) {
      poolHigh.shift();
    }
  }

  // Top up any residual main budget
  if (remainingTotal > 0.01) {
    const remainingPool =
      poolHigh.length > 0
        ? shuffle
          ? [...poolHigh].sort(() => Math.random() - 0.5)
          : [...poolHigh].sort((a, b) => b.metabolizableEnergy - a.metabolizableEnergy)
        : shuffle
        ? [...poolLow].sort(() => Math.random() - 0.5)
        : [...poolLow].sort((a, b) => b.metabolizableEnergy - a.metabolizableEnergy);

    for (const ing of remainingPool) {
      const cap = (limits[ing.id] ?? 100.0) - (currentUsed[ing.id] ?? 0.0);
      const use = Math.min(remainingTotal, cap);
      currentUsed[ing.id] = (currentUsed[ing.id] ?? 0.0) + use;
      remainingTotal -= use;
      if (remainingTotal <= 0.01) break;
    }
  }

  // STEP 3: GUARANTEE EXACT 100.0% BALANCE
  const totalUsed = Object.values(currentUsed).reduce((sum, v) => sum + v, 0);
  if (Math.abs(totalUsed - 100.0) > 0.001) {
    // Find main energy ingredient with highest inclusion to absorb minor rounding difference
    const mainEntries = Object.entries(currentUsed).filter(([id]) =>
      mainIngredients.some(m => m.id === id)
    );
    const leadEntry =
      mainEntries.length > 0
        ? mainEntries.sort((a, b) => b[1] - a[1])[0]
        : Object.entries(currentUsed).sort((a, b) => b[1] - a[1])[0];

    if (leadEntry) {
      const diff = 100.0 - totalUsed;
      const adjusted = leadEntry[1] + diff;
      if (adjusted > 0) {
        currentUsed[leadEntry[0]] = adjusted;
      }
    }
  }

  // Build final output
  const finalUsed: Record<string, number> = {};
  let totalPercentage = 0;
  Object.entries(currentUsed).forEach(([id, percent]) => {
    if (percent > 0.001) {
      finalUsed[id] = percent;
      totalPercentage += percent;
    }
  });

  const finalNutrients = { ca: 0.0, p: 0.0, lys: 0.0, met: 0.0, protein: 0.0, fiber: 0.0, energy: 0.0 };
  Object.entries(finalUsed).forEach(([id, percent]) => {
    const ing = allIngredients.find(i => i.id === id || i.name === id);
    if (!ing) return;
    const factor = percent / 100.0;
    finalNutrients.protein += ing.crudeProtein * factor;
    finalNutrients.fiber += ing.crudeFiber * factor;
    finalNutrients.energy += ing.metabolizableEnergy * factor;
    finalNutrients.ca += getCaPercent(ing) * factor;
    finalNutrients.p += getPPercent(ing) * factor;

    const isPureLys = ing.name.toLowerCase().includes("lysine") || (ing.crudeProtein > 80.0 && ing.lysine > 50.0);
    const ingDietaryLys = isPureLys ? ing.lysine : ing.crudeProtein * (ing.lysine / 100.0);
    finalNutrients.lys += ingDietaryLys * factor;

    const isPureMet =
      ing.name.toLowerCase().includes("methionine") || (ing.crudeProtein > 80.0 && ing.methionine + ing.cystine > 50.0);
    const ingDietaryMet = isPureMet ? ing.methionine + ing.cystine : ing.crudeProtein * ((ing.methionine + ing.cystine) / 100.0);
    finalNutrients.met += ingDietaryMet * factor;
  });

  const targetDietaryLys =
    targetRequirement.dietaryLysine && targetRequirement.dietaryLysine > 0
      ? targetRequirement.dietaryLysine
      : targetRequirement.lysine > 0
      ? (targetRequirement.lysine / 100.0) * targetProtein * 0.85
      : 0.95;

  const targetDietaryMet =
    targetRequirement.dietaryMethionine && targetRequirement.dietaryMethionine > 0
      ? targetRequirement.dietaryMethionine
      : targetRequirement.methionineCystine > 0
      ? (targetRequirement.methionineCystine / 100.0) * targetProtein * 0.85
      : 0.55;

  const nutritionalComparison = [
    {
      label: "Crude Protein (%)",
      target: targetProtein,
      actual: finalNutrients.protein,
      isDeficient: finalNutrients.protein < targetProtein - 0.2,
    },
    {
      label: "Digestible Protein (%)",
      target: targetRequirement.digestibleProtein || targetProtein * 0.85,
      actual: finalNutrients.protein * 0.85,
      isDeficient: finalNutrients.protein * 0.85 < (targetRequirement.digestibleProtein || targetProtein * 0.85) - 0.2,
    },
    {
      label: "Crude Fiber (%)",
      target: targetRequirement.crudeFiber,
      actual: finalNutrients.fiber,
      isDeficient: finalNutrients.fiber < targetRequirement.crudeFiber - 0.1,
    },
    {
      label: "Metabolizable Energy (kcal/kg)",
      target: targetRequirement.metabolizableEnergy,
      actual: finalNutrients.energy,
      isDeficient: finalNutrients.energy < targetRequirement.metabolizableEnergy - 100,
    },
    {
      label: "Calcium (%)",
      target: targetRequirement.calcium,
      actual: finalNutrients.ca,
      isDeficient: finalNutrients.ca < targetRequirement.calcium - 0.05,
    },
    {
      label: "Phosphorus (%)",
      target: targetRequirement.phosphorus,
      actual: finalNutrients.p,
      isDeficient: finalNutrients.p < targetRequirement.phosphorus - 0.05,
    },
    {
      label: "Dietary Lysine (%)",
      target: targetDietaryLys,
      actual: finalNutrients.lys,
      isDeficient: finalNutrients.lys < targetDietaryLys - 0.05,
    },
    {
      label: "Dietary Methionine + Cystine (%)",
      target: targetDietaryMet,
      actual: finalNutrients.met,
      isDeficient: finalNutrients.met < targetDietaryMet - 0.05,
    },
  ];

  return {
    ingredients: finalUsed,
    targetRequirement,
    nutritionalComparison,
    totalPercentage: Math.min(100.0, totalPercentage),
    error:
      totalPercentage < 99.5
        ? `Formulation incomplete. Total mix sums to ${totalPercentage.toFixed(1)}%. Try selecting more energy and protein ingredients.`
        : undefined,
  };
}

/**
 * Inclusion safety alerts matching Android FeedViewModel.kt lines 980-1015
 */
export function checkIngredientSafety(
  items: { ingredient: FeedIngredient; quantity: number }[],
  stage: string = "Grower"
): InclusionSafetyAlert[] {
  if (items.length === 0) return [];
  const totalQty = Math.max(0.0001, items.reduce((sum, item) => sum + item.quantity, 0));
  const alerts: InclusionSafetyAlert[] = [];

  items.forEach(({ ingredient: ing, quantity: qty }) => {
    const currentPercent = (qty / totalQty) * 100.0;
    let maxLimit = 100.0;
    const stageLower = stage.toLowerCase();
    if (stageLower === "creep" || stageLower === "pre-starter") {
      maxLimit = Math.min(ing.maxStarter * 0.7, ing.maxStarter);
    } else if (stageLower === "weaner/starter" || stageLower === "starter" || stageLower === "weaner") {
      maxLimit = ing.maxStarter;
    } else if (stageLower === "grower") {
      maxLimit = ing.maxGrower;
    } else if (stageLower === "finisher") {
      maxLimit = ing.maxFinisher;
    } else if (stageLower === "pregnant" || stageLower === "gestating") {
      maxLimit = Math.min(ing.maxGrower, ing.maxFinisher);
    } else if (stageLower === "lactating") {
      maxLimit = ing.maxGrower;
    } else {
      maxLimit = Math.min(ing.maxStarter, Math.min(ing.maxGrower, ing.maxFinisher));
    }

    if (maxLimit > 0.01 && maxLimit < 99.9 && currentPercent > maxLimit + 0.05) {
      const ingNameLower = ing.name.toLowerCase();
      let riskKey = "risk_exceeds_limit";
      let risk = `Exceeds safe recommended inclusion limit (${maxLimit.toFixed(1)}%) for ${stage} stage`;

      if (ingNameLower.includes("cottonseed")) {
        riskKey = "risk_gossypol_toxicity";
        risk = "Excess gossypol toxicity risk (heart & liver damage in monogastrics)";
      } else if (ingNameLower.includes("cassava peel")) {
        riskKey = "risk_hydrocyanic_acid";
        risk = "High hydrocyanic acid & fibrous anti-nutritional factor risk";
      } else if (ingNameLower.includes("salt") && currentPercent > 0.5) {
        riskKey = "risk_salt_toxicity";
        risk = "Risk of hypernatremia / salt toxicity; ensure unlimited fresh water";
      } else if (ingNameLower.includes("fish") && currentPercent > 10.0) {
        riskKey = "risk_fishy_taint";
        risk = "Fishy taint risk in meat quality and high sodium / mineral load";
      } else if (ingNameLower.includes("wheat bran") || ingNameLower.includes("rice bran")) {
        riskKey = "risk_excess_fiber";
        risk = "Excess dietary fiber impairs nutrient digestion and feed conversion";
      } else if (ingNameLower.includes("bone meal") || ingNameLower.includes("dcp")) {
        riskKey = "risk_excess_mineral";
        risk = "Excess mineral inclusion can disrupt calcium-to-phosphorus absorption";
      }

      alerts.push({
        ingredientName: ing.name,
        currentPercent,
        maxAllowedPercent: maxLimit,
        stage,
        riskDescription: risk,
        riskKey,
      });
    }
  });

  return alerts;
}

/**
 * Intake rate calculations based on pig counts and daily guidelines
 * Matches Android FeedViewModel.kt lines 907-935
 */
export function calculateRequirements(stats: { [key: string]: number }, days: number = 1): { [key: string]: number } {
  const rates: { [stage: string]: number } = {
    Starter: 0.7,
    Grower: 1.8,
    Finisher: 2.5,
    breeders_starter: 0.7,
    breeders_grower: 1.8,
    gilts: 2.2,
    boars: 2.2,
    sows: 2.2,
    Pregnant: 2.2,
    Lactating: 5.5,
  };

  const results: { [key: string]: number } = {};
  let totalDaily = 0.0;

  Object.entries(rates).forEach(([stage, rate]) => {
    const count = stats[stage] ?? 0;
    const dailyAmount = count * rate;
    if (dailyAmount > 0) {
      const displayLabel = `${stage.charAt(0).toUpperCase() + stage.slice(1)} (${count})`;
      results[displayLabel] = dailyAmount * days;
      totalDaily += dailyAmount;
    }
  });

  results["__days"] = days;
  if (days > 1) {
    results["Daily Total"] = totalDaily;
    results["Total for " + days + " Days"] = totalDaily * days;
  } else {
    results["Total Daily Requirement"] = totalDaily;
  }

  return results;
}

/**
 * Comprehensive nutritional analysis matching Android FeedViewModel.kt calculateNutritionalContent
 */
export function analyzeFeedMix(
  items: { ingredient: FeedIngredient; quantity: number }[],
  isPercentageMode: boolean = false,
  isDryMatterMode: boolean = false
): FeedNutrientProfile {
  if (items.length === 0) {
    return {
      crudeProtein: 0,
      metabolizableEnergy: 0,
      digestibleProtein: 0,
      crudeFiber: 0,
      calcium: 0,
      phosphorus: 0,
      lysine: 0,
      methionine: 0,
      totalWeight: 0,
      costPerKg: 0,
      costPer50kgBag: 0,
      totalCost: 0,
      caPRatio: 0,
    };
  }

  const totalQty = Math.max(0.0001, items.reduce((sum, item) => sum + item.quantity, 0));

  let cp = 0;
  let me = 0;
  let cf = 0;
  let ca = 0;
  let p = 0;
  let lys = 0;
  let met = 0;
  let weightedDm = 0;
  let totalCost = 0;

  items.forEach(({ ingredient: ing, quantity: qty }) => {
    const fraction = qty / totalQty;
    const effectiveQty = isPercentageMode ? (qty / 100.0) * 100.0 : qty;

    cp += ing.crudeProtein * fraction;
    me += ing.metabolizableEnergy * fraction;
    cf += ing.crudeFiber * fraction;

    const caPct = ing.calcium > 50.0 ? ing.calcium / 10.0 : ing.calcium;
    const pPct = ing.phosphorus > 50.0 ? ing.phosphorus / 10.0 : ing.phosphorus;
    ca += caPct * fraction;
    p += pPct * fraction;

    const isPureLys = ing.name.toLowerCase().includes("lysine") || (ing.crudeProtein > 80.0 && ing.lysine > 50.0);
    const ingDietaryLys = isPureLys ? ing.lysine : ing.crudeProtein * (ing.lysine / 100.0);
    lys += ingDietaryLys * fraction;

    const isPureMet =
      ing.name.toLowerCase().includes("methionine") || (ing.crudeProtein > 80.0 && ing.methionine + ing.cystine > 50.0);
    const ingDietaryMet = isPureMet ? ing.methionine + ing.cystine : ing.crudeProtein * ((ing.methionine + ing.cystine) / 100.0);
    met += ingDietaryMet * fraction;

    const dmVal = ing.dryMatter && ing.dryMatter > 0 ? ing.dryMatter : 90.0;
    weightedDm += dmVal * fraction;

    if (ing.costPerKg && ing.costPerKg > 0) {
      totalCost += ing.costPerKg * effectiveQty;
    }
  });

  const totalWeight = isPercentageMode ? 100.0 : items.reduce((sum, item) => sum + item.quantity, 0);
  const costPerKg = isPercentageMode
    ? items.reduce((sum, { ingredient: ing, quantity: qty }) => sum + (qty / totalQty) * (ing.costPerKg > 0 ? ing.costPerKg : 0), 0)
    : totalQty > 0.0001
    ? totalCost / totalQty
    : 0;
  const costPer50kgBag = costPerKg * 50.0;
  const caPRatio = p > 0.0001 ? ca / p : 0;

  // Dry Matter conversion if requested
  const dmFactor = isDryMatterMode && weightedDm > 0 ? 100.0 / weightedDm : 1.0;
  const finalCp = cp * dmFactor;
  const finalMe = me * dmFactor;
  const finalCf = cf * dmFactor;
  const finalCa = ca * dmFactor;
  const finalP = p * dmFactor;
  const finalLys = lys * dmFactor;
  const finalMet = met * dmFactor;
  const finalDp = finalCp * 0.85;

  return {
    crudeProtein: finalCp,
    metabolizableEnergy: finalMe,
    digestibleProtein: finalDp,
    crudeFiber: finalCf,
    calcium: finalCa,
    phosphorus: finalP,
    lysine: finalLys,
    methionine: finalMet,
    totalWeight,
    costPerKg,
    costPer50kgBag,
    totalCost: isPercentageMode ? costPerKg * 100.0 : totalCost,
    caPRatio,
  };
}
