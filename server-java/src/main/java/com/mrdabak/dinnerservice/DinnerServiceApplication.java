package com.mrdabak.dinnerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(
    basePackages = {
        "com.mrdabak.dinnerservice.repository",
        "com.mrdabak.dinnerservice.repository.order"
    }
)
@EntityScan(basePackages = "com.mrdabak.dinnerservice.model")
public class DinnerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DinnerServiceApplication.class, args);
    }
}




