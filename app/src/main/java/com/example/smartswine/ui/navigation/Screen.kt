package com.example.smartswine.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object HerdData : Screen("herd_data")
    object Feed : Screen("feed")
    object IngredientList : Screen("ingredient_list")
    object AddIngredient : Screen("add_ingredient")
    object EditIngredient : Screen("edit_ingredient/{ingredientId}") {
        fun createRoute(ingredientId: String) = "edit_ingredient/$ingredientId"
    }
    object ProductionActivities : Screen("production_activities")
    object Financials : Screen("financials")
    object HumanResource : Screen("human_resource")
    object MarketAccess : Screen("market_access")
    object DiseaseFinder : Screen("disease_finder")
    object WeightChecker : Screen("weight_checker")
    object Training : Screen("training")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object EditProfile : Screen("edit_profile")
    object ArchivedPigs : Screen("archived_pigs")
    object TermsOfService : Screen("terms_of_service")
    object FeedCalculationResult : Screen("feed_calculation_result")
    object FeedFormulationResult : Screen("feed_formulation_result")
    object PigProfile : Screen("pig_profile/{pigId}") {
        fun createRoute(pigId: String) = "pig_profile/$pigId"
    }
    object Paywall : Screen("paywall")
    object AdminPanel : Screen("admin_panel")
}
