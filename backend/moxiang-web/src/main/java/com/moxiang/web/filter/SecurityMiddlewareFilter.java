package com.moxiang.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moxiang.common.api.CommonResult;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.constant.SecurityEventType;
import com.moxiang.common.security.SecurityAuditLogger;
import com.moxiang.common.utils.WebUtils;
import com.moxiang.service.security.SecurityPlugin;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

/**
 * Central security middleware filter — the outermost entry point in the request
 * pipeline.
 *
 * <h3>Responsibilities</h3>
 * <ol>
 *   <li><b>Centralized audit logging</b> — records every request start and
 *       completion (path, method, IP, status code, duration) to the
 *       {@code SECURITY_AUDIT} logger via {@link SecurityAuditLogger}.</li>
 *   <li><b>Plugin-based security checks</b> — runs all {@link SecurityPlugin}
 *       implementations (sorted by {@link SecurityPlugin#getOrder()}) before the
 *       request enters the rest of the filter chain.  Any plugin returning
 *       {@code false} or throwing causes an immediate HTTP 403 response.</li>
 *   <li><b>Module coordination</b> — acts as the composition root for the
 *       security subsystem.  Downstream filters ({@code DeviceFingerprintFilter},
 *       {@code JwtAuthFilter}) and AOP aspects ({@code RateLimitAspect},
 *       {@code CaptchaAspect}, {@code SanitizeAspect}) handle their specific
 *       concerns; this filter ties them together via a shared audit trail.</li>
 * </ol>
 *
 * <h3>Filter order in the chain</h3>
 * <pre>
 *   SecurityMiddlewareFilter          ← this class (outermost)
 *   DeviceFingerprintFilter           ← IP blacklist + fingerprint
 *   JwtAuthFilter                     ← token validation + SecurityContext
 *   UsernamePasswordAuthenticationFilter
 *   ... (Spring Security internals)
 *   Controller  →  [RateLimitAspect, CaptchaAspect, SanitizeAspect]
 * </pre>
 */
@Component
public class SecurityMiddlewareFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityMiddlewareFilter.class);

    private final SecurityAuditLogger auditLogger;
    private final List<SecurityPlugin> plugins;
    private final ObjectMapper objectMapper;

    /**
     * {@code plugins} is injected as an optional list — the application starts
     * normally even when no {@link SecurityPlugin} beans are registered.
     */
    public SecurityMiddlewareFilter(SecurityAuditLogger auditLogger,
                                    List<SecurityPlugin> plugins,
                                    ObjectMapper objectMapper) {
        this.auditLogger = auditLogger;
        this.objectMapper = objectMapper;
        // Sort once at construction time; Spring may inject in any order
        this.plugins = plugins.stream()
                .sorted(Comparator.comparingInt(SecurityPlugin::getOrder))
                .toList();
        if (!this.plugins.isEmpty()) {
            log.info("SecurityMiddlewareFilter loaded {} plugin(s): {}",
                    this.plugins.size(),
                    this.plugins.stream().map(SecurityPlugin::getName).toList());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startMs = System.currentTimeMillis();
        String clientIp = WebUtils.getClientIp(request);
        String path = request.getRequestURI();
        String method = request.getMethod();

        auditLogger.log(SecurityEventType.REQ_START, clientIp, null, path,
                "START", "method=" + method);

        // ---- Run registered security plugins ----
        for (SecurityPlugin plugin : plugins) {
            try {
                if (!plugin.check(request, SecurityContextHolder.getContext())) {
                    log.warn("Request blocked by plugin '{}': ip={} path={}", plugin.getName(), clientIp, path);
                    auditLogger.logBlocked(SecurityEventType.PLUGIN_BLOCKED, clientIp, null, path,
                            "plugin=" + plugin.getName());
                    writeForbidden(response);
                    return;
                }
            } catch (Exception e) {
                log.warn("Security plugin '{}' threw exception for ip={} path={}: {}",
                        plugin.getName(), clientIp, path, e.getMessage());
                auditLogger.logBlocked(SecurityEventType.PLUGIN_BLOCKED, clientIp, null, path,
                        "plugin=" + plugin.getName() + " error=" + e.getMessage());
                writeForbidden(response);
                return;
            }
        }

        // ---- Continue through the filter chain ----
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            int status = response.getStatus();
            auditLogger.log(SecurityEventType.REQ_COMPLETE, clientIp, null, path,
                    String.valueOf(status), "method=" + method + " durationMs=" + durationMs);
        }
    }

    // ---- Helpers ----

    /** Writes a JSON 403 response without continuing the filter chain. */
    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        CommonResult<Void> body = CommonResult.failed(ResultCode.FORBIDDEN);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
