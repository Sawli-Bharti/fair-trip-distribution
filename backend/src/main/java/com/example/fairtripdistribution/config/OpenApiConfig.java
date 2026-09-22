package com.example.fairtripdistribution.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI fairTripOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fair Trip Distribution API")
                        .description("""
                                REST API for the Fair Trip Distribution engine.

                                **Authentication:** Register a user at `/api/auth/register`, then login at `/api/auth/login`
                                to receive a JWT token. Click **Authorize** and enter `<your-token>` (without 'Bearer ') to test
                                protected endpoints.

                                **Roles:**
                                - `USER` — can allocate and reject trips.
                                - `ADMIN` — can manage vendors, zones, share configuration, capacity, and view reports.

                                **Fairness algorithm:** Most-Owed-First with carry-forward using integer basis points (10 000 bp = 100%).
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Fair Trip Distribution Team")))
                .servers(List.of(new Server().url("/").description("Local")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the JWT token obtained from /api/auth/login")));
    }
}
