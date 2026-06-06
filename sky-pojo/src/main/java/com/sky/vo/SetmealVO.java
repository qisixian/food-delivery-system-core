package com.sky.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sky.entity.SetmealDish;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetmealVO implements Serializable {

    @NotNull
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

    //更新时间
    @NotNull
    // TODO 用统一的消息转换器？converter？
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    //分类名称
    @NotNull
    private String categoryName;

    //套餐和菜品的关联关系
    @NotNull
    private List<SetmealDish> setmealDishes = new ArrayList<>();
}
