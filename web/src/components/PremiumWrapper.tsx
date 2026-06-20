"use client";

import React from "react";
import { useAuth } from "@/context/AuthContext";
import Link from "next/link";
import { LockClosedIcon } from "@heroicons/react/24/solid";

interface PremiumWrapperProps {
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

export default function PremiumWrapper({ children, fallback }: PremiumWrapperProps) {
  const { userProfile, loading } = useAuth();

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-emerald-500"></div>
      </div>
    );
  }

  const isPremiumUser = userProfile?.isPremium === true || userProfile?.isAdmin === true;

  if (isPremiumUser) {
    return <>{children}</>;
  }

  if (fallback) {
    return <>{fallback}</>;
  }

  return (
    <div className="flex flex-col items-center justify-center p-8 bg-white/50 backdrop-blur-sm rounded-3xl border border-emerald-100 shadow-xl min-h-[50vh] text-center">
      <div className="bg-emerald-100 p-4 rounded-full mb-6 text-emerald-600">
        <LockClosedIcon className="w-12 h-12" />
      </div>
      <h2 className="text-2xl font-bold text-gray-800 mb-4">Premium Feature</h2>
      <p className="text-gray-600 mb-8 max-w-md">
        This feature is exclusive to Smart Swine Premium users. Upgrade to unlock powerful tools to manage and grow your farm.
      </p>
      <Link href="/dashboard/billing">
        <span className="inline-block px-8 py-4 bg-gradient-to-r from-emerald-600 to-green-500 text-white rounded-xl font-bold shadow-lg hover:shadow-xl hover:from-emerald-700 hover:to-green-600 transition-all duration-300 transform hover:-translate-y-1">
          Upgrade to Premium
        </span>
      </Link>
    </div>
  );
}
