export interface CurrencyInfo {
  code: string;
  symbol: string;
}

const countryToCurrency: Record<string, CurrencyInfo> = {
  "ghana": { code: "GHS", symbol: "GH₵" },
  "nigeria": { code: "NGN", symbol: "₦" },
  "united states": { code: "USD", symbol: "$" },
  "usa": { code: "USD", symbol: "$" },
  "united kingdom": { code: "GBP", symbol: "£" },
  "uk": { code: "GBP", symbol: "£" },
  "canada": { code: "CAD", symbol: "$" },
  "kenya": { code: "KES", symbol: "KSh" },
  "south africa": { code: "ZAR", symbol: "R" },
  "india": { code: "INR", symbol: "₹" },
  "philippines": { code: "PHP", symbol: "₱" },
  "vietnam": { code: "VND", symbol: "₫" },
  "thailand": { code: "THB", symbol: "฿" },
  "china": { code: "CNY", symbol: "¥" },
  "spain": { code: "EUR", symbol: "€" },
  "france": { code: "EUR", symbol: "€" },
  "germany": { code: "EUR", symbol: "€" },
  "italy": { code: "EUR", symbol: "€" },
  "netherlands": { code: "EUR", symbol: "€" },
  "ireland": { code: "EUR", symbol: "€" },
  "portugal": { code: "EUR", symbol: "€" },
  "australia": { code: "AUD", symbol: "$" },
  "new zealand": { code: "NZD", symbol: "$" },
  "brazil": { code: "BRL", symbol: "R$" },
  "mexico": { code: "MXN", symbol: "$" },
  "uganda": { code: "UGX", symbol: "USh" },
  "tanzania": { code: "TZS", symbol: "TSh" },
  "zambia": { code: "ZMW", symbol: "ZK" },
  "cameroon": { code: "XAF", symbol: "FCFA" },
  "ivory coast": { code: "XOF", symbol: "CFA" },
  "senegal": { code: "XOF", symbol: "CFA" },
  "mali": { code: "XOF", symbol: "CFA" },
  "burkina faso": { code: "XOF", symbol: "CFA" },
  "benin": { code: "XOF", symbol: "CFA" },
  "togo": { code: "XOF", symbol: "CFA" },
  "niger": { code: "XOF", symbol: "CFA" },
  "gabon": { code: "XAF", symbol: "FCFA" },
  "congo": { code: "XAF", symbol: "FCFA" },
  "chad": { code: "XAF", symbol: "FCFA" },
  "central african republic": { code: "XAF", symbol: "FCFA" },
  "equatorial guinea": { code: "XAF", symbol: "FCFA" },
  "liberia": { code: "LRD", symbol: "$" },
  "sierra leone": { code: "SLE", symbol: "Le" },
  "gambia": { code: "GMD", symbol: "D" },
  "rwanda": { code: "RWF", symbol: "FRw" },
  "burundi": { code: "BIF", symbol: "FBu" },
  "ethiopia": { code: "ETB", symbol: "Br" },
  "malawi": { code: "MWK", symbol: "MK" },
  "zimbabwe": { code: "ZWL", symbol: "$" },
  "botswana": { code: "BWP", symbol: "P" },
  "namibia": { code: "NAD", symbol: "$" },
};

export function getCurrencyByCountry(country: string): CurrencyInfo {
  const normalized = country.trim().toLowerCase();
  return countryToCurrency[normalized] || { code: "USD", symbol: "$" };
}
