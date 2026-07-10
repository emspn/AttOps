package com.app.attops

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.attops.core.common.util.MapShareBus
import com.app.attops.core.navigation.Destination
import com.app.attops.core.navigation.NavigationBus
import com.app.attops.core.designsystem.theme.AttOpsTheme
import com.app.attops.core.network.verification.ConnectionVerifier
import com.app.attops.navigation.AttOpsNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var connectionVerifier: ConnectionVerifier
    @Inject lateinit var mapShareBus: MapShareBus
    @Inject lateinit var navigationBus: NavigationBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            AttOpsTheme {
                LaunchedEffect(Unit) { connectionVerifier.verify() }
                MainScreen(navigationBus)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if ((intent?.action == Intent.ACTION_SEND) && (intent.type == "text/plain")) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                lifecycleScope.launch {
                    mapShareBus.postLocation(sharedText)
                    navigationBus.navigateTo(Destination.CreateTask)
                }
            }
        }
    }
}

@Composable
fun MainScreen(navigationBus: NavigationBus) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem("Dashboard", Destination.Dashboard, Icons.Default.Dashboard),
        BottomNavItem("Tasks", Destination.Tasks, Icons.AutoMirrored.Filled.Assignment),
        BottomNavItem("Staff", Destination.Employees, Icons.Default.Group),
        BottomNavItem("Reports", Destination.Reports, Icons.Default.Poll),
        BottomNavItem("Profile", Destination.Profile, Icons.Default.Person)
    )

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                AttOpsBottomBar(navController, bottomNavItems)
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        AttOpsNavHost(
            navController = navController,
            navigationBus = navigationBus,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AttOpsBottomBar(
    navController: NavHostController,
    items: List<BottomNavItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        modifier = Modifier
            .navigationBarsPadding() // FIX: Add padding for system buttons
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.height(64.dp),
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            items.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
                NavigationBarItem(
                    selected = selected,
                    alwaysShowLabel = false,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        val iconScale by animateFloatAsState(if (selected) 1.2f else 1.0f, label = "iconScale")
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.name,
                            modifier = Modifier.size(22.dp).graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                        )
                    },
                    label = {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = (-0.2).sp),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }
}

data class BottomNavItem(val name: String, val route: Destination, val icon: ImageVector)
