package com.example.smartswine.ui.training

data class TrainingTip(
    val id: Int,
    val categoryKey: String,
    val titleKey: String,
    val contentKey: String,
    val defaultTitle: String,
    val defaultContent: String
)

object TrainingTipsData {
    val allTrainingTips = listOf(
        // Category 1: Weaning Management (cat_weaning)
        TrainingTip(
            id = 6,
            categoryKey = "cat_weaning",
            titleKey = "tip_6_title",
            contentKey = "tip_6_content",
            defaultTitle = "Weaning Age for Smallholders",
            defaultContent = "Wean piglets at 28 to 35 days instead of earlier. This allows piglets to grow stronger and adapt to solid feed using simple farm resources."
        ),
        TrainingTip(
            id = 7,
            categoryKey = "cat_weaning",
            titleKey = "tip_7_title",
            contentKey = "tip_7_content",
            defaultTitle = "Gradual Separation Method",
            defaultContent = "Reduce weaning stress by moving the sow out of the farrowing pen and leaving the piglets in their familiar environment for the first week."
        ),
        TrainingTip(
            id = 8,
            categoryKey = "cat_weaning",
            titleKey = "tip_8_title",
            contentKey = "tip_8_content",
            defaultTitle = "Draft Prevention for Weaners",
            defaultContent = "Block cold winds in the nursery pen by installing temporary curtains or wooden boards up to piglet-height (about 50cm)."
        ),
        TrainingTip(
            id = 9,
            categoryKey = "cat_weaning",
            titleKey = "tip_9_title",
            contentKey = "tip_9_content",
            defaultTitle = "DIY Creep Feed Area",
            defaultContent = "Create a simple partition or \"creep box\" in the pen using local timber or bricks, with an entrance only large enough for piglets to access starter feed."
        ),
        TrainingTip(
            id = 10,
            categoryKey = "cat_weaning",
            titleKey = "tip_10_title",
            contentKey = "tip_10_content",
            defaultTitle = "Size-Based Pen Grouping",
            defaultContent = "Group piglets of similar sizes together at weaning. This prevents larger piglets from dominant behavior and hoarding feed."
        ),
        TrainingTip(
            id = 11,
            categoryKey = "cat_weaning",
            titleKey = "tip_11_title",
            contentKey = "tip_11_content",
            defaultTitle = "Homemade Rehydration Solution",
            defaultContent = "Feed a cheap, homemade electrolyte solution (5 tablespoons sugar, 1/2 tablespoon salt in 5 liters clean water) during the first 3 days of weaning to combat stress."
        ),
        TrainingTip(
            id = 12,
            categoryKey = "cat_weaning",
            titleKey = "tip_12_title",
            contentKey = "tip_12_content",
            defaultTitle = "Kitchen Scrap Safety",
            defaultContent = "If feeding kitchen waste to weaners, always boil it for at least 30 minutes to kill viruses and bacteria before feeding."
        ),
        TrainingTip(
            id = 13,
            categoryKey = "cat_weaning",
            titleKey = "tip_13_title",
            contentKey = "tip_13_content",
            defaultTitle = "Gradual Feed Transition",
            defaultContent = "Feed the same starter diet for 5 days after weaning before slowly mixing in grower feed over a 7-day period to prevent diarrhea."
        ),
        TrainingTip(
            id = 14,
            categoryKey = "cat_weaning",
            titleKey = "tip_14_title",
            contentKey = "tip_14_content",
            defaultTitle = "Dry Bedding for Warmth",
            defaultContent = "Provide a thick layer of dry straw, wood shavings, or dry grass in the sleeping area to keep weaned piglets warm without using expensive heaters."
        ),
        TrainingTip(
            id = 15,
            categoryKey = "cat_weaning",
            titleKey = "tip_15_title",
            contentKey = "tip_15_content",
            defaultTitle = "Trough Space Allocation",
            defaultContent = "Ensure all piglets can eat at the same time by providing long, low wooden or concrete feed troughs (about 15-20cm of trough space per piglet)."
        ),
        TrainingTip(
            id = 16,
            categoryKey = "cat_weaning",
            titleKey = "tip_16_title",
            contentKey = "tip_16_content",
            defaultTitle = "Banana Stem Fiber",
            defaultContent = "Hang split green banana stems in the weaning pen. Piglets chew on them, which keeps them occupied and prevents tail-biting."
        ),
        TrainingTip(
            id = 17,
            categoryKey = "cat_weaning",
            titleKey = "tip_17_title",
            contentKey = "tip_17_content",
            defaultTitle = "Clay Soil Mineral Supplement",
            defaultContent = "Put a clean shovel of fresh, red clay soil (from a clean area) in the farrowing/weaning pen. Piglets eat it to get natural iron and minerals."
        ),
        TrainingTip(
            id = 18,
            categoryKey = "cat_weaning",
            titleKey = "tip_18_title",
            contentKey = "tip_18_content",
            defaultTitle = "Clean Water Access",
            defaultContent = "Place water troughs or nipple drinkers low enough (back-height of the smallest piglet) so all weaned pigs can drink easily."
        ),
        TrainingTip(
            id = 19,
            categoryKey = "cat_weaning",
            titleKey = "tip_19_title",
            contentKey = "tip_19_content",
            defaultTitle = "Pre-Weaning Socialization",
            defaultContent = "Open the partition between two farrowing pens a week before weaning to let piglets socialize, reducing fighting when grouped later."
        ),

        // Category 2: Feeding & Nutrition (cat_feeding)
        TrainingTip(
            id = 1,
            categoryKey = "cat_feeding",
            titleKey = "tip_1_title",
            contentKey = "tip_1_content",
            defaultTitle = "Feeding Strategy",
            defaultContent = "High quality feed is essential. Ensure pigs have constant access to fresh water and balanced nutrition tailored to their growth stage."
        ),
        TrainingTip(
            id = 20,
            categoryKey = "cat_feeding",
            titleKey = "tip_20_title",
            contentKey = "tip_20_content",
            defaultTitle = "Agricultural Byproduct Boiling",
            defaultContent = "Boil agricultural byproducts like yam peels, cassava peels, and potato vines to destroy natural toxins and improve digestibility."
        ),
        TrainingTip(
            id = 21,
            categoryKey = "cat_feeding",
            titleKey = "tip_21_title",
            contentKey = "tip_21_content",
            defaultTitle = "Local Protein Sources",
            defaultContent = "Supplement commercial feed by growing high-protein forage crops like Moringa, Azolla, or sweet potato leaves on the farm."
        ),
        TrainingTip(
            id = 22,
            categoryKey = "cat_feeding",
            titleKey = "tip_22_title",
            contentKey = "tip_22_content",
            defaultTitle = "Gestation Feed Restriction",
            defaultContent = "Limit gestating sows to 2-2.5 kg of feed per day to prevent them from becoming overweight, which causes difficult births."
        ),
        TrainingTip(
            id = 23,
            categoryKey = "cat_feeding",
            titleKey = "tip_23_title",
            contentKey = "tip_23_content",
            defaultTitle = "Local Fiber for Sows",
            defaultContent = "Mix cheap local fiber sources like rice bran, wheat bran, or copra meal into sow gestation feed to keep them full and calm."
        ),
        TrainingTip(
            id = 24,
            categoryKey = "cat_feeding",
            titleKey = "tip_24_title",
            contentKey = "tip_24_content",
            defaultTitle = "Low-Cost Gravity Drinkers",
            defaultContent = "Use recycled plastic barrels or drums fitted with nipple drinkers as a cheap, clean water system for all pig pens."
        ),
        TrainingTip(
            id = 25,
            categoryKey = "cat_feeding",
            titleKey = "tip_25_title",
            contentKey = "tip_25_content",
            defaultTitle = "Rainy Season Feed Storage",
            defaultContent = "Store feed bags on wooden pallets off the concrete floor and away from walls to prevent dampness and mold growth."
        ),
        TrainingTip(
            id = 26,
            categoryKey = "cat_feeding",
            titleKey = "tip_26_title",
            contentKey = "tip_26_content",
            defaultTitle = "Boiling Kitchen Waste",
            defaultContent = "Boil hotel or kitchen food waste for 30 minutes to destroy African Swine Fever and Foot-and-Mouth disease viruses."
        ),
        TrainingTip(
            id = 27,
            categoryKey = "cat_feeding",
            titleKey = "tip_27_title",
            contentKey = "tip_27_content",
            defaultTitle = "Brewer's Spent Grain (BSG)",
            defaultContent = "Use wet brewer's grain as a cheap feed base, but limit it to 30-40% of the ration and feed it fresh to avoid mold poisoning."
        ),
        TrainingTip(
            id = 28,
            categoryKey = "cat_feeding",
            titleKey = "tip_28_title",
            contentKey = "tip_28_content",
            defaultTitle = "Charcoal for Digestion",
            defaultContent = "Place small pieces of wood charcoal in grower pens. Pigs chew it to help bind stomach toxins and reduce diarrhea."
        ),
        TrainingTip(
            id = 29,
            categoryKey = "cat_feeding",
            titleKey = "tip_29_title",
            contentKey = "tip_29_content",
            defaultTitle = "Sow Lactation Feeding",
            defaultContent = "Feed lactating sows 3 to 4 small meals throughout the day (instead of one big meal) to encourage them to eat more and produce more milk."
        ),
        TrainingTip(
            id = 30,
            categoryKey = "cat_feeding",
            titleKey = "tip_30_title",
            contentKey = "tip_30_content",
            defaultTitle = "DIY Feed Mixing",
            defaultContent = "When mixing feed on-farm, mix micro-ingredients (vitamins, salt, minerals) into a small batch of maize first before blending into the main pile."
        ),
        TrainingTip(
            id = 31,
            categoryKey = "cat_feeding",
            titleKey = "tip_31_title",
            contentKey = "tip_31_content",
            defaultTitle = "Leucaena Leaf Caution",
            defaultContent = "Limit Leucaena (ipil-ipil) leaf meal to under 5% of feed, as it contains mimosine which can cause hair loss and reproductive failure."
        ),
        TrainingTip(
            id = 32,
            categoryKey = "cat_feeding",
            titleKey = "tip_32_title",
            contentKey = "tip_32_content",
            defaultTitle = "Sow Body Condition Check",
            defaultContent = "Feed thin sows extra feed during mid-gestation, and reduce feed for fat sows to maintain a healthy body condition."
        ),
        TrainingTip(
            id = 33,
            categoryKey = "cat_feeding",
            titleKey = "tip_33_title",
            contentKey = "tip_33_content",
            defaultTitle = "Water Quality Check",
            defaultContent = "Clean drinking troughs daily. If the water looks dirty or smells bad to you, it is not safe for your pigs."
        ),

        // Category 3: Breeding & Farrowing (cat_breeding)
        TrainingTip(
            id = 3,
            categoryKey = "cat_breeding",
            titleKey = "tip_3_title",
            contentKey = "tip_3_content",
            defaultTitle = "Care for Piglets",
            defaultContent = "Keep newborn piglets warm and ensure they receive colostrum within the first hours of birth to build immunity."
        ),
        TrainingTip(
            id = 34,
            categoryKey = "cat_breeding",
            titleKey = "tip_34_title",
            contentKey = "tip_34_content",
            defaultTitle = "Hand Mating in Breeding Pen",
            defaultContent = "Bring the sow in heat to the boar's pen for breeding, rather than letting the boar run loose in the sow herd."
        ),
        TrainingTip(
            id = 35,
            categoryKey = "cat_breeding",
            titleKey = "tip_35_title",
            contentKey = "tip_35_content",
            defaultTitle = "Standing Heat Response",
            defaultContent = "Test for heat by pressing your hands firmly on the sow's back; if she stands completely still and stiffens her ears, she is ready to mate."
        ),
        TrainingTip(
            id = 36,
            categoryKey = "cat_breeding",
            titleKey = "tip_36_title",
            contentKey = "tip_36_content",
            defaultTitle = "Cheap Farrowing Room Sanitation",
            defaultContent = "Scrub farrowing pens with a mixture of agricultural lime and water (whitewashing) to disinfect walls and floors cheaply."
        ),
        TrainingTip(
            id = 37,
            categoryKey = "cat_breeding",
            titleKey = "tip_37_title",
            contentKey = "tip_37_content",
            defaultTitle = "Newborn Piglet Drying",
            defaultContent = "Dry wet newborn piglets immediately using clean, dry sacks or rags to prevent chilling and respiratory sickness."
        ),
        TrainingTip(
            id = 38,
            categoryKey = "cat_breeding",
            titleKey = "tip_38_title",
            contentKey = "tip_38_content",
            defaultTitle = "Alternative Iron Sources",
            defaultContent = "If iron injections are unavailable, rub a clean soil and iron-sulfate paste on the sow's teats daily so piglets ingest iron while nursing."
        ),
        TrainingTip(
            id = 39,
            categoryKey = "cat_breeding",
            titleKey = "tip_39_title",
            contentKey = "tip_39_content",
            defaultTitle = "DIY Crushed Piglet Protection",
            defaultContent = "Install \"guard rails\" (heavy wooden poles or metal pipes placed 20cm out from the walls and 20cm off the floor) in farrowing pens."
        ),
        TrainingTip(
            id = 40,
            categoryKey = "cat_breeding",
            titleKey = "tip_40_title",
            contentKey = "tip_40_content",
            defaultTitle = "Cheap Creep Box Warmth",
            defaultContent = "Use a simple wooden box with a small light bulb or warm bedding (like dry straw) to keep piglets warm in the creep area."
        ),
        TrainingTip(
            id = 41,
            categoryKey = "cat_breeding",
            titleKey = "tip_41_title",
            contentKey = "tip_41_content",
            defaultTitle = "Sow Udder Hygiene",
            defaultContent = "Wash the sow's udder and teats with clean warm water and a mild disinfectant before she farrows to protect newborn piglets."
        ),
        TrainingTip(
            id = 42,
            categoryKey = "cat_breeding",
            titleKey = "tip_42_title",
            contentKey = "tip_42_content",
            defaultTitle = "Colostrum Squeezing for Weak Piglets",
            defaultContent = "Hand-express colostrum from the sow's teats and feed it to weak piglets using a clean syringe or spoon."
        ),
        TrainingTip(
            id = 43,
            categoryKey = "cat_breeding",
            titleKey = "tip_43_title",
            contentKey = "tip_43_content",
            defaultTitle = "Breeding Records Calendar",
            defaultContent = "Mark the breeding date on a calendar; pregnancy lasts 114 days (3 months, 3 weeks, 3 days), allowing you to prepare the farrowing pen."
        ),
        TrainingTip(
            id = 44,
            categoryKey = "cat_breeding",
            titleKey = "tip_44_title",
            contentKey = "tip_44_content",
            defaultTitle = "Replacing Sows (Gilts)",
            defaultContent = "Keep replacement gilts from mothers that had large litter sizes, good milk production, and calm mothering instincts."
        ),
        TrainingTip(
            id = 45,
            categoryKey = "cat_breeding",
            titleKey = "tip_45_title",
            contentKey = "tip_45_content",
            defaultTitle = "Boar Stimulation for Gilts",
            defaultContent = "Walk a mature boar past the gilt pens daily to encourage them to come into heat earlier and show clear heat signs."
        ),
        TrainingTip(
            id = 46,
            categoryKey = "cat_breeding",
            titleKey = "tip_46_title",
            contentKey = "tip_46_content",
            defaultTitle = "Sow Water Access During Parturition",
            defaultContent = "Place a bucket of clean water close to the farrowing sow's head so she can drink without getting up."
        ),
        TrainingTip(
            id = 47,
            categoryKey = "cat_breeding",
            titleKey = "tip_47_title",
            contentKey = "tip_47_content",
            defaultTitle = "Gilt First Mating Age",
            defaultContent = "Wait until a gilt is at least 8 months old and on her second or third heat cycle before mating her to ensure a larger first litter."
        ),

        // Category 4: Biosecurity & Health (cat_health)
        TrainingTip(
            id = 2,
            categoryKey = "cat_health",
            titleKey = "tip_2_title",
            contentKey = "tip_2_content",
            defaultTitle = "Disease Prevention",
            defaultContent = "Biosecurity is key. Limit visitors, provide footbaths, and maintain a strict vaccination schedule with your veterinarian."
        ),
        TrainingTip(
            id = 48,
            categoryKey = "cat_health",
            titleKey = "tip_48_title",
            contentKey = "tip_48_content",
            defaultTitle = "Isolation of New Purchases",
            defaultContent = "Keep newly purchased pigs in a separate pen at least 20 meters away from your main herd for 30 days to check for sickness."
        ),
        TrainingTip(
            id = 49,
            categoryKey = "cat_health",
            titleKey = "tip_49_title",
            contentKey = "tip_49_content",
            defaultTitle = "DIY Footbath Setup",
            defaultContent = "Place a shallow plastic tub or half-cut tire filled with water and disinfectant (like chlorine or agricultural lime) at the farm gate."
        ),
        TrainingTip(
            id = 50,
            categoryKey = "cat_health",
            titleKey = "tip_50_title",
            contentKey = "tip_50_content",
            defaultTitle = "Age Segregation",
            defaultContent = "Keep growers, finishers, and breeding sows in separate pens to prevent older pigs from passing chronic diseases to younger ones."
        ),
        TrainingTip(
            id = 51,
            categoryKey = "cat_health",
            titleKey = "tip_51_title",
            contentKey = "tip_51_content",
            defaultTitle = "Visitor Restriction Policy",
            defaultContent = "Do not allow visitors (especially other pig buyers or farmers) inside your pig pens. Discuss business outside the pen area."
        ),
        TrainingTip(
            id = 52,
            categoryKey = "cat_health",
            titleKey = "tip_52_title",
            contentKey = "tip_52_content",
            defaultTitle = "Farm Perimeter Fencing",
            defaultContent = "Build a simple fence (using bamboo, wood, or wire mesh) around the piggery to keep out stray dogs, wild pigs, and neighbors."
        ),
        TrainingTip(
            id = 53,
            categoryKey = "cat_health",
            titleKey = "tip_53_title",
            contentKey = "tip_53_content",
            defaultTitle = "Safe Needle Disposal",
            defaultContent = "Store used needles in a sealed plastic bottle or container before burying or disposing of them safely to prevent accidental injuries."
        ),
        TrainingTip(
            id = 54,
            categoryKey = "cat_health",
            titleKey = "tip_54_title",
            contentKey = "tip_54_content",
            defaultTitle = "Buyer Vehicle Control",
            defaultContent = "Never allow pig buyers to drive their trucks near your pig pens. Load sold pigs at the farm gate or perimeter fence."
        ),
        TrainingTip(
            id = 55,
            categoryKey = "cat_health",
            titleKey = "tip_55_title",
            contentKey = "tip_55_content",
            defaultTitle = "Natural Parasite Control",
            defaultContent = "Feed crushed papaya seeds or pumpkin seeds to grower pigs as a cheap, natural remedy to help reduce internal worms."
        ),
        TrainingTip(
            id = 56,
            categoryKey = "cat_health",
            titleKey = "tip_56_title",
            contentKey = "tip_56_content",
            defaultTitle = "Clean Tail Docking Tools",
            defaultContent = "If docking tails, disinfect the cutting tool in boiling water or alcohol between piglets to prevent infection."
        ),
        TrainingTip(
            id = 57,
            categoryKey = "cat_health",
            titleKey = "tip_57_title",
            contentKey = "tip_57_content",
            defaultTitle = "Weekly Pen Disinfection",
            defaultContent = "Clean and spray the pig pens weekly with a cheap, effective disinfectant solution, ensuring pigs are moved or floors dry quickly."
        ),
        TrainingTip(
            id = 58,
            categoryKey = "cat_health",
            titleKey = "tip_58_title",
            contentKey = "tip_58_content",
            defaultTitle = "Boar Health Care",
            defaultContent = "Ensure your herd boar is vaccinated and de-wormed regularly, as he can spread infections to all breeding sows."
        ),
        TrainingTip(
            id = 59,
            categoryKey = "cat_health",
            titleKey = "tip_59_title",
            contentKey = "tip_59_content",
            defaultTitle = "DIY Hospital Pen",
            defaultContent = "Set aside one isolated, dry, warm pen at the far end of the farm to treat sick pigs away from the healthy herd."
        ),
        TrainingTip(
            id = 60,
            categoryKey = "cat_health",
            titleKey = "tip_60_title",
            contentKey = "tip_60_content",
            defaultTitle = "Proper Burial of Mortalities",
            defaultContent = "Bury dead pigs at least 2 meters deep, far away from water sources, and cover the carcass with agricultural lime before refilling the soil."
        ),

        // Category 5: Housing & Ventilation (cat_housing)
        TrainingTip(
            id = 61,
            categoryKey = "cat_housing",
            titleKey = "tip_61_title",
            contentKey = "tip_61_content",
            defaultTitle = "Low-Cost Pen Space",
            defaultContent = "Provide at least 1 square meter of space per finisher pig to reduce fighting, stress, and skin lesions."
        ),
        TrainingTip(
            id = 62,
            categoryKey = "cat_housing",
            titleKey = "tip_62_title",
            contentKey = "tip_62_content",
            defaultTitle = "Natural Cross-Ventilation",
            defaultContent = "Design pig houses with open sides (using wire mesh or bamboo slats) to allow natural breeze to blow through and remove odors."
        ),
        TrainingTip(
            id = 63,
            categoryKey = "cat_housing",
            titleKey = "tip_63_title",
            contentKey = "tip_63_content",
            defaultTitle = "Roof Paint for Cooling",
            defaultContent = "Paint metal roofs with white paint or cover them with palm leaves or thatch to reduce heat inside the pig house during hot days."
        ),
        TrainingTip(
            id = 64,
            categoryKey = "cat_housing",
            titleKey = "tip_64_title",
            contentKey = "tip_64_content",
            defaultTitle = "Concrete Floor Groove Pattern",
            defaultContent = "When laying concrete floors, scratch the surface with a broom to create small grooves so pigs do not slip and injure their legs."
        ),
        TrainingTip(
            id = 65,
            categoryKey = "cat_housing",
            titleKey = "tip_65_title",
            contentKey = "tip_65_content",
            defaultTitle = "Dry Sawdust Bedding",
            defaultContent = "Use dry sawdust or wood shavings on solid floors to absorb moisture, keeping pigs dry and reducing clean-up labor."
        ),
        TrainingTip(
            id = 66,
            categoryKey = "cat_housing",
            titleKey = "tip_66_title",
            contentKey = "tip_66_content",
            defaultTitle = "Ammonia Reduction",
            defaultContent = "Clean manure out of pens daily. High ammonia gas levels (indicated by burning eyes or strong smell) can damage pigs' lungs."
        ),
        TrainingTip(
            id = 67,
            categoryKey = "cat_housing",
            titleKey = "tip_67_title",
            contentKey = "tip_67_content",
            defaultTitle = "DIY Mud Wallows",
            defaultContent = "For pigs kept outdoors or in open dirt pens, provide a shaded mud wallow to help them cool down in hot weather."
        ),
        TrainingTip(
            id = 68,
            categoryKey = "cat_housing",
            titleKey = "tip_68_title",
            contentKey = "tip_68_content",
            defaultTitle = "Regular Feed Trough Cleansing",
            defaultContent = "Wash and scrub wooden or concrete feed troughs weekly to prevent soured feed from causing stomach upsets."
        ),
        TrainingTip(
            id = 69,
            categoryKey = "cat_housing",
            titleKey = "tip_69_title",
            contentKey = "tip_69_content",
            defaultTitle = "Cheap Pen Toys",
            defaultContent = "Hang plastic bottles filled with small stones or rubber tires in the pens to give pigs something to play with, reducing fighting."
        ),
        TrainingTip(
            id = 70,
            categoryKey = "cat_housing",
            titleKey = "tip_70_title",
            contentKey = "tip_70_content",
            defaultTitle = "Shade Tree Placement",
            defaultContent = "Plant leafy trees on the east and west sides of the pig house to provide natural shade and block hot afternoon sun."
        ),
        TrainingTip(
            id = 71,
            categoryKey = "cat_housing",
            titleKey = "tip_71_title",
            contentKey = "tip_71_content",
            defaultTitle = "Wet/Dry Pen Zoning",
            defaultContent = "Slope the concrete floor slightly (about 2-3%) toward the drainage channel so the resting area stays dry while the dunging area is wet."
        ),
        TrainingTip(
            id = 72,
            categoryKey = "cat_housing",
            titleKey = "tip_72_title",
            contentKey = "tip_72_content",
            defaultTitle = "Sow Pen Separation",
            defaultContent = "Provide individual gestation pens or partitions using sturdy local timber to protect pregnant sows from being bullied by others."
        ),
        TrainingTip(
            id = 73,
            categoryKey = "cat_housing",
            titleKey = "tip_73_title",
            contentKey = "tip_73_content",
            defaultTitle = "Thatch Roof Insulating",
            defaultContent = "Use coconut leaves, palm leaves, or straw thatch under corrugated metal roofs as a cheap way to insulate the building."
        ),
        TrainingTip(
            id = 74,
            categoryKey = "cat_housing",
            titleKey = "tip_74_title",
            contentKey = "tip_74_content",
            defaultTitle = "Low-Cost Fences",
            defaultContent = "Use split bamboo or wooden poles bound with wire as sturdy, cheap fencing materials for outdoor pig runs."
        ),

        // Category 6: Waste & Environment (cat_waste)
        TrainingTip(
            id = 4,
            categoryKey = "cat_waste",
            titleKey = "tip_4_title",
            contentKey = "tip_4_content",
            defaultTitle = "Waste Management",
            defaultContent = "Proper manure handling prevents odor and disease. Consider composting or using waste as fertilizer for crops."
        ),
        TrainingTip(
            id = 75,
            categoryKey = "cat_waste",
            titleKey = "tip_75_title",
            contentKey = "tip_75_content",
            defaultTitle = "DIY Compost Piles",
            defaultContent = "Pile pig manure in a designated dry corner, cover it with banana leaves or plastic, and turn it weekly with a shovel to make compost."
        ),
        TrainingTip(
            id = 76,
            categoryKey = "cat_waste",
            titleKey = "tip_76_title",
            contentKey = "tip_76_content",
            defaultTitle = "Flexible Bag Biogas",
            defaultContent = "Install a simple, low-cost plastic tubular biogas digester to treat pig manure, producing free cooking gas for the household."
        ),
        TrainingTip(
            id = 77,
            categoryKey = "cat_waste",
            titleKey = "tip_77_title",
            contentKey = "tip_77_content",
            defaultTitle = "Manure Liquid for Crops",
            defaultContent = "Collect wastewater from pen washing in a storage pit, dilute it with clean water, and use it to water bananas or crops."
        ),
        TrainingTip(
            id = 78,
            categoryKey = "cat_waste",
            titleKey = "tip_78_title",
            contentKey = "tip_78_content",
            defaultTitle = "Odor Control with Sawdust",
            defaultContent = "Spread a layer of dry sawdust on manure channels to absorb odors and dry out waste, making it easier to shovel."
        ),
        TrainingTip(
            id = 79,
            categoryKey = "cat_waste",
            titleKey = "tip_79_title",
            contentKey = "tip_79_content",
            defaultTitle = "Roof Water Guttering",
            defaultContent = "Install simple gutters (using halved bamboo or cheap PVC pipes) to divert rainwater away from manure pits and pens."
        ),
        TrainingTip(
            id = 80,
            categoryKey = "cat_waste",
            titleKey = "tip_80_title",
            contentKey = "tip_80_content",
            defaultTitle = "Simple Dead Pig Composting",
            defaultContent = "If burial is not possible, build a simple compost bin using wooden pallets to compost dead piglets in dry sawdust."
        ),
        TrainingTip(
            id = 81,
            categoryKey = "cat_waste",
            titleKey = "tip_81_title",
            contentKey = "tip_81_content",
            defaultTitle = "Sedimentation Pit",
            defaultContent = "Build a small concrete settling box in the drain channel to catch solid manure before the wastewater enters a pond or garden."
        ),
        TrainingTip(
            id = 82,
            categoryKey = "cat_waste",
            titleKey = "tip_82_title",
            contentKey = "tip_82_content",
            defaultTitle = "Living Odor Barriers",
            defaultContent = "Plant fast-growing shrubs or trees (like banana plants or lemongrass) around the piggery to block wind and reduce smells."
        ),
        TrainingTip(
            id = 83,
            categoryKey = "cat_waste",
            titleKey = "tip_83_title",
            contentKey = "tip_83_content",
            defaultTitle = "Manure Drying Platform",
            defaultContent = "Build a simple raised wooden or concrete platform to sun-dry solid manure before packing it in bags for sale as fertilizer."
        ),
        TrainingTip(
            id = 84,
            categoryKey = "cat_waste",
            titleKey = "tip_84_title",
            contentKey = "tip_84_content",
            defaultTitle = "Regular Drain Cleaning",
            defaultContent = "Clear drainage channels daily using a simple scraper to prevent stagnant water, mosquito breeding, and bad smells."
        ),
        TrainingTip(
            id = 85,
            categoryKey = "cat_waste",
            titleKey = "tip_85_title",
            contentKey = "tip_85_content",
            defaultTitle = "Manure Exchange with Neighbors",
            defaultContent = "Partner with nearby vegetable or crop farmers to exchange pig manure for crop waste, straw, or grain byproducts."
        ),
        TrainingTip(
            id = 86,
            categoryKey = "cat_waste",
            titleKey = "tip_86_title",
            contentKey = "tip_86_content",
            defaultTitle = "Soil Feeding Guidelines",
            defaultContent = "Spread composted pig manure onto crop soils at least 2 weeks before planting, rather than directly on growing vegetables."
        ),
        TrainingTip(
            id = 87,
            categoryKey = "cat_waste",
            titleKey = "tip_87_title",
            contentKey = "tip_87_content",
            defaultTitle = "Rain Cover for Manure",
            defaultContent = "Keep manure storage piles covered with a tarpaulin or thatch roof to prevent rain from washing away valuable crop nutrients."
        ),

        // Category 7: General Management & Welfare (cat_general)
        TrainingTip(
            id = 5,
            categoryKey = "cat_general",
            titleKey = "tip_5_title",
            contentKey = "tip_5_content",
            defaultTitle = "Record Keeping",
            defaultContent = "Track growth, health, and financials meticulously to identify trends and improve farm profitability."
        ),
        TrainingTip(
            id = 88,
            categoryKey = "cat_general",
            titleKey = "tip_88_title",
            contentKey = "tip_88_content",
            defaultTitle = "Sorting Boards Handling",
            defaultContent = "Use a simple square piece of plywood or plastic (sorting board) to guide and move pigs gently, rather than using sticks."
        ),
        TrainingTip(
            id = 89,
            categoryKey = "cat_general",
            titleKey = "tip_89_title",
            contentKey = "tip_89_content",
            defaultTitle = "Twice-Daily Pen Walks",
            defaultContent = "Walk slowly through the pens every morning and evening. Observe if all pigs stand up, are active, and look interested in feed."
        ),
        TrainingTip(
            id = 90,
            categoryKey = "cat_general",
            titleKey = "tip_90_title",
            contentKey = "tip_90_content",
            defaultTitle = "Simple Ear Notching",
            defaultContent = "Use a simple V-notch tool to notch piglet ears for identification, disinfecting the tool in alcohol after each piglet."
        ),
        TrainingTip(
            id = 91,
            categoryKey = "cat_general",
            titleKey = "tip_91_title",
            contentKey = "tip_91_content",
            defaultTitle = "Tape Weighing Method",
            defaultContent = "Estimate a pig's weight using a simple sewing tape measure (measure heart girth and length, then calculate weight) if scales are too expensive."
        ),
        TrainingTip(
            id = 92,
            categoryKey = "cat_general",
            titleKey = "tip_92_title",
            contentKey = "tip_92_content",
            defaultTitle = "Castration Age Limit",
            defaultContent = "Castrate male piglets before they are 7 days old. At this age, the procedure is fast, heals quickly, and causes minimal pain."
        ),
        TrainingTip(
            id = 93,
            categoryKey = "cat_general",
            titleKey = "tip_93_title",
            contentKey = "tip_93_content",
            defaultTitle = "Cool Transport Planning",
            defaultContent = "Move pigs to market during the early morning or evening when it is cool, and shade the transport vehicle with coconut leaves."
        ),
        TrainingTip(
            id = 94,
            categoryKey = "cat_general",
            titleKey = "tip_94_title",
            contentKey = "tip_94_content",
            defaultTitle = "Simple Notebook Records",
            defaultContent = "Keep a small, cheap notebook in the barn to write down breeding dates, farrowing dates, and treatments for each sow."
        ),
        TrainingTip(
            id = 95,
            categoryKey = "cat_general",
            titleKey = "tip_95_title",
            contentKey = "tip_95_content",
            defaultTitle = "SOP Posters",
            defaultContent = "Write down simple daily farm rules (like washing hands, cleaning troughs, checking water) on a board near the barn entrance."
        ),
        TrainingTip(
            id = 96,
            categoryKey = "cat_general",
            titleKey = "tip_96_title",
            contentKey = "tip_96_content",
            defaultTitle = "Animal Care Training",
            defaultContent = "Teach children or helpers on the farm to handle pigs gently, avoiding kicking or tail-pulling, which makes pigs wild and dangerous."
        ),
        TrainingTip(
            id = 97,
            categoryKey = "cat_general",
            titleKey = "tip_97_title",
            contentKey = "tip_97_content",
            defaultTitle = "Rodent Attraction Prevention",
            defaultContent = "Store feed in sealed plastic buckets or oil drums to prevent attracting mice, rats, and snakes to the farm."
        ),
        TrainingTip(
            id = 98,
            categoryKey = "cat_general",
            titleKey = "tip_98_title",
            contentKey = "tip_98_content",
            defaultTitle = "Regular Drinker Checks",
            defaultContent = "Check water drinkers twice daily by pressing them with your finger to ensure water flows freely and is not clogged with dirt."
        ),
        TrainingTip(
            id = 99,
            categoryKey = "cat_general",
            titleKey = "tip_99_title",
            contentKey = "tip_99_content",
            defaultTitle = "Clear Culling Rules",
            defaultContent = "Cull sows that fail to get pregnant after two breeding attempts with a proven boar, or those that have small litters (under 6 piglets)."
        ),
        TrainingTip(
            id = 100,
            categoryKey = "cat_general",
            titleKey = "tip_100_title",
            contentKey = "tip_100_content",
            defaultTitle = "Emergency Farm Contacts",
            defaultContent = "Keep clear emergency contacts and instructions posted near the entrance for power outages, water cuts, or fire hazards."
        )
    )
}
