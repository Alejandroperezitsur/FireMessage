# AlertaCampus Backend

Backend para el sistema de notificaciones universitarias AlertaCampus.

## Tecnologías

- Node.js
- Express
- Firebase Admin SDK
- CORS

## Instalación

1. Instalar dependencias:
```bash
npm install
```

2. Configurar Firebase:
   - Ve a la consola de Firebase (https://console.firebase.google.com/)
   - Ve a Project Settings > Service Accounts
   - Genera una nueva clave privada
   - Descarga el archivo JSON
   - Renómbralo a `firebase-service-account.json` y colócalo en la carpeta `backend`

3. Configurar variables de entorno (opcional):
```bash
cp .env.example .env
# Edita .env si necesitas cambiar el puerto
```

## Ejecutar

### Modo desarrollo:
```bash
npm run dev
```

### Modo producción:
```bash
npm start
```

El servidor se iniciará en `http://localhost:3000`

## API Endpoints

### POST /send-notification
Envía una notificación push.

**Body:**
```json
{
  "token": "opcional_token_especifico",
  "title": "Título de la notificación",
  "body": "Cuerpo de la notificación",
  "type": "CAFETERIA_READY",
  "targetRole": "STUDENT",
  "targetCareer": "Ingeniería de Sistemas",
  "sendToAll": false
}
```

**Campos:**
- `token` (opcional): Token FCM específico para enviar a un solo usuario
- `title` (requerido): Título de la notificación
- `body` (requerido): Cuerpo de la notificación
- `type` (requerido): Tipo de notificación (CAFETERIA_READY, ALERTA_IMPORTANTE, EVENTO, MENSAJE_GENERAL)
- `targetRole` (opcional): Rol objetivo (STUDENT, PROFESSOR)
- `targetCareer` (opcional): Carrera objetivo
- `sendToAll` (opcional): Enviar a todos los usuarios (true/false)

**Respuesta exitosa:**
```json
{
  "success": true,
  "message": "Notificación enviada exitosamente",
  "messageId": "message_id_here",
  "log": { ... }
}
```

### GET /logs
Obtiene el historial de envíos de notificaciones.

**Respuesta:**
```json
{
  "success": true,
  "logs": [
    {
      "id": 1234567890,
      "title": "Título",
      "body": "Cuerpo",
      "type": "CAFETERIA_READY",
      "target": "STUDENT",
      "timestamp": "2024-01-01T00:00:00.000Z",
      "messageId": "message_id_here"
    }
  ],
  "total": 1
}
```

### GET /health
Health check del servidor.

**Respuesta:**
```json
{
  "success": true,
  "message": "Servidor funcionando correctamente",
  "timestamp": "2024-01-01T00:00:00.000Z"
}
```

## Segmentación de Notificaciones

El backend soporta múltiples métodos de segmentación:

1. **A un usuario específico**: Proporcionar `token`
2. **Por rol**: Proporcionar `targetRole` (STUDENT o PROFESSOR)
3. **Por carrera**: Proporcionar `targetCareer`
4. **Por rol y carrera**: Proporcionar ambos `targetRole` y `targetCareer`
5. **A todos**: Establecer `sendToAll` a `true`

## Seguridad

- Nunca commits el archivo `firebase-service-account.json` al control de versiones
- Usa variables de entorno para configuración sensible
- Valida todos los inputs antes de procesarlos
- El archivo `.gitignore` ya incluye los archivos sensibles
