package com.app.attops.features.dashboard.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.app.attops.core.navigation.Destination
import com.app.attops.features.dashboard.presentation.screens.DashboardScreen
import com.app.attops.features.dashboard.presentation.screens.NotificationScreen
import com.app.attops.features.dashboard.presentation.screens.ProfileScreen
import com.app.attops.features.dashboard.presentation.state.DashboardUiEvent
import com.app.attops.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.app.attops.features.dashboard.presentation.viewmodel.NotificationViewModel

fun NavGraphBuilder.dashboardNavGraph(
    navController: NavHostController,
    onLogout: () -> Unit,
    onNavigateToEmployees: () -> Unit,
    onNavigateToTasks: () -> Unit
) {
    composable<Destination.Dashboard> {
        val viewModel: DashboardViewModel = hiltViewModel()
        
        LaunchedEffect(Unit) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is DashboardUiEvent.NavigateToEmployees -> onNavigateToEmployees()
                    is DashboardUiEvent.NavigateToTasks -> onNavigateToTasks()
                    is DashboardUiEvent.NavigateToAuth -> onLogout()
                    else -> {}
                }
            }
        }

        DashboardScreen(
            viewModel = viewModel,
            onNotificationsClick = { navController.navigate(Destination.Notifications) }
        )
    }

    composable<Destination.Notifications> {
        val viewModel: NotificationViewModel = hiltViewModel()
        NotificationScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
            onNotificationClick = { type, taskId ->
                if (taskId != null) {
                    navController.navigate(Destination.TaskDetails(taskId))
                }
            }
        )
    }

    composable<Destination.Profile> {
        val viewModel: DashboardViewModel = hiltViewModel()

        LaunchedEffect(Unit) {
            viewModel.uiEvent.collect { event ->
                if (event is DashboardUiEvent.NavigateToAuth) {
                    onLogout()
                }
            }
        }

        ProfileScreen(
            viewModel = viewModel
        )
    }
}
