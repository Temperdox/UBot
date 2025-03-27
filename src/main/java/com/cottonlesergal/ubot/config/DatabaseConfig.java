package com.cottonlesergal.ubot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * Database configuration for UBot application.
 * Provides support for both MySQL and H2 in-memory database,
 * automatically switching to H2 if MySQL connection details are missing.
 */
@Configuration
@Slf4j
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    /**
     * Primary DataSource bean.
     * Will use MySQL if configured, otherwise falls back to H2.
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        boolean isH2 = driverClassName.contains("h2") || url.contains("h2");

        log.info("Configuring database: {}", isH2 ? "H2 (in-memory)" : "MySQL");

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    /**
     * H2 specific DataSource bean.
     * Only activated if the driver is H2.
     */
    @Bean(name = "h2DataSource")
    @ConditionalOnProperty(name = "spring.datasource.driver-class-name", havingValue = "org.h2.Driver")
    public DataSource h2DataSource() {
        log.info("Creating embedded H2 database");
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("ubot_discord")
                .build();
    }
}