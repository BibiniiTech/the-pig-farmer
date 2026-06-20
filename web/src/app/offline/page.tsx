export default function OfflinePage() {
  return (
    <main className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-white px-6 text-center">
      {/* Ambient glow */}
      <div className="pointer-events-none absolute top-[-100px] left-[-80px] h-96 w-96 rounded-full bg-emerald-400/15 blur-3xl" />
      <div className="pointer-events-none absolute bottom-[-80px] right-[-60px] h-80 w-80 rounded-full bg-violet-400/10 blur-3xl" />

      {/* Watermark */}
      <div className="pointer-events-none fixed inset-0 z-0 flex items-center justify-center opacity-[0.08]">
        <img src="/app_logo.png" alt="" className="w-full max-w-[800px] object-contain" />
      </div>

      <div className="relative z-10 flex flex-col items-center gap-6 max-w-sm">
        {/* Logo */}
        <div className="relative mb-2">
          <div className="absolute inset-0 rounded-3xl bg-emerald-300/30 blur-2xl scale-110" />
          <img
            src="/app_logo.png"
            alt="SmartSwine"
            className="relative h-24 w-24 rounded-2xl object-contain border border-zinc-100 bg-white p-2 shadow-xl"
          />
        </div>

        {/* No-wifi icon */}
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-zinc-100 shadow-inner">
          <svg
            className="h-9 w-9 text-zinc-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth="1.8"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M3 3l18 18M8.286 8.286A7.95 7.95 0 0012 7.5c1.93 0 3.7.686 5.077 1.82M12 19.5a.75.75 0 110-1.5.75.75 0 010 1.5zm-4.595-4.595A5.95 5.95 0 0112 13.5c1.433 0 2.747.505 3.77 1.338"
            />
          </svg>
        </div>

        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-zinc-900">
            You&apos;re Offline
          </h1>
          <p className="mt-2 text-sm text-zinc-500 leading-relaxed">
            SmartSwine needs an internet connection to load your farm data.
            Please check your connection and try again.
          </p>
        </div>

        <button
          onClick={() => window.location.reload()}
          className="mt-2 flex items-center gap-2 rounded-2xl bg-gradient-to-r from-emerald-600 to-emerald-700 px-8 py-3.5 text-sm font-bold text-white shadow-lg shadow-emerald-600/20 hover:from-emerald-700 hover:to-emerald-800 active:scale-95 transition-all duration-200"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
            <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          Try Again
        </button>

        <p className="text-xs text-zinc-400">
          SmartSwine · Piggery Manager
        </p>
      </div>
    </main>
  );
}
