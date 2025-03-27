package com.cottonlesergal.ubot.config;

import com.cottonlesergal.ubot.entities.UserCredential;
import com.cottonlesergal.ubot.repositories.UserCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializationConfig {

    @Value("${spring.security.user.name:admin}")
    private String adminUsername;

    @Value("${spring.security.user.password:password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initializeAdminUser(UserCredentialRepository userRepository,
                                                 PasswordEncoder passwordEncoder) {
        return args -> {
            // Check if admin user exists
            if (userRepository.findByUsername(adminUsername).isEmpty()) {
                // Create admin user
                UserCredential admin = UserCredential.builder()
                        .username(adminUsername)
                        .password(passwordEncoder.encode(adminPassword))
                        .build();

                userRepository.save(admin);
                System.out.println("Admin user created: " + adminUsername);
            }
        };
    }
}