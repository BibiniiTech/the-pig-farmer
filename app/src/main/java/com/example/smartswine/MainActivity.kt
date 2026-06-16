package com.example.smartswine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartswine.ui.theme.ThemeViewModel
import com.example.smartswine.ui.theme.SmartSwineTheme
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartswine.ui.auth.AuthScreen
import com.example.smartswine.ui.auth.AuthViewModel
import com.example.smartswine.ui.auth.CompleteProfileScreen
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import com.example.smartswine.ui.dashboard.DashboardScreen
import com.example.smartswine.ui.dashboard.DashboardViewModel
import com.example.smartswine.ui.herd.HerdDataScreen
import com.example.smartswine.ui.herd.HerdViewModel
import com.example.smartswine.ui.herd.PigProfileScreen
import com.example.smartswine.ui.modules.ModulePlaceholderScreen
import com.example.smartswine.ui.feed.AddEditIngredientScreen
import com.example.smartswine.ui.feed.FeedCalculationResultScreen
import com.example.smartswine.ui.feed.FeedFormulationResultScreen
import com.example.smartswine.ui.feed.FeedScreen
import com.example.smartswine.ui.feed.FeedViewModel
import com.example.smartswine.ui.feed.IngredientListScreen
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.ui.production.ProductionActivitiesScreen
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.smartswine.ui.production.ProductionViewModel
import com.example.smartswine.ui.settings.SettingsScreen
import com.example.smartswine.ui.settings.EditProfileScreen
import com.example.smartswine.ui.settings.TermsOfServiceScreen
import com.example.smartswine.ui.settings.PaywallScreen
import com.example.smartswine.ui.herd.ArchivedPigsPage
import com.example.smartswine.ui.financials.FinancialsScreen
import com.example.smartswine.ui.financials.FinancialViewModel
import com.example.smartswine.ui.hr.HumanResourceScreen
import com.example.smartswine.ui.hr.HumanResourceViewModel
import com.example.smartswine.ui.market.MarketScreen
import com.example.smartswine.ui.market.AdminPanelScreen
import com.example.smartswine.ui.diseasefinder.DiseaseFinderScreen
import com.example.smartswine.ui.weight.WeightCheckerScreen
import com.example.smartswine.ui.training.TrainingScreen
import android.Manifest
import android.util.Log
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bibiniitech.smartswine.BuildConfig
import com.example.smartswine.ui.settings.SettingsViewModel
import com.example.smartswine.utils.*
import com.bibiniitech.smartswine.R
import com.example.smartswine.data.FirestoreManager
import com.example.smartswine.data.BillingManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission if needed (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        // Schedule notifications if enabled
        val settingsViewModel = SettingsViewModel.getInstance()
        if (settingsViewModel.notificationsEnabled.value) {
            NotificationWorker.schedule(this)
        }

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val languageViewModel: LanguageViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
            val currentLanguage by languageViewModel.currentLanguage.collectAsStateWithLifecycle()
            val profile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val isProfileComplete by authViewModel.isProfileComplete.collectAsStateWithLifecycle()
            val isStaffAccessDenied by authViewModel.isStaffAccessDenied.collectAsStateWithLifecycle()

            val connectivityObserver = remember { ConnectivityObserver(context = applicationContext) }
            val networkStatus by connectivityObserver.observe().collectAsStateWithLifecycle(initialValue = ConnectivityStatus.Available)

            CompositionLocalProvider(
                LocalAppLanguage provides currentLanguage,
                LocalIsPremium provides (profile?.isPremium == true || profile?.isAdmin == true || profile?.email == "bibiniitech@gmail.com")
            ) {
                SmartSwineTheme(darkTheme = isDarkMode) {
                    val billingManager = BillingManager.getInstance(LocalContext.current)
                    
                    // Refresh billing status on app resume
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                billingManager.queryPurchases()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }
                    
                    val user by authViewModel.user.collectAsStateWithLifecycle()
                    val activeFarmUid by authViewModel.activeFarmUid.collectAsStateWithLifecycle()

                    // Runtime Security Check
                    val appContext = LocalContext.current
                    val isRooted = remember { SecurityUtils.isDeviceRooted() }
                    val isEmulator = remember { SecurityUtils.isEmulator() }
                    
                    // We'll capture the actual found signature for debugging
                    val actualSignature = remember { 
                        try {
                            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                appContext.packageManager.getPackageInfo(appContext.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                            } else {
                                @Suppress("DEPRECATION")
                                appContext.packageManager.getPackageInfo(appContext.packageName, PackageManager.GET_SIGNATURES)
                            }
                            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                packageInfo.signingInfo?.signingCertificateHistory
                            } else {
                                @Suppress("DEPRECATION")
                                packageInfo.signatures
                            }
                            signatures?.firstOrNull()?.let { sig ->
                                val md = java.security.MessageDigest.getInstance("SHA-256")
                                md.digest(sig.toByteArray()).joinToString(":") { String.format("%02X", it) }
                            } ?: "No Signature Found"
                        } catch (e: Exception) { "Error: ${e.message}" }
                    }

                    val isSignatureValid = remember { SecurityUtils.verifyAppSignature(appContext, SecurityUtils.EXPECTED_SIGNATURE_HASH) }
                    val isXposedActive = remember { SecurityUtils.isXposedActive() }
                    val isLuckyPatcherActive = remember { SecurityUtils.isLuckyPatcherInstalled(appContext) }
                    val isInstallerValid = remember { SecurityUtils.verifyInstaller(appContext) }
                    
                    // Log root and installer status, but do not block the user
                    LaunchedEffect(isRooted, isInstallerValid) {
                        if (isRooted) {
                            Log.w("Security", "Running on a rooted device.")
                        }
                        if (!isInstallerValid) {
                            Log.w("Security", "App was not installed from an official store.")
                        }
                    }

                    val securityMessage = remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(isSignatureValid, isXposedActive, isLuckyPatcherActive) {
                        if (!isSignatureValid) {
                            securityMessage.value = "Signature Mismatch.\nExpected: ${SecurityUtils.EXPECTED_SIGNATURE_HASH}\nActual: $actualSignature\n\nPlease ensure you are using the official version."
                        } else if (isXposedActive) {
                            securityMessage.value = "Active hooking framework (Xposed) detected. Please disable it to run SmartSwine."
                        } else if (isLuckyPatcherActive) {
                            securityMessage.value = "Patching or game hacking tools (like Lucky Patcher) detected. Please uninstall them to run SmartSwine."
                        }
                    }

                    securityMessage.value?.let { message ->
                        AlertDialog(
                            onDismissRequest = { },
                            title = { Text("Security Violation") },
                            text = { Text(message) },
                            confirmButton = {
                                Button(onClick = { finish() }) { Text("Exit App") }
                            }
                        )
                    }

                    LaunchedEffect(profile, user, activeFarmUid) {
                        val currentProfile = profile
                        val currentUser = user
                        val farmUid = activeFarmUid
                        
                        if (currentProfile != null && currentUser != null && currentUser.uid == farmUid) {
                            billingManager.isPremium.collect { premium ->
                                if (premium == null) return@collect
                                
                                // Sync Play Store status to Firestore.
                                if (premium == true && (currentProfile.isPremium != true || currentProfile.subscriptionSource != "play_store")) {
                                    authViewModel.updateProfile(currentProfile.copy(isPremium = true, subscriptionSource = "play_store")) { _, _ -> }
                                } else if (premium == false && currentProfile.isPremium == true && currentProfile.subscriptionSource == "play_store" && currentProfile.isKofisPerson != true) {
                                    // Revoke access if subscription expired/cancelled
                                    authViewModel.updateProfile(currentProfile.copy(isPremium = false, subscriptionSource = "")) { _, _ -> }
                                }
                            }
                        }
                    }

                    val dashboardViewModel: DashboardViewModel = viewModel()
                    
                    // Sync activeFarmUid to other ViewModels
                    val productionViewModel: ProductionViewModel = viewModel()
                    val herdViewModel: HerdViewModel = viewModel()
                    val financialViewModel: FinancialViewModel = viewModel()
                    val feedViewModel: FeedViewModel = viewModel()
                    val hrViewModel: HumanResourceViewModel = viewModel()
                    
                    LaunchedEffect(activeFarmUid) {
                        activeFarmUid?.let { uid ->
                            dashboardViewModel.setActiveFarmId(uid)
                            herdViewModel.setActiveFarmId(uid)
                            productionViewModel.setActiveFarmId(uid)
                            financialViewModel.setActiveFarmId(uid)
                            feedViewModel.setActiveFarmId(uid)
                            hrViewModel.setActiveFarmId(uid)
                        }
                    }

                    val navController = rememberNavController()
                    val coroutineScope = rememberCoroutineScope()
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                    val context = LocalContext.current
                    var showExitDialog by remember { mutableStateOf(value = false) }

                    // Determine if we should show exit dialog or use default back behavior
                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route
                    val isAtDashboard = (currentRoute == Screen.Dashboard.route) || (currentRoute == null)

                    BackHandler(enabled = (user != null) && isAtDashboard) {
                        showExitDialog = true
                    }

                    if (showExitDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            title = { Text("Exit App?") },
                            text = { Text("Are you sure you want to exit SmartSwine?") },
                            confirmButton = {
                                Button(onClick = { (context as? android.app.Activity)?.finish() }) {
                                    Text("Exit")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showExitDialog = false }) {
                                    Text("Cancel")
                                }
                            },
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        val notice by GlobalNotice.message.collectAsStateWithLifecycle()
                        if (notice != null) {
                            AlertDialog(
                                onDismissRequest = { },
                                confirmButton = {},
                                title = { Text(stringResource("please_wait")) },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                    ) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(notice!!)
                                    }
                                },
                            )
                        }
                        if (user == null) {
                            AuthScreen(
                                onAuthSuccess = {
                                    // User state will update via AuthStateListener in ViewModel
                                }
                            )
                        } else {
                            val complete = isProfileComplete
                            if (complete == null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (!complete) {
                                if (isStaffAccessDenied) {
                                    AccessDeniedScreen(
                                        onSignOut = {
                                            authViewModel.signOut()
                                        }
                                    )
                                } else {
                                    CompleteProfileScreen(
                                        firebaseUser = user!!,
                                        onProfileCreated = {
                                            // The AuthViewModel's fetchUserProfile will automatically update profile and isProfileComplete
                                        }
                                    )
                                }
                            } else {
                                AppDrawer(
                                    drawerState = drawerState,
                                    userProfile = profile,
                                currentRoute = currentRoute,
                                onNavigateTo = { screen -> 
                                    if (screen == Screen.Dashboard) {
                                        navController.popBackStack(navController.graph.findStartDestination().id, inclusive = false)
                                    } else if (navController.currentDestination?.route != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    coroutineScope.launch { drawerState.close() }
                                },
                                onSignOut = {
                                    authViewModel.signOut()
                                    coroutineScope.launch { drawerState.close() }
                                }
                            ) {
                                Scaffold(
                                    floatingActionButton = {
                                        FloatingActionButton(
                                            onClick = { coroutineScope.launch { drawerState.open() } },
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                                        }
                                    },
                                    floatingActionButtonPosition = FabPosition.Start
                                ) { innerPadding ->
                                    Column(modifier = Modifier.padding(innerPadding)) {
                                        if (networkStatus == ConnectivityStatus.Unavailable) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "You are currently offline. Changes will sync when reconnected.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                        NavHost(
                                            navController = navController,
                                            startDestination = Screen.Dashboard.route,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                        composable(Screen.Dashboard.route) {
                                            val tasks by dashboardViewModel.tasks.collectAsStateWithLifecycle()
                                            val error by dashboardViewModel.error.collectAsStateWithLifecycle()
                                            val isRefreshing by dashboardViewModel.isRefreshing.collectAsStateWithLifecycle()
                                            // ViewModels already declared and synced above

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
                                            // herdViewModel synced above
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
                                            // herdViewModel synced above
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
                                            composable(Screen.FeedCalculationResult.route) { backStackEntry ->
                                                FeedCalculationResultScreen(
                                                    viewModel = feedViewModel,
                                                    onBack = { navController.popBackStack() },
                                                    onNavigateToPaywall = {
                                                        navController.navigate(Screen.Paywall.route)
                                                    }
                                                )
                                            }
                                            composable(Screen.FeedFormulationResult.route) { backStackEntry ->
                                                FeedFormulationResultScreen(
                                                    viewModel = feedViewModel,
                                                    onBack = { navController.popBackStack() }
                                                )
                                            }
                                            composable(Screen.IngredientList.route) { backStackEntry ->
                                                IngredientListScreen(
                                                    onBack = { navController.popBackStack() },
                                                    onNavigateTo = { route -> navController.navigate(route) },
                                                    viewModel = feedViewModel
                                                )
                                            }
                                            composable(Screen.AddIngredient.route) { backStackEntry ->
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
                                            // productionViewModel and herdViewModel synced above
                                            ProductionActivitiesScreen(
                                                viewModel = productionViewModel,
                                                herdViewModel = herdViewModel,
                                                onBack = { navController.popBackStack() }
                                            )
                                        }
                                        composable(Screen.Financials.route) {
                                            // financialViewModel synced above
                                            FinancialsScreen(
                                                viewModel = financialViewModel,
                                                onNavigateToPaywall = {
                                                    navController.navigate(Screen.Paywall.route)
                                                },
                                                onBack = { navController.popBackStack() }
                                            )
                                        }
                                        composable(Screen.HumanResource.route) {
                                            // hrViewModel synced above
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
                                            // herdViewModel synced above
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
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun AppDrawer(
    drawerState: DrawerState,
    userProfile: com.example.smartswine.ui.auth.UserProfile?,
    currentRoute: String?,
    onNavigateTo: (Screen) -> Unit,
    onSignOut: () -> Unit,
    content: @Composable () -> Unit,
) {
        ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(260.dp),
                drawerContainerColor = MaterialTheme.colorScheme.primary,
                drawerContentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = userProfile?.farmName?.takeIf { it.isNotBlank() } ?: stringResource("profile"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    StylishDivider(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    NavigationDrawerItem(
                        label = { Text(stringResource("home"), color = Color.White) },
                        selected = currentRoute == Screen.Dashboard.route || currentRoute == null,
                        onClick = { onNavigateTo(Screen.Dashboard) },
                        icon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color.White) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource("herd_data"), color = Color.White) },
                        selected = currentRoute == Screen.HerdData.route,
                        onClick = { onNavigateTo(Screen.HerdData) },
                        icon = { Icon(painter = painterResource(id = R.drawable.ic_herd_data), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource("feed"), color = Color.White) },
                        selected = currentRoute?.startsWith(Screen.Feed.route) == true,
                        onClick = { onNavigateTo(Screen.Feed) },
                        icon = { Icon(painter = painterResource(id = R.drawable.ic_feed2), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource("herd_activities"), color = Color.White) },
                        selected = currentRoute == Screen.ProductionActivities.route,
                        onClick = { onNavigateTo(Screen.ProductionActivities) },
                        icon = { Icon(painter = painterResource(id = R.drawable.ic_herd_activities), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource("financials"), color = Color.White) },
                        selected = currentRoute == Screen.Financials.route,
                        onClick = { onNavigateTo(Screen.Financials) },
                        icon = { Icon(Icons.Default.Payments, contentDescription = null, tint = Color.White) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource("human_resources"), color = Color.White) },
                        selected = currentRoute == Screen.HumanResource.route,
                        onClick = { onNavigateTo(Screen.HumanResource) },
                        icon = { Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource("market"), color = Color.White) },
                        selected = currentRoute == Screen.MarketAccess.route,
                        onClick = { onNavigateTo(Screen.MarketAccess) },
                        icon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource("symptoms_analyzer"), color = Color.White) },
                        selected = currentRoute == Screen.DiseaseFinder.route,
                        onClick = { onNavigateTo(Screen.DiseaseFinder) },
                        icon = { Icon(painter = painterResource(id = R.drawable.ic_symptoms_analyzer), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource("weight_checker"), color = Color.White) },
                        selected = currentRoute == Screen.WeightChecker.route,
                        onClick = { onNavigateTo(Screen.WeightChecker) },
                        icon = { Icon(painter = painterResource(id = R.drawable.ic_weight_checker), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource("training"), color = Color.White) },
                        selected = currentRoute == Screen.Training.route,
                        onClick = { onNavigateTo(Screen.Training) },
                        icon = { Icon(Icons.Default.School, contentDescription = null, tint = Color.White) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    StylishDivider(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    NavigationDrawerItem(
                        label = { Text(stringResource("settings"), color = Color.White) },
                        selected = currentRoute == Screen.Settings.route,
                        onClick = { onNavigateTo(Screen.Settings) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    if (userProfile?.isAdmin == true || userProfile?.email == "bibiniitech@gmail.com") {
                        StylishDivider(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        NavigationDrawerItem(
                            label = { Text("Admin Panel", color = Color.White) },
                            selected = currentRoute == Screen.AdminPanel.route,
                            onClick = { onNavigateTo(Screen.AdminPanel) },
                            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                    StylishDivider(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    NavigationDrawerItem(
                        label = { Text(stringResource("sign_out"), color = Color.White) },
                        selected = false,
                        onClick = onSignOut,
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent
                        )
                    )
                }
            }
        },
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun AppDrawerPreview() {
    SmartSwineTheme {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
        AppDrawer(
            drawerState = drawerState,
            userProfile = com.example.smartswine.ui.auth.UserProfile(
                firstName = "John",
                lastName = "Doe",
                farmName = "Happy Pig Farm"
            ),
            currentRoute = Screen.Dashboard.route,
            onNavigateTo = {},
            onSignOut = {}
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("Main Content Area")
            }
        }
    }
}

@Composable
fun AccessDeniedScreen(
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Access Denied",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Access Denied",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Your manager's premium subscription has expired, or your access was suspended. Please contact your administrator.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}
