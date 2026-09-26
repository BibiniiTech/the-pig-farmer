package com.example.smartswine.ui.dashboard.components.grid

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartswine.ui.training.TrainingTipsData
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.Translator

@Composable
fun TrainingSectionContent(
    openTrainingCategory: String?,
    onToggleCategory: (String) -> Unit,
    primaryColor: Color,
    onRegisterCoordinates: (String, LayoutCoordinates) -> Unit,
    onItemExpanded: (String) -> Unit
) {
    val currentLanguage = LocalAppLanguage.current
    val tipCategories = remember {
        listOf(
            "cat_weaning" to "Weaning Management",
            "cat_feeding" to "Feed Management & Nutrition",
            "cat_breeding" to "Breeding & Genetics",
            "cat_health" to "Disease Prevention & Bio-Security",
            "cat_housing" to "Housing & Environmental Control",
            "cat_general" to "General Operations"
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tipCategories.forEach { (catKey, defaultName) ->
            val catTips = remember(catKey) {
                TrainingTipsData.allTrainingTips.filter { it.categoryKey == catKey }
            }
            val isCatExpanded = openTrainingCategory == catKey

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (isCatExpanded) primaryColor else primaryColor.copy(alpha = 0.22f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { onRegisterCoordinates(catKey, it) }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onToggleCategory(catKey)
                                if (!isCatExpanded) onItemExpanded(catKey)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = primaryColor.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${catTips.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    )
                                }
                            }
                            Text(
                                text = Translator.getStringWithDefault(catKey, currentLanguage.code, defaultName),
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (isCatExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isCatExpanded,
                        enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                        exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(150))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            catTips.forEach { tip ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = primaryColor.copy(alpha = 0.04f),
                                    border = BorderStroke(0.8.dp, primaryColor.copy(alpha = 0.18f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = Translator.getStringWithDefault(tip.titleKey, currentLanguage.code, tip.defaultTitle),
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                            color = primaryColor
                                        )
                                        Text(
                                            text = Translator.getStringWithDefault(tip.contentKey, currentLanguage.code, tip.defaultContent),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 20.sp
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
}
