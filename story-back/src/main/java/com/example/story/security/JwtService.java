package com.example.story.security;

import com.example.story.configuration.StoryProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Collections;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN = "ACCESS";

    private final StoryProperties storyProperties;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(storyProperties.getJwt().getSecret()));
    }

    public UserDetails detailsFromToken(String token) {
        Claims claims = parseClaims(token);

        String email = claims.getSubject();
        String role = "ROLE_" + claims.get("role", String.class);

        return new User(email, "*****", Collections.singleton(new SimpleGrantedAuthority(role)));
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            return ACCESS_TOKEN.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                    && expiration != null
                    && expiration.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
