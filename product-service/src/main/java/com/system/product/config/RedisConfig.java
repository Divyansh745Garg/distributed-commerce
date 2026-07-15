package com.system.product.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // 1. Define the Global Default Configuration (e.g., 10 minutes)
        // This applies to any @Cacheable annotation where the name isn't explicitly defined below
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // 2. Define Specific Configurations per Cache Name
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Override the TTL for the "products" cache to 1 hour
        cacheConfigurations.put("products", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Example: If you add an "inventory" cache later, you could make it 5 minutes
        // cacheConfigurations.put("inventory", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // 3. Build and return the CacheManager
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}