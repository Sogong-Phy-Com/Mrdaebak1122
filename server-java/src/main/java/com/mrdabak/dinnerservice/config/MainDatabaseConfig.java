package com.mrdabak.dinnerservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
    basePackages = {
        "com.mrdabak.dinnerservice.repository"
    },
    excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.REGEX,
        pattern = "com\\.mrdabak\\.dinnerservice\\.repository\\.order\\..*"
    )
)
public class MainDatabaseConfig {
}

