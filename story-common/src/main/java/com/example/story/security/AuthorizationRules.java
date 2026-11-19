package com.example.story.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthorizationRules {

    private AuthorizationRules() {
    }

    public static void requireAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean granted = authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(candidate -> authority.equals(candidate.getAuthority()));

        if (!granted) {
            throw new AccessDeniedException("Required permission is missing: " + authority);
        }
    }
}
