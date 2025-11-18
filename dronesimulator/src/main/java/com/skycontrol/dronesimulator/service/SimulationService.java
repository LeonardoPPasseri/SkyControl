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

/**
 * Gerencia o ciclo de vida das simulações de drones neste Microsserviço Simulação.
 * É controlado APENAS por eventos RabbitMQ (via InitListener e CommandListener).
 */
@Service
public class SimulationService {

    @Autowired
    private RabbitTemplate rabbitTemplate; // Para o SimulatedDrone enviar msgs

    // Mapa para manter referências a todas as threads ativas.
    private final Map<Long, SimulatedDrone> activeSimulations = new ConcurrentHashMap<>();

    // Pool de threads para executar os SimulatedDrones.
    private final ExecutorService executor = Executors.newCachedThreadPool();


    /**
     * Inicia a simulação para um drone (thread).
     * Chamado pelo InitListener (após receber um evento DRONE_CREATED).
     */
    public void startSimulation(Drone drone) {
        if (!drone.isActive()) {
            return; // Não inicia se o drone estiver marcado como inativo
        }

        if (activeSimulations.containsKey(drone.getId())) {
            System.out.println("[SimulationService] Simulação para Drone " + drone.getId() + " já está ativa. Ignorando.");
            return;
        }

        System.out.println("[SimulationService] Iniciando simulação para Drone ID: " + drone.getId());

        // Cria a instância do SimulatedDrone, passando o RabbitTemplate
        // O SimulatedDrone precisa do RabbitTemplate para publicar Telemetria e Alertas
        SimulatedDrone simulatedDrone = new SimulatedDrone(drone, rabbitTemplate);

        // Armazena e submete ao Executor
        activeSimulations.put(drone.getId(), simulatedDrone);
        executor.submit(simulatedDrone);
    }

    /**
     * Para a simulação de um drone.
     * Chamado pelo InitListener (após receber um evento DRONE_DELETED ou DRONE_UPDATED).
     */
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

    /**
     * Define uma nova posição alvo para o drone.
     * Chamado pelo CommandListener.
     */
    public void setDroneTarget(Long droneId, double lat, double lng) {
        SimulatedDrone simulation = activeSimulations.get(droneId);

        if (simulation != null) {
            System.out.println("[SimulationService] Recebido novo alvo para Drone " + droneId + ": (" + lat + ", " + lng + ")");
            simulation.setTargetPosition(lat, lng);
        } else {
            System.out.println("[SimulationService] Drone " + droneId + " não está ativo. Comando ignorado.");
        }
    }

    /**
     * Retoma a simulação (depois de um comando PAUSE/EMERGENCY).
     * Chamado pelo CommandListener.
     */
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
        System.out.println("[SimulationService] Desligando o Simulador... Parando todas as simulações ativas.");

        // Avisa a todos os drones para pararem
        activeSimulations.values().forEach(SimulatedDrone::stop);

        // Desliga o ExecutorService e espera as threads terminarem
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Força o desligamento se não terminar em 5s
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[SimulationService] Desligamento concluído.");
    }
}