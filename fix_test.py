import os

path = 'backend/src/test/java/com/example/fairtripdistribution/security/Phase5SecurityTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

replacement = """    public void testUserCannotAccessAdminEndpoint() throws Exception {
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
    }"""

import re
content = re.sub(r'public void testUserCannotAccessAdminEndpoint\(\) throws Exception \{.*?(?=\s+@Test\s+public void testMalformedTokenRejected)', replacement, content, flags=re.DOTALL)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated Phase5SecurityTest")
