package com.example.fairtripdistribution.controller;

import com.example.fairtripdistribution.model.dto.AuthRequestDto;
import com.example.fairtripdistribution.model.dto.RegisterRequestDto;
import com.example.fairtripdistribution.model.dto.AuthResponseDto;
import com.example.fairtripdistribution.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register a new user and obtain a JWT token")
@SecurityRequirements   // no auth required for these endpoints
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user",
               description = "Creates a USER account. All registered users receive the USER role. To create an ADMIN, set role=ADMIN (currently only via direct DB seed).",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Registration successful"),
                   @ApiResponse(responseCode = "400", description = "Email already in use or validation error")
               })
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive a JWT token",
               description = "Returns a signed JWT. Include it as `Authorization: Bearer <token>` in subsequent requests.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Login successful, token returned"),
                   @ApiResponse(responseCode = "401", description = "Invalid credentials")
               })
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

