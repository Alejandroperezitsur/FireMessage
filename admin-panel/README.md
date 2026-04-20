# AlertaCampus - Panel Admin

Panel web de administración para enviar notificaciones del sistema AlertaCampus.

## Características

- 🎨 Interfaz moderna y responsive
- 📤 Envío de notificaciones con diferentes tipos
- 🎯 Segmentación por rol, carrera o token específico
- 📋 Historial de envíos en tiempo real
- ✅ Feedback visual de envíos

## Instalación y Uso

1. Asegúrate de que el backend esté corriendo:
```bash
cd backend
npm start
```

2. Abre el archivo `index.html` en tu navegador:
```bash
# O usa un servidor local
npx http-server
```

3. El panel estará disponible en `http://localhost:8080`

## Tipos de Notificación

- **🍔 Cafetería**: Para avisos de pedidos listos y promociones
- **🚨 Alerta Importante**: Para avisos urgentes y emergencias
- **📅 Evento**: Para conferencias, talleres y eventos académicos
- **📢 Mensaje General**: Para avisos generales

## Segmentación

El panel permite enviar notificaciones a:

1. **Todos los usuarios**: Envío masivo a toda la plataforma
2. **Por Rol**: Solo a alumnos o solo a profesores
3. **Por Carrera**: A estudiantes de una carrera específica
4. **Token Específico**: A un usuario específico mediante su token FCM

## Configuración

Por defecto, el panel se conecta a `http://localhost:3000`. Si tu backend está en otra URL, modifica la constante `API_URL` en el archivo `index.html`:

```javascript
const API_URL = 'http://tu-backend-url:puerto';
```

## Características de la UI

- **Diseño Material**: Interfaz limpia y moderna
- **Responsive**: Funciona en desktop y móvil
- **Feedback inmediato**: Alertas de éxito/error
- **Historial**: Registro de los últimos 100 envíos
- **Validación**: Campos requeridos y validación de inputs

## Seguridad

- No exponer credenciales en el frontend
- Usar HTTPS en producción
- Implementar autenticación en el backend para proteger el endpoint
- Validar inputs tanto en frontend como en backend
