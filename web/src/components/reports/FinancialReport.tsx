"use client";

import React from "react";
import ReportLayout from "./ReportLayout";
import { useTranslations } from "next-intl";

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
  title
}) => {
  const t = useTranslations("Reports");
  const tFin = useTranslations("Financials");
  const defaultTitle = t("financialLedger");

  const translateCategory = (cat: string, type: string) => {
    if (!cat) return "";
    const incomeKeys: Record<string, string> = {
      "Pig Sale": "incomeCategories.pigSale",
      "Manure Sale": "incomeCategories.manureSale",
      "Breeding Service": "incomeCategories.breedingService",
      "Equipment Sale": "incomeCategories.equipmentSale",
      "Other": "incomeCategories.other",
    };
    const expenseKeys: Record<string, string> = {
      "Feed": "expenseCategories.feed",
      "Vet/Medication": "expenseCategories.vet",
      "Vet": "expenseCategories.vet",
      "Labor/Salary": "expenseCategories.labor",
      "Labor": "expenseCategories.labor",
      "Equipment": "expenseCategories.equipment",
      "Transport": "expenseCategories.transport",
      "Rent": "expenseCategories.rent",
      "Utility": "expenseCategories.utility",
      "Other": "expenseCategories.other",
    };
    const key = type === "Income" ? incomeKeys[cat] : expenseKeys[cat];
    return key ? tFin(key) : cat;
  };

  const totalIncome = records.filter(r => r.type === "Income").reduce((sum, r) => sum + r.amount, 0);
  const totalExpense = records.filter(r => r.type === "Expense").reduce((sum, r) => sum + r.amount, 0);
  const netProfit = totalIncome - totalExpense;

  const sortedRecords = [...records].sort((a, b) => a.date.localeCompare(b.date));

  let runningBalance = 0;

  return (
    <ReportLayout title={title || defaultTitle}>
      <div className="mb-10 space-y-3 p-6 border border-zinc-200 rounded-2xl bg-zinc-50/50">
        <h3 className="text-[16pt] font-bold text-zinc-800 border-b pb-2 mb-4">{t("summary")}</h3>
        <div className="grid grid-cols-3 gap-8">
          <div>
            <p className="text-[10pt] uppercase text-zinc-500 font-bold tracking-wider">{t("totalIncome")}</p>
            <p className="text-[18pt] font-black text-emerald-700">{currencySymbol}{totalIncome.toFixed(2)}</p>
          </div>
          <div>
            <p className="text-[10pt] uppercase text-zinc-500 font-bold tracking-wider">{t("totalExpense")}</p>
            <p className="text-[18pt] font-black text-rose-700">{currencySymbol}{totalExpense.toFixed(2)}</p>
          </div>
          <div>
            <p className="text-[10pt] uppercase text-zinc-500 font-bold tracking-wider">{t("netBalance")}</p>
            <p className={`text-[18pt] font-black ${netProfit >= 0 ? "text-emerald-800" : "text-rose-800"}`}>
              {currencySymbol}{netProfit.toFixed(2)}
            </p>
          </div>
        </div>
      </div>

      <div className="space-y-4">
        <h3 className="text-[14pt] font-bold text-zinc-800">{t("detailedHistory")}</h3>
        <table className="w-full border-collapse text-[9pt]">
          <thead>
            <tr className="bg-zinc-100 text-left border-y-2 border-zinc-300">
              <th className="p-3 font-bold border-r border-zinc-200">{t("date")}</th>
              <th className="p-3 font-bold border-r border-zinc-200">{t("narration")}</th>
              <th className="p-3 font-bold border-r border-zinc-200 w-1/3">{t("description")}</th>
              <th className="p-3 font-bold border-r border-zinc-200 text-right">{t("totalIncome")}</th>
              <th className="p-3 font-bold border-r border-zinc-200 text-right">{t("totalExpense")}</th>
              <th className="p-3 font-bold text-right">{t("balance")}</th>
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
                  <td className="p-3 border-r border-zinc-100 font-bold text-zinc-700">{translateCategory(record.category, record.type)}</td>
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
