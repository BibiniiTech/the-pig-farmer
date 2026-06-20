"use client";

import React, { createContext, useContext, useEffect, useState } from "react";
import { onAuthStateChanged, User } from "firebase/auth";
import { doc, getDoc, collection, getDocs, query, limit, writeBatch, onSnapshot } from "firebase/firestore";
import { auth, db } from "@/lib/firebase";
import { defaultIngredients } from "@/lib/defaultIngredients";

interface UserProfile {
  firstName: string;
  lastName: string;
  farmName: string;
  country: string;
  email: string;
  isPremium: boolean;
  isAdmin: boolean;
  isKofisPerson: boolean;
  subscriptionSource?: string;
  appLanguage?: string;
  settings?: {
    weaningDays?: string;
    farrowingDays?: string;
    ironDay1?: string;
    ironDay2?: string;
    autoClassifyBarrows?: boolean;
    autoClassifySows?: boolean;
    notificationsEnabled?: boolean;
    selectedCurrency?: string;
    currencySymbol?: string;
    giltAgeThresholdWeeks?: string;
    porkerUseAge?: boolean;
    porkerStarterAge?: string;
    porkerGrowerAge?: string;
    porkerStarterWeight?: string;
    porkerGrowerWeight?: string;
    breederUseAge?: boolean;
    breederPigletAge?: string;
    breederWeanerAge?: string;
    breederGrowerAge?: string;
    breederPigletWeight?: string;
    breederWeanerWeight?: string;
    breederGrowerWeight?: string;
  };
}

interface AuthContextType {
  user: User | null;
  userProfile: UserProfile | null;
  activeFarmUid: string | null;
  isStaff: boolean;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  userProfile: null,
  activeFarmUid: null,
  isStaff: false,
  loading: true,
});

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [userProfile, setUserProfile] = useState<UserProfile | null>(null);
  const [activeFarmUid, setActiveFarmUid] = useState<string | null>(null);
  const [isStaff, setIsStaff] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);

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
            profileUnsubscribe = onSnapshot(userDocRef, (snapshot) => {
              if (snapshot.exists()) {
                const data = snapshot.data();
                // Firestore stores the admin flag as "admin" (not "isAdmin")
                // Mirror the email fallback from the Android app
                const isAdminUser =
                  data.admin === true ||
                  data.email === "bibiniitech@gmail.com";
                setUserProfile({ ...data, isAdmin: isAdminUser } as UserProfile);
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
                      setUserProfile({ ...data, isAdmin: isAdminUser } as UserProfile);
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
        setUserProfile(null);
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

  return (
    <AuthContext.Provider value={{ user, userProfile, activeFarmUid, isStaff, loading }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
