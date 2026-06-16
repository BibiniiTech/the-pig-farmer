export interface FeedIngredient {
  id: string;
  name: string;
  crudeProtein: number;
  crudeFiber: number;
  calcium: number;
  phosphorus: number;
  sodium: number;
  chloride: number;
  potassium: number;
  sulfur: number;
  metabolizableEnergy: number; // ME (kcal/kg)
  dryMatter: number;
  fat: number;
  lysine: number;
  methionine: number;
  cystine: number;
  threonine: number;
  tryptophan: number;
  arginine: number;
  isoleucine: number;
  valine: number;
  category: string;
  description: string;
  quantity: number;
  unit: string;
  costPerKg: number;
  mainCategory: string; // Energy, Protein, Vitamins, Minerals & Salt
  visible: boolean;
  maxStarter: number;
  maxGrower: number;
  maxFinisher: number;
}

export interface NutritionalRequirement {
  stage: string;
  digestibleProtein: number; // as %
  metabolizableEnergy: number; // ME (kcal/kg)
  calcium: number; // as %
  phosphorus: number; // as %
  lysine: number; // as % ptn
  methionineCystine: number; // as % ptn
  tryptophan: number; // as % ptn
  crudeFiber: number; // as %
  minDailyFeed: number; // kg/day
  maxDailyFeed: number; // kg/day
}

export interface FormulationResult {
  ingredients: { [id: string]: number }; // Maps ingredient ID -> percent
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

/**
 * Port of the Android Kotlin Feed formulation Pearson Square & deficit balancing algorithm
 */
export function formulateFeed(
  targetRequirement: NutritionalRequirement,
  allIngredients: FeedIngredient[],
  selectedIds: string[],
  shuffle: boolean = false
): FormulationResult {
  const targetProtein = targetRequirement.digestibleProtein;
  const targetStage = targetRequirement.stage;
  
  const selectedIngredients = allIngredients.filter(ing => selectedIds.includes(ing.id));
  if (selectedIngredients.length === 0) {
    return {
      ingredients: {},
      targetRequirement,
      nutritionalComparison: [],
      totalPercentage: 0,
      error: "Please select at least one ingredient to formulate."
    };
  }

  const mandatoryNames = ["Mycotoxin Binder", "Common salt", "Vitamin Premix"];
  const mandatoryInclusions: { [name: string]: number } = {
    "Mycotoxin Binder": 1.0,
    "Common salt": 0.5,
    "Vitamin Premix": 1.0,
  };

  const currentUsed: { [id: string]: number } = {};
  let availablePercent = 100.0;

  // Add mandatory items first
  mandatoryNames.forEach(name => {
    const ingredient = allIngredients.find(ing => ing.name.toLowerCase().includes(name.toLowerCase()));
    const percent = mandatoryInclusions[name];
    if (ingredient) {
      currentUsed[ingredient.id] = percent;
    } else {
      currentUsed[name] = percent; // fallback using name if matching ID is not in user list
    }
    availablePercent -= percent;
  });

  const supplementalCategory = "Vitamins, Minerals & Salt";
  const supplementalIngredients = selectedIngredients.filter(
    ing => ing.mainCategory === supplementalCategory && 
    !mandatoryNames.some(name => ing.name.toLowerCase().includes(name.toLowerCase()))
  );
  
  const mainIngredients = selectedIngredients.filter(
    ing => ing.mainCategory !== supplementalCategory ||
    mandatoryNames.some(name => ing.name.toLowerCase().includes(name.toLowerCase()))
  );

  // Veterinary limits based on target stage
  const limits: { [id: string]: number } = {};
  selectedIngredients.forEach(ing => {
    let limit = 100.0;
    switch (targetStage.toLowerCase()) {
      case "starter":
        limit = ing.maxStarter;
        break;
      case "grower":
        limit = ing.maxGrower;
        break;
      case "finisher":
        limit = ing.maxFinisher;
        break;
      case "sow":
      case "boar":
      case "pregnant":
      case "lactating":
        limit = Math.min(ing.maxGrower, ing.maxFinisher);
        break;
      default:
        limit = Math.min(ing.maxStarter, ing.maxGrower, ing.maxFinisher);
    }
    limits[ing.id] = limit;
  });

  // Initial pass: Diversity for Main Ingredients (excluding mandatories)
  mainIngredients.forEach(ing => {
    if (mandatoryNames.some(name => ing.name.toLowerCase().includes(name.toLowerCase()))) return;
    const name = ing.name.toLowerCase();
    const minInclusion = name.includes("bran") ? 8.0 : 4.0;
    const limit = limits[ing.id] ?? 50.0;
    const safeStart = Math.min(minInclusion, limit);

    if (availablePercent >= safeStart) {
      currentUsed[ing.id] = (currentUsed[ing.id] ?? 0) + safeStart;
      availablePercent -= safeStart;
    }
  });

  let remainingTotal = availablePercent;

  // Pearson Square Pools for Main Ingredients
  let poolLow = mainIngredients.filter(
    ing => ing.crudeProtein < targetProtein && 
    !mandatoryNames.some(name => ing.name.toLowerCase().includes(name.toLowerCase()))
  );
  let poolHigh = mainIngredients.filter(
    ing => ing.crudeProtein >= targetProtein && 
    !mandatoryNames.some(name => ing.name.toLowerCase().includes(name.toLowerCase()))
  );

  if (shuffle) {
    poolLow = [...poolLow].sort(() => Math.random() - 0.5);
    poolHigh = [...poolHigh].sort(() => Math.random() - 0.5);
  } else {
    poolLow = [...poolLow].sort((a, b) => b.metabolizableEnergy - a.metabolizableEnergy);
    poolHigh = [...poolHigh].sort((a, b) => b.metabolizableEnergy - a.metabolizableEnergy);
  }

  // Balance Main Mix to 100% using Pearson Square logic
  while (remainingTotal > 0.01 && poolLow.length > 0 && poolHigh.length > 0) {
    const low = poolLow[0];
    const high = poolHigh[0];

    const rLow = high.crudeProtein - targetProtein;
    const rHigh = targetProtein - low.crudeProtein;

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
    remainingTotal -= (useX + useY);

    if ((currentUsed[low.id] ?? 0.0) >= ((limits[low.id] ?? 100.0) - 0.01)) {
      poolLow.shift();
    }
    if ((currentUsed[high.id] ?? 0.0) >= ((limits[high.id] ?? 100.0) - 0.01)) {
      poolHigh.shift();
    }
  }

  // Spillover pass if remaining total exists
  if (remainingTotal > 0.01) {
    let remainingPool = poolHigh.length > 0 
      ? [...poolHigh].sort((a, b) => (shuffle ? Math.random() - 0.5 : (b.metabolizableEnergy / (b.crudeProtein + 1)) - (a.metabolizableEnergy / (a.crudeProtein + 1))))
      : [...poolLow].sort((a, b) => (shuffle ? Math.random() - 0.5 : b.metabolizableEnergy - a.metabolizableEnergy));

    for (const ing of remainingPool) {
      const cap = (limits[ing.id] ?? 100.0) - (currentUsed[ing.id] ?? 0.0);
      const use = Math.min(remainingTotal, cap);
      currentUsed[ing.id] = (currentUsed[ing.id] ?? 0.0) + use;
      remainingTotal -= use;
      if (remainingTotal <= 0.01) break;
    }
  }

  // SECONDARY PASS: Supplemental Deficits
  const getCaPercent = (ing: FeedIngredient) => ing.calcium > 10.0 ? ing.calcium / 10.0 : ing.calcium;
  const getPPercent = (ing: FeedIngredient) => ing.phosphorus > 10.0 ? ing.phosphorus / 10.0 : ing.phosphorus;

  const calculateCurrentNutrients = () => {
    let ca = 0.0, p = 0.0, lys = 0.0, met = 0.0;
    Object.entries(currentUsed).forEach(([id, percent]) => {
      const ing = allIngredients.find(i => i.id === id || i.name === id);
      if (!ing) return;
      const factor = percent / 100.0;
      ca += getCaPercent(ing) * factor;
      p += getPPercent(ing) * factor;
      lys += ing.lysine * factor;
      met += (ing.methionine + ing.cystine) * factor;
    });
    return { ca, p, lys, met };
  };

  const targetCa = targetRequirement.calcium;
  const targetP = targetRequirement.phosphorus;
  const targetLys = (targetRequirement.lysine / 100.0) * targetRequirement.digestibleProtein;
  const targetMet = (targetRequirement.methionineCystine / 100.0) * targetRequirement.digestibleProtein;

  let totalAddedSupplements = 0.0;
  const sortedSupplements = [...supplementalIngredients].sort((a, b) => {
    return (b.calcium + b.phosphorus + b.lysine + b.methionine) - (a.calcium + a.phosphorus + a.lysine + a.methionine);
  });

  for (const ing of sortedSupplements) {
    const current = calculateCurrentNutrients();
    const defCa = Math.max(0.0, targetCa - current.ca);
    const defP = Math.max(0.0, targetP - current.p);
    const defLys = Math.max(0.0, targetLys - current.lys);
    const defMet = Math.max(0.0, targetMet - current.met);

    if (defCa <= 0 && defP <= 0 && defLys <= 0 && defMet <= 0) break;

    const needCa = getCaPercent(ing) > 0 ? defCa / (getCaPercent(ing) / 100.0) : 0.0;
    const needP = getPPercent(ing) > 0 ? defP / (getPPercent(ing) / 100.0) : 0.0;
    const needLys = ing.lysine > 0 ? defLys / (ing.lysine / 100.0) : 0.0;
    const needMet = (ing.methionine + ing.cystine) > 0 ? defMet / ((ing.methionine + ing.cystine) / 100.0) : 0.0;

    let needed = Math.max(needCa, needP, needLys, needMet);
    const limit = limits[ing.id] ?? 2.0;
    needed = Math.min(needed, limit);

    if (needed > 0.01) {
      currentUsed[ing.id] = needed;
      totalAddedSupplements += needed;
    }
  }

  // Adjust main ingredients down to make room for supplemental additions
  if (totalAddedSupplements > 0) {
    const mandatoryTotal = Object.values(mandatoryInclusions).reduce((sum, v) => sum + v, 0);
    const mainTotal = 100.0 - mandatoryTotal - totalAddedSupplements;
    const previousMainTotal = 100.0 - mandatoryTotal;
    const scaleFactor = mainTotal / previousMainTotal;

    const mainIds = mainIngredients
      .map(ing => ing.id)
      .filter(id => {
        const ing = allIngredients.find(i => i.id === id);
        return ing && !mandatoryNames.some(name => ing.name.toLowerCase().includes(name.toLowerCase()));
      });

    mainIds.forEach(id => {
      if (currentUsed[id] !== undefined) {
        currentUsed[id] = currentUsed[id] * scaleFactor;
      }
    });
  }

  // Build nutritional comparison output
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
    finalNutrients.lys += ing.lysine * factor;
    finalNutrients.met += (ing.methionine + ing.cystine) * factor;
  });

  const nutritionalComparison = [
    {
      label: "Digestible Protein (%)",
      target: targetRequirement.digestibleProtein,
      actual: finalNutrients.protein,
      isDeficient: finalNutrients.protein < targetRequirement.digestibleProtein - 0.05
    },
    {
      label: "Crude Fiber (%)",
      target: targetRequirement.crudeFiber,
      actual: finalNutrients.fiber,
      isDeficient: finalNutrients.fiber > targetRequirement.crudeFiber + 0.5
    },
    {
      label: "Metabolizable Energy (kcal/kg)",
      target: targetRequirement.metabolizableEnergy,
      actual: finalNutrients.energy,
      isDeficient: finalNutrients.energy < targetRequirement.metabolizableEnergy - 100
    },
    {
      label: "Calcium (%)",
      target: targetRequirement.calcium,
      actual: finalNutrients.ca,
      isDeficient: finalNutrients.ca < targetRequirement.calcium - 0.02
    },
    {
      label: "Phosphorus (%)",
      target: targetRequirement.phosphorus,
      actual: finalNutrients.p,
      isDeficient: finalNutrients.p < targetRequirement.phosphorus - 0.02
    },
    {
      label: "Lysine (%)",
      target: targetLys,
      actual: finalNutrients.lys,
      isDeficient: finalNutrients.lys < targetLys - 0.02
    },
    {
      label: "Methionine + Cystine (%)",
      target: targetMet,
      actual: finalNutrients.met,
      isDeficient: finalNutrients.met < targetMet - 0.02
    }
  ];

  return {
    ingredients: finalUsed,
    targetRequirement,
    nutritionalComparison,
    totalPercentage: Math.min(100.0, totalPercentage),
    error: totalPercentage < 99.9 ? `Formulation incomplete. Total mix sums to ${totalPercentage.toFixed(1)}%. Try selecting more ingredients.` : undefined
  };
}

/**
 * Intake rate calculations based on pig counts and daily guidelines
 */
export function calculateRequirements(stats: { [key: string]: number }, days: number = 1): { [key: string]: number } {
  const rates: { [stage: string]: number } = {
    "Starter": 0.7,
    "Grower": 1.8,
    "Finisher": 2.5,
    "breeders_starter": 0.7,
    "breeders_grower": 1.8,
    "gilts": 2.2,
    "boars": 2.2,
    "sows": 2.2,
    "Pregnant": 2.2,
    "Lactating": 5.5,
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
