package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Slf4j
public class ShopController {

    @Autowired
    RedisTemplate redisTemplate;

    @GetMapping("/status")
    @Schema(description = "获取店铺营业状态")
    public Result getStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get("shop_status");
        log.trace("获取店铺营业状态为: {}", status);
        return Result.success(status);
    }
}
