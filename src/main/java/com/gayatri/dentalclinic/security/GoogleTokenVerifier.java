package com.gayatri.dentalclinic.security;

import com.gayatri.dentalclinic.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Component
public class GoogleTokenVerifier {
    private final String clientId;
    private final JwtDecoder decoder;

    @org.springframework.beans.factory.annotation.Autowired
    public GoogleTokenVerifier(@Value("${app.auth.google.client-id:${GOOGLE_CLIENT_ID:}}") String clientId) {
        this(clientId, NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build());
    }

    GoogleTokenVerifier(String clientId, JwtDecoder decoder) {
        this.clientId = clientId.trim();
        this.decoder = decoder;
    }

    public Identity verify(String credential) {
        if (clientId.isBlank()) {
            throw new BadRequestException("Google sign-in is not configured. Please use email and password.");
        }
        try {
            Jwt jwt = decoder.decode(credential);
            String email = jwt.getClaimAsString("email");
            String subject = jwt.getSubject();
            if (!Set.of("https://accounts.google.com", "accounts.google.com").contains(jwt.getClaimAsString("iss"))
                    || !jwt.getAudience().contains(clientId)
                    || jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())
                    || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))
                    || subject == null || subject.isBlank() || subject.length() > 255
                    || email == null || email.isBlank() || email.length() > 254) {
                throw new IllegalArgumentException("Invalid Google claims");
            }
            email = email.toLowerCase(Locale.ROOT);
            String hostedDomain = jwt.getClaimAsString("hd");
            boolean authoritativeEmail = email.endsWith("@gmail.com") || (hostedDomain != null && !hostedDomain.isBlank());
            return new Identity(subject, email, jwt.getClaimAsString("given_name"),
                    jwt.getClaimAsString("family_name"), authoritativeEmail);
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            throw new BadRequestException("Google sign-in could not be verified. Please try again.");
        }
    }

    public record Identity(String subject, String email, String firstName, String lastName,
                           boolean authoritativeEmail) { }
}
