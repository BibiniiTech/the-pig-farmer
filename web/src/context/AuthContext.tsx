"use client";

import React, { createContext, useContext, useEffect, useState, useRef } from "react";
import { onAuthStateChanged, User } from "firebase/auth";
import { doc, getDoc, collection, getDocs, query, limit, writeBatch, onSnapshot, updateDoc, setDoc, deleteDoc } from "firebase/firestore";
import { auth, db } from "@/lib/firebase";
import { defaultIngredients } from "@/lib/defaultIngredients";
import { getCurrencyByCountry } from "@/lib/currencyUtils";
import { UserProfile } from "@/lib/types";

interface AuthContextType {
  user: User | null;
  userProfile: UserProfile | null;
  activeFarmUid: string | null;
  isStaff: boolean;
  isStaffDenied: boolean;
  isFinancialsRestricted: boolean;
  loading: boolean;
  isProfileComplete: boolean;
  isPassActive: boolean;
  passTimeRemaining: string;
  isPaidPremium: boolean;
  activate3HourPass: () => Promise<void>;
  createGoogleUserProfile: (profileData: {
    firstName: string;
    lastName: string;
    farmName: string;
    country: string;
  }) => Promise<void>;
  detachFromStaffRegistryAndCreateFarm: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  userProfile: null,
  activeFarmUid: null,
  isStaff: false,
  isStaffDenied: false,
  isFinancialsRestricted: false,
  loading: true,
  isProfileComplete: true,
  isPassActive: false,
  passTimeRemaining: "",
  isPaidPremium: false,
  activate3HourPass: async () => {},
  createGoogleUserProfile: async () => {},
  detachFromStaffRegistryAndCreateFarm: async () => {},
});

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [rawProfile, setRawProfile] = useState<UserProfile | null>(null);
  const [activeFarmUid, setActiveFarmUid] = useState<string | null>(null);
  const [isStaff, setIsStaff] = useState<boolean>(false);
  const [isStaffDenied, setIsStaffDenied] = useState<boolean>(false);
  const [isFinancialsRestricted, setIsFinancialsRestricted] = useState<boolean>(false);
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
    let staffUnsubscribe: (() => void) | null = null;

    const unsubscribeAuth = onAuthStateChanged(auth, async (currentUser) => {
      // Clean up previous profile listener
      if (profileUnsubscribe) {
        profileUnsubscribe();
        profileUnsubscribe = null;
      }
      if (staffUnsubscribe) {
        staffUnsubscribe();
        staffUnsubscribe = null;
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
            setIsStaffDenied(false);
            setIsFinancialsRestricted(false);

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
                  // Verify staff record in manager's staff sub-collection (matching Android AuthViewModel)
                  const staffCollRef = collection(db, "users", managerUid, "staff");
                  const staffQuery = query(staffCollRef, limit(100));
                  
                  staffUnsubscribe = onSnapshot(staffQuery, (staffSnap) => {
                    const matchedStaff = staffSnap.docs
                      .map((d) => ({ id: d.id, ...d.data() } as any))
                      .find((s) => s.email?.trim().toLowerCase() === email);

                    if (!matchedStaff || matchedStaff.allowAppAccess !== true || matchedStaff.status === "Archived") {
                      setIsStaff(true);
                      setIsStaffDenied(true);
                      setIsFinancialsRestricted(true);
                      setActiveFarmUid(null);
                      return;
                    }

                    // Staff is authorized
                    setIsStaff(true);
                    setIsStaffDenied(false);
                    setActiveFarmUid(managerUid);

                    // Check role restrictions (matching Android role check)
                    const role = (matchedStaff.role || "").toLowerCase();
                    const isRestricted =
                      role.includes("hand") ||
                      role.includes("labor") ||
                      role.includes("labour") ||
                      role.includes("worker") ||
                      role.includes("herdsman") ||
                      role.includes("attendant");
                    setIsFinancialsRestricted(isRestricted);
                  });

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
                } else {
                  setIsStaff(false);
                  setIsStaffDenied(false);
                  setIsFinancialsRestricted(false);
                }
              } else {
                setIsStaff(false);
                setIsStaffDenied(false);
                setIsFinancialsRestricted(false);
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
        setIsStaffDenied(false);
        setIsFinancialsRestricted(false);
      }
      setLoading(false);
    });

    return () => {
      unsubscribeAuth();
      if (profileUnsubscribe) {
        profileUnsubscribe();
      }
      if (staffUnsubscribe) {
        staffUnsubscribe();
      }
    };
  }, []);

  const isPaidPremium =
    rawProfile?.isPremium === true ||
    (rawProfile as any)?.admin === true ||
    rawProfile?.isAdmin === true ||
    rawProfile?.email === "bibiniitech@gmail.com";

  // The 3-hour pass is strictly device-local and does NOT grant full account premium status
  const userProfile: UserProfile | null = rawProfile
    ? {
        ...rawProfile,
        isPremium: isPaidPremium,
      }
    : null;

  const isProfileComplete = Boolean(
    isStaff ||
    (rawProfile && rawProfile.farmName?.trim() && rawProfile.country?.trim())
  );

  const createGoogleUserProfile = async ({
    firstName,
    lastName,
    farmName,
    country,
  }: {
    firstName: string;
    lastName: string;
    farmName: string;
    country: string;
  }) => {
    if (!user) throw new Error("No authenticated user");
    const currency = getCurrencyByCountry(country);
    const newProfile: UserProfile = {
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      farmName: farmName.trim(),
      country: country.trim(),
      email: user.email?.trim().toLowerCase() || "",
      isPremium: false,
      isAdmin: user.email?.trim().toLowerCase() === "bibiniitech@gmail.com",
      isKofisPerson: false,
      settings: {
        selectedCurrency: currency.code,
        currencySymbol: currency.symbol,
        weaningDays: "56",
        farrowingDays: "114",
        ironDay1: "3",
        ironDay2: "10",
        autoClassifyBarrows: true,
        autoClassifySows: true,
        giltAgeThresholdWeeks: "26",
        porkerUseAge: true,
        porkerStarterAge: "16",
        porkerGrowerAge: "24",
        porkerStarterWeight: "25",
        porkerGrowerWeight: "60",
        breederUseAge: true,
        breederPigletAge: "8",
        breederWeanerAge: "16",
        breederGrowerAge: "24",
        breederPigletWeight: "10",
        breederWeanerWeight: "25",
        breederGrowerWeight: "60",
        notificationsEnabled: true,
      },
    };

    const userDocRef = doc(db, "users", user.uid);
    await setDoc(userDocRef, {
      ...newProfile,
      createdAt: new Date(),
    });

    setActiveFarmUid(user.uid);
    setIsStaff(false);
    setRawProfile(newProfile);

    // Seed default ingredients if empty
    try {
      const ingredientsCollRef = collection(db, "users", user.uid, "feed_ingredients");
      const ingredientsSnap = await getDocs(query(ingredientsCollRef, limit(1)));
      if (ingredientsSnap.empty) {
        const batch = writeBatch(db);
        defaultIngredients.forEach((ing) => {
          const newDocRef = doc(ingredientsCollRef);
          batch.set(newDocRef, {
            ...ing,
            id: newDocRef.id,
          });
        });
        await batch.commit();
      }
    } catch (seedErr) {
      console.error("Error seeding default ingredients:", seedErr);
    }
  };

  const detachFromStaffRegistryAndCreateFarm = async () => {
    if (!user) throw new Error("No authenticated user");
    const email = user.email?.trim().toLowerCase();
    if (email) {
      try {
        await deleteDoc(doc(db, "staff_registry", email));
      } catch (e) {
        console.warn("Could not delete from staff_registry:", e);
      }
    }

    const currency = getCurrencyByCountry("Ghana");
    const newProfile: UserProfile = {
      firstName: user.displayName?.split(" ")[0] || "Farmer",
      lastName: user.displayName?.split(" ").slice(1).join(" ") || "",
      farmName: "My Swine Farm",
      country: "Ghana",
      email: email || "",
      isPremium: false,
      isAdmin: email === "bibiniitech@gmail.com",
      isKofisPerson: false,
      settings: {
        selectedCurrency: currency.code,
        currencySymbol: currency.symbol,
        weaningDays: "56",
        farrowingDays: "114",
        ironDay1: "3",
        ironDay2: "10",
        autoClassifyBarrows: true,
        autoClassifySows: true,
        giltAgeThresholdWeeks: "26",
        porkerUseAge: true,
        porkerStarterAge: "16",
        porkerGrowerAge: "24",
        porkerStarterWeight: "25",
        porkerGrowerWeight: "60",
        breederUseAge: true,
        breederPigletAge: "8",
        breederWeanerAge: "16",
        breederGrowerAge: "24",
        breederPigletWeight: "10",
        breederWeanerWeight: "25",
        breederGrowerWeight: "60",
        notificationsEnabled: true,
      },
    };

    const userDocRef = doc(db, "users", user.uid);
    await setDoc(userDocRef, {
      ...newProfile,
      createdAt: new Date(),
    });

    setActiveFarmUid(user.uid);
    setIsStaff(false);
    setIsStaffDenied(false);
    setIsFinancialsRestricted(false);
    setRawProfile(newProfile);

    // Seed default ingredients if empty
    try {
      const ingredientsCollRef = collection(db, "users", user.uid, "feed_ingredients");
      const ingredientsSnap = await getDocs(query(ingredientsCollRef, limit(1)));
      if (ingredientsSnap.empty) {
        const batch = writeBatch(db);
        defaultIngredients.forEach((ing) => {
          const newDocRef = doc(ingredientsCollRef);
          batch.set(newDocRef, {
            ...ing,
            id: newDocRef.id,
          });
        });
        await batch.commit();
      }
    } catch (seedErr) {
      console.error("Error seeding default ingredients:", seedErr);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        userProfile,
        activeFarmUid,
        isStaff,
        isStaffDenied,
        isFinancialsRestricted,
        loading,
        isProfileComplete,
        isPassActive,
        passTimeRemaining,
        isPaidPremium,
        activate3HourPass,
        createGoogleUserProfile,
        detachFromStaffRegistryAndCreateFarm,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
