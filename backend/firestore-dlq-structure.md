# Estructura Dead Letter Queue - Firestore v3.1

## Colección: idempotency_keys
Almacena keys de idempotencia procesadas para evitar duplicados.

```
idempotency_keys/{idempotencyKey}
{
  processedAt: timestamp,
  notificationId: string,
  title: string,
  target: string
}
```

## Colección: failed_notifications
Dead Letter Queue para notificaciones fallidas que pueden reprocesarse.

```
failed_notifications/{failureId}
{
  notificationId: string,
  idempotencyKey: string,
  title: string,
  body: string,
  target: string,
  error: string,
  errorCode: string,
  retryCount: number,
  lastRetryAt: timestamp | null,
  timestamp: timestamp
}
```

## Índices recomendados

### idempotency_keys
- `processedAt` (descendente, para limpieza periódica)

### failed_notifications
- `retryCount` (para filtrar por número de reintentos)
- `timestamp` (descendente, para reprocesamiento FIFO)
- `errorCode` (para análisis de errores por tipo)

## Estrategia de Reprocesamiento

1. **Cron job diario**: Reprocesar notificaciones con `retryCount < 3`
2. **Manual**: Endpoint `POST /reprocess-failed` con parámetro `limit`
3. **Limpieza**: Eliminar notificaciones con `retryCount >= 3` después de 30 días

## Event Versioning

Cada notificación incluye campo `version`:
- `version: 1` - Payload actual
- `version: 2` - Futuros campos adicionales

Esto permite evolución del schema sin breaking changes.
