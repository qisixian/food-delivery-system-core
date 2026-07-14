package com.sky.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sky.constant.LogFields;
import com.sky.dto.GoogleTokenResponseDTO;
import com.sky.entity.User;
import com.sky.properties.GoogleLoginProperties;
import com.sky.properties.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class GoogleAuthService {

    @Autowired
    UserService userService;

    @Autowired
    private GoogleLoginProperties googleLoginProperties;

    public String buildAuthorizationUrl(){
        return UriComponentsBuilder
                .fromUriString(googleLoginProperties.getAuthUrl())
                .queryParam("client_id", googleLoginProperties.getClientId())
                .queryParam("redirect_uri", googleLoginProperties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", googleLoginProperties.getScope())
//                .queryParam("state", state)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .toUriString();
    }

    public String handleCallback(String code) {
        GoogleTokenResponseDTO googleTokenResponseDTO = exchangeCodeForToken(code);
//        log.debug("Google Token Response: {}", googleTokenResponseDTO.toString());
//        log.debug("id_token: {}", googleTokenResponseDTO.getIdToken());

        String googleOpenId = getUserInfoFromToken(googleTokenResponseDTO);
//        log.debug("googleOpenId: {}", googleOpenId);

        User user = userService.getOrCreateUser(googleOpenId);

        log.atInfo().addKeyValue(LogFields.USER_ID, user.getId()).log("Google login success");

        // 换成自己系统的用户id，生成JWT
        String token = userService.createToken(user);

        String redirectUrl = googleLoginProperties.getFrontendCallbackUrl() + "?id=" + user.getId() + "&token=" + token;
        return redirectUrl;
    }

    public GoogleTokenResponseDTO exchangeCodeForToken(String code){
        return WebClient.create()
                .post()
                .uri(googleLoginProperties.getTokenUrl())
                .body(BodyInserters.fromFormData("code", code)
                        .with("client_id", googleLoginProperties.getClientId())
                        .with("client_secret", googleLoginProperties.getClientSecret())
                        .with("redirect_uri", googleLoginProperties.getRedirectUri())
                        .with("grant_type", "authorization_code"))
                .retrieve()
                .bodyToMono(GoogleTokenResponseDTO.class)
                .block();
    }

    public String getUserInfoFromToken(GoogleTokenResponseDTO googleTokenResponseDTO){
        // 使用 access token 获取用户信息
        // 但是其实 JWT解析 id_token 就能拿到用户信息了，不需要再发请求了
        // 更推荐直接不用这个请求
        JsonNode userInfo = WebClient.create()
                .get()
                .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                .headers(headers -> headers.setBearerAuth(googleTokenResponseDTO.getAccessToken()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        String googleOpenId = userInfo.get("id").asText();
        return googleOpenId;
    }

}
