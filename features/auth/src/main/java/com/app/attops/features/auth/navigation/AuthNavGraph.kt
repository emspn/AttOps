package com.app.attops.features.auth.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.app.attops.core.navigation.Destination
import com.app.attops.features.auth.presentation.AuthChoiceScreen
import com.app.attops.features.auth.presentation.AuthUiEvent
import com.app.attops.features.auth.presentation.AuthViewModel
import com.app.attops.features.auth.presentation.EmployeeLoginScreen
import com.app.attops.features.auth.presentation.OrgCreationScreen
import com.app.attops.features.auth.presentation.SplashScreen

fun NavGraphBuilder.authNavGraph(navController: NavHostController) {
    navigation<Destination.AuthGraph>(
        startDestination = Destination.Splash
    ) {
        composable<Destination.Splash> {
            val viewModel: AuthViewModel = hiltViewModel()
            SplashScreen(
                viewModel = viewModel,
                onNavigateToAuth = {
                    navController.navigate(Destination.AuthChoice) {
                        popUpTo(Destination.Splash) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.Splash) { inclusive = true }
                    }
                }
            )
        }

        composable<Destination.AuthChoice> {
            val viewModel: AuthViewModel = hiltViewModel()
            AuthChoiceScreen(
                onGoogleSignInClick = {
                    viewModel.signInWithGoogle()
                },
                onEmployeeLoginClick = {
                    navController.navigate(Destination.EmployeeLogin)
                }
            )

            LaunchedEffect(Unit) {
                viewModel.uiEvent.collect { event ->
                    when (event) {
                        is AuthUiEvent.NavigateToDashboard -> {
                            navController.navigate(Destination.Dashboard) {
                                popUpTo(Destination.AuthGraph) { inclusive = true }
                            }
                        }
                        is AuthUiEvent.NavigateToOrgCreation -> {
                            navController.navigate(Destination.OrgCreation)
                        }
                        else -> {}
                    }
                }
            }
        }

        composable<Destination.OrgCreation> {
            val viewModel: AuthViewModel = hiltViewModel()
            OrgCreationScreen(
                onOrgCreated = { name, type, phone, address ->
                    viewModel.createOrganization(name, type, phone, address)
                }
            )

            LaunchedEffect(Unit) {
                viewModel.uiEvent.collect { event ->
                    if (event is AuthUiEvent.NavigateToDashboard) {
                        navController.navigate(Destination.Dashboard) {
                            popUpTo(Destination.AuthGraph) { inclusive = true }
                        }
                    }
                }
            }
        }

        composable<Destination.EmployeeLogin> {
            val viewModel: AuthViewModel = hiltViewModel()
            EmployeeLoginScreen(
                onLoginClick = { orgCode, empId, password ->
                    viewModel.login(orgCode, empId, password)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )

            LaunchedEffect(Unit) {
                viewModel.uiEvent.collect { event ->
                    if (event is AuthUiEvent.NavigateToDashboard) {
                        navController.navigate(Destination.Dashboard) {
                            popUpTo(Destination.AuthGraph) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}
