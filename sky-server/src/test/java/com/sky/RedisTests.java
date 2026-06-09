package com.sky;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class RedisTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Disabled("Only run manually")
    @Test
    public void testRedisConnection() {
        redisTemplate.opsForValue().set("testKey", "testValue");
        String value = (String) redisTemplate.opsForValue().get("testKey");
        System.out.println("value = " + value);
        assert "testValue".equals(value);
    }

//    @Test
//    public void testFailure() {
//        assert "testValue".equals("wrongValue");
//    }


    @Disabled("Only run manually")
    @Test
    public void testChineseValue() {
        String value = "中文测试";
        redisTemplate.opsForValue().set("testKey", value);
        String redisValue = (String) redisTemplate.opsForValue().get("testKey");
        System.out.println("value = " + redisValue);
        assert value.equals(redisValue);
    }
}
