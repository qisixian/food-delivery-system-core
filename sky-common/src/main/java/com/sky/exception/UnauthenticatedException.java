package com.sky.exception;

public class UnauthenticatedException extends ApplicationAuthenticationException {

    public UnauthenticatedException(String msg) {
        super(msg);
    }

}
