package com.sky.service;

import com.sky.constant.CacheConstant;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class SetmealCacheService {

    @CacheEvict(cacheNames = CacheConstant.SETMEAL_CATEGORY_LIST, key = "#categoryId")
    public void evictSetmealCategoryCache(Long categoryId) {
    }
}
