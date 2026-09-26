import { Pig } from "./types";

/**
 * Triggers the browser's native print dialogue.
 * Works hand-in-hand with @media print CSS styles in globals.css
 * to generate 100% parity vector PDF outputs matching the Android app.
 */
export function exportToPdf() {
  if (typeof window !== "undefined") {
    window.print();
  }
}

/**
 * Resolves raw UUIDs in descriptions like "Pig 9af83... mated with Pig 1b2c..."
 * into human-readable tags like "Pig TAG-001 mated with Pig TAG-002",
 * exactly matching Android PdfGenerator.resolvePigIds.
 */
export function resolvePigIds(text: string, allPigs: Pig[] = []): string {
  if (!text || !text.includes("Pig ")) return text || "";
  const parts = text.split("Pig ");
  let result = parts[0];
  for (let i = 1; i < parts.length; i++) {
    const remaining = parts[i];
    const match = remaining.match(/^([a-zA-Z0-9_-]+)([\s\S]*)$/);
    if (match) {
      const idCandidate = match[1];
      const rest = match[2];
      const matchedPig = allPigs.find((p) => p.id === idCandidate || p.tagNumber === idCandidate);
      const tag = matchedPig ? matchedPig.tagNumber : idCandidate;
      result += `Pig ${tag}${rest}`;
    } else {
      result += `Pig ${remaining}`;
    }
  }
  return result;
}

