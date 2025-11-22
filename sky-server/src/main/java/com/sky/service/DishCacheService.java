package com.sky.service;

import com.sky.constant.CacheConstant;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class DishCacheService {

    @CacheEvict(cacheNames = CacheConstant.DISH_CATEGORY_LIST, key = "#categoryId")
    public void evictDishCategoryCache(Long categoryId) {
    }

}
