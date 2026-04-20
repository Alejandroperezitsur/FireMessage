const cron = require('node-cron');
const admin = require('firebase-admin');

/**
 * MOTOR DE EVENTOS AUTOMÁTICO v3.0
 * Sistema inteligente que envía notificaciones sin intervención humana
 */

/**
 * Envía notificación automática
 */
async function sendAutoNotification(config) {
  try {
    const message = {
      notification: {
        title: config.title,
        body: config.body
      },
      data: {
        type: config.type || 'MENSAJE_GENERAL',
        priority: config.priority || 'NORMAL',
        timestamp: Date.now().toString(),
        automated: 'true'
      },
      topic: config.topic || 'all_users',
      android: {
        priority: config.priority === 'HIGH' ? 'high' : 'normal',
        notification: {
          channel_id: 'alerta_campus_channel',
          sound: config.priority === 'HIGH' ? 'default' : undefined
        }
      }
    };

    const response = await admin.messaging().send(message);
    
    // Guardar en analytics
    await admin.firestore().collection('analytics').add({
      type: 'automated_send',
      title: config.title,
      target: config.topic || 'all_users',
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      messageId: response,
      success: true
    });

    console.log(`✅ [AUTO] Notificación enviada: ${config.title}`);
    return response;
  } catch (error) {
    console.error(`❌ [AUTO] Error al enviar: ${error.message}`);
    
    // Guardar error en analytics
    await admin.firestore().collection('analytics').add({
      type: 'automated_send',
      title: config.title,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      success: false,
      error: error.message
    });
    
    throw error;
  }
}

/**
 * Evalúa y ejecuta reglas dinámicas desde Firestore
 */
async function evaluateAndExecuteRules() {
  try {
    const rulesSnapshot = await admin.firestore()
      .collection('rules')
      .where('enabled', '==', true)
      .get();

    const now = new Date();
    const dayOfWeek = now.getDay(); // 0 = Domingo, 6 = Sábado
    const hour = now.getHours();
    const minute = now.getMinutes();

    for (const doc of rulesSnapshot.docs) {
      const rule = doc.data();
      
      let shouldExecute = false;

      // Evaluar condiciones
      if (rule.condition.type === 'time') {
        const ruleHour = rule.condition.hour || 0;
        const ruleMinute = rule.condition.minute || 0;
        
        if (hour === ruleHour && minute === ruleMinute) {
          shouldExecute = true;
        }
      } else if (rule.condition.type === 'day') {
        if (dayOfWeek === rule.condition.day) {
          shouldExecute = true;
        }
      } else if (rule.condition.type === 'daily') {
        shouldExecute = true;
      }

      if (shouldExecute) {
        console.log(`🔥 [RULE] Ejecutando regla: ${rule.name}`);
        
        await sendAutoNotification({
          title: rule.action.title,
          body: rule.action.body,
          type: rule.action.type,
          priority: rule.action.priority,
          topic: rule.action.topic
        });
      }
    }
  } catch (error) {
    console.error('Error al evaluar reglas:', error);
  }
}

/**
 * Tareas programadas (CRON)
 */

// Recordatorio diario a las 7:00 AM
cron.schedule('0 7 * * *', async () => {
  console.log('🌅 [SCHEDULER] Enviando recordatorio matutino');
  
  await sendAutoNotification({
    title: '¡Buenos días! 🌅',
    body: 'Revisa tu horario de clases para hoy. ¡No te pierdas nada!',
    type: 'MENSAJE_GENERAL',
    priority: 'NORMAL',
    topic: 'all_users'
  });
});

// Recordatorio de fin de semana los viernes a las 3:00 PM
cron.schedule('0 15 * * 5', async () => {
  console.log('🎉 [SCHEDULER] Recordatorio fin de semana');
  
  await sendAutoNotification({
    title: '¡Viernes! 🎉',
    body: 'Prepárate para el fin de semana. Revisa eventos y actividades.',
    type: 'EVENTO',
    priority: 'NORMAL',
    topic: 'all_users'
  });
});

// Alerta de cafetería a las 12:00 PM
cron.schedule('0 12 * * *', async () => {
  console.log('🍔 [SCHEDULER] Alerta de cafetería');
  
  await sendAutoNotification({
    title: '¡Hora de comer! 🍔',
    body: 'La cafetería está lista. ¡Ven por tu comida!',
    type: 'CAFETERIA_READY',
    priority: 'NORMAL',
    topic: 'role_alumno'
  });
});

// Evaluación de reglas dinámicas cada minuto
cron.schedule('* * * * *', async () => {
  await evaluateAndExecuteRules();
});

// Mensaje de inactividad cada 3 días (ejemplo simple)
cron.schedule('0 9 */3 * *', async () => {
  console.log('📢 [SCHEDULER] Mensaje de reactivación');
  
  await sendAutoNotification({
    title: '¿Cómo estás? 👋',
    body: 'Hace tiempo que no te vemos. ¡Revisa las novedades del campus!',
    type: 'MENSAJE_GENERAL',
    priority: 'LOW',
    topic: 'all_users'
  });
});

console.log('🚀 [SCHEDULER] Motor de eventos automático iniciado');
console.log('📅 Tareas programadas activas:');
console.log('  - 07:00 AM: Recordatorio matutino');
console.log('  - 12:00 PM: Alerta de cafetería');
console.log('  - 15:00 PM (Viernes): Recordatorio fin de semana');
console.log('  - Cada minuto: Evaluación de reglas dinámicas');
console.log('  - Cada 3 días: Mensaje de reactivación');

module.exports = { sendAutoNotification, evaluateAndExecuteRules };
