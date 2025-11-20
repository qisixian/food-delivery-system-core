package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.oauth2.google")
@Data
public class GoogleLoginProperties {
    private String authUrl;
    private String tokenUrl;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scope;
}
