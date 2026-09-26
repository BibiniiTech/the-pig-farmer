"use client";

import React from "react";
import { useAuth } from "@/context/AuthContext";

interface ReportLayoutProps {
  title: string;
  children: React.ReactNode;
}

const ReportLayout: React.FC<ReportLayoutProps> = ({ title, children }) => {
  const { userProfile } = useAuth();

  // Format current date exactly matching Android: "YYYY-MM-DD HH:mm:ss"
  const now = new Date();
  const pad = (n: number) => n.toString().padStart(2, "0");
  const dateString = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;

  const fullName = [userProfile?.firstName, userProfile?.lastName].filter(Boolean).join(" ").trim();
  const farmName = userProfile?.farmName?.trim() || fullName || "SmartSwine Farm";
  const farmLogo = userProfile?.farmLogo || "/app_logo.png";

  return (
    <div className="printable-report hidden print:block bg-white text-black font-sans relative min-h-screen w-full p-6 sm:p-10">
      {/* 300x300 watermark centered at 0.08f opacity (matching Android's getWatermarkData & WatermarkHandler) */}
      <div className="fixed inset-0 flex items-center justify-center pointer-events-none opacity-[0.08] z-0">
        <img
          src="/app_logo.png"
          alt="Watermark"
          className="w-[300px] h-[300px] object-contain"
        />
      </div>

      {/* Main printable content */}
      <div className="relative z-10">
        {/* Top-Right Farm Logo (50x50 matching Android scaleToFit(50f, 50f)) */}
        <div className="absolute top-0 right-0">
          <img
            src={farmLogo}
            alt="Logo"
            className="w-[50px] h-[50px] object-contain"
          />
        </div>

        {/* Common Header (exact layout & typography from Android addCommonHeader) */}
        <div className="text-center mb-6 pt-1">
          {/* Farm Name: 22pt, bold, black */}
          <h1 className="text-[22pt] font-bold text-black leading-tight">
            {farmName}
          </h1>

          {/* Report Title: 16pt, bold, dark gray (#404040), marginTop 4pt */}
          <h2 className="text-[16pt] font-bold text-[#404040] mt-1 leading-snug">
            {title}
          </h2>

          {/* Generated On: 10pt, centered */}
          <p className="text-[10pt] text-[#4A4A4A] mt-1">
            Generated on: {dateString}
          </p>

          {/* App Branding: 9pt, gray (#808080) */}
          <p className="text-[9pt] text-[#808080] mt-0.5">
            Exported with the SmartSwine App
          </p>
        </div>

        {/* Report Content Body */}
        <div className="report-content">
          {children}
        </div>

        {/* Running Footer at the bottom of the page (matching Android WatermarkHandler footer) */}
        <div className="mt-12 pt-6 text-center text-[9pt] text-[#808080]">
          SmartSwine App
        </div>
      </div>
    </div>
  );
};

export default ReportLayout;
