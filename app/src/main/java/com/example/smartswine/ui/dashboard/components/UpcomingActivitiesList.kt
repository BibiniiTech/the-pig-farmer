package com.example.smartswine.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smartswine.model.TaskGroup
import com.example.smartswine.model.TaskItem
import com.example.smartswine.ui.dashboard.getTaskIcon
import com.example.smartswine.ui.dashboard.TaskIcon
import com.example.smartswine.ui.dashboard.getTranslatedActivityName
import com.example.smartswine.utils.stringResource

@Composable
fun UpcomingActivitiesList(
    groupedTasks: List<TaskGroup>,
    onTaskGroupClick: (List<TaskItem>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = stringResource("upcoming_activities"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (groupedTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource("no_upcoming_activities"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groupedTasks) { taskGroup ->
                    ElevatedCard(
                        onClick = { onTaskGroupClick(taskGroup.originalTasks) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = if (taskGroup.isOverdue) {
                            CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else {
                            CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                                modifier = Modifier.weight(1f)
                            ) {
                                when (val taskIcon = getTaskIcon(taskGroup.activity)) {
                                    is TaskIcon.Vector -> Icon(
                                        imageVector = taskIcon.imageVector,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = if (taskGroup.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    is TaskIcon.Resource -> Icon(
                                        painter = painterResource(id = taskIcon.resId),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = if (taskGroup.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = getTranslatedActivityName(taskGroup.activity),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (taskGroup.target == "General") stringResource("general") else taskGroup.target,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (taskGroup.isOverdue) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Text(
                                text = taskGroup.date,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (taskGroup.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
