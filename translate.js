const fs = require('fs');

const FILE_PATH = 'app/src/main/java/com/example/smartswine/utils/TranslationUtils.kt';
const content = fs.readFileSync(FILE_PATH, 'utf-8');

const enTrans = content.substring(content.indexOf('private fun getEnTranslations'), content.indexOf('private fun getFrTranslations'));
const enExtras = content.substring(content.indexOf('private fun getExtrasEn'), content.indexOf('private fun getExtrasFr'));

function parseMap(block) {
    const pairs = [];
    const regex = /"((?:[^"\\]|\\.)*)"\s*to\s*"((?:[^"\\]|\\.)*)"/g;
    let match;
    while ((match = regex.exec(block)) !== null) {
        pairs.push({ key: match[1], value: match[2] });
    }
    return pairs;
}

const enTranslations = parseMap(enTrans);
const enExtrasMap = parseMap(enExtras);

async function translateText(text, tl) {
    if (!text || !text.trim()) return text;
    // URL encode safely
    const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=${tl}&dt=t&q=${encodeURIComponent(text)}`;
    for (let i = 0; i < 3; i++) {
        try {
            const res = await fetch(url);
            const data = await res.json();
            if (data && data[0]) {
                let translated = data[0].map(x => x[0]).join('');
                // Repair common format breakage by GT
                translated = translated.replace(/%\s*s/g, '%s').replace(/%\s*d/g, '%d').replace(/\\\s*n/g, '\\n');
                return translated;
            }
        } catch (e) {}
        await new Promise(r => setTimeout(r, 1000));
    }
    return text;
}

async function processTranslations(pairs, tl) {
    const results = [];
    let i = 0;
    for (const pair of pairs) {
        let val = pair.value.replace(/\\"/g, '"');
        let translated = await translateText(val, tl);
        translated = translated.replace(/"/g, '\\"');
        results.push({ key: pair.key, value: translated });
        i++;
        if (i % 100 === 0) console.log(`Translated ${i}/${pairs.length} for ${tl}`);
        await new Promise(r => setTimeout(r, 100)); // 100ms delay to prevent IP ban
    }
    return results;
}

function formatMap(pairs) {
    return 'mapOf(\n' + pairs.map(p => `        "${p.key}" to "${p.value}"`).join(',\n') + '\n    )';
}

async function main() {
    const languages = [
        { code: 'sw', suffix: 'Sw' },
        { code: 'id', suffix: 'Id' },
        { code: 'ht', suffix: 'Ht' },
        { code: 'my', suffix: 'My' }
    ];

    let newContent = content;

    for (const lang of languages) {
        console.log(`Starting ${lang.code}...`);
        const trans = await processTranslations(enTranslations, lang.code);
        const transMapStr = formatMap(trans);
        newContent = newContent.replace(`private fun get${lang.suffix}Translations() = emptyMap<String, String>()`, `private fun get${lang.suffix}Translations(): Map<String, String> = ${transMapStr}`);
        
        const extras = await processTranslations(enExtrasMap, lang.code);
        const extrasMapStr = formatMap(extras);
        newContent = newContent.replace(`private fun getExtras${lang.suffix}() = emptyMap<String, String>()`, `private fun getExtras${lang.suffix}(): Map<String, String> = ${extrasMapStr}`);
    }

    fs.writeFileSync(FILE_PATH, newContent, 'utf-8');
    console.log("Translation injection completed successfully.");
}

main().catch(console.error);
