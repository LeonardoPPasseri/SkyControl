package com.skycontrol.dronebackend.service;

import com.skycontrol.dronebackend.database.JsonDatabaseService;
import com.skycontrol.dronebackend.events.InternalEventBus;
import com.skycontrol.dronebackend.events.InternalEventPublisher;
import com.skycontrol.dronebackend.model.Alert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;


@Service
public class AlertService {

    @Autowired
    private JsonDatabaseService dbService;
    @Autowired
    private InternalEventBus eventBus; 

    // Executado AUTOMATICAMENTE quando o Spring inicia este serviço. Limpa os alertas antigos do JSON.
    @PostConstruct
    public void init() {
        dbService.clearAlerts();
    }

 
    // Alertas EXTERNOS (do RabbitMQ via AlertListener)
    // APENAS salva e notifica o frontend.
    public void processExternalAlert(Alert alert) {
        
        dbService.saveAlert(alert); 

        var event = new InternalEventPublisher.Event("ALERT_CREATED", alert); 
        eventBus.publish(event); 
    }


     // Alertas INTERNOS (Bateria Baixa via TelemetryListener)
     // Salva e notifica o frontend
    public void createInternalAlert(Long droneId, String type, String message) {
        long timestamp = System.currentTimeMillis();
        Alert alert = new Alert(droneId, type, message, timestamp); 
        
        dbService.saveAlert(alert); 

        var event = new InternalEventPublisher.Event("ALERT_CREATED", alert); 
        eventBus.publish(event); 
    }
}