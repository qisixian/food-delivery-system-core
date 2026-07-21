package com.sky.controller.user;

import com.sky.constant.LogFields;
import com.sky.dto.GoogleLoginResultDTO;
import com.sky.enumeration.ThirdPartyErrorType;
import com.sky.enumeration.ThirdPartyProvider;
import com.sky.exception.ThirdPartyServiceException;
import com.sky.properties.GoogleLoginProperties;
import com.sky.service.GoogleAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/user/auth/google")
@Slf4j
public class GoogleAuthController {

    @Autowired
    GoogleAuthService googleAuthService;

    @Autowired
    GoogleLoginProperties googleLoginProperties;

    @GetMapping("login")
    public ResponseEntity<Void> googleLogin() {
        return redirect(URI.create(googleAuthService.buildAuthorizationUrl()));
    }

    @GetMapping("callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {
        // If the user approves the access request, then the response contains an authorization code.
        // If the user does not approve the request, the response contains an error message.
        // http://localhost:8080/user/auth/google/callback?code=4/P7q7W91a-oMsCeLvIaQm6bTrgtp7
        // http://localhost:8080/user/auth/google/callback?error=access_denied
        try {
            if (StringUtils.hasText(error)) {
                throw new ThirdPartyServiceException("Google authorization failed: " + error,
                        ThirdPartyProvider.GOOGLE, ThirdPartyErrorType.BUSINESS_REJECTED);
            }
            if (!StringUtils.hasText(code)) {
                throw new ThirdPartyServiceException("Google authorization code is required",
                        ThirdPartyProvider.GOOGLE, ThirdPartyErrorType.INVALID_REQUEST);
            }
            GoogleLoginResultDTO resultDTO = googleAuthService.loginWithAuthorizationCode(code);

            return redirectWithLoginSuccess(resultDTO);
        } catch (ThirdPartyServiceException ex) {
            log.atInfo().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName())
                    .addKeyValue(LogFields.THIRD_PARTY_ERROR_TYPE, ex.getErrorType()).log(ex.getMessage());
            return redirectWithLoginError(ex.getMessage());
        } catch (Exception ex) {
            log.atError().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName()).setCause(ex)
                    .log("Unexpected Google callback exception");
            return redirectWithLoginError("Unexpected Google login exception");
        }
    }

    private ResponseEntity<Void> redirectWithLoginSuccess(GoogleLoginResultDTO result) {
        URI redirectUri = UriComponentsBuilder
                .fromUriString(googleLoginProperties.getFrontendCallbackUrl())
                .queryParam("id", result.getId())
                .queryParam("token", result.getToken())
                .build()
                .encode()
                .toUri();

        return redirect(redirectUri);
    }

    private ResponseEntity<Void> redirectWithLoginError(String errorMessage) {
        URI redirectUri = UriComponentsBuilder
                .fromUriString(googleLoginProperties.getFrontendCallbackUrl())
                .queryParam("error", errorMessage)
                .build()
                .encode()
                .toUri();

        return redirect(redirectUri);
    }

    private ResponseEntity<Void> redirect(URI uri) {
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(uri)
                .build();
    }
}
