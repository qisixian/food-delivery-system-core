package com.sky.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 地址簿
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressBook implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //用户id
    private Long userId;

    //收货人
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String consignee;

    //手机号
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    //性别 0 女 1 男
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String sex;

    //省级区划编号
    private String provinceCode;

    //省级名称
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String provinceName;

    //市级区划编号
    private String cityCode;

    //市级名称
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String cityName;

    //区级区划编号
    private String districtCode;

    //区级名称
    private String districtName;

    //详细地址
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String detail;

    //标签
    private String label;

    //是否默认 0否 1是
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer isDefault;
}
