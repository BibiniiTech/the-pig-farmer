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
  const { user, userProfile, activeFarmUid, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  const [staff, setStaff] = useState<StaffMember[]>([]);
  const [financialRecords, setFinancialRecords] = useState<any[]>([]);
  const [dataLoading, setDataLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingStaff, setEditingStaff] = useState<StaffMember | null>(null);

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
    if (!activeFarmUid || !confirm("Are you sure you want to remove this staff member?")) return;
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
            <h2 className="text-2xl font-bold text-zinc-900">Staff Management Directory</h2>
            <p className="text-sm text-zinc-500">Add team members, configure salary packages, and manage farm dashboard access.</p>
          </div>

          {/* Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            <div className="backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm bg-white/60">
              <p className="text-xs font-semibold uppercase tracking-wider text-zinc-500">Total Staff</p>
              <p className="text-3xl font-bold mt-2 text-zinc-900">{staff.length}</p>
            </div>
            <div className="backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm bg-white/60">
              <p className="text-xs font-semibold uppercase tracking-wider text-zinc-500">Monthly Payroll</p>
              <p className="text-3xl font-bold mt-2 text-emerald-600">
                ${staff.reduce((sum, member) => sum + member.salary, 0).toFixed(2)}
              </p>
            </div>
            <div className="backdrop-blur-md border border-emerald-200/50 rounded-2xl p-6 shadow-sm bg-emerald-50/40 flex flex-col justify-center items-center gap-3">
              <p className="text-xs font-bold text-emerald-800 uppercase tracking-tight">Expand Your Team</p>
              <div className="flex w-full gap-2">
                <button
                  onClick={() => {
                    const isPremium = userProfile?.isPremium || userProfile?.isAdmin;
                    if (!isPremium) {
                      alert("Adding staff members is a Premium Feature. Please upgrade to expand your team.");
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
                  + Add Staff
                </button>
                <button
                  onClick={() => {
                    const isPremium = userProfile?.isPremium || userProfile?.isAdmin;
                    if (!isPremium) {
                      alert("Exporting PDF reports is a Premium feature. Upgrade to unlock!");
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
                  title={userProfile?.isPremium || userProfile?.isAdmin ? "Export PDF" : "Export PDF (Premium)"}
                >
                  <svg className="h-4 w-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M20 2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-8 11c0 .55-.45 1-1 1H9v2H7.5v-5h3c.55 0 1 .45 1 1v1zm5 2c0 .55-.45 1-1 1h-2.5v-5H16c.55 0 1 .45 1 1v3zm-5.5-4H10v1.5h.5V11zm4.5 1h-.5v2h.5v-2zm2.5 1h-2v-1h2v-1h-2v-1h3.5v5H19v-2z" />
                  </svg>
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
            <p className="text-sm text-zinc-500 text-center py-12">No staff registered in this farm yet.</p>
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
                        <span>Phone</span>
                        <span className="font-semibold text-zinc-800">{member.phone || "N/A"}</span>
                      </div>
                      <div className="flex justify-between py-1">
                        <span>Email</span>
                        <span className="font-semibold text-zinc-800">{member.email || "N/A"}</span>
                      </div>
                      <div className="flex justify-between py-1">
                        <span>Salary</span>
                        <span className="font-semibold text-zinc-800">${member.salary.toFixed(2)}/mo</span>
                      </div>
                      <div className="flex justify-between py-1">
                        <span>Joined</span>
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
                        <span>App Access</span>
                      </label>

                      <div className="space-x-3 text-xs">
                        <button onClick={() => startEdit(member)} className="text-emerald-600 hover:underline font-bold">
                          Edit
                        </button>
                        <button onClick={() => handleDeleteStaff(member)} className="text-rose-600 hover:underline font-bold">
                          Remove
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
        currencySymbol={userProfile?.settings?.currencySymbol || "$"}
      />

      {/* Add / Edit Staff Modal */}
      {(showAddModal || editingStaff) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-md p-6 space-y-6 shadow-2xl">
            <h3 className="text-lg font-bold text-zinc-900">
              {editingStaff ? "Edit Staff Details" : "Register Staff Member"}
            </h3>
            <form onSubmit={editingStaff ? handleUpdateStaff : handleAddStaff} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Full Name</label>
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
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Role</label>
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
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Status</label>
                  <select
                    value={status}
                    onChange={(e) => setStatus(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    <option>Active</option>
                    <option>Inactive</option>
                    <option>On Leave</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Salary ($/mo)</label>
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
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Joined Date</label>
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
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Phone</label>
                  <input
                    type="text"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="+233..."
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Email address</label>
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
                  <span>Allow web/app database login access?</span>
                </label>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
                <button
                  type="button"
                  onClick={() => { setShowAddModal(false); setEditingStaff(null); }}
                  className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:bg-zinc-100 transition"
                >
                  Cancel
                </button>
                <button type="submit" className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition">
                  {editingStaff ? "Save Updates" : "Register Staff"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
