package com.example.smartswine

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smartswine.data.AdRewardManager
import com.example.smartswine.data.BillingManager
import com.example.smartswine.data.SecurityManager
import com.example.smartswine.data.SecurityStatus
import com.google.android.gms.ads.MobileAds
import com.example.smartswine.ui.auth.AccessDeniedScreen
import com.example.smartswine.ui.auth.AuthScreen
import com.example.smartswine.ui.auth.AuthViewModel
import com.example.smartswine.ui.auth.CompleteProfileScreen
import com.example.smartswine.ui.components.WhatsNewDialog
import com.example.smartswine.ui.components.WhatsNewManager
import com.example.smartswine.ui.dashboard.DashboardViewModel
import com.example.smartswine.ui.feed.FeedViewModel
import com.example.smartswine.ui.financials.FinancialViewModel
import com.example.smartswine.ui.herd.HerdViewModel
import com.example.smartswine.ui.hr.HumanResourceViewModel
import com.example.smartswine.ui.navigation.AppDrawer
import com.example.smartswine.ui.navigation.AppNavigation
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.ui.production.ProductionViewModel
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.ui.theme.ThemeViewModel
import com.example.smartswine.utils.*
import kotlinx.coroutines.launch

@Composable
fun SmartSwineApp(onExit: () -> Unit) {
    val themeViewModel: ThemeViewModel = viewModel()
    val languageViewModel: LanguageViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    
    val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentLanguage by languageViewModel.currentLanguage.collectAsStateWithLifecycle()
    val profile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val isProfileComplete by authViewModel.isProfileComplete.collectAsStateWithLifecycle()
    val isStaffAccessDenied by authViewModel.isStaffAccessDenied.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        MobileAds.initialize(context)
    }
    val adRewardManager = remember { AdRewardManager.getInstance(context) }
    val isPassActive by adRewardManager.isPassActive.collectAsStateWithLifecycle()
    val passRemainingTime by adRewardManager.passTimeRemainingFormatted.collectAsStateWithLifecycle()

    val isPaidPremium = profile?.isPremium == true || profile?.isAdmin == true || (profile?.email == "bibiniitech@gmail.com")
    val effectivePremium = isPaidPremium || isPassActive

    val connectivityObserver = remember { ConnectivityObserver(context = context.applicationContext) }
    val networkStatus by connectivityObserver.observe().collectAsStateWithLifecycle(initialValue = ConnectivityStatus.Available)

    CompositionLocalProvider(
        LocalAppLanguage provides currentLanguage,
        LocalIsPremium provides effectivePremium,
        LocalIsPaidPremium provides isPaidPremium,
        LocalPassRemainingTime provides if (isPassActive && !isPaidPremium) passRemainingTime else null,
    ) {
        SmartSwineTheme(darkTheme = isDarkMode) {
            val billingManager = BillingManager.getInstance(context)
            
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
            val securityManager = remember { SecurityManager(context) }
            val securityResult = remember { securityManager.checkSecurity() }

            if (securityResult is SecurityStatus.Violation) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text(stringResource("security_violation")) },
                    text = { Text(securityResult.message) },
                    confirmButton = {
                        Button(onClick = onExit) { Text(stringResource("exit_app")) }
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
            val productionViewModel: ProductionViewModel = viewModel()
            val herdViewModel: HerdViewModel = viewModel()
            val financialViewModel: FinancialViewModel = viewModel()
            val feedViewModel: FeedViewModel = viewModel()
            val hrViewModel: HumanResourceViewModel = viewModel()
            
            LaunchedEffect(activeFarmUid) {
                dashboardViewModel.setActiveFarmId(activeFarmUid)
                herdViewModel.setActiveFarmId(activeFarmUid)
                productionViewModel.setActiveFarmId(activeFarmUid)
                financialViewModel.setActiveFarmId(activeFarmUid)
                feedViewModel.setActiveFarmId(activeFarmUid)
                hrViewModel.setActiveFarmId(activeFarmUid)
            }

            LaunchedEffect(currentLanguage) {
                dashboardViewModel.setLanguage(currentLanguage.code)
            }

            val navController = rememberNavController()
            val coroutineScope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

            var showExitDialog by remember { mutableStateOf(value = false) }
            var showWhatsNewDialog by remember { mutableStateOf(value = false) }

            LaunchedEffect(user, isProfileComplete) {
                if (user != null && isProfileComplete == true && WhatsNewManager.shouldShowWhatsNew(context)) {
                    showWhatsNewDialog = true
                }
            }

            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = currentBackStackEntry?.destination?.route
            val isAtDashboard = (currentRoute == Screen.Dashboard.route) ||
                    (currentRoute == Screen.Dashboard.routeWithArgs) ||
                    (currentRoute == null)

            // Close drawer on back press if it is open
            BackHandler(enabled = drawerState.isOpen) {
                coroutineScope.launch { drawerState.close() }
            }

            // Show exit confirmation only when at root dashboard with closed drawer
            BackHandler(enabled = (user != null) && isAtDashboard && !drawerState.isOpen) {
                showExitDialog = true
            }

            if (showWhatsNewDialog) {
                WhatsNewDialog(
                    onDismiss = { showWhatsNewDialog = false },
                    onExploreGuide = {
                        showWhatsNewDialog = false
                        navController.navigate(Screen.HowToGuide.route)
                    }
                )
            }

            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = { Text(stringResource("exit_app_question")) },
                    text = { Text(stringResource("exit_app_confirm_msg")) },
                    confirmButton = {
                        Button(onClick = onExit) {
                            Text(stringResource("exit"))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitDialog = false }) {
                            Text(stringResource("cancel"))
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
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(notice!!)
                            }
                        },
                    )
                }

                if (user == null) {
                    AuthScreen(onAuthSuccess = { })
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
                                onCreateOwnFarm = {
                                    authViewModel.detachFromStaffRegistryAndCreateFarm()
                                },
                                onSignOut = {
                                    coroutineScope.launch {
                                        dashboardViewModel.setActiveFarmId(null)
                                        herdViewModel.setActiveFarmId(null)
                                        productionViewModel.setActiveFarmId(null)
                                        financialViewModel.setActiveFarmId(null)
                                        feedViewModel.setActiveFarmId(null)
                                        hrViewModel.setActiveFarmId(null)
                                        authViewModel.signOut()
                                    }
                                }
                            )
                        } else {
                            CompleteProfileScreen(
                                firebaseUser = user!!,
                                onProfileCreated = { }
                            )
                        }
                    } else {
                        AppDrawer(
                            drawerState = drawerState,
                            userProfile = profile,
                            currentRoute = currentRoute,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { themeViewModel.toggleTheme() },
                            currentLanguage = currentLanguage,
                            onLanguageChange = { languageViewModel.setLanguage(it) },
                            onAddPigClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.HerdData.createRoute(showAdd = true))
                            },
                            onCalculateFeedClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.Feed.createRoute(showCalculator = true))
                            },
                            onMixFeedClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.Feed.createRoute(showFormulator = true))
                            },
                            onAddEmployeeClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.Dashboard.createRoute(section = "human_resources", subOption = "add")) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                }
                            },
                            onNavigateTo = { screen -> 
                                val isDashboardTarget = screen == Screen.Dashboard ||
                                    screen == Screen.HumanResource ||
                                    screen == Screen.MarketAccess ||
                                    screen == Screen.DiseaseFinder ||
                                    screen == Screen.WeightChecker ||
                                    screen == Screen.Training

                                val targetRoute = when (screen) {
                                    Screen.Dashboard -> Screen.Dashboard.route
                                    Screen.HumanResource -> Screen.Dashboard.createRoute("human_resources")
                                    Screen.MarketAccess -> Screen.Dashboard.createRoute("market")
                                    Screen.DiseaseFinder -> Screen.Dashboard.createRoute("symptoms_analyzer")
                                    Screen.WeightChecker -> Screen.Dashboard.createRoute("weight_checker")
                                    Screen.Training -> Screen.Dashboard.createRoute("training")
                                    else -> screen.route
                                }
                                if (isDashboardTarget) {
                                    navController.navigate(targetRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            inclusive = true
                                        }
                                    }
                                } else if (navController.currentDestination?.route != targetRoute) {
                                    navController.navigate(targetRoute) {
                                        launchSingleTop = true
                                    }
                                }
                                coroutineScope.launch { drawerState.close() }
                            },
                            onSignOut = {
                                coroutineScope.launch {
                                    try {
                                        drawerState.snapTo(DrawerValue.Closed)
                                    } catch (_: Exception) {}
                                    dashboardViewModel.setActiveFarmId(null)
                                    herdViewModel.setActiveFarmId(null)
                                    productionViewModel.setActiveFarmId(null)
                                    financialViewModel.setActiveFarmId(null)
                                    feedViewModel.setActiveFarmId(null)
                                    hrViewModel.setActiveFarmId(null)
                                    authViewModel.signOut()
                                }
                            }
                        ) {
                            Scaffold { innerPadding ->
                                Column(modifier = Modifier.padding(innerPadding)) {
                                    if (networkStatus == ConnectivityStatus.Unavailable) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = stringResource("offline_banner_msg"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                    AppNavigation(
                                        navController = navController,
                                        modifier = Modifier.weight(1f),
                                        profile = profile,
                                        dashboardViewModel = dashboardViewModel,
                                        herdViewModel = herdViewModel,
                                        feedViewModel = feedViewModel,
                                        productionViewModel = productionViewModel,
                                        financialViewModel = financialViewModel,
                                        hrViewModel = hrViewModel,
                                        authViewModel = authViewModel,
                                        themeViewModel = themeViewModel,
                                        languageViewModel = languageViewModel,
                                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
