package com.example.smartswine.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibiniitech.smartswine.R
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.ui.theme.DarkBackground
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.LocalIsPremium
import com.example.smartswine.utils.Translator

@Composable
fun ManagementGrid(
    onNavigateTo: (String) -> Unit
) {
    val currentLanguage = LocalAppLanguage.current
    val isPremium = LocalIsPremium.current
    
    val managementCategories = remember(currentLanguage) {
        listOf(
            ManagementCategory(
                label = Translator.getString("herd_data", currentLanguage.code),
                iconResId = R.drawable.ic_herd_data,
                screen = Screen.HerdData,
                themeColor = Color(0xFF2E7D32),
                themeColorDark = Color(0xFF81C784),
                bgColorLight = Color(0xFFE8F5E9),
                bgColorDark = Color(0xFF1B5E20)
            ),
            ManagementCategory(
                label = Translator.getString("feed", currentLanguage.code),
                iconResId = R.drawable.ic_feed2,
                screen = Screen.Feed,
                themeColor = Color(0xFFE65100),
                themeColorDark = Color(0xFFFFB74D),
                bgColorLight = Color(0xFFFFF3E0),
                bgColorDark = Color(0xFFE65100)
            ),
            ManagementCategory(
                label = Translator.getString("herd_activities", currentLanguage.code),
                iconResId = R.drawable.ic_herd_activities,
                screen = Screen.ProductionActivities,
                themeColor = Color(0xFF00838F),
                themeColorDark = Color(0xFF4DD0E1),
                bgColorLight = Color(0xFFE0F7FA),
                bgColorDark = Color(0xFF006064)
            ),
            ManagementCategory(
                label = Translator.getString("financials", currentLanguage.code),
                icon = Icons.Default.Payments,
                screen = Screen.Financials,
                themeColor = Color(0xFF00796B),
                themeColorDark = Color(0xFF4DB6AC),
                bgColorLight = Color(0xFFE0F2F1),
                bgColorDark = Color(0xFF004D40)
            ),
            ManagementCategory(
                label = Translator.getString("human_resources", currentLanguage.code),
                icon = Icons.Default.Groups,
                screen = Screen.HumanResource,
                themeColor = Color(0xFF7B1FA2),
                themeColorDark = Color(0xFFBA68C8),
                bgColorLight = Color(0xFFF3E5F5),
                bgColorDark = Color(0xFF4A148C)
            ),
            ManagementCategory(
                label = Translator.getString("market", currentLanguage.code),
                icon = Icons.Default.Storefront,
                screen = Screen.MarketAccess,
                themeColor = Color(0xFFC2185B),
                themeColorDark = Color(0xFFF06292),
                bgColorLight = Color(0xFFFFEBEE),
                bgColorDark = Color(0xFF880E4F)
            ),
            ManagementCategory(
                label = Translator.getString("symptoms_analyzer", currentLanguage.code),
                iconResId = R.drawable.ic_symptoms_analyzer,
                screen = Screen.DiseaseFinder,
                themeColor = Color(0xFF3F51B5),
                themeColorDark = Color(0xFF7986CB),
                bgColorLight = Color(0xFFE8EAF6),
                bgColorDark = Color(0xFF1A237E)
            ),
            ManagementCategory(
                label = Translator.getString("weight_checker", currentLanguage.code),
                iconResId = R.drawable.ic_weight_checker,
                screen = Screen.WeightChecker,
                themeColor = Color(0xFF455A64),
                themeColorDark = Color(0xFF90A4AE),
                bgColorLight = Color(0xFFECEFF1),
                bgColorDark = Color(0xFF37474F)
            ),
            ManagementCategory(
                label = Translator.getString("training", currentLanguage.code),
                icon = Icons.Default.School,
                screen = Screen.Training,
                themeColor = Color(0xFF5D4037),
                themeColorDark = Color(0xFFA1887F),
                bgColorLight = Color(0xFFEFEBE9),
                bgColorDark = Color(0xFF3E2723)
            )
        )
    }

    managementCategories.forEach { item ->
        val isLocked = item.screen == Screen.DiseaseFinder && !isPremium
        val isDark = MaterialTheme.colorScheme.background == DarkBackground
        val primaryColor = if (isDark) item.themeColorDark else item.themeColor
        val bgColor = if (isDark) item.bgColorDark else item.bgColorLight
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 2.dp)
                .height(105.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp),
                    clip = false,
                    ambientColor = if (isDark) primaryColor.copy(alpha = 0.3f) else primaryColor.copy(alpha = 0.35f),
                    spotColor = if (isDark) primaryColor.copy(alpha = 0.5f) else primaryColor.copy(alpha = 0.6f)
                )
                .border(
                    BorderStroke(
                        width = 1.dp,
                        color = primaryColor.copy(alpha = if (isDark) 0.3f else 0.4f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            bgColor.copy(alpha = if (isDark) 0.5f else 0.95f),
                            bgColor.copy(alpha = if (isDark) 0.2f else 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
                .clickable {
                    if (isLocked) {
                        onNavigateTo(Screen.Paywall.route)
                    } else {
                        onNavigateTo(item.screen.route) 
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        item.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                tint = primaryColor
                            )
                        } ?: item.iconResId?.let { resId ->
                            Icon(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                tint = primaryColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.3).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Translator.getString(getCategorySubtitleKey(item.screen), currentLanguage.code),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isLocked) MaterialTheme.colorScheme.error else primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private data class ManagementCategory(
    val label: String,
    val icon: ImageVector? = null,
    val iconResId: Int? = null,
    val screen: Screen,
    val themeColor: Color,
    val themeColorDark: Color,
    val bgColorLight: Color,
    val bgColorDark: Color
)

private fun getCategorySubtitleKey(screen: Screen): String {
    return when (screen) {
        Screen.HerdData -> "sub_herd_data"
        Screen.Feed -> "sub_feed"
        Screen.ProductionActivities -> "sub_herd_activities"
        Screen.Financials -> "sub_financials"
        Screen.HumanResource -> "sub_human_resources"
        Screen.MarketAccess -> "sub_market"
        Screen.DiseaseFinder -> "sub_symptoms_analyzer"
        Screen.WeightChecker -> "sub_weight_checker"
        Screen.Training -> "sub_training"
        else -> ""
    }
}
