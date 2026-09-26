"use client";

import React, { useState, useMemo, Suspense } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import { useDevice } from "@/context/DeviceContext";

type GuideCategory = "ALL" | "HERD" | "FEED" | "BREEDING" | "FINANCIALS" | "HEALTH" | "TOOLS";

interface GuideStep {
  stepNumber: number;
  titleKey: string;
  descriptionKey: string;
  tipKey?: string;
}

interface GuideTopic {
  id: string;
  titleKey: string;
  subtitleKey: string;
  category: GuideCategory;
  icon: string;
  actionLabelKey: string;
  actionRoute: string;
  indicationKeys: string[];
  steps: GuideStep[];
}

const TOPICS: GuideTopic[] = [
  {
    id: "herd_management",
    titleKey: "guide_herd_title",
    subtitleKey: "guide_herd_subtitle",
    category: "HERD",
    icon: "pig",
    actionLabelKey: "guide_action_open_herd",
    actionRoute: "/dashboard/herd?action=add",
    indicationKeys: ["indication_assign_roles", "indication_log_litter"],
    steps: [
      { stepNumber: 1, titleKey: "guide_herd_step1_title", descriptionKey: "guide_herd_step1_desc", tipKey: "guide_herd_step1_tip" },
      { stepNumber: 2, titleKey: "guide_herd_step2_title", descriptionKey: "guide_herd_step2_desc", tipKey: "guide_herd_step2_tip" },
      { stepNumber: 3, titleKey: "guide_herd_step3_title", descriptionKey: "guide_herd_step3_desc", tipKey: "guide_herd_step3_tip" },
      { stepNumber: 4, titleKey: "guide_herd_step4_title", descriptionKey: "guide_herd_step4_desc", tipKey: "guide_herd_step4_tip" }
    ]
  },
  {
    id: "feed_formulation",
    titleKey: "guide_feed_title",
    subtitleKey: "guide_feed_subtitle",
    category: "FEED",
    icon: "flask",
    actionLabelKey: "guide_action_open_feed",
    actionRoute: "/dashboard/feed/mix",
    indicationKeys: ["indication_select_stage", "indication_adjust_sliders", "indication_save_mix"],
    steps: [
      { stepNumber: 1, titleKey: "guide_feed_step1_title", descriptionKey: "guide_feed_step1_desc", tipKey: "guide_feed_step1_tip" },
      { stepNumber: 2, titleKey: "guide_feed_step2_title", descriptionKey: "guide_feed_step2_desc", tipKey: "guide_feed_step2_tip" },
      { stepNumber: 3, titleKey: "guide_feed_step3_title", descriptionKey: "guide_feed_step3_desc", tipKey: "guide_feed_step3_tip" },
      { stepNumber: 4, titleKey: "guide_feed_step4_title", descriptionKey: "guide_feed_step4_desc", tipKey: "guide_feed_step4_tip" }
    ]
  },
  {
    id: "breeding_and_activities",
    titleKey: "guide_breeding_title",
    subtitleKey: "guide_breeding_subtitle",
    category: "BREEDING",
    icon: "heart",
    actionLabelKey: "guide_action_open_activities",
    actionRoute: "/dashboard/activities",
    indicationKeys: ["indication_record_mating", "indication_gestation_timer", "indication_log_litter"],
    steps: [
      { stepNumber: 1, titleKey: "guide_breeding_step1_title", descriptionKey: "guide_breeding_step1_desc", tipKey: "guide_breeding_step1_tip" },
      { stepNumber: 2, titleKey: "guide_breeding_step2_title", descriptionKey: "guide_breeding_step2_desc", tipKey: "guide_breeding_step2_tip" },
      { stepNumber: 3, titleKey: "guide_breeding_step3_title", descriptionKey: "guide_breeding_step3_desc", tipKey: "guide_breeding_step3_tip" },
      { stepNumber: 4, titleKey: "guide_breeding_step4_title", descriptionKey: "guide_breeding_step4_desc", tipKey: "guide_breeding_step4_tip" }
    ]
  },
  {
    id: "financials_tracking",
    titleKey: "guide_finances_title",
    subtitleKey: "guide_finances_subtitle",
    category: "FINANCIALS",
    icon: "currency",
    actionLabelKey: "guide_action_open_financials",
    actionRoute: "/dashboard/financials?action=add",
    indicationKeys: ["indication_feed_benchmark", "indication_cop_breakeven"],
    steps: [
      { stepNumber: 1, titleKey: "guide_finances_step1_title", descriptionKey: "guide_finances_step1_desc", tipKey: "guide_finances_step1_tip" },
      { stepNumber: 2, titleKey: "guide_finances_step2_title", descriptionKey: "guide_finances_step2_desc", tipKey: "guide_finances_step2_tip" },
      { stepNumber: 3, titleKey: "guide_finances_step3_title", descriptionKey: "guide_finances_step3_desc", tipKey: "guide_finances_step3_tip" }
    ]
  },
  {
    id: "disease_finder",
    titleKey: "guide_health_title",
    subtitleKey: "guide_health_subtitle",
    category: "HEALTH",
    icon: "shield",
    actionLabelKey: "guide_action_open_disease_finder",
    actionRoute: "/dashboard?section=symptoms",
    indicationKeys: ["indication_select_symptoms"],
    steps: [
      { stepNumber: 1, titleKey: "guide_health_step1_title", descriptionKey: "guide_health_step1_desc", tipKey: "guide_health_step1_tip" },
      { stepNumber: 2, titleKey: "guide_health_step2_title", descriptionKey: "guide_health_step2_desc", tipKey: "guide_health_step2_tip" },
      { stepNumber: 3, titleKey: "guide_health_step3_title", descriptionKey: "guide_health_step3_desc", tipKey: "guide_health_step3_tip" }
    ]
  },
  {
    id: "weight_checker",
    titleKey: "guide_weight_title",
    subtitleKey: "guide_weight_subtitle",
    category: "TOOLS",
    icon: "scale",
    actionLabelKey: "guide_action_open_weight_checker",
    actionRoute: "/dashboard?section=weight&subOption=tape",
    indicationKeys: ["indication_enter_measurements", "indication_adg_forecast"],
    steps: [
      { stepNumber: 1, titleKey: "guide_weight_step1_title", descriptionKey: "guide_weight_step1_desc", tipKey: "guide_weight_step1_tip" },
      { stepNumber: 2, titleKey: "guide_weight_step2_title", descriptionKey: "guide_weight_step2_desc", tipKey: "guide_weight_step2_tip" },
      { stepNumber: 3, titleKey: "guide_weight_step3_title", descriptionKey: "guide_weight_step3_desc", tipKey: "guide_weight_step3_tip" }
    ]
  },
  {
    id: "human_resources",
    titleKey: "guide_hr_title",
    subtitleKey: "guide_hr_subtitle",
    category: "HERD",
    icon: "users",
    actionLabelKey: "guide_action_open_hr",
    actionRoute: "/dashboard?section=hr&action=add",
    indicationKeys: ["indication_assign_roles"],
    steps: [
      { stepNumber: 1, titleKey: "guide_hr_step1_title", descriptionKey: "guide_hr_step1_desc", tipKey: "guide_hr_step1_tip" },
      { stepNumber: 2, titleKey: "guide_hr_step2_title", descriptionKey: "guide_hr_step2_desc", tipKey: "guide_hr_step2_tip" },
      { stepNumber: 3, titleKey: "guide_hr_step3_title", descriptionKey: "guide_hr_step3_desc", tipKey: "guide_hr_step3_tip" }
    ]
  },
  {
    id: "offline_sync",
    titleKey: "guide_offline_title",
    subtitleKey: "guide_offline_subtitle",
    category: "TOOLS",
    icon: "cloud",
    actionLabelKey: "guide_action_open_dashboard",
    actionRoute: "/dashboard",
    indicationKeys: ["indication_offline_logging", "indication_cloud_sync"],
    steps: [
      { stepNumber: 1, titleKey: "guide_offline_step1_title", descriptionKey: "guide_offline_step1_desc", tipKey: "guide_offline_step1_tip" },
      { stepNumber: 2, titleKey: "guide_offline_step2_title", descriptionKey: "guide_offline_step2_desc", tipKey: "guide_offline_step2_tip" }
    ]
  }
];

const CATEGORIES: { id: GuideCategory; labelKey: string }[] = [
  { id: "ALL", labelKey: "category_all" },
  { id: "HERD", labelKey: "category_herd" },
  { id: "FEED", labelKey: "category_feed" },
  { id: "BREEDING", labelKey: "category_breeding" },
  { id: "FINANCIALS", labelKey: "category_financials" },
  { id: "HEALTH", labelKey: "category_health" },
  { id: "TOOLS", labelKey: "category_tools" }
];

function GuideIcon({ name, className = "h-5 w-5" }: { name: string; className?: string }) {
  switch (name) {
    case "pig":
      return (
        <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <ellipse cx="12" cy="13" rx="8" ry="6" />
          <circle cx="9" cy="11" r="1" fill="currentColor" />
          <circle cx="15" cy="11" r="1" fill="currentColor" />
          <ellipse cx="12" cy="14" rx="2.5" ry="1.5" />
          <path strokeLinecap="round" d="M6 8l-2-2m14 2l2-2" />
        </svg>
      );
    case "flask":
      return (
        <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" strokeLinejoin="round" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
        </svg>
      );
    case "heart":
      return (
        <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" strokeLinejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
        </svg>
      );
    case "currency":
      return (
        <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" strokeLinejoin="round" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      );
    case "shield":
      return (
        <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
        </svg>
      );
    case "scale":
      return (
        <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" strokeLinejoin="round" d="M3 6l3 1m0 0l-3 9a5.002 5.002 0 006.001 0M6 7l3 9M6 7l6-2m6 2l3-1m-3 1l-3 9a5.002 5.002 0 006.001 0M18 7l3 9m-3-9l-6-2m0-2v2m0 16V5m0 16H9m3 0h3" />
        </svg>
      );
    case "users":
      return (
        <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
      );
    case "cloud":
      return (
        <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" strokeLinejoin="round" d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 00-9.78 2.096A4.001 4.001 0 003 15z" />
        </svg>
      );
    default:
      return (
        <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      );
  }
}

function HowToGuideContent() {
  const t = useTranslations("Guide");
  const { isMobile } = useDevice();
  const router = useRouter();

  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState<GuideCategory>("ALL");
  const [expandedTopicId, setExpandedTopicId] = useState<string | null>("herd_management");
  const [showVisualMap, setShowVisualMap] = useState<Record<string, boolean>>({ herd_management: true });

  const filteredTopics = useMemo(() => {
    return TOPICS.filter((topic) => {
      const matchesCategory = selectedCategory === "ALL" || topic.category === selectedCategory;
      if (!matchesCategory) return false;

      if (!searchQuery.trim()) return true;

      const q = searchQuery.toLowerCase().trim();
      const title = (t(topic.titleKey) || "").toLowerCase();
      const subtitle = (t(topic.subtitleKey) || "").toLowerCase();
      const hasStepMatch = topic.steps.some(
        (s) =>
          (t(s.titleKey) || "").toLowerCase().includes(q) ||
          (t(s.descriptionKey) || "").toLowerCase().includes(q)
      );

      return title.includes(q) || subtitle.includes(q) || hasStepMatch;
    });
  }, [searchQuery, selectedCategory, t]);

  const toggleVisualGuide = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setShowVisualMap((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <div className="relative min-h-screen bg-zinc-50 dark:bg-zinc-950 text-zinc-900 dark:text-zinc-100 flex flex-col font-sans">
      {!isMobile && (
        <DesktopHeader
          label={t("how_to_guide") || "HOW-TO GUIDE"}
          showBack
          backPath="/dashboard"
          labelColor="text-[#1B5E20] dark:text-[#81C784]"
        />
      )}

      <main className="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 py-6 sm:py-8 space-y-6">
        {/* Top Header Back Row */}
        <div className="flex items-center gap-3">
          <button
            onClick={() => router.push("/dashboard")}
            className="inline-flex items-center gap-2 px-3 py-2 rounded-xl bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-700 dark:text-zinc-300 hover:text-zinc-900 dark:hover:text-white transition font-bold text-xs shadow-xs"
            aria-label="Back to Dashboard"
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
            </svg>
            <span>Back</span>
          </button>
          <h1 className="text-xl sm:text-2xl font-black text-[#1B5E20] dark:text-[#81C784] tracking-tight">
            {t("how_to_guide")}
          </h1>
        </div>

        {/* Intro Hero Banner */}
        <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-emerald-600 to-teal-800 text-white p-6 sm:p-8 shadow-md">
          <div className="absolute -right-6 -bottom-6 w-48 h-48 rounded-full bg-white/10 blur-2xl pointer-events-none" />
          <div className="relative z-10 flex items-start gap-4">
            <div className="h-12 w-12 rounded-2xl bg-white/20 backdrop-blur-md flex items-center justify-center shrink-0 border border-white/30 shadow-inner">
              <svg className="h-6 w-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
            <div className="space-y-1">
              <h2 className="text-lg sm:text-2xl font-black tracking-tight">
                {t("guide_intro_title")}
              </h2>
              <p className="text-xs sm:text-sm text-emerald-100 max-w-2xl leading-relaxed">
                {t("guide_intro_desc")}
              </p>
            </div>
          </div>
        </div>

        {/* Search Bar */}
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-zinc-400">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
              <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={t("guide_search_placeholder")}
            className="w-full pl-11 pr-10 py-3 rounded-xl bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-sm font-medium text-zinc-900 dark:text-zinc-100 placeholder-zinc-400 focus:outline-none focus:ring-2 focus:ring-emerald-500 shadow-xs transition"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery("")}
              className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-zinc-400 hover:text-zinc-600"
              aria-label={t("clear_all")}
            >
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          )}
        </div>

        {/* Category Filter Chips */}
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-1">
          {CATEGORIES.map((cat) => {
            const isSelected = selectedCategory === cat.id;
            return (
              <button
                key={cat.id}
                onClick={() => setSelectedCategory(cat.id)}
                className={`px-3.5 py-1.5 rounded-full text-xs font-bold whitespace-nowrap transition-all duration-200 ${
                  isSelected
                    ? "bg-emerald-600 text-white shadow-sm shadow-emerald-600/30"
                    : "bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-600 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800"
                }`}
              >
                {t(cat.labelKey)}
              </button>
            );
          })}
        </div>

        {/* Guide Topics Accordion List */}
        <div className="space-y-4">
          {filteredTopics.length === 0 ? (
            <div className="p-12 text-center bg-white dark:bg-zinc-900 rounded-2xl border border-zinc-200 dark:border-zinc-800">
              <p className="text-sm font-semibold text-zinc-400">
                {t("guide_no_results")}
              </p>
            </div>
          ) : (
            filteredTopics.map((topic) => {
              const isExpanded = expandedTopicId === topic.id;
              const isVisualOpen = showVisualMap[topic.id] ?? true;

              return (
                <div
                  key={topic.id}
                  className={`rounded-2xl border transition-all duration-300 overflow-hidden bg-white dark:bg-zinc-900 ${
                    isExpanded
                      ? "border-emerald-500 shadow-md ring-1 ring-emerald-500/20"
                      : "border-zinc-200 dark:border-zinc-800 hover:border-zinc-300 dark:hover:border-zinc-700 shadow-xs"
                  }`}
                >
                  {/* Topic Card Header */}
                  <button
                    type="button"
                    onClick={() => setExpandedTopicId(isExpanded ? null : topic.id)}
                    className="w-full flex items-center justify-between p-4 sm:p-5 text-left transition-colors hover:bg-zinc-50/70 dark:hover:bg-zinc-800/40"
                  >
                    <div className="flex items-center gap-3.5 min-w-0">
                      <div className="h-11 w-11 rounded-xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800/60 flex items-center justify-center shrink-0">
                        <GuideIcon name={topic.icon} className="h-5 w-5" />
                      </div>
                      <div className="min-w-0">
                        <h3 className="text-base sm:text-lg font-bold text-zinc-900 dark:text-white truncate">
                          {t(topic.titleKey)}
                        </h3>
                        <p className="text-xs text-zinc-500 dark:text-zinc-400 truncate">
                          {t(topic.subtitleKey)}
                        </p>
                      </div>
                    </div>

                    <div className="ml-3 shrink-0 text-zinc-400">
                      <svg
                        className={`h-5 w-5 transform transition-transform duration-300 ${
                          isExpanded ? "rotate-180 text-emerald-600" : ""
                        }`}
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                        strokeWidth="2.5"
                      >
                        <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
                      </svg>
                    </div>
                  </button>

                  {/* Expanded Content */}
                  {isExpanded && (
                    <div className="border-t border-zinc-150 dark:border-zinc-800/80 p-5 sm:p-6 bg-zinc-50/40 dark:bg-zinc-900/40 space-y-5 animate-in fade-in duration-200">
                      {/* Pictorial Reference Toggle Header */}
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2 text-emerald-700 dark:text-emerald-400 font-bold text-xs uppercase tracking-wider">
                          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                            <path strokeLinecap="round" strokeLinejoin="round" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
                          </svg>
                          <span>{t("pictorial_reference")}</span>
                        </div>

                        <button
                          type="button"
                          onClick={(e) => toggleVisualGuide(topic.id, e)}
                          className="text-xs font-semibold text-zinc-500 hover:text-zinc-800 dark:hover:text-zinc-200"
                        >
                          {isVisualOpen ? t("hide_visual_guide") : t("show_visual_guide")}
                        </button>
                      </div>

                      {/* Visual Reference Indications Box */}
                      {isVisualOpen && (
                        <div className="rounded-xl border border-emerald-200 dark:border-emerald-800/40 bg-emerald-50/60 dark:bg-emerald-950/20 p-4 space-y-2">
                          <p className="text-[11px] font-bold text-emerald-800 dark:text-emerald-300 uppercase tracking-wide">
                            Key Indications & Workflow Hotspots
                          </p>
                          <div className="flex flex-wrap gap-2">
                            {topic.indicationKeys.map((indKey) => (
                              <span
                                key={indKey}
                                className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-white dark:bg-zinc-800 border border-emerald-200 dark:border-emerald-700/60 text-xs font-medium text-emerald-800 dark:text-emerald-200 shadow-2xs"
                              >
                                <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                                {t(indKey) || indKey}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* Numbered Steps */}
                      <div className="space-y-4 pt-1">
                        {topic.steps.map((step) => (
                          <div key={step.stepNumber} className="flex items-start gap-3.5">
                            <div className="h-7 w-7 rounded-full bg-emerald-600 text-white font-bold text-xs flex items-center justify-center shrink-0 shadow-xs">
                              {step.stepNumber}
                            </div>
                            <div className="space-y-1 flex-1">
                              <h4 className="text-sm font-bold text-zinc-900 dark:text-white">
                                {t(step.titleKey)}
                              </h4>
                              <p className="text-xs text-zinc-600 dark:text-zinc-300 leading-relaxed">
                                {t(step.descriptionKey)}
                              </p>
                              {step.tipKey && (
                                <div className="mt-2 flex items-start gap-2 p-2.5 rounded-xl bg-amber-50 dark:bg-amber-950/30 border border-amber-200/80 dark:border-amber-800/40 text-amber-800 dark:text-amber-300 text-[11px] leading-relaxed">
                                  <svg className="h-4 w-4 shrink-0 text-amber-600 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                                  </svg>
                                  <span>{t(step.tipKey)}</span>
                                </div>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>

                      {/* Direct Launch Action Button */}
                      <div className="pt-2">
                        <Link
                          href={topic.actionRoute}
                          className="inline-flex items-center justify-center gap-2 w-full py-3 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs uppercase tracking-wider shadow-sm transition active:scale-[0.99]"
                        >
                          <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 24 24">
                            <polygon points="5 3 19 12 5 21 5 3" />
                          </svg>
                          <span>{t(topic.actionLabelKey)}</span>
                        </Link>
                      </div>
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
      </main>
    </div>
  );
}

export default function HowToGuidePage() {
  return (
    <Suspense
      fallback={
        <div className="flex h-screen items-center justify-center bg-zinc-50 dark:bg-zinc-950">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent" />
        </div>
      }
    >
      <HowToGuideContent />
    </Suspense>
  );
}
