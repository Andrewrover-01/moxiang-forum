package com.moxiang.common.exception;

import com.moxiang.common.api.CommonResult;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.security.SecurityAuditLogger;
import com.moxiang.common.constant.SecurityEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.moxiang.common.exception.CaptchaException;
import com.moxiang.common.exception.RateLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.moxiang.common.utils.WebUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for all controllers.
 *
 * <p>Security-relevant exceptions (authentication failures, access-denied,
 * rate-limit exceedances, and CAPTCHA failures) are additionally routed to the
 * {@link SecurityAuditLogger} so they appear in the centralized security audit trail.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final SecurityAuditLogger auditLogger;

    public GlobalExceptionHandler(SecurityAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @ExceptionHandler(CaptchaException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public CommonResult<Void> handleCaptchaException(CaptchaException e, HttpServletRequest request) {
        String ip = WebUtils.getClientIp(request);
        log.warn("CAPTCHA verification failed: {}", e.getMessage());
        auditLogger.logCaptchaFail(ip, null, request.getRequestURI(), e.getMessage());
        return CommonResult.failed(ResultCode.CAPTCHA_INVALID, e.getMessage());
    }

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public CommonResult<Void> handleRateLimitException(RateLimitException e, HttpServletRequest request) {
        String ip = WebUtils.getClientIp(request);
        log.warn("Rate limit exceeded: {}", e.getMessage());
        auditLogger.logRateLimited(ip, null, request.getRequestURI(), e.getMessage());
        return CommonResult.failed(ResultCode.RATE_LIMITED, e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public CommonResult<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception [{}]: {}", e.getCode(), e.getMessage());
        return CommonResult.failed(ResultCode.FAILED, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleValidation(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String message = fieldErrors.stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", message);
        return CommonResult.validateFailed(message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return CommonResult.validateFailed(message);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public CommonResult<Void> handleAuthenticationException(AuthenticationException e,
                                                            HttpServletRequest request) {
        String ip = WebUtils.getClientIp(request);
        log.warn("Authentication failed: {}", e.getMessage());
        auditLogger.logTokenInvalid(ip, request.getRequestURI(), e.getMessage());
        return CommonResult.unauthorized();
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CommonResult<Void> handleAccessDeniedException(AccessDeniedException e,
                                                          HttpServletRequest request) {
        String ip = WebUtils.getClientIp(request);
        log.warn("Access denied: {}", e.getMessage());
        auditLogger.logAccessDenied(ip, null, request.getRequestURI());
        return CommonResult.forbidden();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<Void> handleException(Exception e) {
        log.error("Unexpected error", e);
        return CommonResult.failed("服务器内部错误，请稍后重试");
    }
}
