package com.moxiang.web.filter;

import com.moxiang.common.constant.AuthConstants;
import com.moxiang.common.utils.JwtUtils;
import com.moxiang.common.utils.RedisUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter — runs once per request, parses the token,
 * validates it against the Redis blacklist, and populates the SecurityContext.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;

    public JwtAuthFilter(JwtUtils jwtUtils, RedisUtils redisUtils) {
        this.jwtUtils = jwtUtils;
        this.redisUtils = redisUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);
        if (StringUtils.hasText(token)) {
            try {
                // Check blacklist
                String blacklistKey = AuthConstants.TOKEN_BLACKLIST_PREFIX + token;
                if (redisUtils.hasKey(blacklistKey)) {
                    log.debug("Token is blacklisted, skipping authentication");
                } else {
                    String username = jwtUtils.getUsernameFromToken(token);
                    String role = jwtUtils.getRoleFromToken(token);
                    Long userId = jwtUtils.getUserIdFromToken(token);

                    if (StringUtils.hasText(username) &&
                            SecurityContextHolder.getContext().getAuthentication() == null) {

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        username,
                                        userId,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                                );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException e) {
                log.debug("Invalid JWT token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AuthConstants.AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(AuthConstants.TOKEN_PREFIX)) {
            return header.substring(AuthConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}
