package com.apvlabs.firemessage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.apvlabs.firemessage.data.model.User
import com.apvlabs.firemessage.di.appModule
import com.apvlabs.firemessage.ui.auth.AuthViewModel
import com.apvlabs.firemessage.ui.auth.LoginScreen
import com.apvlabs.firemessage.ui.auth.RegisterScreen
import com.apvlabs.firemessage.ui.home.HomeScreen
import com.apvlabs.firemessage.ui.home.HomeViewModel
import com.apvlabs.firemessage.ui.notifications.NotificationsScreen
import com.apvlabs.firemessage.ui.notifications.NotificationViewModel
import com.apvlabs.firemessage.ui.theme.FireMessageTheme
import org.koin.android.ext.android.startKoin
import org.koin.androidx.compose.koinViewModel

/**
 * MainActivity - Punto de entrada de la aplicación AlertaCampus
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar Koin
        startKoin {
            modules(appModule)
        }
        
        enableEdgeToEdge()
        setContent {
            FireMessageTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlertaCampusApp()
                }
            }
        }
    }
}

@Composable
fun AlertaCampusApp(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        // Login Screen
        composable("login") {
            val viewModel: AuthViewModel = koinViewModel()
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                viewModel = viewModel
            )
        }
        
        // Register Screen
        composable("register") {
            val viewModel: AuthViewModel = koinViewModel()
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                viewModel = viewModel
            )
        }
        
        // Home Screen
        composable("home") {
            val authViewModel: AuthViewModel = koinViewModel()
            val homeViewModel: HomeViewModel = koinViewModel()
            val user by authViewModel.currentUser.collectAsState()
            
            user?.let { currentUser ->
                HomeScreen(
                    user = currentUser,
                    onNavigateToNotifications = {
                        navController.navigate("notifications")
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    viewModel = homeViewModel
                )
            }
        }
        
        // Notifications Screen
        composable("notifications") {
            val authViewModel: AuthViewModel = koinViewModel()
            val notificationViewModel: NotificationViewModel = koinViewModel()
            
            NotificationsScreen(
                viewModel = notificationViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("notifications") { inclusive = true }
                    }
                }
            )
        }
    }
}