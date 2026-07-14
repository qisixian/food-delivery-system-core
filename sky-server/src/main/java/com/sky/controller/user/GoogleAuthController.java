package com.sky.controller.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.LogFields;
import com.sky.dto.GoogleTokenResponseDTO;
import com.sky.entity.User;
import com.sky.properties.GoogleLoginProperties;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.GoogleAuthService;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    GoogleAuthService googleAuthService;

    @GetMapping("login")
    public Result<String> googleLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect(googleAuthService.buildAuthorizationUrl());
        return Result.success();
    }

    @GetMapping("callback")
    public Result<String> googleCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String code = request.getParameter("code");
//        log.debug("Google login callback，code: {}", code);
        String redirectUrl = googleAuthService.handleCallback(code);
        response.sendRedirect(redirectUrl);
        return Result.success();
    }
}
