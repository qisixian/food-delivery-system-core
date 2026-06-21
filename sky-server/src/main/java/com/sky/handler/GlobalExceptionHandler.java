package com.sky.handler;

import com.sky.constant.LogFields;
import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.exception.UserNotLoginException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result<Void> exceptionHandler(BaseException ex){
        log.atError().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName()).setCause(ex).log(ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result<Void> exceptionHandler(UserNotLoginException ex){
        log.atInfo().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName()).log("exception: " + ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result<Void> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        log.atError().addKeyValue(LogFields.EXCEPTION_CLASS_NAME, ex.getClass().getName()).setCause(ex).log(ex.getMessage());
        return Result.error(MessageConstant.ALREADY_EXISTS);
    }

}
