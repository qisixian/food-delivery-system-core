package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.LogFields;
import com.sky.constant.MessageConstant;
import com.sky.context.UserContext;
import com.sky.exception.UnauthenticatedException;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;
import java.util.Set;

/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserContext userContext;

    private static final Set<String> OPTIONAL_AUTH_PATHS = Set.of(
            "/user/shoppingCart/list"
    );

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        boolean optionalAuth = OPTIONAL_AUTH_PATHS.contains(request.getRequestURI());

        //2、校验令牌
        try {
//            log.debug("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
//            log.debug("当前顾客id：{}", userId);
            userContext.set(userId);
            MDC.put(LogFields.USER_ID, Objects.toString(userId));
            MDC.put("isEmployee", "false");
            //3、通过，放行
            return true;
        } catch (Exception ex) {
            // 放行可选登录的接口
            if(optionalAuth) return true;

            //4、不通过。这里能抛出异常给全局异常处理器处理吗？还是响应401状态码 return false？
            throw new UnauthenticatedException(MessageConstant.UNAUTHENTICATED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 无论请求是否正常结束都要清理
        userContext.remove();
    }
}
