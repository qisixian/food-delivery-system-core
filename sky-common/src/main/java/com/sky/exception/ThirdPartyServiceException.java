package com.sky.exception;

import com.sky.enumeration.ThirdPartyErrorType;
import com.sky.enumeration.ThirdPartyProvider;
import lombok.Getter;

@Getter
public class ThirdPartyServiceException extends BaseException {

    private final ThirdPartyProvider provider;

    private final ThirdPartyErrorType errorType;

    public ThirdPartyServiceException(String msg, ThirdPartyProvider provider, ThirdPartyErrorType errorType) {
        super(msg);
        this.provider = provider;
        this.errorType = errorType;
    }

}
