// Script to generate PWA icons from app_logo.png
// Run with: node scripts/generate-pwa-icons.js
const path = require("path");
const fs = require("fs");

async function main() {
  // Dynamically import sharp (ESM compatible)
  const sharp = require("sharp");

  const INPUT = path.join(__dirname, "../public/app_logo.png");
  const OUTPUT_DIR = path.join(__dirname, "../public/icons");

  if (!fs.existsSync(OUTPUT_DIR)) {
    fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  }

  const sizes = [72, 96, 128, 144, 152, 180, 192, 384, 512];

  console.log("Generating PWA icons...");

  for (const size of sizes) {
    const outputFile = path.join(
      OUTPUT_DIR,
      size === 180 ? "apple-touch-icon.png" : `icon-${size}x${size}.png`
    );

    await sharp(INPUT)
      .resize(size, size, {
        fit: "contain",
        background: { r: 0, g: 0, b: 0, alpha: 0 }, // transparent background
      })
      .png({ quality: 90 })
      .toFile(outputFile);

    console.log(`  ✓ Generated ${size}x${size} → ${path.basename(outputFile)}`);
  }

  console.log(`\nAll icons written to: ${OUTPUT_DIR}`);
}

main().catch((err) => {
  console.error("Error generating icons:", err);
  process.exit(1);
});
