"use client";

import React from "react";
import ReportLayout from "./ReportLayout";
import { StaffMember, FinancialRecord } from "@/lib/types";

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
  title = "Staff & Payroll Report",
}) => {
  const currentYear = new Date().getFullYear().toString();
  const totalStaff = staff.length;
  const activeStaff = staff.filter(
    (s) => s.status?.toLowerCase() !== "inactive" && s.status?.toLowerCase() !== "archived"
  ).length;
  const totalMonthlyPayroll = staff
    .filter((s) => s.status?.toLowerCase() !== "archived")
    .reduce((sum, m) => sum + (m.salary || 0), 0);

  const staffWithYtd = staff.map((member) => {
    const memberPayments = financialRecords.filter(
      (r) =>
        (r.category?.toLowerCase() === "salary" || r.category?.toLowerCase() === "labor/salary") &&
        r.description?.toLowerCase().includes(member.name.toLowerCase())
    );
    const ytd = memberPayments
      .filter((r) => r.date.startsWith(currentYear))
      .reduce((sum, r) => sum + r.amount, 0);
    return { member, ytd, memberPayments };
  });

  const totalYtdPayroll = staffWithYtd.reduce((sum, item) => sum + item.ytd, 0);

  const salaryPayments = financialRecords
    .filter(
      (r) => r.category?.toLowerCase() === "salary" || r.category?.toLowerCase() === "labor/salary"
    )
    .sort((a, b) => b.date.localeCompare(a.date));

  const totalPeriod = salaryPayments.reduce((sum, r) => sum + r.amount, 0);

  return (
    <ReportLayout title={title}>
      <div className="space-y-6">
        {/* Summary section */}
        <div>
          <h3 className="text-[16pt] font-bold text-black mb-2">Summary</h3>
          <div className="space-y-1 text-[11pt] text-black">
            <p>Total Staff: {totalStaff}</p>
            <p>Active Staff: {activeStaff}</p>
            <p>
              Monthly Payroll: {currencySymbol}
              {totalMonthlyPayroll.toFixed(2)}
            </p>
            <p className="font-bold">
              Total YTD Salary Paid ({currentYear}): {currencySymbol}
              {totalYtdPayroll.toFixed(2)}
            </p>
          </div>
        </div>

        {/* Staff Details table */}
        <div>
          <h3 className="text-[16pt] font-bold text-black mb-2">Staff Details</h3>
          <table className="w-full border-collapse text-[9.5pt] border border-zinc-400">
            <thead>
              <tr className="bg-[#D3D3D3] text-black font-bold text-left border-b border-zinc-400">
                <th className="p-2 border border-zinc-400 font-bold w-[24%]">Staff Name</th>
                <th className="p-2 border border-zinc-400 font-bold w-[18%]">Role</th>
                <th className="p-2 border border-zinc-400 font-bold w-[14%]">Status</th>
                <th className="p-2 border border-zinc-400 font-bold w-[16%]">Phone</th>
                <th className="p-2 border border-zinc-400 font-bold text-right w-[14%]">Monthly Salary</th>
                <th className="p-2 border border-zinc-400 font-bold text-right w-[14%]">YTD Salary</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-300">
              {staffWithYtd.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-4 text-center text-zinc-500 italic border border-zinc-300">
                    No staff records found.
                  </td>
                </tr>
              ) : (
                staffWithYtd.map(({ member, ytd }) => (
                  <tr key={member.id} className="text-black">
                    <td className="p-2 border border-zinc-300 font-bold">{member.name}</td>
                    <td className="p-2 border border-zinc-300">{member.role}</td>
                    <td className="p-2 border border-zinc-300">{member.status}</td>
                    <td className="p-2 border border-zinc-300 font-mono text-[9pt]">{member.phone || "—"}</td>
                    <td className="p-2 border border-zinc-300 text-right font-mono font-bold">
                      {currencySymbol}{(member.salary || 0).toFixed(2)}
                    </td>
                    <td className="p-2 border border-zinc-300 text-right font-mono font-bold">
                      {currencySymbol}{ytd.toFixed(2)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Payroll History section */}
        <div>
          <h3 className="text-[16pt] font-bold text-black mb-2">Payroll History</h3>
          <table className="w-full border-collapse text-[9.5pt] border border-zinc-400">
            <thead>
              <tr className="bg-[#D3D3D3] text-black font-bold text-left border-b border-zinc-400">
                <th className="p-2 border border-zinc-400 font-bold w-[18%]">Activity Date</th>
                <th className="p-2 border border-zinc-400 font-bold w-[46%]">Description</th>
                <th className="p-2 border border-zinc-400 font-bold text-right w-[18%]">Amount</th>
                <th className="p-2 border border-zinc-400 font-bold text-left w-[18%]">Category</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-300">
              {salaryPayments.length === 0 ? (
                <tr>
                  <td colSpan={4} className="p-4 text-center text-zinc-500 italic border border-zinc-300">
                    No payroll history records found.
                  </td>
                </tr>
              ) : (
                salaryPayments.map((record) => (
                  <tr key={record.id} className="text-black">
                    <td className="p-2 border border-zinc-300 font-mono text-[9pt]">{record.date}</td>
                    <td className="p-2 border border-zinc-300 leading-snug">{record.description}</td>
                    <td className="p-2 border border-zinc-300 text-right font-mono font-bold">
                      {currencySymbol}{record.amount.toFixed(2)}
                    </td>
                    <td className="p-2 border border-zinc-300">{record.category}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>

          <p className="mt-4 font-bold text-[11pt] text-black">
            Total Payments in Selected Period: {currencySymbol}
            {totalPeriod.toFixed(2)}
          </p>
        </div>
      </div>
    </ReportLayout>
  );
};

export default HRReport;
