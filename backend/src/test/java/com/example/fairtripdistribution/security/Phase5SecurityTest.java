package com.example.fairtripdistribution.security;

import com.example.fairtripdistribution.model.dto.AuthRequestDto;
import com.example.fairtripdistribution.model.dto.RegisterRequestDto;
import com.example.fairtripdistribution.model.entity.User;
import com.example.fairtripdistribution.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = {"app.jwt.expiration=86400000", "app.jwt.secret=8a9b2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0"})
@AutoConfigureMockMvc
public class Phase5SecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
    }

    @Test
    public void testSuccessfulRegistrationAndHashing() throws Exception {
        RegisterRequestDto req = new RegisterRequestDto();
        req.email = "test@example.com";
        req.password = "securePassword123";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("USER"));

        User saved = userRepository.findByEmail("test@example.com").get();
        assertThat(saved.getPasswordHash()).isNotEqualTo("securePassword123");
        assertThat(passwordEncoder.matches("securePassword123", saved.getPasswordHash())).isTrue();
    }

    @Test
    public void testDuplicateRegistrationRejected() throws Exception {
        RegisterRequestDto req = new RegisterRequestDto();
        req.email = "dup@example.com";
        req.password = "securePassword123";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()); // Handled by GlobalExceptionHandler for BusinessValidationException
    }

    @Test
    public void testLoginSuccessAndInvalidRejected() throws Exception {
        RegisterRequestDto reg = new RegisterRequestDto();
        reg.email = "login@example.com";
        reg.password = "securePassword123";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        AuthRequestDto req = new AuthRequestDto();
        req.email = "login@example.com";
        req.password = "securePassword123";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        req.password = "wrongPassword";
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized()); // 401
    }

    @Test
    public void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/vendors"))
                .andExpect(status().isUnauthorized()); // 401
    }

    @Test
        public void testUserCannotAccessAdminEndpoint() throws Exception {
        RegisterRequestDto reg = new RegisterRequestDto();
        reg.email = "user@example.com";
        reg.password = "securePassword123";

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andReturn();
        String response = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();

        // Vendor endpoints are ADMIN only
        mockMvc.perform(get("/api/vendors")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden()); // 403
                
        // Now test ADMIN access
        com.example.fairtripdistribution.model.entity.User admin = new com.example.fairtripdistribution.model.entity.User();
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(passwordEncoder.encode("adminPass"));
        admin.setRole(com.example.fairtripdistribution.model.entity.enums.Role.ADMIN);
        userRepository.save(admin);
        
        AuthRequestDto login = new AuthRequestDto();
        login.email = "admin@example.com";
        login.password = "adminPass";
        
        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andReturn();
        String adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString()).get("token").asText();
        
        mockMvc.perform(get("/api/vendors")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()); // 200
    }
    
    @Test
    public void testMalformedTokenRejected() throws Exception {
        mockMvc.perform(get("/api/vendors")
                .header("Authorization", "Bearer invalid-token-string"))
                .andExpect(status().isUnauthorized()); // 401
    }
}
