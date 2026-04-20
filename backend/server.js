const express = require('express');
const cors = require('cors');
const admin = require('firebase-admin');
const dotenv = require('dotenv');

// Cargar variables de entorno
dotenv.config();

// Inicializar Firebase Admin
const serviceAccount = require('./firebase-service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Almacenamiento en memoria para logs de envíos (en producción usar base de datos)
const sendLogs = [];

/**
 * POST /send-notification
 * Endpoint para enviar notificaciones push
 */
app.post('/send-notification', async (req, res) => {
  try {
    const { token, title, body, type, targetRole, targetCareer, sendToAll } = req.body;

    // Validar campos requeridos
    if (!title || !body || !type) {
      return res.status(400).json({
        success: false,
        message: 'Faltan campos requeridos: title, body, type'
      });
    }

    let message = {
      notification: {
        title: title,
        body: body
      },
      data: {
        type: type,
        timestamp: Date.now().toString()
      },
      android: {
        priority: 'high',
        notification: {
          channel_id: 'alerta_campus_channel',
          sound: 'default'
        }
      }
    };

    let response;

    if (sendToAll) {
      // Enviar a todos los usuarios (usando tópico)
      message.topic = 'all_users';
      response = await admin.messaging().send(message);
    } else if (targetRole && targetCareer) {
      // Enviar por rol y carrera
      const topic = `role_${targetRole}_${targetCareer.toLowerCase().replace(/\s+/g, '_')}`;
      message.topic = topic;
      response = await admin.messaging().send(message);
    } else if (targetRole) {
      // Enviar por rol
      const topic = `role_${targetRole}`;
      message.topic = topic;
      response = await admin.messaging().send(message);
    } else if (targetCareer) {
      // Enviar por carrera
      const topic = `career_${targetCareer.toLowerCase().replace(/\s+/g, '_')}`;
      message.topic = topic;
      response = await admin.messaging().send(message);
    } else if (token) {
      // Enviar a un token específico
      message.token = token;
      response = await admin.messaging().send(message);
    } else {
      return res.status(400).json({
        success: false,
        message: 'Debe proporcionar token, targetRole, targetCareer o sendToAll'
      });
    }

    // Guardar log del envío
    const log = {
      id: Date.now(),
      title,
      body,
      type,
      target: sendToAll ? 'all' : (token ? 'single' : `${targetRole || ''}/${targetCareer || ''}`),
      timestamp: new Date().toISOString(),
      messageId: response
    };
    sendLogs.unshift(log);

    // Mantener solo los últimos 100 logs
    if (sendLogs.length > 100) {
      sendLogs.pop();
    }

    res.json({
      success: true,
      message: 'Notificación enviada exitosamente',
      messageId: response,
      log
    });

  } catch (error) {
    console.error('Error al enviar notificación:', error);
    res.status(500).json({
      success: false,
      message: 'Error al enviar notificación',
      error: error.message
    });
  }
});

/**
 * GET /logs
 * Endpoint para obtener historial de envíos
 */
app.get('/logs', (req, res) => {
  res.json({
    success: true,
    logs: sendLogs,
    total: sendLogs.length
  });
});

/**
 * GET /health
 * Endpoint de health check
 */
app.get('/health', (req, res) => {
  res.json({
    success: true,
    message: 'Servidor funcionando correctamente',
    timestamp: new Date().toISOString()
  });
});

// Iniciar servidor
app.listen(PORT, () => {
  console.log(`🚀 Servidor AlertaCampus corriendo en puerto ${PORT}`);
  console.log(`📡 Endpoint: http://localhost:${PORT}`);
  console.log(`📋 Health check: http://localhost:${PORT}/health`);
});
