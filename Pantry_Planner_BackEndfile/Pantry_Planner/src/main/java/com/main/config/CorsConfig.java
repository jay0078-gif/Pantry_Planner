package com.main.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // apply to all endpoints
                // ✅ Allow both localhost and 127.0.0.1 for flexibility
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // ✅ Allow cookies / authentication headers to be included
                .allowCredentials(true)
                // ✅ Optional: expose headers your frontend may need to read
                .exposedHeaders("Authorization", "Link", "X-Total-Count");
    }
}