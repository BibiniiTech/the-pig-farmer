"use client";

import React from "react";
import ReportLayout from "./ReportLayout";
import { FeedInventoryItem, FeedInventoryTransaction } from "@/lib/types";

interface FeedReportProps {
  feedItems: FeedInventoryItem[];
  transactions: FeedInventoryTransaction[];
  title?: string;
}

const FeedReport: React.FC<FeedReportProps> = ({
  feedItems,
  transactions,
  title = "Feed Inventory Report",
}) => {
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
    totalAdditionInKg += addition * multiplier;
    totalUsageInKg += usage * multiplier;
    totalInStockInKg += inStock * multiplier;

    const unitLabel = item.unit === "bags" ? "Bags" : "kg";

    return {
      name: item.name,
      feedType: item.feedType || "Feed",
      unit: unitLabel,
      addition: addition.toFixed(1),
      usage: usage.toFixed(1),
      inStock: inStock.toFixed(1),
    };
  });

  return (
    <ReportLayout title={title}>
      <div className="space-y-4">
        <table className="w-full border-collapse text-[10pt] border border-zinc-400">
          <thead>
            <tr className="bg-[#D3D3D3] text-black font-bold text-left border-b border-zinc-400">
              <th className="p-2 border border-zinc-400 font-bold w-[30%]">Feed Name</th>
              <th className="p-2 border border-zinc-400 font-bold w-[20%]">Feed Type</th>
              <th className="p-2 border border-zinc-400 font-bold text-center w-[12%]">Unit</th>
              <th className="p-2 border border-zinc-400 font-bold text-right w-[12%]">Addition</th>
              <th className="p-2 border border-zinc-400 font-bold text-right w-[12%]">Usage</th>
              <th className="p-2 border border-zinc-400 font-bold text-right w-[14%]">In Stock</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-300">
            {rows.length === 0 ? (
              <tr>
                <td colSpan={6} className="p-4 text-center text-zinc-500 italic border border-zinc-300">
                  No feed items recorded.
                </td>
              </tr>
            ) : (
              rows.map((row, idx) => (
                <tr key={idx} className="text-black">
                  <td className="p-2 border border-zinc-300 font-bold">{row.name}</td>
                  <td className="p-2 border border-zinc-300">{row.feedType}</td>
                  <td className="p-2 border border-zinc-300 text-center">{row.unit}</td>
                  <td className="p-2 border border-zinc-300 text-right font-mono">{row.addition}</td>
                  <td className="p-2 border border-zinc-300 text-right font-mono">{row.usage}</td>
                  <td className="p-2 border border-zinc-300 text-right font-mono font-bold">{row.inStock}</td>
                </tr>
              ))
            )}
            <tr className="font-bold text-black border-t-2 border-zinc-400">
              <td className="p-2 border border-zinc-400 font-bold">TOTAL (kg)</td>
              <td className="p-2 border border-zinc-400"></td>
              <td className="p-2 border border-zinc-400"></td>
              <td className="p-2 border border-zinc-400 text-right font-mono font-bold">
                {totalAdditionInKg.toFixed(1)}
              </td>
              <td className="p-2 border border-zinc-400 text-right font-mono font-bold">
                {totalUsageInKg.toFixed(1)}
              </td>
              <td className="p-2 border border-zinc-400 text-right font-mono font-bold">
                {totalInStockInKg.toFixed(1)}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </ReportLayout>
  );
};

export default FeedReport;
