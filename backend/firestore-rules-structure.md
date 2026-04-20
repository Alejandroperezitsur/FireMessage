# Estructura de Reglas Dinámicas - Firestore

## Colección: rules
Almacena reglas dinámicas para automatización inteligente.

```
rules/{ruleId}
{
  id: string,
  name: string,
  description: string,
  enabled: boolean,
  condition: {
    type: string (time | day | daily),
    hour: number (0-23),
    minute: number (0-59),
    day: number (0-6, 0=Domingo)
  },
  action: {
    title: string,
    body: string,
    type: string,
    priority: string,
    topic: string
  },
  createdAt: timestamp,
  updatedAt: timestamp
}
```

## Ejemplos de Reglas

### 1. Recordatorio diario a las 7:00 AM
```json
{
  "name": "Recordatorio matutino",
  "description": "Envía recordatorio cada mañana a las 7:00",
  "enabled": true,
  "condition": {
    "type": "time",
    "hour": 7,
    "minute": 0
  },
  "action": {
    "title": "¡Buenos días! 🌅",
    "body": "Revisa tu horario de clases para hoy.",
    "type": "MENSAJE_GENERAL",
    "priority": "NORMAL",
    "topic": "all_users"
  }
}
```

### 2. Alerta de viernes por la tarde
```json
{
  "name": "Viernes festivo",
  "description": "Recordatorio de fin de semana los viernes",
  "enabled": true,
  "condition": {
    "type": "day",
    "day": 5
  },
  "action": {
    "title": "¡Es viernes! 🎉",
    "body": "Prepárate para el fin de semana.",
    "type": "EVENTO",
    "priority": "NORMAL",
    "topic": "all_users"
  }
}
```

### 3. Recordatorio de cafetería al mediodía
```json
{
  "name": "Almuerzo",
  "description": "Alerta de cafetería a las 12:00",
  "enabled": true,
  "condition": {
    "type": "time",
    "hour": 12,
    "minute": 0
  },
  "action": {
    "title": "¡Hora de comer! 🍔",
    "body": "La cafetería está lista.",
    "type": "CAFETERIA_READY",
    "priority": "NORMAL",
    "topic": "role_alumno"
  }
}
```

## Colección: analytics
Métricas y eventos del sistema.

```
analytics/{eventId}
{
  type: string (manual_send | automated_send | opened | dismissed),
  title: string,
  target: string,
  timestamp: timestamp,
  messageId: string,
  success: boolean,
  error: string | null,
  userId: string | null
}
```

## Colección: notification_events
Tracking de interacción de usuarios.

```
notification_events/{eventId}
{
  notificationId: string,
  notificationType: string,
  eventType: string (opened | dismissed),
  userId: string,
  timestamp: timestamp
}
```

## Índices recomendados

### rules
- `enabled` (para filtrar reglas activas)

### analytics
- `timestamp` (descendente, para timeline)
- `type` (para filtrar por tipo de evento)

### notification_events
- `timestamp` (descendente)
- `eventType` (para análisis de interacción)
- `userId` (para análisis por usuario)
