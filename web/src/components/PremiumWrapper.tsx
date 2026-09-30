"use client";

import React, { useState } from "react";
import { useAuth } from "@/context/AuthContext";
import Link from "next/link";
import { LockClosedIcon } from "@heroicons/react/24/solid";
import { useTranslations } from "next-intl";
import RewardedPassModal from "@/components/ads/RewardedPassModal";

interface PremiumWrapperProps {
  children: React.ReactNode;
  fallback?: React.ReactNode;
  featureName?: string;
  allowPass?: boolean;
}

export default function PremiumWrapper({
  children,
  fallback,
  featureName,
  allowPass = true,
}: PremiumWrapperProps) {
  const t = useTranslations("Premium");
  const { userProfile, loading, isPassActive, isPaidPremium } = useAuth();
  const [showRewardedModal, setShowRewardedModal] = useState(false);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-emerald-500"></div>
      </div>
    );
  }

  const isAllowed =
    isPaidPremium ||
    userProfile?.isPremium === true ||
    userProfile?.isAdmin === true ||
    (allowPass && isPassActive);

  if (isAllowed) {
    return <>{children}</>;
  }

  if (fallback) {
    return <>{fallback}</>;
  }

  return (
    <>
      <div className="flex flex-col items-center justify-center p-8 bg-white/50 backdrop-blur-sm rounded-3xl border border-emerald-100 shadow-xl min-h-[50vh] text-center max-w-xl mx-auto my-6">
        <div className="bg-emerald-100 p-4 rounded-full mb-6 text-emerald-600">
          <LockClosedIcon className="w-12 h-12" />
        </div>
        <h2 className="text-2xl font-bold text-gray-800 mb-2">{t("featureTitle")}</h2>
        <p className="text-gray-600 mb-6 max-w-md text-sm">
          {t("featureDesc")}
        </p>

        <div className="flex flex-col sm:flex-row items-center gap-3">
          <button
            type="button"
            onClick={() => setShowRewardedModal(true)}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3.5 bg-gradient-to-r from-amber-500 to-amber-600 text-white rounded-xl font-bold shadow-md hover:shadow-lg hover:from-amber-600 hover:to-amber-700 transition active:scale-95 text-sm"
          >
            <span>▶</span>
            <span>Watch Ad for 3-Hour Pass</span>
          </button>
          <Link href="/dashboard/billing" className="w-full sm:w-auto">
            <span className="w-full inline-block px-6 py-3.5 bg-gradient-to-r from-emerald-600 to-green-500 text-white rounded-xl font-bold shadow-md hover:shadow-lg hover:from-emerald-700 hover:to-green-600 transition active:scale-95 text-sm">
              {t("upgradeButton")}
            </span>
          </Link>
        </div>
      </div>

      <RewardedPassModal
        isOpen={showRewardedModal}
        onClose={() => setShowRewardedModal(false)}
        featureName={featureName || "Premium Feature"}
      />
    </>
  );
}
