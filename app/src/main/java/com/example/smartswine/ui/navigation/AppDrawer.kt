package com.example.smartswine.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibiniitech.smartswine.R
import com.example.smartswine.ui.auth.UserProfile
import com.example.smartswine.ui.components.FarmLogoImage
import com.example.smartswine.ui.components.ManageSubscriptionDialog
import com.example.smartswine.ui.theme.SmartSwineTheme
import androidx.compose.runtime.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import com.example.smartswine.ui.components.PlayStoreReviewManager
import com.example.smartswine.utils.AppLanguage
import com.example.smartswine.utils.stringResource
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(
    drawerState: DrawerState,
    userProfile: UserProfile?,
    currentRoute: String?,
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onLanguageChange: (AppLanguage) -> Unit = {},
    onAddPigClick: () -> Unit = {},
    onCalculateFeedClick: () -> Unit = {},
    onMixFeedClick: () -> Unit = {},
    onAddEmployeeClick: () -> Unit = {},
    onNavigateTo: (Screen) -> Unit,
    onSignOut: () -> Unit,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showManageSubscriptionDialog by remember { mutableStateOf(false) }

    // Wrap ModalNavigationDrawer in RTL so that the drawer slides in from the RIGHT edge
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                // Restore LTR inside the drawer content so all text & controls render normally
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier
                            .width(326.dp)
                            .fillMaxHeight(),
                        drawerShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                        drawerContainerColor = Color(0xFF081E12),
                        drawerContentColor = Color.White
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF0E3820),
                                            Color(0xFF092214),
                                            Color(0xFF04120A)
                                        )
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // ─── SPECCED-UP HEADER ─────────────────────────────────
                                Surface(
                                    color = Color(0xFF134226),
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(0.5.dp, Color(0xFF81C784).copy(alpha = 0.25f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 18.dp, end = 14.dp, top = 22.dp, bottom = 18.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Farm Logo & Info
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Surface(
                                                    modifier = Modifier.size(52.dp),
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = Color.White.copy(alpha = 0.15f),
                                                    border = BorderStroke(1.5.dp, Color(0xFF81C784).copy(alpha = 0.6f)),
                                                    shadowElevation = 2.dp
                                                ) {
                                                    FarmLogoImage(
                                                        farmLogo = userProfile?.farmLogo,
                                                        contentDescription = stringResource("farm_logo"),
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = userProfile?.farmName?.takeIf { it.isNotBlank() }
                                                            ?: stringResource("your_farm"),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val firstName = userProfile?.firstName
                                                    Text(
                                                        text = if (firstName.isNullOrBlank())
                                                            "SmartSwine Manager"
                                                        else
                                                            "${stringResource("farmer")} $firstName",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF81C784),
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            // Quick Close Button
                                            IconButton(
                                                onClick = { coroutineScope.launch { drawerState.close() } },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = stringResource("close_menu"),
                                                    tint = Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Status Pill Badge (Interactive: Free -> Paywall, Premium -> Manage Subscription)
                                        Surface(
                                            onClick = {
                                                if (userProfile?.isPremium == true) {
                                                    coroutineScope.launch { drawerState.close() }
                                                    showManageSubscriptionDialog = true
                                                } else {
                                                    coroutineScope.launch { drawerState.close() }
                                                    onNavigateTo(Screen.Paywall)
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF092917),
                                            border = BorderStroke(
                                                1.dp,
                                                if (userProfile?.isPremium == true) Color(0xFF66BB6A).copy(alpha = 0.5f) else Color(0xFFFFB74D).copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(
                                                            if (userProfile?.isPremium == true) Color(0xFF66BB6A) else Color(0xFFFFB74D),
                                                            CircleShape
                                                        )
                                                )
                                                Text(
                                                    text = if (userProfile?.isPremium == true) stringResource("premium_tier_manage") else stringResource("free_tier_upgrade"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (userProfile?.isPremium == true) Color(0xFFA5D6A7) else Color(0xFFFFE082),
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // ─── CATEGORIZED NAVIGATION ITEMS ───────────────────────
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    // 1. HOME
                                    DrawerNavRow(
                                        label = stringResource("home"),
                                        icon = rememberVectorPainter(Icons.Default.Home),
                                        iconTint = Color(0xFF81C784),
                                        isSelected = currentRoute == Screen.Dashboard.route || currentRoute == Screen.Dashboard.routeWithArgs || currentRoute == null,
                                        onClick = { onNavigateTo(Screen.Dashboard) }
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 2. DARK THEME TOGGLE + LANGUAGE FLAG (SAME LINE)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Dark theme toggle pill
                                        Surface(
                                            onClick = onToggleDarkMode,
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFF134226),
                                            border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.35f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                                        contentDescription = null,
                                                        tint = if (isDarkMode) Color(0xFFFFD54F) else Color(0xFFFFB74D),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = if (isDarkMode) stringResource("dark") else stringResource("light"),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                                Switch(
                                                    checked = isDarkMode,
                                                    onCheckedChange = { onToggleDarkMode() },
                                                    modifier = Modifier
                                                        .scale(0.8f)
                                                        .height(24.dp)
                                                )
                                            }
                                        }

                                        // Language flag dropdown
                                        var showLangMenu by remember { mutableStateOf(false) }
                                        Box {
                                            Surface(
                                                onClick = { showLangMenu = true },
                                                shape = RoundedCornerShape(14.dp),
                                                color = Color(0xFF134226),
                                                border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.35f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = currentLanguage.flag,
                                                        fontSize = 20.sp
                                                    )
                                                    Text(
                                                        text = currentLanguage.code.uppercase(),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF81C784)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowDropDown,
                                                        contentDescription = null,
                                                        tint = Color.White.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = showLangMenu,
                                                onDismissRequest = { showLangMenu = false }
                                            ) {
                                                AppLanguage.entries.forEach { lang ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                Text(lang.flag)
                                                                Text(lang.displayName)
                                                            }
                                                        },
                                                        onClick = {
                                                            onLanguageChange(lang)
                                                            showLangMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 3. QUICK ACCESS
                                    DrawerSectionHeader(stringResource("quick_access"))

                                    // Add pigs
                                    DrawerNavRow(
                                        label = stringResource("add_pigs"),
                                        icon = rememberVectorPainter(Icons.Default.AddCircleOutline),
                                        iconTint = Color(0xFF66BB6A),
                                        isSelected = false,
                                        onClick = onAddPigClick
                                    )

                                    // Calculate feed (renamed from calculator)
                                    DrawerNavRow(
                                        label = stringResource("calculate_feed"),
                                        icon = rememberVectorPainter(Icons.Default.Calculate),
                                        iconTint = Color(0xFFFFA726),
                                        isSelected = false,
                                        onClick = onCalculateFeedClick
                                    )

                                    // Mix feed
                                    DrawerNavRow(
                                        label = stringResource("mix_feed"),
                                        icon = rememberVectorPainter(Icons.Default.Science),
                                        iconTint = Color(0xFFFFB74D),
                                        isSelected = false,
                                        onClick = onMixFeedClick
                                    )

                                    // Analyze feed
                                    DrawerNavRow(
                                        label = stringResource("analyze_feed"),
                                        icon = rememberVectorPainter(Icons.Default.Analytics),
                                        iconTint = Color(0xFF29B6F6),
                                        isSelected = currentRoute == Screen.AnalyzeFeed.route,
                                        onClick = { onNavigateTo(Screen.AnalyzeFeed) }
                                    )

                                    // Add employee
                                    DrawerNavRow(
                                        label = stringResource("add_employee"),
                                        icon = rememberVectorPainter(Icons.Default.PersonAdd),
                                        iconTint = Color(0xFFAB47BC),
                                        isSelected = false,
                                        onClick = onAddEmployeeClick
                                    )

                                    // Find disease
                                    DrawerNavRow(
                                        label = stringResource("find_disease"),
                                        icon = painterResource(R.drawable.ic_symptoms_analyzer),
                                        iconTint = Color(0xFFEC407A),
                                        isSelected = currentRoute == Screen.DiseaseFinder.route,
                                        onClick = { onNavigateTo(Screen.DiseaseFinder) }
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 4.dp))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 4. HOW-TO GUIDE
                                    DrawerNavRow(
                                        label = stringResource("how_to_guide"),
                                        icon = rememberVectorPainter(Icons.Default.MenuBook),
                                        iconTint = Color(0xFF64B5F6),
                                        isSelected = currentRoute == Screen.HowToGuide.route,
                                        onClick = { onNavigateTo(Screen.HowToGuide) }
                                    )

                                    // 5. SETTINGS
                                    DrawerNavRow(
                                        label = stringResource("settings"),
                                        icon = rememberVectorPainter(Icons.Default.Settings),
                                        iconTint = Color(0xFFB0BEC5),
                                        isSelected = currentRoute == Screen.Settings.route,
                                        onClick = { onNavigateTo(Screen.Settings) }
                                    )

                                    // Rate on Google Play
                                    DrawerNavRow(
                                        label = stringResource("rate_5_stars"),
                                        icon = rememberVectorPainter(Icons.Default.Star),
                                        iconTint = Color(0xFFFFB300),
                                        isSelected = false,
                                        onClick = {
                                            coroutineScope.launch { drawerState.close() }
                                            PlayStoreReviewManager.openPlayStoreForReview(context)
                                        }
                                    )

                                    if (userProfile?.isAdmin == true || userProfile?.email == "bibiniitech@gmail.com") {
                                        DrawerNavRow(
                                            label = stringResource("admin_panel"),
                                            icon = rememberVectorPainter(Icons.Default.Lock),
                                            iconTint = Color(0xFFFFB300),
                                            isSelected = currentRoute == Screen.AdminPanel.route,
                                            onClick = { onNavigateTo(Screen.AdminPanel) }
                                        )
                                    }

                                    // 6. SIGN OUT
                                    DrawerNavRow(
                                        label = stringResource("sign_out"),
                                        icon = rememberVectorPainter(Icons.AutoMirrored.Filled.Logout),
                                        iconTint = Color(0xFFEF5350),
                                        isSelected = false,
                                        isDanger = true,
                                        onClick = onSignOut
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // ─── SPECCED-UP FOOTER ─────────────────────────────────
                                Surface(
                                    color = Color(0xFF06180C),
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${stringResource("app_name")} • ${stringResource("pro_edition")}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                        Text(
                                            text = "v${com.bibiniitech.smartswine.BuildConfig.VERSION_NAME}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.5.sp,
                                            color = Color(0xFF81C784)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            content = {
                // Restore LTR for main content screens
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    content()
                }
            }
        )
    }

    if (showManageSubscriptionDialog) {
        ManageSubscriptionDialog(
            onDismiss = { showManageSubscriptionDialog = false }
        )
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black,
        color = Color(0xFF81C784).copy(alpha = 0.85f),
        letterSpacing = 1.2.sp,
        fontSize = 14.sp,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun DrawerNavRow(
    label: String,
    icon: Painter,
    iconTint: Color,
    isSelected: Boolean,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> Color(0xFF1B5530)
        else -> Color.Transparent
    }
    val borderColor = when {
        isSelected -> Color(0xFF81C784).copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    val contentColor = when {
        isDanger -> Color(0xFFFF8A80)
        isSelected -> Color.White
        else -> Color.White.copy(alpha = 0.9f)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        border = if (isSelected) BorderStroke(1.dp, borderColor) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon container with squircle badge
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = if (isSelected) iconTint.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = if (isSelected) iconTint else if (isDanger) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 17.5.sp,
                color = contentColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF81C784), CircleShape)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppDrawerPreview() {
    SmartSwineTheme {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
        AppDrawer(
            drawerState = drawerState,
            userProfile = UserProfile(
                firstName = "John",
                lastName = "Doe",
                farmName = "Happy Pig Farm",
                isPremium = true
            ),
            currentRoute = Screen.Dashboard.route,
            onNavigateTo = {},
            onSignOut = {}
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Main Content Area")
            }
        }
    }
}
