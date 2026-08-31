package com.main;

import com.main.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pantry-rate-limit-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "app.auth.rate-limit.login.max-attempts=2",
        "app.auth.rate-limit.registration.max-attempts=1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRateLimitIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void authenticationEndpointsReturnJsonRateLimitResponses() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(remoteAddress("203.0.113.10"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"owner","password":"wrong-password"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddress("203.0.113.10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"owner","password":"wrong-password"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", matchesPattern("[1-9][0-9]*")))
                .andExpect(jsonPath("$.error").value("Too many attempts. Please try again later"));

        mockMvc.perform(post("/api/auth/register")
                        .with(remoteAddress("203.0.113.20"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"first-limited-user","password":"public-password"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .with(remoteAddress("203.0.113.20"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"blocked-limited-user","password":"public-password"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", matchesPattern("[1-9][0-9]*")))
                .andExpect(jsonPath("$.error").value("Too many attempts. Please try again later"));

        org.assertj.core.api.Assertions.assertThat(
                userRepository.findByUsername("blocked-limited-user")).isEmpty();
    }

    private static RequestPostProcessor remoteAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
