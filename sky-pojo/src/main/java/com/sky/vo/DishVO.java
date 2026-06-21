package com.sky.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sky.entity.DishFlavor;
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

// todo: change to record?
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishVO implements Serializable {

    @NotNull
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
    //更新时间
    // TODO 用统一的消息转换器？converter？
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    //分类名称
    @NotNull
    private String categoryName;
    //菜品关联的口味
    @NotNull
    private List<DishFlavor> flavors = new ArrayList<>();

    //private Integer copies;
}
