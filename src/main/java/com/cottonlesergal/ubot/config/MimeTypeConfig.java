package com.cottonlesergal.ubot.config;

import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to ensure proper MIME types for static resources
 */
@Configuration
public class MimeTypeConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);

        // Ensure proper MIME types for common file extensions
        mappings.remove("html");
        mappings.add("html", "text/html;charset=utf-8");

        mappings.remove("js");
        mappings.add("js", "application/javascript;charset=utf-8");

        mappings.remove("css");
        mappings.add("css", "text/css;charset=utf-8");

        mappings.remove("json");
        mappings.add("json", "application/json;charset=utf-8");

        mappings.remove("svg");
        mappings.add("svg", "image/svg+xml");

        // Add mappings for font files
        mappings.add("woff", "font/woff");
        mappings.add("woff2", "font/woff2");
        mappings.add("ttf", "font/ttf");
        mappings.add("eot", "application/vnd.ms-fontobject");
        mappings.add("otf", "font/otf");

        factory.setMimeMappings(mappings);
    }
}