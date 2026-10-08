package com.example.containermanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Container Management System.
 *
 * This bootstraps the Spring application context, starts the embedded
 * Tomcat server, and triggers component scanning for everything under
 * com.example.containermanagement (controller, service, repository, entity, config).
 */
@SpringBootApplication
public class ContainerManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContainerManagementApplication.class, args);
    }

}
