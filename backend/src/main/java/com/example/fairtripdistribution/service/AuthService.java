package com.example.fairtripdistribution.service;

import com.example.fairtripdistribution.exception.BusinessValidationException;
import com.example.fairtripdistribution.model.dto.AuthRequestDto;
import com.example.fairtripdistribution.model.dto.AuthResponseDto;
import com.example.fairtripdistribution.model.dto.RegisterRequestDto;
import com.example.fairtripdistribution.model.entity.User;
import com.example.fairtripdistribution.model.entity.enums.Role;
import com.example.fairtripdistribution.repository.UserRepository;
import com.example.fairtripdistribution.security.CustomUserDetails;
import com.example.fairtripdistribution.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                       JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByEmail(request.email)) {
            throw new BusinessValidationException("Email is already registered");
        }

        User user = new User();
        user.setEmail(request.email);
        user.setPasswordHash(passwordEncoder.encode(request.password));
        user.setRole(Role.USER);
        userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(userDetails);
        
        return new AuthResponseDto(token, user.getEmail(), user.getRole());
    }

    public AuthResponseDto login(AuthRequestDto request) {
        // This will throw BadCredentialsException if invalid, resulting in 401 via GlobalExceptionHandler or Spring Security
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email, request.password)
        );

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);
        
        return new AuthResponseDto(token, userDetails.getUsername(), userDetails.getRole());
    }
}
