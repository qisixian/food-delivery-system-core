package com.sky.config;

import com.github.pagehelper.PageInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class MybatisConfig {

    @Bean
    PageInterceptor pageInterceptor() {
        PageInterceptor interceptor = new PageInterceptor();
        Properties properties = new Properties();
        properties.setProperty("reasonable", "true");
        interceptor.setProperties(properties);
        return interceptor;
    }
}
