package com.skycontrol.dronesimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DroneSimulatorApplication {

	public static void main(String[] args) {
        System.setProperty("server.port", "8081"); 
		SpringApplication.run(DroneSimulatorApplication.class, args);
	}	
}
