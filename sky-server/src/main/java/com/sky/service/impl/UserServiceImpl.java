package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.properties.GoogleLoginProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GoogleLoginProperties googleLoginProperties;

    @Override
    public User googleLogin(UserLoginDTO userLoginDTO) {
        // 调用 google 登录接口，获得用户openId
        Map<String, String> map = new HashMap<>();
        map.put("client_id", googleLoginProperties.getClientId());
        map.put("client_secret", googleLoginProperties.getClientSecret());
        String json = HttpClientUtil.doGet(googleLoginProperties.getAuthUrl(), map);
        JSONObject jsonObject = JSONObject.parseObject(json);
        String openid = jsonObject.getString("openid");
        // 到这里会不会有什么异常？

        // 判断当前用户是否为新用户
        User user = userMapper.getByOpenid(openid);

        // 如果是新用户，自动注册
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        // 返回这个用户对象
        return user;
    }
}
