"use client";

import React from "react";
import ReportLayout from "./ReportLayout";
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
  const currentYear = new Date().getFullYear().toString();
  const reportTitle = title || `Employee Profile & Salary Report - ${member.name}`;

  // Filter salary payments matching this staff member
  const memberSalaryPayments = financialRecords
    .filter(
      (r) =>
        (r.category?.toLowerCase() === "salary" || r.category?.toLowerCase() === "labor/salary") &&
        r.description?.toLowerCase().includes(member.name.toLowerCase())
    )
    .sort((a, b) => b.date.localeCompare(a.date));

  const ytdSalary = memberSalaryPayments
    .filter((r) => r.date.startsWith(currentYear))
    .reduce((sum, r) => sum + r.amount, 0);

  const lifetimeSalary = memberSalaryPayments.reduce((sum, r) => sum + r.amount, 0);

  return (
    <ReportLayout title={reportTitle}>
      <div className="space-y-6">
        {/* Profile Information */}
        <div>
          <h3 className="text-[16pt] font-bold text-black mb-2">Employee Details</h3>
          <table className="w-full border-collapse text-[10pt] border border-zinc-400">
            <tbody className="divide-y divide-zinc-300">
              <tr className="text-black">
                <td className="p-2 border border-zinc-400 font-bold w-[30%]">Staff Name</td>
                <td className="p-2 border border-zinc-400 w-[70%]">{member.name}</td>
              </tr>
              <tr className="text-black">
                <td className="p-2 border border-zinc-400 font-bold">Role</td>
                <td className="p-2 border border-zinc-400">{member.role}</td>
              </tr>
              <tr className="text-black">
                <td className="p-2 border border-zinc-400 font-bold">Status</td>
                <td className="p-2 border border-zinc-400">{member.status}</td>
              </tr>
              <tr className="text-black">
                <td className="p-2 border border-zinc-400 font-bold">Phone Number</td>
                <td className="p-2 border border-zinc-400 font-mono text-[9pt]">{member.phone || "—"}</td>
              </tr>
              <tr className="text-black">
                <td className="p-2 border border-zinc-400 font-bold">Staff Email</td>
                <td className="p-2 border border-zinc-400">{member.email || "—"}</td>
              </tr>
              <tr className="text-black">
                <td className="p-2 border border-zinc-400 font-bold">Residential Address</td>
                <td className="p-2 border border-zinc-400">{member.residentialAddress || "—"}</td>
              </tr>
              <tr className="text-black">
                <td className="p-2 border border-zinc-400 font-bold">Date of Birth</td>
                <td className="p-2 border border-zinc-400 font-mono text-[9pt]">{member.dateOfBirth || "—"}</td>
              </tr>
              <tr className="text-black">
                <td className="p-2 border border-zinc-400 font-bold">Gender</td>
                <td className="p-2 border border-zinc-400">{member.gender || "—"}</td>
              </tr>
              <tr className="text-black">
                <td className="p-2 border border-zinc-400 font-bold">Join Date</td>
                <td className="p-2 border border-zinc-400 font-mono text-[9pt]">{member.joinDate || "—"}</td>
              </tr>
              <tr className="text-black">
                <td className="p-2 border border-zinc-400 font-bold">System Access</td>
                <td className="p-2 border border-zinc-400">
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
            <h3 className="text-[16pt] font-bold text-black mb-2">Emergency Contact Details</h3>
            <table className="w-full border-collapse text-[10pt] border border-zinc-400">
              <tbody className="divide-y divide-zinc-300">
                <tr className="text-black">
                  <td className="p-2 border border-zinc-400 font-bold w-[30%]">Name</td>
                  <td className="p-2 border border-zinc-400 w-[70%]">{member.emergencyContactName || "—"}</td>
                </tr>
                <tr className="text-black">
                  <td className="p-2 border border-zinc-400 font-bold">Relationship</td>
                  <td className="p-2 border border-zinc-400">{member.emergencyContactRelation || "—"}</td>
                </tr>
                <tr className="text-black">
                  <td className="p-2 border border-zinc-400 font-bold">Phone Number</td>
                  <td className="p-2 border border-zinc-400 font-mono text-[9pt]">{member.emergencyContactPhone || "—"}</td>
                </tr>
                {member.emergencyContactAddress && (
                  <tr className="text-black">
                    <td className="p-2 border border-zinc-400 font-bold">Residential Address</td>
                    <td className="p-2 border border-zinc-400">{member.emergencyContactAddress}</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Compensation Summary (2-column table matching Android compTable) */}
        <div>
          <h3 className="text-[16pt] font-bold text-black mb-2">Compensation Summary</h3>
          <table className="w-full border-collapse text-[10pt] border border-zinc-400">
            <tbody className="divide-y divide-zinc-300">
              <tr className="text-black">
                <td className="p-2 border border-zinc-400 font-bold w-[50%]">Monthly Salary</td>
                <td className="p-2 border border-zinc-400 text-right font-mono font-bold w-[50%]">
                  {currencySymbol}{(member.salary || 0).toFixed(2)}
                </td>
              </tr>
              <tr className="text-black">
                <td className="p-2 border border-zinc-400 font-bold">Total YTD Salary Paid ({currentYear})</td>
                <td className="p-2 border border-zinc-400 text-right font-mono font-bold">
                  {currencySymbol}{ytdSalary.toFixed(2)}
                </td>
              </tr>
              <tr className="text-black">
                <td className="p-2 border border-zinc-400 font-bold">Total Salary Paid (Lifetime)</td>
                <td className="p-2 border border-zinc-400 text-right font-mono font-bold">
                  {currencySymbol}{lifetimeSalary.toFixed(2)}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* Salary Payment History */}
        <div>
          <h3 className="text-[16pt] font-bold text-black mb-2">Salary Payment History</h3>
          {memberSalaryPayments.length === 0 ? (
            <p className="italic text-zinc-500 text-[10pt]">
              No salary payment records found for this employee.
            </p>
          ) : (
            <table className="w-full border-collapse text-[9.5pt] border border-zinc-400">
              <thead>
                <tr className="bg-[#D3D3D3] text-black font-bold text-left border-b border-zinc-400">
                  <th className="p-2 border border-zinc-400 font-bold w-[18%]">Activity Date</th>
                  <th className="p-2 border border-zinc-400 font-bold w-[62%]">Description</th>
                  <th className="p-2 border border-zinc-400 font-bold text-right w-[20%]">Amount</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-300">
                {memberSalaryPayments.map((record) => (
                  <tr key={record.id} className="text-black">
                    <td className="p-2 border border-zinc-300 font-mono text-[9pt]">{record.date}</td>
                    <td className="p-2 border border-zinc-300 leading-snug">{record.description}</td>
                    <td className="p-2 border border-zinc-300 text-right font-mono font-bold">
                      {currencySymbol}{record.amount.toFixed(2)}
                    </td>
                  </tr>
                ))}
                <tr className="font-bold text-black border-t-2 border-zinc-400">
                  <td className="p-2 border border-zinc-400 font-bold">TOTAL</td>
                  <td className="p-2 border border-zinc-400"></td>
                  <td className="p-2 border border-zinc-400 text-right font-mono font-bold">
                    {currencySymbol}{lifetimeSalary.toFixed(2)}
                  </td>
                </tr>
              </tbody>
            </table>
          )}
        </div>
      </div>
    </ReportLayout>
  );
};

export default StaffDetailReport;
