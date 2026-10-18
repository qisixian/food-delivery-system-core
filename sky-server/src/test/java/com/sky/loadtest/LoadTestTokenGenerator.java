package com.sky.loadtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.entity.User;
import com.sky.properties.JwtProperties;
import com.sky.service.impl.UserServiceImpl;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 在 IDEA 中直接运行 main()，工作目录设为项目根目录。
 */
public final class LoadTestTokenGenerator {

    // 修改这里即可，不需要 Program arguments。
    private static final String PROFILE = "dev";
    private static final String DATABASE = "sky_take_out_loadtest";
    private static final long TOKEN_TTL_MILLIS = Duration.ofHours(8).toMillis();
    private static final Path OUTPUT = Path.of("performance/k6/data/loadtest-users.json");

    private static final String ACCOUNT_SQL = """
            SELECT id AS user_id
            FROM `user`
            WHERE openid LIKE 'loadtest_%'
            ORDER BY openid;
            """;

    public static void main(String[] args) throws Exception {
        // 只加载配置，数据库账号、密码和 JWT 密钥沿用项目配置。
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "loadtest", Map.of("spring.profiles.active", PROFILE)));
        ConfigDataEnvironmentPostProcessor.applyTo(environment);

        JwtProperties jwt = new JwtProperties();
        jwt.setUserSecretKey(environment.getRequiredProperty("sky.jwt.user-secret-key"));
        jwt.setUserTokenName(environment.getRequiredProperty("sky.jwt.user-token-name"));
        jwt.setUserTtl(TOKEN_TTL_MILLIS);

        UserServiceImpl userService = new UserServiceImpl();
        ReflectionTestUtils.setField(userService, "jwtProperties", jwt);

        String jdbcUrl = "jdbc:mysql://%s:%s/%s?serverTimezone=Asia/Shanghai"
                + "&useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
        jdbcUrl = jdbcUrl.formatted(
                environment.getRequiredProperty("sky.datasource.host"),
                environment.getRequiredProperty("sky.datasource.port"),
                DATABASE);

        List<TokenAccount> users = new ArrayList<>();
        try (var connection = DriverManager.getConnection(
                jdbcUrl,
                environment.getRequiredProperty("sky.datasource.username"),
                environment.getRequiredProperty("sky.datasource.password"));
             var statement = connection.prepareStatement(ACCOUNT_SQL);
             var rows = statement.executeQuery()) {
            while (rows.next()) {
                long userId = rows.getLong("user_id");
                String token = userService.createToken(User.builder().id(userId).build());
                // ID 以字符串导出，避免 JavaScript 的大整数精度损失。
                users.add(new TokenAccount(Long.toString(userId), token));
            }
        }

        Files.createDirectories(OUTPUT.toAbsolutePath().getParent());
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(
                OUTPUT.toFile(), new Export(jwt.getUserTokenName(), users));
        System.out.printf("已导出 %d 个账号，Token 有效期 %d 小时：%s%n",
                users.size(), Duration.ofMillis(TOKEN_TTL_MILLIS).toHours(), OUTPUT.toAbsolutePath());
    }

    record TokenAccount(String userId, String token) {
    }

    record Export(String tokenHeader, List<TokenAccount> users) {
    }
}
