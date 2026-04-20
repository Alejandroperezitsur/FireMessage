# Estructura de Firestore para AlertaCampus

## Colección: users
Almacena información de usuarios y sus tokens FCM.

```
users/{userId}
{
  id: string,
  email: string,
  name: string,
  role: string (STUDENT | PROFESSOR),
  career: string,
  fcmToken: string | null,
  createdAt: timestamp,
  updatedAt: timestamp
}
```

## Colección: notifications
Almacena todas las notificaciones enviadas para persistencia global y sincronización entre dispositivos.

```
notifications/{notificationId}
{
  id: string,
  title: string,
  body: string,
  type: string (CAFETERIA_READY | ALERTA_IMPORTANTE | EVENTO | MENSAJE_GENERAL),
  priority: string (HIGH | NORMAL | LOW),
  target: string (all_users | role_* | career_* | role_*_career_*),
  targetRole: string | null,
  targetCareer: string | null,
  sendToAll: boolean,
  senderIp: string,
  timestamp: timestamp,
  messageId: string
}
```

## Colección: notification_logs
Almacena logs de envío para auditoría avanzada.

```
notification_logs/{logId}
{
  id: string,
  notificationId: string,
  title: string,
  type: string,
  target: string,
  success: boolean,
  errorCode: string | null,
  errorMessage: string | null,
  timestamp: timestamp,
  ipAddress: string,
  userAgent: string
}
```

## Índices recomendados

### users
- `fcmToken` (para búsqueda rápida de usuarios por token)
- `role` (para segmentación por rol)
- `career` (para segmentación por carrera)
- `role` + `career` (combinado)

### notifications
- `timestamp` (descendente, para timeline)
- `type` (para filtrado por tipo)
- `target` (para auditoría por segmento)

### notification_logs
- `timestamp` (descendente)
- `success` (para análisis de errores)
