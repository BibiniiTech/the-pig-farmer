"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { useDevice } from "@/context/DeviceContext";
import NavbarDropdown from "@/components/NavbarDropdown";
import UserProfileDropdown from "@/components/UserProfileDropdown";
import DesktopHeader from "@/components/layouts/DesktopHeader";
import PremiumWrapper from "@/components/PremiumWrapper";

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

// Symptom translation map
const SYMPTOMS: Record<string, string> = {
  // Skin & Coat
  sym_skin_discoloration: "Skin Discoloration (Red/Purple)",
  sym_diamond_lesions: "Diamond-shaped Skin Lesions",
  sym_itching: "Itching",
  sym_hair_loss: "Hair Loss",
  sym_pale_skin: "Pale Skin",
  sym_jaundice: "Jaundice",
  sym_skin_crusts: "Skin Crusts",
  sym_rough_hair: "Rough Hair Coat",
  sym_sunburn: "Sunburn",
  sym_ear_necrosis: "Ear Necrosis",
  sym_blisters_snout_feet: "Blisters on Snout/Feet",
  // Digestive & Stool
  sym_diarrhea: "Diarrhea",
  sym_vomiting: "Vomiting",
  sym_bloody_diarrhea: "Bloody Diarrhea",
  sym_blood_in_stool: "Blood in Stool",
  sym_rectal_prolapse: "Rectal Prolapse",
  sym_bloat: "Bloat",
  sym_dehydration: "Dehydration",
  sym_loss_of_appetite: "Loss of Appetite",
  sym_weight_loss: "Weight Loss",
  // Respiratory
  sym_coughing: "Coughing",
  sym_difficulty_breathing: "Difficulty Breathing",
  sym_nasal_discharge: "Nasal Discharge",
  sym_sneezing: "Sneezing",
  sym_thumping: "Thumping (Abdominal Breathing)",
  sym_nose_bleeds: "Nose Bleeds",
  sym_frothing: "Frothing at Mouth",
  sym_excessive_salivation: "Excessive Salivation",
  // Behavioral & Nervous
  sym_lethargy: "Lethargy",
  sym_seizures: "Seizures",
  sym_muscle_stiffness: "Muscle Stiffness",
  sym_trembling: "Trembling",
  sym_blindness: "Blindness",
  sym_paralysis: "Paralysis",
  sym_incoordination: "Incoordination",
  sym_circling: "Circling",
  sym_convulsions: "Convulsions",
  sym_nervousness: "Nervousness",
  sym_twitching: "Twitching",
  sym_tail_biting: "Tail Biting",
  // Reproduction
  sym_abortion: "Abortion",
  sym_infertility: "Infertility",
  sym_mummified_piglets: "Mummified Piglets",
  sym_small_litter: "Small Litter Size",
  sym_swollen_vulva: "Swollen Vulva",
  sym_enlarged_scrotum: "Enlarged Scrotum",
  sym_swollen_udder: "Swollen Udder or Mastitis",
  // General & Limbs
  sym_high_fever: "High Fever",
  sym_lameness: "Lameness",
  sym_sudden_death: "Sudden Death",
  sym_swollen_joints: "Swollen Joints",
  sym_swollen_navel: "Swelling of Navel",
  sym_leg_weakness: "Leg Weakness",
  sym_swollen_eyelids: "Swollen Eyelids",
  sym_stunted_growth: "Stunted Growth",
  sym_pale_mucous: "Pale Mucous Membranes",
  sym_bleeding_orifices: "Bleeding from Orifices",
  sym_watery_diarrhea: "Watery Diarrhea",
  sym_abscesses: "Abscesses",
  sym_anemia: "Anemia",
  sym_thirst: "Thirst",
  sym_fractures: "Fractures",
  sym_cracked_hooves: "Cracked Hooves",
  sym_red_urine: "Red/Dark Urine",
};

// Symptom Groups
const SYMPTOM_GROUPS = [
  {
    id: "sg_skin_coat",
    name: "Skin & Coat",
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
    name: "Digestive & Stool",
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
    name: "Respiratory",
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
    name: "Behavioral & Nervous",
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
    name: "Reproduction",
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
    name: "General & Limbs",
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
    name: "African Swine Fever (ASF)",
    scientificName: "Asfivirus",
    symptomKeys: ["sym_high_fever", "sym_loss_of_appetite", "sym_sudden_death", "sym_skin_discoloration", "sym_lethargy", "sym_vomiting", "sym_diarrhea"],
    description: "A highly contagious and deadly viral disease prevalent in West Africa. No vaccine exists.",
    severity: "CRITICAL",
    prevention: "Strict biosecurity and restrict farm access.",
  },
  {
    id: "dis_fmd",
    name: "Foot and Mouth Disease (FMD)",
    scientificName: "Aphthovirus",
    symptomKeys: ["sym_high_fever", "sym_lameness", "sym_blisters_snout_feet", "sym_loss_of_appetite", "sym_nasal_discharge"],
    description: "Viral disease causing blisters on the mouth and feet.",
    severity: "HIGH",
    prevention: "Vaccination and movement control.",
  },
  {
    id: "dis_prrs",
    name: "PRRS (Blue Ear)",
    scientificName: "Arterivirus",
    symptomKeys: ["sym_coughing", "sym_difficulty_breathing", "sym_abortion", "sym_lethargy", "sym_skin_discoloration"],
    description: "Causes respiratory failure and reproductive issues.",
    severity: "HIGH",
    prevention: "Biosecurity and herd vaccination.",
  },
  {
    id: "dis_erysipelas",
    name: "Swine Erysipelas",
    scientificName: "Erysipelothrix rhusiopathiae",
    symptomKeys: ["sym_high_fever", "sym_diamond_lesions", "sym_lameness", "sym_sudden_death"],
    description: "Bacterial infection causing distinct skin lesions and arthritis.",
    severity: "MODERATE",
    prevention: "Regular vaccination and pen hygiene.",
  },
  {
    id: "dis_csf",
    name: "Classical Swine Fever",
    scientificName: "Pestivirus",
    symptomKeys: ["sym_high_fever", "sym_diarrhea", "sym_skin_discoloration", "sym_coughing", "sym_lethargy"],
    description: "Highly contagious viral disease similar to ASF.",
    severity: "CRITICAL",
    prevention: "Vaccination and strict biosecurity.",
  },
  {
    id: "dis_anthrax",
    name: "Anthrax",
    scientificName: "Bacillus anthracis",
    symptomKeys: ["sym_sudden_death", "sym_high_fever", "sym_difficulty_breathing", "sym_bleeding_orifices"],
    description: "Serious bacterial disease that can affect humans.",
    severity: "CRITICAL",
    prevention: "Vaccination and avoid opening carcasses.",
  },
  {
    id: "dis_brucellosis",
    name: "Brucellosis",
    scientificName: "Brucella suis",
    symptomKeys: ["sym_abortion", "sym_lameness", "sym_swollen_joints", "sym_lethargy"],
    description: "Bacterial disease causing reproductive failure.",
    severity: "HIGH",
    prevention: "Testing, culling, and sanitation.",
  },
  {
    id: "dis_coccidiosis",
    name: "Coccidiosis",
    scientificName: "Isospora suis",
    symptomKeys: ["sym_diarrhea", "sym_weight_loss", "sym_lethargy", "sym_loss_of_appetite"],
    description: "Parasitic disease primarily affecting piglets.",
    severity: "MODERATE",
    prevention: "Farrowing pen hygiene.",
  },
  {
    id: "dis_pneumonia",
    name: "Mycoplasmal Pneumonia",
    scientificName: "Mycoplasma hyopneumoniae",
    symptomKeys: ["sym_coughing", "sym_difficulty_breathing", "sym_weight_loss", "sym_lethargy"],
    description: "Chronic respiratory disease leading to poor growth.",
    severity: "MODERATE",
    prevention: "Vaccination and improved ventilation.",
  },
  {
    id: "dis_salmonellosis",
    name: "Salmonellosis",
    scientificName: "Salmonella typhimurium",
    symptomKeys: ["sym_diarrhea", "sym_high_fever", "sym_skin_discoloration", "sym_vomiting"],
    description: "Bacterial infection causing severe gut issues.",
    severity: "HIGH",
    prevention: "Clean water and hygiene.",
  },
  {
    id: "dis_ppv",
    name: "Porcine Parvovirus (PPV)",
    scientificName: "Porcine Parvovirus",
    symptomKeys: ["sym_abortion", "sym_mummified_piglets", "sym_small_litter"],
    description: "Common cause of reproductive failure in gilts.",
    severity: "MODERATE",
    prevention: "Vaccination before breeding.",
  },
  {
    id: "dis_rhinitis",
    name: "Atrophic Rhinitis",
    scientificName: "Bordetella bronchiseptica",
    symptomKeys: ["sym_nasal_discharge", "sym_coughing", "sym_sneezing", "sym_weight_loss"],
    description: "Causes distortion of the snout and respiratory issues.",
    severity: "MODERATE",
    prevention: "Vaccination and air quality.",
  },
  {
    id: "dis_leptospirosis",
    name: "Leptospirosis",
    scientificName: "Leptospira spp.",
    symptomKeys: ["sym_abortion", "sym_high_fever", "sym_loss_of_appetite", "sym_jaundice"],
    description: "Zoonotic bacterial disease affecting kidneys.",
    severity: "HIGH",
    prevention: "Rodent control and vaccination.",
  },
  {
    id: "dis_influenza",
    name: "Swine Influenza",
    scientificName: "Influenza A virus",
    symptomKeys: ["sym_coughing", "sym_high_fever", "sym_nasal_discharge", "sym_difficulty_breathing"],
    description: "Rapidly spreading respiratory infection.",
    severity: "MODERATE",
    prevention: "Ventilation and biosecurity.",
  },
  {
    id: "dis_edema",
    name: "Edema Disease",
    scientificName: "Escherichia coli",
    symptomKeys: ["sym_swollen_eyelids", "sym_seizures", "sym_sudden_death", "sym_loss_of_appetite"],
    description: "Caused by E. coli toxins in weaned piglets.",
    severity: "HIGH",
    prevention: "Improved sanitation and diet.",
  },
  {
    id: "dis_dysentery",
    name: "Swine Dysentery",
    scientificName: "Brachyspira hyodysenteriae",
    symptomKeys: ["sym_bloody_diarrhea", "sym_weight_loss", "sym_lethargy"],
    description: "Severe mucohaemorrhagic diarrhea.",
    severity: "HIGH",
    prevention: "Water medication and disinfection.",
  },
  {
    id: "dis_mange",
    name: "Sarcoptic Mange",
    scientificName: "Sarcoptes scabiei",
    symptomKeys: ["sym_itching", "sym_hair_loss", "sym_skin_crusts"],
    description: "Parasitic skin disease causing extreme discomfort.",
    severity: "LOW",
    prevention: "Acaricide treatment.",
  },
  {
    id: "dis_anemia",
    name: "Iron Deficiency Anemia",
    scientificName: "Iron Deficiency",
    symptomKeys: ["sym_pale_skin", "sym_lethargy", "sym_difficulty_breathing"],
    description: "Common in indoor piglets without iron supplements.",
    severity: "MODERATE",
    prevention: "Iron injections at birth.",
  },
  {
    id: "dis_ulcers",
    name: "Gastric Ulcers",
    scientificName: "Gastric Ulcers",
    symptomKeys: ["sym_vomiting", "sym_pale_skin", "sym_weight_loss", "sym_blood_in_stool"],
    description: "Caused by fine feed or stress.",
    severity: "MODERATE",
    prevention: "Adjust feed particle size.",
  },
  {
    id: "dis_pss",
    name: "Porcine Stress Syndrome",
    scientificName: "Genetic stress susceptibility",
    symptomKeys: ["sym_trembling", "sym_muscle_stiffness", "sym_high_fever", "sym_sudden_death"],
    description: "Genetic condition triggered by stress.",
    severity: "HIGH",
    prevention: "Genetic selection and calm handling.",
  },
  {
    id: "dis_tge",
    name: "Transmissible Gastroenteritis (TGE)",
    scientificName: "Coronavirus",
    symptomKeys: ["sym_vomiting", "sym_diarrhea", "sym_sudden_death", "sym_dehydration"],
    description: "Viral disease with high mortality in young piglets.",
    severity: "CRITICAL",
    prevention: "Biosecurity.",
  },
  {
    id: "dis_pasteurellosis",
    name: "Pasteurellosis",
    scientificName: "Pasteurella multocida",
    symptomKeys: ["sym_coughing", "sym_high_fever", "sym_difficulty_breathing", "sym_thumping"],
    description: "Bacterial pneumonia.",
    severity: "HIGH",
    prevention: "Antibiotics and ventilation.",
  },
  {
    id: "dis_strep",
    name: "Streptococcus suis",
    scientificName: "S. suis",
    symptomKeys: ["sym_high_fever", "sym_seizures", "sym_lameness", "sym_incoordination"],
    description: "Bacterial infection causing meningitis.",
    severity: "HIGH",
    prevention: "Antibiotics and stress reduction.",
  },
  {
    id: "dis_mycotoxicosis",
    name: "Mycotoxicosis",
    scientificName: "Mycotoxicosis",
    symptomKeys: ["sym_loss_of_appetite", "sym_vomiting", "sym_abortion", "sym_swollen_vulva"],
    description: "Caused by mold-contaminated feed.",
    severity: "MODERATE",
    prevention: "Toxin binders and dry storage.",
  },
  {
    id: "dis_greasy_pig",
    name: "Greasy Pig Disease",
    scientificName: "Staphylococcus hyicus",
    symptomKeys: ["sym_skin_discoloration", "sym_skin_crusts", "sym_lethargy", "sym_weight_loss"],
    description: "Bacterial infection causing greasy skin lesions.",
    severity: "MODERATE",
    prevention: "Sanitation and antibiotics.",
  },
  {
    id: "dis_pseudorabies",
    name: "Pseudorabies (Aujeszky's)",
    scientificName: "Suid herpesvirus 1",
    symptomKeys: ["sym_seizures", "sym_trembling", "sym_sudden_death", "sym_abortion", "sym_coughing"],
    description: "Viral disease affecting nervous and respiratory systems.",
    severity: "CRITICAL",
    prevention: "Vaccination and eradication programs.",
  },
  {
    id: "dis_pcv2",
    name: "Porcine Circovirus (PCV2)",
    scientificName: "Circovirus",
    symptomKeys: ["sym_weight_loss", "sym_difficulty_breathing", "sym_diarrhea", "sym_jaundice", "sym_pale_skin"],
    description: "Causes wasting and respiratory issues in growers.",
    severity: "HIGH",
    prevention: "Vaccination.",
  },
  {
    id: "dis_ped",
    name: "Porcine Epidemic Diarrhea (PED)",
    scientificName: "Coronavirus",
    symptomKeys: ["sym_vomiting", "sym_watery_diarrhea", "sym_dehydration", "sym_sudden_death"],
    description: "Similar to TGE, highly fatal to neonates.",
    severity: "CRITICAL",
    prevention: "Strict biosecurity.",
  },
  {
    id: "dis_je",
    name: "Japanese Encephalitis",
    scientificName: "Flavivirus",
    symptomKeys: ["sym_abortion", "sym_mummified_piglets", "sym_infertility", "sym_trembling"],
    description: "Mosquito-borne viral disease.",
    severity: "HIGH",
    prevention: "Mosquito control and vaccination.",
  },
  {
    id: "dis_rabies",
    name: "Rabies",
    scientificName: "Lyssavirus",
    symptomKeys: ["sym_nervousness", "sym_aggression", "sym_excessive_salivation", "sym_paralysis", "sym_sudden_death"],
    description: "Fatal viral disease usually from bites.",
    severity: "CRITICAL",
    prevention: "Vaccination of pets and wildlife control.",
  },
  {
    id: "dis_pox",
    name: "Swine Pox",
    scientificName: "Suipoxvirus",
    symptomKeys: ["sym_skin_crusts", "sym_lethargy", "sym_loss_of_appetite"],
    description: "Viral skin disease, often spread by lice.",
    severity: "LOW",
    prevention: "Louse control.",
  },
  {
    id: "dis_vs",
    name: "Vesicular Stomatitis",
    scientificName: "Rhabdovirus",
    symptomKeys: ["sym_blisters_snout_feet", "sym_excessive_salivation", "sym_loss_of_appetite"],
    description: "Causes blisters; looks like FMD.",
    severity: "HIGH",
    prevention: "Insect control and quarantine.",
  },
  {
    id: "dis_clostridial",
    name: "Clostridial Enteritis",
    scientificName: "Clostridium perfringens",
    symptomKeys: ["sym_bloody_diarrhea", "sym_sudden_death", "sym_bloat"],
    description: "Bacterial infection causing severe gut damage.",
    severity: "CRITICAL",
    prevention: "Vaccination of sows and hygiene.",
  },
  {
    id: "dis_glassers",
    name: "Glasser's Disease",
    scientificName: "Haemophilus parasuis",
    symptomKeys: ["sym_high_fever", "sym_coughing", "sym_lameness", "sym_swollen_joints", "sym_trembling"],
    description: "Causes inflammation of membranes around heart, lungs, joints.",
    severity: "HIGH",
    prevention: "Antibiotics and stress management.",
  },
  {
    id: "dis_app",
    name: "Actinobacillus Pleuropneumonia (APP)",
    scientificName: "A. pleuropneumoniae",
    symptomKeys: ["sym_sudden_death", "sym_high_fever", "sym_nose_bleeds", "sym_difficulty_breathing", "sym_coughing"],
    description: "Severe bacterial pneumonia.",
    severity: "CRITICAL",
    prevention: "Antibiotics and improved environment.",
  },
  {
    id: "dis_ileitis",
    name: "Ileitis (Lawsonia)",
    scientificName: "Lawsonia intracellularis",
    symptomKeys: ["sym_diarrhea", "sym_blood_in_stool", "sym_weight_loss", "sym_pale_skin"],
    description: "Bacterial infection of the small intestine.",
    severity: "MODERATE",
    prevention: "Vaccination and antibiotics.",
  },
  {
    id: "dis_tb",
    name: "Tuberculosis",
    scientificName: "Mycobacterium avium",
    symptomKeys: ["sym_weight_loss", "sym_lethargy", "sym_coughing"],
    description: "Chronic bacterial disease.",
    severity: "LOW",
    prevention: "Bird proofing and sanitation.",
  },
  {
    id: "dis_tetanus",
    name: "Tetanus",
    scientificName: "Clostridium tetani",
    symptomKeys: ["sym_muscle_stiffness", "sym_seizures", "sym_trembling"],
    description: "Bacterial toxin affecting the nervous system.",
    severity: "HIGH",
    prevention: "Sanitation during surgery/castration.",
  },
  {
    id: "dis_eperythrozoonosis",
    name: "Eperythrozoonosis",
    scientificName: "Mycoplasma suis",
    symptomKeys: ["sym_pale_skin", "sym_jaundice", "sym_high_fever", "sym_infertility", "sym_lethargy", "sym_red_urine"],
    description: "Blood parasite causing anemia.",
    severity: "MODERATE",
    prevention: "Insect control and clean needles.",
  },
  {
    id: "dis_ascaris",
    name: "Ascaris suum",
    scientificName: "Large Roundworm",
    symptomKeys: ["sym_coughing", "sym_weight_loss", "sym_stunted_growth", "sym_rough_hair"],
    description: "Internal parasite causing lung and liver damage.",
    severity: "MODERATE",
    prevention: "Regular deworming.",
  },
  {
    id: "dis_whipworms",
    name: "Whipworms",
    scientificName: "Trichuris suis",
    symptomKeys: ["sym_bloody_diarrhea", "sym_weight_loss", "sym_dehydration"],
    description: "Internal parasite in the large intestine.",
    severity: "MODERATE",
    prevention: "Deworming and sanitation.",
  },
  {
    id: "dis_lungworms",
    name: "Lungworms",
    scientificName: "Metastrongylus spp.",
    symptomKeys: ["sym_coughing", "sym_thumping", "sym_stunted_growth"],
    description: "Internal parasite of the lungs.",
    severity: "MODERATE",
    prevention: "Earthworm control and deworming.",
  },
  {
    id: "dis_kidney_worms",
    name: "Kidney Worms",
    scientificName: "Stephanurus dentatus",
    symptomKeys: ["sym_weight_loss", "sym_stunted_growth", "sym_leg_weakness"],
    description: "Internal parasite affecting kidneys and liver.",
    severity: "MODERATE",
    prevention: "Management and hygiene.",
  },
  {
    id: "dis_lice",
    name: "Swine Lice",
    scientificName: "Haematopinus suis",
    symptomKeys: ["sym_itching", "sym_rough_hair", "sym_pale_skin", "sym_anemia"],
    description: "External parasite sucking blood.",
    severity: "LOW",
    prevention: "Insecticide treatment.",
  },
  {
    id: "dis_ringworm",
    name: "Ringworm",
    scientificName: "Dermatophytosis",
    symptomKeys: ["sym_skin_crusts", "sym_itching"],
    description: "Fungal skin infection.",
    severity: "LOW",
    prevention: "Sanitation and topical treatment.",
  },
  {
    id: "dis_mulberry_heart",
    name: "Mulberry Heart Disease",
    scientificName: "Vit E/Se Deficiency",
    symptomKeys: ["sym_sudden_death", "sym_difficulty_breathing", "sym_trembling"],
    description: "Nutritional deficiency affecting the heart.",
    severity: "HIGH",
    prevention: "Supplementation.",
  },
  {
    id: "dis_vit_a",
    name: "Vitamin A Deficiency",
    scientificName: "Vitamin A Deficiency",
    symptomKeys: ["sym_blindness", "sym_incoordination", "sym_infertility", "sym_trembling"],
    description: "Nutritional deficiency.",
    severity: "MODERATE",
    prevention: "Dietary adjustment.",
  },
  {
    id: "dis_rickets",
    name: "Rickets",
    scientificName: "Vit D/Ca/P Deficiency",
    symptomKeys: ["sym_lameness", "sym_swollen_joints", "sym_leg_weakness", "sym_fractures"],
    description: "Nutritional deficiency affecting bone growth.",
    severity: "MODERATE",
    prevention: "Balanced minerals and sunlight.",
  },
  {
    id: "dis_salt_poisoning",
    name: "Salt Poisoning",
    scientificName: "Water Deprivation",
    symptomKeys: ["sym_seizures", "sym_blindness", "sym_circling", "sym_convulsions", "sym_thirst"],
    description: "Caused by lack of water followed by over-consumption.",
    severity: "HIGH",
    prevention: "Constant fresh water access.",
  },
  {
    id: "dis_heat_stroke",
    name: "Heat Stroke",
    scientificName: "Heat Stroke",
    symptomKeys: ["sym_high_fever", "sym_difficulty_breathing", "sym_lethargy", "sym_sudden_death"],
    description: "Overheating due to poor ventilation or shade.",
    severity: "HIGH",
    prevention: "Ventilation, cooling, and shade.",
  },
  {
    id: "dis_rectal_prolapse",
    name: "Rectal Prolapse",
    scientificName: "Rectal Prolapse",
    symptomKeys: ["sym_rectal_prolapse", "sym_blood_in_stool"],
    description: "Often follows coughing, diarrhea, or cold stress.",
    severity: "MODERATE",
    prevention: "Address primary cause and surgery.",
  },
  {
    id: "dis_umbilical_hernia",
    name: "Umbilical Hernia",
    scientificName: "Umbilical Hernia",
    symptomKeys: ["sym_swollen_navel"],
    description: "Developmental defect or navel infection.",
    severity: "LOW",
    prevention: "Sanitation and genetics.",
  },
  {
    id: "dis_scrotal_hernia",
    name: "Scrotal Hernia",
    scientificName: "Scrotal Hernia",
    symptomKeys: ["sym_enlarged_scrotum"],
    description: "Genetic developmental defect.",
    severity: "LOW",
    prevention: "Genetics and surgical repair.",
  },
  {
    id: "dis_splayleg",
    name: "Splayleg",
    scientificName: "Myofibrillar hypoplasia",
    symptomKeys: ["sym_leg_weakness", "sym_difficulty_breathing", "sym_sudden_death"],
    description: "Piglets born unable to stand with hind legs.",
    severity: "MODERATE",
    prevention: "Taping legs and floor traction.",
  },
  {
    id: "dis_vulvovaginitis",
    name: "Vulvovaginitis",
    scientificName: "Zearalenone Toxicity",
    symptomKeys: ["sym_swollen_vulva", "sym_abortion", "sym_infertility"],
    description: "Caused by estrogenic fungal toxins in feed.",
    severity: "MODERATE",
    prevention: "Change feed source.",
  },
  {
    id: "dis_seneca",
    name: "Seneca Valley Virus (SVV)",
    scientificName: "Senecavirus A",
    symptomKeys: ["sym_blisters_snout_feet", "sym_lameness", "sym_sudden_death"],
    description: "Causes blisters similar to FMD.",
    severity: "HIGH",
    prevention: "Biosecurity and diagnosis.",
  },
  {
    id: "dis_deltacoronavirus",
    name: "Porcine Deltacoronavirus",
    scientificName: "PDCoV",
    symptomKeys: ["sym_watery_diarrhea", "sym_vomiting", "sym_dehydration"],
    description: "Viral diarrhea in all ages.",
    severity: "HIGH",
    prevention: "Biosecurity.",
  },
  {
    id: "dis_rotavirus",
    name: "Rotavirus Diarrhea",
    scientificName: "Rotavirus",
    symptomKeys: ["sym_diarrhea", "sym_vomiting", "sym_weight_loss"],
    description: "Common cause of weaning diarrhea.",
    severity: "MODERATE",
    prevention: "Vaccination and hygiene.",
  },
  {
    id: "dis_teschen",
    name: "Teschen/Talfan Disease",
    scientificName: "Sapelovirus/Teschovirus",
    symptomKeys: ["sym_paralysis", "sym_incoordination", "sym_seizures", "sym_trembling"],
    description: "Viral infection of the nervous system.",
    severity: "HIGH",
    prevention: "Biosecurity.",
  },
  {
    id: "dis_vesicular",
    name: "Swine Vesicular Disease",
    scientificName: "Enterovirus",
    symptomKeys: ["sym_blisters_snout_feet", "sym_lameness", "sym_high_fever"],
    description: "Viral disease mimicking FMD.",
    severity: "HIGH",
    prevention: "Strict quarantine.",
  },
  {
    id: "dis_melioidosis",
    name: "Melioidosis",
    scientificName: "Burkholderia pseudomallei",
    symptomKeys: ["sym_high_fever", "sym_coughing", "sym_lameness", "sym_abscesses"],
    description: "Bacterial disease from soil/water.",
    severity: "MODERATE",
    prevention: "Clean water and hygiene.",
  },
  {
    id: "dis_trypanosomiasis",
    name: "Trypanosomiasis",
    scientificName: "Trypanosoma spp.",
    symptomKeys: ["sym_high_fever", "sym_anemia", "sym_weight_loss", "sym_lethargy", "sym_abortion"],
    description: "Protozoan disease spread by tsetse flies.",
    severity: "HIGH",
    prevention: "Fly control and medication.",
  },
  {
    id: "dis_cysticercosis",
    name: "Cysticercosis",
    scientificName: "Taenia solium larvae",
    symptomKeys: ["sym_muscle_stiffness", "sym_seizures"],
    description: "Larval stage of human tapeworm in pig muscle.",
    severity: "LOW",
    prevention: "Prevent access to human waste.",
  },
  {
    id: "dis_toxoplasmosis",
    name: "Toxoplasmosis",
    scientificName: "Toxoplasma gondii",
    symptomKeys: ["sym_abortion", "sym_high_fever", "sym_difficulty_breathing", "sym_lethargy"],
    description: "Zoonotic protozoan infection.",
    severity: "MODERATE",
    prevention: "Cat control and biosecurity.",
  },
  {
    id: "dis_listeriosis",
    name: "Listeriosis",
    scientificName: "Listeria monocytogenes",
    symptomKeys: ["sym_seizures", "sym_circling", "sym_abortion", "sym_high_fever"],
    description: "Bacterial infection of nervous system.",
    severity: "MODERATE",
    prevention: "Sanitation and feed hygiene.",
  },
  {
    id: "dis_ear_necrosis",
    name: "Ear Necrosis",
    scientificName: "Ear Necrosis",
    symptomKeys: ["sym_ear_necrosis", "sym_skin_discoloration", "sym_lethargy"],
    description: "Often caused by stress, overcrowding, or infection.",
    severity: "MODERATE",
    prevention: "Reduce density and treat secondary infection.",
  },
  {
    id: "dis_sunburn",
    name: "Sunburn",
    scientificName: "Solar dermatitis",
    symptomKeys: ["sym_sunburn", "sym_skin_discoloration", "sym_lethargy"],
    description: "Affects white-skinned pigs outdoors.",
    severity: "LOW",
    prevention: "Provide shade.",
  },
  {
    id: "dis_tail_biting",
    name: "Tail Biting",
    scientificName: "Behavioral stress",
    symptomKeys: ["sym_tail_biting", "sym_blood_in_stool", "sym_lameness"],
    description: "Behavioral issue due to stress/boredom.",
    severity: "MODERATE",
    prevention: "Reduce stress and enrich environment.",
  },
  {
    id: "dis_navel_ill",
    name: "Navel Ill",
    scientificName: "Navel Infection",
    symptomKeys: ["sym_swollen_navel", "sym_high_fever", "sym_lameness"],
    description: "Infection of the umbilical cord at birth.",
    severity: "MODERATE",
    prevention: "Iodine dip at birth and sanitation.",
  },
  {
    id: "dis_iron_toxicity",
    name: "Iron Toxicity",
    scientificName: "Ferrous excess",
    symptomKeys: ["sym_sudden_death", "sym_difficulty_breathing", "sym_muscle_stiffness"],
    description: "Excess iron administration leading to sudden death.",
    severity: "HIGH",
    prevention: "Inject accurate dosage.",
  },
  {
    id: "dis_aflatoxicosis",
    name: "Aflatoxicosis",
    scientificName: "Aspergillus flavus toxin",
    symptomKeys: ["sym_loss_of_appetite", "sym_weight_loss", "sym_jaundice", "sym_lethargy", "sym_sudden_death"],
    description: "Fungal toxin poisoning.",
    severity: "HIGH",
    prevention: "Avoid moldy feed and use toxin binders.",
  },
  {
    id: "dis_parakeratosis",
    name: "Parakeratosis",
    scientificName: "Zinc deficiency",
    symptomKeys: ["sym_skin_crusts", "sym_rough_hair", "sym_stunted_growth"],
    description: "Zinc deficiency in swine.",
    severity: "MODERATE",
    prevention: "Ensure adequate zinc levels in diet.",
  },
  {
    id: "dis_biotin_deficiency",
    name: "Biotin Deficiency",
    scientificName: "Vitamin H deficiency",
    symptomKeys: ["sym_lameness", "sym_hair_loss", "sym_leg_weakness", "sym_cracked_hooves"],
    description: "Biotin deficiency in swine.",
    severity: "MODERATE",
    prevention: "Provide biotin supplements.",
  },
  {
    id: "dis_gossypol_toxicity",
    name: "Gossypol Toxicity",
    scientificName: "Gossypol poisoning",
    symptomKeys: ["sym_difficulty_breathing", "sym_thumping", "sym_loss_of_appetite", "sym_sudden_death"],
    description: "Cottonseed meal poisoning.",
    severity: "HIGH",
    prevention: "Limit raw cottonseed meal in feed.",
  },
  {
    id: "dis_cassava_poisoning",
    name: "Cassava Poisoning",
    scientificName: "Cyanogenic glycosides",
    symptomKeys: ["sym_difficulty_breathing", "sym_incoordination", "sym_sudden_death", "sym_lethargy"],
    description: "Cyanide poisoning from raw cassava.",
    severity: "CRITICAL",
    prevention: "Cook or ferment cassava before feeding.",
  },
  {
    id: "dis_sweet_potato_toxicity",
    name: "Sweet Potato Toxicity",
    scientificName: "Ipomeamarone toxicity",
    symptomKeys: ["sym_difficulty_breathing", "sym_thumping", "sym_frothing"],
    description: "Ipomeamarone poisoning.",
    severity: "HIGH",
    prevention: "Do not feed moldy or damaged sweet potatoes.",
  },
  {
    id: "dis_mma",
    name: "MMA Syndrome",
    scientificName: "Mastitis-Metritis-Agalactia",
    symptomKeys: ["sym_high_fever", "sym_lethargy", "sym_loss_of_appetite", "sym_swollen_udder"],
    description: "Post-farrowing infection in sows.",
    severity: "HIGH",
    prevention: "Clean farrowing pens and proper nutrition.",
  },
  {
    id: "dis_pwd",
    name: "Post-Weaning Diarrhea",
    scientificName: "Escherichia coli (F4/F18)",
    symptomKeys: ["sym_diarrhea", "sym_dehydration", "sym_loss_of_appetite", "sym_weight_loss"],
    description: "Colibacillosis in weaned pigs.",
    severity: "HIGH",
    prevention: "Sanitation and diet adjustment.",
  },
  {
    id: "dis_trichinellosis",
    name: "Trichinellosis",
    scientificName: "Trichinella spiralis",
    symptomKeys: ["sym_muscle_stiffness", "sym_lethargy", "sym_incoordination"],
    description: "Zoonotic parasitic infection.",
    severity: "MODERATE",
    prevention: "Do not feed raw garbage; pest control.",
  },
  {
    id: "dis_hydatid_disease",
    name: "Hydatid Disease",
    scientificName: "Echinococcus granulosus",
    symptomKeys: ["sym_weight_loss", "sym_stunted_growth", "sym_lethargy"],
    description: "Larval tapeworm infection.",
    severity: "LOW",
    prevention: "Deworm farm dogs; prevent access to offal.",
  },
  {
    id: "dis_phe",
    name: "Proliferative Hemorrhagic Enteropathy",
    scientificName: "Lawsonia intracellularis",
    symptomKeys: ["sym_bloody_diarrhea", "sym_pale_skin", "sym_sudden_death"],
    description: "Acute intestinal bleeding in young adult pigs.",
    severity: "CRITICAL",
    prevention: "Vaccination and hygiene.",
  },
  {
    id: "dis_mycoplasma_arthritis",
    name: "Mycoplasma Arthritis",
    scientificName: "Mycoplasma hyosynoviae",
    symptomKeys: ["sym_lameness", "sym_swollen_joints", "sym_leg_weakness"],
    description: "Bacterial joint disease.",
    severity: "MODERATE",
    prevention: "Sanitation and quarantine.",
  },
  {
    id: "dis_foot_rot",
    name: "Foot Rot",
    scientificName: "Fusobacterium necrophorum",
    symptomKeys: ["sym_lameness", "sym_lethargy", "sym_loss_of_appetite", "sym_cracked_hooves"],
    description: "Infection of the foot causing lameness.",
    severity: "MODERATE",
    prevention: "Keep pens dry; treat hooves.",
  },
  {
    id: "dis_babesiosis",
    name: "Babesiosis",
    scientificName: "Babesia trautmanni",
    symptomKeys: ["sym_high_fever", "sym_jaundice", "sym_pale_skin", "sym_abortion", "sym_lethargy", "sym_red_urine"],
    description: "Tick-borne protozoan infection.",
    severity: "HIGH",
    prevention: "Acaricide spray for tick control.",
  },
  {
    id: "dis_anaplasmosis",
    name: "Anaplasmosis",
    scientificName: "Anaplasma marginale",
    symptomKeys: ["sym_high_fever", "sym_pale_skin", "sym_jaundice", "sym_lethargy"],
    description: "Blood infection in swine.",
    severity: "MODERATE",
    prevention: "Vector control.",
  },
  {
    id: "dis_sow_hysteria",
    name: "Sow Hysteria",
    scientificName: "Puerperal psychosis",
    symptomKeys: ["sym_nervousness", "sym_lethargy"],
    description: "Aggression or panic in sows after farrowing.",
    severity: "MODERATE",
    prevention: "Provide quiet farrowing area.",
  },
  {
    id: "dis_thorny_headed_worm",
    name: "Thorny-Headed Worm",
    scientificName: "Macracanthorhynchus hirudinaceus",
    symptomKeys: ["sym_diarrhea", "sym_weight_loss", "sym_stunted_growth", "sym_sudden_death"],
    description: "Intestinal parasite.",
    severity: "MODERATE",
    prevention: "Prevent access to June beetle grubs.",
  },
  {
    id: "dis_strongyloidiasis",
    name: "Strongyloidiasis",
    scientificName: "Strongyloides ransomi",
    symptomKeys: ["sym_diarrhea", "sym_dehydration", "sym_stunted_growth", "sym_sudden_death"],
    description: "Threadworm infection.",
    severity: "HIGH",
    prevention: "Keep pens dry; deworming.",
  },
  {
    id: "dis_hyostrongylosis",
    name: "Red Stomach Worm",
    scientificName: "Hyostrongylus rubidus",
    symptomKeys: ["sym_weight_loss", "sym_pale_skin", "sym_rough_hair", "sym_loss_of_appetite"],
    description: "Intestinal worm in grazing pigs.",
    severity: "MODERATE",
    prevention: "Clean pasture management; deworming.",
  },
  {
    id: "dis_oesophagostomiasis",
    name: "Nodular Worm",
    scientificName: "Oesophagostomum spp.",
    symptomKeys: ["sym_weight_loss", "sym_diarrhea", "sym_rough_hair"],
    description: "Intestinal worm in swine.",
    severity: "MODERATE",
    prevention: "Deworming and sanitation.",
  },
  {
    id: "dis_fascioliasis",
    name: "Fascioliasis",
    scientificName: "Fasciola gigantica",
    symptomKeys: ["sym_weight_loss", "sym_lethargy", "sym_stunted_growth", "sym_jaundice"],
    description: "Liver fluke infection.",
    severity: "MODERATE",
    prevention: "Snail control; keep away from wet pastures.",
  },
  {
    id: "dis_emcv",
    name: "EMCV",
    scientificName: "Cardiovirus",
    symptomKeys: ["sym_sudden_death", "sym_difficulty_breathing", "sym_abortion", "sym_trembling"],
    description: "Viral myocarditis and reproductive failure.",
    severity: "CRITICAL",
    prevention: "Rodent control (rodents spread EMCV).",
  },
  {
    id: "dis_demodectic_mange",
    name: "Demodectic Mange",
    scientificName: "Demodex phylloides",
    symptomKeys: ["sym_skin_crusts", "sym_itching", "sym_hair_loss"],
    description: "Skin mite infection.",
    severity: "LOW",
    prevention: "Acaricide treatment.",
  },
  {
    id: "dis_pityriasis_rosea",
    name: "Pityriasis Rosea",
    scientificName: "Pseudo-ringworm",
    symptomKeys: ["sym_skin_crusts", "sym_rough_hair"],
    description: "Non-contagious skin condition.",
    severity: "LOW",
    prevention: "Spontaneous recovery; keep environment clean.",
  },
  {
    id: "dis_photosensitization",
    name: "Photosensitization",
    scientificName: "Solar dermatitis",
    symptomKeys: ["sym_skin_discoloration", "sym_sunburn", "sym_skin_crusts"],
    description: "Skin hypersensitivity to sunlight.",
    severity: "MODERATE",
    prevention: "Keep out of direct sunlight; check pasture plants.",
  },
  {
    id: "dis_shoulder_ulcers",
    name: "Shoulder Ulcers",
    scientificName: "Decubitus ulcers",
    symptomKeys: ["sym_lameness", "sym_pale_skin"],
    description: "Sores in thin sows on hard floors.",
    severity: "MODERATE",
    prevention: "Provide soft bedding; increase feed.",
  },
  {
    id: "dis_ear_biting",
    name: "Ear Biting",
    scientificName: "Stress response behavior",
    symptomKeys: ["sym_ear_necrosis", "sym_bleeding_orifices"],
    description: "Behavioral stress response.",
    severity: "MODERATE",
    prevention: "Reduce stocking density; enrich environment.",
  },
  {
    id: "dis_gastric_torsion",
    name: "Gastric Torsion",
    scientificName: "Gastric dilation-volvulus",
    symptomKeys: ["sym_bloat", "sym_sudden_death", "sym_vomiting"],
    description: "Twisted stomach.",
    severity: "CRITICAL",
    prevention: "Avoid overfeeding in single meals.",
  },
  {
    id: "dis_c_difficile",
    name: "C. difficile Infection",
    scientificName: "Clostridioides difficile",
    symptomKeys: ["sym_diarrhea", "sym_dehydration", "sym_lethargy"],
    description: "Bacterial diarrhea in piglets.",
    severity: "MODERATE",
    prevention: "Clean environment and proper ventilation.",
  },
  {
    id: "dis_cold_stress",
    name: "Cold Stress",
    scientificName: "Hypothermia susceptibility",
    symptomKeys: ["sym_trembling", "sym_lethargy", "sym_sudden_death"],
    description: "Chilling in young piglets.",
    severity: "HIGH",
    prevention: "Provide heat lamps and dry bedding.",
  },
  {
    id: "dis_necrotic_enteritis",
    name: "Necrotic Enteritis",
    scientificName: "Clostridium perfringens Type C",
    symptomKeys: ["sym_bloody_diarrhea", "sym_sudden_death", "sym_dehydration"],
    description: "Clostridial gut infection.",
    severity: "CRITICAL",
    prevention: "Sow vaccination and pen sanitation.",
  },
  {
    id: "dis_navel_bleeding",
    name: "Navel Bleeding",
    scientificName: "Umbilical hemorrhage",
    symptomKeys: ["sym_bleeding_orifices", "sym_pale_skin", "sym_sudden_death"],
    description: "Hemorrhage from umbilical cord.",
    severity: "HIGH",
    prevention: "Tie off bleeding navels; check feed vitamin K.",
  },
  {
    id: "dis_spirochetal_colitis",
    name: "Spirochetal Colitis",
    scientificName: "Brachyspira pilosicoli",
    symptomKeys: ["sym_diarrhea", "sym_weight_loss", "sym_stunted_growth"],
    description: "Bacterial large intestine infection.",
    severity: "MODERATE",
    prevention: "Medication and pen sanitation.",
  },
  {
    id: "dis_salmonella_septicemia",
    name: "Salmonella Septicemia",
    scientificName: "Salmonella choleraesuis",
    symptomKeys: ["sym_high_fever", "sym_skin_discoloration", "sym_difficulty_breathing", "sym_sudden_death"],
    description: "Blood infection from Salmonella.",
    severity: "HIGH",
    prevention: "Clean water; biosecurity.",
  },
  {
    id: "dis_prdc",
    name: "PRDC",
    scientificName: "Porcine Respiratory Disease Complex",
    symptomKeys: ["sym_coughing", "sym_difficulty_breathing", "sym_nasal_discharge", "sym_stunted_growth"],
    description: "Respiratory complex in grower pigs.",
    severity: "HIGH",
    prevention: "Ventilation, vaccination, and density control.",
  },
  {
    id: "dis_ergotism",
    name: "Ergotism",
    scientificName: "Claviceps purpurea",
    symptomKeys: ["sym_lameness", "sym_skin_crusts", "sym_abortion", "sym_infertility"],
    description: "Poisoning from moldy rye/grains.",
    severity: "HIGH",
    prevention: "Inspect feed grains for black ergot bodies.",
  },
  {
    id: "dis_bvdv_pig",
    name: "BVDV in Pigs",
    scientificName: "Pestivirus",
    symptomKeys: ["sym_abortion", "sym_mummified_piglets", "sym_small_litter"],
    description: "Bovine Viral Diarrhea virus in swine.",
    severity: "MODERATE",
    prevention: "Isolate pigs from cattle.",
  },
  {
    id: "dis_endocarditis",
    name: "Endocarditis",
    scientificName: "Streptococcal endocarditis",
    symptomKeys: ["sym_sudden_death", "sym_difficulty_breathing", "sym_lethargy", "sym_lameness"],
    description: "Heart valve infection.",
    severity: "HIGH",
    prevention: "Treat primary infections early.",
  },
  {
    id: "dis_polyserositis",
    name: "Polyserositis",
    scientificName: "Mycoplasma hyorhinis",
    symptomKeys: ["sym_high_fever", "sym_difficulty_breathing", "sym_swollen_joints", "sym_lameness"],
    description: "Infection of body cavity linings.",
    severity: "MODERATE",
    prevention: "Antibiotic treatment; minimize stress.",
  },
  {
    id: "dis_congenital_tremor",
    name: "Congenital Tremor",
    scientificName: "Atypical Porcine Pestivirus",
    symptomKeys: ["sym_trembling", "sym_incoordination", "sym_sudden_death"],
    description: "Shaky pig disease.",
    severity: "MODERATE",
    prevention: "Acclimatize gilts to farm before breeding.",
  },
];

export default function SymptomsPage() {
  const { user, userProfile, loading } = useAuth();
  const { isMobile } = useDevice();
  const router = useRouter();

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
              <h2 className="text-xl font-bold text-zinc-900">Swine Disease Diagnostic Engine</h2>
              <p className="text-sm text-zinc-500 mt-1">
                Select the clinical signs observed in your pigs. The analyzer compares active symptoms against our reference database of swine illnesses.
              </p>
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
                  {selectedSymptoms.size} symptom(s) selected
                </div>
                <div className="flex gap-3">
                  <button
                    onClick={handleReset}
                    className="px-4 py-2 border border-zinc-200 rounded-xl bg-white text-xs font-bold text-zinc-650 hover:bg-zinc-55 transition"
                  >
                    Reset List
                  </button>
                  <button
                    onClick={handleAnalyze}
                    disabled={selectedSymptoms.size === 0}
                    className="px-6 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 disabled:bg-zinc-200 disabled:text-zinc-450 disabled:cursor-not-allowed text-xs font-bold text-white shadow transition-all flex items-center gap-2"
                  >
                    Analyze Symptoms
                  </button>
                </div>
              </div>
            </div>

            {/* Diagnosis results sidebar */}
            <div className="bg-white/60 backdrop-blur-md border border-zinc-200 rounded-2xl p-6 shadow-sm space-y-6">
              <h3 className="text-base font-bold text-zinc-900">Diagnosis Dashboard</h3>

              {!hasAnalyzed ? (
                <div className="py-12 flex flex-col items-center justify-center text-center space-y-3">
                  <span className="text-4xl">🔬</span>
                  <p className="text-xs text-zinc-400 font-semibold max-w-[200px]">
                    Check observed symptoms and click "Analyze Symptoms" to run diagnostic search.
                  </p>
                </div>
              ) : !isPremium ? (
                /* Free Paywall Block */
                <div className="border border-emerald-250 bg-emerald-50/50 rounded-xl p-5 text-center space-y-4">
                  <span className="text-3xl block">🔒</span>
                  <h4 className="text-sm font-bold text-emerald-900">Premium Diagnostics Feature</h4>
                  <p className="text-xs text-zinc-600 leading-relaxed">
                    Analyzing swine diseases is a Premium Feature. Upgraded farms gain full access to the AI diagnostics matcher, match accuracy indicators, and tailored biosecurity prevention guidelines.
                  </p>
                  <Link
                    href="/dashboard/billing"
                    className="block w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow transition"
                  >
                    Upgrade to Premium
                  </Link>
                </div>
              ) : diagnosisResults.length === 0 ? (
                /* No matches found */
                <div className="py-8 text-center space-y-2">
                  <p className="text-xs text-zinc-600 font-medium">
                    No matching diseases found for the selected symptoms.
                  </p>
                  <p className="text-[11px] text-zinc-400 leading-relaxed">
                    Please modify selection or consult your local veterinary officer for physical checks.
                  </p>
                </div>
              ) : (
                /* Results list */
                <div className="space-y-4 max-h-[55vh] overflow-y-auto pr-1 no-scrollbar">
                  <div className="text-xs font-bold text-zinc-500 uppercase tracking-wider">
                    {diagnosisResults.length} Potential Matches
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
                            {disease.severity}
                          </span>
                        </div>

                        {/* Match Indicator */}
                        <div className="space-y-1">
                          <div className="text-[10px] font-bold text-emerald-700">
                            Match Accuracy: {matches} symptom(s) detected
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
                              <strong className="text-zinc-700 block mb-0.5">Description:</strong>
                              {disease.description}
                            </div>

                            {unselectedSymptoms.length > 0 && (
                              <div className="text-[11px] text-zinc-600 leading-relaxed">
                                <strong className="text-zinc-700 block mb-0.5">Other symptoms to watch out for:</strong>
                                <ul className="list-disc list-inside pl-1 space-y-0.5">
                                  {unselectedSymptoms.map(sKey => (
                                    <li key={sKey}>{SYMPTOMS[sKey] || sKey}</li>
                                  ))}
                                </ul>
                              </div>
                            )}

                            {disease.prevention && (
                              <div className="text-[11px] text-zinc-600 leading-relaxed bg-emerald-50/30 p-2.5 rounded-lg border border-emerald-100/50">
                                <strong className="text-emerald-900 block mb-0.5">Prevention & Action Plan:</strong>
                                {disease.prevention}
                              </div>
                            )}
                          </div>
                        )}

                        {!isExpanded && (
                          <div className="text-[10px] text-zinc-400 font-bold text-center pt-1">
                            Click to view details & prevention
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
            <strong>Disclaimer:</strong> This AI tool is powered by veterinary research for educational assistance. It is NOT a substitute for professional veterinary diagnosis. Always consult a qualified veterinary practitioner before initiating treatments or purchasing medications.
          </div>
          </PremiumWrapper>
        </main>
      </div>
    </div>
  );
}
