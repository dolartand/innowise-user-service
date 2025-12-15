package com.innowise.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeaderAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");
        String serviceKey = request.getHeader("X-Service-Key");

        if (hasText(userId) && hasText(role)) {
            try {
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        Long.parseLong(userId),
                        null,
                        Collections.singletonList(authority)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authentication set from headers: userId={}, role={}", userId, role);
            } catch (Exception e) {
                log.error("Authentication failed: {}", e.getMessage());
                filterChain.doFilter(request, response);
            }
        } else if (hasText(serviceKey)) {
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_SERVICE")
            );

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "SERVICE",
                    null,
                    authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Service authentication set for inter-service call: serviceKey={}", serviceKey);
        }
        filterChain.doFilter(request, response);
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") ||
                (path.equals("/api/v1/users") && request.getMethod().equals("POST")) ||
                path.startsWith("/api/v1/users/by-email");
    }
}
