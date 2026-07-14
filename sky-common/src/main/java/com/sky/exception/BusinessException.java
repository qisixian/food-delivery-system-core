package com.sky.exception;

public class BusinessException extends BaseException {

    public BusinessException(String msg) {
        super(msg);
    }

    public BusinessException(String msg, String errorOrigin) {
        super(msg, errorOrigin);
    }

}
