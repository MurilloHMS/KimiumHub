package com.proautokimium.api.domain.entities.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FirstAcessTokenTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 16, 12, 0);

    @Test
    @DisplayName("Deve ser válido quando não expirou e não foi usado")
    void shouldBeValidWhenNotExpiredAndNotUsed() {
        FirstAcessToken token = tokenExpiringAt(NOW.plusMinutes(30));

        assertThat(token.isValid(NOW)).isTrue();
    }

    @Test
    @DisplayName("Deve ser inválido quando expirado")
    void shouldBeInvalidWhenExpired() {
        FirstAcessToken token = tokenExpiringAt(NOW.minusMinutes(1));

        assertThat(token.isValid(NOW)).isFalse();
    }

    @Test
    @DisplayName("Deve ser inválido no instante exato da expiração")
    void shouldBeInvalidAtExactExpirationInstant() {
        FirstAcessToken token = tokenExpiringAt(NOW);

        assertThat(token.isValid(NOW)).isFalse();
    }

    @Test
    @DisplayName("Deve ser inválido após marcado como usado, mesmo dentro do prazo")
    void shouldBeInvalidAfterMarkedUsedEvenBeforeExpiration() {
        FirstAcessToken token = tokenExpiringAt(NOW.plusMinutes(30));

        token.markUsed();

        assertThat(token.isValid(NOW)).isFalse();
        assertThat(token.isUsed()).isTrue();
    }

    private FirstAcessToken tokenExpiringAt(LocalDateTime expiration) {
        FirstAcessToken token = new FirstAcessToken();
        token.setToken("ABC123");
        token.setExpiration(expiration);
        return token;
    }
}
