"use client";

import React from "react";
import ReportLayout from "./ReportLayout";
import { useTranslations } from "next-intl";

interface StaffMember {
  id: string;
  name: string;
  role: string;
  phone: string;
  salary: number;
  joinDate: string;
  status: string;
}

interface FinancialRecord {
  id: string;
  date: string;
  category: string;
  amount: number;
  description: string;
}

interface HRReportProps {
  staff: StaffMember[];
  financialRecords: FinancialRecord[];
  currencySymbol?: string;
  title?: string;
}

const HRReport: React.FC<HRReportProps> = ({
  staff,
  financialRecords,
  currencySymbol = "$",
  title
}) => {
  const t = useTranslations("Reports");
  const defaultTitle = t("hrDirectory");

  const totalMonthlyPayroll = staff.reduce((sum, m) => sum + m.salary, 0);
  const salaryPayments = financialRecords
    .filter(r => r.category === "Labor/Salary" || r.category === "Salary")
    .sort((a, b) => b.date.localeCompare(a.date));

  const totalPeriodPayments = salaryPayments.reduce((sum, r) => sum + r.amount, 0);

  return (
    <ReportLayout title={title || defaultTitle}>
      <div className="space-y-8">
        <div>
          <h3 className="text-[15pt] font-bold text-zinc-800 border-b-2 border-zinc-200 pb-2 mb-4">Staff Details</h3>
          <table className="w-full border-collapse text-[10pt]">
            <thead>
              <tr className="bg-zinc-100 text-left border-y-2 border-zinc-300">
                <th className="p-3 font-bold border-r border-zinc-200">{t("staffName")}</th>
                <th className="p-3 font-bold border-r border-zinc-200">{t("role")}</th>
                <th className="p-3 font-bold border-r border-zinc-200 text-right">{t("monthlyPayroll")}</th>
                <th className="p-3 font-bold">{t("joined")}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-200">
              {staff.map((member) => (
                <tr key={member.id}>
                  <td className="p-3 border-r border-zinc-100 font-bold">{member.name}</td>
                  <td className="p-3 border-r border-zinc-100 text-zinc-600">{member.role}</td>
                  <td className="p-3 border-r border-zinc-100 text-right font-mono font-bold text-emerald-700">
                    {currencySymbol}{member.salary.toFixed(2)}
                  </td>
                  <td className="p-3 font-mono text-zinc-500">{member.joinDate}</td>
                </tr>
              ))}
              <tr className="bg-zinc-50 font-black text-zinc-900 border-t-2 border-zinc-300">
                <td colSpan={2} className="p-3 text-right uppercase tracking-wider">{t("totalMonthlyPayroll")}</td>
                <td className="p-3 text-right font-mono text-[12pt] border-l border-zinc-200">
                  {currencySymbol}{totalMonthlyPayroll.toFixed(2)}
                </td>
                <td></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div>
          <h3 className="text-[15pt] font-bold text-zinc-800 border-b-2 border-zinc-200 pb-2 mb-4">Payroll & Labor History</h3>
          <table className="w-full border-collapse text-[9pt]">
            <thead>
              <tr className="bg-zinc-100 text-left border-y-2 border-zinc-300">
                <th className="p-3 font-bold border-r border-zinc-200">{t("date")}</th>
                <th className="p-3 font-bold border-r border-zinc-200 w-1/2">{t("description")}</th>
                <th className="p-3 font-bold border-r border-zinc-200 text-right">{t("amount")}</th>
                <th className="p-3 font-bold">{t("category")}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-200">
              {salaryPayments.map((record) => (
                <tr key={record.id}>
                  <td className="p-3 border-r border-zinc-100 font-mono">{record.date}</td>
                  <td className="p-3 border-r border-zinc-100 text-zinc-600 italic">{record.description}</td>
                  <td className="p-3 border-r border-zinc-100 text-right font-bold text-rose-700">
                    {currencySymbol}{record.amount.toFixed(2)}
                  </td>
                  <td className="p-3 text-zinc-500 uppercase text-[8pt] font-black">{record.category}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="mt-6 p-4 bg-zinc-800 text-white rounded-xl flex justify-between items-center">
            <span className="text-[10pt] font-bold uppercase tracking-widest opacity-70">Total Payments for Period</span>
            <span className="text-[16pt] font-black font-mono">{currencySymbol}{totalPeriodPayments.toFixed(2)}</span>
          </div>
        </div>
      </div>
    </ReportLayout>
  );
};

export default HRReport;
