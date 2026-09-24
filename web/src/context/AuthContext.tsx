"use client";

import React, { createContext, useContext, useEffect, useState, useRef } from "react";
import { onAuthStateChanged, User } from "firebase/auth";
import { doc, getDoc, collection, getDocs, query, limit, writeBatch, onSnapshot, updateDoc } from "firebase/firestore";
import { auth, db } from "@/lib/firebase";
import { defaultIngredients } from "@/lib/defaultIngredients";
import { getCurrencyByCountry } from "@/lib/currencyUtils";
import { UserProfile } from "@/lib/types";

interface AuthContextType {
  user: User | null;
  userProfile: UserProfile | null;
  activeFarmUid: string | null;
  isStaff: boolean;
  loading: boolean;
  isPassActive: boolean;
  passTimeRemaining: string;
  isPaidPremium: boolean;
  activate3HourPass: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  userProfile: null,
  activeFarmUid: null,
  isStaff: false,
  loading: true,
  isPassActive: false,
  passTimeRemaining: "",
  isPaidPremium: false,
  activate3HourPass: async () => {},
});

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [rawProfile, setRawProfile] = useState<UserProfile | null>(null);
  const [activeFarmUid, setActiveFarmUid] = useState<string | null>(null);
  const [isStaff, setIsStaff] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);
  const [isPassActive, setIsPassActive] = useState<boolean>(false);
  const [passTimeRemaining, setPassTimeRemaining] = useState<string>("");

  // Track last seen country to auto-update currency if it changes (matching Android logic)
  const lastCountryRef = useRef<string | null>(null);

  // 1-second ticker to maintain 3-hour pass remaining time (strictly local to browser, matching Android SharedPreferences)
  useEffect(() => {
    const updateTicker = () => {
      const localExpiry = typeof window !== "undefined"
        ? Number(localStorage.getItem("smartswine_ad_pass_expires_at") || 0)
        : 0;
      const diff = localExpiry - Date.now();

      if (diff > 0) {
        setIsPassActive(true);
        const totalSeconds = Math.floor(diff / 1000);
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;
        setPassTimeRemaining(
          `${String(hours).padStart(2, "0")}h ${String(minutes).padStart(2, "0")}m ${String(seconds).padStart(2, "0")}s`
        );
      } else {
        setIsPassActive(false);
        setPassTimeRemaining("");
      }
    };

    updateTicker();
    const interval = setInterval(updateTicker, 1000);
    return () => clearInterval(interval);
  }, []);

  const activate3HourPass = async () => {
    const durationMs = 3 * 60 * 60 * 1000;
    const newExpiresAt = Date.now() + durationMs;
    if (typeof window !== "undefined") {
      localStorage.setItem("smartswine_ad_pass_expires_at", String(newExpiresAt));
    }
    setIsPassActive(true);
    setPassTimeRemaining("03h 00m 00s");
  };

  useEffect(() => {
    let profileUnsubscribe: (() => void) | null = null;

    const unsubscribeAuth = onAuthStateChanged(auth, async (currentUser) => {
      // Clean up previous profile listener
      if (profileUnsubscribe) {
        profileUnsubscribe();
        profileUnsubscribe = null;
      }

      setUser(currentUser);
      if (currentUser) {
        try {
          // 1. Try to read Owner Profile from users/{uid}
          const userDocRef = doc(db, "users", currentUser.uid);
          const userDocSnap = await getDoc(userDocRef);

          if (userDocSnap.exists()) {
            setActiveFarmUid(currentUser.uid);
            setIsStaff(false);

            // Listen to owner profile in real-time
            profileUnsubscribe = onSnapshot(userDocRef, async (snapshot) => {
              if (snapshot.exists()) {
                const data = snapshot.data();

                // Auto-update currency based on country changes (matching Android logic)
                const currentCountry = data.country || "";
                if (currentCountry && currentCountry !== lastCountryRef.current) {
                  const isCountryChange = lastCountryRef.current !== null && lastCountryRef.current !== "";
                  lastCountryRef.current = currentCountry;

                  // Auto-update if user changed country OR if currency isn't set yet
                  if (isCountryChange || !data.settings?.selectedCurrency || !data.settings?.currencySymbol) {
                    const currency = getCurrencyByCountry(currentCountry);
                    await updateDoc(userDocRef, {
                      "settings.selectedCurrency": currency.code,
                      "settings.currencySymbol": currency.symbol
                    });
                  }
                }

                const isAdminUser =
                  data.admin === true ||
                  data.email === "bibiniitech@gmail.com";
                setRawProfile({ ...data, isAdmin: isAdminUser } as UserProfile);
              }
            });

            // Check and seed default ingredients if collection is empty
            try {
              const ingredientsCollRef = collection(db, "users", currentUser.uid, "feed_ingredients");
              const ingredientsSnap = await getDocs(query(ingredientsCollRef, limit(1)));
              if (ingredientsSnap.empty) {
                console.log("No ingredients found. Seeding default list...");
                const batch = writeBatch(db);
                defaultIngredients.forEach((ing) => {
                  const newDocRef = doc(ingredientsCollRef);
                  batch.set(newDocRef, {
                    ...ing,
                    id: newDocRef.id,
                  });
                });
                await batch.commit();
                console.log("Default ingredients seeded successfully.");
              }
            } catch (seedError) {
              console.error("Error seeding default ingredients:", seedError);
            }
          } else {
            // 2. Check if user is a Staff member
            const email = currentUser.email?.trim().toLowerCase();
            if (email) {
              const registryDocRef = doc(db, "staff_registry", email);
              const registryDocSnap = await getDoc(registryDocRef);

              if (registryDocSnap.exists()) {
                const managerUid = registryDocSnap.data().managerUid;
                if (managerUid) {
                  setActiveFarmUid(managerUid);
                  setIsStaff(true);

                  const managerDocRef = doc(db, "users", managerUid);
                  // Listen to manager profile in real-time
                  profileUnsubscribe = onSnapshot(managerDocRef, (snapshot) => {
                    if (snapshot.exists()) {
                      const data = snapshot.data();
                      const isAdminUser =
                        data.admin === true ||
                        data.email === "bibiniitech@gmail.com";
                      setRawProfile({ ...data, isAdmin: isAdminUser } as UserProfile);
                    }
                  });
                }
              }
            }
          }
        } catch (error) {
          console.error("Error fetching user session metadata:", error);
        }
      } else {
        setRawProfile(null);
        setActiveFarmUid(null);
        setIsStaff(false);
      }
      setLoading(false);
    });

    return () => {
      unsubscribeAuth();
      if (profileUnsubscribe) {
        profileUnsubscribe();
      }
    };
  }, []);

  const isPaidPremium =
    rawProfile?.isPremium === true ||
    (rawProfile as any)?.admin === true ||
    rawProfile?.isAdmin === true ||
    rawProfile?.email === "bibiniitech@gmail.com";

  const effectiveIsPremium = isPaidPremium || isPassActive;

  const userProfile: UserProfile | null = rawProfile
    ? {
        ...rawProfile,
        isPremium: effectiveIsPremium,
      }
    : null;

  return (
    <AuthContext.Provider
      value={{
        user,
        userProfile,
        activeFarmUid,
        isStaff,
        loading,
        isPassActive,
        passTimeRemaining,
        isPaidPremium,
        activate3HourPass,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
