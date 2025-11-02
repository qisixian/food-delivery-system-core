package com.sky.context;

import lombok.extern.slf4j.Slf4j;

/**
 * 使用静态工具类方式实现的 UserContext
 * 同样的效果也可以通过 向Spring ioc容器注入这个单例的bean来实现，每次使用的时候通过 @Autowired 注入
 * 最佳实践：一旦涉及状态、外部依赖或可测试性，优先用依赖注入的 Bean
 */

@Slf4j
public class UserContext {

    public static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void set(Long id) {
        USER_ID.set(id);
        log.info("[Thread:{}] set userId={}", Thread.currentThread().getName(), id);
    }

    public static Long get() {
        return USER_ID.get();
    }

    public static void remove() {
        log.info("[Thread:{}] clear userId={}", Thread.currentThread().getName(), USER_ID.get());
        USER_ID.remove();
    }

}
