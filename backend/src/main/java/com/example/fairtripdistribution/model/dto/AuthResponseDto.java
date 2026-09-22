package com.example.fairtripdistribution.model.dto;
import com.example.fairtripdistribution.model.entity.enums.Role;

public class AuthResponseDto {
    public String token;
    public String email;
    public Role role;
    
    public AuthResponseDto(String token, String email, Role role) {
        this.token = token;
        this.email = email;
        this.role = role;
    }
}
