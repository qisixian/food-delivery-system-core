package com.sky.exception;

public class AuthenticationFailedException extends ApplicationAuthenticationException {

    public AuthenticationFailedException(String msg) {
        super(msg);
    }

}
