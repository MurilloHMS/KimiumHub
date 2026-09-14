package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.TalentBankAccessTokenRepository;
import com.proautokimium.api.Infrastructure.services.secrets.CryptoTokenService;
import com.proautokimium.api.domain.entities.processoSeletivo.TalentBankAccessToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TalentBankAccessTokenServiceTest {

    @Mock TalentBankAccessTokenRepository repository;
    @Mock CryptoTokenService cryptoTokenService;

    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-09-14T14:30:00Z"), ZoneId.of("America/Sao_Paulo"));

    /**
     * Apagado no minuto em que vence, o link de ontem responderia 404 — "confira
     * se copiou o endereço" — em vez de 410, "este link expirou, peça outro".
     */
    @Test
    @DisplayName("A limpeza so leva token vencido ou revogado ha mais de sete dias")
    void limpezaEsperaSeteDias() {
        LocalDateTime seteDiasAtras = LocalDateTime.of(2026, 9, 7, 11, 30);
        List<TalentBankAccessToken> antigos = List.of(new TalentBankAccessToken(), new TalentBankAccessToken());
        when(repository.paraLimpar(seteDiasAtras, seteDiasAtras)).thenReturn(antigos);

        var service = new TalentBankAccessTokenService(repository, cryptoTokenService, RELOGIO);

        assertThat(service.limparAntigos()).isEqualTo(2);
        verify(repository).deleteAll(antigos);
    }
}
