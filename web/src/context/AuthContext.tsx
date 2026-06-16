"use client";

import React, { createContext, useContext, useEffect, useState } from "react";
import { onAuthStateChanged, User } from "firebase/auth";
import { doc, getDoc } from "firebase/firestore";
import { auth, db } from "@/lib/firebase";

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
    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      setUser(currentUser);
      if (currentUser) {
        try {
          // 1. Try to read Owner Profile from users/{uid}
          const userDocRef = doc(db, "users", currentUser.uid);
          const userDocSnap = await getDoc(userDocRef);

          if (userDocSnap.exists()) {
            const profile = userDocSnap.data() as UserProfile;
            setUserProfile(profile);
            setActiveFarmUid(currentUser.uid);
            setIsStaff(false);
          } else {
            // 2. Check if user is a Staff member
            const email = currentUser.email?.trim().toLowerCase();
            if (email) {
              const registryDocRef = doc(db, "staff_registry", email);
              const registryDocSnap = await getDoc(registryDocRef);

              if (registryDocSnap.exists()) {
                const managerUid = registryDocSnap.data().managerUid;
                if (managerUid) {
                  // Fetch manager's profile to get farmName and details
                  const managerDocRef = doc(db, "users", managerUid);
                  const managerDocSnap = await getDoc(managerDocRef);

                  if (managerDocSnap.exists()) {
                    const managerProfile = managerDocSnap.data() as UserProfile;
                    setUserProfile(managerProfile);
                    setActiveFarmUid(managerUid);
                    setIsStaff(true);
                  }
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

    return () => unsubscribe();
  }, []);

  return (
    <AuthContext.Provider value={{ user, userProfile, activeFarmUid, isStaff, loading }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
