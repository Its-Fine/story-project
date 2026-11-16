package com.example.story.security;

import com.example.story.entity.UserEntity;
import com.example.story.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwtToken = authHeader.substring(7);

            if (SecurityContextHolder.getContext().getAuthentication() == null
                    && jwtService.isTokenValid(jwtToken)) {
                UserDetails details = jwtService.detailsFromToken(jwtToken);
                Optional<UserEntity> user = userRepository.findByEmail(details.getUsername());

                if (user.isPresent() && !Boolean.TRUE.equals(user.get().getBanned())) {
                    UsernamePasswordAuthenticationToken token =
                            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
                    token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(token);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
