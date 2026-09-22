package com.example.fairtripdistribution.model.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthRequestDto {
    @NotBlank
    @Email
    public String email;
    
    @NotBlank
    public String password;
}
