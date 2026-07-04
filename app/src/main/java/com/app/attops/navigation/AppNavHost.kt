package com.app.attops.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.app.attops.core.navigation.Destination
import com.app.attops.features.auth.navigation.authNavGraph
import com.app.attops.features.dashboard.navigation.dashboardNavGraph
import com.app.attops.features.employee.navigation.employeeNavGraph

@Composable
fun AttOpsNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Destination = Destination.AuthGraph
) {
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
    }
}
