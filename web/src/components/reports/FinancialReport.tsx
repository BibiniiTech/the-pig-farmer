"use client";

import React from "react";
import ReportLayout from "./ReportLayout";
import { resolvePigIds } from "@/lib/pdfExporter";
import { Pig, FinancialRecord } from "@/lib/types";

interface FinancialReportProps {
  records: FinancialRecord[];
  pigs?: Pig[];
  currencySymbol?: string;
  title?: string;
}

const FinancialReport: React.FC<FinancialReportProps> = ({
  records,
  pigs = [],
  currencySymbol = "$",
  title = "Financial Summary Report",
}) => {
  const totalIncome = records.filter((r) => r.type === "Income").reduce((sum, r) => sum + r.amount, 0);
  const totalExpense = records.filter((r) => r.type === "Expense").reduce((sum, r) => sum + r.amount, 0);
  const netProfit = totalIncome - totalExpense;

  // Sort by date ascending matching Android DateUtils.parseAnyDateNonNull
  const sortedRecords = [...records].sort((a, b) => a.date.localeCompare(b.date));

  let runningBalance = 0;
  const rows = sortedRecords.map((record) => {
    const isIncome = record.type === "Income";
    const incomeVal = isIncome ? record.amount : 0;
    const expenseVal = !isIncome ? record.amount : 0;
    runningBalance += (incomeVal - expenseVal);

    const displayDescription = resolvePigIds(record.description || "", pigs);

    return {
      date: record.date,
      category: record.category || "General",
      description: displayDescription,
      incomeVal,
      expenseVal,
      balance: runningBalance,
    };
  });

  return (
    <ReportLayout title={title}>
      <div className="space-y-6">
        {/* Summary section (matching Android addSection("Summary") lines 535-540) */}
        <div>
          <h3 className="text-[16pt] font-bold text-black mb-2">Summary</h3>
          <div className="space-y-1 text-[11pt] text-black">
            <p>Total Income: {currencySymbol}{totalIncome.toFixed(2)}</p>
            <p>Total Expense: {currencySymbol}{totalExpense.toFixed(2)}</p>
            <p
              className={`font-bold ${
                netProfit >= 0 ? "text-[#2E7D32]" : "text-[#D32F2F]"
              }`}
            >
              Net Profit/Loss: {currencySymbol}{netProfit.toFixed(2)}
            </p>
          </div>
        </div>

        {/* Detailed Transaction History table (matching Android lines 541-670) */}
        <div>
          <h3 className="text-[16pt] font-bold text-black mb-2">
            Detailed Transaction History
          </h3>
          <table className="w-full border-collapse text-[9.5pt] border border-zinc-400">
            <thead>
              <tr className="bg-[#D3D3D3] text-black font-bold text-left border-b border-zinc-400">
                <th className="p-2 border border-zinc-400 font-bold w-[14%]">Date</th>
                <th className="p-2 border border-zinc-400 font-bold w-[16%]">Narration</th>
                <th className="p-2 border border-zinc-400 font-bold w-[34%]">Description</th>
                <th className="p-2 border border-zinc-400 font-bold text-right w-[12%]">Income</th>
                <th className="p-2 border border-zinc-400 font-bold text-right w-[12%]">Expense</th>
                <th className="p-2 border border-zinc-400 font-bold text-right w-[12%]">Balance</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-300">
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-4 text-center text-zinc-500 italic border border-zinc-300">
                    No financial records found.
                  </td>
                </tr>
              ) : (
                rows.map((row, idx) => (
                  <tr key={idx} className="text-black">
                    <td className="p-2 border border-zinc-300 font-mono text-[9pt]">{row.date}</td>
                    <td className="p-2 border border-zinc-300">{row.category}</td>
                    <td className="p-2 border border-zinc-300 leading-snug">{row.description}</td>
                    <td className="p-2 border border-zinc-300 text-right font-mono text-[#2E7D32]">
                      {row.incomeVal > 0 ? row.incomeVal.toFixed(2) : ""}
                    </td>
                    <td className="p-2 border border-zinc-300 text-right font-mono text-[#D32F2F]">
                      {row.expenseVal > 0 ? row.expenseVal.toFixed(2) : ""}
                    </td>
                    <td className="p-2 border border-zinc-400 text-right font-mono font-bold">
                      {row.balance.toFixed(2)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </ReportLayout>
  );
};

export default FinancialReport;
