package com.skycontrol.dronebackend.service;

import com.skycontrol.dronebackend.model.Drone;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skycontrol.dronebackend.config.RabbitMQConfig;
import com.skycontrol.dronebackend.database.JsonDatabaseService;
import com.skycontrol.dronebackend.events.EventPublisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class DroneService {

    @Autowired 
    private EventPublisher eventPublisher;

    @Autowired 
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JsonDatabaseService db; //

    private List<Drone> drones = new ArrayList<>();

    @PostConstruct
    public void init() {
        drones = db.load();
        if (drones == null) drones = new ArrayList<>();
        
        // Inicializa o Simulador com os drones existentes
        initializeSimulator(); 
    }

    private void initializeSimulator() {
        System.out.println("[DroneService] Publicando drones existentes para inicialização do Simulador...");
        for (Drone drone : drones) {
            publishDroneEvent("DRONE_CREATED", drone);
        }
        System.out.println("[DroneService] Inicialização concluída.");
    }
    private void publishDroneEvent(String eventType, Drone drone) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", eventType);
            payload.put("data", drone);
            
            String json = objectMapper.writeValueAsString(payload);
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INIT_EXCHANGE, 
                RabbitMQConfig.INIT_ROUTING_KEY, 
                json
            );
        } catch (Exception e) {
            System.err.println("Erro ao publicar evento de drone: " + e.getMessage());
            
        }
    }
    public List<Drone> getAll() {
        return new ArrayList<>(drones);
    }

    public Drone create(Drone drone) {
        drone.setId(generateId());
        drones.add(drone);
        
        db.save(drones);
        
        // PUBLICAÇÃO INTERNA (ATUALIZA O FRONTEND IMEDIATAMENTE)
        eventPublisher.publish("DRONE_CREATED", drone); 
        
        // PUBLICAÇÃO EXTERNA (INICIA A THREAD NO SIMULADOR)
        publishDroneEvent("DRONE_CREATED", drone); 

        return drone;
    }

    public Drone getById(Long id) {
        return drones.stream()
                .filter(d -> d.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public boolean delete(Long id) {
        Drone drone = getById(id);
        if (drone == null) return false;

        // Remove da lista em memória
        drones.removeIf(d -> d.getId().equals(id));
        
        // Salva a lista atualizada de drones
        db.save(drones);
        
        //  Arquiva a telemetria antiga
        db.archiveTelemetry(id);

        // Notifica o sistema
        eventPublisher.publish("DRONE_DELETED", drone); 
        publishDroneEvent("DRONE_DELETED", drone);

        return true;
    }

    private Long generateId() {
        return drones.stream()
                .mapToLong(Drone::getId)
                .max()
                .orElse(0L) + 1;
    }
}