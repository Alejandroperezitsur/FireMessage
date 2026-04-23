# Instrucciones de Configuración para FCM HTTP v1 API

## Problema
La Cloud Messaging API (Legacy) está deshabilitada y no puede activarse debido a políticas de organización.

## Solución
Usar FCM HTTP v1 API con OAuth 2.0, que es la API actual y no requiere la API Legacy.

## Opción 1: Service Account Personalizada (Recomendada)

Esta opción requiere crear una cuenta de servicio personalizada en Google Cloud Console (no Firebase Console).

### Pasos:

1. **Ir a Google Cloud Console**
   - Ve a https://console.cloud.google.com/
   - Selecciona tu proyecto de Firebase

2. **Crear una cuenta de servicio personalizada**
   - En el menú, ve a "IAM y administración" > "Cuentas de servicio"
   - Haz clic en "CREAR CUENTA DE SERVICIO"
   - Nombre: `fcm-sender` (o el que prefieras)
   - Descripción: "Cuenta de servicio para enviar mensajes FCM"
   - Haz clic en "CREAR Y CONTINUAR"

3. **Asignar permisos**
   - En "Conceder a esta cuenta de servicio acceso al proyecto", selecciona:
     - Rol: "Editor de Firebase Cloud Messaging API" o "Editor" (si no encuentras el específico)
   - Haz clic en "CONTINUAR"
   - Haz clic en "CREAR"

4. **Crear clave para la cuenta de servicio**
   - En la lista de cuentas de servicio, haz clic en la cuenta que acabas de crear
   - Ve a la pestaña "Claves"
   - Haz clic en "AÑADIR CLAVE" > "Crear clave nueva"
   - Selecciona "JSON"
   - Haz clic en "CREAR"
   - El archivo JSON se descargará automáticamente

5. **Configurar el archivo .env**
   - Abre el archivo JSON descargado
   - Copia los siguientes valores al archivo `.env` en la carpeta `backend`:

   ```env
   FIREBASE_PROJECT_ID=tu_project_id
   FIREBASE_CLIENT_EMAIL=tu_service_account_email
   FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\nTu clave privada aquí\n-----END PRIVATE KEY-----\n"
   PORT=3000
   ```

   **IMPORTANTE:** La `PRIVATE_KEY` debe incluir las comillas y los `\n` para los saltos de línea.

6. **Iniciar el servidor**
   ```bash
   npm run start-v1
   ```

## Opción 2: OAuth 2.0 con Refresh Token (Alternativa)

Si no puedes crear cuentas de servicio, puedes usar OAuth 2.0.

### Pasos:

1. **Crear credenciales OAuth 2.0**
   - Ve a https://console.cloud.google.com/
   - Selecciona tu proyecto
   - Ve a "APIs y servicios" > "Credenciales"
   - Haz clic en "CREAR CREDENCIALES" > "ID de cliente de OAuth"
   - Tipo de aplicación: "Aplicación web"
   - Agrega URI de redirección autorizada: `http://localhost:3000`
   - Haz clic en "CREAR"

2. **Obtener Refresh Token**
   - Usa el OAuth 2.0 Playground: https://developers.google.com/oauthplayground/
   - Selecciona los scopes: `https://www.googleapis.com/auth/firebase.messaging`
   - Autoriza con tu cuenta de Google
   - Intercambia el código de autorización por tokens
   - Copia el `refresh_token`

3. **Configurar el archivo .env**
   ```env
   FIREBASE_PROJECT_ID=tu_project_id
   OAUTH_CLIENT_ID=tu_oauth_client_id
   OAUTH_CLIENT_SECRET=tu_oauth_client_secret
   OAUTH_REFRESH_TOKEN=tu_refresh_token
   PORT=3000
   ```

4. **Iniciar el servidor**
   ```bash
   npm run start-v1
   ```

## Verificar configuración

Una vez configurado, inicia el servidor y verifica:

```bash
npm run start-v1
```

Luego visita: http://localhost:3000/health

Debería mostrar:
- `fcmConfigured: true`
- `authMethod: Service Account JWT` o `OAuth 2.0 Refresh Token`

## Problemas Comunes

### Error: "No hay credenciales OAuth 2.0 configuradas"
- Verifica que el archivo `.env` exista y tenga las variables correctas
- Asegúrate de reiniciar el servidor después de modificar el `.env`

### Error: "Invalid JWT"
- Verifica que la `PRIVATE_KEY` esté correctamente formateada con `\n` para los saltos de línea
- Asegúrate de incluir las comillas alrededor de la clave en el archivo `.env`

### Error: "Permission denied"
- Verifica que la cuenta de servicio tenga los permisos correctos en IAM
- Asegúrate de usar el `PROJECT_ID` correcto
