package com.spendwise.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.spendwise.config.AppProperties;
import com.spendwise.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private final SecretKey key = new SecretKeySpec("0123456789abcdef0123456789abcdef".getBytes(), "HmacSHA256");
    private final JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    private final JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    private final AppProperties props = new AppProperties(
            new AppProperties.Jwt("unused", Duration.ofHours(2), "spendwise"),
            new AppProperties.Cors(List.of()), false);

    private final TokenService tokenService = new TokenService(encoder, props);

    private User user() {
        var user = new User("anna@example.com", "hash", "Anna");
        ReflectionTestUtils.setField(user, "id", 42L);
        return user;
    }

    @Test
    void issuedTokenContainsUserClaimsAndExpiry() {
        Instant before = Instant.now();
        var issued = tokenService.issue(user());

        Jwt jwt = decoder.decode(issued.value());
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("anna@example.com");
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Anna");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("spendwise");
        assertThat(issued.expiresAt()).isBetween(before.plus(Duration.ofHours(2)).minusSeconds(1),
                Instant.now().plus(Duration.ofHours(2)));
        assertThat(CurrentUser.id(jwt)).isEqualTo(42L);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        var otherKey = new SecretKeySpec("ffffffffffffffffffffffffffffffff".getBytes(), "HmacSHA256");
        var otherService = new TokenService(new NimbusJwtEncoder(new ImmutableSecret<>(otherKey)), props);

        String foreignToken = otherService.issue(user()).value();
        assertThatThrownBy(() -> decoder.decode(foreignToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        var claims = JwtClaimsSet.builder()
                .subject("42")
                .issuedAt(Instant.now().minus(Duration.ofHours(3)))
                .expiresAt(Instant.now().minus(Duration.ofHours(1)))
                .build();
        String expired = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        assertThatThrownBy(() -> decoder.decode(expired))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("expired");
    }
}
