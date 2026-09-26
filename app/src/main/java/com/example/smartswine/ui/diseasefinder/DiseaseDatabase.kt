package com.example.smartswine.ui.diseasefinder

import kotlin.math.roundToInt
import com.example.smartswine.model.Pig
import com.example.smartswine.model.PigStatus
import com.example.smartswine.model.PigPurpose

enum class PigStage(val key: String, val label: String) {
    ALL("stage_all", "All Stages"),
    SUCKLING("stage_suckling", "Suckling (< 4 wks)"),
    NURSERY("stage_nursery", "Nursery / Weaners (4–10 wks)"),
    GROWER_FINISHER("stage_grower_finisher", "Growers / Finishers (10–24 wks)"),
    BREEDING_STOCK("stage_breeding_stock", "Breeding Stock (Sows/Boars)");

    companion object {
        fun fromPig(pig: Pig?): PigStage {
            if (pig == null) return ALL
            return when (pig.statusEnum) {
                PigStatus.PIGLET -> SUCKLING
                PigStatus.STARTER -> NURSERY
                PigStatus.GROWER, PigStatus.FINISHER -> GROWER_FINISHER
                PigStatus.SOW, PigStatus.GILT, PigStatus.BOAR, PigStatus.BARROW,
                PigStatus.PREGNANT, PigStatus.LACTATING, PigStatus.NURSING -> BREEDING_STOCK
                else -> {
                    if (pig.purposeEnum == PigPurpose.BREEDER) BREEDING_STOCK
                    else ALL
                }
            }
        }
    }
}

data class Disease(
    val nameKey: String,
    val scientificName: String = "",
    val symptomKeys: List<String>,
    val descriptionKey: String,
    val severity: Severity = Severity.MODERATE,
    val preventionKey: String = "",
    val stages: Set<PigStage> = setOf(PigStage.ALL),
    val isNotifiable: Boolean = false,
    val biosecurityProtocol: String? = null
)

enum class Severity(val key: String) {
    LOW("sev_low"),
    MODERATE("sev_moderate"),
    HIGH("sev_high"),
    CRITICAL("sev_critical")
}

data class DiagnosisMatch(
    val disease: Disease,
    val matchPercentage: Int,
    val matchedCount: Int,
    val totalSymptoms: Int,
    val matchedWeight: Double,
    val totalWeight: Double,
    val matchedPathognomonic: List<String>,
    val isNotifiableAlert: Boolean
)

object DiseaseDatabase {
    val symptomWeights = mapOf(
        // Weight 3.0: Pathognomonic / Hallmark Signs
        "sym_diamond_lesions" to 3.0,
        "sym_blisters_snout_feet" to 3.0,
        "sym_bleeding_orifices" to 3.0,
        "sym_nose_bleeds" to 3.0,
        "sym_mummified_piglets" to 3.0,
        "sym_rectal_prolapse" to 3.0,
        "sym_convulsions" to 3.0,
        "sym_paralysis" to 3.0,

        // Weight 2.0: Moderate Specificity
        "sym_bloody_diarrhea" to 2.0,
        "sym_abortion" to 2.0,
        "sym_thumping" to 2.0,
        "sym_swollen_joints" to 2.0,
        "sym_trembling" to 2.0,
        "sym_seizures" to 2.0,
        "sym_swollen_eyelids" to 2.0,
        "sym_jaundice" to 2.0,
        "sym_ear_necrosis" to 2.0,
        "sym_skin_crusts" to 2.0,
        "sym_frothing" to 2.0,
        "sym_excessive_salivation" to 2.0,
        "sym_blood_in_stool" to 2.0,
        "sym_difficulty_breathing" to 2.0,
        "sym_sudden_death" to 2.0,
        "sym_watery_diarrhea" to 2.0,
        "sym_infertility" to 2.0,
        "sym_enlarged_scrotum" to 2.0,
        "sym_swollen_udder" to 2.0,
        "sym_swollen_vulva" to 2.0,
        "sym_abscesses" to 2.0,
        "sym_swollen_navel" to 2.0
    )

    fun getSymptomWeight(key: String): Double = symptomWeights[key] ?: 1.0
    fun isPathognomonic(key: String): Boolean = getSymptomWeight(key) >= 3.0

    val symptomSubtitles = mapOf(
        "sym_thumping" to "Rapid, jerky abdominal/belly breathing",
        "sym_diamond_lesions" to "Raised diamond/square red-purple skin plaques",
        "sym_blisters_snout_feet" to "Fluid-filled sores/vesicles on snout, lips, or hoof band",
        "sym_bleeding_orifices" to "Dark blood from nostrils, mouth, or rectum",
        "sym_nose_bleeds" to "Active blood dripping from the snout",
        "sym_rectal_prolapse" to "Internal pink/red tissue protruding outside the anus",
        "sym_skin_discoloration" to "Purplish or blue-red patches on ears, belly, or snout",
        "sym_incoordination" to "Ataxia, wobbly gait, staggering or swaying while walking",
        "sym_trembling" to "Shaking, shivering, or involuntary body quivers",
        "sym_mummified_piglets" to "Blackened, shriveled dead piglets born at farrowing",
        "sym_swollen_eyelids" to "Puffy, swollen eyes (common in Edema disease)",
        "sym_jaundice" to "Yellowish tint in eyes, mucous membranes, or pale skin",
        "sym_ear_necrosis" to "Blackening, crusting, or rotting of ear tips",
        "sym_convulsions" to "Paddling legs, violent uncontrolled body spasms",
        "sym_watery_diarrhea" to "Profuse liquid yellow/grey scours",
        "sym_bloody_diarrhea" to "Mucoid red/dark tarry scours",
        "sym_stunted_growth" to "Runts failing to gain weight alongside pen-mates",
        "sym_pale_skin" to "Chalky white skin and gums (severe anemia)",
        "sym_rough_hair" to "Dull, unkempt, bristly coat standing on end",
        "sym_muscle_stiffness" to "Stiff wooden gait, locked jaws, or arched back (tetanus)",
        "sym_circling" to "Compulsive walking in circles or tilted head",
        "sym_bloat" to "Severely distended, tight, swollen belly",
        "sym_nasal_discharge" to "Mucus, phlegm, or pus dripping from snout",
        "sym_excessive_salivation" to "Heavy drooling, foaming, or slobbering",
        "sym_blindness" to "Bumping into pen walls, unreactive pupils",
        "sym_swollen_joints" to "Enlarged, hot, painful hocks, knees, or feet",
        "sym_tail_biting" to "Aggressive chewing and bloody wounds on tails",
        "sym_cracked_hooves" to "Splits, fissures, or deep cracks in hoof walls",
        "sym_swollen_navel" to "Enlarged, hot, infected umbilical stump"
    )

    fun getSymptomSubtitle(key: String): String? = symptomSubtitles[key]

    fun diagnose(
        selectedSymptoms: Set<String>,
        stageFilter: PigStage = PigStage.ALL
    ): List<DiagnosisMatch> {
        if (selectedSymptoms.isEmpty()) return emptyList()

        return diseases.asSequence()
            .filter { disease ->
                if (stageFilter == PigStage.ALL) true
                else PigStage.ALL in disease.stages || stageFilter in disease.stages
            }
            .mapNotNull { disease ->
                val matchedSymptoms = disease.symptomKeys.filter { it in selectedSymptoms }
                if (matchedSymptoms.isEmpty()) return@mapNotNull null

                val matchedWeight = matchedSymptoms.sumOf { getSymptomWeight(it) }
                val totalWeight = disease.symptomKeys.sumOf { getSymptomWeight(it) }.coerceAtLeast(1.0)
                val matchPercentage = ((matchedWeight / totalWeight) * 100.0).roundToInt().coerceIn(1, 100)

                val matchedPathognomonic = matchedSymptoms.filter { isPathognomonic(it) }
                val isAlert = disease.isNotifiable && (matchPercentage >= 35 || matchedPathognomonic.isNotEmpty() || matchedSymptoms.size >= 2)

                DiagnosisMatch(
                    disease = disease,
                    matchPercentage = matchPercentage,
                    matchedCount = matchedSymptoms.size,
                    totalSymptoms = disease.symptomKeys.size,
                    matchedWeight = matchedWeight,
                    totalWeight = totalWeight,
                    matchedPathognomonic = matchedPathognomonic,
                    isNotifiableAlert = isAlert
                )
            }
            .sortedWith(
                compareByDescending<DiagnosisMatch> { it.isNotifiableAlert }
                    .thenByDescending { it.matchPercentage }
                    .thenByDescending { it.matchedWeight }
                    .thenByDescending { it.disease.severity.ordinal }
            )
            .toList()
    }

    val symptomGroups = mapOf(
        "sg_skin_coat" to listOf(
            "sym_blisters_snout_feet", "sym_diamond_lesions", "sym_ear_necrosis", "sym_hair_loss", 
            "sym_itching", "sym_jaundice", "sym_pale_skin", "sym_rough_hair", "sym_skin_crusts", 
            "sym_skin_discoloration", "sym_sunburn"
        ),
        "sg_digestive_stool" to listOf(
            "sym_bloat", "sym_blood_in_stool", "sym_bloody_diarrhea", "sym_dehydration", 
            "sym_diarrhea", "sym_loss_of_appetite", "sym_rectal_prolapse", "sym_vomiting", "sym_weight_loss"
        ),
        "sg_respiratory" to listOf(
            "sym_coughing", "sym_difficulty_breathing", "sym_excessive_salivation", "sym_frothing", 
            "sym_nasal_discharge", "sym_nose_bleeds", "sym_sneezing", "sym_thumping"
        ),
        "sg_behavioral_nervous" to listOf(
            "sym_blindness", "sym_circling", "sym_convulsions", "sym_incoordination", "sym_lethargy", 
            "sym_muscle_stiffness", "sym_nervousness", "sym_paralysis", "sym_seizures", "sym_tail_biting", 
            "sym_trembling", "sym_twitching"
        ),
        "sg_reproduction" to listOf(
            "sym_abortion", "sym_enlarged_scrotum", "sym_infertility", "sym_mummified_piglets", 
            "sym_small_litter", "sym_swollen_udder", "sym_swollen_vulva"
        ),
        "sg_general_limbs" to listOf(
            "sym_abscesses", "sym_anemia", "sym_bleeding_orifices", "sym_cracked_hooves", "sym_fractures", 
            "sym_high_fever", "sym_lameness", "sym_leg_weakness", "sym_pale_mucous", "sym_red_urine", 
            "sym_stunted_growth", "sym_sudden_death", "sym_swollen_eyelids", "sym_swollen_joints", 
            "sym_swollen_navel", "sym_thirst", "sym_watery_diarrhea"
        )
    )

    val diseases = listOf(
        Disease(
            "dis_asf", "Asfivirus",
            listOf("sym_high_fever", "sym_loss_of_appetite", "sym_sudden_death", "sym_skin_discoloration", "sym_lethargy", "sym_vomiting", "sym_diarrhea"),
            "dis_asf_desc",
            Severity.CRITICAL, "dis_asf_prev",
            stages = setOf(PigStage.ALL),
            isNotifiable = true,
            biosecurityProtocol = "bio_proto_asf"
        ),
        Disease(
            "dis_fmd", "Aphthovirus",
            listOf("sym_high_fever", "sym_lameness", "sym_blisters_snout_feet", "sym_loss_of_appetite", "sym_nasal_discharge"),
            "dis_fmd_desc",
            Severity.HIGH, "dis_fmd_prev",
            stages = setOf(PigStage.ALL),
            isNotifiable = true,
            biosecurityProtocol = "bio_proto_fmd"
        ),
        Disease(
            "dis_prrs", "Arterivirus",
            listOf("sym_coughing", "sym_difficulty_breathing", "sym_abortion", "sym_lethargy", "sym_skin_discoloration"),
            "dis_prrs_desc",
            Severity.HIGH, "dis_prrs_prev",
            stages = setOf(PigStage.ALL)
        ),
        Disease(
            "dis_erysipelas", "Erysipelothrix rhusiopathiae",
            listOf("sym_high_fever", "sym_diamond_lesions", "sym_lameness", "sym_sudden_death"),
            "dis_erysipelas_desc",
            Severity.MODERATE, "dis_erysipelas_prev",
            stages = setOf(PigStage.GROWER_FINISHER, PigStage.BREEDING_STOCK)
        ),
        Disease(
            "dis_csf", "Pestivirus",
            listOf("sym_high_fever", "sym_diarrhea", "sym_skin_discoloration", "sym_coughing", "sym_lethargy"),
            "dis_csf_desc",
            Severity.CRITICAL, "dis_csf_prev",
            stages = setOf(PigStage.ALL),
            isNotifiable = true,
            biosecurityProtocol = "bio_proto_csf"
        ),
        Disease(
            "dis_anthrax", "Bacillus anthracis",
            listOf("sym_sudden_death", "sym_high_fever", "sym_difficulty_breathing", "sym_bleeding_orifices"),
            "dis_anthrax_desc",
            Severity.CRITICAL, "dis_anthrax_prev",
            stages = setOf(PigStage.ALL),
            isNotifiable = true,
            biosecurityProtocol = "bio_proto_anthrax"
        ),
        Disease(
            "dis_brucellosis", "Brucella suis",
            listOf("sym_abortion", "sym_lameness", "sym_swollen_joints", "sym_lethargy"),
            "dis_brucellosis_desc",
            Severity.HIGH, "dis_brucellosis_prev",
            stages = setOf(PigStage.BREEDING_STOCK)
        ),
        Disease(
            "dis_coccidiosis", "Isospora suis",
            listOf("sym_diarrhea", "sym_weight_loss", "sym_lethargy", "sym_loss_of_appetite"),
            "dis_coccidiosis_desc",
            Severity.MODERATE, "dis_coccidiosis_prev",
            stages = setOf(PigStage.SUCKLING, PigStage.NURSERY)
        ),
        Disease(
            "dis_pneumonia", "Mycoplasma hyopneumoniae",
            listOf("sym_coughing", "sym_difficulty_breathing", "sym_weight_loss", "sym_lethargy"),
            "dis_pneumonia_desc",
            Severity.MODERATE, "dis_pneumonia_prev",
            stages = setOf(PigStage.NURSERY, PigStage.GROWER_FINISHER)
        ),
        Disease(
            "dis_salmonellosis", "Salmonella typhimurium",
            listOf("sym_diarrhea", "sym_high_fever", "sym_skin_discoloration", "sym_vomiting"),
            "dis_salmonellosis_desc",
            Severity.HIGH, "dis_salmonellosis_prev",
            stages = setOf(PigStage.ALL)
        ),
        Disease(
            "dis_ppv", "Parvovirus",
            listOf("sym_abortion", "sym_mummified_piglets", "sym_small_litter"),
            "dis_ppv_desc",
            Severity.MODERATE, "dis_ppv_prev",
            stages = setOf(PigStage.BREEDING_STOCK)
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
            Severity.HIGH, "dis_leptospirosis_prev",
            stages = setOf(PigStage.BREEDING_STOCK, PigStage.GROWER_FINISHER)
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
            Severity.HIGH, "dis_edema_prev",
            stages = setOf(PigStage.NURSERY)
        ),
        Disease(
            "dis_dysentery", "Brachyspira hyodysenteriae",
            listOf("sym_bloody_diarrhea", "sym_weight_loss", "sym_lethargy"),
            "dis_dysentery_desc",
            Severity.HIGH, "dis_dysentery_prev",
            stages = setOf(PigStage.GROWER_FINISHER, PigStage.BREEDING_STOCK)
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
            Severity.MODERATE, "dis_anemia_prev",
            stages = setOf(PigStage.SUCKLING)
        ),
        Disease(
            "dis_ulcers", "",
            listOf("sym_vomiting", "sym_pale_skin", "sym_weight_loss", "sym_blood_in_stool"),
            "dis_ulcers_desc",
            Severity.MODERATE, "dis_ulcers_prev",
            stages = setOf(PigStage.GROWER_FINISHER, PigStage.BREEDING_STOCK)
        ),
        Disease(
            "dis_pss", "",
            listOf("sym_trembling", "sym_muscle_stiffness", "sym_high_fever", "sym_sudden_death"),
            "dis_pss_desc",
            Severity.HIGH, "dis_pss_prev",
            stages = setOf(PigStage.GROWER_FINISHER, PigStage.BREEDING_STOCK)
        ),
        Disease(
            "dis_tge", "Coronavirus",
            listOf("sym_vomiting", "sym_diarrhea", "sym_sudden_death", "sym_dehydration"),
            "dis_tge_desc",
            Severity.CRITICAL, "dis_tge_prev",
            stages = setOf(PigStage.SUCKLING, PigStage.NURSERY, PigStage.GROWER_FINISHER, PigStage.BREEDING_STOCK)
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
            Severity.HIGH, "dis_strep_prev",
            stages = setOf(PigStage.SUCKLING, PigStage.NURSERY)
        ),
        Disease(
            "dis_mycotoxicosis", "",
            listOf("sym_loss_of_appetite", "sym_vomiting", "sym_abortion", "sym_swollen_vulva"),
            "dis_mycotoxicosis_desc",
            Severity.MODERATE, "dis_mycotoxicosis_prev",
            stages = setOf(PigStage.BREEDING_STOCK, PigStage.GROWER_FINISHER)
        ),
        Disease(
            "dis_greasy_pig", "Staphylococcus hyicus",
            listOf("sym_skin_discoloration", "sym_skin_crusts", "sym_lethargy", "sym_weight_loss"),
            "dis_greasy_pig_desc",
            Severity.MODERATE, "dis_greasy_pig_prev",
            stages = setOf(PigStage.SUCKLING, PigStage.NURSERY)
        ),
        Disease(
            "dis_pseudorabies", "Suid herpesvirus 1",
            listOf("sym_seizures", "sym_trembling", "sym_sudden_death", "sym_abortion", "sym_coughing"),
            "dis_pseudorabies_desc",
            Severity.CRITICAL, "dis_pseudorabies_prev",
            stages = setOf(PigStage.ALL),
            isNotifiable = true,
            biosecurityProtocol = "bio_proto_pseudorabies"
        ),
        Disease(
            "dis_pcv2", "Circovirus",
            listOf("sym_weight_loss", "sym_difficulty_breathing", "sym_diarrhea", "sym_jaundice", "sym_pale_skin"),
            "dis_pcv2_desc",
            Severity.HIGH, "dis_pcv2_prev",
            stages = setOf(PigStage.NURSERY, PigStage.GROWER_FINISHER)
        ),
        Disease(
            "dis_ped", "Coronavirus",
            listOf("sym_vomiting", "sym_watery_diarrhea", "sym_dehydration", "sym_sudden_death"),
            "dis_ped_desc",
            Severity.CRITICAL, "dis_ped_prev",
            stages = setOf(PigStage.SUCKLING, PigStage.NURSERY, PigStage.GROWER_FINISHER, PigStage.BREEDING_STOCK)
        ),
        Disease(
            "dis_je", "Flavivirus",
            listOf("sym_abortion", "sym_mummified_piglets", "sym_infertility", "sym_trembling"),
            "dis_je_desc",
            Severity.HIGH, "dis_je_prev",
            stages = setOf(PigStage.BREEDING_STOCK)
        ),
        Disease(
            "dis_rabies", "Lyssavirus",
            listOf("sym_nervousness", "sym_aggression", "sym_excessive_salivation", "sym_paralysis", "sym_sudden_death"),
            "dis_rabies_desc",
            Severity.CRITICAL, "dis_rabies_prev",
            stages = setOf(PigStage.ALL),
            isNotifiable = true,
            biosecurityProtocol = "bio_proto_rabies"
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
            Severity.HIGH, "dis_vs_prev",
            stages = setOf(PigStage.ALL),
            isNotifiable = true,
            biosecurityProtocol = "bio_proto_vesicular_stomatitis"
        ),
        Disease(
            "dis_clostridial", "Clostridium perfringens",
            listOf("sym_bloody_diarrhea", "sym_sudden_death", "sym_bloat"),
            "dis_clostridial_desc",
            Severity.CRITICAL, "dis_clostridial_prev",
            stages = setOf(PigStage.SUCKLING, PigStage.NURSERY)
        ),
        Disease(
            "dis_glassers", "Haemophilus parasuis",
            listOf("sym_high_fever", "sym_coughing", "sym_lameness", "sym_swollen_joints", "sym_trembling"),
            "dis_glassers_desc",
            Severity.HIGH, "dis_glassers_prev",
            stages = setOf(PigStage.NURSERY, PigStage.GROWER_FINISHER)
        ),
        Disease(
            "dis_app", "A. pleuropneumoniae",
            listOf("sym_sudden_death", "sym_high_fever", "sym_nose_bleeds", "sym_difficulty_breathing", "sym_coughing"),
            "dis_app_desc",
            Severity.CRITICAL, "dis_app_prev",
            stages = setOf(PigStage.GROWER_FINISHER, PigStage.BREEDING_STOCK)
        ),
        Disease(
            "dis_ileitis", "Lawsonia intracellularis",
            listOf("sym_diarrhea", "sym_blood_in_stool", "sym_weight_loss", "sym_pale_skin"),
            "dis_ileitis_desc",
            Severity.MODERATE, "dis_ileitis_prev",
            stages = setOf(PigStage.NURSERY, PigStage.GROWER_FINISHER)
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
            Severity.HIGH, "dis_seneca_prev",
            stages = setOf(PigStage.ALL),
            isNotifiable = true,
            biosecurityProtocol = "bio_proto_senecavirus"
        ),
        Disease(
            "dis_deltacoronavirus", "PDCoV",
            listOf("sym_watery_diarrhea", "sym_vomiting", "sym_dehydration"),
            "dis_deltacoronavirus_desc",
            Severity.HIGH, "dis_deltacoronavirus_prev",
            stages = setOf(PigStage.SUCKLING, PigStage.NURSERY)
        ),
        Disease(
            "dis_rotavirus", "Rotavirus",
            listOf("sym_diarrhea", "sym_vomiting", "sym_weight_loss"),
            "dis_rotavirus_desc",
            Severity.MODERATE, "dis_rotavirus_prev",
            stages = setOf(PigStage.SUCKLING, PigStage.NURSERY)
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
            Severity.HIGH, "dis_vesicular_prev",
            stages = setOf(PigStage.ALL),
            isNotifiable = true,
            biosecurityProtocol = "bio_proto_svd"
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
            Severity.MODERATE, "dis_toxoplasmosis_prev",
            stages = setOf(PigStage.BREEDING_STOCK)
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
            Severity.MODERATE, "dis_ear_necrosis_prev",
            stages = setOf(PigStage.NURSERY, PigStage.GROWER_FINISHER)
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
            Severity.MODERATE, "dis_tail_biting_prev",
            stages = setOf(PigStage.NURSERY, PigStage.GROWER_FINISHER)
        ),
        Disease(
            "dis_navel_ill", "",
            listOf("sym_swollen_navel", "sym_high_fever", "sym_lameness"),
            "dis_navel_ill_desc",
            Severity.MODERATE, "dis_navel_ill_prev",
            stages = setOf(PigStage.SUCKLING)
        ),
        Disease(
            "dis_iron_toxicity", "",
            listOf("sym_sudden_death", "sym_difficulty_breathing", "sym_muscle_stiffness"),
            "dis_iron_toxicity_desc",
            Severity.HIGH, "dis_iron_toxicity_prev",
            stages = setOf(PigStage.SUCKLING)
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
            Severity.MODERATE, "dis_parakeratosis_prev",
            stages = setOf(PigStage.NURSERY, PigStage.GROWER_FINISHER)
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
            Severity.HIGH, "dis_mma_prev",
            stages = setOf(PigStage.BREEDING_STOCK)
        ),
        Disease(
            "dis_pwd", "Escherichia coli (F4/F18)",
            listOf("sym_diarrhea", "sym_dehydration", "sym_loss_of_appetite", "sym_weight_loss"),
            "dis_pwd_desc",
            Severity.HIGH, "dis_pwd_prev",
            stages = setOf(PigStage.NURSERY)
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
            Severity.CRITICAL, "dis_phe_prev",
            stages = setOf(PigStage.GROWER_FINISHER, PigStage.BREEDING_STOCK)
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
            Severity.MODERATE, "dis_shoulder_ulcers_prev",
            stages = setOf(PigStage.BREEDING_STOCK)
        ),
        Disease(
            "dis_ear_biting", "",
            listOf("sym_ear_necrosis", "sym_bleeding_orifices"),
            "dis_ear_biting_desc",
            Severity.MODERATE, "dis_ear_biting_prev",
            stages = setOf(PigStage.NURSERY, PigStage.GROWER_FINISHER)
        ),
        Disease(
            "dis_gastric_torsion", "Gastric dilation-volvulus",
            listOf("sym_bloat", "sym_sudden_death", "sym_vomiting").distinct(),
            "dis_gastric_torsion_desc",
            Severity.CRITICAL, "dis_gastric_torsion_prev",
            stages = setOf(PigStage.GROWER_FINISHER, PigStage.BREEDING_STOCK)
        ),
        Disease(
            "dis_c_difficile", "Clostridioides difficile",
            listOf("sym_diarrhea", "sym_dehydration", "sym_lethargy"),
            "dis_c_difficile_desc",
            Severity.MODERATE, "dis_c_difficile_prev",
            stages = setOf(PigStage.SUCKLING)
        ),
        Disease(
            "dis_cold_stress", "",
            listOf("sym_trembling", "sym_lethargy", "sym_sudden_death"),
            "dis_cold_stress_desc",
            Severity.HIGH, "dis_cold_stress_prev",
            stages = setOf(PigStage.SUCKLING, PigStage.NURSERY)
        ),
        Disease(
            "dis_necrotic_enteritis", "Clostridium perfringens Type C",
            listOf("sym_bloody_diarrhea", "sym_sudden_death", "sym_dehydration"),
            "dis_necrotic_enteritis_desc",
            Severity.CRITICAL, "dis_necrotic_enteritis_prev",
            stages = setOf(PigStage.SUCKLING)
        ),
        Disease(
            "dis_navel_bleeding", "",
            listOf("sym_bleeding_orifices", "sym_pale_skin", "sym_sudden_death"),
            "dis_navel_bleeding_desc",
            Severity.HIGH, "dis_navel_bleeding_prev",
            stages = setOf(PigStage.SUCKLING)
        ),
        Disease(
            "dis_spirochetal_colitis", "Brachyspira pilosicoli",
            listOf("sym_diarrhea", "sym_weight_loss", "sym_stunted_growth"),
            "dis_spirochetal_colitis_desc",
            Severity.MODERATE, "dis_spirochetal_colitis_prev",
            stages = setOf(PigStage.GROWER_FINISHER)
        ),
        Disease(
            "dis_salmonella_septicemia", "Salmonella choleraesuis",
            listOf("sym_high_fever", "sym_skin_discoloration", "sym_difficulty_breathing", "sym_sudden_death"),
            "dis_salmonella_septicemia_desc",
            Severity.HIGH, "dis_salmonella_septicemia_prev",
            stages = setOf(PigStage.NURSERY, PigStage.GROWER_FINISHER)
        ),
        Disease(
            "dis_prdc", "Porcine Respiratory Disease Complex",
            listOf("sym_coughing", "sym_difficulty_breathing", "sym_nasal_discharge", "sym_stunted_growth"),
            "dis_prdc_desc",
            Severity.HIGH, "dis_prdc_prev",
            stages = setOf(PigStage.GROWER_FINISHER)
        ),
        Disease(
            "dis_ergotism", "Claviceps purpurea",
            listOf("sym_lameness", "sym_skin_crusts", "sym_abortion", "sym_infertility"),
            "dis_ergotism_desc",
            Severity.HIGH, "dis_ergotism_prev",
            stages = setOf(PigStage.BREEDING_STOCK, PigStage.GROWER_FINISHER)
        ),
        Disease(
            "dis_bvdv_pig", "Pestivirus",
            listOf("sym_abortion", "sym_mummified_piglets", "sym_small_litter"),
            "dis_bvdv_pig_desc",
            Severity.MODERATE, "dis_bvdv_pig_prev",
            stages = setOf(PigStage.BREEDING_STOCK)
        ),
        Disease(
            "dis_endocarditis", "Streptococcal endocarditis",
            listOf("sym_sudden_death", "sym_difficulty_breathing", "sym_lethargy", "sym_lameness"),
            "dis_endocarditis_desc",
            Severity.HIGH, "dis_endocarditis_prev",
            stages = setOf(PigStage.GROWER_FINISHER)
        ),
        Disease(
            "dis_polyserositis", "Mycoplasma hyorhinis",
            listOf("sym_high_fever", "sym_difficulty_breathing", "sym_swollen_joints", "sym_lameness"),
            "dis_polyserositis_desc",
            Severity.MODERATE, "dis_polyserositis_prev",
            stages = setOf(PigStage.NURSERY, PigStage.GROWER_FINISHER)
        ),
        Disease(
            "dis_congenital_tremor", "Atypical Porcine Pestivirus",
            listOf("sym_trembling", "sym_incoordination", "sym_sudden_death"),
            "dis_congenital_tremor_desc",
            Severity.MODERATE, "dis_congenital_tremor_prev",
            stages = setOf(PigStage.SUCKLING)
        )
    )
}
