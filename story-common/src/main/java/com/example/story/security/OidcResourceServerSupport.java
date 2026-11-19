package com.example.story.security;

import com.example.story.configuration.StoryProperties;
import com.example.story.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class OidcResourceServerSupport {

    private final StoryProperties storyProperties;
    private final UserRepository userRepository;

    public JwtDecoder jwtDecoder() {
        StoryProperties.Oidc oidc = storyProperties.getOidc();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(oidc.getJwkSetUri()).build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(oidc.getIssuerUri());
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(oidc.getAudience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "Required audience is missing", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience));
        return decoder;
    }

    public Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> authenticationConverter() {
        return jwt -> {
            String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
            var user = userRepository.findByOidcIssuerAndOidcSubject(issuer, jwt.getSubject())
                    .orElseThrow(() -> new BadCredentialsException("OIDC identity is not linked to an application profile"));
            if (Boolean.TRUE.equals(user.getBanned())) {
                throw new DisabledException("User is banned");
            }

            var authorities = new ArrayList<SimpleGrantedAuthority>();
            userRepository.findRoleNamesByEmail(user.getEmail()).stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .forEach(authorities::add);
            userRepository.findPermissionNamesByEmail(user.getEmail()).stream()
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
            if (authorities.isEmpty()) {
                throw new BadCredentialsException("Application profile has no permissions");
            }

            return new JwtAuthenticationToken(jwt, authorities, user.getEmail());
        };
    }
}
