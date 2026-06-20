"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot, doc, setDoc, deleteDoc, updateDoc } from "firebase/firestore";
import { db, auth } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import UserProfileDropdown from "@/components/UserProfileDropdown";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import HRReport from "@/components/reports/HRReport";
import { sendPasswordResetEmail } from "firebase/auth";
import { ExportPdfIcon } from "@/components/icons/DashboardIcons";
import { useTranslations } from "next-intl";

interface StaffMember {
  id: string;
  name: string;
  role: string;
  phone: string;
  salary: number;
  joinDate: string;
  status: string; // "Active", "Inactive", "On Leave"
  allowAppAccess: boolean;
  email: string;
}

async function signUpUserRest(apiKey: string, email: string): Promise<boolean> {
  try {
    const url = `https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${apiKey}`;
    const tempPassword = Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json"
      },
      body: JSON.stringify({
        email,
        password: tempPassword,
        returnSecureToken: false
      })
    });
    console.log("REST signUp response status:", response.status);
    return response.ok;
  } catch (err) {
    console.error("REST signUp failed:", err);
    return false;
  }
}

async function inviteStaffMember(email: string) {
  const cleanEmail = email.trim().toLowerCase();
  if (!cleanEmail) return;

  try {
    const apiKey = auth.app.options.apiKey;
    if (apiKey) {
      await signUpUserRest(apiKey, cleanEmail);
    }
    await sendPasswordResetEmail(auth, cleanEmail);
    console.log("Invitation email sent to", cleanEmail);
  } catch (err) {
    console.error("Failed to invite staff:", err);
  }
}

export default function HumanResourcesPage() {
  const t = useTranslations("HR");
  const { user, userProfile, activeFarmUid, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  const [staff, setStaff] = useState<StaffMember[]>([]);
  const [financialRecords, setFinancialRecords] = useState<any[]>([]);
  const [dataLoading, setDataLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingStaff, setEditingStaff] = useState<StaffMember | null>(null);

  const currencySymbol = userProfile?.settings?.currencySymbol || "$";

  // Form states
  const [name, setName] = useState("");
  const [role, setRole] = useState("");
  const [phone, setPhone] = useState("");
  const [salary, setSalary] = useState(0);
  const [joinDate, setJoinDate] = useState(new Date().toISOString().split("T")[0]);
  const [status, setStatus] = useState("Active");
  const [allowAppAccess, setAllowAppAccess] = useState(false);
  const [email, setEmail] = useState("");

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!activeFarmUid) return;

    setDataLoading(true);
    const staffRef = collection(db, "users", activeFarmUid, "staff");
    const unsubscribe = onSnapshot(staffRef, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as StaffMember));
      setStaff(list.sort((a, b) => a.name.localeCompare(b.name)));
      setDataLoading(false);
    }, (err) => {
      console.error(err);
      setDataLoading(false);
    });

    // Fetch financials for payroll history
    const finRef = collection(db, "users", activeFarmUid, "financials");
    const unsubscribeFin = onSnapshot(finRef, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as any));
      setFinancialRecords(list);
    });

    return () => {
      unsubscribe();
      unsubscribeFin();
    };
  }, [activeFarmUid]);

  const handleAddStaff = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !name.trim()) return;

    try {
      const staffRef = collection(db, "users", activeFarmUid, "staff");
      const newRef = doc(staffRef);

      const newMember: StaffMember = {
        id: newRef.id,
        name: name.trim(),
        role,
        phone: phone.trim(),
        salary,
        joinDate,
        status,
        allowAppAccess,
        email: email.trim()
      };

      await setDoc(newRef, newMember);

      // Register in staff_registry and send invite if conditions are met
      const isPremium = userProfile?.isPremium || false;
      const cleanEmail = email.trim().toLowerCase();
      if (isPremium && allowAppAccess && cleanEmail) {
        const registryRef = doc(db, "staff_registry", cleanEmail);
        await setDoc(registryRef, { managerUid: activeFarmUid });
        await inviteStaffMember(cleanEmail);
      }

      // Reset
      setName("");
      setRole("");
      setPhone("");
      setSalary(0);
      setEmail("");
      setAllowAppAccess(false);
      setShowAddModal(false);
    } catch (err) {
      console.error("Failed to add staff:", err);
    }
  };

  const handleUpdateStaff = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !editingStaff) return;

    try {
      const isPremium = userProfile?.isPremium || false;
      const cleanEmail = email.trim().toLowerCase();
      const oldEmail = editingStaff.email?.trim().toLowerCase() || "";
      
      const shouldInvite = isPremium && allowAppAccess && cleanEmail &&
        (!editingStaff.allowAppAccess || oldEmail !== cleanEmail);

      // Update registry
      if (isPremium && allowAppAccess && cleanEmail) {
        const registryRef = doc(db, "staff_registry", cleanEmail);
        await setDoc(registryRef, { managerUid: activeFarmUid });
      } else if (cleanEmail) {
        const registryRef = doc(db, "staff_registry", cleanEmail);
        await deleteDoc(registryRef);
      }
      
      // If the email was changed, make sure we clean up the old email from registry
      if (oldEmail && oldEmail !== cleanEmail) {
        const oldRegistryRef = doc(db, "staff_registry", oldEmail);
        await deleteDoc(oldRegistryRef);
      }

      const staffDocRef = doc(db, "users", activeFarmUid, "staff", editingStaff.id);
      await updateDoc(staffDocRef, {
        name: name.trim(),
        role,
        phone: phone.trim(),
        salary,
        joinDate,
        status,
        allowAppAccess,
        email: cleanEmail
      });

      setEditingStaff(null);

      if (shouldInvite) {
        await inviteStaffMember(cleanEmail);
      }
    } catch (err) {
      console.error("Failed to update staff:", err);
    }
  };

  const handleDeleteStaff = async (member: StaffMember) => {
    if (!activeFarmUid || !confirm(t("confirmDelete"))) return;
    try {
      if (member.email) {
        const cleanEmail = member.email.trim().toLowerCase();
        await deleteDoc(doc(db, "staff_registry", cleanEmail));
      }
      await deleteDoc(doc(db, "users", activeFarmUid, "staff", member.id));
    } catch (err) {
      console.error(err);
    }
  };

  const startEdit = (member: StaffMember) => {
    setEditingStaff(member);
    setName(member.name);
    setRole(member.role);
    setPhone(member.phone);
    setSalary(member.salary);
    setJoinDate(member.joinDate);
    setStatus(member.status);
    setAllowAppAccess(member.allowAppAccess);
    setEmail(member.email);
  };

  const handleToggleAccess = async (member: StaffMember) => {
    if (!activeFarmUid) return;
    try {
      const isPremium = userProfile?.isPremium || false;
      const cleanEmail = member.email?.trim().toLowerCase() || "";
      const newAllowAccess = !member.allowAppAccess;

      if (isPremium && newAllowAccess && cleanEmail) {
        // Add to registry and invite
        const registryRef = doc(db, "staff_registry", cleanEmail);
        await setDoc(registryRef, { managerUid: activeFarmUid });
        await inviteStaffMember(cleanEmail);
      } else if (cleanEmail) {
        // Remove from registry
        const registryRef = doc(db, "staff_registry", cleanEmail);
        await deleteDoc(registryRef);
      }

      const ref = doc(db, "users", activeFarmUid, "staff", member.id);
      await updateDoc(ref, { allowAppAccess: newAllowAccess });
    } catch (err) {
      console.error(err);
    }
  };

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-white text-zinc-900">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen bg-white text-zinc-900 flex flex-col font-sans overflow-hidden">
      {/* Watermark Logo Background */}
      {!isMobile && (
        <div className="fixed inset-0 z-0 flex items-center justify-center opacity-[0.15] pointer-events-none select-none">
          <img
            src="/app_logo.png"
            alt="Watermark Background Logo"
            className="w-full max-w-[1100px] max-h-[85vh] object-contain"
          />
        </div>
      )}

      <div className="relative z-10 flex flex-col min-h-screen print:hidden">
        {!isMobile && <DesktopHeader />}

        <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8 space-y-6">
          <div className="space-y-1">
            <h2 className="text-2xl font-bold text-zinc-900">{t("title")}</h2>
            <p className="text-sm text-zinc-500">{t("description")}</p>
          </div>

          {/* Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            <div className="backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm bg-white/60">
              <p className="text-xs font-semibold uppercase tracking-wider text-zinc-500">{t("totalStaff")}</p>
              <p className="text-3xl font-bold mt-2 text-zinc-900">{staff.length}</p>
            </div>
            <div className="backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm bg-white/60">
              <p className="text-xs font-semibold uppercase tracking-wider text-zinc-500">{t("monthlyPayroll")}</p>
              <p className="text-3xl font-bold mt-2 text-emerald-600">
                {currencySymbol}{staff.reduce((sum, member) => sum + member.salary, 0).toFixed(2)}
              </p>
            </div>
            <div className="backdrop-blur-md border border-emerald-200/50 rounded-2xl p-6 shadow-sm bg-emerald-50/40 flex flex-col justify-center items-center gap-3">
              <p className="text-xs font-bold text-emerald-800 uppercase tracking-tight">{t("expandTeam")}</p>
              <div className="flex w-full gap-2">
                <button
                  onClick={() => {
                    const isPremium = userProfile?.isPremium || userProfile?.isAdmin;
                    if (!isPremium) {
                      alert(t("premiumFeatureAdd"));
                      router.push("/dashboard/billing");
                      return;
                    }
                    setEditingStaff(null);
                    setName("");
                    setRole("");
                    setPhone("");
                    setSalary(0);
                    setJoinDate(new Date().toISOString().split("T")[0]);
                    setStatus("Active");
                    setAllowAppAccess(false);
                    setEmail("");
                    setShowAddModal(true);
                  }}
                  className="flex-1 rounded-xl bg-emerald-600 hover:bg-emerald-700 py-3 text-xs font-bold text-white shadow-lg shadow-emerald-600/20 transition-all active:scale-95"
                >
                  {t("addStaff")}
                </button>
                <button
                  onClick={() => {
                    const isPremium = userProfile?.isPremium || userProfile?.isAdmin;
                    if (!isPremium) {
                      alert(t("premiumFeatureExport"));
                      router.push("/dashboard/billing");
                      return;
                    }
                    window.print();
                  }}
                  className={`px-4 rounded-xl border py-3 transition-all active:scale-95 shadow-sm ${
                    userProfile?.isPremium || userProfile?.isAdmin
                      ? "border-zinc-200 bg-white hover:bg-zinc-50 text-zinc-500"
                      : "border-amber-200 bg-amber-50 text-amber-700 hover:bg-amber-100"
                  }`}
                  title={userProfile?.isPremium || userProfile?.isAdmin ? t("exportPdf") : t("exportPdfPremium")}
                >
                  <ExportPdfIcon className="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>

          {dataLoading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="h-44 bg-zinc-100 animate-pulse rounded-2xl" />
              ))}
            </div>
          ) : staff.length === 0 ? (
            <p className="text-sm text-zinc-500 text-center py-12">{t("noStaff")}</p>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {staff.map(member => (
                <div
                  key={member.id}
                  className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-4 relative overflow-hidden group hover:border-emerald-500/30 transition-all duration-300"
                >
                  <div className="absolute top-0 right-0 h-16 w-16 rounded-full bg-emerald-500/5 blur-lg pointer-events-none" />
                  <div className="relative z-10 space-y-4">
                    <div className="flex justify-between items-start">
                      <div>
                        <h4 className="text-base font-bold text-zinc-900">{member.name}</h4>
                        <p className="text-xs text-zinc-500 mt-0.5">{member.role}</p>
                      </div>
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                        member.status === "Active" ? "bg-emerald-50 text-emerald-800" : "bg-zinc-100 text-zinc-500"
                      }`}>
                        {member.status}
                      </span>
                    </div>

                    <div className="divide-y divide-zinc-100 text-xs text-zinc-650 space-y-1.5 pt-1">
                      <div className="flex justify-between py-1">
                        <span>{t("phone")}</span>
                        <span className="font-semibold text-zinc-800">{member.phone || "N/A"}</span>
                      </div>
                      <div className="flex justify-between py-1">
                        <span>{t("email")}</span>
                        <span className="font-semibold text-zinc-800">{member.email || "N/A"}</span>
                      </div>
                      <div className="flex justify-between py-1">
                        <span>{t("salary")}</span>
                        <span className="font-semibold text-zinc-800">{currencySymbol}{member.salary.toFixed(2)}/mo</span>
                      </div>
                      <div className="flex justify-between py-1">
                        <span>{t("joined")}</span>
                        <span className="font-semibold text-zinc-800">{member.joinDate}</span>
                      </div>
                    </div>

                    <div className="pt-3 border-t border-zinc-100 flex items-center justify-between">
                      <label className="flex items-center gap-2 cursor-pointer text-xs text-zinc-600">
                        <input
                          type="checkbox"
                          checked={member.allowAppAccess}
                          onChange={() => handleToggleAccess(member)}
                          className="h-4 w-4 rounded border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                        />
                        <span>{t("appAccess")}</span>
                      </label>

                      <div className="space-x-3 text-xs">
                        <button onClick={() => startEdit(member)} className="text-emerald-600 hover:underline font-bold">
                          {t("edit")}
                        </button>
                        <button onClick={() => handleDeleteStaff(member)} className="text-rose-600 hover:underline font-bold">
                          {t("remove")}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </main>
      </div>

      <HRReport
        staff={staff}
        financialRecords={financialRecords}
        currencySymbol={currencySymbol}
      />

      {/* Add / Edit Staff Modal */}
      {(showAddModal || editingStaff) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-md p-6 space-y-6 shadow-2xl">
            <h3 className="text-lg font-bold text-zinc-900">
              {editingStaff ? t("editStaffDetails") : t("registerStaffMember")}
            </h3>
            <form onSubmit={editingStaff ? handleUpdateStaff : handleAddStaff} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("fullName")}</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. John Doe"
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("role")}</label>
                  <input
                    type="text"
                    required
                    value={role}
                    onChange={(e) => setRole(e.target.value)}
                    placeholder="e.g. Attendant"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("status")}</label>
                  <select
                    value={status}
                    onChange={(e) => setStatus(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    <option value="Active">{t("active")}</option>
                    <option value="Inactive">{t("inactive")}</option>
                    <option value="On Leave">{t("onLeave")}</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("salaryLabel", { symbol: currencySymbol })}</label>
                  <input
                    type="number"
                    step="any"
                    required
                    value={salary}
                    onChange={(e) => setSalary(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("joinedDate")}</label>
                  <input
                    type="date"
                    required
                    value={joinDate}
                    onChange={(e) => setJoinDate(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("phone")}</label>
                  <input
                    type="text"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="+233..."
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">{t("email")}</label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="john@example.com"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
              </div>

              <div>
                <label className="flex items-center gap-2.5 cursor-pointer text-sm text-zinc-700">
                  <input
                    type="checkbox"
                    checked={allowAppAccess}
                    onChange={(e) => setAllowAppAccess(e.target.checked)}
                    className="h-4.5 w-4.5 rounded border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                  />
                  <span>{t("allowAppAccess")}</span>
                </label>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
                <button
                  type="button"
                  onClick={() => { setShowAddModal(false); setEditingStaff(null); }}
                  className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:bg-zinc-100 transition"
                >
                  {t("cancel")}
                </button>
                <button type="submit" className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition">
                  {editingStaff ? t("saveUpdates") : t("registerStaff")}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
