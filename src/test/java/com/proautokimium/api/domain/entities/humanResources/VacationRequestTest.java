package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VacationRequestTest {

    private final Employee employee = new Employee();
    private final Employee reviewer = new Employee();
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 23, 10, 0);

    @Test
    @DisplayName("daysRequested conta os dias corridos, incluindo o primeiro e o último")
    void deveContarDiasCorridosIncluindoExtremos() {
        VacationRequest request = VacationRequest.request(
                employee, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), null, now
        );

        assertThat(request.getDaysRequested()).isEqualTo(10);
    }

    @Test
    @DisplayName("Não deve criar solicitação com data final antes da inicial")
    void naoDeveCriarComDataFinalAntesDaInicial() {
        assertThrows(IllegalArgumentException.class, () ->
                VacationRequest.request(employee, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1), null, now)
        );
    }

    @Test
    @DisplayName("Deve aprovar solicitação pendente")
    void deveAprovarSolicitacaoPendente() {
        VacationRequest request = VacationRequest.request(
                employee, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), null, now
        );

        request.approve(reviewer, "Aprovado, sem conflito", now.plusDays(1));

        assertThat(request.getStatus().name()).isEqualTo("APPROVED");
        assertThat(request.getReviewedBy()).isEqualTo(reviewer);
    }

    @Test
    @DisplayName("Não deve aprovar solicitação que já foi decidida")
    void naoDeveAprovarSolicitacaoJaDecidida() {
        VacationRequest request = VacationRequest.request(
                employee, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), null, now
        );
        request.approve(reviewer, "ok", now);

        assertThrows(IllegalStateException.class, () -> request.approve(reviewer, "de novo", now));
    }

    @Test
    @DisplayName("Não deve reprovar sem motivo")
    void naoDeveReprovarSemMotivo() {
        VacationRequest request = VacationRequest.request(
                employee, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), null, now
        );

        assertThrows(IllegalArgumentException.class, () -> request.reject(reviewer, "  ", now));
        assertThrows(IllegalArgumentException.class, () -> request.reject(reviewer, null, now));
    }

    @Test
    @DisplayName("Deve reprovar solicitação pendente com motivo")
    void deveReprovarComMotivo() {
        VacationRequest request = VacationRequest.request(
                employee, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), null, now
        );

        request.reject(reviewer, "Conflito com outro funcionário do setor", now);

        assertThat(request.getStatus().name()).isEqualTo("REJECTED");
        assertThat(request.getReviewNotes()).isEqualTo("Conflito com outro funcionário do setor");
    }
}
