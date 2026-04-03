package com.moxiang.common.annotation;

import java.lang.annotation.*;

/**
 * Marks a DTO {@code String} field for XSS sanitization.
 *
 * <p>When a controller method is invoked, {@code SanitizeAspect} iterates over the
 * arguments of the method, finds all objects with {@code @Sanitize}-annotated
 * {@code String} fields, and strips potentially dangerous HTML/script content
 * via {@code XssUtils.clean()}.
 *
 * <p>Usage:
 * <pre>{@code
 * @Sanitize
 * private String content;
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sanitize {
}
