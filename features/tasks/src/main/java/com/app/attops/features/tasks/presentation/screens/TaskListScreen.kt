package com.app.attops.features.tasks.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.attops.core.network.model.TaskStatus
import com.app.attops.core.network.model.UserRole
import com.app.attops.features.tasks.presentation.components.TaskCard
import com.app.attops.features.tasks.presentation.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onTaskClick: (String) -> Unit,
    onCreateTaskClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val userRole = uiState.userRole
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Pending", "Active", "Completed")

    val filteredTasks = remember(uiState.tasks, selectedTab) {
        when (selectedTab) {
            1 -> uiState.tasks.filter { it.status == TaskStatus.PENDING }
            2 -> uiState.tasks.filter { it.status == TaskStatus.IN_PROGRESS }
            3 -> uiState.tasks.filter { it.status == TaskStatus.COMPLETED }
            else -> uiState.tasks
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Organization Tasks") }
                )
                SecondaryTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if ((userRole == UserRole.OWNER) || (userRole == UserRole.ADMIN)) {
                FloatingActionButton(onClick = onCreateTaskClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create Task")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
            } else if (filteredTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tasks found in this category.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTasks) { task ->
                        TaskCard(
                            task = task,
                            onClick = { onTaskClick(task.id ?: "") }
                        )
                    }
                }
            }
        }
    }
}
