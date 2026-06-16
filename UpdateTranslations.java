import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateTranslations {
    public static void main(String[] args) throws Exception {
        String filePath = "app/src/main/java/com/example/smartswine/utils/TranslationUtils.kt";
        String content = new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");

        Map<String, Map<String, String>> translations = new HashMap<>();

        Map<String, String> en = new HashMap<>();
        en.put("affected_animals", "Affected Animals");
        en.put("select_animals_to_complete", "Select Animals to Complete");
        en.put("select_outcome", "Select Outcome");
        en.put("heat_detected", "Heat");
        en.put("no_heat", "No Heat");
        en.put("mating_successful", "Mating Successful");
        en.put("mating_failed", "Mating Failed");
        en.put("male_piglets", "Male Piglets");
        en.put("female_piglets", "Female Piglets");
        translations.put("En", en);

        Map<String, String> fr = new HashMap<>();
        fr.put("affected_animals", "Animaux concernés");
        fr.put("select_animals_to_complete", "SÉLECTIONNER LES ANIMAUX À COMPLÉTER");
        fr.put("select_outcome", "SÉLECTIONNER LE RÉSULTAT");
        fr.put("heat_detected", "Chaleur");
        fr.put("no_heat", "Pas de chaleur");
        fr.put("mating_successful", "Accouplement réussi");
        fr.put("mating_failed", "Accouplement échoué");
        fr.put("male_piglets", "Porcelets mâles");
        fr.put("female_piglets", "Porcelets femelles");
        translations.put("Fr", fr);

        Map<String, String> zh = new HashMap<>();
        zh.put("affected_animals", "受影响的动物");
        zh.put("select_animals_to_complete", "选择要完成的动物");
        zh.put("select_outcome", "选择结果");
        zh.put("heat_detected", "发情");
        zh.put("no_heat", "未发情");
        zh.put("mating_successful", "交配成功");
        zh.put("mating_failed", "交配失败");
        zh.put("male_piglets", "雄性仔猪");
        zh.put("female_piglets", "雌性仔猪");
        translations.put("Zh", zh);

        Map<String, String> es = new HashMap<>();
        es.put("affected_animals", "Animales afectados");
        es.put("select_animals_to_complete", "SELECCIONAR ANIMALES PARA COMPLETAR");
        es.put("select_outcome", "SELECCIONAR RESULTADO");
        es.put("heat_detected", "Celo");
        es.put("no_heat", "Sin celo");
        es.put("mating_successful", "Apareamiento exitoso");
        es.put("mating_failed", "Apareamiento fallido");
        es.put("male_piglets", "Lechones machos");
        es.put("female_piglets", "Lechones hembras");
        translations.put("Es", es);

        Map<String, String> tl = new HashMap<>();
        tl.put("affected_animals", "Mga apektadong hayop");
        tl.put("select_animals_to_complete", "PILIIN ANG MGA HAYOP NA KUKUMPLETUHIN");
        tl.put("select_outcome", "PILIIN ANG RESULTA");
        tl.put("heat_detected", "Naglalandi");
        tl.put("no_heat", "Walang landi");
        tl.put("mating_successful", "Matagumpay ang pagtatalik");
        tl.put("mating_failed", "Bigo ang pagtatalik");
        tl.put("male_piglets", "Mga lalaking biik");
        tl.put("female_piglets", "Mga babaeng biik");
        translations.put("Tl", tl);

        Map<String, String> vi = new HashMap<>();
        vi.put("affected_animals", "Động vật bị ảnh hưởng");
        vi.put("select_animals_to_complete", "CHỌN ĐỘNG VẬT ĐỂ HOÀN THÀNH");
        vi.put("select_outcome", "CHỌN KẾT QUẢ");
        vi.put("heat_detected", "Động dục");
        vi.put("no_heat", "Không động dục");
        vi.put("mating_successful", "Giao phối thành công");
        vi.put("mating_failed", "Giao phối thất bại");
        vi.put("male_piglets", "Lợn con đực");
        vi.put("female_piglets", "Lợn con cái");
        translations.put("Vi", vi);

        Map<String, String> th = new HashMap<>();
        th.put("affected_animals", "สัตว์ที่ได้รับผลกระทบ");
        th.put("select_animals_to_complete", "เลือกสัตว์ที่จะดำเนินการ");
        th.put("select_outcome", "เลือกผลลัพธ์");
        th.put("heat_detected", "เป็นสัด");
        th.put("no_heat", "ไม่เป็นสัด");
        th.put("mating_successful", "ผสมพันธุ์สำเร็จ");
        th.put("mating_failed", "ผสมพันธุ์ล้มเหลว");
        th.put("male_piglets", "ลูกหมูตัวผู้");
        th.put("female_piglets", "ลูกหมูตัวเมีย");
        translations.put("Th", th);

        Map<String, String> pt = new HashMap<>();
        pt.put("affected_animals", "Animais afetados");
        pt.put("select_animals_to_complete", "SELECIONAR ANIMAIS PARA CONCLUIR");
        pt.put("select_outcome", "SELECIONAR RESULTADO");
        pt.put("heat_detected", "Cio");
        pt.put("no_heat", "Sem cio");
        pt.put("mating_successful", "Acasalamento bem-sucedido");
        pt.put("mating_failed", "Acasalamento falhou");
        pt.put("male_piglets", "Leitões machos");
        pt.put("female_piglets", "Leitões fêmeas");
        translations.put("Pt", pt);

        Map<String, String> hi = new HashMap<>();
        hi.put("affected_animals", "प्रभावित जानवर");
        hi.put("select_animals_to_complete", "पूरा करने के लिए जानवरों का चयन करें");
        hi.put("select_outcome", "परिणाम चुनें");
        hi.put("heat_detected", "गर्मी (Mad)");
        hi.put("no_heat", "कोई गर्मी नहीं");
        hi.put("mating_successful", "संभोग सफल");
        hi.put("mating_failed", "संभोग विफल");
        hi.put("male_piglets", "नर सुअर के बच्चे");
        hi.put("female_piglets", "मादा सुअर के बच्चे");
        translations.put("Hi", hi);

        for (Map.Entry<String, Map<String, String>> entry : translations.entrySet()) {
            String lang = entry.getKey();
            Map<String, String> map = entry.getValue();

            String funcName = "get" + lang + "Translations";
            Pattern pattern = Pattern.compile("(?s)(private fun " + funcName + "\\(\\): Map<String, String> = mapOf\\()(.*?)(    \\))");
            Matcher matcher = pattern.matcher(content);

            if (matcher.find()) {
                String prefix = matcher.group(1);
                String block = matcher.group(2);
                String suffix = matcher.group(3);

                for (Map.Entry<String, String> kv : map.entrySet()) {
                    String key = kv.getKey();
                    String value = kv.getValue();
                    
                    // Regex to replace existing key-value pair
                    Pattern keyPattern = Pattern.compile("(?m)^(\\s*\"" + key + "\"\\s*to\\s*\").*?(\",?)$");
                    Matcher keyMatcher = keyPattern.matcher(block);
                    
                    if (keyMatcher.find()) {
                        block = keyMatcher.replaceFirst("$1" + value + "$2");
                    } else {
                        // Ensure previous line has a comma before appending
                        if (!block.trim().endsWith(",")) {
                            // Find the last non-whitespace character in the block
                            int lastIndex = block.length() - 1;
                            while (lastIndex >= 0 && Character.isWhitespace(block.charAt(lastIndex))) {
                                lastIndex--;
                            }
                            if (lastIndex >= 0) {
                                block = block.substring(0, lastIndex + 1) + "," + block.substring(lastIndex + 1);
                            }
                        }
                        block += "\n        \"" + key + "\" to \"" + value + "\",";
                    }
                }
                content = matcher.replaceFirst(Matcher.quoteReplacement(prefix + block + suffix));
            } else {
                System.out.println("Could not find function: " + funcName);
            }
        }

        Files.write(Paths.get(filePath), content.getBytes("UTF-8"));
        System.out.println("Translations updated successfully!");
    }
}
