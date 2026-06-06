package com.sky.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "员工登录返回的数据格式")
public class EmployeeLoginVO implements Serializable {

    @Schema(description = "主键值")
    @NotNull
    private Long id;

    @Schema(description = "用户名")
    @NotNull
    private String userName;

    @Schema(description = "姓名")
    @NotNull
    private String name;

    @Schema(description = "jwt令牌")
    @NotNull
    private String token;

}
