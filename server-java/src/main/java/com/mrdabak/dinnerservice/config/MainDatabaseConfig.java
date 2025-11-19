package com.mrdabak.dinnerservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackageClasses = {
        com.mrdabak.dinnerservice.repository.UserRepository.class,
        com.mrdabak.dinnerservice.repository.DinnerTypeRepository.class,
        com.mrdabak.dinnerservice.repository.MenuItemRepository.class,
        com.mrdabak.dinnerservice.repository.DinnerMenuItemRepository.class
    },
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
public class MainDatabaseConfig {

    @Bean(name = "entityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("dataSource") DataSource dataSource) {
        Map<String, String> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.id.new_generator_mappings", "false");
        properties.put("hibernate.jdbc.use_get_generated_keys", "false");

        return builder
            .dataSource(dataSource)
            .packages(
                com.mrdabak.dinnerservice.model.User.class,
                com.mrdabak.dinnerservice.model.DinnerType.class,
                com.mrdabak.dinnerservice.model.MenuItem.class,
                com.mrdabak.dinnerservice.model.DinnerMenuItem.class
            )
            .persistenceUnit("default")
            .properties(properties)
            .build();
    }

    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}

