package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.CurrentAccountContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    @Pointcut("execution(* com.sky.mapper.*.*(..))")
    public void autoFillPointcut() {}

    @Before("autoFillPointcut() && @annotation(autoFill)")
    public void autoFill(JoinPoint joinPoint, AutoFill autoFill) {
        log.trace("Auto-fill aspect triggered for method: {}", joinPoint.getSignature().getName());
        OperationType operationType = autoFill.value();
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            log.warn("No arguments found for method: {}", joinPoint.getSignature().getName());
            return;
        }
        // 约定：如果想实现自动填充，方法的第一个参数必须是需要自动填充的对象
        Object entity = args[0];
        if(operationType == OperationType.INSERT) {
            try {
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class)
                        .invoke(entity, LocalDateTime.now());
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class)
                        .invoke(entity, CurrentAccountContext.getId());
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class)
                        .invoke(entity, LocalDateTime.now());
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class)
                        .invoke(entity, CurrentAccountContext.getId());
            }
            catch (NoSuchMethodException e) {
                log.error("Method setCreateTime not found in class: {}", entity.getClass().getName());
            } catch (IllegalAccessException e) {
                log.error("Illegal access when invoking setCreateTime on class: {}", entity.getClass().getName());
            } catch (InvocationTargetException e) {
                log.error("Invocation target exception when invoking setCreateTime on class: {}", entity.getClass().getName());
            }
        }
        if (operationType == OperationType.UPDATE) {
            try {
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class)
                        .invoke(entity, LocalDateTime.now());
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class)
                        .invoke(entity, CurrentAccountContext.getId());
            }
            catch (NoSuchMethodException e) {
                log.error("Method setUpdateTime not found in class: {}", entity.getClass().getName());
            } catch (IllegalAccessException e) {
                log.error("Illegal access when invoking setUpdateTime on class: {}", entity.getClass().getName());
            } catch (InvocationTargetException e) {
                log.error("Invocation target exception when invoking setUpdateTime on class: {}", entity.getClass().getName());
            }
        }
    }

}
