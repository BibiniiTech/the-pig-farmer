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
import com.example.smartswine.data.BillingManager
import com.example.smartswine.data.SecurityManager
import com.example.smartswine.data.SecurityStatus
import com.example.smartswine.ui.auth.AccessDeniedScreen
import com.example.smartswine.ui.auth.AuthScreen
import com.example.smartswine.ui.auth.AuthViewModel
import com.example.smartswine.ui.auth.CompleteProfileScreen
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
    val connectivityObserver = remember { ConnectivityObserver(context = context.applicationContext) }
    val networkStatus by connectivityObserver.observe().collectAsStateWithLifecycle(initialValue = ConnectivityStatus.Available)

    CompositionLocalProvider(
        LocalAppLanguage provides currentLanguage,
        LocalIsPremium provides (profile?.isPremium == true || profile?.isAdmin == true || (profile?.email == "bibiniitech@gmail.com")),
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
                    title = { Text("Security Violation") },
                    text = { Text(securityResult.message) },
                    confirmButton = {
                        Button(onClick = onExit) { Text("Exit App") }
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
                activeFarmUid?.let { uid ->
                    dashboardViewModel.setActiveFarmId(uid)
                    herdViewModel.setActiveFarmId(uid)
                    productionViewModel.setActiveFarmId(uid)
                    financialViewModel.setActiveFarmId(uid)
                    feedViewModel.setActiveFarmId(uid)
                    hrViewModel.setActiveFarmId(uid)
                }
            }

            LaunchedEffect(currentLanguage) {
                dashboardViewModel.setLanguage(currentLanguage.code)
            }

            val navController = rememberNavController()
            val coroutineScope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

            var showExitDialog by remember { mutableStateOf(value = false) }

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
                        Button(onClick = onExit) {
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
                            AccessDeniedScreen { authViewModel.signOut() }
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
                                        languageViewModel = languageViewModel
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
