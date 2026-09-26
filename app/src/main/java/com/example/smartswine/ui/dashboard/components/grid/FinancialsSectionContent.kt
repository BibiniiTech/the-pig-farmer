package com.example.smartswine.ui.dashboard.components.grid

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartswine.model.FinancialRecord
import com.example.smartswine.ui.financials.FinancialSummaryCard
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.utils.stringResource
import java.util.Locale

@Composable
fun FinancialsSectionContent(
    financialRecords: List<FinancialRecord>,
    currencySymbol: String,
    primaryColor: Color,
    onNavigateTo: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = { onNavigateTo(Screen.Financials.createRoute(showAdd = true)) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource("add_entry"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        FinancialSummaryCard(records = financialRecords, currencySymbol = currencySymbol)

        OutlinedButton(
            onClick = { onNavigateTo(Screen.Financials.route) },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, primaryColor),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource("view_all"), color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
        }

        if (financialRecords.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource("recent_transactions"),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                financialRecords.take(10).forEach { record ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateTo(Screen.Financials.route) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.category, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(
                                    "${record.date} • ${record.description}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = (if (record.type == "Income") "+$currencySymbol" else "-$currencySymbol") + String.format(Locale.getDefault(), "%.2f", record.amount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (record.type == "Income") Color(0xFF4CAF50) else Color.Red
                            )
                        }
                    }
                }
            }
        }
    }
}
