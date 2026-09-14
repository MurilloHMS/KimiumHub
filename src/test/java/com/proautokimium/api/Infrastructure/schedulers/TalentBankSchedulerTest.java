package com.proautokimium.api.Infrastructure.schedulers;

import com.proautokimium.api.Infrastructure.services.processoSeletivo.TalentBankAccessTokenService;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.TalentBankService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TalentBankSchedulerTest {

    @Mock TalentBankService talentBankService;
    @Mock TalentBankAccessTokenService tokenService;
    @InjectMocks TalentBankScheduler scheduler;

    /**
     * O laço mora aqui para que uma pessoa não segure as outras. Com uma
     * transação só para todos, o arquivo que falha no segundo desfaria o
     * primeiro — e toda madrugada travaria no mesmo lugar.
     */
    @Test
    @DisplayName("Uma falha no expurgo nao impede os candidatos seguintes")
    void falhaNaoInterrompe() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        when(talentBankService.idsVencidos()).thenReturn(List.of(a, b, c));
        when(talentBankService.expurgarSeVencido(a)).thenReturn(true);
        when(talentBankService.expurgarSeVencido(b)).thenThrow(new IOException("disco"));
        when(talentBankService.expurgarSeVencido(c)).thenReturn(true);

        scheduler.expurgarVencidos();

        verify(talentBankService).expurgarSeVencido(c);
    }

    @Test
    @DisplayName("Sem ninguem vencido, nao tenta expurgar nada")
    void semVencidos() throws Exception {
        when(talentBankService.idsVencidos()).thenReturn(List.of());

        scheduler.expurgarVencidos();

        verify(talentBankService, never()).expurgarSeVencido(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("A limpeza de tokens delega ao service")
    void limpaTokens() {
        scheduler.limparTokens();

        verify(tokenService).limparAntigos();
    }

    /**
     * Sem {@code zone}, o horário vira o da JVM: basta subir a imagem sem o
     * {@code TZ} para o expurgo das 3h30 rodar às 0h30 — ou, num servidor em
     * UTC-0, no meio do expediente.
     */
    @Test
    @DisplayName("Os tres agendamentos declaram o fuso de Sao Paulo")
    void fusoDeclarado() throws Exception {
        for (String metodo : List.of("limparTokens", "expurgarVencidos", "avisarQuemVenceEmBreve")) {
            Scheduled agenda = TalentBankScheduler.class.getMethod(metodo).getAnnotation(Scheduled.class);
            assertThat(agenda).as(metodo).isNotNull();
            assertThat(agenda.zone()).as(metodo).isEqualTo("America/Sao_Paulo");
        }
    }

    @Test
    @DisplayName("Uma falha no aviso nao impede os candidatos seguintes")
    void falhaNoAvisoNaoInterrompe() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(talentBankService.idsAVencerSemAviso()).thenReturn(List.of(a, b));
        when(talentBankService.avisarSeAVencer(a)).thenThrow(new IllegalStateException("smtp"));
        when(talentBankService.avisarSeAVencer(b)).thenReturn(true);

        scheduler.avisarQuemVenceEmBreve();

        verify(talentBankService).avisarSeAVencer(b);
    }
}
