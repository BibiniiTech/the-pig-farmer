"use client";

import React from "react";
import ReportLayout from "./ReportLayout";
import { formatSwineAge } from "@/lib/swineGrowthDatabase";
import { resolvePigIds } from "@/lib/pdfExporter";
import { Pig, HealthRecord } from "@/lib/types";

interface HerdReportProps {
  pigs: Pig[];
  allPigs?: Pig[]; // For resolving pig IDs in descriptions
  healthRecords?: Record<string, HealthRecord[]>;
  title?: string;
  includeSummary?: boolean;
}

const HerdReport: React.FC<HerdReportProps> = ({
  pigs,
  allPigs = [],
  healthRecords = {},
  title = "Herd Data Report",
  includeSummary = true,
}) => {
  const isProfile = title.toLowerCase().includes("profile");
  const combinedAllPigs = allPigs.length > 0 ? allPigs : pigs;

  // Build combined health records map
  const combinedHealthRecords: Record<string, HealthRecord[]> = { ...healthRecords };
  pigs.forEach((pig) => {
    if (pig.healthRecords && pig.healthRecords.length > 0) {
      if (!combinedHealthRecords[pig.id]) {
        combinedHealthRecords[pig.id] = pig.healthRecords;
      }
    }
  });

  const breeders = pigs.filter((p) => p.purpose?.toLowerCase() === "breeder");
  const porkers = pigs.filter((p) => p.purpose?.toLowerCase() === "porker");

  const maleBreeders = breeders.filter((p) => p.gender?.toLowerCase() === "male").length;
  const femaleBreeders = breeders.filter((p) => p.gender?.toLowerCase() === "female").length;

  const malePorkers = porkers.filter((p) => p.gender?.toLowerCase() === "male").length;
  const femalePorkers = porkers.filter((p) => p.gender?.toLowerCase() === "female").length;

  const hasHealthHistory = Object.values(combinedHealthRecords).some((records) => records && records.length > 0);

  return (
    <ReportLayout title={title}>
      <div className="space-y-6">
        {/* Herd Summary block (exact text and layout matching Android generateHerdReportPdf) */}
        {includeSummary && !isProfile && (
          <div>
            <h3 className="text-[16pt] font-bold text-black mb-2">Herd Summary</h3>
            <div className="space-y-1 text-[11pt] text-black">
              <p>Total Pigs: {pigs.length}</p>
              <p>Total Breeders: {breeders.length}</p>
              <p className="pl-4">
                Breakdown of Breeders: Males: {maleBreeders}, Females: {femaleBreeders}
              </p>
              <p>Total Porkers: {porkers.length}</p>
              <p className="pl-4">
                Breakdown of Porkers: Males: {malePorkers}, Females: {femalePorkers}
              </p>
            </div>
          </div>
        )}

        {/* Main Herd Table (8 columns matching Android: tag_hash, gender, breed, purpose, weight_kg, pen_hash, dob, age) */}
        <div>
          <table className="w-full border-collapse text-[9.5pt] border border-zinc-400">
            <thead>
              <tr className="bg-[#D3D3D3] text-black font-bold text-left border-b border-zinc-400">
                <th className="p-2 border border-zinc-400 font-bold">Tag #</th>
                <th className="p-2 border border-zinc-400 font-bold">Gender</th>
                <th className="p-2 border border-zinc-400 font-bold">Breed</th>
                <th className="p-2 border border-zinc-400 font-bold">Purpose</th>
                <th className="p-2 border border-zinc-400 font-bold text-center">Weight (kg)</th>
                <th className="p-2 border border-zinc-400 font-bold">Pen #</th>
                <th className="p-2 border border-zinc-400 font-bold">Date of Birth</th>
                <th className="p-2 border border-zinc-400 font-bold">Age</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-300">
              {pigs.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-4 text-center text-zinc-500 italic border border-zinc-300">
                    No pigs recorded.
                  </td>
                </tr>
              ) : (
                pigs.map((pig) => (
                  <tr key={pig.id} className="text-black">
                    <td className="p-2 border border-zinc-300 font-mono font-bold">{pig.tagNumber}</td>
                    <td className="p-2 border border-zinc-300">{pig.gender || "—"}</td>
                    <td className="p-2 border border-zinc-300">{pig.breed || "Not specified"}</td>
                    <td className="p-2 border border-zinc-300">{pig.purpose || "—"}</td>
                    <td className="p-2 border border-zinc-300 text-center font-mono">{pig.weight}</td>
                    <td className="p-2 border border-zinc-300">{pig.location || "—"}</td>
                    <td className="p-2 border border-zinc-300 font-mono text-[9pt]">{pig.birthDate || "—"}</td>
                    <td className="p-2 border border-zinc-300">
                      {pig.birthDate ? formatSwineAge(pig.birthDate, true) : "—"}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* History section (matching Android addSection("History") & Target Animal table) */}
        {hasHealthHistory && (
          <div className="space-y-6">
            <h3 className="text-[16pt] font-bold text-black border-b border-zinc-300 pb-1">History</h3>
            {Object.entries(combinedHealthRecords).map(([pigId, records]) => {
              if (!records || records.length === 0) return null;
              const pigTag =
                pigs.find((p) => p.id === pigId)?.tagNumber ||
                combinedAllPigs.find((p) => p.id === pigId)?.tagNumber ||
                pigId;

              return (
                <div key={pigId} className="space-y-2">
                  <h4 className="text-[14pt] font-bold text-black">
                    Target Animal: {pigTag}
                  </h4>
                  <table className="w-full border-collapse text-[9pt] border border-zinc-400">
                    <thead>
                      <tr className="bg-[#D3D3D3] text-black font-bold text-left border-b border-zinc-400">
                        <th className="p-2 border border-zinc-400 w-1/4">Activity Type</th>
                        <th className="p-2 border border-zinc-400 w-1/5">Date</th>
                        <th className="p-2 border border-zinc-400">Notes</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-300">
                      {records.map((record) => {
                        const resolvedNotes = resolvePigIds(record.description || "", combinedAllPigs);
                        return (
                          <tr key={record.id} className="text-black">
                            <td className="p-2 border border-zinc-300 font-medium">{record.type}</td>
                            <td className="p-2 border border-zinc-300 font-mono">{record.date}</td>
                            <td className="p-2 border border-zinc-300 whitespace-pre-line leading-relaxed">
                              {resolvedNotes}
                              {record.medication && (
                                <div className="font-semibold text-zinc-700 mt-0.5">
                                  Medication: {record.medication}
                                </div>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </ReportLayout>
  );
};

export default HerdReport;
