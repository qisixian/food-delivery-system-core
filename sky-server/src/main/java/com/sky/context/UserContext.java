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
        log.atDebug()
                .addKeyValue("userId", id)
                .log("set userId");
    }

    public Long get() {
        return USER_ID.get();
    }

    public void remove() {
        log.atDebug()
                .addKeyValue("userId", USER_ID.get())
                .log("clear userId");
        USER_ID.remove();
    }

}
