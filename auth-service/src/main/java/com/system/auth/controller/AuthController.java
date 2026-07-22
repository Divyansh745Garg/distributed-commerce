package com.system.auth.controller;

import com.system.auth.dto.AuthRequest;
import com.system.auth.dto.AuthResponse;
import com.system.auth.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate; // <-- NEW
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Date; // <-- NEW
import java.util.concurrent.TimeUnit; // <-- NEW

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate; // <-- NEW: Injected automatically by Lombok

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

    // NEW LOGOUT ENDPOINT
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Get exactly how much time is left on this token
                Date expirationDate = jwtUtils.extractExpiration(token);
                long ttlMillis = expirationDate.getTime() - System.currentTimeMillis();

                // If it hasn't expired yet, add it to the Redis Blacklist
                if (ttlMillis > 0) {
                    redisTemplate.opsForValue().set("blacklist:" + token, "invalid", ttlMillis, TimeUnit.MILLISECONDS);
                    return ResponseEntity.ok("Successfully logged out. Token is blacklisted.");
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token is invalid or already expired.");
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid authorization header.");
    }
}