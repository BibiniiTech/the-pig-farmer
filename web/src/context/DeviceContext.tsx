"use client";

import React, { createContext, useContext, useEffect, useState } from "react";

type DeviceType = "mobile" | "desktop";

interface DeviceContextType {
  isMobile: boolean;
  deviceType: DeviceType;
}

const DeviceContext = createContext<DeviceContextType>({
  isMobile: false,
  deviceType: "desktop",
});

function getDeviceFromCookie(): DeviceType {
  if (typeof document === "undefined") return "desktop";
  const match = document.cookie
    .split("; ")
    .find((row) => row.startsWith("device-type="));
  const value = match?.split("=")[1];
  return value === "mobile" ? "mobile" : "desktop";
}

export function DeviceProvider({
  children,
  initialDevice = "desktop",
}: {
  children: React.ReactNode;
  initialDevice?: DeviceType;
}) {
  const [deviceType, setDeviceType] = useState<DeviceType>(initialDevice);

  useEffect(() => {
    // On the client, read the cookie set by middleware for ground-truth
    const cookieDevice = getDeviceFromCookie();
    setDeviceType(cookieDevice);
  }, []);

  return (
    <DeviceContext.Provider
      value={{ isMobile: deviceType === "mobile", deviceType }}
    >
      {children}
    </DeviceContext.Provider>
  );
}

export const useDevice = () => useContext(DeviceContext);
