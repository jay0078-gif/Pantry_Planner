package com.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    // 1️⃣ Password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2️⃣ Authentication provider
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // 3️⃣ Main Security filter chain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll() // register/login open
                .requestMatchers("/api/admin/**", "/api/owner/**")
                    .hasAnyRole("ADMIN", "OWNER")
                .requestMatchers("/api/user/**").hasRole("USER")
                .anyRequest().authenticated()
            )

            // 🧠 Session‑based login
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) -> {
                    // redirect to Suggestions after successful login
                    try {
                        response.sendRedirect("http://localhost:5173/suggestions");
                    } catch (IOException e) {
                        response.sendError(500, "Redirect failed");
                    }
                })
                .failureHandler((request, response, exception) -> {
                    response.sendError(401, "Authentication failed");
                })
                .permitAll()
            )

            // 🔐 Logout configuration – handles both /logout & /api/auth/logout
            .logout(logout -> logout
                .logoutRequestMatcher(new OrRequestMatcher(
                    new AntPathRequestMatcher("/logout", "POST"),
                    new AntPathRequestMatcher("/api/auth/logout", "POST")
                ))
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .permitAll()
                .logoutSuccessHandler((request, response, authentication) -> {
                    // If axios call: just 200 OK, otherwise redirect if browser navigation
                    String accept = request.getHeader("Accept");
                    if (accept != null && accept.contains("application/json")) {
                        response.setStatus(200);
                    } else {
                        try {
                            response.sendRedirect("http://localhost:5173/login");
                        } catch (IOException e) {
                            response.sendError(500, "Logout redirect failed");
                        }
                    }
                })
            )

            .httpBasic(httpBasic -> {});

        http.authenticationProvider(authenticationProvider());
        return http.build();
    }

    // 4️⃣ CORS config (for React dev mode)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // 5️⃣ AuthenticationManager bean for controller injection if needed
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}