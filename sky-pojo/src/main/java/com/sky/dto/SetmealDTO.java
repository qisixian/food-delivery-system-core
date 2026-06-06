package com.sky.dto;

import com.sky.entity.SetmealDish;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class SetmealDTO implements Serializable {

    private Long id;

    //分类id
    @NotNull
    private Long categoryId;

    //套餐名称
    @NotNull
    private String name;

    //套餐价格
    @NotNull
    private BigDecimal price;

    //状态 0:停用 1:启用
    @NotNull
    private Integer status;

    //描述信息
    @NotNull
    private String description;

    //图片
    @NotNull
    private String image;

    //套餐菜品关系
    @NotNull
    private List<SetmealDish> setmealDishes = new ArrayList<>();

}
