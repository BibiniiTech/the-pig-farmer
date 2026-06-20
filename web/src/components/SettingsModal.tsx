"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { db } from "@/lib/firebase";
import { doc, updateDoc, deleteDoc, collection, getDocs, setDoc } from "firebase/firestore";

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function SettingsModal({ isOpen, onClose }: SettingsModalProps) {
  const { user, userProfile, activeFarmUid } = useAuth();
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

      alert("Settings saved successfully!");
      onClose();
    } catch (err) {
      console.error("Error saving settings:", err);
      alert("Failed to save settings.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleClearData = async (type: string) => {
    if (!activeFarmUid || !confirm(`Are you sure you want to clear all ${type} data? This cannot be undone.`)) return;

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
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col shadow-2xl">
        <div className="p-6 border-b border-zinc-100 flex items-center justify-between bg-zinc-50/50">
          <h2 className="text-xl font-bold text-zinc-900 flex items-center gap-2">
            <svg className="h-6 w-6 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            Settings
          </h2>
          <button onClick={onClose} className="p-2 hover:bg-zinc-200 rounded-lg transition text-zinc-400">
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        <div className="flex-1 flex overflow-hidden">
          {/* Sidebar */}
          <div className="w-64 border-r border-zinc-100 bg-zinc-50/30 p-4 space-y-1 overflow-y-auto no-scrollbar">
            <TabButton id="profile" label="Edit Profile" active={activeTab} onClick={setActiveTab} />
            <TabButton id="cycles" label="Cycles & Timing" active={activeTab} onClick={setActiveTab} />
            <TabButton id="status" label="Herd Status" active={activeTab} onClick={setActiveTab} />
            <TabButton id="data" label="Data Management" active={activeTab} onClick={setActiveTab} />
            <TabButton id="about" label="About SmartSwine" active={activeTab} onClick={setActiveTab} />
          </div>

          {/* Content */}
          <div className="flex-1 overflow-y-auto p-8 space-y-8 no-scrollbar">
            {activeTab === "profile" && (
              <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
                <section>
                  <h3 className="text-sm font-black text-zinc-400 uppercase tracking-widest mb-4">Edit Profile</h3>
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
                      <label className="block text-xs font-bold text-zinc-500 mb-1.5 uppercase">Farm Name</label>
                      <input
                        type="text"
                        value={farmName}
                        onChange={(e) => setFarmName(e.target.value)}
                        className="w-full rounded-xl border border-zinc-200 bg-white px-4 py-2.5 text-sm font-semibold text-zinc-900 focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 outline-none transition"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-zinc-500 mb-1.5 uppercase">Country</label>
                      <input
                        type="text"
                        value={country}
                        onChange={(e) => setCountry(e.target.value)}
                        className="w-full rounded-xl border border-zinc-200 bg-white px-4 py-2.5 text-sm font-semibold text-zinc-900 focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 outline-none transition"
                      />
                    </div>

                    <div className="pt-4 space-y-4">
                      <h4 className="text-xs font-black text-zinc-400 uppercase tracking-widest">Localization</h4>
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <label className="block text-[10px] font-black text-zinc-400 uppercase mb-1">Currency Code</label>
                          <input
                            type="text"
                            value={selectedCurrency}
                            onChange={(e) => setSelectedCurrency(e.target.value.toUpperCase())}
                            placeholder="USD"
                            className="w-full rounded-xl border border-zinc-200 bg-white px-4 py-2 text-sm font-semibold text-zinc-900 focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 outline-none transition"
                          />
                        </div>
                        <div>
                          <label className="block text-[10px] font-black text-zinc-400 uppercase mb-1">Symbol</label>
                          <input
                            type="text"
                            value={currencySymbol}
                            onChange={(e) => setCurrencySymbol(e.target.value)}
                            placeholder="$"
                            className="w-full rounded-xl border border-zinc-200 bg-white px-4 py-2 text-sm font-semibold text-zinc-900 focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 outline-none transition"
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                </section>
              </div>
            )}

            {activeTab === "cycles" && (
              <div className="space-y-8 animate-in fade-in slide-in-from-right-4 duration-300">
                <section>
                  <h3 className="text-sm font-black text-zinc-400 uppercase tracking-widest mb-4">Weaning, Farrowing & Iron Injection</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-2xl">
                    <SettingInput
                      label="Weaning Threshold (Days)"
                      value={weaningDays}
                      onChange={setWeaningDays}
                      description="Target age to move piglets to starter pens."
                    />
                    <SettingInput
                      label="Gestation Period (Days)"
                      value={farrowingDays}
                      onChange={setFarrowingDays}
                      description="Standard pregnancy duration for sows."
                    />
                    <SettingInput
                      label="First Iron Injection (Days)"
                      value={ironDay1}
                      onChange={setIronDay1}
                      description="Age for the initial iron dextran shot."
                    />
                    <SettingInput
                      label="Second Iron Injection (Days)"
                      value={ironDay2}
                      onChange={setIronDay2}
                      description="Age for the follow-up iron shot."
                    />
                  </div>
                </section>
              </div>
            )}

            {activeTab === "status" && (
              <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
                <h3 className="text-sm font-black text-zinc-400 uppercase tracking-widest mb-4">Herd Status & Rules</h3>
                <div className="space-y-4">
                   <div className="flex items-center justify-between p-4 bg-zinc-50 rounded-2xl border border-zinc-100">
                      <div>
                        <p className="font-bold text-zinc-800">Auto-Classify Barrows</p>
                        <p className="text-xs text-zinc-500">Automatically set status to Barrow after castration activity.</p>
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
                        <p className="font-bold text-zinc-800">Auto-Classify Sows</p>
                        <p className="text-xs text-zinc-500">Automatically set status to Sow after first farrowing.</p>
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
                        <p className="font-bold text-zinc-800">Gilt Classification</p>
                        <p className="text-xs text-zinc-500">Age threshold for classifying a female as a Gilt if she hasn't farrowed.</p>
                      </div>
                      <div className="flex items-center gap-3">
                        <input
                          type="number"
                          value={giltAgeThreshold}
                          onChange={(e) => setGiltAgeThreshold(e.target.value)}
                          className="w-20 rounded-xl border border-zinc-200 bg-white px-3 py-1.5 text-sm font-bold text-zinc-900 focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 outline-none transition"
                        />
                        <span className="text-xs font-bold text-zinc-400 uppercase tracking-widest">Weeks</span>
                      </div>
                   </div>
                </div>
              </div>
            )}

            {activeTab === "data" && (
              <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
                <h3 className="text-sm font-black text-zinc-400 uppercase tracking-widest mb-4">Data Management</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                   <ClearCard title="Clear Herd Data" description="Delete all pig profiles and health records. Use with caution." onClear={() => handleClearData("pigs")} />
                   <ClearCard title="Clear Financials" description="Delete all income and expense records." onClear={() => handleClearData("financials")} />
                   <ClearCard title="Clear Feed Data" description="Delete all feed inventory and transactions." onClear={() => handleClearData("feed_inventory")} />
                   <ClearCard title="Clear HR Data" description="Delete all staff and payroll records." onClear={() => handleClearData("staff")} />
                   <div className="md:col-span-2 mt-4 p-4 border-2 border-dashed border-zinc-200 rounded-2xl text-center">
                     <p className="text-xs font-bold text-zinc-400 uppercase tracking-widest">Cloud Sync</p>
                     <p className="text-[10px] text-zinc-400 mt-1 font-medium">Your data is automatically synchronized with the SmartSwine Cloud via Firebase.</p>
                   </div>
                </div>
              </div>
            )}

            {activeTab === "about" && (
              <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
                <h3 className="text-sm font-black text-zinc-400 uppercase tracking-widest mb-4">About SmartSwine</h3>
                <div className="flex flex-col items-center text-center space-y-4 py-8">
                  <img src="/app_logo.png" alt="SmartSwine Logo" className="h-24 w-24 object-contain rounded-2xl shadow-xl" />
                  <div>
                    <h4 className="text-lg font-black text-zinc-900">SmartSwine Web</h4>
                    <p className="text-sm font-bold text-emerald-600">v2.1.0-beta</p>
                  </div>
                  <p className="text-xs text-zinc-500 max-w-md leading-relaxed">
                    Designed for desktop access to your farm data. SmartSwine provides advanced herd tracking, feed formulation, and financial analytics for modern pig farmers.
                  </p>
                  <div className="flex gap-4 pt-4">
                    <Link href="/terms" className="text-xs font-bold text-zinc-400 hover:text-emerald-600 transition">Terms of Service</Link>
                    <Link href="/privacy" className="text-xs font-bold text-zinc-400 hover:text-emerald-600 transition">Privacy Policy</Link>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="p-6 border-t border-zinc-100 bg-zinc-50/50 flex justify-end gap-3">
          <button onClick={onClose} className="px-6 py-2.5 rounded-xl border border-zinc-200 bg-white text-sm font-bold text-zinc-600 hover:bg-zinc-100 transition">
            Cancel
          </button>
          <button
            onClick={handleSaveSettings}
            disabled={isSaving}
            className="px-8 py-2.5 rounded-xl bg-emerald-600 text-white text-sm font-bold hover:bg-emerald-700 shadow-lg shadow-emerald-600/20 transition disabled:bg-zinc-300 disabled:shadow-none"
          >
            {isSaving ? "Saving..." : "Save All Settings"}
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

function ClearCard({ title, description, onClear }: { title: string; description: string; onClear: () => void }) {
  return (
    <div className="p-4 bg-rose-50 border border-rose-100 rounded-2xl flex flex-col justify-between hover:bg-rose-100/50 transition text-left">
      <div>
        <p className="font-bold text-rose-900 text-sm">{title}</p>
        <p className="text-[10px] font-bold text-rose-700/60 uppercase tracking-tight mt-1 leading-relaxed">{description}</p>
      </div>
      <button onClick={onClear} className="mt-4 w-full py-2 bg-rose-600 text-white text-[10px] font-black uppercase tracking-widest rounded-xl hover:bg-rose-700 transition">
        Confirm Erasure
      </button>
    </div>
  );
}
