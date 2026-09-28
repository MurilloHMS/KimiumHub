package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
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
        assertThrows(InvalidRequestDataException.class, () ->
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

        assertThrows(InvalidStatusTransitionException.class, () -> request.approve(reviewer, "de novo", now));
    }

    @Test
    @DisplayName("Não deve reprovar sem motivo")
    void naoDeveReprovarSemMotivo() {
        VacationRequest request = VacationRequest.request(
                employee, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), null, now
        );

        assertThrows(InvalidRequestDataException.class, () -> request.reject(reviewer, "  ", now));
        assertThrows(InvalidRequestDataException.class, () -> request.reject(reviewer, null, now));
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

    // ─── Ninguém revisa o próprio pedido ─────────────────────────────────────

    private static void setId(com.proautokimium.api.domain.abstractions.Entity e, java.util.UUID id) throws Exception {
        java.lang.reflect.Field f = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(e, id);
    }

    private static VacationRequest pedidoDe(Employee dono) {
        return VacationRequest.request(dono, java.time.LocalDate.of(2026, 10, 1), java.time.LocalDate.of(2026, 10, 10),
                null, java.time.LocalDateTime.of(2026, 9, 1, 9, 0));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("não aprova o próprio pedido, e o pedido continua pendente")
    void naoAprovaOProprio() {
        Employee dono = new Employee();
        var pedido = pedidoDe(dono);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.proautokimium.api.domain.exceptions.humanResources.SelfReviewException.class,
                () -> pedido.approve(dono, "ok", java.time.LocalDateTime.of(2026, 9, 2, 9, 0)));
        org.assertj.core.api.Assertions.assertThat(pedido.getStatus().name()).isEqualTo("PENDING");
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("não reprova o próprio pedido")
    void naoReprovaOProprio() {
        Employee dono = new Employee();
        var pedido = pedidoDe(dono);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.proautokimium.api.domain.exceptions.humanResources.SelfReviewException.class,
                () -> pedido.reject(dono, "motivo", java.time.LocalDateTime.of(2026, 9, 2, 9, 0)));
        org.assertj.core.api.Assertions.assertThat(pedido.getStatus().name()).isEqualTo("PENDING");
    }

    /**
     * O caso do Hibernate: o dono vem como proxy e o revisor como entidade
     * carregada — objetos diferentes, mesmo id. Comparar por referência
     * deixaria passar.
     */
    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("mesmo id em objetos diferentes ainda é a mesma pessoa")
    void mesmoIdEhAMesmaPessoa() throws Exception {
        java.util.UUID id = java.util.UUID.randomUUID();
        Employee dono = new Employee();
        setId(dono, id);
        Employee mesmaPessoa = new Employee();
        setId(mesmaPessoa, id);
        var pedido = pedidoDe(dono);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.proautokimium.api.domain.exceptions.humanResources.SelfReviewException.class,
                () -> pedido.approve(mesmaPessoa, "ok", java.time.LocalDateTime.of(2026, 9, 2, 9, 0)));
    }

    /**
     * O contrário: `Entity.equals` compara só o id, e dois funcionários ainda
     * sem id (null == null) seriam "iguais". Pessoas diferentes aprovam.
     */
    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("outra pessoa aprova normalmente, mesmo sem id")
    void outraPessoaAprova() {
        Employee dono = new Employee();
        var pedido = pedidoDe(dono);

        pedido.approve(new Employee(), "ok", java.time.LocalDateTime.of(2026, 9, 2, 9, 0));

        org.assertj.core.api.Assertions.assertThat(pedido.getStatus().name()).isEqualTo("APPROVED");
    }

    /** Conta sem funcionário vinculado (ADMIN técnico) não tem com quem comparar. */
    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("revisor sem funcionário vinculado não é tratado como dono")
    void revisorNuloNaoEhDono() {
        Employee dono = new Employee();
        var pedido = pedidoDe(dono);

        pedido.approve(null, "ok", java.time.LocalDateTime.of(2026, 9, 2, 9, 0));

        org.assertj.core.api.Assertions.assertThat(pedido.getStatus().name()).isEqualTo("APPROVED");
    }
}
