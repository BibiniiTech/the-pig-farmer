import Link from "next/link";

export default function NotFound() {
  return (
    <div className="min-h-screen bg-zinc-50 flex flex-col items-center justify-center p-6 text-center font-sans">
      <div className="max-w-md w-full bg-white rounded-3xl p-8 sm:p-10 shadow-xl border border-zinc-200/80">
        <div className="relative inline-block mb-6">
          <img
            src="/app_logo.png"
            alt="SmartSwine"
            className="w-20 h-20 sm:w-24 sm:h-24 object-contain rounded-2xl shadow-md border border-emerald-100 bg-white p-2 mx-auto"
          />
        </div>

        <span className="inline-block px-3 py-1 rounded-full bg-rose-50 text-rose-700 text-xs font-bold uppercase tracking-wider mb-3">
          Error 404
        </span>

        <h1 className="text-2xl sm:text-3xl font-extrabold text-zinc-900 tracking-tight">
          Page Not Found
        </h1>

        <p className="mt-3 text-sm text-zinc-600">
          The page you are looking for might have been moved, removed, or doesn&apos;t exist.
        </p>

        <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3">
          <Link
            href="/dashboard"
            className="w-full sm:w-auto px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white font-semibold text-sm rounded-xl shadow-md transition"
          >
            Go to Dashboard
          </Link>
          <Link
            href="/"
            className="w-full sm:w-auto px-6 py-2.5 bg-zinc-100 hover:bg-zinc-200 text-zinc-700 font-semibold text-sm rounded-xl transition"
          >
            Home Page
          </Link>
        </div>
      </div>
    </div>
  );
}
