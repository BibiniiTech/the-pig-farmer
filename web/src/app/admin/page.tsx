"use client";

import React, { useEffect, useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  collection,
  onSnapshot,
  doc,
  updateDoc,
  deleteDoc,
  addDoc,
  setDoc,
  Timestamp,
  query,
  orderBy,
  runTransaction
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import {
  ChevronLeftIcon,
  StorefrontIcon,
  ListIcon,
  PlayCircleIcon,
  CheckIcon,
  XMarkIcon,
  PencilIcon,
  TrashIcon,
  PlusIcon,
  MagnifyingGlassIcon,
  ChevronDownIcon,
  ChevronUpIcon
} from "@/components/icons/AdminIcons";

// Interfaces
interface Suggestion {
  id: string;
  userId: string;
  providerName: string;
  serviceType: string;
  contact: string;
  email: string;
  city: string;
  country: string;
  timestamp: any;
  status: string;
  adminFeedback: string;
}

interface ProviderListing {
  id: string;
  name: string;
  contact: string;
  email: string;
  location: string;
  description: string;
  isVerified: boolean;
  category: string;
  country: string;
  createdAt: number;
}

interface FeedIngredient {
  id: string;
  name: string;
  mainCategory: string;
  crudeProtein: number;
  metabolizableEnergy: number;
  crudeFiber: number;
  dryMatter: number;
  calcium: number;
  phosphorus: number;
  lysine: number;
  methionine: number;
  nameTranslations: Record<string, string>;
  maxStarter: number;
  maxGrower: number;
  maxFinisher: number;
  visible: boolean;
  unit: string;
}

interface TrainingVideo {
  id: string;
  title: string;
  youtubeId: string;
  createdAt: any;
}

export default function AdminPage() {
  const { userProfile, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  const [activeTab, setActiveTab] = useState<"suggestions" | "ingredients" | "videos">("suggestions");

  // Data States
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [providers, setProviders] = useState<ProviderListing[]>([]);
  const [ingredients, setIngredients] = useState<FeedIngredient[]>([]);
  const [videos, setVideos] = useState<TrainingVideo[]>([]);

  const [isDataLoading, setIsDataLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filter States
  const [ingredientSearch, setIngredientSearch] = useState("");
  const [videoSearch, setVideoSearch] = useState("");

  // UI States (Modals/Dialogs)
  const [showRejectModal, setShowRejectModal] = useState<Suggestion | null>(null);
  const [rejectionReason, setRejectionReason] = useState("");

  const [showEditSuggestion, setShowEditSuggestion] = useState<Suggestion | null>(null);
  const [showDeleteSuggestion, setShowDeleteSuggestion] = useState<Suggestion | null>(null);

  const [showEditProvider, setShowEditProvider] = useState<ProviderListing | null>(null);
  const [showDeleteProvider, setShowDeleteProvider] = useState<ProviderListing | null>(null);

  const [showIngredientModal, setShowIngredientModal] = useState<FeedIngredient | "new" | null>(null);
  const [showDeleteIngredient, setShowDeleteIngredient] = useState<FeedIngredient | null>(null);

  const [showVideoModal, setShowVideoModal] = useState<TrainingVideo | "new" | null>(null);
  const [showDeleteVideo, setShowDeleteVideo] = useState<TrainingVideo | null>(null);

  // Expandable Section States
  const [pendingExpanded, setPendingExpanded] = useState(true);
  const [approvedExpanded, setApprovedExpanded] = useState(false);
  const [rejectedExpanded, setRejectedExpanded] = useState(false);
  const [existingExpanded, setExistingExpanded] = useState(false);
  const [expandedIngredientCategories, setExpandedIngredientCategories] = useState<Record<string, boolean>>({});
  const [expandedDirectoryCountries, setExpandedDirectoryCountries] = useState<Record<string, boolean>>({});

  useEffect(() => {
    if (!loading && (!userProfile || !userProfile.isAdmin)) {
      router.push("/dashboard");
    }
  }, [userProfile, loading, router]);

  useEffect(() => {
    if (!userProfile?.isAdmin) return;

    setIsDataLoading(true);

    const unsubSuggestions = onSnapshot(query(collection(db, "market_suggestions"), orderBy("timestamp", "desc")), (snapshot) => {
      setSuggestions(snapshot.docs.map(d => ({ id: d.id, ...d.data() } as Suggestion)));
    });

    const unsubProviders = onSnapshot(collection(db, "market_providers"), (snapshot) => {
      setProviders(snapshot.docs.map(d => ({ id: d.id, ...d.data() } as ProviderListing)));
    });

    const unsubIngredients = onSnapshot(collection(db, "global_feed_ingredients"), (snapshot) => {
      setIngredients(snapshot.docs.map(d => ({ id: d.id, ...d.data() } as FeedIngredient)));
    });

    const unsubVideos = onSnapshot(query(collection(db, "training_videos"), orderBy("createdAt", "asc")), (snapshot) => {
      setVideos(snapshot.docs.map(d => ({ id: d.id, ...d.data() } as TrainingVideo)));
      setIsDataLoading(false);
    });

    return () => {
      unsubSuggestions();
      unsubProviders();
      unsubIngredients();
      unsubVideos();
    };
  }, [userProfile]);

  // Handlers
  const handleApproveSuggestion = async (suggestion: Suggestion) => {
    try {
      const providerRef = doc(collection(db, "market_providers"));
      const provider: ProviderListing = {
        id: providerRef.id,
        name: suggestion.providerName,
        contact: suggestion.contact,
        email: suggestion.email,
        location: suggestion.city ? `${suggestion.city}, ${suggestion.country}` : suggestion.country,
        description: `Suggested ${suggestion.serviceType} from community.`,
        isVerified: true,
        category: getProviderCategory(suggestion.serviceType),
        country: suggestion.country,
        createdAt: Date.now()
      };

      await runTransaction(db, async (transaction) => {
        transaction.set(providerRef, provider);
        transaction.update(doc(db, "market_suggestions", suggestion.id), { status: "approved" });
      });
    } catch (err: any) {
      alert("Error approving: " + err.message);
    }
  };

  const handleRejectSuggestion = async () => {
    if (!showRejectModal) return;
    try {
      await updateDoc(doc(db, "market_suggestions", showRejectModal.id), {
        status: "rejected",
        adminFeedback: rejectionReason
      });
      setShowRejectModal(null);
      setRejectionReason("");
    } catch (err: any) {
      alert("Error rejecting: " + err.message);
    }
  };

  const getProviderCategory = (serviceType: string) => {
    const type = serviceType.toLowerCase();
    if (["butcher", "meat processor", "abattoir", "meat_processor"].includes(type)) return "buyers";
    if (["vet shop", "vet services", "vet_shop", "vet_services"].includes(type)) return "vets";
    return "vendors";
  };

  const filteredIngredients = ingredients.filter(ing =>
    ing.name.toLowerCase().includes(ingredientSearch.toLowerCase()) ||
    ing.mainCategory.toLowerCase().includes(ingredientSearch.toLowerCase())
  );

  const filteredVideos = videos.filter(v =>
    v.title.toLowerCase().includes(videoSearch.toLowerCase())
  );

  if (loading || !userProfile?.isAdmin) {
    return (
      <div className="flex h-screen items-center justify-center bg-white text-zinc-900">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-zinc-50 text-zinc-900 font-sans pb-20 overflow-hidden">
      {/* Watermark Logo Background */}
      {!isMobile && (
        <div className="fixed inset-0 z-0 flex items-center justify-center opacity-[0.05] pointer-events-none select-none">
          <img
            src="/app_logo.png"
            alt="Watermark Background Logo"
            className="w-full max-w-[1100px] max-h-[85vh] object-contain"
          />
        </div>
      )}

      <div className="relative z-10 flex flex-col min-h-screen">
        {/* Header */}
        {!isMobile && (
          <header className="bg-white/80 backdrop-blur-md border-b border-zinc-200 sticky top-0 z-40">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
              <Link href="/dashboard" className="flex items-center gap-3 hover:opacity-80 transition-opacity cursor-pointer">
                <img src="/app_logo.png" alt="SmartSwine Logo" className="h-8 w-8 object-contain rounded-md" />
                <span className="font-bold text-sm bg-gradient-to-r from-emerald-700 via-emerald-600 to-green-500 bg-clip-text text-transparent mr-2 inline-block">
                  SmartSwine
                </span>
              </Link>

              <div className="flex items-center gap-4">
                <h1 className="text-[10px] font-black text-zinc-400 tracking-widest mr-2">ADMIN PANEL</h1>
                <NavbarDropdown />
              </div>
            </div>

            {/* Tabs */}
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex border-t border-zinc-100">
              <button
                onClick={() => setActiveTab("suggestions")}
                className={`flex-1 py-4 text-sm font-bold flex items-center justify-center gap-2 border-b-2 transition ${
                  activeTab === "suggestions" ? "border-emerald-500 text-emerald-600" : "border-transparent text-zinc-500 hover:text-zinc-700"
                }`}
              >
                <StorefrontIcon className="h-5 w-5" />
                <span className="hidden sm:inline">Suggestions</span>
              </button>
              <button
                onClick={() => setActiveTab("ingredients")}
                className={`flex-1 py-4 text-sm font-bold flex items-center justify-center gap-2 border-b-2 transition ${
                  activeTab === "ingredients" ? "border-emerald-500 text-emerald-600" : "border-transparent text-zinc-500 hover:text-zinc-700"
                }`}
              >
                <ListIcon className="h-5 w-5" />
                <span className="hidden sm:inline">Ingredients</span>
              </button>
              <button
                onClick={() => setActiveTab("videos")}
                className={`flex-1 py-4 text-sm font-bold flex items-center justify-center gap-2 border-b-2 transition ${
                  activeTab === "videos" ? "border-emerald-500 text-emerald-600" : "border-transparent text-zinc-500 hover:text-zinc-700"
                }`}
              >
                <PlayCircleIcon className="h-5 w-5" />
                <span className="hidden sm:inline">Videos</span>
              </button>
            </div>
          </header>
        )}

        {/* Mobile-only Tabs (Floating or sub-header) */}
        {isMobile && (
          <div className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-zinc-100 flex shadow-sm">
            <button
              onClick={() => setActiveTab("suggestions")}
              className={`flex-1 py-3 text-[10px] font-black uppercase tracking-widest transition ${
                activeTab === "suggestions" ? "text-emerald-600 border-b-2 border-emerald-500" : "text-zinc-400"
              }`}
            >
              Suggestions
            </button>
            <button
              onClick={() => setActiveTab("ingredients")}
              className={`flex-1 py-3 text-[10px] font-black uppercase tracking-widest transition ${
                activeTab === "ingredients" ? "text-emerald-600 border-b-2 border-emerald-500" : "text-zinc-400"
              }`}
            >
              Ingredients
            </button>
            <button
              onClick={() => setActiveTab("videos")}
              className={`flex-1 py-3 text-[10px] font-black uppercase tracking-widest transition ${
                activeTab === "videos" ? "text-emerald-600 border-b-2 border-emerald-500" : "text-zinc-400"
              }`}
            >
              Videos
            </button>
          </div>
        )}

        <main className="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {activeTab === "suggestions" && (
          <div className="space-y-6">
            {/* Suggestions Sections */}
            <CollapsibleSection
              title="Pending Suggestions"
              icon={<ListIcon className="h-5 w-5 text-emerald-600" />}
              count={suggestions.filter(s => s.status === "pending").length}
              expanded={pendingExpanded}
              onToggle={() => setPendingExpanded(!pendingExpanded)}
              countColor="bg-emerald-500"
            >
              <div className="space-y-4 pt-4">
                {suggestions.filter(s => s.status === "pending").length === 0 ? (
                  <p className="text-sm text-zinc-500 text-center py-4">No pending suggestions.</p>
                ) : (
                  suggestions.filter(s => s.status === "pending").map(s => (
                    <SuggestionItem
                      key={s.id}
                      suggestion={s}
                      onApprove={() => handleApproveSuggestion(s)}
                      onReject={() => setShowRejectModal(s)}
                      onEdit={() => setShowEditSuggestion(s)}
                      onDelete={() => setShowDeleteSuggestion(s)}
                    />
                  ))
                )}
              </div>
            </CollapsibleSection>

            <CollapsibleSection
              title="Approved Suggestions"
              icon={<CheckIcon className="h-5 w-5 text-green-600" />}
              count={suggestions.filter(s => s.status === "approved").length}
              expanded={approvedExpanded}
              onToggle={() => setApprovedExpanded(!approvedExpanded)}
              countColor="bg-green-500"
            >
              <div className="space-y-4 pt-4">
                {suggestions.filter(s => s.status === "approved").length === 0 ? (
                  <p className="text-sm text-zinc-500 text-center py-4">No approved suggestions.</p>
                ) : (
                  suggestions.filter(s => s.status === "approved").map(s => (
                    <SuggestionItem
                      key={s.id}
                      suggestion={s}
                      onEdit={() => setShowEditSuggestion(s)}
                      onDelete={() => setShowDeleteSuggestion(s)}
                    />
                  ))
                )}
              </div>
            </CollapsibleSection>

            <CollapsibleSection
              title="Rejected Suggestions"
              icon={<XMarkIcon className="h-5 w-5 text-red-600" />}
              count={suggestions.filter(s => s.status === "rejected").length}
              expanded={rejectedExpanded}
              onToggle={() => setRejectedExpanded(!rejectedExpanded)}
              countColor="bg-red-500"
            >
              <div className="space-y-4 pt-4">
                {suggestions.filter(s => s.status === "rejected").length === 0 ? (
                  <p className="text-sm text-zinc-500 text-center py-4">No rejected suggestions.</p>
                ) : (
                  suggestions.filter(s => s.status === "rejected").map(s => (
                    <SuggestionItem
                      key={s.id}
                      suggestion={s}
                      onEdit={() => setShowEditSuggestion(s)}
                      onDelete={() => setShowDeleteSuggestion(s)}
                    />
                  ))
                )}
              </div>
            </CollapsibleSection>

            <CollapsibleSection
              title="Existing Directory"
              icon={<StorefrontIcon className="h-5 w-5 text-emerald-600" />}
              count={providers.length}
              expanded={existingExpanded}
              onToggle={() => setExistingExpanded(!existingExpanded)}
              countColor="bg-emerald-600"
            >
              <div className="space-y-2 pt-4">
                {providers.length === 0 ? (
                  <p className="text-sm text-zinc-500 text-center py-4">No directory listings.</p>
                ) : (
                  Object.entries(groupBy(providers.slice().sort((a, b) => a.name.localeCompare(b.name)), "country"))
                    .sort(([a], [b]) => (a || "Unknown").localeCompare(b || "Unknown"))
                    .map(([country, countryProviders]) => {
                      const label = country || "Unknown";
                      const isExpanded = expandedDirectoryCountries[label] ?? true;
                      return (
                        <div key={label}>
                          <button
                            onClick={() => setExpandedDirectoryCountries(prev => ({ ...prev, [label]: !isExpanded }))}
                            className="w-full flex items-center justify-between py-2 px-1 hover:opacity-70 transition"
                          >
                            <div className="flex items-center gap-2">
                              <span className="text-xs font-black text-zinc-500 uppercase tracking-widest">{label}</span>
                              <span className="px-1.5 py-0.5 rounded-full bg-zinc-200 text-zinc-600 text-[12px] font-black">{(countryProviders as ProviderListing[]).length}</span>
                            </div>
                            {isExpanded ? <ChevronUpIcon className="h-4 w-4 text-zinc-400" /> : <ChevronDownIcon className="h-4 w-4 text-zinc-400" />}
                          </button>
                          {isExpanded && (
                            <div className="space-y-3 pl-2 mt-1">
                              {(countryProviders as ProviderListing[]).map(p => (
                                <ProviderItem
                                  key={p.id}
                                  provider={p}
                                  onEdit={() => setShowEditProvider(p)}
                                  onDelete={() => setShowDeleteProvider(p)}
                                />
                              ))}
                            </div>
                          )}
                        </div>
                      );
                    })
                )}
              </div>
            </CollapsibleSection>
          </div>
        )}

        {activeTab === "ingredients" && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row gap-4">
              <div className="relative flex-1">
                <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-zinc-400" />
                <input
                  type="text"
                  placeholder="Search Ingredients..."
                  value={ingredientSearch}
                  onChange={(e) => setIngredientSearch(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 bg-white border border-zinc-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition"
                />
              </div>
              <button
                onClick={() => setShowIngredientModal("new")}
                className="flex items-center justify-center gap-2 px-6 py-2 bg-emerald-600 text-white font-bold rounded-xl hover:bg-emerald-700 transition"
              >
                <PlusIcon className="h-5 w-5" />
                Add Ingredient
              </button>
            </div>

            <div className="space-y-3">
              {Object.entries(groupBy(
                filteredIngredients.slice().sort((a, b) => a.name.localeCompare(b.name)),
                "mainCategory"
              ))
                .sort(([a], [b]) => a.localeCompare(b))
                .map(([category, items]) => {
                  const isExpanded = expandedIngredientCategories[category] ?? true;
                  return (
                    <div key={category} className="bg-white border border-zinc-200 rounded-2xl overflow-hidden shadow-sm">
                      <button
                        onClick={() => setExpandedIngredientCategories(prev => ({ ...prev, [category]: !isExpanded }))}
                        className="w-full flex items-center justify-between px-4 py-3 hover:bg-zinc-50 transition"
                      >
                        <div className="flex items-center gap-2.5">
                          <span className="text-sm font-black text-zinc-700 uppercase tracking-widest">{category}</span>
                          <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-700 text-[12px] font-black">{(items as FeedIngredient[]).length}</span>
                        </div>
                        {isExpanded ? <ChevronUpIcon className="h-4 w-4 text-zinc-400" /> : <ChevronDownIcon className="h-4 w-4 text-zinc-400" />}
                      </button>
                      {isExpanded && (
                        <div className="p-4 border-t border-zinc-100 grid grid-cols-1 md:grid-cols-2 gap-4">
                          {(items as FeedIngredient[]).map(ing => (
                            <IngredientCard
                              key={ing.id}
                              ingredient={ing}
                              onEdit={() => setShowIngredientModal(ing)}
                              onDelete={() => setShowDeleteIngredient(ing)}
                            />
                          ))}
                        </div>
                      )}
                    </div>
                  );
                })}
              {filteredIngredients.length === 0 && (
                <p className="text-center text-zinc-500 py-12">No ingredients found.</p>
              )}
            </div>
          </div>
        )}

        {activeTab === "videos" && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row gap-4">
              <div className="relative flex-1">
                <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-zinc-400" />
                <input
                  type="text"
                  placeholder="Search Videos..."
                  value={videoSearch}
                  onChange={(e) => setVideoSearch(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 bg-white border border-zinc-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition"
                />
              </div>
              <button
                onClick={() => setShowVideoModal("new")}
                className="flex items-center justify-center gap-2 px-6 py-2 bg-emerald-600 text-white font-bold rounded-xl hover:bg-emerald-700 transition"
              >
                <PlusIcon className="h-5 w-5" />
                Add Video
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {filteredVideos.map(video => (
                <div key={video.id} className="bg-white border border-zinc-200 rounded-2xl p-4 flex justify-between items-center shadow-sm hover:border-emerald-500/30 transition">
                  <div className="flex-1 min-w-0">
                    <h4 className="font-bold text-zinc-900 truncate">{video.title}</h4>
                    <p className="text-xs text-zinc-500 mt-1">YouTube ID: {video.youtubeId}</p>
                  </div>
                  <div className="flex gap-1">
                    <button onClick={() => setShowVideoModal(video)} className="p-2 text-emerald-600 hover:bg-emerald-50 rounded-lg transition"><PencilIcon className="h-5 w-5" /></button>
                    <button onClick={() => setShowDeleteVideo(video)} className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition"><TrashIcon className="h-5 w-5" /></button>
                  </div>
                </div>
              ))}
              {filteredVideos.length === 0 && (
                <p className="col-span-full text-center text-zinc-500 py-12">No video tutorials found.</p>
              )}
            </div>
          </div>
        )}
      </main>

      {/* Modals */}
      {showRejectModal && (
        <Modal title="Reject Suggestion" onClose={() => setShowRejectModal(null)}>
          <div className="space-y-4">
            <p className="text-sm text-zinc-600">Enter feedback / reason for rejection:</p>
            <textarea
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              className="w-full p-3 border border-zinc-200 rounded-xl focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 outline-none h-32 resize-none"
              placeholder="Reason for rejection..."
            />
            <div className="flex gap-3 pt-2">
              <button onClick={() => setShowRejectModal(null)} className="flex-1 py-3 text-zinc-600 font-bold hover:bg-zinc-100 rounded-xl transition">Cancel</button>
              <button
                onClick={handleRejectSuggestion}
                disabled={!rejectionReason.trim()}
                className="flex-1 py-3 bg-emerald-600 text-white font-bold rounded-xl hover:bg-emerald-700 disabled:opacity-50 transition"
              >
                Confirm Rejection
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Edit Suggestion Modal */}
      {showEditSuggestion && (
        <EditSuggestionModal
          suggestion={showEditSuggestion}
          onClose={() => setShowEditSuggestion(null)}
        />
      )}

      {/* Delete Suggestion Modal */}
      {showDeleteSuggestion && (
        <DeleteModal
          title="Delete Suggestion"
          message={`Are you sure you want to permanently delete '${showDeleteSuggestion.providerName}'?`}
          onDelete={async () => {
            await deleteDoc(doc(db, "market_suggestions", showDeleteSuggestion.id));
            setShowDeleteSuggestion(null);
          }}
          onClose={() => setShowDeleteSuggestion(null)}
        />
      )}

      {/* Edit Provider Modal */}
      {showEditProvider && (
        <EditProviderModal
          provider={showEditProvider}
          onClose={() => setShowEditProvider(null)}
        />
      )}

      {/* Delete Provider Modal */}
      {showDeleteProvider && (
        <DeleteModal
          title="Delete Provider Listing"
          message={`Are you sure you want to permanently delete '${showDeleteProvider.name}'?`}
          onDelete={async () => {
            await deleteDoc(doc(db, "market_providers", showDeleteProvider.id));
            setShowDeleteProvider(null);
          }}
          onClose={() => setShowDeleteProvider(null)}
        />
      )}

      {/* Ingredient Modal */}
      {showIngredientModal && (
        <IngredientModal
          ingredient={showIngredientModal === "new" ? null : showIngredientModal}
          onClose={() => setShowIngredientModal(null)}
        />
      )}

      {/* Delete Ingredient Modal */}
      {showDeleteIngredient && (
        <DeleteModal
          title="Delete Global Ingredient"
          message={`Are you sure you want to delete '${showDeleteIngredient.name}'? This will stop seeding this ingredient to new users.`}
          onDelete={async () => {
            await deleteDoc(doc(db, "global_feed_ingredients", showDeleteIngredient.id));
            setShowDeleteIngredient(null);
          }}
          onClose={() => setShowDeleteIngredient(null)}
        />
      )}

      {/* Video Modal */}
      {showVideoModal && (
        <VideoModal
          video={showVideoModal === "new" ? null : showVideoModal}
          onClose={() => setShowVideoModal(null)}
        />
      )}

      {/* Delete Video Modal */}
      {showDeleteVideo && (
        <DeleteModal
          title="Delete Video Tutorial"
          message={`Are you sure you want to permanently delete '${showDeleteVideo.title}'?`}
          onDelete={async () => {
            await deleteDoc(doc(db, "training_videos", showDeleteVideo.id));
            setShowDeleteVideo(null);
          }}
          onClose={() => setShowDeleteVideo(null)}
        />
      )}
    </div>
  </div>
  );
}

// Sub-components

function CollapsibleSection({ title, icon, count, expanded, onToggle, children, countColor }: any) {
  return (
    <div className="bg-white border border-zinc-200 rounded-2xl overflow-hidden shadow-sm">
      <button
        onClick={onToggle}
        className="w-full flex items-center justify-between p-4 hover:bg-zinc-50 transition"
      >
        <div className="flex items-center gap-3">
          {icon}
          <span className="font-bold text-zinc-900">{title}</span>
          {count > 0 && (
            <span className={`px-2 py-0.5 rounded-full text-white text-[12px] font-black ${countColor}`}>
              {count}
            </span>
          )}
        </div>
        {expanded ? <ChevronUpIcon className="h-5 w-5 text-zinc-400" /> : <ChevronDownIcon className="h-5 w-5 text-zinc-400" />}
      </button>
      {expanded && <div className="p-4 border-t border-zinc-100">{children}</div>}
    </div>
  );
}

function SuggestionItem({ suggestion, onApprove, onReject, onEdit, onDelete }: any) {
  return (
    <div className="bg-zinc-50 border border-zinc-200 rounded-xl p-4 space-y-4 hover:border-emerald-500/20 transition">
      <div className="flex justify-between items-start">
        <div>
          <h4 className="font-bold text-zinc-900">{suggestion.providerName}</h4>
          <span className="inline-block px-2 py-0.5 bg-emerald-100 text-emerald-700 text-[12px] font-bold rounded uppercase mt-1">
            {suggestion.serviceType}
          </span>
        </div>
        <div className="flex gap-1">
          <button onClick={onEdit} className="p-2 text-emerald-600 hover:bg-emerald-50 rounded-lg transition"><PencilIcon className="h-4 w-4" /></button>
          <button onClick={onDelete} className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition"><TrashIcon className="h-4 w-4" /></button>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-y-2 text-xs">
        <div><span className="text-zinc-400">User:</span> {suggestion.userId}</div>
        <div><span className="text-zinc-400">Contact:</span> {suggestion.contact}</div>
        {suggestion.email && <div><span className="text-zinc-400">Email:</span> {suggestion.email}</div>}
        <div><span className="text-zinc-400">Location:</span> {suggestion.city}, {suggestion.country}</div>
      </div>

      {suggestion.status === "rejected" && suggestion.adminFeedback && (
        <div className="p-2 bg-red-50 border border-red-100 rounded text-[13px] text-red-600">
          <span className="font-bold">Rejection Reason:</span> {suggestion.adminFeedback}
        </div>
      )}

      {suggestion.status === "pending" && (
        <div className="flex gap-2 pt-2">
          <button onClick={onApprove} className="flex-1 py-2 bg-green-600 text-white text-xs font-bold rounded-lg hover:bg-green-700 transition flex items-center justify-center gap-1">
            <CheckIcon className="h-3.5 w-3.5" /> Approve
          </button>
          <button onClick={onReject} className="flex-1 py-2 bg-red-600 text-white text-xs font-bold rounded-lg hover:bg-red-700 transition flex items-center justify-center gap-1">
            <XMarkIcon className="h-3.5 w-3.5" /> Reject
          </button>
        </div>
      )}
    </div>
  );
}

function ProviderItem({ provider, onEdit, onDelete }: any) {
  return (
    <div className="bg-zinc-50 border border-zinc-200 rounded-xl p-4 space-y-3 hover:border-emerald-500/20 transition">
      <div className="flex justify-between items-start">
        <div>
          <h4 className="font-bold text-zinc-900">{provider.name}</h4>
          <span className="inline-block px-2 py-0.5 bg-blue-100 text-blue-700 text-[12px] font-bold rounded uppercase mt-1">
            {provider.category}
          </span>
        </div>
        <div className="flex gap-1">
          <button onClick={onEdit} className="p-2 text-emerald-600 hover:bg-emerald-50 rounded-lg transition"><PencilIcon className="h-4 w-4" /></button>
          <button onClick={onDelete} className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition"><TrashIcon className="h-4 w-4" /></button>
        </div>
      </div>
      <p className="text-xs text-zinc-600 line-clamp-2">{provider.description}</p>
      <div className="grid grid-cols-2 gap-y-1 text-[13px]">
        <div><span className="text-zinc-400">Contact:</span> {provider.contact}</div>
        <div><span className="text-zinc-400">Location:</span> {provider.location}</div>
        {provider.email && <div><span className="text-zinc-400">Email:</span> {provider.email}</div>}
        <div><span className="text-zinc-400">Country:</span> {provider.country}</div>
      </div>
    </div>
  );
}

function IngredientCard({ ingredient, onEdit, onDelete }: any) {
  return (
    <div className="bg-white border border-zinc-200 rounded-2xl p-4 space-y-4 hover:border-emerald-500/30 transition shadow-sm group">
      <div className="flex justify-between items-start">
        <h4 className="font-bold text-zinc-900 group-hover:text-emerald-700 transition">{ingredient.name}</h4>
        <div className="flex gap-1">
          <button onClick={onEdit} className="p-2 text-emerald-600 hover:bg-emerald-50 rounded-lg transition"><PencilIcon className="h-4 w-4" /></button>
          <button onClick={onDelete} className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition"><TrashIcon className="h-4 w-4" /></button>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="bg-zinc-50 p-2 rounded-lg border border-zinc-100">
          <p className="text-[10px] font-bold text-zinc-400 uppercase tracking-tighter">Protein</p>
          <p className="text-sm font-black text-zinc-800">{ingredient.crudeProtein}%</p>
        </div>
        <div className="bg-zinc-50 p-2 rounded-lg border border-zinc-100">
          <p className="text-[10px] font-bold text-zinc-400 uppercase tracking-tighter">ME (MJ/kg)</p>
          <p className="text-sm font-black text-zinc-800">{(ingredient.metabolizableEnergy / 239.0).toFixed(2)}</p>
        </div>
        <div className="bg-zinc-50 p-2 rounded-lg border border-zinc-100">
          <p className="text-[10px] font-bold text-zinc-400 uppercase tracking-tighter">Lysine</p>
          <p className="text-sm font-black text-zinc-800">{ingredient.lysine}%</p>
        </div>
        <div className="bg-zinc-50 p-2 rounded-lg border border-zinc-100">
          <p className="text-[10px] font-bold text-zinc-400 uppercase tracking-tighter">Methionine</p>
          <p className="text-sm font-black text-zinc-800">{ingredient.methionine}%</p>
        </div>
      </div>
    </div>
  );
}

function Modal({ title, children, onClose }: any) {
  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-zinc-900/40 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl w-full max-w-lg shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200">
        <div className="px-6 py-4 border-b border-zinc-100 flex justify-between items-center">
          <h3 className="font-black text-zinc-900">{title}</h3>
          <button onClick={onClose} className="p-1 hover:bg-zinc-100 rounded-lg transition"><XMarkIcon className="h-5 w-5 text-zinc-400" /></button>
        </div>
        <div className="p-6 max-h-[80vh] overflow-y-auto no-scrollbar">{children}</div>
      </div>
    </div>
  );
}

function DeleteModal({ title, message, onDelete, onClose }: any) {
  return (
    <Modal title={title} onClose={onClose}>
      <div className="space-y-6">
        <p className="text-sm text-zinc-600 leading-relaxed">{message}</p>
        <div className="flex gap-3">
          <button onClick={onClose} className="flex-1 py-3 text-zinc-600 font-bold hover:bg-zinc-100 rounded-xl transition">Cancel</button>
          <button onClick={onDelete} className="flex-1 py-3 bg-red-600 text-white font-bold rounded-xl hover:bg-red-700 transition">Delete</button>
        </div>
      </div>
    </Modal>
  );
}

// Complex Form Modals

function EditSuggestionModal({ suggestion, onClose }: { suggestion: Suggestion, onClose: () => void }) {
  const [formData, setFormData] = useState({...suggestion});
  const services = ["Butcher", "Meat Processor", "Abattoir", "Feed Supplier", "Tools Supplier", "Vet Shop", "Vet Services", "Other"];

  const handleSave = async () => {
    try {
      await updateDoc(doc(db, "market_suggestions", suggestion.id), {...formData});
      onClose();
    } catch (err: any) { alert(err.message); }
  };

  return (
    <Modal title="Edit Suggestion Details" onClose={onClose}>
      <div className="space-y-4">
        <Input label="Provider Name" value={formData.providerName} onChange={(v: string) => setFormData({...formData, providerName: v})} />
        <Select label="Service Type" value={formData.serviceType} options={services} onChange={(v: string) => setFormData({...formData, serviceType: v})} />
        <Input label="Contact" value={formData.contact} onChange={(v: string) => setFormData({...formData, contact: v})} />
        <Input label="Email" value={formData.email} onChange={(v: string) => setFormData({...formData, email: v})} />
        <div className="grid grid-cols-2 gap-4">
          <Input label="City" value={formData.city} onChange={(v: string) => setFormData({...formData, city: v})} />
          <Input label="Country" value={formData.country} onChange={(v: string) => setFormData({...formData, country: v})} />
        </div>
        <button onClick={handleSave} className="w-full py-3 bg-emerald-600 text-white font-bold rounded-xl hover:bg-emerald-700 transition mt-4">Save Changes</button>
      </div>
    </Modal>
  );
}

function EditProviderModal({ provider, onClose }: { provider: ProviderListing, onClose: () => void }) {
  const [formData, setFormData] = useState({...provider});
  const categories = ["vendors", "buyers", "vets"];

  const handleSave = async () => {
    try {
      await updateDoc(doc(db, "market_providers", provider.id), {...formData});
      onClose();
    } catch (err: any) { alert(err.message); }
  };

  return (
    <Modal title="Edit Provider Listing" onClose={onClose}>
      <div className="space-y-4">
        <Input label="Name" value={formData.name} onChange={(v: string) => setFormData({...formData, name: v})} />
        <Select label="Category" value={formData.category} options={categories} onChange={(v: string) => setFormData({...formData, category: v})} />
        <Input label="Contact" value={formData.contact} onChange={(v: string) => setFormData({...formData, contact: v})} />
        <Input label="Email" value={formData.email} onChange={(v: string) => setFormData({...formData, email: v})} />
        <Input label="Location" value={formData.location} onChange={(v: string) => setFormData({...formData, location: v})} />
        <Input label="Country" value={formData.country} onChange={(v: string) => setFormData({...formData, country: v})} />
        <textarea
          placeholder="Description"
          value={formData.description}
          onChange={e => setFormData({...formData, description: e.target.value})}
          className="w-full p-3 border border-zinc-200 rounded-xl focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 outline-none h-24 resize-none text-sm"
        />
        <button onClick={handleSave} className="w-full py-3 bg-emerald-600 text-white font-bold rounded-xl hover:bg-emerald-700 transition mt-4">Save Changes</button>
      </div>
    </Modal>
  );
}

function IngredientModal({ ingredient, onClose }: { ingredient: FeedIngredient | null, onClose: () => void }) {
  const [formData, setFormData] = useState<any>(ingredient || {
    name: "",
    mainCategory: "Energy",
    crudeProtein: 0,
    metabolizableEnergy: 0,
    crudeFiber: 0,
    dryMatter: 0,
    calcium: 0,
    phosphorus: 0,
    lysine: 0,
    methionine: 0,
    maxStarter: 100,
    maxGrower: 100,
    maxFinisher: 100,
    nameTranslations: {},
    visible: true,
    unit: "kg"
  });

  const categories = ["Energy", "Protein", "Vitamins, Minerals & Salt"];

  const handleSave = async () => {
    try {
      // ME conversion: internally it's in kcal/kg, but UI is MJ/kg. 1 MJ/kg = 239 kcal/kg
      const toSave = {
        ...formData,
        crudeProtein: Number(formData.crudeProtein),
        metabolizableEnergy: Number(formData.metabolizableEnergy) * 239.0,
        crudeFiber: Number(formData.crudeFiber),
        dryMatter: Number(formData.dryMatter),
        calcium: Number(formData.calcium),
        phosphorus: Number(formData.phosphorus),
        lysine: Number(formData.lysine),
        methionine: Number(formData.methionine),
        maxStarter: Number(formData.maxStarter),
        maxGrower: Number(formData.maxGrower),
        maxFinisher: Number(formData.maxFinisher),
      };

      if (ingredient) {
        await updateDoc(doc(db, "global_feed_ingredients", ingredient.id), toSave);
      } else {
        await addDoc(collection(db, "global_feed_ingredients"), toSave);
      }
      onClose();
    } catch (err: any) { alert(err.message); }
  };

  return (
    <Modal title={ingredient ? "Edit Feed Ingredient" : "Add Feed Ingredient"} onClose={onClose}>
      <div className="space-y-4">
        <Input label="Ingredient Name" value={formData.name} onChange={(v: string) => setFormData({...formData, name: v})} />
        <Select label="Main Category" value={formData.mainCategory} options={categories} onChange={(v: string) => setFormData({...formData, mainCategory: v})} />

        <div className="grid grid-cols-2 gap-4">
          <Input label="Crude Protein (%)" type="number" value={formData.crudeProtein} onChange={(v: string) => setFormData({...formData, crudeProtein: v})} />
          <Input label="ME (MJ/kg)" type="number" value={formData.metabolizableEnergy} onChange={(v: string) => setFormData({...formData, metabolizableEnergy: v})} />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Input label="Crude Fiber (%)" type="number" value={formData.crudeFiber} onChange={(v: string) => setFormData({...formData, crudeFiber: v})} />
          <Input label="Dry Matter (%)" type="number" value={formData.dryMatter} onChange={(v: string) => setFormData({...formData, dryMatter: v})} />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Input label="Calcium (%)" type="number" value={formData.calcium} onChange={(v: string) => setFormData({...formData, calcium: v})} />
          <Input label="Phosphorus (%)" type="number" value={formData.phosphorus} onChange={(v: string) => setFormData({...formData, phosphorus: v})} />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Input label="Lysine (%)" type="number" value={formData.lysine} onChange={(v: string) => setFormData({...formData, lysine: v})} />
          <Input label="Methionine (%)" type="number" value={formData.methionine} onChange={(v: string) => setFormData({...formData, methionine: v})} />
        </div>

        <p className="text-[10px] font-black text-zinc-400 uppercase tracking-widest pt-2">Max Inclusion Limits (%)</p>

        <div className="grid grid-cols-3 gap-2">
          <Input label="Starter" type="number" value={formData.maxStarter} onChange={(v: string) => setFormData({...formData, maxStarter: v})} />
          <Input label="Grower" type="number" value={formData.maxGrower} onChange={(v: string) => setFormData({...formData, maxGrower: v})} />
          <Input label="Finisher" type="number" value={formData.maxFinisher} onChange={(v: string) => setFormData({...formData, maxFinisher: v})} />
        </div>

        <button onClick={handleSave} className="w-full py-3 bg-emerald-600 text-white font-bold rounded-xl hover:bg-emerald-700 transition mt-4">
          {ingredient ? "Save Changes" : "Add Ingredient"}
        </button>
      </div>
    </Modal>
  );
}

function VideoModal({ video, onClose }: { video: TrainingVideo | null, onClose: () => void }) {
  const [title, setTitle] = useState(video?.title || "");
  const [youtubeLink, setYoutubeLink] = useState(video ? `https://youtube.com/watch?v=${video.youtubeId}` : "");

  const handleSave = async () => {
    try {
      const youtubeId = extractYoutubeId(youtubeLink);
      if (!youtubeId) { alert("Invalid YouTube link or ID"); return; }

      const data = { title, youtubeId, createdAt: video?.createdAt || Timestamp.now() };

      if (video) {
        await updateDoc(doc(db, "training_videos", video.id), data);
      } else {
        await addDoc(collection(db, "training_videos"), data);
      }
      onClose();
    } catch (err: any) { alert(err.message); }
  };

  const extractYoutubeId = (url: string) => {
    const regExp = /^.*((youtu.be\/)|(v\/)|(\/u\/\w\/)|(embed\/)|(watch\?))\??v?=?([^#&?]*).*/;
    const match = url.match(regExp);
    return (match && match[7].length === 11) ? match[7] : url;
  };

  return (
    <Modal title={video ? "Edit Video Tutorial" : "Add Video Tutorial"} onClose={onClose}>
      <div className="space-y-4">
        <Input label="Video Title" value={title} onChange={setTitle} />
        <Input label="YouTube Link or ID" value={youtubeLink} onChange={setYoutubeLink} placeholder="e.g. https://www.youtube.com/watch?v=..." />
        <button onClick={handleSave} className="w-full py-3 bg-emerald-600 text-white font-bold rounded-xl hover:bg-emerald-700 transition mt-4">
          {video ? "Save Changes" : "Add Video"}
        </button>
      </div>
    </Modal>
  );
}

// Utility Components
function Input({ label, value, onChange, type = "text", placeholder = "" }: any) {
  return (
    <div className="space-y-1">
      <label className="text-[10px] font-bold text-zinc-400 uppercase ml-1 tracking-tight">{label}</label>
      <input
        type={type}
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        className="w-full px-4 py-2 bg-zinc-50 border border-zinc-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition text-sm font-medium"
      />
    </div>
  );
}

function Select({ label, value, options, onChange }: any) {
  return (
    <div className="space-y-1">
      <label className="text-[10px] font-bold text-zinc-400 uppercase ml-1 tracking-tight">{label}</label>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full px-4 py-2 bg-zinc-50 border border-zinc-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition text-sm font-medium appearance-none"
      >
        {options.map((opt: string) => (
          <option key={opt} value={opt}>{opt}</option>
        ))}
      </select>
    </div>
  );
}

function groupBy(array: any[], key: string) {
  return array.reduce((result, currentValue) => {
    (result[currentValue[key]] = result[currentValue[key]] || []).push(currentValue);
    return result;
  }, {});
}
