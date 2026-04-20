package com.apvlabs.firemessage.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apvlabs.firemessage.data.model.User

/**
 * Pantalla Principal (Home)
 */
@Composable
fun HomeScreen(
    user: User,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val fcmToken by viewModel.fcmToken.collectAsState()
    
    LaunchedEffect(user.id) {
        viewModel.initializeFcmToken(user.id)
        
        // Suscripción profesional a topics
        viewModel.subscribeToRoleTopic(user.role.name)
        viewModel.subscribeToAllUsers()
        
        if (user.career.isNotBlank()) {
            viewModel.subscribeToCareerTopic(user.career)
            viewModel.subscribeToCombinedTopic(user.role.name, user.career)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AlertaCampus") },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = {
                                // You can add notification count badge here
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Welcome Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "¡Bienvenido!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${user.role.name} - ${user.career}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Status Cards
            Text(
                text = "Estado del Sistema",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusItem(
                        icon = Icons.Default.CheckCircle,
                        label = "Autenticación",
                        status = "Activa",
                        isGood = true
                    )
                    Divider()
                    StatusItem(
                        icon = Icons.Default.Notifications,
                        label = "Notificaciones",
                        status = if (fcmToken != null) "Activas" else "Configurando...",
                        isGood = fcmToken != null
                    )
                    Divider()
                    StatusItem(
                        icon = Icons.Default.School,
                        label = "Suscripción",
                        status = user.role.name,
                        isGood = true
                    )
                    Divider()
                    StatusItem(
                        icon = Icons.Default.Work,
                        label = "Carrera",
                        status = user.career.ifEmpty { "No especificada" },
                        isGood = user.career.isNotBlank()
                    )
                }
            }
            
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Información",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = "Recibirás notificaciones personalizadas según tu rol y carrera.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            if (uiState is HomeUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = (uiState as HomeUiState.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    status: String,
    isGood: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium
        )
    }
}
