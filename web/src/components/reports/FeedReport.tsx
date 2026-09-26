"use client";

import React from "react";
import ReportLayout from "./ReportLayout";
import { useTranslations } from "next-intl";
import { FeedInventoryItem, FeedInventoryTransaction } from "@/lib/types";

interface FeedReportProps {
  feedItems: FeedInventoryItem[];
  transactions: FeedInventoryTransaction[];
  title?: string;
}

const FeedReport: React.FC<FeedReportProps> = ({
  feedItems,
  transactions,
  title,
}) => {
  const t = useTranslations("Reports");
  const defaultTitle = t("feedReportTitle", { fallback: "Feed Inventory Report" });

  let totalAdditionInKg = 0;
  let totalUsageInKg = 0;
  let totalInStockInKg = 0;

  const rows = feedItems.map((item) => {
    const itemTransactions = transactions.filter((tx) => tx.itemId === item.id);

    const getConvertedQty = (tx: FeedInventoryTransaction) => {
      if (tx.unit === item.unit) return tx.quantity;
      if (tx.unit === "bags" && item.unit === "kg") {
        return tx.quantity * (item.unitWeight || 50);
      }
      if (tx.unit === "kg" && item.unit === "bags" && (item.unitWeight || 50) > 0) {
        return tx.quantity / (item.unitWeight || 50);
      }
      return tx.quantity;
    };

    const addition = itemTransactions
      .filter((tx) => tx.type === "Restock" || tx.type === "Addition")
      .reduce((sum, tx) => sum + getConvertedQty(tx), 0);

    const usage = itemTransactions
      .filter((tx) => tx.type === "Usage" || tx.type === "Deduction")
      .reduce((sum, tx) => sum + getConvertedQty(tx), 0);

    const inStock = item.quantity;

    const multiplier = item.unit === "bags" ? (item.unitWeight || 50) : 1;
    const additionInKg = addition * multiplier;
    const usageInKg = usage * multiplier;
    const inStockInKg = inStock * multiplier;

    totalAdditionInKg += additionInKg;
    totalUsageInKg += usageInKg;
    totalInStockInKg += inStockInKg;

    return {
      name: item.name,
      feedType: item.feedType || "Feed",
      unit: item.unit || "kg",
      addition: addition.toFixed(1),
      usage: usage.toFixed(1),
      inStock: inStock.toFixed(1),
    };
  });

  return (
    <ReportLayout title={title || defaultTitle}>
      <div className="space-y-6">
        <div>
          <h3 className="text-[14pt] font-bold text-zinc-800 border-b-2 border-zinc-200 pb-2 mb-4">
            Feed Stock & Transaction Ledger
          </h3>
          <table className="w-full border-collapse text-[10pt]">
            <thead>
              <tr className="bg-zinc-100 text-left border-y-2 border-zinc-300">
                <th className="p-3 font-bold border-r border-zinc-200">Feed Name</th>
                <th className="p-3 font-bold border-r border-zinc-200">Feed Type</th>
                <th className="p-3 font-bold border-r border-zinc-200 text-center">Unit</th>
                <th className="p-3 font-bold border-r border-zinc-200 text-right">Addition</th>
                <th className="p-3 font-bold border-r border-zinc-200 text-right">Usage</th>
                <th className="p-3 font-bold text-right">In Stock</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-200">
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-4 text-center text-zinc-400 italic">
                    No feed items recorded.
                  </td>
                </tr>
              ) : (
                rows.map((row, idx) => (
                  <tr key={idx} className="hover:bg-zinc-50/50">
                    <td className="p-3 border-r border-zinc-100 font-bold text-zinc-900">{row.name}</td>
                    <td className="p-3 border-r border-zinc-100 text-zinc-600">{row.feedType}</td>
                    <td className="p-3 border-r border-zinc-100 text-center uppercase text-zinc-500 font-medium">
                      {row.unit}
                    </td>
                    <td className="p-3 border-r border-zinc-100 text-right font-mono text-emerald-700">
                      {row.addition}
                    </td>
                    <td className="p-3 border-r border-zinc-100 text-right font-mono text-amber-700">
                      {row.usage}
                    </td>
                    <td className="p-3 text-right font-mono font-bold text-zinc-900">
                      {row.inStock}
                    </td>
                  </tr>
                ))
              )}
              <tr className="bg-zinc-100 font-black text-zinc-900 border-t-2 border-zinc-300">
                <td className="p-3 font-bold uppercase tracking-wider">Total (kg)</td>
                <td className="p-3 border-r border-zinc-200" colSpan={2}></td>
                <td className="p-3 border-r border-zinc-200 text-right font-mono text-emerald-800 text-[11pt]">
                  {totalAdditionInKg.toFixed(1)} kg
                </td>
                <td className="p-3 border-r border-zinc-200 text-right font-mono text-amber-800 text-[11pt]">
                  {totalUsageInKg.toFixed(1)} kg
                </td>
                <td className="p-3 text-right font-mono text-zinc-900 text-[11pt]">
                  {totalInStockInKg.toFixed(1)} kg
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </ReportLayout>
  );
};

export default FeedReport;
