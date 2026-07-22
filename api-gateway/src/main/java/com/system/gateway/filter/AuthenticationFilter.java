package com.system.gateway.filter;

import com.system.gateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.StringRedisTemplate; // <-- NEW
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate redisTemplate; // <-- NEW: Redis injected

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public String name() {
        return "AuthenticationFilter";
    }

    @Override
    public GatewayFilter apply(AuthenticationFilter.Config config) {
        return ((exchange, chain) -> {
            System.out.println("🚨 BOUNCER INTERCEPTED: " + exchange.getRequest().getURI().getPath());

            if (exchange.getRequest().getURI().getPath().contains("/api/v1/products") ||
                    exchange.getRequest().getURI().getPath().contains("/api/v1/orders")) {

                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    System.out.println("❌ REJECTED: No Authorization Header Found!");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                String token = "";

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                } else {
                    System.out.println("❌ REJECTED: Header doesn't start with 'Bearer '");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                // --- NEW: REDIS BLACKLIST CHECK ---
                Boolean isBlacklisted = redisTemplate.hasKey("blacklist:" + token);
                if (Boolean.TRUE.equals(isBlacklisted)) {
                    System.out.println("❌ REJECTED: Token is Blacklisted (User Logged Out)!");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
                // ----------------------------------

                try {
                    jwtUtil.validateToken(token);
                    System.out.println("✅ PASSED: Token is Cryptographically Valid!");
                } catch (Exception e) {
                    System.out.println("❌ REJECTED: Token Validation Failed -> " + e.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }
            return chain.filter(exchange);
        });
    }

    // This static class MUST be at the bottom for Spring Cloud Gateway to work!
    public static class Config {
    }
}