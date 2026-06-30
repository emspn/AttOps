package com.app.attops.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations for AttOps.
 */
sealed interface Destination {
    @Serializable
    data object AuthGraph : Destination
    
    @Serializable
    data object Login : Destination

    @Serializable
    data object MainGraph : Destination
    
    @Serializable
    data object Dashboard : Destination

    @Serializable
    data object Attendance : Destination
    
    @Serializable
    data object Tasks : Destination
    
    @Serializable
    data object Employees : Destination
    
    @Serializable
    data object Reports : Destination
    
    @Serializable
    data object Profile : Destination
}
