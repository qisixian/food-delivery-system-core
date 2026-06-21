package com.sky.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;


@Component
public class MdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Map<String, String> previousContextMap = MDC.getCopyOfContextMap();

        try {
            MDC.put("http.method", request.getMethod());
            MDC.put("http.path", request.getRequestURI());
            MDC.put("client.ip", request.getRemoteAddr());

            filterChain.doFilter(request, response);
        } finally {
            if (previousContextMap == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(previousContextMap);
            }
        }
    }
}
