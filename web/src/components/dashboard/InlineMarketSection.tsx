"use client";

import React, { useEffect, useState, useMemo } from "react";
import { collection, onSnapshot, doc, setDoc, query, where, orderBy } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useTranslations } from "next-intl";
import {
  LocalHubIcon,
  ShoppingBagIcon,
  MedicalServicesIcon,
  AddIcon,
  LocationIcon,
  PhoneIcon,
  EmailIcon,
  VerifiedIcon,
  HistoryIcon
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

const ChevronDownIcon = ({ className = "h-5 w-5", ...props }: React.SVGProps<SVGSVGElement>) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.5"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={className}
    {...props}
  >
    <polyline points="6 9 12 15 18 9" />
  </svg>
);

export default function InlineMarketSection() {
  const t = useTranslations("Hub");
  const { user, userProfile } = useAuth();

  const [openSubOption, setOpenSubOption] = useState<string | null>(null);
  const [providers, setProviders] = useState<ProviderListing[]>([]);
  const [mySuggestions, setMySuggestions] = useState<Suggestion[]>([]);
  const [dataLoading, setDataLoading] = useState(true);

  const [successMsg, setSuccessMsg] = useState("");
  const [errorMsg, setErrorMsg] = useState("");

  // Suggestion form
  const [providerName, setProviderName] = useState("");
  const [serviceType, setServiceType] = useState("");
  const [contact, setContact] = useState("");
  const [email, setEmail] = useState("");
  const [city, setCity] = useState("");
  const [country, setCountry] = useState(userProfile?.country || "Ghana");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (userProfile?.country) {
      setCountry(userProfile.country);
    }
  }, [userProfile?.country]);

  useEffect(() => {
    setDataLoading(true);
    const providersRef = collection(db, "market_providers");
    const unsubProviders = onSnapshot(
      providersRef,
      (snapshot) => {
        const list = snapshot.docs.map((d) => ({ id: d.id, ...d.data() } as ProviderListing));
        setProviders(list);
        setDataLoading(false);
      },
      (err) => {
        console.error("Error fetching providers:", err);
        setDataLoading(false);
      }
    );

    let unsubSuggestions = () => {};
    if (user) {
      const suggestionsRef = collection(db, "market_suggestions");
      const q = query(
        suggestionsRef,
        where("userId", "==", user.uid),
        orderBy("timestamp", "desc")
      );
      unsubSuggestions = onSnapshot(
        q,
        (snapshot) => {
          const list = snapshot.docs.map((d) => ({ id: d.id, ...d.data() } as Suggestion));
          setMySuggestions(list);
        },
        (err) => {
          console.error("Error fetching suggestions:", err);
        }
      );
    }

    return () => {
      unsubProviders();
      unsubSuggestions();
    };
  }, [user]);

  const scrollToSubOption = (key: string, delayMs = 150) => {
    if (typeof window === "undefined") return;
    setTimeout(() => {
      const el = document.getElementById(`suboption-${key}`);
      if (el) {
        el.scrollIntoView({ behavior: "smooth", block: "start" });
      }
    }, delayMs);
    setTimeout(() => {
      const el = document.getElementById(`suboption-${key}`);
      if (el) {
        el.scrollIntoView({ behavior: "smooth", block: "start" });
      }
    }, delayMs + 220);
  };

  const toggleSubOption = (key: string) => {
    setOpenSubOption((prev) => {
      const next = prev === key ? null : key;
      if (next) {
        scrollToSubOption(next, 150);
      }
      return next;
    });
  };

  const userCountry = userProfile?.country;
  const filteredProviders = useMemo(() => {
    if (!userCountry) return providers;
    return providers.filter(
      (p) => p.country?.trim().toLowerCase() === userCountry.trim().toLowerCase()
    );
  }, [providers, userCountry]);

  const vendorsList = filteredProviders.filter((p) => p.category === "vendors");
  const buyersList = filteredProviders.filter((p) => p.category === "buyers");
  const vetsList = filteredProviders.filter((p) => p.category === "vets");

  const services = [
    { label: t("services.butcher"), val: "Butcher" },
    { label: t("services.meatProcessor"), val: "Meat Processor" },
    { label: t("services.abattoir"), val: "Abattoir" },
    { label: t("services.feedSupplier"), val: "Feed Supplier" },
    { label: t("services.toolsSupplier"), val: "Tools Supplier" },
    { label: t("services.vetShop"), val: "Vet Shop" },
    { label: t("services.vetServices"), val: "Vet Services" },
    { label: t("services.other"), val: "Other" },
  ];

  const handleCreateSuggestion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !providerName.trim() || !serviceType || !contact.trim()) return;

    setErrorMsg("");
    setSubmitting(true);
    try {
      const isDuplicate =
        providers.some(
          (p) =>
            p.name.toLowerCase() === providerName.trim().toLowerCase() &&
            p.contact === contact.trim()
        ) ||
        mySuggestions.some(
          (s) =>
            s.providerName.toLowerCase() === providerName.trim().toLowerCase() &&
            s.contact === contact.trim()
        );

      if (isDuplicate) {
        setErrorMsg(t("duplicateError"));
        setSubmitting(false);
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
        timestamp: new Date(),
      };

      await setDoc(sugRef, newSuggestion);

      setProviderName("");
      setContact("");
      setEmail("");
      setCity("");
      setServiceType("");
      setSuccessMsg(t("successSubmit"));
      setTimeout(() => setSuccessMsg(""), 5000);
    } catch (err) {
      console.error("Failed to submit suggestion:", err);
      setErrorMsg(t("errorSubmit"));
    } finally {
      setSubmitting(false);
    }
  };

  const subOptions = [
    {
      key: "vendors",
      title: t("verifiedVendors"),
      icon: LocalHubIcon,
      count: vendorsList.length,
      color: "pink",
    },
    {
      key: "buyers",
      title: t("porkBuyers"),
      icon: ShoppingBagIcon,
      count: buyersList.length,
      color: "pink",
    },
    {
      key: "vets",
      title: t("vetServices"),
      icon: MedicalServicesIcon,
      count: vetsList.length,
      color: "pink",
    },
    {
      key: "suggest",
      title: t("suggestProvider"),
      icon: AddIcon,
      count: mySuggestions.length,
      color: "pink",
    },
  ];

  return (
    <div className="space-y-4">
      {/* Directory Region Banner */}
      <div className="bg-pink-50/70 border border-pink-200/80 rounded-xl p-3.5 flex items-center justify-center gap-2.5">
        <LocationIcon className="h-4 w-4 text-pink-600 shrink-0" />
        <p className="text-xs sm:text-sm font-bold text-pink-900">
          {t("directoryRegion", { region: userProfile?.country || t("allRegions") })}
        </p>
      </div>

      {successMsg && (
        <div className="rounded-xl border border-pink-200 bg-pink-50 p-3 text-xs text-pink-800 font-bold text-center animate-pulse">
          {successMsg}
        </div>
      )}

      {errorMsg && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-800 font-bold text-center">
          {errorMsg}
        </div>
      )}

      {/* 4 Single-Open Sub-Option Cards */}
      <div className="space-y-3">
        {subOptions.map((opt) => {
          const isOpen = openSubOption === opt.key;
          const Icon = opt.icon;

          return (
            <div
              key={opt.key}
              id={`suboption-${opt.key}`}
              className="scroll-mt-20 bg-white border border-pink-100 rounded-xl overflow-hidden shadow-xs transition-all"
            >
              {/* Accordion Header Button */}
              <button
                type="button"
                onClick={() => toggleSubOption(opt.key)}
                className="w-full p-4 flex items-center justify-between text-left hover:bg-pink-50/30 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <div className="h-9 w-9 rounded-lg bg-pink-50 text-pink-600 border border-pink-200/60 flex items-center justify-center shrink-0">
                    <Icon className="h-5 w-5" />
                  </div>
                  <div>
                    <span className="text-sm font-bold text-zinc-900">{opt.title}</span>
                    <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-pink-100 text-pink-800">
                      {opt.key === "suggest" ? `${mySuggestions.length} Suggested` : `${opt.count} Listed`}
                    </span>
                  </div>
                </div>
                <ChevronDownIcon
                  className={`h-5 w-5 text-zinc-400 transform transition-transform duration-300 ${
                    isOpen ? "rotate-180 text-pink-600" : ""
                  }`}
                />
              </button>

              {/* Accordion Content */}
              {isOpen && (
                <div className="border-t border-pink-100 p-4 bg-zinc-50/40 animate-fadeIn">
                  {/* Option 1: Vendors */}
                  {opt.key === "vendors" && (
                    <div>
                      {dataLoading ? (
                        <div className="py-8 text-center text-xs text-zinc-400 animate-pulse">Loading vendors...</div>
                      ) : vendorsList.length === 0 ? (
                        <div className="py-8 text-center text-xs text-zinc-400 font-medium">{t("noVendors")}</div>
                      ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                          {vendorsList.map((p) => (
                            <ProviderCardItem key={p.id} provider={p} t={t} />
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  {/* Option 2: Buyers */}
                  {opt.key === "buyers" && (
                    <div>
                      {dataLoading ? (
                        <div className="py-8 text-center text-xs text-zinc-400 animate-pulse">Loading buyers...</div>
                      ) : buyersList.length === 0 ? (
                        <div className="py-8 text-center text-xs text-zinc-400 font-medium">{t("noBuyers")}</div>
                      ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                          {buyersList.map((p) => (
                            <ProviderCardItem key={p.id} provider={p} t={t} />
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  {/* Option 3: Vets */}
                  {opt.key === "vets" && (
                    <div>
                      {dataLoading ? (
                        <div className="py-8 text-center text-xs text-zinc-400 animate-pulse">Loading veterinary services...</div>
                      ) : vetsList.length === 0 ? (
                        <div className="py-8 text-center text-xs text-zinc-400 font-medium">{t("noVets")}</div>
                      ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                          {vetsList.map((p) => (
                            <ProviderCardItem key={p.id} provider={p} t={t} />
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  {/* Option 4: Suggest a Provider */}
                  {opt.key === "suggest" && (
                    <div className="space-y-6">
                      <form onSubmit={handleCreateSuggestion} className="bg-white p-5 rounded-xl border border-pink-100 shadow-xs space-y-3.5">
                        <h4 className="text-xs font-black uppercase tracking-wider text-pink-900">
                          {t("suggestProvider")}
                        </h4>

                        <div className="space-y-1">
                          <label className="text-[10px] font-black uppercase text-zinc-400">{t("businessName")}</label>
                          <input
                            type="text"
                            required
                            value={providerName}
                            onChange={(e) => setProviderName(e.target.value)}
                            placeholder="e.g. Dr. K. Appiah (Vet)"
                            className="w-full rounded-xl border border-zinc-200 bg-zinc-50 px-3.5 py-2.5 text-xs focus:outline-none focus:ring-2 focus:ring-pink-500/20 focus:border-pink-500"
                          />
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                          <div className="space-y-1">
                            <label className="text-[10px] font-black uppercase text-zinc-400">{t("serviceCategory")}</label>
                            <select
                              required
                              value={serviceType}
                              onChange={(e) => setServiceType(e.target.value)}
                              className="w-full rounded-xl border border-zinc-200 bg-zinc-50 px-3.5 py-2.5 text-xs focus:outline-none focus:ring-2 focus:ring-pink-500/20 focus:border-pink-500"
                            >
                              <option value="">{t("selectService")}</option>
                              {services.map((s) => (
                                <option key={s.val} value={s.val}>
                                  {s.label}
                                </option>
                              ))}
                            </select>
                          </div>

                          <div className="space-y-1">
                            <label className="text-[10px] font-black uppercase text-zinc-400">{t("country")}</label>
                            <input
                              type="text"
                              required
                              value={country}
                              onChange={(e) => setCountry(e.target.value)}
                              className="w-full rounded-xl border border-zinc-200 bg-zinc-50 px-3.5 py-2.5 text-xs focus:outline-none focus:ring-2 focus:ring-pink-500/20 focus:border-pink-500"
                            />
                          </div>
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                          <div className="space-y-1">
                            <label className="text-[10px] font-black uppercase text-zinc-400">{t("phoneNumber")}</label>
                            <input
                              type="tel"
                              required
                              value={contact}
                              onChange={(e) => setContact(e.target.value)}
                              placeholder="+233..."
                              className="w-full rounded-xl border border-zinc-200 bg-zinc-50 px-3.5 py-2.5 text-xs focus:outline-none focus:ring-2 focus:ring-pink-500/20 focus:border-pink-500"
                            />
                          </div>

                          <div className="space-y-1">
                            <label className="text-[10px] font-black uppercase text-zinc-400">{t("cityLocation")}</label>
                            <input
                              type="text"
                              value={city}
                              onChange={(e) => setCity(e.target.value)}
                              placeholder="e.g. Kumasi"
                              className="w-full rounded-xl border border-zinc-200 bg-zinc-50 px-3.5 py-2.5 text-xs focus:outline-none focus:ring-2 focus:ring-pink-500/20 focus:border-pink-500"
                            />
                          </div>
                        </div>

                        <div className="space-y-1">
                          <label className="text-[10px] font-black uppercase text-zinc-400">{t("emailOptional")}</label>
                          <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="contact@business.com"
                            className="w-full rounded-xl border border-zinc-200 bg-zinc-50 px-3.5 py-2.5 text-xs focus:outline-none focus:ring-2 focus:ring-pink-500/20 focus:border-pink-500"
                          />
                        </div>

                        <button
                          type="submit"
                          disabled={submitting}
                          className="w-full py-2.5 rounded-xl bg-pink-600 hover:bg-pink-700 text-white text-xs font-bold shadow-sm transition disabled:opacity-50"
                        >
                          {submitting ? "Submitting..." : t("submitSuggestion")}
                        </button>
                      </form>

                      {/* My Suggestions List */}
                      {mySuggestions.length > 0 && (
                        <div className="space-y-2.5">
                          <div className="flex items-center gap-2">
                            <HistoryIcon className="h-4 w-4 text-pink-600" />
                            <h4 className="text-xs font-bold text-zinc-700 uppercase tracking-wider">
                              {t("mySuggestions")} ({mySuggestions.length})
                            </h4>
                          </div>

                          <div className="space-y-2">
                            {mySuggestions.map((s) => (
                              <div
                                key={s.id}
                                className="bg-white border border-zinc-200 rounded-xl p-3.5 flex flex-col gap-1.5"
                              >
                                <div className="flex justify-between items-start">
                                  <h5 className="font-bold text-xs text-zinc-900">{s.providerName}</h5>
                                  <span
                                    className={`text-[9px] font-extrabold px-2 py-0.5 rounded-full ${
                                      s.status === "approved"
                                        ? "bg-emerald-100 text-emerald-800"
                                        : s.status === "rejected"
                                        ? "bg-red-100 text-red-800"
                                        : "bg-amber-100 text-amber-800"
                                    }`}
                                  >
                                    {s.status?.toUpperCase()}
                                  </span>
                                </div>
                                <p className="text-[11px] text-zinc-600">
                                  {s.serviceType} • {s.city ? `${s.city}, ` : ""}{s.country}
                                </p>
                                <p className="text-[10px] text-zinc-400">Tel: {s.contact}</p>
                                {s.adminFeedback && (
                                  <div className="mt-1 p-2 bg-red-50 border border-red-100 rounded-lg text-[10px] text-red-700">
                                    <strong>{t("feedback")}</strong> {s.adminFeedback}
                                  </div>
                                )}
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

function ProviderCardItem({ provider, t }: { provider: ProviderListing; t: any }) {
  return (
    <div className="bg-white border border-pink-100 rounded-xl p-4 flex flex-col justify-between gap-3 hover:shadow-xs transition">
      <div className="space-y-1">
        <div className="flex items-center gap-1.5">
          <h4 className="text-xs font-bold text-zinc-900 truncate">{provider.name}</h4>
          {provider.isVerified && (
            <VerifiedIcon className="h-3.5 w-3.5 text-pink-600 shrink-0" />
          )}
        </div>
        {provider.location && (
          <p className="text-[11px] text-zinc-500 flex items-center gap-1">
            <LocationIcon className="h-3 w-3 shrink-0 text-zinc-400" />
            <span className="truncate">{provider.location}</span>
          </p>
        )}
        {provider.description && (
          <p className="text-[11px] text-zinc-600 line-clamp-2 mt-1">{provider.description}</p>
        )}
      </div>

      <div className="flex items-center gap-2 pt-1">
        {provider.contact && (
          <a
            href={`tel:${provider.contact}`}
            className="flex-1 py-1.5 px-3 rounded-lg bg-pink-50 hover:bg-pink-100 border border-pink-200 text-pink-700 text-[11px] font-bold inline-flex items-center justify-center gap-1.5 transition"
          >
            <PhoneIcon className="h-3 w-3" />
            {t("call")}
          </a>
        )}
        {provider.email ? (
          <a
            href={`mailto:${provider.email}`}
            className="flex-1 py-1.5 px-3 rounded-lg bg-zinc-50 hover:bg-zinc-100 border border-zinc-200 text-zinc-700 text-[11px] font-bold inline-flex items-center justify-center gap-1.5 transition"
          >
            <EmailIcon className="h-3 w-3" />
            {t("email")}
          </a>
        ) : null}
      </div>
    </div>
  );
}
