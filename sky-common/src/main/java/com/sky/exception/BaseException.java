package com.sky.exception;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BaseException extends RuntimeException {

    private final String errorOrigin;

    public BaseException(String msg) {
        this(msg, null);
    }

    public BaseException(String msg, String errorOrigin) {
        super(msg);
        this.errorOrigin = errorOrigin;
    }

}
