package com.app.attops

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
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

    @Inject
    lateinit var connectionVerifier: ConnectionVerifier

    @Inject
    lateinit var mapShareBus: MapShareBus

    @Inject
    lateinit var navigationBus: NavigationBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            AttOpsTheme {
                LaunchedEffect(Unit) {
                    connectionVerifier.verify()
                }
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
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        AttOpsNavHost(
            navController = navController,
            navigationBus = navigationBus,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
