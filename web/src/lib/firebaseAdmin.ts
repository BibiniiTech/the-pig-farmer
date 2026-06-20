import { initializeApp, getApps, cert } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

// True when Next.js is running static page-data collection during `next build`.
// We still need to initialise the app (so getFirestore() works for API routes),
// but we suppress the noisy credential warning since no real request runs.
const isBuildPhase = process.env.NEXT_PHASE === "phase-production-build";

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
      if (!isBuildPhase) {
        console.debug("Firebase Admin SDK initialized with service account.");
      }
    } else {
      // Initialise with project ID only (works for local dev / build phase).
      // Only log in dev — this is expected and not an error.
      if (!isBuildPhase) {
        console.debug(
          "Firebase Admin: no service account credentials found, using project-id-only fallback."
        );
      }
      initializeApp({
        projectId: projectId || "the-pig-farmer-78728"
      });
    }
  } catch (error) {
    console.error("Firebase Admin initialization error:", error);
  }
}

export const dbAdmin = getFirestore();
