package com.innowise.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class SecurityKeyFilter extends OncePerRequestFilter {

    @Value("${service.api.key:service-key}")
    private String expectedKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String serviceKey = request.getHeader("X-Service-Key");

        if (serviceKey == null || !serviceKey.equals(expectedKey)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Access Denied\", \"message\": \"Invalid service key\"}");
            return;
        }

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_SERVICE"));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "SERVICE",
                null,
                authorities
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Service authentication set for internal call to: {}", request.getRequestURI());

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request. getRequestURI();
        return !path.startsWith("/internal/"); // Filter active only for /internal endpoints
    }
}