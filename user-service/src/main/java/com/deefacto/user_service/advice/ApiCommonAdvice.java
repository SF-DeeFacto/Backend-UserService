package com.deefacto.user_service.advice;

import com.deefacto.user_service.advice.parameter.ParameterErrorDto;
import com.deefacto.user_service.common.dto.ApiResponseDto;
import com.deefacto.user_service.common.exception.BadParameter;
import com.deefacto.user_service.common.exception.ClientError;
import com.deefacto.user_service.common.exception.NotFound;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@Order(value = 1)
@RestControllerAdvice
public class ApiCommonAdvice {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({BadParameter.class})
    public ApiResponseDto<String> handleBadParameter(BadParameter e) {
        e.printStackTrace();
        return ApiResponseDto.createError(
                e.getErrorCode(),
                e.getErrorMessage()
        );
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({NotFound.class})
    public ApiResponseDto<String> handleNotFound(NotFound e) {
        e.printStackTrace();
        return ApiResponseDto.createError(
                e.getErrorCode(),
                e.getErrorMessage()
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ClientError.class})
    public ApiResponseDto<String> handleClientError(ClientError e) {
        e.printStackTrace();
        return ApiResponseDto.createError(
                e.getErrorCode(),
                e.getErrorMessage()
        );
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler({DataIntegrityViolationException.class})
    public ApiResponseDto<String> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        e.printStackTrace();
        String errorMessage = "데이터 중복 오류가 발생했습니다.";
        
        // employeeId 중복인지 확인
        if (e.getMessage().contains("employee_id") || e.getMessage().contains("UKr1usl9qoplqsbrhha5e0niqng")) {
            errorMessage = "이미 존재하는 사번입니다.";
        }
        
        return ApiResponseDto.createError("DuplicateData", errorMessage);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({NoResourceFoundException.class})
    public ApiResponseDto<String> handleNoResourceFoundException(NoResourceFoundException e) {
        e.printStackTrace();
        return ApiResponseDto.createError(
                "NoResource",
                "리소스를 찾을 수 없습니다.");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ApiResponseDto<ParameterErrorDto.FieldList> handleArgumentNotValidException(MethodArgumentNotValidException e) {
        e.printStackTrace();
        BindingResult result = e.getBindingResult();
        ParameterErrorDto.FieldList fieldList = ParameterErrorDto.FieldList.of(result);

        String errorMessage = fieldList.getErrorMessage();
        return ApiResponseDto.createError("ParameterNotValid", errorMessage, fieldList);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({Exception.class})
    public ApiResponseDto<String > handleException(Exception e) {
        e.printStackTrace();
        return ApiResponseDto.createError(
                "ServerError",
                "서버 에러입니다.");
    }
}
