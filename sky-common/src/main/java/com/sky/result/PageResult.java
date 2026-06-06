package com.sky.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 封装分页查询结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> implements Serializable {


    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private long total; //总记录数

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<T> records; //当前页数据集合

}
