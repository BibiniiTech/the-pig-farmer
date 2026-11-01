"use client";

import { useEffect } from "react";
import { usePathname } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

export default function MonetagTag() {
  const pathname = usePathname();
  const { userProfile } = useAuth();

  useEffect(() => {
    // 1. Unregister and purge any background service workers (killing legacy Zone 11929267 push ads)
    if (typeof window !== "undefined" && "serviceWorker" in navigator) {
      navigator.serviceWorker.getRegistrations().then((registrations) => {
        for (const reg of registrations) {
          reg.unregister().catch(() => {});
        }
      });
    }

    // 2. Never load ad tags for paid premium members, admins, or on the migration redirect page
    if (pathname === "/moved" || userProfile?.isPremium || userProfile?.isAdmin) {
      return;
    }

    // 3. Inject ONLY the approved In-Page Push tag (Zone 12162725)
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
      console.debug("Monetag in-page tag initialization:", e);
    }
  }, [pathname, userProfile]);

  return null;
}
