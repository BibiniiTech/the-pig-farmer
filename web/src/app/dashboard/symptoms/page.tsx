"use client";

import React, { useState, useEffect } from "react";
import Link from "next/navigation";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import PremiumWrapper from "@/components/PremiumWrapper";
import { useTranslations } from "next-intl";

// Severity Enums
type SeverityLevel = "LOW" | "MODERATE" | "HIGH" | "CRITICAL";

interface Disease {
  id: string;
  name: string;
  scientificName: string;
  symptomKeys: string[];
  description: string;
  severity: SeverityLevel;
  prevention: string;
}

export default function SymptomsPage() {
  const t = useTranslations("Symptoms");
  const { user, userProfile, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

  // Symptom translation map
  const SYMPTOMS: Record<string, string> = {
    sym_skin_discoloration: t("list.sym_skin_discoloration"),
    sym_diamond_lesions: t("list.sym_diamond_lesions"),
    sym_itching: t("list.sym_itching"),
    sym_hair_loss: t("list.sym_hair_loss"),
    sym_pale_skin: t("list.sym_pale_skin"),
    sym_jaundice: t("list.sym_jaundice"),
    sym_skin_crusts: t("list.sym_skin_crusts"),
    sym_rough_hair: t("list.sym_rough_hair"),
    sym_sunburn: t("list.sym_sunburn"),
    sym_ear_necrosis: t("list.sym_ear_necrosis"),
    sym_blisters_snout_feet: t("list.sym_blisters_snout_feet"),
    sym_diarrhea: t("list.sym_diarrhea"),
    sym_vomiting: t("list.sym_vomiting"),
    sym_bloody_diarrhea: t("list.sym_bloody_diarrhea"),
    sym_blood_in_stool: t("list.sym_blood_in_stool"),
    sym_rectal_prolapse: t("list.sym_rectal_prolapse"),
    sym_bloat: t("list.sym_bloat"),
    sym_dehydration: t("list.sym_dehydration"),
    sym_loss_of_appetite: t("list.sym_loss_of_appetite"),
    sym_weight_loss: t("list.sym_weight_loss"),
    sym_coughing: t("list.sym_coughing"),
    sym_difficulty_breathing: t("list.sym_difficulty_breathing"),
    sym_nasal_discharge: t("list.sym_nasal_discharge"),
    sym_sneezing: t("list.sym_sneezing"),
    sym_thumping: t("list.sym_thumping"),
    sym_nose_bleeds: t("list.sym_nose_bleeds"),
    sym_frothing: t("list.sym_frothing"),
    sym_excessive_salivation: t("list.sym_excessive_salivation"),
    sym_lethargy: t("list.sym_lethargy"),
    sym_seizures: t("list.sym_seizures"),
    sym_muscle_stiffness: t("list.sym_muscle_stiffness"),
    sym_trembling: t("list.sym_trembling"),
    sym_blindness: t("list.sym_blindness"),
    sym_paralysis: t("list.sym_paralysis"),
    sym_incoordination: t("list.sym_incoordination"),
    sym_circling: t("list.sym_circling"),
    sym_convulsions: t("list.sym_convulsions"),
    sym_nervousness: t("list.sym_nervousness"),
    sym_twitching: t("list.sym_twitching"),
    sym_tail_biting: t("list.sym_tail_biting"),
    sym_abortion: t("list.sym_abortion"),
    sym_infertility: t("list.sym_infertility"),
    sym_mummified_piglets: t("list.sym_mummified_piglets"),
    sym_small_litter: t("list.sym_small_litter"),
    sym_swollen_vulva: t("list.sym_swollen_vulva"),
    sym_enlarged_scrotum: t("list.sym_enlarged_scrotum"),
    sym_swollen_udder: t("list.sym_swollen_udder"),
    sym_high_fever: t("list.sym_high_fever"),
    sym_lameness: t("list.sym_lameness"),
    sym_sudden_death: t("list.sym_sudden_death"),
    sym_swollen_joints: t("list.sym_swollen_joints"),
    sym_swollen_navel: t("list.sym_swollen_navel"),
    sym_leg_weakness: t("list.sym_leg_weakness"),
    sym_swollen_eyelids: t("list.sym_swollen_eyelids"),
    sym_stunted_growth: t("list.sym_stunted_growth"),
    sym_pale_mucous: t("list.sym_pale_mucous"),
    sym_bleeding_orifices: t("list.sym_bleeding_orifices"),
    sym_watery_diarrhea: t("list.sym_watery_diarrhea"),
    sym_abscesses: t("list.sym_abscesses"),
    sym_anemia: t("list.sym_anemia"),
    sym_thirst: t("list.sym_thirst"),
    sym_fractures: t("list.sym_fractures"),
    sym_cracked_hooves: t("list.sym_cracked_hooves"),
    sym_red_urine: t("list.sym_red_urine"),
  };

  // Symptom Groups
  const SYMPTOM_GROUPS = [
    {
      id: "sg_skin_coat",
      name: t("groups.sg_skin_coat"),
      symptoms: [
        "sym_blisters_snout_feet",
        "sym_diamond_lesions",
        "sym_ear_necrosis",
        "sym_hair_loss",
        "sym_itching",
        "sym_jaundice",
        "sym_pale_skin",
        "sym_rough_hair",
        "sym_skin_crusts",
        "sym_skin_discoloration",
        "sym_sunburn",
      ],
    },
    {
      id: "sg_digestive_stool",
      name: t("groups.sg_digestive_stool"),
      symptoms: [
        "sym_bloat",
        "sym_blood_in_stool",
        "sym_bloody_diarrhea",
        "sym_dehydration",
        "sym_diarrhea",
        "sym_loss_of_appetite",
        "sym_rectal_prolapse",
        "sym_vomiting",
        "sym_weight_loss",
      ],
    },
    {
      id: "sg_respiratory",
      name: t("groups.sg_respiratory"),
      symptoms: [
        "sym_coughing",
        "sym_difficulty_breathing",
        "sym_excessive_salivation",
        "sym_frothing",
        "sym_nasal_discharge",
        "sym_nose_bleeds",
        "sym_sneezing",
        "sym_thumping",
      ],
    },
    {
      id: "sg_behavioral_nervous",
      name: t("groups.sg_behavioral_nervous"),
      symptoms: [
        "sym_blindness",
        "sym_circling",
        "sym_convulsions",
        "sym_incoordination",
        "sym_lethargy",
        "sym_muscle_stiffness",
        "sym_nervousness",
        "sym_paralysis",
        "sym_seizures",
        "sym_tail_biting",
        "sym_trembling",
        "sym_twitching",
      ],
    },
    {
      id: "sg_reproduction",
      name: t("groups.sg_reproduction"),
      symptoms: [
        "sym_abortion",
        "sym_enlarged_scrotum",
        "sym_infertility",
        "sym_mummified_piglets",
        "sym_small_litter",
        "sym_swollen_udder",
        "sym_swollen_vulva",
      ],
    },
    {
      id: "sg_general_limbs",
      name: t("groups.sg_general_limbs"),
      symptoms: [
        "sym_abscesses",
        "sym_anemia",
        "sym_bleeding_orifices",
        "sym_cracked_hooves",
        "sym_fractures",
        "sym_high_fever",
        "sym_lameness",
        "sym_leg_weakness",
        "sym_pale_mucous",
        "sym_red_urine",
        "sym_stunted_growth",
        "sym_sudden_death",
        "sym_swollen_eyelids",
        "sym_swollen_joints",
        "sym_swollen_navel",
        "sym_thirst",
        "sym_watery_diarrhea",
      ],
    },
  ];

  // Complete Diseases Database
  const DISEASES: Disease[] = [
    {
      id: "dis_asf",
      name: t("diseases.dis_asf.name"),
      scientificName: t("diseases.dis_asf.scientificName"),
      symptomKeys: ["sym_high_fever", "sym_loss_of_appetite", "sym_sudden_death", "sym_skin_discoloration", "sym_lethargy", "sym_vomiting", "sym_diarrhea"],
      description: t("diseases.dis_asf.description"),
      severity: "CRITICAL",
      prevention: t("diseases.dis_asf.prevention"),
    },
    {
      id: "dis_fmd",
      name: t("diseases.dis_fmd.name"),
      scientificName: t("diseases.dis_fmd.scientificName"),
      symptomKeys: ["sym_high_fever", "sym_lameness", "sym_blisters_snout_feet", "sym_loss_of_appetite", "sym_nasal_discharge"],
      description: t("diseases.dis_fmd.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_fmd.prevention"),
    },
    {
      id: "dis_prrs",
      name: t("diseases.dis_prrs.name"),
      scientificName: t("diseases.dis_prrs.scientificName"),
      symptomKeys: ["sym_coughing", "sym_difficulty_breathing", "sym_abortion", "sym_lethargy", "sym_skin_discoloration"],
      description: t("diseases.dis_prrs.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_prrs.prevention"),
    },
    {
      id: "dis_erysipelas",
      name: t("diseases.dis_erysipelas.name"),
      scientificName: t("diseases.dis_erysipelas.scientificName"),
      symptomKeys: ["sym_high_fever", "sym_diamond_lesions", "sym_lameness", "sym_sudden_death"],
      description: t("diseases.dis_erysipelas.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_erysipelas.prevention"),
    },
    {
      id: "dis_csf",
      name: t("diseases.dis_csf.name"),
      scientificName: t("diseases.dis_csf.scientificName"),
      symptomKeys: ["sym_high_fever", "sym_diarrhea", "sym_skin_discoloration", "sym_coughing", "sym_lethargy"],
      description: t("diseases.dis_csf.description"),
      severity: "CRITICAL",
      prevention: t("diseases.dis_csf.prevention"),
    },
    {
      id: "dis_anthrax",
      name: t("diseases.dis_anthrax.name"),
      scientificName: t("diseases.dis_anthrax.scientificName"),
      symptomKeys: ["sym_sudden_death", "sym_high_fever", "sym_difficulty_breathing", "sym_bleeding_orifices"],
      description: t("diseases.dis_anthrax.description"),
      severity: "CRITICAL",
      prevention: t("diseases.dis_anthrax.prevention"),
    },
    {
      id: "dis_brucellosis",
      name: t("diseases.dis_brucellosis.name"),
      scientificName: t("diseases.dis_brucellosis.scientificName"),
      symptomKeys: ["sym_abortion", "sym_lameness", "sym_swollen_joints", "sym_lethargy"],
      description: t("diseases.dis_brucellosis.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_brucellosis.prevention"),
    },
    {
      id: "dis_coccidiosis",
      name: t("diseases.dis_coccidiosis.name"),
      scientificName: t("diseases.dis_coccidiosis.scientificName"),
      symptomKeys: ["sym_diarrhea", "sym_weight_loss", "sym_lethargy", "sym_loss_of_appetite"],
      description: t("diseases.dis_coccidiosis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_coccidiosis.prevention"),
    },
    {
      id: "dis_pneumonia",
      name: t("diseases.dis_pneumonia.name"),
      scientificName: t("diseases.dis_pneumonia.scientificName"),
      symptomKeys: ["sym_coughing", "sym_difficulty_breathing", "sym_weight_loss", "sym_lethargy"],
      description: t("diseases.dis_pneumonia.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_pneumonia.prevention"),
    },
    {
      id: "dis_salmonellosis",
      name: t("diseases.dis_salmonellosis.name"),
      scientificName: t("diseases.dis_salmonellosis.scientificName"),
      symptomKeys: ["sym_diarrhea", "sym_high_fever", "sym_skin_discoloration", "sym_vomiting"],
      description: t("diseases.dis_salmonellosis.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_salmonellosis.prevention"),
    },
    {
      id: "dis_ppv",
      name: t("diseases.dis_ppv.name"),
      scientificName: t("diseases.dis_ppv.scientificName"),
      symptomKeys: ["sym_abortion", "sym_mummified_piglets", "sym_small_litter"],
      description: t("diseases.dis_ppv.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_ppv.prevention"),
    },
    {
      id: "dis_rhinitis",
      name: t("diseases.dis_rhinitis.name"),
      scientificName: t("diseases.dis_rhinitis.scientificName"),
      symptomKeys: ["sym_nasal_discharge", "sym_coughing", "sym_sneezing", "sym_weight_loss"],
      description: t("diseases.dis_rhinitis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_rhinitis.prevention"),
    },
    {
      id: "dis_leptospirosis",
      name: t("diseases.dis_leptospirosis.name"),
      scientificName: t("diseases.dis_leptospirosis.scientificName"),
      symptomKeys: ["sym_abortion", "sym_high_fever", "sym_loss_of_appetite", "sym_jaundice"],
      description: t("diseases.dis_leptospirosis.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_leptospirosis.prevention"),
    },
    {
      id: "dis_influenza",
      name: t("diseases.dis_influenza.name"),
      scientificName: t("diseases.dis_influenza.scientificName"),
      symptomKeys: ["sym_coughing", "sym_high_fever", "sym_nasal_discharge", "sym_difficulty_breathing"],
      description: t("diseases.dis_influenza.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_influenza.prevention"),
    },
    {
      id: "dis_edema",
      name: t("diseases.dis_edema.name"),
      scientificName: t("diseases.dis_edema.scientificName"),
      symptomKeys: ["sym_swollen_eyelids", "sym_seizures", "sym_sudden_death", "sym_loss_of_appetite"],
      description: t("diseases.dis_edema.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_edema.prevention"),
    },
    {
      id: "dis_dysentery",
      name: t("diseases.dis_dysentery.name"),
      scientificName: t("diseases.dis_dysentery.scientificName"),
      symptomKeys: ["sym_bloody_diarrhea", "sym_weight_loss", "sym_lethargy"],
      description: t("diseases.dis_dysentery.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_dysentery.prevention"),
    },
    {
      id: "dis_mange",
      name: t("diseases.dis_mange.name"),
      scientificName: t("diseases.dis_mange.scientificName"),
      symptomKeys: ["sym_itching", "sym_hair_loss", "sym_skin_crusts"],
      description: t("diseases.dis_mange.description"),
      severity: "LOW",
      prevention: t("diseases.dis_mange.prevention"),
    },
    {
      id: "dis_anemia",
      name: t("diseases.dis_anemia.name"),
      scientificName: t("diseases.dis_anemia.scientificName"),
      symptomKeys: ["sym_pale_skin", "sym_lethargy", "sym_difficulty_breathing"],
      description: t("diseases.dis_anemia.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_anemia.prevention"),
    },
    {
      id: "dis_ulcers",
      name: t("diseases.dis_ulcers.name"),
      scientificName: t("diseases.dis_ulcers.scientificName"),
      symptomKeys: ["sym_vomiting", "sym_pale_skin", "sym_weight_loss", "sym_blood_in_stool"],
      description: t("diseases.dis_ulcers.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_ulcers.prevention"),
    },
    {
      id: "dis_pss",
      name: t("diseases.dis_pss.name"),
      scientificName: t("diseases.dis_pss.scientificName"),
      symptomKeys: ["sym_trembling", "sym_muscle_stiffness", "sym_high_fever", "sym_sudden_death"],
      description: t("diseases.dis_pss.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_pss.prevention"),
    },
    {
      id: "dis_tge",
      name: t("diseases.dis_tge.name"),
      scientificName: t("diseases.dis_tge.scientificName"),
      symptomKeys: ["sym_vomiting", "sym_diarrhea", "sym_sudden_death", "sym_dehydration"],
      description: t("diseases.dis_tge.description"),
      severity: "CRITICAL",
      prevention: t("diseases.dis_tge.prevention"),
    },
    {
      id: "dis_pasteurellosis",
      name: t("diseases.dis_pasteurellosis.name"),
      scientificName: t("diseases.dis_pasteurellosis.scientificName"),
      symptomKeys: ["sym_coughing", "sym_high_fever", "sym_difficulty_breathing", "sym_thumping"],
      description: t("diseases.dis_pasteurellosis.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_pasteurellosis.prevention"),
    },
    {
      id: "dis_strep",
      name: t("diseases.dis_strep.name"),
      scientificName: t("diseases.dis_strep.scientificName"),
      symptomKeys: ["sym_high_fever", "sym_seizures", "sym_lameness", "sym_incoordination"],
      description: t("diseases.dis_strep.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_strep.prevention"),
    },
    {
      id: "dis_mycotoxicosis",
      name: t("diseases.dis_mycotoxicosis.name"),
      scientificName: t("diseases.dis_mycotoxicosis.scientificName"),
      symptomKeys: ["sym_loss_of_appetite", "sym_vomiting", "sym_abortion", "sym_swollen_vulva"],
      description: t("diseases.dis_mycotoxicosis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_mycotoxicosis.prevention"),
    },
    {
      id: "dis_greasy_pig",
      name: t("diseases.dis_greasy_pig.name"),
      scientificName: t("diseases.dis_greasy_pig.scientificName"),
      symptomKeys: ["sym_skin_discoloration", "sym_skin_crusts", "sym_lethargy", "sym_weight_loss"],
      description: t("diseases.dis_greasy_pig.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_greasy_pig.prevention"),
    },
    {
      id: "dis_pseudorabies",
      name: t("diseases.dis_pseudorabies.name"),
      scientificName: t("diseases.dis_pseudorabies.scientificName"),
      symptomKeys: ["sym_seizures", "sym_trembling", "sym_sudden_death", "sym_abortion", "sym_coughing"],
      description: t("diseases.dis_pseudorabies.description"),
      severity: "CRITICAL",
      prevention: t("diseases.dis_pseudorabies.prevention"),
    },
    {
      id: "dis_pcv2",
      name: t("diseases.dis_pcv2.name"),
      scientificName: t("diseases.dis_pcv2.scientificName"),
      symptomKeys: ["sym_weight_loss", "sym_difficulty_breathing", "sym_diarrhea", "sym_jaundice", "sym_pale_skin"],
      description: t("diseases.dis_pcv2.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_pcv2.prevention"),
    },
    {
      id: "dis_ped",
      name: t("diseases.dis_ped.name"),
      scientificName: t("diseases.dis_ped.scientificName"),
      symptomKeys: ["sym_vomiting", "sym_watery_diarrhea", "sym_dehydration", "sym_sudden_death"],
      description: t("diseases.dis_ped.description"),
      severity: "CRITICAL",
      prevention: t("diseases.dis_ped.prevention"),
    },
    {
      id: "dis_je",
      name: t("diseases.dis_je.name"),
      scientificName: t("diseases.dis_je.scientificName"),
      symptomKeys: ["sym_abortion", "sym_mummified_piglets", "sym_infertility", "sym_trembling"],
      description: t("diseases.dis_je.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_je.prevention"),
    },
    {
      id: "dis_rabies",
      name: t("diseases.dis_rabies.name"),
      scientificName: t("diseases.dis_rabies.scientificName"),
      symptomKeys: ["sym_nervousness", "sym_aggression", "sym_excessive_salivation", "sym_paralysis", "sym_sudden_death"],
      description: t("diseases.dis_rabies.description"),
      severity: "CRITICAL",
      prevention: t("diseases.dis_rabies.prevention"),
    },
    {
      id: "dis_pox",
      name: t("diseases.dis_pox.name"),
      scientificName: t("diseases.dis_pox.scientificName"),
      symptomKeys: ["sym_skin_crusts", "sym_lethargy", "sym_loss_of_appetite"],
      description: t("diseases.dis_pox.description"),
      severity: "LOW",
      prevention: t("diseases.dis_pox.prevention"),
    },
    {
      id: "dis_vs",
      name: t("diseases.dis_vs.name"),
      scientificName: t("diseases.dis_vs.scientificName"),
      symptomKeys: ["sym_blisters_snout_feet", "sym_excessive_salivation", "sym_loss_of_appetite"],
      description: t("diseases.dis_vs.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_vs.prevention"),
    },
    {
      id: "dis_clostridial",
      name: t("diseases.dis_clostridial.name"),
      scientificName: t("diseases.dis_clostridial.scientificName"),
      symptomKeys: ["sym_bloody_diarrhea", "sym_sudden_death", "sym_bloat"],
      description: t("diseases.dis_clostridial.description"),
      severity: "CRITICAL",
      prevention: t("diseases.dis_clostridial.prevention"),
    },
    {
      id: "dis_glassers",
      name: t("diseases.dis_glassers.name"),
      scientificName: t("diseases.dis_glassers.scientificName"),
      symptomKeys: ["sym_high_fever", "sym_coughing", "sym_lameness", "sym_swollen_joints", "sym_trembling"],
      description: t("diseases.dis_glassers.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_glassers.prevention"),
    },
    {
      id: "dis_app",
      name: t("diseases.dis_app.name"),
      scientificName: t("diseases.dis_app.scientificName"),
      symptomKeys: ["sym_sudden_death", "sym_high_fever", "sym_nose_bleeds", "sym_difficulty_breathing", "sym_coughing"],
      description: t("diseases.dis_app.description"),
      severity: "CRITICAL",
      prevention: t("diseases.dis_app.prevention"),
    },
    {
      id: "dis_ileitis",
      name: t("diseases.dis_ileitis.name"),
      scientificName: t("diseases.dis_ileitis.scientificName"),
      symptomKeys: ["sym_diarrhea", "sym_blood_in_stool", "sym_weight_loss", "sym_pale_skin"],
      description: t("diseases.dis_ileitis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_ileitis.prevention"),
    },
    {
      id: "dis_tb",
      name: t("diseases.dis_tb.name"),
      scientificName: t("diseases.dis_tb.scientificName"),
      symptomKeys: ["sym_weight_loss", "sym_lethargy", "sym_coughing"],
      description: t("diseases.dis_tb.description"),
      severity: "LOW",
      prevention: t("diseases.dis_tb.prevention"),
    },
    {
      id: "dis_tetanus",
      name: t("diseases.dis_tetanus.name"),
      scientificName: t("diseases.dis_tetanus.scientificName"),
      symptomKeys: ["sym_muscle_stiffness", "sym_seizures", "sym_trembling"],
      description: t("diseases.dis_tetanus.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_tetanus.prevention"),
    },
    {
      id: "dis_eperythrozoonosis",
      name: t("diseases.dis_eperythrozoonosis.name"),
      scientificName: t("diseases.dis_eperythrozoonosis.scientificName"),
      symptomKeys: ["sym_pale_skin", "sym_jaundice", "sym_high_fever", "sym_infertility", "sym_lethargy", "sym_red_urine"],
      description: t("diseases.dis_eperythrozoonosis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_eperythrozoonosis.prevention"),
    },
    {
      id: "dis_ascaris",
      name: t("diseases.dis_ascaris.name"),
      scientificName: t("diseases.dis_ascaris.scientificName"),
      symptomKeys: ["sym_coughing", "sym_weight_loss", "sym_stunted_growth", "sym_rough_hair"],
      description: t("diseases.dis_ascaris.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_ascaris.prevention"),
    },
    {
      id: "dis_whipworms",
      name: t("diseases.dis_whipworms.name"),
      scientificName: t("diseases.dis_whipworms.scientificName"),
      symptomKeys: ["sym_bloody_diarrhea", "sym_weight_loss", "sym_dehydration"],
      description: t("diseases.dis_whipworms.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_whipworms.prevention"),
    },
    {
      id: "dis_lungworms",
      name: t("diseases.dis_lungworms.name"),
      scientificName: t("diseases.dis_lungworms.scientificName"),
      symptomKeys: ["sym_coughing", "sym_thumping", "sym_stunted_growth"],
      description: t("diseases.dis_lungworms.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_lungworms.prevention"),
    },
    {
      id: "dis_kidney_worms",
      name: t("diseases.dis_kidney_worms.name"),
      scientificName: t("diseases.dis_kidney_worms.scientificName"),
      symptomKeys: ["sym_weight_loss", "sym_stunted_growth", "sym_leg_weakness"],
      description: t("diseases.dis_kidney_worms.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_kidney_worms.prevention"),
    },
    {
      id: "dis_lice",
      name: t("diseases.dis_lice.name"),
      scientificName: t("diseases.dis_lice.scientificName"),
      symptomKeys: ["sym_itching", "sym_rough_hair", "sym_pale_skin", "sym_anemia"],
      description: t("diseases.dis_lice.description"),
      severity: "LOW",
      prevention: t("diseases.dis_lice.prevention"),
    },
    {
      id: "dis_ringworm",
      name: t("diseases.dis_ringworm.name"),
      scientificName: t("diseases.dis_ringworm.scientificName"),
      symptomKeys: ["sym_skin_crusts", "sym_itching"],
      description: t("diseases.dis_ringworm.description"),
      severity: "LOW",
      prevention: t("diseases.dis_ringworm.prevention"),
    },
    {
      id: "dis_mulberry_heart",
      name: t("diseases.dis_mulberry_heart.name"),
      scientificName: t("diseases.dis_mulberry_heart.scientificName"),
      symptomKeys: ["sym_sudden_death", "sym_difficulty_breathing", "sym_trembling"],
      description: t("diseases.dis_mulberry_heart.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_mulberry_heart.prevention"),
    },
    {
      id: "dis_vit_a",
      name: t("diseases.dis_vit_a.name"),
      scientificName: t("diseases.dis_vit_a.scientificName"),
      symptomKeys: ["sym_blindness", "sym_incoordination", "sym_infertility", "sym_trembling"],
      description: t("diseases.dis_vit_a.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_vit_a.prevention"),
    },
    {
      id: "dis_rickets",
      name: t("diseases.dis_rickets.name"),
      scientificName: t("diseases.dis_rickets.scientificName"),
      symptomKeys: ["sym_lameness", "sym_swollen_joints", "sym_leg_weakness", "sym_fractures"],
      description: t("diseases.dis_rickets.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_rickets.prevention"),
    },
    {
      id: "dis_salt_poisoning",
      name: t("diseases.dis_salt_poisoning.name"),
      scientificName: t("diseases.dis_salt_poisoning.scientificName"),
      symptomKeys: ["sym_seizures", "sym_blindness", "sym_circling", "sym_convulsions", "sym_thirst"],
      description: t("diseases.dis_salt_poisoning.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_salt_poisoning.prevention"),
    },
    {
      id: "dis_heat_stroke",
      name: t("diseases.dis_heat_stroke.name"),
      scientificName: t("diseases.dis_heat_stroke.scientificName"),
      symptomKeys: ["sym_high_fever", "sym_difficulty_breathing", "sym_lethargy", "sym_sudden_death"],
      description: t("diseases.dis_heat_stroke.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_heat_stroke.prevention"),
    },
    {
      id: "dis_rectal_prolapse",
      name: t("diseases.dis_rectal_prolapse.name"),
      scientificName: t("diseases.dis_rectal_prolapse.scientificName"),
      symptomKeys: ["sym_rectal_prolapse", "sym_blood_in_stool"],
      description: t("diseases.dis_rectal_prolapse.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_rectal_prolapse.prevention"),
    },
    {
      id: "dis_umbilical_hernia",
      name: t("diseases.dis_umbilical_hernia.name"),
      scientificName: t("diseases.dis_umbilical_hernia.scientificName"),
      symptomKeys: ["sym_swollen_navel"],
      description: t("diseases.dis_umbilical_hernia.description"),
      severity: "LOW",
      prevention: t("diseases.dis_umbilical_hernia.prevention"),
    },
    {
      id: "dis_scrotal_hernia",
      name: t("diseases.dis_scrotal_hernia.name"),
      scientificName: t("diseases.dis_scrotal_hernia.scientificName"),
      symptomKeys: ["sym_enlarged_scrotum"],
      description: t("diseases.dis_scrotal_hernia.description"),
      severity: "LOW",
      prevention: t("diseases.dis_scrotal_hernia.prevention"),
    },
    {
      id: "dis_splayleg",
      name: t("diseases.dis_splayleg.name"),
      scientificName: t("diseases.dis_splayleg.scientificName"),
      symptomKeys: ["sym_leg_weakness", "sym_difficulty_breathing", "sym_sudden_death"],
      description: t("diseases.dis_splayleg.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_splayleg.prevention"),
    },
    {
      id: "dis_vulvovaginitis",
      name: t("diseases.dis_vulvovaginitis.name"),
      scientificName: t("diseases.dis_vulvovaginitis.scientificName"),
      symptomKeys: ["sym_swollen_vulva", "sym_abortion", "sym_infertility"],
      description: t("diseases.dis_vulvovaginitis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_vulvovaginitis.prevention"),
    },
    {
      id: "dis_seneca",
      name: t("diseases.dis_seneca.name"),
      scientificName: t("diseases.dis_seneca.scientificName"),
      symptomKeys: ["sym_blisters_snout_feet", "sym_lameness", "sym_sudden_death"],
      description: t("diseases.dis_seneca.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_seneca.prevention"),
    },
    {
      id: "dis_deltacoronavirus",
      name: t("diseases.dis_deltacoronavirus.name"),
      scientificName: t("diseases.dis_deltacoronavirus.scientificName"),
      symptomKeys: ["sym_watery_diarrhea", "sym_vomiting", "sym_dehydration"],
      description: t("diseases.dis_deltacoronavirus.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_deltacoronavirus.prevention"),
    },
    {
      id: "dis_rotavirus",
      name: t("diseases.dis_rotavirus.name"),
      scientificName: t("diseases.dis_rotavirus.scientificName"),
      symptomKeys: ["sym_diarrhea", "sym_vomiting", "sym_weight_loss"],
      description: t("diseases.dis_rotavirus.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_rotavirus.prevention"),
    },
    {
      id: "dis_teschen",
      name: t("diseases.dis_teschen.name"),
      scientificName: t("diseases.dis_teschen.scientificName"),
      symptomKeys: ["sym_paralysis", "sym_incoordination", "sym_seizures", "sym_trembling"],
      description: t("diseases.dis_teschen.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_teschen.prevention"),
    },
    {
      id: "dis_vesicular",
      name: t("diseases.dis_vesicular.name"),
      scientificName: t("diseases.dis_vesicular.scientificName"),
      symptomKeys: ["sym_blisters_snout_feet", "sym_lameness", "sym_high_fever"],
      description: t("diseases.dis_vesicular.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_vesicular.prevention"),
    },
    {
      id: "dis_melioidosis",
      name: t("diseases.dis_melioidosis.name"),
      scientificName: t("diseases.dis_melioidosis.scientificName"),
      symptomKeys: ["sym_high_fever", "sym_coughing", "sym_lameness", "sym_abscesses"],
      description: t("diseases.dis_melioidosis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_melioidosis.prevention"),
    },
    {
      id: "dis_trypanosomiasis",
      name: t("diseases.dis_trypanosomiasis.name"),
      scientificName: t("diseases.dis_trypanosomiasis.scientificName"),
      symptomKeys: ["sym_high_fever", "sym_anemia", "sym_weight_loss", "sym_lethargy", "sym_abortion"],
      description: t("diseases.dis_trypanosomiasis.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_trypanosomiasis.prevention"),
    },
    {
      id: "dis_cysticercosis",
      name: t("diseases.dis_cysticercosis.name"),
      scientificName: t("diseases.dis_cysticercosis.scientificName"),
      symptomKeys: ["sym_muscle_stiffness", "sym_seizures"],
      description: t("diseases.dis_cysticercosis.description"),
      severity: "LOW",
      prevention: t("diseases.dis_cysticercosis.prevention"),
    },
    {
      id: "dis_toxoplasmosis",
      name: t("diseases.dis_toxoplasmosis.name"),
      scientificName: t("diseases.dis_toxoplasmosis.scientificName"),
      symptomKeys: ["sym_abortion", "sym_high_fever", "sym_difficulty_breathing", "sym_lethargy"],
      description: t("diseases.dis_toxoplasmosis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_toxoplasmosis.prevention"),
    },
    {
      id: "dis_listeriosis",
      name: t("diseases.dis_listeriosis.name"),
      scientificName: t("diseases.dis_listeriosis.scientificName"),
      symptomKeys: ["sym_seizures", "sym_circling", "sym_abortion", "sym_high_fever"],
      description: t("diseases.dis_listeriosis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_listeriosis.prevention"),
    },
    {
      id: "dis_ear_necrosis",
      name: t("diseases.dis_ear_necrosis.name"),
      scientificName: t("diseases.dis_ear_necrosis.scientificName"),
      symptomKeys: ["sym_ear_necrosis", "sym_skin_discoloration", "sym_lethargy"],
      description: t("diseases.dis_ear_necrosis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_ear_necrosis.prevention"),
    },
    {
      id: "dis_sunburn",
      name: t("diseases.dis_sunburn.name"),
      scientificName: t("diseases.dis_sunburn.scientificName"),
      symptomKeys: ["sym_sunburn", "sym_skin_discoloration", "sym_lethargy"],
      description: t("diseases.dis_sunburn.description"),
      severity: "LOW",
      prevention: t("diseases.dis_sunburn.prevention"),
    },
    {
      id: "dis_tail_biting",
      name: t("diseases.dis_tail_biting.name"),
      scientificName: t("diseases.dis_tail_biting.scientificName"),
      symptomKeys: ["sym_tail_biting", "sym_blood_in_stool", "sym_lameness"],
      description: t("diseases.dis_tail_biting.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_tail_biting.prevention"),
    },
    {
      id: "dis_navel_ill",
      name: t("diseases.dis_navel_ill.name"),
      scientificName: t("diseases.dis_navel_ill.scientificName"),
      symptomKeys: ["sym_swollen_navel", "sym_high_fever", "sym_lameness"],
      description: t("diseases.dis_navel_ill.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_navel_ill.prevention"),
    },
    {
      id: "dis_iron_toxicity",
      name: t("diseases.dis_iron_toxicity.name"),
      scientificName: t("diseases.dis_iron_toxicity.scientificName"),
      symptomKeys: ["sym_sudden_death", "sym_difficulty_breathing", "sym_muscle_stiffness"],
      description: t("diseases.dis_iron_toxicity.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_iron_toxicity.prevention"),
    },
    {
      id: "dis_aflatoxicosis",
      name: t("diseases.dis_aflatoxicosis.name"),
      scientificName: t("diseases.dis_aflatoxicosis.scientificName"),
      symptomKeys: ["sym_loss_of_appetite", "sym_weight_loss", "sym_jaundice", "sym_lethargy", "sym_sudden_death"],
      description: t("diseases.dis_aflatoxicosis.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_aflatoxicosis.prevention"),
    },
    {
      id: "dis_parakeratosis",
      name: t("diseases.dis_parakeratosis.name"),
      scientificName: t("diseases.dis_parakeratosis.scientificName"),
      symptomKeys: ["sym_skin_crusts", "sym_rough_hair", "sym_stunted_growth"],
      description: t("diseases.dis_parakeratosis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_parakeratosis.prevention"),
    },
    {
      id: "dis_biotin_deficiency",
      name: t("diseases.dis_biotin_deficiency.name"),
      scientificName: t("diseases.dis_biotin_deficiency.scientificName"),
      symptomKeys: ["sym_lameness", "sym_hair_loss", "sym_leg_weakness", "sym_cracked_hooves"],
      description: t("diseases.dis_biotin_deficiency.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_biotin_deficiency.prevention"),
    },
    {
      id: "dis_gossypol_toxicity",
      name: t("diseases.dis_gossypol_toxicity.name"),
      scientificName: t("diseases.dis_gossypol_toxicity.scientificName"),
      symptomKeys: ["sym_difficulty_breathing", "sym_thumping", "sym_loss_of_appetite", "sym_sudden_death"],
      description: t("diseases.dis_gossypol_toxicity.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_gossypol_toxicity.prevention"),
    },
    {
      id: "dis_cassava_poisoning",
      name: t("diseases.dis_cassava_poisoning.name"),
      scientificName: t("diseases.dis_cassava_poisoning.scientificName"),
      symptomKeys: ["sym_difficulty_breathing", "sym_incoordination", "sym_sudden_death", "sym_lethargy"],
      description: t("diseases.dis_cassava_poisoning.description"),
      severity: "CRITICAL",
      prevention: t("diseases.dis_cassava_poisoning.prevention"),
    },
    {
      id: "dis_sweet_potato_toxicity",
      name: t("diseases.dis_sweet_potato_toxicity.name"),
      scientificName: t("diseases.dis_sweet_potato_toxicity.scientificName"),
      symptomKeys: ["sym_difficulty_breathing", "sym_thumping", "sym_frothing"],
      description: t("diseases.dis_sweet_potato_toxicity.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_sweet_potato_toxicity.prevention"),
    },
    {
      id: "dis_mma",
      name: t("diseases.dis_mma.name"),
      scientificName: t("diseases.dis_mma.scientificName"),
      symptomKeys: ["sym_high_fever", "sym_lethargy", "sym_loss_of_appetite", "sym_swollen_udder"],
      description: t("diseases.dis_mma.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_mma.prevention"),
    },
    {
      id: "dis_pwd",
      name: t("diseases.dis_pwd.name"),
      scientificName: t("diseases.dis_pwd.scientificName"),
      symptomKeys: ["sym_diarrhea", "sym_dehydration", "sym_loss_of_appetite", "sym_weight_loss"],
      description: t("diseases.dis_pwd.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_pwd.prevention"),
    },
    {
      id: "dis_trichinellosis",
      name: t("diseases.dis_trichinellosis.name"),
      scientificName: t("diseases.dis_trichinellosis.scientificName"),
      symptomKeys: ["sym_muscle_stiffness", "sym_lethargy", "sym_incoordination"],
      description: t("diseases.dis_trichinellosis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_trichinellosis.prevention"),
    },
    {
      id: "dis_hydatid_disease",
      name: t("diseases.dis_hydatid_disease.name"),
      scientificName: t("diseases.dis_hydatid_disease.scientificName"),
      symptomKeys: ["sym_weight_loss", "sym_stunted_growth", "sym_lethargy"],
      description: t("diseases.dis_hydatid_disease.description"),
      severity: "LOW",
      prevention: t("diseases.dis_hydatid_disease.prevention"),
    },
    {
      id: "dis_phe",
      name: t("diseases.dis_phe.name"),
      scientificName: t("diseases.dis_phe.scientificName"),
      symptomKeys: ["sym_bloody_diarrhea", "sym_pale_skin", "sym_sudden_death"],
      description: t("diseases.dis_phe.description"),
      severity: "CRITICAL",
      prevention: t("diseases.dis_phe.prevention"),
    },
    {
      id: "dis_mycoplasma_arthritis",
      name: t("diseases.dis_mycoplasma_arthritis.name"),
      scientificName: t("diseases.dis_mycoplasma_arthritis.scientificName"),
      symptomKeys: ["sym_lameness", "sym_swollen_joints", "sym_leg_weakness"],
      description: t("diseases.dis_mycoplasma_arthritis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_mycoplasma_arthritis.prevention"),
    },
    {
      id: "dis_foot_rot",
      name: t("diseases.dis_foot_rot.name"),
      scientificName: t("diseases.dis_foot_rot.scientificName"),
      symptomKeys: ["sym_lameness", "sym_lethargy", "sym_loss_of_appetite", "sym_cracked_hooves"],
      description: t("diseases.dis_foot_rot.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_foot_rot.prevention"),
    },
    {
      id: "dis_babesiosis",
      name: t("diseases.dis_babesiosis.name"),
      scientificName: t("diseases.dis_babesiosis.scientificName"),
      symptomKeys: ["sym_high_fever", "sym_jaundice", "sym_pale_skin", "sym_abortion", "sym_lethargy", "sym_red_urine"],
      description: t("diseases.dis_babesiosis.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_babesiosis.prevention"),
    },
    {
      id: "dis_anaplasmosis",
      name: t("diseases.dis_anaplasmosis.name"),
      scientificName: t("diseases.dis_anaplasmosis.scientificName"),
      symptomKeys: ["sym_high_fever", "sym_pale_skin", "sym_jaundice", "sym_lethargy"],
      description: t("diseases.dis_anaplasmosis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_anaplasmosis.prevention"),
    },
    {
      id: "dis_sow_hysteria",
      name: t("diseases.dis_sow_hysteria.name"),
      scientificName: t("diseases.dis_sow_hysteria.scientificName"),
      symptomKeys: ["sym_nervousness", "sym_lethargy"],
      description: t("diseases.dis_sow_hysteria.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_sow_hysteria.prevention"),
    },
    {
      id: "dis_thorny_headed_worm",
      name: t("diseases.dis_thorny_headed_worm.name"),
      scientificName: t("diseases.dis_thorny_headed_worm.scientificName"),
      symptomKeys: ["sym_diarrhea", "sym_weight_loss", "sym_stunted_growth", "sym_sudden_death"],
      description: t("diseases.dis_thorny_headed_worm.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_thorny_headed_worm.prevention"),
    },
    {
      id: "dis_strongyloidiasis",
      name: t("diseases.dis_strongyloidiasis.name"),
      scientificName: t("diseases.dis_strongyloidiasis.scientificName"),
      symptomKeys: ["sym_diarrhea", "sym_dehydration", "sym_stunted_growth", "sym_sudden_death"],
      description: t("diseases.dis_strongyloidiasis.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_strongyloidiasis.prevention"),
    },
    {
      id: "dis_hyostrongylosis",
      name: t("diseases.dis_hyostrongylosis.name"),
      scientificName: t("diseases.dis_hyostrongylosis.scientificName"),
      symptomKeys: ["sym_weight_loss", "sym_pale_skin", "sym_rough_hair", "sym_loss_of_appetite"],
      description: t("diseases.dis_hyostrongylosis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_hyostrongylosis.prevention"),
    },
    {
      id: "dis_oesophagostomiasis",
      name: t("diseases.dis_oesophagostomiasis.name"),
      scientificName: t("diseases.dis_oesophagostomiasis.scientificName"),
      symptomKeys: ["sym_weight_loss", "sym_diarrhea", "sym_rough_hair"],
      description: t("diseases.dis_oesophagostomiasis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_oesophagostomiasis.prevention"),
    },
    {
      id: "dis_fascioliasis",
      name: t("diseases.dis_fascioliasis.name"),
      scientificName: t("diseases.dis_fascioliasis.scientificName"),
      symptomKeys: ["sym_weight_loss", "sym_lethargy", "sym_stunted_growth", "sym_jaundice"],
      description: t("diseases.dis_fascioliasis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_fascioliasis.prevention"),
    },
    {
      id: "dis_emcv",
      name: t("diseases.dis_emcv.name"),
      scientificName: t("diseases.dis_emcv.scientificName"),
      symptomKeys: ["sym_sudden_death", "sym_difficulty_breathing", "sym_abortion", "sym_trembling"],
      description: t("diseases.dis_emcv.description"),
      severity: "CRITICAL",
      prevention: t("diseases.dis_emcv.prevention"),
    },
    {
      id: "dis_demodectic_mange",
      name: t("diseases.dis_demodectic_mange.name"),
      scientificName: t("diseases.dis_demodectic_mange.scientificName"),
      symptomKeys: ["sym_skin_crusts", "sym_itching", "sym_hair_loss"],
      description: t("diseases.dis_demodectic_mange.description"),
      severity: "LOW",
      prevention: t("diseases.dis_demodectic_mange.prevention"),
    },
    {
      id: "dis_pityriasis_rosea",
      name: t("diseases.dis_pityriasis_rosea.name"),
      scientificName: t("diseases.dis_pityriasis_rosea.scientificName"),
      symptomKeys: ["sym_skin_crusts", "sym_rough_hair"],
      description: t("diseases.dis_pityriasis_rosea.description"),
      severity: "LOW",
      prevention: t("diseases.dis_pityriasis_rosea.prevention"),
    },
    {
      id: "dis_photosensitization",
      name: t("diseases.dis_photosensitization.name"),
      scientificName: t("diseases.dis_photosensitization.scientificName"),
      symptomKeys: ["sym_skin_discoloration", "sym_sunburn", "sym_skin_crusts"],
      description: t("diseases.dis_photosensitization.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_photosensitization.prevention"),
    },
    {
      id: "dis_shoulder_ulcers",
      name: t("diseases.dis_shoulder_ulcers.name"),
      scientificName: t("diseases.dis_shoulder_ulcers.scientificName"),
      symptomKeys: ["sym_lameness", "sym_pale_skin"],
      description: t("diseases.dis_shoulder_ulcers.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_shoulder_ulcers.prevention"),
    },
    {
      id: "dis_ear_biting",
      name: t("diseases.dis_ear_biting.name"),
      scientificName: t("diseases.dis_ear_biting.scientificName"),
      symptomKeys: ["sym_ear_necrosis", "sym_bleeding_orifices"],
      description: t("diseases.dis_ear_biting.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_ear_biting.prevention"),
    },
    {
      id: "dis_gastric_torsion",
      name: t("diseases.dis_gastric_torsion.name"),
      scientificName: t("diseases.dis_gastric_torsion.scientificName"),
      symptomKeys: ["sym_bloat", "sym_sudden_death", "sym_vomiting"],
      description: t("diseases.dis_gastric_torsion.description"),
      severity: "CRITICAL",
      prevention: t("diseases.dis_gastric_torsion.prevention"),
    },
    {
      id: "dis_c_difficile",
      name: t("diseases.dis_c_difficile.name"),
      scientificName: t("diseases.dis_c_difficile.scientificName"),
      symptomKeys: ["sym_diarrhea", "sym_dehydration", "sym_lethargy"],
      description: t("diseases.dis_c_difficile.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_c_difficile.prevention"),
    },
    {
      id: "dis_cold_stress",
      name: t("diseases.dis_cold_stress.name"),
      scientificName: t("diseases.dis_cold_stress.scientificName"),
      symptomKeys: ["sym_trembling", "sym_lethargy", "sym_sudden_death"],
      description: t("diseases.dis_cold_stress.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_cold_stress.prevention"),
    },
    {
      id: "dis_necrotic_enteritis",
      name: t("diseases.dis_necrotic_enteritis.name"),
      scientificName: t("diseases.dis_necrotic_enteritis.scientificName"),
      symptomKeys: ["sym_bloody_diarrhea", "sym_sudden_death", "sym_dehydration"],
      description: t("diseases.dis_necrotic_enteritis.description"),
      severity: "CRITICAL",
      prevention: t("diseases.dis_necrotic_enteritis.prevention"),
    },
    {
      id: "dis_navel_bleeding",
      name: t("diseases.dis_navel_bleeding.name"),
      scientificName: t("diseases.dis_navel_bleeding.scientificName"),
      symptomKeys: ["sym_bleeding_orifices", "sym_pale_skin", "sym_sudden_death"],
      description: t("diseases.dis_navel_bleeding.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_navel_bleeding.prevention"),
    },
    {
      id: "dis_spirochetal_colitis",
      name: t("diseases.dis_spirochetal_colitis.name"),
      scientificName: t("diseases.dis_spirochetal_colitis.scientificName"),
      symptomKeys: ["sym_diarrhea", "sym_weight_loss", "sym_stunted_growth"],
      description: t("diseases.dis_spirochetal_colitis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_spirochetal_colitis.prevention"),
    },
    {
      id: "dis_salmonella_septicemia",
      name: t("diseases.dis_salmonella_septicemia.name"),
      scientificName: t("diseases.dis_salmonella_septicemia.scientificName"),
      symptomKeys: ["sym_high_fever", "sym_skin_discoloration", "sym_difficulty_breathing", "sym_sudden_death"],
      description: t("diseases.dis_salmonella_septicemia.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_salmonella_septicemia.prevention"),
    },
    {
      id: "dis_prdc",
      name: t("diseases.dis_prdc.name"),
      scientificName: t("diseases.dis_prdc.scientificName"),
      symptomKeys: ["sym_coughing", "sym_difficulty_breathing", "sym_nasal_discharge", "sym_stunted_growth"],
      description: t("diseases.dis_prdc.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_prdc.prevention"),
    },
    {
      id: "dis_ergotism",
      name: t("diseases.dis_ergotism.name"),
      scientificName: t("diseases.dis_ergotism.scientificName"),
      symptomKeys: ["sym_lameness", "sym_skin_crusts", "sym_abortion", "sym_infertility"],
      description: t("diseases.dis_ergotism.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_ergotism.prevention"),
    },
    {
      id: "dis_bvdv_pig",
      name: t("diseases.dis_bvdv_pig.name"),
      scientificName: t("diseases.dis_bvdv_pig.scientificName"),
      symptomKeys: ["sym_abortion", "sym_mummified_piglets", "sym_small_litter"],
      description: t("diseases.dis_bvdv_pig.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_bvdv_pig.prevention"),
    },
    {
      id: "dis_endocarditis",
      name: t("diseases.dis_endocarditis.name"),
      scientificName: t("diseases.dis_endocarditis.scientificName"),
      symptomKeys: ["sym_sudden_death", "sym_difficulty_breathing", "sym_lethargy", "sym_lameness"],
      description: t("diseases.dis_endocarditis.description"),
      severity: "HIGH",
      prevention: t("diseases.dis_endocarditis.prevention"),
    },
    {
      id: "dis_polyserositis",
      name: t("diseases.dis_polyserositis.name"),
      scientificName: t("diseases.dis_polyserositis.scientificName"),
      symptomKeys: ["sym_high_fever", "sym_difficulty_breathing", "sym_swollen_joints", "sym_lameness"],
      description: t("diseases.dis_polyserositis.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_polyserositis.prevention"),
    },
    {
      id: "dis_congenital_tremor",
      name: t("diseases.dis_congenital_tremor.name"),
      scientificName: t("diseases.dis_congenital_tremor.scientificName"),
      symptomKeys: ["sym_trembling", "sym_incoordination", "sym_sudden_death"],
      description: t("diseases.dis_congenital_tremor.description"),
      severity: "MODERATE",
      prevention: t("diseases.dis_congenital_tremor.prevention"),
    },
  ];

  const [selectedSymptoms, setSelectedSymptoms] = useState<Set<string>>(new Set());
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set());
  const [diagnosisResults, setDiagnosisResults] = useState<{ disease: Disease; matches: number; matchPercent: number }[]>([]);
  const [hasAnalyzed, setHasAnalyzed] = useState(false);
  const [expandedDisease, setExpandedDisease] = useState<string | null>(null);

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  const toggleGroup = (groupId: string) => {
    setExpandedGroups((prev) => {
      const next = new Set(prev);
      if (next.has(groupId)) {
        next.delete(groupId);
      } else {
        next.add(groupId);
      }
      return next;
    });
  };

  const toggleSymptom = (key: string) => {
    setSelectedSymptoms((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  };

  const handleAnalyze = () => {
    if (selectedSymptoms.size === 0) return;

    const results = DISEASES.map((disease) => {
      const matches = disease.symptomKeys.filter((sKey) => selectedSymptoms.has(sKey)).length;
      const matchPercent = Math.round((matches / disease.symptomKeys.length) * 100);
      return { disease, matches, matchPercent };
    })
      .filter((r) => r.matches > 0)
      .sort((a, b) => b.matches - a.matches || b.matchPercent - a.matchPercent);

    setDiagnosisResults(results);
    setHasAnalyzed(true);
  };

  const handleReset = () => {
    setSelectedSymptoms(new Set());
    setDiagnosisResults([]);
    setHasAnalyzed(false);
  };

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center bg-white text-zinc-900">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent"></div>
      </div>
    );
  }

  const isPremium = userProfile?.isPremium || false;

  return (
    <div className="relative min-h-screen bg-white text-zinc-900 flex flex-col font-sans overflow-hidden">
      {/* Watermark Logo Background */}
      {!isMobile && (
        <div className="fixed inset-0 z-0 flex items-center justify-center opacity-[0.15] pointer-events-none select-none">
          <img
            src="/app_logo.png"
            alt="Watermark Background Logo"
            className="w-full max-w-[1100px] max-h-[85vh] object-contain"
          />
        </div>
      )}

      <div className="relative z-10 flex flex-col min-h-screen">
        {/* Header */}
        {!isMobile && <DesktopHeader />}

        {/* Content */}
        <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
          <PremiumWrapper>
            <div className="bg-white/60 backdrop-blur-sm border border-zinc-200 rounded-2xl p-6 shadow-sm">
              <h2 className="text-xl font-bold text-zinc-900">{t("title")}</h2>
              <p className="text-sm text-zinc-500 mt-1">{t("description")}</p>
            </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
            {/* Symptoms Selection Side */}
            <div className="lg:col-span-2 bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl overflow-hidden shadow-sm flex flex-col">
              {/* Vertical Scrollable Sections */}
              <div className="p-6 space-y-4 max-h-[70vh] overflow-y-auto no-scrollbar">
                {SYMPTOM_GROUPS.map((group) => {
                  const isExpanded = expandedGroups.has(group.id);
                  return (
                    <div key={group.id} className="border border-zinc-100 rounded-xl overflow-hidden">
                      <button
                        onClick={() => toggleGroup(group.id)}
                        className="w-full flex items-center justify-between p-4 bg-zinc-50/50 hover:bg-zinc-100 transition-colors"
                      >
                        <span className="text-sm font-bold text-emerald-800 uppercase tracking-tight">{group.name}</span>
                        <span className="text-zinc-400 font-bold">{isExpanded ? "−" : "+"}</span>
                      </button>

                      {isExpanded && (
                        <div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-3 bg-white">
                          {group.symptoms.map((sKey) => {
                            const isChecked = selectedSymptoms.has(sKey);
                            return (
                              <label
                                key={sKey}
                                onClick={(e) => {
                                  e.preventDefault();
                                  toggleSymptom(sKey);
                                }}
                                className={`flex items-start gap-3 p-3 rounded-xl border cursor-pointer select-none transition duration-300 ${
                                  isChecked
                                    ? "border-emerald-350 bg-emerald-50/40 text-emerald-900"
                                    : "border-zinc-100 bg-white hover:bg-zinc-50/50 text-zinc-700"
                                }`}
                              >
                                <input
                                  type="checkbox"
                                  checked={isChecked}
                                  readOnly
                                  className="mt-1 h-3.5 w-3.5 rounded text-emerald-600 border-zinc-300 focus:ring-emerald-500 pointer-events-none"
                                />
                                <div className="text-sm font-semibold leading-relaxed">
                                  {SYMPTOMS[sKey] || sKey}
                                </div>
                              </label>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>

              {/* Action Buttons */}
              <div className="border-t border-zinc-200 p-4 bg-zinc-50/30 flex flex-wrap gap-4 justify-between items-center">
                <div className="text-xs text-zinc-500 font-medium">
                  {t("symptomsSelected", { count: selectedSymptoms.size })}
                </div>
                <div className="flex gap-3">
                  <button
                    onClick={handleReset}
                    className="px-4 py-2 border border-zinc-200 rounded-xl bg-white text-xs font-bold text-zinc-650 hover:bg-zinc-55 transition"
                  >
                    {t("resetList")}
                  </button>
                  <button
                    onClick={handleAnalyze}
                    disabled={selectedSymptoms.size === 0}
                    className="px-6 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 disabled:bg-zinc-200 disabled:text-zinc-450 disabled:cursor-not-allowed text-xs font-bold text-white shadow transition-all flex items-center gap-2"
                  >
                    {t("analyzeSymptoms")}
                  </button>
                </div>
              </div>
            </div>

            {/* Diagnosis results sidebar */}
            <div className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-6">
              <h3 className="text-base font-bold text-zinc-900">{t("diagnosisDashboard")}</h3>

              {!hasAnalyzed ? (
                <div className="py-12 flex flex-col items-center justify-center text-center space-y-3">
                  <span className="text-4xl">🔬</span>
                  <p className="text-xs text-zinc-400 font-semibold max-w-[200px]">
                    {t("analysisPrompt")}
                  </p>
                </div>
              ) : !isPremium ? (
                /* Free Paywall Block */
                <div className="border border-emerald-250 bg-emerald-50/50 rounded-xl p-5 text-center space-y-4">
                  <span className="text-3xl block">🔒</span>
                  <h4 className="text-sm font-bold text-emerald-900">{t("premiumDiagnostics")}</h4>
                  <p className="text-xs text-zinc-600 leading-relaxed">
                    {t("premiumDesc")}
                  </p>
                  <Link
                    href="/dashboard/billing"
                    className="block w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow transition"
                  >
                    {t("upgradeToPremium")}
                  </Link>
                </div>
              ) : diagnosisResults.length === 0 ? (
                /* No matches found */
                <div className="py-8 text-center space-y-2">
                  <p className="text-xs text-zinc-600 font-medium">
                    {t("noMatches")}
                  </p>
                  <p className="text-[11px] text-zinc-400 leading-relaxed">
                    {t("noMatchesDesc")}
                  </p>
                </div>
              ) : (
                /* Results list */
                <div className="space-y-4 max-h-[55vh] overflow-y-auto pr-1 no-scrollbar">
                  <div className="text-xs font-bold text-zinc-500 uppercase tracking-wider">
                    {t("potentialMatches", { count: diagnosisResults.length })}
                  </div>
                  {diagnosisResults.map(({ disease, matches, matchPercent }) => {
                    let severityColor = "bg-zinc-100 text-zinc-800 border-zinc-200";
                    if (disease.severity === "MODERATE") severityColor = "bg-amber-50 text-amber-800 border-amber-200/50";
                    else if (disease.severity === "HIGH") severityColor = "bg-orange-50 text-orange-800 border-orange-200/50";
                    else if (disease.severity === "CRITICAL") severityColor = "bg-rose-50 text-rose-800 border-rose-200/50";

                    const isExpanded = expandedDisease === disease.id;
                    const unselectedSymptoms = disease.symptomKeys.filter(sKey => !selectedSymptoms.has(sKey));

                    return (
                      <div
                        key={disease.id}
                        onClick={() => setExpandedDisease(isExpanded ? null : disease.id)}
                        className="p-4 rounded-xl border border-zinc-200 bg-white/90 shadow-sm space-y-3 cursor-pointer hover:border-emerald-200 transition-all"
                      >
                        <div className="flex justify-between items-start gap-2">
                          <div>
                            <h4 className="text-xs font-extrabold text-zinc-900">{disease.name}</h4>
                            {disease.scientificName && (
                              <p className="text-[10px] text-zinc-500 italic mt-0.5">{disease.scientificName}</p>
                            )}
                          </div>
                          <span className={`text-[9px] font-extrabold px-2 py-0.5 rounded-full border ${severityColor}`}>
                            {t(`severity.${disease.severity as SeverityLevel}`)}
                          </span>
                        </div>

                        {/* Match Indicator */}
                        <div className="space-y-1">
                          <div className="text-[10px] font-bold text-emerald-700">
                            {t("matchAccuracy", { count: matches })}
                          </div>
                          <div className="h-1.5 w-full bg-zinc-100 rounded-full overflow-hidden">
                            <div
                              className="h-full bg-emerald-500 rounded-full transition-all duration-500"
                              style={{ width: `${matchPercent}%` }}
                            />
                          </div>
                        </div>

                        {isExpanded && (
                          <div className="space-y-3 pt-2 border-t border-zinc-100 animate-in fade-in slide-in-from-top-1 duration-300">
                            <div className="text-[11px] text-zinc-600 leading-relaxed">
                              <strong className="text-zinc-700 block mb-0.5">{t("descriptionLabel")}</strong>
                              {disease.description}
                            </div>

                            {unselectedSymptoms.length > 0 && (
                              <div className="text-[11px] text-zinc-600 leading-relaxed">
                                <strong className="text-zinc-700 block mb-0.5">{t("otherSymptoms")}</strong>
                                <ul className="list-disc list-inside pl-1 space-y-0.5">
                                  {unselectedSymptoms.map(sKey => (
                                    <li key={sKey}>{SYMPTOMS[sKey] || sKey}</li>
                                  ))}
                                </ul>
                              </div>
                            )}

                            {disease.prevention && (
                              <div className="text-[11px] text-zinc-600 leading-relaxed bg-emerald-50/30 p-2.5 rounded-lg border border-emerald-100/50">
                                <strong className="text-emerald-900 block mb-0.5">{t("preventionLabel")}</strong>
                                {disease.prevention}
                              </div>
                            )}
                          </div>
                        )}

                        {!isExpanded && (
                          <div className="text-[10px] text-zinc-400 font-bold text-center pt-1">
                            {t("viewDetails")}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          {/* Disclaimer Banner */}
          <div className="bg-zinc-50/80 backdrop-blur-md border border-zinc-200 rounded-2xl p-4 text-[11px] text-zinc-500 leading-relaxed shadow-sm">
            {t("disclaimer")}
          </div>
          </PremiumWrapper>
        </main>
      </div>
    </div>
  );
}
