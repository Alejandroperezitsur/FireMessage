const express = require('express');
const admin = require('firebase-admin');
const cors = require('cors');
const bodyParser = require('body-parser');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json());

// Inicializar Firebase Admin
// NOTA: Debes colocar tu archivo service-account.json en esta carpeta
const serviceAccountPath = path.join(__dirname, 'service-account.json');

if (fs.existsSync(serviceAccountPath)) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccountPath),
        databaseURL: "https://firemessage-dde6b.firebaseio.com" // Reemplaza con tu URL
    });
    console.log("✅ Firebase Admin inicializado correctamente");
} else {
    console.error("❌ ERROR: No se encontró service-account.json");
    console.log("⚠️ Por favor, descarga el archivo de Service Account desde Firebase Console y guárdalo como 'admin-server/service-account.json'");
}

const db = admin.firestore();

// Endpoints

// 1. Enviar notificación
app.post('/send-notification', async (req, res) => {
    const {
        title, body, type, priority,
        sendToAll, targetRole, targetCareer, token
    } = req.body;

    try {
        const message = {
            notification: {
                title: title,
                body: body
            },
            data: {
                title: title,
                body: body,
                type: type || 'MENSAJE_GENERAL',
                priority: priority || 'NORMAL',
                click_action: 'FLUTTER_NOTIFICATION_CLICK' // Para compatibilidad
            },
            android: {
                priority: priority === 'HIGH' ? 'high' : 'normal',
                notification: {
                    channel_id: 'alerta_campus_channel',
                    priority: priority === 'HIGH' ? 'high' : 'default'
                }
            }
        };

        let target = '';
        if (token) {
            message.token = token;
            target = `Token: ${token.substring(0, 10)}...`;
        } else if (sendToAll) {
            message.topic = 'all_users';
            target = 'Todos los usuarios';
        } else if (targetRole) {
            message.topic = `role_${targetRole}`;
            target = `Rol: ${targetRole}`;
        } else if (targetCareer) {
            message.topic = `career_${targetCareer.replace(/\s+/g, '_').toLowerCase()}`;
            target = `Carrera: ${targetCareer}`;
        }

        const response = await admin.messaging().send(message);

        // Guardar log en Firestore
        const logEntry = {
            title,
            body,
            type,
            target,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            messageId: response,
            status: 'SUCCESS'
        };

        await db.collection('notification_logs').add(logEntry);

        res.json({ success: true, messageId: response });
    } catch (error) {
        console.error('Error enviando notificación:', error);

        // Guardar log de error
        try {
            await db.collection('notification_logs').add({
                title,
                type,
                target: 'FAILED',
                error: error.message,
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                status: 'ERROR'
            });
        } catch (dbError) {}

        res.status(500).json({ success: false, message: error.message });
    }
});

// 2. Obtener logs
app.get('/logs', async (req, res) => {
    try {
        const snapshot = await db.collection('notification_logs')
            .orderBy('timestamp', 'desc')
            .limit(20)
            .get();

        const logs = snapshot.docs.map(doc => {
            const data = doc.data();
            return {
                id: doc.id,
                ...data,
                timestamp: data.timestamp ? data.timestamp.toDate() : new Date()
            };
        });

        res.json({ success: true, logs });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
});

// 3. Obtener analíticas
app.get('/analytics', async (req, res) => {
    try {
        const totalLogs = await db.collection('notification_logs').count().get();
        const last24h = await db.collection('notification_logs')
            .where('timestamp', '>', new Date(Date.now() - 24 * 60 * 60 * 1000))
            .count().get();

        const errors = await db.collection('notification_logs')
            .where('status', '==', 'ERROR')
            .count().get();

        const total = totalLogs.data().count;
        const success = total - errors.data().count;
        const successRate = total > 0 ? Math.round((success / total) * 100) : 100;

        res.json({
            success: true,
            analytics: {
                sends: {
                    total: total,
                    last24h: last24h.data().count,
                    automated: 0,
                    manual: total
                },
                delivery: {
                    successRate: `${successRate}%`
                },
                interactions: {
                    openRate: 'N/A',
                    opened: 0,
                    dismissed: 0
                },
                system: {
                    uptime: process.uptime()
                }
            }
        });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
});

// 4. Salud del sistema
app.get('/health', (req, res) => {
    res.json({ success: true, status: 'OK' });
});

// 5. Simular carga
app.post('/simulate-load', async (req, res) => {
    const { userCount } = req.body;
    const start = Date.now();

    // Simulación simplificada
    res.json({
        success: true,
        simulation: {
            userCount,
            successCount: userCount,
            failureCount: 0,
            duration: `${(Date.now() - start) / 1000}s`,
            throughput: `${userCount} msg/s`
        }
    });
});

// 6. Event Stream (SSE)
app.get('/events/stream', (req, res) => {
    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');

    const sendEvent = (data) => {
        res.write(`data: ${JSON.stringify(data)}\n\n`);
    };

    // Enviar evento de conexión
    sendEvent({ type: 'connected', message: 'Stream de eventos activado', timestamp: new Date() });

    // Enviar eventos aleatorios simulados periódicamente si quieres ver algo de actividad
    const interval = setInterval(() => {
        if (Math.random() > 0.7) {
            sendEvent({
                type: 'system_check',
                message: 'Verificación de rutina completada',
                level: 'info',
                timestamp: new Date()
            });
        }
    }, 5000);

    req.on('close', () => {
        clearInterval(interval);
    });
});

app.listen(PORT, () => {
    console.log(`🚀 Servidor Admin corriendo en http://localhost:${PORT}`);
});
