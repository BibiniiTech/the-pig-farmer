"use client";

import React from "react";
import ReportLayout from "./ReportLayout";
import { FeedNutrientProfile, InclusionSafetyAlert } from "@/lib/feedCalculator";
import { NutritionalRequirement } from "@/lib/types";

interface AnalyzedIngredient {
  name: string;
  quantity: number;
}

interface FeedAnalysisReportProps {
  targetStage: string;
  ingredients: AnalyzedIngredient[];
  isPercentage: boolean;
  totalWeight: number;
  profile: FeedNutrientProfile;
  target: NutritionalRequirement;
  alerts?: InclusionSafetyAlert[];
  currencySymbol?: string;
}

const FeedAnalysisReport: React.FC<FeedAnalysisReportProps> = ({
  targetStage,
  ingredients,
  isPercentage,
  totalWeight,
  profile,
  target,
  alerts = [],
  currencySymbol = "$",
}) => {
  const unitLabel = isPercentage ? "%" : "kg";
  const sumQty = Math.max(0.0001, totalWeight);

  const targetCrudeProtein = target.digestibleProtein ? target.digestibleProtein / 0.85 : 16.0;
  const targetDigestibleProtein = target.digestibleProtein || 14.0;
  const targetME = target.metabolizableEnergy || 3200;
  const targetFiber = target.crudeFiber || 5.0;
  const targetCa = target.calcium || 0.75;
  const targetP = target.phosphorus || 0.50;
  const targetLysine = target.lysine || 6.0;
  const targetMethionine = target.methionineCystine || 3.5;

  const comparisons = [
    { name: "Crude Protein (%)", targetVal: targetCrudeProtein, actVal: profile.crudeProtein },
    { name: "Digestible Protein (%)", targetVal: targetDigestibleProtein, actVal: profile.digestibleProtein },
    { name: "ME (kcal/kg)", targetVal: targetME, actVal: profile.metabolizableEnergy },
    { name: "Crude Fiber (%)", targetVal: targetFiber, actVal: profile.crudeFiber },
    { name: "Calcium (%)", targetVal: targetCa, actVal: profile.calcium },
    { name: "Phosphorus (%)", targetVal: targetP, actVal: profile.phosphorus },
    { name: "Lysine (% diet)", targetVal: targetLysine, actVal: profile.lysine },
    { name: "Methionine+Cys (% diet)", targetVal: targetMethionine, actVal: profile.methionine },
  ];

  const ratio = profile.caPRatio || (profile.phosphorus > 0 ? profile.calcium / profile.phosphorus : 0);
  let ratioStatus = "Excessive (Mineral Binding Risk)";
  let ratioColor = "text-[#D32F2F]";
  if (ratio >= 1.2 && ratio <= 1.5) {
    ratioStatus = "Optimal (1.2:1 - 1.5:1)";
    ratioColor = "text-[#2E7D32]";
  } else if (ratio >= 1.0 && ratio <= 2.0) {
    ratioStatus = "Acceptable (1.0:1 - 2.0:1)";
    ratioColor = "text-[#F57F17]";
  } else if (ratio < 1.0) {
    ratioStatus = "Deficient (Inverted Ca:P Risk)";
    ratioColor = "text-[#D32F2F]";
  }

  return (
    <ReportLayout title={`Feed Analysis Report - ${targetStage}`}>
      <div className="space-y-6">
        {/* Ration Composition */}
        <div>
          <h3 className="text-[16pt] font-bold text-black mb-2">Ration Composition</h3>
          <table className="w-full border-collapse text-[10pt] border border-zinc-400">
            <thead>
              <tr className="bg-[#D3D3D3] text-black font-bold text-left border-b border-zinc-400">
                <th className="p-2 border border-zinc-400 font-bold w-[50%]">Ingredient</th>
                <th className="p-2 border border-zinc-400 font-bold text-right w-[25%]">Amount</th>
                <th className="p-2 border border-zinc-400 font-bold text-right w-[25%]">Inclusion Rate</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-300">
              {ingredients.map((ing, idx) => {
                const pct = isPercentage ? ing.quantity : (ing.quantity / sumQty) * 100.0;
                return (
                  <tr key={idx} className="text-black">
                    <td className="p-2 border border-zinc-300">{ing.name}</td>
                    <td className="p-2 border border-zinc-300 text-right font-mono">
                      {ing.quantity.toFixed(2)} {unitLabel}
                    </td>
                    <td className="p-2 border border-zinc-300 text-right font-mono">{pct.toFixed(1)}%</td>
                  </tr>
                );
              })}
              <tr className="font-bold text-black border-t-2 border-zinc-400">
                <td className="p-2 border border-zinc-400 font-bold">TOTAL</td>
                <td className="p-2 border border-zinc-400 text-right font-mono font-bold">
                  {totalWeight.toFixed(1)} {unitLabel}
                </td>
                <td className="p-2 border border-zinc-400 text-right font-mono font-bold">100.0%</td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* Cost Analysis (if costPerKg > 0) */}
        {Boolean(profile.costPerKg && profile.costPerKg > 0) && (
          <div>
            <h3 className="text-[16pt] font-bold text-black mb-2">Cost Analysis</h3>
            <table className="w-full border-collapse text-[10pt] border border-zinc-400">
              <thead>
                <tr className="bg-[#D3D3D3] text-black font-bold text-left border-b border-zinc-400">
                  <th className="p-2 border border-zinc-400 font-bold w-[50%]">Metric</th>
                  <th className="p-2 border border-zinc-400 font-bold text-right w-[50%]">Cost</th>
                </tr>
              </thead>
              <tbody>
                <tr className="text-black">
                  <td className="p-2 border border-zinc-300">Cost/kg</td>
                  <td className="p-2 border border-zinc-300 text-right font-mono font-bold">
                    {currencySymbol}{(profile.costPerKg || 0).toFixed(2)}
                  </td>
                </tr>
                <tr className="text-black">
                  <td className="p-2 border border-zinc-300">Cost/50kg Bag</td>
                  <td className="p-2 border border-zinc-300 text-right font-mono font-bold">
                    {currencySymbol}{(profile.costPer50kgBag || 0).toFixed(2)}
                  </td>
                </tr>
                {!isPercentage && totalWeight > 0 && (
                  <tr className="font-bold text-black">
                    <td className="p-2 border border-zinc-300 font-bold">Batch Total Cost</td>
                    <td className="p-2 border border-zinc-300 text-right font-mono font-bold">
                      {currencySymbol}{(profile.totalCost || 0).toFixed(2)}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Nutritional Analysis */}
        <div>
          <h3 className="text-[16pt] font-bold text-black mb-2">
            Nutritional Analysis ({targetStage})
          </h3>
          <table className="w-full border-collapse text-[10pt] border border-zinc-400">
            <thead>
              <tr className="bg-[#D3D3D3] text-black font-bold text-left border-b border-zinc-400">
                <th className="p-2 border border-zinc-400 font-bold w-[40%]">Nutrient</th>
                <th className="p-2 border border-zinc-400 font-bold text-right w-[20%]">Target</th>
                <th className="p-2 border border-zinc-400 font-bold text-right w-[20%]">Actual</th>
                <th className="p-2 border border-zinc-400 font-bold text-center w-[20%]">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-300">
              {comparisons.map((c, idx) => {
                const isFiber = c.name.toLowerCase().includes("fiber");
                const isAdequate = isFiber
                  ? c.actVal <= c.targetVal * 1.05
                  : c.actVal >= c.targetVal * 0.95;
                const statusText = c.actVal <= 0.001 ? "-" : isAdequate ? "OK" : isFiber ? "Excess" : "Deficient";
                const colorClass = isAdequate ? "text-[#2E7D32]" : "text-[#D32F2F]";

                return (
                  <tr key={idx} className="text-black">
                    <td className="p-2 border border-zinc-300 font-medium">{c.name}</td>
                    <td className="p-2 border border-zinc-300 text-right font-mono">{c.targetVal.toFixed(2)}</td>
                    <td className={`p-2 border border-zinc-300 text-right font-mono font-bold ${colorClass}`}>
                      {c.actVal.toFixed(2)}
                    </td>
                    <td className={`p-2 border border-zinc-300 text-center font-bold ${colorClass}`}>
                      {statusText}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          {/* Calcium-to-Phosphorus Ratio */}
          <p className={`mt-3 text-[11pt] font-bold ${ratioColor}`}>
            Calcium-to-Phosphorus Ratio (Ca:P): {ratio.toFixed(2)}:1 — {ratioStatus}
          </p>
        </div>

        {/* Veterinary Safety Alerts */}
        {alerts.length > 0 && (
          <div>
            <h3 className="text-[16pt] font-bold text-black mb-2">Veterinary Safety Alerts</h3>
            <table className="w-full border-collapse text-[10pt] border border-zinc-400">
              <thead>
                <tr className="bg-[#D3D3D3] text-black font-bold text-left border-b border-zinc-400">
                  <th className="p-2 border border-zinc-400 font-bold w-[25%]">Ingredient</th>
                  <th className="p-2 border border-zinc-400 font-bold text-right w-[15%]">Actual</th>
                  <th className="p-2 border border-zinc-400 font-bold text-right w-[15%]">Max Limit</th>
                  <th className="p-2 border border-zinc-400 font-bold w-[45%]">Risk / Clinical Guidance</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-300">
                {alerts.map((alert, idx) => (
                  <tr key={idx} className="text-black">
                    <td className="p-2 border border-zinc-300 font-bold">{alert.ingredientName}</td>
                    <td className="p-2 border border-zinc-300 text-right font-mono">{alert.currentPercent.toFixed(1)}%</td>
                    <td className="p-2 border border-zinc-300 text-right font-mono">{alert.maxAllowedPercent.toFixed(1)}%</td>
                    <td className="p-2 border border-zinc-300 text-[#D32F2F] text-[9.5pt] leading-tight">
                      {alert.riskDescription}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Disclaimer */}
        <p className="pt-6 text-center text-[11pt] font-bold text-[#D32F2F] leading-relaxed">
          Disclaimer: Consult a Vet before use
        </p>
      </div>
    </ReportLayout>
  );
};

export default FeedAnalysisReport;
