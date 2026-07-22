package com.sky.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sky.constant.LogFields;
import com.sky.dto.GoogleLoginResultDTO;
import com.sky.dto.GoogleTokenResponseDTO;
import com.sky.entity.User;
import com.sky.enumeration.ThirdPartyErrorType;
import com.sky.enumeration.ThirdPartyProvider;
import com.sky.exception.ThirdPartyServiceException;
import com.sky.properties.GoogleLoginProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class GoogleAuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private WebClient webClient;

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

    public GoogleLoginResultDTO loginWithAuthorizationCode(String code) {
        GoogleTokenResponseDTO googleTokenResponseDTO = exchangeCodeForToken(code);

        String googleOpenId = getOpenIdFromToken(googleTokenResponseDTO);

        User user = userService.getOrCreateUser(googleOpenId);

        log.atInfo().addKeyValue(LogFields.USER_ID, user.getId()).log("Google login success");

        // 换成自己系统的用户id，生成JWT
        String token = userService.createToken(user);

        return GoogleLoginResultDTO.builder()
                .id(user.getId())
                .token(token)
                .build();
    }

    public GoogleTokenResponseDTO exchangeCodeForToken(String code){
        // HTTP调用外部系统怎么做异常处理，这里接口的异常是怎么定义的
        // WebClientResponseException$BadRequest: 400 Bad Request from POST https://oauth2.googleapis.com/token
        GoogleTokenResponseDTO tokenResponse = webClient
                .post()
                .uri(googleLoginProperties.getTokenUrl())
                .body(BodyInserters.fromFormData("code", code)
                        .with("client_id", googleLoginProperties.getClientId())
                        .with("client_secret", googleLoginProperties.getClientSecret())
                        .with("redirect_uri", googleLoginProperties.getRedirectUri())
                        .with("grant_type", "authorization_code"))
                .retrieve()
                .onStatus(
                        status -> status.value() == 429 || status.is5xxServerError(),
                        response -> Mono.just(
                                new ThirdPartyServiceException(
                                        "Google OAuth service is unavailable, status code: " + response.statusCode(),
                                        ThirdPartyProvider.GOOGLE, ThirdPartyErrorType.SERVICE_UNAVAILABLE
                                )
                        )
                )
                .onStatus(
                        status -> status.is4xxClientError(),
                        response -> response.bodyToMono(JsonNode.class)
                                .defaultIfEmpty(JsonNodeFactory.instance.objectNode())
                                .map(body -> {
                                    String error = body.path("error").asText();
                                    return new ThirdPartyServiceException(
                                            "Google rejected authorization code: " + error,
                                            ThirdPartyProvider.GOOGLE, ThirdPartyErrorType.INVALID_REQUEST
                                    );
                                })
                )
                .bodyToMono(GoogleTokenResponseDTO.class)
                .timeout(Duration.ofSeconds(5))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200))
                        .filter(throwable ->
                                throwable instanceof ThirdPartyServiceException exception
                                        && exception.getErrorType() == ThirdPartyErrorType.SERVICE_UNAVAILABLE
                        )
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .onErrorMap(TimeoutException.class, ex -> new ThirdPartyServiceException(
                        "Google OAuth token request timed out",
                        ThirdPartyProvider.GOOGLE, ThirdPartyErrorType.TIMEOUT
                ))
                .block();
        if(tokenResponse == null || !StringUtils.hasText(tokenResponse.getAccessToken())) {
            throw new ThirdPartyServiceException("Google token response does not contain access token",
                    ThirdPartyProvider.GOOGLE, ThirdPartyErrorType.INVALID_RESPONSE);
        }
        return tokenResponse;
    }

    public String getOpenIdFromToken(GoogleTokenResponseDTO googleTokenResponseDTO){
        // 使用 access token 获取用户信息
        // 但是其实 JWT解析 id_token 就能拿到用户信息了，不需要再发请求了
        // 更推荐直接不用这个请求
        JsonNode userInfo = webClient
                .get()
                .uri(googleLoginProperties.getUserInfoUrl())
                .headers(headers -> headers.setBearerAuth(googleTokenResponseDTO.getAccessToken()))
                .retrieve()
                .onStatus(
                        status -> status.value() == 429 || status.is5xxServerError(),
                        response -> Mono.just(
                                new ThirdPartyServiceException(
                                        "Google get userinfo service is unavailable, status code: " + response.statusCode(),
                                        ThirdPartyProvider.GOOGLE, ThirdPartyErrorType.SERVICE_UNAVAILABLE
                                )
                        )
                )
                .onStatus(
                        status -> status.is4xxClientError(),
                        response -> response.bodyToMono(JsonNode.class)
                                .defaultIfEmpty(JsonNodeFactory.instance.objectNode())
                                .map(body -> {
                                    String error = body.path("error").asText();
                                    return new ThirdPartyServiceException(
                                            "Google rejected access token: " + error,
                                            ThirdPartyProvider.GOOGLE, ThirdPartyErrorType.INVALID_REQUEST
                                    );
                                })
                )
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(5))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200))
                        .filter(throwable ->
                                throwable instanceof ThirdPartyServiceException exception
                                        && exception.getErrorType() == ThirdPartyErrorType.SERVICE_UNAVAILABLE
                        )
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .onErrorMap(TimeoutException.class, ex -> new ThirdPartyServiceException(
                        "Google userinfo request timed out",
                        ThirdPartyProvider.GOOGLE, ThirdPartyErrorType.TIMEOUT
                ))
                .block();
        if(userInfo == null || !userInfo.has("id") || !StringUtils.hasText(userInfo.get("id").asText())) {
            throw new ThirdPartyServiceException("Google userinfo response does not contain user openid",
                    ThirdPartyProvider.GOOGLE, ThirdPartyErrorType.INVALID_RESPONSE);
        }
        return userInfo.get("id").asText();
    }

}
