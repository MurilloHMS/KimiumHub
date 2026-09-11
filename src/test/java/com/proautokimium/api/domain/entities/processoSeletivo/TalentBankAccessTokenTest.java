package com.proautokimium.api.domain.entities.processoSeletivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O link de acesso do candidato ao próprio cadastro.
 *
 * <p>JUnit puro: a entidade recebe o {@code agora} em vez de chamar
 * {@code LocalDateTime.now()}, que é o que a torna testável sem Spring e sem
 * relógio falso.
 */
class TalentBankAccessTokenTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 11, 14, 0);

    private static TalentBankAccessToken tokenQueExpiraEm(LocalDateTime expiracao) {
        TalentBankAccessToken token = new TalentBankAccessToken();
        token.setTokenHash("hash");
        token.setCreatedAt(AGORA.minusMinutes(1));
        token.setExpiresAt(expiracao);
        return token;
    }

    @Test
    @DisplayName("Token dentro da janela vale")
    void dentroDaJanelaVale() {
        assertThat(tokenQueExpiraEm(AGORA.plusHours(1)).isValid(AGORA))
                .as("isBefore trocado por isAfter mataria todo token recem-criado")
                .isTrue();
    }

    /**
     * O instante exato da expiração já não vale. É o off-by-one que deixa o
     * token viver um tique a mais — pequeno, e do tipo que ninguém nota.
     */
    @Test
    @DisplayName("No instante exato da expiracao ja nao vale")
    void noInstanteExatoNaoVale() {
        assertThat(tokenQueExpiraEm(AGORA).isValid(AGORA)).isFalse();
    }

    /**
     * <b>É esta a garantia inteira do "pedir link novo mata o anterior".</b>
     * Um {@code isValid} que esquecesse o {@code revokedAt} deixaria todo link
     * antigo, em toda caixa de entrada antiga, continuar sendo chave.
     */
    @Test
    @DisplayName("Token revogado nao vale, mesmo dentro da janela")
    void revogadoNaoVale() {
        TalentBankAccessToken token = tokenQueExpiraEm(AGORA.plusHours(10));
        token.revoke(AGORA.minusMinutes(5));

        assertThat(token.isValid(AGORA)).isFalse();
    }

    /**
     * Revogar de novo não pode mover a data. Sem isso a resposta a "quando este
     * link morreu?" se perde na segunda chamada.
     */
    @Test
    @DisplayName("Revogar e idempotente e nao move a data original")
    void revogarEIdempotente() {
        TalentBankAccessToken token = tokenQueExpiraEm(AGORA.plusHours(10));
        LocalDateTime primeira = AGORA.minusHours(2);

        token.revoke(primeira);
        token.revoke(AGORA);

        assertThat(token.getRevokedAt()).isEqualTo(primeira);
    }
}
