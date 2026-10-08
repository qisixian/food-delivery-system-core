package com.sky.config;

import com.sky.constant.CacheConstant;
import com.sky.constant.LogFields;
import com.sky.utils.LogRateLimiter;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import java.time.Duration;
import java.util.Map;

@Slf4j
@Configuration
public class RedisConfig implements CachingConfigurer {


    private final Duration logRateLimitInterval;
    private final Map<CacheFailureType, LogRateLimiter> failureLimiters;

    public RedisConfig(
            @Value("${sky.logging.rate-limit.interval}")
            Duration logRateLimitInterval) {

        this.logRateLimitInterval = logRateLimitInterval;

        this.failureLimiters = Map.of(
                CacheFailureType.CONNECTION_UNAVAILABLE,
                new LogRateLimiter(logRateLimitInterval),
                CacheFailureType.COMMAND_TIMEOUT,
                new LogRateLimiter(logRateLimitInterval)
        );
    }

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

    private enum CacheFailureType {
        CONNECTION_UNAVAILABLE,
        COMMAND_TIMEOUT,
        UNKNOWN_FAILURE
    }

    private CacheFailureType classifyFailure(RuntimeException ex) {
        if (ex instanceof RedisConnectionFailureException
                || isDisconnectedCommandRejection(ex)) {
            return CacheFailureType.CONNECTION_UNAVAILABLE;
        }

        if (ex instanceof QueryTimeoutException) {
            return CacheFailureType.COMMAND_TIMEOUT;
        }

        return CacheFailureType.UNKNOWN_FAILURE;
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

        CacheFailureType type = classifyFailure(ex);

        if (type == CacheFailureType.UNKNOWN_FAILURE) {
            throw ex;
        }

        LogRateLimiter.Decision decision = failureLimiters.get(type).tryAcquire();

        if (decision.allowed()) {
            log.atWarn()
                    .addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName())
                    .addKeyValue("event", "cache_degraded")
                    .addKeyValue("operation", operation)
                    .addKeyValue("cache", cache.getName())
                    .addKeyValue("key", key)
                    .addKeyValue("suppressed_count", decision.suppressedCount())
                    .addKeyValue("log_interval_seconds", logRateLimitInterval.toSeconds())
                    .setCause(ex)
                    .log("Redis unavailable; continuing without cache");
        }
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
