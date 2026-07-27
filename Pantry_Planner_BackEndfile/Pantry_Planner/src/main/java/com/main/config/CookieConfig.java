// src/main/java/com/main/config/CookieConfig.java
package com.main.config;

import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CookieConfig {
    @Bean
    public CookieSameSiteSupplier cookieSameSiteSupplier() {
        // Force all cookies to be SameSite=None  -> usable across ports
        return CookieSameSiteSupplier.ofNone();
    }
}