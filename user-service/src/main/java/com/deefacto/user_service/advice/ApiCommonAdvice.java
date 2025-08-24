package com.deefacto.user_service.advice;

import com.deefacto.user_service.common.dto.ApiResponseDto;
import com.deefacto.user_service.common.exception.CustomException;
import com.deefacto.user_service.common.exception.ErrorCodeInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(value = 1)
@RestControllerAdvice
public class ApiCommonAdvice {
    @ExceptionHandler(CustomException.class)

    public ResponseEntity<ApiResponseDto<String>> handleCustomException2(CustomException e) {
        ErrorCodeInterface errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus()) // HTTP status
                .body(ApiResponseDto.createError(
                        errorCode.getCode(),       // "COMMON404" 같은 에러코드
                        e.getMessage()     // 메시지
                ));
    }
}

