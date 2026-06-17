"use client";

import React, { useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { doc, onSnapshot, updateDoc, collection, setDoc, deleteDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";

interface Pig {
  id: string;
  tagNumber: string;
  birthDate: string;
  breed: string;
  gender: string;
  weight: number;
  purpose: string;
  sowTag: string;
  boarTag: string;
  location: string;
  source: string;
  status: string;
  notes: string;
  castrated?: boolean;
  castrationDate?: string;
  teethClipped?: boolean;
  tailDocked?: boolean;
  ironInjections?: number;
  weaned?: boolean;
}

interface HealthRecord {
  id: string;
  date: string;
  type: string;
  description: string;
}

export default function PigProfilePage() {
  const { user, activeFarmUid, loading } = useAuth();
  const router = useRouter();
  const params = useParams();
  const pigId = params?.id as string;

  const [pig, setPig] = useState<Pig | null>(null);
  const [healthRecords, setHealthRecords] = useState<HealthRecord[]>([]);
  const [dataLoading, setDataLoading] = useState(true);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showRecordModal, setShowRecordModal] = useState(false);

  // Edit fields
  const [breed, setBreed] = useState("");
  const [purpose, setPurpose] = useState("");
  const [location, setLocation] = useState("");
  const [status, setStatus] = useState("");
  const [weight, setWeight] = useState(0);
  const [notes, setNotes] = useState("");

  // Log Health Record fields
  const [recordDate, setRecordDate] = useState(new Date().toISOString().split("T")[0]);
  const [recordType, setRecordType] = useState("Medication");
  const [recordDesc, setRecordDesc] = useState("");

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!activeFarmUid || !pigId) return;

    setDataLoading(true);

    // 1. Listen to Pig Document
    const pigDocRef = doc(db, "users", activeFarmUid, "pigs", pigId);
    const unsubscribePig = onSnapshot(pigDocRef, (snapshot) => {
      if (snapshot.exists()) {
        const data = { id: snapshot.id, ...snapshot.data() } as Pig;
        setPig(data);
        setBreed(data.breed);
        setPurpose(data.purpose);
        setLocation(data.location || "");
        setStatus(data.status);
        setWeight(data.weight || 0);
        setNotes(data.notes || "");
      } else {
        setPig(null);
      }
    }, (error) => console.error("Error fetching pig details:", error));

    // 2. Listen to Pig Health Records Subcollection
    const recordsRef = collection(db, "users", activeFarmUid, "pigs", pigId, "health_records");
    const unsubscribeRecords = onSnapshot(recordsRef, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as HealthRecord));
      setHealthRecords(list.sort((a, b) => b.date.localeCompare(a.date)));
      setDataLoading(false);
    }, (error) => {
      console.error("Error fetching health records:", error);
      setDataLoading(false);
    });

    return () => {
      unsubscribePig();
      unsubscribeRecords();
    };
  }, [activeFarmUid, pigId]);

  const handleUpdatePig = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !pigId) return;

    try {
      const pigDocRef = doc(db, "users", activeFarmUid, "pigs", pigId);
      await updateDoc(pigDocRef, {
        breed,
        purpose,
        location,
        status,
        weight,
        notes
      });
      setShowEditModal(false);
    } catch (err) {
      console.error("Update failed:", err);
    }
  };

  const handleAddHealthRecord = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid || !pigId) return;

    try {
      const recordsRef = collection(db, "users", activeFarmUid, "pigs", pigId, "health_records");
      const newRef = doc(recordsRef);
      await setDoc(newRef, {
        id: newRef.id,
        date: recordDate,
        type: recordType,
        description: recordDesc
      });
      setRecordDesc("");
      setShowRecordModal(false);
    } catch (err) {
      console.error("Adding record failed:", err);
    }
  };

  const handleDeletePig = async () => {
    if (!activeFarmUid || !pigId || !confirm("Are you sure you want to delete this pig profile?")) return;
    try {
      const pigDocRef = doc(db, "users", activeFarmUid, "pigs", pigId);
      await deleteDoc(pigDocRef);
      router.push("/dashboard/herd");
    } catch (err) {
      console.error("Failed to delete pig:", err);
    }
  };

  if (loading || !user || dataLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-white text-zinc-900">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  if (!pig) {
    return (
      <div className="flex h-screen flex-col items-center justify-center bg-white text-zinc-900 p-4">
        <p className="text-lg font-semibold text-zinc-500">Pig profile not found.</p>
        <Link href="/dashboard/herd" className="mt-4 text-emerald-600 hover:underline">
          Back to Herd
        </Link>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen bg-white text-zinc-900 flex flex-col font-sans overflow-hidden">
      {/* Watermark Logo Background */}
      <div className="fixed inset-0 z-0 flex items-center justify-center opacity-[0.15] pointer-events-none select-none">
        <img
          src="/app_logo.png"
          alt="Watermark Background Logo"
          className="w-full max-w-[1100px] max-h-[85vh] object-contain"
        />
      </div>

      <div className="relative z-10 flex flex-col min-h-screen">
        <header className="border-b border-zinc-200 bg-white/80 backdrop-blur-md sticky top-0 z-50">
          <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <Link href="/dashboard/herd" className="text-zinc-500 hover:text-zinc-950 transition">
                ← Herd Data
              </Link>
              <span className="text-zinc-300">|</span>
              <span className="font-bold text-lg text-zinc-900 flex items-center gap-2">
                🐖 Pig Profile: {pig.tagNumber}
              </span>
            </div>

            <div className="flex gap-2">
              <button
                onClick={() => setShowEditModal(true)}
                className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-650 hover:bg-zinc-100 hover:text-zinc-950 transition"
              >
                ✏️ Edit Details
              </button>
              <button
                onClick={() => setShowRecordModal(true)}
                className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white shadow-lg shadow-emerald-600/10 transition"
              >
                + Log Health
              </button>
              <Link
                href="/dashboard"
                className="text-xs font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200/50 px-3 py-1.5 rounded-lg transition duration-200 flex items-center justify-center"
              >
                Back to Home
              </Link>
            </div>
          </div>
        </header>

        <main className="flex-1 max-w-4xl w-full mx-auto px-4 py-8 grid grid-cols-1 md:grid-cols-12 gap-8">
          {/* Left Side: Summary Card */}
          <div className="md:col-span-5 space-y-6">
            <div className="bg-white/70 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-4 relative overflow-hidden">
              <div className="absolute top-0 right-0 h-16 w-16 rounded-full bg-emerald-500/5 blur-lg" />
              <div>
                <p className="text-xs font-semibold text-zinc-400 font-mono">Status & Location</p>
                <h2 className="text-xl font-bold text-zinc-900 mt-1">{pig.tagNumber}</h2>
                <p className="text-sm text-zinc-500 mt-0.5">{pig.breed}</p>
              </div>

              <div className="divide-y divide-zinc-100 text-sm">
                <div className="py-2.5 flex justify-between">
                  <span className="text-zinc-500">Gender</span>
                  <span className="font-semibold">{pig.gender}</span>
                </div>
                <div className="py-2.5 flex justify-between">
                  <span className="text-zinc-500">Purpose</span>
                  <span className="font-semibold">{pig.purpose}</span>
                </div>
                <div className="py-2.5 flex justify-between">
                  <span className="text-zinc-500">Current Status</span>
                  <span className="font-semibold text-emerald-700">{pig.status}</span>
                </div>
                <div className="py-2.5 flex justify-between">
                  <span className="text-zinc-500">Weight</span>
                  <span className="font-semibold">{pig.weight} kg</span>
                </div>
                <div className="py-2.5 flex justify-between">
                  <span className="text-zinc-500">Location</span>
                  <span className="font-semibold">{pig.location || "Unassigned"}</span>
                </div>
                <div className="py-2.5 flex justify-between">
                  <span className="text-zinc-500">Birth Date</span>
                  <span className="font-semibold">{pig.birthDate}</span>
                </div>
                <div className="py-2.5 flex justify-between">
                  <span className="text-zinc-500">Sow Tag</span>
                  <span className="font-semibold font-mono">{pig.sowTag || "N/A"}</span>
                </div>
                <div className="py-2.5 flex justify-between">
                  <span className="text-zinc-500">Boar Tag</span>
                  <span className="font-semibold font-mono">{pig.boarTag || "N/A"}</span>
                </div>
              </div>

              {pig.notes && (
                <div className="bg-zinc-50/70 p-3 rounded-lg border border-zinc-150 text-xs text-zinc-600">
                  <p className="font-bold text-zinc-500 uppercase text-[9px] mb-1">Notes</p>
                  {pig.notes}
                </div>
              )}

              <button
                onClick={handleDeletePig}
                className="w-full text-center text-xs font-semibold text-rose-600 hover:text-rose-700 pt-2 border-t border-zinc-100 hover:underline"
              >
                Delete Profile
              </button>
            </div>
          </div>

          {/* Right Side: Health Timeline */}
          <div className="md:col-span-7 space-y-6">
            <div className="bg-white/70 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm">
              <h3 className="text-lg font-bold text-zinc-900 mb-4">Health History</h3>

              {healthRecords.length === 0 ? (
                <p className="text-sm text-zinc-500 text-center py-12">No health actions or procedures logged.</p>
              ) : (
                <div className="relative border-l border-zinc-200 pl-4 ml-2 space-y-6">
                  {healthRecords.map((record) => (
                    <div key={record.id} className="relative">
                      {/* Timeline dot */}
                      <span className="absolute -left-[21px] top-1.5 h-3.5 w-3.5 rounded-full border-2 border-emerald-500 bg-white" />
                      <div>
                        <div className="flex justify-between items-start">
                          <p className="text-sm font-bold text-zinc-800">{record.type}</p>
                          <span className="text-xs text-zinc-400 font-mono">{record.date}</span>
                        </div>
                        {record.description && (
                          <p className="text-xs text-zinc-500 mt-1 whitespace-pre-line">{record.description}</p>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </main>
      </div>

      {/* Edit Details Modal */}
      {showEditModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-md p-6 space-y-6 shadow-2xl">
            <h3 className="text-lg font-bold text-zinc-900">Edit Pig Details</h3>
            <form onSubmit={handleUpdatePig} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Breed</label>
                <input
                  type="text"
                  required
                  value={breed}
                  onChange={(e) => setBreed(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Purpose</label>
                  <select
                    value={purpose}
                    onChange={(e) => setPurpose(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  >
                    <option>Porker</option>
                    <option>Breeder</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Status</label>
                  <input
                    type="text"
                    required
                    value={status}
                    onChange={(e) => setStatus(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Weight (kg)</label>
                  <input
                    type="number"
                    step="any"
                    value={weight}
                    onChange={(e) => setWeight(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Location / Pen</label>
                  <input
                    type="text"
                    value={location}
                    onChange={(e) => setLocation(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Notes</label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  rows={2}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
                <button
                  type="button"
                  onClick={() => setShowEditModal(false)}
                  className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition"
                >
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Log Health Record Modal */}
      {showRecordModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-md p-6 space-y-6 shadow-2xl">
            <h3 className="text-lg font-bold text-zinc-900">Log Health Action</h3>
            <form onSubmit={handleAddHealthRecord} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Action Date</label>
                <input
                  type="date"
                  required
                  value={recordDate}
                  onChange={(e) => setRecordDate(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Action Type</label>
                <select
                  value={recordType}
                  onChange={(e) => setRecordType(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                >
                  <option>Medication</option>
                  <option>Vaccination</option>
                  <option>Treatment</option>
                  <option>Heat Detection</option>
                  <option>Breeding</option>
                  <option>Pregnancy Confirmation</option>
                  <option>Deworming</option>
                  <option>Other</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Description / Outcome</label>
                <textarea
                  required
                  value={recordDesc}
                  onChange={(e) => setRecordDesc(e.target.value)}
                  placeholder="e.g. Administered 2ml iron supplement."
                  rows={3}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
                <button
                  type="button"
                  onClick={() => setShowRecordModal(false)}
                  className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition"
                >
                  Log Action
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
