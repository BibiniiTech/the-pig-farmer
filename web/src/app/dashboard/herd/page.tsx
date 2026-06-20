"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot, doc, setDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import UserProfileDropdown from "@/components/UserProfileDropdown";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import HerdReport from "@/components/reports/HerdReport";
import { evaluatePerformance, calculateAgeMonths } from "@/lib/swineGrowthDatabase";

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
}

const STANDARD_BREEDS = [
  "Large White",
  "Landrace",
  "Duroc",
  "Hampshire",
  "Berkshire",
  "Large White x Landrace (F1)",
  "Duroc x (Large White x Landrace)",
  "Duroc x Landrace",
  "Duroc x Large White",
  "Hampshire x Landrace",
  "Hampshire x Large White",
  "Local / Heritage",
  "Other"
];

const ArchiveIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.4"
    strokeLinecap="round"
    strokeLinejoin="round"
    {...props}
  >
    <polyline points="21 8 21 21 3 21 3 8" />
    <rect x="1" y="3" width="22" height="5" rx="1" />
    <line x1="10" y1="12" x2="14" y2="12" />
  </svg>
);

export default function HerdPage() {
  const { user, userProfile, activeFarmUid, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  const [pigs, setPigs] = useState<Pig[]>([]);
  const [archivedPigs, setArchivedPigs] = useState<Pig[]>([]);
  const [viewingArchived, setViewingArchived] = useState(false);
  const [dataLoading, setDataLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);

  const [currentSlide, setCurrentSlide] = useState(0);
  const timerRef = React.useRef<NodeJS.Timeout | null>(null);

  const [herdStats, setHerdStats] = useState({
    total: 0,
    breeders_count: 0,
    porkers_count: 0,
    breeders_piglets: 0,
    breeders_starter: 0,
    breeders_grower: 0,
    boars: 0,
    gilts: 0,
    Pregnant: 0,
    Lactating: 0,
    sows: 0,
    Starter: 0,
    Grower: 0,
    Finisher: 0,
  });

  // Form states
  const [isMultiple, setIsMultiple] = useState(false);
  const [birthDate, setBirthDate] = useState(new Date().toISOString().split("T")[0]);
  const [breed, setBreed] = useState("Large White");
  const [selectedBreed, setSelectedBreed] = useState("Large White");
  const [customBreed, setCustomBreed] = useState("");
  const [purpose, setPurpose] = useState("Porker");
  const [sowTag, setSowTag] = useState("");
  const [boarTag, setBoarTag] = useState("");
  const [source, setSource] = useState("Born on farm");
  const [notes, setNotes] = useState("");
  
  // Single mode inputs
  const [tagNumber, setTagNumber] = useState("");
  const [gender, setGender] = useState("Male");
  const [weight, setWeight] = useState(0);
  const [location, setLocation] = useState("");
  const [purchasePrice, setPurchasePrice] = useState(0);

  interface MultiPigEntry {
    tagNumber: string;
    weight: number;
    location: string;
  }

  // Batch mode inputs
  const [maleQty, setMaleQty] = useState(0);
  const [femaleQty, setFemaleQty] = useState(0);
  const [malePigs, setMalePigs] = useState<MultiPigEntry[]>([]);
  const [femalePigs, setFemalePigs] = useState<MultiPigEntry[]>([]);

  const handleMaleQtyChange = (qty: number) => {
    setMaleQty(qty);
    setMalePigs((prev) => {
      const next = [...prev];
      if (qty > next.length) {
        for (let i = next.length; i < qty; i++) {
          next.push({ tagNumber: "", weight: 0, location: "" });
        }
      } else if (qty < next.length) {
        next.splice(qty);
      }
      return next;
    });
  };

  const handleFemaleQtyChange = (qty: number) => {
    setFemaleQty(qty);
    setFemalePigs((prev) => {
      const next = [...prev];
      if (qty > next.length) {
        for (let i = next.length; i < qty; i++) {
          next.push({ tagNumber: "", weight: 0, location: "" });
        }
      } else if (qty < next.length) {
        next.splice(qty);
      }
      return next;
    });
  };

  const resetTimer = React.useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
    }
    timerRef.current = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % 4);
    }, 5000);
  }, []);

  useEffect(() => {
    resetTimer();
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [resetTimer]);

  const handleSlideChange = (index: number) => {
    setCurrentSlide(index);
    resetTimer();
    if (index === 3) {
      setViewingArchived(true);
    } else {
      setViewingArchived(false);
    }
  };

  const handleCardClick = () => {
    handleSlideChange((currentSlide + 1) % 4);
  };

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!activeFarmUid) return;

    setDataLoading(true);
    const pigsQuery = collection(db, "users", activeFarmUid, "pigs");
    const unsubscribe = onSnapshot(pigsQuery, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Pig));
      setPigs(list.sort((a, b) => a.tagNumber.localeCompare(b.tagNumber)));
      
      const breeders = list.filter(p => p.purpose === "Breeder");
      const porkers = list.filter(p => p.purpose === "Porker");

      setHerdStats({
        total: list.length,
        breeders_count: breeders.length,
        porkers_count: porkers.length,
        
        breeders_piglets: breeders.filter(p => p.status === "Piglet").length,
        breeders_starter: breeders.filter(p => p.status === "Starter").length,
        breeders_grower: breeders.filter(p => p.status === "Grower").length,
        boars: breeders.filter(p => p.status === "Boar").length,
        gilts: breeders.filter(p => p.status === "Gilt").length,
        Pregnant: breeders.filter(p => p.status === "Pregnant").length,
        Lactating: breeders.filter(p => p.status === "Lactating").length,
        sows: breeders.filter(p => p.status === "Sow").length,

        Starter: porkers.filter(p => p.status === "Starter").length,
        Grower: porkers.filter(p => p.status === "Grower").length,
        Finisher: porkers.filter(p => p.status === "Finisher").length,
      });

      setDataLoading(false);
    }, (error) => {
      console.error("Error listening to pigs:", error);
      setDataLoading(false);
    });

    const archivedQuery = collection(db, "users", activeFarmUid, "archived_pigs");
    const unsubscribeArchived = onSnapshot(archivedQuery, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Pig));
      setArchivedPigs(list.sort((a, b) => a.tagNumber.localeCompare(b.tagNumber)));
    }, (error) => {
      console.error("Error listening to archived pigs:", error);
    });

    return () => {
      unsubscribe();
      unsubscribeArchived();
    };
  }, [activeFarmUid]);

  const handleAddPig = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFarmUid) return;

    const isPremium = userProfile?.isPremium || userProfile?.isAdmin;
    const additionalPigs = isMultiple ? malePigs.filter(p => p.tagNumber.trim() !== "").length + femalePigs.filter(p => p.tagNumber.trim() !== "").length : 1;

    if (!isPremium && herdStats.total + additionalPigs > 20) {
      alert("Free users can only have up to 20 pigs total. Please upgrade to Premium to add more.");
      router.push("/dashboard/billing");
      setShowAddModal(false);
      return;
    }

    try {
      const pigsCollection = collection(db, "users", activeFarmUid, "pigs");

      if (!isMultiple) {
        // Single Add
        if (!tagNumber.trim()) return;
        const newRef = doc(pigsCollection);
        const newPig: Pig = {
          id: newRef.id,
          tagNumber: tagNumber.trim(),
          birthDate,
          breed,
          gender,
          weight,
          purpose,
          sowTag,
          boarTag,
          location,
          source,
          status: purpose === "Breeder" ? (gender === "Male" ? "Boar" : "Sow") : "Piglet",
          notes
        };
        await setDoc(newRef, newPig);

        if (source === "Brought to farm" && purchasePrice > 0) {
          const finRef = doc(collection(db, "users", activeFarmUid, "financials"));
          await setDoc(finRef, {
            id: finRef.id,
            date: new Date().toISOString().split("T")[0],
            type: "Expense",
            category: "Livestock Purchase",
            amount: purchasePrice,
            description: `Purchase of pig with Tag: ${tagNumber.trim()}`,
            pigId: newRef.id
          });
        }
      } else {
        // Batch Add
        const validMales = malePigs.filter((p) => p.tagNumber.trim() !== "");
        const validFemales = femalePigs.filter((p) => p.tagNumber.trim() !== "");

        for (const entry of validMales) {
          const newRef = doc(pigsCollection);
          const newPig: Pig = {
            id: newRef.id,
            tagNumber: entry.tagNumber.trim(),
            birthDate,
            breed,
            gender: "Male",
            weight: entry.weight,
            purpose,
            sowTag,
            boarTag,
            location: entry.location,
            source,
            status: purpose === "Breeder" ? "Boar" : "Piglet",
            notes: notes || "Batch addition (Male)"
          };
          await setDoc(newRef, newPig);
        }

        for (const entry of validFemales) {
          const newRef = doc(pigsCollection);
          const newPig: Pig = {
            id: newRef.id,
            tagNumber: entry.tagNumber.trim(),
            birthDate,
            breed,
            gender: "Female",
            weight: entry.weight,
            purpose,
            sowTag,
            boarTag,
            location: entry.location,
            source,
            status: purpose === "Breeder" ? "Sow" : "Piglet",
            notes: notes || "Batch addition (Female)"
          };
          await setDoc(newRef, newPig);
        }

        if (source === "Brought to farm" && purchasePrice > 0) {
          const finRef = doc(collection(db, "users", activeFarmUid, "financials"));
          await setDoc(finRef, {
            id: finRef.id,
            date: new Date().toISOString().split("T")[0],
            type: "Expense",
            category: "Livestock Purchase",
            amount: purchasePrice,
            description: `Purchase of batch of pigs (Qty: ${validMales.length + validFemales.length})`,
          });
        }
      }

      // Reset Form
      setTagNumber("");
      setNotes("");
      setWeight(0);
      setLocation("");
      setPurchasePrice(0);
      setSource("Born on farm");
      setMaleQty(0);
      setFemaleQty(0);
      setMalePigs([]);
      setFemalePigs([]);
      setBreed("Large White");
      setSelectedBreed("Large White");
      setCustomBreed("");
      setShowAddModal(false);
    } catch (err) {
      console.error("Failed to add pig:", err);
    }
  };

  // Stats calculation
  const boarCount = pigs.filter(p => p.gender === "Male" && p.purpose === "Breeder").length;
  const sowCount = pigs.filter(p => p.gender === "Female" && p.purpose === "Breeder").length;
  const pigletCount = pigs.filter(p => p.status === "Piglet").length;
  const growerCount = pigs.filter(p => p.status === "Grower" || p.status === "Starter").length;
  const finisherCount = pigs.filter(p => p.status === "Finisher").length;

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

        <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8 space-y-8">
          {/* Herd Summary Stats Card (StatsRibbon) */}
          <div
            onClick={handleCardClick}
            className="cursor-pointer bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 relative overflow-hidden group hover:border-emerald-500/30 transition-all duration-300 shadow-sm min-h-[148px] flex flex-col justify-between"
          >
            {/* Background design elements */}
            <div className="absolute top-0 right-0 h-32 w-32 rounded-full bg-emerald-500/5 blur-2xl group-hover:bg-emerald-500/10 transition-all duration-300 pointer-events-none" />
            <div className="absolute bottom-0 left-0 h-24 w-24 rounded-full bg-teal-500/5 blur-xl pointer-events-none" />

            <div className="flex-1 flex flex-col items-center justify-center text-center px-4">
              {dataLoading ? (
                <div className="space-y-2 flex flex-col items-center">
                  <div className="h-6 w-48 bg-zinc-200 animate-pulse rounded" />
                  <div className="h-4 w-64 bg-zinc-100 animate-pulse rounded" />
                </div>
              ) : (
                <div
                  key={currentSlide}
                  className="animate-slide-in flex flex-col items-center text-center space-y-2 select-none"
                >
                  {currentSlide === 0 && (
                    <>
                      <h3 className="text-lg sm:text-xl font-bold text-zinc-800 tracking-tight">
                        Total number of Pigs: <span className="text-xl sm:text-2xl font-extrabold text-emerald-600">{herdStats.total}</span>
                      </h3>
                      <p className="text-xs sm:text-sm font-medium text-zinc-500">
                        Breeders: <span className="font-semibold text-zinc-700">{herdStats.breeders_count}</span> | Porkers: <span className="font-semibold text-zinc-700">{herdStats.porkers_count}</span>
                      </p>
                    </>
                  )}

                  {currentSlide === 1 && (
                    <>
                      <h3 className="text-lg sm:text-xl font-bold text-zinc-800 tracking-tight">
                        Total Number of Breeders: <span className="text-xl sm:text-2xl font-extrabold text-emerald-600">{herdStats.breeders_count}</span>
                      </h3>
                      <p className="text-[11px] sm:text-xs font-medium text-zinc-500 leading-relaxed max-w-2xl">
                        Piglet: <span className="font-semibold text-zinc-700">{herdStats.breeders_piglets}</span> | Starter: <span className="font-semibold text-zinc-700">{herdStats.breeders_starter}</span> | Grower: <span className="font-semibold text-zinc-700">{herdStats.breeders_grower}</span> | Boar: <span className="font-semibold text-zinc-700">{herdStats.boars}</span> | Gilt: <span className="font-semibold text-zinc-700">{herdStats.gilts}</span>
                        <span className="block mt-0.5">
                          Pregnant: <span className="font-semibold text-zinc-700">{herdStats.Pregnant}</span> | Lactating: <span className="font-semibold text-zinc-700">{herdStats.Lactating}</span> | Sow: <span className="font-semibold text-zinc-700">{herdStats.sows}</span>
                        </span>
                      </p>
                    </>
                  )}

                  {currentSlide === 2 && (
                    <>
                      <h3 className="text-lg sm:text-xl font-bold text-zinc-800 tracking-tight">
                        Total Number of Porkers: <span className="text-xl sm:text-2xl font-extrabold text-emerald-600">{herdStats.porkers_count}</span>
                      </h3>
                      <p className="text-xs sm:text-sm font-medium text-zinc-500 leading-relaxed">
                        Starter: <span className="font-semibold text-zinc-700">{herdStats.Starter}</span> | Grower: <span className="font-semibold text-zinc-700">{herdStats.Grower}</span> | Finisher: <span className="font-semibold text-zinc-700">{herdStats.Finisher}</span>
                      </p>
                    </>
                  )}

                  {currentSlide === 3 && (
                    <>
                      <div className="flex items-center gap-2 text-emerald-700">
                        <ArchiveIcon className="h-5 w-5" />
                        <h3 className="text-lg sm:text-xl font-bold text-zinc-800 tracking-tight">
                          Archived Pigs
                        </h3>
                      </div>
                      <p className="text-xs sm:text-sm font-medium text-zinc-500">
                        View culled or sold animals
                      </p>
                    </>
                  )}
                </div>
              )}
            </div>

            {/* Slide Pagination Indicator Dots */}
            <div className="flex justify-center gap-2 mt-2 z-20">
              {[0, 1, 2, 3].map((index) => (
                <button
                  key={index}
                  onClick={(e) => {
                    e.stopPropagation(); // prevent card click handler from firing
                    handleSlideChange(index);
                  }}
                  className={`h-1.5 rounded-full transition-all duration-300 ${
                    currentSlide === index ? "w-5 bg-emerald-500" : "w-1.5 bg-zinc-300 hover:bg-zinc-400"
                  }`}
                  aria-label={`Go to slide ${index + 1}`}
                />
              ))}
            </div>
          </div>

          <div className="flex justify-end">
            <button
              onClick={() => {
                const isPremium = userProfile?.isPremium || userProfile?.isAdmin;
                if (!isPremium && herdStats.total >= 20) {
                  alert("Free users can only have up to 20 pigs. Please upgrade to Premium to add more.");
                  router.push("/dashboard/billing");
                } else {
                  setShowAddModal(true);
                }
              }}
              className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-6 py-2.5 text-xs font-bold text-white shadow-lg shadow-emerald-600/20 transition-all active:scale-95"
            >
              + Add Pigs
            </button>
          </div>

          {/* Herd List Grid */}
          <div className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4 gap-4 flex-wrap">
              <h2 className="text-lg font-bold text-zinc-900">
                {viewingArchived ? "Pigs Inventory (Archived)" : "Pigs Inventory"}
              </h2>
              <button
                onClick={() => {
                  window.print();
                }}
                className="rounded-lg border border-zinc-200 bg-zinc-50/50 px-3 py-1.5 text-xs font-semibold text-zinc-650 hover:bg-zinc-100 transition shadow-sm flex items-center gap-1.5"
              >
                <svg className="h-3.5 w-3.5 text-zinc-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20 2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-8 11c0 .55-.45 1-1 1H9v2H7.5v-5h3c.55 0 1 .45 1 1v1zm5 2c0 .55-.45 1-1 1h-2.5v-5H16c.55 0 1 .45 1 1v3zm-5.5-4H10v1.5h.5V11zm4.5 1h-.5v2h.5v-2zm2.5 1h-2v-1h2v-1h-2v-1h3.5v5H19v-2z" />
                </svg>
                <span>Export PDF</span>
              </button>
            </div>
            {dataLoading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                {[...Array(6)].map((_, i) => (
                  <div key={i} className="h-28 bg-zinc-100 animate-pulse rounded-xl" />
                ))}
              </div>
            ) : (viewingArchived ? archivedPigs : pigs).length === 0 ? (
              <p className="text-sm text-zinc-500 text-center py-12">
                {viewingArchived ? "No archived pigs found." : "No pigs registered in this farm yet."}
              </p>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                {(viewingArchived ? archivedPigs : pigs).map((pig) => {
                  const birthDateObj = new Date(pig.birthDate);
                  const ageDays = isNaN(birthDateObj.getTime())
                    ? -1
                    : Math.floor((Date.now() - birthDateObj.getTime()) / (1000 * 60 * 60 * 24));
                  const performance = evaluatePerformance(pig.breed, ageDays, pig.weight);
                  const ageMonths = calculateAgeMonths(pig.birthDate);

                  let performanceBadgeColor = "bg-zinc-200 text-zinc-600";
                  if (performance === "Excellent") performanceBadgeColor = "bg-amber-100 text-amber-700";
                  else if (performance === "Good") performanceBadgeColor = "bg-green-100 text-green-800";
                  else if (performance === "Caution") performanceBadgeColor = "bg-yellow-100 text-yellow-800";
                  else if (performance === "Poor") performanceBadgeColor = "bg-red-100 text-red-800";

                  return (
                    <Link
                      href={`/dashboard/herd/${pig.id}`}
                      key={pig.id}
                      className="bg-white/90 hover:bg-zinc-50/90 border border-zinc-200 hover:border-emerald-500/40 rounded-xl p-4 transition-all shadow-sm block group relative overflow-hidden"
                    >
                      <div className="absolute top-0 right-0 h-16 w-16 rounded-full bg-emerald-500/5 blur-lg group-hover:bg-emerald-500/10 transition-all pointer-events-none" />
                      <div className="flex justify-between items-start">
                        <div>
                          <p className="text-xs font-semibold text-zinc-400 font-mono">Tag Number</p>
                          <p className="text-lg font-bold text-zinc-900 group-hover:text-emerald-700 transition">
                            {pig.tagNumber}
                          </p>
                        </div>
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${performanceBadgeColor}`}>
                          {performance}
                        </span>
                      </div>

                      <div className="grid grid-cols-2 gap-2 mt-4 text-xs text-zinc-500">
                        <div>
                          <span className="font-semibold">Age:</span> {ageMonths === 0 ? "Less than 1 month" : `${ageMonths} month${ageMonths === 1 ? "" : "s"}`}
                        </div>
                        <div>
                          <span className="font-semibold">Gender:</span> {pig.gender}
                        </div>
                        <div>
                          <span className="font-semibold">Weight:</span> {pig.weight} kg
                        </div>
                        <div>
                          <span className="font-semibold">Location:</span> {pig.location || "N/A"}
                        </div>
                      </div>
                    </Link>
                  );
                })}
              </div>
            )}
          </div>
        </main>
      </div>

      <HerdReport
        pigs={viewingArchived ? archivedPigs : pigs}
        title={viewingArchived ? "Herd Inventory Report (Archived)" : "Herd Inventory Report"}
      />

      {/* Add Pigs Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-lg p-6 space-y-6 shadow-2xl max-h-[90vh] overflow-y-auto no-scrollbar">
            <h3 className="text-lg font-bold text-zinc-900">Add New Pigs</h3>

            {/* Mode selection */}
            <div className="flex gap-2 p-1 bg-zinc-100 rounded-lg">
              <button
                type="button"
                onClick={() => setIsMultiple(false)}
                className={`flex-1 py-1.5 text-xs font-bold rounded-md transition ${!isMultiple ? "bg-white text-zinc-800 shadow" : "text-zinc-500"}`}
              >
                Single Pig
              </button>
              <button
                type="button"
                onClick={() => setIsMultiple(true)}
                className={`flex-1 py-1.5 text-xs font-bold rounded-md transition ${isMultiple ? "bg-white text-zinc-800 shadow" : "text-zinc-500"}`}
              >
                Multiple Batch
              </button>
            </div>

            <form onSubmit={handleAddPig} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Breed</label>
                  <select
                    value={selectedBreed}
                    onChange={(e) => {
                      const val = e.target.value;
                      setSelectedBreed(val);
                      if (val !== "Other") {
                        setBreed(val);
                      } else {
                        setBreed(customBreed);
                      }
                    }}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  >
                    {STANDARD_BREEDS.map((b) => (
                      <option key={b}>{b}</option>
                    ))}
                  </select>
                </div>
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
              </div>

              {selectedBreed === "Other" && (
                <div className="animate-fade-in">
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Specify Breed</label>
                  <input
                    type="text"
                    required
                    value={customBreed}
                    onChange={(e) => {
                      const val = e.target.value;
                      setCustomBreed(val);
                      setBreed(val);
                    }}
                    placeholder="e.g. Berkshire x Large White"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
              )}

              {!isMultiple ? (
                <>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Tag Number</label>
                      <input
                        type="text"
                        required
                        value={tagNumber}
                        onChange={(e) => setTagNumber(e.target.value)}
                        placeholder="e.g. SW-001"
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Gender</label>
                      <select
                        value={gender}
                        onChange={(e) => setGender(e.target.value)}
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                      >
                        <option>Male</option>
                        <option>Female</option>
                      </select>
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
                        placeholder="e.g. Pen A"
                        className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                      />
                    </div>
                  </div>
                </>
              ) : (
                <>
                  {/* Males Section */}
                  <div className="space-y-3">
                    <div className="flex items-center justify-between border-b border-zinc-150 pb-2">
                      <h4 className="text-sm font-bold text-zinc-800">Males</h4>
                      <div className="flex items-center gap-2">
                        <label className="text-xs font-semibold text-zinc-500">Qty:</label>
                        <input
                          type="number"
                          min="0"
                          value={maleQty || ""}
                          onChange={(e) => handleMaleQtyChange(parseInt(e.target.value) || 0)}
                          className="w-16 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 text-center"
                        />
                      </div>
                    </div>

                    {maleQty > 0 && (
                      <div className="space-y-2 max-h-[200px] overflow-y-auto pr-1 no-scrollbar">
                        <div className="grid grid-cols-12 gap-2 text-[10px] font-bold text-zinc-500 px-1">
                          <div className="col-span-4">Tag Number</div>
                          <div className="col-span-4">Weight (kg)</div>
                          <div className="col-span-4">Pen / Location</div>
                        </div>
                        {malePigs.map((pig, idx) => (
                          <div key={idx} className="grid grid-cols-12 gap-2 items-center">
                            <input
                              type="text"
                              required
                              placeholder={`Male Tag ${idx + 1}`}
                              value={pig.tagNumber}
                              onChange={(e) => {
                                const next = [...malePigs];
                                next[idx] = { ...next[idx], tagNumber: e.target.value };
                                setMalePigs(next);
                              }}
                              className="col-span-4 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                            <input
                              type="number"
                              step="any"
                              placeholder="0"
                              value={pig.weight || ""}
                              onChange={(e) => {
                                const next = [...malePigs];
                                next[idx] = { ...next[idx], weight: parseFloat(e.target.value) || 0 };
                                setMalePigs(next);
                              }}
                              className="col-span-4 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                            <input
                              type="text"
                              placeholder="Pen"
                              value={pig.location}
                              onChange={(e) => {
                                const next = [...malePigs];
                                next[idx] = { ...next[idx], location: e.target.value };
                                setMalePigs(next);
                              }}
                              className="col-span-4 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Females Section */}
                  <div className="space-y-3">
                    <div className="flex items-center justify-between border-b border-zinc-150 pb-2">
                      <h4 className="text-sm font-bold text-zinc-800">Females</h4>
                      <div className="flex items-center gap-2">
                        <label className="text-xs font-semibold text-zinc-500">Qty:</label>
                        <input
                          type="number"
                          min="0"
                          value={femaleQty || ""}
                          onChange={(e) => handleFemaleQtyChange(parseInt(e.target.value) || 0)}
                          className="w-16 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 text-center"
                        />
                      </div>
                    </div>

                    {femaleQty > 0 && (
                      <div className="space-y-2 max-h-[200px] overflow-y-auto pr-1 no-scrollbar">
                        <div className="grid grid-cols-12 gap-2 text-[10px] font-bold text-zinc-500 px-1">
                          <div className="col-span-4">Tag Number</div>
                          <div className="col-span-4">Weight (kg)</div>
                          <div className="col-span-4">Pen / Location</div>
                        </div>
                        {femalePigs.map((pig, idx) => (
                          <div key={idx} className="grid grid-cols-12 gap-2 items-center">
                            <input
                              type="text"
                              required
                              placeholder={`Female Tag ${idx + 1}`}
                              value={pig.tagNumber}
                              onChange={(e) => {
                                const next = [...femalePigs];
                                next[idx] = { ...next[idx], tagNumber: e.target.value };
                                setFemalePigs(next);
                              }}
                              className="col-span-4 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                            <input
                              type="number"
                              step="any"
                              placeholder="0"
                              value={pig.weight || ""}
                              onChange={(e) => {
                                const next = [...femalePigs];
                                next[idx] = { ...next[idx], weight: parseFloat(e.target.value) || 0 };
                                setFemalePigs(next);
                              }}
                              className="col-span-4 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                            <input
                              type="text"
                              placeholder="Pen"
                              value={pig.location}
                              onChange={(e) => {
                                const next = [...femalePigs];
                                next[idx] = { ...next[idx], location: e.target.value };
                                setFemalePigs(next);
                              }}
                              className="col-span-4 rounded-lg border border-zinc-200 bg-white px-2 py-1 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </>
              )}

              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Birth Date</label>
                  <input
                    type="date"
                    required
                    value={birthDate}
                    onChange={(e) => setBirthDate(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Sow Tag</label>
                  <input
                    type="text"
                    value={sowTag}
                    onChange={(e) => setSowTag(e.target.value)}
                    placeholder="Mother Tag"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Boar Tag</label>
                  <input
                    type="text"
                    value={boarTag}
                    onChange={(e) => setBoarTag(e.target.value)}
                    placeholder="Father Tag"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Source</label>
                <select
                  value={source}
                  onChange={(e) => setSource(e.target.value)}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                >
                  <option>Born on farm</option>
                  <option>Brought to farm</option>
                </select>
              </div>

              {source === "Brought to farm" && (
                <div className="bg-emerald-50/50 border border-emerald-200/50 rounded-xl p-4 space-y-2 animate-fade-in shadow-sm">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-bold text-emerald-800">Purchase Price</label>
                    <span className="text-[10px] font-semibold text-emerald-600 bg-emerald-100/80 px-2 py-0.5 rounded-full">Financial Expense</span>
                  </div>
                  <input
                    type="number"
                    step="any"
                    required
                    value={purchasePrice === 0 ? "" : purchasePrice}
                    onChange={(e) => setPurchasePrice(parseFloat(e.target.value) || 0)}
                    placeholder="Enter purchase price..."
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                  />
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Notes</label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Any extra comments..."
                  rows={2}
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none focus:ring-1 focus:ring-emerald-500 shadow-sm"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition"
                >
                  Save
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
