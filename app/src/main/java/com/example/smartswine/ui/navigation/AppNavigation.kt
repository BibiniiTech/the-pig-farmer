package com.example.smartswine.ui.navigation

import androidx.compose.runtime.Composable
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
import com.example.smartswine.ui.diseasefinder.DiseaseFinderScreen
import com.example.smartswine.ui.feed.*
import com.example.smartswine.ui.financials.FinancialViewModel
import com.example.smartswine.ui.financials.FinancialsScreen
import com.example.smartswine.ui.herd.*
import com.example.smartswine.ui.hr.HumanResourceScreen
import com.example.smartswine.ui.hr.HumanResourceViewModel
import com.example.smartswine.ui.market.AdminPanelScreen
import com.example.smartswine.ui.market.MarketScreen
import com.example.smartswine.ui.modules.ModulePlaceholderScreen
import com.example.smartswine.ui.production.ProductionActivitiesScreen
import com.example.smartswine.ui.production.ProductionViewModel
import com.example.smartswine.ui.settings.*
import com.example.smartswine.ui.theme.ThemeViewModel
import com.example.smartswine.ui.training.TrainingScreen
import com.example.smartswine.ui.weight.WeightCheckerScreen
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
    languageViewModel: LanguageViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier,
    ) {
        composable(Screen.Dashboard.route) {
            val tasks by dashboardViewModel.tasks.collectAsStateWithLifecycle()
            val groupedTasks by dashboardViewModel.groupedTasks.collectAsStateWithLifecycle()
            val error by dashboardViewModel.error.collectAsStateWithLifecycle()
            val isRefreshing by dashboardViewModel.isRefreshing.collectAsStateWithLifecycle()

            val allPigs by herdViewModel.allPigsIncludingArchived.collectAsStateWithLifecycle()
            val ingredients by feedViewModel.ingredients.collectAsStateWithLifecycle()
            val requirements by feedViewModel.nutritionalRequirements.collectAsStateWithLifecycle()
            val isFormulating by feedViewModel.isFormulating.collectAsStateWithLifecycle()
            val stats by herdViewModel.stats.collectAsStateWithLifecycle()
            val sowTags by herdViewModel.sowTags.collectAsStateWithLifecycle()
            val boarTags by herdViewModel.boarTags.collectAsStateWithLifecycle()

            DashboardScreen(
                profile = profile,
                tasks = tasks,
                groupedTasks = groupedTasks,
                allPigs = allPigs,
                onCompleteTask = { dashboardViewModel.completeTask(it) },
                onDeleteTask = { dashboardViewModel.deleteTask(it) },
                onNavigateTo = { route -> navController.navigate(route) },
                error = error,
                onClearError = { dashboardViewModel.clearError() },
                onLogHealthActivity = { pigIds, record, heat, check, pregnancy, details ->
                    productionViewModel.logHealthActivity(
                        pigIds, record, heat, check, pregnancy, details
                    )
                },
                onAddPig = { formData -> herdViewModel.addPigsFromForm(formData) },
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
                boarTags = boarTags
            )
        }

        // Herd Management
        composable(Screen.HerdData.route) {
            HerdDataScreen(
                viewModel = herdViewModel,
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
                route = Screen.Feed.route + "?showCalculator={showCalculator}",
                arguments = listOf(
                    navArgument("showCalculator") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val showCalculator = backStackEntry.arguments?.getBoolean("showCalculator") ?: false
                FeedScreen(
                    viewModel = feedViewModel,
                    herdViewModel = herdViewModel,
                    onNavigateToPaywall = {
                        navController.navigate(Screen.Paywall.route)
                    },
                    onBack = { navController.popBackStack() },
                    onNavigateTo = { route -> navController.navigate(route) },
                    initiallyShowCalculator = showCalculator
                )
            }
            composable(Screen.FeedCalculationResult.route) {
                FeedCalculationResultScreen(
                    viewModel = feedViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToPaywall = {
                        navController.navigate(Screen.Paywall.route)
                    }
                )
            }
            composable(Screen.FeedFormulationResult.route) {
                FeedFormulationResultScreen(
                    viewModel = feedViewModel,
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
        composable(Screen.ProductionActivities.route) {
            ProductionActivitiesScreen(
                viewModel = productionViewModel,
                herdViewModel = herdViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Financials.route) {
            FinancialsScreen(
                viewModel = financialViewModel,
                onNavigateToPaywall = {
                    navController.navigate(Screen.Paywall.route)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.HumanResource.route) {
            HumanResourceScreen(
                viewModel = hrViewModel,
                onNavigateToPaywall = {
                    navController.navigate(Screen.Paywall.route)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.MarketAccess.route) {
            MarketScreen(
                userCountry = profile?.country ?: "",
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AdminPanel.route) {
            AdminPanelScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.DiseaseFinder.route) {
            DiseaseFinderScreen(
                onNavigateToPaywall = {
                    navController.navigate(Screen.Paywall.route)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.WeightChecker.route) {
            val pigs by herdViewModel.pigs.collectAsStateWithLifecycle()
            WeightCheckerScreen(
                onBack = { navController.popBackStack() },
                pigs = pigs,
                onUpdatePigWeight = { id, weight -> herdViewModel.updatePigWeight(id, weight) }
            )
        }
        composable(Screen.Training.route) {
            TrainingScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Profile.route) {
            ModulePlaceholderScreen("Profile") { navController.popBackStack() }
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onNavigateToTerms = { navController.navigate(Screen.TermsOfService.route) },
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
    }
}
