package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void saveWithFlavor(DishDTO dishDTO);

    void deleteBatch(List<Long> ids);

    DishVO getByIdWithFlavor(Long id);

    void updateWithFlavor(DishDTO dishDTO);

    void changeStatus(Integer status, Long id);

    List<Dish> listByCategoryId(Long categoryId);

    List<DishVO> listWithFlavor(Long categoryId);
}
