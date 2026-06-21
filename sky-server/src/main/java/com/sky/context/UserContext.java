package com.sky.context;

import com.sky.constant.LogFields;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 使单例 bean 方式实现的 UserContext
 * 最佳实践：一旦涉及状态、外部依赖或可测试性，优先用依赖注入的 Bean（相比于使用静态工具类）
 */

@Component
@Slf4j
public class UserContext {

    private final ThreadLocal<Long> userId = new ThreadLocal<>();

    public void set(Long id) {
        userId.set(id);
        log.atDebug()
                .addKeyValue(LogFields.USER_ID, id)
                .log("set userId");
    }

    public Long get() {
        return userId.get();
    }

    public void remove() {
        log.atDebug()
                .addKeyValue(LogFields.USER_ID, userId.get())
                .log("clear userId");
        userId.remove();
    }

}
