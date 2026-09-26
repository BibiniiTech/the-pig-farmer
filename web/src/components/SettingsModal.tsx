"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import { auth, db } from "@/lib/firebase";
import { doc, updateDoc, deleteDoc, collection, getDocs, setDoc, writeBatch } from "firebase/firestore";
import { deleteUser } from "firebase/auth";
import { useTranslations } from "next-intl";
import { SUPPORTED_COUNTRIES, getNormalizedCountryName, getCurrencyByCountry } from "@/lib/currencyUtils";

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const LANGUAGES = [
  { code: "en", displayName: "English", flag: "🇺🇸" },
  { code: "fr", displayName: "Français", flag: "🇫🇷" },
  { code: "zh", displayName: "中文", flag: "🇨🇳" },
  { code: "es", displayName: "Español", flag: "🇲🇽" },
  { code: "es-es", displayName: "Español (Castellano)", flag: "🇪🇸" },
  { code: "es-do", displayName: "Español (Dominicano)", flag: "🇩🇴" },
  { code: "de", displayName: "Deutsch", flag: "🇩🇪" },
  { code: "ja", displayName: "日本語", flag: "🇯🇵" },
  { code: "pt", displayName: "Português", flag: "🇵🇹" },
  { code: "tl", displayName: "Filipino", flag: "🇵🇭" },
  { code: "vi", displayName: "Tiếng Việt", flag: "🇻🇳" },
  { code: "th", displayName: "ไทย", flag: "🇹🇭" },
  { code: "id", displayName: "Bahasa Indonesia", flag: "🇮🇩" },
  { code: "hi", displayName: "हिन्दी", flag: "🇮🇳" },
  { code: "sw", displayName: "Kiswahili", flag: "🇰🇪" },
  { code: "lg", displayName: "Oluganda", flag: "🇺🇬" },
  { code: "rw", displayName: "Ikinyarwanda", flag: "🇷🇼" },
  { code: "ht", displayName: "Kreyòl Ayisyen", flag: "🇭🇹" },
  { code: "my", displayName: "မြန်မာ", flag: "🇲🇲" },
  { code: "tpi", displayName: "Tok Pisin", flag: "🇵🇬" },
  { code: "zgh", displayName: "Tamaziɣt", flag: "🇲🇦" },
  { code: "af", displayName: "Afrikaans", flag: "🇿🇦" },
];

export default function SettingsModal({ isOpen, onClose }: SettingsModalProps) {
  const t = useTranslations("Settings");
  const { user, userProfile, activeFarmUid } = useAuth();
  const { isMobile } = useDevice();
  const [activeTab, setActiveTab] = useState("profile");
  const [isSaving, setIsSaving] = useState(false);

  // Theme & Language (Parity with Android Settings)
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [selectedLang, setSelectedLang] = useState("en");
  const [isLangDropdownOpen, setIsLangDropdownOpen] = useState(false);

  useEffect(() => {
    if (typeof window !== "undefined") {
      setIsDarkMode(
        document.documentElement.classList.contains("dark") ||
        localStorage.getItem("theme") === "dark"
      );
      const match = document.cookie.match(/(?:^|;\s*)NEXT_LOCALE=([^;]*)/);
      if (match) {
        setSelectedLang(match[1]);
      } else if (userProfile?.appLanguage) {
        setSelectedLang(userProfile.appLanguage);
      }
    }
  }, [userProfile, isOpen]);

  const toggleDarkMode = () => {
    const nextDark = !isDarkMode;
    setIsDarkMode(nextDark);
    if (nextDark) {
      document.documentElement.classList.add("dark");
      localStorage.setItem("theme", "dark");
    } else {
      document.documentElement.classList.remove("dark");
      localStorage.setItem("theme", "light");
    }
  };

  const handleLanguageChange = async (langCode: string) => {
    setSelectedLang(langCode);
    setIsLangDropdownOpen(false);
    document.cookie = `NEXT_LOCALE=${langCode}; path=/; max-age=31536000`;
    if (user) {
      try {
        const userDocRef = doc(db, "users", user.uid);
        await setDoc(userDocRef, { appLanguage: langCode }, { merge: true });
      } catch (err) {
        console.error("Failed to update language:", err);
      }
    }
    window.location.reload();
  };

  // Profile States
  const [farmName, setFarmName] = useState(userProfile?.farmName || "");
  const [country, setCountry] = useState(userProfile?.country || "");
  const [farmLogo, setFarmLogo] = useState(userProfile?.farmLogo || "");

  // Cycle States
  const [weaningDays, setWeaningDays] = useState(userProfile?.settings?.weaningDays || "56");
  const [farrowingDays, setFarrowingDays] = useState(userProfile?.settings?.farrowingDays || "114");
  const [ironDay1, setIronDay1] = useState(userProfile?.settings?.ironDay1 || "3");
  const [ironDay2, setIronDay2] = useState(userProfile?.settings?.ironDay2 || "10");

  // Status States
  const [autoClassifyBarrows, setAutoClassifyBarrows] = useState(userProfile?.settings?.autoClassifyBarrows ?? true);
  const [autoClassifySows, setAutoClassifySows] = useState(userProfile?.settings?.autoClassifySows ?? true);
  const [giltAgeThreshold, setGiltAgeThreshold] = useState(userProfile?.settings?.giltAgeThresholdWeeks || "26");

  // Porker Classification States
  const [porkerUseAge, setPorkerUseAge] = useState(userProfile?.settings?.porkerUseAge ?? true);
  const [porkerStarterAge, setPorkerStarterAge] = useState(userProfile?.settings?.porkerStarterAge || "16");
  const [porkerGrowerAge, setPorkerGrowerAge] = useState(userProfile?.settings?.porkerGrowerAge || "24");
  const [porkerStarterWeight, setPorkerStarterWeight] = useState(userProfile?.settings?.porkerStarterWeight || "25");
  const [porkerGrowerWeight, setPorkerGrowerWeight] = useState(userProfile?.settings?.porkerGrowerWeight || "60");

  // Breeder Classification States
  const [breederUseAge, setBreederUseAge] = useState(userProfile?.settings?.breederUseAge ?? true);
  const [breederPigletAge, setBreederPigletAge] = useState(userProfile?.settings?.breederPigletAge || "8");
  const [breederWeanerAge, setBreederWeanerAge] = useState(userProfile?.settings?.breederWeanerAge || "16");
  const [breederGrowerAge, setBreederGrowerAge] = useState(userProfile?.settings?.breederGrowerAge || "24");
  const [breederPigletWeight, setBreederPigletWeight] = useState(userProfile?.settings?.breederPigletWeight || "10");
  const [breederWeanerWeight, setBreederWeanerWeight] = useState(userProfile?.settings?.breederWeanerWeight || "25");
  const [breederGrowerWeight, setBreederGrowerWeight] = useState(userProfile?.settings?.breederGrowerWeight || "60");

  // Currency States
  const [selectedCurrency, setSelectedCurrency] = useState(userProfile?.settings?.selectedCurrency || "USD");
  const [currencySymbol, setCurrencySymbol] = useState(userProfile?.settings?.currencySymbol || "$");

  useEffect(() => {
    if (userProfile) {
      setFarmName(userProfile.farmName || "");
      setCountry(userProfile.country || "");
      setFarmLogo(userProfile.farmLogo || "");
      if (userProfile.settings) {
        setWeaningDays(userProfile.settings.weaningDays || "56");
        setFarrowingDays(userProfile.settings.farrowingDays || "114");
        setIronDay1(userProfile.settings.ironDay1 || "3");
        setIronDay2(userProfile.settings.ironDay2 || "10");
        setAutoClassifyBarrows(userProfile.settings.autoClassifyBarrows ?? true);
        setAutoClassifySows(userProfile.settings.autoClassifySows ?? true);
        setGiltAgeThreshold(userProfile.settings.giltAgeThresholdWeeks || "26");
        setPorkerUseAge(userProfile.settings.porkerUseAge ?? true);
        setPorkerStarterAge(userProfile.settings.porkerStarterAge || "16");
        setPorkerGrowerAge(userProfile.settings.porkerGrowerAge || "24");
        setPorkerStarterWeight(userProfile.settings.porkerStarterWeight || "25");
        setPorkerGrowerWeight(userProfile.settings.porkerGrowerWeight || "60");
        setBreederUseAge(userProfile.settings.breederUseAge ?? true);
        setBreederPigletAge(userProfile.settings.breederPigletAge || "8");
        setBreederWeanerAge(userProfile.settings.breederWeanerAge || "16");
        setBreederGrowerAge(userProfile.settings.breederGrowerAge || "24");
        setBreederPigletWeight(userProfile.settings.breederPigletWeight || "10");
        setBreederWeanerWeight(userProfile.settings.breederWeanerWeight || "25");
        setBreederGrowerWeight(userProfile.settings.breederGrowerWeight || "60");
        setSelectedCurrency(userProfile.settings.selectedCurrency || "USD");
        setCurrencySymbol(userProfile.settings.currencySymbol || "$");
      }
    }
  }, [userProfile, isOpen]);

  if (!isOpen) return null;

  const handleCountryChange = (selectedCountry: string) => {
    setCountry(selectedCountry);
    const currency = getCurrencyByCountry(selectedCountry);
    setSelectedCurrency(currency.code);
    setCurrencySymbol(currency.symbol);
  };

  const handleLogoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      alert("Image size must be less than 5MB");
      return;
    }

    const reader = new FileReader();
    reader.onload = (event) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement("canvas");
        const maxDim = 256;
        let width = img.width;
        let height = img.height;

        if (width > height) {
          if (width > maxDim) {
            height = Math.round((height * maxDim) / width);
            width = maxDim;
          }
        } else {
          if (height > maxDim) {
            width = Math.round((width * maxDim) / height);
            height = maxDim;
          }
        }

        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext("2d");
        if (ctx) {
          ctx.drawImage(img, 0, 0, width, height);
          const isPng = file.type === "image/png" || file.name.toLowerCase().endsWith(".png");
          const compressedDataUrl = isPng
            ? canvas.toDataURL("image/png")
            : canvas.toDataURL("image/jpeg", 0.85);
          setFarmLogo(compressedDataUrl);
        }
      };
      img.src = event.target?.result as string;
    };
    reader.readAsDataURL(file);
  };

  const handleSaveSettings = async () => {
    if (!activeFarmUid) return;
    setIsSaving(true);

    try {
      const userRef = doc(db, "users", activeFarmUid);

      const settingsMap = {
        ...(userProfile?.settings || {}),
        weaningDays,
        farrowingDays,
        ironDay1,
        ironDay2,
        autoClassifyBarrows,
        autoClassifySows,
        giltAgeThresholdWeeks: giltAgeThreshold,
        selectedCurrency,
        currencySymbol,
        porkerUseAge,
        porkerStarterAge,
        porkerGrowerAge,
        porkerStarterWeight,
        porkerGrowerWeight,
        breederUseAge,
        breederPigletAge,
        breederWeanerAge,
        breederGrowerAge,
        breederPigletWeight,
        breederWeanerWeight,
        breederGrowerWeight,
      };

      await updateDoc(userRef, {
        farmName,
        country,
        farmLogo: farmLogo || "",
        settings: settingsMap,
      });

      alert(t("saveSuccess"));
      onClose();
    } catch (err) {
      console.error("Error saving settings:", err);
      alert(t("saveError"));
    } finally {
      setIsSaving(false);
    }
  };

  const [showWhatsNew, setShowWhatsNew] = useState(false);

  const ALL_FARM_SUBCOLLECTIONS = [
    "pigs",
    "archived_pigs",
    "financials",
    "feed_ingredients",
    "nutritional_requirements",
    "tasks",
    "feed_inventory",
    "feed_transactions",
    "feed_inventory_transactions",
    "saved_feed_recipes",
    "farm_alerts",
    "staff",
    "salaries",
  ];

  const handleClearData = async (type: string) => {
    if (!activeFarmUid) return;
    if (!confirm(t("confirmClear", { type }))) return;

    try {
      const targets =
        type === "pigs"
          ? ["pigs", "archived_pigs"]
          : type === "feed"
          ? [
              "feed_ingredients",
              "nutritional_requirements",
              "feed_inventory",
              "feed_transactions",
              "feed_inventory_transactions",
              "saved_feed_recipes",
            ]
          : type === "staff"
          ? ["staff", "salaries"]
          : [type];

      for (const target of targets) {
        const collectionRef = collection(db, "users", activeFarmUid, target);
        const snapshot = await getDocs(collectionRef);
        const batch = writeBatch(db);
        snapshot.docs.forEach((d) => batch.delete(d.ref));
        await batch.commit();
      }
      alert(`${type} data cleared successfully.`);
    } catch (err) {
      console.error(err);
      alert("Failed to clear data.");
    }
  };

  const handleFactoryReset = async () => {
    if (!activeFarmUid) return;
    const confirmText = prompt(
      "Are you sure you want to perform a FACTORY RESET? This will permanently delete ALL herd, financial, feed, task, and staff records. Type 'RESET' to confirm:"
    );
    if (confirmText !== "RESET") return;

    try {
      for (const target of ALL_FARM_SUBCOLLECTIONS) {
        const collectionRef = collection(db, "users", activeFarmUid, target);
        const snapshot = await getDocs(collectionRef);
        const batch = writeBatch(db);
        snapshot.docs.forEach((d) => batch.delete(d.ref));
        await batch.commit();
      }
      alert("Factory reset complete. All farm records have been erased.");
      window.location.reload();
    } catch (err) {
      console.error(err);
      alert("Failed to complete factory reset.");
    }
  };

  const handleDeleteAccount = async () => {
    if (!activeFarmUid || !auth.currentUser) return;
    const confirmText = prompt(
      "Are you sure you want to permanently DELETE your SmartSwine account and all associated farm data? This cannot be undone. Type 'DELETE' to confirm:"
    );
    if (confirmText !== "DELETE") return;

    try {
      for (const target of ALL_FARM_SUBCOLLECTIONS) {
        const collectionRef = collection(db, "users", activeFarmUid, target);
        const snapshot = await getDocs(collectionRef);
        const batch = writeBatch(db);
        snapshot.docs.forEach((d) => batch.delete(d.ref));
        await batch.commit();
      }
      await deleteDoc(doc(db, "users", activeFarmUid));
      await deleteUser(auth.currentUser);
      alert("Account deleted successfully.");
      window.location.href = "/";
    } catch (err: any) {
      console.error(err);
      if (err.code === "auth/requires-recent-login") {
        alert(
          "This operation is sensitive and requires recent authentication. Please log out, log back in, and try deleting your account again."
        );
      } else {
        alert("Failed to delete account. Please try again.");
      }
    }
  };

  return (
    <div className={`fixed inset-0 z-[100] flex justify-center bg-black/60 backdrop-blur-sm p-4 ${isMobile ? 'items-center' : 'items-start pt-20'}`}>
      <div className={`bg-white border border-zinc-200 rounded-2xl w-full overflow-hidden flex flex-col shadow-2xl ${isMobile ? 'max-w-lg max-h-[90vh]' : 'max-w-6xl h-[75vh]'}`}>
        <div className="p-6 border-b border-zinc-100 flex items-center justify-between bg-zinc-50/50">
          <h2 className="text-xl font-bold text-zinc-900 flex items-center gap-2">
            <svg className="h-6 w-6 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            {t("title")}
          </h2>
          <button onClick={onClose} className="p-2 hover:bg-zinc-200 rounded-lg transition text-zinc-400">
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        <div className={`flex-1 flex overflow-hidden ${isMobile ? 'flex-col' : 'flex-row'}`}>
          {/* Sidebar - Hidden on Mobile for Linear Presentation */}
          {!isMobile && (
            <div className="w-64 border-r border-zinc-100 bg-zinc-50/30 p-4 space-y-1 overflow-y-auto no-scrollbar">
              <TabButton id="profile" label={t("editProfile")} active={activeTab} onClick={setActiveTab} />
              <TabButton id="cycles" label={t("cyclesTiming")} active={activeTab} onClick={setActiveTab} />
              <TabButton id="status" label={t("herdStatus")} active={activeTab} onClick={setActiveTab} />
              <TabButton id="data" label={t("dataManagement")} active={activeTab} onClick={setActiveTab} />
              <TabButton id="about" label={t("aboutSmartSwine")} active={activeTab} onClick={setActiveTab} />
            </div>
          )}

          {/* Content - Becomes linear on Mobile */}
          <div className={`flex-1 overflow-y-auto no-scrollbar ${isMobile ? 'p-5 space-y-12' : 'p-8 space-y-8'}`}>
            {(activeTab === "profile" || isMobile) && (
              <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
                <section>
                  <h3 className={`text-sm font-black text-zinc-400 uppercase tracking-widest mb-4 ${isMobile ? 'text-emerald-600' : ''}`}>
                    {t("editProfile")}
                  </h3>
                  <div className="space-y-4 max-w-md">
                    <div className="flex items-center gap-4 mb-6">
                       <div className="h-20 w-20 rounded-full bg-emerald-100 border-2 border-emerald-500 flex items-center justify-center text-emerald-600 text-2xl font-black shadow-inner">
                         {userProfile?.firstName?.charAt(0) || user?.email?.charAt(0).toUpperCase()}
                       </div>
                       <div>
                         <p className="font-bold text-zinc-900">{userProfile?.firstName} {userProfile?.lastName}</p>
                         <p className="text-xs text-zinc-500">{user?.email}</p>
                       </div>
                    </div>

                    {/* Farm Logo Upload Section */}
                    <div className="p-4 bg-zinc-50 border border-zinc-200/80 rounded-2xl space-y-3">
                      <div>
                        <label className="block text-xs font-bold text-zinc-700 uppercase tracking-tight">
                          {t("farmLogo")}
                        </label>
                        <p className="text-[11px] text-zinc-500 mt-0.5 font-medium">
                          {t("farmLogoDesc")}
                        </p>
                      </div>

                      <div className="flex items-center gap-4">
                        <div className="relative h-20 w-20 rounded-2xl bg-white border-2 border-dashed border-emerald-500/40 p-1 flex items-center justify-center overflow-hidden shadow-sm flex-shrink-0 group">
                          <img
                            src={farmLogo || "/app_logo.png"}
                            alt="Farm Logo Preview"
                            className="h-full w-full object-cover rounded-xl"
                            onError={(e) => {
                              (e.currentTarget as HTMLImageElement).src = "/app_logo.png";
                            }}
                          />
                        </div>

                        <div className="flex flex-col gap-2">
                          <label className="inline-flex items-center justify-center px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold cursor-pointer transition shadow-sm hover:shadow active:scale-95 select-none">
                            <span>{farmLogo ? t("changeLogo") : t("uploadLogo")}</span>
                            <input
                              type="file"
                              accept="image/*"
                              className="hidden"
                              onChange={handleLogoUpload}
                            />
                          </label>

                          {farmLogo && (
                            <button
                              type="button"
                              onClick={() => setFarmLogo("")}
                              className="px-4 py-1.5 rounded-xl border border-rose-200 text-rose-600 hover:bg-rose-50 text-xs font-semibold transition active:scale-95"
                            >
                              {t("removeLogo")}
                            </button>
                          )}
                        </div>
                      </div>
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-zinc-500 mb-1.5 uppercase">{t("farmName")}</label>
                      <input
                        type="text"
                        value={farmName}
                        onChange={(e) => setFarmName(e.target.value)}
                        className="w-full rounded-xl border border-zinc-200 bg-white px-4 py-2.5 text-sm font-semibold text-zinc-900 focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 outline-none transition"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-zinc-500 mb-1.5 uppercase">{t("country")}</label>
                      <select
                        value={getNormalizedCountryName(country)}
                        onChange={(e) => handleCountryChange(e.target.value)}
                        className="w-full rounded-xl border border-zinc-200 bg-white px-4 py-2.5 text-sm font-semibold text-zinc-900 focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 outline-none transition"
                      >
                        <option value="" disabled hidden>
                          Select Country
                        </option>
                        {SUPPORTED_COUNTRIES.map((c) => (
                          <option key={c} value={c}>
                            {c}
                          </option>
                        ))}
                        <option value="Other">Other</option>
                      </select>
                    </div>

                    {/* Theme & Language (Side-by-side pill matching Android Settings) */}
                    <div className="pt-2">
                      <label className="block text-[10px] font-black text-zinc-400 uppercase mb-2">
                        Theme & Language
                      </label>
                      <div className="grid grid-cols-2 gap-3">
                        {/* Dark theme toggle pill */}
                        <button
                          type="button"
                          onClick={toggleDarkMode}
                          className="flex items-center justify-between px-3.5 py-2.5 rounded-xl border border-zinc-200 bg-white hover:bg-zinc-50 transition shadow-2xs"
                        >
                          <div className="flex items-center gap-2">
                            {isDarkMode ? (
                              <svg className="h-4 w-4 text-amber-500" fill="currentColor" viewBox="0 0 24 24">
                                <path d="M12 3a9 9 0 109 9c0-.46-.04-.92-.1-1.36a5.389 5.389 0 01-4.4 2.26 5.403 5.403 0 01-3.14-9.8c-.44-.06-.9-.1-1.36-.1z" />
                              </svg>
                            ) : (
                              <svg className="h-4 w-4 text-amber-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                                <circle cx="12" cy="12" r="5" />
                                <path strokeLinecap="round" d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
                              </svg>
                            )}
                            <span className="font-bold text-xs text-zinc-800">
                              {isDarkMode ? "Dark" : "Light"}
                            </span>
                          </div>
                          <div className={`w-8 h-4.5 rounded-full transition-colors flex items-center p-0.5 ${isDarkMode ? "bg-emerald-500 justify-end" : "bg-zinc-300 justify-start"}`}>
                            <div className="w-3.5 h-3.5 rounded-full bg-white shadow-sm" />
                          </div>
                        </button>

                        {/* Language dropdown */}
                        <div className="relative">
                          <button
                            type="button"
                            onClick={() => setIsLangDropdownOpen(!isLangDropdownOpen)}
                            className="w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl border border-zinc-200 bg-white hover:bg-zinc-50 transition shadow-2xs"
                          >
                            <div className="flex items-center gap-2 min-w-0">
                              <span className="text-base leading-none">
                                {LANGUAGES.find((l) => l.code === selectedLang)?.flag || "🇺🇸"}
                              </span>
                              <span className="font-bold text-xs text-zinc-800 uppercase truncate">
                                {selectedLang}
                              </span>
                            </div>
                            <svg className={`h-3.5 w-3.5 text-zinc-400 transition-transform ${isLangDropdownOpen ? "rotate-180" : ""}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M19 9l-7 7-7-7" />
                            </svg>
                          </button>

                          {isLangDropdownOpen && (
                            <>
                              <div className="fixed inset-0 z-30" onClick={() => setIsLangDropdownOpen(false)} />
                              <div className="absolute right-0 mt-1 w-52 max-h-60 overflow-y-auto rounded-xl border border-zinc-200 bg-white shadow-xl z-40 py-1.5 divide-y divide-zinc-100 text-xs">
                                {LANGUAGES.map((lang) => (
                                  <button
                                    key={lang.code}
                                    type="button"
                                    onClick={() => handleLanguageChange(lang.code)}
                                    className={`w-full flex items-center justify-between px-3.5 py-2 hover:bg-emerald-50 text-left transition-colors ${
                                      selectedLang === lang.code ? "bg-emerald-50 text-emerald-600 font-bold" : "text-zinc-700"
                                    }`}
                                  >
                                    <div className="flex items-center gap-2">
                                      <span className="text-base">{lang.flag}</span>
                                      <span>{lang.displayName}</span>
                                    </div>
                                    <span className="text-[10px] uppercase font-bold text-zinc-400">
                                      {lang.code}
                                    </span>
                                  </button>
                                ))}
                              </div>
                            </>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="pt-2">
                      <div className="p-4 bg-zinc-50 border border-zinc-200/80 rounded-2xl flex items-center justify-between">
                        <div>
                          <p className="text-[10px] font-black text-zinc-400 uppercase tracking-tight">Farm Currency</p>
                          <p className="text-xs text-zinc-500 mt-0.5">Auto-selected from your country</p>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="px-3 py-1.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs font-black text-emerald-800 tracking-wide font-mono">
                            {selectedCurrency} ({currencySymbol})
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </section>
                {isMobile && <div className="border-t border-zinc-100 pt-10" />}
              </div>
            )}

            {(activeTab === "cycles" || isMobile) && (
              <div className="space-y-8 animate-in fade-in slide-in-from-right-4 duration-300">
                <section>
                  <h3 className={`text-sm font-black text-zinc-400 uppercase tracking-widest mb-4 ${isMobile ? 'text-emerald-600' : ''}`}>
                    {t("cyclesTiming")}
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-2xl">
                    <SettingInput
                      label={t("weaningThreshold")}
                      value={weaningDays}
                      onChange={setWeaningDays}
                      description={t("weaningDesc")}
                    />
                    <SettingInput
                      label={t("gestationPeriod")}
                      value={farrowingDays}
                      onChange={setFarrowingDays}
                      description={t("gestationDesc")}
                    />
                    <SettingInput
                      label={t("firstIron")}
                      value={ironDay1}
                      onChange={setIronDay1}
                      description={t("firstIronDesc")}
                    />
                    <SettingInput
                      label={t("secondIron")}
                      value={ironDay2}
                      onChange={setIronDay2}
                      description={t("secondIronDesc")}
                    />
                  </div>
                </section>
                {isMobile && <div className="border-t border-zinc-100 pt-10" />}
              </div>
            )}

            {(activeTab === "status" || isMobile) && (
              <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
                <h3 className={`text-sm font-black text-zinc-400 uppercase tracking-widest mb-4 ${isMobile ? 'text-emerald-600' : ''}`}>
                  {t("herdStatus")}
                </h3>
                <div className="space-y-4">
                   <div className="flex items-center justify-between p-4 bg-zinc-50 rounded-2xl border border-zinc-100">
                      <div>
                        <p className="font-bold text-zinc-800">{t("autoBarrows")}</p>
                        <p className="text-xs text-zinc-500">{t("autoBarrowsDesc")}</p>
                      </div>
                      <input
                        type="checkbox"
                        checked={autoClassifyBarrows}
                        onChange={(e) => setAutoClassifyBarrows(e.target.checked)}
                        className="h-5 w-5 text-emerald-600 border-zinc-300 rounded focus:ring-emerald-500"
                      />
                   </div>
                   <div className="flex items-center justify-between p-4 bg-zinc-50 rounded-2xl border border-zinc-100">
                      <div>
                        <p className="font-bold text-zinc-800">{t("autoSows")}</p>
                        <p className="text-xs text-zinc-500">{t("autoSowsDesc")}</p>
                      </div>
                      <input
                        type="checkbox"
                        checked={autoClassifySows}
                        onChange={(e) => setAutoClassifySows(e.target.checked)}
                        className="h-5 w-5 text-emerald-600 border-zinc-300 rounded focus:ring-emerald-500"
                      />
                   </div>

                   <div className="p-4 bg-zinc-50 rounded-2xl border border-zinc-100 space-y-3">
                      <div>
                        <p className="font-bold text-zinc-800">{t("giltClassification")}</p>
                        <p className="text-xs text-zinc-500">{t("giltDesc")}</p>
                      </div>
                      <div className="flex items-center gap-3">
                        <input
                          type="number"
                          value={giltAgeThreshold}
                          onChange={(e) => setGiltAgeThreshold(e.target.value)}
                          className="w-20 rounded-xl border border-zinc-200 bg-white px-3 py-1.5 text-sm font-bold text-zinc-900 focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 outline-none transition"
                        />
                        <span className="text-xs font-bold text-zinc-400 uppercase tracking-widest">{t("weeks")}</span>
                      </div>
                   </div>

                   {/* Porker Status Classification */}
                   <div className="p-4 bg-zinc-50/80 rounded-2xl border border-zinc-200/80 space-y-4">
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                        <div>
                          <p className="font-bold text-zinc-900 text-sm">Porker Classification</p>
                          <p className="text-xs text-zinc-500">Configure transition thresholds between meat porker stages</p>
                        </div>
                        {/* Age vs Weight Switch */}
                        <div className="flex items-center bg-zinc-200/70 p-1 rounded-xl shrink-0 self-start sm:self-auto">
                          <button
                            type="button"
                            onClick={() => setPorkerUseAge(true)}
                            className={`px-3 py-1 text-xs font-bold rounded-lg transition ${
                              porkerUseAge ? "bg-white text-emerald-700 shadow-xs" : "text-zinc-600 hover:text-zinc-900"
                            }`}
                          >
                            Age (Weeks)
                          </button>
                          <button
                            type="button"
                            onClick={() => setPorkerUseAge(false)}
                            className={`px-3 py-1 text-xs font-bold rounded-lg transition ${
                              !porkerUseAge ? "bg-white text-emerald-700 shadow-xs" : "text-zinc-600 hover:text-zinc-900"
                            }`}
                          >
                            Weight (kg)
                          </button>
                        </div>
                      </div>

                      <div className="space-y-2.5 pt-2 border-t border-zinc-200/60 text-xs">
                        {porkerUseAge ? (
                          <>
                            <div className="flex items-center justify-between bg-white p-2.5 rounded-xl border border-zinc-200">
                              <span className="font-semibold text-zinc-700">Starter</span>
                              <div className="flex items-center gap-1.5">
                                <span className="text-zinc-400">0 to</span>
                                <input
                                  type="number"
                                  value={porkerStarterAge}
                                  onChange={(e) => setPorkerStarterAge(e.target.value)}
                                  className="w-16 px-2 py-1 text-center font-bold border border-zinc-200 rounded-lg text-xs"
                                />
                                <span className="text-zinc-400">wks</span>
                              </div>
                            </div>
                            <div className="flex items-center justify-between bg-white p-2.5 rounded-xl border border-zinc-200">
                              <span className="font-semibold text-zinc-700">Grower</span>
                              <div className="flex items-center gap-1.5">
                                <span className="text-zinc-400">{porkerStarterAge || "16"} to</span>
                                <input
                                  type="number"
                                  value={porkerGrowerAge}
                                  onChange={(e) => setPorkerGrowerAge(e.target.value)}
                                  className="w-16 px-2 py-1 text-center font-bold border border-zinc-200 rounded-lg text-xs"
                                />
                                <span className="text-zinc-400">wks</span>
                              </div>
                            </div>
                            <div className="flex items-center justify-between bg-white p-2.5 rounded-xl border border-zinc-200">
                              <span className="font-semibold text-zinc-700">Finisher</span>
                              <div className="flex items-center gap-1.5 font-bold text-zinc-600">
                                <span>{porkerGrowerAge || "24"}+ wks (Slaughter)</span>
                              </div>
                            </div>
                          </>
                        ) : (
                          <>
                            <div className="flex items-center justify-between bg-white p-2.5 rounded-xl border border-zinc-200">
                              <span className="font-semibold text-zinc-700">Starter</span>
                              <div className="flex items-center gap-1.5">
                                <span className="text-zinc-400">0 to</span>
                                <input
                                  type="number"
                                  value={porkerStarterWeight}
                                  onChange={(e) => setPorkerStarterWeight(e.target.value)}
                                  className="w-16 px-2 py-1 text-center font-bold border border-zinc-200 rounded-lg text-xs"
                                />
                                <span className="text-zinc-400">kg</span>
                              </div>
                            </div>
                            <div className="flex items-center justify-between bg-white p-2.5 rounded-xl border border-zinc-200">
                              <span className="font-semibold text-zinc-700">Grower</span>
                              <div className="flex items-center gap-1.5">
                                <span className="text-zinc-400">{porkerStarterWeight || "25"} to</span>
                                <input
                                  type="number"
                                  value={porkerGrowerWeight}
                                  onChange={(e) => setPorkerGrowerWeight(e.target.value)}
                                  className="w-16 px-2 py-1 text-center font-bold border border-zinc-200 rounded-lg text-xs"
                                />
                                <span className="text-zinc-400">kg</span>
                              </div>
                            </div>
                            <div className="flex items-center justify-between bg-white p-2.5 rounded-xl border border-zinc-200">
                              <span className="font-semibold text-zinc-700">Finisher</span>
                              <div className="flex items-center gap-1.5 font-bold text-zinc-600">
                                <span>{porkerGrowerWeight || "60"}+ kg (Slaughter)</span>
                              </div>
                            </div>
                          </>
                        )}
                      </div>
                   </div>

                   {/* Breeder Status Classification */}
                   <div className="p-4 bg-zinc-50/80 rounded-2xl border border-zinc-200/80 space-y-4">
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                        <div>
                          <p className="font-bold text-zinc-900 text-sm">Breeder Classification</p>
                          <p className="text-xs text-zinc-500">Configure transition thresholds between breeding herd stages</p>
                        </div>
                        {/* Age vs Weight Switch */}
                        <div className="flex items-center bg-zinc-200/70 p-1 rounded-xl shrink-0 self-start sm:self-auto">
                          <button
                            type="button"
                            onClick={() => setBreederUseAge(true)}
                            className={`px-3 py-1 text-xs font-bold rounded-lg transition ${
                              breederUseAge ? "bg-white text-emerald-700 shadow-xs" : "text-zinc-600 hover:text-zinc-900"
                            }`}
                          >
                            Age (Weeks)
                          </button>
                          <button
                            type="button"
                            onClick={() => setBreederUseAge(false)}
                            className={`px-3 py-1 text-xs font-bold rounded-lg transition ${
                              !breederUseAge ? "bg-white text-emerald-700 shadow-xs" : "text-zinc-600 hover:text-zinc-900"
                            }`}
                          >
                            Weight (kg)
                          </button>
                        </div>
                      </div>

                      <div className="space-y-2.5 pt-2 border-t border-zinc-200/60 text-xs">
                        {breederUseAge ? (
                          <>
                            <div className="flex items-center justify-between bg-white p-2.5 rounded-xl border border-zinc-200">
                              <span className="font-semibold text-zinc-700">Piglet</span>
                              <div className="flex items-center gap-1.5">
                                <span className="text-zinc-400">0 to</span>
                                <input
                                  type="number"
                                  value={breederPigletAge}
                                  onChange={(e) => setBreederPigletAge(e.target.value)}
                                  className="w-16 px-2 py-1 text-center font-bold border border-zinc-200 rounded-lg text-xs"
                                />
                                <span className="text-zinc-400">wks</span>
                              </div>
                            </div>
                            <div className="flex items-center justify-between bg-white p-2.5 rounded-xl border border-zinc-200">
                              <span className="font-semibold text-zinc-700">Weaner</span>
                              <div className="flex items-center gap-1.5">
                                <span className="text-zinc-400">{breederPigletAge || "8"} to</span>
                                <input
                                  type="number"
                                  value={breederWeanerAge}
                                  onChange={(e) => setBreederWeanerAge(e.target.value)}
                                  className="w-16 px-2 py-1 text-center font-bold border border-zinc-200 rounded-lg text-xs"
                                />
                                <span className="text-zinc-400">wks</span>
                              </div>
                            </div>
                            <div className="flex items-center justify-between bg-white p-2.5 rounded-xl border border-zinc-200">
                              <span className="font-semibold text-zinc-700">Grower</span>
                              <div className="flex items-center gap-1.5">
                                <span className="text-zinc-400">{breederWeanerAge || "16"} to</span>
                                <input
                                  type="number"
                                  value={breederGrowerAge}
                                  onChange={(e) => setBreederGrowerAge(e.target.value)}
                                  className="w-16 px-2 py-1 text-center font-bold border border-zinc-200 rounded-lg text-xs"
                                />
                                <span className="text-zinc-400">wks</span>
                              </div>
                            </div>
                            <div className="flex items-center justify-between bg-white p-2.5 rounded-xl border border-zinc-200">
                              <span className="font-semibold text-zinc-700">Boar / Gilt / Sow</span>
                              <div className="flex items-center gap-1.5 font-bold text-zinc-600">
                                <span>{breederGrowerAge || "24"}+ wks (Adult Herd)</span>
                              </div>
                            </div>
                          </>
                        ) : (
                          <>
                            <div className="flex items-center justify-between bg-white p-2.5 rounded-xl border border-zinc-200">
                              <span className="font-semibold text-zinc-700">Piglet</span>
                              <div className="flex items-center gap-1.5">
                                <span className="text-zinc-400">0 to</span>
                                <input
                                  type="number"
                                  value={breederPigletWeight}
                                  onChange={(e) => setBreederPigletWeight(e.target.value)}
                                  className="w-16 px-2 py-1 text-center font-bold border border-zinc-200 rounded-lg text-xs"
                                />
                                <span className="text-zinc-400">kg</span>
                              </div>
                            </div>
                            <div className="flex items-center justify-between bg-white p-2.5 rounded-xl border border-zinc-200">
                              <span className="font-semibold text-zinc-700">Weaner</span>
                              <div className="flex items-center gap-1.5">
                                <span className="text-zinc-400">{breederPigletWeight || "10"} to</span>
                                <input
                                  type="number"
                                  value={breederWeanerWeight}
                                  onChange={(e) => setBreederWeanerWeight(e.target.value)}
                                  className="w-16 px-2 py-1 text-center font-bold border border-zinc-200 rounded-lg text-xs"
                                />
                                <span className="text-zinc-400">kg</span>
                              </div>
                            </div>
                            <div className="flex items-center justify-between bg-white p-2.5 rounded-xl border border-zinc-200">
                              <span className="font-semibold text-zinc-700">Grower</span>
                              <div className="flex items-center gap-1.5">
                                <span className="text-zinc-400">{breederWeanerWeight || "25"} to</span>
                                <input
                                  type="number"
                                  value={breederGrowerWeight}
                                  onChange={(e) => setBreederGrowerWeight(e.target.value)}
                                  className="w-16 px-2 py-1 text-center font-bold border border-zinc-200 rounded-lg text-xs"
                                />
                                <span className="text-zinc-400">kg</span>
                              </div>
                            </div>
                            <div className="flex items-center justify-between bg-white p-2.5 rounded-xl border border-zinc-200">
                              <span className="font-semibold text-zinc-700">Boar / Gilt / Sow</span>
                              <div className="flex items-center gap-1.5 font-bold text-zinc-600">
                                <span>{breederGrowerWeight || "60"}+ kg (Adult Herd)</span>
                              </div>
                            </div>
                          </>
                        )}
                      </div>
                   </div>
                </div>
                {isMobile && <div className="border-t border-zinc-100 pt-10" />}
              </div>
            )}

            {(activeTab === "data" || isMobile) && (
              <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
                <h3 className={`text-sm font-black text-zinc-400 uppercase tracking-widest mb-4 ${isMobile ? 'text-emerald-600' : ''}`}>
                  {t("dataManagement")}
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                   <ClearCard title={t("clearHerdData")} description={t("clearHerdDesc")} onClear={() => handleClearData("pigs")} t={t} />
                   <ClearCard title={t("clearFinancials")} description={t("clearFinDesc")} onClear={() => handleClearData("financials")} t={t} />
                   <ClearCard title={t("clearFeedData")} description="Permanently delete all feed ingredients, requirements, inventory, and mix transactions." onClear={() => handleClearData("feed")} t={t} />
                   <ClearCard title={t("clearHRData")} description="Permanently delete all staff personnel and salary payment logs." onClear={() => handleClearData("staff")} t={t} />

                   <div className="p-4 bg-amber-50 border border-amber-200 rounded-2xl flex flex-col justify-between hover:bg-amber-100/50 transition text-left">
                     <div>
                       <p className="font-bold text-amber-950 text-sm">Factory Reset Farm</p>
                       <p className="text-[10px] font-bold text-amber-800/80 uppercase tracking-tight mt-1 leading-relaxed">
                         Atomic wipe of all 13 subcollections: herd, financials, feed, tasks, alerts, and HR.
                       </p>
                     </div>
                     <button
                       type="button"
                       onClick={handleFactoryReset}
                       className="mt-4 w-full py-2 bg-amber-700 hover:bg-amber-800 text-white text-[10px] font-black uppercase tracking-widest rounded-xl transition"
                     >
                       Factory Reset All Data
                     </button>
                   </div>

                   <div className="p-4 bg-rose-100 border border-rose-300 rounded-2xl flex flex-col justify-between hover:bg-rose-200/50 transition text-left">
                     <div>
                       <p className="font-bold text-rose-950 text-sm">Delete Account</p>
                       <p className="text-[10px] font-bold text-rose-800/80 uppercase tracking-tight mt-1 leading-relaxed">
                         Permanently delete your user profile, authentication credentials, and all farm databases.
                       </p>
                     </div>
                     <button
                       type="button"
                       onClick={handleDeleteAccount}
                       className="mt-4 w-full py-2 bg-rose-700 hover:bg-rose-800 text-white text-[10px] font-black uppercase tracking-widest rounded-xl transition"
                     >
                       Delete Account
                     </button>
                   </div>

                   <div className="md:col-span-2 mt-4 p-4 border-2 border-dashed border-zinc-200 rounded-2xl text-center">
                     <p className="text-xs font-bold text-zinc-400 uppercase tracking-widest">{t("cloudSync")}</p>
                     <p className="text-[10px] text-zinc-400 mt-1 font-medium">{t("cloudSyncDesc")}</p>
                   </div>
                </div>
                {isMobile && <div className="border-t border-zinc-100 pt-10" />}
              </div>
            )}

            {(activeTab === "about" || isMobile) && (
              <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
                <h3 className={`text-sm font-black text-zinc-400 uppercase tracking-widest mb-4 ${isMobile ? 'text-emerald-600' : ''}`}>
                  {t("aboutSmartSwine")}
                </h3>
                <div className="flex flex-col items-center text-center space-y-4 py-8">
                  <img src="/app_logo.png" alt="SmartSwine Logo" className="h-24 w-24 object-contain rounded-2xl shadow-xl" />
                  <div>
                    <h4 className="text-lg font-black text-zinc-900">SmartSwine Web</h4>
                    <p className="text-sm font-bold text-emerald-600">{t("version")}</p>
                  </div>
                  <p className="text-xs text-zinc-500 max-w-md leading-relaxed">
                    {t("aboutDesc")}
                  </p>

                  <button
                    type="button"
                    onClick={() => setShowWhatsNew(true)}
                    className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200 rounded-xl text-xs font-bold transition"
                  >
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <span>What's New in SmartSwine</span>
                  </button>

                  {/* Developer & Support Contact Info matching Android */}
                  <div className="pt-6 border-t border-zinc-100 w-full max-w-sm space-y-3">
                    <p className="text-[11px] text-zinc-400 font-medium">Developed by</p>
                    <p className="text-sm font-bold text-emerald-700">Goshen Agrifirm & Bibinii Tech</p>

                    <div className="flex items-center justify-center gap-4 text-xs font-semibold">
                      <a
                        href="https://wa.me/233544737870"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 text-emerald-600 hover:underline"
                      >
                        <span className="text-base">💬</span> WhatsApp: +233544737870
                      </a>
                      <span className="text-zinc-300">•</span>
                      <a
                        href="https://t.me/BibiniiTech"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 text-sky-600 hover:underline"
                      >
                        <span className="text-base">✈️</span> Telegram: @BibiniiTech
                      </a>
                    </div>
                    <p className="text-[10px] text-zinc-400">© 2026 SmartSwine. All rights reserved.</p>
                  </div>

                  <div className="flex gap-4 pt-2">
                    <Link href="/terms" className="text-xs font-bold text-zinc-400 hover:text-emerald-600 transition">{t("termsOfService")}</Link>
                    <Link href="/privacy" className="text-xs font-bold text-zinc-400 hover:text-emerald-600 transition">{t("privacyPolicy")}</Link>
                  </div>
                </div>
              </div>
            )}

            {/* What's New Modal Dialog matching Android WhatsNewDialog */}
            {showWhatsNew && (
              <div className="fixed inset-0 z-[120] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-fadeIn">
                <div className="bg-white rounded-2xl border border-zinc-200 max-w-md w-full p-6 shadow-2xl space-y-4">
                  <div className="flex items-center justify-between pb-3 border-b border-zinc-100">
                    <div className="flex items-center gap-2">
                      <span className="text-xl">✨</span>
                      <h3 className="text-base font-bold text-zinc-900">What's New in SmartSwine</h3>
                    </div>
                    <button
                      type="button"
                      onClick={() => setShowWhatsNew(false)}
                      className="p-1.5 hover:bg-zinc-100 rounded-lg text-zinc-400"
                    >
                      <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>

                  <div className="space-y-3.5 max-h-[60vh] overflow-y-auto pr-1 text-xs">
                    <div className="flex items-start gap-3 p-2.5 rounded-xl bg-emerald-50/60 border border-emerald-100">
                      <span className="text-xl shrink-0">📖</span>
                      <div>
                        <p className="font-bold text-zinc-900 text-xs">Pig Farm Training Guide</p>
                        <p className="text-zinc-600 text-[11px] mt-0.5">Comprehensive practical guide covering breeding, piglet care, health management, and feeding protocols.</p>
                      </div>
                    </div>

                    <div className="flex items-start gap-3 p-2.5 rounded-xl bg-blue-50/60 border border-blue-100">
                      <span className="text-xl shrink-0">🌐</span>
                      <div>
                        <p className="font-bold text-zinc-900 text-xs">22 Worldwide Languages</p>
                        <p className="text-zinc-600 text-[11px] mt-0.5">Full multilingual localization across 22 global and regional languages with instant in-app switching.</p>
                      </div>
                    </div>

                    <div className="flex items-start gap-3 p-2.5 rounded-xl bg-amber-50/60 border border-amber-100">
                      <span className="text-xl shrink-0">📏</span>
                      <div>
                        <p className="font-bold text-zinc-900 text-xs">Tape & Scale Weight Checker</p>
                        <p className="text-zinc-600 text-[11px] mt-0.5">Calculate live and carcass weights via heart girth & body length or hanging scales, with lifetime ADG growth projection.</p>
                      </div>
                    </div>

                    <div className="flex items-start gap-3 p-2.5 rounded-xl bg-purple-50/60 border border-purple-100">
                      <span className="text-xl shrink-0">🧪</span>
                      <div>
                        <p className="font-bold text-zinc-900 text-xs">Least-Cost NRC Feed Formulator</p>
                        <p className="text-zinc-600 text-[11px] mt-0.5">Pearson Square least-cost feed formulation with safety inclusion checks and automatic inventory deduction batches.</p>
                      </div>
                    </div>

                    <div className="flex items-start gap-3 p-2.5 rounded-xl bg-rose-50/60 border border-rose-100">
                      <span className="text-xl shrink-0">🛡️</span>
                      <div>
                        <p className="font-bold text-zinc-900 text-xs">Swine Disease Finder & Telehealth</p>
                        <p className="text-zinc-600 text-[11px] mt-0.5">Diagnose symptoms with hallmark sign matching and generate one-click telehealth clinical reports for veterinarians.</p>
                      </div>
                    </div>

                    <div className="flex items-start gap-3 p-2.5 rounded-xl bg-teal-50/60 border border-teal-100">
                      <span className="text-xl shrink-0">☁️</span>
                      <div>
                        <p className="font-bold text-zinc-900 text-xs">Real-Time Cloud Sync & Role Security</p>
                        <p className="text-zinc-600 text-[11px] mt-0.5">Multi-device synchronization between mobile and web with strict keyword-based financial protection for farm staff.</p>
                      </div>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => setShowWhatsNew(false)}
                    className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition shadow-sm"
                  >
                    Got It
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="p-6 border-t border-zinc-100 bg-zinc-50/50 flex justify-end gap-3">
          <button onClick={onClose} className="px-6 py-2.5 rounded-xl border border-zinc-200 bg-white text-sm font-bold text-zinc-600 hover:bg-zinc-100 transition">
            {t("cancel")}
          </button>
          <button
            onClick={handleSaveSettings}
            disabled={isSaving}
            className="px-8 py-2.5 rounded-xl bg-emerald-600 text-white text-sm font-bold hover:bg-emerald-700 shadow-lg shadow-emerald-600/20 transition disabled:bg-zinc-300 disabled:shadow-none"
          >
            {isSaving ? t("saving") : t("saveAll")}
          </button>
        </div>
      </div>
    </div>
  );
}

function TabButton({ id, label, active, onClick }: { id: string; label: string; active: string; onClick: (id: string) => void }) {
  const isSelected = active === id;
  return (
    <button
      onClick={() => onClick(id)}
      className={`w-full text-left px-4 py-3 rounded-xl text-xs font-black uppercase tracking-wider transition duration-200 ${
        isSelected ? "bg-emerald-50 text-emerald-700 shadow-sm border border-emerald-100" : "text-zinc-500 hover:bg-zinc-100 hover:text-zinc-900"
      }`}
    >
      {label}
    </button>
  );
}

function SettingInput({ label, value, onChange, description }: { label: string; value: string; onChange: (val: string) => void; description?: string }) {
  return (
    <div className="space-y-1.5">
      <label className="block text-xs font-bold text-zinc-500 uppercase tracking-tight">{label}</label>
      <input
        type="number"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-xl border border-zinc-200 bg-white px-4 py-2.5 text-sm font-semibold text-zinc-900 focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 outline-none transition"
      />
      {description && <p className="text-[10px] text-zinc-400 font-medium px-1">{description}</p>}
    </div>
  );
}

function ClearCard({ title, description, onClear, t }: { title: string; description: string; onClear: () => void, t: any }) {
  return (
    <div className="p-4 bg-rose-50 border border-rose-100 rounded-2xl flex flex-col justify-between hover:bg-rose-100/50 transition text-left">
      <div>
        <p className="font-bold text-rose-900 text-sm">{title}</p>
        <p className="text-[10px] font-bold text-rose-700/60 uppercase tracking-tight mt-1 leading-relaxed">{description}</p>
      </div>
      <button onClick={onClear} className="mt-4 w-full py-2 bg-rose-600 text-white text-[10px] font-black uppercase tracking-widest rounded-xl hover:bg-rose-700 transition">
        {t("confirmErasure")}
      </button>
    </div>
  );
}
