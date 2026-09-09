package com.sky.oauth;

public interface OAuthProvider {

    String buildAuthorizationUrl(String state);

    OAuthUserInfo authenticate(String code);
}