package com.example.story.security;

import com.example.story.configuration.StoryProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final StoryProperties storyProperties;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(storyProperties.getJwt().getSecret()));
    }

    public String extractAccessTokenSubject(String token) {
        var parser = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .require("token_type", "ACCESS")
                .build();
        Claims claims;
        try {
            claims = parser.parseClaimsJws(token).getBody();
        } catch (IllegalStateException e) {
            // JJWT 0.11 throws this for non-scalar date claims such as exp: {}.
            throw new MalformedJwtException("Invalid token claims", e);
        }
        String subject = claims.get(Claims.SUBJECT, String.class);
        Date expiration = claims.getExpiration();
        if (subject == null || subject.isBlank() || expiration == null || !expiration.after(new Date())) {
            throw new MalformedJwtException("Token must have a subject and a future expiration");
        }
        return subject;
    }

    public boolean isTokenValid(String token) {
        try {
            extractAccessTokenSubject(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
