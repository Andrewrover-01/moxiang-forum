package com.moxiang.web.aspect;

import com.moxiang.common.annotation.Sanitize;
import com.moxiang.common.constant.SecurityEventType;
import com.moxiang.common.utils.WebUtils;
import com.moxiang.common.utils.XssUtils;
import com.moxiang.common.security.SecurityAuditLogger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;

/**
 * AOP aspect that sanitizes {@code @RequestBody} DTO fields annotated with
 * {@link Sanitize} before the controller method executes.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Intercept all methods in {@code @RestController} beans that are
 *       {@code POST} or {@code PUT} (write operations).</li>
 *   <li>Iterate over method arguments.</li>
 *   <li>For each non-null, non-primitive argument, inspect its declared fields
 *       (including inherited) for {@link Sanitize}.</li>
 *   <li>For every {@code String} field annotated with {@link Sanitize}, call
 *       {@link XssUtils#clean(String)}.  If the value changed, log a
 *       {@code SANITIZE_CLEANED} audit event and replace the field value.</li>
 * </ol>
 *
 * <p>Field access is performed with {@link Field#setAccessible(boolean)} to
 * reach private fields without requiring public setters.
 */
@Aspect
@Component
public class SanitizeAspect {

    private static final Logger log = LoggerFactory.getLogger(SanitizeAspect.class);

    private final SecurityAuditLogger auditLogger;

    public SanitizeAspect(SecurityAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    /**
     * Pointcut: all methods in @RestController beans annotated with @PostMapping
     * or @PutMapping.
     */
    @Around("within(@org.springframework.web.bind.annotation.RestController *) && " +
            "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping))")
    public Object sanitize(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        if (args != null) {
            String clientIp = resolveIp();
            Long userId = getCurrentUserId();
            String path = resolvePath();

            for (Object arg : args) {
                if (arg == null) continue;
                Class<?> clazz = arg.getClass();
                // Skip JDK types, primitives, and common value types
                if (isJdkType(clazz)) continue;
                sanitizeObject(arg, clientIp, userId, path);
            }
        }
        return pjp.proceed();
    }

    // ---- Helpers ----

    /**
     * Inspects all declared fields of the object (walking the class hierarchy) and
     * cleans any {@code String} fields annotated with {@link Sanitize}.
     */
    private void sanitizeObject(Object obj, String clientIp, Long userId, String path) {
        Class<?> clazz = obj.getClass();
        while (clazz != null && !isJdkType(clazz)) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Sanitize.class)) continue;
                if (!String.class.equals(field.getType())) continue;

                try {
                    field.setAccessible(true);
                    String original = (String) field.get(obj);
                    if (original == null) continue;

                    String cleaned = XssUtils.clean(original);
                    if (!original.equals(cleaned)) {
                        field.set(obj, cleaned);
                        log.warn("XSS content removed from field '{}' on {} — ip={} userId={}",
                                field.getName(), clazz.getSimpleName(), clientIp, userId);
                        auditLogger.logSanitized(clientIp, userId, path, field.getName());
                    }
                } catch (IllegalAccessException e) {
                    log.warn("Could not sanitize field '{}': {}", field.getName(), e.getMessage());
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private boolean isJdkType(Class<?> clazz) {
        String name = clazz.getName();
        return name.startsWith("java.") || name.startsWith("javax.")
                || name.startsWith("sun.") || name.startsWith("com.sun.")
                || clazz.isPrimitive() || clazz.isArray() || clazz.isEnum();
    }

    private String resolveIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";
        return WebUtils.getClientIp(attrs.getRequest());
    }

    private String resolvePath() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";
        return attrs.getRequest().getRequestURI();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof Long) {
            return (Long) auth.getCredentials();
        }
        return null;
    }
}
