"use client";

import React from "react";
import ReportLayout from "./ReportLayout";
import { useTranslations } from "next-intl";
import { StaffMember, FinancialRecord } from "@/lib/types";

interface StaffDetailReportProps {
  member: StaffMember;
  financialRecords: FinancialRecord[];
  currencySymbol?: string;
  title?: string;
}

const StaffDetailReport: React.FC<StaffDetailReportProps> = ({
  member,
  financialRecords,
  currencySymbol = "$",
  title,
}) => {
  const t = useTranslations("Reports");
  const defaultTitle = `${t("employeeProfileReport", { fallback: "Employee Profile Report" })} - ${member.name}`;

  const currentYear = new Date().getFullYear().toString();

  // Filter salary payments matching this staff member
  const memberSalaryPayments = financialRecords
    .filter(
      (r) =>
        (r.category?.toLowerCase() === "salary" || r.category?.toLowerCase() === "labor/salary") &&
        r.description?.toLowerCase().includes(member.name.toLowerCase())
    )
    .sort((a, b) => b.date.localeCompare(a.date));

  const ytdSalary = memberSalaryPayments
    .filter((r) => r.date.includes(currentYear))
    .reduce((sum, r) => sum + r.amount, 0);

  const lifetimeSalary = memberSalaryPayments.reduce((sum, r) => sum + r.amount, 0);

  return (
    <ReportLayout title={title || defaultTitle}>
      <div className="space-y-8">
        {/* Staff Profile Information */}
        <div>
          <h3 className="text-[14pt] font-bold text-zinc-800 border-b-2 border-zinc-200 pb-2 mb-4">
            Staff Profile Details
          </h3>
          <table className="w-full border-collapse text-[10pt]">
            <tbody className="divide-y divide-zinc-200 border border-zinc-200">
              <tr className="bg-zinc-50/50">
                <td className="p-3 font-bold text-zinc-700 w-1/3 border-r border-zinc-200">Full Name</td>
                <td className="p-3 font-semibold text-zinc-900">{member.name}</td>
              </tr>
              <tr>
                <td className="p-3 font-bold text-zinc-700 border-r border-zinc-200">Job Role / Title</td>
                <td className="p-3 text-zinc-900">{member.role}</td>
              </tr>
              <tr className="bg-zinc-50/50">
                <td className="p-3 font-bold text-zinc-700 border-r border-zinc-200">Employment Status</td>
                <td className="p-3 font-bold">
                  <span
                    className={`inline-block px-2.5 py-0.5 rounded-full text-xs ${
                      member.status === "Active"
                        ? "bg-emerald-100 text-emerald-800"
                        : "bg-zinc-100 text-zinc-700"
                    }`}
                  >
                    {member.status}
                  </span>
                </td>
              </tr>
              <tr>
                <td className="p-3 font-bold text-zinc-700 border-r border-zinc-200">Phone Number</td>
                <td className="p-3 text-zinc-900 font-mono">{member.phone || "—"}</td>
              </tr>
              <tr className="bg-zinc-50/50">
                <td className="p-3 font-bold text-zinc-700 border-r border-zinc-200">Email Address</td>
                <td className="p-3 text-zinc-900">{member.email || "—"}</td>
              </tr>
              <tr>
                <td className="p-3 font-bold text-zinc-700 border-r border-zinc-200">Residential Address</td>
                <td className="p-3 text-zinc-900">{member.residentialAddress || "—"}</td>
              </tr>
              <tr className="bg-zinc-50/50">
                <td className="p-3 font-bold text-zinc-700 border-r border-zinc-200">Date of Birth</td>
                <td className="p-3 text-zinc-900 font-mono">{member.dateOfBirth || "—"}</td>
              </tr>
              <tr>
                <td className="p-3 font-bold text-zinc-700 border-r border-zinc-200">Gender</td>
                <td className="p-3 text-zinc-900">{member.gender || "—"}</td>
              </tr>
              <tr className="bg-zinc-50/50">
                <td className="p-3 font-bold text-zinc-700 border-r border-zinc-200">Date Joined</td>
                <td className="p-3 text-zinc-900 font-mono">{member.joinDate || "—"}</td>
              </tr>
              <tr>
                <td className="p-3 font-bold text-zinc-700 border-r border-zinc-200">App System Access</td>
                <td className="p-3 text-zinc-900">
                  {member.allowAppAccess
                    ? `Enabled (${member.inviteStatus || "active"})`
                    : "Disabled"}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* Emergency Contact */}
        {(member.emergencyContactName || member.emergencyContactPhone) && (
          <div>
            <h3 className="text-[14pt] font-bold text-zinc-800 border-b-2 border-zinc-200 pb-2 mb-4">
              Emergency Contact Information
            </h3>
            <table className="w-full border-collapse text-[10pt]">
              <tbody className="divide-y divide-zinc-200 border border-zinc-200">
                <tr className="bg-zinc-50/50">
                  <td className="p-3 font-bold text-zinc-700 w-1/3 border-r border-zinc-200">Contact Name</td>
                  <td className="p-3 font-semibold text-zinc-900">{member.emergencyContactName || "—"}</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-zinc-700 border-r border-zinc-200">Relationship</td>
                  <td className="p-3 text-zinc-900">{member.emergencyContactRelation || "—"}</td>
                </tr>
                <tr className="bg-zinc-50/50">
                  <td className="p-3 font-bold text-zinc-700 border-r border-zinc-200">Contact Phone</td>
                  <td className="p-3 text-zinc-900 font-mono">{member.emergencyContactPhone || "—"}</td>
                </tr>
                {member.emergencyContactAddress && (
                  <tr>
                    <td className="p-3 font-bold text-zinc-700 border-r border-zinc-200">Contact Address</td>
                    <td className="p-3 text-zinc-900">{member.emergencyContactAddress}</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Compensation Summary */}
        <div>
          <h3 className="text-[14pt] font-bold text-zinc-800 border-b-2 border-zinc-200 pb-2 mb-4">
            Compensation Summary
          </h3>
          <div className="grid grid-cols-3 gap-4 text-center">
            <div className="bg-zinc-50 border border-zinc-200 rounded-xl p-4">
              <p className="text-xs uppercase text-zinc-500 font-bold">Monthly Base Salary</p>
              <p className="text-xl font-black text-zinc-900 mt-1">
                {currencySymbol}
                {(member.salary || 0).toFixed(2)}
              </p>
            </div>
            <div className="bg-zinc-50 border border-zinc-200 rounded-xl p-4">
              <p className="text-xs uppercase text-zinc-500 font-bold">YTD Payments ({currentYear})</p>
              <p className="text-xl font-black text-emerald-700 mt-1">
                {currencySymbol}
                {ytdSalary.toFixed(2)}
              </p>
            </div>
            <div className="bg-zinc-50 border border-zinc-200 rounded-xl p-4">
              <p className="text-xs uppercase text-zinc-500 font-bold">Total Lifetime Salary</p>
              <p className="text-xl font-black text-blue-700 mt-1">
                {currencySymbol}
                {lifetimeSalary.toFixed(2)}
              </p>
            </div>
          </div>
        </div>

        {/* Salary Payment History */}
        <div>
          <h3 className="text-[14pt] font-bold text-zinc-800 border-b-2 border-zinc-200 pb-2 mb-4">
            Salary Payment History
          </h3>
          <table className="w-full border-collapse text-[10pt]">
            <thead>
              <tr className="bg-zinc-100 text-left border-y-2 border-zinc-300">
                <th className="p-3 font-bold border-r border-zinc-200">Date</th>
                <th className="p-3 font-bold border-r border-zinc-200 w-1/2">Description</th>
                <th className="p-3 font-bold text-right">Amount</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-200">
              {memberSalaryPayments.length === 0 ? (
                <tr>
                  <td colSpan={3} className="p-4 text-center text-zinc-400 italic">
                    No salary payments logged for this staff member yet.
                  </td>
                </tr>
              ) : (
                memberSalaryPayments.map((record) => (
                  <tr key={record.id} className="hover:bg-zinc-50/50">
                    <td className="p-3 border-r border-zinc-100 font-mono text-zinc-700">{record.date}</td>
                    <td className="p-3 border-r border-zinc-100 text-zinc-900 font-medium">{record.description}</td>
                    <td className="p-3 text-right font-mono font-bold text-rose-700">
                      {currencySymbol}
                      {record.amount.toFixed(2)}
                    </td>
                  </tr>
                ))
              )}
              <tr className="bg-zinc-100 font-black text-zinc-900 border-t-2 border-zinc-300">
                <td colSpan={2} className="p-3 uppercase tracking-wider text-right">
                  Total Payments in Period:
                </td>
                <td className="p-3 text-right font-mono text-[12pt] text-rose-800">
                  {currencySymbol}
                  {lifetimeSalary.toFixed(2)}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </ReportLayout>
  );
};

export default StaffDetailReport;
