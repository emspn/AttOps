package com.app.attops.features.reports.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.app.attops.core.navigation.Destination
import com.app.attops.features.reports.presentation.screens.EmployeeTimesheetScreen
import com.app.attops.features.reports.presentation.screens.IntegrityScorecardScreen
import com.app.attops.features.reports.presentation.viewmodel.ReportsViewModel

fun NavGraphBuilder.reportsNavGraph(
    navController: NavHostController
) {
    composable<Destination.Reports> {
        val viewModel: ReportsViewModel = hiltViewModel()
        IntegrityScorecardScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
            onEmployeeClick = { employeeId: String, fullName: String ->
                navController.navigate(Destination.EmployeeTimesheet(employeeId, fullName))
            }
        )
    }

    composable<Destination.EmployeeTimesheet> { backStackEntry ->
        val args: Destination.EmployeeTimesheet = backStackEntry.toRoute()
        val viewModel: ReportsViewModel = hiltViewModel()
        EmployeeTimesheetScreen(
            employeeId = args.employeeId,
            fullName = args.fullName,
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
        )
    }
}
