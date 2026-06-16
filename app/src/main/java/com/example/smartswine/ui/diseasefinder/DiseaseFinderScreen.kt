package com.example.smartswine.ui.diseasefinder

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.ui.settings.SettingsSection
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.stringResource

data class Disease(
    val nameKey: String,
    val scientificName: String = "",
    val symptomKeys: List<String>,
    val descriptionKey: String,
    val severity: Severity = Severity.MODERATE,
    val preventionKey: String = ""
)

enum class Severity(val key: String) {
    LOW("sev_low"),
    MODERATE("sev_moderate"),
    HIGH("sev_high"),
    CRITICAL("sev_critical")
}

object DiseaseDatabase {
    val symptomGroups = mapOf(
        "sg_skin_coat" to listOf(
            "sym_skin_discoloration", "sym_diamond_lesions", "sym_itching", "sym_hair_loss", 
            "sym_pale_skin", "sym_jaundice", "sym_skin_crusts", "sym_rough_hair", "sym_sunburn", "sym_ear_necrosis",
            "sym_blisters_snout_feet"
        ),
        "sg_digestive_stool" to listOf(
            "sym_diarrhea", "sym_vomiting", "sym_bloody_diarrhea", "sym_blood_in_stool", "sym_rectal_prolapse", 
            "sym_bloat", "sym_dehydration", "sym_loss_of_appetite", "sym_weight_loss"
        ),
        "sg_respiratory" to listOf(
            "sym_coughing", "sym_difficulty_breathing", "sym_nasal_discharge", "sym_sneezing", 
            "sym_thumping", "sym_nose_bleeds", "sym_frothing", "sym_excessive_salivation"
        ),
        "sg_behavioral_nervous" to listOf(
            "sym_lethargy", "sym_seizures", "sym_muscle_stiffness", "sym_trembling", "sym_blindness", "sym_paralysis", 
            "sym_incoordination", "sym_circling", "sym_convulsions", "sym_nervousness", "sym_twitching", "sym_tail_biting"
        ),
        "sg_reproduction" to listOf(
            "sym_abortion", "sym_infertility", "sym_mummified_piglets", "sym_small_litter", "sym_swollen_vulva", "sym_enlarged_scrotum",
            "sym_swollen_udder"
        ),
        "sg_general_limbs" to listOf(
            "sym_high_fever", "sym_lameness", "sym_sudden_death", "sym_swollen_joints", "sym_swollen_navel", 
            "sym_leg_weakness", "sym_swollen_eyelids", "sym_stunted_growth", "sym_pale_mucous", "sym_bleeding_orifices",
            "sym_cracked_hooves", "sym_red_urine"
        )
    )

    val diseases = listOf(
        Disease(
            "dis_asf", "Asfivirus",
            listOf("sym_high_fever", "sym_loss_of_appetite", "sym_sudden_death", "sym_skin_discoloration", "sym_lethargy", "sym_vomiting", "sym_diarrhea"),
            "dis_asf_desc",
            Severity.CRITICAL, "dis_asf_prev"
        ),
        Disease(
            "dis_fmd", "Aphthovirus",
            listOf("sym_high_fever", "sym_lameness", "sym_blisters_snout_feet", "sym_loss_of_appetite", "sym_nasal_discharge"),
            "dis_fmd_desc",
            Severity.HIGH, "dis_fmd_prev"
        ),
        Disease(
            "dis_prrs", "Arterivirus",
            listOf("sym_coughing", "sym_difficulty_breathing", "sym_abortion", "sym_lethargy", "sym_skin_discoloration"),
            "dis_prrs_desc",
            Severity.HIGH, "dis_prrs_prev"
        ),
        Disease(
            "dis_erysipelas", "Erysipelothrix rhusiopathiae",
            listOf("sym_high_fever", "sym_diamond_lesions", "sym_lameness", "sym_sudden_death"),
            "dis_erysipelas_desc",
            Severity.MODERATE, "dis_erysipelas_prev"
        ),
        Disease(
            "dis_csf", "Pestivirus",
            listOf("sym_high_fever", "sym_diarrhea", "sym_skin_discoloration", "sym_coughing", "sym_lethargy"),
            "dis_csf_desc",
            Severity.CRITICAL, "dis_csf_prev"
        ),
        Disease(
            "dis_anthrax", "Bacillus anthracis",
            listOf("sym_sudden_death", "sym_high_fever", "sym_difficulty_breathing", "sym_bleeding_orifices"),
            "dis_anthrax_desc",
            Severity.CRITICAL, "dis_anthrax_prev"
        ),
        Disease(
            "dis_brucellosis", "Brucella suis",
            listOf("sym_abortion", "sym_lameness", "sym_swollen_joints", "sym_lethargy"),
            "dis_brucellosis_desc",
            Severity.HIGH, "dis_brucellosis_prev"
        ),
        Disease(
            "dis_coccidiosis", "Isospora suis",
            listOf("sym_diarrhea", "sym_weight_loss", "sym_lethargy", "sym_loss_of_appetite"),
            "dis_coccidiosis_desc",
            Severity.MODERATE, "dis_coccidiosis_prev"
        ),
        Disease(
            "dis_pneumonia", "Mycoplasma hyopneumoniae",
            listOf("sym_coughing", "sym_difficulty_breathing", "sym_weight_loss", "sym_lethargy"),
            "dis_pneumonia_desc",
            Severity.MODERATE, "dis_pneumonia_prev"
        ),
        Disease(
            "dis_salmonellosis", "Salmonella typhimurium",
            listOf("sym_diarrhea", "sym_high_fever", "sym_skin_discoloration", "sym_vomiting"),
            "dis_salmonellosis_desc",
            Severity.HIGH, "dis_salmonellosis_prev"
        ),
        Disease(
            "dis_ppv", "Parvovirus",
            listOf("sym_abortion", "sym_mummified_piglets", "sym_small_litter"),
            "dis_ppv_desc",
            Severity.MODERATE, "dis_ppv_prev"
        ),
        Disease(
            "dis_rhinitis", "Bordetella bronchiseptica",
            listOf("sym_nasal_discharge", "sym_coughing", "sym_sneezing", "sym_weight_loss"),
            "dis_rhinitis_desc",
            Severity.MODERATE, "dis_rhinitis_prev"
        ),
        Disease(
            "dis_leptospirosis", "Leptospira spp.",
            listOf("sym_abortion", "sym_high_fever", "sym_loss_of_appetite", "sym_jaundice"),
            "dis_leptospirosis_desc",
            Severity.HIGH, "dis_leptospirosis_prev"
        ),
        Disease(
            "dis_influenza", "Influenza A virus",
            listOf("sym_coughing", "sym_high_fever", "sym_nasal_discharge", "sym_difficulty_breathing"),
            "dis_influenza_desc",
            Severity.MODERATE, "dis_influenza_prev"
        ),
        Disease(
            "dis_edema", "Escherichia coli",
            listOf("sym_swollen_eyelids", "sym_seizures", "sym_sudden_death", "sym_loss_of_appetite"),
            "dis_edema_desc",
            Severity.HIGH, "dis_edema_prev"
        ),
        Disease(
            "dis_dysentery", "Brachyspira hyodysenteriae",
            listOf("sym_bloody_diarrhea", "sym_weight_loss", "sym_lethargy"),
            "dis_dysentery_desc",
            Severity.HIGH, "dis_dysentery_prev"
        ),
        Disease(
            "dis_mange", "Sarcoptes scabiei",
            listOf("sym_itching", "sym_hair_loss", "sym_skin_crusts"),
            "dis_mange_desc",
            Severity.LOW, "dis_mange_prev"
        ),
        Disease(
            "dis_anemia", "",
            listOf("sym_pale_skin", "sym_lethargy", "sym_difficulty_breathing"),
            "dis_anemia_desc",
            Severity.MODERATE, "dis_anemia_prev"
        ),
        Disease(
            "dis_ulcers", "",
            listOf("sym_vomiting", "sym_pale_skin", "sym_weight_loss", "sym_blood_in_stool"),
            "dis_ulcers_desc",
            Severity.MODERATE, "dis_ulcers_prev"
        ),
        Disease(
            "dis_pss", "",
            listOf("sym_trembling", "sym_muscle_stiffness", "sym_high_fever", "sym_sudden_death"),
            "dis_pss_desc",
            Severity.HIGH, "dis_pss_prev"
        ),
        Disease(
            "dis_tge", "Coronavirus",
            listOf("sym_vomiting", "sym_diarrhea", "sym_sudden_death", "sym_dehydration"),
            "dis_tge_desc",
            Severity.CRITICAL, "dis_tge_prev"
        ),
        Disease(
            "dis_pasteurellosis", "Pasteurella multocida",
            listOf("sym_coughing", "sym_high_fever", "sym_difficulty_breathing", "sym_thumping"),
            "dis_pasteurellosis_desc",
            Severity.HIGH, "dis_pasteurellosis_prev"
        ),
        Disease(
            "dis_strep", "S. suis",
            listOf("sym_high_fever", "sym_seizures", "sym_lameness", "sym_incoordination"),
            "dis_strep_desc",
            Severity.HIGH, "dis_strep_prev"
        ),
        Disease(
            "dis_mycotoxicosis", "",
            listOf("sym_loss_of_appetite", "sym_vomiting", "sym_abortion", "sym_swollen_vulva"),
            "dis_mycotoxicosis_desc",
            Severity.MODERATE, "dis_mycotoxicosis_prev"
        ),
        Disease(
            "dis_greasy_pig", "Staphylococcus hyicus",
            listOf("sym_skin_discoloration", "sym_skin_crusts", "sym_lethargy", "sym_weight_loss"),
            "dis_greasy_pig_desc",
            Severity.MODERATE, "dis_greasy_pig_prev"
        ),
        Disease(
            "dis_pseudorabies", "Suid herpesvirus 1",
            listOf("sym_seizures", "sym_trembling", "sym_sudden_death", "sym_abortion", "sym_coughing"),
            "dis_pseudorabies_desc",
            Severity.CRITICAL, "dis_pseudorabies_prev"
        ),
        Disease(
            "dis_pcv2", "Circovirus",
            listOf("sym_weight_loss", "sym_difficulty_breathing", "sym_diarrhea", "sym_jaundice", "sym_pale_skin"),
            "dis_pcv2_desc",
            Severity.HIGH, "dis_pcv2_prev"
        ),
        Disease(
            "dis_ped", "Coronavirus",
            listOf("sym_vomiting", "sym_watery_diarrhea", "sym_dehydration", "sym_sudden_death"),
            "dis_ped_desc",
            Severity.CRITICAL, "dis_ped_prev"
        ),
        Disease(
            "dis_je", "Flavivirus",
            listOf("sym_abortion", "sym_mummified_piglets", "sym_infertility", "sym_trembling"),
            "dis_je_desc",
            Severity.HIGH, "dis_je_prev"
        ),
        Disease(
            "dis_rabies", "Lyssavirus",
            listOf("sym_nervousness", "sym_aggression", "sym_excessive_salivation", "sym_paralysis", "sym_sudden_death"),
            "dis_rabies_desc",
            Severity.CRITICAL, "dis_rabies_prev"
        ),
        Disease(
            "dis_pox", "Suipoxvirus",
            listOf("sym_skin_crusts", "sym_lethargy", "sym_loss_of_appetite"),
            "dis_pox_desc",
            Severity.LOW, "dis_pox_prev"
        ),
        Disease(
            "dis_vs", "Rhabdovirus",
            listOf("sym_blisters_snout_feet", "sym_excessive_salivation", "sym_loss_of_appetite"),
            "dis_vs_desc",
            Severity.HIGH, "dis_vs_prev"
        ),
        Disease(
            "dis_clostridial", "Clostridium perfringens",
            listOf("sym_bloody_diarrhea", "sym_sudden_death", "sym_bloat"),
            "dis_clostridial_desc",
            Severity.CRITICAL, "dis_clostridial_prev"
        ),
        Disease(
            "dis_glassers", "Haemophilus parasuis",
            listOf("sym_high_fever", "sym_coughing", "sym_lameness", "sym_swollen_joints", "sym_trembling"),
            "dis_glassers_desc",
            Severity.HIGH, "dis_glassers_prev"
        ),
        Disease(
            "dis_app", "A. pleuropneumoniae",
            listOf("sym_sudden_death", "sym_high_fever", "sym_nose_bleeds", "sym_difficulty_breathing", "sym_coughing"),
            "dis_app_desc",
            Severity.CRITICAL, "dis_app_prev"
        ),
        Disease(
            "dis_ileitis", "Lawsonia intracellularis",
            listOf("sym_diarrhea", "sym_blood_in_stool", "sym_weight_loss", "sym_pale_skin"),
            "dis_ileitis_desc",
            Severity.MODERATE, "dis_ileitis_prev"
        ),
        Disease(
            "dis_tb", "Mycobacterium avium",
            listOf("sym_weight_loss", "sym_lethargy", "sym_coughing"),
            "dis_tb_desc",
            Severity.LOW, "dis_tb_prev"
        ),
        Disease(
            "dis_tetanus", "Clostridium tetani",
            listOf("sym_muscle_stiffness", "sym_seizures", "sym_trembling"),
            "dis_tetanus_desc",
            Severity.HIGH, "dis_tetanus_prev"
        ),
        Disease(
            "dis_eperythrozoonosis", "Mycoplasma suis",
            listOf("sym_pale_skin", "sym_jaundice", "sym_high_fever", "sym_infertility"),
            "dis_eperythrozoonosis_desc",
            Severity.MODERATE, "dis_eperythrozoonosis_prev"
        ),
        Disease(
            "dis_ascaris", "Large Roundworm",
            listOf("sym_coughing", "sym_weight_loss", "sym_stunted_growth", "sym_rough_hair"),
            "dis_ascaris_desc",
            Severity.MODERATE, "dis_ascaris_prev"
        ),
        Disease(
            "dis_whipworms", "Trichuris suis",
            listOf("sym_bloody_diarrhea", "sym_weight_loss", "sym_dehydration"),
            "dis_whipworms_desc",
            Severity.MODERATE, "dis_whipworms_prev"
        ),
        Disease(
            "dis_lungworms", "Metastrongylus spp.",
            listOf("sym_coughing", "sym_thumping", "sym_stunted_growth"),
            "dis_lungworms_desc",
            Severity.MODERATE, "dis_lungworms_prev"
        ),
        Disease(
            "dis_kidney_worms", "Stephanurus dentatus",
            listOf("sym_weight_loss", "sym_stunted_growth", "sym_leg_weakness"),
            "dis_kidney_worms_desc",
            Severity.MODERATE, "dis_kidney_worms_prev"
        ),
        Disease(
            "dis_lice", "Haematopinus suis",
            listOf("sym_itching", "sym_rough_hair", "sym_pale_skin", "sym_anemia"),
            "dis_lice_desc",
            Severity.LOW, "dis_lice_prev"
        ),
        Disease(
            "dis_ringworm", "Dermatophytosis",
            listOf("sym_skin_crusts", "sym_itching"),
            "dis_ringworm_desc",
            Severity.LOW, "dis_ringworm_prev"
        ),
        Disease(
            "dis_mulberry_heart", "Vit E/Se Deficiency",
            listOf("sym_sudden_death", "sym_difficulty_breathing", "sym_trembling"),
            "dis_mulberry_heart_desc",
            Severity.HIGH, "dis_mulberry_heart_prev"
        ),
        Disease(
            "dis_vit_a", "",
            listOf("sym_blindness", "sym_incoordination", "sym_infertility", "sym_trembling"),
            "dis_vit_a_desc",
            Severity.MODERATE, "dis_vit_a_prev"
        ),
        Disease(
            "dis_rickets", "Vit D/Ca/P Deficiency",
            listOf("sym_lameness", "sym_swollen_joints", "sym_leg_weakness", "sym_fractures"),
            "dis_rickets_desc",
            Severity.MODERATE, "dis_rickets_prev"
        ),
        Disease(
            "dis_salt_poisoning", "Water Deprivation",
            listOf("sym_seizures", "sym_blindness", "sym_circling", "sym_convulsions", "sym_thirst"),
            "dis_salt_poisoning_desc",
            Severity.HIGH, "dis_salt_poisoning_prev"
        ),
        Disease(
            "dis_heat_stroke", "",
            listOf("sym_high_fever", "sym_difficulty_breathing", "sym_lethargy", "sym_sudden_death"),
            "dis_heat_stroke_desc",
            Severity.HIGH, "dis_heat_stroke_prev"
        ),
        Disease(
            "dis_rectal_prolapse", "",
            listOf("sym_rectal_prolapse", "sym_blood_in_stool"),
            "dis_rectal_prolapse_desc",
            Severity.MODERATE, "dis_rectal_prolapse_prev"
        ),
        Disease(
            "dis_umbilical_hernia", "",
            listOf("sym_swollen_navel"),
            "dis_umbilical_hernia_desc",
            Severity.LOW, "dis_umbilical_hernia_prev"
        ),
        Disease(
            "dis_scrotal_hernia", "",
            listOf("sym_enlarged_scrotum"),
            "dis_scrotal_hernia_desc",
            Severity.LOW, "dis_scrotal_hernia_prev"
        ),
        Disease(
            "dis_splayleg", "Myofibrillar hypoplasia",
            listOf("sym_leg_weakness", "sym_difficulty_breathing", "sym_sudden_death"),
            "dis_splayleg_desc",
            Severity.MODERATE, "dis_splayleg_prev"
        ),
        Disease(
            "dis_vulvovaginitis", "Zearalenone Toxicity",
            listOf("sym_swollen_vulva", "sym_abortion", "sym_infertility"),
            "dis_vulvovaginitis_desc",
            Severity.MODERATE, "dis_vulvovaginitis_prev"
        ),
        Disease(
            "dis_seneca", "Senecavirus A",
            listOf("sym_blisters_snout_feet", "sym_lameness", "sym_sudden_death"),
            "dis_seneca_desc",
            Severity.HIGH, "dis_seneca_prev"
        ),
        Disease(
            "dis_deltacoronavirus", "PDCoV",
            listOf("sym_watery_diarrhea", "sym_vomiting", "sym_dehydration"),
            "dis_deltacoronavirus_desc",
            Severity.HIGH, "dis_deltacoronavirus_prev"
        ),
        Disease(
            "dis_rotavirus", "Rotavirus",
            listOf("sym_diarrhea", "sym_vomiting", "sym_weight_loss"),
            "dis_rotavirus_desc",
            Severity.MODERATE, "dis_rotavirus_prev"
        ),
        Disease(
            "dis_teschen", "Sapelovirus/Teschovirus",
            listOf("sym_paralysis", "sym_incoordination", "sym_seizures", "sym_trembling"),
            "dis_teschen_desc",
            Severity.HIGH, "dis_teschen_prev"
        ),
        Disease(
            "dis_vesicular", "Enterovirus",
            listOf("sym_blisters_snout_feet", "sym_lameness", "sym_high_fever"),
            "dis_vesicular_desc",
            Severity.HIGH, "dis_vesicular_prev"
        ),
        Disease(
            "dis_melioidosis", "Burkholderia pseudomallei",
            listOf("sym_high_fever", "sym_coughing", "sym_lameness", "sym_abscesses"),
            "dis_melioidosis_desc",
            Severity.MODERATE, "dis_melioidosis_prev"
        ),
        Disease(
            "dis_trypanosomiasis", "Trypanosoma spp.",
            listOf("sym_high_fever", "sym_anemia", "sym_weight_loss", "sym_lethargy", "sym_abortion"),
            "dis_trypanosomiasis_desc",
            Severity.HIGH, "dis_trypanosomiasis_prev"
        ),
        Disease(
            "dis_cysticercosis", "Taenia solium larvae",
            listOf("sym_muscle_stiffness", "sym_seizures"),
            "dis_cysticercosis_desc",
            Severity.LOW, "dis_cysticercosis_prev"
        ),
        Disease(
            "dis_toxoplasmosis", "Toxoplasma gondii",
            listOf("sym_abortion", "sym_high_fever", "sym_difficulty_breathing", "sym_lethargy"),
            "dis_toxoplasmosis_desc",
            Severity.MODERATE, "dis_toxoplasmosis_prev"
        ),
        Disease(
            "dis_listeriosis", "Listeria monocytogenes",
            listOf("sym_seizures", "sym_circling", "sym_abortion", "sym_high_fever"),
            "dis_listeriosis_desc",
            Severity.MODERATE, "dis_listeriosis_prev"
        ),
        Disease(
            "dis_ear_necrosis", "",
            listOf("sym_ear_necrosis", "sym_skin_discoloration", "sym_lethargy"),
            "dis_ear_necrosis_desc",
            Severity.MODERATE, "dis_ear_necrosis_prev"
        ),
        Disease(
            "dis_sunburn", "",
            listOf("sym_sunburn", "sym_skin_discoloration", "sym_lethargy"),
            "dis_sunburn_desc",
            Severity.LOW, "dis_sunburn_prev"
        ),
        Disease(
            "dis_tail_biting", "",
            listOf("sym_tail_biting", "sym_blood_in_stool", "sym_lameness"),
            "dis_tail_biting_desc",
            Severity.MODERATE, "dis_tail_biting_prev"
        ),
        Disease(
            "dis_navel_ill", "",
            listOf("sym_swollen_navel", "sym_high_fever", "sym_lameness"),
            "dis_navel_ill_desc",
            Severity.MODERATE, "dis_navel_ill_prev"
        ),
        Disease(
            "dis_iron_toxicity", "",
            listOf("sym_sudden_death", "sym_difficulty_breathing", "sym_muscle_stiffness"),
            "dis_iron_toxicity_desc",
            Severity.HIGH, "dis_iron_toxicity_prev"
        ),
        Disease(
            "dis_aflatoxicosis", "Aspergillus flavus toxin",
            listOf("sym_loss_of_appetite", "sym_weight_loss", "sym_jaundice", "sym_lethargy", "sym_sudden_death"),
            "dis_aflatoxicosis_desc",
            Severity.HIGH, "dis_aflatoxicosis_prev"
        ),
        Disease(
            "dis_parakeratosis", "Zinc deficiency",
            listOf("sym_skin_crusts", "sym_rough_hair", "sym_stunted_growth"),
            "dis_parakeratosis_desc",
            Severity.MODERATE, "dis_parakeratosis_prev"
        ),
        Disease(
            "dis_biotin_deficiency", "Vitamin H deficiency",
            listOf("sym_lameness", "sym_hair_loss", "sym_leg_weakness", "sym_cracked_hooves"),
            "dis_biotin_deficiency_desc",
            Severity.MODERATE, "dis_biotin_deficiency_prev"
        ),
        Disease(
            "dis_gossypol_toxicity", "Gossypol poisoning",
            listOf("sym_difficulty_breathing", "sym_thumping", "sym_loss_of_appetite", "sym_sudden_death"),
            "dis_gossypol_toxicity_desc",
            Severity.HIGH, "dis_gossypol_toxicity_prev"
        ),
        Disease(
            "dis_cassava_poisoning", "Cyanogenic glycosides",
            listOf("sym_difficulty_breathing", "sym_incoordination", "sym_sudden_death", "sym_lethargy"),
            "dis_cassava_poisoning_desc",
            Severity.CRITICAL, "dis_cassava_poisoning_prev"
        ),
        Disease(
            "dis_sweet_potato_toxicity", "Ipomeamarone toxicity",
            listOf("sym_difficulty_breathing", "sym_thumping", "sym_frothing"),
            "dis_sweet_potato_toxicity_desc",
            Severity.HIGH, "dis_sweet_potato_toxicity_prev"
        ),
        Disease(
            "dis_mma", "Mastitis-Metritis-Agalactia",
            listOf("sym_high_fever", "sym_lethargy", "sym_loss_of_appetite", "sym_swollen_udder"),
            "dis_mma_desc",
            Severity.HIGH, "dis_mma_prev"
        ),
        Disease(
            "dis_pwd", "Escherichia coli (F4/F18)",
            listOf("sym_diarrhea", "sym_dehydration", "sym_loss_of_appetite", "sym_weight_loss"),
            "dis_pwd_desc",
            Severity.HIGH, "dis_pwd_prev"
        ),
        Disease(
            "dis_trichinellosis", "Trichinella spiralis",
            listOf("sym_muscle_stiffness", "sym_lethargy", "sym_incoordination"),
            "dis_trichinellosis_desc",
            Severity.MODERATE, "dis_trichinellosis_prev"
        ),
        Disease(
            "dis_hydatid_disease", "Echinococcus granulosus",
            listOf("sym_weight_loss", "sym_stunted_growth", "sym_lethargy"),
            "dis_hydatid_disease_desc",
            Severity.LOW, "dis_hydatid_disease_prev"
        ),
        Disease(
            "dis_phe", "Lawsonia intracellularis",
            listOf("sym_bloody_diarrhea", "sym_pale_skin", "sym_sudden_death"),
            "dis_phe_desc",
            Severity.CRITICAL, "dis_phe_prev"
        ),
        Disease(
            "dis_mycoplasma_arthritis", "Mycoplasma hyosynoviae",
            listOf("sym_lameness", "sym_swollen_joints", "sym_leg_weakness"),
            "dis_mycoplasma_arthritis_desc",
            Severity.MODERATE, "dis_mycoplasma_arthritis_prev"
        ),
        Disease(
            "dis_foot_rot", "Fusobacterium necrophorum",
            listOf("sym_lameness", "sym_lethargy", "sym_loss_of_appetite", "sym_cracked_hooves"),
            "dis_foot_rot_desc",
            Severity.MODERATE, "dis_foot_rot_prev"
        ),
        Disease(
            "dis_babesiosis", "Babesia trautmanni",
            listOf("sym_high_fever", "sym_jaundice", "sym_pale_skin", "sym_abortion", "sym_lethargy", "sym_red_urine"),
            "dis_babesiosis_desc",
            Severity.HIGH, "dis_babesiosis_prev"
        ),
        Disease(
            "dis_anaplasmosis", "Anaplasma marginale",
            listOf("sym_high_fever", "sym_pale_skin", "sym_jaundice", "sym_lethargy"),
            "dis_anaplasmosis_desc",
            Severity.MODERATE, "dis_anaplasmosis_prev"
        ),
        Disease(
            "dis_sow_hysteria", "Puerperal psychosis",
            listOf("sym_nervousness", "sym_lethargy"),
            "dis_sow_hysteria_desc",
            Severity.MODERATE, "dis_sow_hysteria_prev"
        ),
        Disease(
            "dis_thorny_headed_worm", "Macracanthorhynchus hirudinaceus",
            listOf("sym_diarrhea", "sym_weight_loss", "sym_stunted_growth", "sym_sudden_death"),
            "dis_thorny_headed_worm_desc",
            Severity.MODERATE, "dis_thorny_headed_worm_prev"
        ),
        Disease(
            "dis_strongyloidiasis", "Strongyloides ransomi",
            listOf("sym_diarrhea", "sym_dehydration", "sym_stunted_growth", "sym_sudden_death"),
            "dis_strongyloidiasis_desc",
            Severity.HIGH, "dis_strongyloidiasis_prev"
        ),
        Disease(
            "dis_hyostrongylosis", "Hyostrongylus rubidus",
            listOf("sym_weight_loss", "sym_pale_skin", "sym_rough_hair", "sym_loss_of_appetite"),
            "dis_hyostrongylosis_desc",
            Severity.MODERATE, "dis_hyostrongylosis_prev"
        ),
        Disease(
            "dis_oesophagostomiasis", "Oesophagostomum spp.",
            listOf("sym_weight_loss", "sym_diarrhea", "sym_rough_hair"),
            "dis_oesophagostomiasis_desc",
            Severity.MODERATE, "dis_oesophagostomiasis_prev"
        ),
        Disease(
            "dis_fascioliasis", "Fasciola gigantica",
            listOf("sym_weight_loss", "sym_lethargy", "sym_stunted_growth", "sym_jaundice"),
            "dis_fascioliasis_desc",
            Severity.MODERATE, "dis_fascioliasis_prev"
        ),
        Disease(
            "dis_emcv", "Cardiovirus",
            listOf("sym_sudden_death", "sym_difficulty_breathing", "sym_abortion", "sym_trembling"),
            "dis_emcv_desc",
            Severity.CRITICAL, "dis_emcv_prev"
        ),
        Disease(
            "dis_demodectic_mange", "Demodex phylloides",
            listOf("sym_skin_crusts", "sym_itching", "sym_hair_loss"),
            "dis_demodectic_mange_desc",
            Severity.LOW, "dis_demodectic_mange_prev"
        ),
        Disease(
            "dis_pityriasis_rosea", "Pseudo-ringworm",
            listOf("sym_skin_crusts", "sym_rough_hair"),
            "dis_pityriasis_rosea_desc",
            Severity.LOW, "dis_pityriasis_rosea_prev"
        ),
        Disease(
            "dis_photosensitization", "Solar dermatitis",
            listOf("sym_skin_discoloration", "sym_sunburn", "sym_skin_crusts"),
            "dis_photosensitization_desc",
            Severity.MODERATE, "dis_photosensitization_prev"
        ),
        Disease(
            "dis_shoulder_ulcers", "Decubitus ulcers",
            listOf("sym_lameness", "sym_pale_skin"),
            "dis_shoulder_ulcers_desc",
            Severity.MODERATE, "dis_shoulder_ulcers_prev"
        ),
        Disease(
            "dis_ear_biting", "",
            listOf("sym_ear_necrosis", "sym_bleeding_orifices"),
            "dis_ear_biting_desc",
            Severity.MODERATE, "dis_ear_biting_prev"
        ),
        Disease(
            "dis_gastric_torsion", "Gastric dilation-volvulus",
            listOf("sym_bloat", "sym_sudden_death", "sym_vomiting"),
            "dis_gastric_torsion_desc",
            Severity.CRITICAL, "dis_gastric_torsion_prev"
        ),
        Disease(
            "dis_c_difficile", "Clostridioides difficile",
            listOf("sym_diarrhea", "sym_dehydration", "sym_lethargy"),
            "dis_c_difficile_desc",
            Severity.MODERATE, "dis_c_difficile_prev"
        ),
        Disease(
            "dis_cold_stress", "",
            listOf("sym_trembling", "sym_lethargy", "sym_sudden_death"),
            "dis_cold_stress_desc",
            Severity.HIGH, "dis_cold_stress_prev"
        ),
        Disease(
            "dis_necrotic_enteritis", "Clostridium perfringens Type C",
            listOf("sym_bloody_diarrhea", "sym_sudden_death", "sym_dehydration"),
            "dis_necrotic_enteritis_desc",
            Severity.CRITICAL, "dis_necrotic_enteritis_prev"
        ),
        Disease(
            "dis_navel_bleeding", "",
            listOf("sym_bleeding_orifices", "sym_pale_skin", "sym_sudden_death"),
            "dis_navel_bleeding_desc",
            Severity.HIGH, "dis_navel_bleeding_prev"
        ),
        Disease(
            "dis_spirochetal_colitis", "Brachyspira pilosicoli",
            listOf("sym_diarrhea", "sym_weight_loss", "sym_stunted_growth"),
            "dis_spirochetal_colitis_desc",
            Severity.MODERATE, "dis_spirochetal_colitis_prev"
        ),
        Disease(
            "dis_salmonella_septicemia", "Salmonella choleraesuis",
            listOf("sym_high_fever", "sym_skin_discoloration", "sym_difficulty_breathing", "sym_sudden_death"),
            "dis_salmonella_septicemia_desc",
            Severity.HIGH, "dis_salmonella_septicemia_prev"
        ),
        Disease(
            "dis_prdc", "Porcine Respiratory Disease Complex",
            listOf("sym_coughing", "sym_difficulty_breathing", "sym_nasal_discharge", "sym_stunted_growth"),
            "dis_prdc_desc",
            Severity.HIGH, "dis_prdc_prev"
        ),
        Disease(
            "dis_ergotism", "Claviceps purpurea",
            listOf("sym_lameness", "sym_skin_crusts", "sym_abortion", "sym_infertility"),
            "dis_ergotism_desc",
            Severity.HIGH, "dis_ergotism_prev"
        ),
        Disease(
            "dis_bvdv_pig", "Pestivirus",
            listOf("sym_abortion", "sym_mummified_piglets", "sym_small_litter"),
            "dis_bvdv_pig_desc",
            Severity.MODERATE, "dis_bvdv_pig_prev"
        ),
        Disease(
            "dis_endocarditis", "Streptococcal endocarditis",
            listOf("sym_sudden_death", "sym_difficulty_breathing", "sym_lethargy", "sym_lameness"),
            "dis_endocarditis_desc",
            Severity.HIGH, "dis_endocarditis_prev"
        ),
        Disease(
            "dis_polyserositis", "Mycoplasma hyorhinis",
            listOf("sym_high_fever", "sym_difficulty_breathing", "sym_swollen_joints", "sym_lameness"),
            "dis_polyserositis_desc",
            Severity.MODERATE, "dis_polyserositis_prev"
        ),
        Disease(
            "dis_congenital_tremor", "Atypical Porcine Pestivirus",
            listOf("sym_trembling", "sym_incoordination", "sym_sudden_death"),
            "dis_congenital_tremor_desc",
            Severity.MODERATE, "dis_congenital_tremor_prev"
        )
    )
}

@Composable
fun DiseaseFinderScreen(onNavigateToPaywall: () -> Unit, onBack: () -> Unit) {
    val selectedSymptoms = remember { mutableStateOf(setOf<String>()) }
    val diagnosisResult = remember { mutableStateOf<List<Pair<Disease, Int>>>(emptyList()) }
    val showResults = remember { mutableStateOf(false) }

    DiseaseFinderContent(
        selectedSymptoms = selectedSymptoms.value,
        onSymptomToggle = { symptom ->
            selectedSymptoms.value = if (symptom in selectedSymptoms.value) {
                selectedSymptoms.value - symptom
            } else {
                selectedSymptoms.value + symptom
            }
        },
        diagnosisResult = diagnosisResult.value,
        showResults = showResults.value,
        onAnalyze = {
            val results = DiseaseDatabase.diseases.map { disease ->
                val matchCount = disease.symptomKeys.count { it in selectedSymptoms.value }
                disease to matchCount
            }.filter { it.second > 0 }
                .sortedByDescending { it.second }
            diagnosisResult.value = results
            showResults.value = true
        },
        onReset = {
            selectedSymptoms.value = emptySet()
            diagnosisResult.value = emptyList()
            showResults.value = false
        },
        onNavigateToPaywall = onNavigateToPaywall,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiseaseFinderContent(
    selectedSymptoms: Set<String>,
    onSymptomToggle: (String) -> Unit,
    diagnosisResult: List<Pair<Disease, Int>>,
    showResults: Boolean,
    onAnalyze: () -> Unit,
    onReset: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onBack: () -> Unit
) {
    val isPremium = com.example.smartswine.utils.LocalIsPremium.current
    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = stringResource("symptoms_analyzer"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onReset) {
                        Text(stringResource("diag_reset"), color = MaterialTheme.colorScheme.primary)
                    }
                }
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource("diag_intro"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        stringResource("diag_observed_symptoms"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                DiseaseDatabase.symptomGroups.forEach { (groupKey, symptoms) ->
                    item {
                        val lang = LocalAppLanguage.current.code
                        val translatedTitle = stringResource(groupKey)
                        val sortedSymptoms = symptoms.sortedBy { key -> Translator.getString(key, lang) }
                        
                        SettingsSection(
                            title = translatedTitle,
                            isCollapsible = true
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                sortedSymptoms.forEach { symptomKey ->
                                    val isSelected = symptomKey in selectedSymptoms
                                    OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSymptomToggle(symptomKey) },
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        ),
                                        colors = CardDefaults.outlinedCardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = stringResource(symptomKey),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    com.example.smartswine.utils.PremiumWrapper(
                        isPremium = isPremium,
                        onLockedClick = onNavigateToPaywall
                    ) {
                        Button(
                            onClick = {
                                if (isPremium) onAnalyze()
                                else onNavigateToPaywall()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .height(56.dp),
                            enabled = selectedSymptoms.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource("diag_analyze_btn"))
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = showResults,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        DiagnosisResults(diagnosisResult, selectedSymptoms)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }
}

@Composable
fun DiagnosisResults(results: List<Pair<Disease, Int>>, selectedSymptoms: Set<String>) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)) {
        Text(stringResource("diag_potential_matches"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (results.isEmpty()) {
            Text(stringResource("diag_no_matches"), color = MaterialTheme.colorScheme.error)
        } else {
            results.forEach { (disease, matches) ->
                DiseaseResultCard(disease, matches, selectedSymptoms)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource("diag_disclaimer"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun DiseaseResultCard(disease: Disease, matchCount: Int, selectedSymptoms: Set<String>) {
    val expanded = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded.value = !expanded.value },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(disease.nameKey), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (disease.scientificName.isNotEmpty()) {
                        Text(disease.scientificName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Surface(
                    color = when(disease.severity) {
                        Severity.CRITICAL -> MaterialTheme.colorScheme.error
                        Severity.HIGH -> Color(0xFFF44336)
                        Severity.MODERATE -> Color(0xFFFF9800)
                        Severity.LOW -> Color(0xFF4CAF50)
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        stringResource(disease.severity.key).uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Text(
                stringResource("diag_match_accuracy", matchCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (expanded.value) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(stringResource(disease.descriptionKey), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    val unselectedSymptoms = disease.symptomKeys.filter { it !in selectedSymptoms }
                    if (unselectedSymptoms.isNotEmpty()) {
                        Text(
                            text = stringResource("diag_other_symptoms_to_lookout"),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        unselectedSymptoms.forEach { symKey ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(stringResource(symKey), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(stringResource("diag_prevention_plan"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(disease.preventionKey), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DiseaseFinderPreview() {
    SmartSwineTheme {
        DiseaseFinderContent(
            selectedSymptoms = setOf("sym_high_fever", "sym_coughing"),
            onSymptomToggle = {},
            diagnosisResult = listOf(
                DiseaseDatabase.diseases[0] to 2,
                DiseaseDatabase.diseases[1] to 1
            ),
            showResults = true,
            onAnalyze = {},
            onReset = {},
            onNavigateToPaywall = {},
            onBack = {}
        )
    }
}
