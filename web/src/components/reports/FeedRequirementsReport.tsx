"use client";

import React from "react";
import ReportLayout from "./ReportLayout";

export interface FeedRequirementBreakdownItem {
  category: string;
  count?: number;
  dailyTotal: number;
  periodTotal?: number;
}

interface FeedRequirementsReportProps {
  days?: number;
  breakdown: FeedRequirementBreakdownItem[];
  totalDaily: number;
  totalPeriod?: number;
  title?: string;
}

const FeedRequirementsReport: React.FC<FeedRequirementsReportProps> = ({
  days = 1,
  breakdown,
  totalDaily,
  totalPeriod,
  title = "Feed Requirements Report",
}) => {
  const finalDays = days > 0 ? days : 1;
  const finalPeriod = totalPeriod !== undefined ? totalPeriod : totalDaily * finalDays;
  const durationLabel = finalDays > 1 ? `${finalDays} Days` : "Daily (kg)";

  return (
    <ReportLayout title={title}>
      <div className="space-y-4">
        <table className="w-full border-collapse text-[10pt] border border-zinc-400">
          <thead>
            <tr className="bg-[#D3D3D3] text-black font-bold text-left border-b border-zinc-400">
              <th className="p-2 border border-zinc-400 font-bold w-[60%]">Growth Stage</th>
              <th className="p-2 border border-zinc-400 font-bold text-right w-[20%]">Daily</th>
              <th className="p-2 border border-zinc-400 font-bold text-right w-[20%]">{durationLabel}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-300">
            {breakdown.length === 0 ? (
              <tr>
                <td colSpan={3} className="p-4 text-center text-zinc-500 italic border border-zinc-300">
                  No feed requirement entries to display.
                </td>
              </tr>
            ) : (
              breakdown.map((row, idx) => {
                const label = row.count && row.count > 0 && !row.category.includes("(")
                  ? `${row.category} (${row.count})`
                  : row.category;
                const dailyVal = row.dailyTotal || 0;
                const periodVal = row.periodTotal !== undefined ? row.periodTotal : dailyVal * finalDays;

                return (
                  <tr key={idx} className="text-black">
                    <td className="p-2 border border-zinc-300">{label}</td>
                    <td className="p-2 border border-zinc-300 text-right font-mono">{dailyVal.toFixed(1)}</td>
                    <td className="p-2 border border-zinc-300 text-right font-mono">{periodVal.toFixed(1)}</td>
                  </tr>
                );
              })
            )}
            <tr className="font-bold text-black border-t-2 border-zinc-400">
              <td className="p-2 border border-zinc-400 font-bold">TOTAL</td>
              <td className="p-2 border border-zinc-400 text-right font-mono font-bold">{totalDaily.toFixed(1)}</td>
              <td className="p-2 border border-zinc-400 text-right font-mono font-bold">{finalPeriod.toFixed(1)}</td>
            </tr>
          </tbody>
        </table>

        {/* Disclaimer exactly matching Android tr("disclaimer_vet") */}
        <p className="pt-6 text-center text-[12pt] font-bold text-[#D32F2F] leading-relaxed">
          Disclaimer: Consult a Vet before use
        </p>
      </div>
    </ReportLayout>
  );
};

export default FeedRequirementsReport;
