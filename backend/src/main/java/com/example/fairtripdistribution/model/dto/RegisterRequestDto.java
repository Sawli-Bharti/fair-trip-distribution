package com.example.fairtripdistribution.model.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequestDto {
    @NotBlank
    @Email
    public String email;
    
    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters")
    public String password;
}
