"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

interface RewardedPassModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  description?: string;
  featureName?: string;
  onSuccess?: () => void;
}

export default function RewardedPassModal({
  isOpen,
  onClose,
  title,
  description = "Watch a short video ad to unlock all premium analysis, disease finding, and PDF report tools for 3 hours!",
  featureName,
  onSuccess,
}: RewardedPassModalProps) {
  const modalTitle = title || (featureName ? `Unlock ${featureName}` : "Unlock Premium Feature");
  const router = useRouter();
  const { activate3HourPass } = useAuth();
  const [isPlayingAd, setIsPlayingAd] = useState(false);
  const [countdown, setCountdown] = useState(15);
  const [adFinished, setAdFinished] = useState(false);
  const [isActivating, setIsActivating] = useState(false);

  // Reset ad player state when modal opens/closes
  useEffect(() => {
    if (!isOpen) {
      setIsPlayingAd(false);
      setCountdown(15);
      setAdFinished(false);
      setIsActivating(false);
    }
  }, [isOpen]);

  // Handle ad countdown timer
  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (isPlayingAd && countdown > 0) {
      timer = setTimeout(() => {
        setCountdown((prev) => prev - 1);
      }, 1000);
    } else if (isPlayingAd && countdown === 0 && !adFinished) {
      setAdFinished(true);
      handleRewardEarned();
    }
    return () => clearTimeout(timer);
  }, [isPlayingAd, countdown, adFinished]);

  const handleStartAd = () => {
    setIsPlayingAd(true);
    setCountdown(15);
    setAdFinished(false);
  };

  const handleRewardEarned = async () => {
    setIsActivating(true);
    try {
      await activate3HourPass();
      setTimeout(() => {
        onSuccess?.();
        onClose();
      }, 1500);
    } catch (err) {
      console.error("Error activating ad pass:", err);
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div
        className="bg-white dark:bg-zinc-900 border border-emerald-500/30 rounded-3xl w-full max-w-md p-6 sm:p-7 space-y-5 shadow-2xl relative overflow-hidden text-center max-h-[92vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {isPlayingAd ? (
          // In-Ad Video Playing State
          <div className="space-y-4 py-2">
            <div className="flex items-center justify-between text-xs text-zinc-500">
              <span className="bg-emerald-100 text-emerald-800 text-[10px] font-black px-2 py-0.5 rounded uppercase">
                Sponsored Ad
              </span>
              <span className="font-mono font-bold text-zinc-700 dark:text-zinc-300">
                Reward in: {countdown}s
              </span>
            </div>

            {/* Video Player Display Container */}
            <div className="relative aspect-video rounded-2xl bg-zinc-950 flex flex-col items-center justify-center overflow-hidden border border-zinc-800 shadow-inner">
              <div className="absolute inset-0 bg-gradient-to-tr from-emerald-900/40 via-zinc-900 to-black opacity-80" />
              <div className="relative z-10 text-center space-y-3 px-4">
                <div className="w-14 h-14 rounded-full bg-emerald-600/30 border-2 border-emerald-400 flex items-center justify-center mx-auto animate-pulse">
                  <span className="text-2xl">🌾</span>
                </div>
                <h4 className="text-white font-bold text-sm">
                  SmartSwine Commercial Farm Solutions
                </h4>
                <p className="text-zinc-300 text-xs max-w-xs">
                  Advanced nutritional formulation & swine health diagnostics for commercial piggers.
                </p>
              </div>

              {/* Video Progress Bar */}
              <div className="absolute bottom-0 left-0 right-0 h-1.5 bg-zinc-800">
                <div
                  className="h-full bg-emerald-500 transition-all duration-1000 ease-linear"
                  style={{ width: `${((15 - countdown) / 15) * 100}%` }}
                />
              </div>
            </div>

            {adFinished ? (
              <div className="rounded-xl bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-300 p-3 text-center space-y-1 animate-in zoom-in-95">
                <p className="text-xs font-black text-emerald-800 dark:text-emerald-300">
                  🎉 Reward Unlocked!
                </p>
                <p className="text-[11px] text-emerald-700 dark:text-emerald-400">
                  Activating your 3-hour Universal Premium Pass...
                </p>
              </div>
            ) : (
              <p className="text-xs text-zinc-400">
                Please watch the ad to complete your 3-hour temporary pass unlock.
              </p>
            )}

            {!adFinished && (
              <button
                type="button"
                onClick={onClose}
                className="text-xs text-zinc-400 hover:text-zinc-600 underline pt-2"
              >
                Cancel and return
              </button>
            )}
          </div>
        ) : (
          // Initial Rewarded Dialog State matching Android RewardedPassDialog
          <>
            {/* Glowing Icon Badge */}
            <div className="mx-auto w-[76px] h-[76px] rounded-full bg-gradient-to-tr from-amber-400/30 via-emerald-600/25 to-transparent flex items-center justify-center p-2 shadow-inner">
              <div className="w-14 h-14 rounded-full bg-[#133E25] border-2 border-[#FFB300] flex items-center justify-center shadow-lg">
                <svg
                  className="w-7 h-7 text-[#FFB300]"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                >
                  <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z" />
                </svg>
              </div>
            </div>

            {/* Header */}
            <div className="space-y-2">
              <h2 className="text-xl sm:text-2xl font-black text-zinc-900 dark:text-zinc-100 tracking-tight">
                {modalTitle}
              </h2>
              <p className="text-xs text-zinc-500 dark:text-zinc-400 max-w-sm mx-auto leading-relaxed">
                {description}
              </p>
            </div>

            {/* Feature Highlights Matrix */}
            <div className="bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200/80 dark:border-zinc-700/50 rounded-2xl p-4 text-left space-y-2.5 shadow-sm">
              <div className="flex items-start gap-2.5">
                <span className="text-emerald-600 font-bold text-sm">✓</span>
                <span className="text-xs font-medium text-zinc-800 dark:text-zinc-200 leading-snug">
                  PDF Reports for Herd, Feed, HR & Finances
                </span>
              </div>
              <div className="flex items-start gap-2.5">
                <span className="text-emerald-600 font-bold text-sm">✓</span>
                <span className="text-xs font-medium text-zinc-800 dark:text-zinc-200 leading-snug">
                  Mix Balanced Feed with Unlimited Ingredients
                </span>
              </div>
              <div className="flex items-start gap-2.5">
                <span className="text-emerald-600 font-bold text-sm">✓</span>
                <span className="text-xs font-medium text-zinc-800 dark:text-zinc-200 leading-snug">
                  Find Diseases & Veterinary Guidance
                </span>
              </div>
            </div>

            {/* Actions */}
            <div className="space-y-2.5 pt-1">
              {/* Watch Ad Action Button */}
              <button
                type="button"
                onClick={handleStartAd}
                className="w-full py-3.5 px-4 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-xs sm:text-sm flex items-center justify-center gap-2.5 transition-all shadow-md active:scale-[0.98]"
              >
                <svg className="w-5 h-5 shrink-0" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z" />
                </svg>
                <div className="text-left">
                  <div>Watch Ad for 3-Hour Premium Pass</div>
                  <div className="text-[10px] text-emerald-200 font-normal">
                    Instant temporary premium access
                  </div>
                </div>
              </button>

              {/* Upgrade to Premium Access Button */}
              <button
                type="button"
                onClick={() => {
                  onClose();
                  router.push("/dashboard/billing");
                }}
                className="w-full py-3 px-4 rounded-xl border border-amber-500/80 bg-amber-400/10 hover:bg-amber-400/20 text-zinc-900 dark:text-zinc-100 font-bold text-xs flex items-center justify-center gap-2 transition"
              >
                <span>💎 Upgrade to Premium Access</span>
              </button>

              {/* Dismiss */}
              <button
                type="button"
                onClick={onClose}
                className="w-full py-2 text-xs font-medium text-zinc-500 hover:text-zinc-700 transition"
              >
                Maybe Later
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
