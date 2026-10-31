"use client";

import { useEffect } from "react";
import { usePathname } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

export default function MonetagTag() {
  const pathname = usePathname();
  const { userProfile } = useAuth();

  useEffect(() => {
    // Never load ad tags for paid premium members, admins, or on the migration redirect page
    if (pathname === "/moved" || userProfile?.isPremium || userProfile?.isAdmin) {
      return;
    }

    // Prevent duplicate tag injection
    if (document.querySelector('script[data-zone="12162725"]')) {
      return;
    }

    try {
      const s = document.createElement("script");
      s.dataset.zone = "12162725";
      s.src = "https://nap5k.com/tag.min.js";
      const target = [document.documentElement, document.body].filter(Boolean).pop();
      if (target) {
        target.appendChild(s);
      }
    } catch (e) {
      console.debug("Monetag tag initialization:", e);
    }
  }, [pathname, userProfile]);

  return null;
}
