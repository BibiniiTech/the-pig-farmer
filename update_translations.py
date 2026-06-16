import re
import os

filepath = r"app\src\main\java\com\example\smartswine\utils\TranslationUtils.kt"

with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

translations = {
    "en": {
        "affected_animals": "Affected Animals",
        "select_animals_to_complete": "Select Animals to Complete",
        "select_outcome": "Select Outcome",
        "heat_detected": "Heat",
        "no_heat": "No Heat",
        "mating_successful": "Mating Successful",
        "mating_failed": "Mating Failed",
        "male_piglets": "Male Piglets",
        "female_piglets": "Female Piglets",
    },
    "fr": {
        "affected_animals": "Animaux concernés",
        "select_animals_to_complete": "SÉLECTIONNER LES ANIMAUX À COMPLÉTER",
        "select_outcome": "SÉLECTIONNER LE RÉSULTAT",
        "heat_detected": "Chaleur",
        "no_heat": "Pas de chaleur",
        "mating_successful": "Accouplement réussi",
        "mating_failed": "Accouplement échoué",
        "male_piglets": "Porcelets mâles",
        "female_piglets": "Porcelets femelles",
    },
    "zh": {
        "affected_animals": "受影响的动物",
        "select_animals_to_complete": "选择要完成的动物",
        "select_outcome": "选择结果",
        "heat_detected": "发情",
        "no_heat": "未发情",
        "mating_successful": "交配成功",
        "mating_failed": "交配失败",
        "male_piglets": "雄性仔猪",
        "female_piglets": "雌性仔猪",
    },
    "es": {
        "affected_animals": "Animales afectados",
        "select_animals_to_complete": "SELECCIONAR ANIMALES PARA COMPLETAR",
        "select_outcome": "SELECCIONAR RESULTADO",
        "heat_detected": "Celo",
        "no_heat": "Sin celo",
        "mating_successful": "Apareamiento exitoso",
        "mating_failed": "Apareamiento fallido",
        "male_piglets": "Lechones machos",
        "female_piglets": "Lechones hembras",
    },
    "tl": {
        "affected_animals": "Mga apektadong hayop",
        "select_animals_to_complete": "PILIIN ANG MGA HAYOP NA KUKUMPLETUHIN",
        "select_outcome": "PILIIN ANG RESULTA",
        "heat_detected": "Naglalandi",
        "no_heat": "Walang landi",
        "mating_successful": "Matagumpay ang pagtatalik",
        "mating_failed": "Bigo ang pagtatalik",
        "male_piglets": "Mga lalaking biik",
        "female_piglets": "Mga babaeng biik",
    },
    "vi": {
        "affected_animals": "Động vật bị ảnh hưởng",
        "select_animals_to_complete": "CHỌN ĐỘNG VẬT ĐỂ HOÀN THÀNH",
        "select_outcome": "CHỌN KẾT QUẢ",
        "heat_detected": "Động dục",
        "no_heat": "Không động dục",
        "mating_successful": "Giao phối thành công",
        "mating_failed": "Giao phối thất bại",
        "male_piglets": "Lợn con đực",
        "female_piglets": "Lợn con cái",
    },
    "th": {
        "affected_animals": "สัตว์ที่ได้รับผลกระทบ",
        "select_animals_to_complete": "เลือกสัตว์ที่จะดำเนินการ",
        "select_outcome": "เลือกผลลัพธ์",
        "heat_detected": "เป็นสัด",
        "no_heat": "ไม่เป็นสัด",
        "mating_successful": "ผสมพันธุ์สำเร็จ",
        "mating_failed": "ผสมพันธุ์ล้มเหลว",
        "male_piglets": "ลูกหมูตัวผู้",
        "female_piglets": "ลูกหมูตัวเมีย",
    },
    "pt": {
        "affected_animals": "Animais afetados",
        "select_animals_to_complete": "SELECIONAR ANIMAIS PARA CONCLUIR",
        "select_outcome": "SELECIONAR RESULTADO",
        "heat_detected": "Cio",
        "no_heat": "Sem cio",
        "mating_successful": "Acasalamento bem-sucedido",
        "mating_failed": "Acasalamento falhou",
        "male_piglets": "Leitões machos",
        "female_piglets": "Leitões fêmeas",
    },
    "hi": {
        "affected_animals": "प्रभावित जानवर",
        "select_animals_to_complete": "पूरा करने के लिए जानवरों का चयन करें",
        "select_outcome": "परिणाम चुनें",
        "heat_detected": "गर्मी (Mad)",
        "no_heat": "कोई गर्मी नहीं",
        "mating_successful": "संभोग सफल",
        "mating_failed": "संभोग विफल",
        "male_piglets": "नर सुअर के बच्चे",
        "female_piglets": "मादा सुअर के बच्चे",
    }
}

lang_functions = {
    "en": "getEnTranslations",
    "fr": "getFrTranslations",
    "zh": "getZhTranslations",
    "es": "getEsTranslations",
    "tl": "getTlTranslations",
    "vi": "getViTranslations",
    "th": "getThTranslations",
    "pt": "getPtTranslations",
    "hi": "getHiTranslations"
}

# Process each language
for lang, func_name in lang_functions.items():
    # Find the function block
    block_pattern = re.compile(r"(private fun " + func_name + r"\(\): Map<String, String> = mapOf\([^)]*\))", re.DOTALL)
    match = block_pattern.search(content)
    if match:
        block = match.group(1)
        new_block = block
        
        for key, value in translations[lang].items():
            # Replace existing key if it exists
            key_pattern = re.compile(rf'"{key}" to ".*?"')
            if key_pattern.search(new_block):
                new_block = key_pattern.sub(f'"{key}" to "{value}"', new_block)
            else:
                # Add to the end of the mapOf (before the closing parenthesis)
                # Find the last closing parenthesis
                last_paren_idx = new_block.rfind(")")
                # Add the new key-value pair
                new_block = new_block[:last_paren_idx] + f',\n        "{key}" to "{value}"\n    ' + new_block[last_paren_idx:]
        
        content = content.replace(block, new_block)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Translations updated successfully.")
