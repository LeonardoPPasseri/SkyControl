package com.skycontrol.dronebackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.skycontrol.dronebackend.service.DroneCommandService;


import java.util.Map;

@RestController
@RequestMapping("/api/drones")
@CrossOrigin(origins = "*")
public class DroneCommandController {

    @Autowired
    private DroneCommandService commandService;

    // Endpoint para enviar comandos para um drone específico.
    @PostMapping("/{id}/command")
    public ResponseEntity<Void> receiveCommand(
            @PathVariable Long id, 
            @RequestBody Map<String, Object> payload) {
        
        String command = (String) payload.get("command");
        if (command == null) {
            return ResponseEntity.badRequest().build();
        }

        commandService.processCommand(id, payload);
        
        System.out.println("[CommandController] Comando recebido para Drone ID: " + id + " | Payload: " + payload);
        return ResponseEntity.ok().build();
    }
}