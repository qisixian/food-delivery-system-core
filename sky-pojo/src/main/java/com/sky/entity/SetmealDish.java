package com.sky.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 套餐菜品关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetmealDish implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //套餐id
    private Long setmealId;

    //菜品id
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dishId;

    //菜品名称 （冗余字段）
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    //菜品原价
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    //份数
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer copies;
}
