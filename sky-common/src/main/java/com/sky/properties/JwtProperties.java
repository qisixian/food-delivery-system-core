package com.sky.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "sky.jwt")
@Getter
@Setter
@Validated
public class JwtProperties {

    /**
     * 管理端员工生成jwt令牌相关配置
     */
    @NotBlank
    private String adminSecretKey;
    @Positive
    private long adminTtl;
    @NotBlank
    private String adminTokenName;

    /**
     * 用户端用户生成jwt令牌相关配置
     */
    @NotBlank
    private String userSecretKey;
    @Positive
    private long userTtl;
    @NotBlank
    private String userTokenName;

}
