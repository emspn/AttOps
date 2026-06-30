package com.app.attops.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.app.attops.core.navigation.Destination

@Composable
fun AttOpsNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Destination = Destination.Login
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Destination.Login> {
            // Placeholder for Login Screen
        }
        
        composable<Destination.Dashboard> {
            // Placeholder for Dashboard Screen
        }
        
        composable<Destination.Attendance> {
            // Placeholder for Attendance Screen
        }
        
        composable<Destination.Tasks> {
            // Placeholder for Tasks Screen
        }
        
        composable<Destination.Employees> {
            // Placeholder for Employees Screen
        }
        
        composable<Destination.Reports> {
            // Placeholder for Reports Screen
        }
        
        composable<Destination.Profile> {
            // Placeholder for Profile Screen
        }
    }
}
