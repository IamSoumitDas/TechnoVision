package com.soumit.technovision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.soumit.technovision.navigation.AppNavHost
import com.soumit.technovision.navigation.Screen
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            TechnoVisionTheme(isOnline = uiState.isOnline) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    if (uiState.isLoadingRole || uiState.isLoadingData || uiState.startDestination == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.layout.Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                            ) {
                                LoadingIndicator()
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                                androidx.compose.material3.Text(
                                    text = uiState.loadingMessage,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        AppNavHost(
                            navController = navController,
                            startDestination = uiState.startDestination ?: Screen.Auth
                        )
                    }

                    if (uiState.showUpdateDialog && uiState.updateInfo != null) {
                        com.soumit.technovision.ui.common.UpdatePromptDialog(
                            versionName = uiState.updateInfo!!.versionName,
                            onViewUpdate = {
                                viewModel.dismissUpdateDialog()
                                navController.navigate(Screen.AppUpdate)
                            },
                            onDismiss = viewModel::dismissUpdateDialog
                        )
                    }
                }
            }
        }
    }
}
