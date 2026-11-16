package com.example.story.testsupport;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.stream.Stream;

public final class JwtTokens {
    // Public test key, never used by production configuration.
    public static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    public static final Key KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    public static final String EMAIL = "security-test@example.com";

    private JwtTokens() {}

    public static JwtBuilder builder(String type) {
        return Jwts.builder().setSubject(EMAIL).claim("token_type", type)
                .setExpiration(new Date(System.currentTimeMillis() + 300_000));
    }

    public static String sign(JwtBuilder builder) {
        return builder.signWith(KEY, SignatureAlgorithm.HS256).compact();
    }

    public static String token(String type, String role) {
        return sign(builder(type).claim("role", role));
    }

    public static Stream<String> invalidTokens() {
        return Stream.concat(Stream.of("ACCESS", "REFRESH").flatMap(type -> Stream.of(
                sign(builder(type).setExpiration(new Date(System.currentTimeMillis() - 60_000))),
                sign(builder(type).setExpiration(null)),
                sign(builder(type).setSubject(null)),
                sign(builder(type).setSubject("   ")),
                sign(builder(type).claim("sub", java.util.Map.of("invalid", true))),
                sign(builder(type).claim("sub", 123)),
                sign(builder(type).claim("exp", "not-a-date")),
                sign(builder(type).claim("exp", java.util.Map.of("invalid", true))),
                sign(builder(type).claim("nbf", java.util.Map.of("invalid", true))),
                sign(builder(type).setNotBefore(new Date(System.currentTimeMillis() + 60_000))),
                builder(type).signWith(Keys.secretKeyFor(SignatureAlgorithm.HS256)).compact(),
                builder(type).compact()
        )), Stream.of(null, "", "not.a.jwt", sign(builder(null)), sign(builder("UNKNOWN")),
                sign(builder("access")), sign(builder("ACCESS").claim("token_type", 123))));
    }
}
