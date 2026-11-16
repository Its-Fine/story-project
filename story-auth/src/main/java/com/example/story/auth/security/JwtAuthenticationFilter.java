package com.example.story.auth.security;

import com.example.story.auth.repository.AuthUserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthUserRepository authUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = null;
            try {
                email = jwtService.extractAccessTokenSubject(header.substring(7));
            } catch (JwtException | IllegalArgumentException ignored) {
                // Leave invalid credentials unauthenticated; protected routes return 401.
            }
            if (email != null) {
                authUserRepository.findByEmail(email)
                        .filter(user -> !Boolean.TRUE.equals(user.getBanned()))
                        .filter(user -> user.getRole() != null && !user.getRole().isBlank())
                        .ifPresent(user -> {
                            // JWT role claims can outlive a role change. Use current database permissions.
                            var details = User.withUsername(user.getEmail()).password("").roles(user.getRole()).build();
                            var authentication = new UsernamePasswordAuthenticationToken(
                                    details, null, details.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        });
            }
        }
        filterChain.doFilter(request, response);
    }
}
