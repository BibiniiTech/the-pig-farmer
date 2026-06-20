"use client";

import React from "react";
import Link from "next/link";
import { useDevice } from "@/context/DeviceContext";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import { useTranslations } from "next-intl";

export default function TermsOfServicePage() {
  const t = useTranslations("Terms");
  const { isMobile } = useDevice();

  const sections = [
    { title: t("sections.s1.title"), content: t("sections.s1.content") },
    { title: t("sections.s2.title"), content: t("sections.s2.content") },
    { title: t("sections.s3.title"), content: t("sections.s3.content") },
    { title: t("sections.s4.title"), content: t("sections.s4.content") },
    { title: t("sections.s5.title"), content: t("sections.s5.content") },
    { title: t("sections.s6.title"), content: t("sections.s6.content") },
    { title: t("sections.s7.title"), content: t("sections.s7.content") },
    { title: t("sections.s8.title"), content: t("sections.s8.content") },
    { title: t("sections.s9.title"), content: t("sections.s9.content") },
    { title: t("sections.s10.title"), content: t("sections.s10.content") },
    { title: t("sections.s11.title"), content: t("sections.s11.content") }
  ];

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

          <div className="space-y-10">
            {sections.map((section, index) => (
              <section key={index} className="space-y-3">
                <h2 className="text-xl font-bold text-emerald-700">{section.title}</h2>
                <div className="text-zinc-600 leading-relaxed whitespace-pre-line text-lg">
                  {section.content}
                </div>
              </section>
            ))}
          </div>

          <div className="pt-12 border-t border-zinc-100 text-center">
            <p className="text-lg font-bold text-emerald-600">
              {t("contactSupport")} <a href="mailto:bibiniitech@gmail.com" className="hover:underline">bibiniitech@gmail.com</a>
            </p>
          </div>

          <div className="h-20" />
        </main>
      </div>
    </div>
  );
}
