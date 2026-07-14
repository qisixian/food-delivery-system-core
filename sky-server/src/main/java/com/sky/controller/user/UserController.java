package com.sky.controller.user;

import com.sky.dto.UserLoginDTO;
import com.sky.exception.FeatureNotEnabledException;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user/user")
@Slf4j
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        throw new FeatureNotEnabledException("Not implemented yet");
        // 先登录
//        User user = userService.login(userLoginDTO);
        // 然后为用户生成JWT令牌
        // JWT 和 Oauth 是两个东西，Oauth 是google系统的，JWT是我自己内部系统的

//        String token = userService.createToken(user);
//
//        UserLoginVO userLoginVO = UserLoginVO.builder()
//                .id(user.getId())
//                .openid(user.getOpenid())
//                .token(token)
//                .build();

//        return Result.success(userLoginVO);
    }

    @GetMapping("/me")
    public Result<Void> me() {
        return Result.success();
    }
}
