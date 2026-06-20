"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { signInWithEmailAndPassword, signInWithPopup, GoogleAuthProvider, createUserWithEmailAndPassword, sendPasswordResetEmail } from "firebase/auth";
import { doc, setDoc } from "firebase/firestore";
import { auth, db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { getCurrencyByCountry } from "@/lib/currencyUtils";
import { useTranslations } from "next-intl";

export default function LoginPage() {
  const t = useTranslations("Login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isSignUp, setIsSignUp] = useState(false);
  
  // Sign Up fields
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [farmName, setFarmName] = useState("");
  const [country, setCountry] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const router = useRouter();
  const { user, loading: authLoading } = useAuth();

  useEffect(() => {
    if (!authLoading && user) {
      router.push("/dashboard");
    }
  }, [user, authLoading, router]);

  const handleAuthSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    setLoading(true);

    try {
      if (isSignUp) {
        if (!firstName.trim() || !lastName.trim() || !farmName.trim() || !country.trim()) {
          setError(t("fillAllFields"));
          setLoading(false);
          return;
        }
        if (password !== confirmPassword) {
          setError(t("passwordsDoNotMatch"));
          setLoading(false);
          return;
        }
        if (password.length < 6) {
          setError(t("passwordTooShort"));
          setLoading(false);
          return;
        }

        // 1. Create user in Firebase Auth
        const credential = await createUserWithEmailAndPassword(auth, email, password);
        const userId = credential.user.uid;

        // 2. Save profile to Firestore
        const currency = getCurrencyByCountry(country);
        await setDoc(doc(db, "users", userId), {
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          farmName: farmName.trim(),
          country: country.trim(),
          email: email.trim().toLowerCase(),
          isPremium: false,
          isAdmin: false,
          isKofisPerson: false,
          createdAt: new Date(),
          settings: {
            selectedCurrency: currency.code,
            currencySymbol: currency.symbol,
            weaningDays: "56",
            farrowingDays: "114",
            ironDay1: "3",
            ironDay2: "10",
            autoClassifyBarrows: true,
            autoClassifySows: true,
            giltAgeThresholdWeeks: "26"
          }
        });

        router.push("/dashboard");
      } else {
        await signInWithEmailAndPassword(auth, email, password);
        router.push("/dashboard");
      }
    } catch (err: any) {
      console.error(err);
      setError(err.message || t("authFailed"));
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    setError(null);
    setSuccess(null);
    setLoading(true);
    const provider = new GoogleAuthProvider();
    try {
      await signInWithPopup(auth, provider);
      router.push("/dashboard");
    } catch (err: any) {
      console.error(err);
      setError(err.message || t("googleFailed"));
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = async () => {
    setError(null);
    setSuccess(null);
    if (!email.trim()) {
      setError(t("enterEmailReset"));
      return;
    }
    setLoading(true);
    try {
      await sendPasswordResetEmail(auth, email.trim());
      setSuccess(t("resetSent"));
    } catch (err: any) {
      console.error(err);
      setError(err.message || t("resetFailed"));
    } finally {
      setLoading(false);
    }
  };

  if (authLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-zinc-50 text-zinc-900">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-zinc-50 px-4 py-12 sm:px-6 lg:px-8">
      {/* Background glow effects */}
      <div className="absolute top-1/4 left-1/4 -z-10 h-96 w-96 rounded-full bg-emerald-500/10 blur-[100px]" />
      <div className="absolute bottom-1/4 right-1/4 -z-10 h-96 w-96 rounded-full bg-violet-600/10 blur-[100px]" />

      {/* Watermark Logo Background */}
      <div className="fixed inset-0 z-0 flex items-center justify-center opacity-[0.22] pointer-events-none select-none">
        <img
          src="/app_logo.png"
          alt="Watermark Background Logo"
          className="w-full max-w-[1300px] max-h-[90vh] object-contain"
        />
      </div>

      <div className="relative z-10 w-full max-w-md space-y-8 rounded-2xl border border-zinc-200 bg-white/85 p-8 shadow-2xl backdrop-blur-md">
        <div className="text-center flex flex-col items-center">
          <Link href="/" className="flex flex-col items-center hover:opacity-80 transition-opacity cursor-pointer">
            <img
              src="/app_logo.png"
              alt="SmartSwine Logo"
              className="h-16 w-16 object-contain rounded-xl shadow-md border border-zinc-200/50 bg-white p-1 mb-4"
            />
            <h1 id="login-title" className="mt-2 text-3xl font-extrabold tracking-tight bg-gradient-to-r from-emerald-950 via-emerald-800 to-green-600 bg-clip-text text-transparent">
              {t("title")}
            </h1>
          </Link>
          <p className="mt-2 text-sm text-zinc-600">
            {isSignUp ? t("createAccount") : t("signInTitle")}
          </p>
        </div>

        {error && (
          <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {success && (
          <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800">
            {success}
          </div>
        )}

        <form className="mt-6 space-y-4" onSubmit={handleAuthSubmit}>
          <div className="space-y-3 rounded-md">
            {isSignUp && (
              <>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label htmlFor="first-name" className="sr-only">
                      {t("firstName")}
                    </label>
                    <input
                      id="first-name"
                      name="firstName"
                      type="text"
                      required
                      value={firstName}
                      onChange={(e) => setFirstName(e.target.value)}
                      className="relative block w-full rounded-lg border border-zinc-200 bg-white px-3 py-3 text-zinc-900 placeholder-zinc-400 focus:z-10 focus:border-emerald-600 focus:outline-none focus:ring-1 focus:ring-emerald-600 sm:text-sm shadow-sm"
                      placeholder={t("firstName")}
                    />
                  </div>
                  <div>
                    <label htmlFor="last-name" className="sr-only">
                      {t("lastName")}
                    </label>
                    <input
                      id="last-name"
                      name="lastName"
                      type="text"
                      required
                      value={lastName}
                      onChange={(e) => setLastName(e.target.value)}
                      className="relative block w-full rounded-lg border border-zinc-200 bg-white px-3 py-3 text-zinc-900 placeholder-zinc-400 focus:z-10 focus:border-emerald-600 focus:outline-none focus:ring-1 focus:ring-emerald-600 sm:text-sm shadow-sm"
                      placeholder={t("lastName")}
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="farm-name" className="sr-only">
                    {t("farmName")}
                  </label>
                  <input
                    id="farm-name"
                    name="farmName"
                    type="text"
                    required
                    value={farmName}
                    onChange={(e) => setFarmName(e.target.value)}
                    className="relative block w-full rounded-lg border border-zinc-200 bg-white px-3 py-3 text-zinc-900 placeholder-zinc-400 focus:z-10 focus:border-emerald-600 focus:outline-none focus:ring-1 focus:ring-emerald-600 sm:text-sm shadow-sm"
                    placeholder={t("farmName")}
                  />
                </div>

                <div>
                  <label htmlFor="country" className="sr-only">
                    {t("country")}
                  </label>
                  <input
                    id="country"
                    name="country"
                    type="text"
                    required
                    value={country}
                    onChange={(e) => setCountry(e.target.value)}
                    className="relative block w-full rounded-lg border border-zinc-200 bg-white px-3 py-3 text-zinc-900 placeholder-zinc-400 focus:z-10 focus:border-emerald-600 focus:outline-none focus:ring-1 focus:ring-emerald-600 sm:text-sm shadow-sm"
                    placeholder={t("country")}
                  />
                </div>
              </>
            )}

            <div>
              <label htmlFor="email-address" className="sr-only">
                {t("email")}
              </label>
              <input
                id="email-address"
                name="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="relative block w-full rounded-lg border border-zinc-200 bg-white px-3 py-3 text-zinc-900 placeholder-zinc-400 focus:z-10 focus:border-emerald-600 focus:outline-none focus:ring-1 focus:ring-emerald-600 sm:text-sm shadow-sm"
                placeholder={t("email")}
              />
            </div>
            
            <div>
              <label htmlFor="password" className="sr-only">
                {t("password")}
              </label>
              <input
                id="password"
                name="password"
                type="password"
                autoComplete="current-password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="relative block w-full rounded-lg border border-zinc-200 bg-white px-3 py-3 text-zinc-900 placeholder-zinc-400 focus:z-10 focus:border-emerald-600 focus:outline-none focus:ring-1 focus:ring-emerald-600 sm:text-sm shadow-sm"
                placeholder={t("password")}
              />
            </div>

            {isSignUp && (
              <div>
                <label htmlFor="confirm-password" className="sr-only">
                  {t("confirmPassword")}
                </label>
                <input
                  id="confirm-password"
                  name="confirmPassword"
                  type="password"
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="relative block w-full rounded-lg border border-zinc-200 bg-white px-3 py-3 text-zinc-900 placeholder-zinc-400 focus:z-10 focus:border-emerald-600 focus:outline-none focus:ring-1 focus:ring-emerald-600 sm:text-sm shadow-sm"
                  placeholder={t("confirmPassword")}
                />
              </div>
            )}
          </div>

          <div className="pt-2 flex flex-col gap-3">
            <button
              id="submit-login"
              type="submit"
              disabled={loading}
              className="group relative flex w-full justify-center rounded-lg bg-gradient-to-r from-emerald-600 to-emerald-700 py-3 px-4 text-sm font-semibold text-white shadow-md hover:from-emerald-700 hover:to-emerald-800 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:ring-offset-2 focus:ring-offset-white disabled:opacity-50 transition-all duration-200"
            >
              {loading ? t("processing") : isSignUp ? t("signUp") : t("signIn")}
            </button>

            {/* Forgot password link option */}
            {!isSignUp && (
              <div className="text-right text-xs">
                <button
                  type="button"
                  onClick={handleForgotPassword}
                  className="font-semibold text-zinc-500 hover:text-emerald-800 transition"
                >
                  {t("forgotPassword")}
                </button>
              </div>
            )}
          </div>
        </form>

        {/* Toggles between Sign In and Sign Up styled as a button */}
        <div className="mt-4">
          <button
            onClick={() => {
              setIsSignUp(!isSignUp);
              setError(null);
              setSuccess(null);
            }}
            className="w-full text-center rounded-lg border border-emerald-500/20 bg-emerald-500/5 hover:bg-emerald-500/10 py-2.5 text-sm font-semibold text-emerald-800 transition-all duration-200"
          >
            {isSignUp ? t("alreadyHaveAccount") : t("newHere")}
          </button>
        </div>

        <div className="relative mt-6">
          <div className="absolute inset-0 flex items-center" aria-hidden="true">
            <div className="w-full border-t border-zinc-200" />
          </div>
          <div className="relative flex justify-center text-sm">
            <span className="bg-white/80 px-2 text-zinc-500 backdrop-blur-md">{t("orContinueWith")}</span>
          </div>
        </div>

        <div className="mt-6">
          <button
            id="google-login"
            onClick={handleGoogleLogin}
            disabled={loading}
            className="flex w-full items-center justify-center gap-3 rounded-lg border border-zinc-200 bg-white py-3 px-4 text-sm font-medium text-zinc-700 shadow-sm hover:bg-zinc-50 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:ring-offset-2 focus:ring-offset-white transition-all duration-200"
          >
            <svg className="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                fill="#4285F4"
              />
              <path
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                fill="#34A853"
              />
              <path
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                fill="#FBBC05"
              />
              <path
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                fill="#EA4335"
              />
            </svg>
            {t("google")}
          </button>
        </div>
      </div>
    </main>
  );
}
