package com.example.smartswine.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bibiniitech.smartswine.R
import com.example.smartswine.ui.auth.UserProfile
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.utils.stringResource

@Composable
fun AppDrawer(
    drawerState: DrawerState,
    userProfile: UserProfile?,
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
                        verticalAlignment = Alignment.CenterVertically
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
            userProfile = UserProfile(
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
                contentAlignment = Alignment.Center
            ) {
                Text("Main Content Area")
            }
        }
    }
}
