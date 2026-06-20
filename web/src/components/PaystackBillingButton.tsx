"use client";

import React from "react";
import { usePaystackPayment } from "react-paystack";

interface PaystackBillingButtonProps {
  config: any;
  onSuccess: (reference: any) => void;
  onClose: () => void;
}

export default function PaystackBillingButton({ config, onSuccess, onClose }: PaystackBillingButtonProps) {
  const initializePayment = usePaystackPayment(config);

  return (
    <button
      onClick={() => {
        // @ts-ignore
        initializePayment(onSuccess, onClose);
      }}
      className="w-full rounded-xl bg-gradient-to-r from-emerald-600 to-green-500 hover:from-emerald-700 hover:to-green-600 py-3 text-xs font-bold text-white text-center shadow-lg shadow-emerald-600/10 block transition duration-300 transform active:scale-95"
    >
      Upgrade to Premium
    </button>
  );
}
