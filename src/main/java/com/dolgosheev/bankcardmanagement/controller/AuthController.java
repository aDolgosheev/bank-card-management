package com.dolgosheev.bankcardmanagement.controller;

import com.dolgosheev.bankcardmanagement.dto.auth.JwtResponse;
import com.dolgosheev.bankcardmanagement.dto.auth.LoginRequest;
import com.dolgosheev.bankcardmanagement.dto.auth.SignupRequest;
import com.dolgosheev.bankcardmanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API for user authentication and registration")
public class AuthController {
    
    private final AuthService authService;

    @PostMapping("/signin")
    @Operation(summary = "User authentication", description = "User login and JWT token generation")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.authenticateUser(loginRequest));
    }

    @PostMapping("/signup")
    @Operation(summary = "User registration", description = "Register a new user in the system")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        authService.registerUser(signUpRequest);
        return ResponseEntity.ok().body("User successfully registered!");
    }
} 