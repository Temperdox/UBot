package com.cottonlesergal.ubot.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

/**
 * JPA Configuration for the application.
 * Configures entity scanning, repository scanning, and transaction management.
 */
@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = "com.cottonlesergal.ubot.entities")
@EnableJpaRepositories(basePackages = "com.cottonlesergal.ubot.repositories")
public class JpaConfig {

    /**
     * Configure transaction manager for JPA.
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory);
        return txManager;
    }
}