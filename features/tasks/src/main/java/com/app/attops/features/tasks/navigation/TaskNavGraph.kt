package com.app.attops.features.tasks.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.app.attops.core.navigation.Destination
import com.app.attops.core.network.model.UserRole
import com.app.attops.features.tasks.presentation.screens.*
import com.app.attops.features.tasks.presentation.viewmodel.TaskViewModel

fun NavGraphBuilder.taskNavGraph(
    navController: NavHostController
) {
    composable<Destination.Tasks> {
        val viewModel: TaskViewModel = hiltViewModel()
        TaskListScreen(
            viewModel = viewModel,
            onTaskClick = { taskId ->
                navController.navigate(Destination.TaskDetails(taskId))
            },
            onCreateTaskClick = {
                navController.navigate(Destination.CreateTask)
            }
        )
    }

    composable<Destination.TaskDetails> { backStackEntry ->
        val args: Destination.TaskDetails = backStackEntry.toRoute()
        val viewModel: TaskViewModel = hiltViewModel()
        TaskDetailsScreen(
            taskId = args.taskId,
            viewModel = viewModel,
            onBackClick = { navController.popBackStack() }
        )
    }

    composable<Destination.CreateTask> {
        val viewModel: TaskViewModel = hiltViewModel()
        
        // This ensures the ViewModel doesn't reset when we jump back from Maps
        CreateTaskScreen(
            viewModel = viewModel,
            onBackClick = { navController.popBackStack() }
        )
    }
}
