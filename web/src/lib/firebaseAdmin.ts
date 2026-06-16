import { initializeApp, getApps, cert } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

if (!getApps().length) {
  try {
    const projectId = process.env.FIREBASE_PROJECT_ID || process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID;
    const clientEmail = process.env.FIREBASE_CLIENT_EMAIL;
    const privateKey = process.env.FIREBASE_PRIVATE_KEY
      ?.replace(/\\n/g, "\n")
      ?.replace(/^"|"$/g, "")
      ?.replace(/^'|'$/g, "")
      ?.trim();

    const hasValidCert = 
      projectId && 
      clientEmail && 
      privateKey && 
      privateKey.includes("-----BEGIN PRIVATE KEY-----");

    if (hasValidCert) {
      initializeApp({
        credential: cert({
          projectId,
          clientEmail,
          privateKey,
        }),
      });
      console.log("Firebase Admin SDK initialized successfully with service account.");
    } else {
      console.warn(
        "Firebase Admin credentials missing or invalid. Initializing fallback app for build/local environments."
      );
      initializeApp({
        projectId: projectId || "the-pig-farmer-78728"
      });
    }
  } catch (error) {
    console.error("Firebase Admin initialization error:", error);
  }
}

export const dbAdmin = getFirestore();
