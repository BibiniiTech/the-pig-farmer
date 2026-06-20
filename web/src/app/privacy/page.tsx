"use client";

import React, { useEffect } from "react";
import Link from "next/link";
import { useDevice } from "@/context/DeviceContext";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import { useTranslations } from "next-intl";

export default function PrivacyPolicyPage() {
  const t = useTranslations("Privacy");
  const { isMobile } = useDevice();

  return (
    <div className="relative min-h-screen bg-white text-zinc-900 flex flex-col font-sans overflow-hidden">
      {/* Watermark Logo Background */}
      {!isMobile && (
        <div className="fixed inset-0 z-0 flex items-center justify-center opacity-[0.15] pointer-events-none select-none">
          <img
            src="/app_logo.png"
            alt="Watermark Background Logo"
            className="w-full max-w-[1100px] max-h-[85vh] object-contain"
          />
        </div>
      )}

      <div className="relative z-10 flex flex-col min-h-screen">
        {/* Header */}
        {!isMobile && <DesktopHeader />}

        {/* Content Body */}
        <main className="flex-1 max-w-4xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12 space-y-12">
          <div className="text-center space-y-4">
            <h1 className="text-4xl font-extrabold text-zinc-900 tracking-tight">{t("title")}</h1>
            <p className="text-sm text-zinc-500 font-medium">{t("lastUpdated")}</p>
          </div>

          <div className="bg-emerald-50 border border-emerald-100 rounded-3xl p-8 space-y-6 shadow-sm text-center">
            <h2 className="text-2xl font-bold text-emerald-800">{t("importantTitle")}</h2>
            <p className="text-zinc-600 leading-relaxed text-lg max-w-2xl mx-auto">
              {t("importantDesc")}
            </p>
            <div className="pt-4">
              <a
                href="https://sites.google.com/view/smartswine-privacypolicy/home"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center px-8 py-4 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-lg shadow-lg shadow-emerald-200 transition-all hover:scale-[1.02] active:scale-[0.98]"
              >
                {t("viewFull")}
              </a>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 pt-8">
            <div className="space-y-3">
              <h3 className="font-bold text-zinc-800 flex items-center gap-2">
                <span className="text-emerald-600">🛡️</span> {t("dataOwnershipTitle")}
              </h3>
              <p className="text-zinc-600 leading-relaxed">
                {t("dataOwnershipDesc")}
              </p>
            </div>
            <div className="space-y-3">
              <h3 className="font-bold text-zinc-800 flex items-center gap-2">
                <span className="text-emerald-600">☁️</span> {t("cloudSecurityTitle")}
              </h3>
              <p className="text-zinc-600 leading-relaxed">
                {t("cloudSecurityDesc")}
              </p>
            </div>
            <div className="space-y-3">
              <h3 className="font-bold text-zinc-800 flex items-center gap-2">
                <span className="text-emerald-600">🚫</span> {t("noSalesTitle")}
              </h3>
              <p className="text-zinc-600 leading-relaxed">
                {t("noSalesDesc")}
              </p>
            </div>
            <div className="space-y-3">
              <h3 className="font-bold text-zinc-800 flex items-center gap-2">
                <span className="text-emerald-600">🎯</span> {t("accuracyTitle")}
              </h3>
              <p className="text-zinc-600 leading-relaxed">
                {t("accuracyDesc")}
              </p>
            </div>
          </div>

          <div className="h-20" />
        </main>
      </div>
    </div>
  );
}
