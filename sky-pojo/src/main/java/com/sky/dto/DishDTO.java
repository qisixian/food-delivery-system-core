package com.sky.dto;

import com.sky.entity.DishFlavor;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class DishDTO implements Serializable {

    private Long id;
    //菜品名称
    @NotNull
    private String name;
    //菜品分类id
    @NotNull
    private Long categoryId;
    //菜品价格
    @NotNull
    private BigDecimal price;
    //图片
    @NotNull
    private String image;
    //描述信息
    @NotNull
    private String description;
    //0 停售 1 起售
    @NotNull
    private Integer status;
    //口味
    @NotNull
    private List<DishFlavor> flavors = new ArrayList<>();

}
