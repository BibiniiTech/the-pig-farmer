"use client";

import React, { useState, useRef, useEffect } from "react";
import { doc, updateDoc, collection, addDoc, deleteDoc, setDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { StaffMember } from "@/lib/types";
import { useTranslations } from "next-intl";
import { ExportPdfIcon } from "@/components/icons/DashboardIcons";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import { TierLimiter } from "@/lib/tierLimiter";
import HRReport from "@/components/reports/HRReport";

interface InlineHRSectionProps {
  staff: StaffMember[];
  currencySymbol: string;
  activeFarmUid: string;
  initialShowAdd?: boolean;
}

export default function InlineHRSection({
  staff,
  currencySymbol,
  activeFarmUid,
  initialShowAdd = false,
}: InlineHRSectionProps) {
  const t = useTranslations("HR");
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingStaff, setEditingStaff] = useState<StaffMember | null>(null);
  const [staffToViewProfile, setStaffToViewProfile] = useState<StaffMember | null>(null);
  const [staffForFullDetails, setStaffForFullDetails] = useState<StaffMember | null>(null);
  const [staffToPaySalary, setStaffToPaySalary] = useState<StaffMember | null>(null);

  useEffect(() => {
    if (initialShowAdd) {
      openAddModal();
    }
  }, [initialShowAdd]);

  // Form states
  const [name, setName] = useState("");
  const [role, setRole] = useState("");
  const [phone, setPhone] = useState("");
  const [salary, setSalary] = useState(0);
  const [status, setStatus] = useState("Active");
  const [joinDate, setJoinDate] = useState(new Date().toISOString().split("T")[0]);
  const [allowAppAccess, setAllowAppAccess] = useState(false);
  const [email, setEmail] = useState("");
  const [gender, setGender] = useState("Male");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [residentialAddress, setResidentialAddress] = useState("");
  const [emergencyContactName, setEmergencyContactName] = useState("");
  const [emergencyContactPhone, setEmergencyContactPhone] = useState("");
  const [emergencyContactRelation, setEmergencyContactRelation] = useState("");
  const [emergencyContactAddress, setEmergencyContactAddress] = useState("");
  const [photoUrl, setPhotoUrl] = useState("");
  const [saving, setSaving] = useState(false);

  // Pay Salary modal states
  const [payMonth, setPayMonth] = useState("");
  const [payDate, setPayDate] = useState(new Date().toISOString().split("T")[0]);
  const [payBonus, setPayBonus] = useState(0);
  const [payDeduction, setPayDeduction] = useState(0);
  const [payNotes, setPayNotes] = useState("");
  const [paySaving, setPaySaving] = useState(false);

  const { userProfile } = useAuth();
  const router = useRouter();

  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const activeStaff = staff.filter(
    (s) => s.status?.toLowerCase() !== "archived"
  );
  const activeCount = activeStaff.filter((s) => s.status === "Active").length;
  const monthlyPayroll = activeStaff
    .filter((s) => s.status === "Active")
    .reduce((sum, s) => sum + (s.salary || 0), 0);

  const isPremium = Boolean(userProfile?.isPremium || userProfile?.isAdmin);
  const staffLimitReached = !isPremium && activeStaff.length >= TierLimiter.FREE_MAX_STAFF;

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement("canvas");
        const maxDim = 256;
        let w = img.width;
        let h = img.height;
        if (w > h) {
          if (w > maxDim) {
            h = Math.round((h * maxDim) / w);
            w = maxDim;
          }
        } else {
          if (h > maxDim) {
            w = Math.round((w * maxDim) / h);
            h = maxDim;
          }
        }
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext("2d");
        ctx?.drawImage(img, 0, 0, w, h);
        const dataUrl = canvas.toDataURL("image/jpeg", 0.85);
        setPhotoUrl(dataUrl);
      };
      img.src = event.target?.result as string;
    };
    reader.readAsDataURL(file);
  };

  const openAddModal = () => {
    if (staffLimitReached) {
      alert(`Staff limit of ${TierLimiter.FREE_MAX_STAFF} reached for free tier. Please upgrade to add more.`);
      router.push("/dashboard/billing");
      return;
    }
    setName("");
    setRole("");
    setPhone("");
    setSalary(0);
    setStatus("Active");
    setJoinDate(new Date().toISOString().split("T")[0]);
    setAllowAppAccess(false);
    setEmail("");
    setGender("Male");
    setDateOfBirth("");
    setResidentialAddress("");
    setEmergencyContactName("");
    setEmergencyContactPhone("");
    setEmergencyContactRelation("");
    setEmergencyContactAddress("");
    setPhotoUrl("");
    setEditingStaff(null);
    setShowAddModal(true);
  };

  const openEditModal = (member: StaffMember) => {
    setName(member.name || "");
    setRole(member.role || "");
    setPhone(member.phone || "");
    setSalary(member.salary || 0);
    setStatus(member.status || "Active");
    setJoinDate(member.joinDate || new Date().toISOString().split("T")[0]);
    setAllowAppAccess(member.allowAppAccess || false);
    setEmail(member.email || "");
    setGender((member as any).gender || "Male");
    setDateOfBirth((member as any).dateOfBirth || "");
    setResidentialAddress((member as any).residentialAddress || "");
    setEmergencyContactName((member as any).emergencyContactName || "");
    setEmergencyContactPhone((member as any).emergencyContactPhone || "");
    setEmergencyContactRelation((member as any).emergencyContactRelation || "");
    setEmergencyContactAddress((member as any).emergencyContactAddress || "");
    setPhotoUrl(member.photoUrl || "");
    setEditingStaff(member);
    setShowAddModal(true);
  };

  const handleSaveStaff = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !name.trim()) return;

    if (!editingStaff && staffLimitReached) {
      alert(`Staff limit of ${TierLimiter.FREE_MAX_STAFF} reached for free tier. Please upgrade to add more.`);
      router.push("/dashboard/billing");
      return;
    }

    const effectiveAppAccess = isPremium ? allowAppAccess : false;
    const staffEmail = email.trim().toLowerCase();

    setSaving(true);
    try {
      const payload: Partial<StaffMember> = {
        name: name.trim(),
        role: role.trim(),
        phone: phone.trim(),
        salary: Number(salary) || 0,
        status,
        joinDate,
        allowAppAccess: effectiveAppAccess,
        email: staffEmail,
        gender,
        dateOfBirth,
        residentialAddress,
        emergencyContactName,
        emergencyContactPhone,
        emergencyContactRelation,
        emergencyContactAddress,
        photoUrl,
        updatedAt: Date.now(),
      };

      if (editingStaff) {
        const staffRef = doc(db, "users", activeFarmUid, "staff", editingStaff.id);
        await updateDoc(staffRef, payload);

        // If email changed or app access revoked, delete old registry entry
        if (editingStaff.email && (editingStaff.email.trim().toLowerCase() !== staffEmail || !effectiveAppAccess)) {
          await deleteDoc(doc(db, "staff_registry", editingStaff.email.trim().toLowerCase()));
        }
      } else {
        const staffCol = collection(db, "users", activeFarmUid, "staff");
        await addDoc(staffCol, {
          ...payload,
          createdAt: Date.now(),
        });
      }

      // If app access is enabled and email is present, register in staff_registry
      if (effectiveAppAccess && staffEmail) {
        await setDoc(doc(db, "staff_registry", staffEmail), {
          managerUid: activeFarmUid,
          email: staffEmail,
          updatedAt: Date.now(),
        });
      }

      setShowAddModal(false);
      setEditingStaff(null);
    } catch (err) {
      console.error("Error saving staff member:", err);
    } finally {
      setSaving(false);
    }
  };

  const handleArchiveStaff = async () => {
    if (!editingStaff || !activeFarmUid) return;
    if (!confirm(`Are you sure you want to archive ${editingStaff.name}? Their historical records and salary history will be preserved.`)) return;

    setSaving(true);
    try {
      const staffRef = doc(db, "users", activeFarmUid, "staff", editingStaff.id);
      await updateDoc(staffRef, { status: "Archived", allowAppAccess: false });
      if (editingStaff.email) {
        try {
          await deleteDoc(doc(db, "staff_registry", editingStaff.email.trim().toLowerCase()));
        } catch (_) {}
      }
      setShowAddModal(false);
      setEditingStaff(null);
    } catch (err) {
      console.error("Error archiving staff:", err);
    } finally {
      setSaving(false);
    }
  };

  const handlePaySalarySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!staffToPaySalary || !activeFarmUid) return;

    setPaySaving(true);
    try {
      const base = Number(staffToPaySalary.salary) || 0;
      const bonus = Number(payBonus) || 0;
      const deduction = Number(payDeduction) || 0;
      const net = Math.max(0, base + bonus - deduction);

      const record = {
        date: payDate || new Date().toISOString().split("T")[0],
        type: "Expense",
        category: "Salary",
        description: `Salary payment to ${staffToPaySalary.name} (${staffToPaySalary.role}) - Month: ${payMonth || "Current"}. Base: ${currencySymbol}${base.toFixed(2)}, Bonus: +${currencySymbol}${bonus.toFixed(2)}, Deduction: -${currencySymbol}${deduction.toFixed(2)}. Net: ${currencySymbol}${net.toFixed(2)}. Notes: ${payNotes || "None"}`,
        amount: net,
        createdAt: Date.now(),
      };

      await addDoc(collection(db, "users", activeFarmUid, "financials"), record);
      setStaffToPaySalary(null);
      setPayBonus(0);
      setPayDeduction(0);
      setPayNotes("");
    } catch (err) {
      console.error("Error logging salary payment:", err);
    } finally {
      setPaySaving(false);
    }
  };

  const handleExportPdf = () => {
    if (!isPremium) {
      alert("Executive HR PDF export is a SmartSwine Premium feature. Please upgrade to export HR reports.");
      router.push("/dashboard/billing");
      return;
    }
    if (typeof window !== "undefined") {
      window.print();
    }
  };

  return (
    <div className="space-y-4">
      {/* Overview Card */}
      <div className="bg-white border border-purple-100 rounded-2xl p-4 shadow-sm">
        <div className="grid grid-cols-3 gap-2 text-center sm:text-left">
          <div className="p-3 rounded-xl bg-purple-50/70 border border-purple-100">
            <p className="text-[10px] sm:text-[11px] font-bold text-purple-700 uppercase tracking-wider">{t("totalStaff") || "Total Staff"}</p>
            <p className="text-base sm:text-xl font-black text-purple-900 mt-0.5">{staff.length}</p>
          </div>
          <div className="p-3 rounded-xl bg-purple-50/70 border border-purple-100">
            <p className="text-[10px] sm:text-[11px] font-bold text-purple-700 uppercase tracking-wider">{t("active") || "Active Staff"}</p>
            <p className="text-base sm:text-xl font-black text-purple-900 mt-0.5">{activeCount}</p>
          </div>
          <div className="p-3 rounded-xl bg-purple-50/70 border border-purple-100">
            <p className="text-[10px] sm:text-[11px] font-bold text-purple-700 uppercase tracking-wider">{t("monthlyPayroll") || "Monthly Payroll"}</p>
            <p className="text-base sm:text-xl font-black text-purple-900 mt-0.5 truncate">
              {currencySymbol}{monthlyPayroll.toFixed(2)}
            </p>
          </div>
        </div>
      </div>

      {/* Action Buttons: Add Employee & Export to PDF */}
      <div className="space-y-2">
        <button
          type="button"
          onClick={openAddModal}
          className={`w-full py-2.5 px-4 rounded-xl text-white text-xs font-bold shadow-sm transition flex items-center justify-center gap-2 ${
            staffLimitReached
              ? "bg-amber-600 hover:bg-amber-700 shadow-amber-600/10"
              : "bg-purple-700 hover:bg-purple-800"
          }`}
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" d="M10 5a1 1 0 011 1v3h3a1 1 0 110 2h-3v3a1 1 0 11-2 0v-3H6a1 1 0 110-2h3V6a1 1 0 011-1z" clipRule="evenodd" />
          </svg>
          {staffLimitReached
            ? `Staff Limit Reached (${TierLimiter.FREE_MAX_STAFF}) - Upgrade`
            : (t("addStaff") || "Add Employee")}
        </button>

        <button
          type="button"
          onClick={handleExportPdf}
          className={`w-full py-2.5 px-4 rounded-xl border text-xs font-bold transition flex items-center justify-center gap-2 ${
            isPremium
              ? "border-purple-700 text-purple-700 hover:bg-purple-50"
              : "border-amber-300 bg-amber-50 text-amber-800 hover:bg-amber-100"
          }`}
        >
          <ExportPdfIcon className="h-4 w-4" />
          {isPremium ? (t("exportPdf") || "Export to PDF") : "Export to PDF (Premium)"}
        </button>
      </div>

      {/* Staff List */}
      <div className="space-y-2.5">
        {activeStaff.length === 0 ? (
          <div className="p-4 bg-white rounded-xl border border-zinc-200 text-center text-xs text-zinc-500">
            {t("noStaff") || "No active staff members found."}
          </div>
        ) : (
          activeStaff.map((member) => (
            <div
              key={member.id}
              className="p-3.5 rounded-xl bg-white border border-purple-100 hover:border-purple-300 hover:shadow-sm transition space-y-3"
            >
              {/* Top Row: Click to view details */}
              <div
                onClick={() => setStaffToViewProfile(member)}
                className="flex items-center gap-3 cursor-pointer group"
              >
                <div className="h-11 w-11 rounded-full bg-purple-100 border border-purple-200 flex items-center justify-center shrink-0 overflow-hidden text-purple-700 font-bold text-sm">
                  {member.photoUrl ? (
                    <img src={member.photoUrl} alt={member.name} className="h-full w-full object-cover" />
                  ) : (
                    member.name.charAt(0).toUpperCase()
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between">
                    <h4 className="text-xs sm:text-sm font-bold text-zinc-900 group-hover:text-purple-700 transition truncate">
                      {member.name}
                    </h4>
                    <span className="text-[10px] text-purple-600 font-medium ml-2">Details ➔</span>
                  </div>
                  <p className="text-[11px] text-zinc-500 truncate">
                    {member.role} • {member.phone || "No phone"}
                  </p>
                </div>
              </div>

              {/* Action Buttons: Log Salary and Edit */}
              <div className="pt-2 border-t border-zinc-100 flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => {
                    setStaffToPaySalary(member);
                    setPayMonth(new Date().toLocaleString("default", { month: "long", year: "numeric" }));
                    setPayDate(new Date().toISOString().split("T")[0]);
                    setPayBonus(0);
                    setPayDeduction(0);
                    setPayNotes("");
                  }}
                  className="flex-1 py-1.5 px-3 rounded-lg bg-purple-50 hover:bg-purple-100 text-purple-700 text-xs font-bold transition flex items-center justify-center gap-1.5 border border-purple-200/60"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5 text-purple-700" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z" />
                    <path fillRule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clipRule="evenodd" />
                  </svg>
                  Log Salary
                </button>
                <button
                  type="button"
                  onClick={() => openEditModal(member)}
                  className="flex-1 py-1.5 px-3 rounded-lg border border-zinc-300 hover:bg-zinc-50 text-zinc-700 text-xs font-bold transition flex items-center justify-center gap-1.5"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5 text-zinc-500" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                  </svg>
                  {t("edit") || "Edit"}
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Printable HR Report (Active on window.print) */}
      <div className="hidden print:block print:p-6 print:text-black">
        <div className="border-b-2 border-purple-800 pb-3 mb-4">
          <h1 className="text-2xl font-black text-purple-900">SmartSwine — Human Resources Report</h1>
          <p className="text-xs text-zinc-600">Generated: {new Date().toLocaleDateString()} {new Date().toLocaleTimeString()}</p>
        </div>
        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="p-3 border border-purple-200 rounded-lg">
            <p className="text-[10px] font-bold uppercase text-purple-700">{t("totalStaff") || "Total Staff"}</p>
            <p className="text-xl font-bold text-zinc-900">{staff.length}</p>
          </div>
          <div className="p-3 border border-purple-200 rounded-lg">
            <p className="text-[10px] font-bold uppercase text-purple-700">{t("active") || "Active Staff"}</p>
            <p className="text-xl font-bold text-zinc-900">{activeCount}</p>
          </div>
          <div className="p-3 border border-purple-200 rounded-lg">
            <p className="text-[10px] font-bold uppercase text-purple-700">{t("monthlyPayroll") || "Monthly Payroll"}</p>
            <p className="text-xl font-bold text-zinc-900">{currencySymbol}{monthlyPayroll.toFixed(2)}</p>
          </div>
        </div>
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b-2 border-zinc-300 bg-purple-50">
              <th className="p-2 font-bold">{t("fullName") || "Name"}</th>
              <th className="p-2 font-bold">{t("role") || "Role"}</th>
              <th className="p-2 font-bold">{t("status") || "Status"}</th>
              <th className="p-2 font-bold">{t("phone") || "Phone"}</th>
              <th className="p-2 font-bold">{t("salary") || "Monthly Salary"}</th>
              <th className="p-2 font-bold">{t("joined") || "Joined Date"}</th>
            </tr>
          </thead>
          <tbody>
            {activeStaff.map((m) => (
              <tr key={m.id} className="border-b border-zinc-200">
                <td className="p-2 font-bold text-zinc-900">{m.name}</td>
                <td className="p-2 text-zinc-700">{m.role}</td>
                <td className="p-2 font-semibold text-purple-800">{m.status}</td>
                <td className="p-2 text-zinc-600">{m.phone || "—"}</td>
                <td className="p-2 font-bold text-zinc-900">{currencySymbol}{Number(m.salary || 0).toFixed(2)}</td>
                <td className="p-2 text-zinc-600">{m.joinDate || "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Add / Edit Staff Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl w-full max-w-md p-5 space-y-4 shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-zinc-150">
              <h3 className="text-base font-bold text-zinc-900">
                {editingStaff ? (t("editStaffDetails") || "Edit Employee") : (t("registerStaffMember") || "Add New Employee")}
              </h3>
              <button
                type="button"
                onClick={() => setShowAddModal(false)}
                className="text-zinc-400 hover:text-zinc-600 font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveStaff} className="space-y-3.5">
              {/* Photo Upload / Camera Option */}
              <div className="flex items-center gap-4 p-3 bg-purple-50/40 rounded-xl border border-purple-100">
                <div className="h-16 w-16 rounded-full bg-purple-100 border border-purple-200 flex items-center justify-center overflow-hidden shrink-0">
                  {photoUrl ? (
                    <img src={photoUrl} alt="Preview" className="h-full w-full object-cover" />
                  ) : (
                    <span className="text-purple-400 text-2xl font-bold">👤</span>
                  )}
                </div>
                <div className="space-y-1.5 flex-1">
                  <p className="text-xs font-bold text-zinc-800">Employee Photo</p>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => fileInputRef.current?.click()}
                      className="py-1 px-2.5 rounded-lg bg-purple-700 hover:bg-purple-800 text-white text-[11px] font-bold transition"
                    >
                      Upload / Camera
                    </button>
                    {photoUrl && (
                      <button
                        type="button"
                        onClick={() => setPhotoUrl("")}
                        className="py-1 px-2.5 rounded-lg border border-rose-300 text-rose-600 hover:bg-rose-50 text-[11px] font-semibold transition"
                      >
                        Remove
                      </button>
                    )}
                  </div>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={handleImageChange}
                    className="hidden"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-600 mb-1">{t("fullName") || "Full Name"} *</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. John Doe"
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs sm:text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-purple-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 mb-1">{t("role") || "Role"} *</label>
                  <input
                    type="text"
                    required
                    value={role}
                    onChange={(e) => setRole(e.target.value)}
                    placeholder="e.g. Attendant"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs sm:text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-purple-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 mb-1">{t("status") || "Status"}</label>
                  <select
                    value={status}
                    onChange={(e) => setStatus(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs sm:text-sm text-zinc-900 focus:outline-none"
                  >
                    <option value="Active">{t("active") || "Active"}</option>
                    <option value="Inactive">{t("inactive") || "Inactive"}</option>
                    <option value="On Leave">{t("onLeave") || "On Leave"}</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 mb-1">{t("phone") || "Phone"} *</label>
                  <input
                    type="tel"
                    required
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="e.g. 0244123456"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs sm:text-sm text-zinc-900 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 mb-1">{t("salary") || "Monthly Salary"} ({currencySymbol})</label>
                  <input
                    type="number"
                    step="any"
                    value={salary}
                    onChange={(e) => setSalary(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs sm:text-sm text-zinc-900 focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 mb-1">Gender</label>
                  <select
                    value={gender}
                    onChange={(e) => setGender(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs sm:text-sm text-zinc-900 focus:outline-none"
                  >
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 mb-1">Date of Birth</label>
                  <input
                    type="date"
                    value={dateOfBirth}
                    onChange={(e) => setDateOfBirth(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs sm:text-sm text-zinc-900 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-600 mb-1">Residential Address</label>
                <input
                  type="text"
                  value={residentialAddress}
                  onChange={(e) => setResidentialAddress(e.target.value)}
                  placeholder="e.g. House No 12, Piggery Lane"
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs sm:text-sm text-zinc-900 focus:outline-none"
                />
              </div>

              {/* Emergency Contact */}
              <div className="p-3 bg-zinc-50 rounded-xl border border-zinc-200 space-y-2">
                <p className="text-[11px] font-bold text-zinc-700 uppercase tracking-wider">Emergency Contact</p>
                <div className="grid grid-cols-2 gap-2">
                  <input
                    type="text"
                    value={emergencyContactName}
                    onChange={(e) => setEmergencyContactName(e.target.value)}
                    placeholder="Contact Name"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-2.5 py-1.5 text-xs text-zinc-900"
                  />
                  <input
                    type="tel"
                    value={emergencyContactPhone}
                    onChange={(e) => setEmergencyContactPhone(e.target.value)}
                    placeholder="Contact Phone"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-2.5 py-1.5 text-xs text-zinc-900"
                  />
                </div>
              </div>

              {/* App Access */}
              <div className="space-y-2">
                <label className="flex items-center gap-2 cursor-pointer text-xs text-zinc-700">
                  <input
                    type="checkbox"
                    checked={allowAppAccess}
                    disabled={!isPremium}
                    onChange={(e) => {
                      if (!isPremium) {
                        alert("Staff mobile app access requires SmartSwine Premium. Please upgrade to enable worker logins.");
                        router.push("/dashboard/billing");
                        return;
                      }
                      setAllowAppAccess(e.target.checked);
                    }}
                    className="h-4 w-4 rounded border-zinc-300 text-purple-700 focus:ring-purple-500 disabled:opacity-50"
                  />
                  <span>
                    {t("allowAppAccess") || "Allow Mobile App Access for Worker"}
                    {!isPremium && <span className="text-amber-600 font-bold text-[10px] ml-1.5">(Premium Only)</span>}
                  </span>
                </label>
                {allowAppAccess && (
                  <input
                    type="email"
                    required={allowAppAccess}
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="Employee Email Address"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs sm:text-sm text-zinc-900 focus:outline-none"
                  />
                )}
              </div>

              {/* Action Buttons */}
              <div className="flex items-center justify-between gap-2 pt-3 border-t border-zinc-150">
                {editingStaff ? (
                  <button
                    type="button"
                    onClick={handleArchiveStaff}
                    disabled={saving}
                    className="py-2 px-3 rounded-lg border border-rose-300 text-rose-700 hover:bg-rose-50 text-xs font-bold transition"
                  >
                    {t("remove") || "Archive Employee"}
                  </button>
                ) : <div />}

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setShowAddModal(false)}
                    className="py-2 px-3 rounded-lg border border-zinc-300 text-zinc-600 hover:bg-zinc-50 text-xs font-semibold"
                  >
                    {t("cancel") || "Cancel"}
                  </button>
                  <button
                    type="submit"
                    disabled={saving}
                    className="py-2 px-4 rounded-lg bg-purple-700 hover:bg-purple-800 text-white text-xs font-bold shadow-sm transition"
                  >
                    {saving ? "Saving..." : editingStaff ? (t("saveUpdates") || "Save Updates") : (t("registerStaff") || "Register Employee")}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Staff Profile Details Modal */}
      {staffToViewProfile && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-fadeIn">
          <div className="bg-white rounded-2xl w-full max-w-sm p-6 space-y-4 shadow-2xl text-center">
            <div className="h-24 w-24 rounded-full bg-purple-100 border-2 border-purple-200 mx-auto overflow-hidden flex items-center justify-center text-3xl font-bold text-purple-700 shadow-sm">
              {staffToViewProfile.photoUrl ? (
                <img src={staffToViewProfile.photoUrl} alt={staffToViewProfile.name} className="h-full w-full object-cover" />
              ) : (
                staffToViewProfile.name.charAt(0).toUpperCase()
              )}
            </div>

            <div>
              <h3 className="text-lg font-black text-zinc-900">{staffToViewProfile.name}</h3>
              <p className="text-xs font-semibold text-purple-700 mt-0.5">{staffToViewProfile.role}</p>
            </div>

            <div className="p-3.5 bg-zinc-50 rounded-xl border border-zinc-150 text-left space-y-2 text-xs">
              <div className="flex justify-between">
                <span className="text-zinc-500">{t("phone") || "Contact Number"}:</span>
                <span className="font-bold text-zinc-800">{staffToViewProfile.phone || "None"}</span>
              </div>
              {staffToViewProfile.email && (
                <div className="flex justify-between">
                  <span className="text-zinc-500">{t("email") || "Email"}:</span>
                  <span className="font-bold text-zinc-800 truncate ml-2">{staffToViewProfile.email}</span>
                </div>
              )}
              <div className="flex justify-between">
                <span className="text-zinc-500">{t("status") || "Status"}:</span>
                <span className="font-bold text-purple-700">{staffToViewProfile.status}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">{t("salary") || "Monthly Salary"}:</span>
                <span className="font-bold text-zinc-800">{currencySymbol}{staffToViewProfile.salary?.toFixed(2)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">{t("joined") || "Joined Date"}:</span>
                <span className="font-bold text-zinc-800">{staffToViewProfile.joinDate || "N/A"}</span>
              </div>
            </div>

            <div className="flex items-center gap-2 pt-2">
              <button
                type="button"
                onClick={() => {
                  setStaffForFullDetails(staffToViewProfile);
                  setStaffToViewProfile(null);
                }}
                className="flex-1 py-2 px-3 rounded-xl border border-purple-600 text-purple-700 hover:bg-purple-50 text-xs font-bold transition flex items-center justify-center gap-1.5"
              >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                <span>View Details</span>
              </button>

              <button
                type="button"
                onClick={() => setStaffToViewProfile(null)}
                className="flex-1 py-2 px-3 rounded-xl bg-purple-700 hover:bg-purple-800 text-white text-xs font-bold transition shadow-sm"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Staff Full Details Modal */}
      {staffForFullDetails && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-fadeIn">
          <div className="bg-white rounded-2xl w-full max-w-lg p-6 space-y-4 shadow-2xl max-h-[90vh] overflow-y-auto">
            {/* Header */}
            <div className="flex items-center justify-between pb-3 border-b border-zinc-150">
              <div className="flex items-center gap-3">
                <div className="h-14 w-14 rounded-full bg-purple-100 border border-purple-200 flex items-center justify-center text-xl font-bold text-purple-700 overflow-hidden shrink-0">
                  {staffForFullDetails.photoUrl ? (
                    <img src={staffForFullDetails.photoUrl} alt={staffForFullDetails.name} className="h-full w-full object-cover" />
                  ) : (
                    staffForFullDetails.name.charAt(0).toUpperCase()
                  )}
                </div>
                <div>
                  <h3 className="text-base font-black text-zinc-900">{staffForFullDetails.name}</h3>
                  <p className="text-xs font-semibold text-purple-700">{staffForFullDetails.role}</p>
                  <span className={`inline-block text-[10px] font-bold px-2 py-0.5 rounded-full mt-1 ${
                    staffForFullDetails.status === "Active" ? "bg-emerald-50 text-emerald-800 border border-emerald-200" : "bg-amber-50 text-amber-800 border border-amber-200"
                  }`}>
                    {staffForFullDetails.status}
                  </span>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setStaffForFullDetails(null)}
                className="text-zinc-400 hover:text-zinc-600 font-bold p-1"
              >
                ✕
              </button>
            </div>

            {/* Content Sections */}
            <div className="space-y-3.5 text-xs">
              {/* Personal Information */}
              <div className="bg-purple-50/40 p-3.5 rounded-xl border border-purple-100 space-y-2">
                <h4 className="font-bold text-purple-900 uppercase text-[11px] tracking-wider">Personal Information</h4>
                <div className="grid grid-cols-2 gap-2 text-zinc-700">
                  <div>
                    <span className="text-zinc-400 block text-[10px]">Gender</span>
                    <span className="font-semibold">{staffForFullDetails.gender || "—"}</span>
                  </div>
                  <div>
                    <span className="text-zinc-400 block text-[10px]">Date of Birth</span>
                    <span className="font-semibold">{staffForFullDetails.dateOfBirth || "—"}</span>
                  </div>
                  <div>
                    <span className="text-zinc-400 block text-[10px]">Phone</span>
                    <span className="font-semibold">{staffForFullDetails.phone || "—"}</span>
                  </div>
                  <div>
                    <span className="text-zinc-400 block text-[10px]">Email</span>
                    <span className="font-semibold">{staffForFullDetails.email || "—"}</span>
                  </div>
                  <div className="col-span-2">
                    <span className="text-zinc-400 block text-[10px]">Residential Address</span>
                    <span className="font-semibold">{staffForFullDetails.residentialAddress || "—"}</span>
                  </div>
                </div>
              </div>

              {/* Employment & Compensation */}
              <div className="bg-zinc-50 p-3.5 rounded-xl border border-zinc-200 space-y-2">
                <h4 className="font-bold text-zinc-900 uppercase text-[11px] tracking-wider">Employment & Compensation</h4>
                <div className="grid grid-cols-2 gap-2 text-zinc-700">
                  <div>
                    <span className="text-zinc-400 block text-[10px]">Date Joined</span>
                    <span className="font-semibold">{staffForFullDetails.joinDate || "—"}</span>
                  </div>
                  <div>
                    <span className="text-zinc-400 block text-[10px]">Base Monthly Salary</span>
                    <span className="font-black text-purple-800">{currencySymbol}{staffForFullDetails.salary?.toFixed(2)}</span>
                  </div>
                  <div className="col-span-2">
                    <span className="text-zinc-400 block text-[10px]">Mobile App Access</span>
                    <span className="font-semibold">{staffForFullDetails.allowAppAccess ? "Enabled (Worker Account)" : "Disabled"}</span>
                  </div>
                </div>
              </div>

              {/* Emergency Contact */}
              <div className="bg-rose-50/40 p-3.5 rounded-xl border border-rose-100 space-y-2">
                <h4 className="font-bold text-rose-900 uppercase text-[11px] tracking-wider">Emergency Contact</h4>
                <div className="grid grid-cols-2 gap-2 text-zinc-700">
                  <div>
                    <span className="text-zinc-400 block text-[10px]">Contact Person</span>
                    <span className="font-semibold">{staffForFullDetails.emergencyContactName || "—"}</span>
                  </div>
                  <div>
                    <span className="text-zinc-400 block text-[10px]">Relationship</span>
                    <span className="font-semibold">{staffForFullDetails.emergencyContactRelation || "—"}</span>
                  </div>
                  <div>
                    <span className="text-zinc-400 block text-[10px]">Emergency Phone</span>
                    <span className="font-semibold">{staffForFullDetails.emergencyContactPhone || "—"}</span>
                  </div>
                  <div>
                    <span className="text-zinc-400 block text-[10px]">Emergency Address</span>
                    <span className="font-semibold">{staffForFullDetails.emergencyContactAddress || "—"}</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="flex justify-end pt-2">
              <button
                type="button"
                onClick={() => setStaffForFullDetails(null)}
                className="py-2 px-5 rounded-xl bg-purple-700 hover:bg-purple-800 text-white text-xs font-bold transition shadow-sm"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Pay Salary Modal */}
      {staffToPaySalary && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-fadeIn">
          <div className="bg-white rounded-2xl w-full max-w-md p-5 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between pb-3 border-b border-zinc-150">
              <div>
                <h3 className="text-base font-bold text-zinc-900">Log Salary Payment</h3>
                <p className="text-xs text-purple-700">{staffToPaySalary.name} ({staffToPaySalary.role})</p>
              </div>
              <button
                type="button"
                onClick={() => setStaffToPaySalary(null)}
                className="text-zinc-400 hover:text-zinc-600 font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handlePaySalarySubmit} className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 mb-1">Payroll Month</label>
                  <input
                    type="text"
                    required
                    value={payMonth}
                    onChange={(e) => setPayMonth(e.target.value)}
                    placeholder="e.g. September 2026"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs text-zinc-900"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 mb-1">Payment Date</label>
                  <input
                    type="date"
                    required
                    value={payDate}
                    onChange={(e) => setPayDate(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs text-zinc-900"
                  />
                </div>
              </div>

              <div className="p-3 bg-purple-50/50 rounded-xl border border-purple-100 flex items-center justify-between">
                <span className="text-xs text-zinc-600 font-medium">Base Contract Salary</span>
                <span className="text-sm font-bold text-purple-900">{currencySymbol}{staffToPaySalary.salary?.toFixed(2)}</span>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 mb-1">Bonus (+)</label>
                  <input
                    type="number"
                    step="any"
                    value={payBonus}
                    onChange={(e) => setPayBonus(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs text-zinc-900"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-600 mb-1">Deductions (-)</label>
                  <input
                    type="number"
                    step="any"
                    value={payDeduction}
                    onChange={(e) => setPayDeduction(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs text-zinc-900"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-600 mb-1">Notes / Description</label>
                <input
                  type="text"
                  value={payNotes}
                  onChange={(e) => setPayNotes(e.target.value)}
                  placeholder="e.g. Overtime pay included"
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs text-zinc-900"
                />
              </div>

              <div className="p-3 bg-emerald-50 rounded-xl border border-emerald-100 flex items-center justify-between">
                <span className="text-xs font-bold text-emerald-800">Net Amount Paid</span>
                <span className="text-base font-black text-emerald-900">
                  {currencySymbol}
                  {Math.max(0, (staffToPaySalary.salary || 0) + payBonus - payDeduction).toFixed(2)}
                </span>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setStaffToPaySalary(null)}
                  className="py-2 px-3 rounded-lg border border-zinc-300 text-zinc-600 text-xs font-semibold"
                >
                  {t("cancel") || "Cancel"}
                </button>
                <button
                  type="submit"
                  disabled={paySaving}
                  className="py-2 px-4 rounded-lg bg-purple-700 hover:bg-purple-800 text-white text-xs font-bold transition shadow-sm"
                >
                  {paySaving ? "Logging..." : "Confirm Payment"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isPremium && (
        <HRReport
          staff={activeStaff}
          financialRecords={[]}
          currencySymbol={currencySymbol}
        />
      )}
    </div>
  );
}
