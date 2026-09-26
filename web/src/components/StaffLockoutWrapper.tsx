"use client";

import React from "react";
import { useAuth } from "@/context/AuthContext";
import { signOut } from "firebase/auth";
import { auth } from "@/lib/firebase";
import { useTranslations } from "next-intl";

export default function StaffLockoutWrapper({ children }: { children: React.ReactNode }) {
  const t = useTranslations("StaffLockout");
  const { user, userProfile, isStaff, isStaffDenied, loading, detachFromStaffRegistryAndCreateFarm } = useAuth();
  const [detaching, setDetaching] = React.useState(false);

  if (loading) {
    return <>{children}</>;
  }

  const isOwnerNotPremium = userProfile && !userProfile.isPremium && !userProfile.isAdmin;

  // If user is authenticated, is a staff member, and either access is denied or owner is not premium
  if (user && isStaff && (isStaffDenied || isOwnerNotPremium)) {
    return (
      <div className="flex h-screen flex-col items-center justify-center bg-zinc-50 px-4 text-center">
        <div className="max-w-md space-y-6 rounded-2xl bg-white p-8 shadow-xl shadow-zinc-200 border border-zinc-200">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-rose-100">
            <svg className="h-8 w-8 text-rose-600" fill="none" viewBox="0 0 24 24" strokeWidth="1.5" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-zinc-900">
            {isStaffDenied ? "Access Denied" : t("title")}
          </h2>
          <p className="text-zinc-600 text-sm">
            {isStaffDenied
              ? "You do not have permission to view or manage this farm's records. Please contact your farm owner or administrator."
              : t("description")}
          </p>

          <div className="space-y-3 pt-2">
            <button
              onClick={async () => {
                setDetaching(true);
                try {
                  await detachFromStaffRegistryAndCreateFarm();
                } catch (err) {
                  console.error("Failed to create own farm:", err);
                } finally {
                  setDetaching(false);
                }
              }}
              disabled={detaching}
              className="w-full rounded-xl bg-emerald-600 px-4 py-3 text-sm font-semibold text-white shadow-sm hover:bg-emerald-700 transition disabled:opacity-50"
            >
              {detaching ? "Setting up your farm..." : "Create My Own Farm"}
            </button>

            <button
              onClick={() => signOut(auth)}
              className="w-full rounded-xl bg-zinc-100 px-4 py-3 text-sm font-semibold text-zinc-700 hover:bg-zinc-200 transition"
            >
              {t("signOut")}
            </button>
          </div>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
