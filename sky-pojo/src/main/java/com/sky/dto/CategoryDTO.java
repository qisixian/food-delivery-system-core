package com.sky.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class CategoryDTO implements Serializable {

    //主键
    private Long id;

    //类型 1 菜品分类 2 套餐分类
    @NotNull
    private Integer type;

    //分类名称
    @NotNull
    private String name;

    //排序
    @NotNull
    private Integer sort;

}
