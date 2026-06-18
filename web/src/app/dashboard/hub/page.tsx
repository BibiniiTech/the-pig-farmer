"use client";

import React, { useEffect, useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { collection, onSnapshot, doc, setDoc, query, where, orderBy } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import {
  LocalHubIcon,
  ShoppingBagIcon,
  MedicalServicesIcon,
  HistoryIcon,
  AddIcon,
  LocationIcon,
  PhoneIcon,
  EmailIcon,
  VerifiedIcon
} from "@/components/icons/DashboardIcons";

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
  adminFeedback?: string;
  timestamp: any;
}

export default function LocalHubPage() {
  const { user, userProfile, loading } = useAuth();
  const router = useRouter();

  const [providers, setProviders] = useState<ProviderListing[]>([]);
  const [mySuggestions, setMySuggestions] = useState<Suggestion[]>([]);
  const [dataLoading, setDataLoading] = useState(true);

  // Expanded sections state
  const [expandedSections, setExpandedSections] = useState({
    vendors: true,
    buyers: false,
    vets: false,
    mySuggestions: false,
    suggest: false
  });

  const [successMsg, setSuccessMsg] = useState("");
  const [errorMsg, setErrorMsg] = useState("");

  // Suggestion Form states
  const [providerName, setProviderName] = useState("");
  const [serviceType, setServiceType] = useState("");
  const [contact, setContact] = useState("");
  const [email, setEmail] = useState("");
  const [city, setCity] = useState("");
  const [country, setCountry] = useState("");

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (userProfile?.country && !country) {
      setCountry(userProfile.country);
    }
  }, [userProfile, country]);

  useEffect(() => {
    setDataLoading(true);

    // Fetch verified providers globally
    const providersRef = collection(db, "market_providers");
    const unsubProviders = onSnapshot(providersRef, (snapshot) => {
      const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as ProviderListing));
      setProviders(list);
      setDataLoading(false);
    }, (err) => {
      console.error(err);
      setDataLoading(false);
    });

    // Fetch user's suggestions
    let unsubSuggestions = () => {};
    if (user) {
      const suggestionsRef = collection(db, "market_suggestions");
      const q = query(
        suggestionsRef,
        where("userId", "==", user.uid),
        orderBy("timestamp", "desc")
      );
      unsubSuggestions = onSnapshot(q, (snapshot) => {
        const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Suggestion));
        setMySuggestions(list);
      }, (err) => {
        console.error("Suggestions fetch error:", err);
      });
    }

    return () => {
      unsubProviders();
      unsubSuggestions();
    };
  }, [user]);

  const toggleSection = (section: keyof typeof expandedSections) => {
    setExpandedSections(prev => ({ ...prev, [section]: !prev[section] }));
  };

  const handleCreateSuggestion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !providerName.trim() || !serviceType || !contact.trim()) return;

    setErrorMsg("");
    try {
      // Check for duplicates
      const isDuplicate = providers.some(p =>
        p.name.toLowerCase() === providerName.trim().toLowerCase() && p.contact === contact.trim()
      ) || mySuggestions.some(s =>
        s.providerName.toLowerCase() === providerName.trim().toLowerCase() && s.contact === contact.trim()
      );

      if (isDuplicate) {
        setErrorMsg("This provider is already listed or suggested.");
        return;
      }

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
      setServiceType("");
      setSuccessMsg("Listing suggestion submitted successfully! Waiting for admin approval.");
      setTimeout(() => setSuccessMsg(""), 5000);
      setExpandedSections(prev => ({ ...prev, suggest: false, mySuggestions: true }));
    } catch (err) {
      console.error("Failed to submit suggestion:", err);
      setErrorMsg("Failed to submit suggestion. Please try again.");
    }
  };

  const filteredProviders = useMemo(() => {
    if (!userProfile?.country) return providers;
    return providers.filter(p => p.country?.trim().toLowerCase() === userProfile.country.trim().toLowerCase());
  }, [providers, userProfile?.country]);

  const vendorsList = filteredProviders.filter(p => p.category === "vendors");
  const buyersList = filteredProviders.filter(p => p.category === "buyers");
  const vetsList = filteredProviders.filter(p => p.category === "vets");

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-white text-zinc-900">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  const services = [
    "Butcher",
    "Meat Processor",
    "Abattoir",
    "Feed Supplier",
    "Tools Supplier",
    "Vet Shop",
    "Vet Services",
    "Other"
  ];

  return (
    <div className="relative min-h-screen bg-zinc-50 text-zinc-900 flex flex-col font-sans overflow-hidden">
      {/* Watermark Logo Background */}
      <div className="fixed inset-0 z-0 flex items-center justify-center opacity-[0.05] pointer-events-none select-none">
        <img
          src="/app_logo.png"
          alt="Watermark Background Logo"
          className="w-full max-w-[1100px] max-h-[85vh] object-contain"
        />
      </div>

      <div className="relative z-10 flex flex-col min-h-screen">
        <header className="border-b border-zinc-200 bg-white/80 backdrop-blur-md sticky top-0 z-50">
          <div className="max-w-4xl mx-auto px-4 h-16 flex items-center justify-between">
            <Link href="/dashboard" className="flex items-center gap-3 hover:opacity-80 transition-opacity cursor-pointer">
              <img src="/app_logo.png" alt="SmartSwine Logo" className="h-8 w-8 object-contain rounded-md" />
              <span className="font-bold text-sm bg-gradient-to-r from-emerald-700 via-emerald-600 to-green-500 bg-clip-text text-transparent mr-2 inline-block">
                SmartSwine
              </span>
            </Link>

            <div className="flex items-center gap-2">
              <NavbarDropdown />
            </div>
          </div>
        </header>

        <main className="flex-1 max-w-4xl w-full mx-auto px-4 py-8 space-y-6">
          {/* Region Indicator */}
          <div className="bg-emerald-50/50 border border-emerald-100 rounded-2xl p-4 flex items-center justify-center gap-3">
             <LocationIcon className="h-5 w-5 text-emerald-600" />
             <p className="text-sm font-bold text-emerald-800">
               Directory Region: {userProfile?.country || "All Regions"}
             </p>
          </div>

          {successMsg && (
            <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-xs text-emerald-800 font-bold text-center animate-pulse">
              {successMsg}
            </div>
          )}

          {errorMsg && (
            <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-xs text-red-800 font-bold text-center">
              {errorMsg}
            </div>
          )}

          {/* 1. Verified Vendors */}
          <CollapsibleCard
            title="Verified Vendors"
            icon={<LocalHubIcon className="h-6 w-6 text-emerald-600" />}
            count={vendorsList.length}
            expanded={expandedSections.vendors}
            onToggle={() => toggleSection("vendors")}
          >
            {dataLoading ? (
              <LoadingPulse />
            ) : vendorsList.length === 0 ? (
              <EmptyState message="No vendors listed for this region yet." />
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {vendorsList.map(p => <ProviderItem key={p.id} provider={p} />)}
              </div>
            )}
          </CollapsibleCard>

          {/* 2. Pork Buyers & Abattoirs */}
          <CollapsibleCard
            title="Pork Buyers & Abattoirs"
            icon={<ShoppingBagIcon className="h-6 w-6 text-emerald-600" />}
            count={buyersList.length}
            expanded={expandedSections.buyers}
            onToggle={() => toggleSection("buyers")}
          >
            {dataLoading ? (
              <LoadingPulse />
            ) : buyersList.length === 0 ? (
              <EmptyState message="No buyers listed for this region yet." />
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {buyersList.map(p => <ProviderItem key={p.id} provider={p} />)}
              </div>
            )}
          </CollapsibleCard>

          {/* 3. Veterinary Services */}
          <CollapsibleCard
            title="Veterinary Services"
            icon={<MedicalServicesIcon className="h-6 w-6 text-emerald-600" />}
            count={vetsList.length}
            expanded={expandedSections.vets}
            onToggle={() => toggleSection("vets")}
          >
            {dataLoading ? (
              <LoadingPulse />
            ) : vetsList.length === 0 ? (
              <EmptyState message="No veterinary services listed for this region yet." />
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {vetsList.map(p => <ProviderItem key={p.id} provider={p} />)}
              </div>
            )}
          </CollapsibleCard>

          {/* 4. My Suggestions */}
          <CollapsibleCard
            title="My Suggestions"
            icon={<HistoryIcon className="h-6 w-6 text-emerald-600" />}
            count={mySuggestions.length}
            expanded={expandedSections.mySuggestions}
            onToggle={() => toggleSection("mySuggestions")}
          >
            {mySuggestions.length === 0 ? (
              <EmptyState message="You have not submitted any suggestions yet." />
            ) : (
              <div className="space-y-4">
                {mySuggestions.map(s => (
                  <div key={s.id} className="bg-zinc-50 border border-zinc-200 rounded-xl p-4 flex flex-col gap-2">
                    <div className="flex justify-between items-start">
                      <h4 className="font-bold text-zinc-900">{s.providerName}</h4>
                      <StatusBadge status={s.status} />
                    </div>
                    <p className="text-xs text-zinc-600">Category: {s.serviceType}</p>
                    <p className="text-xs text-zinc-500">{s.city}, {s.country}</p>
                    {s.adminFeedback && (
                      <div className="mt-2 p-3 bg-red-50 border border-red-100 rounded-lg text-xs text-red-700">
                        <strong>Feedback:</strong> {s.adminFeedback}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </CollapsibleCard>

          {/* 5. Suggest a Provider */}
          <CollapsibleCard
            title="Suggest a Provider"
            icon={<AddIcon className="h-6 w-6 text-emerald-600" />}
            expanded={expandedSections.suggest}
            onToggle={() => toggleSection("suggest")}
          >
            <form onSubmit={handleCreateSuggestion} className="space-y-4 max-w-lg mx-auto bg-white p-6 rounded-2xl border border-zinc-100 shadow-sm">
              <div className="space-y-1">
                <label className="text-[10px] font-black uppercase text-zinc-400 ml-1">Business / Doctor Name</label>
                <input
                  type="text"
                  required
                  value={providerName}
                  onChange={(e) => setProviderName(e.target.value)}
                  placeholder="e.g. Dr. K. Appiah (Vet)"
                  className="w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-[10px] font-black uppercase text-zinc-400 ml-1">Service Category</label>
                  <select
                    required
                    value={serviceType}
                    onChange={(e) => setServiceType(e.target.value)}
                    className="w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition appearance-none"
                  >
                    <option value="">Select Service</option>
                    {services.map(s => <option key={s} value={s}>{s}</option>)}
                  </select>
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] font-black uppercase text-zinc-400 ml-1">Country</label>
                  <input
                    type="text"
                    required
                    value={country}
                    onChange={(e) => setCountry(e.target.value)}
                    className="w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-[10px] font-black uppercase text-zinc-400 ml-1">Phone Number</label>
                  <input
                    type="text"
                    required
                    value={contact}
                    onChange={(e) => setContact(e.target.value)}
                    placeholder="+233..."
                    className="w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] font-black uppercase text-zinc-400 ml-1">City / Location</label>
                  <input
                    type="text"
                    required
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="e.g. Kumasi"
                    className="w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition"
                  />
                </div>
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-black uppercase text-zinc-400 ml-1">Email address (Optional)</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="contact@vet.com"
                  className="w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition"
                />
              </div>

              <button
                type="submit"
                className="w-full rounded-xl bg-emerald-600 hover:bg-emerald-700 py-4 text-sm font-black text-white shadow-lg shadow-emerald-600/20 transition-all active:scale-[0.98] mt-4 uppercase tracking-widest"
              >
                Submit Suggestion
              </button>
            </form>
          </CollapsibleCard>

          <div className="h-20" />
        </main>
      </div>
    </div>
  );
}

function CollapsibleCard({ title, icon, count, children, expanded, onToggle }: { title: string, icon: React.ReactNode, count?: number, children: React.ReactNode, expanded: boolean, onToggle: () => void }) {
  return (
    <div className="bg-white/60 border border-zinc-200 rounded-3xl overflow-hidden shadow-sm transition-all">
      <button
        onClick={onToggle}
        className="w-full px-6 py-5 flex items-center justify-between hover:bg-zinc-50 transition active:bg-zinc-100"
      >
        <div className="flex items-center gap-4">
          <div className="flex-shrink-0">
            {icon}
          </div>
          <div className="text-left">
            <h3 className="text-base font-black text-zinc-900 leading-tight">{title}</h3>
            {count !== undefined && count > 0 && (
              <span className="text-[10px] font-bold bg-emerald-100 text-emerald-700 px-2 py-0.5 rounded-full mt-1 inline-block">
                {count} {count === 1 ? 'listing' : 'listings'}
              </span>
            )}
          </div>
        </div>
        <span className={`text-zinc-400 transition-transform duration-300 ${expanded ? 'rotate-180' : ''}`}>
          ▼
        </span>
      </button>

      {expanded && (
        <div className="p-6 pt-0 border-t border-zinc-50 animate-in fade-in slide-in-from-top-2 duration-300">
          <div className="mt-6">
            {children}
          </div>
        </div>
      )}
    </div>
  );
}

function ProviderItem({ provider }: { provider: ProviderListing }) {
  return (
    <div className="bg-zinc-50/50 border border-zinc-200 rounded-2xl p-5 space-y-4 hover:border-emerald-500/30 transition-all group">
      <div className="flex justify-between items-start gap-2">
        <div className="flex flex-col gap-1">
          <div className="flex items-center gap-2">
            <h4 className="text-sm font-black text-zinc-900 group-hover:text-emerald-700 transition">{provider.name}</h4>
            {provider.isVerified && (
              <VerifiedIcon className="h-4 w-4 text-emerald-500" />
            )}
          </div>
          <p className="text-[11px] text-zinc-500 flex items-center gap-1 font-medium">
             <LocationIcon className="h-3 w-3 text-emerald-500" /> {provider.location}
          </p>
        </div>
        {provider.isVerified && (
          <span className="shrink-0 text-[10px] font-black px-2 py-0.5 bg-emerald-500 text-white rounded-lg uppercase tracking-tighter">
            Verified
          </span>
        )}
      </div>

      <p className="text-xs text-zinc-600 italic line-clamp-2 leading-relaxed">{provider.description}</p>

      <div className="grid grid-cols-2 gap-2 pt-2">
        <a
          href={`tel:${provider.contact}`}
          className="flex items-center justify-center gap-2 py-2.5 bg-white border border-zinc-200 rounded-xl text-[10px] font-black uppercase text-zinc-700 hover:bg-emerald-50 hover:border-emerald-200 transition"
        >
          <PhoneIcon className="h-3.5 w-3.5" /> Call
        </a>
        {provider.email ? (
           <a
            href={`mailto:${provider.email}`}
            className="flex items-center justify-center gap-2 py-2.5 bg-white border border-zinc-200 rounded-xl text-[10px] font-black uppercase text-zinc-700 hover:bg-emerald-50 hover:border-emerald-200 transition"
           >
            <EmailIcon className="h-3.5 w-3.5" /> Email
           </a>
        ) : (
          <div className="py-2.5 bg-zinc-100/50 border border-transparent rounded-xl text-[10px] font-black uppercase text-zinc-300 flex items-center justify-center gap-2">
            <EmailIcon className="h-3.5 w-3.5 opacity-50" /> No Email
          </div>
        )}
      </div>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const styles = {
    approved: "bg-emerald-100 text-emerald-700 border-emerald-200",
    rejected: "bg-red-100 text-red-700 border-red-200",
    pending: "bg-amber-100 text-amber-700 border-amber-200"
  }[status] || "bg-zinc-100 text-zinc-700 border-zinc-200";

  return (
    <span className={`text-[10px] font-black px-2 py-1 rounded-lg border uppercase ${styles}`}>
      {status}
    </span>
  );
}

function EmptyState({ message }: { message: string }) {
  return (
    <div className="py-12 flex flex-col items-center justify-center text-center px-4">
      <div className="bg-zinc-100 p-6 rounded-full mb-4">
        <LocationIcon className="h-10 w-10 text-zinc-300" />
      </div>
      <p className="text-sm font-bold text-zinc-400">{message}</p>
    </div>
  );
}

function LoadingPulse() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      {[1, 2, 3, 4].map(i => (
        <div key={i} className="h-32 bg-zinc-100 animate-pulse rounded-2xl" />
      ))}
    </div>
  );
}
