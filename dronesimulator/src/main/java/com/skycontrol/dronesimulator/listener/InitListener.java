package com.skycontrol.dronesimulator.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skycontrol.dronesimulator.config.RabbitMQConfig;
import com.skycontrol.dronesimulator.model.Drone;
import com.skycontrol.dronesimulator.service.SimulationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
@Component
public class InitListener {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimulationService simulationService; // O serviço que gerencia as threads

    @RabbitListener(queues = RabbitMQConfig.INIT_QUEUE)
    public void handleInitMessage(String initJson) {
        try {
            Map<String, Object> wrapper = objectMapper.readValue(initJson, new TypeReference<Map<String, Object>>() {});
            String eventType = (String) wrapper.get("eventType");
            
            Drone drone = objectMapper.convertValue(wrapper.get("data"), Drone.class);

            if (drone == null) return;

            switch (eventType) {
                case "DRONE_CREATED":
                    simulationService.startSimulation(drone);
                    break;
                case "DRONE_DELETED":
                    simulationService.stopSimulation(drone);
                    break;
            }

        } catch (Exception e) {
            System.err.println("[InitListener] Erro ao processar mensagem de inicialização: " + e.getMessage());
        }
    }
}