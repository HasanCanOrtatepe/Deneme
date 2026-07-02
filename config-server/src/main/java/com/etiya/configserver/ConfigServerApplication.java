package com.etiya.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server. Serves each service's configuration from the {@code configs/} folder
 * of the backing Git repository, organized per service (e.g. {@code configs/product-service/}).
 * Clients reach it at {@code http://localhost:8888} and fetch {application}/{profile}.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
