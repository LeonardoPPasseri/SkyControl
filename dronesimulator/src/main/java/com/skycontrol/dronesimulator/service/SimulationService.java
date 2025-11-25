package com.skycontrol.dronesimulator.service; // PACOTE SUGERIDO PARA O NOVO MICROSSERVIÇO

import com.skycontrol.dronesimulator.model.Drone;
import com.skycontrol.dronesimulator.simulator.SimulatedDrone;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


// Gerencia o ciclo de vida das simulações de drones neste Microsserviço Simulação.
// É controlado APENAS por eventos RabbitMQ (via InitListener e CommandListener).

@Service
public class SimulationService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final Map<Long, SimulatedDrone> activeSimulations = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();


    public void startSimulation(Drone drone) {
        if (!drone.isActive()) {
            return;
        }

        if (activeSimulations.containsKey(drone.getId())) {
            System.out.println("[SimulationService] Simulação para Drone " + drone.getId() + " já está ativa. Ignorando.");
            return;
        }

        System.out.println("[SimulationService] Iniciando simulação para Drone ID: " + drone.getId());

        SimulatedDrone simulatedDrone = new SimulatedDrone(drone, rabbitTemplate);

        activeSimulations.put(drone.getId(), simulatedDrone);
        executor.submit(simulatedDrone);
    }
    public void stopSimulation(Drone drone) {
        SimulatedDrone simulation = activeSimulations.get(drone.getId());

        if (simulation != null) {
            System.out.println("[SimulationService] Parando simulação para Drone ID: " + drone.getId());
            simulation.stop(); // Sinaliza para a thread parar
            activeSimulations.remove(drone.getId()); // Remove do mapa
        } else {
            System.out.println("[SimulationService] Simulação para Drone " + drone.getId() + " não encontrada para parar. Ignorando.");
        }
    }
    public void setDroneTarget(Long droneId, double lat, double lng) {
        SimulatedDrone simulation = activeSimulations.get(droneId);

        if (simulation != null) {
            System.out.println("[SimulationService] Recebido novo alvo para Drone " + droneId + ": (" + lat + ", " + lng + ")");
            simulation.setTargetPosition(lat, lng);
        } else {
            System.out.println("[SimulationService] Drone " + droneId + " não está ativo. Comando ignorado.");
        }
    }

    public void resumeSimulation(Long droneId) {
        SimulatedDrone simulation = activeSimulations.get(droneId);

        if (simulation != null) {
            System.out.println("[SimulationService] Retomando simulação para Drone " + droneId);
            simulation.resume();
        } else {
            System.out.println("[SimulationService] Drone " + droneId + " não está ativo. Comando ignorado.");
        }
    }

    @PreDestroy
    public void cleanup() {

        activeSimulations.values().forEach(SimulatedDrone::stop);

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}