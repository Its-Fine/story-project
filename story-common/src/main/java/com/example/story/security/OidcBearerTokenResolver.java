package com.example.story.security;

import com.example.story.configuration.StoryProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class OidcBearerTokenResolver implements BearerTokenResolver {

    private final StoryProperties storyProperties;
    private final ObjectMapper objectMapper;
    private final DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        if (!storyProperties.getOidc().isEnabled()) {
            return null;
        }

        String token = delegate.resolve(request);
        if (token == null) {
            return null;
        }

        try {
            String[] segments = token.split("\\.");
            if (segments.length != 3) {
                return null;
            }
            byte[] payload = Base64.getUrlDecoder().decode(segments[1]);
            JsonNode claims = objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
            String issuer = claims.path("iss").asText(null);
            return storyProperties.getOidc().getIssuerUri().equals(issuer) ? token : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
