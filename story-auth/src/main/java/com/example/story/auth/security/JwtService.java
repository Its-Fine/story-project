package com.example.story.auth.security;

import com.example.story.auth.config.StoryAuthProperties;
import com.example.story.auth.entity.AuthUser;
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
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(getSigningKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    public UserDetails detailsFromToken(String token) {
        Claims claims = parseClaims(token);

        String email = claims.getSubject();
        String role = "ROLE_" + claims.get("role", String.class);

        return new User(email, "*****", Collections.singleton(new SimpleGrantedAuthority(role)));
    }

    public boolean isAccessTokenValid(String token) {
        return isTokenValid(token, ACCESS_TOKEN);
    }

    public boolean isRefreshTokenValid(String token) {
        return isTokenValid(token, REFRESH_TOKEN);
    }

    private boolean isTokenValid(String token, String expectedType) {
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            return expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
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
