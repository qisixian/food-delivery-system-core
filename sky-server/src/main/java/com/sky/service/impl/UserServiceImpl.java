package com.sky.service.impl;

import com.sky.constant.JwtClaimsConstant;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    JwtProperties jwtProperties;

    @Autowired
    private Clock clock;

    public User getOrCreateUser(String openid) {
        // 判断当前用户是否为新用户
        User user = userMapper.getByOpenid(openid);
        // 如果是新用户，自动注册
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now(clock))
                    .build();
            userMapper.insert(user);
        }
        return user;
    }

    @Override
    public String createToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        return JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims
        );
    }
}
