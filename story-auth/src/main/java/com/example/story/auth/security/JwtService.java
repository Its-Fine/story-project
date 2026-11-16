package com.example.story.auth.security;

import com.example.story.auth.config.StoryAuthProperties;
import com.example.story.auth.entity.AuthUser;
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

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN = "ACCESS";
    private static final String REFRESH_TOKEN = "REFRESH";

    private final StoryAuthProperties storyAuthProperties;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(storyAuthProperties.getJwt().getSecret()));
    }

    public String generateAccessToken(AuthUser user) {
        return generateToken(user, ACCESS_TOKEN, storyAuthProperties.getJwt().getExpirationAccess().toMillis());
    }

    public String generateRefreshToken(AuthUser user) {
        return generateToken(user, REFRESH_TOKEN, storyAuthProperties.getJwt().getExpirationRefresh().toMillis());
    }

    private String generateToken(AuthUser user, String tokenType, long expirationMillis) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMillis))
                .signWith(getSigningKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractAccessTokenSubject(String token) {
        return parseSubject(token, ACCESS_TOKEN);
    }

    public String extractRefreshTokenSubject(String token) {
        return parseSubject(token, REFRESH_TOKEN);
    }

    public boolean isAccessTokenValid(String token) {
        return isTokenValid(token, ACCESS_TOKEN);
    }

    public boolean isRefreshTokenValid(String token) {
        return isTokenValid(token, REFRESH_TOKEN);
    }

    private boolean isTokenValid(String token, String expectedType) {
        try {
            parseSubject(token, expectedType);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String parseSubject(String token, String expectedType) {
        var parser = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .require(TOKEN_TYPE_CLAIM, expectedType)
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
}
