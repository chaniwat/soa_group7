package com.shenzhentagram.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by phompang on 3/6/2017 AD.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class JwtTokenMissingException extends AuthenticationException {
    public JwtTokenMissingException(String msg) {
        super(msg);
    }
}
