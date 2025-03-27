package com.cottonlesergal.ubot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Configure path matching to not use suffix pattern matching
     * This helps ensure proper resolution of resource MIME types
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseSuffixPatternMatch(false);
    }

    /**
     * Configure content negotiation to favor path extension over other strategies
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .favorPathExtension(true)
                .ignoreAcceptHeader(false)
                .useRegisteredExtensionsOnly(false);
    }
}