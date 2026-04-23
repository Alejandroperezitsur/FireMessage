const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const { body, validationResult } = require('express-validator');
const sanitizeHtml = require('sanitize-html');
const { v4: uuidv4 } = require('uuid');
const axios = require('axios');

require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// Configuración de FCM HTTP v1 API
const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || ''; // Ejemplo: alertacampus-12345
const CLIENT_EMAIL = process.env.FIREBASE_CLIENT_EMAIL || ''; // Ejemplo: firebase-adminsdk-xxxxx@alertacampus-12345.iam.gserviceaccount.com
const PRIVATE_KEY = process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n') || ''; // Clave privada del service account

// Si no tiene service account, puede usar OAuth 2.0 con client credentials
const OAUTH_CLIENT_ID = process.env.OAUTH_CLIENT_ID || '';
const OAUTH_CLIENT_SECRET = process.env.OAUTH_CLIENT_SECRET || '';
const OAUTH_REFRESH_TOKEN = process.env.OAUTH_REFRESH_TOKEN || '';

let accessToken = null;
let tokenExpiry = null;

/**
 * Obtiene OAuth 2.0 access token usando refresh token
 */
async function getAccessToken() {
  // Si el token aún es válido, retornarlo
  if (accessToken && tokenExpiry && Date.now() < tokenExpiry) {
    return accessToken;
  }

  try {
    // Método 1: Usar service account credentials (si están disponibles)
    if (CLIENT_EMAIL && PRIVATE_KEY) {
      const jwt = require('jsonwebtoken');
      
      const now = Math.floor(Date.now() / 1000);
      const payload = {
        iss: CLIENT_EMAIL,
        scope: 'https://www.googleapis.com/auth/firebase.messaging',
        aud: 'https://oauth2.googleapis.com/token',
        iat: now,
        exp: now + 3600
      };

      const token = jwt.sign(payload, PRIVATE_KEY, { algorithm: 'RS256' });

      const response = await axios.post('https://oauth2.googleapis.com/token', {
        grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        assertion: token
      });

      accessToken = response.data.access_token;
      tokenExpiry = Date.now() + (response.data.expires_in * 1000) - 60000; // 1 minuto antes de expirar
      return accessToken;
    }

    // Método 2: Usar OAuth 2.0 refresh token
    if (OAUTH_REFRESH_TOKEN && OAUTH_CLIENT_ID && OAUTH_CLIENT_SECRET) {
      const response = await axios.post('https://oauth2.googleapis.com/token', {
        client_id: OAUTH_CLIENT_ID,
        client_secret: OAUTH_CLIENT_SECRET,
        refresh_token: OAUTH_REFRESH_TOKEN,
        grant_type: 'refresh_token'
      });

      accessToken = response.data.access_token;
      tokenExpiry = Date.now() + (response.data.expires_in * 1000) - 60000;
      return accessToken;
    }

    throw new Error('No hay credenciales OAuth 2.0 configuradas');
  } catch (error) {
    console.error('Error al obtener access token:', error.message);
    throw error;
  }
}

// Middleware de seguridad
app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '10kb' }));

// Rate limiting
const limiter = rateLimit({
  windowMs: 60 * 1000,
  max: 10,
  message: {
    success: false,
    message: 'Demasiadas solicitudes. Por favor espera un momento.'
  },
  standardHeaders: true,
  legacyHeaders: false
});

app.use('/send-notification', limiter);

// Almacenamiento en memoria para logs
const sendLogs = [];

/**
 * Normaliza nombre de carrera para tópico FCM
 */
function normalizeCareerName(career) {
  return career
    .toLowerCase()
    .replace(/\s+/g, '_')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9_]/g, '');
}

/**
 * POST /send-notification
 * Endpoint para enviar notificaciones usando FCM HTTP v1 API
 */
app.post('/send-notification',
  [
    body('title').trim().isLength({ min: 1, max: 100 }).withMessage('Título requerido (1-100 caracteres)'),
    body('body').trim().isLength({ min: 1, max: 500 }).withMessage('Cuerpo requerido (1-500 caracteres)'),
    body('type').isIn(['CAFETERIA_READY', 'ALERTA_IMPORTANTE', 'EVENTO', 'MENSAJE_GENERAL'])
      .withMessage('Tipo de notificación inválido'),
    body('priority').optional().isIn(['HIGH', 'NORMAL', 'LOW']).withMessage('Prioridad inválida')
  ],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({
        success: false,
        message: 'Validación fallida',
        errors: errors.array()
      });
    }

    if (!PROJECT_ID) {
      return res.status(500).json({
        success: false,
        message: 'FIREBASE_PROJECT_ID no configurada. Configúrala en el archivo .env'
      });
    }

    try {
      const {
        title,
        body,
        type,
        priority,
        targetRole,
        targetCareer,
        sendToAll,
        token
      } = req.body;

      // Sanitización de inputs
      const sanitizedTitle = sanitizeHtml(title, { allowedTags: [], allowedAttributes: {} });
      const sanitizedBody = sanitizeHtml(body, { allowedTags: [], allowedAttributes: {} });

      // Prioridad por defecto según tipo
      const finalPriority = priority || (type === 'ALERTA_IMPORTANTE' ? 'HIGH' : 'NORMAL');

      // Obtener access token
      const accessToken = await getAccessToken();

      // Configuración del mensaje FCM HTTP v1
      const message = {
        message: {
          notification: {
            title: sanitizedTitle,
            body: sanitizedBody
          },
          data: {
            type: type,
            priority: finalPriority,
            timestamp: Date.now().toString(),
            version: '1'
          },
          android: {
            priority: finalPriority === 'HIGH' ? 'HIGH' : 'NORMAL',
            notification: {
              channelId: 'alerta_campus_channel',
              sound: type === 'ALERTA_IMPORTANTE' ? 'default' : undefined
            }
          }
        }
      };

      let targetDescription = '';

      // Estrategia de envío por Topics o Token
      if (sendToAll) {
        message.message.topic = 'all_users';
        targetDescription = 'all_users';
      } else if (targetRole && targetCareer) {
        const normalizedCareer = normalizeCareerName(targetCareer);
        const combinedTopic = `role_${targetRole.toLowerCase()}_${normalizedCareer}`;
        message.message.topic = combinedTopic;
        targetDescription = combinedTopic;
      } else if (targetRole) {
        const roleTopic = `role_${targetRole.toLowerCase()}`;
        message.message.topic = roleTopic;
        targetDescription = roleTopic;
      } else if (targetCareer) {
        const careerTopic = `career_${normalizeCareerName(targetCareer)}`;
        message.message.topic = careerTopic;
        targetDescription = careerTopic;
      } else if (token) {
        message.message.token = token;
        targetDescription = `token_${token.substring(0, 10)}...`;
      } else {
        return res.status(400).json({
          success: false,
          message: 'Debe proporcionar token, targetRole, targetCareer o sendToAll'
        });
      }

      // Enviar a FCM HTTP v1 API
      const startTime = Date.now();
      const fcmUrl = `https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send`;
      
      const response = await axios.post(fcmUrl, message, {
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json'
        }
      });
      
      const latency = Date.now() - startTime;

      // Log
      const log = {
        id: Date.now(),
        title: sanitizedTitle,
        body: sanitizedBody,
        type: type,
        priority: finalPriority,
        target: targetDescription,
        targetRole: targetRole || null,
        targetCareer: targetCareer || null,
        sendToAll: !!sendToAll,
        senderIp: req.ip,
        timestamp: new Date().toISOString(),
        messageId: response.data.name || 'unknown',
        latency: latency,
        success: true
      };

      sendLogs.unshift(log);
      if (sendLogs.length > 500) {
        sendLogs.pop();
      }

      res.json({
        success: true,
        message: 'Notificación enviada exitosamente',
        messageId: response.data.name,
        target: targetDescription,
        latency: latency,
        log
      });

    } catch (error) {
      console.error('Error al enviar notificación:', error.message);

      res.status(500).json({
        success: false,
        message: 'Error al enviar notificación',
        error: error.message,
        details: error.response?.data || null
      });
    }
  }
);

/**
 * GET /logs
 */
app.get('/logs', (req, res) => {
  const limit = parseInt(req.query.limit) || 50;
  const logs = sendLogs.slice(0, limit);
  
  res.json({
    success: true,
    logs: logs,
    total: sendLogs.length,
    returned: logs.length,
    timestamp: new Date().toISOString()
  });
});

/**
 * GET /stats
 */
app.get('/stats', (req, res) => {
  const stats = {
    total: sendLogs.length,
    byType: {},
    byTarget: {},
    last24h: sendLogs.filter(log => {
      const logTime = new Date(log.timestamp);
      const dayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
      return logTime > dayAgo;
    }).length
  };

  sendLogs.forEach(log => {
    stats.byType[log.type] = (stats.byType[log.type] || 0) + 1;
    stats.byTarget[log.target] = (stats.byTarget[log.target] || 0) + 1;
  });

  res.json({
    success: true,
    stats,
    timestamp: new Date().toISOString()
  });
});

/**
 * GET /health
 */
app.get('/health', (req, res) => {
  res.json({
    success: true,
    message: 'Servidor funcionando correctamente',
    uptime: process.uptime(),
    memory: process.memoryUsage(),
    timestamp: new Date().toISOString(),
    fcmConfigured: !!PROJECT_ID,
    authMethod: CLIENT_EMAIL && PRIVATE_KEY ? 'Service Account JWT' : 
                OAUTH_REFRESH_TOKEN ? 'OAuth 2.0 Refresh Token' : 'None'
  });
});

/**
 * GET /analytics
 */
app.get('/analytics', (req, res) => {
  const now = new Date();
  const dayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
  const weekAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);

  const last24hLogs = sendLogs.filter(log => new Date(log.timestamp) > dayAgo);
  const lastWeekLogs = sendLogs.filter(log => new Date(log.timestamp) > weekAgo);

  const analytics = {
    sends: {
      total: sendLogs.length,
      last24h: last24hLogs.length,
      last7days: lastWeekLogs.length,
      byType: lastWeekLogs.reduce((acc, log) => {
        acc[log.type] = (acc[log.type] || 0) + 1;
        return acc;
      }, {}),
      byTarget: lastWeekLogs.reduce((acc, log) => {
        acc[log.target] = (acc[log.target] || 0) + 1;
        return acc;
      }, {})
    },
    performance: {
      avgLatency: lastWeekLogs.length > 0 
        ? Math.round(lastWeekLogs.reduce((sum, log) => sum + (log.latency || 0), 0) / lastWeekLogs.length) 
        : 0,
      maxLatency: lastWeekLogs.length > 0 
        ? Math.max(...lastWeekLogs.map(log => log.latency || 0)) 
        : 0
    },
    system: {
      uptime: process.uptime(),
      memory: process.memoryUsage(),
      timestamp: now.toISOString()
    }
  };

  res.json({
    success: true,
    analytics,
    timestamp: now.toISOString()
  });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error('Error no manejado:', err);
  res.status(500).json({
    success: false,
    message: 'Error interno del servidor',
    error: process.env.NODE_ENV === 'production' ? undefined : err.message
  });
});

// Iniciar servidor
app.listen(PORT, () => {
  console.log(`🚀 Servidor AlertaCampus (FCM HTTP v1 API) corriendo en puerto ${PORT}`);
  console.log(`📡 Endpoint: http://localhost:${PORT}`);
  console.log(`📋 Health check: http://localhost:${PORT}/health`);
  console.log(`📊 Stats: http://localhost:${PORT}/stats`);
  console.log(`📈 Analytics: http://localhost:${PORT}/analytics`);
  console.log(`⚠️  FIREBASE_PROJECT_ID configurada: ${!!PROJECT_ID}`);
  console.log(`🔐 Método de autenticación: ${CLIENT_EMAIL && PRIVATE_KEY ? 'Service Account JWT' : OAUTH_REFRESH_TOKEN ? 'OAuth 2.0' : 'None'}`);
  
  if (!PROJECT_ID) {
    console.log(`⚠️  ADVERTENCIA: FIREBASE_PROJECT_ID no configurada`);
  }
  if (!CLIENT_EMAIL || !PRIVATE_KEY) {
    console.log(`⚠️  ADVERTENCIA: Credenciales de service account no configuradas`);
  }
  if (!OAUTH_REFRESH_TOKEN) {
    console.log(`⚠️  ADVERTENCIA: OAuth refresh token no configurado`);
  }
  console.log(`\n📝 Configuración requerida en .env:`);
  console.log(`   FIREBASE_PROJECT_ID=tu_project_id`);
  console.log(`   # Opción 1: Service Account JWT`);
  console.log(`   FIREBASE_CLIENT_EMAIL=tu_service_account_email`);
  console.log(`   FIREBASE_PRIVATE_KEY=tu_private_key`);
  console.log(`   # Opción 2: OAuth 2.0`);
  console.log(`   OAUTH_CLIENT_ID=tu_oauth_client_id`);
  console.log(`   OAUTH_CLIENT_SECRET=tu_oauth_client_secret`);
  console.log(`   OAUTH_REFRESH_TOKEN=tu_oauth_refresh_token`);
});
