package com.enterprise_wrapper_api.wrapper_api.security.controller;

import com.enterprise_wrapper_api.wrapper_api.security.dto.AuthRequest;
import com.enterprise_wrapper_api.wrapper_api.security.dto.AuthResponse;
import com.enterprise_wrapper_api.wrapper_api.security.dto.RegisterRequest;
import com.enterprise_wrapper_api.wrapper_api.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Register, login, and get current user info")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account and returns a JWT token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration successful"),
        @ApiResponse(responseCode = "400", description = "Username or email already taken")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "Login", description = "Authenticate with username and password to get a JWT token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Get current user", description = "Returns the authenticated user's username and role")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current user info"),
        @ApiResponse(responseCode = "403", description = "No valid token provided")
    })
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(
            org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                "role", authentication.getAuthorities().iterator().next().getAuthority()
        ));
    }
}
