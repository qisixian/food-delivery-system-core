package com.sky.controller.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.LogFields;
import com.sky.dto.GoogleTokenResponseDTO;
import com.sky.entity.User;
import com.sky.properties.GoogleLoginProperties;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user/auth/google")
@Slf4j
public class GoogleAuthController {

    @Autowired
    UserService userService;

    @Autowired
    JwtProperties jwtProperties;

    @Autowired
    private GoogleLoginProperties googleLoginProperties;

    @GetMapping("login")
    public Result<String> googleLogin(HttpServletResponse response) throws IOException {

        String authorizationUrl = UriComponentsBuilder
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

        response.sendRedirect(authorizationUrl);

        return Result.success();
    }

    @GetMapping("callback")
    public Result<String> googleCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String code = request.getParameter("code");
//        log.debug("Google login callback，code: {}", code);
        // 使用 code 交换 access token
        GoogleTokenResponseDTO googleTokenResponseDTO = WebClient.create()
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

//        log.debug("Google Token Response: {}", googleTokenResponseDTO.toString());
//        log.debug("id_token: {}", googleTokenResponseDTO.getIdToken());

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
//        log.debug("googleOpenId: {}", googleOpenId);

        // 看是否要创建用户
        User user = userService.gerOrCreateUser(googleOpenId);

        log.atInfo().addKeyValue(LogFields.USER_ID, user.getId()).log("Google login success");

        // 换成自己系统的用户id，生成JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims
        );

        response.sendRedirect(googleLoginProperties.getFrontendCallbackUrl() + "?id=" + user.getId() + "&token=" + token);

        return Result.success();
    }
}
