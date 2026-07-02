package com.app.attops.features.employee.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.app.attops.core.navigation.Destination
import com.app.attops.features.employee.presentation.screens.AddEmployeeScreen
import com.app.attops.features.employee.presentation.screens.EditEmployeeScreen
import com.app.attops.features.employee.presentation.screens.EmployeeListScreen
import com.app.attops.features.employee.presentation.state.EmployeeUiEvent
import com.app.attops.features.employee.presentation.viewmodel.EmployeeViewModel

fun NavGraphBuilder.employeeNavGraph(navController: NavHostController) {
    composable<Destination.Employees> {
        val viewModel: EmployeeViewModel = hiltViewModel()
        EmployeeListScreen(
            viewModel = viewModel,
            onEmployeeClick = { employeeId ->
                navController.navigate(Destination.EditEmployee(employeeId))
            },
            onAddEmployeeClick = {
                navController.navigate(Destination.AddEmployee)
            }
        )
    }

    composable<Destination.AddEmployee> {
        val viewModel: EmployeeViewModel = hiltViewModel()
        
        LaunchedEffect(Unit) {
            viewModel.uiEvent.collect { event ->
                if (event is EmployeeUiEvent.NavigateBack) {
                    navController.popBackStack()
                }
            }
        }

        AddEmployeeScreen(
            viewModel = viewModel,
            onBackClick = { navController.popBackStack() }
        )
    }

    composable<Destination.EditEmployee> { backStackEntry ->
        val destination: Destination.EditEmployee = backStackEntry.toRoute()
        val viewModel: EmployeeViewModel = hiltViewModel()

        LaunchedEffect(Unit) {
            viewModel.uiEvent.collect { event ->
                if (event is EmployeeUiEvent.NavigateBack) {
                    navController.popBackStack()
                }
            }
        }

        EditEmployeeScreen(
            employeeId = destination.employeeId,
            viewModel = viewModel,
            onBackClick = { navController.popBackStack() }
        )
    }
}
