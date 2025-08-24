package com.deefacto.user_service.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCodeInterface {
    String getCode();
    String getMessage();
    HttpStatus getStatus();
}
