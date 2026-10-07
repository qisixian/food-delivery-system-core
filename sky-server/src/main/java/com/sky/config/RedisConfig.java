package com.sky.config;

import com.sky.constant.CacheConstant;
import com.sky.constant.LogFields;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientOptionsBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
public class RedisConfig implements CachingConfigurer {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        // 默认用的是JdkSerializationRedisSerializer，会导致乱码问题
        // 默认序列化存入的是16进制？不用String的序列号，就会乱码？
        template.setKeySerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public LettuceClientOptionsBuilderCustomizer lettuceClientOptionsCustomizer() {
        return builder -> builder
                .autoReconnect(true)
                .disconnectedBehavior(
                        ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {

            @Override
            public void handleCacheGetError(
                    RuntimeException ex, Cache cache, Object key) {
                handleConnectionFailure(ex, cache, key, "GET");
            }

            @Override
            public void handleCachePutError(
                    RuntimeException ex, Cache cache,
                    Object key, Object value) {
                handleConnectionFailure(ex, cache, key, "PUT");
            }
        };
    }

    private void handleConnectionFailure(
            RuntimeException ex, Cache cache, Object key, String operation) {

        // 所有缓存均允许在 Redis 连接失败时降级
        boolean canFallback = ex instanceof RedisConnectionFailureException
                || ex instanceof QueryTimeoutException
                || isDisconnectedCommandRejection(ex);

        if (!canFallback) {
            throw ex;
        }

        log.atWarn()
                .addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName())
                .addKeyValue("event", "cache_degraded")
                .addKeyValue("operation", operation)
                .addKeyValue("cache", cache.getName())
                .addKeyValue("key", key)
                .setCause(ex)
                .log("Redis unavailable; continuing without cache");
    }

    private boolean isDisconnectedCommandRejection(RuntimeException ex) {
        if (!(ex instanceof RedisSystemException redisEx)) {
            return false;
        }

        Throwable cause = redisEx.getMostSpecificCause();
        return cause.getClass() == RedisException.class
                && "Currently not connected. Commands are rejected."
                .equals(cause.getMessage());
    }
}
