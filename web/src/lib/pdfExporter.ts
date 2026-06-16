/**
 * Triggers the browser's native print dialogue.
 * Works hand-in-hand with @media print CSS styles in globals.css
 * to generate vector PDF outputs.
 */
export function exportToPdf() {
  if (typeof window !== "undefined") {
    window.print();
  }
}
