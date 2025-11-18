package com.skycontrol.dronebackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skycontrol.dronebackend.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DroneCommandService {

    @Autowired
    private RabbitTemplate rabbitTemplate; 

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Recebe um comando genérico (do DroneCommandController) e decide para onde enviá-lo.
     */
    public void processCommand(Long droneId, Map<String, Object> payload) {
        
        String command = (String) payload.get("command");
        if (command == null) return;

        
        // ---------------------------------------------------------------------
        // 2. COMANDOS EXTERNOS (AGORA INCLUI O CONTINUE)
        // ---------------------------------------------------------------------
        // Todos os comandos operacionais são enviados para o Microsserviço Simulador.
        if (command.equals("GO_TO_POSITION") || 
            command.equals("SET_FORMATION") || 
            command.equals("CONTINUE")) { 

            publishCommandToRabbitMQ(droneId, payload);
        
        } 
        
        else {
            System.err.println("[DroneCommandService] Comando desconhecido ou não roteado: " + command);
        }
    }

    /**
     * Publica um payload de comando no exchange de comandos do RabbitMQ.
     * Esta lógica permanece inalterada.
     */
    private void publishCommandToRabbitMQ(Long droneId, Map<String, Object> payload) {
        try {
            String routingKey = "drone." + droneId + ".command";
            
            // Adiciona o droneId ao payload antes de enviar
            payload.put("droneId", droneId);
            
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            rabbitTemplate.convertAndSend(RabbitMQConfig.COMMAND_EXCHANGE, routingKey, jsonPayload);

            System.out.println("[DroneCommandService] Comando EXTERNO publicado para RabbitMQ: " + jsonPayload);

        } catch (Exception e) {
            System.err.println("[DroneCommandService] Falha ao publicar comando no RabbitMQ: " + e.getMessage());
        }
    }
    
}