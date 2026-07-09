package com.app.attops.features.tasks.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.attops.core.network.model.Task
import com.app.attops.core.network.model.TaskPriority
import com.app.attops.core.network.model.TaskStatus

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                PriorityBadge(priority = task.priority)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (!task.locationName.isNullOrEmpty()) {
                Text(
                    text = "📍 ${task.locationName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = task.status)
                if (!task.dueDate.isNullOrEmpty()) {
                    Text(
                        text = "Due: ${task.dueDate?.split("T")?.firstOrNull()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: TaskPriority) {
    val color = when (priority) {
        TaskPriority.HIGH -> Color(0xFFEF4444)
        TaskPriority.MEDIUM -> Color(0xFFF59E0B)
        TaskPriority.LOW -> Color(0xFF10B981)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = priority.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusBadge(status: TaskStatus) {
    val color = when (status) {
        TaskStatus.PENDING -> Color.Gray
        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        TaskStatus.COMPLETED -> Color(0xFF10B981)
        TaskStatus.CANCELLED -> Color.Red
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status.name.replace("_", " "),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
