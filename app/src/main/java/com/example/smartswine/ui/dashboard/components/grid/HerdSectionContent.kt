package com.example.smartswine.ui.dashboard.components.grid

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartswine.model.Pig
import com.example.smartswine.model.PigStatus
import com.example.smartswine.ui.herd.StatsRibbon
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.utils.stringResource

@Composable
fun HerdSectionContent(
    allPigs: List<Pig>,
    herdStats: Map<String, Int>,
    primaryColor: Color,
    onAddPigClick: () -> Unit,
    onNavigateTo: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Rotating Herd Summary Card
        StatsRibbon(
            stats = herdStats,
            onShowArchived = { onNavigateTo(Screen.ArchivedPigs.route) }
        )

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAddPigClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource("add_pig_btn"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick = { onNavigateTo(Screen.HerdData.route) },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, primaryColor),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource("view_all"), color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
            }
        }

        // 2. Pigs List Preview (up to 10 active pigs, excluding archived)
        val activePigs = remember(allPigs) {
            allPigs.filter { pig ->
                !pig.location.equals("Archived", ignoreCase = true) &&
                !pig.status.contains("Archived", ignoreCase = true) &&
                !pig.status.contains("Culled", ignoreCase = true) &&
                !pig.status.equals("Sold", ignoreCase = true) &&
                !pig.status.equals("Dead", ignoreCase = true) &&
                !pig.status.equals("Slaughtered", ignoreCase = true) &&
                pig.statusEnum != PigStatus.ARCHIVED &&
                pig.statusEnum != PigStatus.CULLED
            }.take(10)
        }
        if (activePigs.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Text(
                    text = stringResource("no_pigs_yet"),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${stringResource("herd_data")} (${activePigs.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                activePigs.forEach { pig ->
                    CompactPigDropdownCard(
                        pig = pig,
                        onClick = { onNavigateTo(Screen.PigProfile.createRoute(pig.id)) }
                    )
                }
            }
        }
    }
}
