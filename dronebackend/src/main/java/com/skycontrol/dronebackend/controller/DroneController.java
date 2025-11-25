package com.skycontrol.dronebackend.controller;

import com.skycontrol.dronebackend.model.Drone;
import com.skycontrol.dronebackend.service.DroneService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/drones")
public class DroneController {

    private final DroneService droneService;

    public DroneController(DroneService droneService) {
        this.droneService = droneService;
    }

    @PostMapping
    public Drone create(@RequestBody Drone drone) {
        return droneService.create(drone);
    }

    @GetMapping
    public List<Drone> getAll() {
        return droneService.getAll();
    }

    @GetMapping("/{id}")
    public Drone getById(@PathVariable Long id) {
        return droneService.getById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        droneService.delete(id);
    }
}
