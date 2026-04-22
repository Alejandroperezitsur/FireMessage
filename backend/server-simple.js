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

// Configuración de FCM
const FCM_API_KEY = process.env.FCM_API_KEY || ''; // Debe configurarse en .env
const FCM_URL = 'https://fcm.googleapis.com/fcm/send';

// Middleware de seguridad
app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '10kb' }));

// Rate limiting: 10 requests por minuto por IP
const limiter = rateLimit({
  windowMs: 60 * 1000, // 1 minuto
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
    .replace(/[\u0300-\u036f]/g, '') // Remover acentos
    .replace(/[^a-z0-9_]/g, '');
}

/**
 * POST /send-notification
 * Endpoint para enviar notificaciones usando FCM HTTP Legacy API
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

    if (!FCM_API_KEY) {
      return res.status(500).json({
        success: false,
        message: 'FCM_API_KEY no configurada. Por favor configúrala en el archivo .env'
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

      // Configuración del mensaje FCM
      const message = {
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
          priority: finalPriority === 'HIGH' ? 'high' : 'normal',
          notification: {
            channel_id: 'alerta_campus_channel',
            sound: type === 'ALERTA_IMPORTANTE' ? 'default' : undefined
          }
        }
      };

      let targetDescription = '';

      // Estrategia de envío por Topics o Token
      if (sendToAll) {
        message.to = '/topics/all_users';
        targetDescription = 'all_users';
      } else if (targetRole && targetCareer) {
        const normalizedCareer = normalizeCareerName(targetCareer);
        const combinedTopic = `role_${targetRole.toLowerCase()}_${normalizedCareer}`;
        message.to = `/topics/${combinedTopic}`;
        targetDescription = combinedTopic;
      } else if (targetRole) {
        const roleTopic = `role_${targetRole.toLowerCase()}`;
        message.to = `/topics/${roleTopic}`;
        targetDescription = roleTopic;
      } else if (targetCareer) {
        const careerTopic = `career_${normalizeCareerName(targetCareer)}`;
        message.to = `/topics/${careerTopic}`;
        targetDescription = careerTopic;
      } else if (token) {
        message.to = token;
        targetDescription = `token_${token.substring(0, 10)}...`;
      } else {
        return res.status(400).json({
          success: false,
          message: 'Debe proporcionar token, targetRole, targetCareer o sendToAll'
        });
      }

      // Enviar a FCM
      const startTime = Date.now();
      const response = await axios.post(FCM_URL, message, {
        headers: {
          'Authorization': `key=${FCM_API_KEY}`,
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
        messageId: response.data.messageId || 'unknown',
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
        messageId: response.data.messageId,
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
 * Endpoint para obtener historial de envíos
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
 * Endpoint para estadísticas de envíos
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
 * Endpoint de health check
 */
app.get('/health', (req, res) => {
  res.json({
    success: true,
    message: 'Servidor funcionando correctamente',
    uptime: process.uptime(),
    memory: process.memoryUsage(),
    timestamp: new Date().toISOString(),
    fcmConfigured: !!FCM_API_KEY
  });
});

/**
 * GET /analytics
 * Endpoint para analytics
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
  console.log(`🚀 Servidor AlertaCampus (HTTP Legacy API) corriendo en puerto ${PORT}`);
  console.log(`📡 Endpoint: http://localhost:${PORT}`);
  console.log(`📋 Health check: http://localhost:${PORT}/health`);
  console.log(`📊 Stats: http://localhost:${PORT}/stats`);
  console.log(`📈 Analytics: http://localhost:${PORT}/analytics`);
  console.log(`⚠️  FCM_API_KEY configurada: ${!!FCM_API_KEY}`);
  if (!FCM_API_KEY) {
    console.log(`⚠️  ADVERTENCIA: FCM_API_KEY no configurada. Configúrala en el archivo .env`);
    console.log(`📝 Ejemplo: FCM_API_KEY=your_fcm_api_key_here`);
  }
});
