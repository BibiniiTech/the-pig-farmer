"use client";

import React from "react";

interface ReportLayoutProps {
  title: string;
  children: React.ReactNode;
}

const ReportLayout: React.FC<ReportLayoutProps> = ({ title, children }) => {
  const generatedOn = new Date().toLocaleString("en-US", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).replace(/(\d+)\/(\d+)\/(\d+)/, "$3-$1-$2");

  return (
    <div className="hidden print:block p-10 bg-white text-black font-sans relative min-h-screen w-full">
      {/* Watermark */}
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none opacity-[0.12] z-0">
        <img src="/app_logo.png" alt="Watermark" className="w-[400px] h-[400px] object-contain" />
      </div>

      {/* Header */}
      <div className="relative z-10">
        <div className="absolute top-0 right-0">
          <img src="/app_logo.png" alt="Logo" className="w-[60px] h-[60px] object-contain" />
        </div>

        <div className="text-center mb-10 border-b pb-6 border-zinc-200">
          <h1 className="text-[26pt] font-bold tracking-tight">SmartSwine</h1>
          <p className="text-[13pt] italic text-zinc-600 mt-1">Farm Management Simplified</p>
          <h2 className="text-[20pt] font-bold mt-6 uppercase tracking-wide">{title}</h2>
          <p className="text-[10pt] text-zinc-500 mt-2">Generated on: {generatedOn}</p>
        </div>

        {/* Content */}
        <div className="report-content">
          {children}
        </div>

        {/* Footer */}
        <div className="mt-12 pt-6 border-t border-zinc-100 text-center text-[9pt] text-zinc-400 italic">
          Disclaimer: Consult a Vet before making health or nutritional decisions.
        </div>
      </div>
    </div>
  );
};

export default ReportLayout;
