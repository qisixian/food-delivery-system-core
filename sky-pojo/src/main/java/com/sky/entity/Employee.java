package com.sky.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    // TODO：把这个注释放在respVo里而不是实体类里；并且改成用@NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String sex;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String idNumber;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    // TODO 这个反序列化注释最好放在respVo里而不是实体类里；用统一的消息转换器？converter？
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    // TODO 这个反序列化注释最好放在respVo里而不是实体类里；用统一的消息转换器？converter？
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createUser;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long updateUser;

}
