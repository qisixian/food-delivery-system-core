package com.sky.service;

import com.sky.constant.LogFields;
import com.sky.dto.GoogleLoginResultDTO;
import com.sky.entity.User;
import com.sky.oauth.OAuthProvider;
import com.sky.oauth.OAuthUserInfo;
import com.sky.properties.CookieProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OAuthLoginService {

    @Autowired
    private UserService userService;

    public LoginResult oAuthlogin(OAuthProvider oAuthProvider, String code) {
        OAuthUserInfo oAuthUserInfo = oAuthProvider.authenticate(code);

        User user = userService.getOrCreateUser(oAuthUserInfo.providerUserId());

        log.atInfo().addKeyValue(LogFields.USER_ID, user.getId()).log("OAuth login success");

        // 换成自己系统的用户id，生成JWT
        String token = userService.createToken(user);

        return LoginResult.builder()
                .id(user.getId())
                .token(token)
                .build();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoginResult {
        private Long id;
        private String token;
    }
}
