import type { Metadata } from "next";
import MigrationNoticeContent from "@/components/MigrationNoticeContent";

export const metadata: Metadata = {
  title: "SmartSwine - We Have Moved to www.smartswine.app",
  description: "SmartSwine has officially moved to www.smartswine.app. Please update your bookmarks to access the platform.",
  other: {
    refresh: "5; url=https://www.smartswine.app",
  },
};

export default function MovedPage() {
  return <MigrationNoticeContent />;
}
