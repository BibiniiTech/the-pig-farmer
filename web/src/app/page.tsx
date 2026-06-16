"use client";

import Link from "next/link";

export default function Home() {
  return (
    <main className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-zinc-950 px-4 text-center">
      {/* Glow ambient background */}
      <div className="absolute top-1/4 left-1/3 -z-10 h-[500px] w-[500px] rounded-full bg-emerald-500/5 blur-[120px]" />
      <div className="absolute bottom-1/4 right-1/3 -z-10 h-[500px] w-[500px] rounded-full bg-violet-600/5 blur-[120px]" />

      <div className="max-w-3xl space-y-8">
        <div className="inline-flex items-center gap-2 rounded-full border border-zinc-800 bg-zinc-900/50 px-4 py-1.5 text-sm text-zinc-300 backdrop-blur-md">
          <span className="flex h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
          <span>Now in Web Beta</span>
        </div>

        <h1 className="text-4xl font-extrabold tracking-tight text-white sm:text-6xl bg-gradient-to-r from-white via-zinc-200 to-zinc-400 bg-clip-text text-transparent">
          Smart Swine Management <br />
          <span className="bg-gradient-to-r from-emerald-400 to-violet-500 bg-clip-text text-transparent">
            Simplified & Optimized
          </span>
        </h1>

        <p className="mx-auto max-w-xl text-lg text-zinc-400">
          Formulate precise feed mixes, track herd growth, manage tasks, and optimize financials in real-time. Shared backend with your mobile app.
        </p>

        <div className="flex flex-wrap justify-center gap-4 pt-4">
          <Link
            id="start-button"
            href="/login"
            className="rounded-lg bg-gradient-to-r from-emerald-500 to-emerald-600 px-8 py-4 text-base font-semibold text-white shadow-lg shadow-emerald-500/20 hover:from-emerald-600 hover:to-emerald-700 transition-all duration-200"
          >
            Launch Web App
          </Link>
          <a
            href="https://play.google.com"
            target="_blank"
            rel="noopener noreferrer"
            className="rounded-lg border border-zinc-800 bg-zinc-900/40 px-8 py-4 text-base font-semibold text-zinc-300 hover:bg-zinc-900 hover:text-white transition-all duration-200"
          >
            Download Mobile App
          </a>
        </div>
      </div>
    </main>
  );
}
