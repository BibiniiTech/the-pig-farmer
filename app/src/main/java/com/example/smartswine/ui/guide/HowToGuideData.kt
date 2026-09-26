package com.example.smartswine.ui.guide

import com.example.smartswine.ui.navigation.Screen

enum class GuideCategory(val labelKey: String) {
    ALL("guide_category_all"),
    HERD("herd_data"),
    FEED("feed"),
    BREEDING("breeding_mating"),
    FINANCIALS("financials"),
    HEALTH("guide_category_health"),
    TOOLS("guide_category_tools")
}

data class GuideStep(
    val stepNumber: Int,
    val titleKey: String,
    val descriptionKey: String,
    val tipKey: String? = null
)

data class GuideTopic(
    val id: String,
    val titleKey: String,
    val subtitleKey: String,
    val category: GuideCategory,
    val iconName: String,
    val actionLabelKey: String,
    val actionRoute: String,
    val steps: List<GuideStep>
)

object HowToGuideData {
    val topics: List<GuideTopic> = listOf(
        GuideTopic(
            id = "herd_management",
            titleKey = "guide_herd_title",
            subtitleKey = "guide_herd_subtitle",
            category = GuideCategory.HERD,
            iconName = "Pets",
            actionLabelKey = "guide_action_open_herd",
            actionRoute = Screen.HerdData.createRoute(showAdd = true),
            steps = listOf(
                GuideStep(
                    stepNumber = 1,
                    titleKey = "guide_herd_step1_title",
                    descriptionKey = "guide_herd_step1_desc",
                    tipKey = "guide_herd_step1_tip"
                ),
                GuideStep(
                    stepNumber = 2,
                    titleKey = "guide_herd_step2_title",
                    descriptionKey = "guide_herd_step2_desc",
                    tipKey = "guide_herd_step2_tip"
                ),
                GuideStep(
                    stepNumber = 3,
                    titleKey = "guide_herd_step3_title",
                    descriptionKey = "guide_herd_step3_desc",
                    tipKey = "guide_herd_step3_tip"
                ),
                GuideStep(
                    stepNumber = 4,
                    titleKey = "guide_herd_step4_title",
                    descriptionKey = "guide_herd_step4_desc",
                    tipKey = "guide_herd_step4_tip"
                )
            )
        ),
        GuideTopic(
            id = "feed_formulation",
            titleKey = "guide_feed_title",
            subtitleKey = "guide_feed_subtitle",
            category = GuideCategory.FEED,
            iconName = "Science",
            actionLabelKey = "guide_action_open_feed",
            actionRoute = Screen.Feed.createRoute(showCalculator = false, showFormulator = true),
            steps = listOf(
                GuideStep(
                    stepNumber = 1,
                    titleKey = "guide_feed_step1_title",
                    descriptionKey = "guide_feed_step1_desc",
                    tipKey = "guide_feed_step1_tip"
                ),
                GuideStep(
                    stepNumber = 2,
                    titleKey = "guide_feed_step2_title",
                    descriptionKey = "guide_feed_step2_desc",
                    tipKey = "guide_feed_step2_tip"
                ),
                GuideStep(
                    stepNumber = 3,
                    titleKey = "guide_feed_step3_title",
                    descriptionKey = "guide_feed_step3_desc",
                    tipKey = "guide_feed_step3_tip"
                ),
                GuideStep(
                    stepNumber = 4,
                    titleKey = "guide_feed_step4_title",
                    descriptionKey = "guide_feed_step4_desc",
                    tipKey = "guide_feed_step4_tip"
                )
            )
        ),
        GuideTopic(
            id = "breeding_and_activities",
            titleKey = "guide_breeding_title",
            subtitleKey = "guide_breeding_subtitle",
            category = GuideCategory.BREEDING,
            iconName = "Favorite",
            actionLabelKey = "guide_action_open_activities",
            actionRoute = Screen.ProductionActivities.route,
            steps = listOf(
                GuideStep(
                    stepNumber = 1,
                    titleKey = "guide_breeding_step1_title",
                    descriptionKey = "guide_breeding_step1_desc",
                    tipKey = "guide_breeding_step1_tip"
                ),
                GuideStep(
                    stepNumber = 2,
                    titleKey = "guide_breeding_step2_title",
                    descriptionKey = "guide_breeding_step2_desc",
                    tipKey = "guide_breeding_step2_tip"
                ),
                GuideStep(
                    stepNumber = 3,
                    titleKey = "guide_breeding_step3_title",
                    descriptionKey = "guide_breeding_step3_desc",
                    tipKey = "guide_breeding_step3_tip"
                ),
                GuideStep(
                    stepNumber = 4,
                    titleKey = "guide_breeding_step4_title",
                    descriptionKey = "guide_breeding_step4_desc",
                    tipKey = "guide_breeding_step4_tip"
                )
            )
        ),
        GuideTopic(
            id = "financials_tracking",
            titleKey = "guide_finances_title",
            subtitleKey = "guide_finances_subtitle",
            category = GuideCategory.FINANCIALS,
            iconName = "AttachMoney",
            actionLabelKey = "guide_action_open_financials",
            actionRoute = Screen.Financials.createRoute(showAdd = true),
            steps = listOf(
                GuideStep(
                    stepNumber = 1,
                    titleKey = "guide_finances_step1_title",
                    descriptionKey = "guide_finances_step1_desc",
                    tipKey = "guide_finances_step1_tip"
                ),
                GuideStep(
                    stepNumber = 2,
                    titleKey = "guide_finances_step2_title",
                    descriptionKey = "guide_finances_step2_desc",
                    tipKey = "guide_finances_step2_tip"
                ),
                GuideStep(
                    stepNumber = 3,
                    titleKey = "guide_finances_step3_title",
                    descriptionKey = "guide_finances_step3_desc",
                    tipKey = "guide_finances_step3_tip"
                )
            )
        ),
        GuideTopic(
            id = "disease_finder",
            titleKey = "guide_health_title",
            subtitleKey = "guide_health_subtitle",
            category = GuideCategory.HEALTH,
            iconName = "LocalHospital",
            actionLabelKey = "guide_action_open_disease_finder",
            actionRoute = Screen.DiseaseFinder.route,
            steps = listOf(
                GuideStep(
                    stepNumber = 1,
                    titleKey = "guide_health_step1_title",
                    descriptionKey = "guide_health_step1_desc",
                    tipKey = "guide_health_step1_tip"
                ),
                GuideStep(
                    stepNumber = 2,
                    titleKey = "guide_health_step2_title",
                    descriptionKey = "guide_health_step2_desc",
                    tipKey = "guide_health_step2_tip"
                ),
                GuideStep(
                    stepNumber = 3,
                    titleKey = "guide_health_step3_title",
                    descriptionKey = "guide_health_step3_desc",
                    tipKey = "guide_health_step3_tip"
                )
            )
        ),
        GuideTopic(
            id = "weight_checker",
            titleKey = "guide_weight_title",
            subtitleKey = "guide_weight_subtitle",
            category = GuideCategory.TOOLS,
            iconName = "Straighten",
            actionLabelKey = "guide_action_open_weight_checker",
            actionRoute = Screen.WeightChecker.route,
            steps = listOf(
                GuideStep(
                    stepNumber = 1,
                    titleKey = "guide_weight_step1_title",
                    descriptionKey = "guide_weight_step1_desc",
                    tipKey = "guide_weight_step1_tip"
                ),
                GuideStep(
                    stepNumber = 2,
                    titleKey = "guide_weight_step2_title",
                    descriptionKey = "guide_weight_step2_desc",
                    tipKey = "guide_weight_step2_tip"
                ),
                GuideStep(
                    stepNumber = 3,
                    titleKey = "guide_weight_step3_title",
                    descriptionKey = "guide_weight_step3_desc",
                    tipKey = "guide_weight_step3_tip"
                )
            )
        ),
        GuideTopic(
            id = "human_resources",
            titleKey = "guide_hr_title",
            subtitleKey = "guide_hr_subtitle",
            category = GuideCategory.HERD,
            iconName = "People",
            actionLabelKey = "guide_action_open_hr",
            actionRoute = Screen.Dashboard.createRoute(section = "human_resources"),
            steps = listOf(
                GuideStep(
                    stepNumber = 1,
                    titleKey = "guide_hr_step1_title",
                    descriptionKey = "guide_hr_step1_desc",
                    tipKey = "guide_hr_step1_tip"
                ),
                GuideStep(
                    stepNumber = 2,
                    titleKey = "guide_hr_step2_title",
                    descriptionKey = "guide_hr_step2_desc",
                    tipKey = "guide_hr_step2_tip"
                ),
                GuideStep(
                    stepNumber = 3,
                    titleKey = "guide_hr_step3_title",
                    descriptionKey = "guide_hr_step3_desc",
                    tipKey = "guide_hr_step3_tip"
                )
            )
        ),
        GuideTopic(
            id = "offline_sync",
            titleKey = "guide_offline_title",
            subtitleKey = "guide_offline_subtitle",
            category = GuideCategory.TOOLS,
            iconName = "CloudSync",
            actionLabelKey = "guide_action_open_dashboard",
            actionRoute = Screen.Dashboard.route,
            steps = listOf(
                GuideStep(
                    stepNumber = 1,
                    titleKey = "guide_offline_step1_title",
                    descriptionKey = "guide_offline_step1_desc",
                    tipKey = "guide_offline_step1_tip"
                ),
                GuideStep(
                    stepNumber = 2,
                    titleKey = "guide_offline_step2_title",
                    descriptionKey = "guide_offline_step2_desc",
                    tipKey = "guide_offline_step2_tip"
                )
            )
        )
    )
}
