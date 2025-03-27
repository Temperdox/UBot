package com.cottonlesergal.ubot.config;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Hibernate specific configuration to handle MySQL compatibility issues.
 */
@Configuration
public class HibernateConfig {

    @Autowired
    private Environment env;

    /**
     * Customize Hibernate properties to ensure compatibility with MySQL.
     * This specifically addresses sequence-related issues that can occur.
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            // Disable use of sequences in MySQL
            hibernateProperties.put("hibernate.id.new_generator_mappings", "false");

            // Skip sequence scanning during schema validation
            hibernateProperties.put("hibernate.sequence.skip_validation", "true");

            // Default UUID generator implementation
            hibernateProperties.put("hibernate.id.uuid_generator_use_random", "true");

            // Configure proper ID optimizer
            hibernateProperties.put("hibernate.id.optimizer.pooled.preferred", "pooled-lo");

            // Use quoted identifiers to avoid reserved word issues
            hibernateProperties.put("hibernate.globally_quoted_identifiers", "true");

            // For debugging purposes
            if (env.getProperty("spring.profiles.active", "").contains("dev")) {
                hibernateProperties.put("hibernate.format_sql", "true");
                hibernateProperties.put("hibernate.use_sql_comments", "true");
            }
        };
    }
}