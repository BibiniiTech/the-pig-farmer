package com.example.smartswine.ui.dashboard.components.grid

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartswine.model.FinancialRecord
import com.example.smartswine.model.StaffMember
import com.example.smartswine.ui.hr.StaffAvatar
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.utils.TierLimiter
import com.example.smartswine.utils.stringResource
import java.util.Locale

@Composable
fun HumanResourcesSectionContent(
    staff: List<StaffMember>,
    financialRecords: List<FinancialRecord>,
    currencySymbol: String,
    primaryColor: Color,
    isPremium: Boolean,
    isPaidPremium: Boolean,
    onAddStaffClick: () -> Unit,
    onExportPdfClick: () -> Unit,
    onViewStaffProfile: (String) -> Unit,
    onPaySalary: (StaffMember) -> Unit,
    onEditStaff: (StaffMember) -> Unit,
    onNavigateTo: (String) -> Unit
) {
    val activeStaff = remember(staff) {
        staff.filter { !it.status.equals("Archived", ignoreCase = true) }
    }
    val totalStaffCount = staff.size
    val activeStaffCount = activeStaff.count { !it.status.equals("Inactive", ignoreCase = true) }
    val monthlyPayrollTotal = activeStaff.sumOf { it.salary }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.22f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource("total_staff"), style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$totalStaffCount", style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp), fontWeight = FontWeight.Bold, color = primaryColor)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource("active_staff"), style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$activeStaffCount", style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp), fontWeight = FontWeight.Bold, color = primaryColor)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource("monthly_payroll"), style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = String.format(Locale.getDefault(), "%.2f", monthlyPayrollTotal), style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp), fontWeight = FontWeight.Bold, color = primaryColor)
                }
            }
        }

        val staffLimitReached = !isPaidPremium && staff.size >= TierLimiter.FREE_MAX_STAFF
        Button(
            onClick = { 
                if (staffLimitReached) onNavigateTo(Screen.Paywall.route)
                else onAddStaffClick() 
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = if (staffLimitReached) MaterialTheme.colorScheme.error else primaryColor),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(if (staffLimitReached) Icons.Default.Lock else Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (staffLimitReached) "Staff Limit Reached (${TierLimiter.FREE_MAX_STAFF}) - Upgrade" else stringResource("add_employee"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        OutlinedButton(
            onClick = onExportPdfClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, primaryColor)
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp), tint = primaryColor)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource("export_to_pdf"),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = primaryColor
            )
        }

        if (activeStaff.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Text(
                    text = stringResource("no_staff_found"),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                activeStaff.forEach { member ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.22f)),
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onViewStaffProfile(member.id) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StaffAvatar(photoUrl = member.photoUrl, name = member.name, size = 48.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${member.role} • ${member.phone.ifBlank { "No phone" }}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "View Profile",
                                    tint = primaryColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            HorizontalDivider(color = primaryColor.copy(alpha = 0.1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onPaySalary(member) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource("log_salary"), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onEditStaff(member) },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, primaryColor),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource("edit"), color = primaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
