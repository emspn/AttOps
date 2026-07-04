package com.app.attops.features.auth.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.app.attops.core.navigation.Destination
import com.app.attops.features.auth.presentation.AuthChoiceScreen
import com.app.attops.features.auth.presentation.AuthUiEvent
import com.app.attops.features.auth.presentation.AuthViewModel
import com.app.attops.features.auth.presentation.EmployeeLoginScreen
import com.app.attops.features.auth.presentation.OrgCreationScreen
import com.app.attops.features.auth.presentation.OrgCreationSuccessScreen
import com.app.attops.features.auth.presentation.SplashScreen
import kotlinx.coroutines.flow.collectLatest

fun NavGraphBuilder.authNavGraph(navController: NavHostController) {
    navigation<Destination.AuthGraph>(
        startDestination = Destination.Splash,
    ) {
        composable<Destination.Splash> {
            val authGraphEntry = remember(it) {
                navController.getBackStackEntry(Destination.AuthGraph)
            }
            val viewModel: AuthViewModel = hiltViewModel(authGraphEntry)
            
            SplashScreen(
                viewModel = viewModel,
                onNavigateToAuth = {
                    navController.navigate(Destination.AuthChoice) {
                        popUpTo(Destination.Splash) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.Splash) { inclusive = true }
                    }
                },
                onNavigateToOrgCreation = {
                    navController.navigate(Destination.OrgCreation) {
                        popUpTo(Destination.Splash) { inclusive = true }
                    }
                },
            )
        }

        composable<Destination.AuthChoice> {
            val authGraphEntry = remember(it) {
                navController.getBackStackEntry(Destination.AuthGraph)
            }
            val viewModel: AuthViewModel = hiltViewModel(authGraphEntry)
            
            AuthChoiceScreen(
                viewModel = viewModel,
                onEmployeeLoginClick = {
                    navController.navigate(Destination.EmployeeLogin)
                },
            )

            LaunchedEffect(Unit) {
                viewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is AuthUiEvent.NavigateToDashboard -> {
                            navController.navigate(Destination.Dashboard) {
                                popUpTo(Destination.AuthGraph) { inclusive = true }
                            }
                        }
                        is AuthUiEvent.NavigateToOrgCreation -> {
                            navController.navigate(Destination.OrgCreation) {
                                popUpTo(Destination.AuthChoice) { inclusive = true }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        composable<Destination.OrgCreation> {
            val authGraphEntry = remember(it) {
                navController.getBackStackEntry(Destination.AuthGraph)
            }
            val viewModel: AuthViewModel = hiltViewModel(authGraphEntry)
            
            OrgCreationScreen(
                viewModel = viewModel,
                onOrgCreated = { name, type, address ->
                    viewModel.createOrganization(name, type, address)
                },
            )

            LaunchedEffect(Unit) {
                viewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is AuthUiEvent.NavigateToOrgCreationSuccess -> {
                            navController.navigate(Destination.OrgCreationSuccess(event.orgCode)) {
                                popUpTo(Destination.OrgCreation) { inclusive = true }
                            }
                        }
                        is AuthUiEvent.NavigateToDashboard -> {
                             navController.navigate(Destination.Dashboard) {
                                popUpTo(Destination.AuthGraph) { inclusive = true }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        composable<Destination.OrgCreationSuccess> { backStackEntry ->
            val destination: Destination.OrgCreationSuccess = backStackEntry.toRoute()
            OrgCreationSuccessScreen(
                orgCode = destination.orgCode,
                onContinueToDashboard = {
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.AuthGraph) { inclusive = true }
                    }
                },
            )
        }

        composable<Destination.EmployeeLogin> {
            val authGraphEntry = remember(it) {
                navController.getBackStackEntry(Destination.AuthGraph)
            }
            val viewModel: AuthViewModel = hiltViewModel(authGraphEntry)

            EmployeeLoginScreen(
                viewModel = viewModel,
                onLoginClick = { orgCode, empId, password ->
                    viewModel.login(orgCode, empId, password)
                },
                onBackClick = {
                    navController.popBackStack()
                },
            )

            LaunchedEffect(Unit) {
                viewModel.uiEvent.collectLatest { event ->
                    if (event is AuthUiEvent.ShowError) {
                        // Snackbar handled in Screen
                    } else if (event is AuthUiEvent.NavigateToDashboard) {
                        navController.navigate(Destination.Dashboard) {
                            popUpTo(Destination.AuthGraph) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}
