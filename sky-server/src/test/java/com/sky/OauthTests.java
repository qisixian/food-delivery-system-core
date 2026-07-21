package com.sky;

import com.sky.properties.GoogleLoginProperties;
import com.sky.utils.HttpClientUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class OauthTests {

    @Autowired
    private GoogleLoginProperties googleLoginProperties;

    @Disabled("Only run manually")
    @Test
    void googleOauthTest() {

        // 调用 google 登录接口，获得用户openId
        // 这里应该是重定向到 googleLoginProperties.getAuthUrl()，不是直接调用
        Map<String, String> map = new HashMap<>();
        map.put("client_id", googleLoginProperties.getClientId());
        map.put("client_secret", googleLoginProperties.getClientSecret());
        String json = HttpClientUtil.doGet(googleLoginProperties.getAuthUrl(), map);
        System.out.println(json);

    }
}
