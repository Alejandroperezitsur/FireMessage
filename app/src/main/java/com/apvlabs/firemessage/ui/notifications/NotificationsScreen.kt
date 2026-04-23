package com.apvlabs.firemessage.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apvlabs.firemessage.data.model.Notification
import org.koin.androidx.compose.koinViewModel
import com.apvlabs.firemessage.data.model.NotificationPriority
import com.apvlabs.firemessage.data.model.NotificationType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de Notificaciones profesional con Room database
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationViewModel = koinViewModel(),
    onLogout: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Mis Notificaciones",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            // Empty state mejorado
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No tienes notificaciones",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Las notificaciones aparecerán aquí",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = {
                            viewModel.markAsRead(notification.id)
                        },
                        onDelete = {
                            viewModel.deleteNotification(notification.id)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar notificación") },
            text = { Text("¿Estás seguro de eliminar esta notificación?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!notification.isRead) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Surface(
                color = getNotificationColor(notification.type).copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = getNotificationIcon(notification.type),
                    contentDescription = null,
                    tint = getNotificationColor(notification.type),
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (notification.isRead) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        maxLines = 1
                    )
                    
                    if (notification.priority == NotificationPriority.HIGH) {
                        Surface(
                            color = Color.Red.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "URGENTE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (notification.isRead) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    },
                    maxLines = 2
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormat.format(Date(notification.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (notification.isRead) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        }
                    )
                    
                    // Type badge
                    Surface(
                        color = if (notification.isRead) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        },
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = notification.type.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (notification.isRead) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Delete button
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = if (notification.isRead) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = getNotificationColor(notification.type),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
        }
    }
}

fun getNotificationIcon(type: NotificationType) = when (type) {
    NotificationType.CAFETERIA_READY -> Icons.Default.Restaurant
    NotificationType.ALERTA_IMPORTANTE -> Icons.Default.Warning
    NotificationType.EVENTO -> Icons.Default.Event
    NotificationType.MENSAJE_GENERAL -> Icons.Default.Notifications
}

fun getNotificationColor(type: NotificationType) = when (type) {
    NotificationType.CAFETERIA_READY -> Color(0xFFFF6B35)
    NotificationType.ALERTA_IMPORTANTE -> Color(0xFFDC2626)
    NotificationType.EVENTO -> Color(0xFF2563EB)
    NotificationType.MENSAJE_GENERAL -> Color(0xFF4B5563)
}
