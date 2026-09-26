package com.example.smartswine.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard") {
        const val routeWithArgs = "dashboard?section={section}&subOption={subOption}&tag={tag}"
        fun createRoute(section: String? = null, subOption: String? = null, tag: String? = null): String {
            val params = mutableListOf<String>()
            if (!section.isNullOrBlank()) params.add("section=$section")
            if (!subOption.isNullOrBlank()) params.add("subOption=$subOption")
            if (!tag.isNullOrBlank()) {
                val encodedTag = try {
                    java.net.URLEncoder.encode(tag, "UTF-8")
                } catch (e: Exception) {
                    tag
                }
                params.add("tag=$encodedTag")
            }
            return if (params.isNotEmpty()) "dashboard?${params.joinToString("&")}" else "dashboard"
        }
    }
    object HerdData : Screen("herd_data") {
        const val routeWithArgs = "herd_data?showAdd={showAdd}"
        fun createRoute(showAdd: Boolean = false) =
            if (showAdd) "herd_data?showAdd=true" else "herd_data"
    }
    object Feed : Screen("feed") {
        const val routeWithArgs = "feed?showCalculator={showCalculator}&showFormulator={showFormulator}"
        fun createRoute(showCalculator: Boolean = false, showFormulator: Boolean = false) =
            "feed?showCalculator=$showCalculator&showFormulator=$showFormulator"
    }
    object IngredientList : Screen("ingredient_list")
    object AddIngredient : Screen("add_ingredient")
    object EditIngredient : Screen("edit_ingredient/{ingredientId}") {
        fun createRoute(ingredientId: String) = "edit_ingredient/$ingredientId"
    }
    object ProductionActivities : Screen("production_activities") {
        const val routeWithArgs = "production_activities?initialActivity={initialActivity}&pigId={pigId}"
        fun createRoute(initialActivity: String? = null, pigId: String? = null): String {
            val params = mutableListOf<String>()
            if (!initialActivity.isNullOrBlank()) {
                params.add("initialActivity=${java.net.URLEncoder.encode(initialActivity, "UTF-8")}")
            }
            if (!pigId.isNullOrBlank()) {
                params.add("pigId=${java.net.URLEncoder.encode(pigId, "UTF-8")}")
            }
            return if (params.isNotEmpty()) "production_activities?${params.joinToString("&")}" else "production_activities"
        }
    }
    object Financials : Screen("financials") {
        const val routeWithArgs = "financials?showAdd={showAdd}"
        fun createRoute(showAdd: Boolean = false) =
            if (showAdd) "financials?showAdd=true" else "financials"
    }
    object HumanResource : Screen("human_resource") {
        const val routeWithArgs = "human_resource?showAdd={showAdd}"
        fun createRoute(showAdd: Boolean = false) =
            Dashboard.createRoute(section = "human_resources", subOption = if (showAdd) "add" else null)
    }
    object MarketAccess : Screen("market_access")
    object DiseaseFinder : Screen("disease_finder")
    object WeightChecker : Screen("weight_checker") {
        const val routeWithArgs = "weight_checker?tag={tag}"
        fun createRoute(tag: String? = null) = if (!tag.isNullOrBlank()) "weight_checker?tag=$tag" else "weight_checker"
    }
    object Training : Screen("training")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object EditProfile : Screen("edit_profile")
    object ArchivedPigs : Screen("archived_pigs")
    object TermsOfService : Screen("terms_of_service")
    object FeedCalculationResult : Screen("feed_calculation_result")
    object FeedFormulationResult : Screen("feed_formulation_result")
    object AnalyzeFeed : Screen("analyze_feed")
    object PigProfile : Screen("pig_profile/{pigId}") {
        fun createRoute(pigId: String) = "pig_profile/$pigId"
    }
    object Paywall : Screen("paywall")
    object AdminPanel : Screen("admin_panel")
    object HowToGuide : Screen("how_to_guide")
}
