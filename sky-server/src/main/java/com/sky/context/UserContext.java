package com.sky.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 使单例 bean 方式实现的 UserContext
 * 最佳实践：一旦涉及状态、外部依赖或可测试性，优先用依赖注入的 Bean（相比于使用静态工具类）
 */

@Component
@Slf4j
public class UserContext {

    public final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public void set(Long id) {
        USER_ID.set(id);
        log.info("[Thread:{}] set userId={}", Thread.currentThread().getName(), id);
    }

    public Long get() {
        return USER_ID.get();
    }

    public void remove() {
        log.info("[Thread:{}] clear userId={}", Thread.currentThread().getName(), USER_ID.get());
        USER_ID.remove();
    }

}
