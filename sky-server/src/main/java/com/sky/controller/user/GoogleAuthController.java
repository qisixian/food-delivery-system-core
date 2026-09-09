package com.sky.controller.user;

import com.sky.constant.LogFields;
import com.sky.enumeration.ThirdPartyErrorType;
import com.sky.enumeration.ThirdPartyProvider;
import com.sky.exception.ThirdPartyServiceException;
import com.sky.oauth.GoogleOAuthProvider;
import com.sky.properties.CookieProperties;
import com.sky.properties.GoogleLoginProperties;
import com.sky.service.OAuthLoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;

@RestController
@RequestMapping("/user/auth/google")
@Slf4j
public class GoogleAuthController {

    @Autowired
    GoogleOAuthProvider googleOAuthProvider;

    @Autowired
    OAuthLoginService oAuthLoginService;

    @Autowired
    GoogleLoginProperties googleLoginProperties;

    @Autowired
    private CookieProperties cookieProperties;

    @GetMapping("login")
    public ResponseEntity<Void> googleLogin() {

        String state = generateState();

        URI uri = URI.create(googleOAuthProvider.buildAuthorizationUrl(state));

        ResponseCookie cookie = ResponseCookie.from("google_oauth_state", state)
                .httpOnly(true)
                .secure(cookieProperties.isSecure()) // 本地 HTTP 调试时设为 false
                .sameSite("Lax")
                .path("/user/auth/google")
                .maxAge(300)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());

        return redirect(uri, headers);
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateState() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    @GetMapping("callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @CookieValue(name = "google_oauth_state", required = false) String cookieState) {
        // If the user approves the access request, then the response contains an authorization code.
        // If the user does not approve the request, the response contains an error message.
        // http://localhost:8080/user/auth/google/callback?code=4/P7q7W91a-oMsCeLvIaQm6bTrgtp7
        // http://localhost:8080/user/auth/google/callback?error=access_denied
        boolean stateValidated = false;
        try {
            if (!StringUtils.hasText(cookieState) || !StringUtils.hasText(state) || !cookieState.equals(state)) {
                throw new ThirdPartyServiceException("Invalid OAuth state",
                        ThirdPartyProvider.GOOGLE, ThirdPartyErrorType.INVALID_REQUEST);
            }
            stateValidated = true;
            if (StringUtils.hasText(error)) {
                throw new ThirdPartyServiceException("Google authorization failed: " + error,
                        ThirdPartyProvider.GOOGLE, ThirdPartyErrorType.BUSINESS_REJECTED);
            }
            if (!StringUtils.hasText(code)) {
                throw new ThirdPartyServiceException("Google authorization code is required",
                        ThirdPartyProvider.GOOGLE, ThirdPartyErrorType.INVALID_REQUEST);
            }

            OAuthLoginService.LoginResult result = oAuthLoginService.oAuthlogin(googleOAuthProvider, code);

            return redirectWithLoginSuccess(result);
        } catch (ThirdPartyServiceException ex) {
            log.atInfo().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName())
                    .addKeyValue(LogFields.THIRD_PARTY_ERROR_TYPE, ex.getErrorType()).log(ex.getMessage());
            return redirectWithLoginError(ex.getMessage(), stateValidated);
        } catch (Exception ex) {
            log.atError().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName()).setCause(ex)
                    .log("Unexpected Google callback exception");
            return redirectWithLoginError("Unexpected Google login exception", stateValidated);
        }
    }

    private ResponseEntity<Void> redirectWithLoginSuccess(OAuthLoginService.LoginResult result) {
        URI redirectUri = UriComponentsBuilder
                .fromUriString(googleLoginProperties.getFrontendCallbackUrl())
                .queryParam("id", result.getId())
                .queryParam("token", result.getToken())
                .build()
                .encode()
                .toUri();

        return redirectAndCleanCookie(redirectUri);
    }

    private ResponseEntity<Void> redirectWithLoginError(String errorMessage, boolean cleanCookie) {
        URI redirectUri = UriComponentsBuilder
                .fromUriString(googleLoginProperties.getFrontendCallbackUrl())
                .queryParam("error", errorMessage)
                .build()
                .encode()
                .toUri();
        if (cleanCookie) {
            return redirectAndCleanCookie(redirectUri);
        }else {
            return redirect(redirectUri);
        }
    }

    private ResponseEntity<Void> redirect(URI uri) {
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(uri)
                .build();
    }

    private ResponseEntity<Void> redirect(URI uri, HttpHeaders headers) {
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(uri)
                .headers(headers)
                .build();
    }

    private ResponseEntity<Void> redirectAndCleanCookie(URI uri) {
        ResponseCookie expiredCookie = ResponseCookie
                .from("google_oauth_state", "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite("Lax")
                .path("/user/auth/google")
                .maxAge(0)
                .build();

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(uri)
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }
}
