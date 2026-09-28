package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import com.proautokimium.api.domain.entities.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReimbursementTest {

    private final Employee employee = new Employee();
    private final Employee reviewer = new Employee();
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 23, 10, 0);

    private Reimbursement newRequest() {
        return Reimbursement.request(
                employee, LocalDate.of(2026, 7, 20), new BigDecimal("150.00"),
                "Restaurante", "Almoço com cliente", "nota.jpg", "EMP001/nota.jpg", now
        );
    }

    @Test
    @DisplayName("Não deve criar reembolso com valor zero ou negativo")
    void naoDeveCriarComValorInvalido() {
        assertThrows(InvalidRequestDataException.class, () -> Reimbursement.request(
                employee, LocalDate.of(2026, 7, 20), BigDecimal.ZERO,
                "Restaurante", "Almoço", "nota.jpg", "path", now
        ));
        assertThrows(InvalidRequestDataException.class, () -> Reimbursement.request(
                employee, LocalDate.of(2026, 7, 20), new BigDecimal("-10"),
                "Restaurante", "Almoço", "nota.jpg", "path", now
        ));
    }

    @Test
    @DisplayName("Deve aprovar reembolso pendente")
    void deveAprovarPendente() {
        Reimbursement reimbursement = newRequest();

        reimbursement.approve(reviewer, "Ok, dentro da política", now.plusHours(1));

        assertThat(reimbursement.getStatus().name()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Não deve reprovar sem motivo")
    void naoDeveReprovarSemMotivo() {
        Reimbursement reimbursement = newRequest();

        assertThrows(InvalidRequestDataException.class, () -> reimbursement.reject(reviewer, null, now));
        assertThrows(InvalidRequestDataException.class, () -> reimbursement.reject(reviewer, "   ", now));
    }

    @Test
    @DisplayName("Não deve pagar reembolso que ainda não foi aprovado")
    void naoDevePagarSemAprovar() {
        Reimbursement reimbursement = newRequest();

        assertThrows(InvalidStatusTransitionException.class, () -> reimbursement.pay(LocalDate.of(2026, 8, 5), now));
    }

    @Test
    @DisplayName("Deve pagar reembolso aprovado e mudar status pra PAID")
    void devePagarReembolsoAprovado() {
        Reimbursement reimbursement = newRequest();
        reimbursement.approve(reviewer, "Ok", now);

        reimbursement.pay(LocalDate.of(2026, 8, 5), now.plusDays(1));

        assertThat(reimbursement.getStatus().name()).isEqualTo("PAID");
        assertThat(reimbursement.getPaymentDate()).isEqualTo(LocalDate.of(2026, 8, 5));
    }

    @Test
    @DisplayName("Não deve aprovar reembolso que já foi decidido")
    void naoDeveAprovarJaDecidido() {
        Reimbursement reimbursement = newRequest();
        reimbursement.reject(reviewer, "Fora da política", now);

        assertThrows(InvalidStatusTransitionException.class, () -> reimbursement.approve(reviewer, "ok", now));
    }

    // ─── Ninguém revisa o próprio pedido ─────────────────────────────────────

    private static void setId(com.proautokimium.api.domain.abstractions.Entity e, java.util.UUID id) throws Exception {
        java.lang.reflect.Field f = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(e, id);
    }

    private static Reimbursement pedidoDe(Employee dono) {
        return Reimbursement.request(dono, java.time.LocalDate.of(2026, 9, 1), new java.math.BigDecimal("50.00"),
                "Combustível", "visita", "nota.jpg", "EMP/nota.jpg", java.time.LocalDateTime.of(2026, 9, 1, 9, 0));
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
