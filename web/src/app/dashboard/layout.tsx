"use client";

import React from "react";
import { useDevice } from "@/context/DeviceContext";
import MobileShell from "@/components/layouts/MobileShell";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isMobile } = useDevice();

  if (isMobile) {
    return <MobileShell>{children}</MobileShell>;
  }

  return <>{children}</>;
}
