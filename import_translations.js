const fs = require('fs');
const path = require('path');

const filepath = 'c:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/utils/TranslationUtils.kt';

const languages = [
    { code: 'en', name: 'English', suffix: 'En' },
    { code: 'fr', name: 'French', suffix: 'Fr' },
    { code: 'zh', name: 'Chinese', suffix: 'Zh' },
    { code: 'es', name: 'Spanish', suffix: 'Es' },
    { code: 'tl', name: 'Filipino', suffix: 'Tl' },
    { code: 'vi', name: 'Vietnamese', suffix: 'Vi' },
    { code: 'th', name: 'Thai', suffix: 'Th' },
    { code: 'pt', name: 'Portuguese', suffix: 'Pt' },
    { code: 'hi', name: 'Hindi', suffix: 'Hi' },
    { code: 'sw', name: 'Swahili', suffix: 'Sw' },
    { code: 'id', name: 'Indonesian', suffix: 'Id' },
    { code: 'ht', name: 'Creole', suffix: 'Ht' },
    { code: 'my', name: 'Burmese', suffix: 'My' }
];

function parseCsv(text) {
    const lines = [];
    let row = [""];
    let inQuotes = false;
    for (let i = 0; i < text.length; i++) {
        const c = text[i];
        const next = text[i+1];
        if (inQuotes) {
            if (c === '"') {
                if (next === '"') {
                    row[row.length - 1] += '"';
                    i++;
                } else {
                    inQuotes = false;
                }
            } else {
                row[row.length - 1] += c;
            }
        } else {
            if (c === '"') {
                inQuotes = true;
            } else if (c === ',') {
                row.push("");
            } else if (c === '\r' || c === '\n') {
                if (c === '\r' && next === '\n') {
                    i++;
                }
                lines.push(row);
                row = [""];
            } else {
                row[row.length - 1] += c;
            }
        }
    }
    if (row.length > 1 || row[0] !== "") {
        lines.push(row);
    }
    return lines;
}

function formatMap(pairs) {
    if (pairs.length === 0) {
        return 'mapOf()';
    }
    return 'mapOf(\n' + pairs.map(p => `        "${p.key}" to "${p.value}"`).join(',\n') + '\n    )';
}

function findMapOfEnd(content, mapOfStart) {
    let parenCount = 0;
    let inString = false;
    let escape = false;
    
    for (let i = mapOfStart; i < content.length; i++) {
        const c = content[i];
        if (inString) {
            if (escape) {
                escape = false;
            } else if (c === '\\') {
                escape = true;
            } else if (c === '"') {
                inString = false;
            }
        } else {
            if (c === '"') {
                inString = true;
            } else if (c === '(') {
                parenCount++;
            } else if (c === ')') {
                parenCount--;
                if (parenCount === 0) {
                    return i;
                }
            }
        }
    }
    return -1;
}

function replaceMapBlock(content, funcName, newBlockStr) {
    const startFuncIndex = content.indexOf(`private fun ${funcName}`);
    if (startFuncIndex === -1) {
        console.error(`Error: Could not find function ${funcName} in TranslationUtils.kt`);
        return null;
    }

    const mapOfStart = content.indexOf("mapOf(", startFuncIndex);
    if (mapOfStart === -1) {
        console.error(`Error: Could not find mapOf in ${funcName}`);
        return null;
    }

    const mapOfEnd = findMapOfEnd(content, mapOfStart);
    if (mapOfEnd === -1) {
        console.error(`Error: Could not find closing parenthesis for ${funcName}`);
        return null;
    }

    return content.substring(0, mapOfStart) + newBlockStr + content.substring(mapOfEnd + 1);
}

function main() {
    const args = process.argv.slice(2);
    
    // Choose default file if none specified
    let csvPath = args[0];
    if (!csvPath) {
        if (fs.existsSync('c:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app_strings_all.csv')) {
            csvPath = 'c:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app_strings_all.csv';
        } else {
            csvPath = 'c:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app_strings.csv';
        }
    }

    if (!fs.existsSync(csvPath)) {
        console.error(`Error: CSV file not found at ${csvPath}`);
        process.exit(1);
    }

    if (!fs.existsSync(filepath)) {
        console.error(`Error: TranslationUtils.kt not found at ${filepath}`);
        process.exit(1);
    }

    console.log(`Reading CSV from ${csvPath}...`);
    const csvText = fs.readFileSync(csvPath, 'utf-8');
    const rows = parseCsv(csvText);

    if (rows.length < 2) {
        console.error("Error: CSV is empty or only contains header.");
        process.exit(1);
    }

    // Header mapping
    const headers = rows[0];
    const colIndexToLang = {};
    for (let i = 2; i < headers.length; i++) {
        const headerName = headers[i].trim().toLowerCase();
        const lang = languages.find(l => l.name.toLowerCase() === headerName || l.code.toLowerCase() === headerName);
        if (lang) {
            colIndexToLang[i] = lang;
        } else {
            console.warn(`Warning: Header "${headers[i]}" did not match any supported language.`);
        }
    }

    // Prepare container for parsed translations
    const csvTranslations = {}; // lang_code -> { main: [], extras: [] }
    for (const lang of languages) {
        csvTranslations[lang.code] = { main: [], extras: [] };
    }

    // Process all CSV rows
    for (let i = 1; i < rows.length; i++) {
        const row = rows[i];
        if (row.length < 3) continue;
        const key = row[0].trim();
        const type = row[1].trim().toLowerCase(); // 'main' or 'extras'
        if (!key) continue;

        for (let colIdx = 2; colIdx < row.length; colIdx++) {
            const lang = colIndexToLang[colIdx];
            if (!lang) continue;

            const val = row[colIdx];
            if (val !== undefined && val !== null) {
                const val_trimmed = val.trim();
                if (val_trimmed === '') continue; // Skip empty translations, fallback will happen automatically

                // Escape double quotes and newlines for Kotlin
                const escapedVal = val_trimmed
                    .replace(/\r?\n/g, '\\n')
                    .replace(/"/g, '\\"');

                if (type === 'extras') {
                    csvTranslations[lang.code].extras.push({ key, value: escapedVal });
                } else {
                    csvTranslations[lang.code].main.push({ key, value: escapedVal });
                }
            }
        }
    }

    let content = fs.readFileSync(filepath, 'utf-8');

    // Update each language
    for (const lang of languages) {
        const mainFuncName = `get${lang.suffix}Translations`;
        const extrasFuncName = `getExtras${lang.suffix}`;

        const mainBlockStr = formatMap(csvTranslations[lang.code].main);
        const extrasBlockStr = formatMap(csvTranslations[lang.code].extras);

        // Replace main function map
        let updatedContent = replaceMapBlock(content, mainFuncName, mainBlockStr);
        if (!updatedContent) {
            console.error(`Failed to update ${mainFuncName}. Aborting.`);
            process.exit(1);
        }
        content = updatedContent;

        // Replace extras function map
        updatedContent = replaceMapBlock(content, extrasFuncName, extrasBlockStr);
        if (!updatedContent) {
            console.error(`Failed to update ${extrasFuncName}. Aborting.`);
            process.exit(1);
        }
        content = updatedContent;

        console.log(`Updated translations for ${lang.name} (${csvTranslations[lang.code].main.length} main, ${csvTranslations[lang.code].extras.length} extras)`);
    }

    fs.writeFileSync(filepath, content, 'utf-8');
    console.log(`\nSuccess: TranslationUtils.kt has been successfully updated with all translations from the CSV!`);
}

main();
