package com.example.smartswine.ui.dashboard.components.grid

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibiniitech.smartswine.R
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.utils.getTranslatedActivityType
import com.example.smartswine.utils.stringResource

private data class ActivityData(
    val name: String,
    val iconResId: Int? = null,
    val icon: ImageVector? = null
)

@Composable
fun ProductionSectionContent(
    primaryColor: Color,
    onNavigateTo: (String) -> Unit
) {
    var activityPage by remember { mutableIntStateOf(0) }

    val activityItems = remember {
        listOf(
            ActivityData("Heat Detection", iconResId = R.drawable.ic_heat),
            ActivityData("Breeding/Mating", iconResId = R.drawable.ic_breeding),
            ActivityData("Confirm Pregnancy", iconResId = R.drawable.ic_pregnancy_check),
            ActivityData("Farrowing", iconResId = R.drawable.ic_farrowing),
            ActivityData("Weaning", iconResId = R.drawable.ic_weaning),
            ActivityData("Castration", iconResId = R.drawable.ic_castration),
            ActivityData("Teeth Clipping", iconResId = R.drawable.ic_teeth_clipping),
            ActivityData("Tail Docking", iconResId = R.drawable.ic_tail_docking),
            ActivityData("Deworming", iconResId = R.drawable.ic_deworming),
            ActivityData("Iron Injection", iconResId = R.drawable.ic_iron),
            ActivityData("Vaccination", iconResId = R.drawable.ic_vaccination),
            ActivityData("Medication", iconResId = R.drawable.ic_medication),
            ActivityData("Culling", iconResId = R.drawable.ic_culling),
            ActivityData("Custom", icon = Icons.AutoMirrored.Filled.NoteAdd)
        )
    }

    val pagedActivities = remember(activityPage) {
        if (activityPage == 0) activityItems.take(8) else activityItems.drop(8)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        pagedActivities.forEach { act ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigateTo(Screen.ProductionActivities.createRoute(act.name)) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = primaryColor.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            act.iconResId?.let { resId ->
                                Icon(
                                    painter = painterResource(id = resId),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = primaryColor
                                )
                            } ?: act.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = primaryColor
                                )
                            }
                        }
                    }
                    Text(
                        text = getTranslatedActivityType(act.name),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = primaryColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (activityPage == 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { activityPage = 1 }) {
                    Text(stringResource("next"), color = primaryColor, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(onClick = { activityPage = 0 }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource("previous"), color = primaryColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
