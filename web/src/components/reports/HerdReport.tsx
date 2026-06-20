"use client";

import React from "react";
import ReportLayout from "./ReportLayout";

interface HealthRecord {
  id: string;
  date: string;
  type: string;
  description: string;
}

interface Pig {
  id: string;
  tagNumber: string;
  birthDate: string;
  breed: string;
  gender: string;
  weight: number;
  purpose: string;
  location: string;
  healthRecords?: HealthRecord[];
}

interface HerdReportProps {
  pigs: Pig[];
  allPigs?: Pig[]; // For resolving IDs in descriptions
  title?: string;
  includeSummary?: boolean;
}

const HerdReport: React.FC<HerdReportProps> = ({ pigs, allPigs = [], title = "Herd Data Report", includeSummary = true }) => {
  const calculateAgeMonths = (birthDate: string) => {
    const birth = new Date(birthDate);
    const now = new Date();
    if (isNaN(birth.getTime())) return 0;
    let months = (now.getFullYear() - birth.getFullYear()) * 12 + (now.getMonth() - birth.getMonth());
    if (now.getDate() < birth.getDate()) months--;
    return Math.max(0, months);
  };

  const getAgeText = (dob: string) => {
    const ageMonths = calculateAgeMonths(dob);
    if (ageMonths < 12) return `${ageMonths} mo`;
    return `${Math.floor(ageMonths / 12)}yr ${ageMonths % 12}mo`;
  };

  const breeders = pigs.filter(p => p.purpose === "Breeder");
  const porkers = pigs.filter(p => p.purpose === "Porker");

  return (
    <ReportLayout title={title}>
      {includeSummary && (
        <div className="mb-10 space-y-2 border-l-4 border-emerald-500 pl-6 py-2 bg-zinc-50 rounded-r-xl">
          <h3 className="text-[16pt] font-bold text-zinc-800">Herd Summary</h3>
          <p className="text-[12pt]">Total Pigs: {pigs.length}</p>
          <p className="text-[12pt]">Total Breeders: {breeders.length}</p>
          <p className="text-[11pt] text-zinc-600 italic ml-4">
            Breakdown: Males: {breeders.filter(p => p.gender === "Male").length}, Females: {breeders.filter(p => p.gender === "Female").length}
          </p>
          <p className="text-[12pt]">Total Porkers: {porkers.length}</p>
          <p className="text-[11pt] text-zinc-600 italic ml-4">
            Breakdown: Males: {porkers.filter(p => p.gender === "Male").length}, Females: {porkers.filter(p => p.gender === "Female").length}
          </p>
        </div>
      )}

      <table className="w-full border-collapse mb-10 text-[10pt]">
        <thead>
          <tr className="bg-zinc-100 text-left border-y-2 border-zinc-300">
            <th className="p-3 font-bold border-r border-zinc-200">Tag #</th>
            <th className="p-3 font-bold border-r border-zinc-200">Gender</th>
            <th className="p-3 font-bold border-r border-zinc-200">Breed</th>
            <th className="p-3 font-bold border-r border-zinc-200">Purpose</th>
            <th className="p-3 font-bold border-r border-zinc-200 text-center">Weight (kg)</th>
            <th className="p-3 font-bold border-r border-zinc-200">Pen #</th>
            <th className="p-3 font-bold border-r border-zinc-200">DOB</th>
            <th className="p-3 font-bold">Age</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-zinc-200">
          {pigs.map((pig) => (
            <tr key={pig.id}>
              <td className="p-3 border-r border-zinc-100 font-mono font-bold">{pig.tagNumber}</td>
              <td className="p-3 border-r border-zinc-100">{pig.gender}</td>
              <td className="p-3 border-r border-zinc-100">{pig.breed || "N/A"}</td>
              <td className="p-3 border-r border-zinc-100 uppercase text-[8pt] font-black tracking-tighter text-zinc-500">{pig.purpose}</td>
              <td className="p-3 border-r border-zinc-100 text-center font-mono">{pig.weight}</td>
              <td className="p-3 border-r border-zinc-100">{pig.location || "N/A"}</td>
              <td className="p-3 border-r border-zinc-100 font-mono text-[9pt]">{pig.birthDate}</td>
              <td className="p-3 font-bold text-emerald-700">{getAgeText(pig.birthDate)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {pigs.some(p => p.healthRecords && p.healthRecords.length > 0) && (
        <div className="mt-12 space-y-8">
          <h3 className="text-[16pt] font-bold border-b-2 border-zinc-800 pb-2 mb-6">Health & Activity History</h3>
          {pigs.filter(p => p.healthRecords && p.healthRecords.length > 0).map(pig => (
            <div key={pig.id} className="space-y-3">
              <h4 className="text-[13pt] font-bold text-emerald-800">Target Animal: {pig.tagNumber}</h4>
              <table className="w-full border-collapse text-[9pt]">
                <thead>
                  <tr className="bg-zinc-50 text-left border-y border-zinc-200">
                    <th className="p-2 font-bold w-1/4">Activity Type</th>
                    <th className="p-2 font-bold w-1/5">Date</th>
                    <th className="p-2 font-bold">Notes / Description</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100">
                  {pig.healthRecords?.map(record => (
                    <tr key={record.id}>
                      <td className="p-2 font-semibold text-zinc-700">{record.type}</td>
                      <td className="p-2 font-mono">{record.date}</td>
                      <td className="p-2 text-zinc-600 italic leading-relaxed">{record.description}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))}
        </div>
      )}
    </ReportLayout>
  );
};

export default HerdReport;
