package com.apvlabs.firemessage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.apvlabs.firemessage.ui.auth.AuthUiState
import com.apvlabs.firemessage.ui.auth.AuthViewModel
import com.apvlabs.firemessage.ui.auth.LoginScreen
import com.apvlabs.firemessage.ui.auth.RegisterScreen
import com.apvlabs.firemessage.ui.home.HomeScreen
import com.apvlabs.firemessage.ui.home.HomeViewModel
import com.apvlabs.firemessage.ui.notifications.NotificationsScreen
import com.apvlabs.firemessage.ui.notifications.NotificationViewModel
import com.apvlabs.firemessage.ui.theme.FireMessageTheme
import org.koin.androidx.compose.koinViewModel

/**
 * MainActivity - Punto de entrada de la aplicación AlertaCampus
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
    val authViewModel: AuthViewModel = koinViewModel()
    val uiState by authViewModel.uiState.collectAsState()
    var isCheckingAuth by remember { mutableStateOf(true) }

    // Verificar estado de autenticación al iniciar
    LaunchedEffect(Unit) {
        authViewModel.checkAuthStatus()
        isCheckingAuth = false
    }

    if (isCheckingAuth) {
        // Pantalla de carga simple mientras verificamos la sesión
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Determinar destino inicial basándonos en el estado actual
    val startDestination = remember {
        if (authViewModel.uiState.value is AuthUiState.Success) "home" else "login"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login Screen
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                viewModel = authViewModel
            )
        }
        
        // Register Screen
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                viewModel = authViewModel
            )
        }
        
        // Home Screen
        composable("home") {
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
            } ?: run {
                // Si por alguna razón el usuario es null, volver al login
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            }
        }
        
        // Notifications Screen
        composable("notifications") {
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
