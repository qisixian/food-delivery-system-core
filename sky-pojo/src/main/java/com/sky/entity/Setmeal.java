package com.sky.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Setmeal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    //分类id
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;

    //套餐名称
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    //套餐价格
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    //状态 0:停用 1:启用
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    //描述信息
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    //图片
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String image;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createUser;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long updateUser;
}
