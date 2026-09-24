"use client";

import React, { useState, useMemo } from "react";
import { useTranslations } from "next-intl";
import { ALL_TRAINING_TIPS, TrainingTip } from "@/lib/trainingTipsData";

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

const SearchIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" {...props}>
    <circle cx="11" cy="11" r="8" />
    <line x1="21" y1="21" x2="16.65" y2="16.65" />
  </svg>
);

interface TipCategoryDef {
  key: string;
  defaultName: string;
}

const CATEGORIES: TipCategoryDef[] = [
  { key: "cat_weaning", defaultName: "Weaning Management" },
  { key: "cat_feeding", defaultName: "Feed Management & Nutrition" },
  { key: "cat_breeding", defaultName: "Breeding & Genetics" },
  { key: "cat_health", defaultName: "Disease Prevention & Bio-Security" },
  { key: "cat_housing", defaultName: "Housing & Environmental Control" },
  { key: "cat_general", defaultName: "General Operations" },
];

export default function InlineTipsSection() {
  const t = useTranslations("Training");

  const [openCategory, setOpenCategory] = useState<string | null>("cat_weaning");
  const [searchQuery, setSearchQuery] = useState("");
  const [expandedTipId, setExpandedTipId] = useState<number | null>(null);

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

  const toggleCategory = (catKey: string) => {
    setOpenCategory((prev) => {
      const next = prev === catKey ? null : catKey;
      if (next) {
        scrollToSubOption(next, 150);
      }
      return next;
    });
  };

  const toggleTip = (id: number) => {
    setExpandedTipId((prev) => (prev === id ? null : id));
  };

  const getCategoryLabel = (cat: TipCategoryDef) => {
    const rawKey = cat.key.replace("cat_", "");
    try {
      const trans = t(`categories.${rawKey}`);
      if (trans && !trans.includes("categories.")) return trans;
    } catch (_) {}
    return cat.defaultName;
  };

  const tipsWithContent = useMemo(() => {
    return ALL_TRAINING_TIPS.map((tip) => {
      let title = tip.defaultTitle;
      let content = tip.defaultContent;
      try {
        const transTitle = t(`tips.tip${tip.id}.title`);
        const transContent = t(`tips.tip${tip.id}.content`);
        if (transTitle && !transTitle.includes("tips.tip")) title = transTitle;
        if (transContent && !transContent.includes("tips.tip")) content = transContent;
      } catch (_) {}

      return {
        ...tip,
        title,
        content,
      };
    });
  }, [t]);

  const filteredTips = useMemo(() => {
    if (!searchQuery.trim()) return tipsWithContent;
    const q = searchQuery.toLowerCase();
    return tipsWithContent.filter(
      (tip) =>
        tip.title.toLowerCase().includes(q) ||
        tip.content.toLowerCase().includes(q)
    );
  }, [tipsWithContent, searchQuery]);

  return (
    <div className="space-y-4">
      {/* Search Input */}
      <div className="relative">
        <input
          type="text"
          placeholder={t("searchTips") || "Search farming tips..."}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full rounded-xl border border-stone-200 bg-white pl-9 pr-8 py-2.5 text-xs font-semibold text-zinc-900 focus:border-[#5D4037] focus:outline-none focus:ring-1 focus:ring-[#5D4037] shadow-xs"
        />
        <SearchIcon className="absolute left-3 top-3 h-4 w-4 text-stone-400 pointer-events-none" />
        {searchQuery && (
          <button
            type="button"
            onClick={() => setSearchQuery("")}
            className="absolute right-3 top-2.5 text-xs text-stone-400 hover:text-stone-600 font-bold"
          >
            ✕
          </button>
        )}
      </div>

      {/* If Searching, show matched tips list */}
      {searchQuery.trim() ? (
        <div className="space-y-2.5">
          <p className="text-xs font-bold text-[#5D4037]">
            Search Results ({filteredTips.length}):
          </p>
          {filteredTips.length === 0 ? (
            <div className="p-6 text-center text-xs text-stone-400 bg-white rounded-xl border border-stone-200">
              {t("noTipsFound") || "No matching tips found."}
            </div>
          ) : (
            filteredTips.map((tip) => (
              <TipCardItem
                key={tip.id}
                tip={tip}
                isExpanded={expandedTipId === tip.id}
                onToggle={() => toggleTip(tip.id)}
              />
            ))
          )}
        </div>
      ) : (
        /* 6 Category Accordions matching Android TrainingSectionContent.kt */
        <div className="space-y-2.5">
          {CATEGORIES.map((cat) => {
            const catTips = tipsWithContent.filter((t) => t.categoryKey === cat.key);
            const isOpen = openCategory === cat.key;
            const categoryTitle = getCategoryLabel(cat);

            return (
              <div
                key={cat.key}
                id={`suboption-${cat.key}`}
                className={`scroll-mt-20 bg-white border rounded-xl overflow-hidden shadow-xs transition-all ${
                  isOpen ? "border-[#5D4037]/50 shadow-sm" : "border-stone-200 hover:border-stone-300"
                }`}
              >
                {/* Accordion Category Header */}
                <button
                  type="button"
                  onClick={() => toggleCategory(cat.key)}
                  className={`w-full p-4 flex items-center justify-between text-left transition-colors ${
                    isOpen ? "bg-[#EFEBE9]/60" : "hover:bg-stone-50/70"
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <span className="h-7 w-7 rounded-lg bg-[#5D4037]/10 text-[#5D4037] flex items-center justify-center text-xs font-black shrink-0">
                      {catTips.length}
                    </span>
                    <span className="text-xs sm:text-sm font-bold text-zinc-900">
                      {categoryTitle}
                    </span>
                  </div>
                  <ChevronDownIcon
                    className={`h-5 w-5 text-stone-400 transform transition-transform duration-300 ${
                      isOpen ? "rotate-180 text-[#5D4037]" : ""
                    }`}
                  />
                </button>

                {/* Category Tips Content */}
                {isOpen && (
                  <div className="border-t border-[#5D4037]/15 p-3.5 bg-[#EFEBE9]/20 space-y-2 animate-fadeIn">
                    {catTips.map((tip) => (
                      <TipCardItem
                        key={tip.id}
                        tip={tip}
                        isExpanded={expandedTipId === tip.id}
                        onToggle={() => toggleTip(tip.id)}
                      />
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function TipCardItem({
  tip,
  isExpanded,
  onToggle,
}: {
  tip: { id: number; title: string; content: string; categoryKey: string };
  isExpanded: boolean;
  onToggle: () => void;
}) {
  return (
    <div className="bg-white border border-stone-200/90 rounded-xl overflow-hidden shadow-xs transition hover:border-[#5D4037]/30">
      <button
        type="button"
        onClick={onToggle}
        className="w-full p-3.5 flex items-start justify-between text-left hover:bg-stone-50/50 transition"
      >
        <div className="space-y-0.5 pr-2">
          <h5 className="text-xs sm:text-sm font-bold text-[#5D4037]">{tip.title}</h5>
        </div>
        <ChevronDownIcon
          className={`h-4 w-4 text-stone-400 shrink-0 transform transition-transform ${
            isExpanded ? "rotate-180 text-[#5D4037]" : ""
          }`}
        />
      </button>

      {isExpanded && (
        <div className="border-t border-stone-100 p-3.5 bg-stone-50/50 text-xs sm:text-sm text-zinc-700 leading-relaxed animate-fadeIn">
          {tip.content}
        </div>
      )}
    </div>
  );
}
