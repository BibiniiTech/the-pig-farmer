"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import { db } from "@/lib/firebase";
import { doc, updateDoc, deleteDoc, collection, getDocs, setDoc } from "firebase/firestore";
import { useTranslations } from "next-intl";

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function SettingsModal({ isOpen, onClose }: SettingsModalProps) {
  const t = useTranslations("Settings");
  const { user, userProfile, activeFarmUid } = useAuth();
  const { isMobile } = useDevice();
  const [activeTab, setActiveTab] = useState("profile");
  const [isSaving, setIsSaving] = useState(false);

  // Profile States
  const [farmName, setFarmName] = useState(userProfile?.farmName || "");
  const [country, setCountry] = useState(userProfile?.country || "");

  // Cycle States
  const [weaningDays, setWeaningDays] = useState(userProfile?.settings?.weaningDays || "56");
  const [farrowingDays, setFarrowingDays] = useState(userProfile?.settings?.farrowingDays || "114");
  const [ironDay1, setIronDay1] = useState(userProfile?.settings?.ironDay1 || "3");
  const [ironDay2, setIronDay2] = useState(userProfile?.settings?.ironDay2 || "10");

  // Status States
  const [autoClassifyBarrows, setAutoClassifyBarrows] = useState(userProfile?.settings?.autoClassifyBarrows ?? true);
  const [autoClassifySows, setAutoClassifySows] = useState(userProfile?.settings?.autoClassifySows ?? true);
  const [giltAgeThreshold, setGiltAgeThreshold] = useState(userProfile?.settings?.giltAgeThresholdWeeks || "26");

  // Currency States
  const [selectedCurrency, setSelectedCurrency] = useState(userProfile?.settings?.selectedCurrency || "USD");
  const [currencySymbol, setCurrencySymbol] = useState(userProfile?.settings?.currencySymbol || "$");

  useEffect(() => {
    if (userProfile) {
      setFarmName(userProfile.farmName || "");
      setCountry(userProfile.country || "");
      if (userProfile.settings) {
        setWeaningDays(userProfile.settings.weaningDays || "56");
        setFarrowingDays(userProfile.settings.farrowingDays || "114");
        setIronDay1(userProfile.settings.ironDay1 || "3");
        setIronDay2(userProfile.settings.ironDay2 || "10");
        setAutoClassifyBarrows(userProfile.settings.autoClassifyBarrows ?? true);
        setAutoClassifySows(userProfile.settings.autoClassifySows ?? true);
        setGiltAgeThreshold(userProfile.settings.giltAgeThresholdWeeks || "26");
        setSelectedCurrency(userProfile.settings.selectedCurrency || "USD");
        setCurrencySymbol(userProfile.settings.currencySymbol || "$");
      }
    }
  }, [userProfile, isOpen]);

  if (!isOpen) return null;

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
      };

      await updateDoc(userRef, {
        farmName,
        country,
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

  const handleClearData = async (type: string) => {
    if (!activeFarmUid || !confirm(t("confirmClear", { type }))) return;

    try {
      const collectionRef = collection(db, "users", activeFarmUid, type);
      const snapshot = await getDocs(collectionRef);
      const deletePromises = snapshot.docs.map((doc) => deleteDoc(doc.ref));

      if (type === "pigs") {
        const archRef = collection(db, "users", activeFarmUid, "archived_pigs");
        const archSnap = await getDocs(archRef);
        archSnap.docs.forEach(d => deletePromises.push(deleteDoc(d.ref)));
      }

      await Promise.all(deletePromises);
      alert(`${type} data cleared successfully.`);
    } catch (err) {
      console.error(err);
      alert("Failed to clear data.");
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
                      <input
                        type="text"
                        value={country}
                        onChange={(e) => setCountry(e.target.value)}
                        className="w-full rounded-xl border border-zinc-200 bg-white px-4 py-2.5 text-sm font-semibold text-zinc-900 focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 outline-none transition"
                      />
                    </div>

                    <div className="pt-4 space-y-4">
                      <h4 className="text-xs font-black text-zinc-400 uppercase tracking-widest">{t("localization")}</h4>
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <label className="block text-[10px] font-black text-zinc-400 uppercase mb-1">{t("currencyCode")}</label>
                          <input
                            type="text"
                            value={selectedCurrency}
                            onChange={(e) => setSelectedCurrency(e.target.value.toUpperCase())}
                            placeholder="USD"
                            className="w-full rounded-xl border border-zinc-200 bg-white px-4 py-2.5 text-sm font-semibold text-zinc-900 focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 outline-none transition"
                          />
                        </div>
                        <div>
                          <label className="block text-[10px] font-black text-zinc-400 uppercase mb-1">{t("symbol")}</label>
                          <input
                            type="text"
                            value={currencySymbol}
                            onChange={(e) => setCurrencySymbol(e.target.value)}
                            placeholder="$"
                            className="w-full rounded-xl border border-zinc-200 bg-white px-4 py-2.5 text-sm font-semibold text-zinc-900 focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 outline-none transition"
                          />
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
                   <ClearCard title={t("clearFeedData")} description={t("clearFeedDesc")} onClear={() => handleClearData("feed_inventory")} t={t} />
                   <ClearCard title={t("clearHRData")} description={t("clearHRDesc")} onClear={() => handleClearData("staff")} t={t} />
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
                  <div className="flex gap-4 pt-4">
                    <Link href="/terms" className="text-xs font-bold text-zinc-400 hover:text-emerald-600 transition">{t("termsOfService")}</Link>
                    <Link href="/privacy" className="text-xs font-bold text-zinc-400 hover:text-emerald-600 transition">{t("privacyPolicy")}</Link>
                  </div>
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
