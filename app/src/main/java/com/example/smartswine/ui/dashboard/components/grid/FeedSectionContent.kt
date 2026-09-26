package com.example.smartswine.ui.dashboard.components.grid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Science
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.utils.stringResource

@Composable
fun FeedSectionContent(
    primaryColor: Color,
    onShowFeedCalculator: () -> Unit,
    onShowFeedFormulator: () -> Unit,
    onNavigateTo: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 4 Clean Sub-Options: Feed Inventory, Calculate Feed, Mix Feed, Analyze Feed
        CleanDropdownOption(
            title = stringResource("feed_inventory"),
            icon = Icons.Default.Inventory,
            primaryColor = primaryColor,
            onClick = { onNavigateTo(Screen.Feed.route) }
        )

        CleanDropdownOption(
            title = stringResource("calculate_feed"),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            primaryColor = primaryColor,
            onClick = onShowFeedCalculator
        )

        CleanDropdownOption(
            title = stringResource("mix_feed"),
            icon = Icons.Default.Science,
            primaryColor = primaryColor,
            onClick = onShowFeedFormulator
        )

        CleanDropdownOption(
            title = stringResource("analyze_feed"),
            icon = Icons.Default.Analytics,
            primaryColor = primaryColor,
            onClick = { onNavigateTo(Screen.AnalyzeFeed.route) }
        )
    }
}
