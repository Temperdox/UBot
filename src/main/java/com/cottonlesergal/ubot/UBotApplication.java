package com.cottonlesergal.ubot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for UBot - Discord Bot Control Panel.
 * This application connects to Discord via JDA and provides a web interface
 * that mimics Discord's UI for controlling and monitoring the bot.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@Slf4j
public class UBotApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        try {
            log.info("Starting UBot application");
            SpringApplication.run(UBotApplication.class, args);
            log.info("UBot application started successfully");
        } catch (Exception e) {
            log.error("Failed to start UBot application", e);
            System.exit(1);
        }
    }
}