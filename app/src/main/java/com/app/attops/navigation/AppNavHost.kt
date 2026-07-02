package com.app.attops.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.app.attops.core.navigation.Destination
import com.app.attops.features.auth.navigation.authNavGraph
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
        // Auth Feature Graph
        authNavGraph(navController)

        // Employee Feature Graph
        employeeNavGraph(navController)

        // Main Dashboard Placeholder
        composable<Destination.Dashboard> {
            // This will be replaced by the Dashboard feature graph in Phase 7
        }
    }
}
