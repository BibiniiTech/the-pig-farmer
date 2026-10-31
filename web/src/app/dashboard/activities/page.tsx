"use client";

import { useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";

function ActivitiesRedirectContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const activity = searchParams.get("activity");

  useEffect(() => {
    if (activity) {
      router.replace(`/dashboard?section=activities&activity=${encodeURIComponent(activity)}`);
    } else {
      router.replace("/dashboard?section=activities");
    }
  }, [activity, router]);

  return (
    <div className="flex h-screen items-center justify-center bg-[#F8FAF9] dark:bg-[#121212]">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-cyan-600 border-t-transparent" />
    </div>
  );
}

export default function ActivitiesPage() {
  return (
    <Suspense fallback={
      <div className="flex h-screen items-center justify-center bg-[#F8FAF9] dark:bg-[#121212]">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-cyan-600 border-t-transparent" />
      </div>
    }>
      <ActivitiesRedirectContent />
    </Suspense>
  );
}
