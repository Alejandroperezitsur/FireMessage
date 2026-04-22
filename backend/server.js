const express = require('express');
const cors = require('cors');
const admin = require('firebase-admin');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const { body, validationResult } = require('express-validator');
const sanitizeHtml = require('sanitize-html');
const { v4: uuidv4 } = require('uuid');
const { sendWithTransaction, validateNotificationPayload } = require('./transactionManager');
const eventStreamManager = require('./eventStream');

require('dotenv').config();

// Inicializar Firebase Admin
const serviceAccount = require('./firebase-service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const app = express();
const PORT = process.env.PORT || 3000;

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

// Almacenamiento en memoria para logs avanzados (en producción usar base de datos)
const sendLogs = [];
const invalidTokens = new Set(); // Cache de tokens inválidos

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
 * Determina prioridad de Android basada en tipo de notificación
 */
function getAndroidPriority(type) {
  const highPriority = ['ALERTA_IMPORTANTE'];
  const lowPriority = ['MENSAJE_GENERAL'];
  
  if (highPriority.includes(type)) return 'high';
  if (lowPriority.includes(type)) return 'normal';
  return 'normal';
}

/**
 * Determina si debe vibrar según prioridad
 */
function shouldVibrate(type) {
  return type === 'ALERTA_IMPORTANTE';
}

/**
 * Limpia tokens inválidos automáticamente
 */
async function cleanupInvalidToken(token) {
  try {
    const db = admin.firestore();
    const usersSnapshot = await db.collection('users')
      .where('fcmToken', '==', token)
      .get();
    
    if (!usersSnapshot.empty) {
      const batch = db.batch();
      usersSnapshot.docs.forEach(doc => {
        batch.update(doc.ref, { fcmToken: null });
      });
      await batch.commit();
      console.log(`🧹 Token inválido limpiado: ${token.substring(0, 20)}...`);
    }
  } catch (error) {
    console.error('Error al limpiar token:', error);
  }
}

/**
 * Sistema de reintentos con backoff exponencial
 */
async function sendWithRetry(message, maxRetries = 3) {
  let attempt = 0;
  let lastError;
  
  while (attempt < maxRetries) {
    try {
      const response = await admin.messaging().send(message);
      return { success: true, response, attempts: attempt + 1 };
    } catch (error) {
      lastError = error;
      attempt++;
      
      // Backoff exponencial: 1s, 2s, 4s
      if (attempt < maxRetries) {
        const delay = Math.pow(2, attempt - 1) * 1000;
        console.log(`⏳ [RETRY] Intento ${attempt}/${maxRetries} en ${delay}ms...`);
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }
  }
  
  return { success: false, error: lastError, attempts: maxRetries };
}

/**
 * POST /send-notification
 * Endpoint profesional v3.1 con idempotencia, transacciones y validación fuerte
 */
app.post('/send-notification',
  [
    body('title').trim().isLength({ min: 1, max: 100 }).withMessage('Título requerido (1-100 caracteres)'),
    body('body').trim().isLength({ min: 1, max: 500 }).withMessage('Cuerpo requerido (1-500 caracteres)'),
    body('type').isIn(['CAFETERIA_READY', 'ALERTA_IMPORTANTE', 'EVENTO', 'MENSAJE_GENERAL'])
      .withMessage('Tipo de notificación inválido'),
    body('priority').optional().isIn(['HIGH', 'NORMAL', 'LOW']).withMessage('Prioridad inválida'),
    body('idempotencyKey').optional().isUUID().withMessage('Idempotency key debe ser UUID válido')
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

    try {
      const payload = req.body;
      
      // Validación fuerte con Joi
      const validation = validateNotificationPayload(payload);
      if (!validation.valid) {
        return res.status(400).json({
          success: false,
          message: 'Validación de payload fallida',
          errors: validation.errors
        });
      }

      const {
        title,
        body,
        type,
        priority,
        targetRole,
        targetCareer,
        sendToAll,
        token,
        idempotencyKey
      } = validation.data;

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
          version: '1',
          idempotencyKey: idempotencyKey || uuidv4()
        },
        android: {
          priority: finalPriority === 'HIGH' ? 'high' : 'normal',
          notification: {
            channel_id: 'alerta_campus_channel',
            sound: shouldVibrate(type) ? 'default' : undefined,
            badge: type === 'ALERTA_IMPORTANTE' ? 1 : undefined
          }
        }
      };

      let targetDescription = '';

      // Estrategia de envío por Topics
      if (sendToAll) {
        message.topic = 'all_users';
        targetDescription = 'all_users';
      } else if (targetRole && targetCareer) {
        const normalizedCareer = normalizeCareerName(targetCareer);
        const combinedTopic = `role_${targetRole.toLowerCase()}_${normalizedCareer}`;
        message.topic = combinedTopic;
        targetDescription = combinedTopic;
      } else if (targetRole) {
        const roleTopic = `role_${targetRole.toLowerCase()}`;
        message.topic = roleTopic;
        targetDescription = roleTopic;
      } else if (targetCareer) {
        const careerTopic = `career_${normalizeCareerName(targetCareer)}`;
        message.topic = careerTopic;
        targetDescription = careerTopic;
      } else if (token) {
        message.token = token;
        targetDescription = `token_${token.substring(0, 10)}...`;
      } else {
        return res.status(400).json({
          success: false,
          message: 'Debe proporcionar token, targetRole, targetCareer o sendToAll'
        });
      }

      // Enviar con transacción atómica (TODO o NADA)
      const result = await sendWithTransaction(message, {
        notificationId: uuidv4(),
        title: sanitizedTitle,
        body: sanitizedBody,
        type: type,
        priority: finalPriority,
        target: targetDescription,
        targetRole: targetRole || null,
        targetCareer: targetCareer || null,
        sendToAll: !!sendToAll,
        idempotencyKey: idempotencyKey || uuidv4(),
        version: 1,
        automated: false
      }, {
        senderIp: req.ip,
        userAgent: req.get('user-agent'),
        retryCount: 0
      });

      // Emitir evento al stream
      eventStreamManager.emitNotificationSent({
        notificationId: result.notificationId,
        title: sanitizedTitle,
        target: targetDescription,
        latency: result.latency,
        idempotent: result.idempotent,
        messageId: result.messageId
      });

      // Log avanzado
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
        userAgent: req.get('user-agent'),
        timestamp: new Date().toISOString(),
        messageId: result.messageId,
        idempotencyKey: result.idempotencyKey,
        latency: result.latency,
        version: 1,
        idempotent: result.idempotent,
        success: true
      };

      sendLogs.unshift(log);

      if (sendLogs.length > 500) {
        sendLogs.pop();
      }

      res.json({
        success: true,
        message: result.idempotent ? 'Notificación ya procesada (idempotente)' : 'Notificación enviada exitosamente',
        messageId: result.messageId,
        notificationId: result.notificationId,
        idempotencyKey: result.idempotencyKey,
        target: targetDescription,
        latency: result.latency,
        idempotent: result.idempotent,
        log
      });

    } catch (error) {
      console.error('Error al enviar notificación:', error);

      // Emitir evento de fallo al stream
      eventStreamManager.emitNotificationFailed({
        error: error.message,
        errorCode: error.code,
        target: targetDescription
      });

      // Structured log de error
      console.log(JSON.stringify({
        event: 'notification_send_error',
        status: 'error',
        error: error.message,
        errorCode: error.code,
        timestamp: new Date().toISOString()
      }));

      // Si el error es por token inválido, limpiarlo
      if (error.code === 'messaging/registration-token-not-registered' ||
          error.code === 'messaging/invalid-argument') {
        const token = req.body.token;
        if (token && !invalidTokens.has(token)) {
          invalidTokens.add(token);
          cleanupInvalidToken(token);
        }
      }

      res.status(500).json({
        success: false,
        message: 'Error al enviar notificación',
        error: error.message,
        code: error.code || 'UNKNOWN_ERROR'
      });
    }
  }
);

/**
 * GET /logs
 * Endpoint para obtener historial de envíos avanzado
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
    timestamp: new Date().toISOString()
  });
});

/**
 * GET /analytics
 * Endpoint para analytics reales (métricas avanzadas)
 */
app.get('/analytics', async (req, res) => {
  try {
    const now = new Date();
    const dayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
    const weekAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);

    const last24hLogs = sendLogs.filter(log => new Date(log.timestamp) > dayAgo);
    const lastWeekLogs = sendLogs.filter(log => new Date(log.timestamp) > weekAgo);

    const analyticsSnapshot = await admin.firestore()
      .collection('analytics')
      .where('timestamp', '>', admin.firestore.Timestamp.fromDate(weekAgo))
      .get();

    let automatedSends = 0;
    let manualSends = 0;
    let deliverySuccess = 0;
    let deliveryFailures = 0;

    analyticsSnapshot.docs.forEach(doc => {
      const data = doc.data();
      if (data.type === 'automated_send') {
        automatedSends++;
      } else if (data.type === 'manual_send') {
        manualSends++;
      }
      if (data.success === true) {
        deliverySuccess++;
      } else if (data.success === false) {
        deliveryFailures++;
      }
    });

    const interactionsSnapshot = await admin.firestore()
      .collection('notification_events')
      .where('timestamp', '>', admin.firestore.Timestamp.fromDate(weekAgo))
      .get();

    let openedCount = 0;
    let dismissedCount = 0;
    const interactionsByType = {};

    interactionsSnapshot.docs.forEach(doc => {
      const data = doc.data();
      if (data.eventType === 'opened') {
        openedCount++;
      } else if (data.eventType === 'dismissed') {
        dismissedCount++;
      }
      
      const type = data.notificationType || 'unknown';
      interactionsByType[type] = interactionsByType[type] || { opened: 0, dismissed: 0 };
      if (data.eventType === 'opened') {
        interactionsByType[type].opened++;
      } else if (data.eventType === 'dismissed') {
        interactionsByType[type].dismissed++;
      }
    });

    const analytics = {
      sends: {
        total: sendLogs.length,
        last24h: last24hLogs.length,
        last7days: lastWeekLogs.length,
        automated: automatedSends,
        manual: manualSends,
        byType: lastWeekLogs.reduce((acc, log) => {
          acc[log.type] = (acc[log.type] || 0) + 1;
          return acc;
        }, {}),
        byTarget: lastWeekLogs.reduce((acc, log) => {
          acc[log.target] = (acc[log.target] || 0) + 1;
          return acc;
        }, {})
      },
      delivery: {
        success: deliverySuccess,
        failures: deliveryFailures,
        successRate: deliverySuccess + deliveryFailures > 0 
          ? ((deliverySuccess / (deliverySuccess + deliveryFailures)) * 100).toFixed(2) + '%' 
          : 'N/A'
      },
      interactions: {
        total: openedCount + dismissedCount,
        opened: openedCount,
        dismissed: dismissedCount,
        openRate: openedCount + dismissedCount > 0 
          ? ((openedCount / (openedCount + dismissedCount)) * 100).toFixed(2) + '%' 
          : 'N/A',
        byType: interactionsByType
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
  } catch (error) {
    console.error('Error al obtener analytics:', error);
    res.status(500).json({
      success: false,
      message: 'Error al obtener analytics',
      error: error.message
    });
  }
});

/**
 * GET /events/stream
 * Server-Sent Events endpoint para stream en tiempo real
 */
app.get('/events/stream', (req, res) => {
  // Headers para SSE
  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');
  res.setHeader('Access-Control-Allow-Origin', '*');

  // Agregar cliente al stream
  eventStreamManager.addClient(res);

  // Manejar desconexión del cliente
  req.on('close', () => {
    eventStreamManager.removeClient(res);
  });

  // Keep-alive cada 30 segundos
  const keepAlive = setInterval(() => {
    try {
      res.write(': keep-alive\n\n');
    } catch (error) {
      clearInterval(keepAlive);
      eventStreamManager.removeClient(res);
    }
  }, 30000);
});

/**
 * GET /stream-stats
 * Estadísticas del stream
 */
app.get('/stream-stats', (req, res) => {
  res.json({
    success: true,
    stats: eventStreamManager.getStats(),
    timestamp: new Date().toISOString()
  });
});

/**
 * POST /reprocess-failed
 * Endpoint para reprocesar notificaciones fallidas (Dead Letter Queue)
 */
app.post('/reprocess-failed', async (req, res) => {
  const { limit = 10 } = req.body;
  const { reprocessFailedNotifications } = require('./transactionManager');
  
  try {
    const reprocessed = await reprocessFailedNotifications(limit);
    
    res.json({
      success: true,
      message: `Reprocesadas ${reprocessed} notificaciones fallidas`,
      reprocessed,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    console.error('Error al reprocesar notificaciones:', error);
    res.status(500).json({
      success: false,
      message: 'Error al reprocesar notificaciones fallidas',
      error: error.message
    });
  }
});

/**
 * POST /simulate-load
 * Simulación de carga real (1000 usuarios)
 * Feature WOW para demostración
 */
app.post('/simulate-load', async (req, res) => {
  const { userCount = 1000 } = req.body;
  
  console.log(`🚀 [SIMULATION] Iniciando simulación de ${userCount} usuarios`);
  const startTime = Date.now();
  
  try {
    const promises = [];
    
    for (let i = 0; i < userCount; i++) {
      const message = {
        notification: {
          title: `Simulación #${i + 1}`,
          body: 'Mensaje de prueba de carga'
        },
        data: {
          type: 'MENSAJE_GENERAL',
          priority: 'NORMAL',
          timestamp: Date.now().toString(),
          simulated: 'true'
        },
        topic: 'all_users'
      };
      
      promises.push(
        admin.messaging().send(message)
          .then(response => ({ success: true, response }))
          .catch(error => ({ success: false, error: error.message }))
      );
    }
    
    const results = await Promise.all(promises);
    
    const successCount = results.filter(r => r.success).length;
    const failureCount = results.filter(r => !r.success).length;
    const duration = Date.now() - startTime;
    
    await admin.firestore().collection('analytics').add({
      type: 'load_simulation',
      userCount: userCount,
      successCount: successCount,
      failureCount: failureCount,
      duration: duration,
      timestamp: admin.firestore.FieldValue.serverTimestamp()
    });
    
    console.log(`✅ [SIMULATION] Completada: ${successCount}/${userCount} exitos en ${duration}ms`);
    
    res.json({
      success: true,
      simulation: {
        userCount,
        successCount,
        failureCount,
        duration: `${duration}ms`,
        throughput: `${(userCount / (duration / 1000)).toFixed(2)} req/s`
      },
      timestamp: new Date().toISOString()
    });
    
  } catch (error) {
    console.error('Error en simulación:', error);
    res.status(500).json({
      success: false,
      message: 'Error en simulación de carga',
      error: error.message
    });
  }
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
  console.log(`🚀 Servidor AlertaCampus v3.1 (SISTEMA DISTRIBUIDO RESILIENTE) corriendo en puerto ${PORT}`);
  console.log(`📡 Endpoint: http://localhost:${PORT}`);
  console.log(`📋 Health check: http://localhost:${PORT}/health`);
  console.log(`📊 Stats: http://localhost:${PORT}/stats`);
  console.log(`📈 Analytics: http://localhost:${PORT}/analytics`);
  console.log(`🔄 Reprocess Failed: http://localhost:${PORT}/reprocess-failed`);
  console.log(`🔒 Security: Helmet + Rate Limiting habilitados`);
  console.log(`🎯 FCM Topics: role_*, career_*, combined topics habilitados`);
  console.log(`🤖 Motor de eventos automático: ACTIVO`);
  console.log(`🔐 Idempotencia: ACTIVO`);
  console.log(`📦 Transacciones: ACTIVO`);
  console.log(`💀 Dead Letter Queue: ACTIVO`);
});

// Iniciar motor de eventos programados
require('./eventScheduler');
