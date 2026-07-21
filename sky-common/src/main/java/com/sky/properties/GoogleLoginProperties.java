package com.sky.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "sky.oauth2.google")
@Getter
@Setter
@Validated
public class GoogleLoginProperties {

    @NotBlank
    @URL
    private String authUrl;

    @NotBlank
    @URL
    private String tokenUrl;

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    @NotBlank
    @URL
    private String redirectUri;

    @NotBlank
    private String scope;

    @NotBlank
    @URL
    private String frontendCallbackUrl;
}
