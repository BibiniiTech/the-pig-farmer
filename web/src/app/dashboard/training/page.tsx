"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { collection, onSnapshot, doc, setDoc, deleteDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import NavbarDropdown from "@/components/NavbarDropdown";

interface TrainingVideo {
  id: string;
  title: string;
  youtubeId: string;
  createdAt: number;
}

interface Tip {
  id: number;
  category: "weaning" | "feeding" | "health" | "breeding" | "general" | "housing" | "waste";
  title: string;
  content: string;
}

// Sample of highly informative swine husbandry tips matching Android database
const MANUAL_TIPS: Tip[] = [
  // Weaning
  {
    id: 6,
    category: "weaning",
    title: "Weaning Age for Smallholders",
    content: "Wean piglets at 28 to 35 days instead of earlier. This allows piglets to grow stronger and adapt to solid feed using simple farm resources."
  },
  {
    id: 7,
    category: "weaning",
    title: "Gradual Separation Method",
    content: "Reduce weaning stress by moving the sow out of the farrowing pen and leaving the piglets in their familiar environment for the first week."
  },
  {
    id: 8,
    category: "weaning",
    title: "Draft Prevention for Weaners",
    content: "Block cold winds in the nursery pen by installing temporary curtains or wooden boards up to piglet-height (about 50cm)."
  },
  {
    id: 11,
    category: "weaning",
    title: "Homemade Rehydration Solution",
    content: "Feed a cheap, homemade electrolyte solution (5 tablespoons sugar, 1/2 tablespoon salt in 5 liters clean water) during the first 3 days of weaning to combat stress."
  },
  {
    id: 17,
    category: "weaning",
    title: "Clay Soil Mineral Supplement",
    content: "Put a clean shovel of fresh, red clay soil (from a clean area) in the farrowing/weaning pen. Piglets eat it to get natural iron and minerals."
  },

  // Feeding & Nutrition
  {
    id: 20,
    category: "feeding",
    title: "Agricultural Byproduct Boiling",
    content: "Boil agricultural byproducts like yam peels, cassava peels, and potato vines to destroy natural toxins and improve digestibility."
  },
  {
    id: 21,
    category: "feeding",
    title: "Local Protein Sources",
    content: "Supplement commercial feed by growing high-protein forage crops like Moringa, Azolla, or sweet potato leaves on the farm."
  },
  {
    id: 22,
    category: "feeding",
    title: "Gestation Feed Restriction",
    content: "Limit gestating sows to 2-2.5 kg of feed per day to prevent them from becoming overweight, which causes difficult births."
  },
  {
    id: 26,
    category: "feeding",
    title: "Boiling Kitchen Waste",
    content: "Boil hotel or kitchen food waste for 30 minutes to destroy African Swine Fever and Foot-and-Mouth disease viruses."
  },
  {
    id: 28,
    category: "feeding",
    title: "Charcoal for Digestion",
    content: "Place small pieces of wood charcoal in grower pens. Pigs chew it to help bind stomach toxins and reduce diarrhea."
  },

  // Breeding & Farrowing
  {
    id: 34,
    category: "breeding",
    title: "Hand Mating in Breeding Pen",
    content: "Bring the sow in heat to the boar's pen for breeding, rather than letting the boar run loose in the sow herd."
  },
  {
    id: 35,
    category: "breeding",
    title: "Standing Heat Response",
    content: "Test for heat by pressing your hands firmly on the sow's back; if she stands completely still and stiffens her ears, she is ready to mate."
  },
  {
    id: 39,
    category: "breeding",
    title: "DIY Crushed Piglet Protection",
    content: "Install \"guard rails\" (heavy wooden poles or metal pipes placed 20cm out from the walls and 20cm off the floor) in farrowing pens."
  },
  {
    id: 43,
    category: "breeding",
    title: "Breeding Records Calendar",
    content: "Mark the breeding date on a calendar; pregnancy lasts 114 days (3 months, 3 weeks, 3 days), allowing you to prepare the farrowing pen."
  },
  {
    id: 47,
    category: "breeding",
    title: "Gilt First Mating Age",
    content: "Wait until a gilt is at least 8 months old and on her second or third heat cycle before mating her to ensure a larger first litter."
  },

  // Biosecurity & Health
  {
    id: 48,
    category: "health",
    title: "Isolation of New Purchases",
    content: "Keep newly purchased pigs in a separate pen at least 20 meters away from your main herd for 30 days to check for sickness."
  },
  {
    id: 49,
    category: "health",
    title: "DIY Footbath Setup",
    content: "Place a shallow plastic tub or half-cut tire filled with water and disinfectant (like chlorine or agricultural lime) at the farm gate."
  },
  {
    id: 51,
    category: "health",
    title: "Visitor Restriction Policy",
    content: "Do not allow visitors (especially other pig buyers or farmers) inside your pig pens. Discuss business outside the pen area."
  },
  {
    id: 55,
    category: "health",
    title: "Natural Parasite Control",
    content: "Feed crushed papaya seeds or pumpkin seeds to grower pigs as a cheap, natural remedy to help reduce internal worms."
  },
  {
    id: 60,
    category: "health",
    title: "Proper Burial of Mortalities",
    content: "Bury dead pigs at least 2 meters deep, far away from water sources, and cover the carcass with agricultural lime before refilling the soil."
  },

  // Housing, Waste & General
  {
    id: 61,
    category: "housing",
    title: "Low-Cost Pen Space",
    content: "Provide at least 1 square meter of space per finisher pig to reduce fighting, stress, and skin lesions."
  },
  {
    id: 62,
    category: "housing",
    title: "Natural Cross-Ventilation",
    content: "Design pig houses with open sides (using wire mesh or bamboo slats) to allow natural breeze to blow through and remove odors."
  },
  {
    id: 75,
    category: "waste",
    title: "DIY Compost Piles",
    content: "Pile pig manure in a designated dry corner, cover it with banana leaves or plastic, and turn it weekly with a shovel to make compost."
  },
  {
    id: 76,
    category: "waste",
    title: "Flexible Bag Biogas",
    content: "Install a simple, low-cost plastic tubular biogas digester to treat pig manure, producing free cooking gas for the household."
  },
  {
    id: 92,
    category: "general",
    title: "Castration Age Limit",
    content: "Castrate male piglets before they are 7 days old. At this age, the procedure is fast, heals quickly, and causes minimal pain."
  },
  {
    id: 94,
    category: "general",
    title: "Simple Notebook Records",
    content: "Keep a small, cheap notebook in the barn to write down breeding dates, farrowing dates, and treatments for each sow."
  }
];

export default function TrainingPage() {
  const { user, activeFarmUid, loading } = useAuth();
  const router = useRouter();

  const [videos, setVideos] = useState<TrainingVideo[]>([]);
  const [videosLoading, setVideosLoading] = useState(true);

  // Manual states
  const [activeCategory, setActiveCategory] = useState<string>("weaning");
  const [searchQuery, setSearchQuery] = useState("");

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

        // Seed defaults if empty
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

      // Reset
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
    if (!confirm("Are you sure you want to delete this training video tutorial?")) return;
    try {
      await deleteDoc(doc(db, "training_videos", id));
    } catch (err) {
      console.error("Failed to delete video:", err);
    }
  };

  // Filter tips
  const filteredTips = MANUAL_TIPS.filter((tip) => {
    const matchesCat = tip.category === activeCategory;
    const matchesSearch =
      tip.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      tip.content.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCat && matchesSearch;
  });

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
      <div className="fixed inset-0 z-0 flex items-center justify-center opacity-[0.15] pointer-events-none select-none">
        <img
          src="/app_logo.png"
          alt="Watermark Background Logo"
          className="w-full max-w-[1100px] max-h-[85vh] object-contain"
        />
      </div>

      <div className="relative z-10 flex flex-col min-h-screen">
        {/* Header */}
        <header className="border-b border-zinc-200 bg-white/80 backdrop-blur-md sticky top-0 z-50">
          <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <img src="/app_logo.png" alt="SmartSwine Logo" className="h-8 w-8 object-contain rounded-md" />
              <NavbarDropdown />
            </div>
            <Link
              href="/dashboard"
              className="text-xs font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200/50 px-3 py-1.5 rounded-lg transition duration-200"
            >
              Back to Home
            </Link>
          </div>
        </header>

        {/* Content Body */}
        <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
          <div className="bg-white/85 backdrop-blur-sm border border-zinc-200 rounded-2xl p-6 shadow-sm">
            <h2 className="text-xl font-bold text-zinc-900">Husbandry Tips & Expert Video Guides</h2>
            <p className="text-sm text-zinc-500 mt-1">
              Explore best practices in modern swine management or upload video links for staff training and visual guidance.
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Left Column: Husbandry Manual Tips (Span 7) */}
            <div className="lg:col-span-7 bg-white/80 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <h3 className="text-base font-bold text-zinc-900 flex items-center gap-2">
                  <span>📖</span> Pig Management Manual
                </h3>
                {/* Search Bar */}
                <input
                  type="text"
                  placeholder="Search manual guides..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="rounded-xl border border-zinc-200 bg-white/70 px-4 py-1.5 text-xs font-semibold focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500"
                />
              </div>

              {/* Categories Navigation Grid */}
              <div className="grid grid-cols-3 sm:grid-cols-6 gap-2">
                {[
                  { key: "weaning", label: "Weaning" },
                  { key: "feeding", label: "Feeding" },
                  { key: "breeding", label: "Breeding" },
                  { key: "health", label: "Health" },
                  { key: "housing", label: "Housing" },
                  { key: "waste", label: "Waste" }
                ].map((cat) => (
                  <button
                    key={cat.key}
                    onClick={() => {
                      setActiveCategory(cat.key);
                      setSearchQuery("");
                    }}
                    className={`py-2 rounded-xl text-[11px] font-bold border transition duration-300 ${
                      activeCategory === cat.key
                        ? "bg-violet-600 text-white border-violet-600 shadow-sm"
                        : "bg-white text-zinc-600 border-zinc-200 hover:bg-zinc-50"
                    }`}
                  >
                    {cat.label}
                  </button>
                ))}
              </div>

              {/* Tips content list */}
              <div className="space-y-4">
                {filteredTips.length === 0 ? (
                  <p className="text-xs text-zinc-400 font-semibold text-center py-12 bg-zinc-50/20 border border-dashed border-zinc-200 rounded-xl">
                    No matching guides found in this category.
                  </p>
                ) : (
                  filteredTips.map((tip) => (
                    <div
                      key={tip.id}
                      className="p-5 rounded-2xl border border-zinc-200 bg-white/95 hover:border-violet-500/25 transition duration-300 shadow-sm space-y-2"
                    >
                      <h4 className="text-sm font-extrabold text-zinc-900">{tip.title}</h4>
                      <p className="text-xs text-zinc-650 leading-relaxed font-semibold">{tip.content}</p>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Right Column: YouTube Tutorials List (Span 5) */}
            <div className="lg:col-span-5 bg-white/80 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-6">
              <div className="flex justify-between items-center">
                <h3 className="text-base font-bold text-zinc-900 flex items-center gap-2">
                  <span>🎥</span> Video Tutorials
                </h3>
                <button
                  onClick={() => setShowAddForm(!showAddForm)}
                  className="rounded-lg bg-violet-50 hover:bg-violet-100 border border-violet-200 px-3 py-1 text-xs font-bold text-violet-700 transition"
                >
                  {showAddForm ? "Close Form" : "Add Video"}
                </button>
              </div>

              {/* Add Video Form */}
              {showAddForm && (
                <form onSubmit={handleAddVideo} className="bg-zinc-50/70 border border-zinc-200 p-4 rounded-xl space-y-4 shadow-inner">
                  <h4 className="text-xs font-bold text-zinc-700">Add New Video Link</h4>
                  
                  <div>
                    <label className="block text-[11px] font-bold text-zinc-500 mb-1">Tutorial Title</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Nursery Farrowing setup"
                      value={newTitle}
                      onChange={(e) => setNewTitle(e.target.value)}
                      className="w-full rounded-lg border border-zinc-250 bg-white px-3 py-2 text-xs font-semibold focus:border-violet-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-[11px] font-bold text-zinc-500 mb-1">YouTube URL or Video ID</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. https://youtube.com/watch?v=..."
                      value={newYoutubeLink}
                      onChange={(e) => setNewYoutubeLink(e.target.value)}
                      className="w-full rounded-lg border border-zinc-250 bg-white px-3 py-2 text-xs font-semibold focus:border-violet-500 focus:outline-none"
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={addLoading}
                    className="w-full py-2 rounded-xl bg-violet-650 hover:bg-violet-750 text-white text-xs font-bold shadow transition disabled:bg-zinc-300"
                  >
                    {addLoading ? "Saving video..." : "Save Video Tutorial"}
                  </button>
                </form>
              )}

              {/* Video List */}
              <div className="space-y-6 max-h-[60vh] overflow-y-auto pr-1 no-scrollbar">
                {videosLoading ? (
                  <div className="space-y-4">
                    {[...Array(2)].map((_, i) => (
                      <div key={i} className="h-44 bg-zinc-100 animate-pulse rounded-xl" />
                    ))}
                  </div>
                ) : videos.length === 0 ? (
                  <p className="text-xs text-zinc-400 font-semibold text-center py-8">No video tutorials cataloged yet.</p>
                ) : (
                  videos.map((vid) => (
                    <div key={vid.id} className="border border-zinc-200 rounded-xl bg-white overflow-hidden shadow-sm space-y-3">
                      {/* Fluid Aspect Ratio YouTube Embed */}
                      <div className="relative w-full aspect-video bg-zinc-950">
                        <iframe
                          src={`https://www.youtube.com/embed/${vid.youtubeId}`}
                          title={vid.title}
                          frameBorder="0"
                          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                          allowFullScreen
                          className="absolute inset-0 w-full h-full"
                        />
                      </div>
                      
                      <div className="p-3.5 flex justify-between items-center gap-3">
                        <h4 className="text-xs font-bold text-zinc-800 leading-snug">{vid.title}</h4>
                        <button
                          onClick={() => handleDeleteVideo(vid.id)}
                          className="p-1.5 rounded-lg border border-zinc-150 hover:border-rose-200 text-zinc-400 hover:text-rose-600 hover:bg-rose-50/50 transition"
                          title="Remove video"
                        >
                          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
