package com.ts.cbdcwalletapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ts.cbdcwalletapp.data.Repository
import com.ts.cbdcwalletapp.data.TokenManager
import com.ts.cbdcwalletapp.network.RetrofitClient
import com.ts.cbdcwalletapp.ui.*
import com.ts.cbdcwalletapp.ui.theme.CBDCWalletAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CBDCWalletAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val tokenManager = TokenManager(applicationContext)
                    val apiService = RetrofitClient.getApiService(applicationContext)
                    val repository = Repository(apiService)
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModelFactory(repository, tokenManager)
                    )

                    // Check if user is already logged in
                    viewModel.checkAuth()
                    val startDestination = if (tokenManager.getToken() != null) Screen.Dashboard.route else Screen.Login.route

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable(Screen.Login.route) {
                            LoginScreen(navController, viewModel)
                        }
                        composable(Screen.Signup.route) {
                            SignupScreen(navController, viewModel)
                        }
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(navController, viewModel)
                        }
                        composable(Screen.OfflineTransfer.route) {
                            OfflineTransferScreen(navController, viewModel, tokenManager.getUser())
                        }
                    }
                }
            }
        }
    }
}