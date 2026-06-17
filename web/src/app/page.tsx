"use client";

import React, { useState } from "react";
import Link from "next/link";

export default function Home() {
  const [langMenuOpen, setLangMenuOpen] = useState(false);
  const [selectedLang, setSelectedLang] = useState("en");

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

  const currentLang = languages.find((l) => l.code === selectedLang) || languages[0];

  return (
    <main className="relative flex min-h-screen flex-col items-center overflow-hidden bg-zinc-50 px-4 text-center">
      {/* Floating Upper Left Logo */}
      <div className="absolute top-6 left-6 z-20 flex items-center gap-3 select-none">
        <img
          src="/app_logo.png"
          alt="SmartSwine Logo"
          className="h-10 w-10 object-contain rounded-lg shadow-sm border border-zinc-200/50 bg-white p-1"
        />
        <div className="flex flex-col items-start text-left">
          <span className="font-extrabold text-lg tracking-tight text-zinc-900 leading-tight">
            SmartSwine
          </span>
          <span className="text-[10px] font-bold text-emerald-800 tracking-wider uppercase leading-none mt-0.5">
            Piggery Manager
          </span>
        </div>
      </div>

      {/* Floating Upper Right Language Selector */}
      <div className="absolute top-6 right-6 z-20">
        <button
          onClick={() => setLangMenuOpen(!langMenuOpen)}
          className="flex items-center gap-2 rounded-lg border border-zinc-200 bg-white px-3.5 py-2 text-sm font-semibold text-zinc-700 shadow-sm hover:bg-zinc-50 transition-all select-none"
        >
          <span>{currentLang.flag}</span>
          <span className="hidden sm:inline">{currentLang.name}</span>
          <svg
            className={`h-4 w-4 text-zinc-400 transition-transform ${langMenuOpen ? "rotate-180" : ""}`}
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fillRule="evenodd"
              d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z"
              clipRule="evenodd"
            />
          </svg>
        </button>

        {langMenuOpen && (
          <>
            {/* Click outside overlay */}
            <div
              className="fixed inset-0 z-20 cursor-default"
              onClick={() => setLangMenuOpen(false)}
            />
            {/* Dropdown Menu */}
            <div className="absolute right-0 mt-2 w-48 rounded-xl border border-zinc-200 bg-white p-1.5 shadow-xl z-30">
              <div className="grid grid-cols-1 gap-0.5">
                {languages.map((lang) => (
                  <button
                    key={lang.code}
                    onClick={() => {
                      setSelectedLang(lang.code);
                      setLangMenuOpen(false);
                    }}
                    className={`flex items-center gap-2.5 w-full rounded-lg px-3 py-2 text-left text-sm transition-colors ${
                      selectedLang === lang.code
                        ? "bg-emerald-50 text-emerald-800 font-semibold"
                        : "text-zinc-700 hover:bg-zinc-50"
                    }`}
                  >
                    <span className="text-base leading-none">{lang.flag}</span>
                    <span>{lang.name}</span>
                  </button>
                ))}
              </div>
            </div>
          </>
        )}
      </div>

      {/* Glow ambient background */}
      <div className="absolute top-1/4 left-1/3 -z-10 h-[500px] w-[500px] rounded-full bg-emerald-500/10 blur-[120px]" />
      <div className="absolute bottom-1/4 right-1/3 -z-10 h-[500px] w-[500px] rounded-full bg-violet-600/10 blur-[120px]" />

      {/* Watermark Logo Background */}
      <div className="fixed inset-0 z-0 flex items-center justify-center opacity-[0.18] pointer-events-none select-none">
        <img
          src="/app_logo.png"
          alt="Watermark Background Logo"
          className="w-full max-w-[1100px] max-h-[85vh] object-contain"
        />
      </div>

      {/* Center Hero Wrapper */}
      <div className="relative z-10 max-w-3xl flex-1 flex flex-col items-center justify-center space-y-8 py-24">
        <div className="inline-flex items-center gap-2 rounded-full border border-zinc-200 bg-white/70 px-4 py-1.5 text-sm text-zinc-600 shadow-sm backdrop-blur-md">
          <span className="flex h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
          <span>Now in Web Beta</span>
        </div>

        <h1 className="text-5xl font-extrabold tracking-tight sm:text-6xl">
          <span className="block bg-gradient-to-r from-emerald-950 via-emerald-800 to-green-600 bg-clip-text text-transparent">
            SmartSwine
          </span>
          <span className="block text-2xl sm:text-3xl font-bold text-emerald-900 mt-1">
            Piggery Manager
          </span>
          <span className="block text-2xl sm:text-3xl font-bold text-emerald-900 mt-1">
            Farm Smarter: Not Harder
          </span>
        </h1>

        <p className="mx-auto max-w-xl text-lg text-zinc-600">
          Track growth performance, Formulate feed with local ingredients, diagnose disease before they spread, weigh with tape, manage tasks, and optimize staff activities and financials in real-time. Shared data across devices.
        </p>

        <div className="flex flex-wrap justify-center gap-4 pt-4">
          <Link
            id="start-button"
            href="/login"
            className="rounded-lg bg-gradient-to-r from-emerald-600 to-emerald-700 px-8 py-4 text-base font-semibold text-white shadow-lg shadow-emerald-600/20 hover:from-emerald-700 hover:to-emerald-800 transition-all duration-200"
          >
            Launch Web App
          </Link>
          <a
            href="https://play.google.com/store/apps/details?id=com.bibiniitech.smartswine"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2.5 rounded-lg border border-zinc-200 bg-white px-8 py-4 text-base font-semibold text-zinc-700 hover:bg-zinc-50 hover:text-zinc-900 shadow-sm transition-all duration-200"
          >
            <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M3.60938 2.0625C3.39688 2.275 3.28125 2.6125 3.28125 3.0375V20.9625C3.28125 21.3875 3.39688 21.725 3.60938 21.9375L3.68438 22.0125L13.7219 11.975V11.825L3.68438 1.7875L3.60938 2.0625Z" fill="#00E5FF"/>
              <path d="M17.0625 8.6125L13.725 11.95V12.05L17.0625 15.3875L17.1375 15.3125L21.0875 13.0625C22.2125 12.425 22.2125 11.575 21.0875 10.9375L17.1375 8.6875L17.0625 8.6125Z" fill="#FFC107"/>
              <path d="M17.1375 15.3125L13.725 11.9062L3.60938 22.0219C3.95938 22.3844 4.54688 22.4219 5.23438 22.0219L17.1375 15.3125Z" fill="#FF3D00"/>
              <path d="M17.1375 8.6875L5.23438 1.97812C4.54688 1.57812 3.95938 1.61562 3.60938 1.97812L13.725 12.0938L17.1375 8.6875Z" fill="#4CAF50"/>
            </svg>
            <span>Download Mobile App</span>
          </a>
        </div>
      </div>

      {/* Footer */}
      <footer className="relative z-10 w-full mt-auto py-6 border-t border-zinc-200/50 flex flex-col sm:flex-row items-center justify-between gap-4 text-zinc-500 text-xs max-w-5xl px-4">
        <div className="text-center sm:text-left space-y-1">
          <p>Copyright 2026</p>
          <p>Developed by: Goshen AgriFirm & Bibinii Tech</p>
        </div>

        <div className="flex items-center gap-4">
          {/* WhatsApp Link */}
          <a
            href="https://wa.me/233544737870"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1.5 hover:text-emerald-600 transition-colors"
            title="Chat on WhatsApp (+233544737870)"
          >
            <svg className="h-5 w-5 fill-current" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L0 24l6.335-1.662c1.746.953 3.71 1.458 5.704 1.459h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
            </svg>
            <span className="font-semibold">WhatsApp</span>
          </a>

          {/* Telegram Link */}
          <a
            href="https://t.me/BibiniiTech"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1.5 hover:text-sky-600 transition-colors"
            title="Chat on Telegram (t.me/BibiniiTech)"
          >
            <svg className="h-5 w-5 fill-current" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 6.8c-.15 1.58-.8 5.42-1.13 7.19-.14.75-.42 1-.68 1.03-.58.05-1.02-.38-1.58-.75-.88-.58-1.38-.94-2.23-1.5-.99-.65-.35-1.01.22-1.59.15-.15 2.71-2.48 2.76-2.69.01-.03.01-.14-.07-.2-.08-.06-.19-.04-.27-.02-.12.02-1.96 1.24-5.54 3.66-.52.36-.97.53-1.34.52-.42-.01-1.22-.24-1.82-.44-.73-.24-1.31-.37-1.26-.78.03-.22.33-.44.91-.67 3.56-1.55 5.92-2.57 7.09-3.07 3.38-1.42 4.09-1.66 4.54-1.67.1 0 .32.02.47.14.12.1.16.24.18.33.02.09.03.27.01.37z"/>
            </svg>
            <span className="font-semibold">Telegram</span>
          </a>
        </div>
      </footer>
    </main>
  );
}
