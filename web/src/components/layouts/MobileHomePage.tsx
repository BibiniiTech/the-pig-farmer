"use client";

import React, { useState } from "react";
import Link from "next/link";

interface MobileHomePageProps {
  onInstallClick?: () => void;
  showInstallButton?: boolean;
}

const languages = [
  { code: "en", name: "English", flag: "🇺🇸" },
  { code: "es", name: "Español", flag: "🇪🇸" },
  { code: "fr", name: "Français", flag: "🇫🇷" },
  { code: "hi", name: "हिन्दी", flag: "🇮🇳" },
  { code: "pt", name: "Português", flag: "🇵🇹" },
  { code: "th", name: "ไทย", flag: "🇹🇭" },
  { code: "tl", name: "Filipino", flag: "🇵🇭" },
  { code: "vi", name: "Tiếng Việt", flag: "🇻🇳" },
  { code: "zh", name: "中文", flag: "🇨🇳" },
];

export default function MobileHomePage({
  onInstallClick,
  showInstallButton = false,
}: MobileHomePageProps) {
  const [langMenuOpen, setLangMenuOpen] = useState(false);
  const [selectedLang, setSelectedLang] = useState("en");
  const currentLang = languages.find((l) => l.code === selectedLang) || languages[0];

  return (
    <div className="relative flex flex-col min-h-screen bg-white overflow-hidden select-none">
      {/* Ambient glow blobs */}
      <div className="pointer-events-none absolute top-[-80px] left-[-60px] h-72 w-72 rounded-full bg-emerald-400/20 blur-3xl" />
      <div className="pointer-events-none absolute bottom-20 right-[-60px] h-64 w-64 rounded-full bg-violet-400/15 blur-3xl" />

      {/* Watermark */}
      <div className="pointer-events-none fixed inset-0 z-0 flex items-center justify-center opacity-[0.12]">
        <img src="/app_logo.png" alt="" className="w-[90vw] max-h-[70vh] object-contain" />
      </div>

      {/* ── Top Bar ── */}
      <header className="relative z-50 flex items-center justify-between h-14 px-4 bg-white/90 backdrop-blur-md border-b border-zinc-100 shadow-sm">
        <div className="flex items-center gap-2 select-none">
          <img src="/app_logo.png" alt="SmartSwine Logo" className="h-7 w-7 object-contain rounded-md" />
          <span className="font-extrabold text-sm bg-gradient-to-r from-emerald-700 via-emerald-600 to-green-500 bg-clip-text text-transparent">
            SmartSwine
          </span>
        </div>

        {/* Right Actions */}
        <div className="flex items-center gap-0.5">
          {/* Language picker */}
          <div className="relative">
            <button
              id="mobile-lang-toggle"
              onClick={() => setLangMenuOpen(!langMenuOpen)}
              className="p-2 rounded-xl text-lg hover:bg-zinc-100 transition-colors"
              aria-label="Select Language"
            >
              <span>{currentLang.flag}</span>
            </button>
            {langMenuOpen && (
              <>
                <div className="fixed inset-0 z-30" onClick={() => setLangMenuOpen(false)} />
                <div className="absolute right-0 mt-1 w-44 rounded-xl border border-zinc-200 bg-white shadow-xl z-40 py-1.5 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
                  <div className="px-3 py-1 text-[10px] font-bold text-zinc-400 uppercase tracking-wider border-b border-zinc-100 mb-1">
                    Language
                  </div>
                  <div className="max-h-[240px] overflow-y-auto">
                    {languages.map((lang) => (
                      <button
                        key={lang.code}
                        onClick={() => { setSelectedLang(lang.code); setLangMenuOpen(false); }}
                        className={`flex w-full items-center justify-between px-4 py-2 text-sm font-semibold transition-colors ${
                          selectedLang === lang.code
                            ? "bg-emerald-50 text-emerald-800 font-semibold"
                            : "text-zinc-700 hover:bg-zinc-50"
                        }`}
                      >
                        <div className="flex items-center gap-2">
                          <span className="text-base leading-none">{lang.flag}</span>
                          <span>{lang.name}</span>
                        </div>
                        {selectedLang === lang.code && <div className="h-1.5 w-1.5 rounded-full bg-emerald-500" />}
                      </button>
                    ))}
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      </header>

      {/* ── Hero ── */}
      <main className="relative z-10 flex flex-1 flex-col items-center justify-center px-6 pb-8 pt-4 text-center">
        {/* Status pill */}
        <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-zinc-200 bg-white/80 px-4 py-1.5 text-xs text-zinc-600 shadow-sm backdrop-blur-md">
          <span className="flex h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
          <span>Now in Web Beta</span>
        </div>

        {/* Big logo */}
        <div className="mb-6 relative">
          <div className="absolute inset-0 rounded-3xl bg-emerald-400/20 blur-2xl scale-110" />
          <img
            src="/app_logo.png"
            alt="SmartSwine"
            className="relative h-32 w-32 object-contain rounded-3xl shadow-xl border border-zinc-100 bg-white p-2"
          />
        </div>

        <h1 className="mb-2 text-4xl font-extrabold tracking-tight">
          <span className="block bg-gradient-to-r from-emerald-950 via-emerald-800 to-green-600 bg-clip-text text-transparent">
            SmartSwine
          </span>
        </h1>
        <p className="mb-1 text-lg font-bold text-emerald-900">Piggery Manager</p>
        <p className="mb-1 text-base font-semibold text-emerald-800">Farm Smarter: Not Harder</p>

        <p className="mb-8 mt-3 max-w-xs text-sm text-zinc-500 leading-relaxed">
          Track growth, formulate feed, diagnose disease, weigh with tape, manage tasks, and optimize finances — all in real-time.
        </p>

        {/* CTA buttons — stacked for mobile */}
        <div className="flex w-full max-w-xs flex-col gap-3">
          <Link
            id="mobile-start-button"
            href="/login"
            className="flex items-center justify-center rounded-2xl bg-gradient-to-r from-emerald-600 to-emerald-700 px-6 py-4 text-sm font-bold text-white shadow-lg shadow-emerald-600/25 hover:from-emerald-700 hover:to-emerald-800 active:scale-95 transition-all duration-200"
          >
            Launch Web App
            <svg className="ml-2 h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
              <path strokeLinecap="round" strokeLinejoin="round" d="M17 8l4 4m0 0l-4 4m4-4H3" />
            </svg>
          </Link>

          <a
            href="https://play.google.com/store/apps/details?id=com.bibiniitech.smartswine"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-center gap-2.5 rounded-2xl border border-zinc-200 bg-white px-6 py-4 text-sm font-bold text-zinc-700 shadow-sm hover:bg-zinc-50 active:scale-95 transition-all duration-200"
          >
            <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none">
              <path d="M3.60938 2.0625C3.39688 2.275 3.28125 2.6125 3.28125 3.0375V20.9625C3.28125 21.3875 3.39688 21.725 3.60938 21.9375L3.68438 22.0125L13.7219 11.975V11.825L3.68438 1.7875L3.60938 2.0625Z" fill="#00E5FF"/>
              <path d="M17.0625 8.6125L13.725 11.95V12.05L17.0625 15.3875L17.1375 15.3125L21.0875 13.0625C22.2125 12.425 22.2125 11.575 21.0875 10.9375L17.1375 8.6875L17.0625 8.6125Z" fill="#FFC107"/>
              <path d="M17.1375 15.3125L13.725 11.9062L3.60938 22.0219C3.95938 22.3844 4.54688 22.4219 5.23438 22.0219L17.1375 15.3125Z" fill="#FF3D00"/>
              <path d="M17.1375 8.6875L5.23438 1.97812C4.54688 1.57812 3.95938 1.61562 3.60938 1.97812L13.725 12.0938L17.1375 8.6875Z" fill="#4CAF50"/>
            </svg>
            <span>Download Mobile App</span>
          </a>

          {/* PWA Install prompt — shown only when browser supports it */}
          {showInstallButton && (
            <button
              id="pwa-install-button"
              onClick={onInstallClick}
              className="flex items-center justify-center gap-2 rounded-2xl border border-emerald-200 bg-emerald-50 px-6 py-4 text-sm font-bold text-emerald-700 hover:bg-emerald-100 active:scale-95 transition-all duration-200"
            >
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
              </svg>
              Install App
            </button>
          )}
        </div>
      </main>

      {/* Footer */}
      <footer className="relative z-10 px-6 pb-safe-bottom pb-6 pt-4 border-t border-zinc-100 text-center">
        <p className="text-xs text-zinc-400">Copyright 2026 · Goshen AgriFirm & Bibinii Tech</p>
        <div className="mt-2 flex items-center justify-center gap-4">
          <a href="https://wa.me/233544737870" target="_blank" rel="noopener noreferrer" className="text-xs font-semibold text-zinc-500 hover:text-emerald-600 transition-colors flex items-center gap-1">
            <svg className="h-4 w-4 fill-current" viewBox="0 0 24 24"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L0 24l6.335-1.662c1.746.953 3.71 1.458 5.704 1.459h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/></svg>
            WhatsApp
          </a>
          <a href="https://t.me/BibiniiTech" target="_blank" rel="noopener noreferrer" className="text-xs font-semibold text-zinc-500 hover:text-sky-600 transition-colors flex items-center gap-1">
            <svg className="h-4 w-4 fill-current" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 6.8c-.15 1.58-.8 5.42-1.13 7.19-.14.75-.42 1-.68 1.03-.58.05-1.02-.38-1.58-.75-.88-.58-1.38-.94-2.23-1.5-.99-.65-.35-1.01.22-1.59.15-.15 2.71-2.48 2.76-2.69.01-.03.01-.14-.07-.2-.08-.06-.19-.04-.27-.02-.12.02-1.96 1.24-5.54 3.66-.52.36-.97.53-1.34.52-.42-.01-1.22-.24-1.82-.44-.73-.24-1.31-.37-1.26-.78.03-.22.33-.44.91-.67 3.56-1.55 5.92-2.57 7.09-3.07 3.38-1.42 4.09-1.66 4.54-1.67.1 0 .32.02.47.14.12.1.16.24.18.33.02.09.03.27.01.37z"/></svg>
            Telegram
          </a>
        </div>
      </footer>
    </div>
  );
}
