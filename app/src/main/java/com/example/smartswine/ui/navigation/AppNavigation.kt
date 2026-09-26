package com.example.smartswine.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.smartswine.ui.auth.AuthViewModel
import com.example.smartswine.ui.auth.UserProfile
import com.example.smartswine.ui.dashboard.DashboardScreen
import com.example.smartswine.ui.dashboard.DashboardViewModel
import com.example.smartswine.ui.feed.*
import com.example.smartswine.ui.financials.FinancialViewModel
import com.example.smartswine.ui.financials.FinancialsScreen
import com.example.smartswine.ui.herd.*
import com.example.smartswine.ui.hr.HumanResourceViewModel
import com.example.smartswine.ui.market.AdminPanelScreen
import com.example.smartswine.ui.modules.ModulePlaceholderScreen
import com.example.smartswine.ui.production.ProductionActivitiesScreen
import com.example.smartswine.ui.production.ProductionViewModel
import com.example.smartswine.ui.guide.HowToGuideScreen
import com.example.smartswine.ui.settings.*
import com.example.smartswine.ui.theme.ThemeViewModel
import com.example.smartswine.utils.LanguageViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    profile: UserProfile?,
    dashboardViewModel: DashboardViewModel,
    herdViewModel: HerdViewModel,
    feedViewModel: FeedViewModel,
    productionViewModel: ProductionViewModel,
    financialViewModel: FinancialViewModel,
    hrViewModel: HumanResourceViewModel,
    authViewModel: AuthViewModel,
    themeViewModel: ThemeViewModel,
    languageViewModel: LanguageViewModel,
    onOpenDrawer: () -> Unit = {}
) {
    val isFinancialsRestricted by authViewModel.isFinancialsRestricted.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier,
    ) {
        composable(
            route = Screen.Dashboard.routeWithArgs,
            arguments = listOf(
                navArgument("section") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("subOption") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("tag") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val targetSection = backStackEntry.arguments?.getString("section")
            val targetSubOption = backStackEntry.arguments?.getString("subOption")
            val targetTag = backStackEntry.arguments?.getString("tag")
            val tasks by dashboardViewModel.tasks.collectAsStateWithLifecycle()
            val groupedTasks by dashboardViewModel.groupedTasks.collectAsStateWithLifecycle()
            val taskAlerts by dashboardViewModel.taskAlerts.collectAsStateWithLifecycle()
            val weightAlerts by dashboardViewModel.weightAlerts.collectAsStateWithLifecycle()
            val stockAlerts by dashboardViewModel.stockAlerts.collectAsStateWithLifecycle()
            val totalNotificationCount by dashboardViewModel.totalNotificationCount.collectAsStateWithLifecycle()
            val error by dashboardViewModel.error.collectAsStateWithLifecycle()
            val isRefreshing by dashboardViewModel.isRefreshing.collectAsStateWithLifecycle()

            val allPigs by herdViewModel.allPigsIncludingArchived.collectAsStateWithLifecycle()
            val ingredients by feedViewModel.ingredients.collectAsStateWithLifecycle()
            val requirements by feedViewModel.nutritionalRequirements.collectAsStateWithLifecycle()
            val isFormulating by feedViewModel.isFormulating.collectAsStateWithLifecycle()
            val stats by herdViewModel.stats.collectAsStateWithLifecycle()
            val sowTags by herdViewModel.sowTags.collectAsStateWithLifecycle()
            val boarTags by herdViewModel.boarTags.collectAsStateWithLifecycle()
            val financialRecords by financialViewModel.records.collectAsStateWithLifecycle()
            val hrStaff by hrViewModel.staff.collectAsStateWithLifecycle()

            DashboardScreen(
                targetSection = targetSection,
                targetSubOption = targetSubOption,
                targetTag = targetTag,
                profile = profile,
                tasks = tasks,
                groupedTasks = groupedTasks,
                taskAlerts = taskAlerts,
                weightAlerts = weightAlerts,
                stockAlerts = stockAlerts,
                totalNotificationCount = totalNotificationCount,
                allPigs = allPigs,
                financialRecords = financialRecords,
                staff = hrStaff,
                isFinancialsRestricted = isFinancialsRestricted,
                onCompleteTask = { dashboardViewModel.completeTask(it) },
                onDeleteTask = { dashboardViewModel.deleteTask(it) },
                onSnoozeTasks = { tasks, hours -> dashboardViewModel.snoozeTasks(tasks, hours) },
                onDeleteTaskForPig = { tasks, pigIdentifier -> dashboardViewModel.deleteTaskForPig(tasks, pigIdentifier) },
                onSnoozeWeightAlert = { pigId, days -> dashboardViewModel.snoozeWeightAlert(pigId, days) },
                onDismissWeightAlert = { pigId -> dashboardViewModel.dismissWeightAlert(pigId) },
                onSnoozeStockAlert = { itemId, hours -> dashboardViewModel.snoozeStockAlert(itemId, hours) },
                onDismissStockAlert = { itemId -> dashboardViewModel.dismissStockAlert(itemId) },
                onNavigateTo = { route -> navController.navigate(route) },
                error = error,
                onClearError = { dashboardViewModel.clearError() },
                onLogHealthActivity = { pigIds, record, heat, check, pregnancy, details ->
                    productionViewModel.logHealthActivity(
                        pigIds, record, heat, check, pregnancy, details
                    )
                },
                onAddPig = { formData -> herdViewModel.addPigsFromForm(formData) },
                onAddStaff = { member -> hrViewModel.addStaff(member) },
                onUpdateStaff = { member -> hrViewModel.updateStaff(member) },
                onArchiveStaff = { member -> hrViewModel.archiveStaff(member) },
                onPaySalary = { staffMember, month, notes, bonus, deduction, adjDesc ->
                    hrViewModel.logSalaryPayment(staffMember, month, notes, bonus, deduction, adjDesc)
                },
                onUpdatePigWeight = { id, weight -> herdViewModel.updatePigWeight(id, weight) },
                ingredients = ingredients,
                nutritionalRequirements = requirements,
                isFormulating = isFormulating,
                herdStats = stats,
                onCalculateRequirements = { data ->
                    feedViewModel.calculateRequirements(data)
                    navController.navigate(Screen.FeedCalculationResult.route)
                },
                onFormulateFeed = { name, ingredientIds ->
                    feedViewModel.formulateFeed(name, ingredientIds)
                    navController.navigate(Screen.FeedFormulationResult.route)
                },
                onRefresh = { dashboardViewModel.refresh() },
                isRefreshing = isRefreshing,
                sowTags = sowTags,
                boarTags = boarTags,
                onOpenDrawer = onOpenDrawer
            )
        }

        // Herd Management
        composable(
            route = Screen.HerdData.routeWithArgs,
            arguments = listOf(
                navArgument("showAdd") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val showAdd = backStackEntry.arguments?.getBoolean("showAdd") ?: false
            HerdDataScreen(
                viewModel = herdViewModel,
                initiallyShowAdd = showAdd,
                onNavigateToPigProfile = { pigId ->
                    navController.navigate(Screen.PigProfile.createRoute(pigId))
                },
                onNavigateToArchived = {
                    navController.navigate(Screen.ArchivedPigs.route)
                },
                onNavigateToPaywall = {
                    navController.navigate(Screen.Paywall.route)
                }
            ) { navController.popBackStack() }
        }
        composable(
            route = Screen.PigProfile.route,
            arguments = listOf(navArgument("pigId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val pigId = backStackEntry.arguments?.getString("pigId") ?: return@composable
            PigProfileScreen(
                pigId = pigId,
                viewModel = herdViewModel,
                onNavigateToPaywall = {
                    navController.navigate(Screen.Paywall.route)
                },
                onBack = { navController.popBackStack() },
            )
        }

        // Feed Management
        navigation(startDestination = Screen.Feed.route, route = "feed_management") {
            composable(
                route = Screen.Feed.routeWithArgs,
                arguments = listOf(
                    navArgument("showCalculator") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("showFormulator") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val showCalculator = backStackEntry.arguments?.getBoolean("showCalculator") ?: false
                val showFormulator = backStackEntry.arguments?.getBoolean("showFormulator") ?: false
                FeedScreen(
                    viewModel = feedViewModel,
                    herdViewModel = herdViewModel,
                    onNavigateToPaywall = {
                        navController.navigate(Screen.Paywall.route)
                    },
                    onBack = { navController.popBackStack() },
                    onNavigateTo = { route -> navController.navigate(route) },
                    initiallyShowCalculator = showCalculator,
                    initiallyShowFormulator = showFormulator
                )
            }
            composable(Screen.FeedCalculationResult.route) {
                FeedCalculationResultScreen(
                    viewModel = feedViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToPaywall = {
                        navController.navigate(Screen.Paywall.route)
                    },
                    onNavigateToAnalyze = {
                        navController.navigate(Screen.AnalyzeFeed.route)
                    }
                )
            }
            composable(Screen.FeedFormulationResult.route) {
                FeedFormulationResultScreen(
                    viewModel = feedViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AnalyzeFeed.route) {
                AnalyzeFeedScreen(
                    viewModel = feedViewModel,
                    onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.IngredientList.route) {
                IngredientListScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateTo = { route -> navController.navigate(route) },
                    viewModel = feedViewModel
                )
            }
            composable(Screen.AddIngredient.route) {
                AddEditIngredientScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = feedViewModel
                )
            }
            composable(
                route = Screen.EditIngredient.route,
                arguments = listOf(navArgument("ingredientId") { type = NavType.StringType })
            ) { backStackEntry ->
                val ingredientId = backStackEntry.arguments?.getString("ingredientId")
                AddEditIngredientScreen(
                    ingredientId = ingredientId,
                    onBack = { navController.popBackStack() },
                    viewModel = feedViewModel
                )
            }
        }
        composable(
            route = Screen.ProductionActivities.routeWithArgs,
            arguments = listOf(
                navArgument("initialActivity") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("pigId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val initialActivity = backStackEntry.arguments?.getString("initialActivity")
            val decodedActivity = initialActivity?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            val initialPigId = backStackEntry.arguments?.getString("pigId")
            val decodedPigId = initialPigId?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            ProductionActivitiesScreen(
                viewModel = productionViewModel,
                herdViewModel = herdViewModel,
                initialActivity = decodedActivity,
                initialPigId = decodedPigId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Financials.routeWithArgs,
            arguments = listOf(
                navArgument("showAdd") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val showAdd = backStackEntry.arguments?.getBoolean("showAdd") ?: false
            if (isFinancialsRestricted) {
                ModulePlaceholderScreen("Restricted Access: Financial records are reserved for farm managers and owners.") {
                    navController.popBackStack()
                }
            } else {
                FinancialsScreen(
                    viewModel = financialViewModel,
                    initialShowAdd = showAdd,
                    userCountry = profile?.country ?: "",
                    onNavigateToPaywall = {
                        navController.navigate(Screen.Paywall.route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(
            route = Screen.HumanResource.routeWithArgs,
            arguments = listOf(
                navArgument("showAdd") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val showAdd = backStackEntry.arguments?.getBoolean("showAdd") ?: false
            if (isFinancialsRestricted) {
                ModulePlaceholderScreen("Restricted Access: Human resource records are reserved for farm managers and owners.") {
                    navController.popBackStack()
                }
            } else {
                LaunchedEffect(showAdd) {
                    navController.navigate(Screen.Dashboard.createRoute("human_resources", if (showAdd) "add" else null)) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        }
        composable(Screen.MarketAccess.route) {
            LaunchedEffect(Unit) {
                navController.navigate(Screen.Dashboard.createRoute("market")) {
                    popUpTo(Screen.Dashboard.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
        composable(Screen.AdminPanel.route) {
            AdminPanelScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.DiseaseFinder.route) {
            LaunchedEffect(Unit) {
                navController.navigate(Screen.Dashboard.createRoute("symptoms_analyzer")) {
                    popUpTo(Screen.Dashboard.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
        composable(
            route = Screen.WeightChecker.routeWithArgs,
            arguments = listOf(
                navArgument("tag") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val tag = backStackEntry.arguments?.getString("tag")
            LaunchedEffect(tag) {
                navController.navigate(Screen.Dashboard.createRoute("weight_checker", "tape", tag)) {
                    popUpTo(Screen.Dashboard.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
        composable(Screen.Training.route) {
            LaunchedEffect(Unit) {
                navController.navigate(Screen.Dashboard.createRoute("training")) {
                    popUpTo(Screen.Dashboard.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
        composable(Screen.Profile.route) {
            ModulePlaceholderScreen("Profile") { navController.popBackStack() }
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onNavigateToTerms = { navController.navigate(Screen.TermsOfService.route) },
                onNavigateToGuide = { navController.navigate(Screen.HowToGuide.route) },
                themeViewModel = themeViewModel,
                settingsViewModel = SettingsViewModel.getInstance(),
                languageViewModel = languageViewModel
            )
        }
        composable(Screen.TermsOfService.route) {
            TermsOfServiceScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                authViewModel = authViewModel
            )
        }
        composable(Screen.ArchivedPigs.route) {
            val archivedPigs by herdViewModel.archivedPigs.collectAsStateWithLifecycle()
            ArchivedPigsPage(
                pigs = archivedPigs,
                onBack = { navController.popBackStack() },
                onNavigateToPigProfile = { pigId ->
                    navController.navigate(Screen.PigProfile.createRoute(pigId))
                }
            )
        }
        composable(Screen.Paywall.route) {
            PaywallScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.HowToGuide.route) {
            HowToGuideScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRoute = { targetRoute ->
                    navController.navigate(targetRoute)
                }
            )
        }
    }
}
