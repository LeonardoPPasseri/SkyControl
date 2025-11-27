package com.skycontrol.dronebackend.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skycontrol.dronebackend.config.RabbitMQConfig;
import com.skycontrol.dronebackend.model.Alert;
import com.skycontrol.dronebackend.service.AlertService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AlertListener {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlertService alertService;

    
     //Ouve a fila de Alertas (definida em RabbitMQConfig).
     // Esta fila recebe alertas de emergência dos drones.
     
    @RabbitListener(queues = RabbitMQConfig.ALERT_QUEUE)
    public void handleAlertMessage(String alertJson) {
        try {
            Alert alert = objectMapper.readValue(alertJson, Alert.class); 
            alertService.processExternalAlert(alert); 
        } catch (Exception e) {
            System.err.println("[AlertListener] ERRO CRÍTICO ao processar alerta. JSON: " + alertJson + " | Erro: " + e.getMessage());
        }
    }
}