package com.sky.handler;

import com.sky.constant.LogFields;
import com.sky.constant.MessageConstant;
import com.sky.exception.*;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    public Result<Void> exceptionHandler(FeatureNotEnabledException ex){
        log.atInfo().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName()).log(ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result<Void> exceptionHandler(InputValidationException ex){
        log.atInfo().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName()).log(ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result<Void> exceptionHandler(ResourceNotFoundException ex){
        log.atInfo().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName()).log(ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result<Void> exceptionHandler(BusinessException ex){
        log.atWarn().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName()).log(ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result<Void> exceptionHandler(InvalidBusinessOperationException ex){
        log.atInfo().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName()).log(ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result<Void> exceptionHandler(ApplicationAuthenticationException ex){
        log.atInfo().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName()).log(ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result<Void> exceptionHandler(DuplicateKeyException ex){
        log.atInfo().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName()).log(ex.getMessage());
        return Result.error(MessageConstant.ALREADY_EXISTS);
    }

    @ExceptionHandler
    public Result<Void> exceptionHandler(BaseException ex){
        log.atError().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName()).setCause(ex).log(ex.getMessage());
        return Result.error(ex.getMessage());
    }

    // Spring MVC 框架异常，目前通过 ResponseEntityExceptionHandler 自动处理

    @ExceptionHandler
    public Result<Void> handleUnexpectedException(Exception ex) {
        log.atError().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName()).setCause(ex).log("Unexpected system exception");
        return Result.error(MessageConstant.UNEXPECTED_SYSTEM_ERROR);
    }

}
