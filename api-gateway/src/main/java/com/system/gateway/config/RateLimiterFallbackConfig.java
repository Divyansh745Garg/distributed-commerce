package com.system.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RateLimiterFallbackConfig {

    @Bean
    @Primary
    public RateLimiter<RedisRateLimiter.Config> routeAwareRateLimiter(RedisRateLimiter defaultLimiter) {
        return new RateLimiter<RedisRateLimiter.Config>() {

            @Override
            public Mono<RateLimiter.Response> isAllowed(String routeId, String id) {
                return defaultLimiter.isAllowed(routeId, id)
                        .onErrorResume(throwable -> {
                            // 🔒 HIGH-RISK / FINANCIAL ROUTES: Fail-Closed (CP Strategy)
                            if ("auth-service".equalsIgnoreCase(routeId) || "order-service".equalsIgnoreCase(routeId)) {
                                System.err.println("🚨 REDIS DOWN - BLOCKING SENSITIVE ROUTE [" + routeId + "]! Shielding Auth/Orders (Fail-Closed). Error: " + throwable.getMessage());

                                // Return allowed = false (Requests blocked to protect system state)
                                return Mono.just(new RateLimiter.Response(false, new HashMap<>()));
                            }

                            // 🔓 LOW-RISK / READ-ONLY ROUTES: Fail-Open (AP Strategy)
                            System.err.println("🚨 REDIS DOWN - FAILING OPEN FOR READ ROUTE [" + routeId + "]! Preserving user experience. Error: " + throwable.getMessage());

                            // Return allowed = true (Traffic bypasses rate limiter to maintain uptime)
                            return Mono.just(new RateLimiter.Response(true, new HashMap<>()));
                        });
            }

            // --- Delegate all standard configuration methods to the inner Redis limiter ---

            @Override
            public Map<String, RedisRateLimiter.Config> getConfig() {
                return defaultLimiter.getConfig();
            }

            @Override
            public Class<RedisRateLimiter.Config> getConfigClass() {
                return defaultLimiter.getConfigClass();
            }

            @Override
            public RedisRateLimiter.Config newConfig() {
                return defaultLimiter.newConfig();
            }
        };
    }
}