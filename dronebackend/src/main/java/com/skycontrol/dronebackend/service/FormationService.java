package com.skycontrol.dronebackend.service;

import com.skycontrol.dronebackend.database.JsonDatabaseService;
import com.skycontrol.dronebackend.model.Drone;
import com.skycontrol.dronebackend.model.DroneTelemetry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FormationService {

    @Autowired
    private JsonDatabaseService dbService; 

    @Autowired
    private DroneCommandService commandService; 

    private static final double SPACING = 0.0020; // Distância (~200m)

    public void applyFormation(String formationType, Long leaderId) {
        // Carrega drones ativos
        List<Drone> activeDrones = dbService.load().stream()
                .filter(Drone::isActive)
                .sorted(Comparator.comparing(Drone::getId))
                .collect(Collectors.toList());

        if (activeDrones.isEmpty()) return;

        // Identifica o Líder
        Drone leaderTemp = null;
        if (leaderId != null) {
            leaderTemp = activeDrones.stream()
                    .filter(d -> d.getId().equals(leaderId))
                    .findFirst()
                    .orElse(null);
        }
        // Se não tem líder, pega o primeiro (Menor ID)
        if (leaderTemp == null) leaderTemp = activeDrones.get(0);
        
        final Drone leader = leaderTemp;

        // Obtém a posição do Líder (Centro da Formação)
        double centerLat = -22.9000;
        double centerLng = -43.2000;

        List<DroneTelemetry> telemetries = dbService.loadTelemetry();
        DroneTelemetry leaderTel = telemetries.stream()
                .filter(t -> t.getDroneId() == leader.getId().intValue())
                .findFirst()
                .orElse(null);

        if (leaderTel != null) {
            centerLat = leaderTel.getLat();
            centerLng = leaderTel.getLng();
        }

        // Posiciona o Líder no Centro
        if ("BASE".equals(formationType)) {
            centerLat = -22.9000;
            centerLng = -43.2000;
        }
        sendCommandToDrone(leader.getId(), centerLat, centerLng);

        // Distribui os Seguidores
        List<Drone> followers = new ArrayList<>(activeDrones);
        followers.remove(leader);

        for (int i = 0; i < followers.size(); i++) {
            Drone drone = followers.get(i);

            int dist = (i / 2) + 1;
            
            int side = (i % 2 == 0) ? 1 : -1;

            double targetLat = centerLat;
            double targetLng = centerLng;

            switch (formationType) {
                case "LINE":
                    // Linha Horizontal: Líder no meio, outros na esquerda/direita
                    targetLng = centerLng + (side * dist * SPACING);
                    break;

                case "COLUMN":
                    // Coluna Vertical: Líder no meio, outros atrás/frente
                    targetLat = centerLat - (side * dist * SPACING); 
                    break;

                case "DIAGONAL":
                    // Diagonal (/): Líder no meio, outros em Nordeste/Sudoeste
                    targetLat = centerLat + (side * dist * SPACING);
                    targetLng = centerLng + (side * dist * SPACING);
                    break;

                case "V_SHAPE":
                    // Formação V: Líder na frente, outros abrem para trás
                    targetLat = centerLat - (dist * SPACING); 
                    // E abrem para os lados (Alterna Lng)
                    targetLng = centerLng + (side * dist * SPACING);
                    break;
                
                case "BASE":
                    // Retornar à Base (Formação de pouso aleatória próxima à base)
                    targetLat = -22.9000 + (randSignal() * dist * 0.0005);
                    targetLng = -43.2000 + (randSignal() * dist * 0.0005);
                    break;
            }

            sendCommandToDrone(drone.getId(), targetLat, targetLng);
        }
    }
    
    // Helper para gerar 1 ou -1 aleatoriamente
    private int randSignal() {
        return Math.random() < 0.5 ? -1 : 1;
    }

    private void sendCommandToDrone(Long droneId, double lat, double lng) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("command", "GO_TO_POSITION");
        payload.put("lat", lat);
        payload.put("lng", lng);
        commandService.processCommand(droneId, payload); //
    }
}