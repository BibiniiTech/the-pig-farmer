"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { signOut } from "firebase/auth";
import { collection, query, limit, onSnapshot } from "firebase/firestore";
import { auth, db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";

export default function DashboardPage() {
  const { user, userProfile, activeFarmUid, isStaff, loading } = useAuth();
  const router = useRouter();

  const [pigCount, setPigCount] = useState<number>(0);
  const [feedCount, setFeedCount] = useState<number>(0);
  const [recentFinancials, setRecentFinancials] = useState<any[]>([]);
  const [statsLoading, setStatsLoading] = useState(true);

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!activeFarmUid) return;

    setStatsLoading(true);

    // 1. Listen to Pigs Count
    const pigsQuery = collection(db, "users", activeFarmUid, "pigs");
    const unsubscribePigs = onSnapshot(pigsQuery, (snapshot) => {
      setPigCount(snapshot.size);
    }, (error) => console.error("Error querying pigs:", error));

    // 2. Listen to Feed Inventory Count
    const feedQuery = collection(db, "users", activeFarmUid, "feed_inventory");
    const unsubscribeFeed = onSnapshot(feedQuery, (snapshot) => {
      setFeedCount(snapshot.size);
    }, (error) => console.error("Error querying feed:", error));

    // 3. Listen to Recent Financial Transactions
    const financialsQuery = query(
      collection(db, "users", activeFarmUid, "financials"),
      limit(5)
    );
    const unsubscribeFinancials = onSnapshot(financialsQuery, (snapshot) => {
      const records = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      setRecentFinancials(records);
      setStatsLoading(false);
    }, (error) => {
      console.error("Error querying financials:", error);
      setStatsLoading(false);
    });

    return () => {
      unsubscribePigs();
      unsubscribeFeed();
      unsubscribeFinancials();
    };
  }, [activeFarmUid]);

  const handleSignOut = async () => {
    try {
      await signOut(auth);
      router.push("/login");
    } catch (err) {
      console.error("Logout failed:", err);
    }
  };

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-zinc-950 text-zinc-100">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 flex flex-col font-sans">
      {/* Navbar Header */}
      <header className="border-b border-zinc-900 bg-zinc-900/40 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="text-2xl">🐖</span>
            <span className="font-bold text-lg bg-gradient-to-r from-emerald-400 to-violet-400 bg-clip-text text-transparent mr-6">
              Smart Swine Dashboard
            </span>
            <nav className="hidden md:flex items-center gap-2">
              <Link href="/dashboard/feed" className="text-xs font-semibold text-zinc-300 hover:text-white transition-all px-3 py-2 rounded-lg hover:bg-zinc-900/60">
                🌾 Feed Inventory
              </Link>
              <Link href="/dashboard/formulator" className="text-xs font-semibold text-zinc-300 hover:text-white transition-all px-3 py-2 rounded-lg hover:bg-zinc-900/60">
                🧪 Feed Formulator
              </Link>
              <Link href="/dashboard/billing" className="text-xs font-semibold text-zinc-300 hover:text-white transition-all px-3 py-2 rounded-lg hover:bg-zinc-900/60">
                💎 Billing & Premium
              </Link>
            </nav>
          </div>

          <div className="flex items-center gap-4">
            <div className="text-right hidden sm:block">
              <p className="text-sm font-medium text-white">{userProfile?.farmName || "Loading Farm..."}</p>
              <p className="text-xs text-zinc-400">
                {isStaff ? "Staff Member" : "Farm Owner"} {userProfile?.isPremium && "• Premium"}
              </p>
            </div>
            <button
              id="logout-button"
              onClick={handleSignOut}
              className="rounded-lg border border-zinc-800 bg-zinc-900/60 px-4 py-2 text-xs font-semibold text-zinc-300 hover:bg-zinc-800 hover:text-white transition-all"
            >
              Sign Out
            </button>
          </div>
        </div>
      </header>

      {/* Main Body */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-zinc-900/30 border border-zinc-900 rounded-2xl p-6">
          <div>
            <h1 className="text-2xl font-bold text-white">Welcome back, {userProfile?.firstName || user.email}!</h1>
            <p className="text-sm text-zinc-400 mt-1">Here is a quick snapshot of your farm operations today.</p>
          </div>
          {userProfile?.isPremium ? (
            <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-500/10 px-3 py-1 text-xs font-medium text-emerald-400 border border-emerald-500/20">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse" />
              Premium Access Active
            </span>
          ) : (
            <div className="flex items-center gap-3">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-zinc-800 px-3 py-1 text-xs font-medium text-zinc-400">
                Free Tier
              </span>
              <Link href="/dashboard/billing" className="inline-flex items-center rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 border border-emerald-500/25 px-3 py-1 text-xs font-bold text-emerald-400 transition-all">
                💎 Upgrade
              </Link>
            </div>
          )}
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
          <div className="bg-zinc-900/50 border border-zinc-900 rounded-2xl p-6 relative overflow-hidden group hover:border-emerald-500/20 transition-all duration-300">
            <div className="absolute top-0 right-0 h-24 w-24 rounded-full bg-emerald-500/5 blur-xl group-hover:bg-emerald-500/10 transition-all duration-300" />
            <h3 className="text-sm font-medium text-zinc-400">Total Pigs Active</h3>
            {statsLoading ? (
              <div className="h-8 w-16 bg-zinc-800 animate-pulse rounded mt-2" />
            ) : (
              <p className="text-3xl font-extrabold text-white mt-2">{pigCount}</p>
            )}
            <p className="text-xs text-emerald-500 mt-2">Active in Herd Manager</p>
          </div>

          <Link href="/dashboard/feed" className="bg-zinc-900/50 border border-zinc-900 rounded-2xl p-6 relative overflow-hidden group hover:border-violet-500/20 transition-all duration-300 cursor-pointer block">
            <div className="absolute top-0 right-0 h-24 w-24 rounded-full bg-violet-600/5 blur-xl group-hover:bg-violet-600/10 transition-all duration-300" />
            <h3 className="text-sm font-medium text-zinc-400">Feed Stock Types</h3>
            {statsLoading ? (
              <div className="h-8 w-16 bg-zinc-800 animate-pulse rounded mt-2" />
            ) : (
              <p className="text-3xl font-extrabold text-white mt-2">{feedCount}</p>
            )}
            <p className="text-xs text-violet-400 mt-2">Managed in Feed Inventory</p>
          </Link>

          <div className="bg-zinc-900/50 border border-zinc-900 rounded-2xl p-6 relative overflow-hidden group hover:border-amber-500/20 transition-all duration-300">
            <div className="absolute top-0 right-0 h-24 w-24 rounded-full bg-amber-500/5 blur-xl group-hover:bg-amber-500/10 transition-all duration-300" />
            <h3 className="text-sm font-medium text-zinc-400">Ledger Records</h3>
            {statsLoading ? (
              <div className="h-8 w-16 bg-zinc-800 animate-pulse rounded mt-2" />
            ) : (
              <p className="text-3xl font-extrabold text-white mt-2">{recentFinancials.length}</p>
            )}
            <p className="text-xs text-amber-500 mt-2">Recorded transactions</p>
          </div>
        </div>

        {/* Quick Tools / Navigation Section */}
        <div className="bg-zinc-900/30 border border-zinc-900 rounded-2xl p-6">
          <h3 className="text-lg font-bold text-white mb-4">Farm Operations</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Link href="/dashboard/feed" className="flex items-start gap-4 p-4 rounded-xl border border-zinc-800/80 bg-zinc-900/20 hover:border-emerald-500/30 hover:bg-zinc-900/40 transition duration-300 group">
              <span className="text-3xl p-3 bg-emerald-500/10 rounded-xl text-emerald-400 group-hover:scale-110 transition duration-300">🌾</span>
              <div>
                <h4 className="text-base font-bold text-white group-hover:text-emerald-400 transition">Feed Inventory</h4>
                <p className="text-sm text-zinc-400 mt-1">Check stock levels, record restocks or feed usage transactions, and view low stock warning alerts.</p>
              </div>
            </Link>

            <Link href="/dashboard/formulator" className="flex items-start gap-4 p-4 rounded-xl border border-zinc-800/80 bg-zinc-900/20 hover:border-violet-500/30 hover:bg-zinc-900/40 transition duration-300 group">
              <span className="text-3xl p-3 bg-violet-500/10 rounded-xl text-violet-400 group-hover:scale-110 transition duration-300">🧪</span>
              <div>
                <h4 className="text-base font-bold text-white group-hover:text-violet-400 transition">Feed Formulator</h4>
                <p className="text-sm text-zinc-400 mt-1">Formulate custom feed rations based on growth stages. Compare actual analysis against target requirements.</p>
              </div>
            </Link>
          </div>
        </div>

        {/* Recent Financials Section */}
        <div className="bg-zinc-900/40 border border-zinc-900 rounded-2xl p-6">
          <h3 className="text-lg font-bold text-white mb-4">Recent Transactions</h3>
          {statsLoading ? (
            <div className="space-y-3">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="h-12 bg-zinc-900 animate-pulse rounded-lg" />
              ))}
            </div>
          ) : recentFinancials.length === 0 ? (
            <p className="text-sm text-zinc-500 text-center py-4">No recent financial transactions found.</p>
          ) : (
            <div className="divide-y divide-zinc-900">
              {recentFinancials.map((record) => (
                <div key={record.id} className="py-3 flex justify-between items-center">
                  <div>
                    <p className="text-sm font-medium text-white">{record.category}</p>
                    <p className="text-xs text-zinc-500">{record.date} • {record.description}</p>
                  </div>
                  <span className={`text-sm font-semibold ${record.type === "Income" ? "text-emerald-500" : "text-red-500"}`}>
                    {record.type === "Income" ? "+" : "-"}${record.amount.toFixed(2)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
