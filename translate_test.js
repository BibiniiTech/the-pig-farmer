const fs = require('fs');

const content = fs.readFileSync('app/src/main/java/com/example/smartswine/utils/TranslationUtils.kt', 'utf-8');

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

console.log('Main pairs:', parseMap(enTrans).length);
console.log('Extras pairs:', parseMap(enExtras).length);
