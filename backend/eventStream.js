/**
 * EVENT STREAM MANAGER v3.2
 * Sistema de eventos en tiempo real con Server-Sent Events (SSE)
 */

class EventStreamManager {
  constructor() {
    this.clients = new Set();
    this.eventHistory = [];
    this.maxHistorySize = 100;
  }

  /**
   * Agrega un cliente al stream
   */
  addClient(res) {
    this.clients.add(res);
    
    // Enviar historial reciente al nuevo cliente
    this.eventHistory.forEach(event => {
      this.sendEvent(res, event);
    });
    
    // Enviar evento de conexión
    this.sendEvent(res, {
      type: 'connected',
      message: 'Conectado al stream de eventos',
      timestamp: new Date().toISOString()
    });
  }

  /**
   * Elimina un cliente del stream
   */
  removeClient(res) {
    this.clients.delete(res);
  }

  /**
   * Envía un evento a todos los clientes
   */
  broadcast(event) {
    const eventWithTimestamp = {
      ...event,
      timestamp: event.timestamp || new Date().toISOString()
    };
    
    // Guardar en historial
    this.eventHistory.push(eventWithTimestamp);
    if (this.eventHistory.length > this.maxHistorySize) {
      this.eventHistory.shift();
    }
    
    // Enviar a todos los clientes
    this.clients.forEach(client => {
      this.sendEvent(client, eventWithTimestamp);
    });
  }

  /**
   * Envía un evento a un cliente específico
   */
  sendEvent(res, event) {
    try {
      res.write(`data: ${JSON.stringify(event)}\n\n`);
    } catch (error) {
      // Cliente desconectado
      this.removeClient(res);
    }
  }

  /**
   * Emite evento de notificación enviada
   */
  emitNotificationSent(data) {
    this.broadcast({
      type: 'notification_sent',
      level: 'info',
      ...data
    });
  }

  /**
   * Emite evento de notificación fallida
   */
  emitNotificationFailed(data) {
    this.broadcast({
      type: 'notification_failed',
      level: 'error',
      ...data
    });
  }

  /**
   * Emite evento de retry
   */
  emitRetryAttempt(data) {
    this.broadcast({
      type: 'retry_attempt',
      level: 'warning',
      ...data
    });
  }

  /**
   * Emite evento de regla disparada
   */
  emitRuleTriggered(data) {
    this.broadcast({
      type: 'rule_triggered',
      level: 'info',
      ...data
    });
  }

  /**
   * Emite evento de simulación
   */
  emitSimulation(data) {
    this.broadcast({
      type: 'simulation',
      level: 'info',
      ...data
    });
  }

  /**
   * Emite evento de salud del sistema
   */
  emitHealthStatus(data) {
    this.broadcast({
      type: 'health_status',
      level: data.status === 'ok' ? 'info' : 'warning',
      ...data
    });
  }

  /**
   * Emite evento de métricas actualizadas
   */
  emitMetricsUpdate(data) {
    this.broadcast({
      type: 'metrics_update',
      level: 'info',
      ...data
    });
  }

  /**
   * Obtiene estadísticas del stream
   */
  getStats() {
    return {
      connectedClients: this.clients.size,
      eventHistorySize: this.eventHistory.length,
      recentEvents: this.eventHistory.slice(-10)
    };
  }
}

// Singleton instance
const eventStreamManager = new EventStreamManager();

module.exports = eventStreamManager;
