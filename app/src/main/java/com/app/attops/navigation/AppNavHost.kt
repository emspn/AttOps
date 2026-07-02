package com.app.attops.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.app.attops.core.navigation.Destination
import com.app.attops.features.auth.navigation.authNavGraph

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

        // Main Dashboard Placeholder
        composable<Destination.Dashboard> {
            // This will be replaced by the Dashboard feature graph in Phase 7
        }
    }
}
