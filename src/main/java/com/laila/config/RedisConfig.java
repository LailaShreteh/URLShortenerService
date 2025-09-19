package com.laila.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            org.springframework.core.env.Environment env
    ) {
        String host = env.getProperty("spring.data.redis.host", "localhost");
        int port = Integer.parseInt(env.getProperty("spring.data.redis.port", "6379"));
        String password = env.getProperty("spring.data.redis.password", "");

        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration(host, port);
        if (!password.isBlank()) {
            conf.setPassword(RedisPassword.of(password));
        }
        return new LettuceConnectionFactory(conf);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

    @Bean
    public CacheManager cacheManager(LettuceConnectionFactory cf) {
        var config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24))       // default TTL for annotated caches
                .disableCachingNullValues();
        return RedisCacheManager.builder(cf).cacheDefaults(config).build();
    }
}