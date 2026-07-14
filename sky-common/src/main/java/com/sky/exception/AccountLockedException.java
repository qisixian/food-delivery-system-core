package com.sky.exception;

/**
 * 账号被锁定异常
 */
public class AccountLockedException extends ApplicationAuthenticationException {

    public AccountLockedException(String msg) {
        super(msg);
    }

}
