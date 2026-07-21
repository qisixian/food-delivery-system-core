package com.sky;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest(properties = {
        "sky.oauth2.google.client-id=test-client-id",
        "sky.oauth2.google.client-secret=test-client-secret"
})
class RedisTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Disabled("Only run manually")
    @Test
    void testRedisConnection() {
        redisTemplate.opsForValue().set("testKey", "testValue");
        String value = (String) redisTemplate.opsForValue().get("testKey");
        System.out.println("value = " + value);
        assert "testValue".equals(value);
    }

    @Disabled("Only run manually")
    @Test
    void testChineseValue() {
        String value = "中文测试";
        redisTemplate.opsForValue().set("testKey", value);
        String redisValue = (String) redisTemplate.opsForValue().get("testKey");
        System.out.println("value = " + redisValue);
        assert value.equals(redisValue);
    }
}
