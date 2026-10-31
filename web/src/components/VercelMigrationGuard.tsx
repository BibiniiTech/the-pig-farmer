"use client";

import React, { useEffect, useState } from "react";
import MigrationNoticeContent from "./MigrationNoticeContent";

export default function VercelMigrationGuard({ children }: { children: React.ReactNode }) {
  const [isVercel, setIsVercel] = useState(false);

  useEffect(() => {
    if (typeof window !== "undefined") {
      const hostname = window.location.hostname.toLowerCase();
      if (hostname.includes(".vercel.app")) {
        setIsVercel(true);
      }
    }
  }, []);

  if (isVercel) {
    return <MigrationNoticeContent />;
  }

  return <>{children}</>;
}
