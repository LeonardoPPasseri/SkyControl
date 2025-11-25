package com.skycontrol.dronesimulator.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skycontrol.dronesimulator.config.RabbitMQConfig;
import com.skycontrol.dronesimulator.model.Alert;
import com.skycontrol.dronesimulator.model.Drone;
import com.skycontrol.dronesimulator.model.DroneTelemetry;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Random;

public class SimulatedDrone implements Runnable {

    private static final String TELEMETRY_EXCHANGE = "telemetry.exchange";
    private static final String ALERT_EXCHANGE = RabbitMQConfig.ALERT_EXCHANGE; //
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Random rand = new Random();
    private static final double METROS_POR_CICLO = 3500; // Aproximadamente 3,5 km a cada ciclo (3s)

    // Estados do Drone
    private enum State { IDLE, MOVING, PAUSED }
    private volatile State currentState = State.IDLE; // Drone começa parado

    // Controle de Thread
    private final Object pauseLock = new Object();
    private volatile boolean active = true;

    // Estado de Posição e Bateria 
    private Drone drone;
    private double lat, lng, altitude;
    private double targetLat, targetLng;
    private int battery;
    private RabbitTemplate rabbitTemplate;

    public SimulatedDrone(Drone drone, RabbitTemplate rabbitTemplate) {
        this.drone = drone;
        this.rabbitTemplate = rabbitTemplate;
        
        // Posição inicial
        this.lat = -22.9000 + (rand.nextDouble() - 0.5) * 0.1;
        this.lng = -43.2000 + (rand.nextDouble() - 0.5) * 0.1;
        this.altitude = 100.0 + (rand.nextDouble() * 20.0); 
        this.battery = 100;
        
        this.targetLat = this.lat;
        this.targetLng = this.lng;

        System.out.println("[SimulatedDrone] Thread INICIADA para Drone ID: " + drone.getId() + " em estado IDLE.");
    }

    @Override
    public void run() {
        while (active) {
            try {
                synchronized (pauseLock) {
                    while (currentState == State.PAUSED) {
                        System.out.println("[SimulatedDrone " + drone.getId() + "] PAUSADO (Emergência). Aguardando comando...");
                        pauseLock.wait();
                    }
                }

                if (currentState == State.MOVING) {
                    updateMovementToTarget();
                }
                
                // Bateria
                updateBatteryDrain();

                // Telemetria
                sendTelemetry();

                // Emergência
                checkEmergencyAlert();

                // Dorme por 3 segundos
                Thread.sleep(3000);

            } catch (InterruptedException e) {
                this.active = false;
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                if (active) e.printStackTrace();
            }
        }
    }

    // MÉTODOS DE LÓGICA INTERNA 
    private void updateMovementToTarget() {
        double distLat = targetLat - lat;
        double distLng = targetLng - lng;
        double distance = Math.sqrt(distLat * distLat + distLng * distLng);
        double step = METROS_POR_CICLO / 111111.0; 

        if (distance < step) {
            this.lat = targetLat;
            this.lng = targetLng;
            this.currentState = State.IDLE;
            System.out.println("[SimulatedDrone " + drone.getId() + "] CHEGOU AO ALVO. Voltando para IDLE.");
        } else {
            this.lat += (distLat / distance) * step;
            this.lng += (distLng / distance) * step;
            
            // Simula uma pequena variação de altitude APENAS durante o movimento
            this.altitude += (rand.nextDouble() - 0.5) * 0.5;
        }
    }

    private void updateBatteryDrain() {
        if (this.battery > 0) {
            this.battery -= 1; // Gasta 1% a cada 3s
        } else {
            this.battery = 100; // Recarrega (para teste)
        }
    }

    private void sendTelemetry() throws Exception {
        DroneTelemetry telemetry = new DroneTelemetry(
                drone.getId().intValue(), 
                this.lat, 
                this.lng,
                this.altitude,
                (currentState == State.MOVING) ? 20 + rand.nextDouble() * 10 : 0,
                this.battery
        );
        String json = mapper.writeValueAsString(telemetry);
        String routingKey = "drone." + telemetry.getDroneId() + ".telemetry";
        rabbitTemplate.convertAndSend(TELEMETRY_EXCHANGE, routingKey, json);
    }

    private void checkEmergencyAlert() throws Exception {
        // Chance de 1 em 500 (0,2%) a cada ciclo
        if (rand.nextInt(500) == 1) {
            System.out.println("!!! [SimulatedDrone " + drone.getId() + "] ALERTA DE EMERGÊNCIA DETECTADO !!!");
            
            Alert alert = new Alert(
                this.drone.getId(), "EMERGENCY_SIGNAL",
                "Sinal de emergência detectado pelo Drone " + this.drone.getId(),
                System.currentTimeMillis(), this.lat, this.lng
            );

            String alertJson = mapper.writeValueAsString(alert);
            String routingKey = "drone." + alert.getDroneId() + ".alert";
            rabbitTemplate.convertAndSend(ALERT_EXCHANGE, routingKey, alertJson); //

            this.currentState = State.PAUSED;
        }
    }

    // MÉTODOS DE CONTROLE EXTERNO

    public void stop() {
        this.active = false;
        resume();
    }

    public void resume() {
        synchronized (pauseLock) {
            this.currentState = State.IDLE; 
            pauseLock.notify();
        }
    }

    public void setTargetPosition(double lat, double lng) {
        this.targetLat = lat;
        this.targetLng = lng;
        this.currentState = State.MOVING;
        System.out.println("[SimulatedDrone " + drone.getId() + "] Comando 'GO_TO_POSITION' recebido. Movendo para: " + lat + "," + lng);
    }
    
    public Long getDroneId() {
        return this.drone.getId();
    }
}