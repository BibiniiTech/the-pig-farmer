"use client";

import React from "react";
import { useDevice } from "@/context/DeviceContext";
import MobileShell from "@/components/layouts/MobileShell";
import CompleteProfileModal from "@/components/CompleteProfileModal";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isMobile } = useDevice();

  return (
    <>
      <CompleteProfileModal />
      {isMobile ? <MobileShell>{children}</MobileShell> : children}
    </>
  );
}
