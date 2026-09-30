import React from "react";
import {
  HerdDataIcon,
  FeedManagementIcon,
  WeightCheckerIcon,
  SymptomsAnalyzerIcon,
  HerdActivitiesIcon,
  FinancialsIcon,
  HumanResourcesIcon,
  LocalHubIcon,
  TrainingTipsIcon,
} from "@/components/icons/DashboardIcons";

interface FeatureItem {
  number: number;
  title: string;
  icon: React.ComponentType<React.SVGProps<SVGSVGElement>>;
  points: string[];
  primaryColor: string;
  bgLight: string;
  borderColor: string;
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
      icon: HerdDataIcon,
      primaryColor: "#2E7D32",
      bgLight: "#E8F5E9",
      borderColor: "rgba(46, 125, 50, 0.28)",
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
      icon: FeedManagementIcon,
      primaryColor: "#E65100",
      bgLight: "#FFF3E0",
      borderColor: "rgba(230, 81, 0, 0.28)",
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
      icon: WeightCheckerIcon,
      primaryColor: "#455A64",
      bgLight: "#ECEFF1",
      borderColor: "rgba(69, 90, 100, 0.28)",
      points: [
        getText("feature3_p1", "Weigh pigs accurately with just a simple measuring tape—no expensive scale needed"),
        getText("feature3_p2", "Find estimated carcass (dressed meat) weights before slaughter or sale"),
      ],
    },
    {
      number: 4,
      title: getText("feature4_title", "Disease Finder"),
      icon: SymptomsAnalyzerIcon,
      primaryColor: "#3F51B5",
      bgLight: "#E8EAF6",
      borderColor: "rgba(63, 81, 181, 0.28)",
      points: [
        getText("feature4_p1", "Select symptoms to quickly identify sicknesses affecting your pig"),
        getText("feature4_p2", "Get immediate treatment actions and medicines from our swine health database"),
      ],
    },
    {
      number: 5,
      title: getText("feature5_title", "Herd Activities"),
      icon: HerdActivitiesIcon,
      primaryColor: "#00838F",
      bgLight: "#E0F7FA",
      borderColor: "rgba(0, 131, 143, 0.28)",
      points: [
        getText("feature5_p1", "Track heat detection, breeding, farrowing, iron shots, deworming, and weaning"),
        getText("feature5_p2", "Receive automatic alerts so you never miss a farrowing or farm routine"),
      ],
    },
    {
      number: 6,
      title: getText("feature6_title", "Financials"),
      icon: FinancialsIcon,
      primaryColor: "#00796B",
      bgLight: "#E0F2F1",
      borderColor: "rgba(0, 121, 107, 0.28)",
      points: [
        getText("feature6_p1", "See exactly which expenses are taking away your hard-earned profits"),
        getText("feature6_p2", "Get the recommended minimum selling price per kg to make sure you always make profit"),
        getText("feature6_p3", "Turn day-to-day records into clean, printable farm financial reports"),
      ],
    },
    {
      number: 7,
      title: getText("feature7_title", "Human Resources"),
      icon: HumanResourcesIcon,
      primaryColor: "#7B1FA2",
      bgLight: "#F3E5F5",
      borderColor: "rgba(123, 31, 162, 0.28)",
      points: [
        getText("feature7_p1", "Manage worker pay in one place—automatically added to your farm expenses"),
        getText("feature7_p2", "Share pig data access with workers to log daily chores without seeing farm money"),
      ],
    },
    {
      number: 8,
      title: getText("feature8_title", "Market Hub"),
      icon: LocalHubIcon,
      primaryColor: "#C2185B",
      bgLight: "#FFEBEE",
      borderColor: "rgba(194, 24, 91, 0.28)",
      points: [
        getText("feature8_p1", "Find trusted local buyers, feed sellers, and vet services near you"),
        getText("feature8_p2", "Suggest and share good local vendors and services with fellow farmers"),
      ],
    },
    {
      number: 9,
      title: getText("feature9_title", "Pig Farming Tips"),
      icon: TrainingTipsIcon,
      primaryColor: "#5D4037",
      bgLight: "#EFEBE9",
      borderColor: "rgba(93, 64, 55, 0.28)",
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
        <span className="inline-flex items-center gap-1.5 px-3.5 py-1 rounded-full text-xs font-extrabold uppercase tracking-wider bg-emerald-100/90 text-emerald-800 border border-emerald-200 shadow-xs mb-3">
          {getText("allInOneApp", "All-in-One Pig Farming App")}
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
              className="flex flex-col rounded-2xl bg-white/95 backdrop-blur-sm p-6 shadow-xs hover:shadow-md transition-all duration-200"
              style={{
                border: `1px solid ${feature.borderColor}`,
                borderTop: `4px solid ${feature.primaryColor}`,
              }}
            >
              {/* Card Header with App-Themed Icon & Numbered Title */}
              <div className="flex items-center gap-3.5 mb-4">
                <div
                  className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl shadow-xs"
                  style={{
                    backgroundColor: feature.bgLight,
                    color: feature.primaryColor,
                    border: `1px solid ${feature.borderColor}`,
                  }}
                >
                  <Icon className="h-6 w-6" aria-hidden="true" />
                </div>
                <div className="min-w-0">
                  <h3 className="text-lg font-extrabold text-zinc-900 leading-tight">
                    {feature.number}. {feature.title}
                  </h3>
                </div>
              </div>

              {/* Bullet Points with module-colored checkmark */}
              <ul className="space-y-2.5 text-xs sm:text-sm text-zinc-600 flex-1">
                {feature.points.map((point, idx) => (
                  <li key={idx} className="flex items-start gap-2.5">
                    <svg
                      className="h-4 w-4 mt-0.5 shrink-0"
                      style={{ color: feature.primaryColor }}
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
