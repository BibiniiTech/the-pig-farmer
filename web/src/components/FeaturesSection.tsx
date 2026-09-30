import React from "react";
import {
  ClipboardDocumentListIcon,
  SparklesIcon,
  ScaleIcon,
  HeartIcon,
  CalendarDaysIcon,
  BanknotesIcon,
  UserGroupIcon,
  BuildingStorefrontIcon,
  LightBulbIcon,
} from "@heroicons/react/24/outline";

interface FeatureItem {
  number: number;
  title: string;
  icon: React.ComponentType<{ className?: string }>;
  points: string[];
}

interface FeaturesSectionProps {
  t?: (key: string) => string;
}

export default function FeaturesSection({ t }: FeaturesSectionProps) {
  // Helper to get translated string or fallback
  const getText = (key: string, fallback: string): string => {
    if (t) {
      try {
        const val = t(key);
        if (val && val !== key) return val;
      } catch {
        // fallback
      }
    }
    return fallback;
  };

  const features: FeatureItem[] = [
    {
      number: 1,
      title: getText("feature1_title", "Herd Data"),
      icon: ClipboardDocumentListIcon,
      points: [
        getText("feature1_p1", "Keep and manage all your pig records safely in one place"),
        getText("feature1_p2", "Trace each pig's family tree, health history, and medications"),
        getText("feature1_p3", "Know how fast your pigs are growing with instant performance ratings"),
        getText("feature1_p4", "Update pig weights to see automatic daily weight gain calculations"),
      ],
    },
    {
      number: 2,
      title: getText("feature2_title", "Feed"),
      icon: SparklesIcon,
      points: [
        getText("feature2_p1", "Mix your own balanced feed from local grains and crops for all pig stages"),
        getText("feature2_p2", "Check if your feed gives your pigs the right nutrition to grow fast and healthy"),
        getText("feature2_p3", "Estimate exactly how much feed your pigs will eat ahead of time"),
        getText("feature2_p4", "Track your feed stock so you never run out of feed unexpectedly"),
      ],
    },
    {
      number: 3,
      title: getText("feature3_title", "Weigh Pigs"),
      icon: ScaleIcon,
      points: [
        getText("feature3_p1", "Weigh pigs accurately with just a simple measuring tape—no expensive scale needed"),
        getText("feature3_p2", "Find estimated carcass (dressed meat) weights before slaughter or sale"),
      ],
    },
    {
      number: 4,
      title: getText("feature4_title", "Disease Finder"),
      icon: HeartIcon,
      points: [
        getText("feature4_p1", "Select symptoms to quickly identify sicknesses affecting your pig"),
        getText("feature4_p2", "Get immediate treatment actions and medicines from our swine health database"),
      ],
    },
    {
      number: 5,
      title: getText("feature5_title", "Herd Activities"),
      icon: CalendarDaysIcon,
      points: [
        getText("feature5_p1", "Track heat detection, breeding, farrowing, iron shots, deworming, and weaning"),
        getText("feature5_p2", "Receive automatic alerts so you never miss a farrowing or farm routine"),
      ],
    },
    {
      number: 6,
      title: getText("feature6_title", "Financials"),
      icon: BanknotesIcon,
      points: [
        getText("feature6_p1", "See exactly which expenses are taking away your hard-earned profits"),
        getText("feature6_p2", "Get the recommended minimum selling price per kg to make sure you always make profit"),
        getText("feature6_p3", "Turn day-to-day records into clean, printable farm financial reports"),
      ],
    },
    {
      number: 7,
      title: getText("feature7_title", "Human Resources"),
      icon: UserGroupIcon,
      points: [
        getText("feature7_p1", "Manage worker pay in one place—automatically added to your farm expenses"),
        getText("feature7_p2", "Share pig data access with workers to log daily chores without seeing farm money"),
      ],
    },
    {
      number: 8,
      title: getText("feature8_title", "Market Hub"),
      icon: BuildingStorefrontIcon,
      points: [
        getText("feature8_p1", "Find trusted local buyers, feed sellers, and vet services near you"),
        getText("feature8_p2", "Suggest and share good local vendors and services with fellow farmers"),
      ],
    },
    {
      number: 9,
      title: getText("feature9_title", "Pig Farming Tips"),
      icon: LightBulbIcon,
      points: [
        getText("feature9_p1", "Learn how to grow your farm from our practical tips database covering every aspect of pig farming"),
        getText("feature9_p2", "Get expert guidance on housing, feeding, breeding, and disease prevention in simple words"),
      ],
    },
  ];

  return (
    <section className="w-full max-w-5xl px-4 py-8 select-none">
      {/* Section Header */}
      <div className="text-center mb-10">
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-emerald-100/90 text-emerald-800 border border-emerald-200 shadow-xs mb-3">
          All-in-One Toolkit
        </span>
        <h2 className="text-3xl sm:text-4xl font-extrabold text-emerald-950 tracking-tight">
          {getText("featuresHeading", "Features")}
        </h2>
        <p className="mt-2 text-sm sm:text-base text-zinc-600 max-w-xl mx-auto">
          {getText(
            "featuresSubtitle",
            "Simple, practical tools designed to help every pig farmer save costs, keep pigs healthy, and maximize profits."
          )}
        </p>
      </div>

      {/* Features Grid: 1 col on mobile, 2 on tablet, 3 on desktop */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 text-left">
        {features.map((feature) => {
          const Icon = feature.icon;
          return (
            <div
              key={feature.number}
              className="flex flex-col rounded-2xl border border-zinc-200/90 bg-white/95 backdrop-blur-sm p-6 shadow-sm hover:shadow-md hover:border-emerald-300 transition-all duration-200"
            >
              {/* Card Header with Icon & Numbered Title */}
              <div className="flex items-center gap-3.5 mb-4">
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-emerald-50 text-emerald-700 border border-emerald-100/80 shadow-xs">
                  <Icon className="h-6 w-6" aria-hidden="true" />
                </div>
                <div>
                  <span className="text-[11px] font-bold text-emerald-600 uppercase tracking-wider">
                    Feature {feature.number}
                  </span>
                  <h3 className="text-lg font-bold text-zinc-900 leading-tight">
                    {feature.number}. {feature.title}
                  </h3>
                </div>
              </div>

              {/* Bullet Points */}
              <ul className="space-y-2.5 text-xs sm:text-sm text-zinc-600 flex-1">
                {feature.points.map((point, idx) => (
                  <li key={idx} className="flex items-start gap-2.5">
                    <svg
                      className="h-4 w-4 mt-0.5 shrink-0 text-emerald-600"
                      viewBox="0 0 20 20"
                      fill="currentColor"
                      aria-hidden="true"
                    >
                      <path
                        fillRule="evenodd"
                        d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z"
                        clipRule="evenodd"
                      />
                    </svg>
                    <span className="leading-snug">{point}</span>
                  </li>
                ))}
              </ul>
            </div>
          );
        })}
      </div>
    </section>
  );
}
