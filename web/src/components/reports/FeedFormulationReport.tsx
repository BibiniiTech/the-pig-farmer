"use client";

import React from "react";
import ReportLayout from "./ReportLayout";

export interface FormulationNutrientItem {
  label: string;
  target: number;
  actual: number;
  isDeficient?: boolean;
}

export interface FormulationIngredientItem {
  name: string;
  percent: number;
}

export interface FormulationAdditiveItem {
  name: string;
  percent: number;
  note: string;
}

interface FeedFormulationReportProps {
  title: string;
  ingredients: FormulationIngredientItem[];
  additives?: FormulationAdditiveItem[];
  total: number;
  nutritionalComparison: FormulationNutrientItem[];
  costPerKg?: number;
  costPer50kgBag?: number;
  currencySymbol?: string;
}

const FeedFormulationReport: React.FC<FeedFormulationReportProps> = ({
  title,
  ingredients,
  additives = [],
  total,
  nutritionalComparison,
  costPerKg = 0,
  costPer50kgBag = 0,
  currencySymbol = "$",
}) => {
  const final50kgCost = costPer50kgBag > 0 ? costPer50kgBag : costPerKg * 50;

  return (
    <ReportLayout title={`Feed Formulation Report - ${title}`}>
      <div className="space-y-6">
        {/* Ingredients Composition */}
        <div>
          <h3 className="text-[16pt] font-bold text-black mb-2">Ingredients Composition</h3>
          <table className="w-full border-collapse text-[10pt] border border-zinc-400">
            <thead>
              <tr className="bg-[#D3D3D3] text-black font-bold text-left border-b border-zinc-400">
                <th className="p-2 border border-zinc-400 font-bold w-[50%]">Ingredient</th>
                <th className="p-2 border border-zinc-400 font-bold text-right w-[20%]">Percentage (%)</th>
                <th className="p-2 border border-zinc-400 font-bold text-right w-[30%]">Qty (based on Batch Size)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-300">
              {ingredients.map((item, idx) => (
                <tr key={idx} className="text-black">
                  <td className="p-2 border border-zinc-300">{item.name}</td>
                  <td className="p-2 border border-zinc-300 text-right font-mono">{item.percent.toFixed(1)}%</td>
                  <td className="p-2 border border-zinc-300 text-right font-mono">
                    {((item.percent / 100.0) * 1000).toFixed(1)} kg
                  </td>
                </tr>
              ))}
              {additives.map((item, idx) => (
                <tr key={`add-${idx}`} className="text-black">
                  <td className="p-2 border border-zinc-300">{item.name}</td>
                  <td className="p-2 border border-zinc-300 text-right font-mono">{item.percent.toFixed(1)}%</td>
                  <td className="p-2 border border-zinc-300 text-right text-xs text-zinc-600">{item.note}</td>
                </tr>
              ))}
              <tr className="font-bold text-black border-t-2 border-zinc-400">
                <td className="p-2 border border-zinc-400 font-bold">TOTAL</td>
                <td className="p-2 border border-zinc-400 text-right font-mono font-bold">{total.toFixed(1)}%</td>
                <td className="p-2 border border-zinc-400 text-right font-mono font-bold">1000.0 kg</td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* Cost Analysis (if costPerKg > 0) */}
        {costPerKg > 0 && (
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
                    {currencySymbol}{costPerKg.toFixed(2)}
                  </td>
                </tr>
                <tr className="text-black">
                  <td className="p-2 border border-zinc-300">Cost/50kg Bag</td>
                  <td className="p-2 border border-zinc-300 text-right font-mono font-bold">
                    {currencySymbol}{final50kgCost.toFixed(2)}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        )}

        {/* Formula Incomplete Warning */}
        {total < 99.9 && (
          <p className="text-[12pt] font-bold text-[#D32F2F]">
            Formula incomplete: total inclusion is only {total.toFixed(1)}%.
          </p>
        )}

        {/* Nutritional Analysis */}
        <div>
          <h3 className="text-[16pt] font-bold text-black mb-2">Nutritional Analysis</h3>
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
              {nutritionalComparison.map((item, idx) => {
                const isDeficient = Boolean(item.isDeficient);
                return (
                  <tr key={idx} className="text-black">
                    <td className="p-2 border border-zinc-300 font-medium">{item.label}</td>
                    <td className="p-2 border border-zinc-300 text-right font-mono">{item.target.toFixed(2)}</td>
                    <td
                      className={`p-2 border border-zinc-300 text-right font-mono font-bold ${
                        isDeficient ? "text-[#D32F2F]" : "text-[#2E7D32]"
                      }`}
                    >
                      {item.actual.toFixed(2)}
                    </td>
                    <td
                      className={`p-2 border border-zinc-300 text-center font-bold ${
                        isDeficient ? "text-[#D32F2F]" : "text-[#2E7D32]"
                      }`}
                    >
                      {isDeficient ? "DEFICIENT" : "OK"}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* Disclaimer */}
        <p className="pt-6 text-center text-[12pt] font-bold text-[#D32F2F] leading-relaxed">
          Disclaimer: Consult a Vet before use
        </p>
      </div>
    </ReportLayout>
  );
};

export default FeedFormulationReport;
