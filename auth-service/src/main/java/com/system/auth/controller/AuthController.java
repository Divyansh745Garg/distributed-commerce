package com.system.auth.controller;

import com.system.auth.dto.AuthRequest;
import com.system.auth.dto.AuthResponse;
import com.system.auth.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthRequest request) {
        // Mock authentication validation for architecture demonstration
        if ("admin".equals(request.getUsername()) && "password".equals(request.getPassword())) {

            // Generate the token containing the username and a mocked "USER" role
            String token = jwtUtils.generateToken(request.getUsername(), "ROLE_USER");
            return ResponseEntity.ok(new AuthResponse(token, "Bearer"));

        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    // A protected test endpoint to verify the filter works
    @GetMapping("/verify")
    public ResponseEntity<String> verifySecurity() {
        return ResponseEntity.ok("You have successfully accessed a secured endpoint!");
    }
}