# 🔔 AlertaCampus - Sistema Inteligente de Notificaciones Universitarias

Sistema completo de notificaciones push para universidades con segmentación inteligente por rol y carrera.

## 📋 Descripción del Proyecto

AlertaCampus es un sistema de notificaciones universitarias que permite enviar avisos personalizados a estudiantes y profesores sobre:
- 📢 Avisos importantes (clases canceladas, cambios de salón)
- 🍔 Cafetería (pedido listo, promociones)
- 🚨 Emergencias (simulacros, alertas reales)
- 📅 Eventos (conferencias, talleres)

## 🏗️ Arquitectura

### Android (Cliente)
- **Kotlin** con Jetpack Compose
- **MVVM limpio** con Repository pattern
- **StateFlow / LiveData** para gestión de estado
- **Manejo de errores robusto**
- **Offline-first** con caché local (DataStore)
- **Firebase** (Auth, FCM, Firestore)

### Backend (Node.js)
- **Express** para API REST
- **Firebase Admin SDK** para envío de notificaciones
- **Segmentación inteligente** por rol, carrera o token específico
- **Logs de envío** para auditoría

### Panel Admin (Web)
- **HTML + JavaScript** (sin frameworks)
- **Interfaz moderna y responsive**
- **Envío de notificaciones** con diferentes tipos
- **Historial de envíos** en tiempo real

## 📁 Estructura del Proyecto

```
FireMessage/
├── app/                          # Aplicación Android
│   ├── src/main/
│   │   ├── java/com/apvlabs/firemessage/
│   │   │   ├── data/            # Capa de datos
│   │   │   │   ├── model/       # Modelos de datos
│   │   │   │   ├── local/       # Caché local
│   │   │   │   ├── remote/      # Servicios Firebase/API
│   │   │   │   └── repository/  # Repositorios
│   │   │   ├── ui/              # Capa de UI
│   │   │   │   ├── auth/        # Pantallas de auth
│   │   │   │   ├── home/        # Pantalla principal
│   │   │   │   ├── notifications/# Pantalla de notificaciones
│   │   │   │   └── theme/       # Tema Material Design 3
│   │   │   ├── service/         # Firebase Messaging Service
│   │   │   ├── di/              # Inyección de dependencias (Koin)
│   │   │   └── MainActivity.kt
│   │   └── res/                 # Recursos Android
│   └── build.gradle.kts         # Dependencias Android
├── backend/                      # Backend Node.js
│   ├── server.js                # Servidor Express
│   ├── package.json             # Dependencias Node
│   ├── firebase-service-account.json # Credenciales Firebase (NO commitear)
│   └── README.md                # Documentación backend
├── admin-panel/                 # Panel web de administración
│   ├── index.html               # Interfaz admin
│   └── README.md                # Documentación panel
└── README.md                    # Este archivo
```

## 🚀 Guía de Instalación y Configuración

### 1. Configurar Firebase

1. Crea un proyecto en [Firebase Console](https://console.firebase.google.com/)
2. Habilita los siguientes servicios:
   - **Authentication**: Email/Password
   - **Cloud Firestore**: Base de datos
   - **Cloud Messaging**: Notificaciones push
3. Agrega una app Android:
   - Descarga `google-services.json` y colócalo en `app/`
4. Genera clave de servicio para el backend:
   - Project Settings > Service Accounts
   - Genera nueva clave privada
   - Renómbrala a `firebase-service-account.json` y colócala en `backend/`

### 2. Configurar Android

1. Abre el proyecto en Android Studio
2. Sync Gradle (los lint errors se resolverán después del sync)
3. Configura el `google-services.json` en `app/`
4. Build y run la app

### 3. Configurar Backend

```bash
cd backend
npm install
npm start
```

El backend estará disponible en `http://localhost:3000`

### 4. Configurar Panel Admin

1. Abre `admin-panel/index.html` en tu navegador
2. O usa un servidor local:
```bash
cd admin-panel
npx http-server
```

## 📱 Funcionalidades del Cliente Android

### Autenticación
- ✅ Registro con email, contraseña, nombre, rol y carrera
- ✅ Login con email y contraseña
- ✅ Logout seguro

### Gestión de Tokens FCM
- ✅ Obtención automática de token
- ✅ Envío al backend
- ✅ Actualización si el token cambia
- ✅ Suscripción a tópicos por rol y carrera

### Recepción de Notificaciones
- ✅ Foreground: banner in-app
- ✅ Background: notificación push del sistema
- ✅ Notificación clickeable: navega a pantalla específica
- ✅ Iconos y colores distintivos por tipo

### UI de Notificaciones
- ✅ Lista de notificaciones recibidas
- ✅ Persistencia local (offline-first)
- ✅ Estados: leída/no leída
- ✅ UX limpia con Material Design 3
- ✅ Dark mode support
- ✅ Animaciones suaves

### Tipos de Notificación
- 🍔 **CAFETERIA_READY**: Icono naranja, avisos de cafetería
- 🚨 **ALERTA_IMPORTANTE**: Icono rojo, avisos urgentes
- 📅 **EVENTO**: Icono azul, eventos académicos
- 📢 **MENSAJE_GENERAL**: Icono gris, avisos generales

### Segmentación Inteligente
- ✅ Selección de carrera (8 carreras predefinidas)
- ✅ Selección de rol (Alumno/Profesor)
- ✅ Suscripción automática a tópicos
- ✅ Recepción de notificaciones segmentadas

## 🔌 API del Backend

### POST /send-notification
Envía una notificación push.

**Body:**
```json
{
  "token": "opcional",
  "title": "Título",
  "body": "Mensaje",
  "type": "CAFETERIA_READY",
  "targetRole": "STUDENT",
  "targetCareer": "Ingeniería de Sistemas",
  "sendToAll": false
}
```

### GET /logs
Obtiene historial de envíos.

### GET /health
Health check del servidor.

## 🧪 Testing y Edge Cases

El sistema implementa manejo de:
- ✅ Sin internet (caché local)
- ✅ Token inválido (validación en backend)
- ✅ Notificación duplicada (ID único)
- ✅ App cerrada (notificación del sistema)
- ✅ App en foreground/background (manejo diferenciado)
- ✅ Usuario no autenticado (redirección a login)

## 🔐 Seguridad

- ✅ Validación de inputs en backend
- ✅ No exposición de server key en cliente
- ✅ Uso correcto de Firebase Admin
- ✅ Credenciales en `.gitignore`
- ✅ HTTPS recomendado en producción

## 📦 Tecnologías Utilizadas

### Android
- Kotlin
- Jetpack Compose
- Material Design 3
- Firebase (Auth, FCM, Firestore)
- Koin (DI)
- DataStore (Caché local)
- Navigation Compose
- Coroutines & Flow

### Backend
- Node.js
- Express
- Firebase Admin SDK
- CORS
- dotenv

### Admin Panel
- HTML5
- CSS3
- JavaScript (Vanilla)

## 🎨 Características UX/UI

- Material Design 3
- Dark mode
- Animaciones suaves
- Estados vacíos bien diseñados
- Feedback visual (loading, success, error)
- Responsive design

## 📝 Notas Importantes

### Para que el sistema funcione correctamente:

1. **Firebase Configuration**:
   - Configura correctamente `google-services.json` en Android
   - Configura `firebase-service-account.json` en backend
   - Habilita Email/Password en Firebase Auth
   - Habilita Firestore y FCM en Firebase Console

2. **Android Manifest**:
   - Permisos de INTERNET y POST_NOTIFICATIONS ya configurados
   - FirebaseMessagingService ya registrado

3. **Backend**:
   - Instala dependencias con `npm install`
   - Asegúrate de que el puerto 3000 esté disponible

4. **Sync Gradle**:
   - Los lint errors se resolverán después del sync
   - Las dependencias se descargarán automáticamente

## 🚀 Próximas Mejoras (Bonus)

- [ ] Autenticación en panel admin
- [ ] Base de datos persistente para logs
- [ ] Dashboard con estadísticas
- [ ] Programación de notificaciones
- [ ] Plantillas de notificaciones
- [ ] Multi-idioma
- [ ] Testing unitario y E2E

## 📄 Licencia

Este proyecto es para fines educativos.

## 👨‍💻 Autor

Desarrollado como proyecto de sistema de notificaciones universitarias.

---

**¡El sistema está listo para usar! Sigue los pasos de configuración y comienza a enviar notificaciones.** 🎉
