package com.sky.oauth;

import lombok.Builder;

@Builder
public record OAuthUserInfo(
        String providerUserId,
        String email,
        String name,
        String avatarUrl
) {
}
