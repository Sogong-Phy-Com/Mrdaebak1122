package com.mrdabak.dinnerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "com.mrdabak.dinnerservice.model")
public class DinnerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DinnerServiceApplication.class, args);
    }
}




