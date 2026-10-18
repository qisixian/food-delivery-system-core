package com.sky.service;

import com.mysql.cj.exceptions.MysqlErrorNumbers;
import com.sky.dto.ShoppingCartDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
// 保证重试循环在事务外面执行
@Transactional(propagation = Propagation.NEVER)
public class ShoppingCartRetryService {

    private static final int MAX_ATTEMPTS = 2;

    @Autowired
    private ShoppingCartService shoppingCartService;

    public void addShoppingCart(ShoppingCartDTO dto) {
        executeWithRetry(() -> shoppingCartService.addShoppingCart(dto));

    }

    public void removeShoppingCart(ShoppingCartDTO dto) {
        executeWithRetry(() -> shoppingCartService.removeShoppingCart(dto));
    }

    public void cleanShoppingCart() {
        executeWithRetry(() -> shoppingCartService.cleanShoppingCart());
    }

    private void executeWithRetry(Runnable action) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                action.run();
                return;
            } catch (RuntimeException e) {
                // 只重试已确认的 MySQL 死锁。
                if (!isMysqlDeadlock(e) || attempt == MAX_ATTEMPTS) {
                    throw e;
                }

                // 每次重试前随机等待 20～50 ms。
                long delayMs = ThreadLocalRandom.current().nextLong(20, 51);

                log.atWarn().log("Cart deadlock: attempt={}/{}, retryInMs={}", attempt, MAX_ATTEMPTS, delayMs);

                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    private boolean isMysqlDeadlock(Throwable exception) {
        for (Throwable cause = exception;
             cause != null;
             cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException
                    && sqlException.getErrorCode() == MysqlErrorNumbers.ER_LOCK_DEADLOCK) {
                return true;
            }
        }
        return false;
    }
}