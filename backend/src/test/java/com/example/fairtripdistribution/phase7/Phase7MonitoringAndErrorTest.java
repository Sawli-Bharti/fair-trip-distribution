package com.example.fairtripdistribution.phase7;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "app.jwt.expiration=86400000",
    "app.jwt.secret=8a9b2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0",
    "spring.main.allow-bean-definition-overriding=true",
    // Disable Redis health check in tests since Redis is not running
    "management.health.redis.enabled=false"
})
@AutoConfigureMockMvc
public class Phase7MonitoringAndErrorTest {

    @TestConfiguration
    static class InMemoryCache {
        @Bean @Primary
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                "vendors", "activeZones", "vendorZoneShares", "dailyReports", "monthlyReports");
        }
    }

    @Autowired MockMvc mockMvc;

    // ----------------------------------------------------------
    // 1. Actuator /health is publicly accessible and returns UP
    // ----------------------------------------------------------
    @Test
    public void testActuatorHealthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status", is("UP")));
    }

    // ----------------------------------------------------------
    // 2. /actuator/env must NOT be exposed (returns 404, not 200)
    // ----------------------------------------------------------
    @Test
    public void testActuatorEnvNotExposed() throws Exception {
        // When the endpoint is not exposed, Spring Boot returns 404 (not found in actuator)
        mockMvc.perform(get("/actuator/env"))
               .andExpect(status().is4xxClientError()); // 404 when not exposed
    }

    // ----------------------------------------------------------
    // 3. 404 — ResourceNotFoundException produces consistent body
    // ----------------------------------------------------------
    @Test
    public void testResourceNotFoundReturnsConsistentBody() throws Exception {
        // Trips endpoint is accessible by USER role. A non-existent trip raises ResourceNotFoundException.
        String userToken = registerAndLogin("user_404@test.com");

        // Reject a non-existent trip — triggers ResourceNotFoundException
        mockMvc.perform(post("/api/trips/reject")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"externalTripId\":\"NONEXISTENT-TRIP-99999\",\"reason\":\"test\"}"))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$.status", is(404)))
               .andExpect(jsonPath("$.message").exists())
               .andExpect(jsonPath("$.timestamp").exists())
               .andExpect(jsonPath("$.path").exists())
               // Must never expose a raw stack trace
               .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ----------------------------------------------------------
    // 4. Validation error returns 400 with consistent body
    // ----------------------------------------------------------
    @Test
    public void testValidationErrorReturns400() throws Exception {
        // A missing required field on allocation → validation error
        String userToken = registerAndLogin("user_val@test.com");

        mockMvc.perform(post("/api/trips/allocate")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.status", is(400)))
               .andExpect(jsonPath("$.timestamp").exists());
    }

    // ----------------------------------------------------------
    // 5. Unauthenticated request returns 401
    // ----------------------------------------------------------
    @Test
    public void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/vendors"))
               .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------
    // 6. Business rule violation returns 400 with message (no stack trace)
    // ----------------------------------------------------------
    @Test
    public void testBusinessErrorReturns400() throws Exception {
        String userToken = registerAndLogin("user_biz@test.com");

        // No zone configured → BusinessValidationException
        mockMvc.perform(post("/api/trips/allocate")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"externalTripId\":\"NO-ZONE-BIZ-99\",\"distance\":\"5.0\",\"tripType\":\"NORMAL\"}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.status", is(400)))
               .andExpect(jsonPath("$.message").exists())
               .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ----------------------------------------------------------
    // helpers
    // ----------------------------------------------------------

    private String registerAndLogin(String email) throws Exception {
        String regBody = String.format(
            "{\"email\":\"%s\",\"password\":\"Password123!\",\"role\":\"USER\"}", email);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(regBody));

        String loginBody = String.format(
            "{\"email\":\"%s\",\"password\":\"Password123!\"}", email);
        String resp = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(loginBody))
               .andReturn().getResponse().getContentAsString();
        return resp.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}
