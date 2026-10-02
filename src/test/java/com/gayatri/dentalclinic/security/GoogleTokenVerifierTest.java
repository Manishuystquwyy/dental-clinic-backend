package com.gayatri.dentalclinic.security;

import com.gayatri.dentalclinic.exception.BadRequestException;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import java.time.Instant;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

class GoogleTokenVerifierTest {
    private final com.nimbusds.jose.jwk.RSAKey key = new RSAKeyGenerator(2048).generate();
    private final GoogleTokenVerifier verifier = new GoogleTokenVerifier("client-id",
            NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build());

    GoogleTokenVerifierTest() throws Exception { }

    private String token(String issuer, String audience, Instant expiry, boolean verified, String email, String subject) throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject(subject)
                .expirationTime(Date.from(expiry)).issueTime(new Date()).claim("email", email)
                .claim("email_verified", verified).build();
        var token = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        token.sign(new RSASSASigner(key));
        return token.serialize();
    }

    @Test void acceptsValidSignedGoogleIdentity() throws Exception {
        var identity = verifier.verify(token("https://accounts.google.com", "client-id", Instant.now().plusSeconds(300), true, "user@gmail.com", "google-sub"));
        assertEquals("google-sub", identity.subject());
        assertTrue(identity.authoritativeEmail());
    }

    @Test void rejectsWrongAudienceIssuerExpiredUnverifiedAndMissingSubject() throws Exception {
        assertThrows(BadRequestException.class, () -> verifier.verify(token("https://accounts.google.com", "other-client", Instant.now().plusSeconds(300), true, "user@gmail.com", "sub")));
        assertThrows(BadRequestException.class, () -> verifier.verify(token("https://attacker.example", "client-id", Instant.now().plusSeconds(300), true, "user@gmail.com", "sub")));
        assertThrows(BadRequestException.class, () -> verifier.verify(token("https://accounts.google.com", "client-id", Instant.now().minusSeconds(1), true, "user@gmail.com", "sub")));
        assertThrows(BadRequestException.class, () -> verifier.verify(token("https://accounts.google.com", "client-id", Instant.now().plusSeconds(300), false, "user@gmail.com", "sub")));
        assertThrows(BadRequestException.class, () -> verifier.verify(token("https://accounts.google.com", "client-id", Instant.now().plusSeconds(300), true, "user@gmail.com", null)));
    }

    @Test void rejectsForgedSignatureAndMalformedTokens() throws Exception {
        var otherKey = new RSAKeyGenerator(2048).generate();
        var forged = SignedJWT.parse(token("https://accounts.google.com", "client-id", Instant.now().plusSeconds(300), true, "user@gmail.com", "sub"));
        forged = new SignedJWT(forged.getHeader(), forged.getJWTClaimsSet());
        forged.sign(new RSASSASigner(otherKey));
        String forgedToken = forged.serialize();
        assertThrows(BadRequestException.class, () -> verifier.verify(forgedToken));
        assertThrows(BadRequestException.class, () -> verifier.verify("not-a-token"));
    }

    @Test void thirdPartyEmailIsNotAuthoritative() throws Exception {
        assertFalse(verifier.verify(token("accounts.google.com", "client-id", Instant.now().plusSeconds(300), true, "user@example.com", "sub")).authoritativeEmail());
    }

    @Test void disabledConfigurationFailsClosed() {
        assertThrows(BadRequestException.class, () -> new GoogleTokenVerifier(" ", token -> { throw new AssertionError(); }).verify("token"));
    }
}
