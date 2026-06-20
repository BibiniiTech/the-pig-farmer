"use client";

import React from "react";
import ReportLayout from "./ReportLayout";

interface FinancialRecord {
  id: string;
  date: string;
  type: string;
  category: string;
  amount: number;
  description: string;
  pigId?: string;
}

interface Pig {
  id: string;
  tagNumber: string;
}

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
  title = "Financial Summary Report"
}) => {
  const totalIncome = records.filter(r => r.type === "Income").reduce((sum, r) => sum + r.amount, 0);
  const totalExpense = records.filter(r => r.type === "Expense").reduce((sum, r) => sum + r.amount, 0);
  const netProfit = totalIncome - totalExpense;

  const sortedRecords = [...records].sort((a, b) => a.date.localeCompare(b.date));

  let runningBalance = 0;

  return (
    <ReportLayout title={title}>
      <div className="mb-10 space-y-3 p-6 border border-zinc-200 rounded-2xl bg-zinc-50/50">
        <h3 className="text-[16pt] font-bold text-zinc-800 border-b pb-2 mb-4">Executive Summary</h3>
        <div className="grid grid-cols-3 gap-8">
          <div>
            <p className="text-[10pt] uppercase text-zinc-500 font-bold tracking-wider">Total Income</p>
            <p className="text-[18pt] font-black text-emerald-700">{currencySymbol}{totalIncome.toFixed(2)}</p>
          </div>
          <div>
            <p className="text-[10pt] uppercase text-zinc-500 font-bold tracking-wider">Total Expense</p>
            <p className="text-[18pt] font-black text-rose-700">{currencySymbol}{totalExpense.toFixed(2)}</p>
          </div>
          <div>
            <p className="text-[10pt] uppercase text-zinc-500 font-bold tracking-wider">Net Profit/Loss</p>
            <p className={`text-[18pt] font-black ${netProfit >= 0 ? "text-emerald-800" : "text-rose-800"}`}>
              {currencySymbol}{netProfit.toFixed(2)}
            </p>
          </div>
        </div>
      </div>

      <div className="space-y-4">
        <h3 className="text-[14pt] font-bold text-zinc-800">Detailed Transaction History</h3>
        <table className="w-full border-collapse text-[9pt]">
          <thead>
            <tr className="bg-zinc-100 text-left border-y-2 border-zinc-300">
              <th className="p-3 font-bold border-r border-zinc-200">Date</th>
              <th className="p-3 font-bold border-r border-zinc-200">Narration</th>
              <th className="p-3 font-bold border-r border-zinc-200 w-1/3">Description</th>
              <th className="p-3 font-bold border-r border-zinc-200 text-right">Income</th>
              <th className="p-3 font-bold border-r border-zinc-200 text-right">Expense</th>
              <th className="p-3 font-bold text-right">Balance</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-200">
            {sortedRecords.map((record) => {
              const isIncome = record.type === "Income";
              const amount = record.amount;
              runningBalance += isIncome ? amount : -amount;

              const linkedPig = pigs.find(p => p.id === record.pigId);
              const displayDescription = linkedPig
                ? `${record.description} (Pig ${linkedPig.tagNumber})`
                : record.description;

              return (
                <tr key={record.id} className={isIncome ? "bg-emerald-50/20" : "bg-rose-50/20"}>
                  <td className="p-3 border-r border-zinc-100 font-mono">{record.date}</td>
                  <td className="p-3 border-r border-zinc-100 font-bold text-zinc-700">{record.category}</td>
                  <td className="p-3 border-r border-zinc-100 text-zinc-600 italic leading-snug">{displayDescription}</td>
                  <td className="p-3 border-r border-zinc-100 text-right font-bold text-emerald-700">
                    {isIncome ? amount.toFixed(2) : ""}
                  </td>
                  <td className="p-3 border-r border-zinc-100 text-right font-bold text-rose-700">
                    {!isIncome ? amount.toFixed(2) : ""}
                  </td>
                  <td className={`p-3 text-right font-black ${runningBalance >= 0 ? "text-emerald-900" : "text-rose-900"}`}>
                    {currencySymbol}{runningBalance.toFixed(2)}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </ReportLayout>
  );
};

export default FinancialReport;
