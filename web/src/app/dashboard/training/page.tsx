"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { collection, onSnapshot, doc, setDoc, deleteDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import { useTranslations } from "next-intl";

interface TrainingVideo {
  id: string;
  title: string;
  youtubeId: string;
  createdAt: number;
}

interface Tip {
  id: number;
  category: string;
  title: string;
  content: string;
}

export default function TrainingPage() {
  const t = useTranslations("Training");
  const { user, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  const CATEGORIES = [
    { key: "all", label: t("categories.all") },
    { key: "cat_weaning", label: t("categories.weaning") },
    { key: "cat_feeding", label: t("categories.feeding") },
    { key: "cat_breeding", label: t("categories.breeding") },
    { key: "cat_health", label: t("categories.health") },
    { key: "cat_housing", label: t("categories.housing") },
    { key: "cat_waste", label: t("categories.waste") },
    { key: "cat_general", label: t("categories.general") }
  ];

  const MANUAL_TIPS: Tip[] = [
    { id: 6, category: "cat_weaning", title: t("tips.tip6.title"), content: t("tips.tip6.content") },
    { id: 7, category: "cat_weaning", title: t("tips.tip7.title"), content: t("tips.tip7.content") },
    { id: 8, category: "cat_weaning", title: t("tips.tip8.title"), content: t("tips.tip8.content") },
    { id: 11, category: "cat_weaning", title: t("tips.tip11.title"), content: t("tips.tip11.content") },
    { id: 17, category: "cat_weaning", title: t("tips.tip17.title"), content: t("tips.tip17.content") },
    { id: 20, category: "cat_feeding", title: t("tips.tip20.title"), content: t("tips.tip20.content") },
    { id: 21, category: "cat_feeding", title: t("tips.tip21.title"), content: t("tips.tip21.content") },
    { id: 22, category: "cat_feeding", title: t("tips.tip22.title"), content: t("tips.tip22.content") },
    { id: 26, category: "cat_feeding", title: t("tips.tip26.title"), content: t("tips.tip26.content") },
    { id: 28, category: "cat_feeding", title: t("tips.tip28.title"), content: t("tips.tip28.content") },
    { id: 34, category: "cat_breeding", title: t("tips.tip34.title"), content: t("tips.tip34.content") },
    { id: 35, category: "cat_breeding", title: t("tips.tip35.title"), content: t("tips.tip35.content") },
    { id: 39, category: "cat_breeding", title: t("tips.tip39.title"), content: t("tips.tip39.content") },
    { id: 43, category: "cat_breeding", title: t("tips.tip43.title"), content: t("tips.tip43.content") },
    { id: 47, category: "cat_breeding", title: t("tips.tip47.title"), content: t("tips.tip47.content") },
    { id: 48, category: "cat_health", title: t("tips.tip48.title"), content: t("tips.tip48.content") },
    { id: 49, category: "cat_health", title: t("tips.tip49.title"), content: t("tips.tip49.content") },
    { id: 51, category: "cat_health", title: t("tips.tip51.title"), content: t("tips.tip51.content") },
    { id: 55, category: "cat_health", title: t("tips.tip55.title"), content: t("tips.tip55.content") },
    { id: 60, category: "cat_health", title: t("tips.tip60.title"), content: t("tips.tip60.content") },
    { id: 61, category: "cat_housing", title: t("tips.tip61.title"), content: t("tips.tip61.content") },
    { id: 62, category: "cat_housing", title: t("tips.tip62.title"), content: t("tips.tip62.content") },
    { id: 75, category: "cat_waste", title: t("tips.tip75.title"), content: t("tips.tip75.content") },
    { id: 76, category: "cat_waste", title: t("tips.tip76.title"), content: t("tips.tip76.content") },
    { id: 92, category: "cat_general", title: t("tips.tip92.title"), content: t("tips.tip92.content") },
    { id: 94, category: "cat_general", title: t("tips.tip94.title"), content: t("tips.tip94.content") }
  ];

  const [videos, setVideos] = useState<TrainingVideo[]>([]);
  const [videosLoading, setVideosLoading] = useState(true);

  // UI States
  const [activeCategory, setActiveCategory] = useState<string>("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [expandedTips, setExpandedTips] = useState<Record<number, boolean>>({});
  const [expandedCategories, setExpandedCategories] = useState<Record<string, boolean>>({});

  // Add video form states
  const [showAddForm, setShowAddForm] = useState(false);
  const [newTitle, setNewTitle] = useState("");
  const [newYoutubeLink, setNewYoutubeLink] = useState("");
  const [addLoading, setAddLoading] = useState(false);

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  // Listen to training videos
  useEffect(() => {
    if (!user) return;

    setVideosLoading(true);
    const videosQuery = collection(db, "training_videos");
    const unsubscribe = onSnapshot(
      videosQuery,
      (snapshot) => {
        const list = snapshot.docs.map((doc) => ({
          id: doc.id,
          ...doc.data(),
        })) as TrainingVideo[];

        if (list.length === 0) {
          seedDefaultVideos();
        } else {
          setVideos(list.sort((a, b) => a.createdAt - b.createdAt));
          setVideosLoading(false);
        }
      },
      (error) => {
        console.error("Error loading videos:", error);
        setVideosLoading(false);
      }
    );

    return () => unsubscribe();
  }, [user]);

  const seedDefaultVideos = async () => {
    try {
      const defaults = [
        { id: "v1", title: "Modern Pig Farming Basics", youtubeId: "q_v_tYp6V8M", createdAt: Date.now() },
        { id: "v2", title: "Advanced Feeding Guide", youtubeId: "d6p-T8S8pS0", createdAt: Date.now() + 10 },
        { id: "v3", title: "Pig Housing Design", youtubeId: "L-9jYkR_S6k", createdAt: Date.now() + 20 },
        { id: "v4", title: "Health and Vaccination", youtubeId: "W6H3-F_P-zU", createdAt: Date.now() + 30 }
      ];

      for (const v of defaults) {
        await setDoc(doc(db, "training_videos", v.id), v);
      }
    } catch (err) {
      console.error("Failed to seed default videos:", err);
    }
  };

  const extractYoutubeId = (input: string): string => {
    const trimmed = input.trim();
    if (trimmed.length === 11) return trimmed;
    const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|\&v=)([^#\&\?]*).*/;
    const match = trimmed.match(regExp);
    return (match && match[2].length === 11) ? match[2] : "";
  };

  const handleAddVideo = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim() || !newYoutubeLink.trim()) return;

    const yId = extractYoutubeId(newYoutubeLink);
    if (!yId) {
      alert("Invalid YouTube link or ID. Please check URL.");
      return;
    }

    setAddLoading(true);
    try {
      const newRef = doc(collection(db, "training_videos"));
      const video: TrainingVideo = {
        id: newRef.id,
        title: newTitle.trim(),
        youtubeId: yId,
        createdAt: Date.now()
      };
      await setDoc(newRef, video);

      setNewTitle("");
      setNewYoutubeLink("");
      setShowAddForm(false);
    } catch (err) {
      console.error("Failed to add video:", err);
      alert("Error adding video doc to database.");
    } finally {
      setAddLoading(false);
    }
  };

  const handleDeleteVideo = async (id: string) => {
    if (!confirm(t("confirmDelete"))) return;
    try {
      await deleteDoc(doc(db, "training_videos", id));
    } catch (err) {
      console.error("Failed to delete video:", err);
    }
  };

  // Filter tips
  const getFilteredTips = (catKey: string) => {
    return MANUAL_TIPS.filter((tip) => {
      const matchesCat = tip.category === catKey;
      const matchesSearch =
        tip.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        tip.content.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesCat && matchesSearch;
    });
  };

  if (loading) {
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
        <div className="fixed inset-0 z-0 flex items-center justify-center opacity-[0.1] pointer-events-none select-none">
          <img
            src="/app_logo.png"
            alt="Watermark Background Logo"
            className="w-full max-w-[1100px] max-h-[85vh] object-contain"
          />
        </div>
      )}

      <div className="relative z-10 flex flex-col min-h-screen">
        {!isMobile && <DesktopHeader />}

        {/* Content Body */}
        <main className="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-12">

          {/* Section 1: Quick Farming Tips */}
          <section className="space-y-6">
            <div className="text-center space-y-2">
              <h2 className="text-2xl font-black text-emerald-600 uppercase tracking-tight">{t("title")}</h2>
              <div className="h-1 w-24 bg-emerald-500 mx-auto rounded-full opacity-30" />
            </div>

            {/* Search and Filters */}
            <div className="space-y-4">
              <div className="flex flex-col sm:flex-row gap-4 items-center justify-between">
                {/* Horizontal scrollable categories */}
                <div className="hidden lg:flex overflow-x-auto pb-2 gap-2 w-full no-scrollbar">
                  {CATEGORIES.map((cat) => (
                    <button
                      key={cat.key}
                      onClick={() => setActiveCategory(cat.key)}
                      className={`px-4 py-2 rounded-full text-xs font-bold border whitespace-nowrap transition duration-300 ${
                        activeCategory === cat.key
                          ? "bg-emerald-600 text-white border-emerald-600 shadow-md transform scale-105"
                          : "bg-white text-zinc-500 border-zinc-200 hover:border-emerald-300 hover:text-emerald-600"
                      }`}
                    >
                      {cat.label}
                    </button>
                  ))}
                </div>

                <div className="relative w-full sm:w-64">
                  <input
                    type="text"
                    placeholder={t("searchTips")}
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full rounded-xl border border-zinc-200 bg-white/70 pl-9 pr-4 py-2 text-xs font-semibold focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                  <svg className="absolute left-3 top-2.5 h-4 w-4 text-zinc-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </div>
              </div>

              {/* Tips List Display */}
              <div className="space-y-4 min-h-[300px]">
                {activeCategory === "all" ? (
                  CATEGORIES.filter(c => c.key !== "all").map((cat) => {
                    const catTips = getFilteredTips(cat.key);
                    if (catTips.length === 0 && searchQuery) return null;
                    const isExpanded = expandedCategories[cat.key];

                    return (
                      <div key={cat.key} className="space-y-3">
                        <button
                          onClick={() => setExpandedCategories(prev => ({ ...prev, [cat.key]: !isExpanded }))}
                          className="w-full flex justify-between items-center py-2 px-1 border-b border-zinc-100 group"
                        >
                          <h3 className="text-sm font-black text-zinc-800 uppercase tracking-widest group-hover:text-emerald-600 transition">
                            {cat.label}
                          </h3>
                          <div className="text-zinc-400 group-hover:text-emerald-500 transition">
                            {isExpanded ? (
                              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 15l7-7 7 7" /></svg>
                            ) : (
                              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M19 9l-7 7-7-7" /></svg>
                            )}
                          </div>
                        </button>
                        {isExpanded && (
                          <div className="grid grid-cols-1 gap-3 pl-2">
                            {catTips.map(tip => (
                              <TipCard
                                key={tip.id}
                                tip={tip}
                                isExpanded={expandedTips[tip.id]}
                                onToggle={() => setExpandedTips(prev => ({ ...prev, [tip.id]: !prev[tip.id] }))}
                              />
                            ))}
                          </div>
                        )}
                      </div>
                    );
                  })
                ) : (
                  <div className="grid grid-cols-1 gap-4">
                    {getFilteredTips(activeCategory).length === 0 ? (
                      <div className="text-center py-20 bg-zinc-50/50 rounded-3xl border-2 border-dashed border-zinc-100">
                        <p className="text-zinc-400 font-bold text-sm">{t("noTipsFound")}</p>
                      </div>
                    ) : (
                      getFilteredTips(activeCategory).map(tip => (
                        <TipCard
                          key={tip.id}
                          tip={tip}
                          isExpanded={expandedTips[tip.id]}
                          onToggle={() => setExpandedTips(prev => ({ ...prev, [tip.id]: !prev[tip.id] }))}
                        />
                      ))
                    )}
                  </div>
                )}
              </div>
            </div>
          </section>

          {/* Section 2: Video Tutorials */}
          <section className="space-y-8">
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="text-center sm:text-left space-y-2">
                <h2 className="text-2xl font-black text-emerald-600 uppercase tracking-tight">{t("videoTutorials")}</h2>
                <div className="h-1 w-24 bg-emerald-500 rounded-full opacity-30 mx-auto sm:mx-0" />
              </div>
              <button
                onClick={() => setShowAddForm(!showAddForm)}
                className="flex items-center gap-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 px-4 py-2 text-xs font-bold text-white shadow-lg shadow-emerald-200 transition active:scale-95"
              >
                {showAddForm ? t("closeForm") : t("addVideo")}
              </button>
            </div>

            {/* Add Video Form */}
            {showAddForm && (
              <form onSubmit={handleAddVideo} className="bg-zinc-50/80 backdrop-blur-sm border border-zinc-200 p-6 rounded-3xl space-y-4 shadow-inner max-w-2xl mx-auto">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[11px] font-black text-zinc-500 uppercase mb-1.5 ml-1">{t("tutorialTitle")}</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Nursery setup"
                      value={newTitle}
                      onChange={(e) => setNewTitle(e.target.value)}
                      className="w-full rounded-xl border border-zinc-250 bg-white px-4 py-2.5 text-xs font-semibold focus:border-emerald-500 focus:outline-none shadow-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-black text-zinc-500 uppercase mb-1.5 ml-1">{t("youtubeLink")}</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. https://youtu.be/..."
                      value={newYoutubeLink}
                      onChange={(e) => setNewYoutubeLink(e.target.value)}
                      className="w-full rounded-xl border border-zinc-250 bg-white px-4 py-2.5 text-xs font-semibold focus:border-emerald-500 focus:outline-none shadow-sm"
                    />
                  </div>
                </div>
                <button
                  type="submit"
                  disabled={addLoading}
                  className="w-full py-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-black uppercase tracking-widest shadow-md transition disabled:bg-zinc-300"
                >
                  {addLoading ? t("saving") : t("saveTutorial")}
                </button>
              </form>
            )}

            {/* Horizontal Video List */}
            <div className="relative group">
              <div className="flex overflow-x-auto pb-8 pt-2 gap-6 no-scrollbar snap-x scroll-smooth">
                {videosLoading ? (
                  <div className="flex gap-6">
                    {[...Array(3)].map((_, i) => (
                      <div key={i} className="flex-shrink-0 w-72 h-56 bg-zinc-100 animate-pulse rounded-3xl" />
                    ))}
                  </div>
                ) : videos.length === 0 ? (
                  <div className="w-full text-center py-16 bg-zinc-50/50 rounded-3xl border-2 border-dashed border-zinc-100">
                    <p className="text-zinc-400 font-bold text-sm">{t("noVideos")}</p>
                  </div>
                ) : (
                  videos.map((vid) => (
                    <VideoCard key={vid.id} video={vid} onDelete={handleDeleteVideo} />
                  ))
                )}
              </div>
              {/* Fade edges */}
              <div className="absolute top-0 right-0 h-full w-12 bg-gradient-to-l from-white to-transparent pointer-events-none opacity-0 group-hover:opacity-100 transition-opacity" />
              <div className="absolute top-0 left-0 h-full w-12 bg-gradient-to-r from-white to-transparent pointer-events-none opacity-0 group-hover:opacity-100 transition-opacity" />
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}

function TipCard({ tip, isExpanded, onToggle }: { tip: Tip; isExpanded: boolean; onToggle: () => void }) {
  return (
    <div
      className={`group bg-white/60 border rounded-2xl overflow-hidden transition-all duration-300 cursor-pointer ${
        isExpanded
          ? "border-emerald-500 ring-4 ring-emerald-50 shadow-xl"
          : "border-zinc-200 hover:border-emerald-300 shadow-sm"
      }`}
      onClick={onToggle}
    >
      <div className="p-5 flex justify-between items-center gap-4">
        <h4 className={`text-sm font-bold transition-colors ${isExpanded ? "text-emerald-700" : "text-zinc-800 group-hover:text-emerald-600"}`}>
          {tip.title}
        </h4>
        <div className={`transition-transform duration-300 ${isExpanded ? "text-emerald-500 rotate-180" : "text-zinc-300 group-hover:text-emerald-400"}`}>
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M19 9l-7 7-7-7" />
          </svg>
        </div>
      </div>
      {isExpanded && (
        <div className="px-5 pb-5 animate-in slide-in-from-top-2 duration-300">
          <div className="h-px w-full bg-zinc-100 mb-4" />
          <p className="text-xs text-zinc-650 leading-relaxed font-semibold">
            {tip.content}
          </p>
        </div>
      )}
    </div>
  );
}

function VideoCard({ video, onDelete }: { video: TrainingVideo; onDelete: (id: string) => void }) {
  return (
    <div className="snap-center flex-shrink-0 w-72 bg-white border border-zinc-200 rounded-3xl overflow-hidden shadow-sm hover:shadow-xl hover:border-emerald-300 hover:-translate-y-1 transition-all duration-300 group">
      <div className="relative aspect-video bg-zinc-900 flex items-center justify-center overflow-hidden">
         <img
            src={`https://img.youtube.com/vi/${video.youtubeId}/mqdefault.jpg`}
            alt={video.title}
            className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110 opacity-90"
         />
         <div className="absolute inset-0 bg-black/20 flex items-center justify-center group-hover:bg-black/40 transition-colors">
            <div className="w-14 h-14 bg-emerald-600 rounded-full flex items-center justify-center text-white shadow-2xl transform scale-90 group-hover:scale-100 transition-transform duration-300">
              <svg className="w-8 h-8 ml-1" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clipRule="evenodd" />
              </svg>
            </div>
         </div>
         <a
            href={`https://www.youtube.com/watch?v=${video.youtubeId}`}
            target="_blank"
            rel="noopener noreferrer"
            className="absolute inset-0 z-10"
         />
      </div>
      <div className="p-5 flex justify-between items-start gap-4">
        <h4 className="text-[13px] font-black text-zinc-800 leading-tight line-clamp-2 group-hover:text-emerald-700 transition-colors">
          {video.title}
        </h4>
        <button
          onClick={(e) => { e.preventDefault(); onDelete(video.id); }}
          className="p-2 rounded-lg text-zinc-300 hover:text-rose-500 hover:bg-rose-50 transition-all active:scale-90"
          title="Remove video"
        >
           <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
           </svg>
        </button>
      </div>
    </div>
  );
}
