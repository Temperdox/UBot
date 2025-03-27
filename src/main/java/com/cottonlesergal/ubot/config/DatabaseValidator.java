package com.cottonlesergal.ubot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Arrays;

/**
 * Database validation utility.
 * Validates database connectivity and configuration during startup.
 */
@Configuration
@Slf4j
public class DatabaseValidator {

    @Autowired
    private Environment env;

    /**
     * Validate database connection and configuration at startup.
     */
    @Bean
    public CommandLineRunner validateDatabase(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        return args -> {
            boolean isDevProfile = Arrays.asList(env.getActiveProfiles()).contains("dev");

            log.info("---------- Database Validation ----------");
            log.info("Active profile: {}", Arrays.toString(env.getActiveProfiles()));

            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                log.info("Database connection established successfully");
                log.info("Connected to: {} {}", metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion());
                log.info("JDBC Driver: {} {}", metaData.getDriverName(), metaData.getDriverVersion());
                log.info("URL: {}", metaData.getURL());

                // Test query to verify connectivity
                if (isDevProfile) {
                    String dbTime = jdbcTemplate.queryForObject("SELECT NOW()", String.class);
                    log.info("Database server time: {}", dbTime);
                }

                log.info("Database validation completed successfully");
            } catch (Exception e) {
                log.error("Database validation failed: {}", e.getMessage());
                if (isDevProfile) {
                    log.error("Exception details:", e);
                }
            }
            log.info("---------------------------------------");
        };
    }
}