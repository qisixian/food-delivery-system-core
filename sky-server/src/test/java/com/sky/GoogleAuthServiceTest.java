package com.sky;

import com.sky.dto.GoogleLoginResultDTO;
import com.sky.dto.GoogleTokenResponseDTO;
import com.sky.entity.User;
import com.sky.enumeration.ThirdPartyErrorType;
import com.sky.enumeration.ThirdPartyProvider;
import com.sky.exception.ThirdPartyServiceException;
import com.sky.properties.GoogleLoginProperties;
import com.sky.service.GoogleAuthService;
import com.sky.service.UserService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    private static final String AUTH_CODE = "auth-code";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String GOOGLE_OPEN_ID = "google-openid";

    private MockWebServer mockWebServer;
    private GoogleLoginProperties googleLoginProperties;
    private GoogleAuthService googleAuthService;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        googleLoginProperties = new GoogleLoginProperties();
        googleLoginProperties.setAuthUrl("https://accounts.google.com/o/oauth2/v2/auth");
        googleLoginProperties.setTokenUrl(mockWebServer.url("/token").toString());
        googleLoginProperties.setUserInfoUrl(mockWebServer.url("/oauth2/v2/userinfo").toString());
        googleLoginProperties.setClientId("client-id");
        googleLoginProperties.setClientSecret("client-secret");
        googleLoginProperties.setRedirectUri("http://localhost/callback");
        googleLoginProperties.setScope("openid email profile");
        googleLoginProperties.setFrontendCallbackUrl("http://localhost/frontend/callback");

        googleAuthService = new GoogleAuthService();
        ReflectionTestUtils.setField(googleAuthService, "userService", userService);
        ReflectionTestUtils.setField(googleAuthService, "webClient", WebClient.builder().build());
        ReflectionTestUtils.setField(googleAuthService, "googleLoginProperties", googleLoginProperties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    class BuildAuthorizationUrlTests {

        @Test
        void buildAuthorizationUrl_whenPropertiesAreConfigured_thenReturnsGoogleAuthorizationUrl() {
            UriComponents uri = UriComponentsBuilder
                    .fromUriString(googleAuthService.buildAuthorizationUrl())
                    .build();

            assertEquals("https", uri.getScheme());
            assertEquals("accounts.google.com", uri.getHost());
            assertEquals("/o/oauth2/v2/auth", uri.getPath());

            MultiValueMap<String, String> params = uri.getQueryParams();

            assertEquals("client-id", params.getFirst("client_id"));
            assertEquals("http://localhost/callback", params.getFirst("redirect_uri"));
            assertEquals("code", params.getFirst("response_type"));
        }
    }

    @Nested
    class ExchangeCodeForTokenTests {

        @Test
        void exchangeCodeForToken_whenGoogleReturnsAccessToken_thenReturnsTokenResponse() throws InterruptedException {
            mockWebServer.enqueue(jsonResponse(200, tokenJson(ACCESS_TOKEN)));

            GoogleTokenResponseDTO result = googleAuthService.exchangeCodeForToken(AUTH_CODE);

            assertEquals(ACCESS_TOKEN, result.getAccessToken());
            assertTokenRequest(takeRequest());
        }

        @Test
        void exchangeCodeForToken_whenGoogleReturns4xx_thenThrowsInvalidRequest() throws InterruptedException {
            mockWebServer.enqueue(jsonResponse(400, "{\"error\":\"invalid_grant\"}"));

            ThirdPartyServiceException exception = assertThrows(
                    ThirdPartyServiceException.class,
                    () -> googleAuthService.exchangeCodeForToken(AUTH_CODE)
            );

            assertGoogleException(exception, ThirdPartyErrorType.INVALID_REQUEST);
            assertTrue(exception.getMessage().contains("invalid_grant"));
            assertTokenRequest(takeRequest());
        }

        @Test
        void exchangeCodeForToken_whenGoogleReturns429_thenThrowsServiceUnavailableAfterRetries() {
            enqueueRepeated(jsonResponse(429, "{\"error\":\"rate_limit\"}"), 3);

            ThirdPartyServiceException exception = assertThrows(
                    ThirdPartyServiceException.class,
                    () -> googleAuthService.exchangeCodeForToken(AUTH_CODE)
            );

            assertGoogleException(exception, ThirdPartyErrorType.SERVICE_UNAVAILABLE);
            assertEquals(3, mockWebServer.getRequestCount());
        }

        @Test
        void exchangeCodeForToken_whenGoogleReturns5xx_thenThrowsServiceUnavailableAfterRetries() {
            enqueueRepeated(jsonResponse(500, "{\"error\":\"server_error\"}"), 3);

            ThirdPartyServiceException exception = assertThrows(
                    ThirdPartyServiceException.class,
                    () -> googleAuthService.exchangeCodeForToken(AUTH_CODE)
            );

            assertGoogleException(exception, ThirdPartyErrorType.SERVICE_UNAVAILABLE);
            assertEquals(3, mockWebServer.getRequestCount());
        }

        @Test
        void exchangeCodeForToken_whenGoogleResponseTimesOut_thenThrowsTimeout() {
            mockWebServer.enqueue(jsonResponse(200, tokenJson(ACCESS_TOKEN)).setBodyDelay(6, TimeUnit.SECONDS));

            ThirdPartyServiceException exception = assertThrows(
                    ThirdPartyServiceException.class,
                    () -> googleAuthService.exchangeCodeForToken(AUTH_CODE)
            );

            assertGoogleException(exception, ThirdPartyErrorType.TIMEOUT);
            assertTrue(exception.getMessage().contains("timed out"));
            assertEquals(1, mockWebServer.getRequestCount());
        }

        @Test
        void exchangeCodeForToken_whenResponseBodyHasNoAccessToken_thenThrowsInvalidResponse() {
            mockWebServer.enqueue(jsonResponse(200, "{\"expires_in\":3600,\"token_type\":\"Bearer\"}"));

            ThirdPartyServiceException exception = assertThrows(
                    ThirdPartyServiceException.class,
                    () -> googleAuthService.exchangeCodeForToken(AUTH_CODE)
            );

            assertGoogleException(exception, ThirdPartyErrorType.INVALID_RESPONSE);
            assertEquals(1, mockWebServer.getRequestCount());
        }

        @Test
        void exchangeCodeForToken_whenAccessTokenIsBlank_thenThrowsInvalidResponse() {
            mockWebServer.enqueue(jsonResponse(200, tokenJson("   ")));

            ThirdPartyServiceException exception = assertThrows(
                    ThirdPartyServiceException.class,
                    () -> googleAuthService.exchangeCodeForToken(AUTH_CODE)
            );

            assertGoogleException(exception, ThirdPartyErrorType.INVALID_RESPONSE);
            assertEquals(1, mockWebServer.getRequestCount());
        }
    }

    @Nested
    class GetOpenIdFromTokenTests {

        @Test
        void getOpenIdFromToken_whenGoogleReturnsUserId_thenReturnsOpenId() throws InterruptedException {
            mockWebServer.enqueue(jsonResponse(200, userInfoJson(GOOGLE_OPEN_ID)));

            String result = googleAuthService.getOpenIdFromToken(tokenResponse());

            assertEquals(GOOGLE_OPEN_ID, result);
            assertUserInfoRequest(takeRequest());
        }

        @Test
        void getOpenIdFromToken_whenGoogleReturns4xx_thenThrowsInvalidRequest() throws InterruptedException {
            mockWebServer.enqueue(jsonResponse(401, "{\"error\":\"invalid_token\"}"));

            ThirdPartyServiceException exception = assertThrows(
                    ThirdPartyServiceException.class,
                    () -> googleAuthService.getOpenIdFromToken(tokenResponse())
            );

            assertGoogleException(exception, ThirdPartyErrorType.INVALID_REQUEST);
            assertTrue(exception.getMessage().contains("invalid_token"));
            assertUserInfoRequest(takeRequest());
        }

        @Test
        void getOpenIdFromToken_whenGoogleReturns429_thenThrowsServiceUnavailableAfterRetries() {
            enqueueRepeated(jsonResponse(429, "{\"error\":\"rate_limit\"}"), 3);

            ThirdPartyServiceException exception = assertThrows(
                    ThirdPartyServiceException.class,
                    () -> googleAuthService.getOpenIdFromToken(tokenResponse())
            );

            assertGoogleException(exception, ThirdPartyErrorType.SERVICE_UNAVAILABLE);
            assertEquals(3, mockWebServer.getRequestCount());
        }

        @Test
        void getOpenIdFromToken_whenGoogleReturns5xx_thenThrowsServiceUnavailableAfterRetries() {
            enqueueRepeated(jsonResponse(500, "{\"error\":\"server_error\"}"), 3);

            ThirdPartyServiceException exception = assertThrows(
                    ThirdPartyServiceException.class,
                    () -> googleAuthService.getOpenIdFromToken(tokenResponse())
            );

            assertGoogleException(exception, ThirdPartyErrorType.SERVICE_UNAVAILABLE);
            assertEquals(3, mockWebServer.getRequestCount());
        }

        @Test
        void getOpenIdFromToken_whenGoogleResponseTimesOut_thenThrowsTimeout() {
            mockWebServer.enqueue(jsonResponse(200, userInfoJson(GOOGLE_OPEN_ID)).setBodyDelay(6, TimeUnit.SECONDS));

            ThirdPartyServiceException exception = assertThrows(
                    ThirdPartyServiceException.class,
                    () -> googleAuthService.getOpenIdFromToken(tokenResponse())
            );

            assertGoogleException(exception, ThirdPartyErrorType.TIMEOUT);
            assertTrue(exception.getMessage().contains("timed out"));
            assertEquals(1, mockWebServer.getRequestCount());
        }

        @Test
        void getOpenIdFromToken_whenResponseBodyHasNoId_thenThrowsInvalidResponse() {
            mockWebServer.enqueue(jsonResponse(200, "{\"email\":\"user@example.com\"}"));

            ThirdPartyServiceException exception = assertThrows(
                    ThirdPartyServiceException.class,
                    () -> googleAuthService.getOpenIdFromToken(tokenResponse())
            );

            assertGoogleException(exception, ThirdPartyErrorType.INVALID_RESPONSE);
            assertEquals(1, mockWebServer.getRequestCount());
        }

        @Test
        void getOpenIdFromToken_whenIdIsBlank_thenThrowsInvalidResponse() {
            mockWebServer.enqueue(jsonResponse(200, userInfoJson("   ")));

            ThirdPartyServiceException exception = assertThrows(
                    ThirdPartyServiceException.class,
                    () -> googleAuthService.getOpenIdFromToken(tokenResponse())
            );

            assertGoogleException(exception, ThirdPartyErrorType.INVALID_RESPONSE);
            assertEquals(1, mockWebServer.getRequestCount());
        }
    }

    @Nested
    class LoginWithAuthorizationCodeTests {

        @Test
        void loginWithAuthorizationCode_whenGoogleFlowSucceeds_thenReturnsUserIdAndSystemToken()
                throws InterruptedException {
            mockWebServer.enqueue(jsonResponse(200, tokenJson(ACCESS_TOKEN)));
            mockWebServer.enqueue(jsonResponse(200, userInfoJson(GOOGLE_OPEN_ID)));
            User user = User.builder().id(100L).openid(GOOGLE_OPEN_ID).build();
            when(userService.getOrCreateUser(GOOGLE_OPEN_ID)).thenReturn(user);
            when(userService.createToken(user)).thenReturn("system-jwt");

            GoogleLoginResultDTO result = googleAuthService.loginWithAuthorizationCode(AUTH_CODE);

            assertEquals(100L, result.getId());
            assertEquals("system-jwt", result.getToken());
            verify(userService).getOrCreateUser(GOOGLE_OPEN_ID);
            verify(userService).createToken(user);

            RecordedRequest tokenRequest = takeRequest();
            assertTokenRequest(tokenRequest);
            RecordedRequest userInfoRequest = takeRequest();
            assertUserInfoRequest(userInfoRequest);
        }

        @Test
        void loginWithAuthorizationCode_whenTokenExchangeFails_thenPropagatesThirdPartyServiceException() {
            mockWebServer.enqueue(jsonResponse(400, "{\"error\":\"invalid_grant\"}"));

            ThirdPartyServiceException exception = assertThrows(
                    ThirdPartyServiceException.class,
                    () -> googleAuthService.loginWithAuthorizationCode(AUTH_CODE)
            );

            assertGoogleException(exception, ThirdPartyErrorType.INVALID_REQUEST);
            verifyNoInteractions(userService);
            assertEquals(1, mockWebServer.getRequestCount());
        }

        @Test
        void loginWithAuthorizationCode_whenUserInfoLookupFails_thenPropagatesThirdPartyServiceException() {
            mockWebServer.enqueue(jsonResponse(200, tokenJson(ACCESS_TOKEN)));
            mockWebServer.enqueue(jsonResponse(401, "{\"error\":\"invalid_token\"}"));

            ThirdPartyServiceException exception = assertThrows(
                    ThirdPartyServiceException.class,
                    () -> googleAuthService.loginWithAuthorizationCode(AUTH_CODE)
            );

            assertGoogleException(exception, ThirdPartyErrorType.INVALID_REQUEST);
            verifyNoInteractions(userService);
            assertEquals(2, mockWebServer.getRequestCount());
        }
    }

    private MockResponse jsonResponse(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .setBody(body);
    }

    private void enqueueRepeated(MockResponse response, int count) {
        for (int i = 0; i < count; i++) {
            mockWebServer.enqueue(response.clone());
        }
    }

    private GoogleTokenResponseDTO tokenResponse() {
        GoogleTokenResponseDTO tokenResponse = new GoogleTokenResponseDTO();
        tokenResponse.setAccessToken(ACCESS_TOKEN);
        return tokenResponse;
    }

    private String tokenJson(String accessToken) {
        return """
                {
                  "access_token": "%s",
                  "expires_in": 3600,
                  "token_type": "Bearer"
                }
                """.formatted(accessToken);
    }

    private String userInfoJson(String openId) {
        return """
                {
                  "id": "%s"
                }
                """.formatted(openId);
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        return request;
    }

    private void assertTokenRequest(RecordedRequest request) {
        assertEquals("POST", request.getMethod());
        assertEquals("/token", request.getPath());

        String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
        assertNotNull(contentType);
        assertTrue(contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE));

        MultiValueMap<String, String> form = UriComponentsBuilder
                .fromUriString("http://localhost/?" + request.getBody().readUtf8())
                .build()
                .getQueryParams();

        assertEquals(AUTH_CODE, form.getFirst("code"));
        assertEquals(googleLoginProperties.getClientId(), form.getFirst("client_id"));
        assertEquals(googleLoginProperties.getClientSecret(), form.getFirst("client_secret"));
        assertEquals(googleLoginProperties.getRedirectUri(), form.getFirst("redirect_uri"));
        assertEquals("authorization_code", form.getFirst("grant_type"));
    }

    private void assertUserInfoRequest(RecordedRequest request) {
        assertEquals("GET", request.getMethod());
        assertEquals("/oauth2/v2/userinfo", request.getPath());
    }

    private void assertGoogleException(ThirdPartyServiceException exception, ThirdPartyErrorType errorType) {
        assertEquals(ThirdPartyProvider.GOOGLE, exception.getProvider());
        assertEquals(errorType, exception.getErrorType());
    }
}
