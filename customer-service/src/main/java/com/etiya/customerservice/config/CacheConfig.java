package com.etiya.customerservice.config;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * Enables the Spring cache abstraction backed by Redis (see spring.cache.* in application.yml).
 * Cache values are serialized as JSON (Jackson, java.time support built in) instead of JDK
 * serialization, so response DTOs don't need to implement Serializable.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        // Default typing embeds the concrete DTO class in the JSON, so a cache hit deserializes
        // back to the original response type ("unsafe" is fine: we only ever read our own writes).
        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
                .enableSpringCacheNullValueSupport()
                .build();
        return builder -> builder.cacheDefaults(
                RedisCacheConfiguration.defaultCacheConfig()
                        .serializeValuesWith(RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer)));
    }
}
