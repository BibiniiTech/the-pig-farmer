"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { signOut } from "firebase/auth";
import { auth } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import SettingsModal from "@/components/SettingsModal";

export default function UserProfileDropdown() {
  const [isOpen, setIsOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const router = useRouter();
  const { userProfile, isStaff } = useAuth();

  const handleSignOut = async () => {
    try {
      setIsOpen(false);
      await signOut(auth);
      router.push("/login");
    } catch (err) {
      console.error("Logout failed:", err);
    }
  };

  const initials = (userProfile?.firstName?.[0] || "U").toUpperCase();

  return (
    <div className="relative inline-block text-left">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2.5 p-1 pr-3 rounded-full border border-zinc-200 bg-white hover:bg-zinc-50 transition shadow-sm focus:outline-none select-none group"
      >
        <div className="h-8 w-8 rounded-full bg-gradient-to-br from-emerald-400 to-emerald-700 flex items-center justify-center text-white font-bold text-xs shadow-inner">
          {initials}
        </div>
        <div className="hidden lg:block text-left leading-none">
          <p className="text-[11px] font-bold text-zinc-900 truncate max-w-[100px]">
            {userProfile?.firstName}
          </p>
          <p className="text-[9px] text-zinc-400 font-semibold mt-0.5">
            {isStaff ? "Staff" : "Owner"}
          </p>
        </div>
        <svg
          className={`h-3 w-3 text-zinc-400 transition-transform duration-300 ${isOpen ? "rotate-180" : ""}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth="2.5"
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {/* Click Outside Overlay */}
      {isOpen && (
        <div
          className="fixed inset-0 z-30 cursor-default"
          onClick={() => setIsOpen(false)}
        />
      )}

      {/* Profile Menu */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-56 rounded-2xl border border-zinc-200 bg-white shadow-2xl z-40 py-2 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200 origin-top-right">
          <div className="px-4 py-3 border-b border-zinc-100 bg-zinc-50/50">
            <p className="text-xs font-bold text-zinc-900">{userProfile?.firstName} {userProfile?.lastName}</p>
            <p className="text-[10px] text-zinc-500 truncate">{userProfile?.email}</p>
          </div>

          <div className="py-1">
            <button
              onClick={() => {
                setIsOpen(false);
                setIsSettingsModalOpen(true);
              }}
              className="flex w-full items-center gap-3 px-4 py-2.5 text-xs font-bold text-zinc-700 hover:bg-zinc-50 transition duration-200"
            >
              <svg className="h-4 w-4 text-zinc-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <span>Account Settings</span>
            </button>
          </div>

          <div className="border-t border-zinc-100 my-1" />

          <div className="py-1">
            <button
              onClick={handleSignOut}
              className="flex w-full items-center gap-3 px-4 py-2.5 text-xs font-bold text-red-600 hover:bg-red-50 transition duration-200"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
              <span>Sign Out</span>
            </button>
          </div>
        </div>
      )}

      <SettingsModal
        isOpen={isSettingsModalOpen}
        onClose={() => setIsSettingsModalOpen(false)}
      />
    </div>
  );
}
