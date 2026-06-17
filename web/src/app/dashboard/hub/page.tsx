"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot, doc, setDoc, serverTimestamp } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import NavbarDropdown from "@/components/NavbarDropdown";

interface ProviderListing {
  id: string;
  name: string;
  contact: string;
  email: string;
  location: string;
  description: string;
  isVerified: boolean;
  category: string; // "vendors", "buyers", "vets"
  country: string;
}

interface Suggestion {
  id: string;
  userId: string;
  providerName: string;
  serviceType: string;
  contact: string;
  email: string;
  city: string;
  country: string;
  status: string;
}

export default function LocalHubPage() {
  const { user, activeFarmUid, loading } = useAuth();
  const router = useRouter();

  const [providers, setProviders] = useState<ProviderListing[]>([]);
  const [dataLoading, setDataLoading] = useState(true);
  const [activeCategory, setActiveCategory] = useState<"vets" | "vendors" | "buyers">("vets");
  
  const [showSuggestModal, setShowSuggestModal] = useState(false);
  const [successMsg, setSuccessMsg] = useState("");

  // Suggestion Form states
  const [providerName, setProviderName] = useState("");
  const [serviceType, setServiceType] = useState("vets");
  const [contact, setContact] = useState("");
  const [email, setEmail] = useState("");
  const [city, setCity] = useState("");
  const [country, setCountry] = useState("Ghana");

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    setDataLoading(true);

    // Fetch verified providers globally
    const providersRef = collection(db, "market_providers");
    const unsubscribe = onSnapshot(providersRef, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as ProviderListing));
      setProviders(list);
      setDataLoading(false);
    }, (err) => {
      console.error(err);
      setDataLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const handleCreateSuggestion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !providerName.trim()) return;

    try {
      const sugRef = doc(collection(db, "market_suggestions"));
      const newSuggestion = {
        id: sugRef.id,
        userId: user.uid,
        providerName: providerName.trim(),
        serviceType,
        contact: contact.trim(),
        email: email.trim(),
        city: city.trim(),
        country,
        status: "pending",
        timestamp: new Date()
      };

      await setDoc(sugRef, newSuggestion);

      // Reset
      setProviderName("");
      setContact("");
      setEmail("");
      setCity("");
      setShowSuggestModal(false);
      setSuccessMsg("Listing suggestion submitted successfully! Waiting for admin approval.");
      setTimeout(() => setSuccessMsg(""), 5000);
    } catch (err) {
      console.error("Failed to submit suggestion:", err);
    }
  };

  // Filter listings
  const filteredListings = providers.filter(p => p.category === activeCategory);

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
              <img src="/app_logo.png" alt="SmartSwine Logo" className="h-8 w-8 object-contain rounded-md" />
              <NavbarDropdown />
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => setShowSuggestModal(true)}
                className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white shadow shadow-emerald-600/10 transition"
              >
                + Suggest Listing
              </button>
              <Link
                href="/dashboard"
                className="text-xs font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200/50 px-3 py-1.5 rounded-lg transition duration-200"
              >
                Back to Home
              </Link>
            </div>
          </div>

          {/* Sub Navigation Category Tabs */}
          <div className="max-w-7xl mx-auto px-4 border-t border-zinc-100 flex gap-4">
            {[
              { id: "vets", label: "🩺 Veterinarians" },
              { id: "vendors", label: "🌾 Feed & Supply Sellers" },
              { id: "buyers", label: "🥩 Pig & Pork Buyers" }
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveCategory(tab.id as any)}
                className={`py-3 text-xs font-bold border-b-2 transition-all ${
                  activeCategory === tab.id
                    ? "border-emerald-600 text-emerald-700"
                    : "border-transparent text-zinc-500 hover:text-zinc-800"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </header>

        <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8 space-y-6">
          {successMsg && (
            <div className="rounded-xl border border-emerald-200 bg-emerald-50/70 p-4 text-xs text-emerald-800 font-medium">
              {successMsg}
            </div>
          )}

          {dataLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="h-44 bg-zinc-100 animate-pulse rounded-2xl" />
              ))}
            </div>
          ) : filteredListings.length === 0 ? (
            <div className="h-96 flex flex-col items-center justify-center border border-dashed border-zinc-200 rounded-2xl text-center p-6 bg-zinc-50/40 backdrop-blur-sm">
              <span className="text-3xl mb-3">🌐</span>
              <p className="font-semibold text-zinc-500 text-sm">No Listings Found</p>
              <p className="text-xs text-zinc-400 mt-1">Be the first to suggest a vetted contact in this category.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredListings.map(listing => (
                <div
                  key={listing.id}
                  className="bg-white/70 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-4 relative overflow-hidden group hover:border-emerald-500/30 transition-all duration-300"
                >
                  <div className="absolute top-0 right-0 h-16 w-16 rounded-full bg-emerald-500/5 blur-lg" />
                  <div className="flex justify-between items-start">
                    <div>
                      <h4 className="text-base font-bold text-zinc-900">{listing.name}</h4>
                      <p className="text-xs text-zinc-500 mt-0.5">{listing.location}, {listing.country}</p>
                    </div>
                    {listing.isVerified && (
                      <span className="text-[10px] font-bold px-2 py-0.5 bg-emerald-50 text-emerald-700 border border-emerald-100 rounded-full flex items-center gap-1">
                        ✓ Verified
                      </span>
                    )}
                  </div>

                  <p className="text-xs text-zinc-650 italic leading-relaxed">{listing.description}</p>

                  <div className="pt-3 border-t border-zinc-100 flex flex-wrap gap-2 text-xs">
                    {listing.contact && (
                      <span className="px-2.5 py-1 bg-zinc-50 border border-zinc-150 rounded-lg font-mono">
                        📞 {listing.contact}
                      </span>
                    )}
                    {listing.email && (
                      <span className="px-2.5 py-1 bg-zinc-50 border border-zinc-150 rounded-lg font-mono">
                        ✉️ {listing.email}
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </main>
      </div>

      {/* Suggest Listing Modal */}
      {showSuggestModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white border border-zinc-200 rounded-2xl w-full max-w-md p-6 space-y-6 shadow-2xl">
            <h3 className="text-lg font-bold text-zinc-900">Suggest verified provider</h3>
            <form onSubmit={handleCreateSuggestion} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Business / Doctor Name</label>
                <input
                  type="text"
                  required
                  value={providerName}
                  onChange={(e) => setProviderName(e.target.value)}
                  placeholder="e.g. Dr. K. Appiah (Vet)"
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Service Category</label>
                  <select
                    value={serviceType}
                    onChange={(e) => setServiceType(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    <option value="vets">Veterinarian</option>
                    <option value="vendors">Supply Seller</option>
                    <option value="buyers">Pig Buyer</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Country</label>
                  <select
                    value={country}
                    onChange={(e) => setCountry(e.target.value)}
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  >
                    <option>Ghana</option>
                    <option>Nigeria</option>
                    <option>Kenya</option>
                    <option>Togo</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Phone Number</label>
                  <input
                    type="text"
                    required
                    value={contact}
                    onChange={(e) => setContact(e.target.value)}
                    placeholder="+233..."
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-500 mb-1.5">City / Location</label>
                  <input
                    type="text"
                    required
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="e.g. Kumasi"
                    className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-500 mb-1.5">Email address (Optional)</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="contact@vet.com"
                  className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 focus:outline-none shadow-sm"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-zinc-150">
                <button
                  type="button"
                  onClick={() => setShowSuggestModal(false)}
                  className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-semibold text-zinc-500 hover:bg-zinc-100 transition"
                >
                  Cancel
                </button>
                <button type="submit" className="rounded-lg bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white transition">
                  Submit Suggestion
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
