package com.app.attops.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.app.attops.core.navigation.Destination
import com.app.attops.core.navigation.NavigationBus
import com.app.attops.features.auth.navigation.authNavGraph
import com.app.attops.features.dashboard.navigation.dashboardNavGraph
import com.app.attops.features.employee.navigation.employeeNavGraph
import com.app.attops.features.tasks.navigation.taskNavGraph

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.navigation.compose.composable

@Composable
fun AttOpsNavHost(
    navController: NavHostController,
    navigationBus: NavigationBus,
    modifier: Modifier = Modifier,
    startDestination: Destination = Destination.AuthGraph
) {
    LaunchedEffect(Unit) {
        navigationBus.events.collect { destination ->
            navController.navigate(destination)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Module 1-3: Auth & Onboarding
        authNavGraph(navController)

        // Module 4: Basic Dashboard
        dashboardNavGraph(
            navController = navController,
            onLogout = {
                navController.navigate(Destination.AuthGraph) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onNavigateToEmployees = {
                navController.navigate(Destination.Employees)
            },
            onNavigateToTasks = {
                navController.navigate(Destination.Tasks)
            }
        )

        // Module 5-6: Employee Management
        employeeNavGraph(navController)

        // Phase 4: Tasks & Attendance
        taskNavGraph(navController)

        composable<Destination.Reports> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Reports Module (Coming Soon)")
            }
        }
    }
}
