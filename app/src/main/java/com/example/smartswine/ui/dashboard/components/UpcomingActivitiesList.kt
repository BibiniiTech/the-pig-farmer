package com.example.smartswine.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibiniitech.smartswine.R
import com.example.smartswine.model.FarmAlert
import com.example.smartswine.model.TaskGroup
import com.example.smartswine.model.TaskItem
import com.example.smartswine.ui.dashboard.TaskIcon
import com.example.smartswine.ui.dashboard.getTaskIcon
import com.example.smartswine.ui.dashboard.getTranslatedActivityName
import com.example.smartswine.utils.stringResource

@Composable
fun UpcomingActivitiesList(
    taskAlerts: List<FarmAlert.TaskAlert> = emptyList(),
    weightAlerts: List<FarmAlert.WeightAlert> = emptyList(),
    stockAlerts: List<FarmAlert.LowStockAlert> = emptyList(),
    onTaskAlertClick: (FarmAlert.TaskAlert) -> Unit,
    onSnoozeTask: (List<TaskItem>) -> Unit = {},
    onSnoozeTaskDuration: (List<TaskItem>, Int) -> Unit = { tasks, hours -> },
    onDeleteTask: (List<TaskItem>) -> Unit = {},
    onSnoozeWeightAlert: (String, Int) -> Unit = { _, _ -> },
    onDismissWeightAlert: (String) -> Unit = {},
    onSnoozeStockAlert: (String, Int) -> Unit = { _, _ -> },
    onDismissStockAlert: (String) -> Unit = {},
    onNavigateToWeightChecker: (String) -> Unit = {},
    onNavigateToFeed: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val totalCount = taskAlerts.size + weightAlerts.size + stockAlerts.size
    var pendingDeleteAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    if (pendingDeleteAction != null) {
        val (itemTitle, onConfirm) = pendingDeleteAction!!
        AlertDialog(
            onDismissRequest = { pendingDeleteAction = null },
            title = {
                Text(
                    text = stringResource("delete_notification_question"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource("delete_notification_confirm", itemTitle))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        pendingDeleteAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource("delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAction = null }) {
                    Text(stringResource("cancel"))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .navigationBarsPadding()
    ) {
        // ─── HEADER ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource("notifications"),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = if (totalCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "$totalCount ${stringResource("notifications")}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (totalCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─── CONTENT LIST ──────────────────────────────────────────
        if (totalCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource("all_caught_up"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource("no_pending_notifications"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Tasks / Procedures
                if (taskAlerts.isNotEmpty()) {
                    item {
                        SectionHeaderLabel(
                            title = "${stringResource("herd_procedures_tasks")} (${taskAlerts.size})",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(taskAlerts) { taskAlert ->
                        val activityTitle = getTranslatedActivityName(taskAlert.activity)
                        TaskAlertCard(
                            taskAlert = taskAlert,
                            onClick = { onTaskAlertClick(taskAlert) },
                            onSnoozeDuration = { hours -> onSnoozeTaskDuration(taskAlert.originalTasks, hours) },
                            onDelete = {
                                pendingDeleteAction = Pair(activityTitle) {
                                    onDeleteTask(taskAlert.originalTasks)
                                }
                            }
                        )
                    }
                }

                // 2. Weight Check Reminders
                if (weightAlerts.isNotEmpty()) {
                    item {
                        SectionHeaderLabel(
                            title = "${stringResource("weight_check_reminders")} (${weightAlerts.size})",
                            color = Color(0xFFF57F17)
                        )
                    }
                    items(weightAlerts) { weightAlert ->
                        val pigLabel = stringResource("pig")
                        WeightAlertCard(
                            weightAlert = weightAlert,
                            onWeighClick = {
                                onDismiss()
                                onNavigateToWeightChecker(weightAlert.pig.tagNumber)
                            },
                            onSnooze = { days -> onSnoozeWeightAlert(weightAlert.pig.id, days) },
                            onDismiss = {
                                pendingDeleteAction = Pair("$pigLabel #${weightAlert.pig.tagNumber}") {
                                    onDismissWeightAlert(weightAlert.pig.id)
                                }
                            }
                        )
                    }
                }

                // 3. Low Feed Stock Alerts
                if (stockAlerts.isNotEmpty()) {
                    item {
                        SectionHeaderLabel(
                            title = "${stringResource("low_inventory_alerts")} (${stockAlerts.size})",
                            color = Color(0xFFD84315)
                        )
                    }
                    items(stockAlerts) { stockAlert ->
                        StockAlertCard(
                            stockAlert = stockAlert,
                            onRestockClick = {
                                onDismiss()
                                onNavigateToFeed()
                            },
                            onSnooze = { hours -> onSnoozeStockAlert(stockAlert.item.id, hours) },
                            onDismiss = {
                                pendingDeleteAction = Pair(stockAlert.item.name) {
                                    onDismissStockAlert(stockAlert.item.id)
                                }
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeaderLabel(
    title: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = color,
        letterSpacing = 1.1.sp,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 4.dp)
    )
}

@Composable
private fun NotificationFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun TaskAlertCard(
    taskAlert: FarmAlert.TaskAlert,
    onClick: () -> Unit,
    onSnoozeDuration: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var showSnoozeMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (taskAlert.isOverdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            if (taskAlert.isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Activity Icon squircle
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (taskAlert.isOverdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when (val taskIcon = getTaskIcon(taskAlert.activity)) {
                            is TaskIcon.Vector -> Icon(
                                imageVector = taskIcon.imageVector,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (taskAlert.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            is TaskIcon.Resource -> Icon(
                                painter = painterResource(id = taskIcon.resId),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (taskAlert.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getTranslatedActivityName(taskAlert.activity),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val targetDisplay = when {
                        taskAlert.target.equals("General", ignoreCase = true) -> stringResource("general")
                        taskAlert.target.startsWith("Pigs ", ignoreCase = true) -> "${stringResource("pigs")} ${taskAlert.target.substring(5)}"
                        taskAlert.target.startsWith("Pig ", ignoreCase = true) -> "${stringResource("pig")} ${taskAlert.target.substring(4)}"
                        taskAlert.target.contains(",") -> "${stringResource("pigs")} ${taskAlert.target}"
                        taskAlert.target.isNotBlank() -> "${stringResource("pig")} ${taskAlert.target}"
                        else -> stringResource("general")
                    }
                    Text(
                        text = targetDisplay,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (taskAlert.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (taskAlert.isOverdue) stringResource("overdue").uppercase() else "${stringResource("due")} ${taskAlert.date}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (taskAlert.isOverdue) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (taskAlert.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = if (taskAlert.isOverdue) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = stringResource("record"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        IconButton(
                            onClick = { showSnoozeMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = stringResource("snooze"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSnoozeMenu,
                            onDismissRequest = { showSnoozeMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text(stringResource("snooze_1_hour"), fontSize = 15.sp) }, onClick = { onSnoozeDuration(1); showSnoozeMenu = false })
                            DropdownMenuItem(text = { Text(stringResource("snooze_24_hours_tomorrow"), fontSize = 15.sp) }, onClick = { onSnoozeDuration(24); showSnoozeMenu = false })
                            DropdownMenuItem(text = { Text(stringResource("snooze_3_days"), fontSize = 15.sp) }, onClick = { onSnoozeDuration(72); showSnoozeMenu = false })
                            DropdownMenuItem(text = { Text(stringResource("snooze_1_week"), fontSize = 15.sp) }, onClick = { onSnoozeDuration(168); showSnoozeMenu = false })
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource("delete"),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightAlertCard(
    weightAlert: FarmAlert.WeightAlert,
    onWeighClick: () -> Unit,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var showSnoozeMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onWeighClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, Color(0xFFF57F17).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF57F17).copy(alpha = 0.18f)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_weight_checker),
                            contentDescription = null,
                            tint = Color(0xFFF57F17),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource("weight_check_reminder"),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stringResource("pig_tag_prefix")}${weightAlert.pig.tagNumber} (${weightAlert.pig.breed.ifEmpty { stringResource("breeder") }})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource("last_weighed_days_ago", weightAlert.daysSinceLastWeigh),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF57F17)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onWeighClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = stringResource("weigh"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        IconButton(
                            onClick = { showSnoozeMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = stringResource("snooze"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSnoozeMenu,
                            onDismissRequest = { showSnoozeMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text(stringResource("snooze_1_week"), fontSize = 15.sp) }, onClick = { onSnooze(7); showSnoozeMenu = false })
                            DropdownMenuItem(text = { Text(stringResource("snooze_2_weeks"), fontSize = 15.sp) }, onClick = { onSnooze(14); showSnoozeMenu = false })
                            DropdownMenuItem(text = { Text(stringResource("snooze_1_month"), fontSize = 15.sp) }, onClick = { onSnooze(30); showSnoozeMenu = false })
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource("delete"),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockAlertCard(
    stockAlert: FarmAlert.LowStockAlert,
    onRestockClick: () -> Unit,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var showSnoozeMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onRestockClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, Color(0xFFD84315).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFD84315).copy(alpha = 0.18f)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_feed2),
                            contentDescription = null,
                            tint = Color(0xFFD84315),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource("low_feed_stock"),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stockAlert.item.name} (${stockAlert.item.feedType})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource("stock_left_min_format", stockAlert.currentQty, stockAlert.unit, stockAlert.minThreshold),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF7043)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onRestockClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = stringResource("restock"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        IconButton(
                            onClick = { showSnoozeMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = stringResource("snooze"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSnoozeMenu,
                            onDismissRequest = { showSnoozeMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text(stringResource("snooze_24_hours"), fontSize = 15.sp) }, onClick = { onSnooze(24); showSnoozeMenu = false })
                            DropdownMenuItem(text = { Text(stringResource("snooze_3_days"), fontSize = 15.sp) }, onClick = { onSnooze(72); showSnoozeMenu = false })
                            DropdownMenuItem(text = { Text(stringResource("snooze_1_week"), fontSize = 15.sp) }, onClick = { onSnooze(168); showSnoozeMenu = false })
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource("delete"),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

