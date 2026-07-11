package com.app.attops.features.tasks.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.attops.core.network.model.TaskStatus
import com.app.attops.core.network.model.UserRole
import com.app.attops.features.tasks.presentation.components.TaskCard
import com.app.attops.features.tasks.presentation.viewmodel.TaskSortOrder
import com.app.attops.features.tasks.presentation.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onTaskClick: (String) -> Unit,
    onCreateTaskClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userRole = uiState.userRole
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Pending", "Active", "Review", "Done")
    
    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val filteredTasks = remember(uiState.filteredTasks, selectedTab) {
        when (selectedTab) {
            1 -> uiState.filteredTasks.filter { it.status == TaskStatus.PENDING }
            2 -> uiState.filteredTasks.filter { it.status == TaskStatus.IN_PROGRESS }
            3 -> uiState.filteredTasks.filter { it.status == TaskStatus.COMPLETED }
            4 -> uiState.filteredTasks.filter { it.status == TaskStatus.APPROVED }
            else -> uiState.filteredTasks
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 3.dp
            ) {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        title = {
                            if (isSearchActive) {
                                TextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.onSearchQueryChange(it) },
                                    placeholder = { Text("Search tasks...") },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            } else {
                                Text("Tasks", fontWeight = FontWeight.ExtraBold)
                            }
                        },
                        navigationIcon = {
                            if (isSearchActive) {
                                IconButton(onClick = { 
                                    isSearchActive = false
                                    viewModel.onSearchQueryChange("")
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            if (!isSearchActive) {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            }
                            
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.Default.FilterList, contentDescription = "Sort")
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Recently Added") },
                                        onClick = { 
                                            viewModel.onSortOrderChange(TaskSortOrder.RECENTLY_ADDED)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Due Date (Earliest)") },
                                        onClick = { 
                                            viewModel.onSortOrderChange(TaskSortOrder.DUE_DATE_ASC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Priority (High First)") },
                                        onClick = { 
                                            viewModel.onSortOrderChange(TaskSortOrder.PRIORITY_HIGH)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    )

                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 0.dp, // Maximize space for tabs
                        modifier = Modifier.fillMaxWidth(),
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { 
                                    Text(
                                        text = title, 
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selectedTab == index) FontWeight.ExtraBold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    ) 
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if ((userRole == UserRole.OWNER) || (userRole == UserRole.ADMIN)) {
                // Raise FAB to clear the floating bottom navigation bar
                Box(modifier = Modifier.navigationBarsPadding().padding(bottom = 86.dp)) {
                    FloatingActionButton(
                        onClick = onCreateTaskClick,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        }
    ) { padding ->
        // IMPORTANT: Use padding provided by Scaffold to avoid overlap
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = filteredTasks, 
                label = "TaskListAnimation",
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { tasks ->
                if (uiState.isLoading && tasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator() 
                    }
                } else if (tasks.isEmpty()) {
                    EmptyState(tabs[selectedTab])
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(tasks, key = { it.id ?: it.hashCode() }) { task ->
                            TaskCard(task = task, onClick = { onTaskClick(task.id ?: "") })
                        }
                        item { Spacer(modifier = Modifier.height(150.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(category: String) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(imageVector = Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
        Text(text = "No $category Tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
