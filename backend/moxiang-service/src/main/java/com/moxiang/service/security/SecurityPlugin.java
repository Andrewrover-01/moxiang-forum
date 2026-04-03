package com.moxiang.service.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContext;

/**
 * SPI interface for pluggable security checks in the request-processing pipeline.
 *
 * <p>Implementations are discovered via Spring's component scan and executed by
 * {@code SecurityMiddlewareFilter} in ascending {@link #getOrder()} sequence.
 *
 * <h3>Implementing a plugin</h3>
 * <pre>{@code
 * @Component
 * public class MyCustomPlugin implements SecurityPlugin {
 *     public String getName() { return "MyCustomPlugin"; }
 *     public int getOrder() { return 100; }
 *
 *     public boolean check(HttpServletRequest request, SecurityContext ctx) {
 *         // return false or throw to block the request
 *         return true;
 *     }
 * }
 * }</pre>
 */
public interface SecurityPlugin {

    /**
     * A short, unique human-readable name used in audit log entries.
     */
    String getName();

    /**
     * Execution order relative to other plugins (lower value runs first).
     * Plugins with equal order values run in an unspecified sequence.
     */
    int getOrder();

    /**
     * Inspects the incoming request and decides whether it should proceed.
     *
     * @param request the current HTTP request
     * @param context the current Spring Security context (may be empty for
     *                unauthenticated requests)
     * @return {@code true} if the request should continue through the filter chain;
     *         {@code false} to block it (the filter will return HTTP 403)
     * @throws RuntimeException optionally thrown to block the request with a
     *                          specific error; the exception message is logged
     */
    boolean check(HttpServletRequest request, SecurityContext context);
}
