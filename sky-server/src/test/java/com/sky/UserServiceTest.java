package com.sky;

import com.sky.constant.JwtClaimsConstant;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.service.impl.UserServiceImpl;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String USER_SECRET_KEY = "12345678901234567890123456789012";
    private static final long USER_TTL = 3_600_000L;

    @Mock
    private UserMapper userMapper;

    private UserServiceImpl userService;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-07-22T10:15:30Z"), ZoneId.of("UTC"));

        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setUserSecretKey(USER_SECRET_KEY);
        jwtProperties.setUserTtl(USER_TTL);

        userService = new UserServiceImpl();
        ReflectionTestUtils.setField(userService, "userMapper", userMapper);
        ReflectionTestUtils.setField(userService, "jwtProperties", jwtProperties);
        ReflectionTestUtils.setField(userService, "clock", clock);
    }

    @Test
    void getOrCreateUser_whenUserExists_thenReturnsExistingUser() {
        String openid = "openid-001";
        User existingUser = User.builder()
                .id(1L)
                .openid(openid)
                .build();
        when(userMapper.getByOpenid(openid)).thenReturn(existingUser);

        User result = userService.getOrCreateUser(openid);

        assertSame(existingUser, result);
        verify(userMapper).getByOpenid(openid);
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void getOrCreateUser_whenUserDoesNotExist_thenCreatesAndReturnsUser() {
        String openid = "openid-new";
        LocalDateTime expectedCreateTime = LocalDateTime.now(clock);
        when(userMapper.getByOpenid(openid)).thenReturn(null);

        User result = userService.getOrCreateUser(openid);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).getByOpenid(openid);
        verify(userMapper).insert(userCaptor.capture());

        User insertedUser = userCaptor.getValue();
        assertSame(insertedUser, result);
        assertEquals(openid, result.getOpenid());
        assertEquals(expectedCreateTime, result.getCreateTime());
    }

    @Test
    void createToken_whenUserProvided_thenReturnsJwtContainingUserId() {
        User user = User.builder()
                .id(100L)
                .build();

        String token = userService.createToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims claims = JwtUtil.parseJWT(USER_SECRET_KEY, token);
        assertEquals(100L, ((Number) claims.get(JwtClaimsConstant.USER_ID)).longValue());
        assertNotNull(claims.getExpiration());
    }
}
