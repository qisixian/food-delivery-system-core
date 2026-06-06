package com.sky.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 菜品口味
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishFlavor implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
    //菜品id
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dishId;

    //口味名称
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    //口味数据list
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String value;

}
