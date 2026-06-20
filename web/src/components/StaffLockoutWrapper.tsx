"use client";

import React from "react";
import { useAuth } from "@/context/AuthContext";
import { signOut } from "firebase/auth";
import { auth } from "@/lib/firebase";
import { useTranslations } from "next-intl";

export default function StaffLockoutWrapper({ children }: { children: React.ReactNode }) {
  const t = useTranslations("StaffLockout");
  const { user, userProfile, isStaff, loading } = useAuth();

  if (loading) {
    return <>{children}</>;
  }

  // If user is authenticated, is a staff member, and the farm owner is no longer premium
  if (user && isStaff && userProfile && !userProfile.isPremium && !userProfile.isAdmin) {
    return (
      <div className="flex h-screen flex-col items-center justify-center bg-zinc-50 px-4 text-center">
        <div className="max-w-md space-y-6 rounded-2xl bg-white p-8 shadow-xl shadow-zinc-200 border border-zinc-200">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-rose-100">
            <svg className="h-8 w-8 text-rose-600" fill="none" viewBox="0 0 24 24" strokeWidth="1.5" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-zinc-900">{t("title")}</h2>
          <p className="text-zinc-600 text-sm">
            {t("description")}
          </p>
          <button
            onClick={() => signOut(auth)}
            className="mt-4 w-full rounded-xl bg-zinc-900 px-4 py-3 text-sm font-semibold text-white shadow-sm hover:bg-zinc-800 transition"
          >
            {t("signOut")}
          </button>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
