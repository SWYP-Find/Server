package com.swyp.app.global.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class ApiLoggingAspect {

    // 1. 모든 컨트롤러 패키지 하위의 메서드를 타겟으로 잡습니다.
    @Around("execution(* com.swyp.app.domain..controller..*.*(..))")
    public Object logApiExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        // 2. 현재 요청의 HttpServletRequest 가져오기
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String method = (request != null) ? request.getMethod() : "UNKNOWN";
        String uri = (request != null) ? request.getRequestURI() : "UNKNOWN";
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long start = System.currentTimeMillis();

        try {
            // 3. 실제 비즈니스 로직 실행
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - start;

            // 4. 성공 로그 기록
            log.info(">>> [API SUCCESS] {}: {} | Controller: {}.{} | Time: {}ms",
                     method, uri, className, methodName, executionTime);

            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - start;

            // 5. 에러 로그 기록 (에러 발생 시에도 시간 측정)
            log.error(">>> [API ERROR] {}: {} | Controller: {}.{} | Time: {}ms | Message: {}",
                      method, uri, className, methodName, executionTime, e.getMessage());

            throw e;
        }
    }
}